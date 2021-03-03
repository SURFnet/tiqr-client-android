package org.tiqr.authenticator.scan

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.core.view.doOnLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.tiqr.authenticator.R
import org.tiqr.authenticator.TiqrApplication
import org.tiqr.authenticator.auth.AuthenticationChallenge
import org.tiqr.authenticator.auth.EnrollmentChallenge
import org.tiqr.authenticator.authentication.AuthenticationActivityGroup
import org.tiqr.authenticator.base.BaseActivity
import org.tiqr.authenticator.databinding.ScanBinding
import org.tiqr.authenticator.enrollment.EnrollmentActivityGroup
import org.tiqr.service.authentication.AuthenticationService
import org.tiqr.service.authentication.AuthenticationService.OnParseAuthenticationChallengeListener
import org.tiqr.service.authentication.ParseAuthenticationChallengeError
import org.tiqr.service.enrollment.EnrollmentService
import org.tiqr.service.enrollment.EnrollmentService.OnParseEnrollmentChallengeListener
import org.tiqr.service.enrollment.ParseEnrollmentChallengeError
import javax.inject.Inject

class ScanActivity : BaseActivity<ScanBinding>() {
    override val layout: Int = R.layout.scan

    @Inject
    internal lateinit var enrollmentService: EnrollmentService

    @Inject
    internal lateinit var authenticationService: AuthenticationService

    private lateinit var scanComponent: ScanComponent
    private lateinit var broadcastManager: LocalBroadcastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TiqrApplication.component(this).inject(this)
        broadcastManager = LocalBroadcastManager.getInstance(this)

        binding.headerView.apply {
            hideRightButton()
            setOnLeftClickListener { onBackPressed() }
        }

        binding.viewFinder.doOnLayout {
            scanComponent = ScanComponent(
                    context = this,
                    lifecycleOwner = this,
                    viewFinder = binding.viewFinder,
                    viewFinderRatio = it.height.toFloat() / it.width.toFloat()
            ) { result ->
                binding.progress.show()
                if (enrollmentService.isEnrollmentChallenge(result)) {
                    enroll(result)
                } else {
                    authenticate(result)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_FOCUS,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                ScanKeyEventsReceiver.createEvent(keyCode).run {
                    broadcastManager.sendBroadcast(this)
                }
                true // Mark as handled since we sent the broadcast because currently scanning
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    /**
     * Parse authentication challenge and start authentication process.
     *
     * @param challenge
     */
    private fun authenticate(challenge: String) {
        authenticationService.parseAuthenticationChallenge(challenge, object : OnParseAuthenticationChallengeListener {
            override fun onParseAuthenticationChallengeSuccess(challenge: AuthenticationChallenge) {
                val intent = Intent(this@ScanActivity, AuthenticationActivityGroup::class.java)
                intent.putExtra("org.tiqr.challenge", challenge)
                intent.putExtra("org.tiqr.protocolVersion", "2")
                startActivity(intent)
                finish()
            }

            override fun onParseAuthenticationChallengeError(error: ParseAuthenticationChallengeError) {
                binding.progress.hide()

                AlertDialog.Builder(this@ScanActivity)
                        .setTitle(error.title)
                        .setMessage(error.message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok_button) { _, _ -> scanComponent.resumeScanning() }
                        .show()
            }
        })
    }

    /**
     * Parse enrollment challenge and start enrollment process.
     *
     * @param challenge
     */
    private fun enroll(challenge: String) {
        enrollmentService.parseEnrollmentChallenge(challenge, object : OnParseEnrollmentChallengeListener {
            override fun onParseEnrollmentChallengeSuccess(challenge: EnrollmentChallenge) {
                val intent = Intent(this@ScanActivity, EnrollmentActivityGroup::class.java)
                intent.putExtra("org.tiqr.challenge", challenge)
                intent.putExtra("org.tiqr.protocolVersion", "2")
                startActivity(intent)
                finish()
            }

            override fun onParseEnrollmentChallengeError(error: ParseEnrollmentChallengeError) {
                binding.progress.hide()

                AlertDialog.Builder(this@ScanActivity)
                        .setTitle(error.title)
                        .setMessage(error.message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok_button) { _, _ -> scanComponent.resumeScanning() }
                        .show()
            }
        })
    }
}
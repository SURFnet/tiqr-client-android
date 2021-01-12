package org.tiqr.authenticator

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tiqr.authenticator.auth.AuthenticationChallenge
import org.tiqr.authenticator.auth.EnrollmentChallenge
import org.tiqr.authenticator.authentication.AuthenticationActivityGroup
import org.tiqr.authenticator.base.BaseActivity
import org.tiqr.authenticator.databinding.ActivityMainBinding
import org.tiqr.authenticator.datamodel.DbAdapter
import org.tiqr.authenticator.dialog.ActivityDialog
import org.tiqr.authenticator.enrollment.EnrollmentActivityGroup
import org.tiqr.service.authentication.AuthenticationService
import org.tiqr.service.authentication.ParseAuthenticationChallengeError
import org.tiqr.service.enrollment.EnrollmentService
import org.tiqr.service.enrollment.ParseEnrollmentChallengeError
import javax.inject.Inject

class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    internal lateinit var enrollmentService: EnrollmentService

    @Inject
    internal lateinit var authenticationService: AuthenticationService

    @Inject
    internal lateinit var dbAdapter: DbAdapter

    private var activityDialog: ActivityDialog? = null


    override val layout = R.layout.activity_main

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TiqrApplication.component(this).inject(this)

        binding.header.hideLeftButton()

        val contentResource: Int
        if (dbAdapter.identityCount() > 0) {
            contentResource = R.string.main_text_instructions
        } else {
            binding.header.hideRightButton()
            contentResource = R.string.main_text_welcome
        }

        loadContentsIntoWebView(contentResource, binding.webview)

        binding.btnScan.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CAMERA)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScanning()
                }
            }
        }
    }

    private fun startScanning() {
        // TODO: open scan activity
    }

    private fun loadContentsIntoWebView(contentResourceId: Int, webView: WebView) {
        val data = getString(contentResourceId)
        //needed to render chars correctly
        webView.loadDataWithBaseURL(null, data, "text/html", "utf-8", null)
        webView.setBackgroundColor(Color.TRANSPARENT)
    }

    public override fun onStart() {
        super.onStart()
        // Handle tiqrauth:// and tiqrenroll:// URLs
        val intent = intent
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            // Perform the actions needed to parse the raw challenge
            activityDialog = ActivityDialog.show(this)
            val rawChallenge = intent.dataString
            if (enrollmentService.isEnrollmentChallenge(rawChallenge!!)) {
                enroll(rawChallenge)
            } else {
                authenticate(rawChallenge)
            }
        }
    }

    /**
     * Parse authentication challenge and start authentication process.
     *
     * @param challenge The raw authentication challenge.
     */
    private fun authenticate(challenge: String) {
        authenticationService.parseAuthenticationChallenge(challenge, object : AuthenticationService.OnParseAuthenticationChallengeListener {
            override fun onParseAuthenticationChallengeSuccess(challenge: AuthenticationChallenge) {
                activityDialog?.cancel()
                val intent = Intent(applicationContext, AuthenticationActivityGroup::class.java)
                intent.putExtra("org.tiqr.challenge", challenge)
                intent.putExtra("org.tiqr.protocolVersion", "2")
                startActivity(intent)
            }

            override fun onParseAuthenticationChallengeError(error: ParseAuthenticationChallengeError) {
                activityDialog?.cancel()
                AlertDialog.Builder(this@MainActivity)
                        .setTitle(error.title)
                        .setMessage(error.message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok_button, null)
                        .show()
            }
        })
    }

    /**
     * Parse enrollment challenge and start enrollment process.
     *
     * @param challenge The raw challenge to enroll for.
     */
    private fun enroll(challenge: String) {
        enrollmentService.parseEnrollmentChallenge(challenge, object : EnrollmentService.OnParseEnrollmentChallengeListener {
            override fun onParseEnrollmentChallengeSuccess(challenge: EnrollmentChallenge) {
                activityDialog?.cancel()
                val intent = Intent(applicationContext, EnrollmentActivityGroup::class.java)
                intent.putExtra("org.tiqr.challenge", challenge)
                intent.putExtra("org.tiqr.protocolVersion", "2")
                startActivity(intent)
            }

            override fun onParseEnrollmentChallengeError(error: ParseEnrollmentChallengeError) {
                activityDialog?.cancel()

                AlertDialog.Builder(this@MainActivity)
                        .setTitle(error.title)
                        .setMessage(error.message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok_button, null)
                        .show()
            }
        })
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 42
    }
}

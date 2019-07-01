package org.tiqr.authenticator

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import org.tiqr.authenticator.base.BaseActivity
import org.tiqr.authenticator.databinding.ActivityAboutBinding
import org.tiqr.authenticator.general.HeaderView

/**
 * About screen.
 */
class AboutActivity : BaseActivity<ActivityAboutBinding>() {

    override val layout = R.layout.activity_about


    /**
     * Create activity.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)

        val headerView = findViewById<View>(R.id.headerView) as HeaderView
        headerView.setOnLeftClickListener(View.OnClickListener { onBackPressed() })
        headerView.hideRightButton()

        val versionTextView = findViewById<View>(R.id.versionName) as TextView
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            versionTextView.text = getString(R.string.app_name) + " " + versionName
        } catch (e: PackageManager.NameNotFoundException) {
            versionTextView.visibility = View.GONE
        }

        val logoSurfnet = findViewById<View>(R.id.logo_surfnet) as ImageView
        logoSurfnet.setOnClickListener { openURL("http://www.surfnet.nl/") }

        val logoEgeniq = findViewById<View>(R.id.logo_egeniq) as ImageView
        logoEgeniq.setOnClickListener { openURL("http://www.egeniq.com/") }

        val logoTiqr = findViewById<View>(R.id.logo_tiqr) as ImageView
        logoTiqr.setOnClickListener { openURL("http://tiqr.org/") }

        val logoStroomt = findViewById<View>(R.id.logo_stroomt) as ImageView
        logoStroomt.setOnClickListener { openURL("http://www.stroomt.com/") }
        val debugInfo = findViewById<View>(R.id.debug_info) as TextView
        debugInfo.text = getDebugInfo()
    }

    /**
     * Returns informations about the device the app is running on.
     *
     * @return OS and device details as a string, to display to the user.
     */
    private fun getDebugInfo(): String {
        var result = "Debug informations (make a screenshot of this when submitting a bug report):"
        result += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")"
        result += "\n API Level: " + android.os.Build.VERSION.SDK_INT
        result += "\n Device: " + android.os.Build.DEVICE
        result += "\n Model and product: " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")"
        return result
    }

    /**
     * Open URL in browser.
     *
     * @param url url
     */
    private fun openURL(url: String) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }
}

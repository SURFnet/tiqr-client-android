package org.tiqr.authenticator.general

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

import org.tiqr.authenticator.AboutActivity
import org.tiqr.authenticator.R

class FooterView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    init {

        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutInflater.inflate(R.layout.view_footer, this)

        val infoImage = findViewById<View>(R.id.footer_icon_info) as ImageView
        infoImage.setOnClickListener { showAboutActivity() }

        val surfnetImage = findViewById<View>(R.id.footer_surfnet_logo) as ImageView
        surfnetImage.setOnClickListener { openWebURL("http://www.surfnet.nl") }
    }

    fun hideInfoIcon() {
        val infoImage = findViewById<View>(R.id.footer_icon_info) as ImageView
        infoImage.visibility = View.GONE
    }

    /**
     * Start the About activity view
     */
    private fun showAboutActivity() {
        val intent = Intent(context, AboutActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * When a user clicks one of the icons, open a browser
     *
     * @param inURL
     */
    private fun openWebURL(inURL: String) {
        val browse = Intent(Intent.ACTION_VIEW, Uri.parse(inURL))
        browse.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(browse)
    }
}
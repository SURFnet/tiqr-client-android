package org.tiqr.authenticator.general

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

import org.tiqr.authenticator.R
import org.tiqr.authenticator.identity.IdentityAdminActivity

/**
 * Created by andrei on 14/07/15.
 */
class HeaderView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private var leftButton: ImageView
    private var rightButton: ImageView

    init {

        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutInflater.inflate(R.layout.tiqr_titlebar, this)

        leftButton = findViewById(R.id.left_button)
        rightButton = findViewById(R.id.right_button)

        setOnRightClickListener(OnClickListener { doIdentityAdmin() })
    }

    fun setOnLeftClickListener(listener: OnClickListener) {
        leftButton.setOnClickListener(listener)
    }

    fun setRightIcon(resourceId: Int) {
        rightButton.setImageResource(resourceId)
    }

    fun setOnRightClickListener(listener: OnClickListener) {
        rightButton.setOnClickListener(listener)
    }

    fun hideRightButton() {
        rightButton.visibility = View.GONE
    }

    fun hideLeftButton() {
        leftButton.visibility = View.GONE
    }

    fun doIdentityAdmin() {
        val identityIntent = Intent(context, IdentityAdminActivity::class.java)
        context.startActivity(identityIntent)
    }
}

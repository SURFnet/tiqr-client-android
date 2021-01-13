/*
 * Copyright (c) 2010-2019 SURFnet bv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of SURFnet bv nor the names of its contributors
 *    may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.tiqr.authenticator

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import androidx.transition.TransitionManager
import org.tiqr.authenticator.base.BindingActivity
import org.tiqr.authenticator.databinding.ActivityMainBinding
import org.tiqr.authenticator.scan.ScanKeyEventsReceiver
import org.tiqr.authenticator.scan.ScanFragment
import org.tiqr.authenticator.util.extensions.currentNavigationFragment

class MainActivity : BindingActivity<ActivityMainBinding>(), NavController.OnDestinationChangedListener {
    private lateinit var navController: NavController

    @LayoutRes
    override val layout = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from launch theme to regular theme
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        navController = findNavController(R.id.nav_host_fragment)
        with(navController) {
            setSupportActionBar(binding.toolbar)
            setupActionBarWithNavController(this,
                    AppBarConfiguration.Builder(
                            setOf(R.id.start, R.id.enrollment_summary, R.id.authentication_summary)
                    ).build()
            )
            supportActionBar?.setDisplayShowTitleEnabled(false)

            addOnDestinationChangedListener(this@MainActivity)

            Navigation.setViewNavController(binding.bottombar, this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        navController.removeOnDestinationChangedListener(this)
    }

    override fun onSupportNavigateUp() = navController.navigateUp() || super.onSupportNavigateUp()

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        when (destination.id) { // Toggle FLAG_SECURE
            R.id.enrollment_pin,
            R.id.enrollment_pin_verify,
            R.id.authentication_pin -> window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else -> window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        when (destination.id) {
            R.id.scan,
            R.id.about,
            R.id.authentication_pin,
            R.id.enrollment_pin,
            R.id.enrollment_pin_verify -> toggleBottomBar(visible = false, infoVisible = false)
            R.id.authentication_summary,
            R.id.enrollment_confirm,
            R.id.enrollment_summary,
            R.id.identity_list,
            R.id.identity_detail -> toggleBottomBar(visible = true, infoVisible = false)
            else -> toggleBottomBar(visible = true)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_FOCUS,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (supportFragmentManager.currentNavigationFragment<ScanFragment>() != null) {
                    ScanKeyEventsReceiver.createEvent(keyCode).run {
                        LocalBroadcastManager.getInstance(this@MainActivity)
                                .sendBroadcast(this)
                    }
                    true // Mark as handled since we sent the broadcast because currently scanning
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    /**
     * Toggle the bottom bar visibility with slide animation.
     */
    private fun toggleBottomBar(visible: Boolean, infoVisible: Boolean = true) {
        TransitionManager.endTransitions(binding.container)
        binding.bottombar.children.forEach {
            it.clearAnimation()
        }

        val transition = AutoTransition()
        transition.addListener(object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                if (infoVisible.not()) binding.bottombar.infoVisible = infoVisible
            }

            override fun onTransitionStart(transition: Transition) {
                if (infoVisible) binding.bottombar.infoVisible = infoVisible
            }
        })

        ConstraintSet().apply {
            clone(binding.container)

            if (visible) {
                clear(binding.bottombar.id, ConstraintSet.TOP)
                connect(binding.bottombar.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            } else {
                clear(binding.bottombar.id, ConstraintSet.BOTTOM)
                connect(binding.bottombar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            }

            applyTo(binding.container)
        }

        TransitionManager.beginDelayedTransition(binding.container, transition)
    }
}

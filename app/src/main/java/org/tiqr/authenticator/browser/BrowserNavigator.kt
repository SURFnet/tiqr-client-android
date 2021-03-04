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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
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

package org.tiqr.authenticator.browser

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.ClassType
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.Navigator.Name
import org.tiqr.authenticator.R
import timber.log.Timber

/**
 * [Navigator] to add a browser [NavDestination]
 * which opens the link in Chrome Custom Tabs, or fallbacks to the default browser.
 */
@Name("browser")
class BrowserNavigator(private val context: Context) : Navigator<BrowserNavigator.Destination>() {
    private val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                            .setToolbarColor(ContextCompat.getColor(context, R.color.primaryColor))
                            .build()
            )
            .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
            .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()

    override fun navigate(destination: Destination, args: Bundle?, navOptions: NavOptions?, navigatorExtras: Extras?): NavDestination? {
        val url = args?.getString("url") ?: return null
        val link = url.toUri()
        val referrer = "android-app://${context.packageName}".toUri()

        if (destination.isChromeCustomTabSupported) {
            customTabsIntent.apply {
                intent.putExtra(Intent.EXTRA_REFERRER, referrer)
                launchUrl(context, link)
            }
        } else {
            // Open with the default browser
            Intent(Intent.ACTION_VIEW, link).apply {
                putExtra(Intent.EXTRA_REFERRER, referrer)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.run {
                try {
                    context.startActivity(this)
                } catch (e: ActivityNotFoundException) {
                    // Very unlikely, but better to guard against this
                    Timber.e(e, "Cannot open the browser")
                    Toast.makeText(context, R.string.browser_error_launch, Toast.LENGTH_SHORT).show()
                }
            }
        }

        return null // Do not add to the back stack, managed by Chrome Custom Tabs
    }

    override fun createDestination(): Destination = Destination(context, this)

    override fun popBackStack() = true // Managed by Chrome Custom Tabs

    @ClassType(Activity::class)
    class Destination(context: Context, navigator: Navigator<out NavDestination>) : NavDestination(navigator) {
        val isChromeCustomTabSupported = context.isChromeCustomTabSupported()

        /**
         * Check if Chrome Custom Tabs is supported
         */
        private fun Context.isChromeCustomTabSupported(): Boolean {
            // Get default VIEW intent handler that can view a web url.
            val activityIntent = Intent()
                    .setAction(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.fromParts("http", "", null))

            // Get all apps that can handle VIEW intents.
            val pm = packageManager
            val resolvedActivities = pm.queryIntentActivities(activityIntent, 0)
            resolvedActivities.forEach { info ->
                Intent().apply {
                    action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
                    setPackage(info.activityInfo.packageName)
                }.run {
                    return pm.resolveService(this, 0) != null
                }
            }
            return false
        }
    }
}


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

package org.tiqr.authenticator.util.databinding

import android.annotation.SuppressLint
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.parseAsHtml
import androidx.databinding.BindingAdapter
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.tiqr.authenticator.MainNavDirections
import org.tiqr.authenticator.R

/**
 * Parse the string to html
 */
@BindingAdapter(value = ["htmlText"])
fun TextView.htmlText(html: String) {
    text = html.parseAsHtml()
}

/**
 * Parse the string resource to html
 */
@BindingAdapter(value = ["htmlText"])
fun TextView.htmlText(@StringRes html: Int) {
    text = context.getString(html).parseAsHtml()
}

/**
 * Enable (or disable) clickable links
 */
@BindingAdapter(value = ["enableLinks"])
fun TextView.enableLinks(enable: Boolean) {
    if (enable) {
        BetterLinkMovementMethod
                .linkify(Linkify.WEB_URLS, this)
                .setOnLinkClickListener { _, url ->
                    findNavController().navigate(MainNavDirections.openBrowser(url))
                    true
                }
    }
}

/**
 * Get the app name and version
 */
@SuppressLint("SetTextI18n")
@BindingAdapter(value = ["appName"])
fun TextView.appName(appName: String) {
    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    text = context.getString(R.string.about_label_version, appName, versionName)
}

/**
 * Open browser with specified url
 */
@BindingAdapter(value = ["openBrowser"])
fun View.openBrowser(url: String) {
    if (url.isEmpty()) return
    setOnClickListener(Navigation.createNavigateOnClickListener(MainNavDirections.openBrowser(url)))
}
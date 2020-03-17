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

package org.tiqr.authenticator.widget

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import androidx.core.content.withStyledAttributes
import androidx.navigation.findNavController
import com.google.android.material.bottomappbar.BottomAppBar
import org.tiqr.authenticator.MainNavDirections
import org.tiqr.authenticator.R
import org.tiqr.authenticator.util.Urls.URL_SURFNET

/**
 * Custom [BottomAppBar] to display and handle the Info and Surfnet actions.
 */
class BottomBarView : BottomAppBar {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.withStyledAttributes(attrs, R.styleable.BottomBarView) {
            infoVisible = getBoolean(R.styleable.BottomBarView_showInfo, true)
        }
    }

    var infoVisible: Boolean = true
        set(value) {
            if (value) {
                setNavigationIcon(R.drawable.ic_info)
            } else {
                navigationIcon = null
            }

            field = value
        }

    init {
        layoutTransition = LayoutTransition() // = animateLayoutChanges

        setNavigationOnClickListener {
            findNavController().navigate(MainNavDirections.openAbout())
        }

        LayoutInflater.from(context)
                .inflate(R.layout.view_bottombar, this, true).run {
                    findViewById<ImageButton>(R.id.surfnet)
                }.setOnClickListener {
                    findNavController().navigate(MainNavDirections.openBrowser(URL_SURFNET))
                }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        infoVisible = infoVisible
    }
}
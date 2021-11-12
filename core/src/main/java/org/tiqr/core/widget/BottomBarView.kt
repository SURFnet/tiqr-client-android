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

package org.tiqr.core.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.google.android.material.bottomappbar.BottomAppBar
import org.tiqr.core.MainNavDirections
import org.tiqr.core.R
import org.tiqr.core.databinding.ViewBottombarBinding
import org.tiqr.core.util.Urls

/**
 * Custom [BottomAppBar] to display and handle the Info and Surfnet actions.
 */
class BottomBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var infoVisible: Boolean = true
        set(value) {
            binding.leftIconView.isVisible = value
            field = value
        }

    var rightIcon: Drawable? = null
        set(value) {
            setRightIconAndEnableListener(value)
        }

    var leftIcon: Drawable? = null
        set(value) {
            setLeftIconAndEnableListener(value)
        }
    var binding = ViewBottombarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.leftIconView.setImageDrawable(leftIcon)
        binding.rightIconView.setImageDrawable(rightIcon)
        context.withStyledAttributes(attrs, R.styleable.BottomBarView) {
            infoVisible = getBoolean(R.styleable.BottomBarView_showInfo, true)
        }
    }

    private fun setRightIconAndEnableListener(value: Drawable?) {
        binding.rightIconView.setImageDrawable(value)
        binding.rightIconView.setOnClickListener {
            findNavController().navigate(MainNavDirections.openBrowser(Urls.URL_SURFNET))
        }
    }

    private fun setLeftIconAndEnableListener(value: Drawable?) {
        binding.leftIconView.setImageDrawable(value)
        binding.leftIconView.setOnClickListener {
            findNavController().navigate(MainNavDirections.openAbout())
        }
    }
}

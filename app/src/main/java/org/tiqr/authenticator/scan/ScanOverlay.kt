/*
 * Copyright (c) 2010-2020 SURFnet bv
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

package org.tiqr.authenticator.scan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.*
import org.tiqr.authenticator.R

/**
 * Overlay view for displaying a scan-frame.
 */
class ScanOverlay : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val maskPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ColorUtils.setAlphaComponent(Color.BLACK, 0x99)
    }

    private val frameBorderPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 0f // hairline
        style = Paint.Style.STROKE
        color = Color.BLACK
    }

    private val framePaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.primaryColor)
        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, context.resources.displayMetrics)
        strokeCap = Paint.Cap.ROUND
    }

    private var maskPath: Path? = null
    private var frameRect: RectF? = null
    private val framePath: Path = Path()
    private val frameCornerSize: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, context.resources.displayMetrics).toInt()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val frameRect = frameRect ?: return
        val maskPath = maskPath ?: return

        val (left, top, right, bottom) = frameRect

        canvas?.apply {
            // Draw mask around frame
            drawPath(maskPath, maskPaint)

            // Draw the frame border
            drawRect(left, top, right + 0.5f, bottom + 0.5f, frameBorderPaint)

            // Draw the frame corners
            with(framePath) {
                reset()
                // draw top left corner
                moveTo(left, top + frameCornerSize)
                lineTo(left, top)
                lineTo(left + frameCornerSize, top)
                // draw top right corner
                moveTo(right - frameCornerSize, top)
                lineTo(right, top)
                lineTo(right, top + frameCornerSize)
                // draw bottom right corner
                moveTo(right, bottom - frameCornerSize)
                lineTo(right, bottom)
                lineTo(right - frameCornerSize, bottom)
                // draw bottom left corner
                moveTo(left + frameCornerSize, bottom)
                lineTo(left, bottom)
                lineTo(left, bottom - frameCornerSize)
            }
            drawPath(framePath, framePaint)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        invalidateFrameRect(right - left, bottom - top)
        invalidateMaskPath(right - left, bottom - top)
    }

    /**
     * Calculate the frame rect
     */
    private fun invalidateFrameRect(w: Int = width, h: Int = height) {
        frameRect = getCenterBoxFrame(w, h)
    }

    /**
     * Calculate the mask path
     */
    private fun invalidateMaskPath(w: Int = width, h: Int = height) {
        val frameRect = frameRect ?: return

        val viewPath = Path().apply {
            addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
        }

        val framePath = Path().apply {
            addRect(frameRect, Path.Direction.CW)
        }

        maskPath = viewPath.minus(framePath)
    }
}
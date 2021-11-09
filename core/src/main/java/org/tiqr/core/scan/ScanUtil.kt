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

package org.tiqr.core.scan

import android.graphics.RectF

private const val frameAspectRatio = 1.0f
private const val frameSizePercentage = 0.75f

/**
 * Calculate the center box.
 */
internal fun getCenterBoxFrame(width: Int, height: Int, sizePercentage: Float = frameSizePercentage): RectF? {
    if (width > 0 && height > 0) {
        val viewRatio = width.toFloat() / height.toFloat()
        val frameWidth: Float
        val frameHeight: Float
        if (viewRatio <= frameAspectRatio) {
            frameWidth = width.toFloat() * sizePercentage
            frameHeight = frameWidth / frameAspectRatio
        } else {
            frameHeight = height * sizePercentage
            frameWidth = frameHeight * frameAspectRatio
        }
        val frameLeft = (width - frameWidth) / 2
        val frameTop = (height - frameHeight) / 2

        return RectF(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight)
    } else {
        return null
    }
}
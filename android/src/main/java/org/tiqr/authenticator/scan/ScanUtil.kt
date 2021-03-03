package org.tiqr.authenticator.scan

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
package org.tiqr.authenticator.scan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.core.graphics.minus
import org.tiqr.authenticator.R

/**
 * Overlay view for displaying a scan-frame.
 */
class ScanFrameOverlay : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val maskPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ColorUtils.setAlphaComponent(Color.BLACK, 0x99)
    }

    private val frameBorderPaint: Paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 0f // hairline
        style = Paint.Style.STROKE
        color = Color.BLACK
    }

    private val framePaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.tiqr_green)
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

        maskPath = viewPath - framePath
    }
}
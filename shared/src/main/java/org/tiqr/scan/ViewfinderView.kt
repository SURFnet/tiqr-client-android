/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tiqr.scan

import com.google.zxing.ResultPoint

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

import org.tiqr.R

import java.util.ArrayList

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
class ViewfinderView// This constructor is used when the class is built from an XML resource.
(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val _paint: Paint
    private var _resultBitmap: Bitmap? = null
    private val _maskColor: Int
    private val _resultColor: Int
    private val _laserColor: Int
    private val _resultPointColor: Int
    private var _scannerAlpha: Int = 0
    private var _possibleResultPoints: MutableList<ResultPoint>? = null
    private var _lastPossibleResultPoints: List<ResultPoint>? = null
    private var _previewWidth: Int = 0
    private var _previewHeight: Int = 0
    private var _finderArea: Rect? = null

    init {

        // Initialize these once for performance rather than calling them every time in onDraw().
        _paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val resources = resources
        _maskColor = resources.getColor(R.color.viewfinder_mask)
        _resultColor = resources.getColor(R.color.result_view)
        _laserColor = resources.getColor(R.color.viewfinder_laser)
        _resultPointColor = resources.getColor(R.color.possible_result_points)
        _scannerAlpha = 0
        _possibleResultPoints = ArrayList(5)
        _lastPossibleResultPoints = null
    }

    /**
     * Sets the camera preview frame resolution.
     *
     * @param width
     * @param height
     */
    fun setPreviewSize(width: Int, height: Int) {
        _previewWidth = width
        _previewHeight = height
    }

    /**
     * Sets the view finder area.
     *
     * @param finderArea Finder area.
     */
    fun setFinderArea(finderArea: Rect) {
        _finderArea = finderArea
    }

    @SuppressLint("DrawAllocation")
    public override fun onDraw(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height

        val frame = if (_finderArea != null) _finderArea else Rect(0, 0, width, height)

        frame?.also {

            val previewFrame = Rect()
            previewFrame.left = frame.left * _previewWidth / width
            previewFrame.right = frame.right * _previewWidth / width
            previewFrame.top = frame.top * _previewHeight / height
            previewFrame.bottom = frame.bottom * _previewHeight / height

            // Draw the exterior (i.e. outside the framing rect) darkened
            _paint.color = if (_resultBitmap != null) _resultColor else _maskColor
            canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), _paint)
            canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), (frame.bottom + 1).toFloat(), _paint)
            canvas.drawRect((frame.right + 1).toFloat(), frame.top.toFloat(), width.toFloat(), (frame.bottom + 1).toFloat(), _paint)
            canvas.drawRect(0f, (frame.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), _paint)

            if (_resultBitmap != null) {
                // Draw the opaque result bitmap over the scanning rectangle
                _paint.alpha = CURRENT_POINT_OPACITY
                canvas.drawBitmap(_resultBitmap!!, null, frame, _paint)
            } else {

                // Draw a red "laser scanner" line through the middle to show decoding is active
                _paint.color = _laserColor
                _paint.alpha = SCANNER_ALPHA[_scannerAlpha]
                _scannerAlpha = (_scannerAlpha + 1) % SCANNER_ALPHA.size
                val middle = frame.height() / 2 + frame.top
                canvas.drawRect((frame.left + 2).toFloat(), (middle - 1).toFloat(), (frame.right - 1).toFloat(), (middle + 2).toFloat(), _paint)

                val scaleX = frame.width() / previewFrame.width().toFloat()
                val scaleY = frame.height() / previewFrame.height().toFloat()

                val currentPossible = _possibleResultPoints
                val currentLast = _lastPossibleResultPoints
                val frameLeft = frame.left
                val frameTop = frame.top
                if (currentPossible!!.isEmpty()) {
                    _lastPossibleResultPoints = null
                } else {
                    _possibleResultPoints = ArrayList(5)
                    _lastPossibleResultPoints = currentPossible
                    _paint.alpha = CURRENT_POINT_OPACITY
                    _paint.color = _resultPointColor
                    synchronized(currentPossible) {
                        for (point in currentPossible) {
                            canvas.drawCircle((frameLeft + (point.x * scaleX).toInt()).toFloat(),
                                    (frameTop + (point.y * scaleY).toInt()).toFloat(),
                                    POINT_SIZE.toFloat(), _paint)
                        }
                    }
                }
                if (currentLast != null) {
                    _paint.alpha = CURRENT_POINT_OPACITY / 2
                    _paint.color = _resultPointColor
                    synchronized(currentLast) {
                        val radius = POINT_SIZE / 2.0f
                        for (point in currentLast) {
                            canvas.drawCircle((frameLeft + (point.x * scaleX).toInt()).toFloat(),
                                    (frameTop + (point.y * scaleY).toInt()).toFloat(),
                                    radius, _paint)
                        }
                    }
                }

                // Request another update at the animation interval, but only repaint the laser line,
                // not the entire viewfinder mask.
                postInvalidateDelayed(ANIMATION_DELAY,
                        frame.left - POINT_SIZE,
                        frame.top - POINT_SIZE,
                        frame.right + POINT_SIZE,
                        frame.bottom + POINT_SIZE)
            }
        }
    }

    fun drawViewfinder() {
        val resultBitmap = this._resultBitmap
        this._resultBitmap = null
        resultBitmap?.recycle()
        invalidate()
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    fun drawResultBitmap(barcode: Bitmap) {
        _resultBitmap = barcode
        invalidate()
    }

    fun addPossibleResultPoint(point: ResultPoint) {
        val points = _possibleResultPoints
        points?.also {
            synchronized(points) {
                points.add(point)
                val size = points.size
                if (size > MAX_RESULT_POINTS) {
                    // trim it
                    points.subList(0, size - MAX_RESULT_POINTS / 2).clear()
                }
            }
        }
    }

    companion object {

        private val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
        private val ANIMATION_DELAY = 80L
        private val CURRENT_POINT_OPACITY = 0xA0
        private val MAX_RESULT_POINTS = 20
        private val POINT_SIZE = 6
    }

}
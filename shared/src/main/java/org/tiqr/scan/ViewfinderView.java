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

package org.tiqr.scan;

import com.google.zxing.ResultPoint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import org.tiqr.R;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;

    private final Paint _paint;
    private Bitmap _resultBitmap;
    private final int _maskColor;
    private final int _resultColor;
    private final int _laserColor;
    private final int _resultPointColor;
    private int _scannerAlpha;
    private List<ResultPoint> _possibleResultPoints;
    private List<ResultPoint> _lastPossibleResultPoints;
    private int _previewWidth;
    private int _previewHeight;
    private Rect _finderArea;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        _paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        _maskColor = resources.getColor(R.color.viewfinder_mask);
        _resultColor = resources.getColor(R.color.result_view);
        _laserColor = resources.getColor(R.color.viewfinder_laser);
        _resultPointColor = resources.getColor(R.color.possible_result_points);
        _scannerAlpha = 0;
        _possibleResultPoints = new ArrayList<ResultPoint>(5);
        _lastPossibleResultPoints = null;
    }

    /**
     * Sets the camera preview frame resolution.
     *
     * @param width
     * @param height
     */
    public void setPreviewSize(int width, int height) {
        _previewWidth = width;
        _previewHeight = height;
    }

    /**
     * Sets the view finder area.
     *
     * @param finderArea Finder area.
     */
    public void setFinderArea(Rect finderArea) {
        _finderArea = finderArea;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        Rect frame = _finderArea != null ? _finderArea : new Rect(0, 0, width, height);

        Rect previewFrame = new Rect();
        previewFrame.left = frame.left * _previewWidth / width;
        previewFrame.right = frame.right * _previewWidth / width;
        previewFrame.top = frame.top * _previewHeight / height;
        previewFrame.bottom = frame.bottom * _previewHeight / height;

        // Draw the exterior (i.e. outside the framing rect) darkened
        _paint.setColor(_resultBitmap != null ? _resultColor : _maskColor);
        canvas.drawRect(0, 0, width, frame.top, _paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, _paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, _paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, _paint);

        if (_resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            _paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(_resultBitmap, null, frame, _paint);
        } else {

            // Draw a red "laser scanner" line through the middle to show decoding is active
            _paint.setColor(_laserColor);
            _paint.setAlpha(SCANNER_ALPHA[_scannerAlpha]);
            _scannerAlpha = (_scannerAlpha + 1) % SCANNER_ALPHA.length;
            int middle = frame.height() / 2 + frame.top;
            canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, _paint);

            float scaleX = frame.width() / (float) previewFrame.width();
            float scaleY = frame.height() / (float) previewFrame.height();

            List<ResultPoint> currentPossible = _possibleResultPoints;
            List<ResultPoint> currentLast = _lastPossibleResultPoints;
            int frameLeft = frame.left;
            int frameTop = frame.top;
            if (currentPossible.isEmpty()) {
                _lastPossibleResultPoints = null;
            } else {
                _possibleResultPoints = new ArrayList<ResultPoint>(5);
                _lastPossibleResultPoints = currentPossible;
                _paint.setAlpha(CURRENT_POINT_OPACITY);
                _paint.setColor(_resultPointColor);
                synchronized (currentPossible) {
                    for (ResultPoint point : currentPossible) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                POINT_SIZE, _paint);
                    }
                }
            }
            if (currentLast != null) {
                _paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                _paint.setColor(_resultPointColor);
                synchronized (currentLast) {
                    float radius = POINT_SIZE / 2.0f;
                    for (ResultPoint point : currentLast) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                radius, _paint);
                    }
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this._resultBitmap;
        this._resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        _resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = _possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

}
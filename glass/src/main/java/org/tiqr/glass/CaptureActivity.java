/*
 * Copyright (C) 2014 ZXing authors
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

package org.tiqr.glass;

import org.tiqr.glass.R;
import org.tiqr.scan.ScanHelper;
import org.tiqr.scan.ViewfinderView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.TextParsedResult;
import com.google.zxing.client.result.URIParsedResult;

/**
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements ScanHelper.OnScanListener {
    private final static int CAMERA_PREVIEW_WIDTH = 1280;
    private final static int CAMERA_PREVIEW_HEIGHT = 720;
    private final static int CAMERA_ZOOM = 2;

    private ViewfinderView _viewfinderView;
    private ScanHelper _captureManager;

    private ParsedResult _result;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.capture);

        _viewfinderView = (ViewfinderView)findViewById(R.id.viewfinder);

        SurfaceView previewView = (SurfaceView)findViewById(R.id.preview);
        _captureManager = new ScanHelper(previewView, this);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        _captureManager.start();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        _captureManager.stop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (_result != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    handleResult();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    reset();
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void handleResult() {
        Intent intent;
        if (_result.getType() == ParsedResultType.URI) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(((URIParsedResult)_result).getURI()));
        } else {
            intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra("query", ((TextParsedResult)_result).getText());
        }
        startActivity(intent);
    }

    private synchronized void reset() {
        TextView statusView = (TextView) findViewById(R.id.status_view);
        statusView.setVisibility(View.GONE);
        _result = null;
    }

    @Override
    public void onScanCameraOpen(ScanHelper helper, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        params.setPreviewSize(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT);
        CameraConfigurationUtils.setFocus(params, true, false, false);
        CameraConfigurationUtils.setBestPreviewFPS(params);
        CameraConfigurationUtils.setBarcodeSceneMode(params);
        CameraConfigurationUtils.setVideoStabilization(params);
        CameraConfigurationUtils.setMetering(params);
        CameraConfigurationUtils.setZoom(params, CAMERA_ZOOM);
        camera.setParameters(params);

        _viewfinderView.setPreviewSize(params.getPreviewSize().width, params.getPreviewSize().height);

        int finderAreaWidth = Math.max(5 * _viewfinderView.getWidth() / 8, 240);
        int finderAreaHeight = Math.max(5 * _viewfinderView.getHeight() / 8, 240);
        Rect finderArea = new Rect();
        finderArea.left = (_viewfinderView.getWidth() - finderAreaWidth) / 2;
        finderArea.top = (_viewfinderView.getHeight() - finderAreaHeight) / 2;
        finderArea.right = finderArea.left + finderAreaWidth;
        finderArea.bottom = finderArea.top + finderAreaHeight;
        _viewfinderView.setFinderArea(finderArea);

        Rect scanArea = new Rect();
        scanArea.left = (params.getPreviewSize().width / _viewfinderView.getWidth()) * finderArea.left;
        scanArea.top = (params.getPreviewSize().height / _viewfinderView.getHeight()) * finderArea.top;
        scanArea.right =  (params.getPreviewSize().width / _viewfinderView.getWidth()) * finderArea.right;
        scanArea.bottom = (params.getPreviewSize().height / _viewfinderView.getHeight()) * finderArea.bottom;
        helper.setScanArea(scanArea);
    }

    @Override
    public void onScanPossibleResultPoint(ScanHelper helper, ResultPoint point) {
        _viewfinderView.addPossibleResultPoint(point);
    }

    @Override
    public boolean onScanResult(ScanHelper helper, ParsedResult result) {
        _result = result;

        // Plays success sound
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(Sounds.SUCCESS);

        TextView statusView = (TextView) findViewById(R.id.status_view);
        String text = result.getDisplayResult();
        statusView.setText(text);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(14, 56 - text.length() / 4));
        statusView.setVisibility(View.VISIBLE);

        return true;
    }
}
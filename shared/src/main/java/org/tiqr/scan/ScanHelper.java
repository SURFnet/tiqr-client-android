package org.tiqr.scan;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.HybridBinarizer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Helper for scanning QR codes.
 */
public final class ScanHelper implements Camera.PreviewCallback, ResultPointCallback, TextureView.SurfaceTextureListener {
    private final static int START  = 1;
    private final static int RESUME = 2;
    private final static int DECODE = 3;
    private final static int RESULT = 4;
    private final static int STOP   = 5;

    public interface OnScanListener {
        public void onScanCameraOpen(ScanHelper helper, Camera camera);
        public void onScanPossibleResultPoint(ScanHelper helper, ResultPoint point);
        public boolean onScanResult(ScanHelper helper, ParsedResult result);
    }

    private final TextureView _previewView;
    private final OnScanListener _listener;
    private Rect _scanArea;

    private boolean _running = false;
    private boolean _surfaceCreated = false;

    private Camera _camera;
    private byte[] _previewBuffer;
    private Handler _handler;
    private CountDownLatch _handlerInitLatch;

    /**
     * Constructor.
     *
     * @param previewView
     * @param resultListener
     */
    public ScanHelper(TextureView previewView, OnScanListener resultListener) {
        _previewView = previewView;
        _listener = resultListener;

        previewView.setSurfaceTextureListener(this);
    }

    /**
     * Scan the entire preview image for QR codes.
     */
    public void resetScanRect() {
        _scanArea = null;
    }

    /**
     * Set the area of the preview image that is scanned for QR codes.
     */
    public void setScanArea(Rect scanArea) {
        _scanArea = scanArea;
    }

    /**
     * Start scanner.
     */
    public synchronized void start() {
        if (_running) {
            return;
        }

        _running = true;
        _start();
    }

    private void _start() {
        if (!_running || !_surfaceCreated) {
            return;
        }

        try {
            _camera = Camera.open();
            _listener.onScanCameraOpen(this, _camera);
            _camera.setPreviewTexture(_previewView.getSurfaceTexture());
            _camera.startPreview();

            Camera.Parameters params = _camera.getParameters();
            _previewBuffer = new byte[params.getPreviewSize().height * params.getPreviewSize().width * 3 / 2];

            _handlerInitLatch = new CountDownLatch(1);

            new Thread() {
                public void run() {
                    Looper.prepare();
                    _handler = new DecodeHandler();
                    _handlerInitLatch.countDown();
                    Looper.loop();
                }
            }.start();

            _getHandler().obtainMessage(START).sendToTarget();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Handler _getHandler() {
        try {
            _handlerInitLatch.await();
        } catch (InterruptedException ie) {
        }

        return _handler;
    }

    /**
     * Stop scanner.
     */
    public synchronized void stop() {
        _running = false;
        _stop();
    }

    private void _stop() {
        if (_camera != null) {
            _camera.stopPreview();
            _camera.release();
            _camera = null;
        }

        if (_handler != null) {
            _getHandler().obtainMessage(STOP).sendToTarget();
            _handler = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        _getHandler().obtainMessage(DECODE, data).sendToTarget();
    }

    @Override
    public void foundPossibleResultPoint(ResultPoint point) {
        _listener.onScanPossibleResultPoint(this, point);
    }

    private void _decode(byte[] data) {
        final Map<DecodeHintType, Object> hints;
        hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Arrays.asList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, this);

        Result rawResult = null;

        Camera.Size previewSize = _camera.getParameters().getPreviewSize();
        Rect scanArea = _scanArea != null ? _scanArea : new Rect(0, 0, previewSize.width, previewSize.height);
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, previewSize.width, previewSize.height, scanArea.left, scanArea.top, scanArea.width(), scanArea.height(), false);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            rawResult = new MultiFormatReader().decode(bitmap, hints);
        } catch (ReaderException re) {
            // continue
        }

        if (rawResult != null) {
            ParsedResult result = ResultParser.parseResult(rawResult);
            _getHandler().obtainMessage(RESULT, result).sendToTarget();
        } else {
            // Failed to decode a valid result, try again
            _getHandler().obtainMessage(RESUME).sendToTarget();
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        _surfaceCreated = true;
        _start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // do nothing
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // do nothing
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        _surfaceCreated = false;
        _stop();
        return true;
    }

    private final class DecodeHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            if (!_running) {
                return;
            }

            switch (message.what) {
                case START:
                    _camera.setPreviewCallbackWithBuffer(ScanHelper.this);
                    // intentionally no break
                case RESUME:
                    _camera.addCallbackBuffer(_previewBuffer);
                    break;
                case DECODE:
                    _decode((byte[])message.obj);
                    break;
                case RESULT:
                    final ParsedResult result = (ParsedResult)message.obj;
                    _previewView.post(new Runnable() {
                        public void run() {
                            if (!_listener.onScanResult(ScanHelper.this, result) && _running) {
                                _getHandler().obtainMessage(RESUME).sendToTarget();
                            }
                        }
                    });
                    break;
                case STOP:
                    Looper.myLooper().quit();
                    break;
            }
        }
    }
}
package org.tiqr.scan

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.TextureView

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import com.google.zxing.ResultPointCallback
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ResultParser
import com.google.zxing.common.HybridBinarizer

import java.util.Arrays
import java.util.EnumMap
import java.util.concurrent.CountDownLatch

/**
 * Helper for scanning QR codes.
 */
class ScanHelper
/**
 * Constructor.
 *
 * @param previewView
 * @param resultListener
 */
(private val _previewView: TextureView, private val _listener: OnScanListener) : Camera.PreviewCallback, ResultPointCallback, TextureView.SurfaceTextureListener {
    private var _scanArea: Rect? = null

    private var _running = false
    private var _surfaceCreated = false

    private var _camera: Camera? = null
    private var _previewBuffer: ByteArray? = null
    private var _handler: Handler? = null
    private var _handlerInitLatch: CountDownLatch? = null

    interface OnScanListener {
        fun onScanCameraOpen(helper: ScanHelper, camera: Camera?)
        fun onScanPossibleResultPoint(helper: ScanHelper, point: ResultPoint)
        fun onScanResult(helper: ScanHelper, result: ParsedResult): Boolean
    }

    init {

        _previewView.surfaceTextureListener = this
    }

    /**
     * Scan the entire preview image for QR codes.
     */
    fun resetScanRect() {
        _scanArea = null
    }

    /**
     * Set the area of the preview image that is scanned for QR codes.
     */
    fun setScanArea(scanArea: Rect) {
        _scanArea = scanArea
    }

    /**
     * Start scanner.
     */
    @Synchronized
    fun start() {
        if (_running) {
            return
        }

        _running = true
        _start()
    }

    private fun _start() {
        if (!_running || !_surfaceCreated) {
            return
        }

        try {
            _camera = Camera.open()
            _listener.onScanCameraOpen(this, _camera)
            _camera!!.setPreviewTexture(_previewView.surfaceTexture)
            _camera!!.startPreview()

            val params = _camera!!.parameters
            _previewBuffer = ByteArray(params.previewSize.height * params.previewSize.width * 3 / 2)

            _handlerInitLatch = CountDownLatch(1)

            object : Thread() {
                override fun run() {
                    Looper.prepare()
                    _handler = DecodeHandler()
                    _handlerInitLatch!!.countDown()
                    Looper.loop()
                }
            }.start()

            _getHandler()!!.obtainMessage(START).sendToTarget()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    private fun _getHandler(): Handler? {
        try {
            _handlerInitLatch!!.await()
        } catch (ie: InterruptedException) {
        }

        return _handler
    }

    /**
     * Stop scanner.
     */
    @Synchronized
    fun stop() {
        _running = false
        _stop()
    }

    private fun _stop() {
        if (_camera != null) {
            _camera!!.stopPreview()
            _camera!!.release()
            _camera = null
        }

        if (_handler != null) {
            _getHandler()!!.obtainMessage(STOP).sendToTarget()
            _handler = null
        }
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        _getHandler()!!.obtainMessage(DECODE, data).sendToTarget()
    }

    override fun foundPossibleResultPoint(point: ResultPoint) {
        _listener.onScanPossibleResultPoint(this, point)
    }

    private fun _decode(data: ByteArray) {
        val hints: MutableMap<DecodeHintType, Any>
        hints = EnumMap(DecodeHintType::class.java)
        hints[DecodeHintType.POSSIBLE_FORMATS] = Arrays.asList(BarcodeFormat.QR_CODE)
        hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = this

        var rawResult: Result? = null

        val previewSize = _camera!!.parameters.previewSize
        val scanArea = if (_scanArea != null) _scanArea else Rect(0, 0, previewSize.width, previewSize.height)
        scanArea?.let {
            val source = PlanarYUVLuminanceSource(data, previewSize.width, previewSize.height, it.left, it.top, it.width(), it.height(), false)

            val bitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                rawResult = MultiFormatReader().decode(bitmap, hints)
            } catch (re: ReaderException) {
                // continue
            }

            if (rawResult != null) {
                val result = ResultParser.parseResult(rawResult)
                _getHandler()!!.obtainMessage(RESULT, result).sendToTarget()
            } else {
                // Failed to decode a valid result, try again
                _getHandler()!!.obtainMessage(RESUME).sendToTarget()
            }
        }
    }


    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        _surfaceCreated = true
        _start()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        // do nothing
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // do nothing
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        _surfaceCreated = false
        _stop()
        return true
    }

    private inner class DecodeHandler : Handler() {
        override fun handleMessage(message: Message) {
            if (!_running) {
                return
            }

            when (message.what) {
                START -> {
                    _camera!!.setPreviewCallbackWithBuffer(this@ScanHelper)
                    _camera!!.addCallbackBuffer(_previewBuffer)
                }
                // intentionally no break
                RESUME -> _camera!!.addCallbackBuffer(_previewBuffer)
                DECODE -> _decode(message.obj as ByteArray)
                RESULT -> {
                    val result = message.obj as ParsedResult
                    _previewView.post {
                        if (!_listener.onScanResult(this@ScanHelper, result) && _running) {
                            _getHandler()!!.obtainMessage(RESUME).sendToTarget()
                        }
                    }
                }
                STOP -> Looper.myLooper()!!.quit()
            }
        }
    }

    companion object {
        private val START = 1
        private val RESUME = 2
        private val DECODE = 3
        private val RESULT = 4
        private val STOP = 5
    }
}
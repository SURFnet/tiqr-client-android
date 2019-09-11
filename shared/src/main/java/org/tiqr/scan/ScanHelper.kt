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

class ScanHelper (private val previewView: TextureView, private val listener: OnScanListener) : Camera.PreviewCallback, ResultPointCallback, TextureView.SurfaceTextureListener {
    
    private var scanArea: Rect? = null

    private var running = false
    private var surfaceCreated = false

    private var camera: Camera? = null
    private var previewBuffer: ByteArray? = null
    private var handler: Handler? = null
    private var handlerInitLatch: CountDownLatch? = null

    interface OnScanListener {
        fun onScanCameraOpen(helper: ScanHelper, camera: Camera?)
        fun onScanPossibleResultPoint(helper: ScanHelper, point: ResultPoint)
        fun onScanResult(helper: ScanHelper, result: ParsedResult): Boolean
    }

    init {
        previewView.surfaceTextureListener = this
    }

    /**
     * Scan the entire preview image for QR codes.
     */
    fun resetScanRect() {
        scanArea = null
    }

    /**
     * Set the area of the preview image that is scanned for QR codes.
     */
    fun setScanArea(scanArea: Rect) {
        this.scanArea = scanArea
    }

    /**
     * Start scanner.
     */
    @Synchronized
    fun start() {
        if (running) {
            return
        }
        running = true
        realStart()
    }

    private fun realStart() {
        if (!running || !surfaceCreated) {
            return
        }

        try {
            camera = Camera.open()
            listener.onScanCameraOpen(this, camera)
            camera!!.setPreviewTexture(previewView.surfaceTexture)
            camera!!.startPreview()

            val params = camera!!.parameters
            previewBuffer = ByteArray(params.previewSize.height * params.previewSize.width * 3 / 2)

            handlerInitLatch = CountDownLatch(1)

            object : Thread() {
                override fun run() {
                    Looper.prepare()
                    handler = DecodeHandler()
                    handlerInitLatch!!.countDown()
                    Looper.loop()
                }
            }.start()

            getHandler()!!.obtainMessage(START).sendToTarget()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    private fun getHandler(): Handler? {
        try {
            handlerInitLatch!!.await()
        } catch (ie: InterruptedException) {
        }

        return handler
    }

    /**
     * Stop scanner.
     */
    @Synchronized
    fun stop() {
        running = false
        realStop()
    }

    private fun realStop() {
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.release()
            camera = null
        }

        if (handler != null) {
            getHandler()!!.obtainMessage(STOP).sendToTarget()
            handler = null
        }
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        getHandler()!!.obtainMessage(DECODE, data).sendToTarget()
    }

    override fun foundPossibleResultPoint(point: ResultPoint) {
        listener.onScanPossibleResultPoint(this, point)
    }

    private fun decode(data: ByteArray) {
        val hints: MutableMap<DecodeHintType, Any>
        hints = EnumMap(DecodeHintType::class.java)
        hints[DecodeHintType.POSSIBLE_FORMATS] = Arrays.asList(BarcodeFormat.QR_CODE)
        hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = this

        var rawResult: Result? = null

        val previewSize = camera!!.parameters.previewSize
        val scanArea = if (scanArea != null) scanArea else Rect(0, 0, previewSize.width, previewSize.height)
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
                getHandler()!!.obtainMessage(RESULT, result).sendToTarget()
            } else {
                // Failed to decode a valid result, try again
                getHandler()!!.obtainMessage(RESUME).sendToTarget()
            }
        }
    }


    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceCreated = true
        realStart()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        // do nothing
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // do nothing
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        surfaceCreated = false
        realStop()
        return true
    }

    private inner class DecodeHandler : Handler() {
        override fun handleMessage(message: Message) {
            if (!running) {
                return
            }

            when (message.what) {
                START -> {
                    camera!!.setPreviewCallbackWithBuffer(this@ScanHelper)
                    camera!!.addCallbackBuffer(previewBuffer)
                }
                // intentionally no break
                RESUME -> camera!!.addCallbackBuffer(previewBuffer)
                DECODE -> decode(message.obj as ByteArray)
                RESULT -> {
                    val result = message.obj as ParsedResult
                    previewView.post {
                        if (!listener.onScanResult(this@ScanHelper, result) && running) {
                            getHandler()!!.obtainMessage(RESUME).sendToTarget()
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
/*
 * Copyright (c) 2010-2019 SURFnet bv
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
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import androidx.annotation.CheckResult
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tiqr.authenticator.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Component to scan a QR code using [androidx.camera].
 * The processing of the camera image to a QR code is delegated to [ScanAnalyzer].
 */
class ScanComponent(
        private val context: Context,
        private val lifecycleOwner: LifecycleOwner,
        private val viewFinder: PreviewView,
        private val viewFinderRatio: Float,
        private val scanResult: (result: String) -> Unit
) : DefaultLifecycleObserver {
    companion object {
        private const val BEEP_VOLUME = 0.1f
        private const val VIBRATE_DURATION = 200L
    }

    //region Camera
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    private lateinit var cameraAnalysis: ImageAnalysis
    private val cameraAnalyzer = ScanAnalyzer(lifecycleOwner, viewFinderRatio, ::onDetected)
    //endregion

    //region Sound
    private val soundPool: SoundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build())
            .build()
    private val beepSound: Int = soundPool.load(context, R.raw.beep, 1)
    private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    //endregion

    private val lifecycleScope = lifecycleOwner.lifecycleScope

    private val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    private val keyReceiver = CameraKeyEventsReceiver { enable -> camera.cameraControl.enableTorch(enable) }

    init {
        lifecycleScope.launch {
            cameraProvider = initCameraProvider()
            startCamera(cameraProvider)
        }

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            broadcastManager.registerReceiver(keyReceiver, CameraKeyEventsReceiver.filter)
        }
    }

    /**
     * Initialize the [ProcessCameraProvider]
     */
    private suspend fun initCameraProvider(): ProcessCameraProvider {
        return withContext(Dispatchers.Default) {
            ProcessCameraProvider.getInstance(context).await()
        }
    }

    /**
     * Resume the camera and QR code detection and returns a boolean indicating it's resume result.
     */
    @CheckResult
    fun resumeScanning(): Boolean {
        return if (::cameraProvider.isInitialized) {
            startCamera(cameraProvider)
            true
        } else {
            false
        }
    }

    /**
     * Start the camera and QR code detection
     */
    private fun startCamera(cameraProvider: ProcessCameraProvider) {
        fun aspectRatio(width: Int, height: Int): Int {
            val previewRatio = max(width, height).toDouble() / min(width, height)
            return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
                AspectRatio.RATIO_4_3
            } else {
                AspectRatio.RATIO_16_9
            }
        }

        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        cameraPreview = Preview.Builder()
                .setTargetName("tiqr QR scanner")
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(viewFinder.display.rotation)
                .build()

        cameraAnalysis = ImageAnalysis.Builder().build().apply {
            setAnalyzer(ContextCompat.getMainExecutor(context), cameraAnalyzer)
        }

        camera = cameraProvider.run {
            unbindAll()
            bindToLifecycle(lifecycleOwner, cameraSelector, cameraPreview, cameraAnalysis)
        }.apply {
            cameraPreview.setSurfaceProvider(viewFinder.createSurfaceProvider(cameraInfo))
        }
    }

    /**
     * Stop the camera and QR code detection
     */
    private fun stopCamera() {
        cameraProvider.unbindAll()
    }

    /**
     * A QR code has been detected
     */
    private fun onDetected(qrCode: String) {
        stopCamera()
        alertDetection()
        scanResult.invoke(qrCode)
    }

    /**
     * Beep and vibrate to notify the user.
     */
    private fun alertDetection() {
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            // User has turned off sound and/or vibrations, so we should respect that
            return
        }

        soundPool.play(beepSound, BEEP_VOLUME, BEEP_VOLUME, 1, 0, 1f)

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") // deprecated in API 26
                vibrator.vibrate(VIBRATE_DURATION)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        broadcastManager.registerReceiver(keyReceiver, CameraKeyEventsReceiver.filter)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)

        broadcastManager.unregisterReceiver(keyReceiver)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        soundPool.release()
    }
}



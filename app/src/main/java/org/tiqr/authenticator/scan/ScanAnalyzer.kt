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

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.toRect
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.lang.Exception
import kotlin.math.roundToInt

/**
 * Analyzer to detect QR codes using Firebase ML-Kit
 */
class ScanAnalyzer(
        lifecycleOwner: LifecycleOwner,
        private val viewFinderRatio: Float,
        private val result: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val lifecycleScope = lifecycleOwner.lifecycleScope
    private val detector: FirebaseVisionBarcodeDetector

    init {
        detector = FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
                .build()
                .run {
                    FirebaseVision.getInstance()
                            .getVisionBarcodeDetector(this)
                }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {
        image.image?.let { scanned ->
            lifecycleScope.launch {
                val rotation = image.imageInfo.rotationDegrees.toFirebaseRotation()
                val isPortrait = rotation.isOrientationPortrait()
                val width = if (isPortrait) scanned.height else scanned.width
                val height = if (isPortrait) scanned.width else scanned.height

                try {
                    detector.detectInImage(FirebaseVisionImage.fromMediaImage(scanned, rotation)).await()
                            .run {
                                val barcode = firstOrNull() ?: return@run
                                val barcodeBox = barcode.boundingBox
                                val centerBox: Rect? = if (isPortrait) {
                                    val cropped = (height / viewFinderRatio).roundToInt()
                                    getCenterBoxFrame(cropped, height)?.toRect()?.apply {
                                        offset((width - cropped) / 2, 0)
                                    }
                                } else {
                                    val cropped = (width / viewFinderRatio).roundToInt()
                                    getCenterBoxFrame(width, cropped)?.toRect()?.apply {
                                        offset((height - cropped) / 2, 0)
                                    }
                                }

                                if (centerBox != null && barcodeBox != null) {
                                    if (centerBox.contains(barcodeBox)) {
                                        // Only process barcode inside center box
                                        barcode.displayValue?.run(result)
                                    }
                                }
                            }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Timber.e(e, "Error analyzing image")
                    }
                } finally {
                    image.close()
                }
            }
        }
    }

    /**
     * Convert the rotation from the camera to a rotation Firebase understands.
     */
    private fun Int.toFirebaseRotation(): Int {
        return when (this) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> FirebaseVisionImageMetadata.ROTATION_0
        }
    }

    /**
     * Detect if the orientation is portrait.
     */
    private fun Int.isOrientationPortrait(): Boolean {
        return when (this) {
            FirebaseVisionImageMetadata.ROTATION_90,
            FirebaseVisionImageMetadata.ROTATION_270 -> true
            else -> false
        }
    }
}
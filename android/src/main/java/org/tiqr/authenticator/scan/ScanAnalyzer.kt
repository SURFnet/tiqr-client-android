package org.tiqr.authenticator.scan

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.toRect
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    private val detector: BarcodeScanner

    init {
        detector = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
                .run {
                    BarcodeScanning.getClient(this)
                }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {
        image.image?.let { scanned ->
            lifecycleScope.launch {
                val rotation = image.imageInfo.rotationDegrees
                val isPortrait = rotation.isPortrait()
                val width = if (isPortrait) scanned.height else scanned.width
                val height = if (isPortrait) scanned.width else scanned.height

                try {
                    detector.process(InputImage.fromMediaImage(scanned, rotation)).await()
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
                        Log.e("QR","Error analyzing image", e)
                    }
                } finally {
                    image.close()
                }
            }
        }
    }

    /**
     * Detect if the orientation is portrait.
     */
    private fun Int.isPortrait(): Boolean {
        return when (this) {
            90, 270 -> true
            else -> false
        }
    }
}
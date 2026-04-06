package com.android.daviddev.ecoscancmem.camera

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class CompositeAnalyzer(
    private val onTextDetected: (String) -> Unit,
    private val onObjectDetected: (List<DetectedObject>) -> Unit
) : ImageAnalysis.Analyzer {

    // ML Kit clients
    private val textRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()
    )

    // Throttle: evita sobrecarga frame-a-frame
    private var lastAnalyzedMs = 0L
    private val isProcessing = AtomicBoolean(false)
    private val THROTTLE_MS = 800L

    // Análise
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()

        // Descarta frame se ainda está a processar ou dentro do throttle
        if (isProcessing.get() || now - lastAnalyzedMs < THROTTLE_MS) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing.set(true)
        lastAnalyzedMs = now

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        // Corre OCR e Object Detection em paralelo.
        // O imageProxy só fecha quando AMBOS terminam.
        var ocrDone = false
        var objDone = false

        fun tryClose() {
            if (ocrDone && objDone) {
                imageProxy.close()
                isProcessing.set(false)
            }
        }

        // 1. OCR
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()
                if (text.isNotBlank()) onTextDetected(text)
            }
            .addOnCompleteListener {
                ocrDone = true
                tryClose()
            }

        // 2. Object Detection
        objectDetector.process(inputImage)
            .addOnSuccessListener { objects ->
                if (objects.isNotEmpty()) onObjectDetected(objects)
            }
            .addOnCompleteListener {
                objDone = true
                tryClose()
            }
    }
}
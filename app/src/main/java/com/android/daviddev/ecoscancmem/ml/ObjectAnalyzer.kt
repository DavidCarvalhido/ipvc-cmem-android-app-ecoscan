package com.android.daviddev.ecoscancmem.ml

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectAnalyzer(private val onResult: (List<DetectedObject>) -> Unit) :
    ImageAnalysis.Analyzer {
    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            // STREAM_MODE é mais rápido que SINGLE_IMAGE e adequado ao viewfinder
            .enableClassification()
            .enableMultipleObjects()
            .build()
    )

    private var lastAnalyzedTime = 0L
    private val THROTTLE_MS = 600L

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedTime < THROTTLE_MS) {
            imageProxy.close()
            return
        }
        lastAnalyzedTime = now

        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(
            mediaImage, imageProxy.imageInfo.rotationDegrees
        )

        detector.process(image)
            .addOnSuccessListener { objects ->
                if (objects.isNotEmpty()) onResult(objects)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
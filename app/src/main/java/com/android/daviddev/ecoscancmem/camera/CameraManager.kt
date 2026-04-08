package com.android.daviddev.ecoscancmem.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.objects.DetectedObject
import java.io.File
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onTextDetected: (String) -> Unit,
    private val onObjectDetected: (List<DetectedObject>) -> Unit
) {
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var torchEnabled = false
    private var cameraProvider: ProcessCameraProvider? = null

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { analysis ->
                    analysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        CompositeAnalyzer(onTextDetected, onObjectDetected)
                    )
                }

            try {
                cameraProvider?.unbindAll()
                // Guarda a referência à Camera retornada pelo bind
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, imageCapture, imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraManager", "Bind failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Lanterna do dispositivo
    fun toggleTorch() {
        camera?.cameraControl?.enableTorch(
            camera?.cameraInfo?.torchState?.value != TorchState.ON
        )
    }

    //val isTorchEnabled: Boolean get() = torchEnabled

    // Captura
    fun capturePhoto(onSaved: (Uri) -> Unit) {
        val outputFile = File(context.externalCacheDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { onSaved(it) }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraManager", "Capture failed: ${exc.message}")
                }
            }
        )
    }

    fun release() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e("CameraManager", "Release failed: ${e.message}")
        }
        cameraProvider = null
        imageCapture = null
        camera = null
    }
}
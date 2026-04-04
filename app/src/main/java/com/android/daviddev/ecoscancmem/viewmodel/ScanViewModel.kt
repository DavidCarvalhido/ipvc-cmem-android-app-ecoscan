package com.android.daviddev.ecoscancmem.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.android.daviddev.ecoscancmem.data.model.MaterialDatabase
import com.android.daviddev.ecoscancmem.data.model.ScanResult
import com.google.mlkit.vision.objects.DetectedObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ScanViewModel : ViewModel() {

    private val _analysisResult = MutableStateFlow<ScanResult?>(null)
    val analysisResult: StateFlow<ScanResult?> = _analysisResult

    private val _isAnalysing = MutableStateFlow(false)
    val isAnalysing: StateFlow<Boolean> = _isAnalysing

    private var lastOcrText: String? = null
    private var lastObjectLabel: String? = null

    fun processDetectedText(text: String) {
        lastOcrText = text
        tryBuildResult()
    }

    fun processDetectedObjects(objects: List<DetectedObject>) {
        objects.firstOrNull()?.labels?.maxByOrNull { it.confidence }?.let {
            if (it.confidence > 0.6f) {
                lastObjectLabel = it.text
                tryBuildResult()
            }
        }
    }

    private fun tryBuildResult() {
        val ocrText = lastOcrText
        val objectLabel = lastObjectLabel

        val info = when {
            ocrText != null -> {
                val code = extractRecycleCode(ocrText)
                if (code != null) MaterialDatabase.getByCode(code)
                else if (objectLabel != null) MaterialDatabase.getByLabel(objectLabel)
                else null
            }
            objectLabel != null -> MaterialDatabase.getByLabel(objectLabel)
            else -> null
        } ?: return

        val recycleCode = ocrText?.let { extractRecycleCode(it) } ?: info.recycleCode

        _analysisResult.value = ScanResult(
            materialName      = info.name,
            materialSubtitle  = info.subtitle,
            recycleCode       = recycleCode,
            isRecyclable      = info.isRecyclable,
            confidencePercent = 87,
            ecopointColor     = info.ecopointColor,
            co2SavedGrams     = info.co2SavedGrams,
            decompYears       = info.decompYears,
            energySavedPercent = info.energySavedPercent,
            ocrRawText        = ocrText,
            ocrDecodedText    = if (ocrText != null) info.subtitle else null,
            tips              = info.tips
        )
    }

    private fun extractRecycleCode(text: String): String? {
        val regex = Regex("""(PET|HDPE|PVC|LDPE|PP|PS|GL|PAP|ALU|FE)[\s\-]?\d{0,2}""",
            RegexOption.IGNORE_CASE)
        return regex.find(text)?.value?.trim()
    }

    fun clearResult() {
        _analysisResult.value = null
        lastOcrText = null
        lastObjectLabel = null
    }

    fun saveCapture(uri: Uri) {
        _analysisResult.value = _analysisResult.value?.copy(capturedImageUri = uri.toString())
    }
}
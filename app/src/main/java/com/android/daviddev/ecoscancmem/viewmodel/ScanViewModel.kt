package com.android.daviddev.ecoscancmem.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.daviddev.ecoscancmem.data.model.MaterialDatabase
import com.android.daviddev.ecoscancmem.data.model.ScanResult
import com.google.mlkit.vision.objects.DetectedObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val _analysisResult = MutableStateFlow<ScanResult?>(null)
    val analysisResult: StateFlow<ScanResult?> = _analysisResult.asStateFlow()

    private val _isAnalysing = MutableStateFlow(false)
    val isAnalysing: StateFlow<Boolean> = _isAnalysing.asStateFlow()

    private val _luxLevel = MutableStateFlow(0f)
    val luxLevel: StateFlow<Float> = _luxLevel.asStateFlow()

    private val _isLightSufficient = MutableStateFlow(true)
    val isLightSufficient: StateFlow<Boolean> = _isLightSufficient.asStateFlow()

    private val _isDeviceStable = MutableStateFlow(true)
    val isDeviceStable: StateFlow<Boolean> = _isDeviceStable.asStateFlow()

    private val _stabilityLabel = MutableStateFlow("Estável")
    val stabilityLabel: StateFlow<String> = _stabilityLabel.asStateFlow()

    private val _activeModeLabel = MutableStateFlow("OCR + Obj.")
    val activeModeLabel: StateFlow<String> = _activeModeLabel.asStateFlow()

    private val _scanMode = MutableStateFlow(ScanMode.OBJECT)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    enum class ScanMode { OBJECT, LABEL }

    // ESTADO INTERNO
    private var lastOcrText: String? = null
    private var lastObjectLabel: String? = null
    private var lastObjectConfidence: Float = 0f

    // Janela deslizante de aceleração para calcular estabilidade
    private val accelWindow = ArrayDeque<Float>(ACCEL_WINDOW_SIZE)

    // Debounce: evita múltiplos resultados em rápida sucessão
    private var lastResultTimestamp = 0L

    // Cooldown após captura manual
    private var captureCooldownActive = false

    companion object {
        private const val LUX_THRESHOLD = 50f          // abaixo -> aviso de luz
        private const val ACCEL_THRESHOLD = 1.2f       // m/s2 - acima -> dispositivo instável
        private const val ACCEL_WINDOW_SIZE = 10       // amostras na janela
        private const val RESULT_DEBOUNCE_MS = 1500L   // ms entre resultados
        private const val MIN_CONFIDENCE = 0.62f       // confiança mínima do ML Kit
    }

    // SENSOR: LUZ AMBIENTE
    fun onLuxChanged(lux: Float) {
        _luxLevel.value = lux
        _isLightSufficient.value = lux >= LUX_THRESHOLD
    }

    // SENSOR: ACELERÓMETRO
    /**
     * Recebe valores brutos do acelerómetro (x, y, z em m/s²).
     * Calcula a magnitude total e mantém uma janela deslizante
     * para suavizar leituras e evitar falsos positivos.
     */
    fun onAccelerometerChanged(x: Float, y: Float, z: Float) {
        // Magnitude do vetor de aceleração (exclui gravidade com low-pass filter)
        val magnitude = sqrt(x * x + y * y + z * z)

        // Janela deslizante: mantém as últimas N amostras
        if (accelWindow.size >= ACCEL_WINDOW_SIZE) accelWindow.removeFirst()
        accelWindow.addLast(magnitude)

        // Desvio padrão da janela - mede variação, não valor absoluto
        val mean = accelWindow.average().toFloat()
        val variance = accelWindow.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = sqrt(variance)

        val stable = stdDev < ACCEL_THRESHOLD
        _isDeviceStable.value = stable
        _stabilityLabel.value = if (stable) "Estável" else "Em movimento"
    }

    // ML KIT: TEXTO (OCR)
    fun processDetectedText(text: String) {
        if (!canAcceptResult()) return
        if (text.isBlank()) return

        lastOcrText = text

        // Tenta extrair código de reciclagem diretamente
        val code = extractRecycleCode(text)
        if (code != null) {
            buildAndEmitResult(recycleCodeOverride = code)
        } else if (lastObjectLabel != null) {
            // Combina OCR + deteção de objeto
            buildAndEmitResult()
        }
    }

    // ML KIT: OBJECT DETECTION
    fun processDetectedObjects(objects: List<DetectedObject>) {
        if (!canAcceptResult()) return
        if (objects.isEmpty()) return

        val best = objects
            .flatMap { obj -> obj.labels.map { label -> label to obj } }
            .maxByOrNull { it.first.confidence }
            ?: return

        val (label, _) = best
        if (label.confidence < MIN_CONFIDENCE) return

        lastObjectLabel = label.text
        lastObjectConfidence = label.confidence

        buildAndEmitResult()
    }

    // CONSTRUÇÃO DO RESULTADO
    private fun buildAndEmitResult(recycleCodeOverride: String? = null) {
        // Não emite resultado se a luz for insuficiente ou dispositivo instável
        if (!_isLightSufficient.value) return
        if (!_isDeviceStable.value) return

        val ocrText = lastOcrText
        val objectLabel = lastObjectLabel

        val info: MaterialDatabase.MaterialInfo = when {
            recycleCodeOverride != null ->
                MaterialDatabase.getByCode(recycleCodeOverride)

            ocrText != null -> {
                val code = extractRecycleCode(ocrText)
                when {
                    code != null -> MaterialDatabase.getByCode(code)
                    objectLabel != null -> MaterialDatabase.getByLabel(objectLabel)
                    else -> return
                }
            }

            objectLabel != null ->
                MaterialDatabase.getByLabel(objectLabel)

            else -> return
        }

        val confidence = when {
            recycleCodeOverride != null -> 95
            lastObjectConfidence > 0f -> (lastObjectConfidence * 100).toInt()
            else -> 75
        }

        _analysisResult.value = ScanResult(
            materialName = info.name,
            materialSubtitle = info.subtitle,
            recycleCode = recycleCodeOverride ?: info.recycleCode,
            isRecyclable = info.isRecyclable,
            confidencePercent = confidence,
            ecopointColor = info.ecopointColor,
            co2SavedGrams = info.co2SavedGrams,
            decompYears = info.decompYears,
            energySavedPercent = info.energySavedPercent,
            ocrRawText = ocrText,
            ocrDecodedText = if (ocrText != null) info.subtitle else null,
            tips = info.tips
        )

        lastResultTimestamp = System.currentTimeMillis()
        _isAnalysing.value = false
    }

    // CAPTURA MANUAL
    fun saveCapture(uri: Uri) {
        _analysisResult.value = _analysisResult.value?.copy(
            capturedImageUri = uri.toString()
        )
        // Cooldown de 2s após captura para evitar análises duplicadas
        captureCooldownActive = true
        viewModelScope.launch {
            delay(2000)
            captureCooldownActive = false
        }
    }

    // MODO DE SCAN (Objeto/Rótulo)
    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
        _activeModeLabel.value = when (mode) {
            ScanMode.OBJECT -> "OCR + Obj."
            ScanMode.LABEL -> "OCR"
        }
        clearDetectionBuffers()
    }

    // LIMPEZA
    fun clearResult() {
        _analysisResult.value = null
        clearDetectionBuffers()
        _isAnalysing.value = false
    }

    private fun clearDetectionBuffers() {
        lastOcrText = null
        lastObjectLabel = null
        lastObjectConfidence = 0f
    }

    // AUXILIARES
    /**
     * Debounce: só aceita novo resultado após RESULT_DEBOUNCE_MS
     * e fora de cooldown de captura.
     */
    private fun canAcceptResult(): Boolean {
        if (captureCooldownActive) return false
        if (_analysisResult.value != null) return false
        val elapsed = System.currentTimeMillis() - lastResultTimestamp
        return elapsed > RESULT_DEBOUNCE_MS
    }

    /**
     * Extrai o código de reciclagem padrão do texto OCR.
     * Suporta formatos como "PET 1", "HDPE2", "GL 70", "PAP 20", "ALU 41".
     */
    private fun extractRecycleCode(text: String): String? {
        val regex = Regex(
            pattern = """(PET|HDPE|PVC|LDPE|PP|PS|GL|PAP|ALU|FE)\s*\d{0,2}""",
            option = RegexOption.IGNORE_CASE
        )
        return regex.find(text)?.value?.trim()?.uppercase()
    }
}
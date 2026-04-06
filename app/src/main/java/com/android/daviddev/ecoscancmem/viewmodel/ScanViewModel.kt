package com.android.daviddev.ecoscancmem.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.daviddev.ecoscancmem.data.MaterialMapper
import com.android.daviddev.ecoscancmem.data.ScanRepository
import com.android.daviddev.ecoscancmem.data.db.EcoScanDatabase
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

    // Repositório da app
    private val repository = ScanRepository(
        EcoScanDatabase.getInstance(application)
    )

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
        _isAnalysing.value = true
        buildAndEmitResult()
    }

    // ML KIT: OBJECT DETECTION
    fun processDetectedObjects(objects: List<DetectedObject>) {
        if (!canAcceptResult()) return
        if (objects.isEmpty()) return

        // Apanha o objeto com label de maior confiança
        data class Candidate(val label: String, val confidence: Float)

        val best = objects
            .flatMap { obj -> obj.labels.map { label -> Candidate(label.text, label.confidence) } }
            .maxByOrNull { it.confidence }
            ?: return

        // Guarda sempre — o MaterialMapper decide se tem confiança suficiente
        lastObjectLabel = best.label
        lastObjectConfidence = best.confidence

        _isAnalysing.value = true
        buildAndEmitResult()
    }

    // CONSTRUÇÃO DO RESULTADO
    private fun buildAndEmitResult(recycleCodeOverride: String? = null) {
        if (!_isLightSufficient.value) {
            _isAnalysing.value = false; return
        }
        if (!_isDeviceStable.value) {
            _isAnalysing.value = false; return
        }

        val mapped = if (recycleCodeOverride != null) {
            MaterialMapper.MappedMaterial(
                key = recycleCodeOverride,
                confidence = 95,
                source = MaterialMapper.Source.OCR_CODE
            )
        } else {
            MaterialMapper.resolve(
                objectLabel = lastObjectLabel,
                objectConfidence = lastObjectConfidence,
                ocrText = lastOcrText
            )
        }

        // Sem resultado com confiança suficiente — continua a analisar
        if (mapped == null) return

        val info = MaterialDatabase.getByCode(mapped.key)

        _analysisResult.value = ScanResult(
            materialName = info.name,
            materialSubtitle = info.subtitle,
            recycleCode = mapped.key.let {
                // tenta extrair código exato do OCR se disponível
                lastOcrText?.let { t -> MaterialMapper.extractCode(t) } ?: info.recycleCode
            },
            isRecyclable = info.isRecyclable,
            confidencePercent = mapped.confidence,
            ecopointColor = info.ecopointColor,
            co2SavedGrams = info.co2SavedGrams,
            decompYears = info.decompYears,
            energySavedPercent = info.energySavedPercent,
            ocrRawText = lastOcrText,
            ocrDecodedText = if (lastOcrText != null) info.subtitle else null,
            tips = info.tips
        )

        lastResultTimestamp = System.currentTimeMillis()
        _isAnalysing.value = false
    }

    // CAPTURA MANUAL
    fun saveCapture(uri: Uri) {
        val current = _analysisResult.value ?: return
        val withUri = current.copy(capturedImageUri = uri.toString())
        _analysisResult.value = withUri

        viewModelScope.launch {
            repository.save(withUri)
        }

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

    // Novo metodo chamado pelo ResultFragment no botão "Guardar"
    fun saveCurrentResult() {
        viewModelScope.launch {
            _analysisResult.value?.let { repository.save(it) }
        }
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
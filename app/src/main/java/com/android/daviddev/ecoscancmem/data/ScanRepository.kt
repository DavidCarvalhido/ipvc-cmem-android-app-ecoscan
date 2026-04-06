package com.android.daviddev.ecoscancmem.data

import com.android.daviddev.ecoscancmem.data.db.EcoScanDatabase
import com.android.daviddev.ecoscancmem.data.db.ScanEntity
import com.android.daviddev.ecoscancmem.data.model.EcopointColor
import com.android.daviddev.ecoscancmem.data.model.ScanResult
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

class ScanRepository(db: EcoScanDatabase) {
    private val dao = db.scanDao()

    val allScans: Flow<List<ScanEntity>> = dao.getAllScans()
    val totalScans: Flow<Int> = dao.getTotalScans()
    val totalCo2Saved: Flow<Int?> = dao.getTotalCo2Saved()

    suspend fun save(result: ScanResult): Long {
        val entity = ScanEntity(
            materialName = result.materialName,
            materialSubtitle = result.materialSubtitle,
            recycleCode = result.recycleCode,
            isRecyclable = result.isRecyclable,
            ecopointColor = result.ecopointColor.name,
            co2SavedGrams = result.co2SavedGrams,
            decompYears = result.decompYears,
            energySavedPercent = result.energySavedPercent,
            ocrRawText = result.ocrRawText,
            tips = JSONArray(result.tips).toString(),
            capturedImageUri = result.capturedImageUri
        )
        return dao.insertScan(entity)
    }

    suspend fun delete(entity: ScanEntity) = dao.deleteScan(entity)

    suspend fun deleteAll() = dao.deleteAll()

    // Converte ScanEntity de volta para exibição na lista
    fun ScanEntity.toDisplayModel() = HistoryItem(
        id = id,
        materialName = materialName,
        recycleCode = recycleCode,
        isRecyclable = isRecyclable,
        ecopointColor = runCatching { EcopointColor.valueOf(ecopointColor) }
            .getOrDefault(EcopointColor.NONE),
        co2SavedGrams = co2SavedGrams,
        ocrRawText = ocrRawText,
        capturedImageUri = capturedImageUri,
        timestamp = timestamp
    )
}

data class HistoryItem(
    val id: Int,
    val materialName: String,
    val recycleCode: String,
    val isRecyclable: Boolean,
    val ecopointColor: EcopointColor,
    val co2SavedGrams: Int,
    val ocrRawText: String?,
    val capturedImageUri: String?,
    val timestamp: Long
)
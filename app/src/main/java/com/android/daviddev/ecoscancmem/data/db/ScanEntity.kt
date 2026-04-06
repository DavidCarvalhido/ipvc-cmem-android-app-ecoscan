package com.android.daviddev.ecoscancmem.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materialName: String,
    val materialSubtitle: String,
    val recycleCode: String,
    val isRecyclable: Boolean,
    val ecopointColor: String, // nome do enum guardado como String
    val co2SavedGrams: Int,
    val decompYears: Int,
    val energySavedPercent: Int,
    val ocrRawText: String?,
    val tips: String, // lista serializada como JSON string
    val capturedImageUri: String?,
    val timestamp: Long = System.currentTimeMillis()
)
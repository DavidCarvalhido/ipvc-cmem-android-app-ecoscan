package com.android.daviddev.ecoscancmem.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScanResult(
    val materialName: String,
    val materialSubtitle: String,
    val recycleCode: String, // ex: "PET 1"
    val isRecyclable: Boolean,
    val confidencePercent: Int, // 0-100
    val ecopointColor: EcopointColor,
    val co2SavedGrams: Int,
    val decompYears: Int,
    val energySavedPercent: Int,
    val ocrRawText: String?, // null se não foi detetado texto
    val ocrDecodedText: String?,
    val tips: List<String>,
    val capturedImageUri: String? = null
) : Parcelable

enum class EcopointColor {
    YELLOW, // plástico e metal
    BLUE, // papel e cartão
    GREEN, // vidro
    RED, // pilhas/eletrónico
    NONE // aterro
}
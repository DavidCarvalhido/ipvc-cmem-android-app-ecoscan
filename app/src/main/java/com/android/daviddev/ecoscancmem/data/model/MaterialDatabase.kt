package com.android.daviddev.ecoscancmem.data.model

object MaterialDatabase {

    data class MaterialInfo(
        val name: String,
        val subtitle: String,
        val recycleCode: String,
        val isRecyclable: Boolean,
        val ecopointColor: EcopointColor,
        val co2SavedGrams: Int,
        val decompYears: Int,
        val energySavedPercent: Int,
        val tips: List<String>
    )

    private val materials = mapOf(
        // Plásticos
        "PET" to MaterialInfo(
            name = "Plástico PET",
            subtitle = "Garrafa · Código ♻ 1 · Reciclável",
            recycleCode = "PET 1",
            isRecyclable = true,
            ecopointColor = EcopointColor.YELLOW,
            co2SavedGrams = 240,
            decompYears = 450,
            energySavedPercent = 75,
            tips = listOf(
                "Esvazia e enxagua antes de depositar",
                "Remove a tampa — pode ser de material diferente (PP 5)",
                "Amassa para reduzir o volume"
            )
        ),
        "HDPE" to MaterialInfo(
            name = "Plástico HDPE",
            subtitle = "Frasco · Código ♻ 2 · Reciclável",
            recycleCode = "HDPE 2",
            isRecyclable = true,
            ecopointColor = EcopointColor.YELLOW,
            co2SavedGrams = 180,
            decompYears = 400,
            energySavedPercent = 70,
            tips = listOf(
                "Esvazia o frasco completamente",
                "Não precisas de remover o rótulo de papel"
            )
        ),
        "GLASS" to MaterialInfo(
            name = "Vidro",
            subtitle = "Embalagem · 100% reciclável",
            recycleCode = "GL 70",
            isRecyclable = true,
            ecopointColor = EcopointColor.GREEN,
            co2SavedGrams = 300,
            decompYears = 1000000,
            energySavedPercent = 30,
            tips = listOf(
                "Deposita no ecoponto verde",
                "Não quebres — pode ser perigoso",
                "Remove tampas metálicas antes de depositar"
            )
        ),
        "PAPER" to MaterialInfo(
            name = "Papel / Cartão",
            subtitle = "Embalagem · Código ♻ 20-22 · Reciclável",
            recycleCode = "PAP 20",
            isRecyclable = true,
            ecopointColor = EcopointColor.BLUE,
            co2SavedGrams = 150,
            decompYears = 5,
            energySavedPercent = 60,
            tips = listOf(
                "Deposita no ecoponto azul",
                "Cartão molhado ou engordurado vai para o lixo geral",
                "Dobra as caixas para poupar espaço"
            )
        ),
        "METAL" to MaterialInfo(
            name = "Metal / Alumínio",
            subtitle = "Lata · Código ♻ 41 · Reciclável",
            recycleCode = "ALU 41",
            isRecyclable = true,
            ecopointColor = EcopointColor.YELLOW,
            co2SavedGrams = 560,
            decompYears = 200,
            energySavedPercent = 95,
            tips = listOf(
                "Esvazia e enxagua a lata",
                "Não precisas de a esmagar — facilita a triagem"
            )
        ),
        "UNKNOWN" to MaterialInfo(
            name = "Material desconhecido",
            subtitle = "Não foi possível identificar",
            recycleCode = "—",
            isRecyclable = false,
            ecopointColor = EcopointColor.NONE,
            co2SavedGrams = 0,
            decompYears = 0,
            energySavedPercent = 0,
            tips = listOf(
                "Consulta o símbolo de reciclagem na embalagem",
                "Em caso de dúvida, deposita no lixo geral"
            )
        )
    )

    fun getByCode(code: String): MaterialInfo {
        return when {
            code.contains("PET", ignoreCase = true) || code.contains("1") -> materials["PET"]!!
            code.contains("HDPE", ignoreCase = true) || code.contains("2") -> materials["HDPE"]!!
            code.contains("GL", ignoreCase = true) -> materials["GLASS"]!!
            code.contains("PAP", ignoreCase = true) || code.contains("20") ||
                    code.contains("21") || code.contains("22") -> materials["PAPER"]!!

            code.contains("ALU", ignoreCase = true) || code.contains("41") -> materials["METAL"]!!
            else -> materials["UNKNOWN"]!!
        }
    }

    fun getByLabel(label: String): MaterialInfo {
        return when {
            label.contains("bottle", ignoreCase = true) ||
                    label.contains("garrafa", ignoreCase = true) -> materials["PET"]!!

            label.contains("glass", ignoreCase = true) ||
                    label.contains("vidro", ignoreCase = true) -> materials["GLASS"]!!

            label.contains("paper", ignoreCase = true) ||
                    label.contains("cardboard", ignoreCase = true) ||
                    label.contains("papel", ignoreCase = true) -> materials["PAPER"]!!

            label.contains("can", ignoreCase = true) ||
                    label.contains("metal", ignoreCase = true) ||
                    label.contains("lata", ignoreCase = true) -> materials["METAL"]!!

            else -> materials["UNKNOWN"]!!
        }
    }
}
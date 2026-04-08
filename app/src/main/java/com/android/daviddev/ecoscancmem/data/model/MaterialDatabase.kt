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
                "Esvazie e enxague antes de depositar",
                "Remova a tampa — pode ser de material diferente (PP 5)",
                "Amasse para reduzir o volume"
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
                "Esvazie o frasco completamente",
                "Não precisa de remover o rótulo de papel"
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
                "Deposite no ecoponto verde",
                "Não quebre — pode ser perigoso",
                "Remova tampas metálicas antes de depositar"
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
                "Deposite no ecoponto azul",
                "Cartão molhado ou engordurado vai para o lixo geral",
                "Dobre as caixas para poupar espaço"
            )
        ),
        "METAL" to MaterialInfo(
            name = "Metal / Aço",
            subtitle = "Lata de conserva · Código ♻ FE 40 · Reciclável",
            recycleCode = "FE 40",
            isRecyclable = true,
            ecopointColor = EcopointColor.YELLOW,
            co2SavedGrams = 400,
            decompYears = 200,
            energySavedPercent = 75,
            tips = listOf(
                "Esvazie e enxague a lata antes de depositar",
                "Remova a tampa com cuidado — deposite junto com a lata",
                "Rótulos de papel não precisam de ser retirados"
            )
        ),
        "ALU" to MaterialInfo(
            name = "Alumínio",
            subtitle = "Lata de bebida · Código ♻ ALU 41 · Reciclável",
            recycleCode = "ALU 41",
            isRecyclable = true,
            ecopointColor = EcopointColor.YELLOW,
            co2SavedGrams = 560,
            decompYears = 200,
            energySavedPercent = 95,
            tips = listOf(
                "Esvazie a lata completamente",
                "Não precisa de a esmagar — facilita a triagem automática",
                "Cápsulas de café de alumínio também vão aqui"
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
                "Consulte o símbolo de reciclagem na embalagem",
                "Em caso de dúvida, deposite no lixo geral"
            )
        )
    )

    fun getByCode(code: String): MaterialInfo {
        return when {
            code.equals("METAL", ignoreCase = true) -> materials["METAL"]!!
            code.startsWith("FE", ignoreCase = true) -> materials["METAL"]!!
            code.startsWith("ALU", ignoreCase = true) -> materials["ALU"]!!
            code.contains("PET", ignoreCase = true) || code == "1" -> materials["PET"]!!
            code.contains("HDPE", ignoreCase = true) || code == "2" -> materials["HDPE"]!!
            code.contains("PVC", ignoreCase = true) || code == "3" -> materials["PVC"]!!
            code.contains("LDPE", ignoreCase = true) || code == "4" -> materials["LDPE"]!!
            code.contains("PP", ignoreCase = true) || code == "5" -> materials["PP"]!!
            code.contains("PS", ignoreCase = true) || code == "6" -> materials["PS"]!!
            code.contains("GL", ignoreCase = true) -> materials["GLASS"]!!
            code.contains("PAP", ignoreCase = true) -> materials["PAPER"]!!
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
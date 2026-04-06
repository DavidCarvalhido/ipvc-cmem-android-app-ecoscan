package com.android.daviddev.ecoscancmem.data

object MaterialMapper {

    data class MappedMaterial(
        val key: String,           // chave para MaterialDatabase
        val confidence: Int,       // 0-100, calculado internamente
        val source: Source
    )

    enum class Source { OCR_CODE, OCR_TEXT, OBJECT_LABEL, COMBINED }

    // Mapeamento de labels do ML Kit -> material
    // O ML Kit devolve labels em inglês do seu conjunto fixo.
    // Este mapa cobre os casos mais comuns e os erros de classificação
    // conhecidos (ex: "bottle" para latas cilíndricas).

    private val labelToMaterial = mapOf(
        // Plástico
        "bottle" to "PET",
        "plastic bag" to "LDPE",
        "plastic" to "PET",
        "container" to "PET",
        "cup" to "PP",
        "straw" to "PP",
        "packaging" to "PET",
        "water bottle" to "PET",
        "beverage" to "PET",

        // Metal - inclui os erros comuns do ML Kit com latas
        "tin" to "METAL",
        "can" to "METAL",
        "aluminum" to "METAL",
        "aluminium" to "METAL",
        "metal" to "METAL",
        "food can" to "METAL",
        "tin can" to "METAL",
        "beverage can" to "METAL",
        // "bottle" quando contexto OCR sugere metal (tratado em resolveConflict)

        // Vidro
        "glass" to "GLASS",
        "glass bottle" to "GLASS",
        "jar" to "GLASS",
        "wine bottle" to "GLASS",

        // Papel/Cartão
        "paper" to "PAPER",
        "cardboard" to "PAPER",
        "newspaper" to "PAPER",
        "box" to "PAPER",
        "carton" to "PAPER",

        // Esferovite/PS
        "foam" to "PS",
        "styrofoam" to "PS",

        // Fallback
        "food" to "UNKNOWN",
        "plant" to "UNKNOWN",
        "flower" to "UNKNOWN",
        "person" to "UNKNOWN"
    )

    // Palavras-chave OCR que indicam metal
    // Latas de conserva raramente têm "ALU 41" impresso — mais comum
    // é texto como "recicle", "aço", "steel", "iron", ou o nome do
    // produto (atum, feijão, etc.) que indicamos como heurística.

    private val ocrMetalKeywords = setOf(
        "alu", "alumínio", "aluminum", "aluminium",
        "fe ", "aço", "steel", "iron", "inox",
        "atum", "sardinha", "aveludado", "feijão", "grão",
        "conserva", "tuna", "beans", "tomato"  // produtos típicos em lata
    )

    private val ocrGlassKeywords = setOf(
        "gl ", "vidro", "glass", "vinho", "wine",
        "cerveja", "beer", "azeite", "olive"
    )

    private val ocrPaperKeywords = setOf(
        "pap", "papel", "paper", "cartão", "cardboard",
        "recycle", "recicle"
    )

    // Regex de códigos de reciclagem - versão alargada

    private val recycleCodeRegex = Regex(
        // Cobre: "PET 1", "HDPE2", "ALU 41", "FE 40", "GL70",
        //        "PAP 20", "cod 1", "cod PET", "recycle 1", etc.
        pattern = """(?:♻\s*)?""" +
                """(PET|HDPE|PVC|LDPE|PP|PS|GL|PAP|ALU|FE|ABS|PC|PMMA|OTHER)""" +
                """\s*\d{0,2}""" +
                """|(?:♻\s*)(\d{1,2})""",
        option = RegexOption.IGNORE_CASE
    )

    // Códigos numéricos simples
    private val numericCodeMap = mapOf(
        "1" to "PET", "2" to "HDPE", "3" to "PVC",
        "4" to "LDPE", "5" to "PP", "6" to "PS",
        "7" to "OTHER",
        "20" to "PAPER", "21" to "PAPER", "22" to "PAPER",
        "40" to "METAL", "41" to "METAL",
        "70" to "GLASS", "71" to "GLASS", "72" to "GLASS"
    )

    // API pública

    /**
     * Resolve o material a partir da label do ML Kit e/ou texto OCR.
     * Devolve null se não for possível determinar com confiança mínima.
     */
    fun resolve(
        objectLabel: String?,
        objectConfidence: Float,
        ocrText: String?
    ): MappedMaterial? {

        val ocrCode = ocrText?.let { extractCode(it) }
        val ocrMaterial = ocrText?.let { inferFromOcrText(it) }
        val objMaterial = objectLabel?.let { mapLabel(it) }

        return when {
            // 1. Código de reciclagem explícito no OCR - maior prioridade
            ocrCode != null ->
                MappedMaterial(ocrCode, 95, Source.OCR_CODE)

            // 2. OCR identifica material e objeto concorda
            ocrMaterial != null && objMaterial == ocrMaterial ->
                MappedMaterial(ocrMaterial, 88, Source.COMBINED)

            // 3. OCR identifica material mas objeto discorda
            //    -> confia no OCR (texto é mais fiável que label genérica)
            ocrMaterial != null && objMaterial != null && objMaterial != ocrMaterial ->
                MappedMaterial(ocrMaterial, 78, Source.OCR_TEXT)

            // 4. Só OCR sem código mas com keywords
            ocrMaterial != null ->
                MappedMaterial(ocrMaterial, 70, Source.OCR_TEXT)

            // 5. Só Object Detection com confiança suficiente
            objMaterial != null && objMaterial != "UNKNOWN"
                    && objectConfidence >= 0.55f ->
                MappedMaterial(
                    objMaterial,
                    (objectConfidence * 100).toInt().coerceAtMost(82),
                    Source.OBJECT_LABEL
                )

            // 6. Não conseguiu determinar
            else -> null
        }
    }

    // Extração de código de reciclagem

    fun extractCode(text: String): String? {
        val match = recycleCodeRegex.find(text) ?: return null

        // Grupo 1: sigla textual (PET, ALU, etc.)
        val sigla = match.groupValues[1].takeIf { it.isNotBlank() }
        if (sigla != null) return sigla.uppercase()

        // Grupo 2: código numérico puro (cod 1, cod 41, etc.)
        val num = match.groupValues[2].takeIf { it.isNotBlank() }
        if (num != null) return numericCodeMap[num] ?: "UNKNOWN"

        return null
    }

    // Inferência por keywords OCR

    private fun inferFromOcrText(text: String): String? {
        val lower = text.lowercase()
        return when {
            ocrMetalKeywords.any { lower.contains(it) } -> "METAL"
            ocrGlassKeywords.any { lower.contains(it) } -> "GLASS"
            ocrPaperKeywords.any { lower.contains(it) } -> "PAPER"
            lower.contains("pet") || lower.contains("plástico") -> "PET"
            lower.contains("hdpe") -> "HDPE"
            lower.contains("ldpe") -> "LDPE"
            else -> null
        }
    }

    // Mapeamento de label do ML Kit

    private fun mapLabel(label: String): String? {
        val lower = label.lowercase().trim()
        // procura correspondência exata primeiro, depois parcial
        return labelToMaterial[lower]
            ?: labelToMaterial.entries
                .firstOrNull { lower.contains(it.key) }
                ?.value
    }
}
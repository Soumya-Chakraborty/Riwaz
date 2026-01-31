package com.example.riwaz.utils

import kotlin.math.*

/**
 * Shruti (Microtonal) Analyzer for Indian Classical Music
 * 
 * Indian classical music uses 22 shrutis (microtones) within an octave,
 * compared to 12 semitones in Western music. This analyzer detects:
 * 
 * - Precise shruti positions
 * - Deviation from expected shruti frequencies
 * - Raga-specific shruti expectations
 * - Intonation quality assessment
 */
class ShrutiAnalyzer {
    
    companion object {
        // 22 Shrutis with their names and cents values from Sa
        // Based on traditional Indian musicological ratios
        val SHRUTIS = listOf(
            ShrutiDefinition("Sa", 0f, "षड्ज"),
            ShrutiDefinition("ri1", 22f, "एकश्रुति रिषभ"),      // Komal Re (low)
            ShrutiDefinition("ri2", 70f, "द्विश्रुति रिषभ"),     // Komal Re (standard)
            ShrutiDefinition("Ri1", 90f, "त्रिश्रुति रिषभ"),     // Komal Re (high)
            ShrutiDefinition("Ri2", 112f, "चतुश्रुति रिषभ"),    // Shuddha Re
            ShrutiDefinition("ga1", 182f, "एकश्रुति गान्धार"),   // Komal Ga (low)
            ShrutiDefinition("ga2", 204f, "द्विश्रुति गान्धार"),  // Komal Ga (standard)
            ShrutiDefinition("Ga1", 294f, "त्रिश्रुति गान्धार"),  // Shuddha Ga (low)
            ShrutiDefinition("Ga2", 316f, "चतुश्रुति गान्धार"),   // Shuddha Ga (standard)
            ShrutiDefinition("Ma1", 386f, "एकश्रुति मध्यम"),      // Shuddha Ma
            ShrutiDefinition("Ma2", 408f, "द्विश्रुति मध्यम"),     // Shuddha Ma (high)
            ShrutiDefinition("ma1", 520f, "त्रिश्रुति मध्यम"),     // Teevra Ma (low)
            ShrutiDefinition("ma2", 590f, "चतुश्रुति मध्यम"),     // Teevra Ma (standard)
            ShrutiDefinition("Pa", 702f, "पञ्चम"),               // Pa (fixed)
            ShrutiDefinition("dha1", 792f, "एकश्रुति धैवत"),     // Komal Dha (low)
            ShrutiDefinition("dha2", 814f, "द्विश्रुति धैवत"),    // Komal Dha (standard)
            ShrutiDefinition("Dha1", 884f, "त्रिश्रुति धैवत"),    // Shuddha Dha (low)
            ShrutiDefinition("Dha2", 906f, "चतुश्रुति धैवत"),     // Shuddha Dha (standard)
            ShrutiDefinition("ni1", 996f, "एकश्रुति निषाद"),     // Komal Ni (low)
            ShrutiDefinition("ni2", 1018f, "द्विश्रुति निषाद"),   // Komal Ni (standard)
            ShrutiDefinition("Ni1", 1088f, "त्रिश्रुति निषाद"),   // Shuddha Ni (low)
            ShrutiDefinition("Ni2", 1110f, "चतुश्रुति निषाद")    // Shuddha Ni (standard)
        )
        
        // Raga-specific shruti preferences
        // Key = Raga name, Value = Map of note -> preferred shruti index
        val RAGA_SHRUTI_MAP = mapOf(
            "Yaman" to mapOf(
                "Re" to 4,      // Shuddha Re (Ri2)
                "Ga" to 8,      // Tivra Ga (Ga2)
                "Ma" to 12,     // Tivra Ma (ma2)
                "Dha" to 17,    // Shuddha Dha (Dha2)
                "Ni" to 21      // Tivra Ni (Ni2)
            ),
            "Bhairav" to mapOf(
                "Re" to 2,      // Komal Re (ri2) - uniquely low
                "Ga" to 8,      // Shuddha Ga (Ga2)
                "Ma" to 10,     // Shuddha Ma (Ma2)
                "Dha" to 15,    // Komal Dha (dha2)
                "Ni" to 21      // Shuddha Ni (Ni2)
            ),
            "Todi" to mapOf(
                "Re" to 2,      // Komal Re (ri2)
                "Ga" to 6,      // Komal Ga (ga2)
                "Ma" to 12,     // Tivra Ma (ma2)
                "Dha" to 15,    // Komal Dha (dha2)
                "Ni" to 21      // Shuddha Ni (Ni2)
            ),
            "Malkauns" to mapOf(
                "Ga" to 6,      // Komal Ga (ga2)
                "Ma" to 10,     // Shuddha Ma (Ma2)
                "Dha" to 14,    // Komal Dha (dha1) - low variant
                "Ni" to 18      // Komal Ni (ni1)
            ),
            "Darbari" to mapOf(
                "Re" to 4,      // Shuddha Re but unique treatment
                "Ga" to 5,      // Komal Ga - special low (ga1)
                "Ma" to 10,     // Shuddha Ma
                "Dha" to 14,    // Komal Dha - special low (dha1)
                "Ni" to 18      // Komal Ni (ni1) - with andolan
            )
        )
        
        // Default 12-note mapping to shruti indices
        val DEFAULT_SWAR_TO_SHRUTI = mapOf(
            "Sa" to 0,
            "Re(k)" to 2,   // Komal Re
            "Re" to 4,      // Shuddha Re
            "Ga(k)" to 6,   // Komal Ga
            "Ga" to 8,      // Shuddha Ga
            "Ma" to 10,     // Shuddha Ma
            "Ma(t)" to 12,  // Tivra Ma
            "Pa" to 13,     // Pa
            "Dha(k)" to 15, // Komal Dha
            "Dha" to 17,    // Shuddha Dha
            "Ni(k)" to 19,  // Komal Ni
            "Ni" to 21      // Shuddha Ni
        )
    }
    
    /**
     * Analyze pitch for shruti precision
     */
    fun analyzeShruti(
        frequency: Float,
        tonicFrequency: Float,
        raga: String = ""
    ): ShrutiAnalysisResult {
        if (frequency <= 0 || tonicFrequency <= 0) {
            return ShrutiAnalysisResult(
                detectedShruti = null,
                deviationCents = 0f,
                expectedShruti = null,
                intonationQuality = IntonationQuality.UNVOICED,
                nearestSwar = "",
                suggestions = emptyList()
            )
        }
        
        // Calculate cents from tonic (handling octave)
        val centsFromTonic = frequencyToCentsWithOctave(tonicFrequency, frequency)
        
        // Find nearest shruti
        val nearestShruti = findNearestShruti(centsFromTonic)
        val deviationCents = centsFromTonic - nearestShruti.cents
        
        // Find which swar this corresponds to
        val nearestSwar = findNearestSwar(centsFromTonic)
        
        // Get expected shruti for this raga (if specified)
        val expectedShruti = getExpectedShrutiForRaga(nearestSwar, raga)
        
        // Calculate intonation quality
        val intonationQuality = assessIntonation(deviationCents, expectedShruti, nearestShruti)
        
        // Generate suggestions
        val suggestions = generateSuggestions(
            deviationCents, nearestSwar, raga, expectedShruti, nearestShruti
        )
        
        return ShrutiAnalysisResult(
            detectedShruti = nearestShruti,
            deviationCents = deviationCents,
            expectedShruti = expectedShruti,
            intonationQuality = intonationQuality,
            nearestSwar = nearestSwar,
            suggestions = suggestions
        )
    }
    
    /**
     * Analyze a sequence of pitches for overall shruti precision
     */
    fun analyzeShrutiSequence(
        frequencies: List<Float>,
        tonicFrequency: Float,
        raga: String = ""
    ): ShrutiSequenceResult {
        val analyses = frequencies.filter { it > 0 }.map { freq ->
            analyzeShruti(freq, tonicFrequency, raga)
        }
        
        if (analyses.isEmpty()) {
            return ShrutiSequenceResult(
                averageDeviation = 0f,
                maxDeviation = 0f,
                intonationScore = 0f,
                qualityDistribution = emptyMap(),
                problematicNotes = emptyList()
            )
        }
        
        val deviations = analyses.map { abs(it.deviationCents) }
        val averageDeviation = deviations.average().toFloat()
        val maxDeviation = deviations.maxOrNull() ?: 0f
        
        // Intonation score: 100% at 0 deviation, decreasing with deviation
        // 10 cents deviation = ~90% score
        val intonationScore = (1 - averageDeviation / 50f).coerceIn(0f, 1f)
        
        // Quality distribution
        val qualityDistribution = analyses.groupBy { it.intonationQuality }
            .mapValues { (_, v) -> v.size.toFloat() / analyses.size }
        
        // Problematic notes (>20 cents deviation)
        val problematicNotes = analyses.filter { abs(it.deviationCents) > 20 }
            .distinctBy { it.nearestSwar }
            .map { ProblematicNote(it.nearestSwar, it.deviationCents) }
        
        return ShrutiSequenceResult(
            averageDeviation = averageDeviation,
            maxDeviation = maxDeviation,
            intonationScore = intonationScore,
            qualityDistribution = qualityDistribution,
            problematicNotes = problematicNotes
        )
    }
    
    /**
     * Get expected shruti positions for a raga
     */
    fun getExpectedShrutisForRaga(raga: String): Map<String, ShrutiDefinition> {
        val ragaPrefs = RAGA_SHRUTI_MAP[raga] ?: return emptyMap()
        
        return ragaPrefs.mapValues { (_, shrutiIndex) ->
            SHRUTIS.getOrElse(shrutiIndex) { SHRUTIS[0] }
        }
    }
    
    // ==================== Private Helpers ====================
    
    private fun frequencyToCentsWithOctave(tonicFreq: Float, freq: Float): Float {
        val rawCents = (1200 * ln(freq.toDouble() / tonicFreq) / ln(2.0)).toFloat()
        // Normalize to one octave (0-1200 cents)
        return ((rawCents % 1200f) + 1200f) % 1200f
    }
    
    private fun findNearestShruti(cents: Float): ShrutiDefinition {
        return SHRUTIS.minByOrNull { abs(it.cents - cents) } ?: SHRUTIS[0]
    }
    
    private fun findNearestSwar(cents: Float): String {
        val swarCents = mapOf(
            "Sa" to 0f,
            "Re(k)" to 90f,
            "Re" to 200f,
            "Ga(k)" to 315f,
            "Ga" to 400f,
            "Ma" to 500f,
            "Ma(t)" to 600f,
            "Pa" to 700f,
            "Dha(k)" to 800f,
            "Dha" to 900f,
            "Ni(k)" to 1015f,
            "Ni" to 1100f
        )
        
        return swarCents.minByOrNull { abs(it.value - cents) }?.key ?: "Sa"
    }
    
    private fun getExpectedShrutiForRaga(swar: String, raga: String): ShrutiDefinition? {
        if (raga.isEmpty()) return null
        
        val ragaPrefs = RAGA_SHRUTI_MAP[raga] ?: return null
        
        // Map swar name to simplified form
        val simpleSwar = when {
            swar.contains("(k)") -> swar.replace("(k)", "").replace("Re", "Re").replace("Ga", "Ga")
                .replace("Dha", "Dha").replace("Ni", "Ni")
            swar.contains("(t)") -> swar.replace("(t)", "")
            else -> swar
        }
        
        val shrutiIndex = ragaPrefs[simpleSwar] ?: return null
        return SHRUTIS.getOrElse(shrutiIndex) { null }
    }
    
    private fun assessIntonation(
        deviationCents: Float,
        expectedShruti: ShrutiDefinition?,
        actualShruti: ShrutiDefinition
    ): IntonationQuality {
        val absDeviation = abs(deviationCents)
        
        return when {
            absDeviation <= 5 -> IntonationQuality.EXCELLENT
            absDeviation <= 12 -> IntonationQuality.GOOD
            absDeviation <= 25 -> IntonationQuality.ACCEPTABLE
            absDeviation <= 40 -> IntonationQuality.NEEDS_WORK
            else -> IntonationQuality.POOR
        }
    }
    
    private fun generateSuggestions(
        deviationCents: Float,
        nearestSwar: String,
        raga: String,
        expectedShruti: ShrutiDefinition?,
        actualShruti: ShrutiDefinition
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (abs(deviationCents) > 15) {
            val direction = if (deviationCents > 0) "lower" else "raise"
            suggestions.add("Try to $direction your $nearestSwar slightly (${abs(deviationCents).toInt()} cents)")
        }
        
        if (expectedShruti != null && expectedShruti != actualShruti) {
            suggestions.add("In $raga, $nearestSwar should lean towards ${expectedShruti.hindiName}")
        }
        
        // Raga-specific suggestions
        if (raga == "Darbari" && (nearestSwar == "Ga(k)" || nearestSwar == "Dha(k)")) {
            suggestions.add("In Darbari, komal $nearestSwar uses extra-low shruti with andolan")
        }
        
        return suggestions
    }
}

// ==================== Data Classes ====================

data class ShrutiDefinition(
    val name: String,
    val cents: Float,
    val hindiName: String
)

enum class IntonationQuality {
    EXCELLENT,      // Within 5 cents
    GOOD,           // Within 12 cents
    ACCEPTABLE,     // Within 25 cents
    NEEDS_WORK,     // Within 40 cents
    POOR,           // More than 40 cents
    UNVOICED        // No pitch detected
}

data class ShrutiAnalysisResult(
    val detectedShruti: ShrutiDefinition?,
    val deviationCents: Float,
    val expectedShruti: ShrutiDefinition?,
    val intonationQuality: IntonationQuality,
    val nearestSwar: String,
    val suggestions: List<String>
)

data class ShrutiSequenceResult(
    val averageDeviation: Float,
    val maxDeviation: Float,
    val intonationScore: Float,     // 0-1, higher is better
    val qualityDistribution: Map<IntonationQuality, Float>,
    val problematicNotes: List<ProblematicNote>
)

data class ProblematicNote(
    val swar: String,
    val averageDeviation: Float
)

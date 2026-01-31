package com.example.riwaz.utils

import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * Data class for microtonal analysis results
 */
data class MicrotonalAnalysis(
    val swar: String,
    val microtonalDeviation: Float, // in cents
    val intendedExpression: String, // "komal", "tivra", "andolan", etc.
    val accuracy: Float,
    val isIntentional: Boolean = false
)

/**
 * Data class for ornamentation detection
 */
data class Ornamentation(
    val type: OrnamentType,
    val duration: Float, // in seconds
    val intensity: Float, // 0.0 to 1.0
    val accuracy: Float
)

/**
 * Enum for different types of ornamentations in Indian classical music
 */
enum class OrnamentType {
    MEEND,      // Gliding between notes
    ANDOLAN,    // Oscillation around a note
    KAN_SWAR,   // Grace note
    GAMAK,      // Vibratory movement
    TAN,        // Rapid melodic passage
    MURKI       // Quick grace note pattern
}

/**
 * Data class for melodic patterns
 */
data class MelodicPattern(
    val sequence: List<String>,
    val ragaCompliance: Float,
    val emotionalExpression: String,
    val technicalComplexity: String
)

/**
 * Class for advanced microtonal and ornamentation analysis
 */
class AdvancedMusicAnalyzer {
    
    companion object {
        private const val TAG = "AdvancedMusicAnalyzer"
        private const val MICROTONAL_TOLERANCE_CENTS = 50f // Acceptable deviation in cents
        private const val MEEND_THRESHOLD_CENTS = 20f     // Minimum deviation to qualify as meend
        private const val ANDOLAN_THRESHOLD_CENTS = 15f   // Minimum oscillation for andolan
    }
    
    /**
     * Performs microtonal analysis on a detected frequency
     */
    fun analyzeMicrotonalContent(detectedFreq: Float, expectedSwar: String): MicrotonalAnalysis {
        val expectedFreq = getExpectedFrequency(expectedSwar)
        val centsDeviation = calculateCentsDifference(detectedFreq, expectedFreq)
        
        val intendedExpression = when {
            expectedSwar.contains("(k)") && centsDeviation > 0 && centsDeviation < 100 -> "komal-variation"
            expectedSwar.contains("(t)") && centsDeviation < 0 && centsDeviation > -100 -> "tivra-variation"
            centsDeviation > MEEND_THRESHOLD_CENTS && centsDeviation < 100 -> "meend-approach"
            else -> "standard"
        }
        
        val accuracy = calculateMicrotonalAccuracy(centsDeviation, expectedSwar)
        val isIntentional = isIntentionalMicrotonalVariation(centsDeviation, expectedSwar)
        
        return MicrotonalAnalysis(
            swar = expectedSwar,
            microtonalDeviation = centsDeviation,
            intendedExpression = intendedExpression,
            accuracy = accuracy,
            isIntentional = isIntentional
        )
    }
    
    /**
     * Calculates microtonal accuracy considering Indian classical music norms
     */
    private fun calculateMicrotonalAccuracy(centsDeviation: Float, expectedSwar: String): Float {
        // For Indian classical music, some microtonal variations are intentional
        val tolerance = when {
            expectedSwar.contains("(k)") || expectedSwar.contains("(t)") -> 75f // More tolerance for komal/tivra
            else -> 50f // Standard tolerance
        }
        
        return if (centsDeviation <= tolerance) {
            1f - (centsDeviation / tolerance) * 0.8f // Even intentional variations affect accuracy slightly
        } else {
            maxOf(0f, 0.2f - ((centsDeviation - tolerance) / 100f)) // Severe penalty for excessive deviation
        }
    }
    
    /**
     * Determines if a microtonal variation is intentional based on raga context
     */
    private fun isIntentionalMicrotonalVariation(centsDeviation: Float, expectedSwar: String): Boolean {
        return when {
            // Komal notes often have intentional variations
            expectedSwar.contains("(k)") && centsDeviation < 75 -> true
            // Tivra notes often have intentional variations
            expectedSwar.contains("(t)") && centsDeviation < 75 -> true
            // Small variations are often intentional
            centsDeviation < 30 -> true
            else -> false
        }
    }
    
    /**
     * Detects ornamentations in a sequence of pitch values
     */
    fun detectOrnamentations(pitchSequence: List<Float>, timeSequence: List<Float>): List<Ornamentation> {
        val ornaments = mutableListOf<Ornamentation>()
        
        if (pitchSequence.size < 3) return ornaments // Need at least 3 points to detect patterns
        
        for (i in 1 until pitchSequence.size - 1) {
            val prevPitch = pitchSequence[i-1]
            val currentPitch = pitchSequence[i]
            val nextPitch = pitchSequence[i+1]
            
            val prevTime = timeSequence[i-1]
            val currentTime = timeSequence[i]
            val nextTime = timeSequence[i+1]
            
            // Check for meend (glide between notes)
            if (isMeend(prevPitch, currentPitch, nextPitch)) {
                val duration = nextTime - prevTime
                val intensity = calculateMeendIntensity(prevPitch, currentPitch, nextPitch)
                val accuracy = calculateOrnamentationAccuracy(prevPitch, currentPitch, nextPitch)
                
                ornaments.add(
                    Ornamentation(
                        type = OrnamentType.MEEND,
                        duration = duration,
                        intensity = intensity,
                        accuracy = accuracy
                    )
                )
            }
            
            // Check for andolan (oscillation)
            if (isAndolan(prevPitch, currentPitch, nextPitch)) {
                val duration = nextTime - prevTime
                val intensity = calculateAndolanIntensity(prevPitch, currentPitch, nextPitch)
                val accuracy = calculateOrnamentationAccuracy(prevPitch, currentPitch, nextPitch)
                
                ornaments.add(
                    Ornamentation(
                        type = OrnamentType.ANDOLAN,
                        duration = duration,
                        intensity = intensity,
                        accuracy = accuracy
                    )
                )
            }
        }
        
        return ornaments
    }
    
    /**
     * Checks if a sequence represents a meend (glide)
     */
    private fun isMeend(prevPitch: Float, currentPitch: Float, nextPitch: Float): Boolean {
        val centsDiff1 = calculateCentsDifference(prevPitch, currentPitch)
        val centsDiff2 = calculateCentsDifference(currentPitch, nextPitch)
        
        // Meend involves gradual pitch change between two distinct notes
        return centsDiff1 > MEEND_THRESHOLD_CENTS && centsDiff2 > MEEND_THRESHOLD_CENTS
    }
    
    /**
     * Checks if a sequence represents an andolan (oscillation)
     */
    private fun isAndolan(prevPitch: Float, currentPitch: Float, nextPitch: Float): Boolean {
        // Andolan involves oscillation around a central note
        val avgPitch = (prevPitch + currentPitch + nextPitch) / 3
        val deviation1 = calculateCentsDifference(prevPitch, avgPitch)
        val deviation2 = calculateCentsDifference(nextPitch, avgPitch)
        
        return deviation1 > ANDOLAN_THRESHOLD_CENTS && deviation2 > ANDOLAN_THRESHOLD_CENTS
    }
    
    /**
     * Calculates meend intensity based on pitch trajectory
     */
    private fun calculateMeendIntensity(prevPitch: Float, currentPitch: Float, nextPitch: Float): Float {
        val totalDeviation = calculateCentsDifference(prevPitch, nextPitch)
        return minOf(1.0f, totalDeviation / 100f) // Normalize to 0-1 range
    }
    
    /**
     * Calculates andolan intensity based on oscillation magnitude
     */
    private fun calculateAndolanIntensity(prevPitch: Float, currentPitch: Float, nextPitch: Float): Float {
        val avgPitch = (prevPitch + currentPitch + nextPitch) / 3
        val deviation1 = calculateCentsDifference(prevPitch, avgPitch)
        val deviation2 = calculateCentsDifference(nextPitch, avgPitch)
        
        val avgDeviation = (deviation1 + deviation2) / 2
        return minOf(1.0f, avgDeviation / 50f) // Normalize to 0-1 range
    }
    
    /**
     * Calculates ornamentation accuracy
     */
    private fun calculateOrnamentationAccuracy(prevPitch: Float, currentPitch: Float, nextPitch: Float): Float {
        // For now, basic accuracy calculation
        // In a real implementation, this would consider proper execution of ornamentations
        return 0.8f // Placeholder value
    }
    
    /**
     * Analyzes melodic phrases for raga compliance with sophisticated pattern matching
     */
    fun analyzeMelodicPhrases(swarSequence: List<String>, raga: String): List<MelodicPattern> {
        val patterns = mutableListOf<MelodicPattern>()
        val ragaSpecificPatterns = getRagaSpecificPatterns(raga)

        // Look for common raga phrases and patterns with flexible matching
        for (i in 0 until swarSequence.size - 2) {
            // Check for different phrase lengths (2-6 notes)
            for (length in 2..minOf(6, swarSequence.size - i)) {
                val phrase = swarSequence.subList(i, i + length)

                // Check against known raga phrases with flexible matching
                val compliance = checkPhraseCompliance(phrase, ragaSpecificPatterns)
                val emotionalContent = inferEmotionalContent(phrase, raga)
                val technicalComplexity = assessTechnicalComplexity(phrase)

                if (compliance > 0.3f) { // Only add meaningful patterns
                    patterns.add(MelodicPattern(phrase, compliance, emotionalContent, technicalComplexity))
                }
            }
        }

        return patterns
    }

    /**
     * Checks phrase compliance against raga-specific patterns
     */
    private fun checkPhraseCompliance(phrase: List<String>, ragaPatterns: List<List<String>>): Float {
        // Check if the phrase matches any of the raga's characteristic patterns
        val matches = ragaPatterns.count { ragaPhrase ->
            ragaPhrase.size <= phrase.size && 
            ragaPhrase.withIndex().all { (idx, swar) -> 
                idx < phrase.size && (swar == phrase[idx] || swar.contains(phrase[idx]))
            }
        }
        
        return if (ragaPatterns.isNotEmpty()) matches.toFloat() / ragaPatterns.size else 0f
    }
    
    /**
     * Infers emotional content from melodic phrase
     */
    private fun inferEmotionalContent(phrase: List<String>, raga: String): String {
        // This would be implemented based on raga theory and melodic analysis
        // For now, returning a placeholder
        return when (raga) {
            "Yaman" -> "Romantic and devotional"
            "Bhairav" -> "Serene and meditative"
            "Todi" -> "Pathetic and devotional"
            "Malkauns" -> "Serious and contemplative"
            else -> "Expressive"
        }
    }
    
    /**
     * Assesses technical complexity of a phrase
     */
    private fun assessTechnicalComplexity(phrase: List<String>): String {
        val noteChanges = phrase.zipWithNext().count { (a, b) -> a != b }
        val range = calculateSwarRange(phrase)
        
        return when {
            noteChanges > 5 && range > 5 -> "Very Complex"
            noteChanges > 3 && range > 3 -> "Complex"
            noteChanges > 1 -> "Moderate"
            else -> "Simple"
        }
    }
    
    /**
     * Calculates the range of swars in a phrase
     */
    private fun calculateSwarRange(phrase: List<String>): Int {
        val swarIndices = phrase.map { swar ->
            when (swar) {
                "Sa" -> 1
                "Re(k)" -> 2
                "Re" -> 3
                "Ga(k)" -> 4
                "Ga" -> 5
                "Ma" -> 6
                "Ma(t)" -> 7
                "Pa" -> 8
                "Dha(k)" -> 9
                "Dha" -> 10
                "Ni(k)" -> 11
                "Ni" -> 12
                else -> 1
            }
        }
        
        return if (swarIndices.isNotEmpty()) {
            swarIndices.maxOrNull()?.minus(swarIndices.minOrNull() ?: 0) ?: 0
        } else 0
    }
    
    /**
     * Gets raga-specific characteristic patterns
     */
    private fun getRagaSpecificPatterns(raga: String): List<List<String>> {
        return when (raga) {
            "Yaman" -> listOf(
                listOf("Ni", "Sa", "Re", "Ga"),
                listOf("Ma(t)", "Pa", "Dha", "Ni"),
                listOf("Ga", "Ma(t)", "Pa")
            )
            "Bhairav" -> listOf(
                listOf("Sa", "Re(k)", "Ga", "Ma"),
                listOf("Pa", "Dha(k)", "Ni", "Sa'")
            )
            "Todi" -> listOf(
                listOf("Sa", "Re(k)", "Ga(k)", "Ma"),
                listOf("Pa", "Dha(k)", "Ni(k)", "Sa'")
            )
            "Malkauns" -> listOf(
                listOf("Sa", "Ga(k)", "Ma", "Ni(k)", "Sa'"),
                listOf("Ma", "Ni(k)", "Ga(k)", "Sa")
            )
            else -> listOf(listOf("Sa", "Re", "Ga", "Pa")) // Generic pattern
        }
    }
    
    /**
     * Calculates frequency difference in cents (1/100th of a semitone)
     */
    private fun calculateCentsDifference(freq1: Float, freq2: Float): Float {
        if (freq1 <= 0 || freq2 <= 0) return Float.MAX_VALUE
        return 1200 * kotlin.math.log2(freq1 / freq2).absoluteValue
    }
    
    /**
     * Gets expected frequency for a swar based on standard tuning
     */
    private fun getExpectedFrequency(swar: String): Float {
        val baseFreq = 261.63f // Sa at C
        val ratio = when (swar) {
            "Sa" -> 1.0f
            "Re(k)" -> 1.053f
            "Re" -> 1.125f
            "Ga(k)" -> 1.189f
            "Ga" -> 1.260f
            "Ma" -> 1.335f
            "Ma(t)" -> 1.414f
            "Pa" -> 1.498f
            "Dha(k)" -> 1.587f
            "Dha" -> 1.682f
            "Ni(k)" -> 1.782f
            "Ni" -> 1.888f
            else -> 1.0f
        }
        
        return baseFreq * ratio
    }
}
package com.example.riwaz.utils

import kotlin.math.max
import kotlin.math.min

/**
 * Vocal range assessment for Indian Classical Music
 * Analyzes recorded sessions to determine vocal range and compare with typical ranges
 */
class VocalRangeAnalyzer {
    
    /**
     * Represents vocal ranges for different voice types
     */
    data class VoiceRange(
        val name: String,
        val minFreq: Float, // in Hz
        val maxFreq: Float, // in Hz
        val description: String
    )
    
    /**
     * Typical vocal ranges for Indian Classical Music
     */
    companion object {
        val TYPICAL_VOICE_RANGES = mapOf(
            "Soprano" to VoiceRange("Soprano", 261.63f, 1046.50f, "Highest female voice"),
            "Mezzo-Soprano" to VoiceRange("Mezzo-Soprano", 196.00f, 783.99f, "Middle female voice"),
            "Alto" to VoiceRange("Alto", 174.61f, 698.46f, "Lowest female voice"),
            "Tenor" to VoiceRange("Tenor", 130.81f, 523.25f, "Highest male voice"),
            "Baritone" to VoiceRange("Baritone", 98.00f, 392.00f, "Middle male voice"),
            "Bass" to VoiceRange("Bass", 65.41f, 261.63f, "Lowest male voice")
        )
        
        // Standard frequencies for Indian classical music (based on Sa = 261.63 Hz - C scale)
        val INDIAN_CLASSICAL_NOTES = mapOf(
            "Sa" to 261.63f,      // C
            "Re(k)" to 275.71f,   // C# (Komala Re)
            "Re" to 293.66f,      // D (Shuddha Re)
            "Ga(k)" to 309.23f,   // D# (Komala Ga)
            "Ga" to 329.63f,      // E (Shuddha Ga)
            "Ma" to 349.23f,      // F (Shuddha Ma)
            "Ma(t)" to 370.79f,   // F# (Tivra Ma)
            "Pa" to 392.00f,      // G (Panchama)
            "Dha(k)" to 413.41f,  // G# (Komala Dha)
            "Dha" to 440.00f,     // A (Shuddha Dha)
            "Ni(k)" to 466.16f,   // A# (Komala Ni)
            "Ni" to 493.88f       // B (Shuddha Ni)
        )
    }
    
    /**
     * Analyzes a recording to determine vocal range
     * @param audioData The audio samples to analyze
     * @param sampleRate Sample rate of the audio
     * @return VocalRangeResult with min/max frequencies and range assessment
     */
    fun analyzeVocalRange(audioData: FloatArray, sampleRate: Int): VocalRangeResult {
        val pitchTracker = mutableListOf<Float>()
        
        // Analyze the audio in segments to track pitch variations
        val windowSize = sampleRate / 10 // 100ms windows
        val hopSize = windowSize / 2     // 50% overlap
        
        var startIdx = 0
        while (startIdx < audioData.size - windowSize) {
            val window = audioData.sliceArray(startIdx until min(startIdx + windowSize, audioData.size))
            val pitch = AudioAnalyzer().detectPitch(window, sampleRate)
            
            if (pitch > 0) { // Valid pitch detected
                pitchTracker.add(pitch)
            }
            
            startIdx += hopSize
        }
        
        if (pitchTracker.isEmpty()) {
            return VocalRangeResult(0f, 0f, "", emptyList())
        }
        
        val minFreq = pitchTracker.minOrNull() ?: 0f
        val maxFreq = pitchTracker.maxOrNull() ?: 0f
        
        // Determine which notes were covered
        val coveredNotes = getCoveredNotes(minFreq, maxFreq)
        
        return VocalRangeResult(
            minFrequency = minFreq,
            maxFrequency = maxFreq,
            voiceTypeAssessment = assessVoiceType(minFreq, maxFreq),
            coveredNotes = coveredNotes
        )
    }
    
    /**
     * Determines which notes were covered in the vocal range
     */
    private fun getCoveredNotes(minFreq: Float, maxFreq: Float): List<String> {
        val covered = mutableListOf<String>()
        
        for ((note, freq) in INDIAN_CLASSICAL_NOTES) {
            if (freq >= minFreq * 0.8f && freq <= maxFreq * 1.2f) { // Allow some tolerance
                covered.add(note)
            }
        }
        
        return covered
    }
    
    /**
     * Assesses the likely voice type based on vocal range
     */
    private fun assessVoiceType(minFreq: Float, maxFreq: Float): String {
        var bestMatch = "Unknown"
        var bestScore = 0f
        
        for ((voiceTypeName, voiceRange) in TYPICAL_VOICE_RANGES) {
            val overlap = calculateRangeOverlap(
                minFreq, maxFreq,
                voiceRange.minFreq, voiceRange.maxFreq
            )
            
            if (overlap > bestScore) {
                bestScore = overlap
                bestMatch = voiceTypeName
            }
        }
        
        return bestMatch
    }
    
    /**
     * Calculates the overlap between two frequency ranges
     */
    private fun calculateRangeOverlap(min1: Float, max1: Float, min2: Float, max2: Float): Float {
        if (max1 < min2 || max2 < min1) return 0f // No overlap
        
        val overlapStart = max(min1, min2)
        val overlapEnd = min(max1, max2)
        val overlapSize = overlapEnd - overlapStart
        
        // Calculate as percentage of the smaller range
        val range1Size = max1 - min1
        val range2Size = max2 - min2
        val minRangeSize = min(range1Size, range2Size)
        
        return if (minRangeSize > 0) overlapSize / minRangeSize else 0f
    }
    
    /**
     * Compares a user's vocal range with typical ranges
     */
    fun compareWithTypicalRanges(vocalRange: VocalRangeResult): List<VoiceRangeComparison> {
        val comparisons = mutableListOf<VoiceRangeComparison>()
        
        for ((voiceTypeName, voiceRange) in TYPICAL_VOICE_RANGES) {
            val overlap = calculateRangeOverlap(
                vocalRange.minFrequency, vocalRange.maxFrequency,
                voiceRange.minFreq, voiceRange.maxFreq
            )
            
            val minCoverage = calculateCoverage(
                vocalRange.minFrequency,
                voiceRange.minFreq, voiceRange.maxFreq
            )
            
            val maxCoverage = calculateCoverage(
                vocalRange.maxFrequency,
                voiceRange.minFreq, voiceRange.maxFreq
            )
            
            comparisons.add(
                VoiceRangeComparison(
                    voiceType = voiceTypeName,
                    overlapPercentage = overlap,
                    minCoverage = minCoverage,
                    maxCoverage = maxCoverage,
                    isPotentialMatch = overlap > 0.3f // At least 30% overlap
                )
            )
        }
        
        return comparisons
    }
    
    /**
     * Calculates how much of a range is covered by a boundary value
     */
    private fun calculateCoverage(boundary: Float, rangeMin: Float, rangeMax: Float): Float {
        return when {
            boundary < rangeMin -> 0f
            boundary > rangeMax -> 1f
            else -> (boundary - rangeMin) / (rangeMax - rangeMin)
        }
    }
}

/**
 * Data class for vocal range analysis results
 */
data class VocalRangeResult(
    val minFrequency: Float,
    val maxFrequency: Float,
    val voiceTypeAssessment: String,
    val coveredNotes: List<String>
)

/**
 * Data class for voice range comparison
 */
data class VoiceRangeComparison(
    val voiceType: String,
    val overlapPercentage: Float,
    val minCoverage: Float,
    val maxCoverage: Float,
    val isPotentialMatch: Boolean
)
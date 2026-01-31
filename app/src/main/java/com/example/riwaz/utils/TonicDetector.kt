package com.example.riwaz.utils

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Improved Tonic Detection with Harmonic Analysis
 * Uses multiple techniques to accurately detect the user's reference Sa (tonic)
 */
class TonicDetector {
    
    companion object {
        private const val MIN_FREQ = 80.0f   // Minimum vocal frequency
        private const val MAX_FREQ = 500.0f  // Maximum vocal frequency for Sa
        private const val HARMONIC_TOLERANCE = 0.05f // 5% tolerance for harmonic relationships
    }
    
    /**
     * Detects the tonic (Sa) frequency from the audio signal
     */
    fun detectTonic(audioData: FloatArray, sampleRate: Int): TonicDetectionResult {
        // Multiple approaches to detect tonic
        val histogramBased = detectTonicByHistogram(audioData, sampleRate)
        val harmonicBased = detectTonicByHarmonicAnalysis(audioData, sampleRate)
        val stabilityBased = detectTonicByStability(audioData, sampleRate)
        
        // Combine results using weighted voting
        val combinedResult = combineTonicResults(
            histogramBased,
            harmonicBased,
            stabilityBased
        )
        
        return combinedResult
    }
    
    /**
     * Detects tonic by analyzing pitch histogram (most frequently occurring pitch)
     */
    private fun detectTonicByHistogram(audioData: FloatArray, sampleRate: Int): TonicDetectionResult {
        // Process audio in chunks to detect prevalent pitches
        val chunkSize = sampleRate / 2 // Half-second chunks
        val pitchHistogram = mutableMapOf<Float, Int>()
        
        for (i in 0 until audioData.size step chunkSize) {
            val chunk = audioData.sliceArray(i until minOf(i + chunkSize, audioData.size))
            val yinDetector = YinPitchDetector(sampleRate)
            val pitch = yinDetector.detectPitchOptimized(chunk)
            
            if (pitch > 0 && pitch >= MIN_FREQ && pitch <= MAX_FREQ) {
                // Quantize to nearest 5Hz to group similar frequencies
                val quantizedPitch = (kotlin.math.round(pitch / 5)).toInt() * 5f
                pitchHistogram[quantizedPitch] = pitchHistogram.getOrDefault(quantizedPitch, 0) + 1
            }
        }
        
        // Find the most frequent pitch
        val mostFrequentPitch = pitchHistogram.maxByOrNull { it.value }?.key ?: 0f
        
        return TonicDetectionResult(
            frequency = mostFrequentPitch,
            confidence = if (mostFrequentPitch > 0) {
                val count = pitchHistogram[mostFrequentPitch]?.toFloat() ?: 0f
                val total = pitchHistogram.values.sum().toFloat()
                if (total > 0) count / total else 0f
            } else 0f,
            method = "Histogram"
        )
    }
    
    /**
     * Detects tonic by analyzing harmonic relationships
     */
    private fun detectTonicByHarmonicAnalysis(audioData: FloatArray, sampleRate: Int): TonicDetectionResult {
        // Analyze the harmonic structure of prominent pitches
        val chunkSize = sampleRate / 2
        val harmonicScores = mutableMapOf<Float, Float>()
        
        for (i in 0 until audioData.size step chunkSize) {
            val chunk = audioData.sliceArray(i until minOf(i + chunkSize, audioData.size))
            val yinDetector = YinPitchDetector(sampleRate)
            val primaryPitch = yinDetector.detectPitchOptimized(chunk)
            
            if (primaryPitch > 0 && primaryPitch >= MIN_FREQ && primaryPitch <= MAX_FREQ) {
                // Check if this pitch could be a harmonic of lower frequencies
                for (candidateTonic in (MIN_FREQ.toInt()..primaryPitch.toInt()) step 5) {
                    val candidateFreq = candidateTonic.toFloat()
                    
                    // Check if primary pitch is a harmonic of candidate tonic
                    val harmonicNumber = primaryPitch / candidateFreq
                    val roundedHarmonic = harmonicNumber.toInt()
                    
                    if (abs(harmonicNumber - roundedHarmonic.toFloat()) < HARMONIC_TOLERANCE && roundedHarmonic in 1..8) {
                        // This suggests candidateFreq could be the tonic
                        val score = harmonicScores.getOrDefault(candidateFreq, 0f) + (1f / roundedHarmonic.toFloat())
                        harmonicScores[candidateFreq] = score
                    }
                }
            }
        }
        
        // Find the frequency with the highest harmonic score
        val bestCandidate = harmonicScores.maxByOrNull { it.value }?.key ?: 0f
        
        return TonicDetectionResult(
            frequency = bestCandidate,
            confidence = if (bestCandidate > 0) {
                val score = harmonicScores[bestCandidate] ?: 0f
                val total = harmonicScores.values.sum()
                if (total > 0) score / total else 0f
            } else 0f,
            method = "Harmonic Analysis"
        )
    }
    
    /**
     * Detects tonic by analyzing pitch stability over time
     */
    private fun detectTonicByStability(audioData: FloatArray, sampleRate: Int): TonicDetectionResult {
        // Find the most stable pitch over time
        val chunkSize = sampleRate / 2
        val pitchStability = mutableMapOf<Float, Float>()
        val pitchOccurrences = mutableMapOf<Float, MutableList<Float>>() // Track pitch values over time
        
        for (i in 0 until audioData.size step chunkSize) {
            val chunk = audioData.sliceArray(i until minOf(i + chunkSize, audioData.size))
            val yinDetector = YinPitchDetector(sampleRate)
            val pitch = yinDetector.detectPitchOptimized(chunk)
            
            if (pitch > 0 && pitch >= MIN_FREQ && pitch <= MAX_FREQ) {
                // Quantize to group similar frequencies
                val quantizedPitch = (kotlin.math.round(pitch / 5)).toInt() * 5f
                
                if (!pitchOccurrences.containsKey(quantizedPitch)) {
                    pitchOccurrences[quantizedPitch] = mutableListOf()
                }
                pitchOccurrences[quantizedPitch]?.add(pitch)
            }
        }
        
        // Calculate stability as inverse of variance for each pitch
        for ((pitch, occurrences) in pitchOccurrences) {
            if (occurrences.size >= 3) { // Need at least 3 occurrences to calculate stability
                val mean = occurrences.average()
                val variance = occurrences.sumOf { (it - mean) * (it - mean) } / occurrences.size
                val stability = if (variance > 0) 1f / (1f + variance.toFloat()) else 1f // Higher stability = lower variance
                
                pitchStability[pitch] = stability
            }
        }
        
        // Find the most stable pitch
        val mostStablePitch = pitchStability.maxByOrNull { it.value }?.key ?: 0f
        
        return TonicDetectionResult(
            frequency = mostStablePitch,
            confidence = if (mostStablePitch > 0) pitchStability[mostStablePitch] ?: 0f else 0f,
            method = "Stability Analysis"
        )
    }
    
    /**
     * Combines results from multiple detection methods
     */
    private fun combineTonicResults(
        histogramResult: TonicDetectionResult,
        harmonicResult: TonicDetectionResult,
        stabilityResult: TonicDetectionResult
    ): TonicDetectionResult {
        // Weight the results based on confidence
        val totalConfidence = histogramResult.confidence + harmonicResult.confidence + stabilityResult.confidence
        
        if (totalConfidence == 0f) {
            return TonicDetectionResult(0f, 0f, "No reliable detection")
        }
        
        // Weighted average of frequencies
        val weightedFreq = (
            histogramResult.frequency * histogramResult.confidence +
            harmonicResult.frequency * harmonicResult.confidence +
            stabilityResult.frequency * stabilityResult.confidence
        ) / totalConfidence
        
        // Overall confidence is the average of individual confidences
        val overallConfidence = totalConfidence / 3f
        
        return TonicDetectionResult(
            frequency = weightedFreq,
            confidence = overallConfidence,
            method = "Combined (${histogramResult.method}, ${harmonicResult.method}, ${stabilityResult.method})"
        )
    }
    
    /**
     * Adjusts a given frequency relative to the detected tonic
     */
    fun adjustToTonic(frequency: Float, tonic: Float): Float {
        if (tonic <= 0) return frequency
        
        // Calculate the ratio relative to tonic
        val ratio = frequency / tonic
        
        // Find the closest octave of the tonic
        var adjustedRatio = ratio
        while (adjustedRatio > 2.0f) adjustedRatio /= 2.0f
        while (adjustedRatio < 1.0f) adjustedRatio *= 2.0f
        
        return tonic * adjustedRatio
    }

    /**
     * Validates if a detected tonic is musically reasonable
     */
    fun validateTonic(tonic: Float): Boolean {
        return tonic >= MIN_FREQ && tonic <= MAX_FREQ
    }
}

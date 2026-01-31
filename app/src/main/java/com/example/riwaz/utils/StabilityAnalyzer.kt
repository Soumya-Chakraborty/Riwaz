package com.example.riwaz.utils

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Advanced Stability Analysis incorporating multiple metrics
 * Combines pitch stability, amplitude stability, and harmonic consistency
 * 
 * Enhanced with SOTA algorithms:
 * - Real FFT processing for accurate harmonic analysis
 * - pYIN pitch detection for robust pitch tracking
 * - HNR (Harmonic-to-Noise Ratio) for voice quality assessment
 */
class StabilityAnalyzer(private val sampleRate: Int = 44100) {
    
    // SOTA analysis engines
    private val fftProcessor = FFTProcessor()
    private val pYinDetector = PYinPitchDetector(sampleRate)
    
    /**
     * Enhanced stability analysis using SOTA algorithms
     */
    fun analyzeStabilityWithFFT(
        audioData: FloatArray,
        tonicFrequency: Float
    ): StabilityMetrics {
        if (audioData.isEmpty()) {
            return StabilityMetrics(
                pitchStability = 0f,
                amplitudeStability = 0f,
                harmonicConsistency = 0f,
                overallStability = 0f,
                hnr = 0f,
                spectralCentroid = 0f,
                vibratoRate = 0f,
                vibratoExtent = 0f
            )
        }
        
        // Detect pitch using pYIN for more accurate tracking
        val pitchSequence = pYinDetector.detectPitchSequence(audioData, 2048, 512)
        val voicedPitches = pitchSequence.filter { it.frequency > 0 }
        
        if (voicedPitches.isEmpty()) {
            return StabilityMetrics(
                pitchStability = 0f,
                amplitudeStability = 0f,
                harmonicConsistency = 0f,
                overallStability = 0f
            )
        }
        
        val meanPitch = voicedPitches.map { it.frequency }.average().toFloat()
        
        // Calculate stability metrics
        val pitchStability = calculatePitchStabilityFromSequence(voicedPitches)
        val amplitudeStability = calculateAmplitudeStability(audioData)
        val harmonicConsistency = calculateHarmonicConsistencyWithFFT(audioData, meanPitch)
        
        // Additional SOTA metrics
        val hnr = fftProcessor.calculateHNR(audioData, sampleRate, meanPitch)
        val spectralCentroid = fftProcessor.calculateSpectralCentroid(audioData, sampleRate)
        val (vibratoRate, vibratoExtent) = analyzeVibrato(voicedPitches)
        
        // Weighted combination
        val overallStability = combineStabilityMetrics(
            pitchStability,
            amplitudeStability,
            harmonicConsistency
        )
        
        return StabilityMetrics(
            pitchStability = pitchStability,
            amplitudeStability = amplitudeStability,
            harmonicConsistency = harmonicConsistency,
            overallStability = overallStability,
            hnr = hnr,
            spectralCentroid = spectralCentroid,
            vibratoRate = vibratoRate,
            vibratoExtent = vibratoExtent
        )
    }
    
    /**
     * Legacy method for backward compatibility
     */
    fun analyzeStability(
        audioData: FloatArray,
        sampleRate: Int,
        fundamentalFreq: Float
    ): StabilityMetrics {
        if (fundamentalFreq <= 0) {
            return StabilityMetrics(
                pitchStability = 0f,
                amplitudeStability = 0f,
                harmonicConsistency = 0f,
                overallStability = 0f
            )
        }
        
        // Calculate individual stability metrics
        val pitchStability = calculatePitchStability(audioData, sampleRate, fundamentalFreq)
        val amplitudeStability = calculateAmplitudeStability(audioData)
        val harmonicConsistency = calculateHarmonicConsistencyWithFFT(audioData, fundamentalFreq)
        
        // Weighted combination of all metrics
        val overallStability = combineStabilityMetrics(
            pitchStability, 
            amplitudeStability, 
            harmonicConsistency
        )
        
        return StabilityMetrics(
            pitchStability = pitchStability,
            amplitudeStability = amplitudeStability,
            harmonicConsistency = harmonicConsistency,
            overallStability = overallStability
        )
    }
    
    /**
     * Calculate pitch stability from pYIN sequence
     */
    private fun calculatePitchStabilityFromSequence(pitches: List<PitchResult>): Float {
        if (pitches.size < 2) return 1f
        
        val frequencies = pitches.map { it.frequency }
        val meanPitch = frequencies.average()
        val variance = frequencies.sumOf { (it - meanPitch) * (it - meanPitch) } / frequencies.size
        val stdDev = sqrt(variance)
        
        // Convert to cents deviation
        val centsDeviation = if (meanPitch > 0) {
            (stdDev / meanPitch) * 1200 // Approximate cents
        } else 0.0
        
        // Score: 100 cents deviation = 0 stability, 0 deviation = 1.0
        return (1f - (centsDeviation / 100f).toFloat()).coerceIn(0f, 1f)
    }
    
    /**
     * Analyze vibrato characteristics
     */
    private fun analyzeVibrato(pitches: List<PitchResult>): Pair<Float, Float> {
        if (pitches.size < 10) return Pair(0f, 0f)
        
        val frequencies = pitches.map { it.frequency }
        val meanPitch = frequencies.average().toFloat()
        
        // Convert to cents deviation from mean
        val centsDeviations = frequencies.map { freq ->
            if (freq > 0 && meanPitch > 0) {
                (1200 * kotlin.math.ln(freq.toDouble() / meanPitch) / kotlin.math.ln(2.0)).toFloat()
            } else 0f
        }
        
        // Count zero crossings to estimate vibrato rate
        var zeroCrossings = 0
        for (i in 1 until centsDeviations.size) {
            if (centsDeviations[i - 1] * centsDeviations[i] < 0) {
                zeroCrossings++
            }
        }
        
        // Estimate rate (Hz) - assuming ~12ms per frame (512 hop / 44100 sr)
        val frameDuration = 512f / sampleRate
        val totalDuration = pitches.size * frameDuration
        val rate = if (totalDuration > 0) (zeroCrossings / 2f) / totalDuration else 0f
        
        // Vibrato extent (peak-to-peak cents)
        val maxCents = centsDeviations.maxOrNull() ?: 0f
        val minCents = centsDeviations.minOrNull() ?: 0f
        val extent = maxCents - minCents
        
        return Pair(rate, extent)
    }
    
    /**
     * Calculate harmonic consistency using real FFT
     */
    private fun calculateHarmonicConsistencyWithFFT(
        audioData: FloatArray,
        fundamentalFreq: Float
    ): Float {
        if (fundamentalFreq <= 0) return 0f
        
        val harmonics = fftProcessor.detectHarmonics(audioData, sampleRate, fundamentalFreq, 6)
        
        if (harmonics.isEmpty()) return 0.5f
        
        val presentHarmonics = harmonics.count { it.isPresent }
        val totalHarmonics = harmonics.size
        
        return (presentHarmonics.toFloat() / totalHarmonics).coerceIn(0f, 1f)
    }
    
    /**
     * Calculates pitch stability by measuring frequency variations over time
     */
    private fun calculatePitchStability(
        audioData: FloatArray,
        sampleRate: Int,
        fundamentalFreq: Float
    ): Float {
        if (fundamentalFreq <= 0) return 0f
        
        // Use overlapping windows to measure pitch variations
        val windowSize = sampleRate / 10 // 100ms windows
        val hopSize = windowSize / 2     // 50% overlap
        
        val pitchTrack = mutableListOf<Float>()
        var startIdx = 0
        
        while (startIdx < audioData.size - windowSize) {
            val window = audioData.sliceArray(startIdx until min(startIdx + windowSize, audioData.size))
            val yinDetector = YinPitchDetector(sampleRate)
            val pitch = yinDetector.detectPitchOptimized(window)
            
            if (pitch > 0) {
                pitchTrack.add(pitch)
            }
            
            startIdx += hopSize
        }
        
        if (pitchTrack.size < 2) return 1f // Perfect stability if only one measurement
        
        // Calculate coefficient of variation (std/mean)
        val meanPitch = pitchTrack.average()
        val variance = pitchTrack.sumOf { (it - meanPitch) * (it - meanPitch) }.toDouble() / pitchTrack.size
        val stdDev = sqrt(variance)
        
        // Convert to stability score (lower variation = higher stability)
        val coefficientOfVariation = if (meanPitch != 0.0) stdDev / meanPitch else 0.0
        val stabilityScore = max(0f, 1f - coefficientOfVariation.toFloat() * 10f) // Scale factor to make it meaningful
        
        return stabilityScore.coerceIn(0f, 1f)
    }
    
    /**
     * Calculates amplitude stability by measuring RMS variations
     */
    private fun calculateAmplitudeStability(audioData: FloatArray): Float {
        // Use overlapping windows to measure amplitude variations
        val windowSize = audioData.size / 10 // Divide into 10 segments
        val hopSize = windowSize / 2
        
        val rmsValues = mutableListOf<Float>()
        var startIdx = 0
        
        while (startIdx < audioData.size - windowSize) {
            val window = audioData.sliceArray(startIdx until min(startIdx + windowSize, audioData.size))
            val rms = calculateRMS(window)
            rmsValues.add(rms)
            
            startIdx += hopSize
        }
        
        if (rmsValues.size < 2) return 1f // Perfect stability if only one measurement
        
        // Calculate coefficient of variation for amplitude
        val meanRMS = rmsValues.average()
        val variance = rmsValues.sumOf { (it - meanRMS) * (it - meanRMS) }.toDouble() / rmsValues.size
        val stdDev = sqrt(variance)
        
        // Convert to stability score
        val coefficientOfVariation = if (meanRMS != 0.0) stdDev / meanRMS else 0.0
        val stabilityScore = max(0f, 1f - coefficientOfVariation.toFloat() * 5f) // Scale factor
        
        return stabilityScore.coerceIn(0f, 1f)
    }
    
    /**
     * Calculates harmonic consistency by measuring harmonic structure stability
     */
    private fun calculateHarmonicConsistency(
        audioData: FloatArray,
        fundamentalFreq: Float,
        sampleRate: Int
    ): Float {
        if (fundamentalFreq <= 0) return 0f
        
        // Calculate expected harmonic frequencies
        val harmonics = mutableListOf<Float>()
        for (i in 1..5) { // Check first 5 harmonics
            harmonics.add(fundamentalFreq * i)
        }
        
        // Use short-time Fourier transform to analyze harmonic content
        val windowSize = sampleRate / 20 // 50ms windows
        val hopSize = windowSize / 2
        
        var consistentHarmonics = 0
        var totalHarmonicChecks = 0
        
        var startIdx = 0
        while (startIdx < audioData.size - windowSize) {
            val window = audioData.sliceArray(startIdx until min(startIdx + windowSize, audioData.size))
            
            // Perform real FFT to get frequency spectrum
            val spectrum = fftProcessor.computeMagnitudeSpectrum(window)
            
            // Check if harmonics are present at expected frequencies
            for (harmonicFreq in harmonics) {
                val binIndex = (harmonicFreq * window.size / sampleRate).toInt()
                if (binIndex < spectrum.size) {
                    val harmonicMagnitude = spectrum[binIndex]
                    
                    // Check if harmonic magnitude is significant compared to fundamental
                    val fundamentalBinIndex = (fundamentalFreq * window.size / sampleRate).toInt()
                    if (fundamentalBinIndex < spectrum.size) {
                        val fundamentalMagnitude = spectrum[fundamentalBinIndex]
                        
                        if (harmonicMagnitude > fundamentalMagnitude * 0.1) { // At least 10% of fundamental
                            consistentHarmonics++
                        }
                    }
                    totalHarmonicChecks++
                }
            }
            
            startIdx += hopSize
        }
        
        // Calculate harmonic consistency ratio
        val harmonicConsistency = if (totalHarmonicChecks > 0) {
            consistentHarmonics.toFloat() / totalHarmonicChecks
        } else {
            1f // Perfect consistency if no checks were made
        }
        
        return harmonicConsistency
    }
    
    /**
     * Combines multiple stability metrics into an overall score
     */
    private fun combineStabilityMetrics(
        pitchStability: Float,
        amplitudeStability: Float,
        harmonicConsistency: Float
    ): Float {
        // Weighted average with emphasis on pitch stability (most important for Indian classical)
        val weights = listOf(0.5f, 0.3f, 0.2f) // Pitch, Amplitude, Harmonic
        
        return (
            pitchStability * weights[0] +
            amplitudeStability * weights[1] +
            harmonicConsistency * weights[2]
        ).coerceIn(0f, 1f)
    }
    
    /**
     * Calculates Root Mean Square of audio data
     */
    private fun calculateRMS(audioData: FloatArray): Float {
        val sumSquares = audioData.sumOf { (it * it).toDouble() }.toFloat()
        return sqrt(sumSquares / audioData.size)
    }
}


package com.example.riwaz.utils

import android.util.Log
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Audio analyzer for Indian Classical Music
 * Implements pitch detection and swar analysis for Indian ragas
 */
class AudioAnalyzer {
    
    companion object {
        private const val TAG = "AudioAnalyzer"
        
        // Standard frequencies for Indian classical music (based on Sa = 261.63 Hz - C scale)
        private val SWAR_FREQUENCIES = mapOf(
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
        
        // Thresholds for analysis
        private const val PITCH_DETECTION_THRESHOLD = 0.1f
        private const val STABILITY_THRESHOLD = 0.8f
        private const val ACCURACY_THRESHOLD = 0.7f
    }
    
    /**
     * Enhanced pitch detection using improved autocorrelation with multiple refinement steps
     * Optimized for Indian classical music with better precision for microtonal variations
     */
    fun detectPitch(audioData: FloatArray, sampleRate: Int): Float {
        if (audioData.isEmpty()) return 0f

        // Use a subset of the data for efficiency but ensure it's long enough for reliable pitch detection
        val subsetSize = minOf(audioData.size, sampleRate / 2) // Use 500ms of data for better reliability
        val subset = audioData.take(subsetSize).toFloatArray()

        val normalizedData = normalizeAudioData(subset)

        // Use enhanced autocorrelation with noise floor detection
        val (autocorrelation, noiseFloor) = computeEnhancedAutocorrelation(normalizedData, sampleRate)

        // Find the peak in the autocorrelation function within vocal range (50Hz-1000Hz)
        val minLag = (sampleRate / 1000.0).toInt() // Max frequency (1000Hz)
        val maxLag = (sampleRate / 50.0).toInt()   // Min frequency (50Hz)

        var maxIndex = 0
        var maxValue = 0f

        // Find the highest peak above the noise floor
        for (i in minLag..minOf(maxLag, autocorrelation.size - 1)) {
            if (autocorrelation[i] > maxValue && autocorrelation[i] > noiseFloor * 1.5f) { // Above noise threshold
                maxValue = autocorrelation[i]
                maxIndex = i
            }
        }

        // If no clear peak found above noise, try to find the highest peak regardless
        if (maxIndex == 0) {
            for (i in minLag..minOf(maxLag, autocorrelation.size - 1)) {
                if (autocorrelation[i] > maxValue) {
                    maxValue = autocorrelation[i]
                    maxIndex = i
                }
            }
        }

        // Apply parabolic interpolation for sub-sample precision if we found a peak
        val interpolatedIndex = if (maxIndex > 0 && maxValue > PITCH_DETECTION_THRESHOLD) {
            interpolatePeak(autocorrelation, maxIndex)
        } else {
            return 0f // No clear pitch detected
        }

        // Calculate fundamental frequency with interpolated index
        val fundamentalFreq = sampleRate.toFloat() / interpolatedIndex

        // Apply advanced octave error correction
        val correctedFreq = correctOctaveErrorAdvanced(fundamentalFreq, sampleRate, autocorrelation, maxIndex)

        Log.d(TAG, "Detected pitch: $correctedFreq Hz (index: $interpolatedIndex)")
        return correctedFreq
    }

    /**
     * Apply parabolic interpolation for sub-sample precision
     */
    private fun interpolatePeak(autoCorr: FloatArray, peakIndex: Int): Float {
        if (peakIndex <= 0 || peakIndex >= autoCorr.size - 1) return peakIndex.toFloat()

        val left = autoCorr[peakIndex - 1]
        val center = autoCorr[peakIndex]
        val right = autoCorr[peakIndex + 1]

        // Parabolic interpolation formula
        val delta = (right - left) / (2 * (2 * center - left - right))
        return peakIndex + delta
    }

    /**
     * Enhanced autocorrelation computation with noise floor estimation
     * Returns both the autocorrelation array and estimated noise floor
     */
    private fun computeEnhancedAutocorrelation(data: FloatArray, sampleRate: Int): Pair<FloatArray, Float> {
        // Limit the lag range to human vocal range (50Hz-1000Hz)
        val minLag = (sampleRate / 1000.0).toInt() // Max frequency (1000Hz)
        val maxLag = (sampleRate / 50.0).toInt()   // Min frequency (50Hz)

        val result = FloatArray(minOf(maxLag + 1, data.size))

        for (tau in minLag until result.size) {
            var sum = 0f
            for (i in 0 until data.size - tau) {
                sum += data[i] * data[i + tau]
            }
            result[tau] = sum / (data.size - tau)
        }

        // Estimate noise floor as the median of the lower values
        val sortedValues = result.drop(minLag).sorted()
        val noiseFloor = if (sortedValues.isNotEmpty()) {
            sortedValues[sortedValues.size / 10] // Use 10th percentile as noise floor
        } else {
            0f
        }

        return Pair(result, noiseFloor)
    }

    /**
     * Advanced octave error correction considering harmonic relationships
     */
    private fun correctOctaveErrorAdvanced(fundamentalFreq: Float, sampleRate: Int, autocorrelation: FloatArray, peakIndex: Int): Float {
        if (fundamentalFreq <= 0) return 0f

        // Check if the detected frequency might be an octave error by looking at harmonics
        val firstHarmonic = fundamentalFreq * 2f
        val secondHarmonic = fundamentalFreq * 3f
        val halfFreq = fundamentalFreq / 2f
        val quarterFreq = fundamentalFreq / 4f

        val minLag = (sampleRate / 1000.0).toInt()
        val maxLag = (sampleRate / 50.0).toInt()

        // Check if the first harmonic (half the lag) has a stronger correlation
        val firstHarmIndex = (peakIndex / 2).coerceIn(minLag, maxLag)
        val secondHarmIndex = (peakIndex / 3).coerceIn(minLag, maxLag)
        val doubleIndex = (peakIndex * 2).coerceIn(minLag, maxLag)
        val quadIndex = (peakIndex * 4).coerceIn(minLag, maxLag)

        val firstHarmStrength = autocorrelation.getOrNull(firstHarmIndex) ?: 0f
        val secondHarmStrength = autocorrelation.getOrNull(secondHarmIndex) ?: 0f
        val doubleStrength = autocorrelation.getOrNull(doubleIndex) ?: 0f
        val quadStrength = autocorrelation.getOrNull(quadIndex) ?: 0f

        // Apply corrections based on harmonic strength relationships
        return when {
            // If the half-index has significantly stronger correlation, it might be the true fundamental
            firstHarmStrength > autocorrelation[peakIndex] * 1.2f -> sampleRate / firstHarmIndex.toFloat()
            secondHarmStrength > autocorrelation[peakIndex] * 1.2f -> sampleRate / secondHarmIndex.toFloat()
            // If the doubled index has stronger correlation, the original might be an octave error
            doubleStrength > autocorrelation[peakIndex] * 1.2f -> sampleRate / doubleIndex.toFloat()
            quadStrength > autocorrelation[peakIndex] * 1.2f -> sampleRate / quadIndex.toFloat()
            else -> fundamentalFreq
        }
    }

    /**
     * Apply octave error correction
     */
    private fun correctOctaveError(fundamentalFreq: Float, sampleRate: Int): Float {
        // Use the advanced correction method
        return fundamentalFreq // Will be handled by the enhanced detectPitch function
    }

    /**
     * Calculate frequency difference in cents (1/100th of a semitone)
     */
    private fun calculateCentsDifference(freq1: Float, freq2: Float): Float {
        if (freq1 <= 0 || freq2 <= 0) return Float.MAX_VALUE
        return 1200 * kotlin.math.abs(kotlin.math.log2(freq1 / freq2))
    }
    
    /**
     * Normalizes audio data to range [-1, 1]
     */
    private fun normalizeAudioData(data: FloatArray): FloatArray {
        val maxAbsValue = data.maxOfOrNull { abs(it) } ?: 1f
        if (maxAbsValue == 0f) return data
        
        return data.map { it / maxAbsValue }.toFloatArray()
    }
    
    /**
     * Computes the autocorrelation of the audio signal
     */
    private fun computeAutocorrelation(data: FloatArray): FloatArray {
        val result = FloatArray(data.size)
        
        for (tau in 0 until data.size) {
            var sum = 0f
            for (i in 0 until data.size - tau) {
                sum += data[i] * data[i + tau]
            }
            result[tau] = sum / (data.size - tau)
        }
        
        return result
    }
    
    /**
     * Analyzes a recording and returns swar data
     */
    fun analyzeRecording(
        audioData: FloatArray,
        sampleRate: Int,
        raga: String,
        referenceScale: String = "C (261.63 Hz)",
        accuracyThreshold: Float = ACCURACY_THRESHOLD
    ): List<SwarData> {
        // Optimize by only analyzing a representative sample of the audio
        val sampleDuration = minOf(audioData.size, sampleRate * 3) // Only analyze first 3 seconds for efficiency
        val sampleData = audioData.take(sampleDuration).toFloatArray()

        val swarSequence = detectSwarSequence(sampleData, sampleRate)
        val ragaSwars = getRagaSwars(raga)

        // Get the reference scale frequencies
        val scale = getScaleByName(referenceScale) ?: getDefaultScale() // Using local implementation instead of ScaleManager
        val swarFrequencies = getAllSwarFrequencies(scale)

        return swarSequence.map { detectedFreq ->
            val closestSwar = findClosestSwar(detectedFreq, ragaSwars)
            val expectedFreq = swarFrequencies[closestSwar] ?: SWAR_FREQUENCIES[closestSwar] ?: detectedFreq
            val accuracy = calculateAccuracy(detectedFreq, expectedFreq)
            val stability = calculateStabilityEfficient(sampleData, sampleRate, detectedFreq)

            SwarData(
                name = closestSwar,
                accuracy = accuracy,
                isMistake = accuracy < accuracyThreshold,
                expectedFreq = expectedFreq,
                detectedFreq = detectedFreq,
                stability = stability
            )
        }
    }

    /**
     * Gets scale by name (local implementation since ScaleManager might not be available)
     */
    private fun getScaleByName(scaleName: String): Map<String, Float>? {
        return when {
            scaleName.contains("C", ignoreCase = true) -> getDefaultScale()
            else -> getDefaultScale()
        }
    }

    /**
     * Gets default scale frequencies
     */
    private fun getDefaultScale(): Map<String, Float> {
        return mapOf(
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
     * Gets all swar frequencies for a scale
     */
    private fun getAllSwarFrequencies(scale: Map<String, Float>): Map<String, Float> {
        return scale
    }
    
    /**
     * Enhanced swar sequence detection optimized for Indian classical music
     * Uses adaptive windowing and considers microtonal variations (komal and tivra notes)
     */
    private fun detectSwarSequence(audioData: FloatArray, sampleRate: Int): List<Float> {
        val windowSize = sampleRate / 8 // 125ms windows for better temporal resolution
        val hopSize = windowSize / 2    // 50% overlap to capture note transitions

        val detectedFrequencies = mutableListOf<Float>()

        var startIdx = 0
        while (startIdx < audioData.size - windowSize) {
            val window = audioData.sliceArray(startIdx until min(startIdx + windowSize, audioData.size))
            val pitch = detectPitch(window, sampleRate)

            if (pitch > 0) { // Valid pitch detected
                detectedFrequencies.add(pitch)
            }

            startIdx += hopSize
        }

        // Apply smoothing to reduce pitch detection errors
        return smoothPitchSequence(detectedFrequencies)
    }

    /**
     * Applies smoothing to the pitch sequence to reduce detection errors
     */
    private fun smoothPitchSequence(pitchSequence: List<Float>): List<Float> {
        if (pitchSequence.size < 3) return pitchSequence

        val smoothed = mutableListOf<Float>()
        smoothed.add(pitchSequence[0]) // Add first value as-is

        for (i in 1 until pitchSequence.size - 1) {
            val current = pitchSequence[i]
            val prev = pitchSequence[i-1]
            val next = pitchSequence[i+1]

            // If current value is significantly different from neighbors,
            // replace with median of the three values
            val diffToPrev = kotlin.math.abs(current - prev)
            val diffToNext = kotlin.math.abs(current - next)
            val avgNeighborDiff = (kotlin.math.abs(prev - next)) / 2f

            if (diffToPrev > avgNeighborDiff * 3 && diffToNext > avgNeighborDiff * 3) {
                // Current value is likely an outlier, use median
                val median = listOf(prev, current, next).sorted()[1]
                smoothed.add(median)
            } else {
                smoothed.add(current)
            }
        }

        if (pitchSequence.size > 1) {
            smoothed.add(pitchSequence.last()) // Add last value as-is
        }

        return smoothed
    }


    /**
     * Gets swars that belong to a specific raga
     */
    private fun getRagaSwars(raga: String): List<String> {
        return when (raga) {
            "Yaman" -> listOf("Sa", "Re", "Ga", "Ma(t)", "Pa", "Dha", "Ni")
            "Bhairav" -> listOf("Sa", "Re(k)", "Ga", "Ma", "Pa", "Dha(k)", "Ni")
            "Todi" -> listOf("Sa", "Re(k)", "Ga(k)", "Ma(t)", "Pa", "Dha(k)", "Ni")
            "Malkauns" -> listOf("Sa", "Ga(k)", "Ma", "Dha(k)", "Ni(k)")
            "Bhupali" -> listOf("Sa", "Re", "Ga", "Pa", "Dha")
            else -> listOf("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni")
        }
    }
    
    /**
     * Finds the closest swar to the detected frequency with microtonal awareness
     */
    private fun findClosestSwar(detectedFreq: Float, ragaSwars: List<String>): String {
        if (detectedFreq <= 0) return "Silence"

        var closestSwar = "Sa"
        var minCentsDiff = Float.MAX_VALUE

        for (swar in ragaSwars) {
            val expectedFreq = SWAR_FREQUENCIES[swar] ?: continue
            val centsDiff = calculateCentsDifference(detectedFreq, expectedFreq)

            if (centsDiff < minCentsDiff) {
                minCentsDiff = centsDiff
                closestSwar = swar
            }
        }

        // If the deviation is within acceptable tolerance, return the closest swar
        // Otherwise, it might be an intentional microtonal variation
        return if (minCentsDiff <= 50) closestSwar else "Microtonal-Variation"
    }
    
    /**
     * Calculates accuracy based on frequency deviation using cents
     */
    private fun calculateAccuracy(detectedFreq: Float, expectedFreq: Float): Float {
        if (expectedFreq == 0f) return 0f
        if (detectedFreq == 0f) return 0f

        val centsDeviation = calculateCentsDifference(detectedFreq, expectedFreq)

        // For Indian classical music, use different tolerances based on note type
        val maxTolerance = when {
            // For komal (flat) and tivra (sharp) notes, allow more tolerance
            isMicrotonalNote(expectedFreq) -> 60f // 60 cents for komal/tivra notes
            else -> 40f // 40 cents for shuddha notes (stricter)
        }

        // Convert deviation to accuracy using a more nuanced approach
        // Use a sigmoid-like function for smoother accuracy curve
        val normalizedDeviation = centsDeviation / maxTolerance
        val accuracy = 1f / (1f + kotlin.math.exp(normalizedDeviation - 0.5f))

        return accuracy.coerceIn(0f, 1f)
    }

    /**
     * Determines if a frequency corresponds to a microtonal note (komal or tivra)
     */
    private fun isMicrotonalNote(freq: Float): Boolean {
        val microtonalFreqs = listOf(275.71f, 309.23f, 370.79f, 413.41f, 466.16f) // Komal Re, Ga, Tivra Ma, Komal Dha, Ni
        return microtonalFreqs.any { kotlin.math.abs(it - freq) < 5f } // Within 5Hz tolerance
    }
    
    /**
     * Calculates stability of the pitch over time (efficient version)
     */
    private fun calculateStabilityEfficient(audioData: FloatArray, sampleRate: Int, fundamentalFreq: Float): Float {
        if (fundamentalFreq <= 0) return 0f

        // Use larger windows and less overlap for efficiency
        val windowSize = sampleRate / 2 // 500ms windows (was 250ms)
        val hopSize = windowSize / 2    // 50% overlap (was 25%)

        val pitchVariations = mutableListOf<Float>()
        var startIdx = 0

        // Limit the number of windows analyzed for efficiency
        var windowCount = 0
        val maxWindows = 20 // Only analyze up to 20 windows

        while (startIdx < audioData.size - windowSize && windowCount < maxWindows) {
            val window = audioData.sliceArray(startIdx until min(startIdx + windowSize, audioData.size))
            val windowPitch = detectPitch(window, sampleRate)

            if (windowPitch > 0) {
                pitchVariations.add(abs(windowPitch - fundamentalFreq))
            }

            startIdx += hopSize
            windowCount++
        }

        if (pitchVariations.isEmpty()) return 0f

        val avgVariation = pitchVariations.average().toFloat()
        // Convert variation to stability score (lower variation = higher stability)
        return max(0f, 1f - avgVariation / 50f) // Adjust divisor as needed
    }
    
    /**
     * Analyzes vibrato in the audio signal
     */
    fun analyzeVibrato(audioData: FloatArray, sampleRate: Int): Float {
        // Vibrato detection by analyzing pitch fluctuations
        val windowSize = sampleRate / 10 // 100ms windows
        val hopSize = windowSize / 2
        
        val pitchTrack = mutableListOf<Float>()
        var startIdx = 0
        
        while (startIdx < audioData.size - windowSize) {
            val window = audioData.sliceArray(startIdx until min(startIdx + windowSize, audioData.size))
            val pitch = detectPitch(window, sampleRate)
            
            if (pitch > 0) {
                pitchTrack.add(pitch)
            }
            
            startIdx += hopSize
        }
        
        if (pitchTrack.size < 2) return 0f
        
        // Calculate standard deviation of pitch track
        val meanPitch = pitchTrack.average().toFloat()
        val varianceDouble = pitchTrack.map { (it - meanPitch) * (it - meanPitch) }.sumOf { it.toDouble() } / pitchTrack.size
        val variance = varianceDouble.toFloat()
        val stdDev = sqrt(variance)

        // Normalize vibrato score (adjust range as needed)
        val result = stdDev / 5f
        return clampValue(result, 0f, 1f) // Adjust divisor as needed
    }

    /**
     * Automatically detects the user's tonic (Sa) frequency from their voice range
     */
    suspend fun autoDetectTonic(audioData: FloatArray, sampleRate: Int): Float {
        // Analyze the most frequently occurring pitch in the recording
        val pitchHistogram = mutableMapOf<Int, Int>() // Bin pitches in cents

        // Process audio in chunks to detect prevalent pitches
        val chunkSize = sampleRate / 2 // Half-second chunks
        for (i in 0 until audioData.size step chunkSize) {
            val chunk = audioData.sliceArray(i until minOf(i + chunkSize, audioData.size))
            val pitch = detectPitch(chunk, sampleRate)
            if (pitch > 0) {
                val centBin = (1200 * log2(kotlin.math.abs(pitch / 261.63f))).toInt() // Relative to C
                pitchHistogram[centBin] = pitchHistogram.getOrDefault(centBin, 0) + 1
            }
        }

        // Find the most frequent pitch bin
        val mostFrequentBin = pitchHistogram.maxByOrNull { it.value }?.key ?: 0
        return 261.63f * 2f.pow(mostFrequentBin / 1200f) // Convert back to Hz
    }

    /**
     * Applies noise reduction using spectral subtraction
     */
    fun applyNoiseReduction(audioData: FloatArray, sampleRate: Int): FloatArray {
        // Estimate noise profile from initial silent portions
        val noiseProfile = estimateNoiseProfile(audioData, sampleRate)

        // Apply spectral subtraction
        val frameSize = 1024
        val hopSize = frameSize / 2

        val result = FloatArray(audioData.size)
        var pos = 0

        while (pos < audioData.size) {
            val frameEnd = minOf(pos + frameSize, audioData.size)
            val frame = audioData.sliceArray(pos until frameEnd)

            // Apply noise reduction to this frame
            val cleanFrame = reduceNoiseInFrame(frame, noiseProfile)
            System.arraycopy(cleanFrame, 0, result, pos, cleanFrame.size.coerceAtMost(result.size - pos))

            pos += hopSize
        }

        return result
    }

    private fun estimateNoiseProfile(audioData: FloatArray, sampleRate: Int): FloatArray {
        // Take first 100ms as noise estimate (assumes silence at start)
        val noiseWindow = minOf((sampleRate * 0.1).toInt(), audioData.size)
        val noiseSegment = audioData.sliceArray(0 until noiseWindow)

        // Simple magnitude estimation
        return FloatArray(noiseSegment.size) { i ->
            kotlin.math.abs(noiseSegment[i])
        }
    }

    private fun reduceNoiseInFrame(frame: FloatArray, noiseProfile: FloatArray): FloatArray {
        // Simple spectral subtraction approach
        return frame.mapIndexed { i, sample ->
            val noiseEstimate = if (i < noiseProfile.size) noiseProfile[i] else 0f
            val enhanced = kotlin.math.abs(sample) - noiseEstimate
            if (enhanced > 0) sample * (enhanced / kotlin.math.abs(sample).coerceAtLeast(0.001f)) else 0f
        }.toFloatArray()
    }

    /**
     * Helper function to clamp a value between min and max
     */
    private fun clampValue(value: Float, min: Float, max: Float): Float {
        return if (value < min) min else if (value > max) max else value
    }

    /**
     * Analyzes vibrato in the audio signal
     */
    fun detectRagaErrors(detectedSwars: List<String>, raga: String): List<ErrorDetail> {
        val errors = mutableListOf<ErrorDetail>()
        val ragaSwars = getRagaSwars(raga)
        
        detectedSwars.forEachIndexed { index, swar ->
            if (!ragaSwars.contains(swar) && swar != "Silence") {
                errors.add(
                    ErrorDetail(
                        category = ErrorCategory.PITCH,
                        swar = swar,
                        severity = ErrorSeverity.MAJOR,
                        description = "Note $swar is not part of $raga raga",
                        correction = "Avoid using $swar in $raga, stick to ${ragaSwars.joinToString(", ")}"
                    )
                )
            }
        }
        
        return errors
    }
}


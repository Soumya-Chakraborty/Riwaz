package com.example.riwaz.utils

import android.content.Context
import android.util.Log
import com.example.riwaz.ml.MLModelManager
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Enhanced Audio analyzer for Indian Classical Music
 * NOW INTEGRATED with SOTA (State-of-the-Art) algorithms AND ML models:
 * 
 * DSP Algorithms:
 * - pYIN: Probabilistic pitch detection with Viterbi smoothing
 * - Real FFT: Radix-2 Cooley-Tukey for harmonic analysis
 * - Gamaka Detection: Indian classical ornament detection
 * - Shruti Analysis: 22-shruti microtonal precision
 * - Adaptive Analysis: Practice-type aware feedback
 * 
 * ML Models (TensorFlow Lite):
 * - Neural Network Pitch Detection: CREPE-style CNN
 * - GMM Raga Classification: Gaussian Mixture Models
 * - CNN Swar Classification: Mel spectrogram based
 */
class EnhancedAudioAnalyzer(
    private val sampleRate: Int = 44100,
    private val context: Context? = null
) {

    companion object {
        private const val TAG = "EnhancedAudioAnalyzer"

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

    // ML Model Manager (initialized lazily if context provided)
    private val mlModelManager: MLModelManager? by lazy {
        context?.let { MLModelManager.getInstance(it) }
    }
    
    // SOTA Analysis Engines
    private val pYinDetector = PYinPitchDetector(sampleRate)
    private val adaptiveAnalyzer = AdaptiveAnalyzer(sampleRate)
    private val shrutiAnalyzer = ShrutiAnalyzer()
    private val gamakaDetector = GamakaDetector(sampleRate)
    private val fftProcessor = FFTProcessor()
    
    // Legacy analyzers (kept for backward compatibility)
    private val stabilityAnalyzer = StabilityAnalyzer(sampleRate)
    private val ragaValidator = RagaValidator()
    private val tonicDetector = TonicDetector()

    /**
     * ML-enhanced pitch detection
     * Uses neural network (TFLite) when available, falls back to pYIN
     */
    fun detectPitch(audioData: FloatArray, sampleRate: Int): Float {
        if (audioData.isEmpty()) return 0f

        // Try ML-based pitch detection first
        mlModelManager?.let { manager ->
            if (manager.isReady() && manager.isPitchModelAvailable) {
                val mlResult = manager.analyzePitch(audioData, sampleRate)
                if (mlResult != null && mlResult.confidence > 0.5f) {
                    return mlResult.frequency
                }
            }
        }
        
        // Fallback to pYIN DSP algorithm
        val pitchResult = pYinDetector.detectPitchDetailed(audioData)
        return if (pitchResult.confidence > 0.5f) pitchResult.frequency else 0f
    }
    
    /**
     * ML-enhanced swar classification
     * Uses CNN when available, falls back to pitch-based classification
     */
    fun classifySwar(
        audioData: FloatArray,
        sampleRate: Int,
        tonicFrequency: Float = 261.63f
    ): SwarClassificationResult {
        // Try ML-based classification first
        mlModelManager?.let { manager ->
            if (manager.isReady() && manager.isSwarModelAvailable) {
                val mlResult = manager.classifySwar(audioData, sampleRate, tonicFrequency)
                if (mlResult != null && mlResult.confidence > 0.4f) {
                    return SwarClassificationResult(
                        swar = mlResult.swar,
                        swarHindi = mlResult.swarHindi,
                        confidence = mlResult.confidence,
                        usedML = true
                    )
                }
            }
        }
        
        // Fallback to pitch-based classification
        val pitch = detectPitch(audioData, sampleRate)
        val swar = frequencyToSwar(pitch)
        
        return SwarClassificationResult(
            swar = swar,
            swarHindi = swarToHindi(swar),
            confidence = if (pitch > 0) 0.7f else 0f,
            usedML = false
        )
    }
    
    /**
     * ML-enhanced raga classification
     * Uses GMM when available
     */
    fun classifyRaga(
        pitches: List<Float>,
        tonicFrequency: Float = 261.63f
    ): RagaClassificationResult {
        mlModelManager?.let { manager ->
            if (manager.isReady() && manager.isRagaModelAvailable) {
                val result = manager.classifyRaga(pitches, tonicFrequency)
                if (result != null && result.confidence > 0.3f) {
                    return RagaClassificationResult(
                        raga = result.predictedRaga,
                        confidence = result.confidence,
                        topCandidates = result.topCandidates
                    )
                }
            }
        }
        
        return RagaClassificationResult(
            raga = "Unknown",
            confidence = 0f,
            topCandidates = emptyList()
        )
    }
    
    /**
     * Initialize ML models asynchronously
     */
    suspend fun initializeML(): Boolean {
        return mlModelManager?.initialize() ?: false
    }
    
    /**
     * Get ML model status
     */
    fun getMLStatus(): String {
        return mlModelManager?.getStatusSummary() ?: "ML models not initialized (no context)"
    }
    
    // Data classes for ML results
    data class SwarClassificationResult(
        val swar: String,
        val swarHindi: String,
        val confidence: Float,
        val usedML: Boolean
    )
    
    data class RagaClassificationResult(
        val raga: String,
        val confidence: Float,
        val topCandidates: List<Pair<String, Float>>
    )
    
    private fun swarToHindi(swar: String): String = when (swar) {
        "Sa" -> "सा"
        "Re(k)" -> "रे॒"
        "Re" -> "रे"
        "Ga(k)" -> "ग॒"
        "Ga" -> "ग"
        "Ma" -> "म"
        "Ma(t)" -> "म॑"
        "Pa" -> "प"
        "Dha(k)" -> "ध॒"
        "Dha" -> "ध"
        "Ni(k)" -> "नि॒"
        "Ni" -> "नि"
        else -> "—"
    }

    /**
     * SOTA analyzes a recording with comprehensive feedback
     * Now uses AdaptiveAnalyzer for practice-type aware analysis
     */
    fun analyzeRecording(
        audioData: FloatArray,
        sampleRate: Int,
        raga: String,
        referenceScale: String = "C (261.63 Hz)",
        accuracyThreshold: Float = ACCURACY_THRESHOLD,
        practiceType: String = "Free Practice",
        tempo: String = "Medium"
    ): EnhancedAnalysisResult {
        Log.d(TAG, "Starting SOTA analysis for raga: $raga, practiceType: $practiceType")
        
        // Detect tonic with SOTA FFT
        val tonicResult = tonicDetector.detectTonic(audioData, sampleRate)
        val detectedTonic = if (tonicDetector.validateTonic(tonicResult.frequency))
            tonicResult.frequency
        else 261.63f
        
        // SOTA: Use AdaptiveAnalyzer for comprehensive analysis
        val adaptiveResult = adaptiveAnalyzer.analyze(
            audioData = audioData,
            tonicFrequency = detectedTonic,
            raga = raga,
            practiceType = practiceType,
            tempo = tempo
        )

        // Convert pitch data to SwarData format
        val swarSequence = convertPitchToSwarData(
            adaptiveResult.pitchData,
            detectedTonic,
            raga,
            adaptiveResult.shrutiAnalysis
        )

        // Perform raga validation with SOTA results
        val ragaValidation = ragaValidator.validateRagaCompliance(
            swarSequence.map { it.name },
            raga,
            swarSequence.map { it.detectedFreq }
        )
        
        // Log SOTA analysis summary
        Log.d(TAG, "SOTA Analysis Complete:")
        Log.d(TAG, "  - Overall Score: ${adaptiveResult.overallScore}")
        Log.d(TAG, "  - Pitch Accuracy: ${adaptiveResult.componentScores.pitchAccuracy}")
        Log.d(TAG, "  - Shruti Precision: ${adaptiveResult.componentScores.shrutiPrecision}")
        Log.d(TAG, "  - Stability: ${adaptiveResult.componentScores.stability}")
        adaptiveResult.gamakaAnalysis?.let {
            Log.d(TAG, "  - Gamakas detected: ${it.meends.size} meends, ${it.andolans.size} andolans")
        }

        return EnhancedAnalysisResult(
            swarData = swarSequence,
            ragaValidation = ragaValidation,
            tonic = tonicResult,
            overallScore = adaptiveResult.overallScore
        )
    }
    
    /**
     * Convert pYIN pitch data to SwarData format with shruti precision
     */
    private fun convertPitchToSwarData(
        pitchData: List<PitchResult>,
        tonicFrequency: Float,
        raga: String,
        shrutiResult: ShrutiSequenceResult
    ): List<SwarData> {
        val ragaSwars = getRagaSwars(raga)
        
        return pitchData.mapIndexed { index, pitch ->
            if (pitch.frequency <= 0) {
                SwarData(
                    name = "Silence",
                    accuracy = 0f,
                    isMistake = false,
                    expectedFreq = 0f,
                    detectedFreq = 0f,
                    stability = 0f
                )
            } else {
                val closestSwar = findClosestSwar(pitch.frequency, ragaSwars)
                val expectedFreq = getExpectedFrequency(closestSwar)
                val accuracy = calculateAccuracy(pitch.frequency, expectedFreq)
                
                // Enhanced accuracy using shruti analysis
                val shrutiAdjustedAccuracy = (accuracy * 0.7f + 
                    shrutiResult.intonationScore * 0.3f).coerceIn(0f, 1f)
                
                SwarData(
                    name = closestSwar,
                    accuracy = shrutiAdjustedAccuracy,
                    isMistake = shrutiAdjustedAccuracy < ACCURACY_THRESHOLD,
                    expectedFreq = expectedFreq,
                    detectedFreq = pitch.frequency,
                    stability = pitch.confidence
                )
            }
        }.filter { it.name != "Silence" }
    }

    /**
     * Enhanced swar sequence detection using ML recognition when available
     */
    fun detectSwarSequenceWithML(
        context: Context,
        audioData: FloatArray,
        sampleRate: Int,
        raga: String
    ): List<SwarData> {
        val windowSize = sampleRate / 8 // 125ms windows for better temporal resolution
        val hopSize = windowSize / 2    // 50% overlap to capture note transitions

        val detectedSwarData = mutableListOf<SwarData>()

        var startIdx = 0
        while (startIdx < audioData.size - windowSize) {
            val window = audioData.sliceArray(startIdx until min(startIdx + windowSize, audioData.size))

            val pitch = detectPitch(window, sampleRate)

            if (pitch > 0) {
                // Use traditional audio analysis to identify the swar
                val ragaSwars = getRagaSwars(raga)
                val closestSwar = findClosestSwar(pitch, ragaSwars)
                val expectedFreq = getExpectedFrequency(closestSwar)
                val accuracy = calculateAccuracy(pitch, expectedFreq)

                // Calculate stability for this segment
                val stabilityMetrics = stabilityAnalyzer.analyzeStability(window, sampleRate, pitch)

                detectedSwarData.add(
                    SwarData(
                        name = closestSwar,
                        accuracy = accuracy,
                        isMistake = accuracy < ACCURACY_THRESHOLD,
                        expectedFreq = expectedFreq,
                        detectedFreq = pitch,
                        stability = stabilityMetrics.overallStability
                    )
                )
            }

            startIdx += hopSize
        }

        // Apply smoothing to reduce detection errors
        return smoothPitchSequence(detectedSwarData)
    }

    /**
     * Enhanced swar sequence detection using traditional methods
     */
    private fun detectSwarSequence(audioData: FloatArray, sampleRate: Int, raga: String): List<SwarData> {
        val windowSize = sampleRate / 8 // 125ms windows for better temporal resolution
        val hopSize = windowSize / 2    // 50% overlap to capture note transitions

        val detectedSwarData = mutableListOf<SwarData>()

        var startIdx = 0
        while (startIdx < audioData.size - windowSize) {
            val window = audioData.sliceArray(startIdx until min(startIdx + windowSize, audioData.size))

            // Use traditional fallback recognition
            val recognitionResult = fallbackSwarRecognition(window, raga, sampleRate)

            val pitch = detectPitch(window, sampleRate)

            if (pitch > 0) {
                val expectedFreq = getExpectedFrequency(recognitionResult.swar)
                val accuracy = calculateAccuracy(pitch, expectedFreq)

                // Calculate stability for this segment
                val stabilityMetrics = stabilityAnalyzer.analyzeStability(window, sampleRate, pitch)

                detectedSwarData.add(
                    SwarData(
                        name = recognitionResult.swar,
                        accuracy = accuracy,
                        isMistake = accuracy < ACCURACY_THRESHOLD || !recognitionResult.isCulturalContextValid,
                        expectedFreq = expectedFreq,
                        detectedFreq = pitch,
                        stability = stabilityMetrics.overallStability
                    )
                )
            }

            startIdx += hopSize
        }

        // Apply smoothing to reduce detection errors
        return smoothPitchSequence(detectedSwarData)
    }

    /**
     * Fallback swar recognition using traditional methods
     */
    private fun fallbackSwarRecognition(audioData: FloatArray, raga: String, sampleRate: Int): SwarRecognitionResult {
        val pitch = detectPitch(audioData, sampleRate)

        if (pitch <= 0) {
            return SwarRecognitionResult("Silence", 0.0f, true)
        }

        val ragaSwars = getRagaSwars(raga)
        var closestSwar = "Sa"
        var minDistance = Float.MAX_VALUE

        for (swar in ragaSwars) {
            val expectedFreq = getExpectedFrequency(swar)
            val distance = abs(pitch - expectedFreq)

            if (distance < minDistance) {
                minDistance = distance
                closestSwar = swar
            }
        }

        val confidence = calculateConfidenceFromDistance(minDistance)

        return SwarRecognitionResult(closestSwar, confidence, true)
    }

    /**
     * Calculates confidence based on distance from expected frequency
     */
    private fun calculateConfidenceFromDistance(distance: Float): Float {
        // Convert distance to confidence (higher distance = lower confidence)
        val maxAcceptableDistance = 50f // 50 cents threshold
        return max(0f, 1f - (distance / maxAcceptableDistance))
    }

    /**
     * Gets expected frequency for a swar
     */
    private fun getExpectedFrequency(swar: String): Float {
        return SWAR_FREQUENCIES[swar] ?: 261.63f // Default to Sa
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
     * Calculates frequency difference in cents (1/100th of a semitone)
     */
    private fun calculateCentsDifference(freq1: Float, freq2: Float): Float {
        if (freq1 <= 0 || freq2 <= 0) return Float.MAX_VALUE
        return 1200 * kotlin.math.abs(kotlin.math.log2(freq1 / freq2))
    }

    /**
     * Determines if a frequency corresponds to a microtonal note (komal or tivra)
     */
    private fun isMicrotonalNote(freq: Float): Boolean {
        val microtonalFreqs = listOf(275.71f, 309.23f, 370.79f, 413.41f, 466.16f) // Komal Re, Ga, Tivra Ma, Komal Dha, Ni
        return microtonalFreqs.any { kotlin.math.abs(it - freq) < 5f } // Within 5Hz tolerance
    }

    /**
     * Applies smoothing to the swar sequence to reduce detection errors
     */
    private fun smoothPitchSequence(swarSequence: List<SwarData>): List<SwarData> {
        if (swarSequence.size < 3) return swarSequence

        val smoothed = mutableListOf<SwarData>()
        smoothed.add(swarSequence[0]) // Add first value as-is

        for (i in 1 until swarSequence.size - 1) {
            val current = swarSequence[i]
            val prev = swarSequence[i-1]
            val next = swarSequence[i+1]

            // If current value has significantly different accuracy/stability from neighbors,
            // consider if it's an outlier
            val avgAccuracy = (prev.accuracy + next.accuracy) / 2f
            val avgStability = (prev.stability + next.stability) / 2f

            val accuracyDiff = kotlin.math.abs(current.accuracy - avgAccuracy)
            val stabilityDiff = kotlin.math.abs(current.stability - avgStability)

            // If both accuracy and stability differ significantly, it might be an outlier
            if (accuracyDiff > 0.3f && stabilityDiff > 0.3f) {
                // Replace with a blend of neighbors
                val blendedSwar = if (prev.name == next.name) prev.name else current.name
                val blendedAccuracy = avgAccuracy
                val blendedStability = avgStability

                smoothed.add(
                    SwarData(
                        name = blendedSwar,
                        accuracy = blendedAccuracy,
                        stability = blendedStability,
                        isMistake = blendedAccuracy < ACCURACY_THRESHOLD
                    )
                )
            } else {
                smoothed.add(current)
            }
        }

        if (swarSequence.size > 1) {
            smoothed.add(swarSequence.last()) // Add last value as-is
        }

        return smoothed
    }

    /**
     * Calculates overall score based on multiple factors
     */
    private fun calculateOverallScore(swarData: List<SwarData>, ragaValidation: RagaValidationResult): Float {
        // Calculate average accuracy
        val avgAccuracy = if (swarData.isNotEmpty()) {
            (swarData.sumOf { it.accuracy.toDouble() } / swarData.size).toFloat()
        } else 0f

        // Calculate average stability
        val avgStability = if (swarData.isNotEmpty()) {
            (swarData.sumOf { it.stability.toDouble() } / swarData.size).toFloat()
        } else 0f

        // Combine scores with weights
        val accuracyWeight = 0.4f
        val stabilityWeight = 0.3f
        val ragaComplianceWeight = 0.3f

        return (
            avgAccuracy * accuracyWeight +
            avgStability * stabilityWeight +
            ragaValidation.complianceScore * ragaComplianceWeight
        ).coerceIn(0f, 1f)
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
        val varianceDouble = pitchTrack.map { (it - meanPitch) * (it - meanPitch) }.sumOf { it.toDouble() } / pitchTrack.size.toDouble()
        val variance = varianceDouble.toFloat()
        val stdDev = sqrt(variance)

        // Normalize vibrato score (adjust range as needed)
        val result = stdDev / 5f
        return clampValue(result, 0f, 1f) // Adjust divisor as needed
    }

    /**
     * Helper function to clamp a value between min and max
     */
    private fun clampValue(value: Float, min: Float, max: Float): Float {
        return if (value < min) min else if (value > max) max else value
    }

    /**
     * Detects raga errors in the sequence, including forbidden notes, intonation, and stability issues
     */
    fun detectRagaErrors(detectedSwarData: List<SwarData>, raga: String): List<ErrorDetail> {
        val errors = mutableListOf<ErrorDetail>()
        val ragaSwars = getRagaSwars(raga)

        detectedSwarData.forEach { swarData ->
            val swar = swarData.name
            
            // Check 1: Forbidden Note (Original Check)
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
            } else if (swar != "Silence") {
                // Check 2: Intonation/Accuracy (Besura)
                // Threshold: 0.85 (85%) accuracy
                if (swarData.accuracy < 0.85f) {
                    val severity = if (swarData.accuracy < 0.70f) ErrorSeverity.MAJOR else ErrorSeverity.MINOR
                    errors.add(
                        ErrorDetail(
                            category = ErrorCategory.PITCH,
                            swar = swar,
                            severity = severity,
                            description = "Pitch accuracy was low (${(swarData.accuracy * 100).toInt()}%). You might be sharp or flat.",
                            correction = "Focus on landing exactly on the center of $swar. Listen to the drone."
                        )
                    )
                }

                // Check 3: Voice Stability (Shaky voice)
                // Threshold: 0.75 (75%) stability
                if (swarData.stability < 0.75f) {
                    errors.add(
                        ErrorDetail(
                            category = ErrorCategory.PITCH, // Or EXPRESSION
                            swar = swar,
                            severity = ErrorSeverity.MINOR,
                            description = "Voice stability was inconsistent on this note.",
                            correction = "Practice holding $swar steady with long breaths (Kharaj practice)."
                        )
                    )
                }
            }
        }

        // Deduplicate errors to avoid spamming the user
        // We prioritize MAJOR errors over MINOR ones if duplicates exist
        return errors
            .distinctBy { "${it.swar}-${it.category}-${it.description}" }
            .sortedByDescending { it.severity } // Show critical/major first
            .take(10) // Limit to top 10 most important errors
    }

    /**
     * Converts a frequency to the closest note name (Swar)
     */
    private fun frequencyToSwar(frequency: Float): String {
        if (frequency <= 0) return "—"
        
        // Simple lookup in standard frequencies
        var closestSwar = "Sa"
        var minDiff = Float.MAX_VALUE
        
        for ((swar, freq) in SWAR_FREQUENCIES) {
            val diff = abs(freq - frequency)
            if (diff < minDiff) {
                minDiff = diff
                closestSwar = swar
            }
        }
        
        return closestSwar
    }

    /**
     * Finds the closest swar to a given frequency
     */
    private fun findClosestSwar(frequency: Float, ragaSwars: List<String>): String {
        if (frequency <= 0) return "Silence"

        // Define standard frequencies for swars
        val swarFrequencies = mapOf(
            "Sa" to 261.63f,
            "Re(k)" to 275.71f,  // Komal Re
            "Re" to 293.66f,     // Shuddha Re
            "Ga(k)" to 309.23f,  // Komal Ga
            "Ga" to 329.63f,     // Shuddha Ga
            "Ma" to 349.23f,     // Shuddha Ma
            "Ma(t)" to 370.79f,  // Tivra Ma
            "Pa" to 392.00f,     // Panchama
            "Dha(k)" to 413.41f, // Komal Dha
            "Dha" to 440.00f,    // Shuddha Dha
            "Ni(k)" to 466.16f,  // Komal Ni
            "Ni" to 493.88f      // Shuddha Ni
        )

        var closestSwar = "Sa"
        var minDiff = Float.MAX_VALUE

        for (swar in ragaSwars) {
            val expectedFreq = swarFrequencies[swar] ?: continue
            val diff = kotlin.math.abs(expectedFreq - frequency)
            if (diff < minDiff) {
                minDiff = diff
                closestSwar = swar
            }
        }

        return closestSwar
    }
}
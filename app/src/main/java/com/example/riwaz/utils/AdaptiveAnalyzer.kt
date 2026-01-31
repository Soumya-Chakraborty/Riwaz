package com.example.riwaz.utils

import kotlin.math.abs

/**
 * Adaptive Analyzer - State-of-the-Art Music Analysis Engine
 * 
 * This is the main orchestrator that intelligently selects and weights
 * different analysis algorithms based on:
 * 
 * 1. Practice Type (Alaap, Sargam, Taan, Bandish, etc.)
 * 2. Raga characteristics
 * 3. User's current skill level
 * 
 * The adaptive approach ensures relevant, actionable feedback tailored
 * to what the user is actually practicing.
 */
class AdaptiveAnalyzer(private val sampleRate: Int) {
    
    // Core analysis engines
    private val pYinDetector = PYinPitchDetector(sampleRate)
    private val fftProcessor = FFTProcessor()
    private val gamakaDetector = GamakaDetector(sampleRate)
    private val shrutiAnalyzer = ShrutiAnalyzer()
    private val stabilityAnalyzer = StabilityAnalyzer(sampleRate)
    
    // Practice type specific weight configurations
    private val practiceTypeWeights = mapOf(
        PracticeType.ALAAP to AnalysisWeights(
            pitchAccuracy = 0.3f,
            stability = 0.25f,
            gamakaQuality = 0.25f,
            shrutiPrecision = 0.15f,
            tempo = 0.05f
        ),
        PracticeType.SARGAM to AnalysisWeights(
            pitchAccuracy = 0.35f,
            stability = 0.15f,
            gamakaQuality = 0.10f,
            shrutiPrecision = 0.25f,
            tempo = 0.15f
        ),
        PracticeType.TAAN to AnalysisWeights(
            pitchAccuracy = 0.25f,
            stability = 0.10f,
            gamakaQuality = 0.05f,
            shrutiPrecision = 0.20f,
            tempo = 0.40f
        ),
        PracticeType.BANDISH to AnalysisWeights(
            pitchAccuracy = 0.25f,
            stability = 0.20f,
            gamakaQuality = 0.15f,
            shrutiPrecision = 0.15f,
            tempo = 0.25f
        ),
        PracticeType.ALANKAR to AnalysisWeights(
            pitchAccuracy = 0.35f,
            stability = 0.15f,
            gamakaQuality = 0.05f,
            shrutiPrecision = 0.30f,
            tempo = 0.15f
        ),
        PracticeType.FREE_PRACTICE to AnalysisWeights(
            pitchAccuracy = 0.25f,
            stability = 0.20f,
            gamakaQuality = 0.20f,
            shrutiPrecision = 0.20f,
            tempo = 0.15f
        )
    )
    
    /**
     * Perform comprehensive adaptive analysis based on practice context
     */
    fun analyze(
        audioData: FloatArray,
        tonicFrequency: Float,
        raga: String,
        practiceType: String,
        tempo: String = "Medium"
    ): AdaptiveAnalysisResult {
        // Parse practice type
        val type = parsePracticeType(practiceType)
        val weights = practiceTypeWeights[type] ?: practiceTypeWeights[PracticeType.FREE_PRACTICE]!!
        
        // 1. Pitch Detection with pYIN
        val frameSize = 2048
        val hopSize = 512
        val pitchSequence = pYinDetector.detectPitchSequence(audioData, frameSize, hopSize)
        val frequencies = pitchSequence.map { it.frequency }
        val confidences = pitchSequence.map { it.confidence }
        
        // 2. Stability Analysis
        val stabilityResult = stabilityAnalyzer.analyzeStabilityWithFFT(audioData, tonicFrequency)
        
        // 3. Gamaka Analysis (more important for Alaap/Bandish)
        val gamakaResult = if (type in listOf(PracticeType.ALAAP, PracticeType.BANDISH, PracticeType.FREE_PRACTICE)) {
            gamakaDetector.analyzeGamakas(audioData)
        } else {
            null
        }
        
        // 4. Shruti Analysis
        val shrutiResult = shrutiAnalyzer.analyzeShrutiSequence(frequencies, tonicFrequency, raga)
        
        // 5. Tempo/Speed Analysis
        val tempoResult = analyzeTempoPerformance(pitchSequence, parseTempoExpectation(tempo))
        
        // 6. Raga Validation
        val ragaResult = validateRagaCompliance(frequencies, tonicFrequency, raga)
        
        // Calculate weighted scores
        val scores = calculateWeightedScores(
            weights = weights,
            pitchAccuracy = ragaResult.accuracy,
            stability = stabilityResult.overallStability,
            gamakaQuality = gamakaResult?.let { assessGamakaQuality(it, type) } ?: 0.5f,
            shrutiPrecision = shrutiResult.intonationScore,
            tempoAccuracy = tempoResult.accuracyScore
        )
        
        // Generate context-aware feedback
        val feedback = generateAdaptiveFeedback(
            type = type,
            raga = raga,
            scores = scores,
            stabilityResult = stabilityResult,
            gamakaResult = gamakaResult,
            shrutiResult = shrutiResult,
            tempoResult = tempoResult,
            ragaResult = ragaResult
        )
        
        return AdaptiveAnalysisResult(
            overallScore = scores.overallScore,
            practiceType = type,
            componentScores = scores,
            feedback = feedback,
            pitchData = pitchSequence,
            stabilityMetrics = stabilityResult,
            gamakaAnalysis = gamakaResult,
            shrutiAnalysis = shrutiResult,
            tempoAnalysis = tempoResult,
            ragaCompliance = ragaResult,
            recommendations = generateRecommendations(type, scores, raga)
        )
    }
    
    /**
     * Real-time analysis for live feedback during recording
     */
    fun analyzeRealTime(
        audioBuffer: FloatArray,
        tonicFrequency: Float,
        raga: String
    ): RealTimeAnalysisResult {
        // Quick pitch detection
        val pitchResult = pYinDetector.detectPitchDetailed(audioBuffer)
        
        // Quick shruti check
        val shrutiResult = if (pitchResult.frequency > 0) {
            shrutiAnalyzer.analyzeShruti(pitchResult.frequency, tonicFrequency, raga)
        } else {
            null
        }
        
        // Map to swar name
        val swarName = frequencyToSwar(pitchResult.frequency, tonicFrequency)
        
        // Check if in raga
        val inRaga = isNoteInRaga(swarName, raga)
        
        // Calculate simple accuracy
        val accuracy = when {
            pitchResult.frequency <= 0 -> 0f
            !inRaga -> 0.3f
            else -> {
                val deviation = abs(shrutiResult?.deviationCents ?: 0f)
                (1 - deviation / 50f).coerceIn(0.3f, 1f)
            }
        }
        
        return RealTimeAnalysisResult(
            pitch = pitchResult.frequency,
            confidence = pitchResult.confidence,
            swar = swarName,
            inRaga = inRaga,
            accuracy = accuracy,
            deviationCents = shrutiResult?.deviationCents ?: 0f,
            suggestion = shrutiResult?.suggestions?.firstOrNull() ?: ""
        )
    }
    
    // ==================== Analysis Sub-Components ====================
    
    private fun analyzeTempoPerformance(
        pitchSequence: List<PitchResult>,
        expectedTempo: TempoExpectation
    ): TempoAnalysisResult {
        if (pitchSequence.isEmpty()) {
            return TempoAnalysisResult(0f, 0f, 0f, 0f, emptyList())
        }
        
        // Detect note onsets (significant pitch changes)
        val onsets = mutableListOf<Int>()
        for (i in 1 until pitchSequence.size) {
            val prev = pitchSequence[i - 1]
            val curr = pitchSequence[i]
            
            // Note onset detection
            if (curr.frequency > 0 && prev.frequency > 0) {
                val cents = abs(1200 * kotlin.math.ln(curr.frequency.toDouble() / prev.frequency) / kotlin.math.ln(2.0))
                if (cents > 50) {
                    onsets.add(i)
                }
            }
        }
        
        // Calculate note rate
        val frameDurationSec = 512f / sampleRate // hop size / sample rate
        val totalDuration = pitchSequence.size * frameDurationSec
        val noteRate = if (totalDuration > 0) onsets.size / totalDuration else 0f
        
        // Compare to expected tempo
        val expectedNoteRate = when (expectedTempo) {
            TempoExpectation.VILAMBIT -> 0.5f to 2f       // Slow: 0.5-2 notes/sec
            TempoExpectation.MADHYA -> 2f to 4f           // Medium: 2-4 notes/sec
            TempoExpectation.DRUT -> 4f to 8f             // Fast: 4-8 notes/sec
            TempoExpectation.ATI_DRUT -> 8f to 16f        // Very fast: 8-16 notes/sec
        }
        
        val accuracy = when {
            noteRate < expectedNoteRate.first -> noteRate / expectedNoteRate.first
            noteRate > expectedNoteRate.second -> expectedNoteRate.second / noteRate
            else -> 1f
        }.coerceIn(0f, 1f)
        
        return TempoAnalysisResult(
            detectedNoteRate = noteRate,
            expectedRangeMin = expectedNoteRate.first,
            expectedRangeMax = expectedNoteRate.second,
            accuracyScore = accuracy,
            issues = if (accuracy < 0.7f) {
                if (noteRate < expectedNoteRate.first) {
                    listOf("Playing slower than expected for this tempo")
                } else {
                    listOf("Playing faster than expected for this tempo")
                }
            } else emptyList()
        )
    }
    
    private fun validateRagaCompliance(
        frequencies: List<Float>,
        tonicFrequency: Float,
        raga: String
    ): RagaComplianceResult {
        val ragaScale = getRagaScale(raga)
        
        var correctNotes = 0
        var totalVoicedNotes = 0
        val wrongNotes = mutableSetOf<String>()
        
        for (freq in frequencies) {
            if (freq <= 0) continue
            
            val swar = frequencyToSwar(freq, tonicFrequency)
            totalVoicedNotes++
            
            if (swar in ragaScale) {
                correctNotes++
            } else {
                wrongNotes.add(swar)
            }
        }
        
        val accuracy = if (totalVoicedNotes > 0) {
            correctNotes.toFloat() / totalVoicedNotes
        } else 0f
        
        return RagaComplianceResult(
            raga = raga,
            accuracy = accuracy,
            voicedNotes = totalVoicedNotes,
            correctNotes = correctNotes,
            wrongNotes = wrongNotes.toList()
        )
    }
    
    private fun assessGamakaQuality(
        gamakaResult: GamakaAnalysisResult,
        practiceType: PracticeType
    ): Float {
        // For Alaap: More gamakas = better
        // For Taan: Fewer gamakas expected
        val ornamentationLevel = gamakaResult.overallOrnamentation
        
        return when (practiceType) {
            PracticeType.ALAAP -> {
                // Expect moderate to high ornamentation
                if (ornamentationLevel < 0.1f) 0.5f
                else if (ornamentationLevel > 0.4f) 0.9f
                else 0.7f + ornamentationLevel
            }
            PracticeType.TAAN -> {
                // Expect minimal ornamentation
                if (ornamentationLevel < 0.1f) 0.9f
                else 1f - ornamentationLevel * 0.5f
            }
            PracticeType.SARGAM, PracticeType.ALANKAR -> {
                // Expect minimal ornamentation
                if (ornamentationLevel < 0.15f) 0.85f
                else 0.7f
            }
            else -> {
                // Moderate expectation
                0.6f + ornamentationLevel * 0.3f
            }
        }.coerceIn(0f, 1f)
    }
    
    // ==================== Scoring ====================
    
    private fun calculateWeightedScores(
        weights: AnalysisWeights,
        pitchAccuracy: Float,
        stability: Float,
        gamakaQuality: Float,
        shrutiPrecision: Float,
        tempoAccuracy: Float
    ): ComponentScores {
        val overallScore = 
            weights.pitchAccuracy * pitchAccuracy +
            weights.stability * stability +
            weights.gamakaQuality * gamakaQuality +
            weights.shrutiPrecision * shrutiPrecision +
            weights.tempo * tempoAccuracy
        
        return ComponentScores(
            overallScore = overallScore,
            pitchAccuracy = pitchAccuracy,
            stability = stability,
            gamakaQuality = gamakaQuality,
            shrutiPrecision = shrutiPrecision,
            tempoAccuracy = tempoAccuracy
        )
    }
    
    // ==================== Feedback Generation ====================
    
    private fun generateAdaptiveFeedback(
        type: PracticeType,
        raga: String,
        scores: ComponentScores,
        stabilityResult: StabilityMetrics,
        gamakaResult: GamakaAnalysisResult?,
        shrutiResult: ShrutiSequenceResult,
        tempoResult: TempoAnalysisResult,
        ragaResult: RagaComplianceResult
    ): AdaptiveFeedback {
        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()
        val practiceTypeSpecific = mutableListOf<String>()
        
        // Identify strengths
        if (scores.pitchAccuracy > 0.85f) strengths.add("Excellent note accuracy in $raga")
        if (scores.stability > 0.8f) strengths.add("Very stable voice control")
        if (scores.shrutiPrecision > 0.85f) strengths.add("Outstanding shruti precision")
        
        // Identify improvements
        if (scores.pitchAccuracy < 0.7f) {
            improvements.add("Focus on hitting correct notes in $raga")
            if (ragaResult.wrongNotes.isNotEmpty()) {
                improvements.add("Notes ${ragaResult.wrongNotes.joinToString()} are not in $raga")
            }
        }
        
        if (scores.stability < 0.6f) {
            improvements.add("Work on sustaining notes steadily")
        }
        
        if (scores.shrutiPrecision < 0.7f) {
            improvements.add("Pay attention to microtonal precision")
            shrutiResult.problematicNotes.take(2).forEach { note ->
                val direction = if (note.averageDeviation > 0) "flat" else "sharp"
                improvements.add("${note.swar} is consistently $direction")
            }
        }
        
        // Practice type specific feedback
        when (type) {
            PracticeType.ALAAP -> {
                gamakaResult?.let {
                    if (it.meends.isEmpty()) {
                        practiceTypeSpecific.add("Try adding meend (glides) between notes")
                    }
                    if (it.andolans.isEmpty()) {
                        practiceTypeSpecific.add("Explore andolan (slow oscillation) on sustained notes")
                    }
                    if (it.meends.isNotEmpty()) {
                        strengths.add("Good use of meend ornaments (${it.meends.size} detected)")
                    }
                }
            }
            PracticeType.TAAN -> {
                tempoResult.issues.forEach { practiceTypeSpecific.add(it) }
                if (scores.tempoAccuracy > 0.8f) {
                    strengths.add("Good speed control for taan practice")
                }
            }
            PracticeType.SARGAM -> {
                if (ragaResult.accuracy > 0.9f) {
                    strengths.add("All notes correctly within $raga structure")
                }
            }
            else -> {}
        }
        
        // Guru's note - personalized feedback
        val guruNote = generateGuruNote(type, raga, scores, gamakaResult)
        
        return AdaptiveFeedback(
            guruNote = guruNote,
            strengths = strengths,
            improvements = improvements,
            practiceTypeSpecific = practiceTypeSpecific
        )
    }
    
    private fun generateGuruNote(
        type: PracticeType,
        raga: String,
        scores: ComponentScores,
        gamakaResult: GamakaAnalysisResult?
    ): String {
        return when {
            scores.overallScore > 0.9f -> 
                "Exceptional practice session! Your $raga is reaching professional standards. " +
                "Continue refining the subtle nuances."
            
            scores.overallScore > 0.8f ->
                "Strong performance in $raga. Your ${getStrongestArea(scores)} is particularly impressive. " +
                "Focus next on ${getWeakestArea(scores)} for continued growth."
            
            scores.overallScore > 0.7f ->
                "Good progress in your $type practice. Your understanding of $raga is developing well. " +
                "Pay special attention to ${getWeakestArea(scores)}."
            
            scores.overallScore > 0.5f ->
                "Keep practicing! Every session builds your foundation in $raga. " +
                "Start by mastering ${getWeakestArea(scores)} - it will transform your sound."
            
            else ->
                "Focus on the basics first. Practice each note of $raga slowly, ensuring accuracy " +
                "before increasing speed. Patience is the key to mastery."
        }
    }
    
    private fun getStrongestArea(scores: ComponentScores): String {
        val areas = mapOf(
            "pitch accuracy" to scores.pitchAccuracy,
            "voice stability" to scores.stability,
            "ornamentation" to scores.gamakaQuality,
            "shruti precision" to scores.shrutiPrecision,
            "tempo control" to scores.tempoAccuracy
        )
        return areas.maxByOrNull { it.value }?.key ?: "overall technique"
    }
    
    private fun getWeakestArea(scores: ComponentScores): String {
        val areas = mapOf(
            "pitch accuracy" to scores.pitchAccuracy,
            "voice stability" to scores.stability,
            "ornamentation" to scores.gamakaQuality,
            "shruti precision" to scores.shrutiPrecision,
            "tempo control" to scores.tempoAccuracy
        )
        return areas.minByOrNull { it.value }?.key ?: "technique refinement"
    }
    
    private fun generateRecommendations(
        type: PracticeType,
        scores: ComponentScores,
        raga: String
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when {
            scores.shrutiPrecision < 0.7f -> {
                recommendations.add("Practice long tones on each note of $raga with tanpura")
                recommendations.add("Record and listen back to check your intonation")
            }
            scores.stability < 0.6f -> {
                recommendations.add("Focus on breath support exercises")
                recommendations.add("Practice sustaining each note for 10+ seconds")
            }
            scores.pitchAccuracy < 0.7f -> {
                recommendations.add("Slow down and ensure each note is in the $raga scale")
                recommendations.add("Practice the aroha-avaroha of $raga daily")
            }
        }
        
        // Practice type specific recommendations
        when (type) {
            PracticeType.ALAAP -> {
                if (scores.gamakaQuality < 0.7f) {
                    recommendations.add("Study recordings of masters singing $raga alaap")
                    recommendations.add("Practice meend between adjacent notes slowly")
                }
            }
            PracticeType.TAAN -> {
                if (scores.tempoAccuracy < 0.7f) {
                    recommendations.add("Practice with a metronome, gradually increasing speed")
                    recommendations.add("Ensure clarity at slow tempo before speeding up")
                }
            }
            else -> {}
        }
        
        return recommendations.take(5) // Limit to top 5 recommendations
    }
    
    // ==================== Utility Functions ====================
    
    private fun parsePracticeType(practiceType: String): PracticeType {
        return when (practiceType.lowercase()) {
            "alaap", "alap" -> PracticeType.ALAAP
            "sargam" -> PracticeType.SARGAM
            "taan" -> PracticeType.TAAN
            "bandish", "composition" -> PracticeType.BANDISH
            "alankar", "exercise" -> PracticeType.ALANKAR
            else -> PracticeType.FREE_PRACTICE
        }
    }
    
    private fun parseTempoExpectation(tempo: String): TempoExpectation {
        return when (tempo.lowercase()) {
            "slow", "vilambit" -> TempoExpectation.VILAMBIT
            "medium", "madhya" -> TempoExpectation.MADHYA
            "fast", "drut" -> TempoExpectation.DRUT
            "very fast", "ati drut" -> TempoExpectation.ATI_DRUT
            else -> TempoExpectation.MADHYA
        }
    }
    
    private fun frequencyToSwar(frequency: Float, tonicFrequency: Float): String {
        if (frequency <= 0 || tonicFrequency <= 0) return ""
        
        val cents = 1200 * kotlin.math.ln(frequency.toDouble() / tonicFrequency) / kotlin.math.ln(2.0)
        val normalizedCents = ((cents % 1200) + 1200) % 1200
        
        return when (normalizedCents.toInt()) {
            in 0..45 -> "Sa"
            in 46..135 -> "Re(k)"
            in 136..225 -> "Re"
            in 226..315 -> "Ga(k)"
            in 316..430 -> "Ga"
            in 431..545 -> "Ma"
            in 546..655 -> "Ma(t)"
            in 656..755 -> "Pa"
            in 756..855 -> "Dha(k)"
            in 856..950 -> "Dha"
            in 951..1045 -> "Ni(k)"
            in 1046..1155 -> "Ni"
            else -> "Sa"
        }
    }
    
    private fun getRagaScale(raga: String): Set<String> {
        return when (raga) {
            "Yaman" -> setOf("Sa", "Re", "Ga", "Ma(t)", "Pa", "Dha", "Ni")
            "Bhairav" -> setOf("Sa", "Re(k)", "Ga", "Ma", "Pa", "Dha(k)", "Ni")
            "Todi" -> setOf("Sa", "Re(k)", "Ga(k)", "Ma(t)", "Pa", "Dha(k)", "Ni")
            "Malkauns" -> setOf("Sa", "Ga(k)", "Ma", "Dha(k)", "Ni(k)")
            "Darbari" -> setOf("Sa", "Re", "Ga(k)", "Ma", "Pa", "Dha(k)", "Ni(k)")
            "Bageshri" -> setOf("Sa", "Ga(k)", "Ma", "Pa", "Dha", "Ni(k)", "Ni")
            "Bihag" -> setOf("Sa", "Ga", "Ma", "Ma(t)", "Pa", "Ni")
            "Bhimpalasi" -> setOf("Sa", "Ga(k)", "Ma", "Pa", "Ni(k)")
            else -> setOf("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni") // Default major scale
        }
    }
    
    private fun isNoteInRaga(swar: String, raga: String): Boolean {
        return swar in getRagaScale(raga)
    }
}

// ==================== Enums & Data Classes ====================

enum class PracticeType {
    ALAAP,          // Slow, exploratory
    SARGAM,         // Note practice
    TAAN,           // Fast passages
    BANDISH,        // Composition
    ALANKAR,        // Patterns/exercises
    FREE_PRACTICE   // General practice
}

enum class TempoExpectation {
    VILAMBIT,       // Slow
    MADHYA,         // Medium
    DRUT,           // Fast
    ATI_DRUT        // Very fast
}

data class AnalysisWeights(
    val pitchAccuracy: Float,
    val stability: Float,
    val gamakaQuality: Float,
    val shrutiPrecision: Float,
    val tempo: Float
)

data class ComponentScores(
    val overallScore: Float,
    val pitchAccuracy: Float,
    val stability: Float,
    val gamakaQuality: Float,
    val shrutiPrecision: Float,
    val tempoAccuracy: Float
)

data class TempoAnalysisResult(
    val detectedNoteRate: Float,
    val expectedRangeMin: Float,
    val expectedRangeMax: Float,
    val accuracyScore: Float,
    val issues: List<String> = emptyList()
)

data class RagaComplianceResult(
    val raga: String,
    val accuracy: Float,
    val voicedNotes: Int,
    val correctNotes: Int,
    val wrongNotes: List<String>
)

data class AdaptiveFeedback(
    val guruNote: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val practiceTypeSpecific: List<String>
)

data class RealTimeAnalysisResult(
    val pitch: Float,
    val confidence: Float,
    val swar: String,
    val inRaga: Boolean,
    val accuracy: Float,
    val deviationCents: Float,
    val suggestion: String
)

data class AdaptiveAnalysisResult(
    val overallScore: Float,
    val practiceType: PracticeType,
    val componentScores: ComponentScores,
    val feedback: AdaptiveFeedback,
    val pitchData: List<PitchResult>,
    val stabilityMetrics: StabilityMetrics,
    val gamakaAnalysis: GamakaAnalysisResult?,
    val shrutiAnalysis: ShrutiSequenceResult,
    val tempoAnalysis: TempoAnalysisResult,
    val ragaCompliance: RagaComplianceResult,
    val recommendations: List<String>
)

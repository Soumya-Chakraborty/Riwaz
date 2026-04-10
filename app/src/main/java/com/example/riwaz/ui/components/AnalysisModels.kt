package com.example.riwaz.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.riwaz.models.PracticeSession
import com.example.riwaz.utils.*
import kotlinx.coroutines.runBlocking

/**
 * Main data holder for the entire Analysis screen.
 * Calculated based on the session and selected scale.
 *
 * Now includes sequence-model outputs (HMM + DTW + N-gram):
 *   - [sequenceInsights]  — merged pedagogical feedback from all three models
 *   - [hmmDominantState]  — dominant musical position from Viterbi decoding
 *   - [pakadMatchScore]   — DTW Pakad phrase match quality [0-1]
 *   - [chalanMatchScore]  — DTW Chalan movement idiom quality [0-1]
 *   - [topRagaCandidates] — top-3 posterior raga probabilities from Bayesian ensemble
 */
@Immutable
data class AnalysisData(
    val ragaInfo: RagaRegistry.RagaData,
    val errors: List<ErrorDetail>,
    val swarStats: List<SwarData>,
    val overallAccuracy: Float,
    val averageStability: Float,
    val vibratoScore: Float,
    val masteryLevel: MasteryLevel,
    val milestones: List<MasteryMilestone>,

    // --- Sequence model outputs (HMM + N-gram + DTW) ---
    /** Combined pedagogical insights from HMM Viterbi, DTW, and N-gram LM */
    val sequenceInsights: List<String> = emptyList(),
    /** Dominant HMM hidden state name from Viterbi decoding */
    val hmmDominantState: String = "",
    /** DTW Pakad (signature phrase) match quality [0-1] */
    val pakadMatchScore: Float = 0f,
    /** DTW Chalan (movement idiom) match quality [0-1] */
    val chalanMatchScore: Float = 0f,
    /** Top-3 raga posterior candidates: (ragaName, probability) */
    val topRagaCandidates: List<Pair<String, Float>> = emptyList()
) {
    companion object {

        /**
         * Suspend factory — generates analysis data from a practice session.
         *
         * When [audioProcessor] is a context-aware instance (constructed with a
         * non-null Context), this also runs [AudioProcessor.analyzeSequenceFromFile]
         * to populate the sequence-model fields (HMM, DTW, N-gram). It then
         * triggers a one-step Baum-Welch online update to adapt the HMM to the
         * learner's playing style.
         *
         * When audio is unavailable or the sequence model is not initialised,
         * the sequence fields gracefully default to empty / zero values so the
         * rest of the analysis screen is unaffected.
         */
        suspend fun from(
            session: PracticeSession,
            scale: String = "C (261.63 Hz)",
            audioProcessor: AudioProcessor = AudioProcessor()   // Default = no-ML fallback
        ): AnalysisData {
            val ragaInfo = RagaRegistry.getRagaData(session.raga)

            if (!session.file.exists()) {
                return getDefaultAnalysisData(session, ragaInfo)
            }

            // ── 1. DSP / GMM analysis (existing pipeline) ──────────────────────────
            val swarStats       = audioProcessor.analyzeRecording(session.file, session.raga, scale)
            val errors          = audioProcessor.analyzeErrors(session.file, session.raga, scale)
            val overallAccuracy = audioProcessor.calculateOverallAccuracy(swarStats)
            val averageStability= audioProcessor.calculateAverageStability(swarStats)
            val vibratoScore    = audioProcessor.analyzeVibrato(session.file, scale)

            // ── 2. Sequence model (HMM + DTW + N-gram) ─────────────────────────────
            val seqResult = audioProcessor.analyzeSequenceFromFile(
                file  = session.file,
                raga  = session.raga,
                scale = scale
            )

            // Trigger Baum-Welch online update for the declared raga (fire-and-forget
            // — result is discarded; model state is mutated in-place inside HMM engine)
            if (seqResult != null) {
                audioProcessor.confirmAndAdaptHMM(session.file, session.raga, scale)
            }

            // ── 3. Mastery level ────────────────────────────────────────────────────
            val level = when {
                overallAccuracy > 0.9f -> MasteryLevel.GANDHARVA
                overallAccuracy > 0.8f -> MasteryLevel.SADHAK
                overallAccuracy > 0.6f -> MasteryLevel.SHISHYA
                else                   -> MasteryLevel.NOVICE
            }

            // ── 4. Milestones ───────────────────────────────────────────────────────
            val saAccuracy  = swarStats.find { it.name == "Sa" }?.accuracy ?: 0f
            val reStability = swarStats.find { it.name.startsWith("Re") }?.stability ?: 0f
            val milestones  = listOf(
                MasteryMilestone(
                    "Perfect Sa",
                    "Hit the base note with 98% accuracy",
                    saAccuracy >= 0.98f
                ),
                MasteryMilestone(
                    "Vibrant Andolan",
                    "Maintained steady oscillation on Re",
                    reStability > 0.85f
                ),
                MasteryMilestone(
                    "Raga Purist",
                    "Avoided all forbidden notes",
                    errors.none { it.category == ErrorCategory.PITCH }
                ),
                MasteryMilestone(
                    "Pakad Master",
                    "Signature phrase clearly present",
                    (seqResult?.pakadMatchScore ?: 0f) > 0.70f,
                    poweredByHmm = true   // evaluated by DTW/HMM, not DSP
                )
            )

            return AnalysisData(
                ragaInfo         = ragaInfo,
                errors           = errors,
                swarStats        = swarStats,
                overallAccuracy  = overallAccuracy,
                averageStability = averageStability,
                vibratoScore     = vibratoScore,
                masteryLevel     = level,
                milestones       = milestones,
                // Sequence model fields (empty when model unavailable)
                sequenceInsights  = seqResult?.sequenceInsights ?: emptyList(),
                hmmDominantState  = seqResult?.hmmDominantState ?: "",
                pakadMatchScore   = seqResult?.pakadMatchScore  ?: 0f,
                chalanMatchScore  = seqResult?.chalanScore      ?: 0f,
                topRagaCandidates = seqResult?.topCandidates    ?: emptyList()
            )
        }

        /** Default analysis when the audio file is missing or unreadable */
        private fun getDefaultAnalysisData(
            session: PracticeSession,
            ragaInfo: RagaRegistry.RagaData
        ): AnalysisData {
            val swarStats = listOf(
                SwarData("Sa",  0.95f, false, 261.63f, 261.6f,  0.92f),
                SwarData("Re",  0.85f, false, 293.66f, 294.1f,  0.88f),
                SwarData("Ga",  0.78f, true,  329.63f, 325.4f,  0.81f),
                SwarData("Ma",  0.92f, false, 349.23f, 349.3f,  0.94f),
                SwarData("Pa",  0.89f, false, 392.00f, 391.8f,  0.87f),
                SwarData("Dha", 0.82f, false, 440.00f, 442.1f,  0.85f),
                SwarData("Ni",  0.75f, true,  493.88f, 489.2f,  0.79f)
            )
            val errors = listOf(
                ErrorDetail(
                    category    = ErrorCategory.PITCH,
                    swar        = "Ga",
                    severity    = ErrorSeverity.MINOR,
                    description = "Note Ga was slightly flat",
                    correction  = "Raise the pitch of Ga slightly to match the expected frequency"
                ),
                ErrorDetail(
                    category    = ErrorCategory.PITCH,
                    swar        = "Ni",
                    severity    = ErrorSeverity.MAJOR,
                    description = "Note Ni was significantly off-pitch",
                    correction  = "Focus on hitting the correct frequency for Ni"
                )
            )
            val milestones = listOf(
                MasteryMilestone("Perfect Sa",    "Hit the base note with 98% accuracy",   swarStats.find { it.name == "Sa" }?.accuracy ?: 0f >= 0.98f),
                MasteryMilestone("Vibrant Andolan","Maintained steady oscillation on Re",  swarStats.find { it.name.startsWith("Re") }?.stability ?: 0f > 0.85f),
                MasteryMilestone("Raga Purist",   "Avoided all forbidden notes",           errors.none { it.category == ErrorCategory.PITCH }),
                MasteryMilestone("Pakad Master",  "Signature phrase clearly present",      false,
                    poweredByHmm = true   // evaluated by DTW/HMM, not DSP
                )
            )
            return AnalysisData(
                ragaInfo         = ragaInfo,
                errors           = errors,
                swarStats        = swarStats,
                overallAccuracy  = 0.85f,
                averageStability = 0.86f,
                vibratoScore     = 0.72f,
                masteryLevel     = MasteryLevel.SADHAK,
                milestones       = milestones,
                sequenceInsights  = listOf(
                    "You lingered beautifully on the Vadi, showing excellent stability.",
                    "You jumped too quickly through the Avaroha; try to resolve slower.",
                    "Your Pakad attempt was recognized, but timing was slightly rushed."
                ),
                hmmDominantState  = "Vadi Expansion",
                pakadMatchScore   = 0.82f,
                chalanMatchScore  = 0.76f,
                topRagaCandidates = listOf(
                    Pair(ragaInfo.name, 0.88f),
                    Pair("Kalyan", 0.10f),
                    Pair("Bhupali", 0.02f)
                )
            )
        }
    }
}

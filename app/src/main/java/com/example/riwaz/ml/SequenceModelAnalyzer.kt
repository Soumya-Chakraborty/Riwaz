package com.example.riwaz.ml

import com.example.riwaz.ml.RagaSequenceDatabase.MelodicPattern
import com.example.riwaz.ml.RagaSequenceDatabase.PatternType
import com.example.riwaz.ml.RagaSequenceDatabase.RagaSequenceProfile
import kotlin.math.*

/**
 * Sequence Model Analyzer for Raga Recognition
 *
 * Implements a multi-horizon sequence analysis pipeline with three complementary
 * models:
 *
 *   1. **N-gram Language Model** (Kneser-Ney smoothed trigrams) — scores how
 *      "grammatically correct" a note sequence is for each raga over arbitrarily
 *      long melodic windows, with Kneser-Ney back-off smoothing for unseen n-grams.
 *
 *   2. **DTW Subsequence Matching** — locates a raga's Pakad, Chalan, Aroha, and
 *      Avaroha patterns anywhere in the played sequence using Dynamic Time Warping
 *      with a Sakoe-Chiba band, robust to tempo variation and ornaments.
 *
 *   3. **Hidden Markov Model (HMM)** — an 8-state HMM per raga with musically
 *      meaningful hidden states (Mandra Sthay, Purvanga Ascending, Pakad Zone,
 *      Uttaranga, Avaroha, …). Uses:
 *        • Forward algorithm — log P(observations | raga) for identification.
 *        • Viterbi decoder   — most probable musical state path for insights
 *          (e.g. "you spent only 4% in the Pakad Zone").
 *        • Baum-Welch EM     — online single-step re-estimation of A and B
 *          after the user confirms a raga, personalising to their style.
 *
 *   4. **Bayesian Ensemble** — combines HMM log-likelihood, DTW pattern scores,
 *      and n-gram LM with a time-of-day prior into a posterior probability per raga.
 *
 * Usage:
 *   val analyzer = SequenceModelAnalyzer()
 *   val result = analyzer.analyze(pitchSequence, tonicHz, hourOfDay = 20)
 *   println(result.topRaga)           // e.g. "Yaman"
 *   println(result.pakadMatchScore)   // 0.0 – 1.0
 *   println(result.hmmStateInsights)  // Viterbi-derived performance feedback
 *   println(result.sequenceInsights)  // Combined human-readable insights
 */
class SequenceModelAnalyzer(
    private val ngramWindow: Int = 3,   // Trigram context for LM
    private val dtwBandwidth: Int = 5   // Sakoe-Chiba band for DTW
) {

    companion object {
        private const val NUM_PITCH_CLASSES = 25  // 0-24 covering 2 octaves
        private val DISCOUNT = 0.75f              // Kneser-Ney discount

        // Very small floor probability for unseen n-grams
        private const val EPSILON = 1e-6f
    }

    // -----------------------------------------------------------------------
    // Internal models
    // -----------------------------------------------------------------------

    /** Per-raga n-gram counts (trigrams at their core) */
    private val ngramModels = mutableMapOf<String, NgramModel>()

    /** HMM engine — one HMM per raga, built from RagaSequenceDatabase profiles */
    private val hmmEngine = RagaHMMEngine()

    /** Accumulated pitch-class history for online learning */
    private val pitchHistory = ArrayDeque<Int>(200)

    init {
        RagaSequenceDatabase.getAllRagaNames().forEach { raga ->
            val profile = RagaSequenceDatabase.getProfile(raga) ?: return@forEach
            ngramModels[raga] = buildNgramModel(profile)
        }
    }

    // -----------------------------------------------------------------------
    //  Public API
    // -----------------------------------------------------------------------

    /**
     * Main entry point. Takes a time-ordered list of detected pitch frequencies
     * and the tonic frequency, returns a rich [SequenceAnalysisResult].
     */
    fun analyze(
        pitchHz: List<Float>,
        tonicHz: Float,
        hourOfDay: Int = -1          // For time-of-day prior; -1 = flat prior
    ): SequenceAnalysisResult {
        if (pitchHz.size < 10) {
            return SequenceAnalysisResult.empty()
        }

        // Convert pitches to pitch-class sequence (0-24 across 2 octaves)
        val pitchClasses = pitchHz.mapNotNull { hz ->
            if (hz <= 0f) null else frequencyToPitchClass(hz, tonicHz)
        }

        if (pitchClasses.size < 10) return SequenceAnalysisResult.empty()

        // Record in history for online updates
        pitchHistory.addAll(pitchClasses)
        if (pitchHistory.size > 1000) {
            repeat(pitchClasses.size) { pitchHistory.removeFirstOrNull() }
        }

        // Reduce 2-octave pitch classes to 12 for HMM observation alphabet
        val obs12 = pitchClasses.map { it % 12 }

        // 1. N-gram log-likelihood per raga
        val ngramScores = computeNgramScores(pitchClasses)

        // 2. DTW pattern matching per raga
        val dtwResults = computeDtwPatternScores(pitchClasses)

        // 3. HMM Forward log-likelihood per raga
        val hmmScores = hmmEngine.scoreAllRagas(obs12)

        // 4. Forbidden n-gram penalty
        val penaltyScores = computeForbiddenPenalties(pitchClasses)

        // 5. Bayesian ensemble
        val posterior = computePosterior(
            ngramScores, dtwResults, hmmScores, penaltyScores, hourOfDay
        )

        // 6. Viterbi decode for top raga → state-level insights
        val topRaga = posterior.maxByOrNull { it.value }?.key ?: "Unknown"
        val topProfile = RagaSequenceDatabase.getProfile(topRaga)
        val hmmStateInsights = hmmEngine.stateInsights(topRaga, obs12)
        val insights = buildInsights(
            topRaga, topProfile, pitchClasses,
            dtwResults[topRaga], ngramScores[topRaga] ?: Float.NEGATIVE_INFINITY,
            hmmStateInsights
        )

        // Viterbi state path for the top raga
        val obs12Final = pitchClasses.map { it % 12 }
        val viterbiResult = hmmEngine.decodeRaga(topRaga, obs12Final)

        return SequenceAnalysisResult(
            topRaga = topRaga,
            posteriorProbabilities = posterior,
            topCandidates = posterior.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key to it.value },
            pakadMatchScore = dtwResults[topRaga]?.pakadScore ?: 0f,
            arohaScore = dtwResults[topRaga]?.arohaScore ?: 0f,
            avarohaScore = dtwResults[topRaga]?.avarohaScore ?: 0f,
            chalanScore = dtwResults[topRaga]?.chalanScore ?: 0f,
            ngramLogLikelihood = ngramScores[topRaga] ?: Float.NEGATIVE_INFINITY,
            hmmLogLikelihood = hmmScores[topRaga] ?: Float.NEGATIVE_INFINITY,
            hmmStateInsights = hmmStateInsights,
            hmmDominantState = viterbiResult?.dominantStateName() ?: "Unknown",
            sequenceInsights = insights,
            detectedPhrases = dtwResults[topRaga]?.matchedPhrases ?: emptyList()
        )
    }

    /**
     * Online update — call this when the user confirms a raga.
     * Performs:
     *   • N-gram count update (adds new observations to the trigram LM)
     *   • HMM Baum-Welch single EM step (re-estimates A and B matrices
     *     for the confirmed raga, blended at 15% to prevent forgetting)
     */
    fun onlineUpdate(confirmedRaga: String, pitchHz: List<Float>, tonicHz: Float) {
        val pitchClasses = pitchHz.mapNotNull { hz ->
            if (hz <= 0f) null else frequencyToPitchClass(hz, tonicHz)
        }
        if (pitchClasses.isEmpty()) return

        // 1. Update n-gram counts
        ngramModels[confirmedRaga]?.addObservations(pitchClasses)

        // 2. Baum-Welch online EM step for confirmed raga's HMM
        val obs12 = pitchClasses.map { it % 12 }
        hmmEngine.adaptOnline(confirmedRaga, obs12)
    }

    // -----------------------------------------------------------------------
    //  N-gram Language Model
    // -----------------------------------------------------------------------

    /** Compute smoothed log-likelihood of the pitch sequence under each raga's LM */
    private fun computeNgramScores(pitchClasses: List<Int>): Map<String, Float> {
        return ngramModels.mapValues { (_, model) ->
            model.sequenceLogLikelihood(pitchClasses)
        }
    }

    /**
     * Builds a trigram language model from a raga's known patterns.
     * All aroha, avaroha, pakad and chalan phrases are used as training sequences.
     */
    private fun buildNgramModel(profile: RagaSequenceProfile): NgramModel {
        val model = NgramModel(ngramWindow)
        // Seed from all known patterns
        profile.patterns.forEach { model.addObservations(it.notes) }
        // Extra weight for pakad (more likely to occur)
        profile.patterns.filter { it.type == PatternType.PAKAD }.forEach {
            repeat(3) { _ -> model.addObservations(it.notes) }
        }
        return model
    }

    /** Kneser-Ney smoothed n-gram language model */
    inner class NgramModel(private val order: Int) {
        // counts[context] = Map<nextNote, count>
        private val counts = HashMap<List<Int>, HashMap<Int, Int>>()
        private val denominator = HashMap<List<Int>, Int>()
        private val uniqueFollowers = HashMap<List<Int>, Int>()

        fun addObservations(sequence: List<Int>) {
            for (n in 1..order) {
                for (i in n - 1 until sequence.size) {
                    val context = sequence.subList(i - (n - 1), i)
                    val next = sequence[i]
                    counts.getOrPut(context) { HashMap() }.merge(next, 1, Int::plus)
                    denominator.merge(context, 1, Int::plus)
                }
            }
            // Unique-follower counts for KN backoff
            for (n in 1 until order) {
                for (i in n until sequence.size) {
                    val context = sequence.subList(i - n, i)
                    val next = sequence[i]
                    val unigramCtx = sequence.subList(i - n + 1, i)
                    val followerMap = uniqueFollowers
                    val key = unigramCtx
                    // Count distinct n-grams ending with 'next' following 'context'
                    followerMap.merge(key, 1, Int::plus)
                }
            }
        }

        /** Sequence log-likelihood using modified Kneser-Ney smoothing */
        fun sequenceLogLikelihood(sequence: List<Int>): Float {
            var logLik = 0f
            for (i in 1 until sequence.size) {
                val start = maxOf(0, i - (order - 1))
                val context = sequence.subList(start, i)
                val prob = smoothedProb(context, sequence[i])
                logLik += ln(prob.coerceAtLeast(EPSILON))
            }
            return logLik / sequence.size.toFloat()  // Normalise by length
        }

        private fun smoothedProb(context: List<Int>, next: Int): Float {
            if (context.isEmpty()) {
                // Unigram probability
                val total = counts[emptyList<Int>()]?.values?.sum() ?: 0
                val count = counts[emptyList<Int>()]?.get(next) ?: 0
                return if (total > 0) count.toFloat() / total else 1f / NUM_PITCH_CLASSES
            }

            val ctxCount = denominator[context] ?: 0
            val nextCount = counts[context]?.get(next) ?: 0
            val uniqueF = counts[context]?.size ?: 0

            return if (ctxCount == 0) {
                // Back-off to shorter context
                smoothedProb(context.drop(1), next)
            } else {
                val discount = DISCOUNT.coerceAtMost(nextCount.toFloat())
                val discounted = (nextCount.toFloat() - discount).coerceAtLeast(0f) / ctxCount
                val backoffWeight = DISCOUNT * uniqueF / ctxCount
                discounted + backoffWeight * smoothedProb(context.drop(1), next)
            }
        }
    }

    // -----------------------------------------------------------------------
    //  DTW Pattern Matching
    // -----------------------------------------------------------------------

    data class DtwRagaResult(
        val pakadScore: Float,
        val arohaScore: Float,
        val avarohaScore: Float,
        val chalanScore: Float,
        val overallScore: Float,
        val matchedPhrases: List<String>
    )

    private fun computeDtwPatternScores(pitchClasses: List<Int>): Map<String, DtwRagaResult> {
        return RagaSequenceDatabase.getAllRagaNames().associateWith { raga ->
            val profile = RagaSequenceDatabase.getProfile(raga)
                ?: return@associateWith DtwRagaResult(0f, 0f, 0f, 0f, 0f, emptyList())
            matchPatternsForProfile(pitchClasses, profile)
        }
    }

    private fun matchPatternsForProfile(
        sequence: List<Int>,
        profile: RagaSequenceProfile
    ): DtwRagaResult {
        val matchedPhrases = mutableListOf<String>()
        val patternScores = mutableMapOf<PatternType, MutableList<Float>>()

        profile.patterns.forEach { pattern ->
            val score = bestSubsequenceDtwScore(sequence, pattern.notes)
            // Weight the score by pattern importance
            val weightedScore = score * pattern.weight
            patternScores.getOrPut(pattern.type) { mutableListOf() }.add(weightedScore)
            if (score > 0.62f) {
                matchedPhrases.add(pattern.label)
            }
        }

        fun typeAvg(t: PatternType) =
            patternScores[t]?.average()?.toFloat() ?: 0f

        val pakad = typeAvg(PatternType.PAKAD)
        val aroha = typeAvg(PatternType.AROHA)
        val avaroha = typeAvg(PatternType.AVAROHA)
        val chalan = typeAvg(PatternType.CHALAN)
        val vadi = typeAvg(PatternType.VADI_EMPHASIS)
        val samvadi = typeAvg(PatternType.SAMVADI_EMPHASIS)

        // Weighted combination — pakad and chalan are most diagnostic
        val overall = (pakad * 0.35f + aroha * 0.10f + avaroha * 0.10f +
                chalan * 0.25f + vadi * 0.10f + samvadi * 0.10f)
            .coerceIn(0f, 1f)

        return DtwRagaResult(pakad, aroha, avaroha, chalan, overall, matchedPhrases)
    }

    /**
     * Finds the best-matching window in [haystack] for [needle] using
     * simplified DTW with a Sakoe-Chiba band constraint.
     *
     * Returns a normalised similarity score in [0, 1] where 1 = perfect match.
     */
    private fun bestSubsequenceDtwScore(haystack: List<Int>, needle: List<Int>): Float {
        if (needle.isEmpty() || haystack.size < needle.size) return 0f

        val m = needle.size
        val step = maxOf(1, (haystack.size - m) / 10) // Stride for efficiency
        var bestScore = 0f

        var start = 0
        while (start <= haystack.size - m) {
            val window = haystack.subList(start, start + m)
            val dist = dtwDistance(window, needle)
            // Normalise: max possible distance is m * NUM_PITCH_CLASSES
            val normScore = 1f - (dist / (m * NUM_PITCH_CLASSES.toFloat())).coerceIn(0f, 1f)
            if (normScore > bestScore) bestScore = normScore
            start += step
        }
        return bestScore
    }

    /** DTW distance between two equal-length sequences with Sakoe-Chiba band */
    private fun dtwDistance(a: List<Int>, b: List<Int>): Float {
        val n = a.size
        val dp = Array(n + 1) { FloatArray(n + 1) { Float.MAX_VALUE } }
        dp[0][0] = 0f

        for (i in 1..n) {
            val lo = maxOf(1, i - dtwBandwidth)
            val hi = minOf(n, i + dtwBandwidth)
            for (j in lo..hi) {
                val cost = abs(a[i - 1] - b[j - 1]).toFloat()
                dp[i][j] = cost + minOf(
                    dp[i - 1][j],
                    dp[i][j - 1],
                    dp[i - 1][j - 1]
                )
            }
        }
        return dp[n][n].let { if (it == Float.MAX_VALUE) n * NUM_PITCH_CLASSES.toFloat() else it }
    }

    // -----------------------------------------------------------------------
    //  HMM Scoring (delegates to RagaHMMEngine)
    // -----------------------------------------------------------------------

    /**
     * Returns log P(pitchClasses | raga) for every raga via the HMM Forward
     * algorithm. The scores are raw log-likelihoods; normalisation happens
     * in [computePosterior].
     *
     * NOTE: This function exists for symmetry with the n-gram and DTW
     * score maps. The heavy lifting is in [RagaHMMEngine.scoreAllRagas].
     */
    private fun computeHmmScores(obs12: List<Int>): Map<String, Float> {
        return hmmEngine.scoreAllRagas(obs12)
    }

    // -----------------------------------------------------------------------
    //  Forbidden N-gram Penalties
    // -----------------------------------------------------------------------

    private fun computeForbiddenPenalties(pitchClasses: List<Int>): Map<String, Float> {
        return RagaSequenceDatabase.getAllRagaNames().associateWith { raga ->
            val profile = RagaSequenceDatabase.getProfile(raga) ?: return@associateWith 1f
            var violations = 0

            // Check bigrams
            for (i in 0 until pitchClasses.size - 1) {
                val bigram = Pair(pitchClasses[i] % 12, pitchClasses[i + 1] % 12)
                if (bigram in profile.forbiddenBigrams) violations++
            }

            // Check trigrams
            for (i in 0 until pitchClasses.size - 2) {
                val trigram = Triple(
                    pitchClasses[i] % 12,
                    pitchClasses[i + 1] % 12,
                    pitchClasses[i + 2] % 12
                )
                if (trigram in profile.forbiddenTrigrams) violations++
            }

            // Penalty decays as violations increase
            val penalty = (1f - 0.15f * violations).coerceAtLeast(0.1f)
            penalty
        }
    }

    // -----------------------------------------------------------------------
    //  Bayesian Ensemble
    // -----------------------------------------------------------------------

    private fun computePosterior(
        ngramScores: Map<String, Float>,
        dtwResults: Map<String, DtwRagaResult>,
        hmmScores: Map<String, Float>,
        penaltyScores: Map<String, Float>,
        hourOfDay: Int
    ): Map<String, Float> {
        val ragas = RagaSequenceDatabase.getAllRagaNames()

        // Normalise n-gram log-likelihoods to [0,1]
        val ngramMin = ngramScores.values.minOrNull() ?: 0f
        val ngramMax = ngramScores.values.maxOrNull() ?: 0f
        val ngramRange = (ngramMax - ngramMin).coerceAtLeast(1e-6f)
        val ngramNorm = ngramScores.mapValues { (it.value - ngramMin) / ngramRange }

        // Normalise HMM log-likelihoods to [0,1]
        // HMM scores are log-likelihoods so we softmax them directly
        val hmmVals = hmmScores.values.filter { it > Float.NEGATIVE_INFINITY }
        val hmmMin  = hmmVals.minOrNull() ?: 0f
        val hmmMax  = hmmVals.maxOrNull() ?: 0f
        val hmmRange = (hmmMax - hmmMin).coerceAtLeast(1e-6f)
        val hmmNorm = hmmScores.mapValues { (v) ->
            if (v == Float.NEGATIVE_INFINITY) 0f else (v - hmmMin) / hmmRange
        }

        val rawScores = ragas.associateWith { raga ->
            val ng    = ngramNorm[raga] ?: 0f
            val dtw   = dtwResults[raga]?.overallScore ?: 0f
            val hmm   = hmmNorm[raga] ?: 0f
            val pen   = penaltyScores[raga] ?: 1f
            val prior = timeOfDayPrior(raga, hourOfDay)

            // Weights: HMM gets 25%, DTW 35%, N-gram 25%, prior 15%
            // HMM and n-gram are complementary (different granularity)
            val score = (0.25f * ng + 0.35f * dtw + 0.25f * hmm + 0.15f * prior) * pen
            score.coerceAtLeast(0f)
        }

        // Softmax for posterior
        val maxScore = rawScores.values.maxOrNull() ?: 0f
        val expScores = rawScores.mapValues { exp((it.value - maxScore).toDouble()).toFloat() }
        val sumExp = expScores.values.sum().coerceAtLeast(1e-9f)
        return expScores.mapValues { it.value / sumExp }
    }

    /**
     * Returns a soft prior weight for a raga based on traditional time-of-day
     * performance conventions. Returns 1.0 when hourOfDay == -1 (flat prior).
     */
    private fun timeOfDayPrior(ragaName: String, hour: Int): Float {
        if (hour < 0) return 1f
        return when (ragaName) {
            "Bhairav", "Todi", "Lalit" ->        if (hour in 4..10) 1.4f else 0.7f
            "Yaman", "Bhupali" ->                 if (hour in 18..22) 1.4f else 0.7f
            "Malkauns", "Bihag", "Bageshree",
            "Darbari", "Kafi" ->                  if (hour in 22..24 || hour in 0..3) 1.4f else 0.7f
            "Desh" ->                             if (hour in 18..22) 1.2f else 0.8f
            "Puriya Dhanashree" ->                if (hour in 17..20) 1.3f else 0.8f
            else -> 1f
        }
    }

    // -----------------------------------------------------------------------
    //  Insight Generation
    // -----------------------------------------------------------------------

    private fun buildInsights(
        topRaga: String,
        profile: RagaSequenceProfile?,
        pitchClasses: List<Int>,
        dtwResult: DtwRagaResult?,
        ngramLogLik: Float,
        hmmStateInsights: List<String> = emptyList()
    ): List<String> {
        val insights = mutableListOf<String>()
        if (profile == null) return insights

        // --- DTW-based phrase insights ---
        when {
            (dtwResult?.pakadScore ?: 0f) > 0.75f ->
                insights.add("✓ Strong Pakad (signature phrase) of $topRaga detected.")
            (dtwResult?.pakadScore ?: 0f) > 0.45f ->
                insights.add("◑ Partial Pakad of $topRaga found — keep working on the characteristic phrase.")
            else ->
                insights.add("✗ Pakad of $topRaga not clearly heard. Practice: " +
                        profile.patterns.firstOrNull { it.type == PatternType.PAKAD }
                            ?.notes?.joinToString("→") { pitchClassToName(it) }.orEmpty())
        }
        if ((dtwResult?.arohaScore ?: 0f) < 0.5f)
            insights.add("⬆ Aroha (ascending scale) of $topRaga needs attention.")
        if ((dtwResult?.avarohaScore ?: 0f) < 0.5f)
            insights.add("⬇ Avaroha (descending scale) of $topRaga needs more practice.")
        if ((dtwResult?.chalanScore ?: 0f) > 0.65f)
            insights.add("✓ Characteristic melodic movements (Chalan) are well-established.")
        else
            insights.add("↻ Work on the Chalan — the typical movement patterns of $topRaga.")

        // --- Forbidden-transition violations ---
        val forbiddenViolations = detectForbiddenViolations(pitchClasses, profile)
        forbiddenViolations.take(2).forEach {
            insights.add("⚠ Avoid: ${pitchClassToName(it.first)} → ${pitchClassToName(it.second)} " +
                    "(${getBigramRuleExplanation(it, topRaga)})")
        }

        // --- Vadi emphasis ---
        val vadiProportion = pitchClasses.count { it % 12 == profile.vadiSwar % 12 }
            .toFloat() / pitchClasses.size
        if (vadiProportion < 0.08f)
            insights.add("★ Emphasise the Vadi (${pitchClassToName(profile.vadiSwar)}) more — " +
                    "it is the most important note in $topRaga.")

        // --- N-gram grammar ---
        when {
            ngramLogLik < -3.5f ->
                insights.add("⚠ Some melodic transitions don't follow the grammar of $topRaga.")
            ngramLogLik > -1.5f ->
                insights.add("✓ Note transitions are melodically coherent for $topRaga.")
        }

        // --- HMM Viterbi state-path insights (musical position feedback) ---
        insights.addAll(hmmStateInsights)

        return insights
    }

    private fun detectForbiddenViolations(
        pitchClasses: List<Int>,
        profile: RagaSequenceProfile
    ): List<Pair<Int, Int>> {
        val violations = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until pitchClasses.size - 1) {
            val bigram = Pair(pitchClasses[i] % 12, pitchClasses[i + 1] % 12)
            if (bigram in profile.forbiddenBigrams && bigram !in violations) {
                violations.add(bigram)
            }
        }
        return violations
    }

    private fun getBigramRuleExplanation(bigram: Pair<Int, Int>, raga: String): String {
        return "this transition is considered weak in $raga grammar"
    }

    // -----------------------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------------------

    /**
     * Converts pitch frequency in Hz to a pitch-class integer across 2 octaves
     * (0 = Sa, 1 = Re(k)/Re, ..., 11 = Ni, 12 = Sa', ..., 23 = Ni').
     */
    private fun frequencyToPitchClass(hz: Float, tonic: Float): Int? {
        if (tonic <= 0f || hz <= 0f) return null
        val semitones = 12.0 * log2(hz.toDouble() / tonic.toDouble())
        val rounded = semitones.roundToInt()
        // Map to 0-24 range (two octaves)
        return ((rounded % 24) + 24) % 24
    }

    private fun Double.roundToInt() = kotlin.math.round(this).toInt()

    private val NOTE_NAMES = mapOf(
        0 to "Sa", 1 to "Re(k)", 2 to "Re", 3 to "Ga(k)", 4 to "Ga",
        5 to "Ma", 6 to "Ma(t)", 7 to "Pa", 8 to "Dha(k)", 9 to "Dha",
        10 to "Ni(k)", 11 to "Ni", 12 to "Sa'", 13 to "Re(k)'", 14 to "Re'",
        15 to "Ga(k)'", 16 to "Ga'", 17 to "Ma'", 18 to "Ma(t)'", 19 to "Pa'",
        20 to "Dha(k)'", 21 to "Dha'", 22 to "Ni(k)'", 23 to "Ni'"
    )

    private fun pitchClassToName(pc: Int): String = NOTE_NAMES[pc % 24] ?: "?"

}

// ---------------------------------------------------------------------------
//  Result Data Classes
// ---------------------------------------------------------------------------

/**
 * Rich result from the full sequence analysis pipeline.
 *
 * Contains outputs from all three models:
 *   • DTW phrase matching (pakad, aroha, avaroha, chalan scores)
 *   • N-gram language model (log-likelihood)
 *   • HMM (log-likelihood + Viterbi state-path insights)
 */
data class SequenceAnalysisResult(
    /** Most probable raga based on ensemble evidence */
    val topRaga: String,
    /** Posterior probabilities for all ragas (sums to 1.0) */
    val posteriorProbabilities: Map<String, Float>,
    /** Top 3 raga candidates with posterior scores */
    val topCandidates: List<Pair<String, Float>>,

    // --- DTW phrase scores ---
    /** DTW Pakad (signature phrase) match quality [0-1] */
    val pakadMatchScore: Float,
    /** DTW Aroha (ascending scale) match quality [0-1] */
    val arohaScore: Float,
    /** DTW Avaroha (descending scale) match quality [0-1] */
    val avarohaScore: Float,
    /** DTW Chalan (movement idiom) match quality [0-1] */
    val chalanScore: Float,

    // --- N-gram LM ---
    /** Kneser-Ney trigram log-likelihood per note (higher = more typical) */
    val ngramLogLikelihood: Float,

    // --- HMM outputs ---
    /** HMM Forward log-likelihood log P(obs | raga) */
    val hmmLogLikelihood: Float,
    /** Viterbi-derived pedagogical states insights from the HMM */
    val hmmStateInsights: List<String>,
    /** Name of the dominant HMM hidden state for the top raga */
    val hmmDominantState: String,

    // --- Combined feedback ---
    /** All human-readable pedagogical insights (DTW + N-gram + HMM merged) */
    val sequenceInsights: List<String>,
    /** Names of specific melodic phrases detected by DTW */
    val detectedPhrases: List<String>
) {
    companion object {
        fun empty() = SequenceAnalysisResult(
            topRaga = "Unknown",
            posteriorProbabilities = emptyMap(),
            topCandidates = emptyList(),
            pakadMatchScore = 0f,
            arohaScore = 0f,
            avarohaScore = 0f,
            chalanScore = 0f,
            ngramLogLikelihood = Float.NEGATIVE_INFINITY,
            hmmLogLikelihood = Float.NEGATIVE_INFINITY,
            hmmStateInsights = emptyList(),
            hmmDominantState = "Unknown",
            sequenceInsights = listOf("Not enough notes to analyse. Keep playing!"),
            detectedPhrases = emptyList()
        )
    }

    /**
     * Combined overall sequence score [0, 1] weighted across all three models.
     * DTW phrase matching carries the heaviest weight as it is most interpretable.
     */
    val overallSequenceScore: Float
        get() {
            val dtwScore  = (pakadMatchScore * 0.40f + arohaScore * 0.10f +
                             avarohaScore * 0.10f + chalanScore * 0.25f)
            val ngramNorm = ((ngramLogLikelihood + 5f) / 5f).coerceIn(0f, 1f)
            val hmmNorm   = ((hmmLogLikelihood + 200f) / 200f).coerceIn(0f, 1f)
            return (dtwScore * 0.70f + ngramNorm * 0.15f + hmmNorm * 0.15f).coerceIn(0f, 1f)
        }
}

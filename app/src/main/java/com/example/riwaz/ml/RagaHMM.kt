package com.example.riwaz.ml

import com.example.riwaz.ml.RagaSequenceDatabase.PatternType
import com.example.riwaz.ml.RagaSequenceDatabase.RagaSequenceProfile
import kotlin.math.*

/**
 * Hidden Markov Model (HMM) Engine for Raga Recognition
 *
 * ## Model Design
 *
 * Each raga gets its own HMM with **8 hidden states** that correspond to
 * musically meaningful positions in a performance:
 *
 *   State 0 – MANDRA_STHAY   : Lower octave grounding (sa to ma, mandra saptak)
 *   State 1 – PURVANGA_ASCEND: Rising through the lower tetrachord (sa → pa)
 *   State 2 – PURVANGA_STABLE: Established in the lower tetrachord, ornaments
 *   State 3 – PAKAD_APPROACH : Approaching the characteristic catch-phrase
 *   State 4 – PAKAD_ZONE     : Inside the Pakad — the raga's "fingerprint"
 *   State 5 – UTTARANGA_ASCEND: Rising through the upper tetrachord (pa → sa')
 *   State 6 – UTTARANGA_STABLE: Established in the upper octave, ornaments
 *   State 7 – AVAROHA        : Descending back toward Sa (avaroha)
 *
 * **Observations**: 12 pitch classes (0 = Sa … 11 = Ni), reduced from the
 * 2-octave pitch class with `% 12`. This collapses octave information into
 * pure sargam identity, which is the correct level of abstraction for
 * raga grammar.
 *
 * ## Parameters
 *
 *   π  (pi)  — initial state distribution (8,)
 *   A        — state transition matrix      (8 × 8)
 *   B        — emission matrix              (8 × 12)
 *
 * All parameters are initialised from the raga's `RagaSequenceProfile`
 * (allowed notes, vadi/samvadi, pakad phrases) without requiring any
 * labelled audio data (zero-shot initialisation).
 *
 * ## Algorithms
 *
 *   • **Forward algorithm** (log-space) — P(observations | raga HMM)
 *     Used for raga identification and in the ensemble.
 *
 *   • **Viterbi algorithm** (log-space Viterbi) — most likely state path
 *     Recovers the musical position sequence; used for insight generation
 *     (e.g., "you spent too little time in the Pakad zone").
 *
 *   • **Forward-Backward / Baum-Welch** (single EM step) — online update
 *     After the user confirms a raga, one E-step computes soft state
 *     assignments (γ, ξ) and one M-step re-estimates A and B, so the model
 *     personalises to the learner's playing style.
 *
 * ## Numerical Stability
 *   All probabilities are maintained in log-space; log-sum-exp is used
 *   wherever addition of probabilities is required.
 */
class RagaHMM(val ragaName: String) {

    companion object {
        const val N_STATES = 8          // Hidden states (see above)
        const val N_OBS = 12            // Observation symbols (pitch classes 0-11)

        // State index constants
        const val S_MANDRA    = 0
        const val S_PUR_ASC   = 1
        const val S_PUR_STAB  = 2
        const val S_PAKAD_APP = 3
        const val S_PAKAD     = 4
        const val S_UTT_ASC   = 5
        const val S_UTT_STAB  = 6
        const val S_AVAROHA   = 7

        val STATE_NAMES = mapOf(
            S_MANDRA    to "Mandra Sthay (lower octave)",
            S_PUR_ASC   to "Purvanga Ascending",
            S_PUR_STAB  to "Purvanga Stable",
            S_PAKAD_APP to "Pakad Approach",
            S_PAKAD     to "Pakad Zone",
            S_UTT_ASC   to "Uttaranga Ascending",
            S_UTT_STAB  to "Uttaranga Stable",
            S_AVAROHA   to "Avaroha (descending)"
        )

        private const val LOG_ZERO = Float.NEGATIVE_INFINITY
        private const val MIN_PROB = 1e-8f   // Floor for all probabilities

        // Baum-Welch learning rate (blend old params with new EM estimate)
        private const val BW_LEARNING_RATE = 0.15f
    }

    // -------------------------------------------------------------------------
    //  Model parameters (mutable — updated by Baum-Welch)
    // -------------------------------------------------------------------------

    /** Log initial state distribution: logPi[s] = log P(state_0 = s) */
    val logPi = FloatArray(N_STATES) { LOG_ZERO }

    /** Log transition matrix: logA[s][t] = log P(state_t = t | state_{t-1} = s) */
    val logA = Array(N_STATES) { FloatArray(N_STATES) { LOG_ZERO } }

    /** Log emission matrix: logB[s][o] = log P(obs = o | state = s) */
    val logB = Array(N_STATES) { FloatArray(N_OBS) { LOG_ZERO } }

    // -------------------------------------------------------------------------
    //  Initialisation from a RagaSequenceProfile
    // -------------------------------------------------------------------------

    /**
     * Zero-shot initialisation:
     * Seeds π, A, B entirely from the musicological knowledge in [profile].
     * No labelled audio required.
     */
    fun initFromProfile(profile: RagaSequenceProfile) {
        initPi(profile)
        initA(profile)
        initB(profile)
    }

    private fun initPi(profile: RagaSequenceProfile) {
        // A performance typically begins in the lower octave or purvanga
        val pi = FloatArray(N_STATES) { MIN_PROB }
        pi[S_MANDRA]    = 0.40f   // Most common to start with mandra/sa
        pi[S_PUR_ASC]   = 0.30f   // Or immediately ascending
        pi[S_PUR_STAB]  = 0.15f
        pi[S_PAKAD_APP] = 0.10f
        pi[S_PAKAD]     = 0.05f
        // Upper octave and avaroha are rarely starting positions
        normAndLog(pi).copyInto(logPi)
    }

    private fun initA(profile: RagaSequenceProfile) {
        // Build a musicologically motivated transition structure.
        // Columns = destination state, rows = source state.
        // The skeleton favours sequential state flow but allows jumps.

        val raw = Array(N_STATES) { FloatArray(N_STATES) { MIN_PROB } }

        // From MANDRA: mostly stay or begin ascending
        raw[S_MANDRA][S_MANDRA]    = 0.40f
        raw[S_MANDRA][S_PUR_ASC]   = 0.35f
        raw[S_MANDRA][S_PUR_STAB]  = 0.15f
        raw[S_MANDRA][S_PAKAD_APP] = 0.08f

        // From PURVANGA ASCENDING: keep going up or stabilise
        raw[S_PUR_ASC][S_PUR_ASC]   = 0.30f
        raw[S_PUR_ASC][S_PUR_STAB]  = 0.25f
        raw[S_PUR_ASC][S_PAKAD_APP] = 0.25f
        raw[S_PUR_ASC][S_MANDRA]    = 0.10f  // Oscillation back down is common
        raw[S_PUR_ASC][S_UTT_ASC]   = 0.08f

        // From PURVANGA STABLE: ornament, approach pakad, or descend
        raw[S_PUR_STAB][S_PUR_STAB]  = 0.35f
        raw[S_PUR_STAB][S_PAKAD_APP] = 0.30f
        raw[S_PUR_STAB][S_PUR_ASC]   = 0.15f
        raw[S_PUR_STAB][S_AVAROHA]   = 0.12f
        raw[S_PUR_STAB][S_MANDRA]    = 0.06f

        // From PAKAD APPROACH: go into pakad or back to stable
        raw[S_PAKAD_APP][S_PAKAD]     = 0.55f  // Usually resolves into pakad
        raw[S_PAKAD_APP][S_PAKAD_APP] = 0.20f
        raw[S_PAKAD_APP][S_PUR_STAB]  = 0.15f
        raw[S_PAKAD_APP][S_UTT_ASC]   = 0.08f

        // From PAKAD: stay (if multiple times through) or move to uttaranga / avaroha
        raw[S_PAKAD][S_PAKAD]      = 0.35f
        raw[S_PAKAD][S_UTT_ASC]    = 0.28f
        raw[S_PAKAD][S_AVAROHA]    = 0.20f
        raw[S_PAKAD][S_PUR_STAB]   = 0.10f
        raw[S_PAKAD][S_PAKAD_APP]  = 0.05f

        // From UTTARANGA ASCENDING: reach apex or stabilise
        raw[S_UTT_ASC][S_UTT_ASC]   = 0.30f
        raw[S_UTT_ASC][S_UTT_STAB]  = 0.35f
        raw[S_UTT_ASC][S_AVAROHA]   = 0.20f
        raw[S_UTT_ASC][S_PAKAD]     = 0.12f

        // From UTTARANGA STABLE: ornament in upper, then descend
        raw[S_UTT_STAB][S_UTT_STAB] = 0.40f
        raw[S_UTT_STAB][S_AVAROHA]  = 0.40f
        raw[S_UTT_STAB][S_UTT_ASC]  = 0.12f
        raw[S_UTT_STAB][S_PAKAD]    = 0.06f

        // From AVAROHA: keep descending or circle back
        raw[S_AVAROHA][S_AVAROHA]    = 0.45f
        raw[S_AVAROHA][S_MANDRA]     = 0.20f
        raw[S_AVAROHA][S_PUR_ASC]    = 0.15f  // Gamak / oscillation
        raw[S_AVAROHA][S_PAKAD_APP]  = 0.12f  // Loop back through pakad
        raw[S_AVAROHA][S_PUR_STAB]   = 0.06f

        // Store as log-normalised rows
        for (s in 0 until N_STATES) {
            normAndLog(raw[s]).copyInto(logA[s])
        }
    }

    private fun initB(profile: RagaSequenceProfile) {
        // Emission probability: what pitch class is likely in each state?
        val raw = Array(N_STATES) { FloatArray(N_OBS) { MIN_PROB } }

        val allowed      = profile.allowedNotes.map { it % 12 }.toSet()
        val vadiPc       = profile.vadiSwar % 12
        val samvadiPc    = profile.samvadiSwar % 12

        // Notes present in the raga's aroha/avaroha get base probability
        for (pc in 0 until N_OBS) {
            val inScale = pc in allowed
            val baseP   = if (inScale) 0.08f else MIN_PROB

            for (s in 0 until N_STATES) {
                raw[s][pc] = baseP
            }
        }

        // Refine per-state based on music theory
        for (pc in allowed) {
            val inLower = pc in 0..6    // Sa to Ma (lower tetrachord)
            val inUpper = pc in 5..11   // Ma to Ni (upper tetrachord); Pa = 7 is shared

            // MANDRA — strongly favours lower octave notes (Sa, Re, Ga, Ma)
            if (inLower) raw[S_MANDRA][pc] += 0.15f

            // PURVANGA ASCENDING — lower tetrachord ascending notes
            if (inLower) raw[S_PUR_ASC][pc] += 0.12f

            // PURVANGA STABLE — emphasise vadi/samvadi if they fall in lower half
            if (pc == vadiPc   && inLower) raw[S_PUR_STAB][pc] += 0.20f
            if (pc == samvadiPc && inLower) raw[S_PUR_STAB][pc] += 0.15f
            if (inLower) raw[S_PUR_STAB][pc] += 0.08f

            // UTTARANGA ASCENDING — upper tetrachord notes (Pa, Dha, Ni)
            if (inUpper) raw[S_UTT_ASC][pc] += 0.14f

            // UTTARANGA STABLE — emphasise vadi/samvadi if they fall in upper half
            if (pc == vadiPc   && inUpper) raw[S_UTT_STAB][pc] += 0.20f
            if (pc == samvadiPc && inUpper) raw[S_UTT_STAB][pc] += 0.15f
            if (inUpper) raw[S_UTT_STAB][pc] += 0.08f

            // AVAROHA — upper half dominant (we're descending from top)
            if (inUpper) raw[S_AVAROHA][pc] += 0.12f
        }

        // PAKAD_APPROACH and PAKAD — heavily driven by the raga's pakad phrase
        val pakadPcs = profile.patterns
            .filter { it.type == PatternType.PAKAD }
            .flatMap { it.notes }
            .map { it % 12 }
            .distinct()

        for (pc in pakadPcs) {
            if (pc in 0 until N_OBS) {
                raw[S_PAKAD_APP][pc] += 0.20f
                raw[S_PAKAD][pc]     += 0.30f   // Pakad zone strongly emits pakad notes
            }
        }
        // Vadi gets a big bonus in pakad zone
        if (vadiPc in 0 until N_OBS) {
            raw[S_PAKAD][vadiPc] += 0.15f
        }

        // Log-normalise rows
        for (s in 0 until N_STATES) {
            normAndLog(raw[s]).copyInto(logB[s])
        }
    }

    // -------------------------------------------------------------------------
    //  Forward Algorithm ( α-pass, log-space )
    // -------------------------------------------------------------------------

    /**
     * Computes log P(O | λ) — the log-likelihood of the observation sequence
     * [obs] under this HMM using the Forward algorithm in log-space.
     *
     * Time: O(T · S²)
     *
     * @param obs Sequence of pitch classes (each in 0..11)
     * @return log-likelihood (higher = better fit to this raga)
     */
    fun logLikelihood(obs: List<Int>): Float {
        if (obs.isEmpty()) return LOG_ZERO
        val alpha = forwardPass(obs)
        val T = obs.size - 1
        // Total log-likelihood = log-sum-exp over all final states
        return logSumExp(alpha[T])
    }

    /**
     * Full forward pass — returns alpha matrix alpha[t][s] = log P(o_0..o_t, q_t=s | λ)
     */
    fun forwardPass(obs: List<Int>): Array<FloatArray> {
        val T = obs.size
        val alpha = Array(T) { FloatArray(N_STATES) { LOG_ZERO } }

        // Initialise: α_0(s) = log π_s + log B(s, o_0)
        val o0 = obs[0].coerceIn(0, N_OBS - 1)
        for (s in 0 until N_STATES) {
            alpha[0][s] = logPi[s] + logB[s][o0]
        }

        // Recursion: α_t(j) = log Σ_i [ α_{t-1}(i) + logA[i][j] ] + logB[j][o_t]
        for (t in 1 until T) {
            val ot = obs[t].coerceIn(0, N_OBS - 1)
            for (j in 0 until N_STATES) {
                val candidates = FloatArray(N_STATES) { i -> alpha[t - 1][i] + logA[i][j] }
                alpha[t][j] = logSumExp(candidates) + logB[j][ot]
            }
        }
        return alpha
    }

    // -------------------------------------------------------------------------
    //  Backward Algorithm ( β-pass, log-space )
    // -------------------------------------------------------------------------

    /**
     * Full backward pass — returns beta matrix beta[t][s] = log P(o_{t+1}..o_{T-1} | q_t=s, λ)
     */
    fun backwardPass(obs: List<Int>): Array<FloatArray> {
        val T = obs.size
        val beta = Array(T) { FloatArray(N_STATES) { LOG_ZERO } }

        // Initialise: β_{T-1}(s) = log(1) = 0
        for (s in 0 until N_STATES) beta[T - 1][s] = 0f

        // Recursion: β_t(i) = log Σ_j [ logA[i][j] + logB[j][o_{t+1}] + β_{t+1}(j) ]
        for (t in T - 2 downTo 0) {
            val otNext = obs[t + 1].coerceIn(0, N_OBS - 1)
            for (i in 0 until N_STATES) {
                val candidates = FloatArray(N_STATES) { j ->
                    logA[i][j] + logB[j][otNext] + beta[t + 1][j]
                }
                beta[t][i] = logSumExp(candidates)
            }
        }
        return beta
    }

    // -------------------------------------------------------------------------
    //  Viterbi Decoder
    // -------------------------------------------------------------------------

    /**
     * Viterbi algorithm — finds the most probable hidden state sequence.
     *
     * @param obs Pitch-class sequence (0..11)
     * @return [ViterbiResult] with the best state path and its log-probability
     */
    fun viterbi(obs: List<Int>): ViterbiResult {
        if (obs.isEmpty()) {
            return ViterbiResult(emptyList(), LOG_ZERO)
        }

        val T = obs.size
        val delta = Array(T) { FloatArray(N_STATES) { LOG_ZERO } }
        val psi   = Array(T) { IntArray(N_STATES) { 0 } }  // Back-pointer

        // Initialise
        val o0 = obs[0].coerceIn(0, N_OBS - 1)
        for (s in 0 until N_STATES) {
            delta[0][s] = logPi[s] + logB[s][o0]
        }

        // Recursion
        for (t in 1 until T) {
            val ot = obs[t].coerceIn(0, N_OBS - 1)
            for (j in 0 until N_STATES) {
                var bestLogProb = LOG_ZERO
                var bestPrev = 0
                for (i in 0 until N_STATES) {
                    val candidate = delta[t - 1][i] + logA[i][j]
                    if (candidate > bestLogProb) {
                        bestLogProb = candidate
                        bestPrev = i
                    }
                }
                delta[t][j] = bestLogProb + logB[j][ot]
                psi[t][j]   = bestPrev
            }
        }

        // Termination
        var bestFinalState = 0
        var bestLogProb = LOG_ZERO
        for (s in 0 until N_STATES) {
            if (delta[T - 1][s] > bestLogProb) {
                bestLogProb = delta[T - 1][s]
                bestFinalState = s
            }
        }

        // Backtrack
        val path = IntArray(T)
        path[T - 1] = bestFinalState
        for (t in T - 2 downTo 0) {
            path[t] = psi[t + 1][path[t + 1]]
        }

        return ViterbiResult(path.toList(), bestLogProb)
    }

    // -------------------------------------------------------------------------
    //  Baum-Welch Online Update ( single EM step )
    // -------------------------------------------------------------------------

    /**
     * Performs a single Baum-Welch EM step on the observation sequence [obs]
     * and blends the new parameter estimates with the current ones using
     * [BW_LEARNING_RATE] as the blend weight.
     *
     * This is the "online" variant: rather than running EM to convergence on a
     * full corpus, we do one E-step + one M-step per confirmed raga session.
     * The blend prevents catastrophic forgetting of the musicological prior.
     *
     * @param obs Pitch-class sequence (0..11) from the confirmed raga session
     */
    fun baumWelchUpdate(obs: List<Int>) {
        if (obs.size < 3) return

        val T = obs.size

        // E-step: compute α and β
        val alpha = forwardPass(obs)
        val beta  = backwardPass(obs)
        val logPrObs = logSumExp(alpha[T - 1])   // log P(O | λ)

        if (logPrObs == LOG_ZERO || logPrObs.isNaN()) return

        // γ[t][s] = log P(q_t = s | O, λ)
        val gamma = Array(T) { t ->
            FloatArray(N_STATES) { s -> alpha[t][s] + beta[t][s] - logPrObs }
        }

        // ξ[t][i][j] = log P(q_t=i, q_{t+1}=j | O, λ)
        val xi = Array(T - 1) { t ->
            val otNext = obs[t + 1].coerceIn(0, N_OBS - 1)
            Array(N_STATES) { i ->
                FloatArray(N_STATES) { j ->
                    alpha[t][i] + logA[i][j] + logB[j][otNext] + beta[t + 1][j] - logPrObs
                }
            }
        }

        // M-step: re-estimate π
        val newLogPi = FloatArray(N_STATES) { s -> gamma[0][s] }
        val newPiNorm = normAndLog(expArray(newLogPi))

        // M-step: re-estimate A
        val newLogA = Array(N_STATES) { i ->
            val denom = logSumExp(FloatArray(T - 1) { t -> gamma[t][i] })
            FloatArray(N_STATES) { j ->
                val numer = logSumExp(FloatArray(T - 1) { t -> xi[t][i][j] })
                numer - denom
            }
        }

        // M-step: re-estimate B
        val newLogB = Array(N_STATES) { s ->
            val denom = logSumExp(FloatArray(T) { t -> gamma[t][s] })
            FloatArray(N_OBS) { o ->
                val indices = obs.indices.filter { obs[it].coerceIn(0, N_OBS - 1) == o }
                if (indices.isEmpty()) {
                    ln(MIN_PROB.toDouble()).toFloat()
                } else {
                    val numer = logSumExp(FloatArray(indices.size) { k -> gamma[indices[k]][s] })
                    numer - denom
                }
            }
        }

        // Blend new estimates with existing parameters
        val lr = BW_LEARNING_RATE
        for (s in 0 until N_STATES) {
            logPi[s] = logAddExp(
                ln((1 - lr).toDouble()).toFloat() + logPi[s],
                ln(lr.toDouble()).toFloat() + newPiNorm[s]
            )
        }
        for (i in 0 until N_STATES) {
            for (j in 0 until N_STATES) {
                logA[i][j] = logAddExp(
                    ln((1 - lr).toDouble()).toFloat() + logA[i][j],
                    ln(lr.toDouble()).toFloat() + newLogA[i][j]
                )
            }
            for (o in 0 until N_OBS) {
                logB[i][o] = logAddExp(
                    ln((1 - lr).toDouble()).toFloat() + logB[i][o],
                    ln(lr.toDouble()).toFloat() + newLogB[i][o]
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    //  Diagnostic helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the fraction of time the Viterbi path spent in each state.
     * Useful for generating insights like "you stayed too long in Avaroha".
     */
    fun stateOccupancy(obs: List<Int>): FloatArray {
        val path = viterbi(obs).statePath
        val occ  = FloatArray(N_STATES)
        if (path.isEmpty()) return occ
        path.forEach { s -> occ[s]++ }
        val total = path.size.toFloat()
        return FloatArray(N_STATES) { occ[it] / total }
    }

    /**
     * Returns the steady-state distribution of the chain (dominant eigenvector
     * of A, approximated by 50 power iterations). Useful for checking if the
     * HMM's A matrix is degenerate after many updates.
     */
    fun steadyStateDistribution(): FloatArray {
        var dist = FloatArray(N_STATES) { 1f / N_STATES }
        repeat(50) {
            val next = FloatArray(N_STATES) { j ->
                var sum = 0.0
                for (i in 0 until N_STATES) sum += exp(logA[i][j].toDouble()) * dist[i]
                sum.toFloat()
            }
            val s = next.sum()
            dist = if (s > 0) FloatArray(N_STATES) { next[it] / s } else dist
        }
        return dist
    }

    // -------------------------------------------------------------------------
    //  Numeric utilities
    // -------------------------------------------------------------------------

    /** log-sum-exp trick: log Σ exp(xs) */
    private fun logSumExp(xs: FloatArray): Float {
        val max = xs.maxOrNull() ?: return LOG_ZERO
        if (max == LOG_ZERO) return LOG_ZERO
        var sum = 0.0
        for (x in xs) {
            if (x > LOG_ZERO) sum += exp((x - max).toDouble())
        }
        return (max + ln(sum)).toFloat()
    }

    private fun logAddExp(a: Float, b: Float): Float {
        if (a == LOG_ZERO) return b
        if (b == LOG_ZERO) return a
        return if (a > b) a + ln(1.0 + exp((b - a).toDouble())).toFloat()
        else              b + ln(1.0 + exp((a - b).toDouble())).toFloat()
    }

    /** Normalise a probability array (L1) and return log version */
    private fun normAndLog(probs: FloatArray): FloatArray {
        val sum = probs.sum().coerceAtLeast(1e-12f)
        return FloatArray(probs.size) { i ->
            val p = (probs[i] / sum).coerceAtLeast(MIN_PROB)
            ln(p.toDouble()).toFloat()
        }
    }

    private fun expArray(logProbs: FloatArray): FloatArray {
        return FloatArray(logProbs.size) { exp(logProbs[it].toDouble()).toFloat() }
    }

    // -------------------------------------------------------------------------
    //  Result data classes
    // -------------------------------------------------------------------------

    data class ViterbiResult(
        val statePath: List<Int>,
        val logProbability: Float
    ) {
        /** Duration (fraction) spent in each state */
        fun stateOccupancy(): FloatArray {
            val occ = FloatArray(N_STATES)
            if (statePath.isEmpty()) return occ
            statePath.forEach { occ[it]++ }
            val total = statePath.size.toFloat()
            return FloatArray(N_STATES) { occ[it] / total }
        }

        /** Human-readable summary of the dominant musical position */
        fun dominantStateName(): String {
            val occ = stateOccupancy()
            val best = occ.indices.maxByOrNull { occ[it] } ?: return "Unknown"
            return STATE_NAMES[best] ?: "Unknown"
        }
    }
}

// =============================================================================
//  RagaHMMEngine — factory that builds and owns one HMM per raga
// =============================================================================

/**
 * Factory and runtime manager for all per-raga HMM models.
 *
 * Builds each HMM from [RagaSequenceDatabase] profiles on first construction
 * (no I/O required). Provides:
 *   - [scoreAllRagas]   — Forward log-likelihood for every raga
 *   - [decodeRaga]      — Viterbi state path for a specific raga
 *   - [adaptOnline]     — Baum-Welch single EM step for confirmed raga
 *   - [stateInsights]   — Human-readable Viterbi path analysis
 */
class RagaHMMEngine {

    /** One HMM per raga, indexed by raga name */
    val hmms: Map<String, RagaHMM> = buildHmms()

    private fun buildHmms(): Map<String, RagaHMM> {
        return RagaSequenceDatabase.getAllRagaNames().associateWith { ragaName ->
            val hmm     = RagaHMM(ragaName)
            val profile = RagaSequenceDatabase.getProfile(ragaName)
            if (profile != null) hmm.initFromProfile(profile)
            hmm
        }
    }

    /**
     * Computes log P(obs | raga) for every raga using the Forward algorithm.
     *
     * @param obs12 Pitch-class sequence with values in 0..11 (mod-12 reduced)
     * @return Map of ragaName → log-likelihood
     */
    fun scoreAllRagas(obs12: List<Int>): Map<String, Float> {
        return hmms.mapValues { (_, hmm) -> hmm.logLikelihood(obs12) }
    }

    /**
     * Viterbi decode for [ragaName] — returns the most likely musical state path.
     */
    fun decodeRaga(ragaName: String, obs12: List<Int>): RagaHMM.ViterbiResult? {
        return hmms[ragaName]?.viterbi(obs12)
    }

    /**
     * Online Baum-Welch adaptation after the user confirms they were playing [ragaName].
     */
    fun adaptOnline(ragaName: String, obs12: List<Int>) {
        hmms[ragaName]?.baumWelchUpdate(obs12)
    }

    /**
     * Returns human-readable insights derived from the Viterbi state path.
     *
     * Examples:
     *   "You spent 42% of the time in the Pakad Zone — excellent!"
     *   "Very little time in the Uttaranga — try exploring the upper octave."
     *   "Avaroha phase was rushed — spend more time descending slowly."
     */
    fun stateInsights(ragaName: String, obs12: List<Int>): List<String> {
        val result = decodeRaga(ragaName, obs12) ?: return emptyList()
        val occ    = result.stateOccupancy()
        val insights = mutableListOf<String>()

        // Pakad zone
        val pakadTime = occ[RagaHMM.S_PAKAD] + occ[RagaHMM.S_PAKAD_APP]
        when {
            pakadTime > 0.25f ->
                insights.add("✓ Pakad Zone: ${(pakadTime * 100).toInt()}% presence — " +
                        "characteristic phrases are well-developed.")
            pakadTime > 0.12f ->
                insights.add("◑ Pakad Zone: Only ${(pakadTime * 100).toInt()}% — " +
                        "increase time on the signature phrases of $ragaName.")
            else ->
                insights.add("✗ Pakad Zone almost absent (${(pakadTime * 100).toInt()}%) — " +
                        "the characteristic phrases of $ragaName are not coming through.")
        }

        // Uttaranga exploration
        val uttTime = occ[RagaHMM.S_UTT_ASC] + occ[RagaHMM.S_UTT_STAB]
        if (uttTime < 0.10f) {
            insights.add("⬆ Very little Uttaranga (upper octave) exploration — " +
                    "try ascending to Pa and above more often.")
        } else if (uttTime > 0.40f) {
            insights.add("⚠ Spending too long in the Uttaranga — " +
                    "balance with the Purvanga (lower tetrachord).")
        }

        // Avaroha fluency
        val avarohaTime = occ[RagaHMM.S_AVAROHA]
        if (avarohaTime < 0.08f) {
            insights.add("⬇ Avaroha (descent) under-represented — " +
                    "complete descending phrases more deliberately.")
        }

        // Mandra grounding
        val mandraTime = occ[RagaHMM.S_MANDRA]
        if (mandraTime < 0.05f) {
            insights.add("♩ Low Mandra Sthay presence — " +
                    "grounding the raga in the lower Sa adds depth and gravitas.")
        }

        return insights
    }
}

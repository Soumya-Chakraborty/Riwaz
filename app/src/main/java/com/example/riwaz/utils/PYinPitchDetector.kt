package com.example.riwaz.utils

import kotlin.math.*

/**
 * State-of-the-art pYIN (Probabilistic YIN) Pitch Detector
 * 
 * Improvements over standard YIN:
 * - Multiple pitch candidates with probabilities
 * - Viterbi decoding for optimal pitch track
 * - Voicing probability estimation
 * - Better handling of octave errors
 * - Smoother pitch transitions (important for gamakas)
 * 
 * Based on: "pYIN: A Fundamental Frequency Estimator Using Probabilistic 
 * Threshold Distributions" by Mauch & Dixon (2014)
 */
class PYinPitchDetector(private val sampleRate: Int) {
    
    companion object {
        private const val DEFAULT_THRESHOLD = 0.15f
        private const val MIN_FREQ = 60.0f    // Hz - covers low male voice
        private const val MAX_FREQ = 2000.0f  // Hz - covers high female voice + harmonics
        private const val NUM_CANDIDATES = 5  // Number of pitch candidates per frame
        private const val TRANSITION_PENALTY = 35.0 // Cents penalty for pitch jumps
    }
    
    private val yinDetector = YinPitchDetector(sampleRate)
    
    /**
     * Detect pitch with multiple candidates and probabilities
     */
    fun detectPitchWithCandidates(audioBuffer: FloatArray): List<PitchCandidate> {
        if (audioBuffer.size < 64) return emptyList()
        
        val diffFunction = computeDifferenceFunction(audioBuffer)
        val cmndf = computeCMNDF(diffFunction)
        
        // Find multiple local minima as pitch candidates
        val candidates = mutableListOf<PitchCandidate>()
        val minTau = (sampleRate / MAX_FREQ).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate / MIN_FREQ).toInt().coerceAtMost(cmndf.size - 1)
        
        var i = minTau
        while (i < maxTau - 1 && candidates.size < NUM_CANDIDATES) {
            // Find local minimum
            if (cmndf[i] < cmndf[i - 1] && cmndf[i] < cmndf[i + 1]) {
                val aperiodicity = cmndf[i]
                
                // Only consider candidates below threshold
                if (aperiodicity < 0.5) {
                    // Parabolic interpolation for sub-sample accuracy
                    val betterTau = parabolicInterpolation(cmndf, i)
                    val frequency = sampleRate / betterTau
                    
                    // Calculate probability based on aperiodicity
                    val probability = calculateProbability(aperiodicity)
                    
                    if (frequency in MIN_FREQ..MAX_FREQ) {
                        candidates.add(PitchCandidate(
                            frequency = frequency,
                            probability = probability,
                            aperiodicity = aperiodicity
                        ))
                    }
                }
            }
            i++
        }
        
        // Add unvoiced candidate
        candidates.add(PitchCandidate(
            frequency = 0f,
            probability = calculateUnvoicedProbability(cmndf, minTau, maxTau),
            aperiodicity = 1f
        ))
        
        // Normalize probabilities
        val totalProb = candidates.sumOf { it.probability.toDouble() }
        return if (totalProb > 0) {
            candidates.map { it.copy(probability = it.probability / totalProb.toFloat()) }
                .sortedByDescending { it.probability }
        } else {
            candidates
        }
    }
    
    /**
     * Detect pitch using Viterbi decoding for smooth pitch tracking
     * Optimal for detecting gamakas (ornaments) where pitch glides smoothly
     */
    fun detectPitchSequence(
        audioData: FloatArray,
        frameSize: Int = 1024,
        hopSize: Int = 256
    ): List<PitchResult> {
        val numFrames = (audioData.size - frameSize) / hopSize + 1
        if (numFrames <= 0) return emptyList()
        
        // Get candidates for each frame
        val allCandidates = mutableListOf<List<PitchCandidate>>()
        
        for (frameIdx in 0 until numFrames) {
            val startIdx = frameIdx * hopSize
            val endIdx = minOf(startIdx + frameSize, audioData.size)
            val frame = audioData.sliceArray(startIdx until endIdx)
            allCandidates.add(detectPitchWithCandidates(frame))
        }
        
        // Viterbi decoding for optimal path
        return viterbiDecode(allCandidates)
    }
    
    /**
     * Simple single-frame pitch detection (for real-time use)
     */
    fun detectPitch(audioBuffer: FloatArray, threshold: Float = DEFAULT_THRESHOLD): Float {
        val candidates = detectPitchWithCandidates(audioBuffer)
        
        // Return highest probability voiced candidate
        val voiced = candidates.filter { it.frequency > 0 }
        return if (voiced.isNotEmpty() && voiced.first().probability > 0.3f) {
            voiced.first().frequency
        } else {
            0f // Unvoiced
        }
    }
    
    /**
     * Get voicing probability for the current frame
     */
    fun getVoicingProbability(audioBuffer: FloatArray): Float {
        val candidates = detectPitchWithCandidates(audioBuffer)
        return candidates.filter { it.frequency > 0 }.sumOf { it.probability.toDouble() }.toFloat()
    }
    
    /**
     * Detect pitch with confidence and detailed info
     */
    fun detectPitchDetailed(audioBuffer: FloatArray): DetailedPitchResult {
        val candidates = detectPitchWithCandidates(audioBuffer)
        val voicedCandidates = candidates.filter { it.frequency > 0 }
        
        val bestCandidate = voicedCandidates.maxByOrNull { it.probability }
        val voicingProbability = voicedCandidates.sumOf { it.probability.toDouble() }.toFloat()
        
        return DetailedPitchResult(
            frequency = bestCandidate?.frequency ?: 0f,
            confidence = bestCandidate?.probability ?: 0f,
            voicingProbability = voicingProbability,
            aperiodicity = bestCandidate?.aperiodicity ?: 1f,
            allCandidates = candidates
        )
    }
    
    // ==================== Private Implementation ====================
    
    /**
     * Compute YIN difference function
     */
    private fun computeDifferenceFunction(buffer: FloatArray): FloatArray {
        val halfSize = buffer.size / 2
        val diff = FloatArray(halfSize)
        
        for (tau in 1 until halfSize) {
            var sum = 0f
            for (i in 0 until halfSize) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }
        
        return diff
    }
    
    /**
     * Compute Cumulative Mean Normalized Difference Function
     */
    private fun computeCMNDF(diff: FloatArray): FloatArray {
        val cmndf = FloatArray(diff.size)
        cmndf[0] = 1f
        
        var runningSum = 0f
        for (tau in 1 until diff.size) {
            runningSum += diff[tau]
            cmndf[tau] = if (runningSum > 0) {
                diff[tau] * tau / runningSum
            } else {
                1f
            }
        }
        
        return cmndf
    }
    
    /**
     * Parabolic interpolation for sub-sample accuracy
     */
    private fun parabolicInterpolation(array: FloatArray, index: Int): Float {
        if (index < 1 || index >= array.size - 1) return index.toFloat()
        
        val alpha = array[index - 1]
        val beta = array[index]
        val gamma = array[index + 1]
        
        val peak = 0.5f * (alpha - gamma) / (alpha - 2 * beta + gamma)
        return index + peak
    }
    
    /**
     * Calculate probability from aperiodicity value
     * Using Beta distribution approximation
     */
    private fun calculateProbability(aperiodicity: Float): Float {
        // Higher aperiodicity = lower probability
        // Map aperiodicity [0, 0.5] to probability [1, 0]
        val normalized = (aperiodicity / 0.5f).coerceIn(0f, 1f)
        return (1 - normalized * normalized) // Quadratic mapping
    }
    
    /**
     * Calculate probability of unvoiced frame
     */
    private fun calculateUnvoicedProbability(cmndf: FloatArray, minTau: Int, maxTau: Int): Float {
        // If no good minima found, likely unvoiced
        var minVal = 1f
        for (tau in minTau until maxTau) {
            if (cmndf[tau] < minVal) minVal = cmndf[tau]
        }
        
        // High minimum CMNDF value suggests unvoiced
        return minVal.coerceIn(0f, 1f)
    }
    
    /**
     * Viterbi decoding for optimal pitch sequence
     */
    private fun viterbiDecode(allCandidates: List<List<PitchCandidate>>): List<PitchResult> {
        if (allCandidates.isEmpty()) return emptyList()
        
        val numFrames = allCandidates.size
        
        // Initialize with first frame probabilities
        var prevScores = allCandidates[0].map { ln(it.probability.toDouble().coerceAtLeast(1e-10)) }
        var prevStates = allCandidates[0].indices.toList()
        
        val backpointers = mutableListOf<List<Int>>()
        
        // Forward pass
        for (t in 1 until numFrames) {
            val currCandidates = allCandidates[t]
            val prevCandidates = allCandidates[t - 1]
            
            val currScores = mutableListOf<Double>()
            val currBackpointers = mutableListOf<Int>()
            
            for (j in currCandidates.indices) {
                var bestScore = Double.NEGATIVE_INFINITY
                var bestPrev = 0
                
                for (i in prevCandidates.indices) {
                    val transitionScore = calculateTransitionScore(
                        prevCandidates[i].frequency,
                        currCandidates[j].frequency
                    )
                    val score = prevScores[i] + transitionScore
                    
                    if (score > bestScore) {
                        bestScore = score
                        bestPrev = i
                    }
                }
                
                val emissionScore = ln(currCandidates[j].probability.toDouble().coerceAtLeast(1e-10))
                currScores.add(bestScore + emissionScore)
                currBackpointers.add(bestPrev)
            }
            
            prevScores = currScores
            backpointers.add(currBackpointers)
        }
        
        // Backtrack
        var bestFinalState = prevScores.indices.maxByOrNull { prevScores[it] } ?: 0
        val pitchPath = mutableListOf<PitchResult>()
        
        for (t in (numFrames - 1) downTo 0) {
            val candidate = allCandidates[t][bestFinalState]
            pitchPath.add(0, PitchResult(
                frequency = candidate.frequency,
                confidence = candidate.probability,
                voicingProbability = if (candidate.frequency > 0) candidate.probability else 0f
            ))
            
            if (t > 0) {
                bestFinalState = backpointers[t - 1][bestFinalState]
            }
        }
        
        return pitchPath
    }
    
    /**
     * Calculate transition score between two pitches
     * Penalizes large jumps (in cents)
     */
    private fun calculateTransitionScore(prevFreq: Float, currFreq: Float): Double {
        if (prevFreq <= 0 || currFreq <= 0) {
            // Transition to/from unvoiced
            return -1.0
        }
        
        // Calculate interval in cents
        val cents = 1200 * ln(currFreq.toDouble() / prevFreq) / ln(2.0)
        val absCents = abs(cents)
        
        // Penalize large jumps
        return -absCents / TRANSITION_PENALTY
    }
}

/**
 * Data class for pitch candidate
 */
data class PitchCandidate(
    val frequency: Float,
    val probability: Float,
    val aperiodicity: Float
)

/**
 * Data class for pitch result
 */
data class PitchResult(
    val frequency: Float,
    val confidence: Float,
    val voicingProbability: Float
)

/**
 * Detailed pitch result with all candidates
 */
data class DetailedPitchResult(
    val frequency: Float,
    val confidence: Float,
    val voicingProbability: Float,
    val aperiodicity: Float,
    val allCandidates: List<PitchCandidate>
)

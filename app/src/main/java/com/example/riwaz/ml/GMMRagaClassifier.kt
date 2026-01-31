package com.example.riwaz.ml

import kotlin.math.*

/**
 * Gaussian Mixture Model (GMM) for Raga Classification
 * 
 * Uses pitch histograms, note transition matrices, and phrase patterns
 * to identify the raga being performed.
 * 
 * Each raga has its own GMM with 8-16 Gaussian components representing
 * typical note distributions and transitions.
 */
class GMMRagaClassifier {
    
    companion object {
        private const val N_COMPONENTS = 8  // Gaussian components per raga
        private const val FEATURE_DIM = 159 // 12 + 144 + 2 + 1
    }
    
    // Raga models - each contains GMM parameters
    private val ragaModels = mutableMapOf<String, RagaGMM>()
    
    // Feature extractor
    private val featureExtractor = FeatureExtractor()
    
    init {
        // Initialize GMM models for common ragas
        initializeRagaModels()
    }
    
    data class RagaGMM(
        val ragaName: String,
        val weights: FloatArray,          // (N_COMPONENTS,) mixture weights
        val means: Array<FloatArray>,     // (N_COMPONENTS, FEATURE_DIM) means
        val variances: Array<FloatArray>, // (N_COMPONENTS, FEATURE_DIM) diagonal covariances
        val arohana: List<Int>,           // Ascending scale
        val avarohana: List<Int>,         // Descending scale
        val vadiSwar: Int,                // Most important note
        val samvadiSwar: Int              // Second most important note
    )
    
    data class ClassificationResult(
        val predictedRaga: String,
        val confidence: Float,
        val allScores: Map<String, Float>,
        val topCandidates: List<Pair<String, Float>>
    )
    
    /**
     * Classify the raga from a sequence of detected pitches
     */
    fun classifyRaga(
        pitches: List<Float>,
        tonicFrequency: Float = 261.63f,
        minPitches: Int = 50
    ): ClassificationResult {
        if (pitches.size < minPitches) {
            return ClassificationResult(
                predictedRaga = "Unknown",
                confidence = 0f,
                allScores = emptyMap(),
                topCandidates = emptyList()
            )
        }
        
        // Extract features
        val features = extractFeatures(pitches, tonicFrequency)
        
        // Compute log-likelihood for each raga
        val scores = mutableMapOf<String, Float>()
        
        ragaModels.forEach { (ragaName, gmm) ->
            val logLikelihood = computeLogLikelihood(features, gmm)
            scores[ragaName] = logLikelihood
        }
        
        // Softmax to get probabilities
        val probabilities = softmaxScores(scores)
        
        // Sort by probability
        val sorted = probabilities.entries.sortedByDescending { it.value }
        val topCandidates = sorted.take(3).map { it.key to it.value }
        
        return ClassificationResult(
            predictedRaga = sorted.firstOrNull()?.key ?: "Unknown",
            confidence = sorted.firstOrNull()?.value ?: 0f,
            allScores = probabilities,
            topCandidates = topCandidates
        )
    }
    
    /**
     * Update GMM model with new training data (online learning)
     */
    fun updateModel(
        ragaName: String,
        pitches: List<Float>,
        tonicFrequency: Float = 261.63f,
        learningRate: Float = 0.01f
    ) {
        val gmm = ragaModels[ragaName] ?: return
        val features = extractFeatures(pitches, tonicFrequency)
        
        // Simple online update of means using moving average
        val responsibilities = computeResponsibilities(features, gmm)
        
        for (k in 0 until N_COMPONENTS) {
            val weight = responsibilities[k] * learningRate
            for (d in features.indices) {
                gmm.means[k][d] = (1 - weight) * gmm.means[k][d] + weight * features[d]
            }
        }
    }
    
    /**
     * Get raga characteristics
     */
    fun getRagaInfo(ragaName: String): RagaGMM? = ragaModels[ragaName]
    
    /**
     * Check if a pitch sequence follows raga rules
     */
    fun validateRagaCompliance(
        ragaName: String,
        pitches: List<Float>,
        tonicFrequency: Float
    ): Float {
        val gmm = ragaModels[ragaName] ?: return 0f
        
        // Get pitch classes
        val pitchClasses = pitches.filter { it > 0 }.map { pitch ->
            val semitones = 12 * log2(pitch / tonicFrequency)
            ((semitones % 12 + 12).toInt() % 12)
        }
        
        // Check if notes are in the raga's scale
        val allowedNotes = (gmm.arohana + gmm.avarohana).toSet()
        val validNotes = pitchClasses.count { it in allowedNotes }
        
        return validNotes.toFloat() / pitchClasses.size.coerceAtLeast(1)
    }
    
    // --- Private methods ---
    
    private fun extractFeatures(pitches: List<Float>, tonicFrequency: Float): FloatArray {
        val features = FloatArray(FEATURE_DIM)
        var idx = 0
        
        // 1. Pitch histogram (12 dims)
        val histogram = featureExtractor.pitchHistogram(pitches, tonicFrequency)
        for (i in 0 until 12) {
            features[idx++] = histogram[i]
        }
        
        // 2. Note transition matrix flattened (144 dims)
        val transitions = featureExtractor.noteTransitionMatrix(pitches, tonicFrequency)
        for (i in 0 until 12) {
            for (j in 0 until 12) {
                features[idx++] = transitions[i][j]
            }
        }
        
        // 3. Mean and variance of pitch (2 dims)
        val validPitches = pitches.filter { it > 0 }
        val meanPitch = validPitches.average().toFloat()
        val varPitch = validPitches.map { (it - meanPitch).pow(2) }.average().toFloat()
        features[idx++] = meanPitch / tonicFrequency  // Normalized
        features[idx++] = sqrt(varPitch) / tonicFrequency
        
        // 4. Vadi-samvadi emphasis score (1 dim)
        val topNotes = histogram.indices.sortedByDescending { histogram[it] }.take(2)
        features[idx] = if (topNotes.size >= 2) {
            histogram[topNotes[0]] + histogram[topNotes[1]]
        } else {
            0f
        }
        
        return features
    }
    
    private fun computeLogLikelihood(features: FloatArray, gmm: RagaGMM): Float {
        var totalLogLik = Float.NEGATIVE_INFINITY
        
        for (k in 0 until N_COMPONENTS) {
            val logWeight = ln(gmm.weights[k].toDouble()).toFloat()
            val logGaussian = computeLogGaussian(features, gmm.means[k], gmm.variances[k])
            val componentLogLik = logWeight + logGaussian
            
            // Log-sum-exp for numerical stability
            totalLogLik = logAddExp(totalLogLik, componentLogLik)
        }
        
        return totalLogLik
    }
    
    private fun computeLogGaussian(x: FloatArray, mean: FloatArray, variance: FloatArray): Float {
        var logProb = 0f
        
        for (d in x.indices) {
            val diff = x[d] - mean[d]
            val v = variance[d].coerceAtLeast(1e-6f)
            logProb -= 0.5f * (ln(2 * PI * v).toFloat() + diff * diff / v)
        }
        
        return logProb
    }
    
    private fun computeResponsibilities(features: FloatArray, gmm: RagaGMM): FloatArray {
        val logLikelihoods = FloatArray(N_COMPONENTS)
        
        for (k in 0 until N_COMPONENTS) {
            val logWeight = ln(gmm.weights[k].toDouble()).toFloat()
            logLikelihoods[k] = logWeight + computeLogGaussian(features, gmm.means[k], gmm.variances[k])
        }
        
        // Softmax for responsibilities
        val maxLog = logLikelihoods.maxOrNull() ?: 0f
        val exp = logLikelihoods.map { exp((it - maxLog).toDouble()).toFloat() }
        val sum = exp.sum()
        
        return exp.map { it / sum }.toFloatArray()
    }
    
    private fun softmaxScores(scores: Map<String, Float>): Map<String, Float> {
        val maxScore = scores.values.maxOrNull() ?: 0f
        val exp = scores.mapValues { exp((it.value - maxScore).toDouble()).toFloat() }
        val sum = exp.values.sum()
        return exp.mapValues { it.value / sum }
    }
    
    private fun logAddExp(a: Float, b: Float): Float {
        return if (a > b) {
            a + ln(1 + exp((b - a).toDouble())).toFloat()
        } else {
            b + ln(1 + exp((a - b).toDouble())).toFloat()
        }
    }
    
    private fun initializeRagaModels() {
        // Initialize GMM models for major ragas with hand-crafted parameters
        // In production, these would be trained on real data
        
        // Yaman - evening raga, all shuddha except teevra Ma
        ragaModels["Yaman"] = createRagaGMM(
            name = "Yaman",
            arohana = listOf(0, 2, 4, 6, 7, 9, 11),  // Sa Re Ga Ma(t) Pa Dha Ni
            avarohana = listOf(11, 9, 7, 6, 4, 2, 0),
            vadiSwar = 4,    // Ga
            samvadiSwar = 9, // Dha
            emphasizedNotes = listOf(0, 4, 7, 9, 11)  // Sa, Ga, Pa, Dha, Ni
        )
        
        // Bhairav - morning raga with komal Re and Dha
        ragaModels["Bhairav"] = createRagaGMM(
            name = "Bhairav",
            arohana = listOf(0, 1, 4, 5, 7, 8, 11),  // Sa Re(k) Ga Ma Pa Dha(k) Ni
            avarohana = listOf(11, 8, 7, 5, 4, 1, 0),
            vadiSwar = 8,    // Dha(k)
            samvadiSwar = 1, // Re(k)
            emphasizedNotes = listOf(0, 1, 4, 7, 8)
        )
        
        // Todi - afternoon raga, komal Re, Ga, Dha
        ragaModels["Todi"] = createRagaGMM(
            name = "Todi",
            arohana = listOf(0, 1, 3, 6, 7, 8, 11),  // Sa Re(k) Ga(k) Ma(t) Pa Dha(k) Ni
            avarohana = listOf(11, 8, 7, 6, 3, 1, 0),
            vadiSwar = 8,    // Dha(k)
            samvadiSwar = 3, // Ga(k)
            emphasizedNotes = listOf(0, 3, 6, 8)
        )
        
        // Malkauns - night raga, pentatonic
        ragaModels["Malkauns"] = createRagaGMM(
            name = "Malkauns",
            arohana = listOf(0, 3, 5, 8, 10),  // Sa Ga(k) Ma Dha(k) Ni(k)
            avarohana = listOf(10, 8, 5, 3, 0),
            vadiSwar = 5,     // Ma
            samvadiSwar = 0,  // Sa
            emphasizedNotes = listOf(0, 3, 5, 8, 10)
        )
        
        // Bhupali - evening raga, pentatonic
        ragaModels["Bhupali"] = createRagaGMM(
            name = "Bhupali",
            arohana = listOf(0, 2, 4, 7, 9),  // Sa Re Ga Pa Dha
            avarohana = listOf(9, 7, 4, 2, 0),
            vadiSwar = 4,    // Ga
            samvadiSwar = 9, // Dha
            emphasizedNotes = listOf(0, 2, 4, 7, 9)
        )
        
        // Desh - night raga
        ragaModels["Desh"] = createRagaGMM(
            name = "Desh",
            arohana = listOf(0, 2, 4, 5, 7, 9, 10),
            avarohana = listOf(11, 9, 7, 5, 4, 2, 0),
            vadiSwar = 7,    // Pa
            samvadiSwar = 2, // Re
            emphasizedNotes = listOf(0, 2, 5, 7, 9)
        )
        
        // Kafi - any time
        ragaModels["Kafi"] = createRagaGMM(
            name = "Kafi",
            arohana = listOf(0, 2, 3, 5, 7, 9, 10),  // Sa Re Ga(k) Ma Pa Dha Ni(k)
            avarohana = listOf(10, 9, 7, 5, 3, 2, 0),
            vadiSwar = 7,    // Pa
            samvadiSwar = 2, // Re
            emphasizedNotes = listOf(0, 3, 5, 7, 10)
        )
        
        // Bihag - night raga
        ragaModels["Bihag"] = createRagaGMM(
            name = "Bihag",
            arohana = listOf(0, 4, 5, 6, 7, 9, 11),
            avarohana = listOf(11, 9, 7, 6, 5, 4, 0),
            vadiSwar = 4,    // Ga
            samvadiSwar = 9, // Dha
            emphasizedNotes = listOf(0, 4, 6, 7, 11)
        )
        
        // Bageshree - night raga
        ragaModels["Bageshree"] = createRagaGMM(
            name = "Bageshree",
            arohana = listOf(0, 3, 5, 8, 10),
            avarohana = listOf(11, 10, 8, 5, 3, 2, 0),
            vadiSwar = 5,     // Ma
            samvadiSwar = 0,  // Sa
            emphasizedNotes = listOf(0, 3, 5, 8, 10)
        )
        
        // Puriya Dhanashree - evening
        ragaModels["Puriya Dhanashree"] = createRagaGMM(
            name = "Puriya Dhanashree",
            arohana = listOf(0, 1, 4, 6, 7, 9, 11),
            avarohana = listOf(11, 9, 7, 6, 4, 1, 0),
            vadiSwar = 7,    // Pa
            samvadiSwar = 1, // Re(k)
            emphasizedNotes = listOf(0, 1, 6, 7, 9)
        )
    }
    
    private fun createRagaGMM(
        name: String,
        arohana: List<Int>,
        avarohana: List<Int>,
        vadiSwar: Int,
        samvadiSwar: Int,
        emphasizedNotes: List<Int>
    ): RagaGMM {
        // Create GMM components centered around typical feature patterns
        val weights = FloatArray(N_COMPONENTS) { 1f / N_COMPONENTS }
        val means = Array(N_COMPONENTS) { FloatArray(FEATURE_DIM) }
        val variances = Array(N_COMPONENTS) { FloatArray(FEATURE_DIM) { 0.1f } }
        
        // Initialize means based on raga characteristics
        for (k in 0 until N_COMPONENTS) {
            var idx = 0
            
            // Pitch histogram means - emphasize raga notes with some variation
            for (note in 0 until 12) {
                means[k][idx++] = when {
                    note == vadiSwar -> 0.25f + k * 0.02f
                    note == samvadiSwar -> 0.18f + k * 0.01f
                    note in emphasizedNotes -> 0.12f + k * 0.01f
                    note in arohana || note in avarohana -> 0.05f
                    else -> 0.01f
                }
            }
            
            // Transition matrix means - common transitions
            for (from in 0 until 12) {
                for (to in 0 until 12) {
                    val inScale = from in arohana && to in arohana
                    means[k][idx++] = if (inScale) 0.1f else 0.01f
                }
            }
            
            // Pitch statistics
            means[k][idx++] = 1.0f + k * 0.1f  // Mean pitch / tonic
            means[k][idx++] = 0.3f + k * 0.05f // Pitch variance
            
            // Vadi-samvadi emphasis
            means[k][idx] = 0.4f + k * 0.02f
        }
        
        return RagaGMM(
            ragaName = name,
            weights = weights,
            means = means,
            variances = variances,
            arohana = arohana,
            avarohana = avarohana,
            vadiSwar = vadiSwar,
            samvadiSwar = samvadiSwar
        )
    }
}

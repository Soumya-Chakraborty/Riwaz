package com.example.riwaz.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * Central manager for all ML models
 * Handles lazy loading, fallback to DSP algorithms, and resource management
 */
class MLModelManager(private val context: Context) {
    
    companion object {
        private const val TAG = "MLModelManager"
        
        @Volatile
        private var instance: MLModelManager? = null
        
        fun getInstance(context: Context): MLModelManager {
            return instance ?: synchronized(this) {
                instance ?: MLModelManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // ML Models
    private var pitchDetector: TFLitePitchDetector? = null
    private var ragaClassifier: GMMRagaClassifier? = null
    private var swarClassifier: CNNSwarClassifier? = null
    private var featureExtractor: FeatureExtractor? = null
    
    // Initialization state
    private var isInitialized = false
    private var initJob: Job? = null
    
    // Model availability
    var isPitchModelAvailable = false
        private set
    var isRagaModelAvailable = false
        private set
    var isSwarModelAvailable = false
        private set
    
    data class MLAnalysisResult(
        val pitch: Float,
        val pitchConfidence: Float,
        val swar: String,
        val swarHindi: String,
        val swarConfidence: Float,
        val predictedRaga: String,
        val ragaConfidence: Float,
        val usedNeuralNetwork: Boolean
    )
    
    /**
     * Initialize all ML models asynchronously
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        
        Log.d(TAG, "Initializing ML models...")
        
        try {
            // Initialize feature extractor
            featureExtractor = FeatureExtractor()
            
            // Initialize pitch detector
            pitchDetector = TFLitePitchDetector(context)
            isPitchModelAvailable = pitchDetector?.initialize() == true
            Log.d(TAG, "Pitch detector initialized: $isPitchModelAvailable")
            
            // Initialize GMM raga classifier (no TFLite needed)
            ragaClassifier = GMMRagaClassifier()
            isRagaModelAvailable = true
            Log.d(TAG, "Raga classifier initialized")
            
            // Initialize CNN swar classifier
            swarClassifier = CNNSwarClassifier(context)
            isSwarModelAvailable = swarClassifier?.initialize() == true
            Log.d(TAG, "Swar classifier initialized: $isSwarModelAvailable")
            
            isInitialized = true
            Log.d(TAG, "ML models initialization complete")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ML models: ${e.message}")
            false
        }
    }
    
    /**
     * Initialize in background without blocking
     */
    fun initializeAsync(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        initJob = scope.launch {
            initialize()
        }
    }
    
    /**
     * Full ML analysis of audio
     */
    fun analyze(
        audio: FloatArray,
        sampleRate: Int,
        tonicFrequency: Float,
        currentRaga: String? = null
    ): MLAnalysisResult {
        // Pitch detection
        val pitchResult = pitchDetector?.detectPitch(audio, sampleRate)
        val pitch = pitchResult?.frequency ?: 0f
        val pitchConfidence = pitchResult?.confidence ?: 0f
        
        // Swar classification (with pitch hint if available)
        val swarResult = if (pitch > 0) {
            swarClassifier?.classifySwarWithPitchHint(audio, sampleRate, pitch, tonicFrequency)
        } else {
            swarClassifier?.classifySwar(audio, sampleRate, tonicFrequency)
        }
        
        val swar = swarResult?.swar ?: "—"
        val swarHindi = swarResult?.swarHindi ?: "—"
        val swarConfidence = swarResult?.confidence ?: 0f
        
        // Raga classification (if we have enough pitch history)
        val ragaResult = if (currentRaga == null) {
            ragaClassifier?.classifyRaga(listOf(pitch), tonicFrequency)
        } else {
            null
        }
        
        return MLAnalysisResult(
            pitch = pitch,
            pitchConfidence = pitchConfidence,
            swar = swar,
            swarHindi = swarHindi,
            swarConfidence = swarConfidence,
            predictedRaga = ragaResult?.predictedRaga ?: currentRaga ?: "Unknown",
            ragaConfidence = ragaResult?.confidence ?: 0f,
            usedNeuralNetwork = isPitchModelAvailable || isSwarModelAvailable
        )
    }
    
    /**
     * Analyze pitch only (faster)
     */
    fun analyzePitch(audio: FloatArray, sampleRate: Int): TFLitePitchDetector.PitchPrediction? {
        return pitchDetector?.detectPitch(audio, sampleRate)
    }
    
    /**
     * Classify swar only
     */
    fun classifySwar(
        audio: FloatArray,
        sampleRate: Int,
        tonicFrequency: Float
    ): CNNSwarClassifier.SwarPrediction? {
        return swarClassifier?.classifySwar(audio, sampleRate, tonicFrequency)
    }
    
    /**
     * Classify raga from pitch sequence
     */
    fun classifyRaga(
        pitches: List<Float>,
        tonicFrequency: Float
    ): GMMRagaClassifier.ClassificationResult? {
        return ragaClassifier?.classifyRaga(pitches, tonicFrequency)
    }
    
    /**
     * Validate if notes match raga rules
     */
    fun validateRagaCompliance(
        ragaName: String,
        pitches: List<Float>,
        tonicFrequency: Float
    ): Float {
        return ragaClassifier?.validateRagaCompliance(ragaName, pitches, tonicFrequency) ?: 0f
    }
    
    /**
     * Get feature extractor for custom analysis
     */
    fun getFeatureExtractor(): FeatureExtractor {
        return featureExtractor ?: FeatureExtractor()
    }
    
    /**
     * Get raga info
     */
    fun getRagaInfo(ragaName: String) = ragaClassifier?.getRagaInfo(ragaName)
    
    /**
     * Get available ragas
     */
    fun getAvailableRagas(): List<String> {
        return listOf(
            "Yaman", "Bhairav", "Todi", "Malkauns", "Bhupali",
            "Desh", "Kafi", "Bihag", "Bageshree", "Puriya Dhanashree"
        )
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        initJob?.cancel()
        pitchDetector?.close()
        swarClassifier?.close()
        pitchDetector = null
        swarClassifier = null
        ragaClassifier = null
        featureExtractor = null
        isInitialized = false
        instance = null
    }
    
    /**
     * Check if ready for analysis
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * Get model status summary
     */
    fun getStatusSummary(): String {
        return buildString {
            append("ML Models Status:\n")
            append("- Pitch Detector: ${if (isPitchModelAvailable) "Neural Network" else "DSP Fallback"}\n")
            append("- Raga Classifier: GMM (${getAvailableRagas().size} ragas)\n")
            append("- Swar Classifier: ${if (isSwarModelAvailable) "CNN" else "DSP Fallback"}")
        }
    }
}

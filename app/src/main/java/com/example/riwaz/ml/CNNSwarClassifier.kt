package com.example.riwaz.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.*

/**
 * CNN-based Swar (Note) Classifier using TensorFlow Lite
 * 
 * Architecture:
 * - Input: Mel Spectrogram (128 bands × 32 frames)
 * - 3 Convolutional layers with BatchNorm and MaxPool
 * - 2 Fully connected layers
 * - Output: 13 classes (12 swars + silence)
 */
class CNNSwarClassifier(private val context: Context) {
    
    companion object {
        private const val TAG = "CNNSwarClassifier"
        private const val MODEL_FILENAME = "swar_classifier.tflite"
        private const val N_MELS = 128
        private const val N_FRAMES = 32
        private const val N_CLASSES = 13
        
        // Swar labels
        val SWAR_LABELS = arrayOf(
            "Sa",      // 0
            "Re♭",     // 1 (Komal Re)
            "Re",      // 2 (Shuddha Re)
            "Ga♭",     // 3 (Komal Ga)
            "Ga",      // 4 (Shuddha Ga)
            "Ma",      // 5 (Shuddha Ma)
            "Ma#",     // 6 (Teevra Ma)
            "Pa",      // 7
            "Dha♭",    // 8 (Komal Dha)
            "Dha",     // 9 (Shuddha Dha)
            "Ni♭",     // 10 (Komal Ni)
            "Ni",      // 11 (Shuddha Ni)
            "Silence"  // 12
        )
        
        // Hindi labels
        val SWAR_LABELS_HINDI = arrayOf(
            "सा", "रे॒", "रे", "ग॒", "ग", "म", "म॑", "प", "ध॒", "ध", "नि॒", "नि", "—"
        )
    }
    
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isModelLoaded = false
    
    // Feature extractor
    private val featureExtractor = FeatureExtractor()
    
    // Simulated CNN for demonstration
    private val simulatedCNN = SimulatedSwarCNN()
    
    data class SwarPrediction(
        val swar: String,
        val swarHindi: String,
        val classIndex: Int,
        val confidence: Float,
        val allConfidences: FloatArray,
        val isSilence: Boolean
    )
    
    /**
     * Initialize the CNN model
     */
    fun initialize(): Boolean {
        return try {
            val modelBuffer = loadModelFile()
            if (modelBuffer != null) {
                val options = Interpreter.Options()
                
                try {
                    gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                    Log.d(TAG, "GPU delegate enabled for swar classifier")
                } catch (e: Exception) {
                    Log.w(TAG, "GPU not available for swar classifier")
                }
                
                interpreter = Interpreter(modelBuffer, options)
                isModelLoaded = true
                Log.d(TAG, "Swar classifier model loaded")
            } else {
                Log.w(TAG, "Using simulated swar CNN")
                isModelLoaded = false
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize swar classifier: ${e.message}")
            isModelLoaded = false
            true
        }
    }
    
    /**
     * Classify swar from audio chunk
     * @param audio Audio samples (recommended: 1024-2048 samples)
     * @param sampleRate Sample rate
     * @param tonicFrequency Tonic (Sa) frequency for reference
     */
    fun classifySwar(
        audio: FloatArray,
        sampleRate: Int,
        tonicFrequency: Float = 261.63f
    ): SwarPrediction {
        // Check for silence
        val rms = sqrt(audio.map { it * it }.average()).toFloat()
        if (rms < 0.01f) {
            return SwarPrediction(
                swar = "Silence",
                swarHindi = "—",
                classIndex = 12,
                confidence = 1.0f,
                allConfidences = FloatArray(N_CLASSES) { if (it == 12) 1f else 0f },
                isSilence = true
            )
        }
        
        // Extract mel spectrogram features
        val melSpec = featureExtractor.prepareForCNN(audio, N_FRAMES)
        
        // Run inference
        val confidences = if (isModelLoaded && interpreter != null) {
            runTFLiteInference(melSpec)
        } else {
            simulatedCNN.forward(audio, sampleRate, tonicFrequency)
        }
        
        // Find top prediction
        val topClass = confidences.indices.maxByOrNull { confidences[it] } ?: 12
        val confidence = confidences[topClass]
        
        return SwarPrediction(
            swar = SWAR_LABELS[topClass],
            swarHindi = SWAR_LABELS_HINDI[topClass],
            classIndex = topClass,
            confidence = confidence,
            allConfidences = confidences,
            isSilence = topClass == 12
        )
    }
    
    /**
     * Classify swar with pitch hint for improved accuracy
     */
    fun classifySwarWithPitchHint(
        audio: FloatArray,
        sampleRate: Int,
        detectedPitch: Float,
        tonicFrequency: Float
    ): SwarPrediction {
        // Get base prediction
        val basePrediction = classifySwar(audio, sampleRate, tonicFrequency)
        
        if (detectedPitch <= 0 || basePrediction.isSilence) {
            return basePrediction
        }
        
        // Calculate expected class from pitch
        val semitones = 12 * log2(detectedPitch / tonicFrequency)
        val expectedClass = ((semitones % 12 + 12).toInt() % 12)
        
        // Boost confidence if CNN agrees with pitch detector
        val adjustedConfidences = basePrediction.allConfidences.copyOf()
        
        // Small boost to pitch-detected class
        if (expectedClass < 12) {
            adjustedConfidences[expectedClass] *= 1.2f
        }
        
        // Renormalize
        val sum = adjustedConfidences.sum()
        for (i in adjustedConfidences.indices) {
            adjustedConfidences[i] /= sum
        }
        
        val topClass = adjustedConfidences.indices.maxByOrNull { adjustedConfidences[it] } ?: 12
        
        return SwarPrediction(
            swar = SWAR_LABELS[topClass],
            swarHindi = SWAR_LABELS_HINDI[topClass],
            classIndex = topClass,
            confidence = adjustedConfidences[topClass],
            allConfidences = adjustedConfidences,
            isSilence = topClass == 12
        )
    }
    
    /**
     * Batch classification for analysis
     */
    fun classifySequence(
        audio: FloatArray,
        sampleRate: Int,
        tonicFrequency: Float,
        frameSize: Int = 2048,
        hopSize: Int = 512
    ): List<SwarPrediction> {
        val results = mutableListOf<SwarPrediction>()
        var pos = 0
        
        while (pos + frameSize <= audio.size) {
            val chunk = audio.sliceArray(pos until pos + frameSize)
            results.add(classifySwar(chunk, sampleRate, tonicFrequency))
            pos += hopSize
        }
        
        return results
    }
    
    /**
     * Get swar distribution for a segment
     */
    fun getSwarDistribution(predictions: List<SwarPrediction>): Map<String, Float> {
        val distribution = mutableMapOf<String, Float>()
        val nonSilent = predictions.filter { !it.isSilence }
        
        if (nonSilent.isEmpty()) return distribution
        
        for (pred in nonSilent) {
            distribution[pred.swar] = (distribution[pred.swar] ?: 0f) + pred.confidence
        }
        
        // Normalize
        val total = distribution.values.sum()
        distribution.forEach { (k, v) -> distribution[k] = v / total }
        
        return distribution
    }
    
    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
    }
    
    // --- Private methods ---
    
    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            val assetFileDescriptor = context.assets.openFd(MODEL_FILENAME)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun runTFLiteInference(melSpec: Array<FloatArray>): FloatArray {
        // Flatten mel spectrogram to 1D input
        val inputSize = N_MELS * N_FRAMES
        val inputBuffer = ByteBuffer.allocateDirect(inputSize * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        for (frame in 0 until N_FRAMES) {
            for (mel in 0 until N_MELS) {
                inputBuffer.putFloat(melSpec[mel][frame])
            }
        }
        inputBuffer.rewind()
        
        val outputBuffer = ByteBuffer.allocateDirect(N_CLASSES * 4)
        outputBuffer.order(ByteOrder.nativeOrder())
        
        interpreter?.run(inputBuffer, outputBuffer)
        
        outputBuffer.rewind()
        val output = FloatArray(N_CLASSES)
        for (i in 0 until N_CLASSES) {
            output[i] = outputBuffer.float
        }
        
        return softmax(output)
    }
    
    private fun softmax(input: FloatArray): FloatArray {
        val maxVal = input.maxOrNull() ?: 0f
        val exp = input.map { exp((it - maxVal).toDouble()).toFloat() }
        val sum = exp.sum()
        return exp.map { it / sum }.toFloatArray()
    }
    
    /**
     * Simulated CNN that uses pitch detection to mimic classification
     */
    private inner class SimulatedSwarCNN {
        
        fun forward(audio: FloatArray, sampleRate: Int, tonicFrequency: Float): FloatArray {
            val confidences = FloatArray(N_CLASSES)
            
            // Estimate pitch using autocorrelation
            val pitch = estimatePitch(audio, sampleRate)
            
            if (pitch <= 0) {
                confidences[12] = 1f  // Silence
                return confidences
            }
            
            // Convert to swar class
            val semitones = 12 * log2(pitch / tonicFrequency)
            val normalizedSemitones = ((semitones % 12) + 12) % 12
            val baseClass = normalizedSemitones.roundToInt() % 12
            
            // Create distribution around detected note
            for (i in 0 until 12) {
                val distance = minOf(
                    abs(i - baseClass),
                    12 - abs(i - baseClass)
                )
                confidences[i] = exp(-distance * 2.0).toFloat()
            }
            
            // Small probability for silence
            confidences[12] = 0.01f
            
            return softmax(confidences)
        }
        
        private fun estimatePitch(audio: FloatArray, sampleRate: Int): Float {
            val n = audio.size
            val minLag = sampleRate / 1000  // Max 1000 Hz
            val maxLag = sampleRate / 50    // Min 50 Hz
            
            // Autocorrelation
            var maxCorr = 0f
            var bestLag = 0
            
            for (lag in minLag until minOf(maxLag, n / 2)) {
                var corr = 0f
                for (i in 0 until n - lag) {
                    corr += audio[i] * audio[i + lag]
                }
                if (corr > maxCorr) {
                    maxCorr = corr
                    bestLag = lag
                }
            }
            
            // Check if valid pitch
            val norm = audio.map { it * it }.sum()
            if (bestLag == 0 || maxCorr / norm < 0.3f) {
                return 0f
            }
            
            return sampleRate.toFloat() / bestLag
        }
    }
}

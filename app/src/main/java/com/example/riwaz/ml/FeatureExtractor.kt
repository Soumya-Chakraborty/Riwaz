package com.example.riwaz.ml

import kotlin.math.*

/**
 * Feature Extractor for ML-based audio analysis
 * Extracts Mel spectrograms, MFCCs, chroma features for neural networks
 */
class FeatureExtractor(
    private val sampleRate: Int = 44100,
    private val nMels: Int = 128,
    private val nMFCC: Int = 13,
    private val hopLength: Int = 512,
    private val nFFT: Int = 2048
) {
    // Mel filterbank
    private val melFilterbank: Array<FloatArray> by lazy { createMelFilterbank() }
    
    // DCT matrix for MFCC
    private val dctMatrix: Array<FloatArray> by lazy { createDCTMatrix() }
    
    /**
     * Computes Mel spectrogram from audio data
     * @param audio Raw audio samples
     * @return Mel spectrogram (nMels x nFrames)
     */
    fun melSpectrogram(audio: FloatArray): Array<FloatArray> {
        val frames = frameAudio(audio)
        val nFrames = frames.size
        
        val melSpec = Array(nMels) { FloatArray(nFrames) }
        
        frames.forEachIndexed { frameIdx, frame ->
            // Apply Hanning window
            val windowed = applyWindow(frame)
            
            // FFT
            val spectrum = computeFFTMagnitude(windowed)
            
            // Apply mel filterbank
            for (mel in 0 until nMels) {
                var energy = 0f
                for (bin in spectrum.indices) {
                    energy += spectrum[bin] * melFilterbank[mel][bin.coerceAtMost(melFilterbank[mel].size - 1)]
                }
                // Log scale with floor to avoid log(0)
                melSpec[mel][frameIdx] = ln((energy + 1e-10f).toDouble()).toFloat()
            }
        }
        
        return melSpec
    }
    
    /**
     * Computes MFCCs from audio
     * @param audio Raw audio samples
     * @return MFCCs (nMFCC x nFrames)
     */
    fun mfcc(audio: FloatArray): Array<FloatArray> {
        val melSpec = melSpectrogram(audio)
        val nFrames = melSpec[0].size
        
        val mfccs = Array(nMFCC) { FloatArray(nFrames) }
        
        for (frame in 0 until nFrames) {
            // Get mel spectrum for this frame
            val melFrame = FloatArray(nMels) { melSpec[it][frame] }
            
            // Apply DCT
            for (i in 0 until nMFCC) {
                var sum = 0f
                for (j in 0 until nMels) {
                    sum += dctMatrix[i][j] * melFrame[j]
                }
                mfccs[i][frame] = sum
            }
        }
        
        return mfccs
    }
    
    /**
     * Computes chroma features (pitch class distribution)
     * @param audio Raw audio samples
     * @return Chroma features (12 x nFrames)
     */
    fun chromaFeatures(audio: FloatArray): Array<FloatArray> {
        val frames = frameAudio(audio)
        val nFrames = frames.size
        
        val chroma = Array(12) { FloatArray(nFrames) }
        
        frames.forEachIndexed { frameIdx, frame ->
            val windowed = applyWindow(frame)
            val spectrum = computeFFTMagnitude(windowed)
            
            // Map frequency bins to pitch classes
            for (bin in 1 until spectrum.size) {
                val freq = bin * sampleRate.toFloat() / nFFT
                if (freq > 20 && freq < 5000) {
                    val pitchClass = freqToPitchClass(freq)
                    chroma[pitchClass][frameIdx] += spectrum[bin]
                }
            }
            
            // Normalize
            val sum = chroma.sumOf { it[frameIdx].toDouble() }.toFloat()
            if (sum > 0) {
                for (pc in 0 until 12) {
                    chroma[pc][frameIdx] /= sum
                }
            }
        }
        
        return chroma
    }
    
    /**
     * Computes pitch histogram (12 pitch classes normalized)
     */
    fun pitchHistogram(pitches: List<Float>, tonicFreq: Float = 261.63f): FloatArray {
        val histogram = FloatArray(12)
        var count = 0
        
        for (pitch in pitches) {
            if (pitch > 0) {
                val semitones = 12 * log2(pitch / tonicFreq)
                val pitchClass = ((semitones % 12 + 12) % 12).roundToInt() % 12
                histogram[pitchClass]++
                count++
            }
        }
        
        // Normalize
        if (count > 0) {
            for (i in 0 until 12) {
                histogram[i] /= count
            }
        }
        
        return histogram
    }
    
    /**
     * Computes note transition matrix (bigrams)
     */
    fun noteTransitionMatrix(pitches: List<Float>, tonicFreq: Float = 261.63f): Array<FloatArray> {
        val matrix = Array(12) { FloatArray(12) }
        var lastPitchClass = -1
        var transitions = 0
        
        for (pitch in pitches) {
            if (pitch > 0) {
                val semitones = 12 * log2(pitch / tonicFreq)
                val pitchClass = ((semitones % 12 + 12) % 12).roundToInt() % 12
                
                if (lastPitchClass >= 0 && lastPitchClass != pitchClass) {
                    matrix[lastPitchClass][pitchClass]++
                    transitions++
                }
                lastPitchClass = pitchClass
            }
        }
        
        // Normalize rows
        for (i in 0 until 12) {
            val rowSum = matrix[i].sum()
            if (rowSum > 0) {
                for (j in 0 until 12) {
                    matrix[i][j] /= rowSum
                }
            }
        }
        
        return matrix
    }
    
    /**
     * Prepares audio for CNN input (fixed size mel spectrogram)
     */
    fun prepareForCNN(audio: FloatArray, targetFrames: Int = 32): Array<FloatArray> {
        val melSpec = melSpectrogram(audio)
        val nFrames = melSpec[0].size
        
        // Resize to target frames
        val resized = Array(nMels) { FloatArray(targetFrames) }
        
        for (mel in 0 until nMels) {
            for (frame in 0 until targetFrames) {
                val srcFrame = (frame * nFrames / targetFrames).coerceIn(0, nFrames - 1)
                resized[mel][frame] = melSpec[mel][srcFrame]
            }
        }
        
        // Normalize to [-1, 1]
        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE
        for (mel in 0 until nMels) {
            for (frame in 0 until targetFrames) {
                minVal = minOf(minVal, resized[mel][frame])
                maxVal = maxOf(maxVal, resized[mel][frame])
            }
        }
        
        val range = maxVal - minVal
        if (range > 0) {
            for (mel in 0 until nMels) {
                for (frame in 0 until targetFrames) {
                    resized[mel][frame] = 2 * (resized[mel][frame] - minVal) / range - 1
                }
            }
        }
        
        return resized
    }
    
    // --- Private helper functions ---
    
    private fun frameAudio(audio: FloatArray): List<FloatArray> {
        val frames = mutableListOf<FloatArray>()
        var start = 0
        while (start + nFFT <= audio.size) {
            frames.add(audio.sliceArray(start until start + nFFT))
            start += hopLength
        }
        return frames
    }
    
    private fun applyWindow(frame: FloatArray): FloatArray {
        return FloatArray(frame.size) { i ->
            val window = 0.5f * (1 - cos(2 * PI * i / (frame.size - 1))).toFloat()
            frame[i] * window
        }
    }
    
    private fun computeFFTMagnitude(frame: FloatArray): FloatArray {
        val n = frame.size
        val real = frame.copyOf()
        val imag = FloatArray(n)
        
        // Simple DFT for magnitude (for production, use FFTProcessor)
        val nBins = n / 2 + 1
        val magnitude = FloatArray(nBins)
        
        for (k in 0 until nBins) {
            var re = 0f
            var im = 0f
            for (t in 0 until n) {
                val angle = 2 * PI * k * t / n
                re += real[t] * cos(angle).toFloat()
                im -= real[t] * sin(angle).toFloat()
            }
            magnitude[k] = sqrt(re * re + im * im)
        }
        
        return magnitude
    }
    
    private fun createMelFilterbank(): Array<FloatArray> {
        val nBins = nFFT / 2 + 1
        val filterbank = Array(nMels) { FloatArray(nBins) }
        
        val fMin = 0f
        val fMax = sampleRate / 2f
        
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)
        
        val melPoints = FloatArray(nMels + 2) { i ->
            melMin + i * (melMax - melMin) / (nMels + 1)
        }
        
        val freqPoints = melPoints.map { melToHz(it) }
        val binPoints = freqPoints.map { (it * nFFT / sampleRate).roundToInt() }
        
        for (mel in 0 until nMels) {
            val startBin = binPoints[mel]
            val centerBin = binPoints[mel + 1]
            val endBin = binPoints[mel + 2]
            
            for (bin in startBin until centerBin) {
                if (bin < nBins && centerBin > startBin) {
                    filterbank[mel][bin] = (bin - startBin).toFloat() / (centerBin - startBin)
                }
            }
            for (bin in centerBin until endBin) {
                if (bin < nBins && endBin > centerBin) {
                    filterbank[mel][bin] = (endBin - bin).toFloat() / (endBin - centerBin)
                }
            }
        }
        
        return filterbank
    }
    
    private fun createDCTMatrix(): Array<FloatArray> {
        val matrix = Array(nMFCC) { FloatArray(nMels) }
        for (i in 0 until nMFCC) {
            for (j in 0 until nMels) {
                matrix[i][j] = cos(PI * i * (j + 0.5) / nMels).toFloat()
            }
        }
        return matrix
    }
    
    private fun hzToMel(hz: Float): Float = 2595 * log10(1 + hz / 700)
    
    private fun melToHz(mel: Float): Float = 700 * (10f.pow(mel / 2595) - 1)
    
    private fun freqToPitchClass(freq: Float): Int {
        val noteNum = 12 * log2(freq / 440.0) + 69
        return ((noteNum % 12 + 12) % 12).roundToInt() % 12
    }
}

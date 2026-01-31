package com.example.riwaz.utils

import kotlin.math.*

/**
 * State-of-the-art FFT Processor using Radix-2 Cooley-Tukey algorithm
 * 
 * Features:
 * - Fast O(n log n) FFT computation
 * - Hanning window for spectral leakage reduction
 * - Magnitude and phase spectrum calculation
 * - Harmonic detection and analysis
 * - Spectral centroid calculation for timbre analysis
 */
class FFTProcessor {
    
    companion object {
        private const val TWO_PI = 2.0 * PI
    }
    
    /**
     * Compute FFT of real-valued audio data
     * Returns complex spectrum as array of [real, imaginary] pairs
     */
    fun computeFFT(audioData: FloatArray): Array<DoubleArray> {
        // Pad to next power of 2 if necessary
        val n = nextPowerOfTwo(audioData.size)
        
        // Apply Hanning window and convert to complex
        val real = DoubleArray(n)
        val imag = DoubleArray(n)
        
        for (i in audioData.indices) {
            val window = hanningWindow(i, audioData.size)
            real[i] = audioData[i].toDouble() * window
            imag[i] = 0.0
        }
        
        // Perform in-place FFT
        fftRadix2(real, imag)
        
        return Array(n) { i -> doubleArrayOf(real[i], imag[i]) }
    }
    
    /**
     * Compute magnitude spectrum from FFT result
     * Returns only the positive frequencies (first half)
     */
    fun computeMagnitudeSpectrum(audioData: FloatArray): FloatArray {
        val fft = computeFFT(audioData)
        val n = fft.size
        val halfN = n / 2
        
        return FloatArray(halfN) { i ->
            val real = fft[i][0]
            val imag = fft[i][1]
            sqrt(real * real + imag * imag).toFloat()
        }
    }
    
    /**
     * Compute power spectrum (magnitude squared) in dB
     */
    fun computePowerSpectrumDB(audioData: FloatArray): FloatArray {
        val magnitude = computeMagnitudeSpectrum(audioData)
        val reference = magnitude.maxOrNull() ?: 1f
        
        return FloatArray(magnitude.size) { i ->
            val power = magnitude[i] / reference
            if (power > 0) (20 * log10(power.toDouble())).toFloat() else -120f
        }
    }
    
    /**
     * Detect harmonic frequencies from audio data
     * Returns list of detected harmonics with their magnitudes
     */
    fun detectHarmonics(
        audioData: FloatArray,
        sampleRate: Int,
        fundamentalFreq: Float,
        numHarmonics: Int = 8
    ): List<HarmonicInfo> {
        if (fundamentalFreq <= 0) return emptyList()
        
        val magnitude = computeMagnitudeSpectrum(audioData)
        val freqResolution = sampleRate.toFloat() / (magnitude.size * 2)
        
        val harmonics = mutableListOf<HarmonicInfo>()
        
        for (h in 1..numHarmonics) {
            val expectedFreq = fundamentalFreq * h
            val binIndex = (expectedFreq / freqResolution).toInt()
            
            if (binIndex < magnitude.size) {
                // Search in a small window around expected bin
                val windowSize = 3
                var maxMag = 0f
                var maxBin = binIndex
                
                for (offset in -windowSize..windowSize) {
                    val idx = binIndex + offset
                    if (idx in magnitude.indices && magnitude[idx] > maxMag) {
                        maxMag = magnitude[idx]
                        maxBin = idx
                    }
                }
                
                val actualFreq = maxBin * freqResolution
                harmonics.add(HarmonicInfo(
                    harmonicNumber = h,
                    expectedFrequency = expectedFreq,
                    detectedFrequency = actualFreq,
                    magnitude = maxMag,
                    isPresent = maxMag > magnitude.average() * 0.5
                ))
            }
        }
        
        return harmonics
    }
    
    /**
     * Calculate spectral centroid (brightness of sound)
     * Higher values indicate brighter/sharper timbre
     */
    fun calculateSpectralCentroid(audioData: FloatArray, sampleRate: Int): Float {
        val magnitude = computeMagnitudeSpectrum(audioData)
        val freqResolution = sampleRate.toFloat() / (magnitude.size * 2)
        
        var weightedSum = 0.0
        var magnitudeSum = 0.0
        
        for (i in magnitude.indices) {
            val freq = i * freqResolution
            weightedSum += freq * magnitude[i]
            magnitudeSum += magnitude[i]
        }
        
        return if (magnitudeSum > 0) (weightedSum / magnitudeSum).toFloat() else 0f
    }
    
    /**
     * Calculate spectral flatness (noisiness vs tonality)
     * Values close to 1 = noise-like, close to 0 = tonal
     */
    fun calculateSpectralFlatness(audioData: FloatArray): Float {
        val magnitude = computeMagnitudeSpectrum(audioData)
        val nonZero = magnitude.filter { it > 0 }
        
        if (nonZero.isEmpty()) return 0f
        
        val geometricMean = exp(nonZero.map { ln(it.toDouble()) }.average())
        val arithmeticMean = nonZero.average()
        
        return (geometricMean / arithmeticMean).toFloat().coerceIn(0f, 1f)
    }
    
    /**
     * Calculate harmonic-to-noise ratio (voice quality indicator)
     * Higher values indicate clearer, more tonal voice
     */
    fun calculateHNR(
        audioData: FloatArray,
        sampleRate: Int,
        fundamentalFreq: Float
    ): Float {
        if (fundamentalFreq <= 0) return 0f
        
        val harmonics = detectHarmonics(audioData, sampleRate, fundamentalFreq)
        val magnitude = computeMagnitudeSpectrum(audioData)
        
        // Sum harmonic energy
        var harmonicEnergy = 0.0
        harmonics.filter { it.isPresent }.forEach { h ->
            harmonicEnergy += h.magnitude * h.magnitude
        }
        
        // Total energy
        var totalEnergy = 0.0
        magnitude.forEach { m ->
            totalEnergy += m * m
        }
        
        // Noise energy = total - harmonic
        val noiseEnergy = totalEnergy - harmonicEnergy
        
        return if (noiseEnergy > 0) {
            (10 * log10(harmonicEnergy / noiseEnergy)).toFloat().coerceIn(-20f, 40f)
        } else {
            40f // Very clean signal
        }
    }
    
    // ==================== Private Helper Functions ====================
    
    /**
     * Radix-2 Cooley-Tukey FFT (in-place)
     */
    private fun fftRadix2(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        val bits = (ln(n.toDouble()) / ln(2.0)).toInt()
        
        // Bit-reversal permutation
        for (i in 0 until n) {
            val j = bitReverse(i, bits)
            if (j > i) {
                // Swap real parts
                var temp = real[i]
                real[i] = real[j]
                real[j] = temp
                // Swap imaginary parts
                temp = imag[i]
                imag[i] = imag[j]
                imag[j] = temp
            }
        }
        
        // Cooley-Tukey iterative FFT
        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val tableStep = n / size
            
            for (i in 0 until n step size) {
                var k = 0
                for (j in i until i + halfSize) {
                    val angle = -TWO_PI * k / size
                    val cos = cos(angle)
                    val sin = sin(angle)
                    
                    val tReal = real[j + halfSize] * cos - imag[j + halfSize] * sin
                    val tImag = real[j + halfSize] * sin + imag[j + halfSize] * cos
                    
                    real[j + halfSize] = real[j] - tReal
                    imag[j + halfSize] = imag[j] - tImag
                    real[j] = real[j] + tReal
                    imag[j] = imag[j] + tImag
                    
                    k += tableStep
                }
            }
            size *= 2
        }
    }
    
    /**
     * Bit reversal for FFT
     */
    private fun bitReverse(x: Int, bits: Int): Int {
        var result = 0
        var value = x
        for (i in 0 until bits) {
            result = (result shl 1) or (value and 1)
            value = value shr 1
        }
        return result
    }
    
    /**
     * Hanning window function for spectral leakage reduction
     */
    private fun hanningWindow(index: Int, size: Int): Double {
        return 0.5 * (1 - cos(TWO_PI * index / (size - 1)))
    }
    
    /**
     * Find next power of 2
     */
    private fun nextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) power *= 2
        return power
    }
}

/**
 * Data class for harmonic information
 */
data class HarmonicInfo(
    val harmonicNumber: Int,
    val expectedFrequency: Float,
    val detectedFrequency: Float,
    val magnitude: Float,
    val isPresent: Boolean
)

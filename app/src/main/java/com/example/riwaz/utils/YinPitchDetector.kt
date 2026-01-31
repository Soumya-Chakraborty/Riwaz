package com.example.riwaz.utils

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * YIN pitch detection algorithm implementation optimized for Indian classical music
 * Based on the paper "YIN, a fundamental frequency estimator for speech and music"
 */
class YinPitchDetector(private val sampleRate: Int) {
    
    companion object {
        private const val DEFAULT_THRESHOLD = 0.15f
        private const val MIN_FREQ = 50.0f  // Minimum frequency for vocal range
        private const val MAX_FREQ = 1000.0f // Maximum frequency for vocal range
    }
    
    /**
     * Detects pitch using the YIN algorithm
     * @param audioBuffer Input audio buffer
     * @param threshold Threshold for peak picking (default 0.15)
     * @return Detected fundamental frequency in Hz, or 0.0 if no pitch detected
     */
    fun detectPitch(audioBuffer: FloatArray, threshold: Float = DEFAULT_THRESHOLD): Float {
        if (audioBuffer.isEmpty()) return 0.0f
        
        // Compute difference function
        val diffFunction = computeDifferenceFunction(audioBuffer)
        
        // Compute cumulative mean normalized difference function
        val cmndf = computeCumulativeMeanNormalizedDifference(diffFunction)
        
        // Find the first minimum after the global maximum
        val tau = findFirstMinimumAfterGlobalMaximum(cmndf, threshold)
        
        if (tau <= 0) return 0.0f
        
        // Perform parabolic interpolation for sub-sample accuracy
        val interpolatedTau = parabolicInterpolation(cmndf, tau)
        
        // Convert to frequency
        val frequency = sampleRate.toFloat() / interpolatedTau
        
        // Validate frequency is within vocal range
        return if (frequency >= MIN_FREQ && frequency <= MAX_FREQ) frequency else 0.0f
    }
    
    /**
     * Computes the difference function as described in the YIN algorithm
     */
    private fun computeDifferenceFunction(audioBuffer: FloatArray): FloatArray {
        val bufferSize = audioBuffer.size
        val halfBufferSize = bufferSize shr 1  // Divide by 2 using bit shift
        val diffFunction = FloatArray(halfBufferSize)
        
        for (tau in 0 until halfBufferSize) {
            var sum = 0.0f
            for (j in 0 until halfBufferSize) {
                val diff = audioBuffer[j] - audioBuffer[j + tau]
                sum += diff * diff
            }
            diffFunction[tau] = sum
        }
        
        return diffFunction
    }
    
    /**
     * Computes the cumulative mean normalized difference function
     */
    private fun computeCumulativeMeanNormalizedDifference(diffFunction: FloatArray): FloatArray {
        val size = diffFunction.size
        val cmndf = FloatArray(size)
        
        // Initialize first element
        if (size > 0) {
            cmndf[0] = 1.0f
        }
        
        var sum = diffFunction[0].toDouble()
        
        for (i in 1 until size) {
            sum += diffFunction[i]
            // Cumulative mean normalized difference
            cmndf[i] = diffFunction[i] * i / sum.toFloat()
        }
        
        return cmndf
    }
    
    /**
     * Finds the first minimum after the global maximum that falls below the threshold
     */
    private fun findFirstMinimumAfterGlobalMaximum(cmndf: FloatArray, threshold: Float): Int {
        // Find the first period (tau) that is below the threshold
        var tau = 0
        while (tau < cmndf.size && cmndf[tau] > threshold) {
            tau++
        }
        
        // If we didn't find a value below the threshold, return 0
        if (tau >= cmndf.size) return 0
        
        // Find the smallest value after the first minimum
        var minValue = cmndf[tau]
        var minIndex = tau
        
        // Look for the next minimum in a reasonable range
        val searchEnd = min(cmndf.size, tau + 100) // Limit search to avoid noise
        for (i in tau until searchEnd) {
            if (cmndf[i] < minValue) {
                minValue = cmndf[i]
                minIndex = i
            }
        }
        
        return minIndex
    }
    
    /**
     * Performs parabolic interpolation for sub-sample accuracy
     */
    private fun parabolicInterpolation(array: FloatArray, x: Int): Float {
        if (x < 1 || x >= array.size - 1) return x.toFloat()
        
        val left = array[x - 1]
        val center = array[x]
        val right = array[x + 1]
        
        // Calculate the offset for interpolation
        val offset = (left - right) / (2 * (left - 2 * center + right))
        
        return x + offset
    }
    
    /**
     * Optimized version for real-time processing
     */
    fun detectPitchOptimized(audioBuffer: FloatArray, threshold: Float = DEFAULT_THRESHOLD): Float {
        if (audioBuffer.isEmpty()) return 0.0f
        
        // Use a smaller buffer for real-time processing
        val effectiveSize = min(audioBuffer.size, sampleRate / 4) // 250ms window
        val processedBuffer = if (audioBuffer.size > effectiveSize) {
            audioBuffer.sliceArray(0 until effectiveSize)
        } else {
            audioBuffer
        }
        
        // Compute difference function
        val diffFunction = computeDifferenceFunction(processedBuffer)
        
        // Compute cumulative mean normalized difference function
        val cmndf = computeCumulativeMeanNormalizedDifference(diffFunction)
        
        // Find the first minimum after the global maximum
        val tau = findFirstMinimumAfterGlobalMaximum(cmndf, threshold)
        
        if (tau <= 0) return 0.0f
        
        // Perform parabolic interpolation for sub-sample accuracy
        val interpolatedTau = parabolicInterpolation(cmndf, tau)
        
        // Convert to frequency
        val frequency = sampleRate.toFloat() / interpolatedTau
        
        // Validate frequency is within vocal range
        return if (frequency >= MIN_FREQ && frequency <= MAX_FREQ) frequency else 0.0f
    }
}
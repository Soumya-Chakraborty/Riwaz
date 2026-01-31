package com.example.riwaz.utils

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Rhythm and tempo analysis for Indian Classical Music
 * Detects rhythmic patterns and compares with ideal taal structures
 */
class RhythmAnalyzer {
    companion object {
        private const val TAG = "RhythmAnalyzer"
        
        // Common taals and their beat counts
        val TAAL_STRUCTURES = mapOf(
            "Teentaal" to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
            "Jhaptaal" to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
            "Ektaal" to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
            "Rupak" to listOf(1, 2, 3, 4, 5, 6, 7),
            "Deepchandi" to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        )
    }
    
    /**
     * Analyzes rhythm patterns in audio data
     * @param audioData The audio samples to analyze
     * @param sampleRate Sample rate of the audio
     * @return RhythmAnalysisResult with tempo and accuracy data
     */
    fun analyzeRhythm(audioData: FloatArray, sampleRate: Int): RhythmAnalysisResult {
        val onsetDetection = detectOnsets(audioData, sampleRate)
        val tempoEstimate = estimateTempo(onsetDetection, sampleRate)
        val beatAccuracy = measureBeatAccuracy(onsetDetection, tempoEstimate)
        
        return RhythmAnalysisResult(
            tempo = tempoEstimate,
            beatAccuracy = beatAccuracy,
            onsets = onsetDetection
        )
    }
    
    /**
     * Detects onsets (attacks) in the audio signal
     */
    private fun detectOnsets(audioData: FloatArray, sampleRate: Int): List<Long> {
        val frameSize = sampleRate / 10 // 100ms frames
        val hopSize = frameSize / 2
        val energies = mutableListOf<Double>()

        var startIdx = 0
        while (startIdx < audioData.size - frameSize) {
            val frame = audioData.sliceArray(startIdx until (startIdx + frameSize).coerceAtMost(audioData.size))
            val energy = calculateFrameEnergy(frame)
            energies.add(energy)
            startIdx += hopSize
        }

        // Detect peaks in energy to identify onsets
        val onsetFrames = mutableListOf<Int>()
        for (i in 1 until energies.size - 1) {
            if (energies[i] > energies[i-1] && energies[i] > energies[i+1] && energies[i] > 0.1) {
                onsetFrames.add(i)
            }
        }

        // Convert frame indices to time in milliseconds
        return onsetFrames.map { (it * hopSize * 1000L) / sampleRate }
    }
    
    /**
     * Calculates energy of an audio frame
     */
    private fun calculateFrameEnergy(frame: FloatArray): Double {
        var sum = 0.0
        for (sample in frame) {
            sum += (sample * sample).toDouble()
        }
        return sum / frame.size
    }
    
    /**
     * Estimates tempo based on detected onsets
     */
    private fun estimateTempo(onsets: List<Long>, sampleRate: Int): Double {
        if (onsets.size < 2) return 0.0
        
        val intervals = mutableListOf<Long>()
        for (i in 1 until onsets.size) {
            intervals.add(onsets[i] - onsets[i-1])
        }
        
        if (intervals.isEmpty()) return 0.0
        
        // Calculate median interval to reduce outlier effect
        val sortedIntervals = intervals.sorted()
        val medianInterval = if (sortedIntervals.size % 2 == 0) {
            (sortedIntervals[sortedIntervals.size / 2 - 1] + sortedIntervals[sortedIntervals.size / 2]) / 2.0
        } else {
            sortedIntervals[sortedIntervals.size / 2].toDouble()
        }
        
        // Convert to beats per minute (BPM)
        return if (medianInterval > 0) 60000.0 / medianInterval else 0.0
    }
    
    /**
     * Measures how accurately the rhythm matches expected patterns
     */
    private fun measureBeatAccuracy(onsets: List<Long>, estimatedTempo: Double): Float {
        if (estimatedTempo <= 0 || onsets.size < 2) return 0f
        
        // Calculate expected beat times based on tempo
        val beatIntervalMs = (60000.0 / estimatedTempo).toFloat()
        val expectedBeats = mutableListOf<Float>()
        
        var currentTime = onsets[0].toFloat()
        while (currentTime < onsets.last().toFloat()) {
            expectedBeats.add(currentTime)
            currentTime += beatIntervalMs
        }
        
        // Calculate accuracy based on how close actual onsets are to expected beats
        var totalAccuracy = 0f
        var matchedBeats = 0
        
        for (expectedBeat in expectedBeats) {
            val closestOnset = onsets.minByOrNull { abs(it - expectedBeat.toLong()) }
            if (closestOnset != null) {
                val deviation = abs(closestOnset - expectedBeat.toLong())
                // Accuracy decreases with deviation (max accuracy when deviation is 0)
                val accuracy = (1 - (deviation.toFloat() / beatIntervalMs.coerceAtLeast(1f))).coerceIn(0f, 1f)
                totalAccuracy += accuracy
                matchedBeats++
            }
        }
        
        return if (matchedBeats > 0) totalAccuracy / matchedBeats else 0f
    }
    
    /**
     * Compares user rhythm with ideal taal pattern
     */
    fun compareWithTaal(userOnsets: List<Long>, taalName: String): TunalComparisonResult {
        val taalStructure = TAAL_STRUCTURES[taalName] ?: TAAL_STRUCTURES["Teentaal"]!!
        val estimatedTempo = estimateTempo(userOnsets, 44100) // Assuming 44.1kHz
        
        // Calculate expected beat times based on taal structure
        val beatIntervalMs = (60000.0 / estimatedTempo).toFloat()
        val expectedBeatTimes = taalStructure.mapIndexed { index, beat ->
            (index * beatIntervalMs).toLong()
        }
        
        // Match user onsets to expected beats
        val beatMatches = mutableListOf<BeatMatch>()
        for ((index, expectedTime) in expectedBeatTimes.withIndex()) {
            val closestOnset = userOnsets.minByOrNull { abs(it - expectedTime) }
            if (closestOnset != null) {
                val deviation = abs(closestOnset - expectedTime)
                val accuracy = (1 - (deviation.toFloat() / beatIntervalMs.coerceAtLeast(1f))).coerceIn(0f, 1f)
                
                beatMatches.add(
                    BeatMatch(
                        beatNumber = index + 1,
                        expectedTime = expectedTime,
                        actualTime = closestOnset,
                        deviation = deviation,
                        accuracy = accuracy
                    )
                )
            }
        }
        
        // Calculate overall accuracy
        val overallAccuracy = if (beatMatches.isNotEmpty()) {
            (beatMatches.sumOf { it.accuracy.toDouble() } / beatMatches.size).toFloat()
        } else 0f
        
        return TunalComparisonResult(
            taalName = taalName,
            overallAccuracy = overallAccuracy,
            beatMatches = beatMatches
        )
    }
}

/**
 * Data class for rhythm analysis results
 */
data class RhythmAnalysisResult(
    val tempo: Double, // Beats per minute
    val beatAccuracy: Float, // Accuracy of rhythm (0.0 to 1.0)
    val onsets: List<Long> // Times of detected onsets in milliseconds
)

/**
 * Data class for taal comparison results
 */
data class TunalComparisonResult(
    val taalName: String,
    val overallAccuracy: Float,
    val beatMatches: List<BeatMatch>
)

/**
 * Data class for individual beat match
 */
data class BeatMatch(
    val beatNumber: Int,
    val expectedTime: Long,
    val actualTime: Long,
    val deviation: Long,
    val accuracy: Float
)
package com.example.riwaz.utils

import kotlin.math.*

/**
 * State-of-the-art Gamaka (Indian Classical Music Ornament) Detector
 * 
 * Detects the following ornaments:
 * - Meend: Gliding between notes (pitch portamento)
 * - Andolan: Slow oscillation on a note
 * - Kampan: Fast vibrato-like oscillation
 * - Sparsh/Kan Swar: Grace notes (quick touches)
 * - Murki: Cluster of quick notes
 * - Zamzama: Extended murki with more notes
 * 
 * Based on research in computational analysis of Indian classical music
 */
class GamakaDetector(private val sampleRate: Int) {
    
    companion object {
        // Thresholds for ornament detection
        private const val MEEND_MIN_DURATION_MS = 100   // Minimum meend duration
        private const val MEEND_MIN_CENTS = 50          // Minimum pitch change for meend
        private const val MEEND_MAX_RATE = 200f         // Max cents/second for meend (slower than vibrato)
        
        private const val ANDOLAN_MIN_CYCLES = 2        // Minimum oscillation cycles
        private const val ANDOLAN_MAX_RATE = 4f         // Hz - slow oscillation
        private const val ANDOLAN_MIN_EXTENT = 20       // Cents
        private const val ANDOLAN_MAX_EXTENT = 100      // Cents
        
        private const val KAMPAN_MIN_RATE = 4f          // Hz
        private const val KAMPAN_MAX_RATE = 8f          // Hz
        private const val KAMPAN_MIN_EXTENT = 15        // Cents
        private const val KAMPAN_MAX_EXTENT = 80        // Cents
        
        private const val GRACE_NOTE_MAX_DURATION_MS = 80   // Quick touch
        private const val GRACE_NOTE_MIN_CENTS = 100        // Significant pitch jump
        
        private const val MURKI_MAX_NOTE_DURATION_MS = 150  // Quick notes in murki
        private const val MURKI_MIN_NOTES = 3               // At least 3 notes
    }
    
    private val pYinDetector = PYinPitchDetector(sampleRate)
    
    /**
     * Analyze audio for all types of gamakas
     */
    fun analyzeGamakas(audioData: FloatArray): GamakaAnalysisResult {
        val frameSize = sampleRate / 20  // 50ms frames for good temporal resolution
        val hopSize = frameSize / 4       // 75% overlap
        
        // Get detailed pitch sequence
        val pitchSequence = pYinDetector.detectPitchSequence(audioData, frameSize, hopSize)
        
        if (pitchSequence.isEmpty()) {
            return GamakaAnalysisResult(
                meends = emptyList(),
                andolans = emptyList(),
                kampans = emptyList(),
                graceNotes = emptyList(),
                murkis = emptyList(),
                overallOrnamentation = 0f
            )
        }
        
        val frameDurationMs = (hopSize * 1000f / sampleRate)
        
        // Detect each type of ornament
        val meends = detectMeends(pitchSequence, frameDurationMs)
        val andolans = detectAndolans(pitchSequence, frameDurationMs)
        val kampans = detectKampans(pitchSequence, frameDurationMs)
        val graceNotes = detectGraceNotes(pitchSequence, frameDurationMs)
        val murkis = detectMurkis(pitchSequence, frameDurationMs)
        
        // Calculate overall ornamentation score
        val totalFrames = pitchSequence.size
        val ornamentedFrames = calculateOrnamentedFrames(
            meends, andolans, kampans, graceNotes, murkis, totalFrames
        )
        val overallOrnamentation = ornamentedFrames.toFloat() / totalFrames.coerceAtLeast(1)
        
        return GamakaAnalysisResult(
            meends = meends,
            andolans = andolans,
            kampans = kampans,
            graceNotes = graceNotes,
            murkis = murkis,
            overallOrnamentation = overallOrnamentation
        )
    }
    
    /**
     * Detect Meend (pitch glide between notes)
     */
    private fun detectMeends(
        pitchSequence: List<PitchResult>,
        frameDurationMs: Float
    ): List<MeendInfo> {
        val meends = mutableListOf<MeendInfo>()
        
        var i = 0
        while (i < pitchSequence.size - 1) {
            val startPitch = pitchSequence[i]
            
            // Skip unvoiced frames
            if (startPitch.frequency <= 0) {
                i++
                continue
            }
            
            // Look for continuous pitch movement
            var j = i + 1
            var direction = 0 // 1 = ascending, -1 = descending
            var totalCents = 0f
            
            while (j < pitchSequence.size) {
                val currPitch = pitchSequence[j]
                
                if (currPitch.frequency <= 0) break
                
                val cents = frequencyToCents(startPitch.frequency, currPitch.frequency)
                val frameDirection = when {
                    cents > 10 -> 1
                    cents < -10 -> -1
                    else -> 0
                }
                
                // Check if direction is consistent
                if (direction == 0) {
                    direction = frameDirection
                } else if (frameDirection != 0 && frameDirection != direction) {
                    break // Direction changed
                }
                
                totalCents = cents
                
                // Check rate (should be gradual for meend)
                val durationMs = (j - i) * frameDurationMs
                val rate = abs(cents) / (durationMs / 1000f) // cents per second
                
                if (rate > MEEND_MAX_RATE) break // Too fast for meend
                
                j++
            }
            
            // Check if this qualifies as a meend
            val durationMs = (j - i) * frameDurationMs
            if (durationMs >= MEEND_MIN_DURATION_MS && abs(totalCents) >= MEEND_MIN_CENTS) {
                val endPitch = pitchSequence[j - 1]
                meends.add(MeendInfo(
                    startFrame = i,
                    endFrame = j - 1,
                    startFrequency = startPitch.frequency,
                    endFrequency = endPitch.frequency,
                    durationMs = durationMs,
                    cents = totalCents,
                    isAscending = direction > 0
                ))
                i = j
            } else {
                i++
            }
        }
        
        return meends
    }
    
    /**
     * Detect Andolan (slow oscillation)
     */
    private fun detectAndolans(
        pitchSequence: List<PitchResult>,
        frameDurationMs: Float
    ): List<AndolanInfo> {
        return detectOscillation(
            pitchSequence, frameDurationMs,
            minRate = 0.5f,
            maxRate = ANDOLAN_MAX_RATE,
            minExtent = ANDOLAN_MIN_EXTENT,
            maxExtent = ANDOLAN_MAX_EXTENT,
            minCycles = ANDOLAN_MIN_CYCLES
        ).map { osc ->
            AndolanInfo(
                startFrame = osc.startFrame,
                endFrame = osc.endFrame,
                centerFrequency = osc.centerFrequency,
                rate = osc.rate,
                extent = osc.extent,
                numCycles = osc.numCycles
            )
        }
    }
    
    /**
     * Detect Kampan (fast vibrato-like oscillation)
     */
    private fun detectKampans(
        pitchSequence: List<PitchResult>,
        frameDurationMs: Float
    ): List<KampanInfo> {
        return detectOscillation(
            pitchSequence, frameDurationMs,
            minRate = KAMPAN_MIN_RATE,
            maxRate = KAMPAN_MAX_RATE,
            minExtent = KAMPAN_MIN_EXTENT,
            maxExtent = KAMPAN_MAX_EXTENT,
            minCycles = 2
        ).map { osc ->
            KampanInfo(
                startFrame = osc.startFrame,
                endFrame = osc.endFrame,
                centerFrequency = osc.centerFrequency,
                rate = osc.rate,
                extent = osc.extent
            )
        }
    }
    
    /**
     * Generic oscillation detection for both Andolan and Kampan
     */
    private fun detectOscillation(
        pitchSequence: List<PitchResult>,
        frameDurationMs: Float,
        minRate: Float,
        maxRate: Float,
        minExtent: Int,
        maxExtent: Int,
        minCycles: Int
    ): List<OscillationInfo> {
        val oscillations = mutableListOf<OscillationInfo>()
        
        // Convert pitch sequence to cents relative to local mean
        val voicedIndices = pitchSequence.indices.filter { pitchSequence[it].frequency > 0 }
        if (voicedIndices.size < 10) return oscillations
        
        var i = 0
        while (i < voicedIndices.size - minCycles * 4) {
            val windowSize = 20 // Analyze 20 frames at a time
            val endIdx = minOf(i + windowSize, voicedIndices.size)
            
            val windowIndices = voicedIndices.subList(i, endIdx)
            val windowPitches = windowIndices.map { pitchSequence[it].frequency }
            
            if (windowPitches.size < 4) {
                i++
                continue
            }
            
            val centerFreq = windowPitches.average().toFloat()
            val centsValues = windowPitches.map { frequencyToCents(centerFreq, it) }
            
            // Detect oscillation by counting zero crossings
            var zeroCrossings = 0
            for (j in 1 until centsValues.size) {
                if (centsValues[j - 1] * centsValues[j] < 0) {
                    zeroCrossings++
                }
            }
            
            val durationSec = (endIdx - i) * frameDurationMs / 1000f
            val rate = zeroCrossings / (2 * durationSec) // Hz (2 crossings per cycle)
            val extent = (centsValues.maxOrNull() ?: 0f) - (centsValues.minOrNull() ?: 0f)
            
            if (rate in minRate..maxRate && extent in minExtent.toFloat()..maxExtent.toFloat()) {
                val numCycles = (zeroCrossings / 2)
                if (numCycles >= minCycles) {
                    oscillations.add(OscillationInfo(
                        startFrame = windowIndices.first(),
                        endFrame = windowIndices.last(),
                        centerFrequency = centerFreq,
                        rate = rate,
                        extent = extent,
                        numCycles = numCycles
                    ))
                    i = endIdx
                    continue
                }
            }
            
            i++
        }
        
        return oscillations
    }
    
    /**
     * Detect Grace Notes (quick pitch touches)
     */
    private fun detectGraceNotes(
        pitchSequence: List<PitchResult>,
        frameDurationMs: Float
    ): List<GraceNoteInfo> {
        val graceNotes = mutableListOf<GraceNoteInfo>()
        
        val maxFrames = (GRACE_NOTE_MAX_DURATION_MS / frameDurationMs).toInt().coerceAtLeast(1)
        
        var i = 1
        while (i < pitchSequence.size - 1) {
            val prev = pitchSequence[i - 1]
            val curr = pitchSequence[i]
            val next = pitchSequence[i + 1]
            
            // Look for quick jump and return
            if (prev.frequency > 0 && curr.frequency > 0 && next.frequency > 0) {
                val cents1 = abs(frequencyToCents(prev.frequency, curr.frequency))
                val cents2 = abs(frequencyToCents(curr.frequency, next.frequency))
                
                // Quick excursion that returns
                if (cents1 >= GRACE_NOTE_MIN_CENTS && cents2 >= GRACE_NOTE_MIN_CENTS) {
                    val returnsCents = abs(frequencyToCents(prev.frequency, next.frequency))
                    
                    // Check if it returns close to original pitch
                    if (returnsCents < 50) {
                        graceNotes.add(GraceNoteInfo(
                            frame = i,
                            mainFrequency = prev.frequency,
                            graceFrequency = curr.frequency,
                            cents = cents1,
                            isUpper = curr.frequency > prev.frequency
                        ))
                    }
                }
            }
            
            i++
        }
        
        return graceNotes
    }
    
    /**
     * Detect Murki (cluster of quick notes)
     */
    private fun detectMurkis(
        pitchSequence: List<PitchResult>,
        frameDurationMs: Float
    ): List<MurkiInfo> {
        val murkis = mutableListOf<MurkiInfo>()
        
        val maxFramesPerNote = (MURKI_MAX_NOTE_DURATION_MS / frameDurationMs).toInt().coerceAtLeast(1)
        
        var i = 0
        while (i < pitchSequence.size - MURKI_MIN_NOTES) {
            // Look for sequence of quick pitch changes
            var noteCount = 1
            var j = i + 1
            var lastNoteFrame = i
            
            while (j < pitchSequence.size && j - lastNoteFrame <= maxFramesPerNote * 2) {
                val prev = pitchSequence[j - 1]
                val curr = pitchSequence[j]
                
                if (prev.frequency > 0 && curr.frequency > 0) {
                    val cents = abs(frequencyToCents(prev.frequency, curr.frequency))
                    
                    // Significant pitch change = new note
                    if (cents > 50) {
                        noteCount++
                        lastNoteFrame = j
                    }
                }
                
                j++
            }
            
            if (noteCount >= MURKI_MIN_NOTES) {
                val notes = mutableListOf<Float>()
                for (k in i until j) {
                    if (pitchSequence[k].frequency > 0) {
                        notes.add(pitchSequence[k].frequency)
                    }
                }
                
                val durationMs = (j - i) * frameDurationMs
                murkis.add(MurkiInfo(
                    startFrame = i,
                    endFrame = j - 1,
                    noteCount = noteCount,
                    durationMs = durationMs,
                    averageNoteSpeed = noteCount / (durationMs / 1000f)
                ))
                i = j
            } else {
                i++
            }
        }
        
        return murkis
    }
    
    /**
     * Calculate total ornamented frames
     */
    private fun calculateOrnamentedFrames(
        meends: List<MeendInfo>,
        andolans: List<AndolanInfo>,
        kampans: List<KampanInfo>,
        graceNotes: List<GraceNoteInfo>,
        murkis: List<MurkiInfo>,
        totalFrames: Int
    ): Int {
        val ornamentedFrameSet = mutableSetOf<Int>()
        
        meends.forEach { for (f in it.startFrame..it.endFrame) ornamentedFrameSet.add(f) }
        andolans.forEach { for (f in it.startFrame..it.endFrame) ornamentedFrameSet.add(f) }
        kampans.forEach { for (f in it.startFrame..it.endFrame) ornamentedFrameSet.add(f) }
        graceNotes.forEach { ornamentedFrameSet.add(it.frame) }
        murkis.forEach { for (f in it.startFrame..it.endFrame) ornamentedFrameSet.add(f) }
        
        return ornamentedFrameSet.size
    }
    
    /**
     * Convert frequency ratio to cents
     */
    private fun frequencyToCents(refFreq: Float, freq: Float): Float {
        if (refFreq <= 0 || freq <= 0) return 0f
        return (1200 * ln(freq.toDouble() / refFreq) / ln(2.0)).toFloat()
    }
}

// ==================== Data Classes ====================

data class GamakaAnalysisResult(
    val meends: List<MeendInfo>,
    val andolans: List<AndolanInfo>,
    val kampans: List<KampanInfo>,
    val graceNotes: List<GraceNoteInfo>,
    val murkis: List<MurkiInfo>,
    val overallOrnamentation: Float
)

data class MeendInfo(
    val startFrame: Int,
    val endFrame: Int,
    val startFrequency: Float,
    val endFrequency: Float,
    val durationMs: Float,
    val cents: Float,
    val isAscending: Boolean
)

data class AndolanInfo(
    val startFrame: Int,
    val endFrame: Int,
    val centerFrequency: Float,
    val rate: Float,       // Hz
    val extent: Float,     // cents
    val numCycles: Int
)

data class KampanInfo(
    val startFrame: Int,
    val endFrame: Int,
    val centerFrequency: Float,
    val rate: Float,       // Hz
    val extent: Float      // cents
)

data class GraceNoteInfo(
    val frame: Int,
    val mainFrequency: Float,
    val graceFrequency: Float,
    val cents: Float,
    val isUpper: Boolean
)

data class MurkiInfo(
    val startFrame: Int,
    val endFrame: Int,
    val noteCount: Int,
    val durationMs: Float,
    val averageNoteSpeed: Float  // notes per second
)

private data class OscillationInfo(
    val startFrame: Int,
    val endFrame: Int,
    val centerFrequency: Float,
    val rate: Float,
    val extent: Float,
    val numCycles: Int
)

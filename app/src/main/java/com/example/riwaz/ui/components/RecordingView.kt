package com.example.riwaz.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riwaz.utils.AdaptiveAnalyzer
import com.example.riwaz.utils.RealTimeAnalysisResult
import com.example.riwaz.ui.theme.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.PI

// Default tonic frequency (C4 = 261.63 Hz, can be customized)
private const val DEFAULT_TONIC_FREQUENCY = 261.63f
private const val SAMPLE_RATE = 44100

@Composable
fun RecordingView(
    amplitudes: List<Float>,
    raga: String,
    practiceType: String,
    tempo: String,
    saffronColor: Color,
    tonicFrequency: Float = DEFAULT_TONIC_FREQUENCY,
    audioBuffer: FloatArray? = null,  // Real audio buffer for SOTA analysis
    onRealTimeFeedback: (currentPitch: Float, currentSwar: String, pitchAccuracy: Float) -> Unit = { _, _, _ -> }
) {
    var millisElapsed by remember { mutableLongStateOf(0L) }

    // State for real-time pitch feedback
    var currentPitch by remember { mutableFloatStateOf(0f) }
    var currentSwar by remember { mutableStateOf("Silence") }
    var pitchAccuracy by remember { mutableFloatStateOf(0f) }
    var deviationCents by remember { mutableFloatStateOf(0f) }
    var inRaga by remember { mutableStateOf(true) }
    var suggestion by remember { mutableStateOf("") }
    
    // SOTA Adaptive Analyzer
    val adaptiveAnalyzer = remember { AdaptiveAnalyzer(SAMPLE_RATE) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (true) {
            millisElapsed = System.currentTimeMillis() - startTime
            delay(50)
        }
    }

    // SOTA Real-time pitch analysis using AdaptiveAnalyzer
    LaunchedEffect(audioBuffer, amplitudes) {
        if (audioBuffer != null && audioBuffer.isNotEmpty()) {
            // Use SOTA adaptive analyzer for real-time feedback
            val result = adaptiveAnalyzer.analyzeRealTime(
                audioBuffer = audioBuffer,
                tonicFrequency = tonicFrequency,
                raga = raga
            )
            
            currentPitch = result.pitch
            currentSwar = if (result.swar.isNotEmpty()) result.swar else "Silence"
            pitchAccuracy = result.accuracy
            deviationCents = result.deviationCents
            inRaga = result.inRaga
            suggestion = result.suggestion
            
            onRealTimeFeedback(currentPitch, currentSwar, pitchAccuracy)
        } else if (amplitudes.isNotEmpty()) {
            // Fallback: estimate from amplitudes when no audio buffer available
            val avgAmplitude = amplitudes.average().toFloat()
            if (avgAmplitude > 0.1f) {
                // Use amplitude-based estimation (less accurate but still functional)
                currentSwar = estimateSwarFromAmplitude(amplitudes, raga)
                pitchAccuracy = 0.6f + (avgAmplitude * 0.3f).coerceAtMost(0.3f)
                inRaga = true
                suggestion = ""
                
                onRealTimeFeedback(0f, currentSwar, pitchAccuracy)
            } else {
                currentPitch = 0f
                currentSwar = "Silence"
                pitchAccuracy = 0f
                inRaga = true
                suggestion = ""
                
                onRealTimeFeedback(0f, currentSwar, 0f)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        // Recording indicator with enhanced animation
        RecordingIndicator(
            saffronColor = saffronColor,
            pulseAlpha = pulseAlpha
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Raga title with entrance animation
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = raga,
                color = saffronColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Practice type and tempo with delayed animation
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(delayMillis = 100)) + fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "$practiceType • $tempo",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-time pitch feedback with smooth updates
        AnimatedContent(
            targetState = Triple(currentPitch, currentSwar, pitchAccuracy),
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 300)) + scaleIn(initialScale = 0.9f) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 300))
            },
            label = "pitch-feedback-animation"
        ) { (pitch, swar, accuracy) ->
            PitchFeedbackView(
                currentPitch = pitch,
                currentSwar = swar,
                pitchAccuracy = accuracy,
                saffronColor = saffronColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animated timer with smooth transitions
        val totalSeconds = (millisElapsed / 1000).toInt()
        val displayMillis = ((millisElapsed % 1000) / 10).toInt()

        AnimatedContent(
            targetState = formatTimeWithMillis(totalSeconds, displayMillis),
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 200)) + slideInVertically() togetherWith
                fadeOut(animationSpec = tween(durationMillis = 200)) + slideOutVertically()
            },
            label = "timer-animation"
        ) { timerText ->
            Text(
                text = timerText,
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.5).sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Animated waveform visualization with improved design
        AnimatedWaveform(
            amplitudes = amplitudes,
            saffronColor = saffronColor
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RecordingIndicator(saffronColor: Color, pulseAlpha: Float) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(saffronColor)
            .alpha(pulseAlpha)
    )
}

@Composable
private fun AnimatedWaveform(
    amplitudes: List<Float>,
    saffronColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_painter")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Draw background grid lines for better visual reference
        drawLine(
            color = iOSSoftGray,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )

        val path = Path().apply {
            moveTo(0f, centerY)

            val stepSize = if (amplitudes.isNotEmpty()) width / amplitudes.size else width / 10f
            var currentX = 0f

            amplitudes.forEach { amplitude ->
                val normalizedAmplitude = amplitude.coerceIn(0f, 1f)
                val y = centerY - (normalizedAmplitude * centerY * 0.8f)
                lineTo(currentX, y)
                currentX += stepSize
            }

            // Add some animated sine wave effect
            val animatedPoints = 20
            val animatedStep = width / animatedPoints
            for (i in 0..animatedPoints) {
                val x = i * animatedStep
                val y = centerY + sin(((x / width) * 2 * PI + waveOffset * 2 * PI).toFloat()) * 10f
                lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = saffronColor,
            style = Stroke(width = 4f)
        )

        // Draw gradient fill under the waveform
        val fillPath = Path().apply {
            moveTo(0f, centerY)
            addPath(path)
            lineTo(width, centerY)
            close()
        }

        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    saffronColor.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                startY = centerY - 40f,
                endY = centerY + 40f
            )
        )
    }
}

fun formatTimeWithMillis(seconds: Int, millis: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d.%02d", m, s, millis)
}

/**
 * Helper function to get the closest swar to a given pitch
 */
fun getClosestSwar(pitch: Float): String {
    val swarFrequencies = mapOf(
        "Sa" to 261.63f,
        "Re(k)" to 275.71f,
        "Re" to 293.66f,
        "Ga(k)" to 309.23f,
        "Ga" to 329.63f,
        "Ma" to 349.23f,
        "Ma(t)" to 370.79f,
        "Pa" to 392.00f,
        "Dha(k)" to 413.41f,
        "Dha" to 440.00f,
        "Ni(k)" to 466.16f,
        "Ni" to 493.88f
    )

    return swarFrequencies.minByOrNull { (_, freq) -> kotlin.math.abs(freq - pitch) }?.key ?: "Unknown"
}

/**
 * Helper function to calculate swar accuracy based on raga
 */
fun calculateSwarAccuracy(pitch: Float, raga: String): Float {
    val closestSwar = getClosestSwar(pitch)

    // Define raga-specific swars
    val ragaSwars = when (raga) {
        "Yaman" -> listOf("Sa", "Re", "Ga", "Ma(t)", "Pa", "Dha", "Ni")
        "Bhairav" -> listOf("Sa", "Re(k)", "Ga", "Ma", "Pa", "Dha(k)", "Ni")
        "Todi" -> listOf("Sa", "Re(k)", "Ga(k)", "Ma(t)", "Pa", "Dha(k)", "Ni")
        "Malkauns" -> listOf("Sa", "Ga(k)", "Ma", "Dha(k)", "Ni(k)")
        "Bhupali" -> listOf("Sa", "Re", "Ga", "Pa", "Dha")
        else -> listOf("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni")
    }

    // Calculate accuracy based on whether the detected swar is in the raga
    return if (ragaSwars.contains(closestSwar)) {
        // Further accuracy based on how close the pitch is to the expected frequency
        val expectedFreq = when (closestSwar) {
            "Sa" -> 261.63f
            "Re(k)" -> 275.71f
            "Re" -> 293.66f
            "Ga(k)" -> 309.23f
            "Ga" -> 329.63f
            "Ma" -> 349.23f
            "Ma(t)" -> 370.79f
            "Pa" -> 392.00f
            "Dha(k)" -> 413.41f
            "Dha" -> 440.00f
            "Ni(k)" -> 466.16f
            "Ni" -> 493.88f
            else -> 261.63f
        }

        val deviation = kotlin.math.abs(expectedFreq - pitch) / expectedFreq
        (1 - deviation * 2).coerceIn(0f, 1f) // Max 50% deviation allowed
    } else {
        0.1f // Very low accuracy if swar is not in raga
    }
}

@Composable
fun PitchFeedbackView(
    currentPitch: Float,
    currentSwar: String,
    pitchAccuracy: Float,
    saffronColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = iOSMediumGray
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Current Note Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Note:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = currentSwar,
                    color = when {
                        pitchAccuracy > 0.8f -> saffronColor
                        pitchAccuracy > 0.6f -> AmberWarning
                        pitchAccuracy > 0f -> RedError
                        else -> Color.Gray
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pitch Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pitch:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (currentPitch > 0) "${currentPitch.toInt()} Hz" else "Silence",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Accuracy Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accuracy:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (pitchAccuracy > 0) "${(pitchAccuracy * 100).toInt()}%" else "-",
                    color = when {
                        pitchAccuracy > 0.8f -> saffronColor
                        pitchAccuracy > 0.6f -> AmberWarning
                        pitchAccuracy > 0f -> RedError
                        else -> Color.Gray
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated accuracy indicator bar with improved styling
            Text(
                text = "Precision",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(iOSSoftGray)
            ) {
                if (pitchAccuracy > 0) {
                    AnimatedProgressBar(
                        progress = pitchAccuracy.coerceIn(0f, 1f),
                        color = when {
                            pitchAccuracy > 0.8f -> saffronColor
                            pitchAccuracy > 0.6f -> AmberWarning
                            else -> RedError
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedProgressBar(progress: Float, color: Color) {
    val progressAnimation by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progress_bar_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(progressAnimation)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

/**
 * Fallback: Estimate swar from amplitude patterns when real audio buffer isn't available
 * This provides basic feedback based on amplitude envelope analysis
 */
private fun estimateSwarFromAmplitude(amplitudes: List<Float>, raga: String): String {
    if (amplitudes.isEmpty()) return "Silence"
    
    val avg = amplitudes.average().toFloat()
    val max = amplitudes.maxOrNull() ?: 0f
    
    // Very basic heuristic - in practice would need real pitch detection
    // Higher amplitudes often correlate with higher pitches due to voice effort
    val ragaNotes = getRagaNotesSimple(raga)
    
    return when {
        avg < 0.1f -> "Silence"
        avg < 0.3f -> ragaNotes.getOrNull(0) ?: "Sa"
        avg < 0.5f -> ragaNotes.getOrNull(2) ?: "Ga"
        avg < 0.7f -> ragaNotes.getOrNull(4) ?: "Pa"
        else -> ragaNotes.getOrNull(5) ?: "Dha"
    }
}

/**
 * Get basic notes for a raga for fallback display
 */
private fun getRagaNotesSimple(raga: String): List<String> {
    return when (raga) {
        "Yaman" -> listOf("Sa", "Re", "Ga", "Ma(t)", "Pa", "Dha", "Ni")
        "Bhairav" -> listOf("Sa", "Re(k)", "Ga", "Ma", "Pa", "Dha(k)", "Ni")
        "Todi" -> listOf("Sa", "Re(k)", "Ga(k)", "Ma(t)", "Pa", "Dha(k)", "Ni")
        "Malkauns" -> listOf("Sa", "Ga(k)", "Ma", "Dha(k)", "Ni(k)")
        "Darbari" -> listOf("Sa", "Re", "Ga(k)", "Ma", "Pa", "Dha(k)", "Ni(k)")
        else -> listOf("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni")
    }
}
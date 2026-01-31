package com.example.riwaz.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riwaz.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * Premium Recording View - Clean, focused recording interface
 * Features:
 * - Animated waveform with glow effects
 * - Large timer display
 * - Raga and session info header
 * - Clean design without distracting real-time detection
 */
@Composable
fun PremiumRecordingView(
    amplitudes: List<Float>,
    raga: String,
    practiceType: String,
    tempo: String,
    saffronColor: Color,
    tonicFrequency: Float = 261.63f,
    audioBuffer: FloatArray? = null,
    onRealTimeFeedback: (currentPitch: Float, currentSwar: String, pitchAccuracy: Float) -> Unit = { _, _, _ -> }
) {
    var millisElapsed by remember { mutableLongStateOf(0L) }
    
    // Timer
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (true) {
            millisElapsed = System.currentTimeMillis() - startTime
            delay(50)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        iOSBlack,
                        Color(0xFF0A0A0A),
                        iOSDarkGray.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        // Background glow effect
        BackgroundGlow(saffronColor = saffronColor)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with Raga info
            PremiumRecordingHeader(
                raga = raga,
                practiceType = practiceType,
                tempo = tempo,
                saffronColor = saffronColor
            )
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Large Timer Display - Central focus
            PremiumTimer(millisElapsed = millisElapsed, saffronColor = saffronColor)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recording Status Indicator
            RecordingStatusIndicator(saffronColor = saffronColor)
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Waveform visualization
            PremiumWaveform(
                amplitudes = amplitudes,
                saffronColor = saffronColor
            )
            
            Spacer(modifier = Modifier.height(100.dp)) // Space for bottom button
        }
    }
}

@Composable
private fun RecordingStatusIndicator(saffronColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            RedError.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(RedError, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            "Recording in progress...",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            "Tap stop when finished",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun BackgroundGlow(saffronColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(glowScale)
            .blur(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = 100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            saffronColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun PremiumRecordingHeader(
    raga: String,
    practiceType: String,
    tempo: String,
    saffronColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "indicator")
    val indicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Recording indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = RedError.copy(alpha = indicatorAlpha),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "RECORDING",
                color = RedError,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        
        // Raga badge
        Surface(
            color = saffronColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, saffronColor.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "राग",
                    color = saffronColor.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    raga,
                    color = saffronColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Practice type row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            practiceType,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
        Text(
            " • ",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 14.sp
        )
        Text(
            tempo,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PremiumTimer(millisElapsed: Long, saffronColor: Color) {
    val totalSeconds = (millisElapsed / 1000).toInt()
    val displayMillis = ((millisElapsed % 1000) / 10).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        // Minutes
        Text(
            String.format("%02d", minutes),
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-2).sp
        )
        Text(
            ":",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 72.sp,
            fontWeight = FontWeight.Thin,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        // Seconds
        Text(
            String.format("%02d", seconds),
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-2).sp
        )
        // Milliseconds
        Text(
            ".${String.format("%02d", displayMillis)}",
            color = saffronColor.copy(alpha = 0.6f),
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(bottom = 14.dp)
        )
    }
}

@Composable
private fun PremiumWaveform(
    amplitudes: List<Float>,
    saffronColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(iOSMediumGray.copy(alpha = 0.3f))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barSpacing = 3f
        
        // Draw amplitude bars with gradient
        if (amplitudes.isNotEmpty()) {
            amplitudes.forEachIndexed { index, amplitude ->
                val x = index * (width / amplitudes.size)
                val barHeight = amplitude.coerceIn(0f, 1f) * height * 0.85f
                
                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        saffronColor,
                        saffronColor.copy(alpha = 0.3f)
                    ),
                    startY = centerY - barHeight / 2,
                    endY = centerY + barHeight / 2
                )
                
                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset(x, centerY - barHeight / 2),
                    size = androidx.compose.ui.geometry.Size(
                        (width / amplitudes.size) - barSpacing,
                        barHeight.coerceAtLeast(4f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        } else {
            // Animated idle wave
            val path = Path()
            path.moveTo(0f, centerY)
            
            for (x in 0..width.toInt() step 4) {
                val y = centerY + sin(x / 30f + wavePhase) * 20f
                path.lineTo(x.toFloat(), y)
            }
            
            drawPath(
                path = path,
                color = saffronColor.copy(alpha = 0.4f),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }
        
        // Center line
        drawLine(
            color = saffronColor.copy(alpha = 0.15f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )
    }
}


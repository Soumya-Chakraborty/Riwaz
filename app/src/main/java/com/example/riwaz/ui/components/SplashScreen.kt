package com.example.riwaz.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.example.riwaz.ui.theme.*

@Composable
fun RiwazSplashScreen(saffronColor: Color) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    // Animation for the floating particles
    val particleAnimations = List(8) { index ->
        remember { Animatable(0f) }.apply {
            LaunchedEffect(Unit) {
                animateTo(
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 3000 + (index * 200), easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        // Animate the main elements with staggered delays
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        kotlinx.coroutines.delay(200)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )

        // Animate rotation for a subtle effect
        rotation.animateTo(
            targetValue = 10f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        rotation.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E), // Deep navy blue
                        Color(0xFF16213E), // Dark blue
                        Color(0xFF0F3460), // Rich blue
                        iOSBlack
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background particles
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            particleAnimations.forEachIndexed { index, animatable ->
                val angle = (360f / particleAnimations.size) * index
                val radius = 100 * 3.5f // Approximate conversion for screen density

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    val particleX = centerX + radius * kotlin.math.cos((animatable.value * Math.PI / 180).toDouble()).toFloat()
                    val particleY = centerY + radius * kotlin.math.sin((animatable.value * Math.PI / 180).toDouble()).toFloat()

                    drawCircle(
                        color = saffronColor.copy(alpha = 0.15f),
                        radius = 8f,
                        center = Offset(particleX, particleY)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated music note with rotation
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .rotate(rotation.value)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                saffronColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎵",
                    fontSize = 56.sp,
                    modifier = Modifier.alpha(alpha.value)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated main title with wavy effect
            AnimatedTitle(
                text = "रियाज़",
                saffronColor = saffronColor,
                fontSize = 44.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Animated subtitle
            AnimatedTitle(
                text = "Riwaz",
                saffronColor = Color.White.copy(alpha = 0.8f),
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Animated loading dots with wave effect
            LoadingDots(saffronColor = saffronColor)
        }
    }
}

@Composable
private fun AnimatedTitle(text: String, saffronColor: Color, fontSize: TextUnit) {
    val infiniteTransition = rememberInfiniteTransition(label = "title_wave")
    val offsets = List(text.length) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "letter_offset_${index}"
        )
    }

    Row(horizontalArrangement = Arrangement.Center) {
        text.forEachIndexed { index, char ->
            Text(
                text = char.toString(),
                color = saffronColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = (offsets[index].value * 8f).dp)
            )
        }
    }
}

@Composable
private fun LoadingDots(saffronColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")

    val dotAnimations = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0.3f at 100 using LinearEasing
                    1f at 300 using LinearEasing
                    0.3f at 600
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_animation_${index}"
        ).apply {
            // Add staggered delay
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay((index * 200).toLong())
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotAnimations.forEachIndexed { index, animValue ->
            Box(
                modifier = Modifier
                    .size((8f + (animValue.value * 4f)).dp)
                    .clip(CircleShape)
                    .background(saffronColor)
            )
        }
    }
}
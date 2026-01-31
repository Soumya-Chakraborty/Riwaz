package com.example.riwaz.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riwaz.R
import com.example.riwaz.models.PracticeSession
import com.example.riwaz.ui.theme.*
import com.example.riwaz.viewmodel.RecordingViewModel

/**
 * Recording button with pulsating animation
 */
@Composable
fun RecordingButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    saffronColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by if (isRecording) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(80.dp)
            .alpha(pulseAlpha),
        containerColor = if (isRecording) iOSRed else saffronColor,
        contentColor = Color.White,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
            )
        } else {
            Text("🎵", fontSize = 40.sp)
        }
    }
}

@Composable
fun RecorderApp(
    recordings: List<PracticeSession>,
    isRecording: Boolean,
    playingPath: String?,
    playbackProgress: Float,
    amplitudes: List<Float>,
    selectedRaga: String,
    selectedPracticeType: String,
    selectedTempo: String,
    saffronColor: Color,
    recordingViewModel: RecordingViewModel,
    onRecordToggle: () -> Unit,
    onPlayToggle: (String) -> Unit,
    onDeleteRecording: (PracticeSession) -> Unit,
    onAnalyzeRecording: (PracticeSession) -> Unit
) {
    Scaffold(
        bottomBar = {
            // Always show the recording button (start or stop)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                RecordingButton(
                    isRecording = isRecording,
                    onClick = onRecordToggle,
                    saffronColor = saffronColor,
                    modifier = Modifier.size(74.dp)
                )
            }
        },
        containerColor = iOSBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 200))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "रियाज़",
                        color = saffronColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.app_copyright),
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = isRecording,
                transitionSpec = {
                    if (targetState) {
                        // Entering recording view
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                        // Exiting recordings view
                        slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(durationMillis = 300))
                    } else {
                        // Entering recordings view
                        slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                        // Exiting recording view
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(durationMillis = 300))
                    }
                },
                label = "recording-state-transition"
            ) { recording ->
                if (recording) {
                    PremiumRecordingView(
                        amplitudes = amplitudes,
                        raga = selectedRaga,
                        practiceType = selectedPracticeType,
                        tempo = selectedTempo,
                        saffronColor = saffronColor,
                        onRealTimeFeedback = { currentPitch, currentSwar, pitchAccuracy ->
                            recordingViewModel.updateRealTimeFeedback(currentPitch, currentSwar, pitchAccuracy)
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recordings) { session ->
                            RecordingItem(
                                session = session,
                                isPlaying = playingPath == session.file.absolutePath,
                                progress = if (playingPath == session.file.absolutePath) playbackProgress else 0f,
                                saffronColor = saffronColor,
                                onPlayToggle = { onPlayToggle(session.file.absolutePath) },
                                onDelete = { onDeleteRecording(session) },
                                onAnalyze = { onAnalyzeRecording(session) }
                            )
                        }
                    }
                }
            }
        }
    }
}
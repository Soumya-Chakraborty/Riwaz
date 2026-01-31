package com.example.riwaz.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import com.example.riwaz.models.PracticeSession
import com.example.riwaz.ui.theme.*
import com.example.riwaz.viewmodel.AnalysisViewModel

/**
 * Main Analysis Screen that provides deep insights into a practice session.
 * It coordinates data calculation, scale selection, and the overall UI layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisView(
    session: PracticeSession,
    isPlaying: Boolean,
    progress: Float,
    saffronColor: Color,
    analysisViewModel: AnalysisViewModel, // Add ViewModel parameter
    onPlayToggle: () -> Unit,
    onBack: () -> Unit
) {
    var selectedScale by remember { mutableStateOf<String?>(null) }
    var showScaleDialog by remember { mutableStateOf(true) }

    // Get analysis data from ViewModel instead of computing directly
    val analysisData by analysisViewModel.analysisData.collectAsState()
    val isLoading by analysisViewModel.isLoading.collectAsState()

    // Trigger analysis when session changes and scale is selected
    LaunchedEffect(session, selectedScale) {
        if (selectedScale != null) {
            analysisViewModel.analyzeSession(session, selectedScale!!)
        }
    }

    // Force scale selection if not already set, to ensure frequency calculations are accurate
    if (showScaleDialog && selectedScale == null) {
        ScaleSelectionDialog(
            saffronColor = saffronColor,
            onScaleSelected = { scale ->
                selectedScale = scale
                showScaleDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            AnalysisTopBar(
                session = session,
                selectedScale = selectedScale,
                saffronColor = saffronColor,
                onBack = onBack,
                onChangeScale = { showScaleDialog = true }
            )
        },
        containerColor = iOSBlack
    ) { padding ->
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            label = "analysis-content-transition"
        ) { loading ->
            if (loading) {
                // Animated loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = saffronColor
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Analyzing your practice...",
                            color = Color.White
                        )
                    }
                }
            } else if (analysisData != null) {
                AnalysisContent(
                    modifier = Modifier.padding(padding),
                    session = session,
                    analysisData = analysisData!!,
                    isPlaying = isPlaying,
                    progress = progress,
                    saffronColor = saffronColor,
                    onPlayToggle = onPlayToggle
                )
            } else {
                // Animated error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(initialScale = 0.8f) + fadeIn(),
                        exit = scaleOut(targetScale = 1.2f) + fadeOut()
                    ) {
                        Text(
                            text = "No analysis data available",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vertical scrollable content of the analysis screen, organizing various data cards.
 */
@Composable
private fun AnalysisContent(
    modifier: Modifier = Modifier,
    session: PracticeSession,
    analysisData: AnalysisData,
    isPlaying: Boolean,
    progress: Float,
    saffronColor: Color,
    onPlayToggle: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Qualitative feedback from Guru
        GurukulFeedbackHeader(analysisData = analysisData, saffronColor = saffronColor)

        // Metadata summary
        SessionQuickInfoCard(session = session, saffronColor = saffronColor)

        // Key performance metrics
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                PerformanceOverviewCard(
                    accuracy = analysisData.overallAccuracy,
                    saffronColor = saffronColor
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MasteryBadgeCard(level = analysisData.masteryLevel)
            }
        }

        // Voice quality metrics
        StabilityOverviewCard(
            stability = analysisData.averageStability,
            vibrato = analysisData.vibratoScore,
            saffronColor = saffronColor
        )

        // Playback and Real-time pitch visualization
        PlaybackAnalysisCard(
            isPlaying = isPlaying,
            progress = progress,
            saffronColor = saffronColor,
            onPlayToggle = onPlayToggle
        )

        // Positive reinforcement
        MilestonesCard(milestones = analysisData.milestones, saffronColor = saffronColor)

        // Error breakdown
        AnalysisCard(
            title = "Detailed Error Report",
            icon = Icons.Default.Analytics,
            saffronColor = saffronColor
        ) {
            ErrorAnalysisView(
                errors = analysisData.errors,
                saffronColor = saffronColor
            )
        }

        // Visual precision map - Premium Version
        AnalysisCard(
            title = "Swar Precision Map",
            icon = Icons.Default.GraphicEq,
            saffronColor = saffronColor
        ) {
            PremiumSwarPrecisionWheel(
                swarData = analysisData.swarStats,
                ragaNotes = analysisData.ragaInfo.swars.toSet(),
                saffronColor = saffronColor,
                onSwarSelected = { swarData ->
                    // Handle swar selection for detailed feedback
                    println("Selected swar: ${swarData.name} with accuracy: ${swarData.accuracy}")
                }
            )
        }

        // Academic/Reference section
        AnalysisCard(
            title = "Raga Reference & Tips",
            icon = Icons.Default.Lightbulb,
            saffronColor = saffronColor
        ) {
            RagaInsights(
                ragaInfo = analysisData.ragaInfo,
                saffronColor = saffronColor
            )
        }

        // User notes from the session
        if (session.notes.isNotEmpty()) {
            AnalysisCard(
                title = "Session Notes",
                icon = Icons.Default.Notes,
                saffronColor = saffronColor
            ) {
                Text(
                    session.notes,
                    color = Color.White.copy(0.8f),
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
        }

        // Actionable advice for next practice
        PracticeRecommendationCard(
            errors = analysisData.errors,
            saffronColor = saffronColor
        )

        Spacer(Modifier.height(32.dp))
    }
}

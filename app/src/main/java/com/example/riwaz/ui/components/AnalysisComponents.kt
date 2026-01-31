package com.example.riwaz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riwaz.models.PracticeSession
import com.example.riwaz.ui.theme.iOSBlack
import com.example.riwaz.ui.theme.iOSRed
import com.example.riwaz.utils.*
import kotlin.math.sin

/**
 * Top app bar for the Analysis screen, showing session info and scale actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisTopBar(
    session: PracticeSession,
    selectedScale: String?,
    saffronColor: Color,
    onBack: () -> Unit,
    onChangeScale: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Analysis: ${session.raga}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        session.practiceType,
                        color = saffronColor,
                        fontSize = 12.sp
                    )
                    selectedScale?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "• Scale: $it",
                            color = Color.White.copy(0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = onChangeScale) {
                Icon(Icons.Default.Tune, "Change Scale", tint = saffronColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = iOSBlack)
    )
}

/**
 * A stylized header providing qualitative feedback from a "Guru" perspective.
 */
@Composable
fun GurukulFeedbackHeader(analysisData: AnalysisData, saffronColor: Color) {
    val feedback = remember(analysisData.overallAccuracy) {
        when {
            analysisData.overallAccuracy > 0.9f -> "Your resonance is divine today. Focus on the subtle nuances of Teevra Ma."
            analysisData.overallAccuracy > 0.8f -> "Strong performance. Your stability in the lower octave is improving."
            else -> "A good start. Work on your breath control during the Komal Re andolan."
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = saffronColor.copy(0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, saffronColor.copy(0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = saffronColor.copy(0.2f)
            ) {
                Icon(
                    Icons.Default.Face,
                    contentDescription = null,
                    tint = saffronColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Guru's Note",
                    color = saffronColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    feedback,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Display a badge indicating the user's mastery level for this session.
 */
@Composable
fun MasteryBadgeCard(level: MasteryLevel) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Verified,
                contentDescription = null,
                tint = level.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                level.label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                "Mastery Level",
                color = Color.White.copy(0.4f),
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Lists the achievements or milestones reached during the session.
 */
@Composable
fun MilestonesCard(milestones: List<MasteryMilestone>, saffronColor: Color) {
    AnalysisCard(
        title = "Today's Achievements",
        icon = Icons.Default.EmojiEvents,
        saffronColor = saffronColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            milestones.forEach { milestone ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (milestone.isAchieved) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (milestone.isAchieved) Color(0xFF4CAF50) else Color.White.copy(0.2f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            milestone.title,
                            color = if (milestone.isAchieved) Color.White else Color.White.copy(0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            milestone.description,
                            color = Color.White.copy(0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Horizontal row of session metadata (Tempo, Date, Type).
 */
@Composable
fun SessionQuickInfoCard(session: PracticeSession, saffronColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoItem(label = "Tempo", value = session.tempo, icon = Icons.Default.Speed, color = saffronColor)
            VerticalDivider(modifier = Modifier.height(32.dp), color = Color.White.copy(0.1f))
            InfoItem(label = "Date", value = "Today", icon = Icons.Default.CalendarToday, color = saffronColor)
            VerticalDivider(modifier = Modifier.height(32.dp), color = Color.White.copy(0.1f))
            InfoItem(label = "Type", value = session.practiceType, icon = Icons.Default.Mic, color = saffronColor)
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color.copy(0.6f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(0.4f), fontSize = 10.sp)
    }
}

/**
 * Circular progress indicator for overall accuracy.
 */
@Composable
fun PerformanceOverviewCard(
    accuracy: Float,
    saffronColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { accuracy },
                    modifier = Modifier.size(40.dp),
                    color = saffronColor,
                    strokeWidth = 3.dp,
                    trackColor = Color.White.copy(0.1f)
                )
                Text(
                    "${(accuracy * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Accuracy",
                color = Color.White.copy(0.4f),
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Detailed stats on voice stability and vibrato.
 */
@Composable
fun StabilityOverviewCard(
    stability: Float,
    vibrato: Float,
    saffronColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Voice Stability", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    Text("${(stability * 100).toInt()}%", color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { stability },
                        modifier = Modifier.fillMaxWidth(0.8f).height(3.dp).clip(CircleShape),
                        color = Color(0xFF4CAF50),
                        trackColor = Color.White.copy(0.1f)
                    )
                }
                VerticalDivider(modifier = Modifier.height(44.dp).padding(horizontal = 12.dp), color = Color.White.copy(0.1f))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibrato Quality", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    Text("${(vibrato * 100).toInt()}%", color = Color(0xFF2196F3), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { vibrato },
                        modifier = Modifier.fillMaxWidth(0.8f).height(3.dp).clip(CircleShape),
                        color = Color(0xFF2196F3),
                        trackColor = Color.White.copy(0.1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Analysis: Your sustained notes are steady, but your vibrato depth could be more consistent in the higher range.",
                color = Color.White.copy(0.5f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Shows a list of errors with severity levels and correction tips.
 */
@Composable
fun ErrorAnalysisView(
    errors: List<ErrorDetail>,
    saffronColor: Color
) {
    if (errors.isEmpty()) {
        Text(
            "No significant errors detected. Great job!",
            color = Color.White.copy(0.6f),
            modifier = Modifier.padding(8.dp)
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        errors.forEach { error ->
            ErrorCard(error = error, saffronColor = saffronColor)
        }
    }
}

@Composable
private fun ErrorCard(error: ErrorDetail, saffronColor: Color) {
    val backgroundColor = when (error.severity) {
        ErrorSeverity.CRITICAL -> iOSRed.copy(0.1f)
        ErrorSeverity.MAJOR -> Color(0xFFFF9800).copy(0.1f)
        ErrorSeverity.MINOR -> Color.White.copy(0.05f)
    }

    val severityColor = when (error.severity) {
        ErrorSeverity.CRITICAL -> iOSRed
        else -> Color(0xFFFF9800)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(severityColor, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${error.swar}: ${error.category.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                SeverityBadge(severity = error.severity)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                error.description,
                color = Color.White.copy(0.8f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(10.dp))
            CorrectionHint(
                correction = error.correction,
                saffronColor = saffronColor
            )
        }
    }
}

@Composable
private fun SeverityBadge(severity: ErrorSeverity) {
    Surface(
        color = Color.White.copy(0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            severity.name,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = Color.White.copy(0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CorrectionHint(correction: String, saffronColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(0.3f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AutoFixHigh,
            contentDescription = null,
            tint = saffronColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            correction,
            color = saffronColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Bar chart showing accuracy for each Swar.
 */
@Composable
fun EnhancedSwarPrecisionChart(
    swarStats: List<SwarData>,
    saffronColor: Color
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            swarStats.forEach { stat ->
                SwarBar(stat = stat, saffronColor = saffronColor)
            }
        }
        Spacer(Modifier.height(12.dp))
        ChartLegend()
    }
}

@Composable
private fun RowScope.SwarBar(stat: SwarData, saffronColor: Color) {
    val barColor = remember(stat.isMistake, stat.accuracy) {
        when {
            stat.isMistake -> iOSRed
            stat.accuracy > 0.9f -> Color(0xFF4CAF50)
            else -> saffronColor
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            "${(stat.accuracy * 100).toInt()}%",
            color = barColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight(stat.accuracy.coerceAtLeast(0.1f))
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(barColor, barColor.copy(alpha = 0.3f))
                    )
                )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stat.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ChartLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendItem(color = iOSRed, label = "Needs Work")
        Spacer(Modifier.width(16.dp))
        LegendItem(color = Color(0xFF4CAF50), label = "Perfect")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White.copy(0.5f), fontSize = 11.sp)
    }
}

/**
 * Reference section for the current Raga, showing correct Swars and tips.
 */
@Composable
fun RagaInsights(
    ragaInfo: RagaRegistry.RagaData,
    saffronColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SwarReferenceGrid(
            swars = ragaInfo.swars,
            vadi = ragaInfo.vadi,
            samvadi = ragaInfo.samvadi,
            saffronColor = saffronColor
        )
        HorizontalDivider(color = Color.White.copy(0.1f))
        ragaInfo.tips.forEach { tip ->
            TipItem(tip = tip, saffronColor = saffronColor)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwarReferenceGrid(
    swars: List<String>,
    vadi: String?,
    samvadi: String?,
    saffronColor: Color
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        swars.forEach { swar ->
            val isVadi = swar == vadi
            val isSamvadi = swar == samvadi
            SwarChip(
                swar = swar,
                isVadi = isVadi,
                isSamvadi = isSamvadi,
                saffronColor = saffronColor
            )
        }
    }
}

@Composable
private fun SwarChip(
    swar: String,
    isVadi: Boolean = false,
    isSamvadi: Boolean = false,
    saffronColor: Color
) {
    val freq = remember(swar) { com.example.riwaz.utils.FrequencyCalculator.calculate(swar, "C") }
    val borderColor = when {
        isVadi -> saffronColor
        isSamvadi -> Color(0xFF4CAF50)
        else -> saffronColor.copy(0.2f)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isVadi || isSamvadi) borderColor.copy(0.15f) else saffronColor.copy(0.1f),
        border = androidx.compose.foundation.BorderStroke(if (isVadi || isSamvadi) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    swar,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isVadi || isSamvadi) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isVadi) "V" else "S",
                        color = borderColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Text(
                "${freq.toInt()}Hz",
                color = Color.White.copy(0.5f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TipItem(tip: String, saffronColor: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.Lightbulb,
            contentDescription = null,
            tint = saffronColor,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            tip,
            color = Color.White.copy(0.8f),
            fontSize = 13.sp
        )
    }
}

/**
 * Suggestions for improvement based on the errors detected in the session.
 */
@Composable
fun PracticeRecommendationCard(
    errors: List<ErrorDetail>,
    saffronColor: Color
) {
    if (errors.isEmpty()) return

    AnalysisCard(
        title = "Personalized Practice Plan",
        icon = Icons.Default.AutoFixHigh,
        saffronColor = saffronColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Based on today's session, focus on these exercises:",
                color = Color.White.copy(0.7f),
                fontSize = 13.sp
            )
            
            val recommendations = remember(errors) {
                errors.map { error ->
                    when (error.category) {
                        ErrorCategory.PITCH -> "Slow Alankars focusing on ${error.swar}"
                        ErrorCategory.EXPRESSION -> "Meend practice from Ni to ${error.swar}"
                        ErrorCategory.TIMING -> "Metronome-based Palta for ${error.swar}"
                    }
                }.distinct().take(3)
            }

            recommendations.forEach { recommendation ->
                RecommendationItem(text = recommendation, saffronColor = saffronColor)
            }
        }
    }
}

@Composable
private fun RecommendationItem(text: String, saffronColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = saffronColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = Color.White, fontSize = 14.sp)
    }
}

/**
 * Container for the playback controls and pitch graph visualization.
 */
@Composable
fun PlaybackAnalysisCard(
    isPlaying: Boolean,
    progress: Float,
    saffronColor: Color,
    onPlayToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = saffronColor.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, saffronColor.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PlaybackHeader(
                isPlaying = isPlaying,
                saffronColor = saffronColor,
                onPlayToggle = onPlayToggle
            )

            Spacer(Modifier.height(14.dp))
            PitchGraph(
                saffronColor = saffronColor,
                progress = progress
            )

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = saffronColor,
                trackColor = Color.White.copy(0.1f)
            )
        }
    }
}

@Composable
private fun PlaybackHeader(
    isPlaying: Boolean,
    saffronColor: Color,
    onPlayToggle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onPlayToggle),
            shape = CircleShape,
            color = saffronColor
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.padding(10.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "Session Review",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isPlaying) "Pitch mapping in real-time..." else "Tap to review performance",
                color = Color.White.copy(0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PitchGraph(
    saffronColor: Color,
    progress: Float
) {
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val width = size.width
        val height = size.height
        val midY = height / 2

        // Background static wave
        val bgPath = Path().apply {
            moveTo(0f, midY)
            for (i in 0..100) {
                val x = (i / 100f) * width
                val y = midY + sin(i * 0.2f) * 15f + sin(i * 0.5f) * 5f
                lineTo(x, y)
            }
        }
        drawPath(bgPath, Color.White.copy(0.1f), style = Stroke(2.dp.toPx()))

        // Active progress wave
        val activePath = Path().apply {
            moveTo(0f, midY)
            val limit = (progress * 100).toInt()
            for (i in 0..limit) {
                val x = (i / 100f) * width
                val y = midY + sin(i * 0.2f) * 15f + sin(i * 0.5f) * 5f
                lineTo(x, y)
            }
        }
        drawPath(activePath, saffronColor, style = Stroke(3.dp.toPx()))
    }
}

/**
 * Reusable card wrapper for different analysis sections.
 */
@Composable
fun AnalysisCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    saffronColor: Color,
    content: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = saffronColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(0.02f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.05f))
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * Dialog for selecting the base musical scale (Sa).
 */
@Composable
fun ScaleSelectionDialog(
    saffronColor: Color,
    onScaleSelected: (String) -> Unit
) {
    val scales = remember {
        listOf(
            "C" to "Low Male",
            "D" to "Male (Std)",
            "E" to "High Male",
            "F" to "Female (Std)",
            "G" to "High Female"
        )
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("Set Base Scale (Sa)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Calibration ensures accurate frequency matching for your voice range.",
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                scales.forEach { (note, description) ->
                    ScaleOption(
                        note = note,
                        description = description,
                        onClick = { onScaleSelected(note) }
                    )
                }
            }
        },
        confirmButton = {},
        containerColor = iOSBlack,
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(0.8f)
    )
}

@Composable
private fun ScaleOption(
    note: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(note, color = Color.White, fontWeight = FontWeight.Bold)
            Text(description, color = Color.White.copy(0.5f), fontSize = 12.sp)
        }
    }
}

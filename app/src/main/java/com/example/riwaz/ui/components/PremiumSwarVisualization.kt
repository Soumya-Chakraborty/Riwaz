package com.example.riwaz.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riwaz.ui.theme.*
import com.example.riwaz.utils.SwarData
import com.example.riwaz.utils.ShrutiAnalyzer
import kotlin.math.*

/**
 * Premium Swar Precision Visualization
 * 
 * Features:
 * - Circular octave wheel showing all 12 notes
 * - Animated radial precision indicators
 * - Shruti-level deviation display (22 shrutis)
 * - Glassmorphism design with gradients
 * - Interactive note selection with detailed breakdown
 */
@Composable
fun PremiumSwarPrecisionWheel(
    swarData: List<SwarData>,
    ragaNotes: Set<String>,
    tonicFrequency: Float = 261.63f,
    saffronColor: Color = SaffronPrimary,
    modifier: Modifier = Modifier,
    onSwarSelected: ((SwarData) -> Unit)? = null
) {
    var selectedSwar by remember { mutableStateOf<SwarData?>(null) }
    
    // Aggregate swar data by note name
    val swarStats = remember(swarData) {
        swarData.groupBy { it.name }
            .mapValues { (_, swars) ->
                SwarStats(
                    name = swars.first().name,
                    avgAccuracy = swars.map { it.accuracy }.average().toFloat(),
                    avgStability = swars.map { it.stability }.average().toFloat(),
                    occurrences = swars.size,
                    avgDeviation = swars.map { abs(it.expectedFreq - it.detectedFreq) }.average().toFloat(),
                    isInRaga = ragaNotes.contains(swars.first().name.replace("(k)", "").replace("(t)", ""))
                )
            }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        iOSBlack,
                        iOSDarkGray.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with gradient text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "स्वर शुद्धता",
                    color = saffronColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Swar Precision",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Overall score badge
            val overallAccuracy = swarStats.values.map { it.avgAccuracy }.average().toFloat()
            PrecisionBadge(accuracy = overallAccuracy, saffronColor = saffronColor)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Octave Wheel Visualization
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            saffronColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            OctaveWheelCanvas(
                swarStats = swarStats,
                selectedSwar = selectedSwar?.name,
                ragaNotes = ragaNotes,
                saffronColor = saffronColor,
                onNoteClick = { noteName ->
                    val swar = swarData.find { it.name == noteName }
                    selectedSwar = swar
                    swar?.let { onSwarSelected?.invoke(it) }
                }
            )
            
            // Center display
            CenterDisplay(
                selectedSwar = selectedSwar,
                swarStats = swarStats,
                saffronColor = saffronColor
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Horizontal swar cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            itemsIndexed(swarStats.values.toList().sortedByDescending { it.occurrences }) { _, stats ->
                AnimatedSwarCard(
                    stats = stats,
                    isSelected = selectedSwar?.name == stats.name,
                    saffronColor = saffronColor,
                    onClick = {
                        val swar = swarData.find { it.name == stats.name }
                        selectedSwar = if (selectedSwar?.name == stats.name) null else swar
                        swar?.let { onSwarSelected?.invoke(it) }
                    }
                )
            }
        }
        
        // Selected swar detail panel
        AnimatedVisibility(
            visible = selectedSwar != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedSwar?.let { swar ->
                Spacer(modifier = Modifier.height(16.dp))
                SwarDetailPanel(
                    swar = swar,
                    stats = swarStats[swar.name],
                    saffronColor = saffronColor
                )
            }
        }
    }
}

@Composable
private fun PrecisionBadge(accuracy: Float, saffronColor: Color) {
    val animatedAccuracy by animateFloatAsState(
        targetValue = accuracy,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "accuracy"
    )
    
    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(16.dp),
        color = when {
            accuracy >= 0.85f -> GreenSuccess.copy(alpha = 0.2f)
            accuracy >= 0.7f -> AmberWarning.copy(alpha = 0.2f)
            else -> RedError.copy(alpha = 0.2f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                accuracy >= 0.85f -> GreenSuccess.copy(alpha = 0.5f)
                accuracy >= 0.7f -> AmberWarning.copy(alpha = 0.5f)
                else -> RedError.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    accuracy >= 0.85f -> Icons.Default.Star
                    accuracy >= 0.7f -> Icons.Default.ThumbUp
                    else -> Icons.Default.TrendingUp
                },
                contentDescription = null,
                tint = when {
                    accuracy >= 0.85f -> GreenSuccess
                    accuracy >= 0.7f -> AmberWarning
                    else -> RedError
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "${(animatedAccuracy * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    accuracy >= 0.85f -> GreenSuccess
                    accuracy >= 0.7f -> AmberWarning
                    else -> RedError
                }
            )
        }
    }
}

@Composable
private fun OctaveWheelCanvas(
    swarStats: Map<String, SwarStats>,
    selectedSwar: String?,
    ragaNotes: Set<String>,
    saffronColor: Color,
    onNoteClick: (String) -> Unit
) {
    // All 12 notes in order
    val allNotes = listOf(
        "Sa", "Re(k)", "Re", "Ga(k)", "Ga", "Ma", "Ma(t)", "Pa", "Dha(k)", "Dha", "Ni(k)", "Ni"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2 - 20f
        val innerRadius = outerRadius * 0.4f
        
        // Draw concentric circles for reference
        for (i in 1..4) {
            val radius = innerRadius + (outerRadius - innerRadius) * (i / 4f)
            drawCircle(
                color = Color.White.copy(alpha = 0.05f * i),
                radius = radius,
                center = center,
                style = Stroke(width = 1f)
            )
        }
        
        // Draw precision arcs for each note
        allNotes.forEachIndexed { index, note ->
            val startAngle = -90f + (index * 30f) - 14f
            val sweepAngle = 28f
            
            val stats = swarStats[note]
            val isInRaga = ragaNotes.contains(note.replace("(k)", "").replace("(t)", ""))
            val accuracy = stats?.avgAccuracy ?: 0f
            val isSelected = note == selectedSwar
            
            // Calculate radius based on accuracy
            val noteRadius = innerRadius + (outerRadius - innerRadius) * accuracy.coerceIn(0f, 1f)
            
            // Base arc color
            val arcColor = when {
                !isInRaga && stats != null -> Color.Magenta.copy(alpha = 0.6f)
                accuracy >= 0.85f -> GreenSuccess
                accuracy >= 0.7f -> AmberWarning
                accuracy >= 0.3f -> RedError
                else -> Color.White.copy(alpha = 0.15f)
            }
            
            // Draw glow for selected note
            if (isSelected) {
                drawArc(
                    color = saffronColor.copy(alpha = glowAlpha),
                    startAngle = startAngle - 2,
                    sweepAngle = sweepAngle + 4,
                    useCenter = true,
                    topLeft = Offset(center.x - outerRadius - 10, center.y - outerRadius - 10),
                    size = Size((outerRadius + 10) * 2, (outerRadius + 10) * 2)
                )
            }
            
            // Draw precision arc
            if (stats != null) {
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            arcColor.copy(alpha = 0.8f),
                            arcColor.copy(alpha = 0.4f)
                        ),
                        center = center,
                        radius = outerRadius
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - noteRadius, center.y - noteRadius),
                    size = Size(noteRadius * 2, noteRadius * 2)
                )
                
                // Inner cutout
                drawArc(
                    color = iOSBlack,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2)
                )
            }
            
            // Draw raga indicator dot
            if (isInRaga) {
                val dotAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                val dotRadius = outerRadius + 15
                val dotX = center.x + dotRadius * cos(dotAngle).toFloat()
                val dotY = center.y + dotRadius * sin(dotAngle).toFloat()
                
                drawCircle(
                    color = saffronColor,
                    radius = 5f,
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}

@Composable
private fun CenterDisplay(
    selectedSwar: SwarData?,
    swarStats: Map<String, SwarStats>,
    saffronColor: Color
) {
    val stats = selectedSwar?.name?.let { swarStats[it] }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (selectedSwar != null && stats != null) {
            // Show selected note
            Text(
                selectedSwar.name,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = saffronColor
            )
            Text(
                "${(stats.avgAccuracy * 100).toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    stats.avgAccuracy >= 0.85f -> GreenSuccess
                    stats.avgAccuracy >= 0.7f -> AmberWarning
                    else -> RedError
                }
            )
            Text(
                "${stats.occurrences} times",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        } else {
            // Default display
            val totalNotes = swarStats.values.sumOf { it.occurrences }
            val avgAccuracy = swarStats.values.map { it.avgAccuracy }.average().toFloat()
            
            Text(
                "सा",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = saffronColor.copy(alpha = 0.5f)
            )
            Text(
                "$totalNotes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "notes analyzed",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun AnimatedSwarCard(
    stats: SwarStats,
    isSelected: Boolean,
    saffronColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    val borderColor = when {
        !stats.isInRaga -> Color.Magenta
        stats.avgAccuracy >= 0.85f -> GreenSuccess
        stats.avgAccuracy >= 0.7f -> AmberWarning
        else -> RedError
    }
    
    Card(
        modifier = Modifier
            .width(80.dp)
            .scale(scale)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, saffronColor, RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                saffronColor.copy(alpha = 0.15f) 
            else 
                iOSMediumGray.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Note name
            Text(
                stats.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) saffronColor else Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Circular progress
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { stats.avgAccuracy },
                    modifier = Modifier.fillMaxSize(),
                    color = borderColor,
                    trackColor = iOSSoftGray,
                    strokeWidth = 4.dp
                )
                Text(
                    "${(stats.avgAccuracy * 100).toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Occurrence count
            Text(
                "×${stats.occurrences}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            // Raga indicator
            if (stats.isInRaga) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "राग",
                    fontSize = 10.sp,
                    color = saffronColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SwarDetailPanel(
    swar: SwarData,
    stats: SwarStats?,
    saffronColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = iOSMediumGray.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        swar.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = saffronColor
                    )
                    if (stats?.isInRaga == true) {
                        Text(
                            "Part of Raga Scale",
                            fontSize = 12.sp,
                            color = GreenSuccess
                        )
                    }
                }
                
                // Large accuracy circle
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = swar.accuracy,
                        animationSpec = tween(1000),
                        label = "progress"
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = when {
                            swar.accuracy >= 0.85f -> GreenSuccess
                            swar.accuracy >= 0.7f -> AmberWarning
                            else -> RedError
                        },
                        trackColor = iOSSoftGray,
                        strokeWidth = 6.dp
                    )
                    Text(
                        "${(swar.accuracy * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Expected",
                    value = "${swar.expectedFreq.toInt()} Hz",
                    icon = Icons.Default.MusicNote,
                    color = TealPrimary
                )
                MetricItem(
                    label = "Detected",
                    value = "${swar.detectedFreq.toInt()} Hz",
                    icon = Icons.Default.Mic,
                    color = if (abs(swar.expectedFreq - swar.detectedFreq) < 10) GreenSuccess else RedError
                )
                MetricItem(
                    label = "Stability",
                    value = "${(swar.stability * 100).toInt()}%",
                    icon = Icons.Default.Waves,
                    color = AmberWarning
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Shruti deviation indicator
            ShrutiDeviationBar(
                expectedFreq = swar.expectedFreq,
                detectedFreq = swar.detectedFreq,
                saffronColor = saffronColor
            )
            
            // Suggestion
            val deviation = swar.detectedFreq - swar.expectedFreq
            if (abs(deviation) > 5) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = if (deviation > 0) RedError.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (deviation > 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (deviation > 0) RedError else AmberWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (deviation > 0) 
                                "Pitch is ${abs(deviation).toInt()} Hz high - try singing lower" 
                            else 
                                "Pitch is ${abs(deviation).toInt()} Hz low - try singing higher",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ShrutiDeviationBar(
    expectedFreq: Float,
    detectedFreq: Float,
    saffronColor: Color
) {
    val deviationCents = if (expectedFreq > 0 && detectedFreq > 0) {
        (1200 * ln(detectedFreq.toDouble() / expectedFreq) / ln(2.0)).toFloat()
    } else 0f
    
    val animatedDeviation by animateFloatAsState(
        targetValue = deviationCents,
        animationSpec = tween(800),
        label = "deviation"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Shruti Deviation",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                "${if (deviationCents >= 0) "+" else ""}${deviationCents.toInt()} cents",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    abs(deviationCents) <= 10 -> GreenSuccess
                    abs(deviationCents) <= 25 -> AmberWarning
                    else -> RedError
                }
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Deviation bar with center marker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iOSSoftGray)
        ) {
            // Gradient background for deviation zones
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(RedError.copy(alpha = 0.3f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(AmberWarning.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(GreenSuccess.copy(alpha = 0.3f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(AmberWarning.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(RedError.copy(alpha = 0.3f))
                )
            }
            
            // Center line (perfect pitch)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .background(saffronColor)
            )
            
            // Deviation indicator
            val normalizedDeviation = (animatedDeviation / 50f).coerceIn(-1f, 1f) // ±50 cents range
            val indicatorOffset = (0.5f + normalizedDeviation * 0.5f)
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .offset(x = (indicatorOffset * 280).dp) // Approximate positioning
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            )
        }
        
        // Scale labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-50¢", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
            Text("-25¢", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
            Text("0", fontSize = 9.sp, color = saffronColor, fontWeight = FontWeight.Bold)
            Text("+25¢", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
            Text("+50¢", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
        }
    }
}

// Data class for aggregated swar statistics
data class SwarStats(
    val name: String,
    val avgAccuracy: Float,
    val avgStability: Float,
    val occurrences: Int,
    val avgDeviation: Float,
    val isInRaga: Boolean
)

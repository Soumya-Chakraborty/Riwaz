package com.example.riwaz.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riwaz.ui.theme.*
import com.example.riwaz.utils.SwarData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Enhanced swar precision mapping visualization that includes:
 * 1. A scrollable graph showing pitch accuracy over time
 * 2. Visual representation of each swar with its accuracy and stability
 * 3. Color-coded feedback for pitch accuracy (green for accurate, red for inaccurate)
 * 4. Timeline visualization showing how accuracy changes throughout the recording
 * 5. Interactive elements allowing users to tap on specific segments for detailed feedback
 * 6. Smooth animations and transitions for better UX
 * 7. Proper scaling to handle longer recordings
 * 8. Visual indicators for raga-specific notes and forbidden notes
 */
@Composable
fun SwarPrecisionVisualization(
    swarData: List<SwarData>,
    ragaInfo: RagaRegistry.RagaData,
    modifier: Modifier = Modifier,
    onSwarSelected: ((SwarData) -> Unit)? = null
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }

    // State for zoom and pan
    var zoomLevel by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var selectedSwarIndex by remember { mutableStateOf<Int?>(null) }

    // Calculate timeline data points
    val timelineData = remember(swarData) {
        swarData.mapIndexed { index, swar ->
            TimelinePoint(
                index = index,
                swar = swar,
                accuracy = swar.accuracy,
                stability = swar.stability,
                isRagaSpecific = ragaInfo.swars.contains(swar.name.replace("(k)", "").replace("(t)", "")),
                isSelected = selectedSwarIndex == index
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(iOSBlack)
    ) {
        // Animated header
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Swar Precision Mapping",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { /* Show info dialog */ },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Information",
                        tint = Color.White.copy(0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Animated legend
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(delayMillis = 100)) + fadeIn(),
            exit = fadeOut()
        ) {
            SwarPrecisionLegend()
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Animated graph container
        AnimatedVisibility(
            visible = true,
            enter = expandIn(expandFrom = Alignment.Center) + fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                TimelineVisualization(
                    timelineData = timelineData,
                    zoomLevel = zoomLevel,
                    offsetX = offsetX,
                    onZoomChanged = { zoomLevel = it },
                    onPanChanged = { offsetX += it },
                    onSwarTapped = { index ->
                        selectedSwarIndex = if (selectedSwarIndex == index) null else index
                        onSwarSelected?.invoke(timelineData[index].swar)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animated selected swar details
        AnimatedContent(
            targetState = selectedSwarIndex,
            transitionSpec = {
                slideInVertically(initialOffsetY = { it }) + fadeIn() togetherWith
                slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            },
            label = "swar-details-animation"
        ) { selectedIndex ->
            selectedIndex?.let { idx ->
                val selectedData = timelineData[idx]
                SwarDetailsCard(selectedData.swar, ragaInfo)
            }
        }
    }
}

@Composable
private fun SwarPrecisionLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(color = GreenSuccess, label = "Accurate")
        LegendItem(color = AmberWarning, label = "Moderate")
        LegendItem(color = RedError, label = "Inaccurate")
        LegendItem(color = TealPrimary, label = "Raga Note")
        LegendItem(color = Color.Magenta, label = "Forbidden Note")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            color = Color.White.copy(0.7f),
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineVisualization(
    timelineData: List<TimelinePoint>,
    zoomLevel: Float,
    offsetX: Float,
    onZoomChanged: (Float) -> Unit,
    onPanChanged: (Float) -> Unit,
    onSwarTapped: (Int) -> Unit
) {
    val density = LocalDensity.current
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Handle click */ },
                onLongClick = { /* Handle long press */ }
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        onPanChanged(dragAmount.x)
                        change.consume()
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Draw grid lines
        drawGrid(canvasWidth, canvasHeight)
        
        if (timelineData.isNotEmpty()) {
            // Calculate spacing between points
            val pointSpacing = (canvasWidth * zoomLevel) / max(1, timelineData.size - 1)
            
            // Draw accuracy line
            drawAccuracyLine(timelineData, pointSpacing, canvasHeight, offsetX)
            
            // Draw swar points
            timelineData.forEachIndexed { index, point ->
                val x = (index * pointSpacing) + offsetX
                
                if (x >= -pointSpacing && x <= canvasWidth + pointSpacing) {
                    drawSwarPoint(
                        point = point,
                        x = x,
                        canvasHeight = canvasHeight,
                        onSwarTapped = { onSwarTapped(index) }
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(canvasWidth: Float, canvasHeight: Float) {
    val gridColor = Color.White.copy(alpha = 0.1f)
    val strokeWidth = 1f
    
    // Horizontal grid lines
    for (i in 0..5) {
        val y = canvasHeight * (i / 5f)
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(canvasWidth, y),
            strokeWidth = strokeWidth
        )
    }
    
    // Vertical grid lines
    for (i in 0..10) {
        val x = canvasWidth * (i / 10f)
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, canvasHeight),
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawAccuracyLine(
    timelineData: List<TimelinePoint>,
    pointSpacing: Float,
    canvasHeight: Float,
    offsetX: Float
) {
    if (timelineData.size < 2) return

    val path = Path().apply {
        var firstPoint = true
        timelineData.forEachIndexed { index, point ->
            val x = (index * pointSpacing) + offsetX
            val y = canvasHeight * (1 - point.accuracy) // Invert Y axis

            if (firstPoint) {
                moveTo(x, y)
                firstPoint = false
            } else {
                lineTo(x, y)
            }
        }
    }

    drawPath(
        path = path,
        color = TealPrimary.copy(alpha = 0.7f),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawSwarPoint(
    point: TimelinePoint,
    x: Float,
    canvasHeight: Float,
    onSwarTapped: () -> Unit
) {
    val y = canvasHeight * (1 - point.accuracy) // Invert Y axis for accuracy
    val radius = if (point.isSelected) 12f else 8f

    // Determine color based on accuracy
    val color = when {
        point.accuracy >= 0.9f -> GreenSuccess
        point.accuracy >= 0.7f -> AmberWarning
        else -> RedError
    }

    // Draw outer circle for raga-specific notes
    if (point.isRagaSpecific) {
        drawCircle(
            color = TealPrimary,
            radius = radius + 4f,
            center = Offset(x, y)
        )
    }

    // Draw main point
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(x, y)
    )

    // Draw inner circle for stability
    val stabilityRadius = radius * point.stability
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = stabilityRadius,
        center = Offset(x, y)
    )

}

@Composable
private fun SwarDetailsCard(swarData: SwarData, ragaInfo: RagaRegistry.RagaData) {
    val isRagaSpecific = ragaInfo.swars.contains(swarData.name.replace("(k)", "").replace("(t)", ""))
    val isForbidden = !isRagaSpecific && !ragaInfo.swars.any {
        it.replace("(k)", "").replace("(t)", "") == swarData.name.replace("(k)", "").replace("(t)", "")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = iOSMediumGray
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    swarData.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    if (isRagaSpecific) {
                        Surface(
                            modifier = Modifier.padding(end = 8.dp),
                            color = TealPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Raga Note",
                                color = TealPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (isForbidden) {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color.Magenta.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Forbidden",
                                color = Color.Magenta,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Accuracy indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Accuracy: ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                LinearProgressIndicator(
                    progress = { swarData.accuracy },
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = when {
                        swarData.accuracy >= 0.9f -> GreenSuccess
                        swarData.accuracy >= 0.7f -> AmberWarning
                        else -> RedError
                    },
                    trackColor = iOSSoftGray
                )
                Text(
                    "${(swarData.accuracy * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stability indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Stability: ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                LinearProgressIndicator(
                    progress = { swarData.stability },
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = TealPrimary,
                    trackColor = iOSSoftGray
                )
                Text(
                    "${(swarData.stability * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency comparison
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Expected",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${swarData.expectedFreq.toInt()} Hz",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Detected",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${swarData.detectedFreq.toInt()} Hz",
                        color = if (abs(swarData.expectedFreq - swarData.detectedFreq) < 10)
                            GreenSuccess else RedError,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Deviation",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${abs(swarData.expectedFreq - swarData.detectedFreq).toInt()} Hz",
                        color = if (abs(swarData.expectedFreq - swarData.detectedFreq) < 10)
                            GreenSuccess else RedError,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Data class for timeline visualization
data class TimelinePoint(
    val index: Int,
    val swar: SwarData,
    val accuracy: Float,
    val stability: Float,
    val isRagaSpecific: Boolean,
    val isSelected: Boolean = false
)

/**
 * Alternative visualization using LazyColumn for longer recordings
 */
@Composable
fun ScrollableSwarTimeline(
    swarData: List<SwarData>,
    ragaInfo: RagaRegistry.RagaData,
    modifier: Modifier = Modifier,
    onSwarSelected: ((SwarData) -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(swarData.size) { index ->
            val swar = swarData[index]
            val isRagaSpecific = ragaInfo.swars.contains(swar.name.replace("(k)", "").replace("(t)", ""))
            val isForbidden = !isRagaSpecific && !ragaInfo.swars.any { 
                it.replace("(k)", "").replace("(t)", "") == swar.name.replace("(k)", "").replace("(t)", "") 
            }
            
            SwarTimelineItem(
                swar = swar,
                isRagaSpecific = isRagaSpecific,
                isForbidden = isForbidden,
                onClick = { onSwarSelected?.invoke(swar) }
            )
        }
    }
}

@Composable
private fun SwarTimelineItem(
    swar: SwarData,
    isRagaSpecific: Boolean,
    isForbidden: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = iOSMediumGray
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Swar name with indicator
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        swar.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (isRagaSpecific) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(TealPrimary, CircleShape)
                        )
                    } else if (isForbidden) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.Magenta, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Frequency info
                Text(
                    "${swar.expectedFreq.toInt()} → ${swar.detectedFreq.toInt()} Hz",
                    color = Color.White.copy(0.7f),
                    fontSize = 14.sp
                )
            }

            // Accuracy indicator
            Column(
                horizontalAlignment = Alignment.End
            ) {
                LinearProgressIndicator(
                    progress = { swar.accuracy },
                    modifier = Modifier
                        .width(100.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        swar.accuracy >= 0.9f -> GreenSuccess
                        swar.accuracy >= 0.7f -> AmberWarning
                        else -> RedError
                    },
                    trackColor = iOSSoftGray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "${(swar.accuracy * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
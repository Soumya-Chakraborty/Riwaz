package com.example.riwaz.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riwaz.utils.ScaleManager
import com.example.riwaz.ui.theme.iOSBlack
import com.example.riwaz.ui.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentScale: String,
    onScaleChange: (String) -> Unit,
    currentThreshold: Float,
    onThresholdChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    var selectedScale by remember { mutableStateOf(currentScale) }
    var thresholdValue by remember { mutableStateOf(currentThreshold) }
    
    LaunchedEffect(currentScale) {
        selectedScale = currentScale
    }
    
    LaunchedEffect(currentThreshold) {
        thresholdValue = currentThreshold
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = iOSBlack)
            )
        },
        containerColor = iOSBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Reference Scale",
                    color = SaffronPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select your preferred Sa (tonic) position",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            items(ScaleManager.REFERENCE_SCALES) { scale ->
                ScaleOptionItem(
                    scale = scale,
                    isSelected = selectedScale == scale.name,
                    onSelect = {
                        selectedScale = scale.name
                        onScaleChange(scale.name)
                    },
                    saffronColor = SaffronPrimary
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Analysis Sensitivity",
                    color = SaffronPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Adjust error detection thresholds",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                ThresholdSetting(
                    threshold = thresholdValue,
                    onThresholdChange = { newValue ->
                        thresholdValue = newValue
                        onThresholdChange(newValue)
                    },
                    saffronColor = SaffronPrimary
                )
            }
        }
    }
}

@Composable
fun ScaleOptionItem(
    scale: ScaleManager.ReferenceScale,
    isSelected: Boolean,
    onSelect: () -> Unit,
    saffronColor: Color = SaffronPrimary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) saffronColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, saffronColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = scale.name,
                    color = if (isSelected) saffronColor else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = scale.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Selected",
                    tint = saffronColor
                )
            }
        }
    }
}

@Composable
fun ThresholdSetting(
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    saffronColor: Color = SaffronPrimary
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sensitivity:",
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = when {
                    threshold < 0.5f -> "Beginner"
                    threshold < 0.7f -> "Intermediate"
                    else -> "Advanced"
                },
                color = SaffronPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = threshold,
            onValueChange = onThresholdChange,
            valueRange = 0.1f..0.95f,
            steps = 16, // 17 steps between 0.1 and 0.95
            colors = SliderDefaults.colors(
                thumbColor = SaffronPrimary,
                activeTrackColor = SaffronPrimary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "More Forgiving",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Text(
                text = "More Strict",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}
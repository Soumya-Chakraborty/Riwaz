package com.example.riwaz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riwaz.models.RagaCategory
import com.example.riwaz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailsScreen(
    ragaCategories: List<RagaCategory>,
    practiceTypes: List<String>,
    tempoOptions: List<String>,
    selectedRaga: String,
    selectedPracticeType: String,
    selectedTempo: String,
    notes: String,
    saffronColor: androidx.compose.ui.graphics.Color,
    onRagaChange: (String) -> Unit,
    onPracticeTypeChange: (String) -> Unit,
    onTempoChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var expandedRagaCategory by remember { mutableStateOf<String?>(null) }
    var expandedPracticeType by remember { mutableStateOf(false) }
    var expandedTempo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Practice Session Details",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iOSDarkGray
                )
            )
        },
        containerColor = iOSBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSave,
                containerColor = RedError
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Raga selection
            Text(
                "Select Raga",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            ragaCategories.forEach { category ->
                Column {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (expandedRagaCategory == category.name) saffronColor.copy(alpha = 0.1f) else iOSMediumGray,
                        onClick = { expandedRagaCategory = if (expandedRagaCategory == category.name) null else category.name }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                category.name,
                                color = saffronColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (expandedRagaCategory == category.name) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }

                    if (expandedRagaCategory == category.name) {
                        Column(modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)) {
                            category.ragas.forEach { raga ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onRagaChange(raga) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .background(
                                            if (selectedRaga == raga) saffronColor.copy(alpha = 0.2f) else iOSMediumGray,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedRaga == raga,
                                        onClick = { onRagaChange(raga) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = saffronColor,
                                            unselectedColor = Color.Gray
                                        )
                                    )
                                    Text(
                                        text = raga,
                                        color = if (selectedRaga == raga) saffronColor else Color.White,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Practice type selection
            Text(
                "Practice Type",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = expandedPracticeType,
                onExpandedChange = { expandedPracticeType = it }
            ) {
                OutlinedTextField(
                    value = selectedPracticeType,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = saffronColor,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = saffronColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedPracticeType) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                )

                ExposedDropdownMenu(
                    expanded = expandedPracticeType,
                    onDismissRequest = { expandedPracticeType = false },
                    modifier = Modifier.background(iOSMediumGray)
                ) {
                    practiceTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, color = Color.White) },
                            onClick = {
                                onPracticeTypeChange(type)
                                expandedPracticeType = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tempo selection
            Text(
                "Tempo",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = expandedTempo,
                onExpandedChange = { expandedTempo = it }
            ) {
                OutlinedTextField(
                    value = selectedTempo,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = saffronColor,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = saffronColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedTempo) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                )

                ExposedDropdownMenu(
                    expanded = expandedTempo,
                    onDismissRequest = { expandedTempo = false },
                    modifier = Modifier.background(iOSMediumGray)
                ) {
                    tempoOptions.forEach { tempo ->
                        DropdownMenuItem(
                            text = { Text(tempo, color = Color.White) },
                            onClick = {
                                onTempoChange(tempo)
                                expandedTempo = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes field
            Text(
                "Notes (Optional)",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            TextField(
                value = notes,
                onValueChange = onNotesChange,
                placeholder = { Text("Add practice notes...", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = saffronColor,
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = saffronColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = iOSMediumGray,
                    unfocusedContainerColor = iOSMediumGray,
                    disabledContainerColor = iOSMediumGray
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
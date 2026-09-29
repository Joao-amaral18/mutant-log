package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Exercise
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing

@Composable
fun StartFreeSessionDialog(
    exercises: List<Exercise>,
    isStarting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onStart: (title: String, exerciseIds: List<Long>) -> Unit
) {
    var title by remember { mutableStateOf("Free Session") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedExerciseIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val filteredExercises = remember(exercises, searchQuery) {
        if (searchQuery.isBlank()) {
            exercises
        } else {
            exercises.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.muscleGroup.contains(searchQuery, ignoreCase = true) ||
                    it.baseName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isStarting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .testTag("start_free_session_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF161322),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282338))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF201B30)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "START FREE SESSION",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = MutantColors.TextPrimary
                                )
                            )
                            Text(
                                text = "Pick exercises for your workout",
                                style = MaterialTheme.typography.bodySmall.copy(color = MutantColors.TextMetadata)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isStarting
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MutantColors.TextMetadata)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Workout Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Name") },
                    singleLine = true,
                    enabled = !isStarting,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF2E2742),
                        focusedTextColor = MutantColors.TextPrimary,
                        unfocusedTextColor = MutantColors.TextPrimary,
                        focusedLabelColor = Color(0xFFA855F7),
                        unfocusedLabelColor = MutantColors.TextMetadata
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("free_session_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search exercises or muscle...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFA855F7))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MutantColors.TextMetadata)
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF2E2742),
                        focusedTextColor = MutantColors.TextPrimary,
                        unfocusedTextColor = MutantColors.TextPrimary,
                        focusedPlaceholderColor = MutantColors.TextMetadata,
                        unfocusedPlaceholderColor = MutantColors.TextMetadata
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Selected count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected: ${selectedExerciseIds.size} exercises",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (selectedExerciseIds.isNotEmpty()) Color(0xFFA855F7) else MutantColors.TextMetadata,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (selectedExerciseIds.isNotEmpty()) {
                        TextButton(onClick = { selectedExerciseIds = emptySet() }) {
                            Text("Clear all", color = MutantColors.TextMetadata, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Exercises list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredExercises.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No exercises found.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MutantColors.TextMetadata)
                                )
                            }
                        }
                    } else {
                        items(filteredExercises, key = { it.id }) { exercise ->
                            val isSelected = exercise.id in selectedExerciseIds
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isStarting) {
                                        selectedExerciseIds = if (isSelected) {
                                            selectedExerciseIds - exercise.id
                                        } else {
                                            selectedExerciseIds + exercise.id
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF221A36) else Color(0xFF1A1626),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFA855F7) else Color(0xFF262035)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MutantColors.TextPrimary else MutantColors.TextSecondary
                                            )
                                        )
                                        Text(
                                            text = exercise.muscleGroup,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MutantColors.TextMetadata)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFFA855F7) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFFA855F7) else Color(0xFF49424F),
                                                RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF150826),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isStarting,
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutantColors.TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2742)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onStart(title.ifBlank { "Free Session" }, selectedExerciseIds.toList())
                        },
                        enabled = selectedExerciseIds.isNotEmpty() && !isStarting,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA855F7),
                            contentColor = Color(0xFF150826),
                            disabledContainerColor = Color(0xFF2E2742),
                            disabledContentColor = MutantColors.TextMetadata
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("confirm_start_free_session")
                    ) {
                        if (isStarting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF150826)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Starting...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("START WORKOUT", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.example.data.model.SetSegment
import com.example.ui.theme.*

@Composable
fun IntensitySegmentDialog(
    initialWeight: Float,
    initialReps: Int,
    techniqueName: String, // "Rest-pause" or "Drop set"
    onDismiss: () -> Unit,
    onSaveSegments: (List<SetSegment>) -> Unit
) {
    val segments = remember {
        mutableStateListOf(
            SetSegment(
                workoutSetId = 0,
                segmentIndex = 1,
                type = techniqueName.uppercase().replace(" ", "_"),
                weightKg = if (techniqueName == "Drop set") (initialWeight * 0.75f) else initialWeight,
                reps = (initialReps / 2).coerceAtLeast(4),
                restSeconds = if (techniqueName == "Rest-pause") 20 else 5
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(MutantShapeTokens.Panel)
                .testTag("intensity_segment_dialog"),
            color = MutantSurfaceCard,
            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
        ) {
            Column(modifier = Modifier.padding(MutantSpacing.lgCompact)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$techniqueName PROTOCOL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MutantVolt
                        )
                    )
                    Text(
                        text = "Base: ${initialWeight}kg × $initialReps",
                        style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                    )
                }

                Text(
                    text = "Log segmented mini-sets executed without racking the barbell or during short pause intervals.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary),
                    modifier = Modifier.padding(top = MutantSpacing.xxs, bottom = MutantSpacing.sm)
                )

                // Segments list
                Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                    segments.forEachIndexed { index, seg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MutantDarkNavy,
                            shape = MutantShapeTokens.CompactControl,
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MutantSpacing.compactMd, vertical = MutantSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Segment ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MutantVolt
                                    )
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
                                ) {
                                    // Weight
                                    Text(
                                        text = "${seg.weightKg} kg",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MutantTextPrimary
                                        )
                                    )
                                    Text(
                                        text = "×",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MutantTextMuted)
                                    )
                                    // Reps
                                    Text(
                                        text = "${seg.reps} reps",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MutantTextPrimary
                                        )
                                    )

                                    if (techniqueName == "Rest-pause") {
                                        Text(
                                            text = "(${seg.restSeconds}s rest)",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MutantCyan)
                                        )
                                    }

                                    IconButton(
                                        onClick = { segments.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove segment",
                                            tint = MutantRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MutantSpacing.compactMd))

                // Add segment button
                OutlinedButton(
                    onClick = {
                        val last = segments.lastOrNull()
                        val nextWeight = if (techniqueName == "Drop set")
                            ((last?.weightKg ?: initialWeight) - 10f).coerceAtLeast(10f)
                        else
                            (last?.weightKg ?: initialWeight)
                        val nextReps = ((last?.reps ?: 6) - 2).coerceAtLeast(3)
                        segments.add(
                            SetSegment(
                                workoutSetId = 0,
                                segmentIndex = segments.size + 1,
                                type = techniqueName.uppercase().replace(" ", "_"),
                                weightKg = nextWeight,
                                reps = nextReps,
                                restSeconds = if (techniqueName == "Rest-pause") 20 else 5
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MutantShapeTokens.TinyControl,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutantVolt),
                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantVolt.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(MutantSpacing.compact))
                    Text("Add Next Mini-Segment")
                }

                Spacer(modifier = Modifier.height(MutantSpacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutantTextSecondary),
                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onSaveSegments(segments.toList()) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MutantVolt,
                            contentColor = MutantOnVolt
                        )
                    ) {
                        Text("Attach", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

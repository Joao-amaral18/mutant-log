package com.example.ui.components

import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantTracking

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import com.example.data.repository.ReadinessInput
import com.example.ui.theme.*

@Composable
fun ReadinessDialog(
    initialInput: ReadinessInput = ReadinessInput(),
    onDismiss: () -> Unit,
    onConfirm: (ReadinessInput) -> Unit
) {
    var sleep by remember { mutableStateOf(initialInput.sleep) }
    var energy by remember { mutableStateOf(initialInput.energy) }
    var soreness by remember { mutableStateOf(initialInput.soreness) }
    var jointDiscomfort by remember { mutableStateOf(initialInput.jointDiscomfort) }
    var motivation by remember { mutableStateOf(initialInput.motivation) }

    // Live readiness calculation
    val isFatigued = jointDiscomfort in listOf("Moderate", "Severe") || (energy <= 2 && soreness >= 4)
    val statusText = if (isFatigued) "High fatigue detected" else "Normal"
    val statusColor = if (isFatigued) MutantAmber else MutantEmerald

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(MutantShapeTokens.LargePanel)
                .testTag("readiness_dialog"),
            color = MutantSurfaceCard,
            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(MutantSpacing.lgMd)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYSTEM READINESS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MutantTextPrimary,
                            letterSpacing = MutantTracking.Compact
                        )
                    )

                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = MutantShapeTokens.SmallControl,
                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            ),
                            modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.xxs)
                        )
                    }
                }

                Text(
                    text = "Calibrate neuromuscular status before initiating session.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary),
                    modifier = Modifier.padding(top = MutantSpacing.xxs, bottom = MutantSpacing.md)
                )

                // Sleep Score (1-5)
                ScoreSelectorRow(
                    label = "Sleep Quality",
                    value = sleep,
                    onSelect = { sleep = it }
                )

                Spacer(modifier = Modifier.height(MutantSpacing.sm))

                // Energy (1-5)
                ScoreSelectorRow(
                    label = "Energy Level",
                    value = energy,
                    onSelect = { energy = it }
                )

                Spacer(modifier = Modifier.height(MutantSpacing.sm))

                // Soreness (1-5)
                ScoreSelectorRow(
                    label = "Muscle Soreness",
                    value = soreness,
                    onSelect = { soreness = it },
                    higherIsWorse = true
                )

                Spacer(modifier = Modifier.height(MutantSpacing.sm))

                // Joint Discomfort
                Text(
                    text = "Joint Discomfort",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MutantTextSecondary
                    ),
                    modifier = Modifier.padding(bottom = MutantSpacing.compact)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                ) {
                    listOf("None", "Mild", "Moderate", "Severe").forEach { level ->
                        val selected = jointDiscomfort.equals(level, ignoreCase = true)
                        val badgeColor = when (level) {
                            "None" -> MutantEmerald
                            "Mild" -> MutantCyan
                            "Moderate" -> MutantAmber
                            else -> MutantRed
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable { jointDiscomfort = level },
                            shape = MutantShapeTokens.TinyControl,
                            color = if (selected) badgeColor.copy(alpha = 0.25f) else MutantDarkNavy,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) badgeColor else MutantBorder
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) badgeColor else MutantTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MutantSpacing.sm))

                // Motivation (1-5)
                ScoreSelectorRow(
                    label = "Focus & Motivation",
                    value = motivation,
                    onSelect = { motivation = it }
                )

                if (isFatigued) {
                    Spacer(modifier = Modifier.height(MutantSpacing.mdPlus))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MutantAmber.copy(alpha = 0.12f),
                        shape = MutantShapeTokens.TinyControl,
                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantAmber.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(MutantSpacing.compactMd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MutantAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(MutantSpacing.xs))
                            Text(
                                text = "High fatigue detected. The system suggests consolidating loads or reducing working volume.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MutantAmber)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MutantSpacing.lgMd))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutantTextSecondary),
                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder),
                        shape = MutantShapeTokens.CompactControl
                    ) {
                        Text("CANCEL")
                    }

                    Button(
                        onClick = {
                            onConfirm(
                                ReadinessInput(
                                    sleep = sleep,
                                    energy = energy,
                                    soreness = soreness,
                                    jointDiscomfort = jointDiscomfort,
                                    motivation = motivation
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("confirm_readiness_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MutantVolt,
                            contentColor = MutantOnVolt
                        ),
                        shape = MutantShapeTokens.CompactControl
                    ) {
                        Text(
                            text = "START SESSION",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreSelectorRow(
    label: String,
    value: Int,
    onSelect: (Int) -> Unit,
    higherIsWorse: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MutantTextSecondary
                )
            )
            Text(
                text = "$value / 5",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutantTextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(MutantSpacing.compact))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
        ) {
            (1..5).forEach { num ->
                val selected = num == value
                val activeColor = if (higherIsWorse) {
                    if (num >= 4) MutantRed else if (num >= 3) MutantAmber else MutantEmerald
                } else {
                    if (num >= 4) MutantEmerald else if (num >= 3) MutantCyan else MutantAmber
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable { onSelect(num) },
                    shape = MutantShapeTokens.TinyControl,
                    color = if (selected) activeColor.copy(alpha = 0.25f) else MutantDarkNavy,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) activeColor else MutantBorder
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = num.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) activeColor else MutantTextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

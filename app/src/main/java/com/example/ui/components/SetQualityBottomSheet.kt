package com.example.ui.components

import com.example.ui.designsystem.MutantTracking

import com.example.ui.designsystem.MutantTypeScale

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetQualityBottomSheet(
    weightKg: Float,
    reps: Int,
    rir: Float,
    onSaveAndDismiss: (execution: Int, targetMuscle: Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    var executionQuality by remember { mutableStateOf<Int?>(null) }
    var targetMuscleQuality by remember { mutableStateOf<Int?>(null) }

    fun checkAutoDismiss(exec: Int?, target: Int?) {
        if (exec != null && target != null) {
            onSaveAndDismiss(exec, target)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MutantSurfaceCard,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = MutantSpacing.compactMd)
                    .width(36.dp)
                    .height(4.dp),
                shape = MutantShapeTokens.Micro,
                color = MutantBorder
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MutantSpacing.lgMd)
                .padding(bottom = MutantSpacing.sheetBottom)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SET LOGGED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MutantVolt,
                            letterSpacing = MutantTracking.Section
                        )
                    )
                    Text(
                        text = "${if (weightKg % 1f == 0f) weightKg.toInt() else weightKg} kg × $reps @ ${rir.toInt()} RIR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MutantTextPrimary
                        )
                    )
                }

                TextButton(
                    onClick = {
                        onSaveAndDismiss(executionQuality ?: 3, targetMuscleQuality ?: 3)
                    }
                ) {
                    Text("DONE", color = MutantVolt, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(MutantSpacing.md))

            // Execution Quality
            Text(
                text = "Execution",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutantTextSecondary
                )
            )
            Spacer(modifier = Modifier.height(MutantSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
            ) {
                listOf(
                    4 to "Excellent",
                    3 to "Good",
                    2 to "Compromised",
                    1 to "Bad"
                ).forEach { (score, label) ->
                    val isSelected = executionQuality == score
                    val accentColor = if (score >= 3) MutantEmerald else MutantRed
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable {
                                executionQuality = score
                                checkAutoDismiss(score, targetMuscleQuality)
                            },
                        shape = MutantShapeTokens.TinyControl,
                        color = if (isSelected) accentColor.copy(alpha = 0.2f) else MutantDarkNavy,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) accentColor else MutantBorder
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else MutantTextSecondary,
                                    fontSize = MutantTypeScale.label
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MutantSpacing.md))

            // Target Muscle Sensation
            Text(
                text = "Target muscle",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutantTextSecondary
                )
            )
            Spacer(modifier = Modifier.height(MutantSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
            ) {
                listOf(
                    4 to "Excellent",
                    3 to "Good",
                    2 to "Weak",
                    1 to "None"
                ).forEach { (score, label) ->
                    val isSelected = targetMuscleQuality == score
                    val accentColor = if (score >= 3) MutantCyan else MutantAmber
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable {
                                targetMuscleQuality = score
                                checkAutoDismiss(executionQuality, score)
                            },
                        shape = MutantShapeTokens.TinyControl,
                        color = if (isSelected) accentColor.copy(alpha = 0.2f) else MutantDarkNavy,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) accentColor else MutantBorder
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else MutantTextSecondary,
                                    fontSize = MutantTypeScale.label
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

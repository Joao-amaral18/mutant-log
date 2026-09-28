package com.example.ui.designsystem.components

import com.example.ui.designsystem.MutantTracking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.formatLoad
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing
import com.example.ui.designsystem.MutantTextStyles

@Composable
fun MutantSetInput(
    weightValue: String,
    onWeightChange: (String) -> Unit,
    repsValue: String,
    onRepsChange: (String) -> Unit,
    incrementKg: Float = 2.5f,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.sm)
    ) {
        // Weight Card Container: #17121F
        Surface(
            modifier = Modifier.weight(1f),
            shape = MutantShapeTokens.Card,
            color = MutantColors.SurfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(MutantSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CARGA (KG)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MutantColors.TextMetadata,
                        letterSpacing = MutantTracking.Compact
                    )
                )
                Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                ) {
                    IconButton(
                        onClick = {
                            val current = weightValue.toFloatOrNull() ?: 60f
                            onWeightChange((current - incrementKg).coerceAtLeast(0f).formatLoad())
                        },
                        modifier = Modifier.size(32.dp).testTag("weight_minus_button")
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MutantColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    BasicTextField(
                        value = weightValue,
                        onValueChange = onWeightChange,
                        textStyle = MutantTextStyles.MetricLarge.copy(
                            color = MutantColors.TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(68.dp).testTag("weight_input"),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val current = weightValue.toFloatOrNull() ?: 60f
                            onWeightChange((current + incrementKg).formatLoad())
                        },
                        modifier = Modifier.size(32.dp).testTag("weight_plus_button")
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MutantColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Reps Card Container: #17121F
        Surface(
            modifier = Modifier.weight(1f),
            shape = MutantShapeTokens.Card,
            color = MutantColors.SurfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(MutantSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REPETIÇÕES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MutantColors.TextMetadata,
                        letterSpacing = MutantTracking.Compact
                    )
                )
                Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                ) {
                    IconButton(
                        onClick = {
                            val current = repsValue.toIntOrNull() ?: 10
                            onRepsChange((current - 1).coerceAtLeast(1).toString())
                        },
                        modifier = Modifier.size(32.dp).testTag("reps_minus_button")
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MutantColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    BasicTextField(
                        value = repsValue,
                        onValueChange = onRepsChange,
                        textStyle = MutantTextStyles.MetricLarge.copy(
                            color = MutantColors.TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(54.dp).testTag("reps_input"),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val current = repsValue.toIntOrNull() ?: 10
                            onRepsChange((current + 1).toString())
                        },
                        modifier = Modifier.size(32.dp).testTag("reps_plus_button")
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MutantColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

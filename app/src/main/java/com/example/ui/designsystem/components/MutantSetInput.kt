package com.example.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing
import com.example.ui.designsystem.WorkoutLoggerTokens
import java.math.BigDecimal

@Composable
fun MutantSetInput(
    weightValue: String,
    onWeightChange: (String) -> Unit,
    repsValue: String,
    onRepsChange: (String) -> Unit,
    incrementKg: Float = 2.5f,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val step = incrementKg.takeIf { it.isFinite() && it > 0f } ?: 2.5f
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.sm)) {
        MetricInput(
            label = "LOAD", unit = "KG", value = weightValue, onValueChange = onWeightChange,
            keyboardType = KeyboardType.Decimal, tag = "weight", enabled = enabled,
            adjustments = listOf(-step * 2, -step, step, step * 2),
            onAdjust = { delta ->
                val current = weightValue.toFloatOrNull()?.takeIf { it.isFinite() } ?: 0f
                val next = (current + delta).coerceAtLeast(0f)
                if (next.isFinite()) onWeightChange(decimalLabel(next))
            }, modifier = Modifier.weight(1f)
        )
        MetricInput(
            label = "REPS", unit = "COUNT", value = repsValue, onValueChange = onRepsChange,
            keyboardType = KeyboardType.Number, tag = "reps", enabled = enabled,
            adjustments = listOf(-2f, -1f, 1f, 2f),
            onAdjust = { delta ->
                val current = repsValue.toIntOrNull() ?: 1
                onRepsChange((current.toLong() + delta.toInt()).coerceIn(1, Int.MAX_VALUE.toLong()).toString())
            }, modifier = Modifier.weight(1f)
        )
    }
}

private fun decimalLabel(value: Float): String = BigDecimal(value.toString()).stripTrailingZeros().toPlainString()

@Composable
private fun MetricInput(
    label: String, unit: String, value: String, onValueChange: (String) -> Unit,
    keyboardType: KeyboardType, tag: String, enabled: Boolean,
    adjustments: List<Float>, onAdjust: (Float) -> Unit, modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = MutantShapeTokens.InputChip, color = colors.surfaceContainerLow) {
        Column(Modifier.padding(MutantSpacing.xs), verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
                Text(unit, style = WorkoutLoggerTokens.Label, color = if (tag == "weight") colors.secondary else colors.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    val normalized = input.replace(',', '.')
                    if (normalized.length <= 8 && normalized.all { it.isDigit() || (tag == "weight" && it == '.') } &&
                        normalized.count { it == '.' } <= 1
                    ) onValueChange(normalized)
                }, enabled = enabled,
                textStyle = WorkoutLoggerTokens.Metric.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.secondary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { contentDescription = if (tag == "weight") "Load in kilograms" else "Repetitions" }
                    .testTag("${tag}_input")
            )
            HorizontalDivider(color = colors.outline)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xxs)) {
                adjustments.forEachIndexed { index, delta ->
                    val actionTag = when (index) {
                        0 -> "${tag}_minus_large_button"
                        1 -> "${tag}_minus_button"
                        2 -> "${tag}_plus_button"
                        else -> "${tag}_plus_large_button"
                    }
                    Surface(
                        onClick = { onAdjust(delta) }, enabled = enabled,
                        modifier = Modifier.weight(1f).height(28.dp).testTag(actionTag),
                        shape = MutantShapeTokens.SmallControl, color = colors.surface,
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text((if (delta > 0) "+" else "") + decimalLabel(delta),
                                style = WorkoutLoggerTokens.Label.copy(fontSize = 9.sp, letterSpacing = 0.sp),
                                maxLines = 1, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

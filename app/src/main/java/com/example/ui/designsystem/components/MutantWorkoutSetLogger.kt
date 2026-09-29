package com.example.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.data.model.Exercise
import com.example.data.model.IntensityTechnique
import com.example.data.model.SetSegment
import com.example.data.model.SetType
import com.example.ui.components.PlateVisualizer
import com.example.ui.designsystem.*
import java.math.BigDecimal

@Composable
fun MutantWorkoutSetLogger(
    exercise: Exercise,
    workSetCount: Int,
    warmupSetCount: Int,
    weightValue: String,
    repsValue: String,
    setType: SetType,
    rir: Int,
    technique: IntensityTechnique,
    segments: List<SetSegment>,
    enabled: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onRirChange: (Int) -> Unit,
    onTechniqueChange: (IntensityTechnique) -> Unit,
    onEditSegments: () -> Unit,
    onLogSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val weight = weightValue.toFloatOrNull()
    val reps = repsValue.toIntOrNull()
    val valid = weight != null && weight.isFinite() && weight >= 0 && reps != null && reps > 0
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val padding = if (maxWidth < 340.dp) MutantSpacing.sm else MutantSpacing.md
        Surface(
            shape = MutantShapeTokens.Panel, color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.testTag("set_logger_card")
        ) {
            Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
                ) {
                    Row(Modifier.heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                        Box(Modifier.size(8.dp).rotate(45f).background(colors.secondary))
                        val next = if (setType == SetType.WORK) workSetCount + 1 else warmupSetCount + 1
                        val label = if (setType == SetType.WORK) {
                            "SET ${next.toString().padStart(2, '0')} OF ${exercise.defaultWorkSets.toString().padStart(2, '0')}"
                        } else "WARM-UP ${next.toString().padStart(2, '0')}"
                        Text(label, style = WorkoutLoggerTokens.Label, color = colors.onSurface)
                    }
                    Surface(
                        color = colors.surfaceContainerLow,
                        shape = MutantShapeTokens.TinyControl,
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Row(Modifier.padding(MutantSpacing.micro).selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.micro)) {
                            SetType.entries.forEach { type ->
                                val selected = setType == type
                                Surface(
                                    modifier = Modifier.height(28.dp)
                                        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton,
                                            onClick = { onSetTypeChange(type) })
                                        .testTag("set_type_${type.name}"),
                                    shape = MutantShapeTokens.SmallControl,
                                    color = if (selected) colors.primary else colors.surfaceContainerLow
                                ) {
                                    Box(Modifier.padding(horizontal = MutantSpacing.xs), contentAlignment = Alignment.Center) {
                                        Text(if (type == SetType.WORK) "WORK SET" else "WARM-UP",
                                            style = WorkoutLoggerTokens.Label, maxLines = 1,
                                            color = if (selected) colors.onPrimary else colors.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = colors.outlineVariant)
                MutantSetInput(weightValue, onWeightChange, repsValue, onRepsChange,
                    incrementKg = exercise.defaultIncrementKg, enabled = enabled)
                Surface(color = colors.surfaceContainerLow, shape = MutantShapeTokens.TinyControl) {
                    Row(Modifier.fillMaxWidth().padding(MutantSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("PLATES Â· 20 KG BAR", style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
                            val perSide = ((weight?.takeIf { it.isFinite() } ?: 0f) - 20f).coerceAtLeast(0f) / 2f
                            Text("${BigDecimal(perSide.toString()).stripTrailingZeros().toPlainString()} KG / SIDE",
                                style = WorkoutLoggerTokens.Label, color = colors.onSurface)
                        }
                        PlateVisualizer(totalWeightKg = weight?.takeIf { it.isFinite() } ?: 0f)
                    }
                }
                MutantRirSelector(rir, onRirChange, enabled = enabled)
                MutantTechniqueSelector(technique, onTechniqueChange, enabled = enabled)
                if (technique == IntensityTechnique.REST_PAUSE || technique == IntensityTechnique.DROP_SET) {
                    OutlinedButton(onClick = onEditSegments, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("edit_set_segments")) {
                        Text(if (segments.isEmpty()) "Add segments" else "Edit ${segments.size} segments")
                    }
                }
                if (segments.isNotEmpty()) {
                    Text(segments.joinToString(" Â· ") { "${it.weightKg} kg Ã— ${it.reps}" },
                        style = MaterialTheme.typography.bodySmall, color = colors.secondary)
                }
                Button(
                    onClick = onLogSet, enabled = enabled && valid,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("log_set_button"),
                    shape = MutantShapeTokens.InputChip,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(MutantSpacing.xs))
                    Text("CONFIRM & LOG SET", style = WorkoutLoggerTokens.Label)
                }
                if (!valid) Text("Enter a load of 0 kg or more and at least 1 rep.",
                    style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.testTag("set_input_error"))
            }
        }
    }
}

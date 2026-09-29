package com.example.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing
import com.example.ui.designsystem.WorkoutLoggerTokens

@Composable
fun MutantRirSelector(
    selectedRir: Int,
    onRirSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
        Text("RIR ? REPS IN RESERVE", style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)) {
            (0..4).forEach { rir ->
                val selected = if (rir == 4) selectedRir >= 4 else selectedRir == rir
                val failure = selected && rir == 0
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = { onRirSelected(rir) })
                        .testTag("rir_option_$rir"),
                    shape = MutantShapeTokens.TinyControl,
                    color = when { failure -> colors.errorContainer; selected -> colors.primaryContainer; else -> colors.surfaceContainerLow },
                    border = BorderStroke(1.dp, if (selected) { if (failure) colors.errorContainer else colors.secondary } else colors.outlineVariant)
                ) {
                    Column(Modifier.padding(vertical = MutantSpacing.xxs), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(if (rir == 4) "4+" else "$rir", style = WorkoutLoggerTokens.Label,
                            color = when { failure -> colors.onErrorContainer; selected -> colors.secondary; else -> colors.onSurface })
                        if (rir == 0) Text("FAILURE", style = WorkoutLoggerTokens.Label.copy(fontSize = 8.sp, letterSpacing = 0.sp),
                            color = if (failure) colors.onErrorContainer else colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

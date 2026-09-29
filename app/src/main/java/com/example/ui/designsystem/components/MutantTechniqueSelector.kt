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
import com.example.data.model.IntensityTechnique
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing
import com.example.ui.designsystem.WorkoutLoggerTokens

@Composable
fun MutantTechniqueSelector(
    selectedTechnique: IntensityTechnique,
    onTechniqueSelected: (IntensityTechnique) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val techniques = listOf(
        IntensityTechnique.NONE to "STANDARD", IntensityTechnique.REST_PAUSE to "REST-PAUSE",
        IntensityTechnique.DROP_SET to "DROP SET", IntensityTechnique.PARTIAL_REPS to "PARTIALS"
    )
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
        Text("INTENSITY MODIFIERS", style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth < 300.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f) 2 else 4
            Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.compact)) {
                techniques.chunked(columns).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)) {
                        pair.forEach { (technique, label) ->
                            val selected = selectedTechnique == technique
                            Surface(
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    .selectable(selected = selected, enabled = enabled, role = Role.RadioButton,
                                        onClick = { onTechniqueSelected(technique) })
                                    .testTag("technique_${technique.name}"),
                                shape = MutantShapeTokens.TinyControl,
                                color = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
                                border = BorderStroke(if (selected) 2.dp else 1.dp,
                                    if (selected) colors.secondary else colors.outlineVariant)
                            ) {
                                Box(Modifier.padding(MutantSpacing.xxs), contentAlignment = Alignment.Center) {
                                    Text(label, style = WorkoutLoggerTokens.Label,
                                        color = if (selected) colors.secondary else colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

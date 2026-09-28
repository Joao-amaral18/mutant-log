package com.example.ui.designsystem.components

import com.example.ui.designsystem.MutantTracking

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntensityTechnique
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing

@Composable
fun MutantTechniqueSelector(
    selectedTechnique: IntensityTechnique,
    onTechniqueSelected: (IntensityTechnique) -> Unit,
    modifier: Modifier = Modifier
) {
    val techniques = listOf(
        IntensityTechnique.NONE to "None",
        IntensityTechnique.REST_PAUSE to "Rest-pause",
        IntensityTechnique.DROP_SET to "Drop",
        IntensityTechnique.PARTIAL_REPS to "Partials"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
    ) {
        Text(
            text = "INTENSITY TECHNIQUE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MutantColors.TextMetadata,
                letterSpacing = MutantTracking.Compact
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
        ) {
            techniques.forEach { (tech, label) ->
                val isSelected = selectedTechnique == tech

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MutantColors.PrimaryContainer else MutantColors.SurfaceContainerLow,
                    animationSpec = tween(MutantMotion.Feedback),
                    label = "TechBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MutantColors.OnPrimaryContainer else MutantColors.TextSecondary,
                    animationSpec = tween(MutantMotion.Feedback),
                    label = "TechText"
                )

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(MutantShapeTokens.TinyControl)
                        .background(backgroundColor)
                        .clickable { onTechniqueSelected(tech) }
                        .padding(horizontal = MutantSpacing.mdPlus)
                        .testTag("tech_option_${tech.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}

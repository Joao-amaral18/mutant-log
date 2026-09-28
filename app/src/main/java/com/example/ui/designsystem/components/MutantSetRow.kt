package com.example.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntensityTechnique
import com.example.data.model.SetType
import com.example.data.model.WorkoutSet
import com.example.data.model.formatLoad
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantSemanticColors
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing
import com.example.ui.designsystem.MutantTextStyles
import com.example.ui.designsystem.MutantTypeScale

@Composable
fun MutantSetRow(
    setIndex: Int,
    workoutSet: WorkoutSet,
    onDeleteSet: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("set_row_${workoutSet.id}"),
        shape = MutantShapeTokens.InputChip,
        color = MutantColors.SurfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MutantSpacing.md, vertical = MutantSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Set Index, Success Indicator, Tabular Weight & Reps
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (workoutSet.setType == SetType.WORK) MutantSemanticColors.Success else MutantColors.TextMetadata
                        )
                )

                Text(
                    text = if (workoutSet.setType == SetType.WARMUP) "W$setIndex" else "$setIndex",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (workoutSet.setType == SetType.WORK) MutantColors.TextPrimary else MutantColors.TextMetadata
                    ),
                    modifier = Modifier.width(26.dp)
                )

                // Tabular numbers for weight and reps
                Text(
                    text = "${workoutSet.weightKg.formatLoad()} kg  ×  ${workoutSet.reps}",
                    style = MutantTextStyles.SetValue.copy(
                        color = MutantColors.TextPrimary
                    )
                )

                if (workoutSet.technique != IntensityTechnique.NONE) {
                    Surface(
                        shape = MutantShapeTokens.TinyControl,
                        color = MutantColors.PrimaryContainer,
                        modifier = Modifier.padding(start = MutantSpacing.xxs)
                    ) {
                        Text(
                            text = workoutSet.technique.name.replace("_", "-").lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MutantColors.OnPrimaryContainer,
                                fontSize = MutantTypeScale.micro,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = MutantSpacing.compact, vertical = MutantSpacing.micro)
                        )
                    }
                }
            }

            // Right: RIR Pill & optional delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
            ) {
                Surface(
                    shape = MutantShapeTokens.TinyControl,
                    color = MutantColors.SurfaceContainerHigh
                ) {
                    Text(
                        text = "${workoutSet.rir} RIR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MutantColors.Secondary
                        ),
                        modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.xxs)
                    )
                }

                if (onDeleteSet != null) {
                    IconButton(
                        onClick = onDeleteSet,
                        modifier = Modifier.size(28.dp).testTag("delete_set_${workoutSet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete set",
                            tint = MutantColors.TextMetadata,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// Alias
@Composable
fun MutantSetCard(
    setIndex: Int,
    workoutSet: WorkoutSet,
    onDeleteSet: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) = MutantSetRow(
    setIndex = setIndex,
    workoutSet = workoutSet,
    onDeleteSet = onDeleteSet,
    modifier = modifier
)

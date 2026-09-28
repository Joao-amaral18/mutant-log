package com.example.ui.designsystem.components

import com.example.ui.designsystem.MutantTracking
import com.example.ui.designsystem.MutantTypeScale

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantSpacing

@Composable
fun MutantRirSelector(
    selectedRir: Int,
    onRirSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rirOptions = listOf(0, 1, 2, 3, 4)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
    ) {
        Text(
            text = "REPS IN RESERVE (RIR)",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MutantColors.TextMetadata,
                letterSpacing = MutantTracking.Compact
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
        ) {
            rirOptions.forEach { rir ->
                val isSelected = selectedRir == rir
                val label = if (rir == 4) "4+" else "$rir"

                // Shape-morphing selection: morphs from 8dp to 14dp
                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 14.dp else 8.dp,
                    animationSpec = tween(MutantMotion.Feedback),
                    label = "RirCornerRadius"
                )
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MutantColors.Primary else MutantColors.SurfaceContainerLow,
                    animationSpec = tween(MutantMotion.Feedback),
                    label = "RirBgColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MutantColors.OnPrimary else MutantColors.TextSecondary,
                    animationSpec = tween(MutantMotion.Feedback),
                    label = "RirTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(backgroundColor)
                        .clickable { onRirSelected(rir) }
                        .testTag("rir_option_$rir"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = textColor,
                                            fontSize = MutantTypeScale.numericInput
                        )
                    )
                }
            }
        }
    }
}

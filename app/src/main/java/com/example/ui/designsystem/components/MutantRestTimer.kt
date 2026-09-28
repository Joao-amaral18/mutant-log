package com.example.ui.designsystem.components

import com.example.ui.designsystem.MutantTracking

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing
import com.example.ui.designsystem.MutantTextStyles

@Composable
fun MutantRestTimer(
    remainingSeconds: Int,
    recommendedText: String = "2–3 min",
    onAdd30Seconds: () -> Unit,
    onSkipTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mutant_rest_timer"),
        shape = MutantShapeTokens.Card,
        color = MutantColors.SurfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MutantSpacing.md, vertical = MutantSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MutantColors.Primary,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = "REST INTERVAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MutantColors.TextMetadata,
                            letterSpacing = MutantTracking.Compact
                        )
                    )
                    Text(
                        text = timeFormatted,
                        style = MutantTextStyles.Timer.copy(
                            color = MutantColors.Primary
                        )
                    )
                }

                Text(
                    text = "Rec: $recommendedText",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MutantColors.TextSecondary
                    ),
                    modifier = Modifier.padding(start = MutantSpacing.xxs)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
            ) {
                FilledTonalButton(
                    onClick = onAdd30Seconds,
                    shape = MutantShapeTokens.TinyControl,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MutantColors.PrimaryContainer,
                        contentColor = MutantColors.OnPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = MutantSpacing.compactMd, vertical = MutantSpacing.xxs),
                    modifier = Modifier.height(34.dp).testTag("timer_add_30s")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(MutantSpacing.micro))
                    Text(
                        text = "30s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                IconButton(
                    onClick = onSkipTimer,
                    modifier = Modifier.size(34.dp).testTag("timer_skip")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Pular descanso",
                        tint = MutantColors.TextMetadata,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

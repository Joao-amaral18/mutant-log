package com.example.ui.designsystem.components

import com.example.ui.designsystem.MutantTracking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantSpacing
import com.example.ui.designsystem.MutantTextStyles

@Composable
fun MutantMetric(
    label: String,
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MutantSpacing.micro)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MutantColors.TextMetadata,
                letterSpacing = MutantTracking.Compact
            )
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xxs)
        ) {
            Text(
                text = value,
                style = MutantTextStyles.MetricLarge.copy(
                    color = if (isHighlighted) MutantColors.Primary else MutantColors.TextPrimary
                )
            )
            if (unit != null) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MutantColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun MutantMetricCard(
    title: String,
    primaryMetric: String,
    secondaryMetric: String? = null,
    highlightValue: String? = null,
    highlightLabel: String? = null,
    modifier: Modifier = Modifier
) {
    MutantCard(
        modifier = modifier,
        variant = MutantCardVariant.CONTAINER
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutantColors.TextMetadata,
                    letterSpacing = MutantTracking.Label
                )
            )

            if (highlightValue != null) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xxs)
                ) {
                    if (highlightLabel != null) {
                        Text(
                            text = highlightLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MutantColors.TextSecondary
                            )
                        )
                    }
                    Text(
                        text = highlightValue,
                        style = MutantTextStyles.MetricLarge.copy(
                            color = MutantColors.Primary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MutantSpacing.xxs))

        Text(
            text = primaryMetric,
            style = MutantTextStyles.Metric.copy(
                color = MutantColors.TextPrimary
            )
        )

        if (secondaryMetric != null) {
            Spacer(modifier = Modifier.height(MutantSpacing.micro))
            Text(
                text = secondaryMetric,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MutantColors.TextSecondary,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

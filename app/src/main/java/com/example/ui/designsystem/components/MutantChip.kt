package com.example.ui.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing

@Composable
fun MutantChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    testTag: String? = null
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MutantColors.PrimaryContainer else MutantColors.SurfaceContainerLow,
        animationSpec = tween(MutantMotion.Feedback),
        label = "ChipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MutantColors.OnPrimaryContainer else MutantColors.TextSecondary,
        animationSpec = tween(MutantMotion.Feedback),
        label = "ChipText"
    )

    Surface(
        modifier = modifier
            .clip(MutantShapeTokens.TinyControl)
            .clickable { onClick() }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = MutantShapeTokens.TinyControl,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MutantSpacing.sm, vertical = MutantSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            )
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MutantColors.Primary else MutantColors.TextMetadata
                    )
                )
            }
        }
    }
}

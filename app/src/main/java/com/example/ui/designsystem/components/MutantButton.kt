package com.example.ui.designsystem.components

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantTracking

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantShapeTokens

enum class MutantButtonVariant {
    PRIMARY,
    SECONDARY,
    TONAL
}

@Composable
fun MutantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MutantButtonVariant = MutantButtonVariant.PRIMARY,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    testTag: String = "mutant_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = tween(MutantMotion.Feedback),
        label = "ButtonScale"
    )

    when (variant) {
        MutantButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = MutantShapeTokens.Button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPressed) MutantColors.PrimaryContainer else MutantColors.Primary,
                    contentColor = MutantColors.OnPrimary,
                    disabledContainerColor = MutantColors.Primary.copy(alpha = 0.35f),
                    disabledContentColor = MutantColors.OnPrimary.copy(alpha = 0.5f)
                ),
                modifier = modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(scale)
                    .testTag(testTag)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MutantColors.OnPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(MutantSpacing.compactMd))
                    Text(
                        text = "CARREGANDO...",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = MutantTracking.Compact
                        )
                    )
                } else {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(MutantSpacing.xs))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = MutantTracking.Compact
                        )
                    )
                }
            }
        }
        MutantButtonVariant.SECONDARY, MutantButtonVariant.TONAL -> {
            FilledTonalButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = MutantShapeTokens.Button,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MutantColors.SurfaceContainerHigh,
                    contentColor = MutantColors.TextPrimary
                ),
                modifier = modifier
                    .height(48.dp)
                    .scale(scale)
                    .testTag(testTag)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(MutantSpacing.xs))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = MutantTracking.Tight
                    )
                )
            }
        }
    }
}

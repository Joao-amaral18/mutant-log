package com.example.ui.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing
import com.example.ui.designsystem.MutantStrokeWidths
import com.example.ui.designsystem.MutantTypeScale

/**
 * Canonical Mutant Log loading indicator spinner with high-contrast styling.
 */
@Composable
fun MutantLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = MutantColors.Primary,
    trackColor: Color = MutantColors.OutlineVariant,
    testTag: String = "mutant_loading_indicator"
) {
    CircularProgressIndicator(
        modifier = modifier
            .size(size)
            .testTag(testTag),
        color = color,
        strokeWidth = strokeWidth,
        trackColor = trackColor
    )
}

/**
 * Centered column with a loading indicator, primary message, and optional secondary message.
 */
@Composable
fun MutantLoadingContent(
    modifier: Modifier = Modifier,
    message: String? = "Loading…",
    subMessage: String? = null,
    indicatorColor: Color = MutantColors.Primary,
    indicatorSize: Dp = 40.dp
) {
    Column(
        modifier = modifier.padding(MutantSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)
    ) {
        MutantLoadingIndicator(
            size = indicatorSize,
            color = indicatorColor
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutantColors.TextPrimary,
                    fontSize = MutantTypeScale.body
                )
            )
        }
        if (!subMessage.isNullOrBlank()) {
            Text(
                text = subMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MutantColors.TextSecondary,
                    fontSize = MutantTypeScale.label
                )
            )
        }
    }
}

/**
 * Fullscreen loading screen with background and centered loading content.
 */
@Composable
fun MutantLoadingScreen(
    modifier: Modifier = Modifier,
    message: String? = "Loading…",
    subMessage: String? = null,
    testTag: String = "loading_screen"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MutantColors.Background)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        MutantLoadingContent(
            message = message,
            subMessage = subMessage
        )
    }
}

/**
 * Modal overlay with scrim and elevated container to block touches and provide smooth
 * feedback during asynchronous screen/data transitions (e.g. starting workout, opening draft, saving).
 */
@Composable
fun MutantLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    message: String = "Loading…",
    subMessage: String? = null,
    testTag: String = "loading_overlay"
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(MutantMotion.State, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(MutantMotion.State, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(MutantMotion.Feedback)) +
                scaleOut(targetScale = 0.94f, animationSpec = tween(MutantMotion.Feedback)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MutantColors.Background.copy(alpha = 0.82f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { /* Intercept all touches during transition */ }
                )
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MutantShapeTokens.LargePanel,
                color = MutantColors.SurfaceContainerHigh,
                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantColors.OutlineVariant),
                shadowElevation = 8.dp
            ) {
                MutantLoadingContent(
                    message = message,
                    subMessage = subMessage,
                    indicatorSize = 36.dp
                )
            }
        }
    }
}

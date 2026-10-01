package com.example.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing

enum class MutantCardVariant {
    SURFACE,           // #100D16 (Exercise area / base panel)
    CONTAINER_LOW,     // #131018 (Low emphasis)
    CONTAINER,         // #17121F (Normal card)
    CONTAINER_HIGH,    // #211B2B (Selected / important card)
    CONTAINER_HIGHEST  // #2A2337 (Elevated overlay / focus)
}

@Composable
fun MutantCard(
    modifier: Modifier = Modifier,
    variant: MutantCardVariant = MutantCardVariant.CONTAINER,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    contentPadding: Dp = MutantSpacing.md,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = when (variant) {
        MutantCardVariant.SURFACE -> MutantColors.Surface
        MutantCardVariant.CONTAINER_LOW -> MutantColors.SurfaceContainerLow
        MutantCardVariant.CONTAINER -> MutantColors.SurfaceContainer
        MutantCardVariant.CONTAINER_HIGH -> MutantColors.SurfaceContainerHigh
        MutantCardVariant.CONTAINER_HIGHEST -> MutantColors.SurfaceContainerHighest
    }

    val cardModifier = modifier
        .fillMaxWidth()
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .then(
            if (onClick != null) {
                Modifier.clip(MutantShapeTokens.Card).clickable { onClick() }
            } else {
                Modifier
            }
        )

    Surface(
        modifier = cardModifier,
        shape = MutantShapeTokens.Card,
        color = containerColor,
        // Redesign cards: flat surface with a hairline outline; no tonal tint.
        border = border ?: BorderStroke(1.dp, MutantColors.OutlineVariant),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

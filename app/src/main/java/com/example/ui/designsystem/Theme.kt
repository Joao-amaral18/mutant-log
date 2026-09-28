package com.example.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun MutantTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MutantDarkColors,
        typography = MutantTypography,
        shapes = MutantShapes,
        content = content
    )
}

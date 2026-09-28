package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.ui.designsystem.MutantDarkColors
import com.example.ui.designsystem.MutantShapes
import com.example.ui.designsystem.MutantTypography

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MutantDarkColors,
        typography = MutantTypography,
        shapes = MutantShapes,
        content = content
    )
}

@Composable
fun MutantTheme(
    content: @Composable () -> Unit
) {
    MyApplicationTheme(content = content)
}

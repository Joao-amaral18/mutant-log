package com.example.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/** Stitch tokens scoped to the workout surface. */
object WorkoutLoggerTokens {
    val Background = Color(0xFF14161A)
    val Card = Color(0xFF1A1D23)
    val Input = Color(0xFF0D0E11)
    val Border = Color(0xFF323742)
    val Accent = Color(0xFFC084FC)
    val Action = Color(0xFF9333EA)
    val Failure = Color(0xFFCA1834)
    val DisplayFont = FontFamily(Font(R.font.chakra_petch_bold, FontWeight.Bold))
    val NumericFont = FontFamily(Font(R.font.jetbrains_mono_bold, FontWeight.Bold))
    val Label = TextStyle(fontFamily = NumericFont, fontWeight = FontWeight.Bold,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp)
    val Metric = TextStyle(fontFamily = NumericFont, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp, fontFeatureSettings = "tnum")
}

@Composable
fun WorkoutLoggerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        background = WorkoutLoggerTokens.Background,
        surface = WorkoutLoggerTokens.Card,
        surfaceContainerLow = WorkoutLoggerTokens.Input,
        surfaceContainer = WorkoutLoggerTokens.Card,
        surfaceContainerHigh = Color(0xFF272B33),
        primary = WorkoutLoggerTokens.Action,
        onPrimary = Color.White,
        primaryContainer = Color(0xFF33203F),
        onPrimaryContainer = WorkoutLoggerTokens.Accent,
        secondary = WorkoutLoggerTokens.Accent,
        onSurface = Color(0xFFF1F4F9),
        onSurfaceVariant = Color(0xFFA1A9B6),
        outline = WorkoutLoggerTokens.Border,
        outlineVariant = Color(0xFF272B33),
        errorContainer = WorkoutLoggerTokens.Failure,
        onErrorContainer = Color.White
    ), content = content)
}

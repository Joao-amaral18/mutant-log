package com.example.ui.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object MutantColors {
    val Background = Color(0xFF0C0B10)

    val Surface = Color(0xFF16141D)
    val SurfaceContainerLow = Color(0xFF121017)
    val SurfaceContainer = Color(0xFF16141D)
    val SurfaceContainerHigh = Color(0xFF1E1B27)
    val SurfaceContainerHighest = Color(0xFF2A2634)

    // Brand purple is reserved for current, selected, and primary-action states.
    val Primary = Color(0xFFA98BFF)
    val OnPrimary = Color(0xFF150F24)
    val PrimarySelected = Color(0x24A98BFF)
    val PrimaryIndicator = Color(0x38A98BFF)
    val PrimaryTint = Color(0x14A98BFF)
    val PrimaryTintBorder = Color(0x59A98BFF)

    val PrimaryContainer = Color(0xFF2E2547)
    val OnPrimaryContainer = Color(0xFFE6DEFF)

    val Secondary = Color(0xFFC7B4FF)
    val OnSecondary = Color(0xFF332D41)

    val SecondaryContainer = Color(0xFF4A4458)
    val OnSecondaryContainer = Color(0xFFE8DEF8)

    val Tertiary = Color(0xFF9D8CFF)
    val OnTertiary = Color(0xFF1F1651)

    val TextPrimary = Color(0xFFF3F1F8)
    val TextSecondary = Color(0xFFA29DB0)
    val TextMetadata = Color(0xFF6F6A7D)

    val Outline = Color(0xFF2F2B3B)
    // Control borders sit between card borders and sheet lines.
    val Line = Color(0xFF2A2634)
    val Handle = Color(0xFF3A3547)
    val Scrim = Color(0xB8050408)
    val Toast = Color(0xFFF3F1F8)
    val OnToast = Color(0xFF0C0B10)
    val ToastAccent = Color(0xFF6A4BD6)
    val OutlineVariant = Color(0xFF25222F)

    val Success = Color(0xFF5BD69B)
    val SuccessContainer = Color(0xFF16291F)

    val Warning = Color(0xFFF2B65A)
    val WarningContainer = Color(0xFF2E2415)

    val Error = Color(0xFFFF6B6B)
    val OnError = Color(0xFF1A0707)
    val ErrorContainer = Color(0xFF2E1618)

    // Aliases for MutantPalette compatibility across screens & components
    val AppBackground = Background
    val SurfaceNormal = Surface
    val SurfaceElevated = SurfaceContainer
    val SurfaceEmphasized = SurfaceContainerHigh
    val SurfaceHighest = SurfaceContainerHighest
    val SubtleBorder = OutlineVariant

    val PrimaryPurple = Primary
    val SecondaryPurple = Secondary
    val AlternatePurple = Tertiary

    val SuccessGreen = Success
    val WarningAmber = Warning
    val ErrorRed = Error
}

/** Standard plate colors used by the barbell loading diagram. */
object MutantPlateColors {
    val Red = Color(0xFFDC2626)
    val Blue = Color(0xFF7E57C2)
    val Yellow = Color(0xFFFBC02D)
    val Green = Color(0xFF43A047)
    val White = Color(0xFFF8FAFC)
    val Slate = Color(0xFF64748B)
    val LightSlate = Color(0xFF94A3B8)
    val Collar = Color(0xFFCBD5E1)
    val DarkSlate = Color(0xFF475569)
}

val MutantDarkColors = darkColorScheme(
    primary = MutantColors.Primary,
    onPrimary = MutantColors.OnPrimary,

    primaryContainer = MutantColors.PrimaryContainer,
    onPrimaryContainer = MutantColors.OnPrimaryContainer,

    secondary = MutantColors.Secondary,
    onSecondary = MutantColors.OnSecondary,

    secondaryContainer = MutantColors.SecondaryContainer,
    onSecondaryContainer = MutantColors.OnSecondaryContainer,

    tertiary = MutantColors.Tertiary,
    onTertiary = MutantColors.OnTertiary,

    background = MutantColors.Background,
    onBackground = MutantColors.TextPrimary,

    surface = MutantColors.Surface,
    onSurface = MutantColors.TextPrimary,
    surfaceVariant = MutantColors.SurfaceContainerHigh,
    onSurfaceVariant = MutantColors.TextSecondary,

    surfaceContainerLow = MutantColors.SurfaceContainerLow,
    surfaceContainer = MutantColors.SurfaceContainer,
    surfaceContainerHigh = MutantColors.SurfaceContainerHigh,
    surfaceContainerHighest = MutantColors.SurfaceContainerHighest,

    outline = MutantColors.Outline,
    outlineVariant = MutantColors.OutlineVariant,

    error = MutantColors.Error,
    onError = MutantColors.OnError,
    errorContainer = MutantColors.ErrorContainer
)

// Legacy compatibility bridge
typealias MutantPalette = MutantColors

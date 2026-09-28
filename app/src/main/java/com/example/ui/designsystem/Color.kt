package com.example.ui.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object MutantColors {
    val Background = Color(0xFF09070D)

    val Surface = Color(0xFF100D16)
    val SurfaceContainerLow = Color(0xFF131018)
    val SurfaceContainer = Color(0xFF17121F)
    val SurfaceContainerHigh = Color(0xFF211B2B)
    val SurfaceContainerHighest = Color(0xFF2A2337)

    // Brand purple is reserved for current, selected, and primary-action states.
    val Primary = Color(0xFFB388FF)
    val OnPrimary = Color(0xFF23003D)

    val PrimaryContainer = Color(0xFF432267)
    val OnPrimaryContainer = Color(0xFFE9DDFF)

    val Secondary = Color(0xFFD0BCFF)
    val OnSecondary = Color(0xFF332D41)

    val SecondaryContainer = Color(0xFF4A4458)
    val OnSecondaryContainer = Color(0xFFE8DEF8)

    val Tertiary = Color(0xFF9D8CFF)
    val OnTertiary = Color(0xFF1F1651)

    val TextPrimary = Color(0xFFEAE5EE)
    val TextSecondary = Color(0xFFCBC3D3)
    val TextMetadata = Color(0xFF948C9D)

    val Outline = Color(0xFF49424F)
    val OutlineVariant = Color(0xFF302A36)

    val Success = Color(0xFF4ADE80)
    val SuccessContainer = Color(0xFF193B26)

    val Warning = Color(0xFFFBBF24)
    val WarningContainer = Color(0xFF422E0B)

    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF491216)

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

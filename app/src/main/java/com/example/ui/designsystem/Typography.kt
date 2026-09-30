package com.example.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R

object MutantTypeScale {
    val micro = 10.sp
    val label = 11.sp
    val compact = 12.sp
    val body = 14.sp
    val numericInput = 16.sp
}

object MutantTracking {
    val Tight = 0.5.sp
    val Compact = 1.sp
    val Label = 1.2.sp
    val Section = 1.5.sp
    val Brand = 2.sp
}

// Physical static weight files for Inter
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

// Inter Tight for display identity & screen headers
val InterTight = FontFamily(
    Font(R.font.inter_tight, FontWeight.Bold)
)

// Archivo carries all UI text; the condensed cut is reserved for display headlines.
val Archivo = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold)
)

val ArchivoCondensed = FontFamily(
    Font(R.font.archivo_condensed_bold, FontWeight.Bold),
    Font(R.font.archivo_condensed_extrabold, FontWeight.ExtraBold)
)

// Numbers, eyebrows and set data.
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

/** Redesign type ramp. Sizes come from the Mutant Log screens handoff. */
object MutantType {
    val DisplayHero = TextStyle(fontFamily = ArchivoCondensed, fontWeight = FontWeight.ExtraBold,
        fontSize = 64.sp, lineHeight = 56.sp, letterSpacing = (-0.01).em)
    val DisplayLarge = TextStyle(fontFamily = ArchivoCondensed, fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp, lineHeight = 41.sp)
    val DisplayMedium = TextStyle(fontFamily = ArchivoCondensed, fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp, lineHeight = 33.sp)
    val SheetTitle = TextStyle(fontFamily = ArchivoCondensed, fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, lineHeight = 30.sp)
    val ScreenTitle = TextStyle(fontFamily = ArchivoCondensed, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 28.sp)
    val SectionTitle = TextStyle(fontFamily = ArchivoCondensed, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 22.sp)
    val Brand = TextStyle(fontFamily = ArchivoCondensed, fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp, lineHeight = 22.sp, letterSpacing = 0.03.em)

    val Title = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp)
    val RowTitle = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 19.sp)
    val Button = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 18.sp)
    val ButtonSmall = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 15.sp)
    val Body = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val BodySmall = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val Caption = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 15.sp)
    val Chip = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 14.sp)

    val Eyebrow = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.08.em)
    val MonoLabel = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp)
    val MonoBody = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 16.sp)
    val MonoValue = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp)
    val MonoTimer = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
        fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-0.02).em)
}

// Clean base UI typography (no forced tnum on general text)
val MutantTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),

    headlineSmall = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),

    titleLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),

    titleMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),

    bodySmall = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),

    labelLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),

    labelMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    ),

    labelSmall = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

// Numeric styles with Tabular Figures (tnum) for operational data alignment
object MutantTextStyles {
    val MetricLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        fontFeatureSettings = "tnum"
    )

    val Metric = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        fontFeatureSettings = "tnum"
    )

    val Timer = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        fontFeatureSettings = "tnum"
    )

    val SetValue = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        fontFeatureSettings = "tnum"
    )
}

// Display Typography for brand titles (MUTANT LOG, WORKOUT COMPLETE)
val MutantDisplayTypography = TextStyle(
    fontFamily = InterTight,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp
)

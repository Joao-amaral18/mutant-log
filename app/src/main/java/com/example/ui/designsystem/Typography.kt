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

// Clean base UI typography (no forced tnum on general text)
val MutantTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),

    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),

    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),

    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),

    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),

    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),

    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    ),

    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

// Numeric styles with Tabular Figures (tnum) for operational data alignment
object MutantTextStyles {
    val MetricLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        fontFeatureSettings = "tnum"
    )

    val Metric = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        fontFeatureSettings = "tnum"
    )

    val Timer = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        fontFeatureSettings = "tnum"
    )

    val SetValue = TextStyle(
        fontFamily = Inter,
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

/** Redesign type ramp: Inter Tight for display, Inter for UI, tabular figures for numbers. */
object MutantType {
    private const val Tnum = "tnum"

    val DisplayHero = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp, lineHeight = 50.sp, letterSpacing = (-0.01).em)
    val DisplayLarge = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em)
    val DisplayMedium = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, lineHeight = 29.sp, letterSpacing = (-0.01).em)
    val SheetTitle = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp, lineHeight = 25.sp)
    val ScreenTitle = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 28.sp)
    val SectionTitle = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 22.sp)
    val Brand = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp, lineHeight = 20.sp, letterSpacing = 0.02.em)

    val Title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp)
    val RowTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 19.sp)
    val Button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 18.sp)
    val ButtonSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 15.sp)
    val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val BodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val Caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 15.sp)
    val Chip = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 14.sp)

    // Former mono roles: same metrics, Inter with tabular figures.
    val Eyebrow = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.08.em, fontFeatureSettings = Tnum)
    val MonoLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp,
        fontFeatureSettings = Tnum)
    val MonoBody = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 16.sp,
        fontFeatureSettings = Tnum)
    val MonoValue = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp,
        fontFeatureSettings = Tnum)
    val MonoTimer = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 46.sp,
        letterSpacing = (-0.02).em, fontFeatureSettings = Tnum)
}

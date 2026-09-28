package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Primary font family: Inter
val MutantFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
    Font(R.font.inter, FontWeight.ExtraBold),
    Font(R.font.inter, FontWeight.Black)
)

// Display font family: Inter Tight for large headings & impactful titles
val MutantDisplayFontFamily = FontFamily(
    Font(R.font.inter_tight, FontWeight.Bold),
    Font(R.font.inter_tight, FontWeight.ExtraBold),
    Font(R.font.inter_tight, FontWeight.Black)
)

// Typography with Tabular Numbers enabled for dense numeric operational data
val Typography = Typography(
    // App title (22sp / 700)
    headlineMedium = TextStyle(
        fontFamily = MutantDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),
    // Screen title (20sp / 700)
    titleLarge = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    // Exercise title / name (18sp / 700)
    headlineSmall = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    // Large numeric values, e.g. 60 kg (24sp / 700 with tabular figures)
    headlineLarge = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    // Section title / set row summary (16sp / 600 with tabular figures)
    titleMedium = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        fontFeatureSettings = "tnum"
    ),
    titleSmall = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Body text (14sp / 400)
    bodyLarge = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        fontFeatureSettings = "tnum"
    ),
    // Secondary text (13sp / 400)
    bodyMedium = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp,
        fontFeatureSettings = "tnum"
    ),
    bodySmall = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        fontFeatureSettings = "tnum"
    ),
    // Buttons (Inter 700)
    labelLarge = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    // Section label (12sp / 600)
    labelMedium = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    ),
    // Metadata (11sp / 500)
    labelSmall = TextStyle(
        fontFamily = MutantFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum"
    )
)

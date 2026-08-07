package com.ragnala.pos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ragnala.pos.R

// Cozy slow-life display font (Baloo 2, SIL OFL) for headings & buttons.
// Body stays on a plain sans for readability (DESIGN.md: no ALL CAPS, friendly body).
private val Baloo = FontFamily(
    Font(R.font.baloo2, FontWeight.Normal),
    Font(R.font.baloo2_medium, FontWeight.Medium),
    Font(R.font.baloo2_bold, FontWeight.Bold),
)

val RagnalaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 25.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

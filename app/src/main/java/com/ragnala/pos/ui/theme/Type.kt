package com.ragnala.pos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.ragnala.pos.R

val RagnalaBrandFont = FontFamily(
    Font(R.font.baloo2, FontWeight.Normal),
    Font(R.font.baloo2_medium, FontWeight.Medium),
    Font(R.font.baloo2_bold, FontWeight.Bold),
)

val RagnalaFunctionalFont = FontFamily.SansSerif

val RagnalaNumericFontFeature = "tnum"

val RagnalaBrandDisplay = TextStyle(
    fontFamily = RagnalaBrandFont,
    fontWeight = FontWeight.Bold,
    fontSize = 46.sp,
    lineHeight = 54.sp,
    letterSpacing = (-0.5).sp,
)

val RagnalaBrandHeadline = TextStyle(
    fontFamily = RagnalaBrandFont,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 42.sp,
)

val RagnalaMoneyLarge = TextStyle(
    fontFamily = RagnalaFunctionalFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 32.sp,
    fontFeatureSettings = RagnalaNumericFontFeature,
)

val RagnalaMoneyMedium = TextStyle(
    fontFamily = RagnalaFunctionalFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 26.sp,
    fontFeatureSettings = RagnalaNumericFontFeature,
)

private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val RagnalaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RagnalaBrandFont,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 25.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    bodyMedium = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    bodySmall = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    labelLarge = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = RagnalaFunctionalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

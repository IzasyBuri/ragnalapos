package com.ragnala.pos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = SoftWhite,
    primaryContainer = LeafGreen,
    onPrimaryContainer = TextPrimary,
    secondary = CoffeeBrown,
    onSecondary = SoftWhite,
    secondaryContainer = CoffeeCream,
    onSecondaryContainer = TextPrimary,
    tertiary = NaturalGreen,
    onTertiary = SoftWhite,
    tertiaryContainer = NaturalGreenContainer,
    onTertiaryContainer = TextPrimary,
    background = WarmCream,
    onBackground = TextPrimary,
    surface = SoftWhite,
    onSurface = TextPrimary,
    surfaceVariant = CoffeeCream,
    onSurfaceVariant = TextSecondary,
    outline = AppOutline,
    outlineVariant = AppOutlineVariant,
    error = MutedRed,
    onError = SoftWhite,
    errorContainer = MutedRedContainer,
    onErrorContainer = TextPrimary,
    inverseSurface = TextPrimary,
    inverseOnSurface = SoftWhite,
    inversePrimary = DarkPrimary,
    scrim = TextPrimary.copy(alpha = 0.38f),
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkText,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkText,
    tertiary = DarkSuccess,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkSuccessContainer,
    onTertiaryContainer = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkBackground,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkText,
    inverseSurface = DarkText,
    inverseOnSurface = DarkBackground,
    inversePrimary = ForestGreen,
    scrim = Color(0xCC000000),
)

@Composable
fun RagnalaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RagnalaTypography,
        shapes = RagnalaShapes,
        content = content,
    )
}

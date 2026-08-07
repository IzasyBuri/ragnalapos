package com.ragnala.pos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// DESIGN.md §Design Style — flat colors, soft shadows, no neon, no heavy contrast.
// Semantic colors are non-text only (REVIEW.md §5) — text uses TextPrimary/TextSecondary.
private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = SoftWhite,
    primaryContainer = LeafGreen,
    onPrimaryContainer = TextPrimary,
    secondary = CoffeeBrown,
    onSecondary = SoftWhite,
    secondaryContainer = LeafGreen,
    onSecondaryContainer = TextPrimary,
    background = WarmCream,
    onBackground = TextPrimary,
    surface = SoftWhite,
    onSurface = TextPrimary,
    surfaceVariant = WarmCream,
    onSurfaceVariant = TextSecondary,
    outline = AppOutline,
    error = MutedRed,
    onError = SoftWhite,
)

// "Dark mode should feel like evening inside the café." — DESIGN.md §Dark Theme
private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceHigh,
    onPrimaryContainer = DarkText,
    secondary = DarkAccent,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextSecondary,
    error = MutedRed,
    onError = DarkBackground,
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

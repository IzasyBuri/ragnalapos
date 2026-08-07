package com.ragnala.pos.ui.theme

import androidx.compose.ui.graphics.Color

// Modern café palette (green + white, clean flat look).
// Keeps the same semantic names the app already references (ForestGreen, CoffeeBrown, etc.)
// so no screen code changes; only the hues shift.

// Light scheme
val ForestGreen = Color(0xFF1E7A3D)      // primary — fresh coffee-green (buttons, highlights, active nav)
val CoffeeBrown = Color(0xFF2E8B57)      // secondary — deeper green for tabs/secondary accents
val WarmCream = Color(0xFFFFFFFF)        // background — clean white
val SoftWhite = Color(0xFFFFFFFF)        // surface — white cards
val LeafGreen = Color(0xFFE2F3E7)        // container — soft mint tint (non-text only)
val NaturalGreen = Color(0xFF2E8B57)     // success
val WarmAmber = Color(0xFFF5A623)        // accent / warning / gold (non-text)
val MutedRed = Color(0xFFD9484A)         // error (non-text)
val TextPrimary = Color(0xFF1C1C1C)      // near-black text
val TextSecondary = Color(0xFF6B6B6B)    // muted gray
val AppOutline = Color(0xFFE0E0E0)       // borders, dividers, input outlines

// Dark scheme — "evening in the café"
val DarkBackground = Color(0xFF141712)   // deep roasted brown-black
val DarkSurface = Color(0xFF1F241C)      // warm charcoal-cocoa
val DarkSurfaceHigh = Color(0xFF2C332A)  // lighter cocoa
val DarkText = Color(0xFFF2F5F0)         // warm cream-white
val DarkTextSecondary = Color(0xFFB8C2B4) // muted sage-gray
val DarkPrimary = Color(0xFF5DBB7C)      // lightened green
val DarkAccent = Color(0xFFE8B45B)       // warm gold accent
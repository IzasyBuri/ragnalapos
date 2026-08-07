package com.ragnala.pos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Clean rounded corners — modern café look (12dp cards, 8dp buttons).
val RagnalaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),     // buttons
    medium = RoundedCornerShape(12.dp),   // cards, images
    large = RoundedCornerShape(16.dp),    // dialogs
    extraLarge = RoundedCornerShape(20.dp),// bottom sheets
)
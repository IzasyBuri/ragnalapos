package com.ragnala.pos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RagnalaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(RagnalaRadius.smallControl),
    medium = RoundedCornerShape(RagnalaRadius.button),
    large = RoundedCornerShape(RagnalaRadius.card),
    extraLarge = RoundedCornerShape(RagnalaRadius.bottomSheet),
)

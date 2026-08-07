package com.ragnala.pos.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Gentle steam wisp rising from a cup — cozy slow-life ambience.
 * Draws 3 soft sine wisps that drift upward and fade, on an infinite loop.
 */
@Composable
fun SteamWisp(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF5DBB7C),
) {
    val transition = rememberInfiniteTransition(label = "steam")
    val p1 = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "s1")
    val p2 = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, delayMillis = 700, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "s2")
    val p3 = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, delayMillis = 1400, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "s3")

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun wisp(progress: Float, baseX: Float, amp: Float, alpha: Float) {
            val y = h - progress * h
            val drift = sin(progress * 6.28f + baseX) * amp
            val path = Path().apply {
                moveTo(baseX + drift, y)
                quadraticBezierTo(
                    baseX + drift - amp, y - h * 0.18f,
                    baseX + drift + amp * 0.5f, y - h * 0.36f,
                )
                quadraticBezierTo(
                    baseX + drift + amp, y - h * 0.54f,
                    baseX + drift - amp * 0.3f, y - h * 0.72f,
                )
            }
            drawPath(path, tint.copy(alpha = alpha * (1f - progress) * 0.5f), style = Stroke(width = 3.dp.toPx()))
        }
        wisp(p1.value, w * 0.35f, w * 0.06f, 1f)
        wisp(p2.value, w * 0.5f, w * 0.05f, 1f)
        wisp(p3.value, w * 0.65f, w * 0.06f, 1f)
    }
}

/**
 * A small coin that pops up and settles — playful feedback when a payment lands.
 * [trigger] changing value restarts the pop.
 */
@Composable
fun CoinDrop(
    modifier: Modifier = Modifier,
    trigger: Int = 0,
    tint: Color = Color(0xFFF5A623),
) {
    val transition = rememberInfiniteTransition(label = "coin")
    val rise = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "coinRise")
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val baseY = size.height * 0.8f
        val y = baseY - rise.value * size.height * 0.55f
        val r = size.minDimension * 0.22f * (0.7f + 0.3f * (1f - kotlin.math.abs(rise.value - 0.5f) * 2f))
        // coin body
        drawCircle(tint, r, Offset(cx, y))
        drawCircle(tint.copy(alpha = 0.5f), r * 0.6f, Offset(cx, y))
    }
}

/**
 * Cozy "quest progress" strip: Order → Paid.
 * Filled dots up to and including [currentStep]; the rest are hollow.
 */
@Composable
fun OrderProgressBar(
    currentStep: Int,
    modifier: Modifier = Modifier,
    labels: List<String> = listOf("Order", "Paid"),
    activeColor: Color = Color(0xFF1E7A3D),
    doneColor: Color = Color(0xFFF5A623),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEachIndexed { i, label ->
            val step = i + 1
            val color = when {
                step < currentStep -> doneColor
                step == currentStep -> activeColor
                else -> Color(0xFFDADADA)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    drawCircle(color, radius = size.minDimension / 2f)
                }
                Spacer(Modifier.height(2.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            }
            if (i < labels.lastIndex) {
                Canvas(modifier = Modifier.weight(1f).height(4.dp)) {
                    drawLine(
                        color = if (step < currentStep) doneColor else Color(0xFFDADDD7),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = size.height,
                    )
                }
            }
        }
    }
}

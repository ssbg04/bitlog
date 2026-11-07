package com.crischarles.bitlog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * A pot of water with an egg that bobs and rocks while the timer runs.
 * Steam rises from the water. [active] false freezes the pot and hides the steam.
 */
@Composable
fun BoilingEgg(active: Boolean, size: Dp = 140.dp) {
    val transition = rememberInfiniteTransition(label = "boil")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val bob = if (active) phase else 0f

    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val wobble = if (active) sin(bob * 2f * Math.PI.toFloat()) * 0.04f else 0f
        drawPot(w, h)
        drawWater(w, h, bob)
        translate(left = w * 0.5f, top = h * (0.42f + wobble)) {
            rotate(degrees = if (active) sin(bob * 2f * Math.PI.toFloat()) * 8f else 0f) {
                drawEgg(w * 0.22f)
            }
        }
        if (active) drawSteam(w, h, phase)
    }
}

private fun DrawScope.drawPot(w: Float, h: Float) {
    val potTop = h * 0.55f
    val potBottom = h * 0.92f
    val path = Path().apply {
        moveTo(w * 0.12f, potTop)
        lineTo(w * 0.88f, potTop)
        lineTo(w * 0.80f, potBottom)
        lineTo(w * 0.20f, potBottom)
        close()
    }
    drawPath(path, Color(0xFF455A64))
    drawRect(
        color = Color(0xFF607D8B),
        topLeft = Offset(w * 0.08f, potTop - h * 0.04f),
        size = Size(w * 0.84f, h * 0.05f),
    )
}

private fun DrawScope.drawWater(w: Float, h: Float, phase: Float) {
    val top = h * 0.60f
    val path = Path().apply {
        moveTo(w * 0.16f, top)
        var x = w * 0.16f
        while (x <= w * 0.84f) {
            val y = top + sin((x / w) * 12f + phase * 6.28f) * h * 0.012f
            lineTo(x, y)
            x += w * 0.02f
        }
        lineTo(w * 0.84f, h * 0.88f)
        lineTo(w * 0.16f, h * 0.88f)
        close()
    }
    drawPath(path, Color(0xFF4FC3F7).copy(alpha = 0.85f))
}

private fun DrawScope.drawEgg(radius: Float) {
    drawOval(
        color = Color(0xFFFFF8E7),
        topLeft = Offset(-radius * 0.8f, -radius),
        size = Size(radius * 1.6f, radius * 2f),
    )
}

private fun DrawScope.drawSteam(w: Float, h: Float, phase: Float) {
    for (i in 0 until 3) {
        val offset = (phase + i / 3f) % 1f
        val x = w * (0.38f + i * 0.12f)
        val y = h * (0.46f - offset * 0.3f)
        drawCircle(
            color = Color.White.copy(alpha = (1f - offset) * 0.6f),
            radius = w * 0.04f * (1f + offset),
            center = Offset(x, y),
        )
    }
}

package com.crischarles.bitlog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * High-fidelity animated boiling egg pot.
 * Supports both compact preview and full-screen immersive mode with Apple iOS fluid physics.
 */
@Composable
fun BoilingEgg(
    active: Boolean,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false,
    size: Dp = 140.dp,
    ringing: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "boil_physics")

    // Continuous wave and bobbing cycle
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (ringing) 600 else 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    // Secondary ripple cycle for realistic fluid dynamics
    val ripplePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple",
    )

    val appliedModifier = if (fullscreen) {
        modifier.fillMaxSize()
    } else {
        modifier.size(size)
    }

    Canvas(modifier = appliedModifier) {
        val w = this.size.width
        val h = this.size.height

        val bob = if (active || ringing) phase else 0f
        val wobbleMultiplier = if (ringing) 0.07f else 0.04f
        val wobble = if (active || ringing) sin(bob * 2f * Math.PI.toFloat()) * wobbleMultiplier else 0f

        // If fullscreen, center the pot and water in the lower-middle half with grand scale
        val originX = w * 0.5f
        val originY = if (fullscreen) h * 0.52f else h * 0.42f
        val scale = if (fullscreen) kotlin.math.min(w, h) * 0.72f else w

        // Heat aura under the pot when boiling or ringing
        if (active || ringing) {
            drawHeatGlow(w, h, fullscreen, ringing, phase)
        }

        // Draw boiling pot structure
        drawPot(w, h, scale, fullscreen)

        // Draw animated boiling water with waves
        drawWater(w, h, scale, fullscreen, bob, ripplePhase)

        // Draw rocking bobbing egg
        translate(left = originX, top = originY + (scale * wobble)) {
            val rotAngle = if (active || ringing) {
                sin(bob * 2f * Math.PI.toFloat()) * (if (ringing) 14f else 8f)
            } else {
                0f
            }
            rotate(degrees = rotAngle) {
                drawEgg(scale * 0.22f, ringing)
            }
        }

        // Rising steam clouds & fluid vapor
        if (active || ringing) {
            drawSteam(w, h, scale, fullscreen, phase, ripplePhase, ringing)
        }
    }
}

private fun DrawScope.drawHeatGlow(
    w: Float,
    h: Float,
    fullscreen: Boolean,
    ringing: Boolean,
    phase: Float,
) {
    val glowColor = if (ringing) Color(0xFFFFD700) else Color(0xFFFF6D00)
    val alpha = 0.25f + sin(phase * 2f * Math.PI.toFloat()) * 0.10f
    val potBottom = if (fullscreen) h * 0.76f else h * 0.92f

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = alpha), Color.Transparent),
            center = Offset(w * 0.5f, potBottom),
            radius = if (fullscreen) w * 0.38f else w * 0.45f,
        ),
        topLeft = Offset(w * 0.2f, potBottom - (if (fullscreen) h * 0.06f else h * 0.04f)),
        size = Size(w * 0.6f, if (fullscreen) h * 0.14f else h * 0.12f),
    )
}

private fun DrawScope.drawPot(w: Float, h: Float, scale: Float, fullscreen: Boolean) {
    val potTop = if (fullscreen) h * 0.46f else h * 0.55f
    val potBottom = if (fullscreen) h * 0.75f else h * 0.92f
    val halfWidth = scale * 0.38f

    val path = Path().apply {
        moveTo(w * 0.5f - halfWidth, potTop)
        lineTo(w * 0.5f + halfWidth, potTop)
        lineTo(w * 0.5f + halfWidth * 0.82f, potBottom)
        lineTo(w * 0.5f - halfWidth * 0.82f, potBottom)
        close()
    }

    // Modern glass-ceramic deep slate pot
    drawPath(
        path,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF37474F), Color(0xFF263238)),
            startY = potTop,
            endY = potBottom,
        ),
    )

    // Rim of the pot
    val rimH = if (fullscreen) scale * 0.045f else h * 0.05f
    val rimW = halfWidth * 2.16f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF78909C), Color(0xFF455A64)),
            startY = potTop - rimH,
            endY = potTop,
        ),
        topLeft = Offset(w * 0.5f - rimW / 2f, potTop - rimH * 0.8f),
        size = Size(rimW, rimH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(rimH / 2f, rimH / 2f),
    )
}

private fun DrawScope.drawWater(
    w: Float,
    h: Float,
    scale: Float,
    fullscreen: Boolean,
    phase: Float,
    ripple: Float,
) {
    val top = if (fullscreen) h * 0.49f else h * 0.60f
    val bottom = if (fullscreen) h * 0.73f else h * 0.88f
    val halfWidth = scale * 0.33f

    val path = Path().apply {
        val leftX = w * 0.5f - halfWidth
        val rightX = w * 0.5f + halfWidth
        moveTo(leftX, top)

        var x = leftX
        val step = scale * 0.02f
        while (x <= rightX) {
            val wave1 = sin((x / scale) * 14f + phase * 6.28f) * (scale * 0.016f)
            val wave2 = sin((x / scale) * 24f - ripple * 6.28f) * (scale * 0.008f)
            val y = top + wave1 + wave2
            lineTo(x, y)
            x += step
        }
        lineTo(w * 0.5f + halfWidth * 0.84f, bottom)
        lineTo(w * 0.5f - halfWidth * 0.84f, bottom)
        close()
    }

    drawPath(
        path,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xCC29B6F6), Color(0xEE0288D1)),
            startY = top,
            endY = bottom,
        ),
    )

    // Bubbles inside the boiling water
    for (i in 0 until 5) {
        val bPhase = (phase + i * 0.2f) % 1f
        val bx = w * 0.5f + sin(bPhase * 6.28f + i.toFloat()) * (halfWidth * 0.6f)
        val by = bottom - bPhase * (bottom - top)
        val radius = scale * (0.015f + (i % 3) * 0.006f)
        drawCircle(
            color = Color.White.copy(alpha = (1f - bPhase) * 0.7f),
            radius = radius,
            center = Offset(bx, by),
        )
    }
}

private fun DrawScope.drawEgg(radius: Float, ringing: Boolean) {
    val eggBrush = if (ringing) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFDE7), Color(0xFFFFECB3), Color(0xFFFFD54F)),
            center = Offset(0f, -radius * 0.2f),
            radius = radius * 1.6f,
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFF9E6), Color(0xFFF7E7CE)),
            center = Offset(-radius * 0.2f, -radius * 0.4f),
            radius = radius * 1.5f,
        )
    }

    drawOval(
        brush = eggBrush,
        topLeft = Offset(-radius * 0.78f, -radius),
        size = Size(radius * 1.56f, radius * 2.05f),
    )
}

private fun DrawScope.drawSteam(
    w: Float,
    h: Float,
    scale: Float,
    fullscreen: Boolean,
    phase: Float,
    ripple: Float,
    ringing: Boolean,
) {
    val steamCount = if (fullscreen) 6 else 3
    val steamBaseY = if (fullscreen) h * 0.45f else h * 0.46f

    for (i in 0 until steamCount) {
        val offset = (phase + i.toFloat() / steamCount) % 1f
        val drift = sin(offset * 3.14f + i.toFloat()) * (scale * 0.10f)
        val x = w * 0.5f + ((i - steamCount / 2f) * (scale * 0.12f)) + drift
        val y = steamBaseY - offset * (if (fullscreen) scale * 0.75f else scale * 0.45f)
        val bubbleRadius = scale * 0.045f * (1f + offset * 1.8f)
        val alpha = (1f - offset) * (if (ringing) 0.75f else 0.55f)

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = bubbleRadius,
            center = Offset(x, y),
        )
    }
}

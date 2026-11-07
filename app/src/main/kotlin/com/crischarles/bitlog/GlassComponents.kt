package com.crischarles.bitlog

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.launch

/** True if the device supports hardware RenderEffect and Kyant0 Liquid Glass backdrop. */
val supportsLiquidGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Apple iOS signature spring animation specifications */
val IosInteractiveSpring = spring<Float>(
    dampingRatio = 0.78f,
    stiffness = 380f,
)

val IosIntSizeSpring = spring<IntSize>(
    dampingRatio = 0.78f,
    stiffness = 380f,
)

val IosBouncySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

val IosSheetSpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = 420f,
)

/**
 * Liquid Glass Panel with full optical lens refraction, vibrancy, and specular highlights
 * as featured in Kyant0/AndroidLiquidGlass.
 */
@Composable
fun LiquidGlassPanel(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    corner: Dp = 26.dp,
    surfaceColor: Color = Color.White.copy(alpha = 0.16f),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    val baseModifier = if (supportsLiquidGlass) {
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(10f.dp.toPx())
                lens(14f.dp.toPx(), 28f.dp.toPx())
            },
            highlight = { Highlight.Default },
            onDrawSurface = {
                drawRect(surfaceColor)
            },
        )
    } else {
        modifier
            .clip(shape)
            .background(surfaceColor.copy(alpha = 0.65f))
    }

    Column(
        modifier = baseModifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * Liquid Glass Button featuring Apple iOS interactive spring physics and touch deformation.
 * Replaces Material UI Button completely.
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    corner: Dp = 20.dp,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.White.copy(alpha = 0.20f),
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }
    val currentOnClick by rememberUpdatedState(onClick)
    val shape = RoundedCornerShape(corner)

    val baseModifier = if (supportsLiquidGlass) {
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(6f.dp.toPx())
                lens(10f.dp.toPx(), 20f.dp.toPx())
            },
            highlight = { Highlight.Default },
            layerBlock = {
                scaleX = pressScale.value
                scaleY = pressScale.value
            },
            onDrawSurface = {
                if (tint.isSpecified) {
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = 0.75f))
                }
                drawRect(surfaceColor)
            },
        )
    } else {
        modifier
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .clip(shape)
            .background(if (tint.isSpecified) tint else surfaceColor.copy(alpha = 0.8f))
    }

    Row(
        modifier = baseModifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            pressScale.animateTo(0.94f, IosInteractiveSpring)
                        }
                        tryAwaitRelease()
                        coroutineScope.launch {
                            pressScale.animateTo(1f, IosInteractiveSpring)
                        }
                    },
                    onTap = {
                        if (enabled) currentOnClick()
                    },
                )
            }
            .height(50.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * Circular or capsule Liquid Glass Icon Button with Apple iOS spring feedback.
 */
@Composable
fun LiquidIconButton(
    onClick: () -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    surfaceColor: Color = Color.White.copy(alpha = 0.22f),
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }
    val currentOnClick by rememberUpdatedState(onClick)

    val baseModifier = if (supportsLiquidGlass) {
        modifier.size(size).drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            effects = {
                vibrancy()
                blur(4f.dp.toPx())
                lens(8f.dp.toPx(), 16f.dp.toPx())
            },
            highlight = { Highlight.Default },
            layerBlock = {
                scaleX = pressScale.value
                scaleY = pressScale.value
            },
            onDrawSurface = {
                drawRect(surfaceColor)
            },
        )
    } else {
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .clip(CircleShape)
            .background(surfaceColor.copy(alpha = 0.75f))
    }

    Box(
        modifier = baseModifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            pressScale.animateTo(0.88f, IosInteractiveSpring)
                        }
                        tryAwaitRelease()
                        coroutineScope.launch {
                            pressScale.animateTo(1f, IosInteractiveSpring)
                        }
                    },
                    onTap = {
                        if (enabled) currentOnClick()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Liquid Glass Stepper component for incrementing and decrementing minutes / seconds.
 * Replaces Material UI buttons with an authentic iOS segmented glass pill.
 */
@Composable
fun LiquidStepper(
    label: String,
    value: Int,
    step: Int,
    range: IntRange,
    backdrop: LayerBackdrop,
    textColor: Color,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    val currentOnChange by rememberUpdatedState(onChange)
    val currentValue by rememberUpdatedState(value)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LiquidText(
            text = label,
            style = AppTypography.Headline,
            color = textColor,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LiquidIconButton(
                onClick = { currentOnChange((currentValue - step).coerceIn(range)) },
                backdrop = backdrop,
                size = 38.dp,
                surfaceColor = surfaceColor,
            ) {
                LiquidText(
                    text = "−",
                    style = AppTypography.Title2,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                LiquidText(
                    text = value.toString().padStart(2, '0'),
                    style = AppTypography.Title2,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }

            LiquidIconButton(
                onClick = { currentOnChange((currentValue + step).coerceIn(range)) },
                backdrop = backdrop,
                size = 38.dp,
                surfaceColor = surfaceColor,
            ) {
                LiquidText(
                    text = "+",
                    style = AppTypography.Title2,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
        }
    }
}

/**
 * Full-screen circular liquid progress ring with Apple iOS spring-driven arc.
 */
@Composable
fun LiquidProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = Color.White.copy(alpha = 0.20f),
    progressColor: Color = Color(0xFFFF9500),
) {
    val animatedProgress = animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = IosInteractiveSpring,
        label = "liquid_progress",
    )

    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val strokePx = strokeWidth.toPx()
        val arcRadius = (diameter - strokePx) / 2f
        val arcCenter = Offset(size.width / 2f, size.height / 2f)
        val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
        val topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius)

        // Background track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )

        // Progress arc
        if (animatedProgress.value > 0.001f) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to progressColor,
                    0.5f to Color(0xFFFFB340),
                    1.0f to progressColor,
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress.value * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
    }
}

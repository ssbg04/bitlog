package com.crischarles.bitlog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EggTimerScreen() {
    val context = LocalContext.current
    val timer = remember { EggTimerState() }
    val settings = remember { AppSettings(context) }
    val ring = remember { RingPlayer(context) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var manualMinutes by remember { mutableIntStateOf(5) }
    var manualSeconds by remember { mutableIntStateOf(0) }
    var presetsOpen by remember { mutableStateOf(true) }
    var manualOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var userMinimizedFullscreen by remember { mutableStateOf(false) }

    val manualTotal = manualMinutes * 60 + manualSeconds

    // The backdrop layer for Kyant0 liquid glass shaders
    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFFEEF2FA))
        drawContent()
    }

    // Timer countdown loop
    LaunchedEffect(timer.running) {
        while (timer.running) {
            delay(1000)
            timer.tick()
        }
    }

    // Audio alarm trigger
    LaunchedEffect(timer.ringing) {
        if (timer.ringing) ring.play(settings.ringtoneUri.ifEmpty { null }) else ring.stop()
    }

    DisposableEffect(Unit) {
        onDispose { ring.stop() }
    }

    // Reset minimized state when timer finishes or stops
    LaunchedEffect(timer.totalSeconds) {
        if (timer.totalSeconds == 0) {
            userMinimizedFullscreen = false
        }
    }

    // When timer starts, automatically present full-screen view
    val isTimerActive = timer.running || timer.remainingSeconds > 0 || timer.ringing
    val showFullscreen = isTimerActive && !userMinimizedFullscreen

    // Load background photo and calculate image luminance responsively
    var imageLuminance by remember { mutableFloatStateOf(0.82f) }
    val photo by produceState<ImageBitmap?>(null, settings.backgroundUri) {
        value = if (settings.backgroundUri.isEmpty()) {
            imageLuminance = 0.82f // Built-in pastel gradient luminance
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(settings.backgroundUri))
                        ?.use { stream ->
                            val bitmap = BitmapFactory.decodeStream(stream)
                            if (bitmap != null) {
                                // Calculate responsive average luminance from a downsampled 16x16 grid
                                val sample = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
                                val pixels = IntArray(256)
                                sample.getPixels(pixels, 0, 16, 0, 0, 16, 16)
                                var sum = 0.0
                                for (p in pixels) {
                                    val r = (p shr 16 and 0xFF) / 255.0
                                    val g = (p shr 8 and 0xFF) / 255.0
                                    val b = (p and 0xFF) / 255.0
                                    sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
                                }
                                imageLuminance = (sum / pixels.size).toFloat()
                                bitmap.asImageBitmap()
                            } else null
                        }
                } catch (e: Exception) {
                    imageLuminance = 0.82f
                    null
                }
            }
        }
    }

    // Smoothly animate the luminance for fluid theme adaptation
    val animatedLuminance by animateFloatAsState(
        targetValue = imageLuminance,
        animationSpec = IosInteractiveSpring,
        label = "animated_luminance",
    )

    // Responsive colors based on background image luminance
    val isLightBackground = animatedLuminance > 0.48f
    val textColor = if (isLightBackground) Color(0xFF0F172A) else Color(0xFFF9FAFB)
    val secondaryTextColor = if (isLightBackground) Color(0xFF475569) else Color(0xCCF9FAFB)
    val glassSurfaceColor = if (isLightBackground) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f)
    val accentColor = Color(0xFFFF9500) // Apple signature warm egg amber

    Box(Modifier.fillMaxSize()) {
        // Background canvas with layerBackdrop
        AbstractBackground(photo = photo, backdrop = backdrop)

        // MAIN OVERVIEW SCREEN
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // App Navigation Bar
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    LiquidText(
                        text = "bitlog",
                        style = AppTypography.LargeTitle,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                    LiquidText(
                        text = "Apple Liquid Glass Egg Timer",
                        style = AppTypography.Caption,
                        color = secondaryTextColor,
                    )
                }

                LiquidIconButton(
                    onClick = { showSettings = true },
                    backdrop = backdrop,
                    size = 46.dp,
                    surfaceColor = glassSurfaceColor,
                ) {
                    LiquidText(
                        text = "⚙",
                        style = AppTypography.Title2,
                        color = textColor,
                    )
                }
            }

            // Active or Idle Timer Card
            LiquidGlassPanel(
                backdrop = backdrop,
                surfaceColor = glassSurfaceColor,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        LiquidText(
                            text = if (timer.totalSeconds > 0) timer.label else "No timer running",
                            style = AppTypography.Headline,
                            color = textColor,
                        )
                        LiquidText(
                            text = formatTime(timer.remainingSeconds),
                            style = AppTypography.TimerDisplay,
                            color = textColor,
                        )
                    }

                    BoilingEgg(
                        active = timer.running,
                        ringing = timer.ringing,
                        size = 100.dp,
                    )
                }

                if (timer.ringing) {
                    LiquidText(
                        text = "🍳 Your eggs are perfectly ready!",
                        style = AppTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                    LiquidButton(
                        onClick = { timer.dismissRing() },
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth(),
                        tint = accentColor,
                        surfaceColor = accentColor.copy(alpha = 0.8f),
                    ) {
                        LiquidText(
                            text = "Stop Ringing",
                            style = AppTypography.Headline,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                // Controls row
                if (timer.totalSeconds > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (timer.remainingSeconds > 0) {
                            LiquidButton(
                                onClick = { if (timer.running) timer.pause() else timer.resume() },
                                backdrop = backdrop,
                                modifier = Modifier.weight(1f),
                                tint = if (timer.running) Color(0xFF007AFF) else accentColor,
                                surfaceColor = glassSurfaceColor,
                            ) {
                                LiquidText(
                                    text = if (timer.running) "Pause" else "Resume",
                                    style = AppTypography.Headline,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                        }

                        LiquidButton(
                            onClick = { userMinimizedFullscreen = false },
                            backdrop = backdrop,
                            surfaceColor = glassSurfaceColor,
                        ) {
                            LiquidText(
                                text = "Fullscreen ↗",
                                style = AppTypography.Callout,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                            )
                        }

                        LiquidButton(
                            onClick = {
                                timer.stop()
                                timer.dismissRing()
                                userMinimizedFullscreen = false
                            },
                            backdrop = backdrop,
                            surfaceColor = glassSurfaceColor,
                        ) {
                            LiquidText(
                                text = "Stop",
                                style = AppTypography.Callout,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF3B30),
                            )
                        }
                    }
                }
            }

            // PRESETS ACCORDION
            AnimatedVisibility(
                visible = !manualOpen,
                enter = fadeIn(IosInteractiveSpring),
                exit = fadeOut(IosInteractiveSpring),
            ) {
                LiquidGlassPanel(backdrop = backdrop, surfaceColor = glassSurfaceColor) {
                    AccordionSection(
                        title = "Egg Presets",
                        expanded = presetsOpen,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onToggle = { presetsOpen = !presetsOpen },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EggPresets.forEach { preset ->
                                val isCurrent = timer.running && timer.label == preset.name
                                val presetSurface = if (isCurrent) {
                                    accentColor.copy(alpha = 0.35f)
                                } else {
                                    glassSurfaceColor
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                timer.start(preset.name, preset.seconds)
                                                userMinimizedFullscreen = false
                                            },
                                        )
                                        .background(presetSurface)
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        LiquidText(
                                            text = preset.name,
                                            style = AppTypography.Headline,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                        )
                                        LiquidText(
                                            text = when (preset.name) {
                                                "Soft-boiled" -> "Runny golden yolk, delicate white"
                                                "Medium" -> "Custardy jammy yolk, tender white"
                                                else -> "Fully set yolk, firm white"
                                            },
                                            style = AppTypography.Footnote,
                                            color = secondaryTextColor,
                                        )
                                    }

                                    LiquidText(
                                        text = formatTime(preset.seconds),
                                        style = AppTypography.Title2,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MANUAL TIMER ACCORDION
            LiquidGlassPanel(backdrop = backdrop, surfaceColor = glassSurfaceColor) {
                AccordionSection(
                    title = "Custom Timer",
                    expanded = manualOpen,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    onToggle = { manualOpen = !manualOpen },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LiquidStepper(
                            label = "Minutes",
                            value = manualMinutes,
                            step = 1,
                            range = 0..99,
                            backdrop = backdrop,
                            textColor = textColor,
                            surfaceColor = glassSurfaceColor,
                            onChange = { manualMinutes = it },
                        )

                        LiquidStepper(
                            label = "Seconds",
                            value = manualSeconds,
                            step = 5,
                            range = 0..55,
                            backdrop = backdrop,
                            textColor = textColor,
                            surfaceColor = glassSurfaceColor,
                            onChange = { manualSeconds = it },
                        )

                        LiquidButton(
                            onClick = {
                                timer.start("Custom ${formatTime(manualTotal)}", manualTotal)
                                userMinimizedFullscreen = false
                            },
                            backdrop = backdrop,
                            enabled = manualTotal > 0,
                            modifier = Modifier.fillMaxWidth(),
                            tint = accentColor,
                            surfaceColor = accentColor.copy(alpha = 0.85f),
                        ) {
                            LiquidText(
                                text = "Start Boiling · " + formatTime(manualTotal),
                                style = AppTypography.Headline,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            // HISTORY SECTION
            LiquidGlassPanel(backdrop = backdrop, surfaceColor = glassSurfaceColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    LiquidText(
                        text = "Cooking History",
                        style = AppTypography.Title2,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                    if (timer.history.isNotEmpty()) {
                        LiquidText(
                            text = "Clear",
                            style = AppTypography.Footnote,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF3B30),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { timer.clearHistory() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                if (timer.history.isEmpty()) {
                    LiquidText(
                        text = "No eggs timed yet. Select a preset or custom timer to start boiling!",
                        style = AppTypography.Footnote,
                        color = secondaryTextColor,
                    )
                }

                timer.history.forEach { entry ->
                    val statusText = if (entry.finished) "Completed" else "Stopped"
                    val statusColor = if (entry.finished) Color(0xFF34C759) else Color(0xFFFF9500)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            LiquidText(
                                text = entry.label,
                                style = AppTypography.Callout,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                            )
                            LiquidText(
                                text = "${formatTime(entry.durationSeconds)} · " +
                                    timeFormat.format(Date(entry.timestamp)),
                                style = AppTypography.Caption,
                                color = secondaryTextColor,
                            )
                        }

                        LiquidText(
                            text = statusText,
                            style = AppTypography.Caption,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                        )
                    }
                }
            }
        }

        // FULL SCREEN TIMER ANIMATION WHEN TIMER IS STARTED
        AnimatedVisibility(
            visible = showFullscreen,
            enter = fadeIn(IosInteractiveSpring) + scaleIn(IosInteractiveSpring, initialScale = 0.94f),
            exit = fadeOut(IosInteractiveSpring) + scaleOut(IosInteractiveSpring, targetScale = 0.94f),
        ) {
            FullScreenTimerView(
                timer = timer,
                backdrop = backdrop,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                glassSurfaceColor = glassSurfaceColor,
                accentColor = accentColor,
                onMinimize = { userMinimizedFullscreen = true },
            )
        }
    }

    // Settings Modal Dialog
    if (showSettings) {
        SettingsDialog(
            settings = settings,
            backdrop = backdrop,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            surfaceColor = glassSurfaceColor,
            accentColor = accentColor,
            onDismiss = { showSettings = false },
        )
    }
}

/**
 * Immersive Full Screen Timer View featuring large animated boiling pot,
 * circular liquid progress ring, SF Pro Rounded countdown, and floating liquid action dock.
 */
@Composable
private fun FullScreenTimerView(
    timer: EggTimerState,
    backdrop: LayerBackdrop,
    textColor: Color,
    secondaryTextColor: Color,
    glassSurfaceColor: Color,
    accentColor: Color,
    onMinimize: () -> Unit,
) {
    val progress = if (timer.totalSeconds > 0) {
        1f - (timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat())
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LiquidIconButton(
                onClick = onMinimize,
                backdrop = backdrop,
                size = 44.dp,
                surfaceColor = glassSurfaceColor,
            ) {
                LiquidText(
                    text = "↙",
                    style = AppTypography.Title2,
                    color = textColor,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LiquidText(
                    text = timer.label.uppercase(Locale.getDefault()),
                    style = AppTypography.Caption,
                    fontWeight = FontWeight.Bold,
                    color = secondaryTextColor,
                )
                LiquidText(
                    text = if (timer.ringing) "READY!" else if (timer.running) "BOILING" else "PAUSED",
                    style = AppTypography.Headline,
                    fontWeight = FontWeight.Bold,
                    color = if (timer.ringing) accentColor else textColor,
                )
            }

            LiquidIconButton(
                onClick = {
                    timer.stop()
                    timer.dismissRing()
                },
                backdrop = backdrop,
                size = 44.dp,
                surfaceColor = glassSurfaceColor,
            ) {
                LiquidText(
                    text = "✕",
                    style = AppTypography.Headline,
                    color = Color(0xFFFF3B30),
                )
            }
        }

        // Center: Fullscreen Boiling Egg Animation and Progress Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 70.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Fullscreen boiling egg canvas
            BoilingEgg(
                active = timer.running,
                ringing = timer.ringing,
                fullscreen = true,
                modifier = Modifier.fillMaxSize(),
            )

            // Circular progress ring centered over the boiling display
            LiquidProgressRing(
                progress = progress,
                strokeWidth = 8.dp,
                modifier = Modifier.size(280.dp),
                progressColor = accentColor,
            )

            // Large Digital Countdown in center-top
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LiquidText(
                    text = formatTime(timer.remainingSeconds),
                    style = AppTypography.TimerDisplayFullscreen,
                    color = textColor,
                )
                LiquidText(
                    text = "${(progress * 100).toInt()}% Elapsed",
                    style = AppTypography.Footnote,
                    fontWeight = FontWeight.Medium,
                    color = secondaryTextColor,
                )
            }
        }

        // Bottom Floating Liquid Glass Action Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (timer.ringing) {
                LiquidButton(
                    onClick = { timer.dismissRing() },
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    tint = accentColor,
                    surfaceColor = accentColor.copy(alpha = 0.9f),
                ) {
                    LiquidText(
                        text = "Stop Ringing",
                        style = AppTypography.Title2,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Stop Button
                    LiquidButton(
                        onClick = { timer.stop() },
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f).height(54.dp),
                        surfaceColor = glassSurfaceColor,
                    ) {
                        LiquidText(
                            text = "Reset",
                            style = AppTypography.Headline,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                        )
                    }

                    // Pause / Resume Main Action
                    LiquidButton(
                        onClick = {
                            if (timer.running) timer.pause() else timer.resume()
                        },
                        backdrop = backdrop,
                        modifier = Modifier.weight(1.5f).height(54.dp),
                        tint = if (timer.running) Color(0xFF007AFF) else accentColor,
                        surfaceColor = if (timer.running) Color(0xFF007AFF).copy(alpha = 0.85f) else accentColor.copy(alpha = 0.85f),
                    ) {
                        LiquidText(
                            text = if (timer.running) "❚❚ Pause" else "▶ Resume",
                            style = AppTypography.Headline,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    // +1 Min Button
                    LiquidButton(
                        onClick = {
                            timer.remainingSeconds += 60
                            timer.totalSeconds += 60
                        },
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f).height(54.dp),
                        surfaceColor = glassSurfaceColor,
                    ) {
                        LiquidText(
                            text = "+1m",
                            style = AppTypography.Headline,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Background layer: displays custom image wallpaper or dynamic abstract gradient with soft spheres.
 */
@Composable
private fun AbstractBackground(photo: ImageBitmap?, backdrop: LayerBackdrop) {
    Box(
        Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop),
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFEEF2FA), Color(0xFFDDE7F5), Color(0xFFCAD8EC)),
                        ),
                    ),
            )
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Color(0xFFFFB48A).copy(alpha = 0.55f),
                    radius = size.minDimension * 0.38f,
                    center = Offset(size.width * 0.1f, size.height * 0.15f),
                )
                drawCircle(
                    Color(0xFF8AD7FF).copy(alpha = 0.50f),
                    radius = size.minDimension * 0.44f,
                    center = Offset(size.width * 0.95f, size.height * 0.55f),
                )
                drawCircle(
                    Color(0xFFB9F5A8).copy(alpha = 0.45f),
                    radius = size.minDimension * 0.32f,
                    center = Offset(size.width * 0.25f, size.height * 0.9f),
                )
                drawCircle(
                    Color(0xFFD7B8FF).copy(alpha = 0.40f),
                    radius = size.minDimension * 0.28f,
                    center = Offset(size.width * 0.8f, size.height * 0.05f),
                )
            }
        }
    }
}

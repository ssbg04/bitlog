package com.crischarles.bitlog

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val manualTotal = manualMinutes * 60 + manualSeconds

    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFFEEF2FA))
        drawContent()
    }

    LaunchedEffect(timer.running) {
        while (timer.running) {
            delay(1000)
            timer.tick()
        }
    }

    // Start the alarm when the timer ends, and stop it when the user dismisses it.
    LaunchedEffect(timer.ringing) {
        if (timer.ringing) ring.play(settings.ringtoneUri.ifEmpty { null }) else ring.stop()
    }
    DisposableEffect(Unit) {
        onDispose { ring.stop() }
    }

    Box(Modifier.fillMaxSize()) {
        AbstractBackground(settings.backgroundUri, backdrop)

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Egg Timer", fontSize = 28.sp, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { showSettings = true }) { Text("Settings") }
            }

            GlassPanel(backdrop) {
                BoilingEgg(active = timer.running)
                Text(if (timer.totalSeconds > 0) timer.label else "No timer running", fontSize = 16.sp)
                Text(formatTime(timer.remainingSeconds), fontSize = 64.sp)
                if (timer.ringing) {
                    Text("Your eggs are ready!", fontSize = 18.sp)
                    Button(onClick = { timer.dismissRing() }) { Text("Stop ringing") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (timer.totalSeconds > 0 && timer.remainingSeconds > 0) {
                        Button(onClick = { if (timer.running) timer.pause() else timer.resume() }) {
                            Text(if (timer.running) "Pause" else "Resume")
                        }
                    }
                    if (timer.totalSeconds > 0) {
                        OutlinedButton(onClick = { timer.stop(); timer.dismissRing() }) { Text("Stop") }
                    }
                }
            }

            // Presets hide while the Manual timer is open and come back when it closes.
            AnimatedVisibility(visible = !manualOpen) {
                GlassPanel(backdrop) {
                    AccordionSection(
                        title = "Presets",
                        expanded = presetsOpen,
                        onToggle = { presetsOpen = !presetsOpen },
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            EggPresets.forEach { preset ->
                                GlassPanel(
                                    backdrop,
                                    Modifier.weight(1f).clickable { timer.start(preset.name, preset.seconds) },
                                    corner = 20.dp,
                                ) {
                                    Text(preset.name, fontSize = 14.sp)
                                    Text(formatTime(preset.seconds), fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }

            GlassPanel(backdrop) {
                AccordionSection(
                    title = "Manual timer",
                    expanded = manualOpen,
                    onToggle = { manualOpen = !manualOpen },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stepper("Minutes", manualMinutes, step = 1, range = 0..99) { manualMinutes = it }
                        Stepper("Seconds", manualSeconds, step = 5, range = 0..55) { manualSeconds = it }
                        Button(
                            onClick = { timer.start("Manual " + formatTime(manualTotal), manualTotal) },
                            enabled = manualTotal > 0,
                        ) {
                            Text("Start " + formatTime(manualTotal))
                        }
                    }
                }
            }

            GlassPanel(backdrop) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("History", fontSize = 18.sp, modifier = Modifier.weight(1f))
                    if (timer.history.isNotEmpty()) {
                        TextButton(onClick = { timer.clearHistory() }) { Text("Clear") }
                    }
                }
                if (timer.history.isEmpty()) {
                    Text("No eggs timed yet")
                }
                timer.history.forEach { entry ->
                    val status = if (entry.finished) "Finished" else "Stopped early"
                    Text(
                        "${entry.label} · ${formatTime(entry.durationSeconds)} · $status · " +
                            timeFormat.format(Date(entry.timestamp)),
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
        )
    }
}

/** The user's picked image when one is set, otherwise the built-in abstract gradient with soft circles. */
@Composable
private fun AbstractBackground(imageUri: String, backdrop: LayerBackdrop) {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, imageUri) {
        value = if (imageUri.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(imageUri))
                        ?.use { BitmapFactory.decodeStream(it) }
                        ?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
        val image = photo
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEEF2FA), Color(0xFFDDE7F5)))))
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFFFFB48A).copy(alpha = 0.55f), radius = size.minDimension * 0.35f, center = Offset(size.width * 0.1f, size.height * 0.15f))
                drawCircle(Color(0xFF8AD7FF).copy(alpha = 0.5f), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.95f, size.height * 0.55f))
                drawCircle(Color(0xFFB9F5A8).copy(alpha = 0.45f), radius = size.minDimension * 0.3f, center = Offset(size.width * 0.25f, size.height * 0.9f))
                drawCircle(Color(0xFFD7B8FF).copy(alpha = 0.4f), radius = size.minDimension * 0.25f, center = Offset(size.width * 0.8f, size.height * 0.05f))
            }
        }
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    step: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - step).coerceIn(range)) }) { Text("-") }
        Text(value.toString().padStart(2, '0'), modifier = Modifier.padding(horizontal = 12.dp))
        OutlinedButton(onClick = { onChange((value + step).coerceIn(range)) }) { Text("+") }
    }
}

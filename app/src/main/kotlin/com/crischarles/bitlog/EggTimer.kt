package com.crischarles.bitlog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class EggPreset(
    val name: String,
    val seconds: Int,
    val description: String = "",
)

val EggPresets = listOf(
    EggPreset("Poached", 3 * 60 + 30, "Delicate poached egg, liquid center"),
    EggPreset("Soft-boiled", 6 * 60, "Runny golden yolk, tender delicate white"),
    EggPreset("Jammy / Ramen", 6 * 60 + 45, "Custardy jammy yolk, set white (Ajitsuke Tamago)"),
    EggPreset("Medium", 8 * 60, "Fudgey rich yolk, firm white"),
    EggPreset("Hard-boiled", 10 * 60, "Solid bright yellow yolk, firm white"),
    EggPreset("Onsen Tamago", 13 * 60, "Silky hot spring egg, custard white"),
)

data class HistoryEntry(
    val label: String,
    val durationSeconds: Int,
    val finished: Boolean,
    val timestamp: Long,
)

fun formatTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

/** Holds the countdown and its history. The UI calls [tick] once per second while [running]. */
class EggTimerState {
    var label by mutableStateOf("")
    var totalSeconds by mutableIntStateOf(0)
    var remainingSeconds by mutableIntStateOf(0)
    var running by mutableStateOf(false)
    var ringing by mutableStateOf(false)
    val history = mutableStateListOf<HistoryEntry>()

    /** Silences the alarm without changing the history. */
    fun dismissRing() {
        ringing = false
    }

    fun start(label: String, seconds: Int) {
        if (seconds <= 0) return
        this.label = label
        totalSeconds = seconds
        remainingSeconds = seconds
        running = true
    }

    fun pause() {
        running = false
    }

    fun resume() {
        if (remainingSeconds > 0) running = true
    }

    fun tick() {
        if (!running) return
        if (remainingSeconds > 1) {
            remainingSeconds--
        } else {
            remainingSeconds = 0
            running = false
            ringing = true
            record(finished = true)
        }
    }

    /** Stops the timer. A timer that was still counting down is saved to history as stopped early. */
    fun stop() {
        if (remainingSeconds > 0 && totalSeconds > 0) record(finished = false)
        running = false
        remainingSeconds = 0
        totalSeconds = 0
    }

    fun clearHistory() {
        history.clear()
    }

    private fun record(finished: Boolean) {
        history.add(0, HistoryEntry(label, totalSeconds, finished, System.currentTimeMillis()))
    }
}

package com.crischarles.bitlog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Egg types with their relative sizes and cooking time multipliers.
 * Quail eggs are ~1/5 the size of chicken eggs, duck eggs ~1.5x, ostrich ~24x.
 */
enum class EggType(val displayName: String, val sizeMultiplier: Float, val description: String) {
    QUAIL("Quail", 0.2f, "Tiny delicate eggs, cook in seconds"),
    BANTAM("Bantam", 0.5f, "Small heritage breed eggs"),
    CHICKEN("Chicken", 1.0f, "Standard large chicken eggs"),
    DUCK("Duck", 1.5f, "Richer yolk, larger white"),
    GOOSE("Goose", 3.0f, "Huge eggs, very rich flavor"),
    TURKEY("Turkey", 2.5f, "Similar to duck but larger"),
    OSTRICH("Ostrich", 24.0f, "Massive eggs, feeds a crowd"),
    EMU("Emu", 12.0f, "Dark green shells, rich taste"),
}

/**
 * Base cooking times for CHICKEN eggs (sizeMultiplier = 1.0).
 * Other egg types scale proportionally by their sizeMultiplier.
 */
data class EggPreset(
    val name: String,
    val baseSeconds: Int,
    val description: String = "",
    val eggType: EggType = EggType.CHICKEN,
) {
    fun getCookTimeFor(eggType: EggType): Int {
        return (baseSeconds * eggType.sizeMultiplier).toInt()
    }

    fun getDisplayName(eggType: EggType): String = "${eggType.displayName} ${name}"
}

val EggPresets = listOf(
    EggPreset("Poached", 3 * 60 + 30, "Delicate poached egg, liquid center"),
    EggPreset("Soft-boiled", 6 * 60, "Runny golden yolk, tender delicate white"),
    EggPreset("Jammy / Ramen", 6 * 60 + 45, "Custardy jammy yolk, set white (Ajitsuke Tamago)"),
    EggPreset("Medium", 8 * 60, "Fudgey rich yolk, firm white"),
    EggPreset("Hard-boiled", 10 * 60, "Solid bright yellow yolk, firm white"),
    EggPreset("Onsen Tamago", 13 * 60, "Silky hot spring egg, custard white"),
)

val AllEggTypes = EggType.values()

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

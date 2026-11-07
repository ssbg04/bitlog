package com.crischarles.bitlog

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global singleton managing timer state, application foreground/background status,
 * and dispatching timer commands to [EggTimerService].
 */
object EggTimerManager {
    val state = EggTimerState()

    var isAppForeground by mutableStateOf(true)
        private set

    fun setAppForeground(context: Context, foreground: Boolean) {
        if (isAppForeground != foreground) {
            isAppForeground = foreground
            if (state.running || state.remainingSeconds > 0 || state.ringing) {
                notifyService(context, EggTimerService.ACTION_UPDATE_VISIBILITY)
            }
        }
    }

    fun startTimer(context: Context, label: String, seconds: Int) {
        state.start(label, seconds)
        val intent = Intent(context, EggTimerService::class.java).apply {
            action = EggTimerService.ACTION_START
            putExtra(EggTimerService.EXTRA_LABEL, label)
            putExtra(EggTimerService.EXTRA_SECONDS, seconds)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    fun pauseTimer(context: Context) {
        state.pause()
        notifyService(context, EggTimerService.ACTION_PAUSE)
    }

    fun resumeTimer(context: Context) {
        state.resume()
        notifyService(context, EggTimerService.ACTION_RESUME)
    }

    fun stopTimer(context: Context) {
        state.stop()
        state.dismissRing()
        notifyService(context, EggTimerService.ACTION_STOP)
    }

    fun dismissRing(context: Context) {
        state.dismissRing()
        notifyService(context, EggTimerService.ACTION_DISMISS)
    }

    fun addMinute(context: Context) {
        state.remainingSeconds += 60
        state.totalSeconds += 60
        notifyService(context, EggTimerService.ACTION_ADD_MINUTE)
    }

    private fun notifyService(context: Context, actionStr: String) {
        val intent = Intent(context, EggTimerService::class.java).apply {
            action = actionStr
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }
}


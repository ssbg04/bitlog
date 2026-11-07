package com.crischarles.bitlog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles action button clicks directly from the ongoing or ringing system notification.
 */
class EggTimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            EggTimerService.ACTION_PAUSE -> EggTimerManager.pauseTimer(context)
            EggTimerService.ACTION_RESUME -> EggTimerManager.resumeTimer(context)
            EggTimerService.ACTION_STOP -> EggTimerManager.stopTimer(context)
            EggTimerService.ACTION_DISMISS -> EggTimerManager.dismissRing(context)
        }
    }
}


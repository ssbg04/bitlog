package com.crischarles.bitlog

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * User settings kept in SharedPreferences. An empty string means "use the default":
 * the built-in abstract background and the built-in bell ringtone.
 */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("bitlog_settings", Context.MODE_PRIVATE)

    var backgroundUri by mutableStateOf(prefs.getString(KEY_BACKGROUND, "") ?: "")
        private set

    var ringtoneUri by mutableStateOf(prefs.getString(KEY_RINGTONE, "") ?: "")
        private set

    var notchNotificationEnabled by mutableStateOf(prefs.getBoolean(KEY_NOTCH_NOTIFICATION, true))
        private set

    var systemNotificationEnabled by mutableStateOf(prefs.getBoolean(KEY_SYSTEM_NOTIFICATION, true))
        private set

    fun setBackground(uri: String) {
        prefs.edit().putString(KEY_BACKGROUND, uri).apply()
        backgroundUri = uri
    }

    fun setRingtone(uri: String) {
        prefs.edit().putString(KEY_RINGTONE, uri).apply()
        ringtoneUri = uri
    }

    fun setNotchNotification(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTCH_NOTIFICATION, enabled).apply()
        notchNotificationEnabled = enabled
    }

    fun setSystemNotification(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYSTEM_NOTIFICATION, enabled).apply()
        systemNotificationEnabled = enabled
    }

    private companion object {
        const val KEY_BACKGROUND = "background_uri"
        const val KEY_RINGTONE = "ringtone_uri"
        const val KEY_NOTCH_NOTIFICATION = "notch_notification"
        const val KEY_SYSTEM_NOTIFICATION = "system_notification"
    }
}

package com.crischarles.bitlog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Bulletproof Foreground Service running the egg timer countdown,
 * updating ongoing system notification, managing native camera cutout
 * notch overlay above status bar, and alarming on completion.
 */
class EggTimerService : Service() {

    companion object {
        const val ACTION_START = "com.crischarles.bitlog.ACTION_START"
        const val ACTION_PAUSE = "com.crischarles.bitlog.ACTION_PAUSE"
        const val ACTION_RESUME = "com.crischarles.bitlog.ACTION_RESUME"
        const val ACTION_STOP = "com.crischarles.bitlog.ACTION_STOP"
        const val ACTION_DISMISS = "com.crischarles.bitlog.ACTION_DISMISS"
        const val ACTION_ADD_MINUTE = "com.crischarles.bitlog.ACTION_ADD_MINUTE"
        const val ACTION_UPDATE_VISIBILITY = "com.crischarles.bitlog.ACTION_UPDATE_VISIBILITY"

        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_SECONDS = "extra_seconds"

        private const val NOTIFICATION_ID_ONGOING = 1001
        private const val NOTIFICATION_ID_ALARM = 1002

        private const val CHANNEL_ONGOING = "bitlog_timer_ongoing"
        private const val CHANNEL_ALARM = "bitlog_timer_alarm"
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var notchManager: NotchOverlayManager
    private lateinit var settings: AppSettings
    private lateinit var ringPlayer: RingPlayer
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var isForegroundActive = false
    private val handler = Handler(Looper.getMainLooper())
    private var targetEndTimeMs: Long = 0L

    private val tickRunnable = object : Runnable {
        override fun run() {
            onTick()
            if (EggTimerManager.state.running) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notchManager = NotchOverlayManager(this)
        settings = AppSettings(this)
        ringPlayer = RingPlayer(this)

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {}

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bitlog:timer_wakelock")
        } catch (_: Exception) {}

        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START -> {
                val label = intent.getStringExtra(EXTRA_LABEL) ?: EggTimerManager.state.label
                val seconds = intent.getIntExtra(EXTRA_SECONDS, EggTimerManager.state.totalSeconds)
                handleStart(label, seconds)
            }
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> handleStop()
            ACTION_DISMISS -> handleDismiss()
            ACTION_ADD_MINUTE -> handleAddMinute()
            ACTION_UPDATE_VISIBILITY -> updateVisuals()
        }

        return START_NOT_STICKY
    }

    private fun ensureForeground() {
        if (!isForegroundActive) {
            try {
                val notification = buildOngoingNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID_ONGOING,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                    )
                } else {
                    startForeground(NOTIFICATION_ID_ONGOING, notification)
                }
                isForegroundActive = true
            } catch (_: Exception) {
                try {
                    startForeground(NOTIFICATION_ID_ONGOING, buildOngoingNotification())
                    isForegroundActive = true
                } catch (_: Exception) {}
            }
        }
    }

    private fun handleStart(label: String, seconds: Int) {
        if (seconds <= 0) return
        targetEndTimeMs = System.currentTimeMillis() + (seconds * 1000L)
        acquireWakeLock()
        ensureForeground()

        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
        updateVisuals()
    }

    private fun handlePause() {
        handler.removeCallbacks(tickRunnable)
        updateVisuals()
    }

    private fun handleResume() {
        targetEndTimeMs = System.currentTimeMillis() + (EggTimerManager.state.remainingSeconds * 1000L)
        acquireWakeLock()
        ensureForeground()
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
        updateVisuals()
    }

    private fun handleAddMinute() {
        targetEndTimeMs += 60 * 1000L
        updateVisuals()
    }

    private fun handleStop() {
        handler.removeCallbacks(tickRunnable)
        stopAlarm()
        releaseWakeLock()
        notchManager.remove()
        try {
            if (isForegroundActive) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForegroundActive = false
            }
            notificationManager.cancel(NOTIFICATION_ID_ONGOING)
            notificationManager.cancel(NOTIFICATION_ID_ALARM)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun handleDismiss() {
        stopAlarm()
        releaseWakeLock()
        notchManager.remove()
        try {
            if (isForegroundActive) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForegroundActive = false
            }
            notificationManager.cancel(NOTIFICATION_ID_ONGOING)
            notificationManager.cancel(NOTIFICATION_ID_ALARM)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun onTick() {
        val state = EggTimerManager.state
        if (!state.running) return

        val now = System.currentTimeMillis()
        val remaining = ((targetEndTimeMs - now + 999) / 1000).toInt().coerceAtLeast(0)

        if (remaining > 0) {
            state.remainingSeconds = remaining
            updateVisuals()
        } else {
            state.tick()
            handler.removeCallbacks(tickRunnable)
            triggerAlarm()
            updateVisuals()
        }
    }

    private fun updateVisuals() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { updateVisuals() }
            return
        }

        val state = EggTimerManager.state

        // 1. Update Notification
        if (settings.systemNotificationEnabled) {
            try {
                if (state.ringing) {
                    notificationManager.notify(NOTIFICATION_ID_ALARM, buildAlarmNotification())
                    notificationManager.cancel(NOTIFICATION_ID_ONGOING)
                } else if (state.running || state.remainingSeconds > 0) {
                    notificationManager.notify(NOTIFICATION_ID_ONGOING, buildOngoingNotification())
                }
            } catch (_: Exception) {}
        }

        // 2. Update Native System-level Camera Cutout Notch Overlay
        val shouldShowNotch = settings.notchNotificationEnabled &&
            !EggTimerManager.isAppForeground &&
            (state.running || state.remainingSeconds > 0 || state.ringing) &&
            OverlayPermissionHelper.canDrawOverlays(this)

        try {
            if (shouldShowNotch) {
                notchManager.showOrUpdate(
                    label = state.label,
                    remainingSeconds = state.remainingSeconds,
                    totalSeconds = state.totalSeconds,
                    running = state.running,
                    ringing = state.ringing,
                )
            } else {
                notchManager.remove()
            }
        } catch (_: Exception) {}
    }

    private fun triggerAlarm() {
        try {
            ringPlayer.play(settings.ringtoneUri.ifEmpty { null })
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
                    v.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 500, 300, 500, 300, 500), 0)
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopAlarm() {
        try {
            ringPlayer.stop()
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(30 * 60 * 1000L)
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    private fun buildOngoingNotification(): Notification {
        val state = EggTimerManager.state
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val pauseIntent = Intent(this, EggTimerActionReceiver::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePi = PendingIntent.getBroadcast(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val resumeIntent = Intent(this, EggTimerActionReceiver::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePi = PendingIntent.getBroadcast(
            this, 2, resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, EggTimerActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getBroadcast(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notifIcon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_notification_egg)
        val actionTitle = if (state.running) "Pause" else "Resume"
        val actionPi = if (state.running) pausePi else resumePi
        val pauseResumeAction = Notification.Action.Builder(notifIcon, actionTitle, actionPi).build()
        val stopAction = Notification.Action.Builder(notifIcon, "Stop", stopPi).build()

        val progress = if (state.totalSeconds > 0) {
            (100f * (1f - state.remainingSeconds.toFloat() / state.totalSeconds.toFloat())).toInt().coerceIn(0, 100)
        } else 0

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ONGOING)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(R.drawable.ic_notification_egg)
            .setContentTitle("🍳 Boiling ${state.label} · ${formatTime(state.remainingSeconds)}")
            .setContentText(if (state.running) "$progress% completed · Cooking in progress" else "Timer paused")
            .setProgress(state.totalSeconds, (state.totalSeconds - state.remainingSeconds).coerceAtLeast(0), false)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .setOnlyAlertOnce(true)
            .setColor(0xFFFF9500.toInt())
            .build()
    }

    private fun buildAlarmNotification(): Notification {
        val state = EggTimerManager.state
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissIntent = Intent(this, EggTimerActionReceiver::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPi = PendingIntent.getBroadcast(
            this, 4, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notifIcon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_notification_egg)
        val dismissAction = Notification.Action.Builder(notifIcon, "Stop Ringing", dismissPi).build()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ALARM)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(R.drawable.ic_notification_egg)
            .setContentTitle("🍳 Eggs are Ready!")
            .setContentText("Your ${state.label} eggs are cooked to perfection.")
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(dismissAction)
            .setCategory(Notification.CATEGORY_ALARM)
            .setColor(0xFFFF9500.toInt())
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val ongoingChannel = NotificationChannel(
                    CHANNEL_ONGOING,
                    "Cooking Timer Progress",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows live ongoing countdown while eggs are boiling"
                    setShowBadge(false)
                    setSound(null, null)
                }

                val alarmChannel = NotificationChannel(
                    CHANNEL_ALARM,
                    "Egg Timer Completed Alert",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alerts you with sound and vibration when eggs are ready"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 500)
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                notificationManager.createNotificationChannel(ongoingChannel)
                notificationManager.createNotificationChannel(alarmChannel)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        stopAlarm()
        releaseWakeLock()
        notchManager.remove()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

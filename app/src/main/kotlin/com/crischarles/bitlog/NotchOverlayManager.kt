package com.crischarles.bitlog

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * Manages the native system-level camera cutout pill overlay above the status bar.
 * Uses OEM-aware WindowManager layout parameters, display cutout mode, and supports
 * dynamic resizing between Small Pill and Big Island.
 */
class NotchOverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: NotchOverlayView? = null
    private var isAttached = false
    private var currentLayoutParams: WindowManager.LayoutParams? = null

    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float): Int = (value * density).toInt()

    fun showOrUpdate(
        label: String,
        remainingSeconds: Int,
        totalSeconds: Int,
        running: Boolean,
        ringing: Boolean,
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { showOrUpdate(label, remainingSeconds, totalSeconds, running, ringing) }
            return
        }

        if (!OverlayPermissionHelper.canDrawOverlays(context)) {
            remove()
            return
        }

        if (overlayView == null) {
            overlayView = NotchOverlayView(
                context = context,
                onOpenApp = {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(intent)
                    overlayView?.setMode(NotchDisplayMode.SMALL_PILL)
                },
                onPause = { EggTimerManager.pauseTimer(context) },
                onResume = { EggTimerManager.resumeTimer(context) },
                onStop = { EggTimerManager.stopTimer(context) },
                onDismissRing = { EggTimerManager.dismissRing(context) },
                onAddMinute = { EggTimerManager.addMinute(context) },
                onBoundsChangeRequested = { updateWindowDimensions() },
            ).apply {
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        if (displayMode == NotchDisplayMode.BIG_ISLAND) {
                            setMode(NotchDisplayMode.SMALL_PILL)
                            return@setOnTouchListener true
                        }
                    }
                    false
                }
            }
        }

        overlayView?.update(
            label = label,
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds,
            running = running,
            ringing = ringing,
        )

        if (!isAttached && overlayView != null) {
            attachToWindowManager()
        }
    }

    private fun attachToWindowManager() {
        val view = overlayView ?: return
        val profile = view.profile
        val statusBarHeight = getStatusBarHeight()

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            dp(profile.smallPill.heightDp),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Hugs the physical camera notch / punch hole directly above the status bar
            y = (statusBarHeight * 0.12f).toInt().coerceAtLeast(dp(profile.smallPill.topOffsetDp))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        try {
            windowManager.addView(view, lp)
            currentLayoutParams = lp
            isAttached = true
        } catch (_: Exception) {
            isAttached = false
        }
    }

    private fun updateWindowDimensions() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { updateWindowDimensions() }
            return
        }

        val view = overlayView ?: return
        val lp = currentLayoutParams ?: return
        if (!isAttached) return

        val profile = view.profile
        val statusBarHeight = getStatusBarHeight()

        if (view.displayMode == NotchDisplayMode.BIG_ISLAND) {
            lp.width = dp(profile.bigIsland.widthDp)
            lp.height = dp(profile.bigIsland.heightDp)
            lp.y = (statusBarHeight * 0.12f).toInt().coerceAtLeast(dp(profile.bigIsland.topOffsetDp))
        } else {
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT
            lp.height = dp(profile.smallPill.heightDp)
            lp.y = (statusBarHeight * 0.12f).toInt().coerceAtLeast(dp(profile.smallPill.topOffsetDp))
        }

        try {
            windowManager.updateViewLayout(view, lp)
        } catch (_: Exception) {}
    }

    fun remove() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { remove() }
            return
        }

        val view = overlayView
        val wasAttached = isAttached
        isAttached = false
        overlayView = null
        currentLayoutParams = null

        if (wasAttached && view != null) {
            // Post removal so any active touch event dispatch finishes safely
            mainHandler.post {
                try {
                    if (view.isAttachedToWindow) {
                        windowManager.removeView(view)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun getStatusBarHeight(): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) {
            context.resources.getDimensionPixelSize(resId)
        } else {
            dp(28f)
        }
    }
}

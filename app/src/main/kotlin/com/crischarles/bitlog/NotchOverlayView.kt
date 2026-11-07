package com.crischarles.bitlog

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

enum class NotchDisplayMode {
    SMALL_PILL,
    BIG_ISLAND
}

/**
 * Native system-level camera cutout pill overlay supporting both compact Small Pill
 * and expanded Big Island states with per-OS flavor JSON metadata.
 */
@SuppressLint("ViewConstructor")
class NotchOverlayView(
    context: Context,
    private val onOpenApp: () -> Unit,
    private val onPause: () -> Unit,
    private val onResume: () -> Unit,
    private val onStop: () -> Unit,
    private val onDismissRing: () -> Unit,
    private val onAddMinute: () -> Unit,
    private val onBoundsChangeRequested: () -> Unit,
) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private fun dp(value: Float): Int = (value * density).toInt()

    val profile = NotchProfileManager.getProfile(context)
    var displayMode = NotchDisplayMode.SMALL_PILL
        private set

    // Drawing paints
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val pillRect = RectF()

    // Small Pill UI components
    private val smallPillLayout: LinearLayout
    private val smallEggIcon: MiniEggView
    private val smallLabelText: TextView
    private val smallTimeText: TextView
    private val smallProgressView: MiniProgressView

    // Big Island UI components
    private val bigIslandLayout: LinearLayout
    private val bigLabelText: TextView
    private val bigStatusText: TextView
    private val bigTimeText: TextView
    private val bigSubText: TextView
    private val bigActionBtn1: TextView
    private val bigActionBtn2: TextView
    private val bigActionBtn3: TextView

    private var currentLabel = ""
    private var currentRemaining = 0
    private var currentTotal = 0
    private var isRunning = false
    private var isRinging = false

    init {
        setWillNotDraw(false)

        // 1. SMALL PILL CONTAINER
        smallPillLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(profile.smallPill.paddingHorizontalDp), dp(4f), dp(profile.smallPill.paddingHorizontalDp), dp(4f))
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(profile.smallPill.heightDp))
        }

        smallEggIcon = MiniEggView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(22f), dp(22f)).apply {
                marginEnd = dp(8f)
            }
        }
        smallPillLayout.addView(smallEggIcon)

        val smallTextLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        smallLabelText = TextView(context).apply {
            textSize = 9f
            setTextColor(Color.parseColor("#9E9EA7"))
            typeface = Typeface.DEFAULT_BOLD
            isSingleLine = true
            letterSpacing = 0.06f
        }
        smallTextLayout.addView(smallLabelText)

        smallTimeText = TextView(context).apply {
            textSize = 13.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            isSingleLine = true
        }
        smallTextLayout.addView(smallTimeText)
        smallPillLayout.addView(smallTextLayout)

        smallProgressView = MiniProgressView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(20f), dp(20f)).apply {
                marginStart = dp(10f)
            }
        }
        smallPillLayout.addView(smallProgressView)
        addView(smallPillLayout)

        // 2. BIG ISLAND CONTAINER
        bigIslandLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(18f), dp(14f), dp(18f), dp(14f))
            layoutParams = LayoutParams(dp(profile.bigIsland.widthDp), dp(profile.bigIsland.heightDp))
        }

        // Top Row: Label, Status, and Collapse Button
        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val topTextCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        bigLabelText = TextView(context).apply {
            textSize = 10.5f
            setTextColor(Color.parseColor("#A0A0A8"))
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            isSingleLine = true
        }
        topTextCol.addView(bigLabelText)

        bigStatusText = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#FFA500"))
            typeface = Typeface.DEFAULT_BOLD
        }
        topTextCol.addView(bigStatusText)
        topRow.addView(topTextCol)

        val collapseBtn = TextView(context).apply {
            text = "↙"
            textSize = 16f
            setTextColor(Color.parseColor("#909096"))
            setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
            setOnClickListener { setMode(NotchDisplayMode.SMALL_PILL) }
        }
        topRow.addView(collapseBtn)
        bigIslandLayout.addView(topRow)

        // Center Row: Big Countdown & Info
        val centerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            setOnClickListener { onOpenApp() }
        }

        bigTimeText = TextView(context).apply {
            textSize = 34f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        centerRow.addView(bigTimeText)

        bigSubText = TextView(context).apply {
            textSize = 11.5f
            setTextColor(Color.parseColor("#8E8E93"))
            text = "Open App ↗"
            setPadding(dp(8f), dp(6f), dp(8f), dp(6f))
            setOnClickListener { onOpenApp() }
        }
        centerRow.addView(bigSubText)
        bigIslandLayout.addView(centerRow)

        // Bottom Controls Row
        val controlsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        bigActionBtn1 = createBigButton(context, "Stop", Color.parseColor("#44FF3B30")) {
            if (isRinging) onDismissRing() else onStop()
        }
        controlsRow.addView(bigActionBtn1)

        val spacer1 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(8f), 1)
        }
        controlsRow.addView(spacer1)

        bigActionBtn2 = createBigButton(context, "Pause", Color.parseColor("#44007AFF")) {
            if (isRinging) {
                onDismissRing()
            } else if (isRunning) {
                onPause()
            } else {
                onResume()
            }
        }
        controlsRow.addView(bigActionBtn2)

        val spacer2 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(8f), 1)
        }
        controlsRow.addView(spacer2)

        bigActionBtn3 = createBigButton(context, "+1m", Color.parseColor("#33FFFFFF")) {
            onAddMinute()
        }
        controlsRow.addView(bigActionBtn3)

        bigIslandLayout.addView(controlsRow)
        addView(bigIslandLayout)

        setOnLongClickListener {
            if (displayMode == NotchDisplayMode.SMALL_PILL) {
                onOpenApp()
                true
            } else false
        }
    }

    private fun createBigButton(
        ctx: Context,
        label: String,
        tintColor: Int,
        onClickAction: () -> Unit,
    ): TextView {
        val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(14f).toFloat()
            setColor(tintColor)
        }
        return TextView(ctx).apply {
            text = label
            textSize = 12.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            background = bgDrawable
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClickAction() }
        }
    }

    fun setMode(mode: NotchDisplayMode) {
        if (displayMode == mode) return
        displayMode = mode

        if (mode == NotchDisplayMode.BIG_ISLAND) {
            smallPillLayout.visibility = View.GONE
            bigIslandLayout.visibility = View.VISIBLE
        } else {
            bigIslandLayout.visibility = View.GONE
            smallPillLayout.visibility = View.VISIBLE
        }
        onBoundsChangeRequested()
        invalidate()
    }

    fun update(
        label: String,
        remainingSeconds: Int,
        totalSeconds: Int,
        running: Boolean,
        ringing: Boolean,
    ) {
        this.currentLabel = label
        this.currentRemaining = remainingSeconds
        this.currentTotal = totalSeconds
        this.isRunning = running
        this.isRinging = ringing

        val progress = if (totalSeconds > 0) {
            (1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())).coerceIn(0f, 1f)
        } else 0f

        // Update Small Pill
        smallLabelText.text = label.uppercase()
        if (ringing) {
            smallTimeText.text = "READY! 🍳"
            smallTimeText.setTextColor(Color.parseColor("#FFA500"))
        } else if (!running && remainingSeconds > 0) {
            smallTimeText.text = "${formatTime(remainingSeconds)} ❚❚"
            smallTimeText.setTextColor(Color.parseColor("#0A84FF"))
        } else {
            smallTimeText.text = formatTime(remainingSeconds)
            smallTimeText.setTextColor(Color.WHITE)
        }
        smallEggIcon.setState(running = running, ringing = ringing)
        smallProgressView.setProgress(progress, ringing)

        // Update Big Island
        bigLabelText.text = label.uppercase()
        bigStatusText.text = if (ringing) "READY TO EAT!" else if (running) "BOILING ACTIVE" else "PAUSED"
        bigStatusText.setTextColor(if (ringing) Color.parseColor("#FF9500") else if (running) Color.parseColor("#34C759") else Color.parseColor("#0A84FF"))
        bigTimeText.text = formatTime(remainingSeconds)
        bigActionBtn2.text = if (ringing) "Dismiss" else if (running) "Pause" else "Resume"
        bigActionBtn1.text = if (ringing) "Stop Alarm" else "Reset"

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val radius: Float
        if (displayMode == NotchDisplayMode.SMALL_PILL) {
            radius = dp(profile.smallPill.cornerRadiusDp).toFloat()
            bgPaint.color = profile.smallPill.backgroundColor
            borderPaint.color = profile.smallPill.borderColor
            borderPaint.strokeWidth = dp(profile.smallPill.borderWidthDp).toFloat()
        } else {
            radius = dp(profile.bigIsland.cornerRadiusDp).toFloat()
            bgPaint.color = profile.bigIsland.backgroundColor
            borderPaint.color = profile.bigIsland.borderColor
            borderPaint.strokeWidth = dp(profile.bigIsland.borderWidthDp).toFloat()
        }

        val stroke = borderPaint.strokeWidth / 2f
        pillRect.set(stroke, stroke, width.toFloat() - stroke, height.toFloat() - stroke)
        canvas.drawRoundRect(pillRect, radius, radius, bgPaint)
        canvas.drawRoundRect(pillRect, radius, radius, borderPaint)
        super.onDraw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (displayMode == NotchDisplayMode.SMALL_PILL) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    animate().scaleX(0.94f).scaleY(0.94f).setDuration(100).start()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    val clickSlop = dp(24f)
                    if (event.x in -clickSlop.toFloat()..(width.toFloat() + clickSlop) &&
                        event.y in -clickSlop.toFloat()..(height.toFloat() + clickSlop)) {
                        // Expand into Big Island when tapped!
                        setMode(NotchDisplayMode.BIG_ISLAND)
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    inner class MiniEggView(ctx: Context) : View(ctx) {
        private var pulseScale = 1.0f
        private var isRinging = false
        private var isRunning = false

        private val animator = ValueAnimator.ofFloat(0.85f, 1.15f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                pulseScale = it.animatedValue as Float
                invalidate()
            }
        }

        private val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        private val yolkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9500")
            style = Paint.Style.FILL
        }

        fun setState(running: Boolean, ringing: Boolean) {
            this.isRunning = running
            this.isRinging = ringing
            if (running || ringing) {
                if (!animator.isRunning) animator.start()
            } else {
                if (animator.isRunning) animator.cancel()
                pulseScale = 1.0f
                invalidate()
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            animator.cancel()
        }

        private val eggOval = RectF()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            eggOval.set(cx - dp(6.5f), cy - dp(8.5f), cx + dp(6.5f), cy + dp(8.5f))
            canvas.drawOval(eggOval, shellPaint)

            val yolkRadius = dp(3.8f) * if (isRunning || isRinging) pulseScale else 1.0f
            yolkPaint.color = if (isRinging) Color.parseColor("#FF3B30") else Color.parseColor("#FF9500")
            canvas.drawCircle(cx, cy + dp(1.5f), yolkRadius, yolkPaint)
        }
    }

    inner class MiniProgressView(ctx: Context) : View(ctx) {
        private var progressFraction = 0f
        private var isRinging = false

        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#28FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = dp(2f).toFloat()
        }

        private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9500")
            style = Paint.Style.STROKE
            strokeWidth = dp(2.2f).toFloat()
            strokeCap = Paint.Cap.ROUND
        }

        private val arcRect = RectF()

        fun setProgress(fraction: Float, ringing: Boolean) {
            progressFraction = fraction
            isRinging = ringing
            progressPaint.color = if (ringing) Color.parseColor("#34C759") else Color.parseColor("#FF9500")
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val stroke = progressPaint.strokeWidth / 2f
            arcRect.set(stroke, stroke, width.toFloat() - stroke, height.toFloat() - stroke)
            canvas.drawOval(arcRect, trackPaint)
            val sweepAngle = 360f * progressFraction
            canvas.drawArc(arcRect, -90f, sweepAngle, false, progressPaint)
        }
    }
}

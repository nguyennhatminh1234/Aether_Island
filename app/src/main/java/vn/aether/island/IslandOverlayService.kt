package vn.aether.island

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

class IslandOverlayService : Service() {
    private enum class IslandState { HIDE, COMPACT, EXPANDED }

    companion object {
        private const val CHANNEL_ID = "aether_island_overlay"
        private const val NOTIFICATION_ID = 101
        const val ACTION_STOP = "vn.aether.island.STOP"
    }

    private lateinit var windowManager: WindowManager
    private var islandView: IslandView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopIsland()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (islandView == null) {
            val view = IslandView()
            islandView = view
            val params = WindowManager.LayoutParams(
                view.widthPx,
                view.currentWindowHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = 0
                y = 0
            }
            layoutParams = params
            try {
                windowManager.addView(view, params)
                view.animateEntrance()
            } catch (_: Exception) {
                islandView = null
                layoutParams = null
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aether Island",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Island overlay available while it is running."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Aether Island is active")
            .setContentText("Open Aether Island and tap Stop Island to close the overlay.")
            .setOngoing(true)
            .build()
    }

    private fun updateOverlaySize(width: Int, height: Int) {
        val view = islandView ?: return
        val params = layoutParams ?: return
        if (params.width == width && params.height == height) return
        params.width = width
        params.height = height
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
            stopIsland()
            stopSelf()
        }
    }

    private fun stopIsland() {
        islandView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
        }
        islandView = null
        layoutParams = null
    }

    override fun onDestroy() {
        stopIsland()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class IslandView : View(this@IslandOverlayService) {
        private val density = resources.displayMetrics.density
        private fun dp(value: Float) = value * density
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bounds = RectF()
        private var state = IslandState.HIDE
        private var animatedWidth = dp(42f)
        private var animatedHeight = dp(7f)
        private var animatedRadius = dp(4f)
        private var downX = 0f
        private var downY = 0f
        private var moved = false
        private var animator: ValueAnimator? = null

        val widthPx: Int get() = dp(48f).toInt()
        val currentWindowHeight: Int get() = dp(12f).toInt()

        init {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isClickable = true
        }

        fun animateEntrance() {
            animateTo(IslandState.COMPACT, 520L, true)
            postDelayed({
                if (state == IslandState.COMPACT) animateTo(IslandState.HIDE)
            }, 2600L)
        }

        private fun animateTo(target: IslandState, duration: Long = 360L, entrance: Boolean = false) {
            animator?.cancel()
            state = target
            this@IslandOverlayService.layoutParams?.let { params ->
                val targetY = if (target == IslandState.HIDE) -dp(5f).toInt() else 0
                if (params.y != targetY) {
                    params.y = targetY
                    try { windowManager.updateViewLayout(this, params) } catch (_: Exception) { }
                }
            }
            val startW = animatedWidth
            val startH = animatedHeight
            val startR = animatedRadius
            val endW = when (target) {
                IslandState.HIDE -> dp(42f)
                IslandState.COMPACT -> dp(132f)
                IslandState.EXPANDED -> dp(300f)
            }
            val endH = when (target) {
                IslandState.HIDE -> dp(7f)
                IslandState.COMPACT -> dp(38f)
                IslandState.EXPANDED -> dp(142f)
            }
            val endR = endH / 2f
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration
                interpolator = if (target == IslandState.EXPANDED || entrance) OvershootInterpolator(1.15f)
                    else DecelerateInterpolator()
                addUpdateListener {
                    val f = it.animatedValue as Float
                    animatedWidth = startW + (endW - startW) * f
                    animatedHeight = startH + (endH - startH) * f
                    animatedRadius = startR + (endR - startR) * f
                    updateOverlaySize(
                        (animatedWidth + dp(6f)).toInt(),
                        (animatedHeight + dp(4f)).toInt()
                    )
                    invalidate()
                }
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val centerX = width / 2f
            val left = centerX - animatedWidth / 2f
            val contentTop = dp(2f)
            bounds.set(left, contentTop, left + animatedWidth, contentTop + animatedHeight)

            paint.color = android.graphics.Color.BLACK
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(bounds, animatedRadius, animatedRadius, paint)

            if (state != IslandState.HIDE && animatedHeight > dp(20f)) {
                paint.color = android.graphics.Color.WHITE
                paint.textAlign = Paint.Align.LEFT
                paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                paint.textSize = dp(12f)
                canvas.drawText("AETHER ISLAND", left + dp(18f), contentTop + dp(19f), paint)

                paint.color = 0xFFB8C7FF.toInt()
                canvas.drawCircle(left + dp(19f), contentTop + dp(29f), dp(3f), paint)

                paint.color = 0xFFB7C0D0.toInt()
                paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                paint.textSize = dp(10f)
                canvas.drawText(
                    if (state == IslandState.EXPANDED) "No active activities yet" else "Ready",
                    left + dp(29f), contentTop + dp(32f), paint
                )

                if (state == IslandState.EXPANDED) {
                    paint.color = 0xFF697386.toInt()
                    paint.textSize = dp(9f)
                    canvas.drawText("Tap to collapse  ·  Swipe up to hide", left + dp(18f), contentTop + dp(55f), paint)
                    paint.color = 0xFF303846.toInt()
                    canvas.drawRoundRect(
                        left + dp(18f), contentTop + dp(70f), left + animatedWidth - dp(18f), contentTop + dp(71f),
                        dp(1f), dp(1f), paint
                    )
                    paint.color = 0xFF8F9AAF.toInt()
                    paint.textSize = dp(10f)
                    canvas.drawText("Island Core  /  v0.1", left + dp(18f), contentTop + dp(91f), paint)
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(event.rawY - downY) > dp(12f) || abs(event.rawX - downX) > dp(12f)) moved = true
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val dy = event.rawY - downY
                    if (moved && dy < -dp(35f)) {
                        animateTo(IslandState.HIDE)
                    } else if (!moved) {
                        when (state) {
                            IslandState.HIDE -> animateTo(IslandState.COMPACT, 420L, true)
                            IslandState.COMPACT -> animateTo(IslandState.EXPANDED)
                            IslandState.EXPANDED -> animateTo(IslandState.COMPACT)
                        }
                    }
                    performClick()
                    return true
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }
}

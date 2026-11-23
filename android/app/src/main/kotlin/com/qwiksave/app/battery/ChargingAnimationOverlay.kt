package com.qwiksave.app.battery

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fullscreen charging surge shockwave animation displayed when charger is connected.
 */
@SuppressLint("ClickableViewAccessibility")
class ChargingAnimationOverlay(
    context: Context,
    private val windowManager: WindowManager,
    private val onDismiss: () -> Unit
) : View(context) {

    var batteryPercent: Int = 88
    var template: BatteryTemplateItem = BatteryTemplates.getById("gear5_luffy")

    private var animProgress = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3200L
        addUpdateListener {
            animProgress = it.animatedValue as Float
            invalidate()
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    init {
        animator.start()
        handler.postDelayed({
            dismissSafely()
        }, 3400L)

        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_DOWN) {
                dismissSafely()
                true
            } else false
        }
    }

    private fun dismissSafely() {
        try {
            animator.cancel()
            onDismiss()
        } catch (_: Exception) {}
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val cy = h / 2f

        // 1. Semi-translucent dark cinematic backdrop with fade
        val bgAlpha = ((1f - animProgress) * 210).toInt().coerceIn(0, 215)
        canvas.drawColor(Color.argb(bgAlpha, 10, 15, 30))

        // 2. Expanding multiple shockwave rings
        for (i in 0..3) {
            val ringPhase = (animProgress * 2.2f + (i * 0.28f)) % 1f
            val radius = 90f + (ringPhase * 260f)
            val ringAlpha = ((1f - ringPhase) * 180).toInt().coerceIn(0, 255)
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(ringAlpha, Color.red(template.glowColor.toInt()), Color.green(template.glowColor.toInt()), Color.blue(template.glowColor.toInt()))
                style = Paint.Style.STROKE
                strokeWidth = (4f * (1f - ringPhase)).coerceAtLeast(1f)
            }
            canvas.drawCircle(cx, cy, radius, ringPaint)
        }

        // 3. Central Glowing Energy Core
        val coreRadius = 88f
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(240, 15, 23, 42) // Obsidian core
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, coreRadius, corePaint)

        val coreBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, Color.red(template.glowColor.toInt()), Color.green(template.glowColor.toInt()), Color.blue(template.glowColor.toInt()))
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(cx, cy, coreRadius, coreBorder)

        // 4. Rotating Energy Particles around Core
        for (p in 0 until 12) {
            val angle = (animProgress * Math.PI * 4 + (p * (Math.PI / 6))).toFloat()
            val px = cx + cos(angle) * (coreRadius + 16f)
            val py = cy + sin(angle) * (coreRadius + 16f)
            val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 250, 204, 21) // golden spark
            }
            canvas.drawCircle(px, py, 3.5f, particlePaint)
        }

        // 5. Battery Percentage Number
        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pctY = cy - 2f - (pctPaint.fontMetrics.descent + pctPaint.fontMetrics.ascent) / 2f
        canvas.drawText("$batteryPercent%", cx, pctY, pctPaint)

        // 6. Charging status line
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(250, 204, 21) // Amber-400
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⚡ TURBO CHARGING CONNECTED", cx, cy + coreRadius + 42f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225) // Slate-300
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${template.name} • Tap anywhere to dismiss", cx, cy + coreRadius + 64f, subPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
        animator.cancel()
    }
}

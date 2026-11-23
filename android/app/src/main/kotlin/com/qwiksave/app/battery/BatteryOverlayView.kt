package com.qwiksave.app.battery

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.qwiksave.app.MainActivity
import kotlin.math.sin

/**
 * High-performance, hardware-accelerated custom view that renders
 * anime, emoji, and cyber battery overlays over the Android status bar.
 */
@SuppressLint("ClickableViewAccessibility")
class BatteryOverlayView(
    context: Context,
    private val windowManager: WindowManager? = null,
    private val layoutParams: WindowManager.LayoutParams? = null
) : View(context) {

    var batteryPercent: Int = 88
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var isCharging: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var template: BatteryTemplateItem = BatteryTemplates.getById("gear5_luffy")
        set(value) {
            field = value
            invalidate()
        }

    var scaleFactor: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.7f, 1.5f)
            requestLayout()
            invalidate()
        }

    private var animPhase: Float = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1400L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            animPhase = it.animatedValue as Float
            invalidate()
        }
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    // Drag-to-reposition variables
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    init {
        animator.start()

        setOnTouchListener { _, event ->
            if (windowManager == null || layoutParams == null) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.hypot(dx.toDouble(), dy.toDouble()) > 10) {
                        isDragging = true
                        layoutParams.x = initialX - dx
                        layoutParams.y = initialY + dy
                        windowManager.updateViewLayout(this, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Single tap opens QwikSave Battery Studio
                        try {
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("OPEN_BATTERY_STUDIO", true)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val w = (112 * density * scaleFactor).toInt()
        val h = (46 * density * scaleFactor).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val pulse = (sin(animPhase * Math.PI * 2) * 0.5f + 0.5f).toFloat()

        // 1. Draw Angled Glowing Battery Percentage Capsule (Tilted like reference image)
        canvas.save()
        val capsuleCenterX = w * 0.64f
        val capsuleCenterY = h * 0.58f
        canvas.rotate(-16f, capsuleCenterX, capsuleCenterY)

        val capW = 64f * scaleFactor
        val capH = 26f * scaleFactor
        val capLeft = capsuleCenterX - capW / 2f
        val capTop = capsuleCenterY - capH / 2f
        val capRect = RectF(capLeft, capTop, capLeft + capW, capTop + capH)
        val capCorner = capH / 2f

        // Outer glow aura around capsule
        val glowAlpha = if (isCharging) (140 + (pulse * 95)).toInt() else 75
        glowPaint.color = Color.argb(
            glowAlpha,
            Color.red(template.glowColor.toInt()),
            Color.green(template.glowColor.toInt()),
            Color.blue(template.glowColor.toInt())
        )
        glowPaint.strokeWidth = 3.5f * scaleFactor
        canvas.drawRoundRect(capRect, capCorner, capCorner, glowPaint)

        // Glass capsule fill
        bgPaint.color = Color.argb(235, 15, 23, 42) // Slate-900 92% opaque
        canvas.drawRoundRect(capRect, capCorner, capCorner, bgPaint)

        // Subtle gradient tint inside capsule
        val innerTint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                if (isCharging) 85 else 45,
                Color.red(template.primaryColor.toInt()),
                Color.green(template.primaryColor.toInt()),
                Color.blue(template.primaryColor.toInt())
            )
        }
        canvas.drawRoundRect(capRect, capCorner, capCorner, innerTint)

        // Capsule border
        borderPaint.color = Color.argb(
            245,
            Color.red(template.glowColor.toInt()),
            Color.green(template.glowColor.toInt()),
            Color.blue(template.glowColor.toInt())
        )
        borderPaint.strokeWidth = 1.6f * scaleFactor
        canvas.drawRoundRect(capRect, capCorner, capCorner, borderPaint)

        // Battery percentage text inside angled capsule
        textPaint.color = Color.WHITE
        textPaint.textSize = (capH * 0.58f)
        textPaint.textAlign = Paint.Align.CENTER
        val textY = capsuleCenterY - (textPaint.fontMetrics.descent + textPaint.fontMetrics.ascent) / 2f
        val pctString = if (isCharging) "$batteryPercent% ⚡" else "$batteryPercent%"
        canvas.drawText(pctString, capsuleCenterX, textY, textPaint)

        canvas.restore()

        // 2. Draw Anime Character / Shimeji Avatar sitting on the Top-Left of the capsule
        val avatarRadius = 15f * scaleFactor
        val avatarCenterX = 18f * scaleFactor
        val avatarCenterY = 19f * scaleFactor

        // Pulsing flame/electric aura around avatar
        val avatarAuraAlpha = if (isCharging) (160 + (pulse * 85)).toInt() else 90
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                avatarAuraAlpha,
                Color.red(template.glowColor.toInt()),
                Color.green(template.glowColor.toInt()),
                Color.blue(template.glowColor.toInt())
            )
            style = Paint.Style.STROKE
            strokeWidth = (2.8f + pulse * 1.5f) * scaleFactor
        }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius + 2.5f * scaleFactor, auraPaint)

        // Dark circle background for avatar
        val avatarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(240, 30, 27, 75) // Deep indigo-950
            style = Paint.Style.FILL
        }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBgPaint)

        // Avatar rim border
        val avatarRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                255,
                Color.red(template.primaryColor.toInt()),
                Color.green(template.primaryColor.toInt()),
                Color.blue(template.primaryColor.toInt())
            )
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * scaleFactor
        }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarRimPaint)

        // Character Emoji / Icon inside Avatar Circle
        val activeEmoji = when {
            template.id == "gear5_luffy" -> "👒"
            template.id == "goku_saiyan" -> "🔥"
            template.id == "tanjiro_sun" -> "⚔️"
            template.id == "naruto_sage" -> "🦊"
            template.id == "kawaii_neko" -> "🐱"
            template.id == "sparkle_heart" -> "💖"
            template.id == "dynamic_emoji" -> when {
                batteryPercent >= 80 -> "😎"
                batteryPercent >= 50 -> "😊"
                batteryPercent >= 25 -> "😮"
                else -> "😱"
            }
            template.id == "speed_turbo" -> "🏎️"
            template.id == "reactor_core" -> "⚛️"
            else -> "⚡"
        }

        iconPaint.textSize = (avatarRadius * 1.35f)
        val iconY = avatarCenterY - (iconPaint.fontMetrics.descent + iconPaint.fontMetrics.ascent) / 2f
        canvas.drawText(activeEmoji, avatarCenterX, iconY, iconPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}

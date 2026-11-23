package com.qwiksave.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.qwiksave.app.MainActivity
import com.qwiksave.app.R
import com.qwiksave.app.battery.BatteryOverlayView
import com.qwiksave.app.battery.BatteryTemplates
import com.qwiksave.app.battery.ChargingAnimationOverlay
import com.qwiksave.app.data.UserPreferences

/**
 * Foreground Service for persistent Android Status Bar Battery Overlay & Charging Surge Animations.
 * Runs in background so the anime / emoji battery percentage is visible even when the app is closed.
 */
class BatteryOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: BatteryOverlayView? = null
    private var chargingAnimationView: ChargingAnimationOverlay? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null

    private var currentLevel = 88
    private var isCurrentlyCharging = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    if (level >= 0 && scale > 0) {
                        currentLevel = (level * 100 / scale).coerceIn(0, 100)
                    }
                    isCurrentlyCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL

                    overlayView?.batteryPercent = currentLevel
                    overlayView?.isCharging = isCurrentlyCharging
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isCurrentlyCharging = true
                    overlayView?.isCharging = true
                    if (UserPreferences.isChargingAnimationEnabled(this@BatteryOverlayService)) {
                        showChargingAnimation()
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCurrentlyCharging = false
                    overlayView?.isCharging = false
                    removeChargingAnimation()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)

        setupOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_SETTINGS) {
            applyUpdatedSettings()
        } else if (intent?.action == ACTION_SIMULATE_CHARGE) {
            showChargingAnimation()
        }
        return START_STICKY
    }

    private fun setupOverlay() {
        if (!Settings.canDrawOverlays(this)) return

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            if (overlayView != null) {
                try { windowManager?.removeView(overlayView) } catch (_: Exception) {}
            }

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val gravityPos = when (UserPreferences.getBatteryPosition(this)) {
                "top_center" -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                "top_left" -> Gravity.TOP or Gravity.START
                else -> Gravity.TOP or Gravity.END
            }

            overlayLayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = gravityPos
                x = 24
                y = 10
            }

            val view = BatteryOverlayView(this, windowManager, overlayLayoutParams).apply {
                batteryPercent = currentLevel
                isCharging = isCurrentlyCharging
                template = BatteryTemplates.getById(UserPreferences.getBatteryTemplate(this@BatteryOverlayService))
                scaleFactor = UserPreferences.getBatteryScale(this@BatteryOverlayService)
            }
            overlayView = view

            windowManager?.addView(view, overlayLayoutParams)
        } catch (_: Exception) {}
    }

    private fun applyUpdatedSettings() {
        overlayView?.let { view ->
            view.template = BatteryTemplates.getById(UserPreferences.getBatteryTemplate(this))
            view.scaleFactor = UserPreferences.getBatteryScale(this)

            val gravityPos = when (UserPreferences.getBatteryPosition(this)) {
                "top_center" -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                "top_left" -> Gravity.TOP or Gravity.START
                else -> Gravity.TOP or Gravity.END
            }
            overlayLayoutParams?.gravity = gravityPos
            windowManager?.updateViewLayout(view, overlayLayoutParams)
        }
    }

    private fun showChargingAnimation() {
        if (!Settings.canDrawOverlays(this) || windowManager == null) return
        removeChargingAnimation()

        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val animParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            val animView = ChargingAnimationOverlay(this, windowManager!!) {
                removeChargingAnimation()
            }.apply {
                batteryPercent = currentLevel
                template = BatteryTemplates.getById(UserPreferences.getBatteryTemplate(this@BatteryOverlayService))
            }

            chargingAnimationView = animView
            windowManager?.addView(animView, animParams)
        } catch (_: Exception) {}
    }

    private fun removeChargingAnimation() {
        try {
            chargingAnimationView?.let {
                windowManager?.removeView(it)
                chargingAnimationView = null
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        try {
            if (overlayView != null) windowManager?.removeView(overlayView)
        } catch (_: Exception) {}
        removeChargingAnimation()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Qwik Save Battery Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live stylish battery % and charging animations"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(chan)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_BATTERY_STUDIO", true)
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Qwik Save Battery Show Active ⚡")
            .setContentText("Status bar overlay & charging animation running smoothly")
            .setSmallIcon(R.drawable.ic_stat_qwiksave)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "qwiksave_battery_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_UPDATE_SETTINGS = "com.qwiksave.app.ACTION_UPDATE_BATTERY_SETTINGS"
        const val ACTION_SIMULATE_CHARGE = "com.qwiksave.app.ACTION_SIMULATE_CHARGE"

        fun start(context: Context) {
            try {
                val intent = Intent(context, BatteryOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, BatteryOverlayService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
        }

        fun updateSettings(context: Context) {
            try {
                val intent = Intent(context, BatteryOverlayService::class.java).apply {
                    action = ACTION_UPDATE_SETTINGS
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun simulateCharging(context: Context) {
            try {
                val intent = Intent(context, BatteryOverlayService::class.java).apply {
                    action = ACTION_SIMULATE_CHARGE
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}

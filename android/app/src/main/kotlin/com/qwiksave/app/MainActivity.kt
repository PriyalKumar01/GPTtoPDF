package com.qwiksave.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.qwiksave.app.data.UserPreferences
import com.qwiksave.app.ui.theme.QwikSaveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthDeepLink(intent)

        // Runtime Notification Permission for Android 13+ (Tiramisu, API 33+)
        // Ensures Music Hub notification & background playback controls are always visible in status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Set system navigation bar to match app bottom bar (#0F172A) with crisp white buttons
        window.navigationBarColor = android.graphics.Color.parseColor("#0F172A")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#0F172A"))
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // false = WHITE icons for time, battery, wifi
            isAppearanceLightNavigationBars = false // false = WHITE glowing navigation bar buttons (back, home, recents)
        }

        setContent {
            QwikSaveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthDeepLink(intent)
    }

    private fun handleAuthDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "qwiksave" && uri.host == "auth") {
            val error = uri.getQueryParameter("error") ?: ""
            val isBanned = uri.getQueryParameter("banned")?.toBoolean() ?: false
            if (isBanned || error.isNotBlank()) {
                val msg = if (error.isNotBlank()) error else "Account has been suspended by administration."
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
                return
            }

            val name = uri.getQueryParameter("name") ?: ""
            val email = uri.getQueryParameter("email") ?: ""
            val provider = uri.getQueryParameter("provider") ?: "google"
            val age = uri.getQueryParameter("age")?.toIntOrNull() ?: 0
            if (email.isNotBlank()) {
                UserPreferences.saveVerifiedAuth(this, name, email, provider, age)
            }
        }
    }
}

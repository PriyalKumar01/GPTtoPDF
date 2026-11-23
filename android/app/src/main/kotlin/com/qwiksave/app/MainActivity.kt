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
import kotlinx.coroutines.launch

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
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#0F172A"))
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true // Dark icons (battery, wifi, network, time) by default on light screens
            isAppearanceLightNavigationBars = false // Crisp white buttons on dark navigation bar (#0F172A)
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

            val rawName = uri.getQueryParameter("name") ?: ""
            val email = uri.getQueryParameter("email") ?: ""
            val provider = uri.getQueryParameter("provider") ?: "google"
            val rawAge = uri.getQueryParameter("age")?.toIntOrNull() ?: 0

            val dummyBuzzwords = listOf("test", "fake", "xyz", "asdf", "dummy", "admin", "null", "undefined", "anonymous", "user", "guest")
            val isDummy = { s: String -> dummyBuzzwords.any { s.trim().lowercase().contains(it) } || s.trim().length < 2 }

            val savedName = UserPreferences.getUserName(this)
            val finalName = when {
                rawName.isNotBlank() && !isDummy(rawName) -> rawName.trim()
                savedName.isNotBlank() && !isDummy(savedName) -> savedName.trim()
                rawName.isNotBlank() -> rawName.trim()
                else -> email.substringBefore("@").replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }

            val savedAge = UserPreferences.getUserAge(this)
            val finalAge = if (savedAge in 1..100) savedAge else if (rawAge in 1..100) rawAge else 20

            val rawGender = uri.getQueryParameter("gender") ?: ""
            val savedGender = UserPreferences.getUserGender(this)
            val finalGender = if (rawGender.isNotBlank()) rawGender else if (savedGender.isNotBlank()) savedGender else "Male"

            if (email.isNotBlank()) {
                UserPreferences.saveVerifiedAuth(this, finalName, email, provider, finalAge, finalGender)

                // 1. Async sync to Supabase GoTrue Auth (/auth/v1/signup)
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val authUrl = java.net.URL("https://trfpjmtmsmzljcssvxnn.supabase.co/auth/v1/signup")
                        val authConn = authUrl.openConnection() as java.net.HttpURLConnection
                        authConn.requestMethod = "POST"
                        authConn.setRequestProperty("apikey", "sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                        authConn.setRequestProperty("Content-Type", "application/json")
                        authConn.doOutput = true
                        val authPayload = org.json.JSONObject().apply {
                            put("email", email.trim().lowercase())
                            put("password", "QwikSave_${email.trim().lowercase().hashCode()}_Secure!")
                            put("data", org.json.JSONObject().apply {
                                put("name", finalName)
                                put("age", finalAge)
                                put("gender", finalGender)
                                put("provider", provider.trim().lowercase())
                            })
                        }
                        authConn.outputStream.use { it.write(authPayload.toString().toByteArray(Charsets.UTF_8)) }
                        authConn.responseCode
                    } catch (_: Exception) {}

                    // 2. Async sync to Supabase public.users table
                    try {
                        val url = java.net.URL("https://trfpjmtmsmzljcssvxnn.supabase.co/rest/v1/users")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("apikey", "sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                        conn.setRequestProperty("Authorization", "Bearer sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                        conn.doOutput = true
                        val payload = org.json.JSONObject().apply {
                            put("id", "usr_${System.currentTimeMillis()}_${(1000..9999).random()}")
                            put("name", finalName)
                            put("age", finalAge)
                            put("gender", finalGender)
                            put("email", email.trim().lowercase())
                            put("provider", provider.trim().lowercase())
                            put("verified", true)
                            put("role", if (email.trim().equals("priyalkumar01@gmail.com", ignoreCase = true)) "owner" else "user")
                            put("is_banned", false)
                        }
                        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                        conn.responseCode
                    } catch (_: Exception) {}
                }

                android.widget.Toast.makeText(this, "Welcome, $finalName! Verified via ${provider.replaceFirstChar { it.uppercase() }} ✓", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

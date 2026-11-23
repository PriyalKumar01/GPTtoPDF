package com.qwiksave.app

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwiksave.app.data.UserPreferences
import com.qwiksave.app.ui.components.OnboardingScreen
import com.qwiksave.app.ui.components.SplashScreen
import com.qwiksave.app.ui.main.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class AppStage {
    SPLASH,
    ONBOARDING,
    MAIN
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current

    var currentStage by remember { mutableStateOf(AppStage.SPLASH) }
    var banReason by remember { mutableStateOf<String?>(null) }

    // Reactive listener to auth changes (OAuth deep links / sign outs)
    val isAuthVerified by UserPreferences.isAuthVerifiedFlow.collectAsState()
    LaunchedEffect(isAuthVerified) {
        if (isAuthVerified == true && currentStage == AppStage.ONBOARDING) {
            currentStage = AppStage.MAIN
        } else if (isAuthVerified == false && currentStage == AppStage.MAIN) {
            currentStage = AppStage.ONBOARDING
        }
    }

    // Real-time Ban Check loop when user is inside the app: checks direct Supabase REST & API
    LaunchedEffect(currentStage) {
        if (currentStage == AppStage.MAIN) {
            while (isActive) {
                val email = UserPreferences.getUserEmail(context).trim()
                if (email.isNotBlank() && !email.equals("priyalkumar01@gmail.com", ignoreCase = true)) {
                    try {
                        withContext(Dispatchers.IO) {
                            val encodedEmail = URLEncoder.encode(email, "UTF-8")
                            var isUserBanned = false
                            var reasonText = "Violation of Qwik Save Terms & Conditions (such as providing false details or unauthorized usage)."

                            // 1. Direct Supabase Query for 0-latency instant ban check
                            try {
                                val sbUrl = URL("https://trfpjmtmsmzljcssvxnn.supabase.co/rest/v1/users?email=eq.$encodedEmail&select=*")
                                val sbConn = sbUrl.openConnection() as HttpURLConnection
                                sbConn.connectTimeout = 3000
                                sbConn.readTimeout = 3000
                                sbConn.requestMethod = "GET"
                                sbConn.setRequestProperty("apikey", "sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                                sbConn.setRequestProperty("Authorization", "Bearer sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                                if (sbConn.responseCode == 200) {
                                    val text = sbConn.inputStream.bufferedReader().use { it.readText() }
                                    val array = org.json.JSONArray(text)
                                    if (array.length() > 0) {
                                        val userObj = array.getJSONObject(0)
                                        if (userObj.optBoolean("is_banned", false)) {
                                            isUserBanned = true
                                            val r = userObj.optString("ban_reason", "")
                                            if (r.isNotBlank()) reasonText = r
                                        }
                                    }
                                }
                            } catch (_: Exception) {}

                            // 2. Fallback to API check
                            if (!isUserBanned) {
                                try {
                                    val endpoint = URL("https://qwiksave.vercel.app/api/auth/check-ban?email=$encodedEmail")
                                    val conn = endpoint.openConnection() as HttpURLConnection
                                    conn.connectTimeout = 3000
                                    conn.readTimeout = 3000
                                    conn.requestMethod = "GET"
                                    conn.setRequestProperty("Cache-Control", "no-cache")
                                    if (conn.responseCode == 200) {
                                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                                        val json = JSONObject(text)
                                        if (json.optBoolean("isBanned", false)) {
                                            isUserBanned = true
                                            val r = json.optString("reason", "")
                                            if (r.isNotBlank()) reasonText = r
                                        }
                                    }
                                } catch (_: Exception) {}
                            }

                            if (isUserBanned) {
                                withContext(Dispatchers.Main) {
                                    UserPreferences.clearAuth(context)
                                    banReason = reasonText
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Offline or transient network glitch, keep going
                    }
                }
                delay(5000) // Poll every 5 seconds for instantaneous real-time ban enforcement
            }
        }
    }

    AnimatedContent(
        targetState = currentStage,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "AppStageTransition",
        modifier = Modifier.fillMaxSize()
    ) { stage ->
        when (stage) {
            AppStage.SPLASH -> {
                SplashScreen(
                    onSplashFinished = {
                        val isVerified = UserPreferences.isUserVerified(context)
                        currentStage = if (isVerified) {
                            AppStage.MAIN
                        } else {
                            AppStage.ONBOARDING
                        }
                    }
                )
            }

            AppStage.ONBOARDING -> {
                OnboardingScreen(
                    onComplete = { userName ->
                        currentStage = AppStage.MAIN
                    }
                )
            }

            AppStage.MAIN -> {
                MainScreen(
                    onSignOut = {
                        UserPreferences.clearAuth(context)
                        currentStage = AppStage.ONBOARDING
                    }
                )
            }
        }
    }

    // Blocking Dialog if user is banned by Owner
    banReason?.let { reason ->
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss without signing out */ },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                    Text(
                        "Account Suspended",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Your account has been suspended by administration in accordance with Qwik Save Terms of Service due to a policy violation or falsified profile details.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0x33EF4444),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            "Reason: $reason",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 16.sp
                        )
                    }
                    Text(
                        "All active sessions have been invalidated. Please contact support or review Terms & Conditions.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        banReason = null
                        currentStage = AppStage.ONBOARDING
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Acknowledge & Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0F172A),
            textContentColor = Color.White
        )
    }
}

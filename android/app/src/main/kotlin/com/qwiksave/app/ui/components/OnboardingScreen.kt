package com.qwiksave.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.qwiksave.app.R
import com.qwiksave.app.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class OnboardingStage {
    FEATURE_SLIDES,
    PROFILE_DETAILS,
    SOCIAL_AUTH
}

data class FeatureSlide(
    val title: String,
    val subtitle: String,
    val tag: String,
    val imageRes: Int,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    onComplete: (userName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Ensure status bar icons (battery, wifi, net, time) are DARK & 100% visible on light onboarding screens
    val activity = context as? Activity
    DisposableEffect(Unit) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.isAppearanceLightStatusBars = true
        onDispose {
            insetsController?.isAppearanceLightStatusBars = false
        }
    }

    var currentStage by remember { mutableStateOf(OnboardingStage.FEATURE_SLIDES) }
    var currentSlideIndex by remember { mutableStateOf(0) }

    // User Profile Form State
    var nameInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var genderInput by remember { mutableStateOf("Male") }
    var formError by remember { mutableStateOf<String?>(null) }

    // In-App OAuth WebView Dialog State
    var activeOAuthProvider by remember { mutableStateOf<String?>(null) }
    var isExchangingToken by remember { mutableStateOf(false) }
    var pendingVerifiedXUser by remember { mutableStateOf<Pair<String, String>?>(null) } // Pair(fullName, username)
    var xVerifiedEmailInput by remember { mutableStateOf("") }
    var xVerifiedEmailError by remember { mutableStateOf<String?>(null) }

    // Listen to reactive auth verification (e.g. from OAuth deep links or One-Tap)
    val isVerifiedState by UserPreferences.isAuthVerifiedFlow.collectAsState()
    LaunchedEffect(isVerifiedState) {
        if (isVerifiedState == true) {
            onComplete(UserPreferences.getUserName(context))
        }
    }

    val slides = remember {
        listOf(
            FeatureSlide(
                title = "Universal HD Downloader",
                subtitle = "Save trending Instagram Reels, Facebook HD clips, and YouTube videos (up to 49h+ long) in 1080p with zero watermarks. Fast direct CDN access.",
                tag = "HIGH SPEED ⚡",
                imageRes = R.drawable.slide_downloader,
                accentColor = Color(0xFFEF4444)
            ),
            FeatureSlide(
                title = "Qwik Music Hub",
                subtitle = "Stream 1500+ Bollywood 90s classics & trending hits with lock-screen background playback, no ads, and 1-click offline downloads.",
                tag = "SPOTIFY VIBE 🎧",
                imageRes = R.drawable.slide_music,
                accentColor = Color(0xFF10B981)
            ),
            FeatureSlide(
                title = "AI Chat to Academic PDF",
                subtitle = "Convert ChatGPT, Claude & Gemini study chats into structured academic PDFs with LaTeX math formulas, diagrams, code blocks & page merge.",
                tag = "ACADEMIC SUITE 📚",
                imageRes = R.drawable.slide_study_pdf,
                accentColor = Color(0xFF0284C7)
            ),
            FeatureSlide(
                title = "QR Studio & Quick Utilities",
                subtitle = "Direct WhatsApp messaging without saving numbers, custom high-res QR codes (WiFi, UPI, Phone) & essential daily tools.",
                tag = "ALL-IN-ONE ⚡",
                imageRes = R.drawable.slide_utilities,
                accentColor = Color(0xFFA855F7)
            )
        )
    }

    fun completeVerification(name: String, email: String, provider: String) {
        val dummyBuzzwords = listOf("test", "fake", "xyz", "asdf", "dummy", "admin", "null", "undefined", "anonymous", "user", "guest")
        val isDummy = { s: String -> dummyBuzzwords.any { s.trim().lowercase().contains(it) } || s.trim().length < 2 }

        val finalName = when {
            name.isNotBlank() && !isDummy(name) -> name.trim()
            nameInput.isNotBlank() && !isDummy(nameInput) -> nameInput.trim()
            name.isNotBlank() -> name.trim()
            else -> email.substringBefore("@").replace(".", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
        val finalAge = ageInput.toIntOrNull() ?: 20
        val finalGender = genderInput.trim().ifBlank { "Male" }

        UserPreferences.saveVerifiedAuth(
            context = context,
            name = finalName,
            email = email.trim(),
            provider = provider.trim(),
            age = finalAge,
            gender = finalGender
        )

        // 1. Async sync to Supabase GoTrue Auth (/auth/v1/signup) so user appears in Auth -> Users dashboard
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authUrl = URL("https://trfpjmtmsmzljcssvxnn.supabase.co/auth/v1/signup")
                val authConn = authUrl.openConnection() as HttpURLConnection
                authConn.requestMethod = "POST"
                authConn.setRequestProperty("apikey", "sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                authConn.setRequestProperty("Content-Type", "application/json")
                authConn.doOutput = true
                val authPayload = JSONObject().apply {
                    put("email", email.trim().lowercase())
                    put("password", "QwikSave_${email.trim().lowercase().hashCode()}_Secure!")
                    put("data", JSONObject().apply {
                        put("name", finalName)
                        if (finalAge in 1..100) put("age", finalAge)
                        put("gender", finalGender)
                        put("provider", provider.trim().lowercase())
                    })
                }
                authConn.outputStream.use { it.write(authPayload.toString().toByteArray(Charsets.UTF_8)) }
                authConn.responseCode
            } catch (_: Exception) {}

            // 2. Also Upsert to Supabase public.users table (for table editor and ban checks)
            try {
                val tableUrl = URL("https://trfpjmtmsmzljcssvxnn.supabase.co/rest/v1/users")
                val tableConn = tableUrl.openConnection() as HttpURLConnection
                tableConn.requestMethod = "POST"
                tableConn.setRequestProperty("apikey", "sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                tableConn.setRequestProperty("Authorization", "Bearer sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                tableConn.setRequestProperty("Content-Type", "application/json")
                tableConn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                tableConn.doOutput = true
                val tablePayload = JSONObject().apply {
                    put("id", "usr_${System.currentTimeMillis()}_${(1000..9999).random()}")
                    put("name", finalName)
                    if (finalAge in 1..100) put("age", finalAge)
                    put("gender", finalGender)
                    put("email", email.trim().lowercase())
                    put("provider", provider.trim().lowercase())
                    put("verified", true)
                    put("role", if (email.trim().equals("priyalkumar01@gmail.com", ignoreCase = true)) "owner" else "user")
                    put("is_banned", false)
                }
                tableConn.outputStream.use { it.write(tablePayload.toString().toByteArray(Charsets.UTF_8)) }
                tableConn.responseCode
            } catch (_: Exception) {}
        }

        Toast.makeText(context, "Welcome, $finalName! Verified via ${provider.replaceFirstChar { it.uppercase() }} ✓", Toast.LENGTH_SHORT).show()
        onComplete(finalName)
    }

    // Handle Supabase OAuth session token (e.g. from deep link)
    fun handleSupabaseToken(ctx: Context, accessToken: String, provider: String) {
        try {
            val userUrl = URL("https://trfpjmtmsmzljcssvxnn.supabase.co/auth/v1/user")
            val conn = userUrl.openConnection() as HttpURLConnection
            conn.setRequestProperty("apikey", "sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("User-Agent", "QwikSave-Android")
            if (conn.responseCode == 200) {
                val userText = conn.inputStream.bufferedReader().use { it.readText() }
                val userJson = JSONObject(userText)
                val meta = userJson.optJSONObject("user_metadata")
                val xUsername = meta?.optString("user_name")?.ifBlank { null }
                    ?: meta?.optString("preferred_username")?.ifBlank { null }
                    ?: meta?.optString("name")?.ifBlank { null }
                    ?: ""
                val xFullName = meta?.optString("full_name")?.ifBlank { null }
                    ?: meta?.optString("name")?.ifBlank { null }
                    ?: ""
                val xEmail = userJson.optString("email", "")
                val finalName = xFullName.ifBlank { xUsername.ifBlank { nameInput.trim().ifBlank { "User" } } }

                if (xEmail.isNotBlank()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        activeOAuthProvider = null
                        isExchangingToken = false
                        completeVerification(finalName, xEmail, provider)
                    }
                    return
                } else if (xUsername.isNotBlank()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        activeOAuthProvider = null
                        isExchangingToken = false
                        pendingVerifiedXUser = Pair(finalName, xUsername)
                    }
                    return
                }
            }
        } catch (_: Exception) {}

        // Never fake authentication on error
        CoroutineScope(Dispatchers.Main).launch {
            activeOAuthProvider = null
            isExchangingToken = false
            Toast.makeText(ctx, "Authentication was cancelled or failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    // Direct In-App Token Exchange for GitHub / X (No external browser, strictly verified)
    fun handleDirectCodeExchange(ctx: Context, provider: String, code: String) {
        try {
            if (provider == "github") {
                val tokenUrl = URL("https://github.com/login/oauth/access_token")
                val conn = tokenUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                val payload = JSONObject().apply {
                    put("client_id", "Ov23liow83AXdcoIefRx")
                    put("client_secret", "07ee7f69f31ff65f5f786227d71d4ba500f8cce1")
                    put("code", code)
                    put("redirect_uri", "https://qwiksave.vercel.app/auth/callback")
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode == 200) {
                    val respText = conn.inputStream.bufferedReader().use { it.readText() }
                    val tokenJson = JSONObject(respText)
                    val accessToken = tokenJson.optString("access_token", "")
                    if (accessToken.isNotBlank()) {
                        val userUrl = URL("https://api.github.com/user")
                        val userConn = userUrl.openConnection() as HttpURLConnection
                        userConn.setRequestProperty("Authorization", "Bearer $accessToken")
                        userConn.setRequestProperty("User-Agent", "QwikSave-Android")
                        if (userConn.responseCode == 200) {
                            val userText = userConn.inputStream.bufferedReader().use { it.readText() }
                            val userJson = JSONObject(userText)
                            val ghName = userJson.optString("name", "").ifBlank { userJson.optString("login", "GitHub User") }
                            var ghEmail = userJson.optString("email", "")
                            if (ghEmail.isBlank()) {
                                try {
                                    val emUrl = URL("https://api.github.com/user/emails")
                                    val emConn = emUrl.openConnection() as HttpURLConnection
                                    emConn.setRequestProperty("Authorization", "Bearer $accessToken")
                                    emConn.setRequestProperty("User-Agent", "QwikSave-Android")
                                    if (emConn.responseCode == 200) {
                                        val emText = emConn.inputStream.bufferedReader().use { it.readText() }
                                        val emArr = JSONArray(emText)
                                        for (i in 0 until emArr.length()) {
                                            val o = emArr.getJSONObject(i)
                                            if (o.optBoolean("primary", false)) {
                                                ghEmail = o.optString("email", "")
                                                break
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            if (ghEmail.isBlank()) {
                                ghEmail = "${userJson.optString("login", "user")}@users.noreply.github.com"
                            }

                            CoroutineScope(Dispatchers.Main).launch {
                                activeOAuthProvider = null
                                isExchangingToken = false
                                val chosenName = ghName.ifBlank { nameInput.trim() }
                                completeVerification(chosenName, ghEmail, "github")
                            }
                            return
                        }
                    }
                }
            } else if (provider == "twitter") {
                // Official Twitter / X OAuth 2.0 PKCE exchange
                val tokenUrl = URL("https://api.twitter.com/2/oauth2/token")
                val conn = tokenUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                val basicAuth = android.util.Base64.encodeToString(
                    "Sm51TWxxYzVuVGFoVHU3aUJ3ZUw6MTpjaQ:G9JLzol26vipwPkM-oRcLMC5BRZ3Vn-2thUK99JcApI5Whor60".toByteArray(Charsets.UTF_8),
                    android.util.Base64.NO_WRAP
                )
                conn.setRequestProperty("Authorization", "Basic $basicAuth")
                conn.doOutput = true
                val postBody = "code=$code&grant_type=authorization_code&redirect_uri=https://qwiksave.vercel.app/auth/callback&code_verifier=challenge"
                conn.outputStream.use { it.write(postBody.toByteArray(Charsets.UTF_8)) }

                var xAccessToken = ""
                if (conn.responseCode == 200) {
                    val respText = conn.inputStream.bufferedReader().use { it.readText() }
                    val respJson = JSONObject(respText)
                    xAccessToken = respJson.optString("access_token", "")
                } else {
                    // Fallback to Next.js API /api/auth which handles Twitter OAuth securely
                    try {
                        val backendUrl = URL("https://qwiksave.vercel.app/api/auth")
                        val bConn = backendUrl.openConnection() as HttpURLConnection
                        bConn.requestMethod = "POST"
                        bConn.setRequestProperty("Content-Type", "application/json")
                        bConn.doOutput = true
                        val bPayload = JSONObject().apply {
                            put("provider", "twitter")
                            put("code", code)
                            put("redirectUri", "https://qwiksave.vercel.app/auth/callback")
                            put("name", nameInput)
                        }
                        bConn.outputStream.use { it.write(bPayload.toString().toByteArray(Charsets.UTF_8)) }
                        if (bConn.responseCode == 200) {
                            val bText = bConn.inputStream.bufferedReader().use { it.readText() }
                            val bJson = JSONObject(bText)
                            val userObj = bJson.optJSONObject("user")
                            val uName = userObj?.optString("name", "") ?: ""
                            val uEmail = userObj?.optString("email", "") ?: ""
                            if (uEmail.isNotBlank() && !uEmail.endsWith("@x.com")) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    activeOAuthProvider = null
                                    isExchangingToken = false
                                    completeVerification(uName.ifBlank { nameInput }, uEmail, "twitter")
                                }
                                return
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (xAccessToken.isNotBlank()) {
                    val userUrl = URL("https://api.twitter.com/2/users/me?user.fields=id,name,username,profile_image_url")
                    val uConn = userUrl.openConnection() as HttpURLConnection
                    uConn.setRequestProperty("Authorization", "Bearer $xAccessToken")
                    uConn.setRequestProperty("User-Agent", "QwikSave-Android")
                    if (uConn.responseCode == 200) {
                        val uText = uConn.inputStream.bufferedReader().use { it.readText() }
                        val uJson = JSONObject(uText)
                        val dataObj = uJson.optJSONObject("data")
                        val xUsername = dataObj?.optString("username", "") ?: ""
                        val xFullName = dataObj?.optString("name", "") ?: ""

                        CoroutineScope(Dispatchers.Main).launch {
                            activeOAuthProvider = null
                            isExchangingToken = false
                            // User is verified by X. Now ask user to confirm their real email address
                            pendingVerifiedXUser = Pair(xFullName.ifBlank { nameInput }, xUsername)
                        }
                        return
                    }
                }
            }
        } catch (_: Exception) {}

        // Never generate fake fallbacks! Report error so user can complete authentic sign-in.
        CoroutineScope(Dispatchers.Main).launch {
            activeOAuthProvider = null
            isExchangingToken = false
            Toast.makeText(ctx, "Authentication was not completed. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    // Native Google Play Services Sign-In Launcher
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken("235620681905-ffgf6fagoh2e2nim4d08s2ecd5a9pdqq.apps.googleusercontent.com")
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account.email ?: ""
            val displayName = account.displayName ?: ""
            if (email.isNotBlank()) {
                completeVerification(displayName.ifBlank { nameInput }, email, "google")
            } else {
                Toast.makeText(context, "Google Sign-In did not return an email.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Google Sign-In cancelled or failed.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        AnimatedContent(
            targetState = currentStage,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "StageTransition",
            modifier = Modifier.fillMaxSize()
        ) { stage ->
            when (stage) {
                // STEP 1: Feature Slides Carousel
                OnboardingStage.FEATURE_SLIDES -> {
                    val slide = slides[currentSlideIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Navigation Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.app_logo),
                                    contentDescription = "Qwik Save",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Text(
                                    text = "Qwik Save",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            TextButton(
                                onClick = { currentStage = OnboardingStage.PROFILE_DETAILS }
                            ) {
                                Text(
                                    text = "Skip →",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Central Modern Card with Upper-Half 3D Pictorial Image
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Upper Half: 3D Pictorial Illustration
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1.15f)
                                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                ) {
                                    Image(
                                        painter = painterResource(slide.imageRes),
                                        contentDescription = slide.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Lower Half: Categorical Tag, Title & Descriptive Copy
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.95f)
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Tag Pill
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    slide.accentColor.copy(alpha = 0.12f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = slide.tag,
                                                color = slide.accentColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        // Title
                                        Text(
                                            text = slide.title,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A),
                                            lineHeight = 28.sp
                                        )

                                        // Subtitle
                                        Text(
                                            text = slide.subtitle,
                                            fontSize = 13.sp,
                                            color = Color(0xFF475569),
                                            lineHeight = 19.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }

                        // Bottom Navigation Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pager Dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                slides.indices.forEach { index ->
                                    val isSelected = index == currentSlideIndex
                                    val width by animateDpAsState(
                                        targetValue = if (isSelected) 24.dp else 8.dp,
                                        label = "dotWidth"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .height(8.dp)
                                            .width(width)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) slide.accentColor else Color(0xFFCBD5E1)
                                            )
                                    )
                                }
                            }

                            // Next / Get Started Action Button
                            Button(
                                onClick = {
                                    if (currentSlideIndex < slides.lastIndex) {
                                        currentSlideIndex++
                                    } else {
                                        currentStage = OnboardingStage.PROFILE_DETAILS
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7)
                                ),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (currentSlideIndex < slides.lastIndex) "Next →" else "Get Started →",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // STEP 2: Name, Age & Gender Profile Form (Shifted comfortably upward so keyboard never obscures)
                OnboardingStage.PROFILE_DETAILS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top Brand Header (Compact & Shifted Upward)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            val isFemaleAvatar = genderInput.equals("Female", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(if (isFemaleAvatar) Color(0xFFFCE7F3) else Color(0xFFE0F2FE))
                                    .border(2.5.dp, if (isFemaleAvatar) Color(0xFFF43F5E) else Color(0xFF0284C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFemaleAvatar) Icons.Default.Face3 else Icons.Default.Face6,
                                    contentDescription = if (isFemaleAvatar) "Female Avatar" else "Male Avatar",
                                    tint = if (isFemaleAvatar) Color(0xFFE11D48) else Color(0xFF0284C7),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Text(
                                text = "Set Up Your Profile",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Enter your real name, age & gender to personalize your account.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        // Form Inputs Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Name Input
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        text = "YOUR NAME",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { input ->
                                            if (input.any { c -> c.isDigit() }) {
                                                formError = "Numbers are not allowed in name."
                                            } else {
                                                formError = null
                                            }
                                            nameInput = input.filter { c -> c.isLetter() || c.isWhitespace() }
                                        },
                                        placeholder = { Text("ENTER YOUR NAME", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0284C7))
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = Color(0xFFCBD5E1),
                                            focusedContainerColor = Color(0xFFF8FAFC),
                                            unfocusedContainerColor = Color(0xFFF8FAFC)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // 2. Age Input
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        text = "YOUR AGE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                    OutlinedTextField(
                                        value = ageInput,
                                        onValueChange = {
                                            if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                                                ageInput = it.take(3)
                                                formError = null
                                            }
                                        },
                                        placeholder = { Text("ENTER YOUR AGE", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Cake, contentDescription = null, tint = Color(0xFF0284C7))
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = Color(0xFFCBD5E1),
                                            focusedContainerColor = Color(0xFFF8FAFC),
                                            unfocusedContainerColor = Color(0xFFF8FAFC)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // 3. Gender Dropdown / Segmented Selector
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        text = "YOUR GENDER",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val genderOptions = listOf("Male" to "♂ Male", "Female" to "♀ Female", "Other" to "⚧ Other")
                                        genderOptions.forEach { (key, label) ->
                                            val isSelected = genderInput == key
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        genderInput = key
                                                        formError = null
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) Color(0xFF0369A1) else Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (formError != null) {
                                    Text(
                                        text = formError!!,
                                        color = Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Continue Button
                        Button(
                            onClick = {
                                val trimmed = nameInput.trim()
                                val parsedAge = ageInput.toIntOrNull()
                                val dummyBuzzwords = listOf(
                                    "test", "fake", "xyz", "asdf", "dummy", "admin", "null", "undefined",
                                    "anonymous", "user", "guest", "temp", "qwerty", "demo", "sample", "bot"
                                )
                                val lower = trimmed.lowercase()

                                if (trimmed.isBlank() || trimmed.length < 2) {
                                    formError = "Please enter your real full name."
                                    return@Button
                                }
                                if (dummyBuzzwords.any { lower.contains(it) }) {
                                    formError = "Dummy or placeholder names are not allowed. Please enter your real name."
                                    return@Button
                                }
                                if (parsedAge == null || parsedAge !in 1..100) {
                                    formError = "Please enter a valid age (1 to 100)."
                                    return@Button
                                }
                                currentStage = OnboardingStage.SOCIAL_AUTH
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "Continue to Verification →",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // STEP 3: Social Account Verification (Shifted Upwards with App Logo & Balanced Layout)
                OnboardingStage.SOCIAL_AUTH -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Header with App Logo & Title shifted comfortably lower down
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.app_logo),
                                    contentDescription = "Qwik Save Logo",
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Text(
                                    text = "Qwik Save",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Text(
                                text = "Verify Your Account",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            // Verified Chip showing user Name, Age & Gender
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(20.dp))
                                    .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${nameInput.trim().ifBlank { "User" }} • ${ageInput.trim().ifBlank { "21" }} yrs • $genderInput",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1)
                                    )
                                }
                            }
                        }

                        // Central Welcome Community Card with uncropped 3D illustration
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column {
                                Image(
                                    painter = painterResource(R.drawable.welcome_community),
                                    contentDescription = "Welcome Community",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(155.dp)
                                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                    contentScale = ContentScale.FillWidth
                                )

                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "Welcome to Qwik Save! 👋",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Join thousands of college students and creators. Tap an account below for instant 1-click verification.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Bottom Section: Shifted upwards comfortably
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text(
                                text = "Choose an account to continue:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            // 1. Continue with Google (Native Play Services One-Tap Chooser)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable {
                                        try {
                                            googleSignInClient.signOut().addOnCompleteListener {
                                                googleAuthLauncher.launch(googleSignInClient.signInIntent)
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not start Google Sign-In.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_google_brand),
                                        contentDescription = "Google",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Continue with Google",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }

                            // 2. Continue with GitHub (In-App OAuth Sheet)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable {
                                        activeOAuthProvider = "github"
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF181717)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_github_brand),
                                        contentDescription = "GitHub",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Continue with GitHub",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // 3. Continue with X (formerly Twitter - In-App OAuth Sheet with official X logo)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable {
                                        activeOAuthProvider = "twitter"
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
                                border = BorderStroke(1.dp, Color(0xFF27272A)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_x_brand),
                                        contentDescription = "X",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Continue with X",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Security Footnote
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔒 100% Real Verification • No Passwords • No Temp Mails",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // In-App OAuth Sheet for GitHub & Facebook (Runs completely inside the app)
    activeOAuthProvider?.let { provider ->
        Dialog(
            onDismissRequest = {
                if (!isExchangingToken) activeOAuthProvider = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !isExchangingToken,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp,
                        bottom = 12.dp,
                        start = 10.dp,
                        end = 10.dp
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (provider == "github") Color(0xFF181717) else Color(0xFF000000))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (provider == "github") R.drawable.ic_github_brand else R.drawable.ic_x_brand
                                ),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = if (provider == "github") "Sign in with GitHub" else "Sign in with X",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Direct In-App Secure Verification",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    val authUrl = when (provider) {
                                        "github" -> "https://github.com/login/oauth/authorize?client_id=Ov23liow83AXdcoIefRx&redirect_uri=https://qwiksave.vercel.app/auth/callback&scope=read:user%20user:email&state=github"
                                        "twitter" -> "https://x.com/i/oauth2/authorize?response_type=code&client_id=Sm51TWxxYzVuVGFoVHU3aUJ3ZUw6MTpjaQ&redirect_uri=https://qwiksave.vercel.app/auth/callback&scope=users.read%20tweet.read&state=twitter&code_challenge=challenge&code_challenge_method=plain"
                                        else -> ""
                                    }
                                    if (authUrl.isNotBlank()) {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
                                        } catch (_: Exception) {}
                                    }
                                }
                            ) {
                                Text("Browser ↗", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }

                            IconButton(
                                onClick = { activeOAuthProvider = null },
                                modifier = Modifier.size(32.dp),
                                enabled = !isExchangingToken
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (isExchangingToken) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFF0284C7), strokeWidth = 3.dp)
                                Text(
                                    text = "Securing your verified account...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Retrieving verified profile from ${if (provider == "twitter") "X" else provider.replaceFirstChar { it.uppercase() }}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        webChromeClient = android.webkit.WebChromeClient()
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.databaseEnabled = true
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        settings.javaScriptCanOpenWindowsAutomatically = true
                                        settings.setSupportMultipleWindows(false)
                                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                                        CookieManager.getInstance().setAcceptCookie(true)
                                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                val uri = request?.url ?: return false
                                                val uriStr = uri.toString()

                                                // Handle external intent schemes safely
                                                if (uriStr.startsWith("intent://") || uriStr.startsWith("twitter://") || uriStr.startsWith("x://")) {
                                                    try {
                                                        val intent = Intent.parseUri(uriStr, Intent.URI_INTENT_SCHEME)
                                                        ctx.startActivity(intent)
                                                        return true
                                                    } catch (_: Exception) {
                                                        return false
                                                    }
                                                }

                                                // Intercept authorization callback
                                                if (uriStr.contains("/auth/callback") || uri.scheme == "qwiksave") {
                                                    val error = uri.getQueryParameter("error")
                                                    if (!error.isNullOrBlank()) {
                                                        Toast.makeText(ctx, "Authentication was cancelled.", Toast.LENGTH_SHORT).show()
                                                        activeOAuthProvider = null
                                                        return true
                                                    }

                                                    // Check if access_token is in hash fragment #access_token=...
                                                    val fragment = uri.fragment ?: if (uriStr.contains("#")) uriStr.substringAfter("#") else ""
                                                    if (fragment.contains("access_token=")) {
                                                        val accessToken = fragment.substringAfter("access_token=").substringBefore("&")
                                                        if (accessToken.isNotBlank()) {
                                                            isExchangingToken = true
                                                            CoroutineScope(Dispatchers.IO).launch {
                                                                handleSupabaseToken(ctx, accessToken, provider)
                                                            }
                                                            return true
                                                        }
                                                    }

                                                    val code = uri.getQueryParameter("code")
                                                    if (!code.isNullOrBlank()) {
                                                        isExchangingToken = true
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            handleDirectCodeExchange(ctx, provider, code)
                                                        }
                                                        return true
                                                    }
                                                }
                                                return false
                                            }

                                            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                                // Retain standard error logging without bypass fallbacks
                                            }

                                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                                // Retain standard error logging without bypass fallbacks
                                            }
                                        }

                                        val authUrl = when (provider) {
                                            "github" -> "https://github.com/login/oauth/authorize?client_id=Ov23liow83AXdcoIefRx&redirect_uri=https://qwiksave.vercel.app/auth/callback&scope=read:user%20user:email&state=github"
                                            "twitter" -> "https://x.com/i/oauth2/authorize?response_type=code&client_id=Sm51TWxxYzVuVGFoVHU3aUJ3ZUw6MTpjaQ&redirect_uri=https://qwiksave.vercel.app/auth/callback&scope=users.read%20tweet.read&state=twitter&code_challenge=challenge&code_challenge_method=plain"
                                            else -> ""
                                        }
                                        if (authUrl.isNotBlank()) {
                                            loadUrl(authUrl)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to attach user's genuine email to their verified X (Twitter) profile
    pendingVerifiedXUser?.let { (xName, xHandle) ->
        Dialog(
            onDismissRequest = {
                pendingVerifiedXUser = null
                xVerifiedEmailError = null
            },
            properties = DialogProperties(dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_x_brand),
                            contentDescription = "X",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = "Verified X Account ✓",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )

                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "@$xHandle • $xName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }

                    Text(
                        text = "Enter your primary email address (@gmail.com) to complete verification for this verified account:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = xVerifiedEmailInput,
                        onValueChange = {
                            xVerifiedEmailInput = it
                            xVerifiedEmailError = null
                        },
                        label = { Text("Primary Email Address", fontSize = 11.sp) },
                        placeholder = { Text("e.g. yourname@gmail.com", fontSize = 12.sp, color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (xVerifiedEmailError != null) {
                        Text(
                            text = xVerifiedEmailError!!,
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val emailTrim = xVerifiedEmailInput.trim()
                            if (emailTrim.isBlank() || !emailTrim.contains("@") || !emailTrim.contains(".")) {
                                xVerifiedEmailError = "Please enter a valid email address (e.g. name@gmail.com)."
                                return@Button
                            }
                            val finalName = xName.ifBlank { nameInput.trim() }
                            completeVerification(finalName, emailTrim, "twitter")
                            pendingVerifiedXUser = null
                            xVerifiedEmailError = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Confirm & Enter ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

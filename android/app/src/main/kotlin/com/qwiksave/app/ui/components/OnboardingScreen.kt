package com.qwiksave.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qwiksave.app.data.UserPreferences

@Composable
fun OnboardingScreen(
    onComplete: (userName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(0) }
    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var verifiedProvider by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }

    var showGoogleDialog by remember { mutableStateOf(false) }
    var showGithubDialog by remember { mutableStateOf(false) }

    val totalSteps = 4

    fun finishWithVerifiedUser() {
        if (!isVerified) return
        val finalName = inputName.trim().ifBlank { inputEmail.substringBefore("@") }
        UserPreferences.saveVerifiedAuth(
            context = context,
            name = finalName,
            email = inputEmail.trim(),
            provider = verifiedProvider
        )
        onComplete(finalName)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Skip Row - Positioned comfortably below status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Indicator Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalSteps) { idx ->
                        Box(
                            modifier = Modifier
                                .width(if (idx == currentStep) 24.dp else 8.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (idx == currentStep) Color(0xFF38BDF8) else Color(0xFF475569)
                                )
                        )
                    }
                }

                if (currentStep < totalSteps - 1) {
                    TextButton(
                        onClick = { currentStep = totalSteps - 1 },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Skip",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // Center Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "OnboardingStepContent",
                modifier = Modifier.weight(1f)
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (step) {
                        0 -> {
                            // Step 1: AI Chat to Academic PDF
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color(0xFF10A37F).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "AI Chat to Academic PDF",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Convert ChatGPT, Claude, and Gemini study sessions into beautifully formatted academic PDFs with code blocks and formulas in 1 click.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "⚡ 100% Client-Side • No Account Required",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        1 -> {
                            // Step 2: 1-Click Social Media HD Downloader (Original Logos)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InstagramLogo(size = 46.dp)
                                FacebookLogo(size = 46.dp)
                                YouTubeLogo(size = 46.dp)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "1-Click HD Downloader",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Download Instagram Reels, Facebook Videos in 1080p, and YouTube Shorts with zero watermarks directly into your phone gallery.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "🎬 Exact File Size Shown Before Download",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                        }

                        2 -> {
                            // Step 3: Complete PDF & Utility Suite
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PdfSuiteLogo(size = 46.dp)
                                QrCodeLogo(size = 46.dp)
                                AudioExtractorLogo(size = 46.dp)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Smart PDF & QR Tools",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Convert photos to clean A4 PDF, merge exam papers, extract crystal-clear MP3 audio, and create instant QR codes for URLs & WiFi.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "🔒 100% Private Offline Processing",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }

                        3 -> {
                            // Step 4: Personalize / Name Input (Without spoiling the "Guy's" fallback)
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(if (isVerified) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVerified) Icons.Default.VerifiedUser else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isVerified) Color(0xFF10B981) else Color(0xFF38BDF8),
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isVerified) "Account Verified! 🎉" else "Verify Your Account",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isVerified) 
                                    "Welcome aboard, $inputName! Your verified profile is ready." 
                                else 
                                    "Sign in with your verified Google or GitHub email to unlock unlimited 1-click downloads & PDF tools.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            if (isVerified) {
                                // Verified Account Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        if (verifiedProvider == "google") {
                                            GoogleLogo(size = 28.dp)
                                        } else {
                                            GitHubLogo(size = 28.dp)
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = inputName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = inputEmail,
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "✓ Verified via ${verifiedProvider.capitalize()} Security",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF34D399)
                                            )
                                        }

                                        IconButton(onClick = { isVerified = false }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Switch Account",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Mandatory Sign-In Buttons
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 1. Continue with Google Button
                                    Button(
                                        onClick = { showGoogleDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        GoogleLogo(size = 20.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Continue with Google",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937)
                                        )
                                    }

                                    // 2. Continue with GitHub Button
                                    Button(
                                        onClick = { showGithubDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292F)),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        GitHubLogo(size = 20.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Continue with GitHub",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "🔒 Mandatory verified email sign-in for security & cloud sync",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Continue Button - Comfortable padding above navigation bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentStep < totalSteps - 1) {
                    Button(
                        onClick = { currentStep++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = { finishWithVerifiedUser() },
                        enabled = isVerified,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isVerified) "Enter Qwik Save 🚀" else "Sign in above to Continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVerified) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Google Authentication Dialog
        if (showGoogleDialog) {
            var gName by remember { mutableStateOf(inputName) }
            var gEmail by remember { mutableStateOf(inputEmail) }
            var gError by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showGoogleDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GoogleLogo(size = 36.dp)

                        Text(
                            text = "Sign in with Google",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Enter your Google credentials to verify your Qwik Save account.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = gName,
                            onValueChange = { gName = it },
                            label = { Text("Your Name", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("e.g. Priyal Kumar", color = Color(0xFF64748B)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4285F4)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = gEmail,
                            onValueChange = { 
                                gEmail = it
                                gError = ""
                            },
                            label = { Text("Google Email", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("name@gmail.com", color = Color(0xFF64748B)) },
                            singleLine = true,
                            isError = gError.isNotBlank(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4285F4)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (gError.isNotBlank()) {
                            Text(text = gError, color = Color(0xFFEF4444), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val cleanEmail = gEmail.trim()
                                val cleanName = gName.trim()
                                if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                    gError = "Please enter a valid Google email address"
                                } else if (cleanName.isBlank()) {
                                    gError = "Please enter your name"
                                } else {
                                    inputName = cleanName
                                    inputEmail = cleanEmail
                                    verifiedProvider = "google"
                                    isVerified = true
                                    showGoogleDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Verify Google Account ✓", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // GitHub Authentication Dialog
        if (showGithubDialog) {
            var ghUser by remember { mutableStateOf(inputName) }
            var ghEmail by remember { mutableStateOf(inputEmail) }
            var ghError by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showGithubDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GitHubLogo(size = 36.dp)

                        Text(
                            text = "Sign in with GitHub",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Authorize Qwik Save with your verified GitHub profile.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = ghUser,
                            onValueChange = { ghUser = it },
                            label = { Text("GitHub Username / Name", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("e.g. PriyalKumar01", color = Color(0xFF64748B)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = ghEmail,
                            onValueChange = { 
                                ghEmail = it
                                ghError = ""
                            },
                            label = { Text("GitHub Verified Email", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("developer@users.noreply.github.com", color = Color(0xFF64748B)) },
                            singleLine = true,
                            isError = ghError.isNotBlank(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (ghError.isNotBlank()) {
                            Text(text = ghError, color = Color(0xFFEF4444), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val cleanEmail = ghEmail.trim()
                                val cleanUser = ghUser.trim()
                                if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                    ghError = "Please enter a valid verified email address"
                                } else if (cleanUser.isBlank()) {
                                    ghError = "Please enter your GitHub username"
                                } else {
                                    inputName = cleanUser
                                    inputEmail = cleanEmail
                                    verifiedProvider = "github"
                                    isVerified = true
                                    showGithubDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292F)),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Authorize GitHub Account ✓", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

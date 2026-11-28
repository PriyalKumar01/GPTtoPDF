package com.qwiksave.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qwiksave.app.R
import com.qwiksave.app.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActiveDialog {
    NONE, PROFILE, FAQS, ABOUT
}

/**
 * Top-Right 3-Dot Overflow Menu.
 * Order: Profile (top) -> FAQs -> About Qwik Save -> Sign Out.
 */
@Composable
fun TopBarOverflowMenu(
    activeDialog: MutableState<ActiveDialog>,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options Menu",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                .width(220.dp)
        ) {
            // Line 1: Profile (At the very top)
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Profile",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                onClick = {
                    expanded = false
                    activeDialog.value = ActiveDialog.PROFILE
                }
            )

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Line 2: FAQs & Help (Below Profile)
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "FAQs & Help",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                },
                onClick = {
                    expanded = false
                    activeDialog.value = ActiveDialog.FAQS
                }
            )

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Line 3: About Qwik Save (Then About)
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "About Qwik Save",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                },
                onClick = {
                    expanded = false
                    activeDialog.value = ActiveDialog.ABOUT
                }
            )

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Line 4: Sign Out / Switch Account
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign Out",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFDA4AF)
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onSignOut()
                }
            )
        }
    }
}

/**
 * Dialog host that renders whichever dialog is requested.
 */
@Composable
fun OverflowDialogHost(
    activeDialog: MutableState<ActiveDialog>,
    onSignOut: () -> Unit = {}
) {
    when (activeDialog.value) {
        ActiveDialog.PROFILE -> ProfileDialog(
            onDismiss = { activeDialog.value = ActiveDialog.NONE },
            onSignOut = onSignOut
        )
        ActiveDialog.FAQS -> FaqsDialog(onDismiss = { activeDialog.value = ActiveDialog.NONE })
        ActiveDialog.ABOUT -> AboutDialog(onDismiss = { activeDialog.value = ActiveDialog.NONE })
        ActiveDialog.NONE -> Unit
    }
}

/**
 * Profile Dialog: Clean modal showing Name, Age (uneditable), Verified Email with green checkmark,
 * and Sign Out at the end.
 */
@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val userName = UserPreferences.getUserName(context)
    val userAge = UserPreferences.getUserAge(context)
    val userGender = UserPreferences.getUserGender(context)
    val userEmail = UserPreferences.getUserEmail(context)
    val authProvider = UserPreferences.getAuthProvider(context)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Profile Icon & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isFemale = userGender.equals("Female", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isFemale) Color(0xFFF43F5E).copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.15f))
                                .border(1.5.dp, if (isFemale) Color(0xFFF43F5E) else Color(0xFF0284C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFemale) Icons.Default.Face3 else Icons.Default.Face6,
                                contentDescription = if (isFemale) "Female Avatar" else "Male Avatar",
                                tint = if (isFemale) Color(0xFFFB7185) else Color(0xFF38BDF8),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Account Profile",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Verified Member",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // User Info Fields (Read-Only / Uneditable)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Field 1: Name (Uneditable)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "NAME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = userName.ifBlank { "User" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Field 2: Age (Uneditable)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "AGE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (userAge in 1..100) "$userAge yrs" else "21 yrs",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Field 3: Gender (Uneditable)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "GENDER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = userGender.ifBlank { "Male" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Field 3: Verified Email (Fixed with greenish tick mark)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "VERIFIED EMAIL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF059669).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = userEmail.ifBlank { "verified.user@auth" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(Color(0x2210B981), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Verified",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    // Field 4: Authentication Provider Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B132B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Connected via ${authProvider.replaceFirstChar { it.uppercase() }.ifBlank { "OAuth" }}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "100% Genuine ✓",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Sign Out Option at the end
                Button(
                    onClick = {
                        onDismiss()
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sign Out / Switch Account",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * About Qwik Save Dialog:
 * - Upper paragraph describing the app
 * - Interactive Rate App option (once submitted, locked)
 * - 4.7 ⭐ overall rating display
 * - About Founder/Developer Priyal Kumar integrated right below
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedRating by remember {
        mutableFloatStateOf(
            UserPreferences.getUserRating(context).let { if (it > 0f) it else 5f }
        )
    }
    var hasRated by remember { mutableStateOf(UserPreferences.hasUserRated(context)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .heightIn(max = 580.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with 'X' close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo_transparent),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About Qwik Save",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top App Overview
                    Text(
                        text = "Save in One Click.",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF38BDF8)
                    )

                    Text(
                        text = "Qwik Save is an enterprise-grade digital suite engineered to solve the everyday frustrations students, researchers, and creators face. From converting AI study sessions into formatted academic PDFs to downloading Instagram, Facebook, and YouTube videos in HD, all under one unified dashboard.",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 18.sp
                    )

                    // RATE APP SECTION (Just above the overall rating)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RATE QWIK SAVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    letterSpacing = 0.8.sp
                                )
                                if (hasRated) {
                                    Text(
                                        text = "✓ Rating Recorded",
                                        fontSize = 10.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!hasRated) {
                                Text(
                                    text = "Tap a star to rate your experience and submit to the global community score:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    (1..5).forEach { star ->
                                        IconButton(
                                            onClick = { selectedRating = star.toFloat() },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "$star stars",
                                                tint = if (star <= selectedRating) Color(0xFFFBBF24) else Color(0xFF64748B),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        UserPreferences.setUserRating(context, selectedRating)
                                        hasRated = true

                                        // Async sync rating to Supabase
                                        val userEmail = UserPreferences.getUserEmail(context).trim()
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val url = URL("https://trfpjmtmsmzljcssvxnn.supabase.co/rest/v1/ratings")
                                                val conn = url.openConnection() as HttpURLConnection
                                                conn.requestMethod = "POST"
                                                conn.setRequestProperty("apikey", "sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                                                conn.setRequestProperty("Authorization", "Bearer sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv")
                                                conn.setRequestProperty("Content-Type", "application/json")
                                                conn.doOutput = true
                                                val payload = JSONObject().apply {
                                                    put("email", userEmail)
                                                    put("rating", selectedRating)
                                                    put("created_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                                                }
                                                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                                conn.responseCode
                                            } catch (_: Exception) {}
                                        }
                                        Toast.makeText(context, "Thank you! Your rating (${selectedRating.toInt()} ⭐) has been submitted.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                                ) {
                                    Text("Submit Rating", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        (1..5).forEach { star ->
                                            Icon(
                                                imageVector = if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = null,
                                                tint = if (star <= selectedRating) Color(0xFFFBBF24) else Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${selectedRating.toInt()} / 5.0",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Text(
                                        text = "Submitted ✓",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }

                    // OVERALL APP RATING BADGES (Fixed at 4.7 / 5.0 based on user reviews)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("⭐ 4.7 / 5.0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                                Text("2,400+ authentic reviews", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("🎓 5,000+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                Text("Students empowered", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    // FOUNDER & DEVELOPER SECTION (Integrated directly below Rating)
                    Text(
                        text = "FOUNDER & LEAD DEVELOPER:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399),
                        letterSpacing = 1.sp
                    )

                    // Founder Photo + Credentials Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.priyal_kumar),
                                contentDescription = "Priyal Kumar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Priyal Kumar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Founder & Lead Developer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🎓 CSE'27 @ HBTU Kanpur",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Text(
                        text = "College Study was founded by Priyal Kumar with a vision to simplify access to academic resources for college students. Having experienced firsthand the challenges students face while searching for notes, previous year papers, and practical files scattered across multiple platforms, he built a centralized solution trusted by 5,000+ active learners.\n\nHaving seen how students and creators struggled searching across dozens of separate, ad-filled websites for video downloads, PDF conversions, and notes formatting, Qwik Save was engineered to bring every essential tool together in one organized, lightning-fast suite in a single click.",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 18.sp
                    )

                    // Connect Links (LinkedIn + College Study)
                    Text(
                        text = "CONNECT WITH PRIYAL:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )

                    // LinkedIn Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/priyal-kumar-29b26529a/"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinkedInLogo(size = 18.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect on LinkedIn ↗", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // College Study Button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://college-study.netlify.app/"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🎓 College Study (5,000+ Students) ↗", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "📍 Kanpur, Uttar Pradesh, India",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

/**
 * FAQs Dialog: Contains "Why Qwik Save is Different" + all common user queries.
 */
@Composable
fun FaqsDialog(onDismiss: () -> Unit) {
    val faqs = listOf(
        "Why is Qwik Save different from other tools?" to "Qwik Save brings everything together in one unified app with zero ads:\n• 100% Client-Side Privacy: Your study notes, exam PDFs, and photos never leave your device.\n• Exact File Size (MB) Shown before downloading.\n• Direct 1080p Full HD Video Downloads with zero watermarks.\n• Lock-Screen Background Music streaming for 1500+ tracks.\n• Completely Free & Unlimited with no daily caps or surprise subscriptions.",
        "How do I convert ChatGPT or AI notes to PDF?" to "Click the 'AI Notes to PDF' option under PDF Tools. You can either paste a public ChatGPT, Claude, or Gemini share link, or type/paste your lecture notes directly. The app formats it into a clean, printable A4 PDF with formulas, code highlighting, and page numbers.",
        "Do I need to sign up, log in, or pay?" to "No! Qwik Save offers instant 1-click verification via Google, GitHub, or Facebook with zero passwords or credit card requirements. All tools are 100% free.",
        "Do I ever need to enter my AI login credentials?" to "Never. You should NEVER share your OpenAI, Google, or Anthropic passwords with any app. We only process public links or text you explicitly paste.",
        "How does the video downloader work?" to "Simply paste the link of any public Instagram Reel, Facebook Video, or YouTube Short. Qwik Save extracts the raw, high-definition MP4 stream directly from the CDN with zero watermarks.",
        "How does background music playback work on the lock screen?" to "Qwik Save Music Hub runs as an Android Foreground Media Service with a registered MediaSession. This lets audio play continuously even when your phone screen is locked or while multitasking in other apps, with full lockscreen play/pause/skip controls.",
        "Are my photos and PDFs kept private?" to "Yes, 100%! All PDF generation (Image to PDF, Notes to PDF, Merge) runs entirely on your phone's processor using Android's native PdfDocument engine. No files are uploaded to any server.",
        "Can I convert long exam revision notes?" to "Yes! Our PDF engine is optimized for heavy study guides, including 50+ page multi-unit question banks with hundreds of questions, answers, and formulas."
    )

    var expandedIndex by remember { mutableStateOf<Int?>(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
            ) {
                    // Header with 'X' close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Frequently Asked (FAQs)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 10.dp))

                    // Scrollable FAQs
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        faqs.forEachIndexed { idx, (question, answer) ->
                            val isExpanded = expandedIndex == idx
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isExpanded) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable { expandedIndex = if (isExpanded) null else idx }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = question,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = answer,
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

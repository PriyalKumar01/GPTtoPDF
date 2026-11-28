package com.qwiksave.app.ui.main

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.qwiksave.app.R
import com.qwiksave.app.battery.BatteryStudioDialog
import com.qwiksave.app.data.UserPreferences
import com.qwiksave.app.sdk.AiChatMessage
import com.qwiksave.app.sdk.AiConversationData
import com.qwiksave.app.ui.scanner.ScannerStudio
import com.qwiksave.app.sdk.ExamQuestionItem
import com.qwiksave.app.sdk.ExtractedMedia
import com.qwiksave.app.sdk.QwikSaveClient
import com.qwiksave.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf(UserPreferences.getUserName(context)) }

    // Bottom Navigation: 0 = Explore (Default), 1 = PDF Tools, 2 = Social Media, 3 = Utilities & QR
    var selectedBottomTab by remember { mutableStateOf(0) }
    var selectedSocialPlatform by remember { mutableStateOf(0) } // 0=Instagram, 1=Facebook, 2=YouTube
    var selectedPdfSubTab by remember { mutableStateOf(0) } // 0=AI Notes, 1=Image to PDF, 2=Merge, 3=Compress, 4=PDF to Image
    var isMusicHubOpen by remember { mutableStateOf(false) }
    var activeDedicatedTool by remember { mutableStateOf<String?>(null) } // "pdf-to-image", "freeconvert-compressors", "ppt-to-pdf"
    var showBatteryStudio by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val act = context as? Activity
        if (act?.intent?.getBooleanExtra("OPEN_BATTERY_STUDIO", false) == true) {
            showBatteryStudio = true
            act.intent.removeExtra("OPEN_BATTERY_STUDIO")
        }
    }

    val activeDialog = remember { mutableStateOf(ActiveDialog.NONE) }
    val client = remember { QwikSaveClient() }

    // Ensure status bar icons are crisp bright white over the dark top header (just like WhatsApp!)
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.let {
            val insets = WindowCompat.getInsetsController(it, it.decorView)
            insets.isAppearanceLightStatusBars = false // false = WHITE icons for time, battery, wifi
            insets.isAppearanceLightNavigationBars = false // false = WHITE glowing navigation bar buttons
        }
        onDispose {}
    }

    // Dynamic Thematic Background Color based on Active Section / Tool
    val dynamicThemeBackground = if (isMusicHubOpen) {
        Color(0xFF121212) // Dark Greenish Spotify Hub Vibe
    } else when (selectedBottomTab) {
        1 -> when (selectedPdfSubTab) {
            0 -> Color(0xFFF8FAFC) // AI Study Notes
            1 -> Color(0xFFF5F3FF) // Image ↔ PDF
            else -> Color(0xFFEEF2FF) // Merge & Compress
        }
        2 -> when (selectedSocialPlatform) {
            0 -> Color(0xFFFFF1F2) // Instagram - Soft Rose/Pink tint
            1 -> Color(0xFFF0F7FF) // Facebook - Soft Sky Blue tint
            else -> Color(0xFFFEF2F2) // YouTube - Soft Red tint
        }
        else -> Color(0xFFF8FAFC)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBarSection(
                userName = userName,
                activeDialog = activeDialog,
                onSignOut = onSignOut
            )
        },
        bottomBar = {
            BottomNavigationSection(
                selectedTab = if (isMusicHubOpen || activeDedicatedTool != null) -1 else selectedBottomTab,
                onTabSelected = { tab ->
                    isMusicHubOpen = false
                    activeDedicatedTool = null
                    selectedBottomTab = tab
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(dynamicThemeBackground)
        ) {
            if (isMusicHubOpen) {
                MusicHubScreen(
                    client = client,
                    onBack = {
                        isMusicHubOpen = false
                        selectedBottomTab = 2 // Return to Social Media downloader page
                    }
                )
            } else if (activeDedicatedTool != null) {
                when (activeDedicatedTool) {
                    "pdf-to-image" -> PdfToImageStudioScreen(
                        client = client,
                        onBack = { activeDedicatedTool = null }
                    )
                    "freeconvert-compressors" -> FreeConvertCompressorsStudioScreen(
                        client = client,
                        onBack = { activeDedicatedTool = null }
                    )
                    "ppt-to-pdf" -> PptToPdfStudioScreen(
                        client = client,
                        onBack = { activeDedicatedTool = null }
                    )
                    "pdf-lock" -> PdfLockStudioScreen(
                        client = client,
                        onBack = { activeDedicatedTool = null }
                    )
                }
            } else {
                when (selectedBottomTab) {
                    0 -> ExploreTabContent(
                        client = client,
                        onNavigateToSocial = { platform ->
                            selectedSocialPlatform = platform
                            selectedBottomTab = 2
                        },
                        onNavigateToPdf = { subTab ->
                            selectedPdfSubTab = subTab
                            selectedBottomTab = 1
                        },
                        onNavigateToUtilities = {
                            selectedBottomTab = 3
                        },
                        onNavigateToMusic = {
                            isMusicHubOpen = true
                        },
                        onOpenBatteryStudio = {
                            showBatteryStudio = true
                        },
                        onOpenDedicatedTool = { tool ->
                            activeDedicatedTool = tool
                        }
                    )
                    1 -> PdfToolsTabContent(
                        client = client,
                        selectedSubTab = selectedPdfSubTab,
                        onSubTabChange = { selectedPdfSubTab = it },
                        onOpenDedicatedTool = { activeDedicatedTool = it }
                    )
                    2 -> SocialMediaTabContent(
                        client = client,
                        selectedPlatform = selectedSocialPlatform,
                        onPlatformChange = { selectedSocialPlatform = it },
                        onOpenMusicHub = { isMusicHubOpen = true }
                    )
                    3 -> UtilitiesTabContent(client = client)
                }

                OverflowDialogHost(activeDialog = activeDialog, onSignOut = onSignOut)

                if (showBatteryStudio) {
                    BatteryStudioDialog(onDismiss = { showBatteryStudio = false })
                }
            }
        }
    }
}

/**
 * Top App Bar with Transparent Banner Logo & Tagline, Status Bar Insets, and 3-Dot Menu.
 * Shifted comfortably lower down away from the status bar & notch.
 */
@Composable
fun TopAppBarSection(
    userName: String,
    activeDialog: MutableState<ActiveDialog>,
    onSignOut: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Wide Rectangular Brand Banner with Tagline (Transparent)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.qwik_save_banner_light),
                    contentDescription = "Qwik Save - Save in One Click",
                    modifier = Modifier
                        .height(38.dp)
                        .widthIn(max = 180.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Right: Greeting with Executive PRO Badge & 3-Dot Overflow Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val userGender = com.qwiksave.app.data.UserPreferences.getUserGender(LocalContext.current)
                    val isFemale = userGender.equals("Female", ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isFemale) Color(0xFFF43F5E).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFemale) Icons.Default.Face3 else Icons.Default.Face6,
                            contentDescription = null,
                            tint = if (isFemale) Color(0xFFFB7185) else Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Hello, $userName 👋",
                        color = Color(0xFFF8FAFC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TopBarOverflowMenu(activeDialog = activeDialog, onSignOut = onSignOut)
            }
        }
    }
}

/**
 * Bottom Navigation Bar with Navigation Bar insets.
 */
@Composable
fun BottomNavigationSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        val navItems = listOf(
            Triple(0, "Explore", Icons.Default.Explore),
            Triple(1, "PDF Tools", Icons.Default.PictureAsPdf),
            Triple(2, "Social Media", Icons.Default.PlayCircle),
            Triple(3, "Utilities & QR", Icons.Default.QrCode)
        )

        navItems.forEach { (index, title, icon) ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(if (isSelected) 26.dp else 22.dp)
                    )
                },
                label = {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color(0xFF1E293B),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8)
                )
            )
        }
    }
}

/* ===================================================================================
   TAB 0: EXPLORE (ALL FEATURES) - DEFAULT VIEW
   =================================================================================== */
@Composable
fun ExploreTabContent(
    client: QwikSaveClient,
    onNavigateToSocial: (platform: Int) -> Unit,
    onNavigateToPdf: (subTab: Int) -> Unit,
    onNavigateToUtilities: () -> Unit,
    onNavigateToMusic: () -> Unit = {},
    onOpenBatteryStudio: () -> Unit = {},
    onOpenDedicatedTool: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Title: Explore All Features (styled "Features!" with inverted arc underline)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "Explore All ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.5).sp
                )
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Features!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF2563EB), Color(0xFFEA580C))
                            )
                        ),
                        letterSpacing = (-0.5).sp
                    )
                    // Inverted Curved Underline directly under "Features!" starting strictly from under 'F'
                    Canvas(
                        modifier = Modifier
                            .width(106.dp)
                            .height(6.dp)
                            .padding(top = 1.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val arcPath = Path().apply {
                            moveTo(0f, h - 1.5f)
                            quadraticTo(w * 0.5f, 1.5f, w, h - 1.5f)
                        }
                        drawPath(
                            path = arcPath,
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF2563EB), Color(0xFFEA580C))
                            ),
                            style = Stroke(
                                width = 2.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }
        }

        // Feature Cards 2-per-Row Grid (Modern App UI matching 2-cards per line layout)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Instagram + Facebook
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Instagram",
                    subtitle = "Download Reels & Posts ∞",
                    badge = "Trending",
                    badgeColor = Color(0xFFEC4899),
                    cardBgColor = Color(0xFFFFF1F2),
                    logo = { InstagramLogo(size = 64.dp) },
                    onClick = { onNavigateToSocial(0) }
                )
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Facebook",
                    subtitle = "Download Reels & Long Videos",
                    badge = "Popular",
                    badgeColor = Color(0xFF0284C7),
                    cardBgColor = Color(0xFFEFF6FF),
                    logo = { FacebookLogo(size = 64.dp) },
                    onClick = { onNavigateToSocial(1) }
                )
            }

            // Row 2: YouTube + Music Hub
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "YouTube",
                    subtitle = "Download Shorts, Movies & Dramas",
                    badge = "Universal",
                    badgeColor = Color(0xFFEF4444),
                    cardBgColor = Color(0xFFFEF2F2),
                    logo = { YouTubeLogo(size = 64.dp) },
                    onClick = { onNavigateToSocial(2) }
                )
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Qwik Music",
                    subtitle = "Enjoy Online & Offline Songs",
                    badge = "Song Vibe",
                    badgeColor = Color(0xFF10B981),
                    cardBgColor = Color(0xFFF0FDF4),
                    logo = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF064E3B)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    },
                    onClick = { onNavigateToMusic() }
                )
            }

            // Row 3: AI Chat to PDF + Image to PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "AI Chat to PDF",
                    subtitle = "Convert AI Exam Notes in Seconds",
                    badge = "Exam Time",
                    badgeColor = Color(0xFF10B981),
                    cardBgColor = Color(0xFFECFDF5),
                    logo = { AiNotesLogo(size = 64.dp) },
                    onClick = { onNavigateToPdf(0) }
                )
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Doc Scanner",
                    subtitle = "Scan, Crop & Export PDF",
                    badge = "Pro",
                    badgeColor = Color(0xFFDC2626),
                    cardBgColor = Color(0xFFFEF2F2),
                    logo = { PdfSuiteLogo(size = 64.dp) },
                    onClick = { onNavigateToPdf(1) }
                )
            }

            // Row 4: Merge & Compress PDF (combined card) + PDF to Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Merge & Compress",
                    subtitle = "Combine & Shrink PDFs",
                    badge = "Popular",
                    badgeColor = Color(0xFF0284C7),
                    cardBgColor = Color(0xFFF0F9FF),
                    logo = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    },
                    onClick = { onNavigateToPdf(2) }
                )
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "PDF to Image",
                    subtitle = "Extract Pages to HD Images",
                    badge = "Ultra HD",
                    badgeColor = Color(0xFF0891B2),
                    cardBgColor = Color(0xFFECFEFF),
                    logo = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF06B6D4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    },
                    onClick = { onOpenDedicatedTool("pdf-to-image") }
                )
            }

            // Row 5: QR Studio + Video to MP3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "QR Studio",
                    subtitle = "Generate & Scan Fast QR Codes",
                    badge = "Utility",
                    badgeColor = Color(0xFF0F172A),
                    cardBgColor = Color(0xFFF8FAFC),
                    logo = { QrCodeLogo(size = 64.dp) },
                    onClick = { onNavigateToUtilities() }
                )
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Video to MP3",
                    subtitle = "Extract 320kbps Pure MP3",
                    badge = "HQ Audio",
                    badgeColor = Color(0xFF4F46E5),
                    cardBgColor = Color(0xFFEEF2FF),
                    logo = { AudioExtractorLogo(size = 64.dp) },
                    onClick = { onNavigateToSocial(3) }
                )
            }

            // Row 6: Multi Compressor Hub + PPT / Doc to PDF (Even 12 Cards Total)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "Multi Compressor",
                    subtitle = "PNG, Video, PDF, JPG, GIF",
                    badge = "FreeConvert",
                    badgeColor = Color(0xFF0D9488),
                    cardBgColor = Color(0xFFF0FDFA),
                    logo = { FreeConvertLogo(size = 64.dp) },
                    onClick = { onOpenDedicatedTool("freeconvert-compressors") }
                )
                FeatureGridCard(
                    modifier = Modifier.weight(1f),
                    title = "PPT to PDF",
                    subtitle = "Convert Slides & Docs to PDF",
                    badge = "Academic",
                    badgeColor = Color(0xFFEA580C),
                    cardBgColor = Color(0xFFFFF7ED),
                    logo = { PptToPdfLogo(size = 64.dp) },
                    onClick = { onOpenDedicatedTool("ppt-to-pdf") }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F5F9))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🔒 100% Client-Side Privacy • Zero Tracking • Direct High-Speed Downloads",
                fontSize = 11.5.sp,
                color = Color(0xFF475569),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Modern 2-Cards-Per-Row Grid Card with centered large 3D logo, bold title, tight subtitle,
 * and a minimalistic highlighted border with zero tag distractions.
 */
@Composable
fun FeatureGridCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badge: String = "",
    badgeColor: Color = Color.Transparent,
    cardBgColor: Color = Color.White,
    logo: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(156.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Logo / Icon (Prominent and centered)
            Box(
                modifier = Modifier.size(70.dp),
                contentAlignment = Alignment.Center
            ) {
                logo()
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title in Bold
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subheading immediately adjacent to title with compact line spacing
            Text(
                text = subtitle,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 11.sp
            )
        }
    }
}

/**
 * Automatically continues numbered lists (1. -> 2. -> 3.) and bullet points (• -> •) on Enter.
 * Pressing Enter on an empty list item cancels the list continuation.
 */
fun handleSmartAutoList(oldText: String, newText: String): String {
    if (newText.length > oldText.length && newText.contains("\n")) {
        val diffIdx = newText.indices.firstOrNull { idx -> idx >= oldText.length || newText[idx] != oldText[idx] } ?: return newText
        if (diffIdx < newText.length && newText[diffIdx] == '\n') {
            val lineStart = newText.lastIndexOf('\n', diffIdx - 1) + 1
            val prevLine = newText.substring(lineStart, diffIdx)

            // Numbered list: e.g. "1. ", "2. ", "1) "
            val numMatch = Regex("^(\\s*)(\\d+)([.)])\\s*(.*)$").find(prevLine)
            if (numMatch != null) {
                val indent = numMatch.groupValues[1]
                val currentNum = numMatch.groupValues[2].toIntOrNull() ?: 1
                val punct = numMatch.groupValues[3]
                val content = numMatch.groupValues[4]

                if (content.isBlank()) {
                    // Empty list item -> user pressed Enter to exit list
                    return newText.substring(0, lineStart) + newText.substring(diffIdx + 1)
                } else {
                    val nextItem = "$indent${currentNum + 1}$punct "
                    return newText.substring(0, diffIdx + 1) + nextItem + newText.substring(diffIdx + 1)
                }
            }

            // Bullet list: e.g. "• ", "- ", "* "
            val bulletMatch = Regex("^(\\s*)([•\\-*])\\s*(.*)$").find(prevLine)
            if (bulletMatch != null) {
                val indent = bulletMatch.groupValues[1]
                val bulletChar = bulletMatch.groupValues[2]
                val content = bulletMatch.groupValues[3]

                if (content.isBlank()) {
                    // Empty bullet item -> user pressed Enter to exit list
                    return newText.substring(0, lineStart) + newText.substring(diffIdx + 1)
                } else {
                    val nextBullet = "$indent$bulletChar "
                    return newText.substring(0, diffIdx + 1) + nextBullet + newText.substring(diffIdx + 1)
                }
            }
        }
    }
    return newText
}

/**
 * Wraps selected text with markdown formatting syntax, or inserts placeholder if nothing selected.
 */
fun applyMarkdownWrap(tfv: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
    val text = tfv.text
    val start = tfv.selection.min
    val end = tfv.selection.max
    return if (start != end) {
        val selected = text.substring(start, end)
        val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
        val newCursor = start + prefix.length + selected.length + suffix.length
        TextFieldValue(newText, TextRange(newCursor))
    } else {
        val placeholder = "text"
        val newText = text.substring(0, start) + prefix + placeholder + suffix + text.substring(start)
        val selStart = start + prefix.length
        val selEnd = selStart + placeholder.length
        TextFieldValue(newText, TextRange(selStart, selEnd))
    }
}

/**
 * Professional dropdown menu with dark solid boundary, square corners, and green checkmark indicator.
 */
@Composable
fun <T> DropdownSelectorBox(
    title: String,
    selectedValue: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val anchorWidthDp = with(density) { anchorWidthPx.toDp() }
    val currentLabel = options.find { it.first == selectedValue }?.second ?: ""

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates -> anchorWidthPx = coordinates.size.width },
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF0F172A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentLabel,
                        fontSize = 11.5.sp,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = (if (anchorWidthDp > 0.dp) Modifier.width(anchorWidthDp) else Modifier.fillMaxWidth())
                    .background(Color.White)
                    .border(1.2.dp, Color(0xFF0F172A), RoundedCornerShape(4.dp))
            ) {
                options.forEachIndexed { index, (key, label) ->
                    val isSelected = key == selectedValue
                    DropdownMenuItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) Color(0xFFECFDF5) else Color.White),
                        text = {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF065F46) else Color(0xFF334155)
                            )
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onSelect(key)
                            expanded = false
                        }
                    )
                    if (index < options.size - 1) {
                        HorizontalDivider(
                            color = Color(0xFFE2E8F0),
                            thickness = 0.8.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified PDF formatting dropdowns, checkboxes and permanent branding footer preview.
 */
@Composable
fun PdfCustomizationAccordion(
    paperSizeChoice: String,
    onPaperSizeChange: (String) -> Unit,
    fontSizeScaleChoice: String,
    onFontSizeScaleChange: (String) -> Unit,
    fontFamilyChoice: String,
    onFontFamilyChange: (String) -> Unit,
    borderStyleChoice: String,
    onBorderStyleChange: (String) -> Unit,
    partnerBadgeScope: String,
    onPartnerBadgeScopeChange: (String) -> Unit,
    includeTitleToggle: Boolean,
    onIncludeTitleChange: (Boolean) -> Unit,
    includeDateToggle: Boolean,
    onIncludeDateChange: (Boolean) -> Unit,
    includePageNumbersToggle: Boolean,
    onIncludePageNumbersChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DropdownSelectorBox(
                title = "Paper Size:",
                selectedValue = paperSizeChoice,
                options = listOf(
                    Pair("A4", "A4 (Standard Academic - 210 x 297 mm)"),
                    Pair("Letter", "US Letter (8.5 x 11 in)"),
                    Pair("Legal", "Legal (8.5 x 14 in)")
                ),
                onSelect = onPaperSizeChange
            )

            DropdownSelectorBox(
                title = "Font Scale:",
                selectedValue = fontSizeScaleChoice,
                options = listOf(
                    Pair("small", "Compact (8.5pt body - Max Content)"),
                    Pair("medium", "Standard (10pt body - Balanced)"),
                    Pair("large", "Readable (11.5pt body - Large & Clear)")
                ),
                onSelect = onFontSizeScaleChange
            )

            DropdownSelectorBox(
                title = "Typography:",
                selectedValue = fontFamilyChoice,
                options = listOf(
                    Pair("Helvetica", "Inter / Modern Clean (Sans-Serif)"),
                    Pair("Times", "Times / Academic Serif"),
                    Pair("Courier", "Courier / Monospace Code")
                ),
                onSelect = onFontFamilyChange
            )

            DropdownSelectorBox(
                title = "Page Border Style:",
                selectedValue = borderStyleChoice,
                options = listOf(
                    Pair("shadow", "MS Word Dark Shadow Border"),
                    Pair("clean", "Thin Clean Line Border"),
                    Pair("none", "No Border (Full Bleed)")
                ),
                onSelect = onBorderStyleChange
            )

            DropdownSelectorBox(
                title = "College Study Header:",
                selectedValue = partnerBadgeScope,
                options = listOf(
                    Pair("all-pages", "Every Page Header 🎓"),
                    Pair("first-page", "First Page Only 🎓"),
                    Pair("none", "No Partner Header")
                ),
                onSelect = onPartnerBadgeScopeChange
            )

            // Checkbox Toggles (tightly grouped, checked by default, full row clickable)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIncludeTitleChange(!includeTitleToggle) }
                        .padding(vertical = 1.dp)
                ) {
                    Checkbox(
                        checked = includeTitleToggle,
                        onCheckedChange = onIncludeTitleChange,
                        modifier = Modifier.size(24.dp),
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981), checkmarkColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Include Document Title Banner", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIncludeDateChange(!includeDateToggle) }
                        .padding(vertical = 1.dp)
                ) {
                    Checkbox(
                        checked = includeDateToggle,
                        onCheckedChange = onIncludeDateChange,
                        modifier = Modifier.size(24.dp),
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981), checkmarkColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Include Export Date & Time", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIncludePageNumbersChange(!includePageNumbersToggle) }
                        .padding(vertical = 1.dp)
                ) {
                    Checkbox(
                        checked = includePageNumbersToggle,
                        onCheckedChange = onIncludePageNumbersChange,
                        modifier = Modifier.size(24.dp),
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981), checkmarkColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Include Page Numbers", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                }
            }

            // Permanent PDF Footer Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PDF Footer Preview", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("Page 1 of ~12", fontSize = 9.sp, color = Color(0xFF94A3B8))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Qwik Save",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qwiksave.vercel.app"))
                                context.startActivity(intent)
                            }
                        )
                        Text(
                            text = "Developer: Priyal Kumar ↗",
                            fontSize = 10.sp,
                            color = Color(0xFF475569),
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/priyal-kumar-29b26529a/"))
                                context.startActivity(intent)
                            }
                        )
                    }
                    Text(
                        text = "Partnered with College Study ↗",
                        fontSize = 9.5.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://college-study.netlify.app/"))
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 2.dp))
                    Text(
                        text = "🔒 Official watermark and footer are permanent.",
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/* ===================================================================================
   TAB 1: PDF TOOLS (AI Notes, Image to PDF, Merge, Compress, PDF to Image + Share PDF)
   =================================================================================== */
@Composable
fun PdfToolsTabContent(
    client: QwikSaveClient,
    selectedSubTab: Int,
    onSubTabChange: (Int) -> Unit,
    onOpenDedicatedTool: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val subTabs = listOf("AI Study Notes", "Doc Scanner", "Merge & Compress")

    // Image to PDF states
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageRotations by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var pdfOutputName by remember { mutableStateOf("My_Document") }
    var isPdfConverting by remember { mutableStateOf(false) }
    var pdfGeneratedFile by remember { mutableStateOf<File?>(null) }
    var scannerGeneratedPdfFile by remember { mutableStateOf<File?>(null) }
    var showScannerPdfCompletionDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris
            imageRotations = emptyMap()
            Toast.makeText(context, "${uris.size} images selected!", Toast.LENGTH_SHORT).show()
        }
    }

    // Merge PDF states
    var selectedPdfUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mergeOutputName by remember { mutableStateOf("Merged_Notes") }
    var isMerging by remember { mutableStateOf(false) }
    var mergedPdfFile by remember { mutableStateOf<File?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedPdfUris = uris
            Toast.makeText(context, "${uris.size} PDFs selected for merging!", Toast.LENGTH_SHORT).show()
        }
    }

    // AI Notes to PDF states - 100% ISOLATED PER MODE (NO DUMMY PREFILLS!)
    var aiNotesMode by remember { mutableStateOf(0) } // 0 = Shared Link, 1 = Direct Notes, 2 = Structured Builder
    var aiShareUrl by remember { mutableStateOf("") }

    // Mode 0: Shared Link states
    var sharedTitle by remember { mutableStateOf("") }
    var sharedSubtitle by remember { mutableStateOf("") }
    var sharedStudentName by remember { mutableStateOf("") }
    var extractedConversation by remember { mutableStateOf<AiConversationData?>(null) }
    val activeConversationMessages = remember { mutableStateListOf<AiChatMessage>() }
    var isExtractingConversation by remember { mutableStateOf(false) }

    // Mode 1: Direct Notes states (100% Isolated, Starts Empty)
    var directTitle by remember { mutableStateOf("") }
    var directSubtitle by remember { mutableStateOf("") }
    var directFacultyName by remember { mutableStateOf("") }
    var directNotesContent by remember { mutableStateOf("") }

    // Mode 2: Structured Builder states (100% Isolated, Starts Empty)
    var builderTitle by remember { mutableStateOf("") }
    var builderSubtitle by remember { mutableStateOf("") }
    var builderFacultyName by remember { mutableStateOf("") }
    val structuredQuestions = remember { mutableStateListOf(ExamQuestionItem("", "")) }
    var currentQuestionIndex by remember { mutableStateOf(0) }

    // Shared Formatting Options (Dropdowns & Toggles)
    var borderStyleChoice by remember { mutableStateOf("shadow") } // "shadow", "clean", "none"
    var partnerBadgeScope by remember { mutableStateOf("first-page") } // "first-page", "all-pages", "none"
    var paperSizeChoice by remember { mutableStateOf("A4") } // "A4", "Letter", "Legal"
    var fontSizeScaleChoice by remember { mutableStateOf("medium") } // "small", "medium", "large"
    var fontFamilyChoice by remember { mutableStateOf("Helvetica") } // "Helvetica", "Times", "Courier"
    var includeTitleToggle by remember { mutableStateOf(true) }
    var includeDateToggle by remember { mutableStateOf(true) }
    var includePageNumbersToggle by remember { mutableStateOf(true) }
    var showCustomizationOptions by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var editingTurnId by remember { mutableStateOf<String?>(null) }
    var editingTurnText by remember { mutableStateOf("") }
    val expandedTurnIds = remember { mutableStateListOf<String>() }

    fun calculateEstimatedPages(msgs: List<AiChatMessage>): Int {
        if (msgs.isEmpty()) return 1
        var totalChars = 0
        var tableCount = 0
        var imgCount = 0
        for (m in msgs) {
            var text = m.content
            while (true) {
                val imgIdx = text.indexOf("![")
                if (imgIdx == -1) break
                val endP = text.indexOf(")", imgIdx)
                if (endP == -1) break
                text = text.substring(0, imgIdx) + text.substring(endP + 1)
            }
            while (true) {
                val dIdx = text.indexOf("data:image/", ignoreCase = true)
                if (dIdx == -1) break
                var endIdx = dIdx + 11
                while (endIdx < text.length && !text[endIdx].isWhitespace() && text[endIdx] != '"' && text[endIdx] != '\'') {
                    endIdx++
                }
                text = text.substring(0, dIdx) + text.substring(endIdx)
            }
            val cleanLines = text.split("\n").filterNot { l ->
                val t = l.trim()
                t.length > 30 && t.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' || it == '-' || it == '_' }
            }
            val clean = cleanLines.joinToString("\n").trim()
            totalChars += clean.length
            if (clean.contains("|")) tableCount++
            if (m.content.contains("![") || m.content.contains("data:image")) imgCount++
        }
        val basePages = Math.round(totalChars / 2200.0).toInt().coerceAtLeast(1)
        val extraPages = Math.round(tableCount * 0.25 + imgCount * 0.35).toInt()
        return (basePages + extraPages).coerceAtLeast(1)
    }

    var isNotesGenerating by remember { mutableStateOf(false) }
    var notesPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfCompletionDialog by remember { mutableStateOf(false) }

    // Compress PDF states
    var compressSourceUri by remember { mutableStateOf<Uri?>(null) }
    var compressSourceName by remember { mutableStateOf("") }
    var compressLevel by remember { mutableStateOf(1) } // 0=High, 1=Balanced, 2=Light
    var isCompressing by remember { mutableStateOf(false) }
    var compressedPdfFile by remember { mutableStateOf<File?>(null) }

    val pdfCompressPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            compressSourceUri = uri
            compressSourceName = uri.lastPathSegment ?: "Selected_Document.pdf"
            Toast.makeText(context, "PDF selected for compression", Toast.LENGTH_SHORT).show()
        }
    }

    // PDF to Image states
    var pdfToImageUri by remember { mutableStateOf<Uri?>(null) }
    var pdfToImageName by remember { mutableStateOf("") }
    var isExtractingImages by remember { mutableStateOf(false) }
    var extractedImagesList by remember { mutableStateOf<List<File>>(emptyList()) }

    val pdfToImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pdfToImageUri = uri
            pdfToImageName = uri.lastPathSegment ?: "Document.pdf"
            Toast.makeText(context, "PDF selected for page extraction", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sub-Tab Switcher
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(subTabs) { idx, label ->
                val isSelected = selectedSubTab == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF0F172A) else Color.Transparent)
                        .clickable { onSubTabChange(idx) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF475569)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedSubTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // AI Notes to PDF Section (Matching website layout with Partner Header & Custom Footer)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AiNotesLogo(size = 24.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Chat to Academic Study Notes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Text("Convert ChatGPT, Claude & Gemini chats into clean exam study PDFs", fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)

                        // Mode Selector: Professional Dropdown (Dark solid boundary, square corners)
                        DropdownSelectorBox(
                            title = "Select Notes Mode:",
                            selectedValue = when (aiNotesMode) {
                                0 -> "shared"
                                1 -> "direct"
                                else -> "builder"
                            },
                            options = listOf(
                                Pair("shared", "Shared Link (ChatGPT / Claude / Gemini)"),
                                Pair("direct", "Direct Notes (Custom Text / Lecture Markdown)"),
                                Pair("builder", "Structured Builder (Course Q&A & Revision)")
                            ),
                            onSelect = { modeKey ->
                                aiNotesMode = when (modeKey) {
                                    "shared" -> 0
                                    "direct" -> 1
                                    else -> 2
                                }
                            }
                        )

                        if (aiNotesMode == 0) {
                            // Mode 0: Shared AI Link (ChatGPT / Claude / Gemini)
                            if (extractedConversation == null) {
                                OutlinedTextField(
                                    value = aiShareUrl,
                                    onValueChange = { aiShareUrl = it },
                                    label = { Text("ChatGPT / Claude / Gemini Shared Link", fontSize = 11.sp) },
                                    placeholder = {
                                        Text(
                                            text = "Paste your AI chat link (ChatGPT, Claude, Gemini...)",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val clipText = clipboardManager.getText()?.text
                                            if (!clipText.isNullOrBlank()) {
                                                aiShareUrl = clipText.trim()
                                                Toast.makeText(context, "Link pasted from clipboard! 📋", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                // Percentage Progress Line when extracting AI conversation
                                DownloadProgressBar(
                                    isLoading = isExtractingConversation,
                                    accentColor = Color(0xFF10B981)
                                )

                                // Convert to Academic PDF Button
                                Button(
                                    onClick = {
                                        if (aiShareUrl.isNotBlank()) {
                                            isExtractingConversation = true
                                            coroutineScope.launch {
                                                val res = client.extractAiConversationModel(context, aiShareUrl)
                                                isExtractingConversation = false
                                                res.onSuccess { model ->
                                                    extractedConversation = model
                                                    sharedTitle = model.title
                                                    activeConversationMessages.clear()
                                                    activeConversationMessages.addAll(model.messages)
                                                    Toast.makeText(context, "Extracted ${model.messages.size} conversation messages! ✅", Toast.LENGTH_SHORT).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "Fetch error: ${err.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Please paste or enter an AI share link", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isExtractingConversation
                                ) {
                                    if (isExtractingConversation) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Extracting & Typesetting AI Chat...", fontSize = 11.sp, color = Color.White)
                                    } else {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Convert to Academic PDF 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // High-Quality Hero Graphic (Direct, clean borderless illustration)
                                Image(
                                    painter = painterResource(id = R.drawable.ai_study_hero_illustration),
                                    contentDescription = "AI Study Notes Hero",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(145.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                // Full Interactive Extracted Conversation Preview
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { extractedConversation = null }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Convert another link", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }

                                // Extracted Notes Title & Source URL Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF0F172A))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("EXTRACTED NOTES & EXAM GUIDE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        OutlinedTextField(
                                            value = sharedTitle,
                                            onValueChange = { sharedTitle = it },
                                            label = { Text("Document / Exam Subject Title", fontSize = 10.5.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(6.dp)
                                        )

                                        Text(
                                            text = extractedConversation?.sourceUrl ?: "",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF059669),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // 4-Stat Grid (Prompts, Answers, Total, Accurate Est. Pages) - Uniform 54.dp height
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val statItems = listOf(
                                        Pair("User Prompts", activeConversationMessages.count { it.role == "user" }.toString()),
                                        Pair("AI Answers", activeConversationMessages.count { it.role == "assistant" }.toString()),
                                        Pair("Total Messages", activeConversationMessages.size.toString()),
                                        Pair("Est. PDF Pages", "~${calculateEstimatedPages(activeConversationMessages)} pgs")
                                    )
                                    statItems.forEach { (label, value) ->
                                        Card(
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(text = label, fontSize = 8.5.sp, color = Color(0xFF64748B), maxLines = 1, textAlign = TextAlign.Center)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(text = value, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }

                                // Action Buttons Row: Formatting Options Accordion Toggle + Primary Download PDF
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showCustomizationOptions = !showCustomizationOptions },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            if (showCustomizationOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (showCustomizationOptions) "Hide Options" else "Customization",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            isNotesGenerating = true
                                            coroutineScope.launch {
                                                val cleanFileName = (sharedTitle.ifBlank { "AI_Study_Notes" }).replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".pdf"
                                                val result = client.createAcademicNotesPdf(
                                                    context = context,
                                                    title = sharedTitle.ifBlank { "AI Academic Study Notes" },
                                                    content = "",
                                                    subtitle = sharedSubtitle.ifBlank { null },
                                                    studentName = sharedStudentName.ifBlank { null },
                                                    watermarkText = null,
                                                    borderStyle = borderStyleChoice,
                                                    includePartnerBadge = partnerBadgeScope != "none",
                                                    partnerBadgeScope = partnerBadgeScope,
                                                    paperSize = paperSizeChoice,
                                                    fontSizeScale = fontSizeScaleChoice,
                                                    fontFamily = fontFamilyChoice,
                                                    includeTitle = includeTitleToggle,
                                                    includeDate = includeDateToggle,
                                                    includePageNumbers = includePageNumbersToggle,
                                                    footerText = null,
                                                    questions = emptyList(),
                                                    messages = activeConversationMessages.toList(),
                                                    outputFileName = cleanFileName
                                                )
                                                isNotesGenerating = false
                                                result.onSuccess { file ->
                                                    notesPdfFile = file
                                                    showPdfCompletionDialog = true
                                                    Toast.makeText(context, "Academic PDF created in Downloads/QwikSave!", Toast.LENGTH_SHORT).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = !isNotesGenerating
                                    ) {
                                        if (isNotesGenerating) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Generating...", fontSize = 11.sp)
                                        } else {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Download PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Realistic Download Percentage Bar when generating PDF
                                DownloadProgressBar(
                                    isLoading = isNotesGenerating,
                                    accentColor = Color(0xFF059669)
                                )

                                // Formatting Options Dropdowns Accordion (when opened)
                                if (showCustomizationOptions) {
                                    PdfCustomizationAccordion(
                                        paperSizeChoice = paperSizeChoice,
                                        onPaperSizeChange = { paperSizeChoice = it },
                                        fontSizeScaleChoice = fontSizeScaleChoice,
                                        onFontSizeScaleChange = { fontSizeScaleChoice = it },
                                        fontFamilyChoice = fontFamilyChoice,
                                        onFontFamilyChange = { fontFamilyChoice = it },
                                        borderStyleChoice = borderStyleChoice,
                                        onBorderStyleChange = { borderStyleChoice = it },
                                        partnerBadgeScope = partnerBadgeScope,
                                        onPartnerBadgeScopeChange = { partnerBadgeScope = it },
                                        includeTitleToggle = includeTitleToggle,
                                        onIncludeTitleChange = { includeTitleToggle = it },
                                        includeDateToggle = includeDateToggle,
                                        onIncludeDateChange = { includeDateToggle = it },
                                        includePageNumbersToggle = includePageNumbersToggle,
                                        onIncludePageNumbersChange = { includePageNumbersToggle = it }
                                    )
                                }

                                // Conversation Turns Header + Single Plus 'Add Custom Note'
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Conversation Turns (${activeConversationMessages.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )

                                    TextButton(
                                        onClick = {
                                            activeConversationMessages.add(
                                                AiChatMessage(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    role = "assistant",
                                                    content = "Write your custom exam summary, reminder or topic notes here...",
                                                    authorName = "CUSTOM NOTE"
                                                )
                                            )
                                            Toast.makeText(context, "Custom note turn added! 📝", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF059669))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Add Custom Note", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                    }
                                }

                                // Conversation Turns Interactive List
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    activeConversationMessages.forEachIndexed { idx, msg ->
                                        val isEditing = editingTurnId == msg.id
                                        val isExpanded = expandedTurnIds.contains(msg.id)
                                        val isUser = msg.role == "user"
                                        val isNote = msg.authorName.contains("NOTE", ignoreCase = true) || msg.authorName.contains("CUSTOM", ignoreCase = true)

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when {
                                                    isUser -> Color(0xFFEFF6FF)
                                                    isNote -> Color(0xFFFFFBEB)
                                                    else -> Color(0xFFF8FAFC)
                                                }
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                when {
                                                    isUser -> Color(0xFFBFDBFE)
                                                    isNote -> Color(0xFFFDE68A)
                                                    else -> Color(0xFFE2E8F0)
                                                }
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(
                                                                when {
                                                                    isUser -> Color(0xFF2563EB)
                                                                    isNote -> Color(0xFFD97706)
                                                                    else -> Color(0xFF059669)
                                                                }
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = when {
                                                                isUser -> "Turn #${idx + 1} • USER"
                                                                isNote -> "Turn #${idx + 1} • CUSTOM NOTE"
                                                                else -> "Turn #${idx + 1} • AI ANSWER"
                                                            },
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (!isUser && !isNote && !isEditing) {
                                                            TextButton(
                                                                onClick = {
                                                                    val cleaned = msg.content
                                                                        .replace(Regex("^(Certainly|Sure|Hello|Hi there)[^\\n]*\\n+", RegexOption.IGNORE_CASE), "")
                                                                        .replace(Regex("\\n+(Hope this helps|Let me know if you need anything else|Feel free to ask)[^\\n]*$", RegexOption.IGNORE_CASE), "")
                                                                        .trim()
                                                                    activeConversationMessages[idx] = msg.copy(content = cleaned)
                                                                    Toast.makeText(context, "Cleaned pleasantries!", Toast.LENGTH_SHORT).show()
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                                modifier = Modifier.height(24.dp)
                                                            ) {
                                                                Text("Clean Pleasantries", fontSize = 9.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                                            }
                                                        }

                                                        if (!isEditing) {
                                                            IconButton(
                                                                onClick = {
                                                                    editingTurnId = msg.id
                                                                    editingTurnText = msg.content
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                activeConversationMessages.removeAt(idx)
                                                                Toast.makeText(context, "Turn removed", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }

                                                if (isEditing) {
                                                    OutlinedTextField(
                                                        value = editingTurnText,
                                                        onValueChange = { editingTurnText = it },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        minLines = 3,
                                                        maxLines = 8,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        TextButton(onClick = { editingTurnId = null }) {
                                                            Text("Cancel", fontSize = 11.sp, color = Color(0xFF64748B))
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Button(
                                                            onClick = {
                                                                activeConversationMessages[idx] = msg.copy(content = editingTurnText)
                                                                editingTurnId = null
                                                                Toast.makeText(context, "Turn updated! 💾", Toast.LENGTH_SHORT).show()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                } else {
                                                    val displayText = if (!isExpanded && msg.content.length > 350) {
                                                        msg.content.take(350) + "..."
                                                    } else {
                                                        msg.content
                                                    }

                                                    Text(
                                                        text = displayText,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF334155),
                                                        lineHeight = 15.sp
                                                    )

                                                    if (msg.content.length > 350) {
                                                        Text(
                                                            text = if (isExpanded) "Read Less ▴" else "Read More... ▾",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF059669),
                                                            modifier = Modifier
                                                                .clickable {
                                                                    if (isExpanded) expandedTurnIds.remove(msg.id)
                                                                    else expandedTurnIds.add(msg.id)
                                                                }
                                                                .padding(vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (aiNotesMode == 1) {
                            // Mode 1: Direct Notes (Custom Text / Lecture Notes) - 100% Isolated State
                            // Top Action Row: Formatting Options Toggle + Solid Green Download PDF
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showCustomizationOptions = !showCustomizationOptions },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        if (showCustomizationOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (showCustomizationOptions) "Hide Options" else "Customization",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (directNotesContent.isBlank()) {
                                            Toast.makeText(context, "Please enter your study notes content", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isNotesGenerating = true
                                        coroutineScope.launch {
                                            val cleanFileName = (directTitle.ifBlank { "Direct_Study_Notes" }).replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".pdf"
                                            val result = client.createAcademicNotesPdf(
                                                context = context,
                                                title = directTitle.ifBlank { "Academic Study Notes" },
                                                content = directNotesContent,
                                                subtitle = directSubtitle.ifBlank { null },
                                                studentName = directFacultyName.ifBlank { null },
                                                watermarkText = null,
                                                borderStyle = borderStyleChoice,
                                                includePartnerBadge = partnerBadgeScope != "none",
                                                partnerBadgeScope = partnerBadgeScope,
                                                paperSize = paperSizeChoice,
                                                fontSizeScale = fontSizeScaleChoice,
                                                fontFamily = fontFamilyChoice,
                                                includeTitle = includeTitleToggle,
                                                includeDate = includeDateToggle,
                                                includePageNumbers = includePageNumbersToggle,
                                                footerText = null,
                                                questions = emptyList(),
                                                messages = emptyList(),
                                                outputFileName = cleanFileName
                                            )
                                            isNotesGenerating = false
                                            result.onSuccess { file ->
                                                notesPdfFile = file
                                                showPdfCompletionDialog = true
                                                Toast.makeText(context, "Academic PDF created in Downloads/QwikSave!", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isNotesGenerating
                                ) {
                                    if (isNotesGenerating) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generating...", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Realistic Download Percentage Bar when generating PDF
                            DownloadProgressBar(
                                isLoading = isNotesGenerating,
                                accentColor = Color(0xFF059669)
                            )

                            // Formatting Options Accordion (when opened)
                            if (showCustomizationOptions) {
                                PdfCustomizationAccordion(
                                    paperSizeChoice = paperSizeChoice,
                                    onPaperSizeChange = { paperSizeChoice = it },
                                    fontSizeScaleChoice = fontSizeScaleChoice,
                                    onFontSizeScaleChange = { fontSizeScaleChoice = it },
                                    fontFamilyChoice = fontFamilyChoice,
                                    onFontFamilyChange = { fontFamilyChoice = it },
                                    borderStyleChoice = borderStyleChoice,
                                    onBorderStyleChange = { borderStyleChoice = it },
                                    partnerBadgeScope = partnerBadgeScope,
                                    onPartnerBadgeScopeChange = { partnerBadgeScope = it },
                                    includeTitleToggle = includeTitleToggle,
                                    onIncludeTitleChange = { includeTitleToggle = it },
                                    includeDateToggle = includeDateToggle,
                                    onIncludeDateChange = { includeDateToggle = it },
                                    includePageNumbersToggle = includePageNumbersToggle,
                                    onIncludePageNumbersChange = { includePageNumbersToggle = it }
                                )
                            }

                            OutlinedTextField(
                                value = directTitle,
                                onValueChange = { directTitle = it },
                                label = { Text("Subject / Document Title", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Operating Systems / Software Engineering", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = directSubtitle,
                                onValueChange = { directSubtitle = it },
                                label = { Text("Unit / Topic / Examination Code (Optional)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Unit 3: Process Synchronization", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = directFacultyName,
                                onValueChange = { directFacultyName = it },
                                label = { Text("Student / Faculty Name (Optional)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Prof. Kumar / CS Department", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            var directNotesTfv by remember { mutableStateOf(TextFieldValue(directNotesContent)) }

                            // Quick Markdown Formatting Toolbar for Direct Notes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Format:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                                // Bold [B]
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .clickable {
                                            val nextTfv = applyMarkdownWrap(directNotesTfv, "**", "**")
                                            directNotesTfv = nextTfv
                                            directNotesContent = nextTfv.text
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                }

                                // Italic [I]
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .clickable {
                                            val nextTfv = applyMarkdownWrap(directNotesTfv, "*", "*")
                                            directNotesTfv = nextTfv
                                            directNotesContent = nextTfv.text
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 11.sp, color = Color(0xFF0F172A))
                                }

                                // Bullet Point [•]
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .clickable {
                                            val nextTfv = applyMarkdownWrap(directNotesTfv, "\n• ", "")
                                            directNotesTfv = nextTfv
                                            directNotesContent = nextTfv.text
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("• Bullet", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                }

                                // Numbered List [1.]
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .clickable {
                                            val nextTfv = applyMarkdownWrap(directNotesTfv, "\n1. ", "")
                                            directNotesTfv = nextTfv
                                            directNotesContent = nextTfv.text
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("1. List", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                }

                                // Heading [#]
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .clickable {
                                            val nextTfv = applyMarkdownWrap(directNotesTfv, "\n### ", "\n")
                                            directNotesTfv = nextTfv
                                            directNotesContent = nextTfv.text
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("### H3", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }

                            Text(
                                text = "💡 Tip: Select text to format • Auto-lists continue on Enter • _italic_ • *bold*",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )

                            OutlinedTextField(
                                value = directNotesTfv,
                                onValueChange = { newTfv ->
                                    val smartText = handleSmartAutoList(directNotesContent, newTfv.text)
                                    directNotesTfv = if (smartText != newTfv.text) {
                                        TextFieldValue(smartText, TextRange(smartText.length))
                                    } else {
                                        newTfv
                                    }
                                    directNotesContent = directNotesTfv.text
                                },
                                label = { Text("Study Notes / Lecture Content (Markdown supported)", fontSize = 11.sp) },
                                placeholder = { Text("Paste or type your comprehensive lecture notes, pointers, or code snippets here...", fontSize = 12.sp, color = Color.Gray) },
                                minLines = 8,
                                maxLines = 14,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            // Mode 2: Structured Builder (Course Q&A & Revision) - 100% Isolated State
                            // Top Action Row: Formatting Options Toggle + Solid Green Download PDF
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showCustomizationOptions = !showCustomizationOptions },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        if (showCustomizationOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (showCustomizationOptions) "Hide Options" else "Customization",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        val validQuestions = structuredQuestions.filter { it.question.isNotBlank() || it.answer.isNotBlank() }
                                        if (validQuestions.isEmpty()) {
                                            Toast.makeText(context, "Please enter at least one question and answer", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isNotesGenerating = true
                                        coroutineScope.launch {
                                            val cleanFileName = (builderTitle.ifBlank { "Exam_QA_Guide" }).replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".pdf"
                                            val result = client.createAcademicNotesPdf(
                                                context = context,
                                                title = builderTitle.ifBlank { "Exam Q&A Revision Guide" },
                                                content = "",
                                                subtitle = builderSubtitle.ifBlank { null },
                                                studentName = builderFacultyName.ifBlank { null },
                                                watermarkText = null,
                                                borderStyle = borderStyleChoice,
                                                includePartnerBadge = partnerBadgeScope != "none",
                                                partnerBadgeScope = partnerBadgeScope,
                                                paperSize = paperSizeChoice,
                                                fontSizeScale = fontSizeScaleChoice,
                                                fontFamily = fontFamilyChoice,
                                                includeTitle = includeTitleToggle,
                                                includeDate = includeDateToggle,
                                                includePageNumbers = includePageNumbersToggle,
                                                footerText = null,
                                                questions = validQuestions,
                                                messages = emptyList(),
                                                outputFileName = cleanFileName
                                            )
                                            isNotesGenerating = false
                                            result.onSuccess { file ->
                                                notesPdfFile = file
                                                showPdfCompletionDialog = true
                                                Toast.makeText(context, "Academic PDF created in Downloads/QwikSave!", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isNotesGenerating
                                ) {
                                    if (isNotesGenerating) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generating...", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Realistic Download Percentage Bar when generating PDF
                            DownloadProgressBar(
                                isLoading = isNotesGenerating,
                                accentColor = Color(0xFF059669)
                            )

                            // Formatting Options Accordion (when opened)
                            if (showCustomizationOptions) {
                                PdfCustomizationAccordion(
                                    paperSizeChoice = paperSizeChoice,
                                    onPaperSizeChange = { paperSizeChoice = it },
                                    fontSizeScaleChoice = fontSizeScaleChoice,
                                    onFontSizeScaleChange = { fontSizeScaleChoice = it },
                                    fontFamilyChoice = fontFamilyChoice,
                                    onFontFamilyChange = { fontFamilyChoice = it },
                                    borderStyleChoice = borderStyleChoice,
                                    onBorderStyleChange = { borderStyleChoice = it },
                                    partnerBadgeScope = partnerBadgeScope,
                                    onPartnerBadgeScopeChange = { partnerBadgeScope = it },
                                    includeTitleToggle = includeTitleToggle,
                                    onIncludeTitleChange = { includeTitleToggle = it },
                                    includeDateToggle = includeDateToggle,
                                    onIncludeDateChange = { includeDateToggle = it },
                                    includePageNumbersToggle = includePageNumbersToggle,
                                    onIncludePageNumbersChange = { includePageNumbersToggle = it }
                                )
                            }

                            OutlinedTextField(
                                value = builderTitle,
                                onValueChange = { builderTitle = it },
                                label = { Text("Course / Subject Name", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Operating Systems / Data Structures", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = builderSubtitle,
                                onValueChange = { builderSubtitle = it },
                                label = { Text("Unit / Topic / Examination Code", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Unit 3: Memory Management & Paging", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = builderFacultyName,
                                onValueChange = { builderFacultyName = it },
                                label = { Text("Student / Professor / Faculty Name (Optional)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Prof. R. Sharma / Sneha Verma", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Horizontal Question Card Carousel / Pager
                            val safeIdx = currentQuestionIndex.coerceIn(0, (structuredQuestions.size - 1).coerceAtLeast(0))
                            val currentItem = structuredQuestions.getOrNull(safeIdx) ?: ExamQuestionItem("", "")

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Pager Header: Question Counter Badge + Add & Delete Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF0F172A))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Question ${safeIdx + 1} of ${structuredQuestions.size}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedButton(
                                                onClick = {
                                                    structuredQuestions.add(
                                                        ExamQuestionItem(
                                                            "",
                                                            ""
                                                        )
                                                    )
                                                    currentQuestionIndex = structuredQuestions.size - 1
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0F172A))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Add Question", fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                            }

                                            if (structuredQuestions.size > 1) {
                                                IconButton(
                                                    onClick = {
                                                        structuredQuestions.removeAt(safeIdx)
                                                        if (currentQuestionIndex >= structuredQuestions.size) {
                                                            currentQuestionIndex = (structuredQuestions.size - 1).coerceAtLeast(0)
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Question / Topic Title Input
                                    OutlinedTextField(
                                        value = currentItem.question,
                                        onValueChange = { newQ ->
                                            structuredQuestions[safeIdx] = currentItem.copy(question = newQ)
                                        },
                                        label = { Text("Question / Topic Title", fontSize = 11.sp) },
                                        placeholder = { Text("e.g. Explain Round Robin Scheduling Algorithm", fontSize = 11.sp, color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(6.dp),
                                        minLines = 2,
                                        maxLines = 3
                                    )

                                    var answerTfv by remember(safeIdx, currentItem.answer) { mutableStateOf(TextFieldValue(currentItem.answer)) }

                                    // Quick Markdown Formatting Toolbar for Answer
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Format:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                                        // Bold [B]
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White)
                                                .clickable {
                                                    val nextTfv = applyMarkdownWrap(answerTfv, "**", "**")
                                                    answerTfv = nextTfv
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = nextTfv.text)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                        }

                                        // Italic [I]
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White)
                                                .clickable {
                                                    val nextTfv = applyMarkdownWrap(answerTfv, "*", "*")
                                                    answerTfv = nextTfv
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = nextTfv.text)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 11.sp, color = Color(0xFF0F172A))
                                        }

                                        // Bullet Point [•]
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White)
                                                .clickable {
                                                    val nextTfv = applyMarkdownWrap(answerTfv, "\n• ", "")
                                                    answerTfv = nextTfv
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = nextTfv.text)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("• Bullet", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                        }

                                        // Numbered List [1.]
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White)
                                                .clickable {
                                                    val nextTfv = applyMarkdownWrap(answerTfv, "\n1. ", "")
                                                    answerTfv = nextTfv
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = nextTfv.text)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("1. List", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                        }

                                        // Heading [#]
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White)
                                                .clickable {
                                                    val nextTfv = applyMarkdownWrap(answerTfv, "\n### ", "\n")
                                                    answerTfv = nextTfv
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = nextTfv.text)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("### H3", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                    }

                                    Text(
                                        text = "💡 Tip: Select text to format • Auto-lists continue on Enter • _italic_ • *bold*",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )

                                    // Answer / Explanation Input
                                    OutlinedTextField(
                                        value = answerTfv,
                                        onValueChange = { newTfv ->
                                            val smartText = handleSmartAutoList(currentItem.answer, newTfv.text)
                                            answerTfv = if (smartText != newTfv.text) {
                                                TextFieldValue(smartText, TextRange(smartText.length))
                                            } else {
                                                newTfv
                                            }
                                            structuredQuestions[safeIdx] = currentItem.copy(answer = answerTfv.text)
                                        },
                                        label = { Text("Answer / Explanation (Paragraphs & Pointers)", fontSize = 11.sp) },
                                        placeholder = { Text("Write your explanation, code or bullet points here...", fontSize = 11.sp, color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(6.dp),
                                        minLines = 4,
                                        maxLines = 7
                                    )

                                    // Carousel Navigation Controls (◀ Previous | Counter | Next ▶)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                                            enabled = currentQuestionIndex > 0,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Previous", fontSize = 11.sp)
                                        }

                                        Text(
                                            text = "${safeIdx + 1} / ${structuredQuestions.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )

                                        Button(
                                            onClick = { if (currentQuestionIndex < structuredQuestions.size - 1) currentQuestionIndex++ },
                                            enabled = currentQuestionIndex < structuredQuestions.size - 1,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Next", fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // PDF Completion Dialog with 1-Click Open & Share (Professional Dialog)
                        val currentNotesPdf = notesPdfFile
                        if (showPdfCompletionDialog && currentNotesPdf != null) {
                            ProfessionalPdfSuccessDialog(
                                pdfFile = currentNotesPdf,
                                title = "Academic Study Notes Ready",
                                tagline = "Compiled & neatly typeset for study",
                                onDismiss = { showPdfCompletionDialog = false },
                                onOpen = {
                                    client.openPdfFile(context, currentNotesPdf)
                                    showPdfCompletionDialog = false
                                },
                                onShare = {
                                    client.sharePdfFile(context, currentNotesPdf)
                                }
                            )
                        }
                    }
                }
                }
            }

            1 -> {
                // OKEN SCANNER / CAMSCANNER STYLE DOCUMENT SCANNER STUDIO
                Box(modifier = Modifier.fillMaxSize()) {
                    ScannerStudio(
                        client = client,
                        onPdfGenerated = { file ->
                            scannerGeneratedPdfFile = file
                            showScannerPdfCompletionDialog = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    val currentScannerPdf = scannerGeneratedPdfFile
                    if (showScannerPdfCompletionDialog && currentScannerPdf != null) {
                        ProfessionalPdfSuccessDialog(
                            pdfFile = currentScannerPdf,
                            title = "Document PDF Ready",
                            tagline = "Scanned, cropped & typeset with high fidelity",
                            onDismiss = { showScannerPdfCompletionDialog = false },
                            onOpen = {
                                client.openPdfFile(context, currentScannerPdf)
                                showScannerPdfCompletionDialog = false
                            },
                            onShare = {
                                client.sharePdfFile(context, currentScannerPdf)
                            }
                        )
                    }
                }
            }

            2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Merge PDF Section
                    Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("Merge Multiple PDF Documents", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Combine 2 or more PDF files into a single document", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        OutlinedButton(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedPdfUris.isEmpty()) "Select PDF Files from Storage" else "Selected ${selectedPdfUris.size} PDFs (Tap to re-select)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (selectedPdfUris.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PDF Sequence (${selectedPdfUris.size}): Use ▲ / ▼ to reorder",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                TextButton(
                                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2563EB))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("+ Add More", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedPdfUris.forEachIndexed { index, uri ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // PDF First Page Visual Thumbnail
                                            val thumbBitmap = remember(uri) {
                                                client.renderPdfFirstPageThumbnail(context, uri, targetW = 90, targetH = 120)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 46.dp, height = 60.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.White)
                                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (thumbBitmap != null) {
                                                    Image(
                                                        bitmap = thumbBitmap.asImageBitmap(),
                                                        contentDescription = "Page 1",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                                                }
                                            }

                                            // PDF Name & Page Count
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = uri.lastPathSegment ?: "Document_${index + 1}.pdf",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val pageCount = remember(uri) { client.getPdfPageCount(context, uri) }
                                                Text(
                                                    text = if (pageCount > 0) "$pageCount pages • Position ${index + 1}" else "Position ${index + 1}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            // Reorder & Delete Action Buttons
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Move Up
                                                IconButton(
                                                    onClick = {
                                                        if (index > 0) {
                                                            val mutable = selectedPdfUris.toMutableList()
                                                            val item = mutable.removeAt(index)
                                                            mutable.add(index - 1, item)
                                                            selectedPdfUris = mutable
                                                        }
                                                    },
                                                    enabled = index > 0,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ArrowUpward,
                                                        contentDescription = "Move Up",
                                                        tint = if (index > 0) Color(0xFF2563EB) else Color.LightGray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // Move Down
                                                IconButton(
                                                    onClick = {
                                                        if (index < selectedPdfUris.size - 1) {
                                                            val mutable = selectedPdfUris.toMutableList()
                                                            val item = mutable.removeAt(index)
                                                            mutable.add(index + 1, item)
                                                            selectedPdfUris = mutable
                                                        }
                                                    },
                                                    enabled = index < selectedPdfUris.size - 1,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ArrowDownward,
                                                        contentDescription = "Move Down",
                                                        tint = if (index < selectedPdfUris.size - 1) Color(0xFF2563EB) else Color.LightGray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // Remove
                                                IconButton(
                                                    onClick = {
                                                        val mutable = selectedPdfUris.toMutableList()
                                                        mutable.removeAt(index)
                                                        selectedPdfUris = mutable
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remove",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = mergeOutputName,
                                onValueChange = { mergeOutputName = it },
                                label = { Text("Merged PDF File Name", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Button(
                                onClick = {
                                    if (selectedPdfUris.size < 2) {
                                        Toast.makeText(context, "Please select at least 2 PDF files to merge", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isMerging = true
                                    coroutineScope.launch {
                                        val cleanName = mergeOutputName.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".pdf"
                                        val result = client.mergePdfs(
                                            context = context,
                                            pdfUris = selectedPdfUris,
                                            outputFileName = cleanName
                                        )
                                        isMerging = false
                                        result.onSuccess { file ->
                                            mergedPdfFile = file
                                            Toast.makeText(context, "Merged PDF saved in Downloads/QwikSave!", Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isMerging
                            ) {
                                if (isMerging) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Merging PDFs...", fontSize = 12.sp)
                                } else {
                                    Text("Merge ${selectedPdfUris.size} Documents into One PDF 📚", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        mergedPdfFile?.let { file ->
                            PdfResultCard(
                                file = file,
                                onOpen = { openFileWithIntent(context, file, "application/pdf") },
                                onShare = { client.shareFile(context, file, "application/pdf") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Compress PDF Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Compress, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("Compress PDF File Size", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Reduce megabytes of large PDF files with client-side compression", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        OutlinedButton(
                            onClick = { pdfCompressPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (compressSourceUri == null) "Select PDF to Compress" else "Selected: $compressSourceName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (compressSourceUri != null) {
                            Text("Select Compression Level:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("High (~65%)", "Balanced (~45%)", "Light (~25%)").forEachIndexed { idx, label ->
                                    FilterChip(
                                        selected = compressLevel == idx,
                                        onClick = { compressLevel = idx },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFD97706),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    isCompressing = true
                                    coroutineScope.launch {
                                        val cleanName = "QwikSave_Compressed_${System.currentTimeMillis() % 10000}.pdf"
                                        val res = client.compressPdf(
                                            context = context,
                                            pdfUri = compressSourceUri!!,
                                            compressionLevel = compressLevel,
                                            outputFileName = cleanName
                                        )
                                        isCompressing = false
                                        res.onSuccess { file ->
                                            compressedPdfFile = file
                                            Toast.makeText(context, "Compressed PDF created!", Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isCompressing
                            ) {
                                if (isCompressing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compressing PDF...", fontSize = 12.sp)
                                } else {
                                    Text("Compress PDF Document ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        compressedPdfFile?.let { file ->
                            PdfResultCard(
                                file = file,
                                onOpen = { openFileWithIntent(context, file, "application/pdf") },
                                onShare = { client.shareFile(context, file, "application/pdf") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Lock & Password Protect PDF Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("PDF Password Protect / Lock", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Encrypt PDF with 128-bit password security", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        Button(
                            onClick = { onOpenDedicatedTool("pdf-lock") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open PDF Password Locker 🔒", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // High-Resolution PDF Tools Illustration Graphic (Transparent, zoomed and fitted to screen)
                Image(
                    painter = painterResource(id = R.drawable.pdf_merge_compress_banner),
                    contentDescription = "PDF Productivity Tools",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                }
            }
        }
    }
}

/**
 * Reusable PDF result card with Open and Share PDF buttons.
 */
@Composable
fun PdfResultCard(
    file: File,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFECFDF5), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🎉 PDF Created Successfully!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF065F46))
            Text("Saved to: Downloads/QwikSave/${file.name}", fontSize = 11.sp, color = Color(0xFF047857))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share PDF ↗", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun openFileWithIntent(context: Context, file: File, mimeType: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        val uri = FileProvider.getUriForFile(context, "com.qwiksave.app.fileprovider", file)
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "File saved in Downloads/QwikSave/${file.name}", Toast.LENGTH_SHORT).show()
    }
}


/**
 * Lightweight asynchronous thumbnail loader for videos and carousel photos.
 */
@Composable
fun AsyncThumbnail(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholderIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Image
) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(!url.isNullOrBlank()) }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.inputStream.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                } catch (_: Exception) {}
            }
            isLoading = false
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* ===================================================================================
   TAB 2: SOCIAL MEDIA DOWNLOADER (Themed Pills, Dedicated Inputs, Real Video Preview)
   =================================================================================== */
@Composable
fun SocialMediaTabContent(
    client: QwikSaveClient,
    selectedPlatform: Int,
    onPlatformChange: (Int) -> Unit,
    onOpenMusicHub: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val platforms = listOf("Instagram", "Facebook", "YouTube")

    // Dedicated input states per platform (Prevents cross-tab bleeding)
    var instaUrl by remember { mutableStateOf("") }
    var fbUrl by remember { mutableStateOf("") }
    var ytUrl by remember { mutableStateOf("") }

    // Dedicated extracted media per platform
    var instaResult by remember { mutableStateOf<ExtractedMedia?>(null) }
    var fbFormats by remember { mutableStateOf<List<ExtractedMedia>>(emptyList()) }
    var ytResult by remember { mutableStateOf<ExtractedMedia?>(null) }

    // Video to Audio MP3 Extractor states
    var audioInputUrl by remember { mutableStateOf("") }
    var isAudioExtracting by remember { mutableStateOf(false) }
    var audioExtractedResult by remember { mutableStateOf<ExtractedMedia?>(null) }
    var audioStatusMessage by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Live active download tracking (prevents duplicate rapid clicks & shows live progress percentage)
    val activeDownloads = remember { mutableStateMapOf<String, Int>() }

    fun cleanDisplayQuality(raw: String): String {
        val clean = raw.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
        val sizeMatch = Regex("\\(([^)]+)\\)").find(raw)
        val size = sizeMatch?.groupValues?.get(1)?.trim()
        return if (!size.isNullOrBlank() && size != clean) "$clean ($size)" else clean
    }

    val platformColor = when (selectedPlatform) {
        0 -> Color(0xFFE1306C)
        1 -> Color(0xFF1877F2)
        else -> Color(0xFFFF0000)
    }

    val currentUrl = when (selectedPlatform) {
        0 -> instaUrl
        1 -> fbUrl
        else -> ytUrl
    }

    val currentMedia = when (selectedPlatform) {
        0 -> instaResult
        1 -> fbFormats.firstOrNull()
        else -> ytResult
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Space to center the action card comfortably in the screen
        Spacer(modifier = Modifier.height(14.dp))

        // Platform Switcher with Clean Themed Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            platforms.forEachIndexed { idx, label ->
                val isSelected = selectedPlatform == idx
                val activeBg = when (idx) {
                    0 -> Color(0xFFFDF2F8) // soft rose
                    1 -> Color(0xFFEFF6FF) // soft blue
                    else -> Color(0xFFFEF2F2) // soft red
                }
                val activeBorder = when (idx) {
                    0 -> Color(0xFFE1306C)
                    1 -> Color(0xFF1877F2)
                    else -> Color(0xFFFF0000)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) activeBg else Color.Transparent)
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, activeBorder, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .clickable {
                            onPlatformChange(idx)
                            statusMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (idx) {
                            0 -> InstagramLogo(size = 18.dp)
                            1 -> FacebookLogo(size = 18.dp)
                            2 -> YouTubeLogo(size = 18.dp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) activeBorder else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // Action Card - Clean Mid-Screen Focus
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, platformColor.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (selectedPlatform) {
                        0 -> InstagramLogo(size = 32.dp)
                        1 -> FacebookLogo(size = 32.dp)
                        2 -> YouTubeLogo(size = 32.dp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = when (selectedPlatform) {
                                0 -> "Instagram Reels & Posts HD"
                                1 -> "Facebook Videos in 1080p Full HD"
                                else -> "YouTube Shorts & Videos HD"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = when (selectedPlatform) {
                                0 -> "Paste public Instagram reel, post or story link"
                                1 -> "Paste public Facebook video or watch link"
                                else -> "Paste public YouTube video or shorts link"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                OutlinedTextField(
                    value = currentUrl,
                    onValueChange = { newVal ->
                        when (selectedPlatform) {
                            0 -> instaUrl = newVal
                            1 -> fbUrl = newVal
                            else -> ytUrl = newVal
                        }
                    },
                    placeholder = {
                        Text(
                            text = when (selectedPlatform) {
                                0 -> "https://www.instagram.com/reel/..."
                                1 -> "https://www.facebook.com/watch/..."
                                else -> "https://youtube.com/shorts/..."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        if (currentUrl.isNotEmpty()) {
                            IconButton(onClick = {
                                when (selectedPlatform) {
                                    0 -> instaUrl = ""
                                    1 -> fbUrl = ""
                                    else -> ytUrl = ""
                                }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                            if (!clip.isNullOrEmpty()) {
                                when (selectedPlatform) {
                                    0 -> instaUrl = clip
                                    1 -> fbUrl = clip
                                    else -> ytUrl = clip
                                }
                                Toast.makeText(context, "Pasted from Clipboard!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = platformColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, platformColor)
                    ) {
                        Text("Paste", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val trimmedUrl = currentUrl.trim()
                            if (trimmedUrl.isBlank()) {
                                Toast.makeText(context, "Please enter or paste a valid URL", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Strict platform URL validation to prevent 403 errors and cross-tab mismatch
                            when (selectedPlatform) {
                                0 -> {
                                    val isInsta = trimmedUrl.contains("instagram.com", ignoreCase = true) || trimmedUrl.contains("instagr.am", ignoreCase = true)
                                    if (!isInsta) {
                                        Toast.makeText(context, "Invalid link! Please enter a valid Instagram URL", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                }
                                1 -> {
                                    val isFb = trimmedUrl.contains("facebook.com", ignoreCase = true) || trimmedUrl.contains("fb.watch", ignoreCase = true) || trimmedUrl.contains("fb.me", ignoreCase = true)
                                    if (!isFb) {
                                        Toast.makeText(context, "Invalid link! Please enter a valid Facebook URL", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                }
                                2 -> {
                                    val isYt = trimmedUrl.contains("youtube.com", ignoreCase = true) || trimmedUrl.contains("youtu.be", ignoreCase = true)
                                    if (!isYt) {
                                        Toast.makeText(context, "Invalid link! Please enter a valid YouTube URL", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                }
                            }

                            isLoading = true
                            statusMessage = "Extracting video streams..."
                            coroutineScope.launch {
                                when (selectedPlatform) {
                                    0 -> {
                                        val res = client.extractInstagram(context, trimmedUrl)
                                        isLoading = false
                                        res.onSuccess {
                                            instaResult = it
                                            statusMessage = if (it.items.size > 1) "Ready: Carousel (${it.items.size} items) 📸" else if (!it.isVideo) "Ready: High-Res Photo 🖼️" else "Ready: 1080p HD (${it.fileSizeMb ?: "Full HD"})"
                                            scrollState.animateScrollTo((scrollState.value + 350).coerceAtMost(scrollState.maxValue))
                                        }.onFailure { statusMessage = "Error: ${it.message}" }
                                    }
                                    1 -> {
                                        val res = client.extractFacebook(trimmedUrl)
                                        isLoading = false
                                        res.onSuccess {
                                            fbFormats = it
                                            statusMessage = "Found ${it.size} stream formats"
                                            scrollState.animateScrollTo((scrollState.value + 350).coerceAtMost(scrollState.maxValue))
                                        }.onFailure { statusMessage = "Error: ${it.message}" }
                                    }
                                    2 -> {
                                        val res = client.extractYouTube(trimmedUrl)
                                        isLoading = false
                                        res.onSuccess {
                                            ytResult = it
                                            statusMessage = if (it.items.size > 1) "Ready: Carousel (${it.items.size} items) 📸" else if (!it.isVideo) "Ready: High-Res Photo 🖼️" else "Ready: 1080p HD (${it.fileSizeMb ?: "Full HD"})"
                                            scrollState.animateScrollTo((scrollState.value + 350).coerceAtMost(scrollState.maxValue))
                                        }.onFailure { statusMessage = "Error: ${it.message}" }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = platformColor),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Extracting...", fontSize = 12.sp)
                        } else {
                            Text("Download in 1 Click ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Compact Line Progress Bar with live percentage
                DownloadProgressBar(
                    isLoading = isLoading,
                    accentColor = platformColor
                )

                statusMessage?.let {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Live Video & Audio Preview Player (Watch & Listen directly in-app)
        currentMedia?.let { media ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    var isPlayingPreview by remember(media.streamUrl) { mutableStateOf(false) }

                    if (isPlayingPreview && media.isVideo && media.streamUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        val controller = MediaController(ctx)
                                        controller.setAnchorView(this)
                                        setMediaController(controller)
                                        setVideoURI(Uri.parse(media.streamUrl))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                        setOnErrorListener { _, _, _ ->
                                            isPlayingPreview = false
                                            Toast.makeText(ctx, "Preview playback unavailable for this stream", Toast.LENGTH_SHORT).show()
                                            true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Close Preview Button
                            IconButton(
                                onClick = { isPlayingPreview = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Preview", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .background(Color.Black)
                                .clickable {
                                    if (media.isVideo && media.streamUrl.isNotBlank()) {
                                        isPlayingPreview = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncThumbnail(
                                url = media.thumbnailUrl,
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = "Video Thumbnail",
                                placeholderIcon = Icons.Default.PlayCircle
                            )

                            // Central Play Indicator
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Video Preview",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Duration pill top-left
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = media.durationText ?: "▶ Watch Preview",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Quality pill top-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color(0xFF059669), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = media.quality,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Title & Creator
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = media.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!media.author.isNullOrBlank()) {
                            Text(
                                text = media.author,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Available Streams / Download Result Cards
        if (selectedPlatform == 1 && fbFormats.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Quality to Download:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                fbFormats.forEach { format ->
                    val cleanQuality = cleanDisplayQuality(format.quality)
                    val isDownloadingThis = activeDownloads.containsKey(format.streamUrl)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = cleanQuality,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = format.fileSizeMb ?: "MP4 Video",
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    if (isDownloadingThis) {
                                        Toast.makeText(context, "⚠️ Video is already downloading! Please wait a few seconds...", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    activeDownloads[format.streamUrl] = 0
                                    val filename = "QwikSave_Facebook_${System.currentTimeMillis()}.mp4"
                                    coroutineScope.launch {
                                        Toast.makeText(context, "Downloading Facebook video to Gallery... 🎬", Toast.LENGTH_SHORT).show()
                                        val result = client.downloadMediaDirectly(
                                            context = context,
                                            mediaUrl = format.streamUrl,
                                            filename = filename,
                                            onProgress = { _, _, percent ->
                                                activeDownloads[format.streamUrl] = percent
                                            }
                                        )
                                        activeDownloads.remove(format.streamUrl)
                                        result.onSuccess {
                                            Toast.makeText(context, "Saved to Gallery / Movies! ✅", Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Download error: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.widthIn(min = 108.dp, max = 135.dp).height(38.dp)
                            ) {
                                if (isDownloadingThis) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${activeDownloads[format.streamUrl] ?: 0}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedPlatform == 2 && ytResult != null && (ytResult?.items?.size ?: 0) > 1) {
            val ytRes = ytResult!!
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Quality to Download:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                ytRes.items.forEachIndexed { idx, itm ->
                    val cleanQuality = cleanDisplayQuality(itm.quality)
                    val isDownloadingThis = activeDownloads.containsKey(itm.url)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = cleanQuality,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "High-Speed Direct MP4 Stream",
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    if (isDownloadingThis) {
                                        Toast.makeText(context, "⚠️ Video is already downloading! Please wait a few seconds...", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    activeDownloads[itm.url] = 0
                                    val filename = "QwikSave_YouTube_${ytRes.id}_${idx + 1}_${System.currentTimeMillis()}.mp4"
                                    coroutineScope.launch {
                                        Toast.makeText(context, "Downloading YouTube video to Gallery... 🎬", Toast.LENGTH_SHORT).show()
                                        val result = client.downloadMediaDirectly(
                                            context = context,
                                            mediaUrl = itm.url,
                                            filename = filename,
                                            onProgress = { _, _, percent ->
                                                activeDownloads[itm.url] = percent
                                            }
                                        )
                                        activeDownloads.remove(itm.url)
                                        result.onSuccess {
                                            Toast.makeText(context, "Saved to Gallery / Movies! ✅", Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Download error: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.widthIn(min = 108.dp, max = 135.dp).height(38.dp)
                            ) {
                                if (isDownloadingThis) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${activeDownloads[itm.url] ?: 0}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val activeResult = if (selectedPlatform == 0) instaResult else if (selectedPlatform == 2) ytResult else null
            activeResult?.let { res ->
                // Check if Carousel / Multiple items exist (Instagram Post collection)
                if (res.items.size > 1) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "📸 Carousel (${res.items.size} Items)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        "Multi-image & video collection",
                                        fontSize = 11.sp,
                                        color = Color(0xFF047857)
                                    )
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            Toast.makeText(context, "Saving all ${res.items.size} items to Gallery... 📦", Toast.LENGTH_SHORT).show()
                                            var successCount = 0
                                            for ((idx, itm) in res.items.withIndex()) {
                                                val ext = if (itm.isVideo) "mp4" else "jpg"
                                                val fn = "QwikSave_${res.platform}_${res.id}_item_${idx + 1}_${System.currentTimeMillis()}.$ext"
                                                val dRes = client.downloadMediaDirectly(context, itm.url, fn)
                                                if (dRes.isSuccess) successCount++
                                            }
                                            Toast.makeText(context, "Saved $successCount/${res.items.size} items to Gallery! ✅", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save All (${res.items.size}) 📦", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Horizontal list of individual carousel items with preview and download button
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(res.items.size) { idx ->
                                    val itm = res.items[idx]
                                    Card(
                                        modifier = Modifier.width(140.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Image / Video Thumbnail Preview
                                            AsyncThumbnail(
                                                url = itm.url,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(95.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                placeholderIcon = if (itm.isVideo) Icons.Default.PlayCircle else Icons.Default.Image
                                            )

                                            Text(
                                                text = if (itm.isVideo) "Video ${idx + 1}" else "Photo ${idx + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF065F46)
                                            )

                                            Button(
                                                onClick = {
                                                    val ext = if (itm.isVideo) "mp4" else "jpg"
                                                    val fn = "QwikSave_${res.platform}_${res.id}_item_${idx + 1}_${System.currentTimeMillis()}.$ext"
                                                    coroutineScope.launch {
                                                        Toast.makeText(context, "Downloading item ${idx + 1}...", Toast.LENGTH_SHORT).show()
                                                        val dl = client.downloadMediaDirectly(context, itm.url, fn)
                                                        dl.onSuccess {
                                                            val fld = if (itm.isVideo) "Movies/QwikSave" else "Pictures/QwikSave"
                                                            Toast.makeText(context, "Saved to $fld! ✅", Toast.LENGTH_SHORT).show()
                                                        }.onFailure { e ->
                                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(4.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                                            ) {
                                                Text(if (itm.isVideo) "Save Video" else "Save Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Single Item Result (Photo or Video)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(res.quality, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                Text(res.fileSizeMb ?: if (res.isVideo) "HD MP4 Video" else "High-Res Image", fontSize = 11.sp, color = Color(0xFF047857))
                            }

                            val isDownloadingThis = activeDownloads.containsKey(res.streamUrl)
                            Button(
                                onClick = {
                                    if (isDownloadingThis) {
                                        Toast.makeText(context, "⚠️ File is already downloading! Please wait a few seconds...", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    activeDownloads[res.streamUrl] = 0
                                    val ext = if (res.isVideo) "mp4" else "jpg"
                                    val filename = "QwikSave_${res.platform}_${res.id}_${System.currentTimeMillis()}.$ext"
                                    coroutineScope.launch {
                                        val msg = if (res.isVideo) "Downloading video to Gallery... 🎬" else "Downloading photo to Gallery... 🖼️"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        val result = client.downloadMediaDirectly(
                                            context = context,
                                            mediaUrl = res.streamUrl,
                                            filename = filename,
                                            onProgress = { _, _, percent ->
                                                activeDownloads[res.streamUrl] = percent
                                            }
                                        )
                                        activeDownloads.remove(res.streamUrl)
                                        result.onSuccess {
                                            val dest = if (res.isVideo) "Gallery / Movies! ✅" else "Gallery / Pictures! ✅"
                                            Toast.makeText(context, "Saved to $dest", Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Download error: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                if (isDownloadingThis) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${activeDownloads[res.streamUrl] ?: 0}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (res.isVideo) "Download Video 📥" else "Download Photo 🖼️", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Platform Trust Features Cards - Strictly Equal Height & Clean Alignment (Fixes Uneven Line Wraps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Pair("⚡ Ultra Fast", "Direct CDN\nHigh speed"),
                Pair("✨ No Watermark", "Pure 1080p\nOriginal quality"),
                Pair("🔒 100% Private", "No login\nSafe & secure")
            ).forEach { (title, sub) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            sub,
                            fontSize = 9.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        // Rectangular Spotify Music Hub Banner Card (Greenish Spotify Vibe for Travel Songs)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMusicHub() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0F172A),
                                Color(0xFF064E3B),
                                Color(0xFF065F46)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.2.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFF10B981), Color(0xFF1DB954))
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Glowing Music Headphones Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1DB954).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF1DB954), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Qwik Music Hub",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• SPOTIFY VIBE 🎧",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1DB954)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "1500+ Evergreen 90s Hits, Trending Songs & Road Trip Audio • Ad-Free",
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Open Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1DB954))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Open ➔",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // In-place Video to Audio MP3 Extractor Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AudioExtractorLogo(size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Video to Audio MP3 Extractor", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Extract 320kbps MP3 audio from any video link or file", fontSize = 11.sp, color = Color(0xFF0369A1))
                    }
                }

                Text(
                    text = "Supports YouTube Shorts, Facebook Clips, Instagram Reels, and direct MP4 URLs. Extracts audio directly in-place and saves into Music/QwikSave.",
                    fontSize = 11.sp,
                    color = Color(0xFF334155),
                    lineHeight = 16.sp
                )

                // In-place Video Link Input
                OutlinedTextField(
                    value = audioInputUrl,
                    onValueChange = { audioInputUrl = it },
                    placeholder = { Text("Paste your link", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1) },
                    maxLines = 1,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (audioInputUrl.isNotEmpty()) {
                            IconButton(onClick = { audioInputUrl = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF0369A1), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFFBAE6FD),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clip.isNullOrEmpty()) {
                                audioInputUrl = clip.trim()
                                Toast.makeText(context, "Pasted from Clipboard!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7))
                    ) {
                        Text("Paste", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (audioInputUrl.isBlank()) {
                                Toast.makeText(context, "Please enter or paste a valid video URL", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isAudioExtracting = true
                            audioStatusMessage = "Extracting pristine 320kbps MP3 audio track..."
                            coroutineScope.launch {
                                val clean = audioInputUrl.trim()
                                val res = if (clean.contains("youtube.com") || clean.contains("youtu.be")) {
                                    client.extractYouTube(clean)
                                } else if (clean.contains("facebook.com") || clean.contains("fb.watch")) {
                                    val fbRes = client.extractFacebook(clean)
                                    if (fbRes.isSuccess && fbRes.getOrNull()?.isNotEmpty() == true) {
                                        Result.success(fbRes.getOrNull()!![0])
                                    } else {
                                        Result.failure(Exception("Unable to extract Facebook audio"))
                                    }
                                } else {
                                    client.extractInstagram(context, clean)
                                }

                                isAudioExtracting = false
                                res.onSuccess {
                                    audioExtractedResult = it
                                    audioStatusMessage = "Audio track ready: ${it.title.take(30)}..."
                                }.onFailure {
                                    audioStatusMessage = "Error: ${it.message}"
                                    Toast.makeText(context, "Could not extract audio: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isAudioExtracting
                    ) {
                        if (isAudioExtracting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Extracting...", fontSize = 12.sp)
                        } else {
                            Text("Extract MP3 Audio 🎵", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Compact Line Progress Bar with live percentage for MP3 Extractor
                DownloadProgressBar(
                    isLoading = isAudioExtracting,
                    accentColor = Color(0xFF0284C7)
                )

                audioStatusMessage?.let {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = Color(0xFF0369A1),
                        fontWeight = FontWeight.Medium
                    )
                }

                // If audio extracted, show instant Download button directly in card
                audioExtractedResult?.let { media ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7DD3FC))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🎵 320 kbps High Quality Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(media.title.take(45), fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            Button(
                                onClick = {
                                    val filename = "QwikSave_Audio_${System.currentTimeMillis()}.mp3"
                                    coroutineScope.launch {
                                        Toast.makeText(context, "Downloading MP3 audio to Music... 🎵", Toast.LENGTH_SHORT).show()
                                        val result = client.downloadMediaDirectly(context, media.streamUrl, filename)
                                        result.onSuccess {
                                            Toast.makeText(context, "Saved to Music / QwikSave! 🎶", Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Download error: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download MP3", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ===================================================================================
   TAB 3: UTILITIES & QR CODE STUDIO (Country code dropdown, light sky-blue MP3 card)
   =================================================================================== */
@Composable
fun UtilitiesTabContent(client: QwikSaveClient) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedQrType by remember { mutableStateOf(0) }
    val qrTypes = listOf("Website / URL", "Plain Text", "WiFi", "UPI Pay", "Phone / Contact")

    // Clean input states (NO dummy personal details prefilled!)
    var urlInput by remember { mutableStateOf("") }
    var plainTextInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var upiVpa by remember { mutableStateOf("") }
    var upiName by remember { mutableStateOf("") }

    // Phone / Contact with Country Code Dropdown
    var selectedCountryCode by remember { mutableStateOf("+91 (India)") }
    var countryDropdownOpen by remember { mutableStateOf(false) }
    val countryCodes = listOf(
        "+91 (India)",
        "+1 (USA / Canada)",
        "+44 (UK)",
        "+971 (UAE)",
        "+966 (Saudi Arabia)",
        "+61 (Australia)",
        "+49 (Germany)",
        "+33 (France)",
        "+65 (Singapore)",
        "+60 (Malaysia)",
        "+81 (Japan)",
        "+82 (South Korea)",
        "+7 (Russia)",
        "+92 (Pakistan)",
        "+880 (Bangladesh)",
        "+977 (Nepal)",
        "+94 (Sri Lanka)",
        "+62 (Indonesia)",
        "+27 (South Africa)",
        "+55 (Brazil)",
        "+39 (Italy)",
        "+34 (Spain)",
        "+41 (Switzerland)",
        "+90 (Turkey)"
    )
    var phoneNumberInput by remember { mutableStateOf("") }

    // Formatted payload computation
    val computedQrPayload = remember(selectedQrType, urlInput, plainTextInput, wifiSsid, wifiPassword, upiVpa, upiName, selectedCountryCode, phoneNumberInput) {
        when (selectedQrType) {
            0 -> urlInput.trim().ifBlank { "https://qwiksave.vercel.app" }
            1 -> plainTextInput.trim().ifBlank { "Qwik Save QR Code Studio" }
            2 -> {
                if (wifiSsid.isNotBlank()) "WIFI:S:$wifiSsid;T:WPA;P:$wifiPassword;;"
                else "WIFI:S:MyWifiNetwork;T:WPA;P:SecretKey;;"
            }
            3 -> {
                if (upiVpa.isNotBlank()) "upi://pay?pa=$upiVpa&pn=${Uri.encode(upiName)}"
                else "upi://pay?pa=payee@bank&pn=Merchant"
            }
            else -> {
                val codeOnly = selectedCountryCode.split(" ")[0]
                if (phoneNumberInput.isNotBlank()) "tel:$codeOnly$phoneNumberInput"
                else "tel:+919876543210"
            }
        }
    }

    val qrBitmap = remember(computedQrPayload) {
        client.generateQrBitmap(computedQrPayload, 420, 420)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // QR Code Studio Card - SOLID EXECUTIVE DARK THEME
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QR Code Generator & Downloader", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                Text("Create & export QR codes for Websites, WiFi, UPI & Contacts", fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)

                // Type Switcher: Professional Dropdown (Dark solid boundary, square corners)
                DropdownSelectorBox(
                    title = "Select QR Content Type:",
                    selectedValue = selectedQrType,
                    options = listOf(
                        Pair(0, "Website / URL 🌐"),
                        Pair(2, "WiFi Network 📶"),
                        Pair(3, "UPI Payment 💳"),
                        Pair(4, "Phone / Contact 📞"),
                        Pair(1, "Plain Text Note 📝")
                    ),
                    onSelect = { selectedQrType = it }
                )

                // Dynamic Input Form per Selected QR Type (With helpful placeholders)
                when (selectedQrType) {
                    0 -> {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Website URL", fontSize = 11.sp) },
                            placeholder = { Text("e.g. https://mywebsite.com", fontSize = 12.sp, color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    1 -> {
                        OutlinedTextField(
                            value = plainTextInput,
                            onValueChange = { plainTextInput = it },
                            label = { Text("Plain Text Note / Secret Message", fontSize = 11.sp) },
                            placeholder = { Text("Type any text or secret note to encode...", fontSize = 12.sp, color = Color.Gray) },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text("WiFi Network Name (SSID)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Home_WiFi_5G", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                label = { Text("WiFi Password", fontSize = 11.sp) },
                                placeholder = { Text("Enter WPA/WPA2 password", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    3 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = upiVpa,
                                onValueChange = { upiVpa = it },
                                label = { Text("UPI ID / VPA Address", fontSize = 11.sp) },
                                placeholder = { Text("e.g. yourname@okhdfcbank or upi@paytm", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = upiName,
                                onValueChange = { upiName = it },
                                label = { Text("Payee Merchant / Business Name", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Priyal Kumar", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    4 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Country Code Dropdown Box
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { countryDropdownOpen = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Country Code: $selectedCountryCode", fontSize = 12.sp, color = Color(0xFF0F172A))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = countryDropdownOpen,
                                    onDismissRequest = { countryDropdownOpen = false },
                                    modifier = Modifier.heightIn(max = 260.dp)
                                ) {
                                    countryCodes.forEach { code ->
                                        DropdownMenuItem(
                                            text = { Text(code, fontSize = 12.sp) },
                                            onClick = {
                                                selectedCountryCode = code
                                                countryDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = phoneNumberInput,
                                onValueChange = { input ->
                                    phoneNumberInput = input.filter { it.isDigit() }.take(15)
                                },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                label = { Text("Phone Number (Digits Only)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. 9876543210", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // QR Code Render Canvas (Expanded matching width of Save & Share buttons below)
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Generated QR Code",
                                    modifier = Modifier
                                        .size(260.dp)
                                        .padding(6.dp)
                                )
                            }
                        }
                    }

                    // Save to Gallery & Share Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val uri = client.saveQrBitmapToStorage(context, qrBitmap, "QwikSave_QR_${System.currentTimeMillis()}.png")
                                if (uri != null) {
                                    Toast.makeText(context, "QR Code saved to Pictures/QwikSave!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Saved to gallery!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { client.shareQrBitmap(context, qrBitmap) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F172A))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share QR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ultra-Compact, Stylish Download Progress Line with Realistic Percentage Counter
 * Climbs quickly to ~85-90%, then 92%, 95%, 97%, 98%, 99% until complete.
 */
@Composable
fun DownloadProgressBar(
    isLoading: Boolean,
    accentColor: Color = Color(0xFF1DB954),
    modifier: Modifier = Modifier
) {
    if (!isLoading) return

    var progress by remember { mutableStateOf(0.05f) }
    LaunchedEffect(isLoading) {
        val startTime = System.currentTimeMillis()
        while (isLoading) {
            val elapsed = System.currentTimeMillis() - startTime
            val target = when {
                elapsed < 400 -> 0.20f
                elapsed < 900 -> 0.48f
                elapsed < 1600 -> 0.74f
                elapsed < 2400 -> 0.88f
                elapsed < 3600 -> 0.92f
                elapsed < 5000 -> 0.95f
                elapsed < 7000 -> 0.97f
                elapsed < 9000 -> 0.98f
                else -> 0.99f
            }
            if (progress < target) {
                progress = (progress + 0.04f).coerceAtMost(target)
            }
            delay(50)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 160, easing = LinearEasing),
        label = "progress"
    )

    val percent = (animatedProgress * 100).toInt().coerceIn(1, 99)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        percent < 30 -> "Connecting to server..."
                        percent < 75 -> "Extracting high-speed stream..."
                        percent < 95 -> "Processing media streams..."
                        else -> "Finalizing download..."
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "$percent%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Sleek line bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor.copy(alpha = 0.7f), accentColor)
                        )
                    )
            )
        }
    }
}

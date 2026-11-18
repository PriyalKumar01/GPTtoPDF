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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.qwiksave.app.R
import com.qwiksave.app.data.UserPreferences
import com.qwiksave.app.sdk.ExamQuestionItem
import com.qwiksave.app.sdk.ExtractedMedia
import com.qwiksave.app.sdk.QwikSaveClient
import com.qwiksave.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf(UserPreferences.getUserName(context)) }

    // Bottom Navigation: 0 = Explore (Default), 1 = PDF Tools, 2 = Social Media, 3 = Utilities & QR
    var selectedBottomTab by remember { mutableStateOf(0) }
    var selectedSocialPlatform by remember { mutableStateOf(0) } // 0=Instagram, 1=Facebook, 2=YouTube
    var selectedPdfSubTab by remember { mutableStateOf(0) } // 0=AI Notes, 1=Image to PDF, 2=Merge, 3=Compress, 4=PDF to Image
    var isMusicHubOpen by remember { mutableStateOf(false) }

    val activeDialog = remember { mutableStateOf(ActiveDialog.NONE) }
    val client = remember { QwikSaveClient() }

    // Ensure status bar icons are crisp bright white over the dark top header (just like WhatsApp!)
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.let {
            val insets = WindowCompat.getInsetsController(it, it.decorView)
            insets.isAppearanceLightStatusBars = false // false = WHITE icons for time, battery, wifi
            insets.isAppearanceLightNavigationBars = false
        }
        onDispose {}
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!isMusicHubOpen) {
                TopAppBarSection(
                    userName = userName,
                    activeDialog = activeDialog
                )
            }
        },
        bottomBar = {
            if (!isMusicHubOpen) {
                BottomNavigationSection(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { selectedBottomTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            if (isMusicHubOpen) {
                MusicHubScreen(
                    client = client,
                    onBack = { isMusicHubOpen = false }
                )
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
                        }
                    )
                    1 -> PdfToolsTabContent(
                        client = client,
                        selectedSubTab = selectedPdfSubTab,
                        onSubTabChange = { selectedPdfSubTab = it }
                    )
                    2 -> SocialMediaTabContent(
                        client = client,
                        selectedPlatform = selectedSocialPlatform,
                        onPlatformChange = { selectedSocialPlatform = it },
                        onOpenMusicHub = { isMusicHubOpen = true }
                    )
                    3 -> UtilitiesTabContent(client = client)
                }

                OverflowDialogHost(activeDialog = activeDialog)
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
    activeDialog: MutableState<ActiveDialog>
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
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hello, $userName 👋",
                        color = Color(0xFFF8FAFC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TopBarOverflowMenu(activeDialog = activeDialog)
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
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
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
    onNavigateToUtilities: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var quickInputUrl by remember { mutableStateOf("") }
    var isQuickLoading by remember { mutableStateOf(false) }
    var quickResult by remember { mutableStateOf<ExtractedMedia?>(null) }
    var quickStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shift Universal card comfortably toward the mid-area of the screen
        Spacer(modifier = Modifier.height(14.dp))

        // Universal 1-Click Save Card - EYE ATTRACTIVE LIGHT EMERALD GREEN GRADIENT
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA7F3D0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFECFDF5),
                                Color(0xFFD1FAE5)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚡ Universal 1-Click Save",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF065F46)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF059669), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "4.7 ⭐ (2,000+)",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Paste any Instagram Reel, Facebook Video, or YouTube Short link below:",
                        fontSize = 12.sp,
                        color = Color(0xFF047857),
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = quickInputUrl,
                        onValueChange = { quickInputUrl = it },
                        placeholder = { Text("Paste video or reel URL here...", fontSize = 12.sp, color = Color(0xFF6EE7B7)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (quickInputUrl.isNotEmpty()) {
                                IconButton(onClick = { quickInputUrl = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF064E3B),
                            unfocusedTextColor = Color(0xFF064E3B),
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFF6EE7B7),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
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
                                    quickInputUrl = clip.trim()
                                    Toast.makeText(context, "Pasted from Clipboard!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF065F46)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
                        ) {
                            Text("Paste", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (quickInputUrl.isBlank()) {
                                    Toast.makeText(context, "Please paste a video link first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isQuickLoading = true
                                quickStatus = "Extracting HD media stream..."
                                coroutineScope.launch {
                                    val url = quickInputUrl.lowercase()
                                    val result = when {
                                        url.contains("instagram.com") -> client.extractInstagram(context, quickInputUrl)
                                        url.contains("facebook.com") || url.contains("fb.watch") -> {
                                            val fbList = client.extractFacebook(quickInputUrl)
                                            fbList.map { it.firstOrNull() ?: throw Exception("No media formats found") }
                                        }
                                        else -> client.extractYouTube(quickInputUrl)
                                    }

                                    isQuickLoading = false
                                    result.onSuccess { media ->
                                        quickResult = media
                                        quickStatus = "Extracted: ${media.title} (${media.quality})"
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }.onFailure { err ->
                                        quickStatus = "Extraction notice: ${err.message}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            enabled = !isQuickLoading
                        ) {
                            if (isQuickLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extracting...", fontSize = 12.sp)
                            } else {
                                Text("Extract Media ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    quickStatus?.let {
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Result card after quick extract
                    quickResult?.let { media ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Ready to Download (${media.quality}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = media.title,
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Button(
                                    onClick = {
                                        val ext = if (media.isVideo) "mp4" else "jpg"
                                        val filename = "QwikSave_${media.platform}_${System.currentTimeMillis()}.$ext"
                                        coroutineScope.launch {
                                            val label = if (media.isVideo) "video to Gallery... 🎬" else "photo to Gallery... 🖼️"
                                            Toast.makeText(context, "Downloading $label", Toast.LENGTH_SHORT).show()
                                            val result = client.downloadMediaDirectly(context, media.streamUrl, filename)
                                            result.onSuccess {
                                                val folder = if (media.isVideo) "Movies/QwikSave" else "Pictures/QwikSave"
                                                Toast.makeText(context, "Saved to $folder! ✅", Toast.LENGTH_LONG).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Download error: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val btnText = if (media.isVideo) "Download in 1 Click (${media.fileSizeMb ?: "Full HD"})" else "Download Photo HD 🖼️"
                                    Text(btnText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section Title: Explore All Features
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explore All Features",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Tap any tool to launch",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }

        // Feature Cards Grid (Re-ordered: Popular Social Media at top, then Academic PDF & Tools)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 1. Instagram
            FeatureCard(
                title = "Instagram Reel & Post Downloader",
                description = "Save public Reels, Videos, and Carousels in 1080p original HD without watermarks.",
                badge = "Trending",
                badgeColor = Color(0xFFEC4899),
                logo = { InstagramLogo(size = 36.dp) },
                onClick = { onNavigateToSocial(0) }
            )

            // 2. Facebook
            FeatureCard(
                title = "Facebook Video & Reel Downloader",
                description = "Save public Facebook clips, Reels, and Watch videos in Full HD 1080p and SD.",
                badge = "Fast",
                badgeColor = Color(0xFF2563EB),
                logo = { FacebookLogo(size = 36.dp) },
                onClick = { onNavigateToSocial(1) }
            )

            // 3. YouTube
            FeatureCard(
                title = "YouTube Shorts & Video Downloader",
                description = "Download YouTube Shorts and universal clips with direct high-speed stream access.",
                badge = "Universal",
                badgeColor = Color(0xFFEF4444),
                logo = { YouTubeLogo(size = 36.dp) },
                onClick = { onNavigateToSocial(2) }
            )

            // 4. AI Chat to PDF
            FeatureCard(
                title = "AI Chat to Academic PDF",
                description = "Convert ChatGPT, Claude & Gemini chats into structured study PDFs with formulas & code formatting.",
                badge = "Core",
                badgeColor = Color(0xFF10B981),
                logo = { AiNotesLogo(size = 36.dp) },
                onClick = { onNavigateToPdf(0) }
            )

            // 5. Image to PDF
            FeatureCard(
                title = "Image to PDF Document Converter",
                description = "Combine phone gallery photos into a single clean A4 PDF file with rotation and preview.",
                badge = "Free",
                badgeColor = Color(0xFF059669),
                logo = { PdfSuiteLogo(size = 36.dp) },
                onClick = { onNavigateToPdf(1) }
            )

            // 6. Merge PDF
            FeatureCard(
                title = "Merge Multiple PDF Documents",
                description = "Combine two or more PDF files into a single unified document offline with zero data loss.",
                badge = "Popular",
                badgeColor = Color(0xFF3B82F6),
                logo = { Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(32.dp)) },
                onClick = { onNavigateToPdf(2) }
            )

            // 7. Compress PDF
            FeatureCard(
                title = "Compress PDF File Size",
                description = "Reduce PDF file sizes with customizable compression levels directly on device.",
                badge = "Fast",
                badgeColor = Color(0xFFF59E0B),
                logo = { Icon(Icons.Default.Compress, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(32.dp)) },
                onClick = { onNavigateToPdf(3) }
            )

            // 8. PDF to Image
            FeatureCard(
                title = "PDF to Image Converter",
                description = "Extract and convert PDF pages into high-resolution PNG or JPG pictures in your gallery.",
                badge = "HD",
                badgeColor = Color(0xFF06B6D4),
                logo = { Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF0891B2), modifier = Modifier.size(32.dp)) },
                onClick = { onNavigateToPdf(4) }
            )

            // 9. QR Code Studio
            FeatureCard(
                title = "QR Code Generator & Studio",
                description = "Create customizable QR codes for Websites, WiFi, UPI, Phone, and text with country code selector.",
                badge = "Utility",
                badgeColor = Color(0xFF7C3AED),
                logo = { QrCodeLogo(size = 36.dp) },
                onClick = { onNavigateToUtilities() }
            )

            // 10. Video to MP3
            FeatureCard(
                title = "Video to Audio MP3 Extractor",
                description = "Extract crystal-clear 320kbps MP3 audio tracks directly from videos or links.",
                badge = "MP3",
                badgeColor = Color(0xFF0284C7),
                logo = { AudioExtractorLogo(size = 36.dp) },
                onClick = { onNavigateToUtilities() }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🔒 100% Client-Side Privacy • No Account Required • Save in 1 Click",
                fontSize = 11.sp,
                color = Color(0xFF475569),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    badge: String,
    badgeColor: Color,
    logo: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            logo()

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
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
    onSubTabChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val subTabs = listOf("AI Notes", "Image to PDF", "Merge PDF", "Compress", "PDF to Image")

    // Image to PDF states
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageRotations by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var pdfOutputName by remember { mutableStateOf("My_Document") }
    var isPdfConverting by remember { mutableStateOf(false) }
    var pdfGeneratedFile by remember { mutableStateOf<File?>(null) }

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

    // AI Notes to PDF states
    var aiNotesMode by remember { mutableStateOf(0) } // 0 = URL Link, 1 = Direct Text, 2 = Structured Builder
    var aiShareUrl by remember { mutableStateOf("") }
    var notesTitle by remember { mutableStateOf("Computer Science Exam Revision Notes") }
    var notesSubtitle by remember { mutableStateOf("Unit 1: Fundamentals & Theory") }
    var notesStudentName by remember { mutableStateOf("") }
    var borderStyleChoice by remember { mutableStateOf("shadow") } // "shadow", "clean", "none"
    var partnerBadgeScope by remember { mutableStateOf("first-page") } // "first-page", "all-pages"
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val structuredQuestions = remember {
        mutableStateListOf(
            ExamQuestionItem("Explain Preemptive vs Non-Preemptive Scheduling with examples.", "In preemptive scheduling, CPU can be allocated to higher priority processes, while in non-preemptive scheduling, once a process gets CPU it keeps it until termination or I/O."),
            ExamQuestionItem("What is Round Robin (RR) Scheduling?", "Round Robin is preemptive scheduling with a fixed time quantum per process in circular FIFO queue.")
        )
    }
    var notesContent by remember { mutableStateOf("### Question 1:\nExplain the difference between Preemptive and Non-Preemptive Scheduling with examples.\n\n### Answer:\nIn preemptive scheduling, CPU can be allocated to higher priority processes, while in non-preemptive scheduling, once a process gets CPU it keeps it until termination or I/O.\n\n### Question 2:\nWhat is Round Robin (RR) Scheduling?\n\n### Answer:\nRound Robin is preemptive scheduling with a fixed time quantum per process in circular FIFO queue.") }
    var isNotesGenerating by remember { mutableStateOf(false) }
    var notesPdfFile by remember { mutableStateOf<File?>(null) }

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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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

        when (selectedSubTab) {
            0 -> {
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
                            AiNotesLogo(size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("AI Chat to Academic Study PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Generates university study PDFs with College Study Partner badge", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        // Mode Selector: AI Link vs Direct Text vs Builder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Shared Link", "Direct Notes", "Structured Builder").forEachIndexed { idx, mLabel ->
                                FilterChip(
                                    selected = aiNotesMode == idx,
                                    onClick = { aiNotesMode = idx },
                                    label = { Text(mLabel, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF10B981),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        if (aiNotesMode == 0) {
                            // Shared AI Link
                            OutlinedTextField(
                                value = aiShareUrl,
                                onValueChange = { aiShareUrl = it },
                                label = { Text("ChatGPT / Claude / Gemini Shared Link", fontSize = 11.sp) },
                                placeholder = { Text("https://chatgpt.com/share/...", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else if (aiNotesMode == 1) {
                            // Direct Text
                            OutlinedTextField(
                                value = notesTitle,
                                onValueChange = { notesTitle = it },
                                label = { Text("Subject / Document Title", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = notesContent,
                                onValueChange = { notesContent = it },
                                label = { Text("Study Notes / Lecture Content (Markdown supported)", fontSize = 11.sp) },
                                minLines = 5,
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            // Structured Builder (Subject, Subtitle, Student Name, Watermark, Border & Dynamic Questions)
                            OutlinedTextField(
                                value = notesTitle,
                                onValueChange = { notesTitle = it },
                                label = { Text("Course / Subject Name", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Operating Systems / Data Structures", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = notesSubtitle,
                                onValueChange = { notesSubtitle = it },
                                label = { Text("Unit / Topic / Examination Code", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Unit 3: Memory Management & Paging", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = notesStudentName,
                                onValueChange = { notesStudentName = it },
                                label = { Text("Student / Professor / Faculty Name (Optional)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Prof. R. Sharma / Sneha Verma", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Border Selector
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Page Border Style:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        Pair("shadow", "MS Word Shadow"),
                                        Pair("clean", "Clean Line"),
                                        Pair("none", "None")
                                    ).forEach { (key, lbl) ->
                                        FilterChip(
                                            selected = borderStyleChoice == key,
                                            onClick = { borderStyleChoice = key },
                                            label = { Text(lbl, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF10B981),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            // Partner Header Scope Selector (First Page vs All Pages)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("College Study Partner Header:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        Pair("first-page", "First Page Only 🎓"),
                                        Pair("all-pages", "All Pages 🎓")
                                    ).forEach { (key, lbl) ->
                                        FilterChip(
                                            selected = partnerBadgeScope == key,
                                            onClick = { partnerBadgeScope = key },
                                            label = { Text(lbl, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF2563EB),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0))

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
                                                .background(Color(0xFF2563EB))
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
                                                            "Question ${structuredQuestions.size + 1}",
                                                            "Write the answer details here..."
                                                        )
                                                    )
                                                    currentQuestionIndex = structuredQuestions.size - 1
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("+ Add", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
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
                                        placeholder = { Text("e.g. What is Round Robin Scheduling?", fontSize = 11.sp, color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(6.dp),
                                        minLines = 2,
                                        maxLines = 3
                                    )

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
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = currentItem.answer + " **bold text**")
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
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = currentItem.answer + " *italic text*")
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
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = currentItem.answer + "\n• Point: ")
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
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = currentItem.answer + "\n1. Item: ")
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
                                                    structuredQuestions[safeIdx] = currentItem.copy(answer = currentItem.answer + "\n### Subsection\n")
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("### H3", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                    }

                                    // Answer / Explanation Input
                                    OutlinedTextField(
                                        value = currentItem.answer,
                                        onValueChange = { newA ->
                                            structuredQuestions[safeIdx] = currentItem.copy(answer = newA)
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

                        Button(
                            onClick = {
                                isNotesGenerating = true
                                coroutineScope.launch {
                                    if (aiNotesMode == 0 && aiShareUrl.isNotBlank()) {
                                        val extractRes = client.extractAiConversation(aiShareUrl)
                                        extractRes.onSuccess { (extTitle, extContent) ->
                                            notesTitle = extTitle
                                            notesContent = extContent
                                        }
                                    }

                                    val cleanFileName = notesTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".pdf"
                                    val result = if (aiNotesMode == 2) {
                                        client.createAcademicNotesPdf(
                                            context = context,
                                            title = notesTitle,
                                            content = "",
                                            subtitle = notesSubtitle.ifBlank { null },
                                            studentName = notesStudentName.ifBlank { null },
                                            watermarkText = null,
                                            borderStyle = borderStyleChoice,
                                            includePartnerBadge = true,
                                            footerText = null,
                                            questions = structuredQuestions.toList(),
                                            outputFileName = cleanFileName
                                        )
                                    } else {
                                        client.createAcademicNotesPdf(
                                            context = context,
                                            title = notesTitle,
                                            content = notesContent,
                                            subtitle = null,
                                            studentName = null,
                                            watermarkText = null,
                                            borderStyle = "shadow",
                                            includePartnerBadge = true,
                                            footerText = null,
                                            questions = emptyList(),
                                            outputFileName = cleanFileName
                                        )
                                    }
                                    isNotesGenerating = false
                                    result.onSuccess { file ->
                                        notesPdfFile = file
                                        Toast.makeText(context, "Academic PDF created in Downloads/QwikSave!", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isNotesGenerating
                        ) {
                            if (isNotesGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generating Academic PDF...", fontSize = 12.sp)
                            } else {
                                Text("Generate Academic Study PDF 📚", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Generated PDF Success Box with Open & Share PDF buttons
                        notesPdfFile?.let { file ->
                            PdfResultCard(
                                file = file,
                                onOpen = { openFileWithIntent(context, file, "application/pdf") },
                                onShare = { client.shareFile(context, file, "application/pdf") }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Image to PDF Section WITH PREVIEW, ROTATE & REMOVE
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
                            PdfSuiteLogo(size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("Image to PDF Converter", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Select pictures, rotate, re-order & convert to A4 PDF", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedImageUris.isEmpty()) "Select Images from Gallery" else "Selected ${selectedImageUris.size} Images (Tap to add more)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Interactive Preview Grid with Rotate & Remove
                        if (selectedImageUris.isNotEmpty()) {
                            Text(
                                text = "Photos Preview (${selectedImageUris.size}): Tap rotate or ✕ to adjust",
                                fontSize = 11.sp,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(selectedImageUris) { index, uri ->
                                    val rotation = imageRotations[index] ?: 0
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    ) {
                                        val bitmap = remember(uri) {
                                            try {
                                                val input = context.contentResolver.openInputStream(uri)
                                                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                                                val b = BitmapFactory.decodeStream(input, null, options)
                                                input?.close()
                                                b
                                            } catch (_: Exception) { null }
                                        }

                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Photo ${index + 1}",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .rotate(rotation.toFloat()),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                                        }

                                        // Rotate Button (Bottom Left)
                                        IconButton(
                                            onClick = {
                                                val current = imageRotations[index] ?: 0
                                                imageRotations = imageRotations + (index to (current + 90) % 360)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .size(26.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topEnd = 6.dp))
                                        ) {
                                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }

                                        // Delete Button (Top Right)
                                        IconButton(
                                            onClick = {
                                                val mutable = selectedImageUris.toMutableList()
                                                mutable.removeAt(index)
                                                selectedImageUris = mutable
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(26.dp)
                                                .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(bottomStart = 6.dp))
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }

                                        // Page number label (Bottom Right)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("#${index + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = pdfOutputName,
                            onValueChange = { pdfOutputName = it },
                            label = { Text("Output PDF File Name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                if (selectedImageUris.isEmpty()) {
                                    Toast.makeText(context, "Please select at least 1 image first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isPdfConverting = true
                                coroutineScope.launch {
                                    val cleanName = pdfOutputName.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".pdf"
                                    val result = client.convertImagesToPdf(
                                        context = context,
                                        imageUris = selectedImageUris,
                                        rotations = imageRotations,
                                        outputFileName = cleanName
                                    )
                                    isPdfConverting = false
                                    result.onSuccess { file ->
                                        pdfGeneratedFile = file
                                        Toast.makeText(context, "A4 PDF Saved to Downloads/QwikSave!", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isPdfConverting && selectedImageUris.isNotEmpty()
                        ) {
                            if (isPdfConverting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generating A4 PDF...", fontSize = 12.sp)
                            } else {
                                Text("Generate A4 PDF (${selectedImageUris.size} Photos) 📄", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        pdfGeneratedFile?.let { file ->
                            PdfResultCard(
                                file = file,
                                onOpen = { openFileWithIntent(context, file, "application/pdf") },
                                onShare = { client.shareFile(context, file, "application/pdf") }
                            )
                        }
                    }
                }
            }

            2 -> {
                // Merge PDF Section
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
            }

            3 -> {
                // Compress PDF Section
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
            }

            4 -> {
                // PDF to Image Section
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
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF0891B2), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("PDF to Image Converter", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Extract each page of a PDF document into high-resolution PNG pictures", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        OutlinedButton(
                            onClick = { pdfToImagePickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pdfToImageUri == null) "Select PDF File" else "Selected: $pdfToImageName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (pdfToImageUri != null) {
                            Button(
                                onClick = {
                                    isExtractingImages = true
                                    coroutineScope.launch {
                                        val base = pdfToImageName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                        val res = client.convertPdfToImages(context, pdfToImageUri!!, base)
                                        isExtractingImages = false
                                        res.onSuccess { files ->
                                            extractedImagesList = files
                                            Toast.makeText(context, "Extracted ${files.size} pages to Pictures/QwikSave!", Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isExtractingImages
                            ) {
                                if (isExtractingImages) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Extracting Pages...", fontSize = 12.sp)
                                } else {
                                    Text("Extract All Pages as Pictures 📸", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (extractedImagesList.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFECFEFF), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF06B6D4), RoundedCornerShape(6.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🎉 Pages Extracted Successfully!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF155E75))
                                    Text("Saved ${extractedImagesList.size} high-resolution PNGs to: Pictures/QwikSave/", fontSize = 11.sp, color = Color(0xFF0E7490))
                                }
                            }
                        }
                    }
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

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }.onFailure { statusMessage = "Error: ${it.message}" }
                                    }
                                    1 -> {
                                        val res = client.extractFacebook(trimmedUrl)
                                        isLoading = false
                                        res.onSuccess {
                                            fbFormats = it
                                            statusMessage = "Found ${it.size} stream formats"
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }.onFailure { statusMessage = "Error: ${it.message}" }
                                    }
                                    2 -> {
                                        val res = client.extractYouTube(trimmedUrl)
                                        isLoading = false
                                        res.onSuccess {
                                            ytResult = it
                                            statusMessage = if (it.items.size > 1) "Ready: Carousel (${it.items.size} items) 📸" else if (!it.isVideo) "Ready: High-Res Photo 🖼️" else "Ready: 1080p HD (${it.fileSizeMb ?: "Full HD"})"
                                            scrollState.animateScrollTo(scrollState.maxValue)
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
                            Column {
                                Text(format.quality, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                Text(format.fileSizeMb ?: "MP4 Video", fontSize = 11.sp, color = Color(0xFF047857))
                            }

                            Button(
                                onClick = {
                                    val filename = "QwikSave_Facebook_${System.currentTimeMillis()}.mp4"
                                    coroutineScope.launch {
                                        Toast.makeText(context, "Downloading Facebook video to Gallery... 🎬", Toast.LENGTH_SHORT).show()
                                        val result = client.downloadMediaDirectly(context, format.streamUrl, filename)
                                        result.onSuccess {
                                            Toast.makeText(context, "Saved to Gallery / Movies! ✅", Toast.LENGTH_LONG).show()
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
                                Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            Column {
                                Text(itm.quality, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                Text("High-Speed Direct MP4 Stream", fontSize = 11.sp, color = Color(0xFF047857))
                            }

                            Button(
                                onClick = {
                                    val filename = "QwikSave_YouTube_${ytRes.id}_${idx + 1}_${System.currentTimeMillis()}.mp4"
                                    coroutineScope.launch {
                                        Toast.makeText(context, "Downloading YouTube video to Gallery... 🎬", Toast.LENGTH_SHORT).show()
                                        val result = client.downloadMediaDirectly(context, itm.url, filename)
                                        result.onSuccess {
                                            Toast.makeText(context, "Saved to Gallery / Movies! ✅", Toast.LENGTH_LONG).show()
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
                                Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                            Button(
                                onClick = {
                                    val ext = if (res.isVideo) "mp4" else "jpg"
                                    val filename = "QwikSave_${res.platform}_${res.id}_${System.currentTimeMillis()}.$ext"
                                    coroutineScope.launch {
                                        val msg = if (res.isVideo) "Downloading video to Gallery... 🎬" else "Downloading photo to Gallery... 🖼️"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        val result = client.downloadMediaDirectly(context, res.streamUrl, filename)
                                        result.onSuccess {
                                            val dest = if (res.isVideo) "Gallery / Movies! ✅" else "Gallery / Pictures! ✅"
                                            Toast.makeText(context, "Saved to $dest", Toast.LENGTH_LONG).show()
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
                                Text(if (res.isVideo) "Download Video 📥" else "Download Photo 🖼️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                        1.5.dp,
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1DB954).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF1DB954), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Qwik Music Hub",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1DB954))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "90s & TRENDING",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Spotify vibe • Travel songs, Liked playlist & Offline download 🚗",
                                fontSize = 10.5.sp,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Green Pill Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1DB954))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Open",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
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

    // In-place Video to MP3 Extractor states
    var audioInputUrl by remember { mutableStateOf("") }
    var isAudioExtracting by remember { mutableStateOf(false) }
    var audioExtractedResult by remember { mutableStateOf<ExtractedMedia?>(null) }
    var audioStatusMessage by remember { mutableStateOf<String?>(null) }

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
        "+61 (Australia)",
        "+49 (Germany)",
        "+33 (France)",
        "+81 (Japan)"
    )
    var phoneNumberInput by remember { mutableStateOf("") }

    // Formatted payload computation
    val computedQrPayload = remember(selectedQrType, urlInput, plainTextInput, wifiSsid, wifiPassword, upiVpa, upiName, selectedCountryCode, phoneNumberInput) {
        when (selectedQrType) {
            0 -> urlInput.trim().ifBlank { "https://www.ai-chat2pdf.live" }
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
        // QR Code Studio Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QrCodeLogo(size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("QR Code Generator & Downloader", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Create QR codes for Websites, WiFi, UPI, Phone, and Plain Text", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                // Type Switcher Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(qrTypes) { idx, label ->
                        FilterChip(
                            selected = selectedQrType == idx,
                            onClick = { selectedQrType = idx },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF059669),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

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
                                    onDismissRequest = { countryDropdownOpen = false }
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
                                onValueChange = { phoneNumberInput = it },
                                label = { Text("Phone Number", fontSize = 11.sp) },
                                placeholder = { Text("e.g. 9876543210", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // QR Code Render Canvas
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Generated QR Code",
                                modifier = Modifier
                                    .size(190.dp)
                                    .padding(8.dp)
                            )
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { client.shareQrBitmap(context, qrBitmap) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share QR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Audio Extractor Card - LIGHT SKY BLUISH WHITISH THEME WITH IN-PLACE URL EXTRACTION & DOWNLOAD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)), // Very light sky bluish whitish
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)), // Sky-bluish boundary
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
                    placeholder = { Text("Paste video link (YouTube, FB, Insta, MP4 URL)...", fontSize = 12.sp, color = Color(0xFF64748B)) },
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

package com.qwiksave.app.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.qwiksave.app.sdk.ExtractedMedia
import com.qwiksave.app.sdk.QwikSaveClient
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val client = remember { QwikSaveClient() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Instagram", "Facebook", "Image to PDF", "AI Notes")

    var inputUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var extractedResult by remember { mutableStateOf<ExtractedMedia?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF263448), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Q",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Qwik Save",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFDC2626).copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = Color(0xFFF87171),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "SAVE IN ONE CLICK",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "4.7 ⭐ (2,000+)",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Feature Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    edgePadding = 0.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                extractedResult = null
                                statusMessage = null
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val currentThemeColor = when (selectedTab) {
                        0 -> Color(0xFFC13584) // Instagram Dark Pink
                        1 -> Color(0xFF1877F2) // Facebook Blue
                        2 -> Color(0xFF059669) // PDF Emerald
                        else -> Color(0xFF0F172A) // AI Notes Navy
                    }

                    Text(
                        text = when (selectedTab) {
                            0 -> "Download Instagram Reels & Videos in HD"
                            1 -> "Save Facebook Videos in 1080p Full HD"
                            2 -> "Convert Photos to Clean PDF Document"
                            else -> "Format AI Study Chats to Printable PDF"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = when (selectedTab) {
                            0 -> "Paste public Instagram reel, post, or story link."
                            1 -> "Paste public Facebook reel or watch video link."
                            2 -> "Select multiple images from phone storage to combine."
                            else -> "Paste ChatGPT, Claude, or Gemini shared notes link."
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    // Input Box
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Paste Instagram Link..."
                                    1 -> "Paste Facebook Link..."
                                    2 -> "Select output file name..."
                                    else -> "Paste ChatGPT or Claude Link..."
                                },
                                fontSize = 12.sp
                            )
                        },
                        trailingIcon = {
                            if (inputUrl.isNotEmpty()) {
                                IconButton(onClick = { inputUrl = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )

                    // Action Buttons (Paste & Download)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrEmpty()) {
                                    inputUrl = clip.trim()
                                    Toast.makeText(context, "Pasted from Clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Paste", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (inputUrl.isBlank()) {
                                    Toast.makeText(context, "Please enter or paste a valid link", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isLoading = true
                                statusMessage = "Extracting media streams..."
                                coroutineScope.launch {
                                    val result = when (selectedTab) {
                                        0 -> client.extractInstagram(inputUrl)
                                        1 -> client.extractFacebook(inputUrl).map { it.firstOrNull() ?: throw Exception("No streams found") }
                                        else -> client.extractInstagram(inputUrl)
                                    }

                                    isLoading = false
                                    result.onSuccess { media ->
                                        extractedResult = media
                                        statusMessage = "Ready: ${media.quality} (${media.fileSizeMb ?: "HD"})"
                                    }.onFailure { err ->
                                        statusMessage = "Error: ${err.message}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(2f),
                            colors = ButtonDefaults.buttonColors(containerColor = currentThemeColor),
                            shape = RoundedCornerShape(2.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Processing...", fontSize = 12.sp)
                            } else {
                                Text("Download in 1 Click", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Status Notification
                    statusMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(2.dp))
                                .padding(8.dp)
                        ) {
                            Text(text = msg, fontSize = 11.sp, color = Color(0xFF334155))
                        }
                    }

                    // Download Result Card
                    extractedResult?.let { media ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = media.quality,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = media.fileSizeMb ?: "Full HD",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        client.enqueueDownload(
                                            context = context,
                                            downloadUrl = media.streamUrl,
                                            filename = "QwikSave_${media.platform}_${media.id}.mp4",
                                            title = media.title
                                        )
                                        Toast.makeText(context, "Download started! Check notifications.", Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = currentThemeColor),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Download")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Save Video (${media.fileSizeMb ?: "HD MP4"})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Trust & Privacy Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒 100% Client-Side Privacy • No Account Required • Save in 1 Click",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

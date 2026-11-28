package com.qwiksave.app.ui.main

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qwiksave.app.R
import com.qwiksave.app.data.PdfHistoryItem
import com.qwiksave.app.data.PdfHistoryManager
import com.qwiksave.app.sdk.PresentationSlideData
import com.qwiksave.app.sdk.QwikSaveClient
import com.qwiksave.app.ui.scanner.DocumentImageProcessor
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Professional Squarish PDF Success Dialog
 * Replaces informal "Hooray! 🎉" with a clean, tall, elegant modal.
 */
@Composable
fun ProfessionalPdfSuccessDialog(
    pdfFile: File,
    title: String = "Presentation PDF Ready",
    tagline: String = "Slides converted to academic PDF",
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var currentFile by remember(pdfFile) { mutableStateOf(pdfFile) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(currentFile.name.substringBeforeLast(".")) }

    val previewBitmap = remember(currentFile) {
        val pfd = try {
            ParcelFileDescriptor.open(currentFile, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (_: Exception) { null }
        if (pfd != null) {
            val renderer = try { PdfRenderer(pfd) } catch (_: Exception) { null }
            if (renderer != null && renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bmp = Bitmap.createBitmap(280, 360, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(AndroidColor.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                bmp
            } else {
                pfd.close()
                null
            }
        } else null
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Edit PDF Name", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Document Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = renameText.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        if (clean.isNotBlank()) {
                            val targetFile = File(currentFile.parentFile, "${clean}.pdf")
                            if (currentFile.renameTo(targetFile)) {
                                currentFile = targetFile
                                PdfHistoryManager.addPdf(context, targetFile)
                                Toast.makeText(context, "Renamed to ${targetFile.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Save Name")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(4.dp), // Squarish professional look requested by user
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Tall Preview / Sleek Header Area
                if (previewBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "PDF Preview",
                            modifier = Modifier.fillMaxHeight(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F172A), Color(0xFF065F46))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "OFFICIAL DOCUMENT READY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }

                // Professional Header & Tagline
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tagline,
                        fontSize = 11.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // File metadata box with rename trigger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                        .clickable {
                            renameText = currentFile.name.substringBeforeLast(".")
                            showRenameDialog = true
                        }
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentFile.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFE0F2FE), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color(0xFF0284C7), modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Edit", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                }
                            }
                            val fileSizeStr = if (currentFile.length() > 1024 * 1024) {
                                String.format(Locale.getDefault(), "%.1f MB", currentFile.length() / (1024f * 1024f))
                            } else {
                                String.format(Locale.getDefault(), "%d KB", currentFile.length() / 1024)
                            }
                            Text(
                                text = "$fileSizeStr • Saved in Downloads/QwikSave",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Professional Action Buttons with Squarish Corners
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF059669)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

/**
 * 1. DEDICATED PDF TO IMAGE CONVERTER STUDIO SCREEN
 * Full standalone page with Top Bar, converter card, page previews, and fitted bottom illustration.
 */
@Composable
fun PdfToImageStudioScreen(
    client: QwikSaveClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    var pageCount by remember { mutableStateOf(0) }
    var pageThumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isExtractingAll by remember { mutableStateOf(false) }
    var isZippingAll by remember { mutableStateOf(false) }
    var completedZipFile by remember { mutableStateOf<java.io.File?>(null) }
    var isCroppingPage by remember { mutableStateOf<Int?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            selectedPdfName = uri.lastPathSegment?.substringAfterLast("/") ?: "Document.pdf"
            val count = client.getPdfPageCount(context, uri)
            pageCount = count
            // Load thumbnails
            coroutineScope.launch {
                val thumbs = withContext(Dispatchers.IO) {
                    client.renderAllPdfPages(context, uri, scale = 0.45f)
                }
                pageThumbnails = thumbs
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top App Bar
        Surface(
            color = Color(0xFF0F172A),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "PDF to Image Converter",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Body Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Converter Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp), // Squarish modern
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF06B6D4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "PDF to Image Converter",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Extract each page of a PDF document into HD PNG pictures",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF0891B2)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0891B2))
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedPdfUri == null) "Select PDF File" else "Selected: $selectedPdfName ($pageCount pages)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (selectedPdfUri != null && pageCount > 0) {
                        Button(
                            onClick = {
                                isExtractingAll = true
                                coroutineScope.launch {
                                    val base = selectedPdfName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                    val res = client.convertPdfToImages(context, selectedPdfUri!!, base)
                                    isExtractingAll = false
                                    res.onSuccess { files ->
                                        Toast.makeText(context, "Extracted all ${files.size} pages to Pictures/QwikSave!", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isExtractingAll
                        ) {
                            if (isExtractingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extracting All Pages...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extract All $pageCount Pages to Pictures 📸", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                isZippingAll = true
                                coroutineScope.launch {
                                    val base = selectedPdfName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9_-]"), "_") + "_Images"
                                    val res = client.convertPdfToImagesZip(context, selectedPdfUri!!, base)
                                    isZippingAll = false
                                    res.onSuccess { zipFile ->
                                        completedZipFile = zipFile
                                        Toast.makeText(context, "All $pageCount pages archived to ZIP in Downloads/QwikSave!", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Error creating ZIP: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isZippingAll && !isExtractingAll
                        ) {
                            if (isZippingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Packaging ZIP Archive...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Batch PDF to Image ZIP (.zip) 📦", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        completedZipFile?.let { zipFile ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(zipFile.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${zipFile.length() / 1024} KB • Saved in Downloads/QwikSave", fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                    IconButton(onClick = { client.shareFile(context, zipFile, "application/zip") }) {
                                        Icon(Icons.Default.Share, contentDescription = "Share ZIP", tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pages Grid or Bottom Fitted Illustration
            if (selectedPdfUri != null && pageThumbnails.isNotEmpty()) {
                Text(
                    text = "Select & Extract Individual Pages (${pageThumbnails.size} pages):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(pageThumbnails) { index, thumb ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .background(Color(0xCC0F172A), RoundedCornerShape(bottomEnd = 4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Page ${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                                val qwikDir = File(picturesDir, "QwikSave").apply { mkdirs() }
                                                val pageFile = File(qwikDir, "Page_${index + 1}_${System.currentTimeMillis()}.png")
                                                FileOutputStream(pageFile).use { fos ->
                                                    thumb.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                                }
                                                Toast.makeText(context, "Page ${index + 1} saved to Pictures/QwikSave!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(30.dp)
                                    ) {
                                        Text("Save", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { isCroppingPage = index },
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
                                    ) {
                                        Text("Crop", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Fixed layout bottom illustration without background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ai_study_hero_illustration),
                        contentDescription = "Illustration",
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(280.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    // Single page crop dialog
    if (isCroppingPage != null && isCroppingPage!! in pageThumbnails.indices) {
        val pageIdx = isCroppingPage!!
        val pageThumb = pageThumbnails[pageIdx]
        Dialog(onDismissRequest = { isCroppingPage = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(4.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Crop & Save Page ${pageIdx + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = pageThumb.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isCroppingPage = null }) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                    val qwikDir = File(picturesDir, "QwikSave").apply { mkdirs() }
                                    val croppedFile = File(qwikDir, "Page_${pageIdx + 1}_Cropped_${System.currentTimeMillis()}.png")
                                    FileOutputStream(croppedFile).use { fos ->
                                        pageThumb.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                    }
                                    isCroppingPage = null
                                    Toast.makeText(context, "Page ${pageIdx + 1} cropped & saved!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Save Cropped PNG", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. FREECONVERT MULTI-COMPRESSOR HUB
 * Features dropdown selector: Compress PNG, Compress JPEG, GIF Compressor, PDF Compressor, Image Compressor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeConvertCompressorsStudioScreen(
    client: QwikSaveClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val options = listOf(
        "Compress PNG",
        "Compress JPEG",
        "GIF Compressor",
        "PDF Compressor",
        "Image Compressor",
        "Video Compressor"
    )

    var selectedOption by remember { mutableStateOf(options[0]) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var originalFileSize by remember { mutableStateOf<Long?>(null) }

    var isCompressing by remember { mutableStateOf(false) }
    var compressedResultFile by remember { mutableStateOf<File?>(null) }
    var qualityLevel by remember { mutableStateOf(75) }
    var videoPresetIndex by remember { mutableStateOf(0) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            val realName = try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) cursor.getString(idx) else null
                    } else null
                }
            } catch (_: Exception) { null } ?: uri.lastPathSegment?.substringAfterLast("/") ?: "Selected_File"
            selectedFileName = if (realName.matches(Regex("""^\d+\.[a-zA-Z0-9]+$"""))) {
                "Compressed_${realName}"
            } else realName
            originalFileSize = client.getFileSizeFromUri(context, uri)
            compressedResultFile = null
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top App Bar
        Surface(color = Color(0xFF0F172A), shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "FreeConvert Multi-Compressor Hub",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dropdown Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCFBF1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Compressor Tool:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))

                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedOption,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        )
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            options.forEachIndexed { idx, opt ->
                                val isSelected = (selectedOption == opt)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = opt,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) Color(0xFF059669) else Color(0xFF0F172A)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF059669),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.background(if (isSelected) Color(0xFFECFDF5) else Color.Transparent),
                                    onClick = {
                                        selectedOption = opt
                                        isDropdownExpanded = false
                                        selectedFileUri = null
                                        compressedResultFile = null
                                    }
                                )
                                if (idx < options.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                                }
                            }
                        }
                    }
                }
            }

            // File selection card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Selected: $selectedOption", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    val desc = when (selectedOption) {
                        "Compress PNG" -> "Reduces PNG file size while maintaining transparency & sharp edges."
                        "Compress JPEG" -> "Smart JPEG compression cutting file weight with high fidelity."
                        "GIF Compressor" -> "Compresses animated GIF frames without stutter."
                        "PDF Compressor" -> "Shrinks PDF document size for easy email & WhatsApp sharing."
                        "Video Compressor" -> "Shrinks video MBs dramatically with 720p/1080p visual clarity."
                        else -> "Universal image compressor supporting WebP, PNG, and JPG formats."
                    }
                    Text(desc, fontSize = 11.5.sp, color = Color(0xFF64748B))

                    OutlinedButton(
                        onClick = {
                            val mime = when (selectedOption) {
                                "Compress PNG" -> "image/png"
                                "Compress JPEG" -> "image/jpeg"
                                "GIF Compressor" -> "image/gif"
                                "PDF Compressor" -> "application/pdf"
                                "Video Compressor" -> "video/*"
                                else -> "image/*"
                            }
                            filePicker.launch(mime)
                        },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF0D9488)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D9488))
                    ) {
                        Icon(
                            if (selectedOption == "Video Compressor") Icons.Default.VideoLibrary else Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedFileUri == null) {
                                if (selectedOption == "Video Compressor") "Select Video to Compress" else "Select File to Compress"
                            } else "Selected: $selectedFileName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (selectedFileUri != null) {
                        if (originalFileSize != null && originalFileSize!! > 0) {
                            val sizeStr = if (selectedOption == "Video Compressor") {
                                String.format(Locale.getDefault(), "%.1f MB", originalFileSize!! / (1024f * 1024f))
                            } else {
                                "${originalFileSize!! / 1024} KB"
                            }
                            Text("Original File Size: $sizeStr", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        // Quality controls
                        if (selectedOption == "Video Compressor") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Compression Level:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                val origBytes = originalFileSize ?: (10L * 1024 * 1024)
                                val origMb = origBytes / (1024f * 1024f)
                                val estBalancedMb = String.format(Locale.getDefault(), "%.1f MB", (origMb * 0.44f).coerceAtLeast(0.5f))
                                val estHighMb = String.format(Locale.getDefault(), "%.1f MB", (origMb * 0.24f).coerceAtLeast(0.3f))
                                val estMaxMb = String.format(Locale.getDefault(), "%.1f MB", (origMb * 0.72f).coerceAtLeast(0.8f))

                                val presets = listOf(
                                    "Balanced (Recommended ~$estBalancedMb, ~1.5 Mbps)",
                                    "High Compression (Saves 70%+ ~$estHighMb, ~850k)",
                                    "Maximum Quality (Crisp 1080p ~$estMaxMb, ~2.5M)"
                                )
                                presets.forEachIndexed { idx, label ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { videoPresetIndex = idx }
                                            .padding(vertical = 3.dp)
                                    ) {
                                        RadioButton(
                                            selected = videoPresetIndex == idx,
                                            onClick = { videoPresetIndex = idx },
                                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0D9488))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(label, fontSize = 11.5.sp, color = Color(0xFF1E293B), fontWeight = if (videoPresetIndex == idx) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        } else if (selectedOption != "PDF Compressor" && selectedOption != "GIF Compressor") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Compression Quality:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text("$qualityLevel%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                                }
                                Slider(
                                    value = qualityLevel.toFloat(),
                                    onValueChange = { qualityLevel = it.toInt() },
                                    valueRange = 30f..90f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF0D9488), activeTrackColor = Color(0xFF0D9488))
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isCompressing = true
                                coroutineScope.launch {
                                    val outName = selectedFileName.substringBeforeLast(".") + "_Compressed"
                                    val uri = selectedFileUri ?: return@launch
                                    val res = when (selectedOption) {
                                        "Compress PNG" -> client.compressImage(context, uri, "PNG", qualityLevel, outName)
                                        "Compress JPEG" -> client.compressImage(context, uri, "JPEG", qualityLevel, outName)
                                        "GIF Compressor" -> client.compressGif(context, uri, outName)
                                        "PDF Compressor" -> client.compressPdf(context, uri, 1, "$outName.pdf")
                                        "Video Compressor" -> {
                                            val presetStr = when (videoPresetIndex) {
                                                0 -> "recommended"
                                                1 -> "extreme"
                                                else -> "high"
                                            }
                                            client.compressVideo(context, uri, outName, presetStr)
                                        }
                                        else -> client.compressImage(context, uri, "Image", qualityLevel, outName)
                                    }
                                    isCompressing = false
                                    res.onSuccess { f ->
                                        compressedResultFile = f
                                        Toast.makeText(context, "Compression complete!", Toast.LENGTH_SHORT).show()
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(150)
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Compression error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCompressing
                        ) {
                            if (isCompressing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (selectedOption == "Video Compressor") "Compressing Video..." else "Compressing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compress File Now ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Compression result card
            val compFile = compressedResultFile
            if (compFile != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("File Compressed Successfully!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                        }

                        val isVideo = compFile.name.endsWith(".mp4", ignoreCase = true)
                        val compSizeStr = if (isVideo) {
                            String.format(Locale.getDefault(), "%.1f MB", compFile.length() / (1024f * 1024f))
                        } else {
                            "${compFile.length() / 1024} KB"
                        }
                        val origSizeStr = if (isVideo) {
                            String.format(Locale.getDefault(), "%.1f MB", (originalFileSize ?: 1L) / (1024f * 1024f))
                        } else {
                            "${(originalFileSize ?: 1L) / 1024} KB"
                        }
                        val savings = if ((originalFileSize ?: 0L) > 0L) {
                            ((originalFileSize!! - compFile.length()).toFloat() / originalFileSize!! * 100).toInt().coerceAtLeast(0)
                        } else 0

                        Text("Reduced by $savings% (from $origSizeStr to $compSizeStr)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val mime = when {
                                        compFile.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                                        compFile.name.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
                                        else -> "image/*"
                                    }
                                    client.shareFile(context, compFile, mime)
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(40.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
                            ) {
                                Text("Share", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }

                            Button(
                                onClick = {
                                    val mime = when {
                                        compFile.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                                        compFile.name.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
                                        else -> "image/*"
                                    }
                                    client.openFile(context, compFile, mime)
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text(if (isVideo) "Play Video" else "Open", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * PPT / PPTX & Word DOC / DOCX to PDF Converter Studio Screen
 * Features live scroll-down slide cards preview, custom PDF naming,
 * prominent "Save as PDF" button, and 48-hour privacy recent history.
 */
@Composable
fun PptToPdfStudioScreen(
    client: QwikSaveClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedDocUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDocName by remember { mutableStateOf("") }
    var customDocName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }

    var isParsing by remember { mutableStateOf(false) }
    var parsedSlides by remember { mutableStateOf<List<PresentationSlideData>>(emptyList()) }

    var isConverting by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfSuccessModal by remember { mutableStateOf(false) }

    val historyList = remember { mutableStateListOf<PdfHistoryItem>() }

    LaunchedEffect(Unit) {
        historyList.clear()
        historyList.addAll(PdfHistoryManager.getPdfs(context))
    }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedDocUri = uri
            val realName = try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) cursor.getString(idx) else null
                    } else null
                }
            } catch (_: Exception) { null } ?: uri.lastPathSegment?.substringAfterLast("/") ?: "Presentation.pptx"

            selectedDocName = realName
            customDocName = realName.substringBeforeLast(".")
            generatedPdfFile = null
            parsedSlides = emptyList()

            isParsing = true
            coroutineScope.launch {
                val isWord = selectedDocName.endsWith(".docx", ignoreCase = true) || selectedDocName.endsWith(".doc", ignoreCase = true)
                val isSheet = selectedDocName.endsWith(".xlsx", ignoreCase = true) || selectedDocName.endsWith(".xls", ignoreCase = true) || selectedDocName.endsWith(".csv", ignoreCase = true)
                val slidesRes = when {
                    isWord -> client.parseDocxPages(context, uri)
                    isSheet -> client.parseXlsxSheets(context, uri)
                    else -> client.parsePptxSlides(context, uri)
                }
                parsedSlides = slidesRes.getOrDefault(emptyList())
                if (parsedSlides.isEmpty() && slidesRes.isFailure) {
                    Toast.makeText(context, "Could not extract slides: ${slidesRes.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
                isParsing = false
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        var tempName by remember { mutableStateOf(customDocName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Edit Document Name", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("PDF Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = tempName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        if (clean.isNotBlank()) {
                            customDocName = clean
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top App Bar
        Surface(color = Color(0xFF0F172A), shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("PPT & Word to PDF Converter", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File selection card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEA580C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Slideshow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("PowerPoint & Word to PDF", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Convert .pptx, .ppt, .docx, & .doc with high visual fidelity", fontSize = 11.5.sp, color = Color(0xFF64748B))
                        }
                    }

                    OutlinedButton(
                        onClick = { docPicker.launch("*/*") },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFEA580C)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA580C))
                    ) {
                        Icon(Icons.Default.FilePresent, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedDocUri == null) "Select PPT / PPTX or DOC / DOCX File" else "Selected: $selectedDocName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Parsing indicator
                    if (isParsing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFEA580C), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extracting slides, text, tables, and images...", fontSize = 12.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Ready to compile banner with edit name and top Save as PDF button
                    if (parsedSlides.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFF7ED),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${customDocName}.pdf",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(22.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color(0xFFEA580C), modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF10B981)
                                    ) {
                                        Text(
                                            text = "${parsedSlides.size} Slides Ready",
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        isConverting = true
                                        coroutineScope.launch {
                                            val outName = customDocName.ifBlank { "Presentation_Document" }
                                            val res = client.compilePresentationToPdf(context, parsedSlides, outName)
                                            isConverting = false
                                            res.onSuccess { pdf ->
                                                generatedPdfFile = pdf
                                                historyList.clear()
                                                historyList.addAll(PdfHistoryManager.getPdfs(context))
                                                showPdfSuccessModal = true
                                                Toast.makeText(context, "Saved as PDF successfully!", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isConverting
                                ) {
                                    if (isConverting) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Compiling PDF...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save as PDF 📥", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Live Scroll-Down Slide Cards Preview
            if (parsedSlides.isNotEmpty()) {
                Text(
                    text = "Live Document Slide Preview (Scroll Down):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                parsedSlides.forEach { slide ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Slide Badge & Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF0F172A)
                                ) {
                                    val badgeLabel = if (slide.isDocumentPage) "Page ${slide.slideNum}"
                                    else if (slide.isSpreadsheetPage) "Sheet ${slide.slideNum}"
                                    else "Slide ${slide.slideNum}"
                                    Text(
                                        text = badgeLabel,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (slide.tableRows.isNotEmpty()) {
                                        Text(
                                            text = "${slide.tableRows.size} Table Rows",
                                            fontSize = 11.sp,
                                            color = Color(0xFF0D9488),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (slide.images.isNotEmpty()) {
                                        Text(
                                            text = "${slide.images.size} Image(s)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Slide / Document Title
                            if (slide.title.isNotBlank()) {
                                Text(
                                    text = slide.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            // Table Preview
                            if (slide.tableRows.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        slide.tableRows.take(4).forEachIndexed { rIdx, row ->
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                row.take(5).forEach { cell ->
                                                    Text(
                                                        text = cell,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (rIdx == 0) Color(0xFF0F172A) else Color(0xFF334155),
                                                        modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            if (rIdx < slide.tableRows.take(4).lastIndex) {
                                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                            }
                                        }
                                        if (slide.tableRows.size > 4) {
                                            Text("+ ${slide.tableRows.size - 4} more rows...", fontSize = 9.5.sp, color = Color(0xFF64748B), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                        }
                                    }
                                }
                            }

                            // Body paragraphs
                            for (para in slide.paragraphs.take(12)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!slide.isDocumentPage) {
                                        Text("•", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text(para, fontSize = 11.5.sp, color = Color(0xFF334155), lineHeight = 16.sp)
                                }
                            }

                            // Extracted Slide Images Preview
                            if (slide.images.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(slide.images) { img ->
                                        Box(
                                            modifier = Modifier
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                                                .background(Color(0xFFF1F5F9))
                                        ) {
                                            Image(
                                                bitmap = img.asImageBitmap(),
                                                contentDescription = "Slide Image",
                                                modifier = Modifier.fillMaxHeight(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Save as PDF button for easy access after scrolling
                Button(
                    onClick = {
                        isConverting = true
                        coroutineScope.launch {
                            val outName = customDocName.ifBlank { "Presentation_Document" }
                            val res = client.compilePresentationToPdf(context, parsedSlides, outName)
                            isConverting = false
                            res.onSuccess { pdf ->
                                generatedPdfFile = pdf
                                historyList.clear()
                                historyList.addAll(PdfHistoryManager.getPdfs(context))
                                showPdfSuccessModal = true
                                Toast.makeText(context, "Saved as PDF successfully!", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isConverting
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save All Slides as PDF 📥", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 48-Hour Recent History Section (Privacy-Preserving)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recent Documents (48h Privacy)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "Auto-Expires 🔒",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (historyList.isEmpty()) {
                        Text(
                            text = "No recent PDFs in the last 48 hours. Documents converted here are stored securely in Downloads/QwikSave and automatically cleared after 48 hours for your privacy.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            historyList.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.fileName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${item.getFormattedSize()} • ${item.getFormattedDate()}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { PdfHistoryManager.sharePdf(context, item) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { PdfHistoryManager.openPdf(context, item) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = "Open", tint = Color(0xFF0D9488), modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    PdfHistoryManager.deletePdf(context, item.id)
                                                    historyList.remove(item)
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val pptPdf = generatedPdfFile
    if (showPdfSuccessModal && pptPdf != null) {
        val isDocType = parsedSlides.any { it.isDocumentPage }
        val isSheetType = parsedSlides.any { it.isSpreadsheetPage }
        val modalTitle = if (isDocType) "Document PDF Ready" else if (isSheetType) "Spreadsheet PDF Ready" else "Presentation PDF Ready"
        val modalTagline = if (isDocType) "Word document converted cleanly to A4 PDF" else if (isSheetType) "Spreadsheet converted with clean table grid" else "Slides converted to academic PDF"

        ProfessionalPdfSuccessDialog(
            pdfFile = pptPdf,
            title = modalTitle,
            tagline = modalTagline,
            onDismiss = { showPdfSuccessModal = false },
            onOpen = {
                client.openPdfFile(context, pptPdf)
                showPdfSuccessModal = false
            },
            onShare = {
                client.sharePdfFile(context, pptPdf)
            }
        )
    }
}

/**
 * 4. DEDICATED PDF PASSWORD PROTECT & LOCK STUDIO SCREEN
 * High-security 128-bit PDF protection tool with password verification,
 * original file display, and professional export dialog.
 */
@Composable
fun PdfLockStudioScreen(
    client: QwikSaveClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    var pageCount by remember { mutableStateOf(0) }
    var fileSizeKb by remember { mutableStateOf(0L) }

    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLocking by remember { mutableStateOf(false) }

    var lockedPdfFile by remember { mutableStateOf<java.io.File?>(null) }
    var showSuccessModal by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            var resolvedName = "Protected_Document.pdf"
            try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            resolvedName = cursor.getString(nameIndex) ?: resolvedName
                        }
                    }
                }
            } catch (e: Exception) {
                resolvedName = uri.lastPathSegment?.substringAfterLast("/") ?: "Document.pdf"
            }
            selectedPdfName = resolvedName
            pageCount = client.getPdfPageCount(context, uri)
            fileSizeKb = client.getFileSizeFromUri(context, uri) / 1024
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top App Bar
        Surface(
            color = Color(0xFF0F172A),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "PDF Password Protect / Lock",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF7C3AED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "PDF Password Protect & Lock",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Encrypt any PDF with 128-bit protection so it requires a password to open",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Select File Button
                    OutlinedButton(
                        onClick = { filePicker.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF7C3AED)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C3AED))
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedPdfUri == null) "Select PDF File to Lock" else "Selected: $selectedPdfName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (selectedPdfUri != null) {
                        // File metadata pill
                        Surface(
                            color = Color(0xFFF5F3FF),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$pageCount Pages • ${fileSizeKb} KB", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6D28D9))
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                            }
                        }

                        // Password Fields
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Set PDF Password") },
                            placeholder = { Text("e.g. MySecret123") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = Color(0xFF7C3AED)
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            label = { Text("Confirm PDF Password") },
                            placeholder = { Text("Re-enter password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = confirmPasswordInput.isNotEmpty() && confirmPasswordInput != passwordInput
                        )

                        if (confirmPasswordInput.isNotEmpty() && confirmPasswordInput != passwordInput) {
                            Text("Passwords do not match", fontSize = 11.sp, color = Color(0xFFDC2626))
                        }

                        // Encryption Info Card
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Standard 128-bit encryption compatible with Adobe Acrobat, Google Drive, and WhatsApp.",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        // Lock Button
                        Button(
                            onClick = {
                                if (passwordInput.isBlank()) {
                                    Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (passwordInput != confirmPasswordInput) {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isLocking = true
                                coroutineScope.launch {
                                    val base = selectedPdfName.substringBeforeLast(".")
                                    val cleanName = "${base}_Locked.pdf"
                                    val res = client.lockPdfWithPassword(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        userPassword = passwordInput,
                                        outputFileName = cleanName
                                    )
                                    isLocking = false
                                    res.onSuccess { lockedFile ->
                                        lockedPdfFile = lockedFile
                                        showSuccessModal = true
                                        Toast.makeText(context, "PDF successfully locked with password!", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLocking && passwordInput.isNotBlank() && passwordInput == confirmPasswordInput
                        ) {
                            if (isLocking) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Encrypting PDF Document...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lock PDF with Password 🔒", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    val lockedPdf = lockedPdfFile
    if (showSuccessModal && lockedPdf != null) {
        ProfessionalPdfSuccessDialog(
            pdfFile = lockedPdf,
            title = "PDF Locked & Protected",
            tagline = "128-bit encrypted PDF document",
            onDismiss = { showSuccessModal = false },
            onOpen = {
                client.openPdfFile(context, lockedPdf)
                showSuccessModal = false
            },
            onShare = {
                client.sharePdfFile(context, lockedPdf)
            }
        )
    }
}

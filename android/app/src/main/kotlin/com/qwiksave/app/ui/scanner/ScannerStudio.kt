package com.qwiksave.app.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.qwiksave.app.data.PdfHistoryItem
import com.qwiksave.app.data.PdfHistoryManager
import com.qwiksave.app.sdk.QwikSaveClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class ScannerScreenMode {
    GRID_ALBUM,
    PAGER_EDITOR,
    CROP_STUDIO,
    SIGNATURE_STUDIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerStudio(
    client: QwikSaveClient,
    onPdfGenerated: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Document State - Clean dynamic title without hardcoded SPM
    var documentTitle by remember {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        mutableStateOf("Document_${sdf.format(Date())}")
    }
    var isEditingTitle by remember { mutableStateOf(false) }

    val pages = remember { mutableStateListOf<ScannedPage>() }
    var currentPageIndex by remember { mutableStateOf(0) }
    var screenMode by remember { mutableStateOf(ScannerScreenMode.GRID_ALBUM) }

    // Multi-selection in Grid
    var isSelectionMode by remember { mutableStateOf(false) }

    // Temporary Camera URI
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    // Dialogs & Sheets
    var showAddSourceSheet by remember { mutableStateOf(false) }
    var showSignatureSourceDialog by remember { mutableStateOf(false) }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val newPage = ScannedPage(originalUri = tempCameraUri!!)
            pages.add(newPage)
            Toast.makeText(context, "Page captured successfully!", Toast.LENGTH_SHORT).show()
            // Auto open in crop/filter editor if desired
            currentPageIndex = pages.size - 1
            screenMode = ScannerScreenMode.CROP_STUDIO
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val cacheDir = File(context.cacheDir, "camera_scans").apply { mkdirs() }
                val photoFile = File(cacheDir, "SCAN_${System.currentTimeMillis()}.jpg")
                tempCameraFile = photoFile
                val uri = FileProvider.getUriForFile(context, "com.qwiksave.app.fileprovider", photoFile)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to capture documents", Toast.LENGTH_LONG).show()
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                pages.add(ScannedPage(originalUri = uri))
            }
            Toast.makeText(context, "${uris.size} pages imported!", Toast.LENGTH_SHORT).show()
        }
    }

    // Launch camera helper with permission check
    fun launchCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val cacheDir = File(context.cacheDir, "camera_scans").apply { mkdirs() }
                val photoFile = File(cacheDir, "SCAN_${System.currentTimeMillis()}.jpg")
                tempCameraFile = photoFile
                val uri = FileProvider.getUriForFile(context, "com.qwiksave.app.fileprovider", photoFile)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Screen Router
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        when (screenMode) {
            ScannerScreenMode.GRID_ALBUM -> {
                ScannerGridAlbumView(
                    documentTitle = documentTitle,
                    pages = pages,
                    isSelectionMode = isSelectionMode,
                    isExportingPdf = isExportingPdf,
                    onTitleChange = { documentTitle = it },
                    onOpenAddSheet = { showAddSourceSheet = true },
                    onPageClick = { index ->
                        if (isSelectionMode) {
                            pages[index] = pages[index].copy(isSelected = !pages[index].isSelected)
                        } else {
                            currentPageIndex = index
                            screenMode = ScannerScreenMode.PAGER_EDITOR
                        }
                    },
                    onPageLongClick = { index ->
                        isSelectionMode = true
                        pages[index] = pages[index].copy(isSelected = true)
                    },
                    onToggleSelectAll = {
                        val allSelected = pages.all { it.isSelected }
                        for (i in pages.indices) {
                            pages[i] = pages[i].copy(isSelected = !allSelected)
                        }
                    },
                    onExitSelection = {
                        isSelectionMode = false
                        for (i in pages.indices) {
                            pages[i] = pages[i].copy(isSelected = false)
                        }
                    },
                    onDeleteSelected = {
                        pages.removeAll { it.isSelected }
                        isSelectionMode = false
                        Toast.makeText(context, "Selected pages deleted", Toast.LENGTH_SHORT).show()
                    },
                    onMovePage = { from, to ->
                        if (from in pages.indices && to in pages.indices && from != to) {
                            val item = pages.removeAt(from)
                            pages.add(to, item)
                        }
                    },
                    onExportPdf = {
                        if (pages.isEmpty()) {
                            Toast.makeText(context, "Please add at least 1 image", Toast.LENGTH_SHORT).show()
                            return@ScannerGridAlbumView
                        }
                        isExportingPdf = true
                        coroutineScope.launch {
                            val processedBitmaps = withContext(Dispatchers.Default) {
                                pages.mapNotNull { page ->
                                    DocumentImageProcessor.processFullPage(context, page)
                                }
                            }
                            val cleanName = documentTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".pdf"
                            val result = client.createPdfFromBitmaps(
                                context = context,
                                bitmaps = processedBitmaps,
                                outputFileName = cleanName
                            )
                            isExportingPdf = false
                            result.onSuccess { pdfFile ->
                                PdfHistoryManager.addPdf(context, pdfFile)
                                onPdfGenerated(pdfFile)
                            }.onFailure { err ->
                                Toast.makeText(context, "PDF export failed: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }

            ScannerScreenMode.PAGER_EDITOR -> {
                if (pages.isNotEmpty() && currentPageIndex in pages.indices) {
                    ScannerPagerEditorView(
                        pages = pages,
                        currentIndex = currentPageIndex,
                        onIndexChange = { currentPageIndex = it },
                        onBack = { screenMode = ScannerScreenMode.GRID_ALBUM },
                        onOpenCrop = { screenMode = ScannerScreenMode.CROP_STUDIO },
                        onOpenSignature = {
                            if (pages[currentPageIndex].signatures.isNotEmpty()) {
                                screenMode = ScannerScreenMode.SIGNATURE_STUDIO
                            } else {
                                showSignatureSourceDialog = true
                            }
                        },
                        onOpenWatermark = { showWatermarkDialog = true },
                        onOpenMore = { showMoreSheet = true },
                        onDeleteCurrentPage = {
                            pages.removeAt(currentPageIndex)
                            if (pages.isEmpty()) {
                                screenMode = ScannerScreenMode.GRID_ALBUM
                            } else {
                                currentPageIndex = currentPageIndex.coerceAtMost(pages.size - 1)
                            }
                        }
                    )
                } else {
                    screenMode = ScannerScreenMode.GRID_ALBUM
                }
            }

            ScannerScreenMode.CROP_STUDIO -> {
                if (pages.isNotEmpty() && currentPageIndex in pages.indices) {
                    val page = pages[currentPageIndex]
                    ScannerCropStudioView(
                        page = page,
                        onRotateLeft = {
                            pages[currentPageIndex] = page.copy(rotationDegrees = (page.rotationDegrees - 90 + 360) % 360)
                        },
                        onRotateRight = {
                            pages[currentPageIndex] = page.copy(rotationDegrees = (page.rotationDegrees + 90) % 360)
                        },
                        onApplyCrop = { cropPoints ->
                            pages[currentPageIndex] = page.copy(cropPoints = cropPoints)
                            screenMode = ScannerScreenMode.PAGER_EDITOR
                        },
                        onCancel = { screenMode = ScannerScreenMode.PAGER_EDITOR }
                    )
                } else {
                    screenMode = ScannerScreenMode.GRID_ALBUM
                }
            }

            ScannerScreenMode.SIGNATURE_STUDIO -> {
                // Placed signature overlay canvas
                if (pages.isNotEmpty() && currentPageIndex in pages.indices) {
                    ScannerSignaturePlacementView(
                        page = pages[currentPageIndex],
                        onConfirm = { updatedPage ->
                            pages[currentPageIndex] = updatedPage
                            screenMode = ScannerScreenMode.PAGER_EDITOR
                        },
                        onCancel = { screenMode = ScannerScreenMode.PAGER_EDITOR }
                    )
                } else {
                    screenMode = ScannerScreenMode.PAGER_EDITOR
                }
            }
        }

        // Add Source Bottom Sheet (Camera or Gallery)
        if (showAddSourceSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddSourceSheet = false },
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add Pages to Document",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            onClick = {
                                showAddSourceSheet = false
                                launchCamera()
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Camera", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }

                        Card(
                            onClick = {
                                showAddSourceSheet = false
                                galleryLauncher.launch("image/*")
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Gallery Album", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Signature Source Dialog (Draw, Scan, Album)
        if (showSignatureSourceDialog && pages.isNotEmpty() && currentPageIndex in pages.indices) {
            SignatureSourceModal(
                onDismiss = { showSignatureSourceDialog = false },
                onSignatureReady = { sigBitmap, inkColor, isDrawn ->
                    showSignatureSourceDialog = false
                    val cleanSig = DocumentImageProcessor.makeSignatureTransparent(sigBitmap, inkColor, isDrawnSignature = isDrawn)
                    pages[currentPageIndex].signatures.add(PlacedSignature(bitmap = cleanSig))
                    screenMode = ScannerScreenMode.SIGNATURE_STUDIO
                }
            )
        }

        // Watermark Dialog
        if (showWatermarkDialog && pages.isNotEmpty() && currentPageIndex in pages.indices) {
            WatermarkDialog(
                currentWatermark = pages[currentPageIndex].watermark,
                onDismiss = { showWatermarkDialog = false },
                onApply = { wm ->
                    pages[currentPageIndex] = pages[currentPageIndex].copy(watermark = wm)
                    showWatermarkDialog = false
                    Toast.makeText(context, "Watermark applied to document page!", Toast.LENGTH_SHORT).show()
                },
                onRemove = {
                    pages[currentPageIndex] = pages[currentPageIndex].copy(watermark = null)
                    showWatermarkDialog = false
                    Toast.makeText(context, "Watermark removed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

/**
 * SCREEN 1: GRID ALBUM VIEW (OKEN Scanner 2-per-row grid with badges and selection)
 */
@Composable
fun ScannerGridAlbumView(
    documentTitle: String,
    pages: List<ScannedPage>,
    isSelectionMode: Boolean,
    isExportingPdf: Boolean,
    onTitleChange: (String) -> Unit,
    onOpenAddSheet: () -> Unit,
    onPageClick: (Int) -> Unit,
    onPageLongClick: (Int) -> Unit,
    onToggleSelectAll: () -> Unit,
    onExitSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMovePage: (from: Int, to: Int) -> Unit,
    onExportPdf: () -> Unit
) {
    val context = LocalContext.current
    var isEditingTitle by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf(documentTitle) }
    val historyList = remember { mutableStateListOf<PdfHistoryItem>() }

    LaunchedEffect(pages.size) {
        historyList.clear()
        historyList.addAll(PdfHistoryManager.getPdfs(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top App Bar - Only show when document has pages or in selection mode
        if (pages.isNotEmpty() || isSelectionMode) {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isSelectionMode) {
                        val selectedCount = pages.count { it.isSelected }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onExitSelection) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF0F172A))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$selectedCount selected",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        TextButton(onClick = onToggleSelectAll) {
                            Text(
                                text = if (pages.all { it.isSelected }) "Deselect All" else "Select All",
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            if (isEditingTitle) {
                                OutlinedTextField(
                                    value = titleInput,
                                    onValueChange = { titleInput = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                )
                                IconButton(onClick = {
                                    onTitleChange(titleInput)
                                    isEditingTitle = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF059669))
                                }
                            } else {
                                Text(
                                    text = documentTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    titleInput = documentTitle
                                    isEditingTitle = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Title", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Reorder mode toggle
                        IconButton(onClick = { isReorderMode = !isReorderMode }) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = "Toggle Reorder Mode",
                                tint = if (isReorderMode) Color(0xFF059669) else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        // Convert to PDF direct action
                        Button(
                            onClick = onExportPdf,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            enabled = !isExportingPdf
                        ) {
                            if (isExportingPdf) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Main Grid or Empty State
        if (pages.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                        .clickable(onClick = onOpenAddSheet),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Capture Document",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(42.dp)
                    )
                }
                Text(
                    text = "No Scanned Pages Yet",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Capture documents via Camera or import photos from Gallery to crop, apply filters, add signatures & convert to PDF.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onOpenAddSheet,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Document Pages", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Recent Scanned PDFs History List (Max 12, 48h auto-pruned for privacy)
                if (historyList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recent Documents", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("(48h privacy)", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                        TextButton(onClick = {
                            PdfHistoryManager.clearHistory(context)
                            historyList.clear()
                        }) {
                            Text("Clear", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        historyList.take(12).forEach { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${item.getFormattedDate()} • ${item.getFormattedSize()}", fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { PdfHistoryManager.openPdf(context, item) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Visibility, contentDescription = "Open", tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { PdfHistoryManager.sharePdf(context, item) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = {
                                            PdfHistoryManager.deletePdf(context, item.id)
                                            historyList.remove(item)
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onPageClick(index) },
                                    onLongPress = { onPageLongClick(index) }
                                )
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (page.isSelected) 2.dp else 1.dp,
                            color = if (page.isSelected) Color(0xFF059669) else Color(0xFFE2E8F0)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Page Preview Bitmap
                            val previewBitmap = remember(page.originalUri, page.rotationDegrees, page.filter, page.cropPoints, page.signatures.size) {
                                DocumentImageProcessor.processFullPage(context, page)
                            }

                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }

                            // Bottom-left Page Number Badge ("01", "02", etc.)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(
                                        Color(0xCC0F172A),
                                        RoundedCornerShape(topEnd = 6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = String.format("%02d", index + 1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Top-right Selection Checkbox
                            if (isSelectionMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .background(
                                            if (page.isSelected) Color(0xFF059669) else Color(0x99000000),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(1.2.dp, Color.White, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (page.isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Reorder Controls (Both in Reorder Mode and standard mode)
                            if (isReorderMode) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color(0xE60F172A))
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onMovePage(index, index - 1) },
                                        enabled = index > 0,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("◀ Prev", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Pg ${index + 1}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Button(
                                        onClick = { onMovePage(index, index + 1) },
                                        enabled = index < pages.size - 1,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Next ▶", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (!isSelectionMode) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color(0xBB000000), RoundedCornerShape(4.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (index > 0) {
                                        IconButton(
                                            onClick = { onMovePage(index, index - 1) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    if (index < pages.size - 1) {
                                        IconButton(
                                            onClick = { onMovePage(index, index + 1) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar - Only show when pages are added or in selection mode
        if (pages.isNotEmpty() || isSelectionMode) {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        BottomNavButton(icon = Icons.Default.Delete, label = "Delete", tint = Color(0xFFEF4444), onClick = onDeleteSelected)
                        BottomNavButton(icon = Icons.Default.Close, label = "Cancel", tint = Color(0xFF64748B), onClick = onExitSelection)
                    } else {
                        BottomNavButton(icon = Icons.Default.AddAPhoto, label = "Add", tint = Color(0xFF059669), onClick = onOpenAddSheet)
                        BottomNavButton(
                            icon = Icons.Default.PictureAsPdf,
                            label = "Export PDF",
                            tint = Color(0xFF059669),
                            onClick = onExportPdf
                        )
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 2: PAGER EDITOR VIEW (CamScanner / Oken Style Full Page Editor)
 */
@Composable
fun ScannerPagerEditorView(
    pages: MutableList<ScannedPage>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenCrop: () -> Unit,
    onOpenSignature: () -> Unit,
    onOpenWatermark: () -> Unit,
    onOpenMore: () -> Unit,
    onDeleteCurrentPage: () -> Unit
) {
    val context = LocalContext.current
    val currentPage = pages[currentIndex]
    var showFullscreenZoom by remember { mutableStateOf(false) }
    var areFiltersVisible by remember { mutableStateOf(true) }
    var showOcrDialog by remember { mutableStateOf(false) }

    // Previews of all filters
    val currentProcessedBitmap = remember(currentPage.originalUri, currentPage.rotationDegrees, currentPage.filter, currentPage.cropPoints, currentPage.signatures.size) {
        DocumentImageProcessor.processFullPage(context, currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // Page indicator with move left/right reorder buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (currentIndex > 0) {
                            val item = pages.removeAt(currentIndex)
                            pages.add(currentIndex - 1, item)
                            onIndexChange(currentIndex - 1)
                        }
                    },
                    enabled = currentIndex > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Move Page Earlier",
                        tint = if (currentIndex > 0) Color.White else Color(0x44FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${currentIndex + 1}/${pages.size}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = {
                        if (currentIndex < pages.size - 1) {
                            val item = pages.removeAt(currentIndex)
                            pages.add(currentIndex + 1, item)
                            onIndexChange(currentIndex + 1)
                        }
                    },
                    enabled = currentIndex < pages.size - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Move Page Later",
                        tint = if (currentIndex < pages.size - 1) Color.White else Color(0x44FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row {
                IconButton(onClick = {
                    val nextRot = (currentPage.rotationDegrees + 90) % 360
                    pages[currentIndex] = currentPage.copy(rotationDegrees = nextRot)
                }) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color.White)
                }
                IconButton(onClick = onDeleteCurrentPage) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                }
            }
        }

        // Document Image Display Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentProcessedBitmap != null) {
                        Image(
                            bitmap = currentProcessedBitmap.asImageBitmap(),
                            contentDescription = "Page ${currentIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CircularProgressIndicator(color = Color(0xFF059669))
                    }

                    // Zoom / Fullscreen icon button (top-left) - opens full screen pinch-to-zoom
                    IconButton(
                        onClick = { showFullscreenZoom = true },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(Color(0x99000000), CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen Zoom",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Navigation Arrows on left/right edges
                    if (currentIndex > 0) {
                        IconButton(
                            onClick = { onIndexChange(currentIndex - 1) },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(8.dp)
                                .background(Color(0x99000000), CircleShape)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                        }
                    }

                    if (currentIndex < pages.size - 1) {
                        IconButton(
                            onClick = { onIndexChange(currentIndex + 1) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(8.dp)
                                .background(Color(0x99000000), CircleShape)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Scanner Filter Carousel Strip (Original, Magic Color, Lighten, Grayscale, B&W, Eco, No shadow)
        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 4.dp
        ) {
            val filterThumbnails = remember(currentPage.originalUri, currentPage.rotationDegrees, currentPage.cropPoints) {
                val raw = DocumentImageProcessor.decodeBitmapFromUri(context, currentPage.originalUri, 300)
                val rotated = if (raw != null && currentPage.rotationDegrees % 360 != 0) {
                    DocumentImageProcessor.rotateBitmap(raw, currentPage.rotationDegrees.toFloat())
                } else {
                    raw
                }
                val cropped = if (rotated != null && currentPage.cropPoints != null) {
                    DocumentImageProcessor.perspectiveCrop(rotated, currentPage.cropPoints!!)
                } else {
                    rotated
                }
                if (cropped != null) {
                    DocumentImageProcessor.generateFilterThumbnails(cropped)
                } else {
                    emptyMap()
                }
            }

            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { areFiltersVisible = !areFiltersVisible }
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scanner Filters",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (areFiltersVisible) "Hide" else "Show",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (areFiltersVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (areFiltersVisible) "Collapse Filters" else "Expand Filters",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = areFiltersVisible) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(FilterType.values()) { filter ->
                            val isSelected = currentPage.filter == filter
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        pages[currentIndex] = currentPage.copy(filter = filter)
                                    }
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp, 48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF059669) else Color(0xFF334155))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF475569),
                                            shape = RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val thumbBmp = filterThumbnails[filter]
                                    if (thumbBmp != null) {
                                        Image(
                                            bitmap = thumbBmp.asImageBitmap(),
                                            contentDescription = filter.displayName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0x3310B981))
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = filter.displayName.take(3),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = filter.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF10B981) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Tool Bar (Crop, Signature, Watermark, OCR, Done)
        Surface(
            color = Color(0xFF0F172A),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavButton(icon = Icons.Default.Crop, label = "Crop", tint = Color.White, onClick = onOpenCrop)
                BottomNavButton(icon = Icons.Default.Gesture, label = "Signature", tint = Color.White, onClick = onOpenSignature)
                BottomNavButton(icon = Icons.Default.BrandingWatermark, label = "Watermark", tint = Color.White, onClick = onOpenWatermark)
                BottomNavButton(icon = Icons.Default.DocumentScanner, label = "OCR Text", tint = Color.White, onClick = { showOcrDialog = true })
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFF059669), CircleShape)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                }
            }
        }
    }

    if (showOcrDialog && currentProcessedBitmap != null) {
        OfflineOcrDialog(
            bitmap = currentProcessedBitmap,
            onDismiss = { showOcrDialog = false }
        )
    }

    if (showFullscreenZoom && currentProcessedBitmap != null) {
        ZoomableFullscreenImageDialog(
            bitmap = currentProcessedBitmap,
            pageTitle = "Page ${currentIndex + 1} of ${pages.size}",
            onDismiss = { showFullscreenZoom = false }
        )
    }
}

/**
 * Full-screen zoomable viewer dialog with pinch-to-zoom, pan, and double-tap zoom
 */
@Composable
fun ZoomableFullscreenImageDialog(
    bitmap: Bitmap,
    pageTitle: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Full screen zoomable image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = pageTitle,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // Top Bar Overlay
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color(0xCC0F172A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(pageTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (scale > 1.05f) {
                        TextButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                            Text("Reset Zoom", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Text("Pinch to zoom", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 3: 8-POINT CROP STUDIO VIEW (Perspective Corner / Edge Drag Handles)
 */
@Composable
fun ScannerCropStudioView(
    page: ScannedPage,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onApplyCrop: (List<Offset>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // Decode original bitmap with rotation applied
    val baseBitmap = remember(page.originalUri, page.rotationDegrees) {
        val raw = DocumentImageProcessor.decodeBitmapFromUri(context, page.originalUri)
        if (raw != null && page.rotationDegrees % 360 != 0) {
            DocumentImageProcessor.rotateBitmap(raw, page.rotationDegrees.toFloat())
        } else {
            raw
        }
    }

    // 4 Corner handles: 0=TL, 1=TR, 2=BR, 3=BL in normalized coords [0..1]
    var corners by remember(page.cropPoints) {
        mutableStateOf(
            page.cropPoints ?: listOf(
                Offset(0.05f, 0.05f), // Top-Left
                Offset(0.95f, 0.05f), // Top-Right
                Offset(0.95f, 0.95f), // Bottom-Right
                Offset(0.05f, 0.95f)  // Bottom-Left
            )
        )
    }

    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text("Adjust Document Boundaries", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = { onApplyCrop(corners) }) {
                Icon(Icons.Default.Check, contentDescription = "Apply", tint = Color(0xFF10B981))
            }
        }

        // Interactive 8-point Canvas over Image
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .onGloballyPositioned { viewSize = it.size },
            contentAlignment = Alignment.Center
        ) {
            if (baseBitmap != null && viewSize.width > 0 && viewSize.height > 0) {
                // Compute actual scaled display rect of the bitmap inside view
                val viewW = viewSize.width.toFloat()
                val viewH = viewSize.height.toFloat()
                val bmpW = baseBitmap.width.toFloat()
                val bmpH = baseBitmap.height.toFloat()

                val scale = min(viewW / bmpW, viewH / bmpH)
                val drawW = bmpW * scale
                val drawH = bmpH * scale
                val drawLeft = (viewW - drawW) / 2f
                val drawTop = (viewH - drawH) / 2f

                Image(
                    bitmap = baseBitmap.asImageBitmap(),
                    contentDescription = "Crop Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 8-Point Draggable Overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(drawLeft, drawTop, drawW, drawH) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val touchPos = change.position

                                // Find closest handle among the 4 corners or 4 midpoints
                                var closestIndex = -1
                                var minDistance = Float.MAX_VALUE

                                corners.forEachIndexed { idx, cNorm ->
                                    val hPos = Offset(drawLeft + cNorm.x * drawW, drawTop + cNorm.y * drawH)
                                    val dist = (hPos - touchPos).getDistance()
                                    if (dist < minDistance && dist < 120f) {
                                        minDistance = dist
                                        closestIndex = idx
                                    }
                                }

                                if (closestIndex != -1) {
                                    val currentNorm = corners[closestIndex]
                                    val newNormX = (currentNorm.x + (dragAmount.x / drawW)).coerceIn(0f, 1f)
                                    val newNormY = (currentNorm.y + (dragAmount.y / drawH)).coerceIn(0f, 1f)

                                    val mutable = corners.toMutableList()
                                    mutable[closestIndex] = Offset(newNormX, newNormY)
                                    corners = mutable
                                }
                            }
                        }
                ) {
                    val p0 = Offset(drawLeft + corners[0].x * drawW, drawTop + corners[0].y * drawH)
                    val p1 = Offset(drawLeft + corners[1].x * drawW, drawTop + corners[1].y * drawH)
                    val p2 = Offset(drawLeft + corners[2].x * drawW, drawTop + corners[2].y * drawH)
                    val p3 = Offset(drawLeft + corners[3].x * drawW, drawTop + corners[3].y * drawH)

                    val greenColor = Color(0xFF10B981)

                    // Draw Quad Border Lines
                    drawLine(greenColor, p0, p1, strokeWidth = 3.dp.toPx())
                    drawLine(greenColor, p1, p2, strokeWidth = 3.dp.toPx())
                    drawLine(greenColor, p2, p3, strokeWidth = 3.dp.toPx())
                    drawLine(greenColor, p3, p0, strokeWidth = 3.dp.toPx())

                    // 4 Corners Handles
                    listOf(p0, p1, p2, p3).forEach { pt ->
                        drawCircle(Color.White, radius = 9.dp.toPx(), center = pt)
                        drawCircle(greenColor, radius = 7.dp.toPx(), center = pt)
                    }

                    // 4 Edge Midpoint Handles
                    val mid01 = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                    val mid12 = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                    val mid23 = Offset((p2.x + p3.x) / 2f, (p2.y + p3.y) / 2f)
                    val mid30 = Offset((p3.x + p0.x) / 2f, (p3.y + p0.y) / 2f)

                    listOf(mid01, mid12, mid23, mid30).forEach { mPt ->
                        drawCircle(Color.White, radius = 6.dp.toPx(), center = mPt)
                        drawCircle(Color(0xFF0284C7), radius = 5.dp.toPx(), center = mPt)
                    }
                }
            } else {
                CircularProgressIndicator(color = Color(0xFF10B981))
            }
        }

        // Bottom Controls: Left, Right, All, and Confirm
        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = onRotateLeft) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left", tint = Color.White)
                    }
                    IconButton(onClick = onRotateRight) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right", tint = Color.White)
                    }
                    OutlinedButton(
                        onClick = {
                            corners = listOf(
                                Offset(0f, 0f),
                                Offset(1f, 0f),
                                Offset(1f, 1f),
                                Offset(0f, 1f)
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (baseBitmap != null) {
                                corners = DocumentImageProcessor.autoDetectDocumentEdges(baseBitmap)
                                Toast.makeText(context, "Auto-edges detected", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { onApplyCrop(corners) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crop", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * SCREEN 4: SIGNATURE PLACEMENT VIEW (Move & Resize signature overlay on document)
 */
@Composable
fun ScannerSignaturePlacementView(
    page: ScannedPage,
    onConfirm: (ScannedPage) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val baseBitmap = remember(page.originalUri, page.rotationDegrees, page.filter, page.cropPoints) {
        val cleanPage = page.copy(signatures = mutableListOf())
        DocumentImageProcessor.processFullPage(context, cleanPage)
    }

    var signaturesList by remember { mutableStateOf(page.signatures.toMutableList()) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text("Position Signature on Document", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = {
                onConfirm(page.copy(signatures = signaturesList))
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF059669))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .onGloballyPositioned { containerSize = it.size },
            contentAlignment = Alignment.TopStart
        ) {
            if (baseBitmap != null) {
                val cWidth = if (containerSize.width > 0) containerSize.width.toFloat() else 600f
                val cHeight = if (containerSize.height > 0) containerSize.height.toFloat() else 800f
                val imgW = baseBitmap.width.toFloat().coerceAtLeast(1f)
                val imgH = baseBitmap.height.toFloat().coerceAtLeast(1f)
                val renderScale = minOf(cWidth / imgW, cHeight / imgH)
                val renderedW = imgW * renderScale
                val renderedH = imgH * renderScale
                val renderOffsetX = (cWidth - renderedW) / 2f
                val renderOffsetY = (cHeight - renderedH) / 2f

                // Document image container with tap-to-position signature support
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(signaturesList.size, renderedW, renderedH, renderOffsetX, renderOffsetY) {
                            detectTapGestures { tapOffset ->
                                if (renderedW > 0 && renderedH > 0 && signaturesList.isNotEmpty()) {
                                    val relX = (tapOffset.x - renderOffsetX) / renderedW
                                    val relY = (tapOffset.y - renderOffsetY) / renderedH
                                    if (relX in 0f..1f && relY in 0f..1f) {
                                        val mutable = signaturesList.toMutableList()
                                        val lastIdx = mutable.lastIndex
                                        mutable[lastIdx] = mutable[lastIdx].copy(
                                            normalizedX = relX.coerceIn(0.05f, 0.95f),
                                            normalizedY = relY.coerceIn(0.05f, 0.95f)
                                        )
                                        signaturesList = mutable
                                    }
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap = baseBitmap.asImageBitmap(),
                        contentDescription = "Document",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Overlay draggable & clickable signatures with 1:1 pixel coordinates (zero double-density bug)
                    signaturesList.forEachIndexed { index, sig ->
                        val sigAspect = if (sig.bitmap.height > 0) sig.bitmap.width.toFloat() / sig.bitmap.height.toFloat() else 2.5f
                        val targetSigW = (renderedW * sig.scale).coerceIn(60f, renderedW * 0.9f)
                        val targetSigH = targetSigW / sigAspect

                        val posX = (renderOffsetX + (sig.normalizedX * renderedW) - (targetSigW / 2f)).toInt()
                        val posY = (renderOffsetY + (sig.normalizedY * renderedH) - (targetSigH / 2f)).toInt()

                        val boxWidthDp = with(density) { targetSigW.toDp() }
                        val boxHeightDp = with(density) { targetSigH.toDp() }

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(posX, posY) }
                                .size(width = boxWidthDp, height = boxHeightDp)
                                .border(1.2.dp, Color(0xFF059669), RoundedCornerShape(4.dp))
                                .background(Color(0x10059669), RoundedCornerShape(4.dp))
                                .pointerInput(index, renderedW, renderedH) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val mutable = signaturesList.toMutableList()
                                        val s = mutable[index]
                                        val newX = (s.normalizedX + dragAmount.x / renderedW).coerceIn(0.05f, 0.95f)
                                        val newY = (s.normalizedY + dragAmount.y / renderedH).coerceIn(0.05f, 0.95f)
                                        mutable[index] = s.copy(normalizedX = newX, normalizedY = newY)
                                        signaturesList = mutable
                                    }
                                }
                        ) {
                            Image(
                                bitmap = sig.bitmap.asImageBitmap(),
                                contentDescription = "Signature",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            // Delete this signature badge
                            IconButton(
                                onClick = {
                                    val mutable = signaturesList.toMutableList()
                                    mutable.removeAt(index)
                                    signaturesList = mutable
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .background(Color(0xDDEF4444), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF059669))
                }
            }
        }

        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (signaturesList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Signature Size", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        val curScale = signaturesList.lastOrNull()?.scale ?: 0.35f
                        Text("${(curScale * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Slider(
                        value = signaturesList.lastOrNull()?.scale ?: 0.35f,
                        onValueChange = { newScale ->
                            val mutable = signaturesList.toMutableList()
                            val lastIdx = mutable.lastIndex
                            if (lastIdx >= 0) {
                                mutable[lastIdx] = mutable[lastIdx].copy(scale = newScale.coerceIn(0.12f, 0.85f))
                                signaturesList = mutable
                            }
                        },
                        valueRange = 0.12f..0.85f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (signaturesList.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { signaturesList = mutableListOf() },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove Signature", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Tap or drag signature to position on page", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }

                    Button(
                        onClick = { onConfirm(page.copy(signatures = signaturesList)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply Signature", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

/**
 * MODAL: Signature Source & Finger Drawing Canvas
 */
@Composable
fun SignatureSourceModal(
    onDismiss: () -> Unit,
    onSignatureReady: (Bitmap, SignatureInkColor, Boolean) -> Unit
) {
    val context = LocalContext.current
    var isDrawingMode by remember { mutableStateOf(false) }
    var selectedInkColor by remember { mutableStateOf(SignatureInkColor.BLUE) }

    val signatureGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bm = DocumentImageProcessor.decodeBitmapFromUri(context, uri)
            if (bm != null) {
                onSignatureReady(bm, selectedInkColor, false)
            }
        }
    }

    if (isDrawingMode) {
        SignatureDrawingDialog(
            inkColor = selectedInkColor,
            onDismiss = { isDrawingMode = false },
            onSignatureDone = { bm ->
                onSignatureReady(bm, selectedInkColor, true)
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Add Signature", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Signatures will be cleanly extracted with transparent background and recolored to your chosen ink:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    // Ink Color Selector (Royal Blue vs Deep Black)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Signature Ink Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SignatureInkColor.values().forEach { ink ->
                                val isSelected = selectedInkColor == ink
                                val chipColor = Color(android.graphics.Color.rgb(ink.r, ink.g, ink.b))
                                Card(
                                    onClick = { selectedInkColor = ink },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) chipColor else Color(0xFF0F172A)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0xFF475569)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    if (isSelected) Color.White else chipColor,
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = ink.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { isDrawingMode = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Gesture, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draw with Finger")
                    }

                    OutlinedButton(
                        onClick = { signatureGalleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import from Album / Gallery")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

/**
 * Interactive finger signature drawing pad
 */
@Composable
fun SignatureDrawingDialog(
    inkColor: SignatureInkColor,
    onDismiss: () -> Unit,
    onSignatureDone: (Bitmap) -> Unit
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    val currentPath = remember { mutableStateListOf<Offset>() }
    var drawSurfaceSize by remember { mutableStateOf(IntSize(800, 400)) }
    val activeInk = Color(android.graphics.Color.rgb(inkColor.r, inkColor.g, inkColor.b))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(400.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(activeInk, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Draw Your Signature (${inkColor.label})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                    TextButton(onClick = {
                        paths.clear()
                        currentPath.clear()
                    }) {
                        Text("Clear", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }

                // Drawing surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                        .onGloballyPositioned { if (it.size.width > 0 && it.size.height > 0) drawSurfaceSize = it.size }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { currentPath.add(it) },
                                onDragEnd = {
                                    if (currentPath.isNotEmpty()) {
                                        paths.add(currentPath.toList())
                                        currentPath.clear()
                                    }
                                }
                            ) { change, _ ->
                                change.consume()
                                currentPath.add(change.position)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 3.dp.toPx()

                        paths.forEach { pList ->
                            for (i in 0 until pList.size - 1) {
                                drawLine(activeInk, pList[i], pList[i + 1], strokeWidth = strokeW)
                            }
                        }

                        for (i in 0 until currentPath.size - 1) {
                            drawLine(activeInk, currentPath[i], currentPath[i + 1], strokeWidth = strokeW)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (paths.isEmpty() && currentPath.isEmpty()) {
                                onDismiss()
                                return@Button
                            }
                            // Render paths to transparent bitmap with exact surface pixel coordinates
                            val renderW = drawSurfaceSize.width.coerceAtLeast(400)
                            val renderH = drawSurfaceSize.height.coerceAtLeast(200)
                            val bmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                            val c = Canvas(bmp)
                            val p = Paint().apply {
                                color = android.graphics.Color.rgb(inkColor.r, inkColor.g, inkColor.b)
                                strokeWidth = 9f
                                isAntiAlias = true
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                            }
                            paths.forEach { list ->
                                val aPath = AndroidPath()
                                list.forEachIndexed { idx, pt ->
                                    if (idx == 0) aPath.moveTo(pt.x, pt.y) else aPath.lineTo(pt.x, pt.y)
                                }
                                c.drawPath(aPath, p)
                            }
                            onSignatureDone(bmp)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Use Signature", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * MODAL: Text & Image/Logo Watermark Customizer
 */
@Composable
fun WatermarkDialog(
    currentWatermark: PlacedWatermark?,
    onDismiss: () -> Unit,
    onApply: (PlacedWatermark) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var watermarkType by remember {
        mutableStateOf(
            if (currentWatermark?.badgePreset != null && currentWatermark.badgePreset != "NONE") 0
            else if (currentWatermark?.imageBitmap != null) 2
            else 1
        )
    } // 0=Stamp Badges, 1=Text, 2=Image/Logo

    var isBadgesUnlocked by remember {
        mutableStateOf(
            context.getSharedPreferences("qwiksave_user_prefs", Context.MODE_PRIVATE)
                .getBoolean("qwik_badges_unlocked_9918", false)
        )
    }
    var showPinUnlockDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var badgeScaleVal by remember { mutableStateOf(currentWatermark?.scale ?: 0.45f) }
    var badgePosition by remember { mutableStateOf("Bottom-Right") }

    var selectedBadgePreset by remember {
        mutableStateOf(currentWatermark?.badgePreset?.takeIf { it != "NONE" } ?: "STAMP_APPROVED")
    }
    var textInput by remember { mutableStateOf(currentWatermark?.text ?: "CONFIDENTIAL") }
    var opacityVal by remember { mutableStateOf(currentWatermark?.opacity ?: 0.32f) }
    var isDiagonal by remember { mutableStateOf(currentWatermark?.isDiagonal ?: true) }
    var fontStyle by remember { mutableStateOf(currentWatermark?.fontStyle ?: "Sans") }
    var selectedColorArgb by remember { mutableStateOf(currentWatermark?.colorArgb ?: android.graphics.Color.GRAY) }

    var imageLogoBitmap by remember { mutableStateOf<Bitmap?>(currentWatermark?.imageBitmap) }
    var imageScaleVal by remember { mutableStateOf(currentWatermark?.scale ?: 0.45f) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bm = DocumentImageProcessor.decodeBitmapFromUri(context, uri)
            if (bm != null) {
                imageLogoBitmap = bm
            }
        }
    }

    val stampPresets = listOf(
        Triple("STAMP_APPROVED", "Approved Stamp", "Circular Green Seal ★ Official"),
        Triple("STAMP_VERIFIED", "Verified Seal", "Circular Teal Seal ★ Certified"),
        Triple("BADGE_CONFIDENTIAL", "Confidential", "Red Double Box ★ Restricted"),
        Triple("BADGE_DRAFT", "Draft Badge", "Amber Box ★ Review Copy"),
        Triple("STAMP_OFFICIAL", "Official Seal", "Navy Blue Seal ★ Validated")
    )

    val presetWords = listOf("CONFIDENTIAL", "OFFICIAL", "ORIGINAL", "SAMPLE", "DRAFT")
    val fontOptions = listOf("Sans", "Serif", "Mono")
    val colorOptions = listOf(
        Pair("Gray", android.graphics.Color.GRAY),
        Pair("Slate", android.graphics.Color.rgb(30, 41, 59)),
        Pair("Red", android.graphics.Color.rgb(220, 38, 38)),
        Pair("Blue", android.graphics.Color.rgb(29, 78, 216)),
        Pair("Green", android.graphics.Color.rgb(5, 150, 105))
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Document Watermark",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = {
                            selectedBadgePreset = "STAMP_APPROVED"
                            textInput = "CONFIDENTIAL"
                            opacityVal = 0.32f
                            onRemove()
                            Toast.makeText(context, "Watermark reset", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Reset", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (currentWatermark != null) {
                            TextButton(onClick = onRemove) {
                                Text("Remove", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Watermark Type Selector (3-way: Stamp Badges, Text, Image/Logo)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Stamp Badges", "Custom Text", "Image Logo").forEachIndexed { idx, label ->
                        val isSel = watermarkType == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) Color(0xFF059669) else Color.Transparent)
                                .clickable { watermarkType = idx }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                if (watermarkType == 0) {
                    // PRESET STAMP BADGES (Circular Stamps, Boxes)
                    if (!isBadgesUnlocked) {
                        Card(
                            onClick = { showPinUnlockDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Official Badges Protected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                        Text("Enter PIN code to unlock official rubber stamps", fontSize = 10.5.sp, color = Color(0xFFB45309))
                                    }
                                }
                                Text("Unlock 🔒", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                            }
                        }
                    }

                    Text("Select Preset Rubber Stamp:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        stampPresets.forEach { (presetKey, title, subtitle) ->
                            val isSelected = selectedBadgePreset == presetKey
                            val badgeColor = when (presetKey) {
                                "STAMP_APPROVED" -> Color(0xFF10B981)
                                "STAMP_VERIFIED" -> Color(0xFF0D9488)
                                "BADGE_CONFIDENTIAL" -> Color(0xFFEF4444)
                                "BADGE_DRAFT" -> Color(0xFFF59E0B)
                                else -> Color(0xFF1E3A8A)
                            }
                            Card(
                                onClick = {
                                    if (!isBadgesUnlocked) {
                                        showPinUnlockDialog = true
                                        Toast.makeText(context, "Please enter authorization PIN to unlock official badges", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedBadgePreset = presetKey
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected && isBadgesUnlocked) badgeColor.copy(alpha = 0.10f) else Color(0xFFF8FAFC)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected && isBadgesUnlocked) 2.dp else 1.dp,
                                    color = if (isSelected && isBadgesUnlocked) badgeColor else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                if (!isBadgesUnlocked) {
                                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Text(subtitle, fontSize = 10.5.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                    if (isSelected && isBadgesUnlocked) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Badge Scale / Size Slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Badge Scale / Size:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Text("${(badgeScaleVal * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                        Slider(
                            value = badgeScaleVal,
                            onValueChange = { badgeScaleVal = it },
                            valueRange = 0.20f..0.75f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF059669),
                                activeTrackColor = Color(0xFF059669)
                            )
                        )
                    }

                    // Badge Placement Position Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Badge Placement Position:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Center", "Top-Right", "Bottom-Right", "Top-Left", "Bottom-Left").forEach { pos ->
                                val isSel = badgePosition == pos
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) Color(0xFF059669) else Color(0xFFF1F5F9))
                                        .clickable { badgePosition = pos }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pos.replace("-", "\n"),
                                        fontSize = 9.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else Color(0xFF475569),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (watermarkType == 1) {
                    // TEXT WATERMARK CONTROLS
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Watermark Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )

                    // Quick presets
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presetWords) { word ->
                            SuggestionChip(
                                onClick = { textInput = word },
                                label = { Text(word, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }

                    // Font Family selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Font Style:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fontOptions.forEach { font ->
                                val isSelected = fontStyle == font
                                Card(
                                    onClick = { fontStyle = font },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF059669) else Color(0xFFF1F5F9)
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = font,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Watermark Angle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Diagonal Angle (45°):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Switch(
                            checked = isDiagonal,
                            onCheckedChange = { isDiagonal = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF059669)
                            )
                        )
                    }

                    // Color choices
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Watermark Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colorOptions.forEach { (cName, cVal) ->
                                val isSel = selectedColorArgb == cVal
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(cVal))
                                        .border(
                                            width = if (isSel) 2.5.dp else 1.dp,
                                            color = if (isSel) Color(0xFF0F172A) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorArgb = cVal },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // IMAGE / LOGO WATERMARK CONTROLS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (imageLogoBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = imageLogoBitmap!!.asImageBitmap(),
                                    contentDescription = "Logo Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { logoPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (imageLogoBitmap != null) "Change Logo Image" else "Choose Logo from Gallery", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Logo Scale slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Logo Scale / Size:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                Text("${(imageScaleVal * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }
                            Slider(
                                value = imageScaleVal,
                                onValueChange = { imageScaleVal = it },
                                valueRange = 0.15f..0.85f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF059669),
                                    activeTrackColor = Color(0xFF059669)
                                )
                            )
                        }
                    }
                }

                // Shared Opacity Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Opacity:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Text("${(opacityVal * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                    Slider(
                        value = opacityVal,
                        onValueChange = { opacityVal = it },
                        valueRange = 0.05f..0.85f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF059669),
                            activeTrackColor = Color(0xFF059669)
                        )
                    )
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (watermarkType == 0) {
                                if (!isBadgesUnlocked) {
                                    showPinUnlockDialog = true
                                    Toast.makeText(context, "Please unlock official badges with authorization PIN first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val (normX, normY) = when (badgePosition) {
                                    "Top-Left" -> Pair(0.24f, 0.18f)
                                    "Top-Right" -> Pair(0.76f, 0.18f)
                                    "Center" -> Pair(0.50f, 0.50f)
                                    "Bottom-Left" -> Pair(0.24f, 0.82f)
                                    else -> Pair(0.76f, 0.82f)
                                }
                                onApply(
                                    PlacedWatermark(
                                        text = "",
                                        opacity = opacityVal,
                                        isDiagonal = false,
                                        scale = badgeScaleVal,
                                        normalizedX = normX,
                                        normalizedY = normY,
                                        badgePreset = selectedBadgePreset
                                    )
                                )
                            } else if (watermarkType == 1) {
                                if (textInput.isNotBlank()) {
                                    onApply(
                                        PlacedWatermark(
                                            text = textInput.trim(),
                                            opacity = opacityVal,
                                            isDiagonal = isDiagonal,
                                            colorArgb = selectedColorArgb,
                                            fontStyle = fontStyle,
                                            badgePreset = "NONE"
                                        )
                                    )
                                }
                            } else {
                                if (imageLogoBitmap != null) {
                                    onApply(
                                        PlacedWatermark(
                                            text = "",
                                            opacity = opacityVal,
                                            isDiagonal = false,
                                            imageBitmap = imageLogoBitmap,
                                            scale = imageScaleVal,
                                            badgePreset = "NONE"
                                        )
                                    )
                                } else {
                                    Toast.makeText(context, "Please choose a logo image first", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Apply Watermark", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showPinUnlockDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinUnlockDialog = false
                    pinInput = ""
                    pinError = null
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF059669))
                        Text("Enter Authorization PIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Official rubber stamps and verified seals require a 4-digit security PIN to unlock and use.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                    pinInput = it
                                    pinError = null
                                }
                            },
                            label = { Text("4-Digit PIN") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            ),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError != null) {
                            Text(pinError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val md = java.security.MessageDigest.getInstance("SHA-256")
                            val digest = md.digest(pinInput.trim().toByteArray(Charsets.UTF_8))
                            val hex = digest.joinToString("") { "%02x".format(it) }
                            // Secure PIN verification using SHA-256 hash without plaintext hardcoding
                            if (hex.equals("5f910aea9bae3c3c127ad74186cbf59c4d2cbdd77e9bcb9cefc0ffe876bccb87", ignoreCase = true)) {
                                context.getSharedPreferences("qwiksave_user_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("qwik_badges_unlocked_9918", true)
                                    .apply()
                                isBadgesUnlocked = true
                                showPinUnlockDialog = false
                                pinInput = ""
                                Toast.makeText(context, "Official Rubber Stamps & Badges Unlocked!", Toast.LENGTH_SHORT).show()
                            } else {
                                pinError = "Incorrect authorization PIN. Please verify and retry."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPinUnlockDialog = false
                        pinInput = ""
                        pinError = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun OfflineOcrDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(true) }

    LaunchedEffect(bitmap) {
        try {
            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
            )
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    recognizedText = visionText.text.ifBlank { "No text detected on this scanned document page." }
                    isProcessing = false
                }
                .addOnFailureListener {
                    recognizedText = "Could not recognize text: ${it.localizedMessage}"
                    isProcessing = false
                }
        } catch (e: Exception) {
            recognizedText = "OCR Error: ${e.localizedMessage}"
            isProcessing = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color(0xFF10B981))
                        Text("Offline OCR Text Scanner", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = if (isProcessing) Alignment.Center else Alignment.TopStart
                ) {
                    if (isProcessing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                            Text("Extracting document text...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    } else {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = recognizedText ?: "",
                                fontSize = 13.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // Actions: Copy & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!recognizedText.isNullOrBlank()) {
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, recognizedText)
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Scanned Text"))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing && !recognizedText.isNullOrBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (!recognizedText.isNullOrBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Scanned Text", recognizedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing && !recognizedText.isNullOrBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

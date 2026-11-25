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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
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

    // Document State
    var documentTitle by remember {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        mutableStateOf("SPM_ASSIGNMENT_${sdf.format(Date())}")
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

    // Launch camera helper
    fun launchCamera() {
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
                        onOpenSignature = { showSignatureSourceDialog = true },
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
        if (showSignatureSourceDialog) {
            SignatureSourceModal(
                onDismiss = { showSignatureSourceDialog = false },
                onSignatureReady = { sigBitmap ->
                    showSignatureSourceDialog = false
                    val cleanSig = DocumentImageProcessor.makeSignatureTransparent(sigBitmap)
                    pages[currentPageIndex].signatures.add(PlacedSignature(bitmap = cleanSig))
                    screenMode = ScannerScreenMode.SIGNATURE_STUDIO
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
    var titleInput by remember { mutableStateOf(documentTitle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top App Bar
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
                            tint = Color(0xFF2563EB),
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

                    // Convert to PDF direct action
                    Button(
                        onClick = onExportPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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

        // Main Grid or Empty State
        if (pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFFE2E8F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(40.dp))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Document Pages", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

                            // Reorder Arrows (If not in multi-selection)
                            if (!isSelectionMode) {
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

        // Bottom Action Bar
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
                    BottomNavButton(icon = Icons.Default.AddAPhoto, label = "Add", tint = Color(0xFF2563EB), onClick = onOpenAddSheet)
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
    onOpenMore: () -> Unit,
    onDeleteCurrentPage: () -> Unit
) {
    val context = LocalContext.current
    val currentPage = pages[currentIndex]

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

            Text(
                text = "${currentIndex + 1}/${pages.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

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
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Scanner Filters",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
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
                                    .size(52.dp, 44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF059669) else Color(0xFF334155))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF10B981) else Color(0xFF475569),
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter.displayName.take(3),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
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

        // Bottom Tool Bar (Crop, Signature, Done)
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
    val baseBitmap = remember(page.originalUri, page.rotationDegrees, page.filter, page.cropPoints) {
        DocumentImageProcessor.processFullPage(context, page)
    }

    var signaturesList by remember { mutableStateOf(page.signatures.toMutableList()) }

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
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF10B981))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (baseBitmap != null) {
                Image(
                    bitmap = baseBitmap.asImageBitmap(),
                    contentDescription = "Document",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Drag handles for the placed signature
                signaturesList.forEachIndexed { index, sig ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (sig.normalizedX * 240).dp,
                                y = (sig.normalizedY * 340).dp
                            )
                            .size((sig.scale * 200).dp)
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                            .pointerInput(index) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val mutable = signaturesList.toMutableList()
                                    val s = mutable[index]
                                    val newX = (s.normalizedX + dragAmount.x / 500f).coerceIn(0.1f, 0.9f)
                                    val newY = (s.normalizedY + dragAmount.y / 700f).coerceIn(0.1f, 0.9f)
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
                    }
                }
            } else {
                CircularProgressIndicator(color = Color(0xFF10B981))
            }
        }

        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Drag signature to position on page", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Button(
                    onClick = { onConfirm(page.copy(signatures = signaturesList)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply Signature", fontWeight = FontWeight.Bold)
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
    onSignatureReady: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var isDrawingMode by remember { mutableStateOf(false) }

    val signatureGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bm = DocumentImageProcessor.decodeBitmapFromUri(context, uri)
            if (bm != null) {
                onSignatureReady(bm)
            }
        }
    }

    if (isDrawingMode) {
        SignatureDrawingDialog(
            onDismiss = { isDrawingMode = false },
            onSignatureDone = onSignatureReady
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Add Signature", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Choose how you want to add your signature to this document:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    OutlinedButton(
                        onClick = { isDrawingMode = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Gesture, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draw with Finger")
                    }

                    OutlinedButton(
                        onClick = { signatureGalleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
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
    onDismiss: () -> Unit,
    onSignatureDone: (Bitmap) -> Unit
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath = remember { mutableStateListOf<Offset>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(380.dp),
            shape = RoundedCornerShape(12.dp),
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
                    Text("Draw Your Signature", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
                        .height(230.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
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
                        val inkColor = Color(0xFF0F172A)
                        val strokeW = 3.dp.toPx()

                        paths.forEach { pList ->
                            for (i in 0 until pList.size - 1) {
                                drawLine(inkColor, pList[i], pList[i + 1], strokeWidth = strokeW)
                            }
                        }

                        for (i in 0 until currentPath.size - 1) {
                            drawLine(inkColor, currentPath[i], currentPath[i + 1], strokeWidth = strokeW)
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
                            // Render paths to bitmap
                            val bmp = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888)
                            val c = Canvas(bmp)
                            val p = Paint().apply {
                                color = android.graphics.Color.rgb(15, 23, 42)
                                strokeWidth = 8f
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("Use Signature", fontWeight = FontWeight.Bold)
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

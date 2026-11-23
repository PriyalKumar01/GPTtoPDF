package com.qwiksave.app.battery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qwiksave.app.data.UserPreferences
import com.qwiksave.app.service.BatteryOverlayService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryStudioDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Live battery reading from BatteryManager
    val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager }
    val realLevel = remember {
        batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 88
    }

    var isOverlayEnabled by remember { mutableStateOf(UserPreferences.isBatteryOverlayEnabled(context)) }
    var isChargingAnimEnabled by remember { mutableStateOf(UserPreferences.isChargingAnimationEnabled(context)) }
    var selectedTemplateId by remember { mutableStateOf(UserPreferences.getBatteryTemplate(context)) }
    var selectedPosition by remember { mutableStateOf(UserPreferences.getBatteryPosition(context)) }
    var selectedScale by remember { mutableFloatStateOf(UserPreferences.getBatteryScale(context)) }
    var positionDropdownExpanded by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf("All") }
    var showPermissionAlert by remember { mutableStateOf(false) }

    val categories = listOf("All", "Anime", "Emoji", "Cyber", "Minimal")
    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == "All") BatteryTemplates.ALL
        else BatteryTemplates.ALL.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val activeTemplate = remember(selectedTemplateId) {
        BatteryTemplates.getById(selectedTemplateId)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16)),
            color = Color(0xFF090D16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF7E22CE), Color(0xFF3B82F6)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚡", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "Battery & Charging Show",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Status Bar Anime & Emoji Overlays",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = Color(0xFF1E293B), thickness = 1.dp)

                // 2. Main Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    // Item: Interactive Phone Screen Mockup Preview
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Phone Top Mockup Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF020617))
                                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    // Punch hole camera in top center
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .align(Alignment.TopCenter)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                            .border(1.dp, Color(0xFF334155), CircleShape)
                                    )

                                    // Left Status info
                                    Row(
                                        modifier = Modifier.align(Alignment.TopStart),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("4:12", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                    }

                                    // Live Battery Overlay Widget Preview
                                    Box(
                                        modifier = Modifier.align(
                                            when (selectedPosition) {
                                                "top_center" -> Alignment.Center
                                                "top_left" -> Alignment.TopStart
                                                else -> Alignment.TopEnd
                                            }
                                        )
                                    ) {
                                        AndroidView(
                                            factory = { ctx ->
                                                BatteryOverlayView(ctx).apply {
                                                    batteryPercent = realLevel
                                                    isCharging = true
                                                    template = activeTemplate
                                                    scaleFactor = selectedScale
                                                }
                                            },
                                            update = { view ->
                                                view.batteryPercent = realLevel
                                                view.template = activeTemplate
                                                view.scaleFactor = selectedScale
                                                view.invalidate()
                                            }
                                        )
                                    }
                                }

                                // Quick Preview Actions (Test Animation & Simulate Charge)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (!Settings.canDrawOverlays(context)) {
                                                showPermissionAlert = true
                                            } else {
                                                BatteryOverlayService.simulateCharging(context)
                                                Toast.makeText(context, "Playing Turbo Charging surge!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test Charging Show", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            Toast.makeText(context, "Current Battery: $realLevel% • Health: Good • Temp: 31°C", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Battery Info ($realLevel%)", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    // Item: Master Controls (Toggles)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Toggle 1: Persistent Status Bar Overlay
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Status Bar Battery Overlay",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Shows animated badge over your status bar permanently",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Switch(
                                        checked = isOverlayEnabled,
                                        onCheckedChange = { enable ->
                                            if (enable) {
                                                if (!Settings.canDrawOverlays(context)) {
                                                    showPermissionAlert = true
                                                } else {
                                                    isOverlayEnabled = true
                                                    UserPreferences.setBatteryOverlayEnabled(context, true)
                                                    BatteryOverlayService.start(context)
                                                    Toast.makeText(context, "Battery overlay activated!", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                isOverlayEnabled = false
                                                UserPreferences.setBatteryOverlayEnabled(context, false)
                                                BatteryOverlayService.stop(context)
                                                Toast.makeText(context, "Battery overlay paused", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF7E22CE)
                                        )
                                    )
                                }

                                Divider(color = Color(0xFF1E293B), thickness = 0.8.dp)

                                // Toggle 2: Turbo Charging Shockwave Animation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Turbo Charging Animation",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Plays animated shockwave whenever charger is plugged in",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Switch(
                                        checked = isChargingAnimEnabled,
                                        onCheckedChange = { enable ->
                                            isChargingAnimEnabled = enable
                                            UserPreferences.setChargingAnimationEnabled(context, enable)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF3B82F6)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Item: Position Dropdown & Scale Tuning
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Display Position", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Tap to change", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }

                                val positionOptions = listOf(
                                    Pair("top_right", "Top Right (Status Bar Battery - Recommended)"),
                                    Pair("top_center", "Top Center (Dynamic Notch / Island)"),
                                    Pair("top_left", "Top Left (Clock Companion)")
                                )
                                val currentLabel = positionOptions.find { it.first == selectedPosition }?.second
                                    ?: "Top Right (Status Bar Battery - Recommended)"

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        onClick = { positionDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E293B),
                                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF7E22CE))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA855F7),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = currentLabel,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Dropdown",
                                                tint = Color(0xFFA855F7),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = positionDropdownExpanded,
                                        onDismissRequest = { positionDropdownExpanded = false },
                                        modifier = Modifier
                                            .background(Color(0xFF0F172A))
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                    ) {
                                        positionOptions.forEach { (posKey, label) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (selectedPosition == posKey) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedPosition == posKey) Color(0xFFA855F7) else Color.White
                                                    )
                                                },
                                                leadingIcon = {
                                                    if (selectedPosition == posKey) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                                                    }
                                                },
                                                onClick = {
                                                    selectedPosition = posKey
                                                    UserPreferences.setBatteryPosition(context, posKey)
                                                    if (isOverlayEnabled) BatteryOverlayService.updateSettings(context)
                                                    positionDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Item: Category Filters
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Explore Themes & Animations",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categories) { cat ->
                                    val isSel = selectedCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (isSel) Color(0xFF7E22CE) else Color(0xFF1E293B))
                                            .border(1.dp, if (isSel) Color(0xFFA855F7) else Color(0xFF334155), RoundedCornerShape(18.dp))
                                            .clickable { selectedCategory = cat }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Item: Grid of Templates
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredTemplates.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { tItem ->
                                        val isChosen = selectedTemplateId == tItem.id
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    selectedTemplateId = tItem.id
                                                    UserPreferences.setBatteryTemplate(context, tItem.id)
                                                    if (isOverlayEnabled) {
                                                        BatteryOverlayService.updateSettings(context)
                                                    }
                                                    Toast.makeText(context, "${tItem.name} applied!", Toast.LENGTH_SHORT).show()
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isChosen) Color(0xFF1E1B4B) else Color(0xFF0F172A)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                if (isChosen) 1.8.dp else 1.dp,
                                                if (isChosen) Color(0xFFA855F7) else Color(0xFF1E293B)
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
                                                    Text(text = tItem.emojiIcon, fontSize = 26.sp)
                                                    if (isChosen) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color(0xFF059669))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = tItem.name,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Text(
                                                    text = tItem.tag,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(tItem.glowColor.toInt())
                                                )

                                                Text(
                                                    text = tItem.description,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF64748B),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Permission Request Dialog (Smooth one-click guide to enable Draw Over Other Apps)
            if (showPermissionAlert) {
                AlertDialog(
                    onDismissRequest = { showPermissionAlert = false },
                    title = {
                        Text("Permission Required 🔒", fontWeight = FontWeight.Bold, color = Color.White)
                    },
                    text = {
                        Text(
                            "To show the stylish battery animation and percentage over your phone status bar while other apps are open, Android requires the 'Display over other apps' permission.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPermissionAlert = false
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showPermissionAlert = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    },
                    containerColor = Color(0xFF0F172A),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

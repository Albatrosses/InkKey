package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.KeyEvent
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Slider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.export.ExportManager
import com.example.model.CanvasScrollMode
import com.example.model.PaperStyle
import com.example.model.TabletConfig
import com.example.model.TabletAction
import com.example.model.PressureCurve
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentDoc by viewModel.currentDocument.collectAsState()
    val palmRejection by viewModel.palmRejectionEnabled.collectAsState()
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val tabletConfig by viewModel.tabletConfig.collectAsState()
    val latestDebugEvent by viewModel.latestDebugEvent.collectAsState()
    var testHardwareButtonsEnabled by remember { mutableStateOf(false) }
    val connectedTabletsDetailed = remember(refreshTrigger) { TabletConfig.getConnectedTabletDevicesDetailed(context) }
    val connectedTablets = remember(connectedTabletsDetailed) { connectedTabletsDetailed.map { it.name } }

    var customPrompt by remember { mutableStateOf(ExportManager.DEFAULT_NEET_PROMPT) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    var exportedFiles by remember { mutableStateOf<List<ExportManager.ExportItem>>(emptyList()) }
    var exportDirDisplay by remember { mutableStateOf("") }
    var exportDirAbsolute by remember { mutableStateOf("") }
    var isCustomLocation by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger) {
        exportedFiles = ExportManager.getExportedFiles(context)
        exportDirDisplay = ExportManager.getExportDirectoryDisplayPath(context)
        exportDirAbsolute = ExportManager.getExportDirectoryAbsolutePath(context)
        isCustomLocation = ExportManager.isUsingCustomLocation(context)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                ExportManager.setCustomExportLocation(context, uri)
                refreshTrigger++
                Toast.makeText(context, "Export Location set to selected folder!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not set folder: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = { com.example.util.UpperTrialTrace.share(context) }) {
                Text("Share upper-button trial log")
            }
            // Section 1: Canvas & Paper Settings
            SectionHeader(title = "Default Paper & Canvas", icon = Icons.Default.Palette)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Paper Style Template", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(PaperStyle.RULED, PaperStyle.GRAPH, PaperStyle.PLAIN_WHITE, PaperStyle.BLACKBOARD, PaperStyle.AMOLED_BLACK).forEach { style ->
                            val isSel = currentDoc.paperStyle == style
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 2.dp),
                                onClick = { viewModel.setPaperStyle(style) }
                            ) {
                                Text(
                                    text = if (style == PaperStyle.AMOLED_BLACK) "AMOLED" else style.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text("Canvas Scrolling Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CanvasScrollMode.values().forEach { mode ->
                            val isSel = currentDoc.scrollMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 2.dp),
                                onClick = { viewModel.setScrollMode(mode) }
                            ) {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Physical Stylus & Drawing Tablet (XP-PEN Deco Mini 7)
            SectionHeader(title = "Physical Stylus & Tablet (XP-PEN)", icon = Icons.Default.Tablet)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Hardware Tablet Detection Status
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (connectedTabletsDetailed.isNotEmpty()) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = if (connectedTabletsDetailed.isNotEmpty()) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tablet,
                                        contentDescription = null,
                                        tint = if (connectedTabletsDetailed.isNotEmpty()) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (connectedTabletsDetailed.isNotEmpty())
                                                "Connected: ${connectedTabletsDetailed.size} Drawing Tablet/Stylus"
                                            else
                                                "Ready for XP-PEN Deco Mini 7 / Active Stylus",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (connectedTabletsDetailed.isNotEmpty()) Color(0xFF166534) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "USB-OTG hardware buttons, stylus barrel buttons, and pressure events are mapped directly without drivers.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (connectedTabletsDetailed.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    connectedTabletsDetailed.forEach { device ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(device.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                                    Text(
                                                        "Vendor: ${device.vendorHex} • Product: ${device.productHex} • Sources: ${device.sourceNames.joinToString(", ")}",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFDCFCE7)
                                                ) {
                                                    Text(
                                                        "ONLINE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF166534),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Test Hardware Buttons & Live Event Monitor
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Test Hardware Buttons & Stylus", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Inspect real-time XP-PEN KeyEvents, scan codes, stylus barrel buttons, and assign bindings",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = testHardwareButtonsEnabled,
                                    onCheckedChange = { testHardwareButtonsEnabled = it }
                                )
                            }

                            if (testHardwareButtonsEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "HARDWARE INPUT EVENT MONITOR",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val debug = latestDebugEvent
                                        if (debug != null) {
                                            Text("Event: ${debug.eventType}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Device: ${debug.deviceName} • Device ID: ${debug.deviceId}", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                                            Text("Hardware IDs: Vendor ${debug.vendorHex} • Product ${debug.productHex}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                            Text("Input Source: ${debug.source}", fontSize = 10.sp, color = Color(0xFFA5B4FC))
                                            if (debug.keyCode != 0) {
                                                Text("Key Code: ${debug.keyCode} (${debug.keyName}) • Scan Code: ${debug.scanCode} (0x${Integer.toHexString(debug.scanCode).uppercase()})", fontSize = 10.sp, color = Color(0xFFFDE047), fontWeight = FontWeight.SemiBold)
                                            }
                                            if (debug.buttonState != 0 || debug.buttonStateNames.isNotEmpty()) {
                                                Text("Button State: 0x${Integer.toHexString(debug.buttonState)} [${debug.buttonStateNames.joinToString()}]", fontSize = 10.sp, color = Color(0xFF86EFAC), fontWeight = FontWeight.SemiBold)
                                            }
                                            if (debug.toolType.isNotEmpty()) {
                                                Text("Tool Type: ${debug.toolType} • Pressure: ${(debug.pressure * 100).toInt()}%", fontSize = 10.sp, color = Color(0xFFF9A8D4))
                                            }
                                            Text("Detected Action: ${debug.resolvedAction}", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)

                                            // Quick bind button if key event detected
                                            if (debug.keyCode != 0 || debug.scanCode != 0) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                var showBindMenu by remember { mutableStateOf(false) }
                                                Box {
                                                    OutlinedButton(
                                                        onClick = { showBindMenu = true },
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Assign this Key (${if (debug.keyName.isNotEmpty()) debug.keyName else "Scan ${debug.scanCode}"}) to Express Key K1..K8", fontSize = 10.sp, color = Color.White)
                                                    }
                                                    DropdownMenu(
                                                        expanded = showBindMenu,
                                                        onDismissRequest = { showBindMenu = false }
                                                    ) {
                                                        (1..8).forEach { kIdx ->
                                                            DropdownMenuItem(
                                                                text = { Text("Assign to Express Key K$kIdx") },
                                                                onClick = {
                                                                    viewModel.learnExpressKey(kIdx, debug.keyCode, debug.scanCode)
                                                                    showBindMenu = false
                                                                    Toast.makeText(context, "Key assigned to Express Key K$kIdx!", Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                "Waiting for hardware input... Press any Express Key or Stylus Barrel Button to inspect event details.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 1. Stylus-Only Drawing Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Stylus-Only Drawing Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "When enabled, fingers only pan/scroll the canvas. Only the physical stylus pen draws.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tabletConfig.stylusOnlyDrawing,
                            onCheckedChange = {
                                viewModel.updateTabletConfig(tabletConfig.copy(stylusOnlyDrawing = it))
                            }
                        )
                    }

                    // 2. Palm Rejection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Palm Rejection", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Filters accidental palm touches while writing formulas and notes.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = palmRejection,
                            onCheckedChange = { viewModel.togglePalmRejection() }
                        )
                    }

                    // 3. Pressure Sensitivity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pressure Sensitivity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Modulates line thickness based on physical pen pressure.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tabletConfig.pressureEnabled,
                            onCheckedChange = {
                                viewModel.updateTabletConfig(tabletConfig.copy(pressureEnabled = it))
                            }
                        )
                    }

                    if (tabletConfig.pressureEnabled) {
                        // Pressure Curve Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Pressure Response Curve", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PressureCurve.values().forEach { curve ->
                                    val isSel = tabletConfig.pressureCurve == curve
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            viewModel.updateTabletConfig(tabletConfig.copy(pressureCurve = curve))
                                        }
                                    ) {
                                        Text(
                                            text = curve.displayName,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Min / Max Pressure Sliders
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Min Pressure Threshold", fontSize = 11.sp)
                                Text("${(tabletConfig.minPressure * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = tabletConfig.minPressure,
                                onValueChange = {
                                    if (it < tabletConfig.maxPressure) {
                                        viewModel.updateTabletConfig(tabletConfig.copy(minPressure = it))
                                    }
                                },
                                valueRange = 0.01f..0.5f
                            )
                        }
                    }

                    val allTabletActions = listOf(
                        TabletAction.AREA_ERASER,
                        TabletAction.STROKE_ERASER,
                        TabletAction.PEN,
                        TabletAction.HAND,
                        TabletAction.LASSO,
                        TabletAction.UNDO,
                        TabletAction.REDO,
                        TabletAction.ZOOM_IN,
                        TabletAction.ZOOM_OUT,
                        TabletAction.RESET_ZOOM,
                        TabletAction.PREV_PAGE,
                        TabletAction.NEXT_PAGE,
                        TabletAction.CLEAR_PAGE,
                        TabletAction.NEW_PAGE,
                        TabletAction.DELETE_PAGE,
                        TabletAction.TOGGLE_FULLSCREEN,
                        TabletAction.TOGGLE_TOOLBAR,
                        TabletAction.NONE
                    )

                    // 4. Stylus Barrel Buttons (Lower & Upper)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Physical Stylus Barrel Buttons",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Lower Barrel Button (Barrel 1)
                    var showLowerBarrelMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lower Barrel Button (Button 1)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Near the tip. Primary barrel button.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            OutlinedButton(
                                onClick = { showLowerBarrelMenu = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(tabletConfig.lowerBarrelAction.displayName, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            DropdownMenu(
                                expanded = showLowerBarrelMenu,
                                onDismissRequest = { showLowerBarrelMenu = false }
                            ) {
                                allTabletActions.forEach { action ->
                                    DropdownMenuItem(
                                        text = { Text(action.displayName) },
                                        onClick = {
                                            viewModel.updateTabletConfig(tabletConfig.copy(lowerBarrelAction = action))
                                            showLowerBarrelMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Upper Barrel Button (Barrel 2)
                    var showUpperBarrelMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Upper Barrel Button (Button 2)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Further from the tip. Secondary barrel button.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            OutlinedButton(
                                onClick = { showUpperBarrelMenu = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(tabletConfig.upperBarrelAction.displayName, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            DropdownMenu(
                                expanded = showUpperBarrelMenu,
                                onDismissRequest = { showUpperBarrelMenu = false }
                            ) {
                                allTabletActions.forEach { action ->
                                    DropdownMenuItem(
                                        text = { Text(action.displayName) },
                                        onClick = {
                                            viewModel.updateTabletConfig(tabletConfig.copy(upperBarrelAction = action))
                                            showUpperBarrelMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Swap Barrel Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Swap Stylus Barrel Buttons (Upper ↔ Lower)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Invert stylus barrel buttons if your physical pen's buttons operate in reverse.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tabletConfig.swapBarrelButtons,
                            onCheckedChange = {
                                viewModel.updateTabletConfig(tabletConfig.copy(swapBarrelButtons = it))
                            }
                        )
                    }

                    // 5. XP-PEN Express Keys / Shortcut Buttons
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "XP-PEN Deco Mini 7 Express Keys (K1 - K8)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Customize each physical tablet Express Key. Keys are mapped in natural top-to-bottom order (K1 at top to K8 at bottom).",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Presets & Calibrate Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Reset to XP-PEN Deco Mini 7 V2 natural standard
                                val updatedMap = mapOf(
                                    1 to TabletAction.UNDO,
                                    2 to TabletAction.REDO,
                                    3 to TabletAction.HAND,
                                    4 to TabletAction.PEN,
                                    5 to TabletAction.AREA_ERASER,
                                    6 to TabletAction.STROKE_ERASER,
                                    7 to TabletAction.ZOOM_IN,
                                    8 to TabletAction.ZOOM_OUT
                                )
                                viewModel.updateTabletConfig(tabletConfig.copy(
                                    expressKeyMappings = updatedMap,
                                    customKeyCodes = emptyMap(),
                                    customScanCodes = emptyMap()
                                ))
                                Toast.makeText(context, "Applied Deco Mini 7 standard preset", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("Reset Deco Preset", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                // Clear all custom key overrides so defaults take full effect
                                viewModel.updateTabletConfig(tabletConfig.copy(
                                    customKeyCodes = emptyMap(),
                                    customScanCodes = emptyMap()
                                ))
                                Toast.makeText(context, "Cleared all custom key overrides", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("Clear Overrides", fontSize = 10.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..8).forEach { index ->
                            var showKeyMenu by remember { mutableStateOf(false) }
                            val currentAction = tabletConfig.expressKeyMappings[index] ?: TabletAction.NONE
                            val hasCustomKey = tabletConfig.customKeyCodes[index] != null

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val hasCustomScan = tabletConfig.customScanCodes[index] != null
                                    Column {
                                        Text("Express Key K$index", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        if (hasCustomKey || hasCustomScan) {
                                            val keyDesc = if (hasCustomKey) "KeyCode ${tabletConfig.customKeyCodes[index]}" else ""
                                            val scanDesc = if (hasCustomScan) "Scan ${tabletConfig.customScanCodes[index]}" else ""
                                            Text(
                                                "Bound: ${listOf(keyDesc, scanDesc).filter { it.isNotEmpty() }.joinToString(" • ")}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (hasCustomKey || hasCustomScan) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.clearExpressKeyBinding(index)
                                                    Toast.makeText(context, "Cleared custom binding for K$index", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Clear Custom Key",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Box {
                                            TextButton(
                                                onClick = { showKeyMenu = true },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(currentAction.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                            DropdownMenu(
                                                expanded = showKeyMenu,
                                                onDismissRequest = { showKeyMenu = false }
                                            ) {
                                                allTabletActions.forEach { action ->
                                                    DropdownMenuItem(
                                                        text = { Text(action.displayName) },
                                                        onClick = {
                                                            val updatedMap = tabletConfig.expressKeyMappings.toMutableMap()
                                                            updatedMap[index] = action
                                                            viewModel.updateTabletConfig(tabletConfig.copy(expressKeyMappings = updatedMap))
                                                            showKeyMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Reset Express Keys
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.updateTabletConfig(TabletConfig())
                                Toast.makeText(context, "Tablet settings reset to default", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Tablet Config", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Section 3: ChatGPT Prompt Customization
            SectionHeader(title = "ChatGPT NEET Checking Prompt", icon = Icons.Default.Psychology)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "This prompt is automatically copied to your clipboard whenever you tap 'Send to ChatGPT':",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                customPrompt = ExportManager.DEFAULT_NEET_PROMPT
                                Toast.makeText(context, "Reset prompt to default", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset to Default", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 4: Privacy & Offline Guarantee
            SectionHeader(title = "Privacy & Local Storage", icon = Icons.Default.Lock)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF166534),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "100% Offline & Private",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "InkKey operates offline and stores all your handwriting, diagrams, and formulas locally on your device. No strokes are ever uploaded to cloud servers without your explicit action.",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Section 5: Export Location & Files
            SectionHeader(title = "Export Location", icon = Icons.Default.Folder)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isCustomLocation) Icons.Default.FolderOpen else Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Export Location",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isCustomLocation) "Custom Folder (Visible in File Manager)" else "Default Internal Storage",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = exportDirDisplay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = exportDirAbsolute,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    // Storage Action Buttons: Change Folder & Restore Default
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isCustomLocation) "Change Folder" else "Choose Folder", fontSize = 12.sp)
                        }

                        if (isCustomLocation) {
                            OutlinedButton(
                                onClick = {
                                    ExportManager.resetToDefaultExportLocation(context)
                                    refreshTrigger++
                                    Toast.makeText(context, "Restored default export location", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Default", fontSize = 12.sp)
                            }
                        }
                    }

                    Text(
                        text = "Exports saved to custom folders appear directly in your phone's File Manager (Downloads, Documents, etc.).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Exported Files List Section
                    Text(
                        text = if (exportedFiles.isEmpty()) "Saved Exported Files (0)" else "Saved Exported Files (${exportedFiles.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (exportedFiles.isEmpty()) {
                        Text(
                            text = "No PNG or PDF exports yet. Exported solutions from the canvas or keyboard will appear here for easy opening, sharing, or deletion.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            exportedFiles.take(12).forEach { item ->
                                val sizeKb = (item.sizeBytes / 1024).coerceAtLeast(1)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (item.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = if (item.isPdf) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${sizeKb} KB",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            IconButton(
                                                onClick = { ExportManager.openExportedItem(context, item) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.OpenInNew,
                                                    contentDescription = "Open file",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { ExportManager.shareExportedItem(context, item) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = "Share file",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    val deleted = ExportManager.deleteExportItem(context, item)
                                                    if (deleted) {
                                                        Toast.makeText(context, "Deleted ${item.name}", Toast.LENGTH_SHORT).show()
                                                        refreshTrigger++
                                                    } else {
                                                        Toast.makeText(context, "Could not delete ${item.name}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete file",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 6: Data Management (Clear All Data & Exported Files)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Delete All Saved Answers", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Permanently erases all local answers, strokes, and exported PNG/PDF files.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showClearAllDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", fontSize = 12.sp)
                    }
                }
            }

            // App Version Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "InkKey v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE}) • XP-PEN & NEET Edition",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Dialog: Delete All Data Confirmation
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Delete All Saved Answers & Exports?") },
            text = { Text("Are you sure? This will remove all handwritten answers and pages from the database AND delete all exported PNG/PDF files from device storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData(context) { deletedFilesCount, error ->
                            if (error != null) {
                                Toast.makeText(context, "Deletion error: $error", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Deleted all answers & $deletedFilesCount exported files", Toast.LENGTH_SHORT).show()
                                refreshTrigger++
                            }
                            showClearAllDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

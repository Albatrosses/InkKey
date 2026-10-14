package com.example.ime.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import android.os.Build
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.export.ExportManager
import com.example.ime.data.ImeDocumentRepository
import com.example.ime.data.KeyboardHeightMode
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.NeetSubject
import com.example.model.PaperStyle
import com.example.model.ShapeType
import com.example.model.TabletAction
import com.example.model.TextAnnotation
import com.example.model.ToolType
import com.example.model.ToolbarToolItem
import com.example.util.ShapeRecognizer
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.max

// Curated NEET math, science, and formula symbols organized by discipline
private val NEET_SYMBOL_CATEGORIES = mapOf(
    "Physics" to listOf("Δ", "θ", "λ", "α", "β", "π", "ω", "Ω", "μ", "F⃗", "v⃗", "a⃗", "10⁻⁶", "°C", "kΩ"),
    "Chemistry" to listOf("→", "⇌", "↑", "↓", "pH", "[H⁺]", "[OH⁻]", "ΔH", "H₂O", "H₂SO₄", "e⁻", "mol/L"),
    "Biology" to listOf("♀", "♂", "DNA", "RNA", "ATP", "CO₂", "O₂", "C₆H₁₂O₆", "μm", "bp", "5′", "3′"),
    "Math" to listOf("√", "∫", "∑", "±", "∞", "≠", "≈", "≤", "≥", "½", "¼", "dx", "dy/dx", "ln", "log")
)

private val PEN_COLORS = listOf(
    0xFF0F172AL to "Charcoal",
    0xFF1D4ED8L to "Blue",
    0xFFDC2626L to "Red",
    0xFF16A34AL to "Green",
    0xFF7C3AEDL to "Purple",
    0xFFF8FAFCL to "White Chalk"
)

private val STROKE_WIDTH_OPTIONS = listOf(
    2f to "Fine (2pt)",
    4f to "Normal (4pt)",
    8f to "Medium (8pt)",
    14f to "Bold (14pt)"
)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KeyboardCanvasView(
    onCommitText: (String) -> Unit,
    onCommitImage: (File, Bitmap) -> Unit,
    onSendEditorAction: () -> Unit,
    onDeleteLastChar: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ImeDocumentRepository.getInstance(context) }

    val document by repository.document.collectAsState()
    val currentPageIndex by repository.currentPageIndex.collectAsState()
    val currentTool by repository.currentTool.collectAsState()
    val penColor by repository.penColor.collectAsState()
    val strokeWidth by repository.strokeWidth.collectAsState()
    val highlighterColor by repository.highlighterColor.collectAsState()
    val eraserRadius by repository.eraserRadius.collectAsState()
    val heightMode by repository.heightMode.collectAsState()
    val selectedSymbolId by repository.selectedSymbolId.collectAsState()
    val toolbarItems by repository.toolbarItems.collectAsState()
    val canUndo by repository.canUndo.collectAsState()
    val canRedo by repository.canRedo.collectAsState()
    val tabletConfig by repository.tabletConfig.collectAsState()
    val tabletFeedback by repository.tabletFeedbackMessage.collectAsState()

    val currentPage = document.pages.getOrElse(currentPageIndex) {
        document.pages.firstOrNull() ?: com.example.model.PageData(0)
    }

    // Viewport Pan & Zoom
    var panOffset by remember { mutableStateOf(DrawingPoint(0f, 0f)) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    // Live stroke currently being drawn
    var liveStroke by remember { mutableStateOf<DrawingStroke?>(null) }

    // Menus and dialog states
    var showColorPicker by remember { mutableStateOf(false) }
    var showWidthPicker by remember { mutableStateOf(false) }
    var showPaperMenu by remember { mutableStateOf(false) }
    var showSubjectMenu by remember { mutableStateOf(false) }
    var showClearPageConfirm by remember { mutableStateOf(false) }
    var showDeletePageConfirm by remember { mutableStateOf(false) }
    var showToolbarCustomizeDialog by remember { mutableStateOf(false) }
    var showEraserSizePicker by remember { mutableStateOf(false) }
    var showSymbolsDrawer by remember { mutableStateOf(false) }
    var showTabletQuickBar by remember { mutableStateOf(false) }
    var selectedSymbolCategory by remember { mutableStateOf("Physics") }
    var symbolPlacementMode by remember { mutableStateOf(false) } // false: places movable object on canvas; true: types symbol into chat

    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var shapeSnappingEnabled by remember { mutableStateOf(false) }

    fun retractTransientPanels() {
        showColorPicker = false
        showWidthPicker = false
        showPaperMenu = false
        showSubjectMenu = false
        showEraserSizePicker = false
        showSymbolsDrawer = false
        showTabletQuickBar = false
        notificationMessage = null
    }

    val hasTransientPanel = showColorPicker || showWidthPicker || showPaperMenu ||
        showSubjectMenu || showEraserSizePicker || showSymbolsDrawer || showTabletQuickBar ||
        notificationMessage != null

    LaunchedEffect(hasTransientPanel, notificationMessage) {
        if (hasTransientPanel) {
            delay(2000L)
            retractTransientPanels()
        }
    }

    LaunchedEffect(Unit) {
        repository.uiTabletActions.collect { action ->
            when (action) {
                TabletAction.ZOOM_IN -> zoomScale = (zoomScale * 1.25f).coerceAtMost(3.0f)
                TabletAction.ZOOM_OUT -> zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.6f)
                TabletAction.RESET_ZOOM -> {
                    zoomScale = 1f
                    panOffset = DrawingPoint(0f, 0f)
                }
                TabletAction.TOGGLE_TOOLBAR -> showTabletQuickBar = !showTabletQuickBar
                TabletAction.TOGGLE_FULLSCREEN -> {
                    notificationMessage = "Fullscreen action is available in the main InkKey workspace"
                }
                else -> Unit
            }
        }
    }

    // Selected Symbol drag tracking
    var isDraggingSymbol by remember { mutableStateOf(false) }
    var lastSymbolTouchPoint by remember { mutableStateOf<Offset?>(null) }

    // Determine keyboard height based on height mode (Compact, Expanded, Maximized)
    val keyboardHeight = when (heightMode) {
        KeyboardHeightMode.COMPACT -> 285.dp
        KeyboardHeightMode.EXPANDED -> 480.dp
        KeyboardHeightMode.MAXIMIZED -> 620.dp
    }

    fun exportAndCommitAnswer() {
        if (currentPage.strokes.isEmpty() && currentPage.textAnnotations.isEmpty()) {
            Toast.makeText(context, "Canvas is empty! Write an answer first.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val bitmap = ExportManager.renderPageToBitmap(
                page = currentPage,
                document = document,
                totalPages = document.pages.size,
                scale = 2.0f,
                forceWhiteBackground = false,
                minWidth = 900,
                minHeight = 560
            )

            val file = ExportManager.saveSingleImage(
                context = context,
                bitmap = bitmap,
                baseName = "InkKey_NEET_Answer_${System.currentTimeMillis()}"
            )

            onCommitImage(file, bitmap)
            try {
                bitmap.recycle()
            } catch (ignored: Exception) {}
            notificationMessage = "Answer ready! Delivered to active app or copied to clipboard."
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper: Precise hit testing using actual rendered symbol glyph bounds and orientation
    fun hitTestSymbol(sym: TextAnnotation, cx: Float, cy: Float, padding: Float = 8f): Boolean {
        return sym.hitTest(cx, cy, padding)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(keyboardHeight)
            .testTag("inkkey_ime_surface"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // =========================================================================
            // ROW 1: PRIMARY UTILITY & NAVIGATION BAR
            // =========================================================================
            // =========================================================================
            // ROW 1: PRIMARY UTILITY & NAVIGATION BAR
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Switch IME
                IconButton(
                    onClick = onSwitchKeyboard,
                    modifier = Modifier.size(28.dp).testTag("ime_switch_keyboard_btn")
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = "Switch Keyboard", modifier = Modifier.size(16.dp))
                }

                // Subject Badge
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(document.subject.colorHex),
                        modifier = Modifier.clickable { showSubjectMenu = true }
                    ) {
                        Text(
                            text = document.subject.displayName,
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSubjectMenu,
                        onDismissRequest = { showSubjectMenu = false }
                    ) {
                        NeetSubject.values().forEach { subj ->
                            DropdownMenuItem(
                                text = { Text(subj.displayName, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    repository.setSubject(subj)
                                    showSubjectMenu = false
                                }
                            )
                        }
                    }
                }

                // Page Navigation: Prev, Page X of Y, Next, Add
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                    IconButton(
                        onClick = { repository.goToPage(currentPageIndex - 1) },
                        enabled = currentPageIndex > 0,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Page", modifier = Modifier.size(12.dp))
                    }

                    Text(
                        text = "${currentPageIndex + 1}/${document.pages.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    IconButton(
                        onClick = { repository.goToPage(currentPageIndex + 1) },
                        enabled = currentPageIndex < document.pages.size - 1,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page", modifier = Modifier.size(12.dp))
                    }

                    IconButton(
                        onClick = { repository.addPage() },
                        modifier = Modifier.size(24.dp).testTag("ime_add_page_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Page", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                }

                // Dedicated Clear Page button - clearly visible with label and icon
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clickable { showClearPageConfirm = true }
                        .testTag("ime_clear_page_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Page", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Clear Page", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }

                // Dedicated Delete Page button - clearly visible with label and icon
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clickable {
                            if (document.pages.size <= 1) {
                                showClearPageConfirm = true
                            } else {
                                showDeletePageConfirm = true
                            }
                        }
                        .testTag("ime_delete_page_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Delete Page", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }

                // Undo
                IconButton(
                    onClick = { repository.undo() },
                    enabled = canUndo,
                    modifier = Modifier.size(28.dp).testTag("ime_undo_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = { repository.redo() },
                    enabled = canRedo,
                    modifier = Modifier.size(28.dp).testTag("ime_redo_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Keyboard Height Multi-State Toggle
                IconButton(
                    onClick = {
                        val nextMode = when (heightMode) {
                            KeyboardHeightMode.COMPACT -> KeyboardHeightMode.EXPANDED
                            KeyboardHeightMode.EXPANDED -> KeyboardHeightMode.MAXIMIZED
                            KeyboardHeightMode.MAXIMIZED -> KeyboardHeightMode.COMPACT
                        }
                        repository.setHeightMode(nextMode)
                    },
                    modifier = Modifier.size(28.dp).testTag("ime_expand_toggle_btn")
                ) {
                    val icon = when (heightMode) {
                        KeyboardHeightMode.COMPACT -> Icons.Default.ExpandLess
                        KeyboardHeightMode.EXPANDED -> Icons.Default.Fullscreen
                        KeyboardHeightMode.MAXIMIZED -> Icons.Default.FullscreenExit
                    }
                    Icon(icon, contentDescription = "Adjust Keyboard Height", modifier = Modifier.size(16.dp))
                }

                // Customize / Exchange Toolbar Buttons
                IconButton(
                    onClick = { showToolbarCustomizeDialog = true },
                    modifier = Modifier.size(28.dp).testTag("ime_customize_toolbar_btn")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Customize Toolbar Layout", modifier = Modifier.size(16.dp))
                }

                // Tablet quick controls restored from the clean APK behavior.
                IconButton(
                    onClick = { showTabletQuickBar = !showTabletQuickBar },
                    modifier = Modifier.size(28.dp).testTag("ime_tablet_quick_toggle")
                ) {
                    Icon(
                        Icons.Default.Brush,
                        contentDescription = "Tablet Quick Controls",
                        tint = if (showTabletQuickBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Direct Settings Button (Starts ImeSettingsActivity cleanly)
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(context, com.example.ui.settings.ImeSettingsActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Settings error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(28.dp).testTag("ime_toolbar_settings_btn")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Keyboard Settings", modifier = Modifier.size(16.dp))
                }
            }

            AnimatedVisibility(visible = showTabletQuickBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("Tablet", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { repository.updateTabletConfig(tabletConfig.copy(stylusOnlyDrawing = !tabletConfig.stylusOnlyDrawing)) },
                        modifier = Modifier.height(28.dp)
                    ) { Text("Stylus ${if (tabletConfig.stylusOnlyDrawing) "Only" else "All"}", fontSize = 9.sp) }
                    TextButton(
                        onClick = { repository.updateTabletConfig(tabletConfig.copy(pressureEnabled = !tabletConfig.pressureEnabled)) },
                        modifier = Modifier.height(28.dp)
                    ) { Text("Pressure ${if (tabletConfig.pressureEnabled) "On" else "Off"}", fontSize = 9.sp) }
                    TextButton(
                        onClick = { repository.updateTabletConfig(tabletConfig.copy(stabilizationEnabled = !tabletConfig.stabilizationEnabled)) },
                        modifier = Modifier.height(28.dp)
                    ) { Text("Stabilize ${if (tabletConfig.stabilizationEnabled) "On" else "Off"}", fontSize = 9.sp) }
                    TextButton(
                        onClick = { repository.updateTabletConfig(tabletConfig.copy(swapBarrelButtons = !tabletConfig.swapBarrelButtons)) },
                        modifier = Modifier.height(28.dp)
                    ) { Text("Swap ${if (tabletConfig.swapBarrelButtons) "On" else "Off"}", fontSize = 9.sp) }
                    Text("L: ${tabletConfig.lowerBarrelAction.displayName.removePrefix("Switch to ")}", fontSize = 9.sp)
                    Text("U: ${tabletConfig.upperBarrelAction.displayName.removePrefix("Switch to ")}", fontSize = 9.sp)
                    tabletFeedback?.let { feedback ->
                        Text(feedback, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(context, com.example.ui.settings.ImeSettingsActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            } catch (_: Exception) { }
                        },
                        modifier = Modifier.height(28.dp)
                    ) { Text("Full Settings", fontSize = 9.sp) }
                }
            }

            // =========================================================================
            // ROW 2: CUSTOMIZABLE DRAWING TOOLS ROW
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dynamically render tools in user-customized order
                for (item in toolbarItems) {
                    when (item) {
                        ToolbarToolItem.PEN -> {
                            val isPen = currentTool == ToolType.PEN
                            Surface(
                                shape = CircleShape,
                                color = if (isPen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        onClick = {
                                            repository.setTool(ToolType.PEN)
                                            showColorPicker = false
                                            showWidthPicker = false
                                            showEraserSizePicker = false
                                        },
                                        onLongClick = {
                                            repository.setTool(ToolType.PEN)
                                            showColorPicker = !showColorPicker
                                            showWidthPicker = false
                                            showEraserSizePicker = false
                                        }
                                    )
                                    .testTag("ime_pen_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Brush,
                                        contentDescription = "Pen Tool",
                                        tint = if (isPen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        ToolbarToolItem.STROKE_ERASER -> {
                            val isStrokeEraser = currentTool == ToolType.STROKE_ERASER
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isStrokeEraser) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = {
                                            repository.setTool(ToolType.STROKE_ERASER)
                                            showEraserSizePicker = false
                                            showColorPicker = false
                                            showWidthPicker = false
                                        },
                                        onLongClick = {
                                            repository.setTool(ToolType.STROKE_ERASER)
                                            showEraserSizePicker = !showEraserSizePicker
                                        }
                                    )
                                    .testTag("ime_stroke_eraser_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_eraser),
                                        contentDescription = "Stroke Eraser",
                                        tint = if (isStrokeEraser) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Eraser",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStrokeEraser) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        ToolbarToolItem.AREA_ERASER -> {
                            val isAreaEraser = currentTool == ToolType.AREA_ERASER
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAreaEraser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = {
                                            repository.setTool(ToolType.AREA_ERASER)
                                            showEraserSizePicker = false
                                            showColorPicker = false
                                            showWidthPicker = false
                                        },
                                        onLongClick = {
                                            repository.setTool(ToolType.AREA_ERASER)
                                            showEraserSizePicker = !showEraserSizePicker
                                        }
                                    )
                                    .testTag("ime_area_eraser_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = "Area Eraser",
                                        tint = if (isAreaEraser) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Area",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAreaEraser) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        ToolbarToolItem.HIGHLIGHTER -> {
                            FilledTonalIconButton(
                                onClick = {
                                    repository.setTool(ToolType.HIGHLIGHTER)
                                    showColorPicker = false
                                    showWidthPicker = false
                                    showEraserSizePicker = false
                                },
                                modifier = Modifier.size(32.dp).testTag("ime_highlighter_btn"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (currentTool == ToolType.HIGHLIGHTER) Color(0xFFFBBF24) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if (currentTool == ToolType.HIGHLIGHTER) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Highlighter", modifier = Modifier.size(16.dp))
                            }
                        }

                        ToolbarToolItem.HAND -> {
                            FilledTonalIconButton(
                                onClick = {
                                    repository.setTool(ToolType.HAND)
                                    showColorPicker = false
                                    showWidthPicker = false
                                    showEraserSizePicker = false
                                },
                                modifier = Modifier.size(32.dp).testTag("ime_hand_btn"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (currentTool == ToolType.HAND) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if (currentTool == ToolType.HAND) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.PanTool, contentDescription = "Pan Canvas", modifier = Modifier.size(16.dp))
                            }
                        }

                        ToolbarToolItem.PALETTE -> {
                            // Color circle & stroke width pill
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(penColor))
                                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .clickable {
                                            showColorPicker = !showColorPicker
                                            showWidthPicker = false
                                            showEraserSizePicker = false
                                        }
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.clickable {
                                        showWidthPicker = !showWidthPicker
                                        showColorPicker = false
                                        showEraserSizePicker = false
                                    }
                                ) {
                                    Text(
                                        text = "${strokeWidth.toInt()}pt",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        ToolbarToolItem.SYMBOLS -> {
                            FilledTonalButton(
                                onClick = { showSymbolsDrawer = !showSymbolsDrawer },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp).testTag("ime_symbols_drawer_btn")
                            ) {
                                Text("Σ Symbols", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        ToolbarToolItem.THEMES -> {
                            Box {
                                IconButton(
                                    onClick = { showPaperMenu = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Palette, contentDescription = "Paper Style", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                                }

                                DropdownMenu(
                                    expanded = showPaperMenu,
                                    onDismissRequest = { showPaperMenu = false }
                                ) {
                                    PaperStyle.values().forEach { style ->
                                        DropdownMenuItem(
                                            text = { Text(style.displayName) },
                                            onClick = {
                                                repository.setPaperStyle(style)
                                                showPaperMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        ToolbarToolItem.CLEAR -> {
                            IconButton(
                                onClick = { showClearPageConfirm = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Page", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
                            }
                        }

                        ToolbarToolItem.UNDO_REDO -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { repository.undo() },
                                    enabled = canUndo,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { repository.redo() },
                                    enabled = canRedo,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(15.dp))
                                }
                            }
                        }

                        ToolbarToolItem.PAGES -> {
                            FilledTonalButton(
                                onClick = { repository.addPage() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Page", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        ToolbarToolItem.SHAPE -> {
                            FilledTonalIconButton(
                                onClick = {
                                    shapeSnappingEnabled = !shapeSnappingEnabled
                                    retractTransientPanels()
                                    notificationMessage = if (shapeSnappingEnabled) {
                                        "Shape auto-detect ON"
                                    } else {
                                        "Shape auto-detect OFF"
                                    }
                                },
                                modifier = Modifier.size(32.dp).testTag("ime_shape_snap_btn"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (shapeSnappingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if (shapeSnappingEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.Category, contentDescription = "Shape auto-detect", modifier = Modifier.size(16.dp))
                            }
                        }

                        else -> Unit
                    }
                }

                // Zoom controls
                IconButton(
                    onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(3.0f) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.6f) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                }

                if (zoomScale != 1f || panOffset.x != 0f || panOffset.y != 0f) {
                    TextButton(
                        onClick = {
                            zoomScale = 1f
                            panOffset = DrawingPoint(0f, 0f)
                        },
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Reset", fontSize = 10.sp)
                    }
                }

                // Dedicated Reorder Tools Button in Drawing Bar
                FilledTonalIconButton(
                    onClick = { showToolbarCustomizeDialog = true },
                    modifier = Modifier.size(30.dp).testTag("ime_customize_toolbar_btn"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Reorder Tools", modifier = Modifier.size(15.dp))
                }
            }

            // Eraser Controls Sub-Bar (Active when explicitly opened via long press on eraser)
            AnimatedVisibility(visible = showEraserSizePicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Eraser:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (currentTool == ToolType.STROKE_ERASER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable { repository.setTool(ToolType.STROKE_ERASER) }
                        ) {
                            Text(
                                text = "Stroke",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTool == ToolType.STROKE_ERASER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (currentTool == ToolType.AREA_ERASER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable { repository.setTool(ToolType.AREA_ERASER) }
                        ) {
                            Text(
                                text = "Area",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTool == ToolType.AREA_ERASER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Size:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        listOf(
                            16f to "S",
                            28f to "M",
                            48f to "L"
                        ).forEach { (rad, lbl) ->
                            val isSel = eraserRadius == rad
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(24.dp).clickable { repository.setEraserRadius(rad) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = lbl,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Quick Clear Canvas in eraser sub-bar
                        TextButton(
                            onClick = { showClearPageConfirm = true },
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Clear", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Expandable Pen Color Picker Palette
            AnimatedVisibility(visible = showColorPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ink Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    PEN_COLORS.forEach { (colorHex, _) ->
                        val isSelected = penColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    repository.setPenColor(colorHex)
                                    showColorPicker = false
                                }
                        )
                    }
                }
            }

            // Expandable Stroke Width Picker
            AnimatedVisibility(visible = showWidthPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thickness:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    STROKE_WIDTH_OPTIONS.forEach { (width, label) ->
                        val isSelected = strokeWidth == width
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable {
                                repository.setStrokeWidth(width)
                                showWidthPicker = false
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Expandable Symbols Drawer
            AnimatedVisibility(visible = showSymbolsDrawer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // Category Selector & Placement Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NEET_SYMBOL_CATEGORIES.keys.forEach { cat ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedSymbolCategory == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.clickable { selectedSymbolCategory = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedSymbolCategory == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Toggle: Place as Movable Object on Canvas vs Type into Chat
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable { symbolPlacementMode = !symbolPlacementMode }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (symbolPlacementMode) "⌨ To Chat" else "📍 On Canvas",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Symbol pills row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val symbols = NEET_SYMBOL_CATEGORIES[selectedSymbolCategory] ?: emptyList()
                        symbols.forEach { sym ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.clickable {
                                    if (symbolPlacementMode) {
                                        // Type directly into ChatGPT message composer
                                        onCommitText(sym)
                                    } else {
                                        // DEFAULT: Place as movable, selectable object on canvas
                                        val placeX = (-panOffset.x + 120f) / zoomScale
                                        val placeY = (-panOffset.y + 140f) / zoomScale
                                        repository.insertSymbolOnCanvas(sym, placeX, placeY)
                                        notificationMessage = "Symbol $sym placed! Drag to move or use handles."
                                    }
                                }
                            ) {
                                Text(
                                    text = sym,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active Symbol Edit Toolbar (Resize, Rotate, Move Nudge, Duplicate, Delete, Done)
            AnimatedVisibility(visible = currentTool == ToolType.SYMBOL_EDIT && selectedSymbolId != null) {
                val activeSym = currentPage.textAnnotations.find { it.id == selectedSymbolId }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Symbol: ${activeSym?.text ?: ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )

                    // Font Size Minus
                    IconButton(
                        onClick = { repository.resizeSelectedSymbol(-4f) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease Font Size", tint = Color(0xFF92400E), modifier = Modifier.size(14.dp))
                    }

                    // Font Size Plus
                    IconButton(
                        onClick = { repository.resizeSelectedSymbol(4f) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase Font Size", tint = Color(0xFF92400E), modifier = Modifier.size(14.dp))
                    }

                    // Move Left
                    IconButton(
                        onClick = {
                            repository.moveSelectedSymbol(-25f, 0f)
                            repository.finishMoveSymbol()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", tint = Color(0xFF92400E), modifier = Modifier.size(14.dp))
                    }

                    // Move Right
                    IconButton(
                        onClick = {
                            repository.moveSelectedSymbol(25f, 0f)
                            repository.finishMoveSymbol()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right", tint = Color(0xFF92400E), modifier = Modifier.size(14.dp))
                    }

                    // Move Up
                    IconButton(
                        onClick = {
                            repository.moveSelectedSymbol(0f, -25f)
                            repository.finishMoveSymbol()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.ExpandLess, contentDescription = "Move Up", tint = Color(0xFF92400E), modifier = Modifier.size(16.dp))
                    }

                    // Move Down
                    IconButton(
                        onClick = {
                            repository.moveSelectedSymbol(0f, 25f)
                            repository.finishMoveSymbol()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.ExpandMore, contentDescription = "Move Down", tint = Color(0xFF92400E), modifier = Modifier.size(16.dp))
                    }

                    // Rotate 45°
                    IconButton(
                        onClick = { repository.rotateSelectedSymbol(45f) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate 45°", tint = Color(0xFF92400E), modifier = Modifier.size(14.dp))
                    }

                    // Duplicate Symbol
                    IconButton(
                        onClick = { repository.duplicateSelectedSymbol() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Symbol", tint = Color(0xFF92400E), modifier = Modifier.size(14.dp))
                    }

                    // Delete Symbol
                    IconButton(
                        onClick = { repository.deleteSelectedSymbol() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Symbol", tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                    }

                    // Done Checkmark
                    FilledTonalButton(
                        onClick = {
                            repository.finishMoveSymbol()
                            repository.selectSymbol(null)
                            repository.setTool(ToolType.PEN)
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Notification Banner if present
            AnimatedVisibility(visible = notificationMessage != null) {
                notificationMessage?.let { msg ->
                    Surface(
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.fillMaxWidth().clickable { notificationMessage = null }
                    ) {
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // =========================================================================
            // MAIN HANDWRITING CANVAS SURFACE
            // =========================================================================
            val imeCanvasBg = when (document.paperStyle) {
                PaperStyle.AMOLED_BLACK -> Color(0xFF000000)
                PaperStyle.DARK_MODE -> Color(0xFF0F172A)
                PaperStyle.BLACKBOARD -> Color(0xFF142019)
                PaperStyle.SEPIA -> Color(0xFFFBF5E8)
                PaperStyle.RULED -> Color(0xFFFEFDF9)
                PaperStyle.GRAPH -> Color(0xFFF8FAFC)
                PaperStyle.PLAIN_WHITE -> Color.White
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(imeCanvasBg)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ime_handwriting_canvas")
                        .pointerInteropFilter { motionEvent ->
                            val toolType = motionEvent.getToolType(0)
                            val isStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER
                            val isEraserHardware = toolType == MotionEvent.TOOL_TYPE_ERASER

                            // Palm Rejection: if finger touch size is abnormally large while writing
                            if (!isStylus && motionEvent.size > 0.55f && currentTool != ToolType.HAND) {
                                return@pointerInteropFilter true
                            }

                            val rawX = motionEvent.x
                            val rawY = motionEvent.y
                            val canvasX = (rawX - panOffset.x) / zoomScale
                            val canvasY = (rawY - panOffset.y) / zoomScale
                            val pressure = motionEvent.pressure

                            // Routed once at the IME root before Compose dispatch.
                            val activeTool = repository.transientBarrelTool.value ?: currentTool

                            when (motionEvent.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    // Writing immediately retracts transient drawers/banners so the canvas gets its space back.
                                    if (activeTool != ToolType.HAND) retractTransientPanels()

                                    // 1. Symbol selection priority: tapping any placed symbol selects it and enables moving
                                    if (activeTool != ToolType.HAND) {
                                        val currentlySelected = currentPage.textAnnotations.find { it.id == selectedSymbolId }
                                        val hitSymbol = if (currentlySelected != null && hitTestSymbol(currentlySelected, canvasX, canvasY, padding = 12f)) {
                                            currentlySelected
                                        } else {
                                            currentPage.textAnnotations.findLast { sym ->
                                                hitTestSymbol(sym, canvasX, canvasY, padding = 22f)
                                            }
                                        }

                                        if (hitSymbol != null) {
                                            repository.selectSymbol(hitSymbol.id)
                                            repository.setTool(ToolType.SYMBOL_EDIT)
                                            isDraggingSymbol = true
                                            lastSymbolTouchPoint = Offset(canvasX, canvasY)
                                            return@pointerInteropFilter true
                                        }
                                    }

                                    when (activeTool) {
                                        ToolType.HAND -> {
                                            lastSymbolTouchPoint = Offset(rawX, rawY)
                                        }
                                        ToolType.SYMBOL_EDIT -> {
                                            // Check if user tapped inside or directly on the selected symbol/handles to drag it
                                            val activeSym = currentPage.textAnnotations.find { it.id == selectedSymbolId }
                                            if (activeSym != null && hitTestSymbol(activeSym, canvasX, canvasY, padding = 14f)) {
                                                isDraggingSymbol = true
                                                lastSymbolTouchPoint = Offset(canvasX, canvasY)
                                                return@pointerInteropFilter true
                                            }
                                            // Tapped empty space far from selected symbol: deselect without drawing
                                            repository.finishMoveSymbol()
                                            repository.selectSymbol(null)
                                            repository.setTool(ToolType.PEN)
                                            return@pointerInteropFilter true
                                        }
                                        ToolType.STROKE_ERASER -> {
                                            repository.startEraseGesture()
                                            repository.eraseStrokesAt(canvasX, canvasY, repository.eraserRadius.value)
                                        }
                                        ToolType.AREA_ERASER -> {
                                            repository.startEraseGesture()
                                            repository.eraseAreaStrokes(canvasX, canvasY, repository.eraserRadius.value)
                                        }
                                        else -> {
                                            // Pen / Highlighter: Start new stroke immediately
                                            val sColor = if (activeTool == ToolType.HIGHLIGHTER) highlighterColor else penColor
                                            val sWidth = if (activeTool == ToolType.HIGHLIGHTER) strokeWidth * 3.5f else strokeWidth
                                            val sAlpha = if (activeTool == ToolType.HIGHLIGHTER) 0.45f else 1.0f

                                            liveStroke = DrawingStroke(
                                                toolType = activeTool,
                                                color = sColor,
                                                width = sWidth,
                                                alpha = sAlpha,
                                                points = listOf(DrawingPoint(canvasX, canvasY, pressure))
                                            )
                                        }
                                    }
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    when (activeTool) {
                                        ToolType.HAND -> {
                                            lastSymbolTouchPoint?.let { prev ->
                                                val dx = rawX - prev.x
                                                val dy = rawY - prev.y
                                                panOffset = DrawingPoint(panOffset.x + dx, panOffset.y + dy)
                                                lastSymbolTouchPoint = Offset(rawX, rawY)
                                            }
                                        }
                                        ToolType.SYMBOL_EDIT -> {
                                            if (isDraggingSymbol && lastSymbolTouchPoint != null && selectedSymbolId != null) {
                                                val dx = canvasX - lastSymbolTouchPoint!!.x
                                                val dy = canvasY - lastSymbolTouchPoint!!.y
                                                repository.moveSelectedSymbol(dx, dy)
                                                lastSymbolTouchPoint = Offset(canvasX, canvasY)
                                            }
                                        }
                                        ToolType.STROKE_ERASER -> {
                                            val hSize = motionEvent.historySize
                                            for (h in 0 until hSize) {
                                                val hx = (motionEvent.getHistoricalX(h) - panOffset.x) / zoomScale
                                                val hy = (motionEvent.getHistoricalY(h) - panOffset.y) / zoomScale
                                                repository.eraseStrokesAt(hx, hy, repository.eraserRadius.value)
                                            }
                                            repository.eraseStrokesAt(canvasX, canvasY, repository.eraserRadius.value)
                                        }
                                        ToolType.AREA_ERASER -> {
                                            val hSize = motionEvent.historySize
                                            for (h in 0 until hSize) {
                                                val hx = (motionEvent.getHistoricalX(h) - panOffset.x) / zoomScale
                                                val hy = (motionEvent.getHistoricalY(h) - panOffset.y) / zoomScale
                                                repository.eraseAreaStrokes(hx, hy, repository.eraserRadius.value)
                                            }
                                            repository.eraseAreaStrokes(canvasX, canvasY, repository.eraserRadius.value)
                                        }
                                        else -> {
                                            val cur = liveStroke ?: return@pointerInteropFilter true
                                            val newPts = cur.points.toMutableList()

                                             val hSize = motionEvent.historySize
                                             for (h in 0 until hSize) {
                                                 val hx = (motionEvent.getHistoricalX(h) - panOffset.x) / zoomScale
                                                 val hy = (motionEvent.getHistoricalY(h) - panOffset.y) / zoomScale
                                                 val hp = motionEvent.getHistoricalPressure(h)
                                                 newPts.add(DrawingPoint(hx, hy, hp))
                                             }

                                            newPts.add(DrawingPoint(canvasX, canvasY, pressure))
                                            liveStroke = cur.copy(points = newPts)
                                        }
                                    }
                                }

                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    if (activeTool == ToolType.HAND) {
                                        lastSymbolTouchPoint = null
                                    } else if (activeTool == ToolType.SYMBOL_EDIT) {
                                        if (isDraggingSymbol) {
                                            repository.finishMoveSymbol()
                                        }
                                        isDraggingSymbol = false
                                        lastSymbolTouchPoint = null
                                    } else if (activeTool == ToolType.STROKE_ERASER || activeTool == ToolType.AREA_ERASER) {
                                        repository.endEraseGesture()
                                    } else {
                                        liveStroke?.let { finished ->
                                            if (finished.points.size >= 2) {
                                                val storedStroke = if (shapeSnappingEnabled) {
                                                    ShapeRecognizer.recognizeAndSnap(
                                                        finished.copy(isShape = true, shapeType = ShapeType.NONE)
                                                    )
                                                } else {
                                                    finished
                                                }
                                                repository.addStroke(storedStroke)
                                            }
                                        }
                                        liveStroke = null
                                        isDraggingSymbol = false
                                        lastSymbolTouchPoint = null
                                    }
                                }
                            }
                            true
                        }
                ) {
                    // Draw Paper Background
                    drawImePaperBackground(document.paperStyle, size, zoomScale, panOffset)

                    // Apply native Canvas transformation for strokes & text
                    val canvasNative = drawContext.canvas.nativeCanvas
                    canvasNative.save()
                    canvasNative.translate(panOffset.x, panOffset.y)
                    canvasNative.scale(zoomScale, zoomScale)

                    // 1. Draw Finished Strokes
                    for (stroke in currentPage.strokes) {
                        drawStrokeItem(stroke)
                    }

                    // 2. Draw Live Stroke
                    liveStroke?.let { drawStrokeItem(it) }

                    // 3. Draw Movable Symbols & Text Annotations
                    val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        isFakeBoldText = true
                    }

                    for (symbol in currentPage.textAnnotations) {
                        textPaint.textSize = symbol.fontSize
                        textPaint.color = symbol.color.toInt()

                        canvasNative.save()
                        canvasNative.translate(symbol.x, symbol.y)
                        if (symbol.rotation != 0f) {
                            canvasNative.rotate(symbol.rotation)
                        }
                        canvasNative.drawText(symbol.text, 0f, 0f, textPaint)
                        canvasNative.restore()

                        // Highlight selected symbol with dashed bounding box & handles in native coordinates
                        if (symbol.id == selectedSymbolId) {
                            val boundsPaint = AndroidPaint().apply { textSize = symbol.fontSize }
                            val textWidth = boundsPaint.measureText(symbol.text)
                            val textHeight = symbol.fontSize

                            val boxLeft = symbol.x - 6f
                            val boxTop = symbol.y - textHeight - 4f
                            val boxRight = symbol.x + textWidth + 8f
                            val boxBottom = symbol.y + 8f

                            val bgPaint = AndroidPaint().apply {
                                color = 0x33F59E0B.toInt()
                                style = android.graphics.Paint.Style.FILL
                            }
                            val borderPaint = AndroidPaint().apply {
                                color = 0xFFD97706.toInt()
                                style = android.graphics.Paint.Style.STROKE
                                setStrokeWidth(2f / zoomScale)
                                pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 6f), 0f)
                            }
                            canvasNative.drawRect(boxLeft, boxTop, boxRight, boxBottom, bgPaint)
                            canvasNative.drawRect(boxLeft, boxTop, boxRight, boxBottom, borderPaint)

                            // 4 Corner handle circles
                            val handlePaint = AndroidPaint().apply {
                                color = 0xFFD97706.toInt()
                                style = android.graphics.Paint.Style.FILL
                            }
                            val hRadius = 5f / zoomScale
                            canvasNative.drawCircle(boxLeft, boxTop, hRadius, handlePaint)
                            canvasNative.drawCircle(boxRight, boxTop, hRadius, handlePaint)
                            canvasNative.drawCircle(boxLeft, boxBottom, hRadius, handlePaint)
                            canvasNative.drawCircle(boxRight, boxBottom, hRadius, handlePaint)
                        }
                    }

                    canvasNative.restore()
                }
            }

            // =========================================================================
            // ROW 4: BOTTOM ACTION BAR (ChatGPT Send, Copy PNG, Backspace, Enter)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: 1-Tap Insert NEET Prompt button
                Button(
                    onClick = {
                        val promptText = "Please solve and verify this NEET question with step-by-step reasoning:"
                        onCommitText(promptText)
                        Toast.makeText(context, "NEET Prompt inserted!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                    modifier = Modifier.height(34.dp).testTag("ime_insert_prompt_btn")
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("NEET Prompt", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }

                // Middle: Universal 1-Tap Insert (Delivers to active app or copies to clipboard)
                Button(
                    onClick = { exportAndCommitAnswer() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                    modifier = Modifier.height(34.dp).testTag("ime_universal_insert_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Insert", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }

                // Right: Backspace and Enter keys
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = onDeleteLastChar,
                        modifier = Modifier.size(34.dp).testTag("ime_backspace_btn"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", modifier = Modifier.size(16.dp))
                    }

                    FilledTonalIconButton(
                        onClick = onSendEditorAction,
                        modifier = Modifier.size(34.dp).testTag("ime_enter_btn"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enter / Send", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // =========================================================================
        // IN-CANVAS MODALS (100% WindowManager crash-proof inside InputMethodService)
        // =========================================================================

        // Modal 1: Clear Page Confirmation
        if (showClearPageConfirm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showClearPageConfirm = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.92f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Clear Page ${currentPageIndex + 1}?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All handwriting and placed symbols on Page ${currentPageIndex + 1} will be cleared. Other pages are preserved. (Undo is supported)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showClearPageConfirm = false }) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    repository.clearCurrentPage()
                                    showClearPageConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Clear Page", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Modal 2: Delete Page Confirmation
        if (showDeletePageConfirm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showDeletePageConfirm = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.92f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Delete Page ${currentPageIndex + 1} of ${document.pages.size}?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This page will be deleted. All other pages will be preserved and renumbered. (Undo is supported)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showDeletePageConfirm = false }) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    repository.deleteCurrentPage()
                                    showDeletePageConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete Page", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Modal 3: Toolbar Customization & Preferences Overlay
        if (showToolbarCustomizeDialog) {
            val items = remember { mutableStateListOf<ToolbarToolItem>().apply { addAll(toolbarItems) } }
            var tabIndex by remember { mutableStateOf(0) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showToolbarCustomizeDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(0.96f)
                        .heightIn(max = 265.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Header with Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (tabIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { tabIndex = 0 }
                                ) {
                                    Text(
                                        "Reorder Buttons",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tabIndex == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (tabIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { tabIndex = 1 }
                                ) {
                                    Text(
                                        "Preferences",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tabIndex == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showToolbarCustomizeDialog = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Close", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (tabIndex == 0) {
                            // Reorder Tools Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items.forEachIndexed { index, tool ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${index + 1}. ${tool.title}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Row {
                                                 IconButton(
                                                    onClick = {
                                                        if (index > 0) {
                                                            val item = items.removeAt(index)
                                                            items.add(index - 1, item)
                                                        }
                                                    },
                                                    enabled = index > 0,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.ExpandLess, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        if (index < items.size - 1) {
                                                            val current = items[index]
                                                            items[index] = items[index + 1]
                                                            items[index + 1] = current
                                                        }
                                                    },
                                                    enabled = index < items.size - 1,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Tune, contentDescription = "Swap with Next", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        if (index < items.size - 1) {
                                                            val item = items.removeAt(index)
                                                            items.add(index + 1, item)
                                                        }
                                                    },
                                                    enabled = index < items.size - 1,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.ExpandMore, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        repository.resetToolbarOrder()
                                        showToolbarCustomizeDialog = false
                                    }
                                ) {
                                    Text("Reset Defaults", fontSize = 11.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { showToolbarCustomizeDialog = false }) {
                                        Text("Cancel", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            repository.updateToolbarOrder(items.toList())
                                            showToolbarCustomizeDialog = false
                                        }
                                    ) {
                                        Text("Save Order", fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            // Preferences Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Paper Background:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(PaperStyle.RULED, PaperStyle.GRAPH, PaperStyle.PLAIN_WHITE, PaperStyle.BLACKBOARD, PaperStyle.AMOLED_BLACK).forEach { style ->
                                        val isSel = document.paperStyle == style
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.weight(1f).clickable { repository.setPaperStyle(style) }
                                        ) {
                                            Text(
                                                text = if (style == PaperStyle.AMOLED_BLACK) "AMOLED" else style.displayName.take(7),
                                                fontSize = 9.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Text("Keyboard Height:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(
                                        KeyboardHeightMode.COMPACT to "Compact",
                                        KeyboardHeightMode.EXPANDED to "Expanded",
                                        KeyboardHeightMode.MAXIMIZED to "Maximized"
                                    ).forEach { (mode, label) ->
                                        val isSel = heightMode == mode
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.weight(1f).clickable { repository.setHeightMode(mode) }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 9.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(context, com.example.ui.settings.ImeSettingsActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val fallback = Intent(context, com.example.MainActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                putExtra("OPEN_SETTINGS", true)
                                            }
                                            context.startActivity(fallback)
                                        }
                                        showToolbarCustomizeDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Full App Settings Screen", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawImePaperBackground(
    paperStyle: PaperStyle,
    size: Size,
    zoom: Float,
    pan: DrawingPoint
) {
    when (paperStyle) {
        PaperStyle.RULED -> {
            drawRect(Color(0xFFFEFDF9))
            val spacing = 34f * zoom
            var y = (pan.y % spacing)
            while (y < size.height) {
                if (y >= 0) {
                    drawLine(Color(0xFFD4E0ED), Offset(0f, y), Offset(size.width, y), 1f)
                }
                y += spacing
            }
            // Vertical margin
            val marginX = 52f * zoom + pan.x
            if (marginX in 0f..size.width) {
                drawLine(Color(0xFFFCA5A5), Offset(marginX, 0f), Offset(marginX, size.height), 1.5f)
            }
        }
        PaperStyle.GRAPH -> {
            drawRect(Color(0xFFF8FAFC))
            val spacing = 26f * zoom
            var x = (pan.x % spacing)
            while (x < size.width) {
                if (x >= 0) drawLine(Color(0xFFE2E8F0), Offset(x, 0f), Offset(x, size.height), 0.8f)
                x += spacing
            }
            var y = (pan.y % spacing)
            while (y < size.height) {
                if (y >= 0) drawLine(Color(0xFFE2E8F0), Offset(0f, y), Offset(size.width, y), 0.8f)
                y += spacing
            }
        }
        PaperStyle.BLACKBOARD -> {
            drawRect(Color(0xFF142019))
        }
        PaperStyle.DARK_MODE -> {
            drawRect(Color(0xFF0F172A))
            val spacing = 32f * zoom
            var y = (pan.y % spacing)
            while (y < size.height) {
                if (y >= 0) drawLine(Color(0xFF1E293B), Offset(0f, y), Offset(size.width, y), 0.8f)
                y += spacing
            }
        }
        PaperStyle.SEPIA -> {
            drawRect(Color(0xFFFBF5E8))
            val spacing = 34f * zoom
            var y = (pan.y % spacing)
            while (y < size.height) {
                if (y >= 0) drawLine(Color(0xFFE6D8C3), Offset(0f, y), Offset(size.width, y), 1f)
                y += spacing
            }
        }
        PaperStyle.AMOLED_BLACK -> {
            drawRect(Color(0xFF000000)) // Pure AMOLED pitch black: strictly no ruling or grid
        }
        else -> {
            drawRect(Color.White)
        }
    }
}

private fun DrawScope.drawStrokeItem(stroke: DrawingStroke) {
    val pts = stroke.points
    if (pts.size < 2) return

    val strokeColor = Color(stroke.color)
    val alpha = if (stroke.toolType == ToolType.HIGHLIGHTER) stroke.alpha.coerceIn(0.2f, 0.6f) else 1.0f

    drawPath(
        path = stroke.composePath,
        color = strokeColor.copy(alpha = alpha),
        style = Stroke(
            width = stroke.width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

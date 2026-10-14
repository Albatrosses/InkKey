package com.example.ui.components

import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EraserSize
import com.example.model.ShapeType
import com.example.model.ToolType
import com.example.model.ToolbarToolItem
import com.example.viewmodel.MainViewModel

private val PEN_COLORS: List<Pair<Long, String>> = listOf(
    0xFF0F172AL to "Charcoal",
    0xFF1D4ED8L to "Blue",
    0xFFDC2626L to "Red",
    0xFF16A34AL to "Green",
    0xFF7C3AEDL to "Purple",
    0xFFF8FAFCL to "White Chalk"
)

private val HIGHLIGHTER_COLORS: List<Pair<Long, String>> = listOf(
    0x70FBBF24L to "Amber Yellow",
    0x7022C55EL to "Neon Green",
    0x7006B6D4L to "Cyan",
    0x70F43F5EL to "Rose Pink"
)

private val STROKE_WIDTHS = listOf(
    2f to "Fine",
    4f to "Normal",
    8f to "Medium",
    14f to "Bold"
)

@Composable
fun VerticalDrawingToolbar(
    viewModel: MainViewModel,
    currentTool: ToolType,
    penColor: Long,
    strokeWidth: Float,
    highlighterColor: Long,
    shapeModeEnabled: Boolean,
    selectedShapeType: ShapeType,
    zoomScale: Float,
    palmRejectionEnabled: Boolean,
    selectedStrokesCount: Int,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onOpenMathPalette: () -> Unit,
    onAddTextPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toolbarItems by viewModel.toolbarItems.collectAsState()
    val eraserSize by viewModel.eraserSize.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val currentDoc by viewModel.currentDocument.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()

    var showColorPicker by remember { mutableStateOf(false) }
    var showWidthPicker by remember { mutableStateOf(false) }
    var showShapePicker by remember { mutableStateOf(false) }
    var showEraserSizePicker by remember { mutableStateOf(false) }
    var showToolbarCustomizeDialog by remember { mutableStateOf(false) }
    var showPageManagerDialog by remember { mutableStateOf(false) }
    var showClearPageConfirmDialog by remember { mutableStateOf(false) }
    var showDeletePageConfirmDialog by remember { mutableStateOf(false) }

    if (isCollapsed) {
        // Floating handle when toolbar is hidden/collapsed
        Surface(
            modifier = modifier.testTag("vertical_toolbar_collapsed_handle"),
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            shadowElevation = 6.dp
        ) {
            IconButton(
                onClick = onToggleCollapse,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("open_vertical_toolbar_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open Drawing Toolbar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    } else {
        // Full Vertical Tool Palette along the canvas edge
        Surface(
            modifier = modifier
                .width(56.dp)
                .testTag("drawing_toolbar"),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Collapse Button
                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("collapse_vertical_toolbar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Collapse Toolbar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Main Tool Items vertically arranged
                for (item in toolbarItems) {
                    when (item) {
                        ToolbarToolItem.PEN -> {
                            ToolButton(
                                selected = currentTool == ToolType.PEN,
                                icon = Icons.Default.Brush,
                                contentDescription = "Pen Tool",
                                testTag = "tool_pen",
                                onClick = {
                                    viewModel.setTool(ToolType.PEN)
                                },
                                onLongClick = {
                                    viewModel.setTool(ToolType.PEN)
                                    showColorPicker = true
                                }
                            )
                        }
                        ToolbarToolItem.HAND -> {
                            ToolButton(
                                selected = currentTool == ToolType.HAND,
                                icon = Icons.Default.PanTool,
                                contentDescription = "Hand Pan & Zoom",
                                testTag = "tool_hand",
                                onClick = {
                                    viewModel.setTool(ToolType.HAND)
                                }
                            )
                        }
                        ToolbarToolItem.HIGHLIGHTER -> {
                            ToolButton(
                                selected = currentTool == ToolType.HIGHLIGHTER,
                                icon = Icons.Default.AutoAwesome,
                                contentDescription = "Highlighter",
                                testTag = "tool_highlighter",
                                tint = Color(highlighterColor).copy(alpha = 1f),
                                onClick = {
                                    viewModel.setTool(ToolType.HIGHLIGHTER)
                                },
                                onLongClick = {
                                    viewModel.setTool(ToolType.HIGHLIGHTER)
                                    showColorPicker = true
                                }
                            )
                        }
                        ToolbarToolItem.AREA_ERASER -> {
                            ToolButtonWithPainter(
                                selected = currentTool == ToolType.AREA_ERASER,
                                painter = painterResource(id = R.drawable.ic_eraser),
                                contentDescription = "Area Precision Eraser (Cuts Line)",
                                testTag = "tool_area_eraser",
                                onClick = {
                                    if (currentTool == ToolType.AREA_ERASER) {
                                        showEraserSizePicker = true
                                    } else {
                                        viewModel.setTool(ToolType.AREA_ERASER)
                                    }
                                },
                                onLongClick = {
                                    viewModel.setTool(ToolType.AREA_ERASER)
                                    showEraserSizePicker = true
                                }
                            )
                        }
                        ToolbarToolItem.STROKE_ERASER -> {
                            ToolButtonWithPainter(
                                selected = currentTool == ToolType.STROKE_ERASER,
                                painter = painterResource(id = R.drawable.ic_eraser),
                                contentDescription = "Whole Stroke Eraser (Deletes Line)",
                                testTag = "tool_stroke_eraser",
                                onClick = {
                                    if (currentTool == ToolType.STROKE_ERASER) {
                                        showEraserSizePicker = true
                                    } else {
                                        viewModel.setTool(ToolType.STROKE_ERASER)
                                    }
                                },
                                onLongClick = {
                                    viewModel.setTool(ToolType.STROKE_ERASER)
                                    showEraserSizePicker = true
                                }
                            )
                        }
                        ToolbarToolItem.SYMBOLS -> {
                            IconButton(
                                onClick = onOpenMathPalette,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("open_math_symbols_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Functions,
                                    contentDescription = "Math & NEET Symbols",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        ToolbarToolItem.SHAPE -> {
                            ToolButton(
                                selected = currentTool == ToolType.SHAPE || shapeModeEnabled,
                                icon = Icons.Default.Category,
                                contentDescription = "Shape Snapping",
                                testTag = "tool_shape",
                                onClick = {
                                    showShapePicker = true
                                }
                            )
                        }
                        ToolbarToolItem.UNDO_REDO -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(
                                    onClick = { viewModel.undo() },
                                    enabled = canUndo,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("toolbar_undo_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo",
                                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.redo() },
                                    enabled = canRedo,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("toolbar_redo_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = "Redo",
                                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        ToolbarToolItem.PALETTE -> {
                            // Active Color Circle Preview
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentTool == ToolType.HIGHLIGHTER) Color(highlighterColor).copy(alpha = 1f)
                                        else Color(penColor)
                                    )
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { showColorPicker = true }
                                    .testTag("color_preview_button")
                            )

                            // Stroke Width Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .clickable { showWidthPicker = true }
                                    .testTag("stroke_width_button")
                            ) {
                                Text(
                                    text = "${strokeWidth.toInt()}pt",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        ToolbarToolItem.PAGES -> {
                            IconButton(
                                onClick = { showPageManagerDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toolbar_pages_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Page Management",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        ToolbarToolItem.THEMES -> {
                            IconButton(
                                onClick = onAddTextPrompt,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("add_text_button")
                            ) {
                                Icon(
                                    Icons.Default.TextFields,
                                    contentDescription = "Insert Text",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        ToolbarToolItem.CLEAR -> {
                            IconButton(
                                onClick = { showClearPageConfirmDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toolbar_clear_button")
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Page",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Customize Toolbar Button
                IconButton(
                    onClick = { showToolbarCustomizeDialog = true },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("toolbar_customize_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Customize Toolbar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Dialog: Eraser Size & Mode Selection
    if (showEraserSizePicker) {
        AlertDialog(
            onDismissRequest = { showEraserSizePicker = false },
            title = { Text("Eraser Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Eraser Mode:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentTool == ToolType.AREA_ERASER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setTool(ToolType.AREA_ERASER) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Area Eraser", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Cuts lines & symbols", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentTool == ToolType.STROKE_ERASER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setTool(ToolType.STROKE_ERASER) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Stroke Eraser", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Deletes entire line", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Text("Eraser Size / Radius:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EraserSize.values().forEach { size ->
                            val isSel = eraserSize == size
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setEraserSize(size) }
                            ) {
                                Text(
                                    text = size.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEraserSizePicker = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Dialog: Color Picker
    if (showColorPicker) {
        val colors = if (currentTool == ToolType.HIGHLIGHTER) HIGHLIGHTER_COLORS else PEN_COLORS
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(if (currentTool == ToolType.HIGHLIGHTER) "Highlighter Color" else "Pen / Brush Color", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.chunked(3).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowColors.forEach { (colorLong, name) ->
                                val isSelected = (if (currentTool == ToolType.HIGHLIGHTER) highlighterColor else penColor) == colorLong
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        if (currentTool == ToolType.HIGHLIGHTER) {
                                            viewModel.setHighlighterColor(colorLong)
                                        } else {
                                            viewModel.setPenColor(colorLong)
                                        }
                                        showColorPicker = false
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorLong).copy(alpha = 1f))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                    )
                                    Text(name, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Stroke Width Picker
    if (showWidthPicker) {
        AlertDialog(
            onDismissRequest = { showWidthPicker = false },
            title = { Text("Pen Stroke Width", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    STROKE_WIDTHS.forEach { (width, label) ->
                        val isSelected = strokeWidth == width
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.setStrokeWidth(width)
                                    showWidthPicker = false
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = (width * 1.5f).dp.coerceAtLeast(2.dp))
                                        .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$label\n${width.toInt()}pt",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWidthPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Shape Snapping Picker
    if (showShapePicker) {
        AlertDialog(
            onDismissRequest = { showShapePicker = false },
            title = { Text("Shape Snapping", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a geometric shape to snap automatically or use Auto Detect:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val shapeList = listOf(
                        ShapeType.NONE to "Auto Detect",
                        ShapeType.LINE to "Line",
                        ShapeType.TRIANGLE to "Triangle",
                        ShapeType.RECTANGLE to "Rectangle",
                        ShapeType.CIRCLE to "Circle",
                        ShapeType.ELLIPSE to "Ellipse",
                        ShapeType.ARROW to "Arrow"
                    )
                    shapeList.chunked(3).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chunk.forEach { (type, label) ->
                                val isSelected = shapeModeEnabled && selectedShapeType == type
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.toggleShapeMode(type)
                                            showShapePicker = false
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                if (shapeModeEnabled) {
                    TextButton(onClick = {
                        viewModel.disableShapeMode()
                        showShapePicker = false
                    }) {
                        Text("Turn OFF Snapping", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShapePicker = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Forwarding DrawingToolbar for backward compatibility
    @Composable
    fun DrawingToolbar(
        viewModel: MainViewModel,
        currentTool: ToolType,
        penColor: Long,
        strokeWidth: Float,
        highlighterColor: Long,
        shapeModeEnabled: Boolean,
        selectedShapeType: ShapeType,
        zoomScale: Float,
        palmRejectionEnabled: Boolean,
        selectedStrokesCount: Int,
        onOpenMathPalette: () -> Unit,
        onAddTextPrompt: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        VerticalDrawingToolbar(
            viewModel = viewModel,
            currentTool = currentTool,
            penColor = penColor,
            strokeWidth = strokeWidth,
            highlighterColor = highlighterColor,
            shapeModeEnabled = shapeModeEnabled,
            selectedShapeType = selectedShapeType,
            zoomScale = zoomScale,
            palmRejectionEnabled = palmRejectionEnabled,
            selectedStrokesCount = selectedStrokesCount,
            isCollapsed = false,
            onToggleCollapse = {},
            onOpenMathPalette = onOpenMathPalette,
            onAddTextPrompt = onAddTextPrompt,
            modifier = modifier
        )
    }

    // Dialog: Toolbar Customization
    if (showToolbarCustomizeDialog) {
        val allAvailableTools = ToolbarToolItem.values().toList()
        val currentActiveSet = remember { mutableStateListOf(*toolbarItems.toTypedArray()) }
        val hiddenItems = remember {
            mutableStateListOf(*allAvailableTools.filterNot { it in toolbarItems }.toTypedArray())
        }

        AlertDialog(
            onDismissRequest = { showToolbarCustomizeDialog = false },
            title = { Text("Customize Toolbar Layout", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Rearrange tools (e.g. Brush next to Hand/Pan) or show/hide tools:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Active Primary Toolbar Tools:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    currentActiveSet.forEachIndexed { index, item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = true,
                                        onCheckedChange = {
                                            if (currentActiveSet.size > 3) {
                                                currentActiveSet.removeAt(index)
                                                hiddenItems.add(item)
                                            }
                                        }
                                    )
                                    Text("${index + 1}. ${item.title}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val removed = currentActiveSet.removeAt(index)
                                                currentActiveSet.add(index - 1, removed)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ExpandLess, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < currentActiveSet.size - 1) {
                                                val removed = currentActiveSet.removeAt(index)
                                                currentActiveSet.add(index + 1, removed)
                                            }
                                        },
                                        enabled = index < currentActiveSet.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ExpandMore, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (hiddenItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Hidden / Optional Tools (Tap check to enable):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        hiddenItems.forEachIndexed { hIndex, hItem ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = {
                                            hiddenItems.removeAt(hIndex)
                                            currentActiveSet.add(hItem)
                                        }
                                    )
                                    Text(hItem.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateToolbarOrder(currentActiveSet.toList())
                        showToolbarCustomizeDialog = false
                    }
                ) {
                    Text("Save Order")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.resetToolbarOrder()
                            showToolbarCustomizeDialog = false
                        }
                    ) {
                        Text("Reset Defaults")
                    }
                    TextButton(onClick = { showToolbarCustomizeDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Dialog: Page Manager
    if (showPageManagerDialog) {
        AlertDialog(
            onDismissRequest = { showPageManagerDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Page Management", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Page ${currentPageIndex + 1} of ${currentDoc.pages.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Page Navigation Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.goToPage(currentPageIndex - 1) },
                            enabled = currentPageIndex > 0,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Prev", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.addPage() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Add Page", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.goToPage(currentPageIndex + 1) },
                            enabled = currentPageIndex < currentDoc.pages.size - 1,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Next", fontSize = 11.sp)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Pages quick jump list
                    Text("Jump to Page:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentDoc.pages.forEachIndexed { idx, p ->
                            val isCurrent = idx == currentPageIndex
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.clickable {
                                    viewModel.goToPage(idx)
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Page ${idx + 1}", fontSize = 12.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                                    Text("${p.strokes.size} strokes", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Clear or Delete Current Page
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearPageConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Page", fontSize = 11.sp)
                        }

                        if (currentDoc.pages.size > 1) {
                            Button(
                                onClick = { showDeletePageConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Page", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPageManagerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Clear Page Confirmation
    if (showClearPageConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearPageConfirmDialog = false },
            title = { Text("Clear Current Page?") },
            text = { Text("Are you sure you want to clear Page ${currentPageIndex + 1}? All handwritten strokes and symbols on this page will be removed. Other pages will be preserved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCurrentPage()
                        showClearPageConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearPageConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Delete Page Confirmation
    if (showDeletePageConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePageConfirmDialog = false },
            title = { Text("Delete Current Page?") },
            text = { Text("Are you sure you want to delete Page ${currentPageIndex + 1}? Remaining ${currentDoc.pages.size - 1} page(s) will be preserved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCurrentPage()
                        showDeletePageConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePageConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    tint: Color? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else (tint ?: MaterialTheme.colorScheme.onSurfaceVariant)

    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolButtonWithPainter(
    selected: Boolean,
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    testTag: String,
    tint: Color? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else (tint ?: MaterialTheme.colorScheme.onSurfaceVariant)

    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

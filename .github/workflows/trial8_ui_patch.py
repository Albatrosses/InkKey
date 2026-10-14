from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Trial 8 anchor count for {path}: expected 1, found {count}\nANCHOR:\n{old[:500]}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"patched {path}")


def insert_import(path: str, anchor: str, addition: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if addition.strip() in text:
        return
    if anchor not in text:
        raise SystemExit(f"missing import anchor in {path}: {anchor}")
    p.write_text(text.replace(anchor, anchor + addition, 1), encoding="utf-8")


# -----------------------------------------------------------------------------
# 1) Header: fixed back button + independently horizontally scrollable action row.
# This prevents the back/undo collision on narrow phones and gives the actions a
# reserved 64dp start inset (roughly where redo sat in the reported screenshot).
# -----------------------------------------------------------------------------
header = "app/src/main/java/com/example/ui/components/NeetHeader.kt"
insert_import(header, "import androidx.compose.foundation.clickable\n", "import androidx.compose.foundation.horizontalScroll\nimport androidx.compose.foundation.rememberScrollState\n")

p = Path(header)
text = p.read_text(encoding="utf-8")
start_marker = "            TopAppBar(\n"
end_marker = "            // Sub-row: Page navigation bar\n"
start = text.find(start_marker)
end = text.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit("could not locate NeetHeader TopAppBar block")

new_top = r'''            // Fixed navigation control + independent scrollable tool strip.
            // Keeping these in separate layout layers prevents the back button from
            // ever sharing/overlapping the toolbar's horizontal space.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .size(48.dp)
                        .testTag("header_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(start = 64.dp, end = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier.size(44.dp).testTag("header_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier.size(44.dp).testTag("header_redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFullScreen,
                        modifier = Modifier.size(44.dp).testTag("header_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Full Screen Mode",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showThemeMenu = true },
                            modifier = Modifier.size(44.dp).testTag("header_theme_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Change Paper Theme",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            Text(
                                text = "Paper Style",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            PaperStyle.values().forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.displayName) },
                                    onClick = {
                                        viewModel.setPaperStyle(style)
                                        showThemeMenu = false
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Canvas Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            CanvasScrollMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.displayName) },
                                    onClick = {
                                        viewModel.setScrollMode(mode)
                                        showThemeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onCheckAnswerPrompt,
                        modifier = Modifier.size(44.dp).testTag("check_answer_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Check My Answer Prompt",
                            tint = Color(0xFF059669)
                        )
                    }

                    Button(
                        onClick = onSendToChatGpt,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10A37F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("send_to_chatgpt_primary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Send to ChatGPT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(44.dp).testTag("header_more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save as Image (PNG)") },
                                onClick = {
                                    showMoreMenu = false
                                    onSaveImage()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    showMoreMenu = false
                                    onExportPdf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Current Page") },
                                onClick = {
                                    showMoreMenu = false
                                    showClearConfirmDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Current Page") },
                                onClick = {
                                    showMoreMenu = false
                                    showDeletePageDialog = true
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(44.dp).testTag("canvas_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Preferences",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Keep document metadata available without stealing fixed toolbar width.
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .padding(start = 4.dp, end = 8.dp)
                            .clickable { showEditInfoDialog = true }
                            .testTag("document_title_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(document.subject.colorHex))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = document.subject.displayName,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${document.questionNumber}: ${document.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit title",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

'''
text = text[:start] + new_top + text[end:]
p.write_text(text, encoding="utf-8")
print("patched NeetHeader fixed-back + scrollable actions")


# -----------------------------------------------------------------------------
# 2) Canvas: remove the standalone fullscreen-exit button. Dock the side toolbar
# left/right and allow its collapsed glass handle to move vertically along edge.
# -----------------------------------------------------------------------------
canvas = "app/src/main/java/com/example/ui/canvas/CanvasScreen.kt"
insert_import(canvas, "import androidx.compose.foundation.layout.Box\n", "import androidx.compose.foundation.layout.BoxWithConstraints\n")
insert_import(canvas, "import androidx.compose.foundation.layout.padding\n", "import androidx.compose.foundation.layout.offset\n")
insert_import(canvas, "import androidx.compose.ui.platform.LocalContext\n", "import androidx.compose.ui.platform.LocalDensity\n")
insert_import(canvas, "import androidx.compose.ui.Alignment\n", "import androidx.compose.ui.unit.IntOffset\n")

p = Path(canvas)
text = p.read_text(encoding="utf-8")
text = text.replace(
    "    val context = LocalContext.current\n",
    "    val context = LocalContext.current\n    val density = LocalDensity.current\n",
    1,
)
text = text.replace(
    "    var isExporting by remember { mutableStateOf(false) }\n",
    "    var isExporting by remember { mutableStateOf(false) }\n    var toolbarDockRight by remember { mutableStateOf(false) }\n    var toolbarOffsetYPx by remember { mutableStateOf(0f) }\n",
    1,
)

old_box = '''        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Canvas Area
'''
new_box = '''        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val maxToolbarOffsetPx = with(density) { maxHeight.toPx() * 0.40f }

            // Main Canvas Area
'''
if text.count(old_box) != 1:
    raise SystemExit(f"Canvas main Box anchor count {text.count(old_box)}")
text = text.replace(old_box, new_box, 1)

floating_start = text.find("            // Floating Exit Full Screen Button (visible when Full Screen is enabled)\n")
floating_end = text.find("            // Vertical Tool Palette along the side of the canvas", floating_start)
if floating_start < 0 or floating_end < 0:
    raise SystemExit("could not locate floating fullscreen block")
text = text[:floating_start] + text[floating_end:]

old_toolbar = '''            VerticalDrawingToolbar(
                viewModel = viewModel,
                currentTool = currentTool,
                penColor = penColor,
                strokeWidth = strokeWidth,
                highlighterColor = highlighterColor,
                shapeModeEnabled = shapeModeEnabled,
                selectedShapeType = selectedShapeType,
                zoomScale = zoomScale,
                palmRejectionEnabled = palmRejectionEnabled,
                selectedStrokesCount = selectedStrokeIds.size,
                isCollapsed = isToolbarCollapsed,
                onToggleCollapse = { viewModel.toggleToolbar() },
                onOpenMathPalette = { showMathPalette = true },
                onAddTextPrompt = { showInsertTextDialog = true },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
            )
'''
new_toolbar = '''            VerticalDrawingToolbar(
                viewModel = viewModel,
                currentTool = currentTool,
                penColor = penColor,
                strokeWidth = strokeWidth,
                highlighterColor = highlighterColor,
                shapeModeEnabled = shapeModeEnabled,
                selectedShapeType = selectedShapeType,
                zoomScale = zoomScale,
                palmRejectionEnabled = palmRejectionEnabled,
                selectedStrokesCount = selectedStrokeIds.size,
                isCollapsed = isToolbarCollapsed,
                isFullScreen = isFullScreen,
                dockOnRight = toolbarDockRight,
                onToggleCollapse = { viewModel.toggleToolbar() },
                onExitFullScreen = { viewModel.setFullScreen(false) },
                onDockSideChange = { toolbarDockRight = it },
                onVerticalDrag = { deltaY ->
                    toolbarOffsetYPx = (toolbarOffsetYPx + deltaY)
                        .coerceIn(-maxToolbarOffsetPx, maxToolbarOffsetPx)
                },
                onOpenMathPalette = { showMathPalette = true },
                onAddTextPrompt = { showInsertTextDialog = true },
                modifier = Modifier
                    .align(if (toolbarDockRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .offset { IntOffset(0, toolbarOffsetYPx.roundToInt()) }
                    .padding(horizontal = 4.dp)
            )
'''
if text.count(old_toolbar) != 1:
    raise SystemExit(f"Canvas toolbar anchor count {text.count(old_toolbar)}")
text = text.replace(old_toolbar, new_toolbar, 1)
p.write_text(text, encoding="utf-8")
print("patched CanvasScreen fullscreen + docking")


# -----------------------------------------------------------------------------
# 3) Vertical toolbar: slim, long translucent glass handle with edge docking.
# Fullscreen exit lives INSIDE the expanded side toolbar instead of floating.
# -----------------------------------------------------------------------------
toolbar = "app/src/main/java/com/example/ui/components/DrawingToolbar.kt"
insert_import(toolbar, "import androidx.compose.foundation.ExperimentalFoundationApi\n", "import androidx.compose.foundation.gestures.detectDragGestures\n")
insert_import(toolbar, "import androidx.compose.ui.draw.clip\n", "import androidx.compose.ui.draw.shadow\n")
insert_import(toolbar, "import androidx.compose.ui.graphics.Color\n", "import androidx.compose.ui.graphics.Brush\n")
insert_import(toolbar, "import androidx.compose.ui.platform.testTag\n", "import androidx.compose.ui.input.pointer.pointerInput\n")
insert_import(toolbar, "import androidx.compose.material.icons.filled.Functions\n", "import androidx.compose.material.icons.filled.FullscreenExit\n")
insert_import(toolbar, "import com.example.viewmodel.MainViewModel\n", "import kotlin.math.abs\n")

p = Path(toolbar)
text = p.read_text(encoding="utf-8")
old_sig = '''    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onOpenMathPalette: () -> Unit,
    onAddTextPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
'''
new_sig = '''    isCollapsed: Boolean,
    isFullScreen: Boolean = false,
    dockOnRight: Boolean = false,
    onToggleCollapse: () -> Unit,
    onExitFullScreen: () -> Unit = {},
    onDockSideChange: (Boolean) -> Unit = {},
    onVerticalDrag: (Float) -> Unit = {},
    onOpenMathPalette: () -> Unit,
    onAddTextPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
'''
if text.count(old_sig) != 1:
    raise SystemExit(f"VerticalDrawingToolbar signature count {text.count(old_sig)}")
text = text.replace(old_sig, new_sig, 1)

anchor_state = "    var showDeletePageConfirmDialog by remember { mutableStateOf(false) }\n\n"
if anchor_state not in text:
    raise SystemExit("missing toolbar state anchor")
text = text.replace(
    anchor_state,
    anchor_state + "    var handleHorizontalDrag by remember { mutableStateOf(0f) }\n\n",
    1,
)

start = text.find("    if (isCollapsed) {\n")
full_marker = "    } else {\n        // Full Vertical Tool Palette along the canvas edge\n"
end = text.find(full_marker, start)
if start < 0 or end < 0:
    raise SystemExit("could not locate collapsed toolbar block")

collapsed = r'''    if (isCollapsed) {
        // Long, narrow, translucent edge handle inspired by a water droplet / glass tab.
        val glassShape = if (dockOnRight) {
            RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 6.dp, bottomEnd = 6.dp)
        } else {
            RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp, topStart = 6.dp, bottomStart = 6.dp)
        }

        Box(
            modifier = modifier
                .width(32.dp)
                .height(98.dp)
                .shadow(10.dp, glassShape)
                .clip(glassShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.60f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.26f), glassShape)
                .pointerInput(dockOnRight) {
                    detectDragGestures(
                        onDragStart = { handleHorizontalDrag = 0f },
                        onDragEnd = {
                            if (abs(handleHorizontalDrag) >= 48f) {
                                onDockSideChange(handleHorizontalDrag > 0f)
                            }
                            handleHorizontalDrag = 0f
                        },
                        onDragCancel = { handleHorizontalDrag = 0f }
                    ) { _, dragAmount ->
                        handleHorizontalDrag += dragAmount.x
                        onVerticalDrag(dragAmount.y)
                    }
                }
                .clickable(onClick = onToggleCollapse)
                .testTag("vertical_toolbar_collapsed_handle"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (dockOnRight) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                contentDescription = "Open Drawing Toolbar",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                modifier = Modifier.size(21.dp).testTag("open_vertical_toolbar_button")
            )
        }
''' 
text = text[:start] + collapsed + text[end:]

# Mirror the collapse chevron toward the currently docked edge.
text = text.replace(
    "                        imageVector = Icons.Default.ChevronLeft,\n                        contentDescription = \"Collapse Toolbar\",",
    "                        imageVector = if (dockOnRight) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,\n                        contentDescription = \"Collapse Toolbar\",",
    1,
)

# Make the expanded rail slightly translucent too.
text = text.replace(
    "            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)\n",
    "            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f)\n",
    1,
)

collapse_block = '''                // Main Tool Items vertically arranged
                for (item in toolbarItems) {
'''
fullscreen_block = '''                // In fullscreen, exit lives in this slide toolbar instead of a separate floating button.
                if (isFullScreen) {
                    IconButton(
                        onClick = onExitFullScreen,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toolbar_exit_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Full Screen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // Main Tool Items vertically arranged
                for (item in toolbarItems) {
'''
if text.count(collapse_block) != 1:
    raise SystemExit(f"fullscreen insert anchor count {text.count(collapse_block)}")
text = text.replace(collapse_block, fullscreen_block, 1)
p.write_text(text, encoding="utf-8")
print("patched DrawingToolbar glass handle + fullscreen action")

# Sanity checks so CI fails early if any requested behavior disappeared.
checks = {
    header: ["horizontalScroll(rememberScrollState())", "header_back_button", ".padding(start = 64.dp"],
    canvas: ["BoxWithConstraints", "toolbarDockRight", "onExitFullScreen = { viewModel.setFullScreen(false) }"],
    toolbar: ["vertical_toolbar_collapsed_handle", "Brush.verticalGradient", "toolbar_exit_fullscreen_button", "onDockSideChange"],
}
for path, needles in checks.items():
    data = Path(path).read_text(encoding="utf-8")
    for needle in needles:
        if needle not in data:
            raise SystemExit(f"Trial 8 sanity check failed: {needle} missing from {path}")

print("Trial 8 UI patch complete")

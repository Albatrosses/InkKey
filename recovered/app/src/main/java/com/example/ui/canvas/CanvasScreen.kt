package com.example.ui.canvas

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tablet
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.export.ExportManager
import com.example.model.ToolType
import com.example.ui.components.CheckAnswerPromptDialog
import com.example.ui.components.VerticalDrawingToolbar
import com.example.ui.components.MathScienceSymbolsSheet
import com.example.ui.components.NeetHeader
import com.example.ui.components.SendToChatGptDialog
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.ScreenState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CanvasScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val document by viewModel.currentDocument.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val penColor by viewModel.penColor.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val highlighterColor by viewModel.highlighterColor.collectAsState()
    val shapeModeEnabled by viewModel.shapeModeEnabled.collectAsState()
    val selectedShapeType by viewModel.selectedShapeType.collectAsState()
    val palmRejectionEnabled by viewModel.palmRejectionEnabled.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    val selectedStrokeIds by viewModel.selectedStrokeIds.collectAsState()
    val liveStroke by viewModel.liveStroke.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()
    val isToolbarCollapsed by viewModel.isToolbarCollapsed.collectAsState()
    val tabletFeedback by viewModel.tabletFeedbackMessage.collectAsState()

    LaunchedEffect(tabletFeedback) {
        if (tabletFeedback != null) {
            delay(1800)
            viewModel.clearTabletFeedback()
        }
    }

    // Auto-hide vertical toolbar when user starts writing to maximize clean writing space
    LaunchedEffect(liveStroke) {
        if (liveStroke != null && !isToolbarCollapsed) {
            viewModel.setToolbarCollapsed(true)
        }
    }

    val currentPage = document.pages.getOrElse(currentPageIndex) {
        document.pages.firstOrNull() ?: com.example.model.PageData(0)
    }

    var showSendToChatGptDialog by remember { mutableStateOf(false) }
    var showCheckPromptDialog by remember { mutableStateOf(false) }
    var showMathPalette by remember { mutableStateOf(false) }
    var showInsertTextDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var toolbarDockRight by remember { mutableStateOf(false) }
    var toolbarOffsetYPx by remember { mutableStateOf(0f) }

    // Intercept hardware / gesture back button to safely return to Home
    BackHandler {
        if (isFullScreen) {
            viewModel.setFullScreen(false)
        } else {
            viewModel.scheduleAutoSave()
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("canvas_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isFullScreen) {
                NeetHeader(
                    viewModel = viewModel,
                    document = document,
                    currentPageIndex = currentPageIndex,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onBackToHome = {
                        viewModel.scheduleAutoSave()
                        onNavigateBack()
                    },
                    onToggleFullScreen = {
                        viewModel.toggleFullScreen()
                    },
                    onSendToChatGpt = {
                        showSendToChatGptDialog = true
                    },
                    onCheckAnswerPrompt = {
                        showCheckPromptDialog = true
                    },
                    onExportPdf = {
                        scope.launch {
                            isExporting = true
                            try {
                                val pdfFile = ExportManager.exportAsPdf(context, document, forceWhiteBackground = false)
                                val shareResult = ExportManager.shareAnswer(context, listOf(pdfFile), isPdf = true)
                                val msg = when (shareResult) {
                                    is ExportManager.ShareResult.SentDirectToChatGpt -> "Sent PDF to ChatGPT! Saved: ${pdfFile.name}"
                                    is ExportManager.ShareResult.OpenedShareSheet -> "Share sheet opened. Saved: ${pdfFile.name}"
                                    is ExportManager.ShareResult.SavedLocally -> "Saved PDF: ${pdfFile.name}"
                                    is ExportManager.ShareResult.Error -> shareResult.message
                                }
                                val res = snackbarHostState.showSnackbar(
                                    message = msg,
                                    actionLabel = "Open PDF",
                                    duration = SnackbarDuration.Long
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    ExportManager.openExportedFile(context, pdfFile)
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}")
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    onSaveImage = {
                        scope.launch {
                            isExporting = true
                            try {
                                val files = ExportManager.exportAsPngImages(context, document, scale = 2.0f)
                                val fileName = files.firstOrNull()?.name ?: "PNG"
                                val res = snackbarHostState.showSnackbar(
                                    message = "Saved ${files.size} image(s) to InkKey Exports ($fileName)",
                                    actionLabel = "Open Image",
                                    duration = SnackbarDuration.Long
                                )
                                if (res == SnackbarResult.ActionPerformed && files.isNotEmpty()) {
                                    ExportManager.openExportedFile(context, files.first())
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Save failed: ${e.localizedMessage}")
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    onOpenSettings = {
                        viewModel.scheduleAutoSave()
                        viewModel.navigateTo(ScreenState.SETTINGS)
                    }
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val maxToolbarOffsetPx = with(density) { maxHeight.toPx() * 0.40f }
            val lensViewportWidthPx = with(density) { maxWidth.toPx() }
            val lensHalfWidthPx = with(density) { 7.dp.toPx() }
            val lensEdgeInsetPx = with(density) { 4.dp.toPx() }
            val lensSourceXPx = if (toolbarDockRight) {
                lensViewportWidthPx - lensEdgeInsetPx - lensHalfWidthPx
            } else {
                lensEdgeInsetPx + lensHalfWidthPx
            }
            val lensSourceYPx = with(density) { maxHeight.toPx() / 2f } + toolbarOffsetYPx

            // Main Canvas Area
            HandwritingCanvas(
                viewModel = viewModel,
                document = document,
                pageData = currentPage,
                liveStroke = liveStroke,
                currentTool = currentTool,
                zoomScale = zoomScale,
                panOffset = panOffset,
                selectedStrokeIds = selectedStrokeIds,
                palmRejectionEnabled = palmRejectionEnabled,
                onWritingStarted = {
                    if (!isToolbarCollapsed) {
                        viewModel.setToolbarCollapsed(true)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Hardware Tablet Feedback HUD (XP-PEN Deco Mini 7 & Stylus)
            AnimatedVisibility(
                visible = tabletFeedback != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                tabletFeedback?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xDD0F172A),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tablet,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Vertical Tool Palette along the side of the canvas (scrollable, compact, collapsible)
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
                lensSourceXPx = lensSourceXPx,
                lensSourceYPx = lensSourceYPx,
                onOpenMathPalette = { showMathPalette = true },
                onAddTextPrompt = { showInsertTextDialog = true },
                modifier = Modifier
                    .align(if (toolbarDockRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .offset { IntOffset(0, toolbarOffsetYPx.roundToInt()) }
                    .padding(horizontal = 4.dp)
            )
        }
    }

    // Modal Bottom Sheet: Send to ChatGPT
    if (showSendToChatGptDialog) {
        SendToChatGptDialog(
            document = document,
            isExporting = isExporting,
            onDismiss = { showSendToChatGptDialog = false },
            onSendToChatGpt = { scale, forceWhiteBg, prompt ->
                scope.launch {
                    isExporting = true
                    try {
                        val files = ExportManager.exportAsPngImages(
                            context = context,
                            document = document,
                            scale = scale,
                            forceWhiteBackground = forceWhiteBg
                        )
                        val result = ExportManager.shareAnswer(
                            context = context,
                            files = files,
                            isPdf = false,
                            prompt = prompt,
                            preferChatGptDirect = true
                        )
                        showSendToChatGptDialog = false
                        val msg = when (result) {
                            is ExportManager.ShareResult.SentDirectToChatGpt -> "Opening ChatGPT with answer image!"
                            is ExportManager.ShareResult.OpenedShareSheet -> "Answer exported! Select ChatGPT in share sheet."
                            is ExportManager.ShareResult.SavedLocally -> "Answer saved! Attach from gallery in ChatGPT."
                            is ExportManager.ShareResult.Error -> result.message
                        }
                        snackbarHostState.showSnackbar(msg)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Export error: ${e.localizedMessage}")
                    } finally {
                        isExporting = false
                    }
                }
            },
            onSavePng = { scale, forceWhiteBg ->
                scope.launch {
                    isExporting = true
                    try {
                        val files = ExportManager.exportAsPngImages(
                            context = context,
                            document = document,
                            scale = scale,
                            forceWhiteBackground = forceWhiteBg
                        )
                        showSendToChatGptDialog = false
                        val fileName = files.firstOrNull()?.name ?: "PNG"
                        val res = snackbarHostState.showSnackbar(
                            message = "Saved ${files.size} image(s) to InkKey Exports ($fileName)",
                            actionLabel = "Open",
                            duration = SnackbarDuration.Long
                        )
                        if (res == SnackbarResult.ActionPerformed && files.isNotEmpty()) {
                            ExportManager.openExportedFile(context, files.first())
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Save failed: ${e.localizedMessage}")
                    } finally {
                        isExporting = false
                    }
                }
            },
            onExportPdf = { forceWhiteBg ->
                scope.launch {
                    isExporting = true
                    try {
                        val file = ExportManager.exportAsPdf(context, document, forceWhiteBackground = forceWhiteBg)
                        ExportManager.shareAnswer(context, listOf(file), isPdf = true)
                        showSendToChatGptDialog = false
                        snackbarHostState.showSnackbar("Exported multipage PDF: ${file.name}")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("PDF failed: ${e.localizedMessage}")
                    } finally {
                        isExporting = false
                    }
                }
            },
            onCopyPrompt = { prompt ->
                ExportManager.copyPromptToClipboard(context, prompt)
            }
        )
    }

    // Modal: Check Answer Prompt
    if (showCheckPromptDialog) {
        CheckAnswerPromptDialog(
            onDismiss = { showCheckPromptDialog = false },
            onCopyPrompt = { prompt ->
                ExportManager.copyPromptToClipboard(context, prompt)
            },
            onSendToChatGpt = {
                showSendToChatGptDialog = true
            }
        )
    }

    // Modal Bottom Sheet: Math & Science Palette
    if (showMathPalette) {
        MathScienceSymbolsSheet(
            onDismiss = { showMathPalette = false },
            onSymbolSelected = { symbol ->
                // Stamp symbol into current view center
                val centerX = (-panOffset.x + 300f) / zoomScale
                val centerY = (-panOffset.y + 400f) / zoomScale
                viewModel.addTextAnnotation(symbol, centerX, centerY, fontSize = 24f, color = penColor)
                Toast.makeText(context, "Added \"$symbol\" to canvas", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Insert Text
    if (showInsertTextDialog) {
        var userText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInsertTextDialog = false },
            title = { Text("Insert Text Label") },
            text = {
                OutlinedTextField(
                    value = userText,
                    onValueChange = { userText = it },
                    label = { Text("Enter text, equation, or note") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (userText.isNotBlank()) {
                            val centerX = (-panOffset.x + 200f) / zoomScale
                            val centerY = (-panOffset.y + 350f) / zoomScale
                            viewModel.addTextAnnotation(userText, centerX, centerY, fontSize = 20f, color = penColor)
                        }
                        showInsertTextDialog = false
                    }
                ) {
                    Text("Insert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsertTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

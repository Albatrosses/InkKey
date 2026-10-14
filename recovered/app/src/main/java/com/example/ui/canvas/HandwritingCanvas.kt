package com.example.ui.canvas

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnswerDocument
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.PageData
import com.example.model.PaperStyle
import com.example.model.TextAnnotation
import com.example.model.ToolType
import com.example.viewmodel.MainViewModel
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HandwritingCanvas(
    viewModel: MainViewModel,
    document: AnswerDocument,
    pageData: PageData,
    currentTool: ToolType,
    penColor: Long = 0xFF0F172AL,
    strokeWidth: Float = 4f,
    highlighterColor: Long = 0x70FBBF24L,
    palmRejectionEnabled: Boolean = true,

    zoomScale: Float,
    panOffset: DrawingPoint,
    selectedStrokeIds: Set<String>,
    liveStroke: DrawingStroke?,
    onWritingStarted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedAnnotationId by viewModel.selectedAnnotationId.collectAsState()
    val eraserSize by viewModel.eraserSize.collectAsState()
    val tabletConfig by viewModel.tabletConfig.collectAsState()

    var lassoStart by remember { mutableStateOf<Offset?>(null) }
    var lassoCurrent by remember { mutableStateOf<Offset?>(null) }
    var isDraggingSelection by remember { mutableStateOf(false) }
    var lastDragPoint by remember { mutableStateOf<Offset?>(null) }

    // Symbol dragging state
    var isDraggingAnnotation by remember { mutableStateOf(false) }
    var isResizingAnnotation by remember { mutableStateOf(false) }
    var resizeInitialDist by remember { mutableStateOf(0f) }
    var resizeInitialFontSize by remember { mutableStateOf(24f) }
    var annotationDragStart by remember { mutableStateOf<Offset?>(null) }
    var activeTouchPos by remember { mutableStateOf<Offset?>(null) }

    // Infinite canvas multi-touch gesture state
    var isMultiTouchGesture by remember { mutableStateOf(false) }
    var lastMultiTouchDist by remember { mutableStateOf(0f) }
    var lastMultiTouchCenter by remember { mutableStateOf<Offset?>(null) }
    var lastHandTouch by remember { mutableStateOf<Offset?>(null) }
    var prevBarrel1Pressed by remember { mutableStateOf(false) }
    var prevBarrel2Pressed by remember { mutableStateOf(false) }

    val selectedAnnotation = pageData.textAnnotations.find { it.id == selectedAnnotationId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter { motionEvent ->
                val toolType = motionEvent.getToolType(0)
                val isFinger = toolType == MotionEvent.TOOL_TYPE_FINGER
                val isHardwareEraser = toolType == MotionEvent.TOOL_TYPE_ERASER

                val pointerCount = motionEvent.pointerCount
            if (pointerCount == 1 && !isHardwareEraser && motionEvent.buttonState == 0 && motionEvent.pressure <= 0.002f) {
                if (viewModel.handleTouchMotionEvent(motionEvent)) {
                    return@pointerInteropFilter true
                }
            }

                // Handle multi-touch: Two-finger infinite canvas panning and pinch-to-zoom
                if (pointerCount >= 2) {
                    val p0 = Offset(motionEvent.getX(0), motionEvent.getY(0))
                    val p1 = Offset(motionEvent.getX(1), motionEvent.getY(1))
                    val currentDist = hypot(p1.x - p0.x, p1.y - p0.y)
                    val currentCenter = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)

                    when (motionEvent.actionMasked) {
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            isMultiTouchGesture = true
                            viewModel.cancelLiveStroke()
                            lastMultiTouchDist = currentDist
                            lastMultiTouchCenter = currentCenter
                            activeTouchPos = null
                            return@pointerInteropFilter true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!isMultiTouchGesture) {
                                isMultiTouchGesture = true
                                viewModel.cancelLiveStroke()
                                lastMultiTouchDist = currentDist
                                lastMultiTouchCenter = currentCenter
                                activeTouchPos = null
                                return@pointerInteropFilter true
                            }

                            // Pinch to Zoom
                            if (lastMultiTouchDist > 10f && currentDist > 10f) {
                                val scaleFactor = currentDist / lastMultiTouchDist
                                val newZoom = (zoomScale * scaleFactor).coerceIn(0.5f, 3.5f)
                                viewModel.setZoomScale(newZoom)
                            }

                            // Two-finger 2D Pan
                            lastMultiTouchCenter?.let { prevCenter ->
                                var dx = currentCenter.x - prevCenter.x
                                var dy = currentCenter.y - prevCenter.y
                                when (document.scrollMode) {
                                    com.example.model.CanvasScrollMode.VERTICAL -> dx = 0f
                                    com.example.model.CanvasScrollMode.HORIZONTAL -> dy = 0f
                                    else -> Unit
                                }
                                viewModel.updatePanOffset(dx, dy)
                            }

                            lastMultiTouchDist = currentDist
                            lastMultiTouchCenter = currentCenter
                            return@pointerInteropFilter true
                        }
                        MotionEvent.ACTION_POINTER_UP -> {
                            lastMultiTouchCenter = null
                            lastMultiTouchDist = 0f
                            return@pointerInteropFilter true
                        }
                    }
                }

                if (isMultiTouchGesture) {
                    if (motionEvent.actionMasked == MotionEvent.ACTION_UP || motionEvent.actionMasked == MotionEvent.ACTION_CANCEL) {
                        isMultiTouchGesture = false
                        lastMultiTouchCenter = null
                        lastMultiTouchDist = 0f
                    }
                    return@pointerInteropFilter true
                }

                // 1. Stylus-only drawing mode: if enabled and input is from finger, don't draw (allow panning if HAND tool)
                if (tabletConfig.stylusOnlyDrawing && isFinger && currentTool != ToolType.HAND) {
                    return@pointerInteropFilter true
                }

                // 2. Palm rejection logic for single finger drawing
                if (palmRejectionEnabled && isFinger && currentTool != ToolType.HAND) {
                    val touchSize = motionEvent.size
                    if (touchSize > 0.52f) {
                        return@pointerInteropFilter true
                    }
                }

                // 3. Stylus barrel button detection & mapping (XP-PEN Deco Mini 7 V2 Stylus)
                val buttonState = motionEvent.buttonState

                // Lower Barrel Button (Barrel 1): BUTTON_STYLUS_PRIMARY (32) or TOOL_TYPE_ERASER
                val rawLower = (buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0) || isHardwareEraser

                // Upper Barrel Button (Barrel 2): BUTTON_STYLUS_SECONDARY (64), BUTTON_SECONDARY (2, Right-click), BUTTON_TERTIARY (4), BUTTON_FORWARD (16), BUTTON_BACK (8)
                val rawUpper = (buttonState and (MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY or MotionEvent.BUTTON_FORWARD or MotionEvent.BUTTON_BACK)) != 0

                val isLowerBarrel = if (tabletConfig.swapBarrelButtons) rawUpper else rawLower
                val isUpperBarrel = if (tabletConfig.swapBarrelButtons) rawLower else rawUpper

                // Feed diagnostic event
                if (buttonState != 0 || isHardwareEraser) {
                    // Activity input router has already processed this event.
                }

                // Determine active action with zero hardcoded overrides - user config strictly governs
                val activeHardwareAction: com.example.model.TabletAction? = when {
                    isUpperBarrel -> tabletConfig.upperBarrelAction
                    isLowerBarrel -> tabletConfig.lowerBarrelAction
                    else -> null
                }

                if (activeHardwareAction != null && isInstantTabletAction(activeHardwareAction)) {
                    return@pointerInteropFilter true
                }

                val effectiveTool: ToolType = when (activeHardwareAction) {
                    com.example.model.TabletAction.AREA_ERASER -> ToolType.AREA_ERASER
                    com.example.model.TabletAction.STROKE_ERASER -> ToolType.STROKE_ERASER
                    com.example.model.TabletAction.HAND -> ToolType.HAND
                    com.example.model.TabletAction.PEN -> ToolType.PEN
                    com.example.model.TabletAction.LASSO -> ToolType.LASSO
                    else -> viewModel.upperTrialToolOverride ?: currentTool
                }
                val effectiveHardwareEraser = effectiveTool == ToolType.AREA_ERASER || effectiveTool == ToolType.STROKE_ERASER

                val rawX = motionEvent.x
                val rawY = motionEvent.y

                // Hand tool: single finger drag or stylus with Hand mapped pans canvas infinitely
                if (effectiveTool == ToolType.HAND) {
                    when (motionEvent.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            lastHandTouch = Offset(rawX, rawY)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            lastHandTouch?.let { prev ->
                                var dx = rawX - prev.x
                                var dy = rawY - prev.y
                                when (document.scrollMode) {
                                    com.example.model.CanvasScrollMode.VERTICAL -> dx = 0f
                                    com.example.model.CanvasScrollMode.HORIZONTAL -> dy = 0f
                                    else -> Unit
                                }
                                viewModel.updatePanOffset(dx, dy)
                                lastHandTouch = Offset(rawX, rawY)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            lastHandTouch = null
                        }
                    }
                    return@pointerInteropFilter true
                }

                // Convert screen coordinates to canvas space
                val canvasX = (rawX - panOffset.x) / zoomScale
                val canvasY = (rawY - panOffset.y) / zoomScale

                // 4. Stylus Pressure Sensitivity & Custom Curve (XP-PEN Deco Mini 7 & Active Stylus)
                val rawPressure = motionEvent.pressure
                val effectivePressure = if (tabletConfig.pressureEnabled) {
                    val normalized = ((rawPressure - tabletConfig.minPressure) / (tabletConfig.maxPressure - tabletConfig.minPressure).coerceAtLeast(0.01f)).coerceIn(0.05f, 1f)
                    Math.pow(normalized.toDouble(), tabletConfig.pressureCurve.exponent.toDouble()).toFloat()
                } else {
                    1.0f
                }

                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        activeTouchPos = Offset(canvasX, canvasY)

                        // Notify that user has started writing/interacting with canvas -> auto-hide toolbar
                        if (effectiveTool != ToolType.HAND) {
                            onWritingStarted()
                        }

                        // 1. If an annotation is already selected, check corner handles for resizing
                        if (selectedAnnotation != null && !effectiveHardwareEraser && effectiveTool != ToolType.LASSO) {
                            val activeSym = selectedAnnotation
                            val textWidth = max(activeSym.text.length * activeSym.fontSize * 0.7f, 32f)
                            val textHeight = activeSym.fontSize * 1.4f
                            val corners = listOf(
                                Offset(activeSym.x - 8f, activeSym.y - textHeight - 4f),
                                Offset(activeSym.x + textWidth + 8f, activeSym.y - textHeight - 4f),
                                Offset(activeSym.x - 8f, activeSym.y + 10f),
                                Offset(activeSym.x + textWidth + 8f, activeSym.y + 10f)
                            )
                            val hitCorner = corners.any { hypot(it.x - canvasX, it.y - canvasY) < 28f }
                            if (hitCorner) {
                                viewModel.saveUndoSnapshot() // Save undo once at start of resize
                                isResizingAnnotation = true
                                resizeInitialDist = hypot(canvasX - activeSym.x, canvasY - activeSym.y)
                                resizeInitialFontSize = activeSym.fontSize
                                return@pointerInteropFilter true
                            }
                        }

                        // 2. Check if user tapped an existing symbol to select/edit it
                        if (!effectiveHardwareEraser && effectiveTool != ToolType.LASSO) {
                            val hitAnnotation = pageData.textAnnotations.find { sym ->
                                sym.hitTest(canvasX, canvasY, padding = 24f)
                            }

                            if (hitAnnotation != null) {
                                viewModel.saveUndoSnapshot() // Save undo once at start of drag
                                viewModel.selectAnnotation(hitAnnotation.id)
                                isDraggingAnnotation = true
                                annotationDragStart = Offset(canvasX - hitAnnotation.x, canvasY - hitAnnotation.y)
                                return@pointerInteropFilter true
                            }
                        }

                        // 3. If an annotation was selected and user tapped outside: deselect without drawing
                        if (selectedAnnotationId != null && !effectiveHardwareEraser && effectiveTool != ToolType.LASSO) {
                            viewModel.commitSelectedAnnotation()
                            return@pointerInteropFilter true
                        }

                        // 4. Lasso tool handling
                        if (effectiveTool == ToolType.LASSO) {
                            if (selectedStrokeIds.isNotEmpty()) {
                                val tappedInside = isInsideSelectedStrokes(canvasX, canvasY, pageData.strokes, selectedStrokeIds)
                                if (tappedInside) {
                                    viewModel.saveUndoSnapshot() // Save undo once at start of selection move
                                    isDraggingSelection = true
                                    lastDragPoint = Offset(canvasX, canvasY)
                                    return@pointerInteropFilter true
                                }
                            }
                            lassoStart = Offset(canvasX, canvasY)
                            lassoCurrent = Offset(canvasX, canvasY)
                            viewModel.clearSelection()
                        } else {
                            viewModel.onDrawingPointerDown(DrawingPoint(canvasX, canvasY, effectivePressure), overrideTool = effectiveTool, isHardwareEraser = effectiveHardwareEraser)
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        activeTouchPos = Offset(canvasX, canvasY)

                        if (isResizingAnnotation && selectedAnnotationId != null) {
                            val activeSym = selectedAnnotation
                            if (activeSym != null && resizeInitialDist > 5f) {
                                val curDist = hypot(canvasX - activeSym.x, canvasY - activeSym.y)
                                val ratio = curDist / resizeInitialDist
                                val newSize = (resizeInitialFontSize * ratio).coerceIn(14f, 80f)
                                viewModel.resizeAnnotation(activeSym.id, newSize - activeSym.fontSize, saveUndo = false)
                            }
                            return@pointerInteropFilter true
                        }

                        if (isDraggingAnnotation && selectedAnnotationId != null && annotationDragStart != null) {
                            val newX = canvasX - annotationDragStart!!.x
                            val newY = canvasY - annotationDragStart!!.y
                            viewModel.moveAnnotation(selectedAnnotationId!!, newX, newY)
                            return@pointerInteropFilter true
                        }

                        if (effectiveTool == ToolType.LASSO) {
                            if (isDraggingSelection && lastDragPoint != null) {
                                val dx = canvasX - lastDragPoint!!.x
                                val dy = canvasY - lastDragPoint!!.y
                                viewModel.moveSelectedStrokes(dx, dy)
                                lastDragPoint = Offset(canvasX, canvasY)
                            } else if (lassoStart != null) {
                                lassoCurrent = Offset(canvasX, canvasY)
                            }
                        } else {
                            val hSize = motionEvent.historySize
                            val batch = mutableListOf<DrawingPoint>()
                            for (h in 0 until hSize) {
                                val hx = (motionEvent.getHistoricalX(h) - panOffset.x) / zoomScale
                                val hy = (motionEvent.getHistoricalY(h) - panOffset.y) / zoomScale
                                val hpRaw = motionEvent.getHistoricalPressure(h)
                                val hp = if (tabletConfig.pressureEnabled) {
                                    val norm = ((hpRaw - tabletConfig.minPressure) / (tabletConfig.maxPressure - tabletConfig.minPressure).coerceAtLeast(0.01f)).coerceIn(0.05f, 1f)
                                    Math.pow(norm.toDouble(), tabletConfig.pressureCurve.exponent.toDouble()).toFloat()
                                } else 1f
                                batch.add(DrawingPoint(hx, hy, hp))
                            }
                            batch.add(DrawingPoint(canvasX, canvasY, effectivePressure))
                            viewModel.onDrawingPointerMoveBatch(batch, overrideTool = effectiveTool, isHardwareEraser = effectiveHardwareEraser)
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        activeTouchPos = null
                        if (isResizingAnnotation) {
                            isResizingAnnotation = false
                            viewModel.scheduleAutoSave()
                            return@pointerInteropFilter true
                        }
                        if (isDraggingAnnotation) {
                            isDraggingAnnotation = false
                            annotationDragStart = null
                            viewModel.scheduleAutoSave()
                            return@pointerInteropFilter true
                        }

                        if (effectiveTool == ToolType.LASSO) {
                            if (isDraggingSelection) {
                                isDraggingSelection = false
                                lastDragPoint = null
                            } else if (lassoStart != null && lassoCurrent != null) {
                                val minX = min(lassoStart!!.x, lassoCurrent!!.x)
                                val maxX = max(lassoStart!!.x, lassoCurrent!!.x)
                                val minY = min(lassoStart!!.y, lassoCurrent!!.y)
                                val maxY = max(lassoStart!!.y, lassoCurrent!!.y)
                                viewModel.selectStrokesInRegion(minX, minY, maxX, maxY)
                                lassoStart = null
                                lassoCurrent = null
                            }
                        } else {
                            viewModel.onDrawingPointerUp()
                        }
                    }
                }
                true
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("drawing_canvas")
        ) {
            // 1. Draw Paper Background
            drawCanvasBackground(document.paperStyle, size, zoomScale, panOffset)

            // Apply Canvas Transform: Pan & Zoom
            val canvasNative = drawContext.canvas.nativeCanvas
            canvasNative.save()
            canvasNative.translate(panOffset.x, panOffset.y)
            canvasNative.scale(zoomScale, zoomScale)

            // 2. Draw Finished Strokes on Page
            for (stroke in pageData.strokes) {
                drawSingleStroke(stroke, isSelected = stroke.id in selectedStrokeIds)
            }

            // 3. Draw Live Stroke currently in progress
            if (liveStroke != null) {
                drawSingleStroke(liveStroke, isSelected = false)
            }

            // 4. Draw Text & Symbol Annotations (with Rotation support)
            for (textItem in pageData.textAnnotations) {
                val isSelected = textItem.id == selectedAnnotationId
                val paint = android.graphics.Paint().apply {
                    color = textItem.color.toInt()
                    textSize = textItem.fontSize
                    isAntiAlias = true
                    isFakeBoldText = true
                }

                canvasNative.save()
                canvasNative.translate(textItem.x, textItem.y)
                if (textItem.rotation != 0f) {
                    canvasNative.rotate(textItem.rotation)
                }
                canvasNative.drawText(textItem.text, 0f, 0f, paint)
                canvasNative.restore()

                // Highlight selected symbol with dashed bounding box on canvas
                if (isSelected) {
                    val textWidth = max(textItem.text.length * textItem.fontSize * 0.7f, 32f)
                    val textHeight = textItem.fontSize * 1.4f
                    drawRect(
                        color = Color(0x223B82F6),
                        topLeft = Offset(textItem.x - 8f, textItem.y - textHeight - 4f),
                        size = Size(textWidth + 16f, textHeight + 14f)
                    )
                    drawRect(
                        color = Color(0xFF2563EB),
                        topLeft = Offset(textItem.x - 8f, textItem.y - textHeight - 4f),
                        size = Size(textWidth + 16f, textHeight + 14f),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    )
                    // Draw 4 corner handles
                    val corners = listOf(
                        Offset(textItem.x - 8f, textItem.y - textHeight - 4f),
                        Offset(textItem.x + textWidth + 8f, textItem.y - textHeight - 4f),
                        Offset(textItem.x - 8f, textItem.y + 10f),
                        Offset(textItem.x + textWidth + 8f, textItem.y + 10f)
                    )
                    corners.forEach { corner ->
                        drawCircle(Color.White, radius = 5f, center = corner)
                        drawCircle(Color(0xFF2563EB), radius = 5f, center = corner, style = Stroke(2f))
                    }
                }
            }

            // 5. Draw Eraser circular indicator cursor when active
            if (activeTouchPos != null && (currentTool == ToolType.STROKE_ERASER || currentTool == ToolType.AREA_ERASER)) {
                val radius = eraserSize.radius
                drawCircle(
                    color = Color(0x33EF4444),
                    radius = radius,
                    center = activeTouchPos!!
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = radius,
                    center = activeTouchPos!!,
                    style = Stroke(width = 2f)
                )
            }

            // 6. Draw Lasso Selection Overlay if active
            if (lassoStart != null && lassoCurrent != null) {
                val left = min(lassoStart!!.x, lassoCurrent!!.x)
                val top = min(lassoStart!!.y, lassoCurrent!!.y)
                val width = kotlin.math.abs(lassoCurrent!!.x - lassoStart!!.x)
                val height = kotlin.math.abs(lassoCurrent!!.y - lassoStart!!.y)

                drawRect(
                    color = Color(0x333B82F6),
                    topLeft = Offset(left, top),
                    size = Size(width, height)
                )
                drawRect(
                    color = Color(0xFF3B82F6),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )
            }

            // 7. Draw Bounding Box around Selected Strokes
            if (selectedStrokeIds.isNotEmpty()) {
                val bounds = computeSelectedBounds(pageData.strokes, selectedStrokeIds)
                if (bounds != null) {
                    drawRect(
                        color = Color(0x1E3B82F6),
                        topLeft = Offset(bounds.left - 8f, bounds.top - 8f),
                        size = Size(bounds.width + 16f, bounds.height + 16f)
                    )
                    drawRect(
                        color = Color(0xFF2563EB),
                        topLeft = Offset(bounds.left - 8f, bounds.top - 8f),
                        size = Size(bounds.width + 16f, bounds.height + 16f),
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    )
                }
            }

            canvasNative.restore()
        }

        // Floating Action Bar for Selected Symbol Object
        if (selectedAnnotation != null) {
            val screenX = (selectedAnnotation.x * zoomScale + panOffset.x).roundToInt()
            val textHeight = selectedAnnotation.fontSize * 1.4f * zoomScale
            val screenY = ((selectedAnnotation.y - textHeight - 55f) * zoomScale + panOffset.y).roundToInt().coerceAtLeast(10)

            Surface(
                modifier = Modifier
                    .offset { IntOffset(screenX.coerceAtLeast(10), screenY) }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = selectedAnnotation.text,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    // Shrink size handle
                    IconButton(
                        onClick = { viewModel.resizeAnnotation(selectedAnnotation.id, -3f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Smaller", modifier = Modifier.size(16.dp))
                    }

                    // Enlarge size handle
                    IconButton(
                        onClick = { viewModel.resizeAnnotation(selectedAnnotation.id, +3f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Larger", modifier = Modifier.size(16.dp))
                    }

                    // Rotate 45 deg handle
                    IconButton(
                        onClick = { viewModel.rotateAnnotation(selectedAnnotation.id, 45f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate 45°", modifier = Modifier.size(16.dp))
                    }

                    // Delete independently
                    IconButton(
                        onClick = { viewModel.deleteSelectedAnnotation() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Symbol", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }

                    // Commit / Finish editing
                    IconButton(
                        onClick = { viewModel.commitSelectedAnnotation() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Commit Symbol", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCanvasBackground(
    paperStyle: PaperStyle,
    size: Size,
    zoomScale: Float,
    panOffset: DrawingPoint
) {
    when (paperStyle) {
        PaperStyle.PLAIN_WHITE -> {
            drawRect(Color.White)
        }
        PaperStyle.RULED -> {
            drawRect(Color(0xFFFEFDF9))

            val lineSpacing = 36f * zoomScale
            val startY = ((panOffset.y % lineSpacing) + lineSpacing) % lineSpacing
            var y = startY
            while (y < size.height) {
                if (y >= 0) {
                    drawLine(
                        color = Color(0xFFD4E0ED),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
                y += lineSpacing
            }

            // Red vertical margin line
            val marginX = (72f * zoomScale) + panOffset.x
            if (marginX in 0f..size.width) {
                drawLine(
                    color = Color(0xFFFCA5A5),
                    start = Offset(marginX, 0f),
                    end = Offset(marginX, size.height),
                    strokeWidth = 1.8f
                )
            }
        }
        PaperStyle.GRAPH -> {
            drawRect(Color(0xFFF8FAFC))

            val gridSpacing = 28f * zoomScale
            val startX = ((panOffset.x % gridSpacing) + gridSpacing) % gridSpacing
            var x = startX
            while (x < size.width) {
                if (x >= 0) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 0.8f
                    )
                }
                x += gridSpacing
            }

            val startY = ((panOffset.y % gridSpacing) + gridSpacing) % gridSpacing
            var y = startY
            while (y < size.height) {
                if (y >= 0) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.8f
                    )
                }
                y += gridSpacing
            }
        }
        PaperStyle.BLACKBOARD -> {
            drawRect(Color(0xFF142019)) // Deep blackboard green
        }
        PaperStyle.DARK_MODE -> {
            drawRect(Color(0xFF0F172A)) // Dark slate 900
            val lineSpacing = 36f * zoomScale
            val startY = ((panOffset.y % lineSpacing) + lineSpacing) % lineSpacing
            var y = startY
            while (y < size.height) {
                if (y >= 0) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.8f
                    )
                }
                y += lineSpacing
            }
        }
        PaperStyle.SEPIA -> {
            drawRect(Color(0xFFFBF5E8)) // Warm parchment
            val lineSpacing = 36f * zoomScale
            val startY = ((panOffset.y % lineSpacing) + lineSpacing) % lineSpacing
            var y = startY
            while (y < size.height) {
                if (y >= 0) {
                    drawLine(
                        color = Color(0xFFE8DCC8),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
                y += lineSpacing
            }
        }
        PaperStyle.AMOLED_BLACK -> {
            drawRect(Color(0xFF000000)) // Completely pure AMOLED pitch black: No lines, no grid, no margins
        }
    }
}

private fun DrawScope.drawSingleStroke(stroke: DrawingStroke, isSelected: Boolean) {
    val pts = stroke.points
    if (pts.isEmpty()) return

    val strokeColor = if (isSelected) Color(0xFF2563EB) else Color(stroke.color)
    val strokeAlpha = if (stroke.toolType == ToolType.HIGHLIGHTER) stroke.alpha else 1.0f

    if (pts.size == 1) {
        val p = pts[0]
        drawCircle(
            color = strokeColor.copy(alpha = strokeAlpha),
            radius = (stroke.width / 2f).coerceAtLeast(2f),
            center = Offset(p.x, p.y)
        )
        return
    }

    drawPath(
        path = stroke.composePath,
        color = strokeColor.copy(alpha = strokeAlpha),
        style = Stroke(
            width = stroke.width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun isInstantTabletAction(action: com.example.model.TabletAction): Boolean = when (action) {
    com.example.model.TabletAction.UNDO,
    com.example.model.TabletAction.REDO,
    com.example.model.TabletAction.ZOOM_IN,
    com.example.model.TabletAction.ZOOM_OUT,
    com.example.model.TabletAction.RESET_ZOOM,
    com.example.model.TabletAction.PREV_PAGE,
    com.example.model.TabletAction.NEXT_PAGE,
    com.example.model.TabletAction.CLEAR_PAGE,
    com.example.model.TabletAction.NEW_PAGE,
    com.example.model.TabletAction.DELETE_PAGE,
    com.example.model.TabletAction.TOGGLE_FULLSCREEN,
    com.example.model.TabletAction.TOGGLE_TOOLBAR,
    com.example.model.TabletAction.NONE -> true
    else -> false
}

private fun isInsideSelectedStrokes(
    x: Float,
    y: Float,
    strokes: List<DrawingStroke>,
    selectedIds: Set<String>
): Boolean {
    val bounds = computeSelectedBounds(strokes, selectedIds) ?: return false
    return x in bounds.left..bounds.right && y in bounds.top..bounds.bottom
}

private data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
}

private fun computeSelectedBounds(strokes: List<DrawingStroke>, selectedIds: Set<String>): Bounds? {
    var minX = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var found = false

    for (stroke in strokes) {
        if (stroke.id in selectedIds) {
            for (p in stroke.points) {
                found = true
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
        }
    }
    return if (found) Bounds(minX, minY, maxX, maxY) else null
}

package com.example.ime.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.StrokesSerializer
import com.example.model.AnswerDocument
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.NeetSubject
import com.example.model.PageData
import com.example.model.PaperStyle
import com.example.model.TextAnnotation
import com.example.model.ToolType
import com.example.model.ToolbarToolItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.hypot

enum class KeyboardHeightMode {
    COMPACT,    // ~285dp
    EXPANDED,   // ~480dp
    MAXIMIZED   // ~620dp
}

class ImeDocumentRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("inkkey_ime_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _tabletConfig = MutableStateFlow(com.example.model.TabletConfig.load(context))
    val tabletConfig = _tabletConfig.asStateFlow()
    private val _tabletFeedbackMessage = MutableStateFlow<String?>(null)
    val tabletFeedbackMessage = _tabletFeedbackMessage.asStateFlow()
    private val _uiTabletActions = MutableSharedFlow<com.example.model.TabletAction>(extraBufferCapacity = 16)
    val uiTabletActions = _uiTabletActions.asSharedFlow()
    private val barrelInput = com.example.util.BarrelInputRouter({ _tabletConfig.value }, ::executeTabletAction)
    val transientBarrelTool = barrelInput.tools.tool
    fun reloadTabletConfig() { _tabletConfig.value = com.example.model.TabletConfig.load(context); barrelInput.tools.refresh() }
    fun updateTabletConfig(config: com.example.model.TabletConfig) {
        _tabletConfig.value = config
        com.example.model.TabletConfig.save(context, config)
        barrelInput.tools.refresh()
    }
    fun clearTabletFeedback() { _tabletFeedbackMessage.value = null }
    fun clearTransientBarrelTool() = barrelInput.reset()
    fun handleTouchMotionEvent(event: android.view.MotionEvent) = barrelInput.touch(event)
    fun handleGenericMotionEvent(event: android.view.MotionEvent): Boolean = barrelInput.hover(event)
    fun triggerUpperBarrelFunction(down: Boolean = true) = barrelInput.tools.setPressed(com.example.util.BarrelToolController.Input.KEY_UPPER, down)
    fun triggerLowerBarrelFunction(down: Boolean = true) = barrelInput.tools.setPressed(com.example.util.BarrelToolController.Input.KEY_LOWER, down)

    private var lastHardwareKeySignature: String? = null
    private var lastHardwareKeyHandled: Boolean = false

    fun handleHardwareKeyEvent(event: android.view.KeyEvent): Boolean {
        val signature = "${event.deviceId}:${event.eventTime}:${event.action}:${event.keyCode}:${event.scanCode}:${event.metaState}"
        if (signature == lastHardwareKeySignature) return lastHardwareKeyHandled

        val config = _tabletConfig.value
        val isXpPen = com.example.util.XpPenExpressKeyResolver.isXpPen(event)
        val knownXpIndex = com.example.util.XpPenExpressKeyResolver.resolve(event)
        val customIndex = com.example.util.XpPenExpressKeyResolver.resolveCustomBinding(event.keyCode, event.scanCode, config.customKeyCodes, config.customScanCodes)
        val fallbackIndex = if (isXpPen) null else config.findMatchingKeyIndex(event.keyCode, event.scanCode, event)
        val keyIndex = customIndex ?: knownXpIndex ?: fallbackIndex
        val target = com.example.util.StylusHardwareDetector.classifyKeyEvent(event.keyCode, event.scanCode, event)
        val express = keyIndex != null && (isXpPen || customIndex != null)

        val handled = when {
            express -> {
                val action = config.expressKeyMappings[keyIndex] ?: com.example.model.TabletAction.NONE
                if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0 && action != com.example.model.TabletAction.NONE) {
                    executeTabletAction(action)
                    _tabletFeedbackMessage.value = "K$keyIndex: ${action.displayName}"
                }
                true
            }
            target != com.example.util.StylusHardwareDetector.BarrelTarget.NONE -> {
                val down = event.action == android.view.KeyEvent.ACTION_DOWN
                if (event.action == android.view.KeyEvent.ACTION_UP || (down && event.repeatCount == 0)) {
                    if (target == com.example.util.StylusHardwareDetector.BarrelTarget.UPPER_BARREL) {
                        triggerUpperBarrelFunction(down)
                    } else {
                        triggerLowerBarrelFunction(down)
                    }
                }
                true
            }
            isXpPen && com.example.util.XpPenExpressKeyResolver.isCompositePrefix(event) -> true
            isXpPen -> {
                if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    val rawName = android.view.KeyEvent.keyCodeToString(event.keyCode).removePrefix("KEYCODE_")
                    _tabletFeedbackMessage.value = "XP-Pen unmapped: $rawName · scan ${event.scanCode}"
                }
                true
            }
            else -> false
        }

        lastHardwareKeySignature = signature
        lastHardwareKeyHandled = handled
        return handled
    }

    fun executeTabletAction(action: com.example.model.TabletAction) {
        if (action != com.example.model.TabletAction.NONE) {
            _tabletFeedbackMessage.value = action.displayName
        }
        val tool = com.example.util.BarrelToolController.toolFor(action)
        if (tool != null) { setTool(tool); return }
        when (action) {
            com.example.model.TabletAction.UNDO -> undo()
            com.example.model.TabletAction.REDO -> redo()
            com.example.model.TabletAction.CLEAR_PAGE -> clearCurrentPage()
            com.example.model.TabletAction.NEW_PAGE -> addPage()
            com.example.model.TabletAction.DELETE_PAGE -> deleteCurrentPage()
            com.example.model.TabletAction.PREV_PAGE -> goToPage((_currentPageIndex.value - 1).coerceAtLeast(0))
            com.example.model.TabletAction.NEXT_PAGE -> if (_currentPageIndex.value + 1 < _document.value.pages.size) goToPage(_currentPageIndex.value + 1) else addPage()
            com.example.model.TabletAction.ZOOM_IN,
            com.example.model.TabletAction.ZOOM_OUT,
            com.example.model.TabletAction.RESET_ZOOM,
            com.example.model.TabletAction.TOGGLE_TOOLBAR,
            com.example.model.TabletAction.TOGGLE_FULLSCREEN -> _uiTabletActions.tryEmit(action)
            else -> Unit
        }
    }

    // Active document state
    private val _document = MutableStateFlow(loadPersistedDocument())
    val document: StateFlow<AnswerDocument> = _document.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    // Tools & appearance
    private val _currentTool = MutableStateFlow(ToolType.PEN)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()

    private val _penColor = MutableStateFlow(prefs.getLong("pen_color", 0xFF0F172AL))
    val penColor: StateFlow<Long> = _penColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow(prefs.getFloat("stroke_width", 4f))
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _highlighterColor = MutableStateFlow(0x70FBBF24L)
    val highlighterColor: StateFlow<Long> = _highlighterColor.asStateFlow()

    private val _eraserRadius = MutableStateFlow(28f)
    val eraserRadius: StateFlow<Float> = _eraserRadius.asStateFlow()

    private val _heightMode = MutableStateFlow(
        try {
            KeyboardHeightMode.valueOf(prefs.getString("height_mode", KeyboardHeightMode.COMPACT.name) ?: KeyboardHeightMode.COMPACT.name)
        } catch (e: Exception) {
            KeyboardHeightMode.COMPACT
        }
    )
    val heightMode: StateFlow<KeyboardHeightMode> = _heightMode.asStateFlow()

    // Selected Symbol (for Issue 2)
    private val _selectedSymbolId = MutableStateFlow<String?>(null)
    val selectedSymbolId: StateFlow<String?> = _selectedSymbolId.asStateFlow()

    // Toolbar custom arrangement (Issue 7)
    private val _toolbarItems = MutableStateFlow(loadToolbarOrder())
    val toolbarItems: StateFlow<List<ToolbarToolItem>> = _toolbarItems.asStateFlow()

    // Undo / Redo history stack (capped to 30 items)
    private val undoStack = mutableListOf<AnswerDocument>()
    private val redoStack = mutableListOf<AnswerDocument>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    init {
        updateUndoRedoStates()
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun pushUndoSnapshot() {
        undoStack.add(_document.value.copy())
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoStates()
    }

    fun trimMemory() {
        while (undoStack.size > 5) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoStates()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_document.value.copy())
            _document.value = previous
            updateUndoRedoStates()
            persistDocument()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_document.value.copy())
            _document.value = next
            updateUndoRedoStates()
            persistDocument()
        }
    }

    fun clearTransientState() {
        clearTransientBarrelTool()
        // Selection is document state: hiding the keyboard or opening its editor must not discard it.
    }

    // Tool setters
    fun setTool(tool: ToolType) {
        _currentTool.value = tool
        if (tool != ToolType.SYMBOL_EDIT) {
            _selectedSymbolId.value = null
        }
    }

    fun setPenColor(color: Long) {
        _penColor.value = color
        prefs.edit().putLong("pen_color", color).apply()
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width
        prefs.edit().putFloat("stroke_width", width).apply()
    }

    fun setEraserRadius(radius: Float) {
        _eraserRadius.value = radius
    }

    fun setHeightMode(mode: KeyboardHeightMode) {
        _heightMode.value = mode
        prefs.edit().putString("height_mode", mode.name).apply()
    }

    fun setPaperStyle(style: PaperStyle) {
        _document.value = _document.value.copy(paperStyle = style)
        if (style == PaperStyle.AMOLED_BLACK || style == PaperStyle.DARK_MODE || style == PaperStyle.BLACKBOARD) {
            if (_penColor.value == 0xFF0F172AL || _penColor.value == 0xFF1E293BL || _penColor.value == 0xFF000000L) {
                _penColor.value = 0xFFFFFFFFL
            }
        } else {
            if (_penColor.value == 0xFFFFFFFFL || _penColor.value == 0xFFF1F5F9L) {
                _penColor.value = 0xFF0F172AL
            }
        }
        persistDocument()
    }

    fun setSubject(subject: NeetSubject) {
        _document.value = _document.value.copy(subject = subject)
        persistDocument()
    }

    // Stroke management
    fun addStroke(stroke: DrawingStroke) {
        pushUndoSnapshot()
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                p.copy(strokes = p.strokes + stroke)
            } else p
        }
        _document.value = doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
        persistDocument()
    }

    private var hasRecordedUndoInEraseGesture = false

    fun startEraseGesture() {
        hasRecordedUndoInEraseGesture = false
    }

    fun endEraseGesture() {
        hasRecordedUndoInEraseGesture = false
    }

    fun eraseStrokesAt(x: Float, y: Float, radius: Float = 28f): Boolean {
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val page = doc.pages.getOrNull(pageIdx) ?: return false

        var removedAny = false
        val newStrokes = mutableListOf<DrawingStroke>()

        for (stroke in page.strokes) {
            val pts = stroke.points
            var intersects = false
            val maxDist = radius + stroke.width / 2f

            if (pts.size == 1) {
                if (hypot(pts[0].x - x, pts[0].y - y) <= maxDist) {
                    intersects = true
                }
            } else {
                for (i in 1 until pts.size) {
                    val x1 = pts[i - 1].x
                    val y1 = pts[i - 1].y
                    val x2 = pts[i].x
                    val y2 = pts[i].y
                    val l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
                    val dist = if (l2 == 0f) {
                        hypot(x - x1, y - y1)
                    } else {
                        val t = (((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / l2).coerceIn(0f, 1f)
                        val projX = x1 + t * (x2 - x1)
                        val projY = y1 + t * (y2 - y1)
                        hypot(x - projX, y - projY)
                    }
                    if (dist <= maxDist) {
                        intersects = true
                        break
                    }
                }
            }

            if (intersects) {
                removedAny = true
            } else {
                newStrokes.add(stroke)
            }
        }

        // Also check if any placed symbol is hit by the eraser
        val survivingSymbols = page.textAnnotations.filterNot { sym ->
            val w = kotlin.math.max(sym.text.length * sym.fontSize * 1.2f, 40f)
            val h = kotlin.math.max(sym.fontSize * 1.4f, 40f)
            x in (sym.x - radius)..(sym.x + w + radius) &&
                    y in (sym.y - h - radius)..(sym.y + radius)
        }
        val symbolRemoved = survivingSymbols.size != page.textAnnotations.size

        if (removedAny || symbolRemoved) {
            if (!hasRecordedUndoInEraseGesture) {
                pushUndoSnapshot()
                hasRecordedUndoInEraseGesture = true
            }
            val updatedPages = doc.pages.map { p ->
                if (p.pageIndex == pageIdx) p.copy(strokes = newStrokes, textAnnotations = survivingSymbols) else p
            }
            _document.value = doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
            persistDocument()
        }
        return removedAny || symbolRemoved
    }

    fun eraseAreaStrokes(x: Float, y: Float, radius: Float = 28f): Boolean {
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val page = doc.pages.getOrNull(pageIdx) ?: return false

        var anyModified = false
        val newStrokes = mutableListOf<DrawingStroke>()
        val effectiveRadius = radius.coerceAtLeast(12f)

        for (stroke in page.strokes) {
            val pts = stroke.points
            if (pts.isEmpty()) continue

            // Quick AABB bounds check
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            for (p in pts) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
            val strokeRadius = effectiveRadius + stroke.width / 2f
            if (x < minX - strokeRadius || x > maxX + strokeRadius ||
                y < minY - strokeRadius || y > maxY + strokeRadius
            ) {
                newStrokes.add(stroke)
                continue
            }

            // Subdivide stroke segments densely to detect fine intersections
            val densePoints = mutableListOf<DrawingPoint>()
            if (pts.size == 1) {
                densePoints.add(pts[0])
            } else {
                val step = (effectiveRadius / 3f).coerceIn(3f, 10f)
                densePoints.add(pts[0])
                for (i in 1 until pts.size) {
                    val pPrev = pts[i - 1]
                    val pCurr = pts[i]
                    val dist = hypot(pCurr.x - pPrev.x, pCurr.y - pPrev.y)
                    val numSteps = (dist / step).toInt()
                    if (numSteps > 1) {
                        for (s in 1 until numSteps) {
                            val t = s.toFloat() / numSteps
                            val nx = pPrev.x + t * (pCurr.x - pPrev.x)
                            val ny = pPrev.y + t * (pCurr.y - pPrev.y)
                            val np = pPrev.pressure + t * (pCurr.pressure - pPrev.pressure)
                            densePoints.add(DrawingPoint(nx, ny, np))
                        }
                    }
                    densePoints.add(pCurr)
                }
            }

            // Check which points are erased
            var touchedByEraser = false
            val segments = mutableListOf<MutableList<DrawingPoint>>()
            var currentSeg = mutableListOf<DrawingPoint>()

            for (p in densePoints) {
                val dist = hypot(p.x - x, p.y - y)
                if (dist > strokeRadius) {
                    currentSeg.add(p)
                } else {
                    touchedByEraser = true
                    if (currentSeg.isNotEmpty()) {
                        segments.add(currentSeg)
                        currentSeg = mutableListOf()
                    }
                }
            }
            if (currentSeg.isNotEmpty()) {
                segments.add(currentSeg)
            }

            if (!touchedByEraser) {
                newStrokes.add(stroke)
            } else {
                anyModified = true
                for (seg in segments) {
                    if (seg.size >= 2) {
                        newStrokes.add(stroke.copy(id = java.util.UUID.randomUUID().toString(), points = seg))
                    } else if (seg.size == 1) {
                        val single = seg[0]
                        val dotPts = listOf(single, single.copy(x = single.x + 0.5f))
                        newStrokes.add(stroke.copy(id = java.util.UUID.randomUUID().toString(), points = dotPts))
                    }
                }
            }
        }

        // Erase intersecting symbols
        val remainingAnnotations = mutableListOf<TextAnnotation>()
        var anyAnnotationRemoved = false
        for (annotation in page.textAnnotations) {
            if (annotation.hitTest(x, y, padding = effectiveRadius)) {
                anyAnnotationRemoved = true
                if (_selectedSymbolId.value == annotation.id) {
                    _selectedSymbolId.value = null
                }
            } else {
                remainingAnnotations.add(annotation)
            }
        }

        if (anyModified || anyAnnotationRemoved) {
            if (!hasRecordedUndoInEraseGesture) {
                pushUndoSnapshot()
                hasRecordedUndoInEraseGesture = true
            }
            val updatedPages = doc.pages.map { p ->
                if (p.pageIndex == pageIdx) p.copy(
                    strokes = newStrokes,
                    textAnnotations = remainingAnnotations
                ) else p
            }
            _document.value = doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
            persistDocument()
        }
        return anyModified || anyAnnotationRemoved
    }


    // Symbol Editing (Issue 2)
    fun insertSymbolOnCanvas(symbolText: String, x: Float = 120f, y: Float = 120f) {
        pushUndoSnapshot()
        val symbol = TextAnnotation(
            text = symbolText,
            x = x,
            y = y,
            fontSize = 28f,
            color = _penColor.value,
            isSelected = true
        )
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                p.copy(textAnnotations = p.textAnnotations + symbol)
            } else p
        }
        _document.value = doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
        _selectedSymbolId.value = symbol.id
        _currentTool.value = ToolType.SYMBOL_EDIT
        persistDocument()
    }

    fun selectSymbol(symbolId: String?) {
        _selectedSymbolId.value = symbolId
        if (symbolId != null) {
            _currentTool.value = ToolType.SYMBOL_EDIT
        }
    }

    fun updateSelectedSymbolText(text: String) {
        val id = _selectedSymbolId.value ?: return
        updateSymbolText(id, text)
    }

    fun updateSymbolText(id: String, text: String) {
        if (text.isBlank()) return
        val doc = _document.value
        val index = _currentPageIndex.value
        if (doc.pages.getOrNull(index)?.textAnnotations?.none { it.id == id && it.text != text } != false) return
        pushUndoSnapshot()
        _document.value = doc.copy(pages = doc.pages.map { page ->
            if (page.pageIndex == index) page.copy(textAnnotations = page.textAnnotations.map { if (it.id == id) it.copy(text = text) else it }) else page
        })
        persistDocument()
    }

    fun moveSelectedSymbol(dx: Float, dy: Float) {
        val targetId = _selectedSymbolId.value ?: return
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                val updatedSymbols = p.textAnnotations.map { s ->
                    if (s.id == targetId) s.copy(x = s.x + dx, y = s.y + dy) else s
                }
                p.copy(textAnnotations = updatedSymbols)
            } else p
        }
        _document.value = doc.copy(pages = updatedPages)
    }

    fun commitSymbolMove() {
        pushUndoSnapshot()
        persistDocument()
    }

    fun finishMoveSymbol() {
        persistDocument()
    }

    fun duplicateSelectedSymbol() {
        val targetId = _selectedSymbolId.value ?: return
        pushUndoSnapshot()
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        var newId: String? = null
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                val target = p.textAnnotations.find { it.id == targetId }
                if (target != null) {
                    val cloned = target.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        x = target.x + 35f,
                        y = target.y + 35f
                    )
                    newId = cloned.id
                    p.copy(textAnnotations = p.textAnnotations + cloned)
                } else p
            } else p
        }
        _document.value = doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
        if (newId != null) {
            _selectedSymbolId.value = newId
            _currentTool.value = ToolType.SYMBOL_EDIT
        }
        persistDocument()
    }

    fun resizeSelectedSymbol(deltaSize: Float) {
        val targetId = _selectedSymbolId.value ?: return
        pushUndoSnapshot()
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                val updatedSymbols = p.textAnnotations.map { s ->
                    if (s.id == targetId) s.copy(fontSize = (s.fontSize + deltaSize).coerceIn(14f, 72f)) else s
                }
                p.copy(textAnnotations = updatedSymbols)
            } else p
        }
        _document.value = doc.copy(pages = updatedPages)
        persistDocument()
    }

    fun deleteSelectedSymbol() {
        val targetId = _selectedSymbolId.value ?: return
        pushUndoSnapshot()
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                p.copy(textAnnotations = p.textAnnotations.filterNot { it.id == targetId })
            } else p
        }
        _document.value = doc.copy(pages = updatedPages)
        _selectedSymbolId.value = null
        _currentTool.value = ToolType.PEN
        persistDocument()
    }

    fun bringSymbolToFront() {
        val targetId = _selectedSymbolId.value ?: return
        pushUndoSnapshot()
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                val target = p.textAnnotations.find { it.id == targetId }
                if (target != null) {
                    val remaining = p.textAnnotations.filterNot { it.id == targetId }
                    p.copy(textAnnotations = remaining + target)
                } else p
            } else p
        }
        _document.value = doc.copy(pages = updatedPages)
        persistDocument()
    }

    fun rotateSelectedSymbol(deltaDegrees: Float) {
        val targetId = _selectedSymbolId.value ?: return
        pushUndoSnapshot()
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                val updatedSymbols = p.textAnnotations.map { s ->
                    if (s.id == targetId) s.copy(rotation = (s.rotation + deltaDegrees) % 360f) else s
                }
                p.copy(textAnnotations = updatedSymbols)
            } else p
        }
        _document.value = doc.copy(pages = updatedPages)
        persistDocument()
    }

    // Page Management (Issue 4)

    fun goToPage(index: Int) {
        if (index in 0 until _document.value.pages.size) {
            _currentPageIndex.value = index
            _selectedSymbolId.value = null
        }
    }

    fun addPage() {
        pushUndoSnapshot()
        val doc = _document.value
        val newIndex = doc.pages.size
        val newPage = PageData(pageIndex = newIndex)
        _document.value = doc.copy(pages = doc.pages + newPage)
        _currentPageIndex.value = newIndex
        _selectedSymbolId.value = null
        persistDocument()
    }

    fun clearCurrentPage() {
        pushUndoSnapshot()
        val doc = _document.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) p.copy(strokes = emptyList(), textAnnotations = emptyList()) else p
        }
        _document.value = doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
        _selectedSymbolId.value = null
        persistDocument()
    }

    fun deleteCurrentPage() {
        val doc = _document.value
        if (doc.pages.size <= 1) {
            clearCurrentPage()
            return
        }
        pushUndoSnapshot()
        val pageIdx = _currentPageIndex.value
        val remaining = doc.pages.filterIndexed { index, _ -> index != pageIdx }
        val reindexed = remaining.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
        val nextIdx = (pageIdx - 1).coerceAtLeast(0).coerceAtMost(reindexed.size - 1)
        _document.value = doc.copy(pages = reindexed, updatedAt = System.currentTimeMillis())
        _currentPageIndex.value = nextIdx
        _selectedSymbolId.value = null
        persistDocument()
    }

    // Toolbar custom arrangement (Issue 7)
    fun updateToolbarOrder(items: List<ToolbarToolItem>) {
        _toolbarItems.value = items
        val serialized = items.joinToString(",") { it.id }
        prefs.edit().putString("toolbar_order", serialized).apply()
    }

    fun resetToolbarOrder() {
        val defaults = ToolbarToolItem.values().toList()
        updateToolbarOrder(defaults)
    }

    private fun loadToolbarOrder(): List<ToolbarToolItem> {
        val raw = prefs.getString("toolbar_order", null) ?: return ToolbarToolItem.values().toList()
        val ids = raw.split(",").map { it.trim() }
        val map = ToolbarToolItem.values().associateBy { it.id }
        val loaded = ids.mapNotNull { map[it] }.toMutableList()
        for (item in ToolbarToolItem.values()) {
            if (item !in loaded) loaded.add(item)
        }
        return loaded
    }

    // Persistence
    private var persistJob: Job? = null

    private fun persistDocument() {
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(500)
            try {
                val doc = _document.value
                val pagesJson = StrokesSerializer.serializePages(doc.pages)
                prefs.edit()
                    .putString("persisted_ime_pages", pagesJson)
                    .putString("persisted_ime_title", doc.title)
                    .putString("persisted_ime_subject", doc.subject.name)
                    .putString("persisted_ime_paper", doc.paperStyle.name)
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadPersistedDocument(): AnswerDocument {
        return try {
            val pagesJson = prefs.getString("persisted_ime_pages", null)
            val title = prefs.getString("persisted_ime_title", "NEET Solution") ?: "NEET Solution"
            val subjectName = prefs.getString("persisted_ime_subject", NeetSubject.PHYSICS.name)
            val paperName = prefs.getString("persisted_ime_paper", PaperStyle.RULED.name)

            val subject = try { NeetSubject.valueOf(subjectName ?: "") } catch (e: Exception) { NeetSubject.PHYSICS }
            val paper = try { PaperStyle.valueOf(paperName ?: "") } catch (e: Exception) { PaperStyle.RULED }
            val pages = if (!pagesJson.isNullOrBlank()) StrokesSerializer.deserializePages(pagesJson) else listOf(PageData(0))

            AnswerDocument(
                title = title,
                subject = subject,
                paperStyle = paper,
                pages = pages
            )
        } catch (e: Exception) {
            AnswerDocument(
                title = "NEET Solution",
                subject = NeetSubject.PHYSICS,
                paperStyle = PaperStyle.RULED,
                pages = listOf(PageData(0))
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ImeDocumentRepository? = null

        fun getInstance(context: Context): ImeDocumentRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ImeDocumentRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

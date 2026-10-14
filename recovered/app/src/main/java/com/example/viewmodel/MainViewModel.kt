package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AnswerRepository
import com.example.data.InkKeyDatabase
import com.example.model.AnswerDocument
import com.example.model.CanvasScrollMode
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.EraserSize
import com.example.model.NeetSubject
import com.example.model.PageData
import com.example.model.PaperStyle
import com.example.model.ShapeType
import com.example.model.TextAnnotation
import com.example.model.ToolType
import com.example.model.ToolbarToolItem
import com.example.model.TabletConfig
import com.example.model.TabletAction
import com.example.model.PressureCurve
import com.example.model.HardwareInputDebugEvent
import com.example.model.TabletDeviceInfo
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.InputDevice
import com.example.util.ShapeRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot

enum class ScreenState {
    IME_SETUP,
    HOME,
    CANVAS,
    SETTINGS
}

data class PageSnapshot(
    val strokes: List<DrawingStroke>,
    val textAnnotations: List<TextAnnotation>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AnswerRepository
    private val prefs = application.getSharedPreferences("inkkey_main_prefs", Context.MODE_PRIVATE)

    init {
        val database = InkKeyDatabase.getInstance(application)
        repository = AnswerRepository(database.answerDao())
    }

    val allAnswers: StateFlow<List<AnswerDocument>> = repository.allAnswers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentScreen = MutableStateFlow(ScreenState.IME_SETUP)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()
    private val screenBackStack = ArrayDeque<ScreenState>()

    private val _currentDocument = MutableStateFlow(
        AnswerDocument(
            title = "Physics Numericals - Kinematics",
            subject = NeetSubject.PHYSICS,
            questionNumber = "Q.1",
            paperStyle = PaperStyle.RULED,
            scrollMode = CanvasScrollMode.BOTH_DIRECTION
        )
    )
    val currentDocument: StateFlow<AnswerDocument> = _currentDocument.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    // Tool settings
    private val _currentTool = MutableStateFlow(ToolType.PEN)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()

    private val _penColor = MutableStateFlow(0xFF0F172AL) // Slate 900
    val penColor: StateFlow<Long> = _penColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow(4f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _highlighterColor = MutableStateFlow(0x70FBBF24L) // Amber highlighter with alpha
    val highlighterColor: StateFlow<Long> = _highlighterColor.asStateFlow()

    private val _eraserSize = MutableStateFlow(EraserSize.MEDIUM)
    val eraserSize: StateFlow<EraserSize> = _eraserSize.asStateFlow()

    private val _shapeModeEnabled = MutableStateFlow(false)
    val shapeModeEnabled: StateFlow<Boolean> = _shapeModeEnabled.asStateFlow()

    private val _selectedShapeType = MutableStateFlow(ShapeType.NONE)
    val selectedShapeType: StateFlow<ShapeType> = _selectedShapeType.asStateFlow()

    private val _palmRejectionEnabled = MutableStateFlow(true)
    val palmRejectionEnabled: StateFlow<Boolean> = _palmRejectionEnabled.asStateFlow()

    // Zoom & Pan
    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(DrawingPoint(0f, 0f))
    val panOffset: StateFlow<DrawingPoint> = _panOffset.asStateFlow()

    // Object & Symbol selection
    private val _selectedAnnotationId = MutableStateFlow<String?>(null)
    val selectedAnnotationId: StateFlow<String?> = _selectedAnnotationId.asStateFlow()

    // Lasso selection of strokes
    private val _selectedStrokeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedStrokeIds: StateFlow<Set<String>> = _selectedStrokeIds.asStateFlow()

    // Live stroke being drawn right now
    private val _liveStroke = MutableStateFlow<DrawingStroke?>(null)
    val liveStroke: StateFlow<DrawingStroke?> = _liveStroke.asStateFlow()

    // Undo / Redo stacks: Map of pageIndex -> Stack of full page snapshots
    private val undoStacks = mutableMapOf<Int, MutableList<PageSnapshot>>()
    private val redoStacks = mutableMapOf<Int, MutableList<PageSnapshot>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Full Screen Mode
    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }

    fun setFullScreen(enabled: Boolean) {
        _isFullScreen.value = enabled
    }

    // Toolbar collapsed state
    private val _isToolbarCollapsed = MutableStateFlow(false)
    val isToolbarCollapsed: StateFlow<Boolean> = _isToolbarCollapsed.asStateFlow()

    fun toggleToolbar() {
        _isToolbarCollapsed.value = !_isToolbarCollapsed.value
    }

    fun setToolbarCollapsed(collapsed: Boolean) {
        _isToolbarCollapsed.value = collapsed
    }

    // Physical Stylus & Drawing Tablet (XP-PEN Deco Mini 7) Configuration
    private val _tabletConfig = MutableStateFlow(TabletConfig.load(getApplication()))
    val tabletConfig: StateFlow<TabletConfig> = _tabletConfig.asStateFlow()

    private val _latestDebugEvent = MutableStateFlow<HardwareInputDebugEvent?>(null)
    val latestDebugEvent: StateFlow<HardwareInputDebugEvent?> = _latestDebugEvent.asStateFlow()

    private val _tabletFeedbackMessage = MutableStateFlow<String?>(null)
    val tabletFeedbackMessage: StateFlow<String?> = _tabletFeedbackMessage.asStateFlow()

    fun updateTabletConfig(config: TabletConfig) {
        _tabletConfig.value = config
        TabletConfig.save(getApplication(), config)
        barrelInput.tools.refresh()
    }

    fun clearTabletFeedback() {
        _tabletFeedbackMessage.value = null
    }

    fun learnExpressKey(keyIndex: Int, keyCode: Int, scanCode: Int) {
        val updatedKeys = _tabletConfig.value.customKeyCodes.toMutableMap()
        val updatedScans = _tabletConfig.value.customScanCodes.toMutableMap()
        if (keyCode != 0) updatedKeys[keyIndex] = keyCode
        if (scanCode != 0) updatedScans[keyIndex] = scanCode
        updateTabletConfig(_tabletConfig.value.copy(
            customKeyCodes = updatedKeys,
            customScanCodes = updatedScans
        ))
    }

    fun executeTabletAction(action: TabletAction) {
        when (action) {
            TabletAction.UNDO -> undo()
            TabletAction.REDO -> redo()
            TabletAction.PEN -> setTool(ToolType.PEN)
            TabletAction.AREA_ERASER -> setTool(ToolType.AREA_ERASER)
            TabletAction.STROKE_ERASER -> setTool(ToolType.STROKE_ERASER)
            TabletAction.HAND -> setTool(ToolType.HAND)
            TabletAction.LASSO -> setTool(ToolType.LASSO)
            TabletAction.TOGGLE_FULLSCREEN -> toggleFullScreen()
            TabletAction.TOGGLE_TOOLBAR -> toggleToolbar()
            TabletAction.ZOOM_IN -> zoomIn()
            TabletAction.ZOOM_OUT -> zoomOut()
            TabletAction.RESET_ZOOM -> resetZoom()
            TabletAction.CLEAR_PAGE -> clearCurrentPage()
            TabletAction.NEW_PAGE -> addPage()
            TabletAction.DELETE_PAGE -> deleteCurrentPage()
            TabletAction.PREV_PAGE -> previousPage()
            TabletAction.NEXT_PAGE -> nextPage()
            TabletAction.NONE -> Unit
        }
    }

    fun clearExpressKeyBinding(keyIndex: Int) {
        val updatedKeys = _tabletConfig.value.customKeyCodes.toMutableMap()
        val updatedScans = _tabletConfig.value.customScanCodes.toMutableMap()
        updatedKeys.remove(keyIndex)
        updatedScans.remove(keyIndex)
        updateTabletConfig(_tabletConfig.value.copy(
            customKeyCodes = updatedKeys,
            customScanCodes = updatedScans
        ))
    }

    private val barrelInput = com.example.util.BarrelInputRouter({ _tabletConfig.value }, ::executeTabletAction)
    val transientBarrelTool = barrelInput.tools.tool
    val isBarrelActive = barrelInput.tools.active
    val upperTrialToolOverride: ToolType? get() = transientBarrelTool.value
    fun clearTransientBarrelTool() = barrelInput.reset()
    fun triggerUpperBarrelFunction(isDown: Boolean = true): Boolean {
        barrelInput.tools.setPressed(com.example.util.BarrelToolController.Input.KEY_UPPER, isDown)
        return true
    }
    fun triggerLowerBarrelFunction(isDown: Boolean = true): Boolean {
        barrelInput.tools.setPressed(com.example.util.BarrelToolController.Input.KEY_LOWER, isDown)
        return true
    }
    fun handleUpperTrialHover(event: MotionEvent) { barrelInput.hover(event) }
    fun resetUpperTrialInput() = barrelInput.reset()
    fun handleTouchMotionEvent(event: MotionEvent): Boolean { barrelInput.touch(event); return false }
    fun handleGenericMotionEvent(event: MotionEvent): Boolean = barrelInput.hover(event)

    fun handleHardwareKeyEvent(keyCode: Int, scanCode: Int = 0, event: KeyEvent? = null): Boolean {
        val config = _tabletConfig.value
        val device = event?.device
        val keyIndex = config.findMatchingKeyIndex(keyCode, scanCode, event)
        val target = com.example.util.StylusHardwareDetector.classifyKeyEvent(keyCode, scanCode, event)
        val isXpPen = device?.name?.contains("ugtablet deco mini7 v2", ignoreCase = true) == true
        val custom = config.customKeyCodes.values.any { it != 0 && it == keyCode } ||
            config.customScanCodes.values.any { it != 0 && it == scanCode }
        val express = keyIndex != null && (isXpPen || custom || target == com.example.util.StylusHardwareDetector.BarrelTarget.NONE)
        _latestDebugEvent.value = HardwareInputDebugEvent(
            eventType = "Key ${if (event?.action == KeyEvent.ACTION_UP) "UP" else "DOWN"}",
            deviceName = device?.name ?: "Physical Input", deviceId = device?.id ?: 0,
            vendorId = device?.vendorId ?: 0, productId = device?.productId ?: 0,
            keyCode = keyCode, keyName = KeyEvent.keyCodeToString(keyCode), scanCode = scanCode,
            source = "KEYBOARD / STYLUS", resolvedAction = if (express) "Express Key K$keyIndex" else target.name)
        if (express) {
            val action = config.expressKeyMappings[keyIndex] ?: TabletAction.NONE
            if ((event == null || event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) && action != TabletAction.NONE) {
                executeTabletAction(action)
                _tabletFeedbackMessage.value = "K$keyIndex: ${action.displayName}"
            }
            return true
        }
        if (target != com.example.util.StylusHardwareDetector.BarrelTarget.NONE) {
            val down = event == null || event.action == KeyEvent.ACTION_DOWN
            if (!down || event == null || event.repeatCount == 0) {
                if (target == com.example.util.StylusHardwareDetector.BarrelTarget.UPPER_BARREL) triggerUpperBarrelFunction(down)
                else triggerLowerBarrelFunction(down)
            }
            return true
        }
        return false
    }

    // Customizable Toolbar
    private val defaultToolbarOrder = listOf(
        ToolbarToolItem.PEN,
        ToolbarToolItem.HAND,
        ToolbarToolItem.HIGHLIGHTER,
        ToolbarToolItem.AREA_ERASER,
        ToolbarToolItem.STROKE_ERASER,
        ToolbarToolItem.SYMBOLS,
        ToolbarToolItem.SHAPE,
        ToolbarToolItem.UNDO_REDO,
        ToolbarToolItem.PALETTE,
        ToolbarToolItem.PAGES,
        ToolbarToolItem.THEMES,
        ToolbarToolItem.CLEAR
    )

    private val _toolbarItems = MutableStateFlow<List<ToolbarToolItem>>(loadToolbarPreferences())
    val toolbarItems: StateFlow<List<ToolbarToolItem>> = _toolbarItems.asStateFlow()

    // Autosave job
    private var autoSaveJob: Job? = null

    init {
        // Attempt to restore last unfinished answer on launch
        viewModelScope.launch {
            val latest = repository.getLatestAnswer()
            if (latest != null) {
                _currentDocument.value = latest
                _currentPageIndex.value = 0
                resetUndoRedo()
            }
        }
    }

    private fun loadToolbarPreferences(): List<ToolbarToolItem> {
        val saved = prefs.getString("canvas_toolbar_order", null) ?: return defaultToolbarOrder
        return try {
            val ids = saved.split(",")
            val mapped = ids.mapNotNull { id -> ToolbarToolItem.values().find { it.id == id } }
            if (mapped.size >= 4) mapped else defaultToolbarOrder
        } catch (e: Exception) {
            defaultToolbarOrder
        }
    }

    fun updateToolbarOrder(newOrder: List<ToolbarToolItem>) {
        _toolbarItems.value = newOrder
        val joined = newOrder.joinToString(",") { it.id }
        prefs.edit().putString("canvas_toolbar_order", joined).apply()
    }

    fun resetToolbarOrder() {
        _toolbarItems.value = defaultToolbarOrder
        prefs.edit().remove("canvas_toolbar_order").apply()
    }

    fun navigateTo(screen: ScreenState) {
        if (_currentScreen.value != screen) {
            screenBackStack.addLast(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack() {
        if (screenBackStack.isNotEmpty()) {
            _currentScreen.value = screenBackStack.removeLast()
        } else {
            _currentScreen.value = ScreenState.HOME
        }
    }

    fun startNewAnswer(
        subject: NeetSubject = NeetSubject.PHYSICS,
        paperStyle: PaperStyle = PaperStyle.RULED,
        questionNum: String = "Q.1",
        title: String = ""
    ) {
        val generatedTitle = if (title.isBlank()) {
            "${subject.displayName} Solution - $questionNum"
        } else title

        val newDoc = AnswerDocument(
            title = generatedTitle,
            subject = subject,
            questionNumber = questionNum,
            paperStyle = paperStyle
        )
        if (paperStyle == PaperStyle.AMOLED_BLACK || paperStyle == PaperStyle.DARK_MODE || paperStyle == PaperStyle.BLACKBOARD) {
            if (_penColor.value == 0xFF0F172AL || _penColor.value == 0xFF1E293BL || _penColor.value == 0xFF000000L) {
                _penColor.value = 0xFFFFFFFFL
            }
        }
        _currentDocument.value = newDoc
        _currentPageIndex.value = 0
        _selectedAnnotationId.value = null
        _selectedStrokeIds.value = emptySet()
        _zoomScale.value = 1f
        _panOffset.value = DrawingPoint(0f, 0f)
        resetUndoRedo()

        viewModelScope.launch {
            repository.saveAnswer(newDoc)
        }
    }

    fun loadAnswer(id: String) {
        viewModelScope.launch {
            val answer = repository.getAnswerById(id)
            if (answer != null) {
                _currentDocument.value = answer
                _currentPageIndex.value = 0
                _selectedAnnotationId.value = null
                _selectedStrokeIds.value = emptySet()
                _zoomScale.value = 1f
                _panOffset.value = DrawingPoint(0f, 0f)
                resetUndoRedo()
            }
        }
    }

    fun openAnswer(id: String) {
        loadAnswer(id)
    }

    fun openAnswer(answer: AnswerDocument) {
        _currentDocument.value = answer
        _currentPageIndex.value = 0
        _selectedAnnotationId.value = null
        _selectedStrokeIds.value = emptySet()
        _zoomScale.value = 1f
        _panOffset.value = DrawingPoint(0f, 0f)
        resetUndoRedo()
    }

    fun setCurrentDocument(doc: AnswerDocument) {
        _currentDocument.value = doc
        _currentPageIndex.value = 0
        _selectedAnnotationId.value = null
        _selectedStrokeIds.value = emptySet()
        resetUndoRedo()
    }

    fun resumeLastAnswer() {
        viewModelScope.launch {
            val latest = repository.getLatestAnswer()
            if (latest != null) {
                _currentDocument.value = latest
                _currentPageIndex.value = 0
                _selectedAnnotationId.value = null
                _selectedStrokeIds.value = emptySet()
                _zoomScale.value = 1f
                _panOffset.value = DrawingPoint(0f, 0f)
                resetUndoRedo()
            }
        }
    }


    fun deleteAnswer(id: String) {
        viewModelScope.launch {
            repository.deleteAnswer(id)
        }
    }

    fun duplicateAnswer(answer: AnswerDocument) {
        viewModelScope.launch {
            val duplicated = answer.copy(
                id = java.util.UUID.randomUUID().toString(),
                title = "${answer.title} (Copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveAnswer(duplicated)
        }
    }

    fun renameAnswer(id: String, newTitle: String, newQuestionNum: String) {
        viewModelScope.launch {
            val current = _currentDocument.value
            if (current.id == id) {
                val updated = current.copy(title = newTitle, questionNumber = newQuestionNum)
                _currentDocument.value = updated
                repository.saveAnswer(updated)
            } else {
                val existing = repository.getAnswerById(id)
                if (existing != null) {
                    val updated = existing.copy(title = newTitle, questionNumber = newQuestionNum)
                    repository.saveAnswer(updated)
                }
            }
        }
    }

    // Tool selection
    fun setTool(tool: ToolType) {
        _currentTool.value = tool
        if (tool != ToolType.LASSO) {
            _selectedStrokeIds.value = emptySet()
        }
        if (tool != ToolType.SYMBOL_EDIT) {
            _selectedAnnotationId.value = null
        }
    }

    fun setPenColor(color: Long) {
        _penColor.value = color
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width
    }

    fun setHighlighterColor(color: Long) {
        _highlighterColor.value = color
    }

    fun setEraserSize(size: EraserSize) {
        _eraserSize.value = size
    }

    fun toggleShapeMode(shapeType: ShapeType = ShapeType.NONE) {
        if (_shapeModeEnabled.value && _selectedShapeType.value == shapeType) {
            _shapeModeEnabled.value = false
            _selectedShapeType.value = ShapeType.NONE
        } else {
            _shapeModeEnabled.value = true
            _selectedShapeType.value = shapeType
        }
    }

    fun disableShapeMode() {
        _shapeModeEnabled.value = false
        _selectedShapeType.value = ShapeType.NONE
    }

    fun togglePalmRejection() {
        _palmRejectionEnabled.value = !_palmRejectionEnabled.value
    }

    // Zoom & Pan
    fun setZoomScale(scale: Float) {
        _zoomScale.value = scale.coerceIn(0.5f, 3.5f)
    }

    fun zoomIn() {
        setZoomScale(_zoomScale.value + 0.1f)
    }

    fun zoomOut() {
        setZoomScale(_zoomScale.value - 0.1f)
    }

    fun resetZoom() {
        _zoomScale.value = 1f
        _panOffset.value = DrawingPoint(0f, 0f)
    }

    fun updatePanOffset(dx: Float, dy: Float) {
        val curr = _panOffset.value
        _panOffset.value = DrawingPoint(curr.x + dx, curr.y + dy)
    }

    // Document properties
    fun setPaperStyle(style: PaperStyle) {
        _currentDocument.value = _currentDocument.value.copy(paperStyle = style)
        if (style == PaperStyle.AMOLED_BLACK || style == PaperStyle.DARK_MODE || style == PaperStyle.BLACKBOARD) {
            if (_penColor.value == 0xFF0F172AL || _penColor.value == 0xFF1E293BL || _penColor.value == 0xFF000000L) {
                _penColor.value = 0xFFFFFFFFL
            }
        } else {
            if (_penColor.value == 0xFFFFFFFFL || _penColor.value == 0xFFF1F5F9L) {
                _penColor.value = 0xFF0F172AL
            }
        }
        scheduleAutoSave()
    }

    fun setScrollMode(mode: CanvasScrollMode) {
        _currentDocument.value = _currentDocument.value.copy(scrollMode = mode)
        scheduleAutoSave()
    }

    fun setSubject(subject: NeetSubject) {
        _currentDocument.value = _currentDocument.value.copy(subject = subject)
        scheduleAutoSave()
    }

    fun updateDocumentInfo(title: String, questionNum: String) {
        _currentDocument.value = _currentDocument.value.copy(title = title, questionNumber = questionNum)
        scheduleAutoSave()
    }

    // Page navigation & management
    fun goToPage(index: Int) {
        if (index in 0 until _currentDocument.value.pages.size) {
            _currentPageIndex.value = index
            _selectedAnnotationId.value = null
            _selectedStrokeIds.value = emptySet()
            updateUndoRedoStates()
        }
    }

    fun previousPage() {
        val cur = _currentPageIndex.value
        if (cur > 0) goToPage(cur - 1)
    }

    fun nextPage() {
        val cur = _currentPageIndex.value
        val total = _currentDocument.value.pages.size
        if (cur < total - 1) goToPage(cur + 1)
    }

    fun addPage() {
        val doc = _currentDocument.value
        val newPageIndex = doc.pages.size
        val newPage = PageData(pageIndex = newPageIndex)
        val updatedPages = doc.pages + newPage
        _currentDocument.value = doc.copy(pages = updatedPages)
        _currentPageIndex.value = newPageIndex
        _selectedAnnotationId.value = null
        _selectedStrokeIds.value = emptySet()
        updateUndoRedoStates()
        scheduleAutoSave()
    }

    fun deleteCurrentPage() {
        val doc = _currentDocument.value
        if (doc.pages.size <= 1) {
            clearCurrentPage()
            return
        }
        val currentIndex = _currentPageIndex.value
        val updatedPages = doc.pages.toMutableList()
        updatedPages.removeAt(currentIndex)

        // Re-index remaining pages
        val reindexed = updatedPages.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
        val newIndex = (currentIndex - 1).coerceAtLeast(0)
        _currentDocument.value = doc.copy(pages = reindexed)
        _currentPageIndex.value = newIndex
        _selectedAnnotationId.value = null
        _selectedStrokeIds.value = emptySet()
        resetUndoRedo()
        scheduleAutoSave()
    }

    fun clearCurrentPage() {
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        saveUndoSnapshot()

        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) p.copy(strokes = emptyList(), textAnnotations = emptyList()) else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
        _selectedAnnotationId.value = null
        _selectedStrokeIds.value = emptySet()
        scheduleAutoSave()
    }

    // Stroke Drawing
    private var hasRecordedUndoInEraseGesture = false

    fun cancelLiveStroke() {
        _liveStroke.value = null
    }

    fun onDrawingPointerDown(point: DrawingPoint, overrideTool: ToolType? = null, isHardwareEraser: Boolean = false) {
        val tool = overrideTool ?: (if (isHardwareEraser) ToolType.AREA_ERASER else _currentTool.value)
        if (tool == ToolType.HAND || tool == ToolType.LASSO || tool == ToolType.SYMBOL_EDIT) return

        if (tool == ToolType.STROKE_ERASER) {
            hasRecordedUndoInEraseGesture = false
            eraseStrokesNear(point.x, point.y, radius = _eraserSize.value.radius)
            return
        } else if (tool == ToolType.AREA_ERASER) {
            hasRecordedUndoInEraseGesture = false
            eraseAreaStrokes(point.x, point.y, radius = _eraserSize.value.radius)
            return
        }

        val color = when (tool) {
            ToolType.HIGHLIGHTER -> _highlighterColor.value
            else -> _penColor.value
        }
        val alpha = if (tool == ToolType.HIGHLIGHTER) 0.45f else 1.0f
        val width = when (tool) {
            ToolType.HIGHLIGHTER -> _strokeWidth.value * 3.5f
            else -> _strokeWidth.value
        }

        val stroke = DrawingStroke(
            toolType = tool,
            color = color,
            width = width,
            alpha = alpha,
            points = listOf(point),
            isShape = _shapeModeEnabled.value,
            shapeType = _selectedShapeType.value
        )
        _liveStroke.value = stroke
    }

    fun onDrawingPointerMove(point: DrawingPoint, overrideTool: ToolType? = null, isHardwareEraser: Boolean = false) {
        val tool = overrideTool ?: (if (isHardwareEraser) ToolType.AREA_ERASER else _currentTool.value)
        if (tool == ToolType.HAND || tool == ToolType.LASSO || tool == ToolType.SYMBOL_EDIT) return

        if (tool == ToolType.STROKE_ERASER) {
            eraseStrokesNear(point.x, point.y, radius = _eraserSize.value.radius)
            return
        } else if (tool == ToolType.AREA_ERASER) {
            eraseAreaStrokes(point.x, point.y, radius = _eraserSize.value.radius)
            return
        }

        val current = _liveStroke.value ?: return
        val lastPoint = current.points.lastOrNull()
        if (lastPoint != null) {
            val dist = hypot(point.x - lastPoint.x, point.y - lastPoint.y)
            if (dist < 1.0f) return // Filter out redundant stationary jitter
        }
        val updatedPts = current.points + point
        _liveStroke.value = current.copy(points = updatedPts)
    }

    fun onDrawingPointerMoveBatch(points: List<DrawingPoint>, overrideTool: ToolType? = null, isHardwareEraser: Boolean = false) {
        val tool = overrideTool ?: (if (isHardwareEraser) ToolType.AREA_ERASER else _currentTool.value)
        if (tool == ToolType.HAND || tool == ToolType.LASSO || tool == ToolType.SYMBOL_EDIT) return

        if (tool == ToolType.STROKE_ERASER) {
            val minStep = (_eraserSize.value.radius * 0.4f).coerceAtLeast(6f)
            var lastErasedX = Float.MIN_VALUE
            var lastErasedY = Float.MIN_VALUE
            for (p in points) {
                if (hypot(p.x - lastErasedX, p.y - lastErasedY) >= minStep) {
                    eraseStrokesNear(p.x, p.y, radius = _eraserSize.value.radius)
                    lastErasedX = p.x
                    lastErasedY = p.y
                }
            }
            return
        } else if (tool == ToolType.AREA_ERASER) {
            val minStep = (_eraserSize.value.radius * 0.4f).coerceAtLeast(6f)
            var lastErasedX = Float.MIN_VALUE
            var lastErasedY = Float.MIN_VALUE
            for (p in points) {
                if (hypot(p.x - lastErasedX, p.y - lastErasedY) >= minStep) {
                    eraseAreaStrokes(p.x, p.y, radius = _eraserSize.value.radius)
                    lastErasedX = p.x
                    lastErasedY = p.y
                }
            }
            return
        }

        val current = _liveStroke.value ?: return
        if (points.isEmpty()) return
        val filtered = mutableListOf<DrawingPoint>()
        var lastP = current.points.lastOrNull()
        for (p in points) {
            if (lastP == null || hypot(p.x - lastP.x, p.y - lastP.y) >= 1.0f) {
                filtered.add(p)
                lastP = p
            }
        }
        if (filtered.isNotEmpty()) {
            val combined = ArrayList<DrawingPoint>(current.points.size + filtered.size)
            combined.addAll(current.points)
            combined.addAll(filtered)
            _liveStroke.value = current.copy(points = combined)
        }
    }

    fun onDrawingPointerUp() {
        hasRecordedUndoInEraseGesture = false
        val live = _liveStroke.value ?: return
        _liveStroke.value = null

        if (live.points.size < 2) return

        if (live.toolType == ToolType.STROKE_ERASER || live.toolType == ToolType.AREA_ERASER) {
            return
        }

        // Apply shape snapping if enabled
        val finalStroke = if (_shapeModeEnabled.value) {
            ShapeRecognizer.recognizeAndSnap(live.copy(shapeType = _selectedShapeType.value))
        } else {
            live
        }

        saveUndoSnapshot()
        addStrokeToCurrentPage(finalStroke)
        scheduleAutoSave()
    }

    private fun addStrokeToCurrentPage(stroke: DrawingStroke) {
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                p.copy(strokes = p.strokes + stroke)
            } else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
    }

    // Stroke & Area Eraser
    fun eraseStrokesNear(x: Float, y: Float, radius: Float = 28f): Boolean {
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val page = doc.pages.getOrNull(pageIdx) ?: return false

        var anyRemoved = false
        val remaining = mutableListOf<DrawingStroke>()

        for (stroke in page.strokes) {
            val pts = stroke.points
            if (pts.isEmpty()) continue

            val maxDist = radius + stroke.width / 2f
            val b = stroke.bounds
            // Fast AABB rejection: eliminates 95%+ of strokes immediately
            if (x < b.left - maxDist || x > b.right + maxDist ||
                y < b.top - maxDist || y > b.bottom + maxDist
            ) {
                remaining.add(stroke)
                continue
            }

            var intersects = false
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
                anyRemoved = true
            } else {
                remaining.add(stroke)
            }
        }

        if (anyRemoved) {
            if (!hasRecordedUndoInEraseGesture) {
                saveUndoSnapshot()
                hasRecordedUndoInEraseGesture = true
            }
            val updatedPages = doc.pages.map { p ->
                if (p.pageIndex == pageIdx) p.copy(strokes = remaining) else p
            }
            _currentDocument.value = doc.copy(pages = updatedPages)
            scheduleAutoSave()
        }
        return anyRemoved
    }

    fun eraseAreaStrokes(x: Float, y: Float, radius: Float = 36f): Boolean {
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val page = doc.pages.getOrNull(pageIdx) ?: return false

        var anyModified = false
        val newStrokes = mutableListOf<DrawingStroke>()
        val effectiveRadius = radius.coerceAtLeast(12f)

        for (stroke in page.strokes) {
            val pts = stroke.points
            if (pts.isEmpty()) continue

            // Quick AABB bounds check
            val b = stroke.bounds
            val strokeRadius = effectiveRadius + stroke.width / 2f
            if (x < b.left - strokeRadius || x > b.right + strokeRadius ||
                y < b.top - strokeRadius || y > b.bottom + strokeRadius
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
                    val numSteps = (dist / step).toInt().coerceAtMost(6)
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

        // Erase intersecting symbols / text annotations
        val remainingAnnotations = mutableListOf<TextAnnotation>()
        var anyAnnotationRemoved = false
        for (annotation in page.textAnnotations) {
            if (annotation.hitTest(x, y, padding = effectiveRadius)) {
                anyAnnotationRemoved = true
                if (_selectedAnnotationId.value == annotation.id) {
                    _selectedAnnotationId.value = null
                }
            } else {
                remainingAnnotations.add(annotation)
            }
        }

        if (anyModified || anyAnnotationRemoved) {
            if (!hasRecordedUndoInEraseGesture) {
                saveUndoSnapshot()
                hasRecordedUndoInEraseGesture = true
            }
            val updatedPages = doc.pages.map { p ->
                if (p.pageIndex == pageIdx) p.copy(
                    strokes = newStrokes,
                    textAnnotations = remainingAnnotations
                ) else p
            }
            _currentDocument.value = doc.copy(pages = updatedPages)
            scheduleAutoSave()
        }
        return anyModified || anyAnnotationRemoved
    }

    // Object & Symbol Selection / Editing
    fun selectAnnotation(id: String?) {
        _selectedAnnotationId.value = id
        if (id != null) {
            _currentTool.value = ToolType.SYMBOL_EDIT
        }
    }

    fun moveAnnotation(id: String, newX: Float, newY: Float) {
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                val updatedTexts = p.textAnnotations.map { item ->
                    if (item.id == id) item.copy(x = newX, y = newY) else item
                }
                p.copy(textAnnotations = updatedTexts)
            } else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
    }

    fun resizeAnnotation(id: String, deltaFontSize: Float, saveUndo: Boolean = true) {
        if (saveUndo) saveUndoSnapshot()
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                val updatedTexts = p.textAnnotations.map { item ->
                    if (item.id == id) {
                        val newSize = (item.fontSize + deltaFontSize).coerceIn(14f, 80f)
                        item.copy(fontSize = newSize)
                    } else item
                }
                p.copy(textAnnotations = updatedTexts)
            } else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
        if (saveUndo) scheduleAutoSave()
    }

    fun rotateAnnotation(id: String, deltaDegrees: Float, saveUndo: Boolean = true) {
        if (saveUndo) saveUndoSnapshot()
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                val updatedTexts = p.textAnnotations.map { item ->
                    if (item.id == id) {
                        val newRot = (item.rotation + deltaDegrees) % 360f
                        item.copy(rotation = newRot)
                    } else item
                }
                p.copy(textAnnotations = updatedTexts)
            } else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
        if (saveUndo) scheduleAutoSave()
    }

    fun deleteSelectedAnnotation() {
        val id = _selectedAnnotationId.value ?: return
        saveUndoSnapshot()
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                p.copy(textAnnotations = p.textAnnotations.filterNot { it.id == id })
            } else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
        _selectedAnnotationId.value = null
        scheduleAutoSave()
    }

    fun commitSelectedAnnotation() {
        _selectedAnnotationId.value = null
        _currentTool.value = ToolType.PEN
    }

    // Text Annotation & Math Symbols
    fun addTextAnnotation(text: String, x: Float = 150f, y: Float = 150f, fontSize: Float = 26f, color: Long = 0xFF0F172A) {
        saveUndoSnapshot()
        val annotation = TextAnnotation(
            text = text,
            x = x,
            y = y,
            fontSize = fontSize,
            color = color
        )
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                p.copy(textAnnotations = p.textAnnotations + annotation)
            } else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
        _selectedAnnotationId.value = annotation.id
        _currentTool.value = ToolType.SYMBOL_EDIT
        scheduleAutoSave()
    }

    // Lasso Selection
    fun selectStrokesInRegion(minX: Float, minY: Float, maxX: Float, maxY: Float) {
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val page = doc.pages.getOrNull(pageIdx) ?: return

        val selectedIds = mutableSetOf<String>()
        for (stroke in page.strokes) {
            val containsAny = stroke.points.any { p ->
                p.x in minX..maxX && p.y in minY..maxY
            }
            if (containsAny) {
                selectedIds.add(stroke.id)
            }
        }
        _selectedStrokeIds.value = selectedIds
    }

    fun moveSelectedStrokes(dx: Float, dy: Float) {
        val ids = _selectedStrokeIds.value
        if (ids.isEmpty()) return

        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                val updatedStrokes = p.strokes.map { stroke ->
                    if (stroke.id in ids) {
                        val shiftedPts = stroke.points.map { DrawingPoint(it.x + dx, it.y + dy, it.pressure) }
                        stroke.copy(points = shiftedPts)
                    } else stroke
                }
                p.copy(strokes = updatedStrokes)
            } else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
        scheduleAutoSave()
    }

    fun deleteSelectedStrokes() {
        val ids = _selectedStrokeIds.value
        if (ids.isEmpty()) return

        saveUndoSnapshot()
        val doc = _currentDocument.value
        val pageIdx = _currentPageIndex.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) {
                p.copy(strokes = p.strokes.filterNot { it.id in ids })
            } else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
        _selectedStrokeIds.value = emptySet()
        scheduleAutoSave()
    }

    fun clearSelection() {
        _selectedStrokeIds.value = emptySet()
        _selectedAnnotationId.value = null
    }

    // Undo / Redo
    fun saveUndoSnapshot() {
        val pageIdx = _currentPageIndex.value
        val page = _currentDocument.value.pages.getOrNull(pageIdx) ?: return
        val snapshot = PageSnapshot(strokes = page.strokes, textAnnotations = page.textAnnotations)
        val stack = undoStacks.getOrPut(pageIdx) { mutableListOf() }
        val top = stack.lastOrNull()
        if (top != null && top.strokes == snapshot.strokes && top.textAnnotations == snapshot.textAnnotations) {
            return
        }
        stack.add(snapshot)
        if (stack.size > 15) stack.removeAt(0)

        redoStacks[pageIdx]?.clear()
        updateUndoRedoStates()
    }

    fun trimMemory() {
        undoStacks.values.forEach { stack ->
            while (stack.size > 5) stack.removeAt(0)
        }
        redoStacks.clear()
        updateUndoRedoStates()
    }

    fun undo() {
        val pageIdx = _currentPageIndex.value
        val uStack = undoStacks[pageIdx] ?: return
        if (uStack.isEmpty()) return

        val page = _currentDocument.value.pages.getOrNull(pageIdx) ?: return
        val currentSnapshot = PageSnapshot(strokes = page.strokes, textAnnotations = page.textAnnotations)

        val previousSnapshot = uStack.removeAt(uStack.size - 1)
        val rStack = redoStacks.getOrPut(pageIdx) { mutableListOf() }
        rStack.add(currentSnapshot)

        val doc = _currentDocument.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) p.copy(
                strokes = previousSnapshot.strokes,
                textAnnotations = previousSnapshot.textAnnotations
            ) else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
        _selectedAnnotationId.value = null
        _selectedStrokeIds.value = emptySet()
        updateUndoRedoStates()
        scheduleAutoSave()
    }

    fun redo() {
        val pageIdx = _currentPageIndex.value
        val rStack = redoStacks[pageIdx] ?: return
        if (rStack.isEmpty()) return

        val page = _currentDocument.value.pages.getOrNull(pageIdx) ?: return
        val currentSnapshot = PageSnapshot(strokes = page.strokes, textAnnotations = page.textAnnotations)

        val nextSnapshot = rStack.removeAt(rStack.size - 1)
        val uStack = undoStacks.getOrPut(pageIdx) { mutableListOf() }
        uStack.add(currentSnapshot)

        val doc = _currentDocument.value
        val updatedPages = doc.pages.map { p ->
            if (p.pageIndex == pageIdx) p.copy(
                strokes = nextSnapshot.strokes,
                textAnnotations = nextSnapshot.textAnnotations
            ) else p
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
        _selectedAnnotationId.value = null
        _selectedStrokeIds.value = emptySet()
        updateUndoRedoStates()
        scheduleAutoSave()
    }

    private fun resetUndoRedo() {
        undoStacks.clear()
        redoStacks.clear()
        updateUndoRedoStates()
    }

    private fun updateUndoRedoStates() {
        val pageIdx = _currentPageIndex.value
        _canUndo.value = (undoStacks[pageIdx]?.isNotEmpty() == true)
        _canRedo.value = (redoStacks[pageIdx]?.isNotEmpty() == true)
    }

    // Auto-save debounce
    fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(350)
            repository.saveAnswer(_currentDocument.value)
        }
    }

    fun clearAllData(context: Context, onComplete: (deletedFiles: Int, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAll()
                _currentDocument.value = AnswerDocument()
                _currentPageIndex.value = 0
                _selectedAnnotationId.value = null
                _selectedStrokeIds.value = emptySet()
                resetUndoRedo()

                val deletedCount = withContext(Dispatchers.IO) {
                    com.example.export.ExportManager.clearAllExportedFiles(context)
                }
                onComplete(deletedCount, null)
            } catch (e: Exception) {
                onComplete(0, e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            _currentDocument.value = AnswerDocument()
            _currentPageIndex.value = 0
            _selectedAnnotationId.value = null
            _selectedStrokeIds.value = emptySet()
            resetUndoRedo()
            onComplete()
        }
    }
}

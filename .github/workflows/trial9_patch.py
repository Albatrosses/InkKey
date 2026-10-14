from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Trial 9 anchor count for {path}: expected 1, found {count}\nANCHOR:\n{old[:500]}")
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
# 1) IME repository: one deduplicated hardware-key path shared by the service
# and input-view root. Prefer the recovered XP-Pen shortcut resolver before the
# generic Android mappings so K2=E can never drift into another express index.
# Also forward UI-only actions (zoom/toggle) to the live keyboard canvas.
# -----------------------------------------------------------------------------
repo = "app/src/main/java/com/example/ime/data/ImeDocumentRepository.kt"
insert_import(repo, "import kotlinx.coroutines.flow.MutableStateFlow\n", "import kotlinx.coroutines.flow.MutableSharedFlow\n")
insert_import(repo, "import kotlinx.coroutines.flow.asStateFlow\n", "import kotlinx.coroutines.flow.asSharedFlow\n")

replace_once(
    repo,
    """    private val _tabletFeedbackMessage = MutableStateFlow<String?>(null)
    val tabletFeedbackMessage = _tabletFeedbackMessage.asStateFlow()
    private val barrelInput = com.example.util.BarrelInputRouter({ _tabletConfig.value }, ::executeTabletAction)
""",
    """    private val _tabletFeedbackMessage = MutableStateFlow<String?>(null)
    val tabletFeedbackMessage = _tabletFeedbackMessage.asStateFlow()
    private val _uiTabletActions = MutableSharedFlow<com.example.model.TabletAction>(extraBufferCapacity = 16)
    val uiTabletActions = _uiTabletActions.asSharedFlow()
    private val barrelInput = com.example.util.BarrelInputRouter({ _tabletConfig.value }, ::executeTabletAction)
""",
)

replace_once(
    repo,
    """    fun triggerUpperBarrelFunction(down: Boolean = true) = barrelInput.tools.setPressed(com.example.util.BarrelToolController.Input.KEY_UPPER, down)
    fun triggerLowerBarrelFunction(down: Boolean = true) = barrelInput.tools.setPressed(com.example.util.BarrelToolController.Input.KEY_LOWER, down)

""",
    """    fun triggerUpperBarrelFunction(down: Boolean = true) = barrelInput.tools.setPressed(com.example.util.BarrelToolController.Input.KEY_UPPER, down)
    fun triggerLowerBarrelFunction(down: Boolean = true) = barrelInput.tools.setPressed(com.example.util.BarrelToolController.Input.KEY_LOWER, down)

    private var lastHardwareKeySignature: String? = null
    private var lastHardwareKeyHandled: Boolean = false

    fun handleHardwareKeyEvent(event: android.view.KeyEvent): Boolean {
        val signature = "${event.deviceId}:${event.eventTime}:${event.action}:${event.keyCode}:${event.scanCode}:${event.metaState}"
        if (signature == lastHardwareKeySignature) return lastHardwareKeyHandled

        val config = _tabletConfig.value
        val knownXpIndex = com.example.util.XpPenExpressKeyResolver.resolve(event)
        val fallbackIndex = config.findMatchingKeyIndex(event.keyCode, event.scanCode, event)
        val keyIndex = knownXpIndex ?: fallbackIndex
        val target = com.example.util.StylusHardwareDetector.classifyKeyEvent(event.keyCode, event.scanCode, event)
        val isXpPen = com.example.util.XpPenExpressKeyResolver.isXpPen(event)
        val custom = config.customKeyCodes.values.any { it != 0 && it == event.keyCode } ||
            config.customScanCodes.values.any { it != 0 && it == event.scanCode }
        val express = keyIndex != null && (isXpPen || custom)

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
            else -> false
        }

        lastHardwareKeySignature = signature
        lastHardwareKeyHandled = handled
        return handled
    }

""",
)

replace_once(
    repo,
    """            com.example.model.TabletAction.PREV_PAGE -> goToPage((_currentPageIndex.value - 1).coerceAtLeast(0))
            com.example.model.TabletAction.NEXT_PAGE -> if (_currentPageIndex.value + 1 < _document.value.pages.size) goToPage(_currentPageIndex.value + 1) else addPage()
            else -> Unit
""",
    """            com.example.model.TabletAction.PREV_PAGE -> goToPage((_currentPageIndex.value - 1).coerceAtLeast(0))
            com.example.model.TabletAction.NEXT_PAGE -> if (_currentPageIndex.value + 1 < _document.value.pages.size) goToPage(_currentPageIndex.value + 1) else addPage()
            com.example.model.TabletAction.ZOOM_IN,
            com.example.model.TabletAction.ZOOM_OUT,
            com.example.model.TabletAction.RESET_ZOOM,
            com.example.model.TabletAction.TOGGLE_TOOLBAR,
            com.example.model.TabletAction.TOGGLE_FULLSCREEN -> _uiTabletActions.tryEmit(action)
            else -> Unit
""",
)

# -----------------------------------------------------------------------------
# 2) Main activity: use the same recovered resolver so workspace and IME cannot
# disagree about K1..K8. This keeps the clean K2=E behavior intact.
# -----------------------------------------------------------------------------
vm = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
replace_once(
    vm,
    """        val device = event?.device
        val keyIndex = config.findMatchingKeyIndex(keyCode, scanCode, event)
        val target = com.example.util.StylusHardwareDetector.classifyKeyEvent(keyCode, scanCode, event)
        val isXpPen = device?.name?.contains("ugtablet deco mini7 v2", ignoreCase = true) == true
""",
    """        val device = event?.device
        val keyIndex = event?.let { com.example.util.XpPenExpressKeyResolver.resolve(it) }
            ?: config.findMatchingKeyIndex(keyCode, scanCode, event)
        val target = com.example.util.StylusHardwareDetector.classifyKeyEvent(keyCode, scanCode, event)
        val isXpPen = com.example.util.XpPenExpressKeyResolver.isXpPen(event)
""",
)

# -----------------------------------------------------------------------------
# 3) IME service/input-view root: route external tablet keys through the same
# repository helper from every path Android may use. The repository deduplicates
# the same KeyEvent if both root dispatch and service callback see it.
# -----------------------------------------------------------------------------
service = "app/src/main/java/com/example/ime/InkKeyInputMethodService.kt"
p = Path(service)
text = p.read_text(encoding="utf-8")

root_anchor = '''            override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
                com.example.util.UpperTrialTrace.motion(event, "IME_VIEW_GENERIC")
                val handled = inputRepo.handleGenericMotionEvent(event)
                return if (handled) true else super.dispatchGenericMotionEvent(event)
            }
'''
root_replacement = root_anchor + '''
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                return if (inputRepo.handleHardwareKeyEvent(event)) true else super.dispatchKeyEvent(event)
            }

            override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
                return if (inputRepo.handleHardwareKeyEvent(event)) true else super.dispatchKeyEventPreIme(event)
            }
'''
if text.count(root_anchor) != 1:
    raise SystemExit(f"Trial 9 IME root key anchor count {text.count(root_anchor)}")
text = text.replace(root_anchor, root_replacement, 1)

start = text.find("    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {\n")
end = text.find("    override fun onGenericMotionEvent(event: android.view.MotionEvent): Boolean {\n", start)
if start < 0 or end < 0:
    raise SystemExit("Trial 9 could not locate IME service key callback block")
new_keys = '''    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event)
        val repo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
        if (repo.handleHardwareKeyEvent(event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyUp(keyCode, event)
        val repo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
        if (repo.handleHardwareKeyEvent(event)) return true
        return super.onKeyUp(keyCode, event)
    }

'''
text = text[:start] + new_keys + text[end:]
p.write_text(text, encoding="utf-8")
print("patched IME express-key delivery")

# -----------------------------------------------------------------------------
# 4) Keyboard UI: transient panels/banner auto-retract after 2 seconds and as
# soon as writing begins. Shape button becomes a real auto-detect toggle and
# finished strokes are passed through ShapeRecognizer before storage.
# -----------------------------------------------------------------------------
keyboard = "app/src/main/java/com/example/ime/ui/KeyboardCanvasView.kt"
insert_import(keyboard, "import androidx.compose.runtime.Composable\n", "import androidx.compose.runtime.LaunchedEffect\n")
insert_import(keyboard, "import com.example.model.PaperStyle\n", "import com.example.model.ShapeType\nimport com.example.model.TabletAction\n")
insert_import(keyboard, "import com.example.model.ToolbarToolItem\n", "import com.example.util.ShapeRecognizer\n")
insert_import(keyboard, "import java.io.FileOutputStream\n", "import kotlinx.coroutines.delay\n")

replace_once(
    keyboard,
    """    var notificationMessage by remember { mutableStateOf<String?>(null) }

    // Selected Symbol drag tracking
""",
    """    var notificationMessage by remember { mutableStateOf<String?>(null) }
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
""",
)

old_shape = '''                        ToolbarToolItem.SHAPE -> {
                            FilledTonalIconButton(
                                onClick = {
                                    notificationMessage = "Shape snapping active while drawing diagrams!"
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Category, contentDescription = "Shapes", modifier = Modifier.size(16.dp))
                            }
                        }
'''
new_shape = '''                        ToolbarToolItem.SHAPE -> {
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
'''
if Path(keyboard).read_text(encoding="utf-8").count(old_shape) != 1:
    raise SystemExit("Trial 9 shape-button anchor mismatch")
replace_once(keyboard, old_shape, new_shape)

replace_once(
    keyboard,
    """                                MotionEvent.ACTION_DOWN -> {
                                    // 1. Symbol selection priority: tapping any placed symbol selects it and enables moving
""",
    """                                MotionEvent.ACTION_DOWN -> {
                                    // Writing immediately retracts transient drawers/banners so the canvas gets its space back.
                                    if (activeTool != ToolType.HAND) retractTransientPanels()

                                    // 1. Symbol selection priority: tapping any placed symbol selects it and enables moving
""",
)

replace_once(
    keyboard,
    """                                        liveStroke?.let { finished ->
                                            if (finished.points.size >= 2) {
                                                repository.addStroke(finished)
                                            }
                                        }
""",
    """                                        liveStroke?.let { finished ->
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
""",
)

# -----------------------------------------------------------------------------
# 5) Trial 8 edge handle: substantially more transparent water/glass treatment.
# Keep enough border/specular highlight to remain discoverable over white paper.
# -----------------------------------------------------------------------------
toolbar = "app/src/main/java/com/example/ui/components/DrawingToolbar.kt"
p = Path(toolbar)
text = p.read_text(encoding="utf-8")
for old, new in [
    (".shadow(10.dp, glassShape)", ".shadow(5.dp, glassShape)"),
    ("Color.White.copy(alpha = 0.20f)", "Color.White.copy(alpha = 0.08f)"),
    ("MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.60f)", "MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.26f)"),
    ("MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)", "MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)"),
    ("Color.White.copy(alpha = 0.26f)", "Color.White.copy(alpha = 0.36f)"),
    ("MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f)", "MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)"),
    ("MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)", "MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)"),
]:
    if old not in text:
        raise SystemExit(f"Trial 9 glass alpha anchor missing: {old}")
    text = text.replace(old, new, 1)
p.write_text(text, encoding="utf-8")
print("patched glass transparency")

# Sanity checks: fail before Gradle if any requested Trial 9 behavior did not land.
checks = {
    repo: ["handleHardwareKeyEvent(event: android.view.KeyEvent)", "XpPenExpressKeyResolver.resolve", "uiTabletActions"],
    service: ["dispatchKeyEventPreIme", "repo.handleHardwareKeyEvent(event)"],
    keyboard: ["delay(2000L)", "retractTransientPanels()", "ShapeRecognizer.recognizeAndSnap", "ime_shape_snap_btn"],
    toolbar: ["surfaceContainerHigh.copy(alpha = 0.26f)", "surfaceContainerHigh.copy(alpha = 0.72f)"],
}
for path, needles in checks.items():
    data = Path(path).read_text(encoding="utf-8")
    for needle in needles:
        if needle not in data:
            raise SystemExit(f"Trial 9 sanity check failed: {needle} missing from {path}")

print("Trial 9 patch complete")

from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Trial 10 anchor count for {path}: expected 1, found {count}\nANCHOR:\n{old[:700]}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"patched {path}")


# -----------------------------------------------------------------------------
# 1) IME express keys: XP-Pen uses ONLY the six physically recovered chords.
# Generic fallbacks are forbidden for XP-Pen because they were the source of
# wrong K-number matches. Explicit user-learned bindings still take priority.
# Composite modifier prefix packets are swallowed so Ctrl+S/Z never leak into
# the host app before the express action resolves.
# -----------------------------------------------------------------------------
repo = "app/src/main/java/com/example/ime/data/ImeDocumentRepository.kt"
old_repo = '''        val config = _tabletConfig.value
        val knownXpIndex = com.example.util.XpPenExpressKeyResolver.resolve(event)
        val fallbackIndex = config.findMatchingKeyIndex(event.keyCode, event.scanCode, event)
        val keyIndex = knownXpIndex ?: fallbackIndex
        val target = com.example.util.StylusHardwareDetector.classifyKeyEvent(event.keyCode, event.scanCode, event)
        val isXpPen = com.example.util.XpPenExpressKeyResolver.isXpPen(event)
        val custom = config.customKeyCodes.values.any { it != 0 && it == event.keyCode } ||
            config.customScanCodes.values.any { it != 0 && it == event.scanCode }
        val express = keyIndex != null && (isXpPen || custom)
'''
new_repo = '''        val config = _tabletConfig.value
        val isXpPen = com.example.util.XpPenExpressKeyResolver.isXpPen(event)
        val knownXpIndex = com.example.util.XpPenExpressKeyResolver.resolve(event)
        val customIndex = (1..8).firstOrNull { index ->
            val customKey = config.customKeyCodes[index]
            val customScan = config.customScanCodes[index]
            (customKey != null && customKey != 0 && customKey == event.keyCode) ||
                (customScan != null && customScan != 0 && customScan == event.scanCode)
        }
        val fallbackIndex = if (isXpPen) null else config.findMatchingKeyIndex(event.keyCode, event.scanCode, event)
        val keyIndex = customIndex ?: knownXpIndex ?: fallbackIndex
        val target = com.example.util.StylusHardwareDetector.classifyKeyEvent(event.keyCode, event.scanCode, event)
        val express = keyIndex != null && (isXpPen || customIndex != null)
'''
replace_once(repo, old_repo, new_repo)

replace_once(
    repo,
    '''                    executeTabletAction(action)
                    _tabletFeedbackMessage.value = "K$keyIndex: ${action.displayName}"
''',
    '''                    executeTabletAction(action)
                    val chord = if (isXpPen && customIndex == null) {
                        com.example.util.XpPenExpressKeyResolver.chordLabel(keyIndex)
                    } else {
                        "Learned"
                    }
                    _tabletFeedbackMessage.value = "K$keyIndex ($chord): ${action.displayName}"
''',
)

replace_once(
    repo,
    '''            target != com.example.util.StylusHardwareDetector.BarrelTarget.NONE -> {
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
''',
    '''            target != com.example.util.StylusHardwareDetector.BarrelTarget.NONE -> {
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
''',
)

# -----------------------------------------------------------------------------
# 2) Workspace express keys: same strict physical mapping as IME. This prevents
# Activity and keyboard mode from assigning different K numbers to one button.
# -----------------------------------------------------------------------------
vm = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
old_vm = '''        val device = event?.device
        val keyIndex = event?.let { com.example.util.XpPenExpressKeyResolver.resolve(it) }
            ?: config.findMatchingKeyIndex(keyCode, scanCode, event)
        val target = com.example.util.StylusHardwareDetector.classifyKeyEvent(keyCode, scanCode, event)
        val isXpPen = com.example.util.XpPenExpressKeyResolver.isXpPen(event)
        val custom = config.customKeyCodes.values.any { it != 0 && it == keyCode } ||
            config.customScanCodes.values.any { it != 0 && it == scanCode }
'''
new_vm = '''        val device = event?.device
        val isXpPen = com.example.util.XpPenExpressKeyResolver.isXpPen(event)
        val knownXpIndex = event?.let { com.example.util.XpPenExpressKeyResolver.resolve(it) }
        val customIndex = (1..8).firstOrNull { index ->
            val customKey = config.customKeyCodes[index]
            val customScan = config.customScanCodes[index]
            (customKey != null && customKey != 0 && customKey == keyCode) ||
                (customScan != null && customScan != 0 && customScan == scanCode)
        }
        val fallbackIndex = if (isXpPen) null else config.findMatchingKeyIndex(keyCode, scanCode, event)
        val keyIndex = customIndex ?: knownXpIndex ?: fallbackIndex
        val target = com.example.util.StylusHardwareDetector.classifyKeyEvent(keyCode, scanCode, event)
        val custom = customIndex != null
'''
replace_once(vm, old_vm, new_vm)

replace_once(
    vm,
    '''                executeTabletAction(action)
                _tabletFeedbackMessage.value = "K$keyIndex: ${action.displayName}"
''',
    '''                executeTabletAction(action)
                val chord = if (isXpPen && customIndex == null) {
                    com.example.util.XpPenExpressKeyResolver.chordLabel(keyIndex)
                } else {
                    "Learned"
                }
                _tabletFeedbackMessage.value = "K$keyIndex ($chord): ${action.displayName}"
''',
)

replace_once(
    vm,
    '''        if (target != com.example.util.StylusHardwareDetector.BarrelTarget.NONE) {
            val down = event == null || event.action == KeyEvent.ACTION_DOWN
            if (!down || event == null || event.repeatCount == 0) {
                if (target == com.example.util.StylusHardwareDetector.BarrelTarget.UPPER_BARREL) triggerUpperBarrelFunction(down)
                else triggerLowerBarrelFunction(down)
            }
            return true
        }
        return false
''',
    '''        if (target != com.example.util.StylusHardwareDetector.BarrelTarget.NONE) {
            val down = event == null || event.action == KeyEvent.ACTION_DOWN
            if (!down || event == null || event.repeatCount == 0) {
                if (target == com.example.util.StylusHardwareDetector.BarrelTarget.UPPER_BARREL) triggerUpperBarrelFunction(down)
                else triggerLowerBarrelFunction(down)
            }
            return true
        }
        if (event != null && isXpPen && com.example.util.XpPenExpressKeyResolver.isCompositePrefix(event)) {
            return true
        }
        if (event != null && isXpPen) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val rawName = KeyEvent.keyCodeToString(event.keyCode).removePrefix("KEYCODE_")
                _tabletFeedbackMessage.value = "XP-Pen unmapped: $rawName · scan ${event.scanCode}"
            }
            return true
        }
        return false
''',
)

# -----------------------------------------------------------------------------
# 3) Canvas -> lens coordinates. The handle center is expressed in the same
# screen-space coordinate system used by HandwritingCanvas, so GlassLensHandle
# can redraw exactly what lies under it at 1.42x.
# -----------------------------------------------------------------------------
canvas = "app/src/main/java/com/example/ui/canvas/CanvasScreen.kt"
replace_once(
    canvas,
    '''            val maxToolbarOffsetPx = with(density) { maxHeight.toPx() * 0.40f }

            // Main Canvas Area
''',
    '''            val maxToolbarOffsetPx = with(density) { maxHeight.toPx() * 0.40f }
            val lensViewportWidthPx = with(density) { maxWidth.toPx() }
            val lensHalfWidthPx = with(density) { 17.dp.toPx() }
            val lensEdgeInsetPx = with(density) { 4.dp.toPx() }
            val lensSourceXPx = if (toolbarDockRight) {
                lensViewportWidthPx - lensEdgeInsetPx - lensHalfWidthPx
            } else {
                lensEdgeInsetPx + lensHalfWidthPx
            }
            val lensSourceYPx = with(density) { maxHeight.toPx() / 2f } + toolbarOffsetYPx

            // Main Canvas Area
''',
)

replace_once(
    canvas,
    '''                onVerticalDrag = { deltaY ->
                    toolbarOffsetYPx = (toolbarOffsetYPx + deltaY)
                        .coerceIn(-maxToolbarOffsetPx, maxToolbarOffsetPx)
                },
                onOpenMathPalette = { showMathPalette = true },
''',
    '''                onVerticalDrag = { deltaY ->
                    toolbarOffsetYPx = (toolbarOffsetYPx + deltaY)
                        .coerceIn(-maxToolbarOffsetPx, maxToolbarOffsetPx)
                },
                lensSourceXPx = lensSourceXPx,
                lensSourceYPx = lensSourceYPx,
                onOpenMathPalette = { showMathPalette = true },
''',
)

# -----------------------------------------------------------------------------
# 4) Collapsed toolbar becomes the pure GlassLensHandle. No theme-derived fill
# survives, so AMOLED and ruled paper use exactly the same optical treatment.
# -----------------------------------------------------------------------------
toolbar = "app/src/main/java/com/example/ui/components/DrawingToolbar.kt"
replace_once(
    toolbar,
    '''    onDockSideChange: (Boolean) -> Unit = {},
    onVerticalDrag: (Float) -> Unit = {},
    onOpenMathPalette: () -> Unit,
''',
    '''    onDockSideChange: (Boolean) -> Unit = {},
    onVerticalDrag: (Float) -> Unit = {},
    lensSourceXPx: Float = 0f,
    lensSourceYPx: Float = 0f,
    onOpenMathPalette: () -> Unit,
''',
)

replace_once(
    toolbar,
    '''    val currentDoc by viewModel.currentDocument.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()

''',
    '''    val currentDoc by viewModel.currentDocument.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val lensPanOffset by viewModel.panOffset.collectAsState()
    val lensLiveStroke by viewModel.liveStroke.collectAsState()
    val lensPage = currentDoc.pages.getOrElse(currentPageIndex) {
        currentDoc.pages.firstOrNull() ?: com.example.model.PageData(0)
    }

''',
)

p = Path(toolbar)
text = p.read_text(encoding="utf-8")
start = text.find("    if (isCollapsed) {\n")
end_marker = "    } else {\n        // Full Vertical Tool Palette along the canvas edge\n"
end = text.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit("Trial 10 could not locate collapsed toolbar block")
new_collapsed = '''    if (isCollapsed) {
        GlassLensHandle(
            pageData = lensPage,
            paperStyle = currentDoc.paperStyle,
            panOffset = lensPanOffset,
            zoomScale = zoomScale,
            liveStroke = lensLiveStroke,
            sourceCenterXPx = lensSourceXPx,
            sourceCenterYPx = lensSourceYPx,
            dockOnRight = dockOnRight,
            onOpen = onToggleCollapse,
            onDockSideChange = onDockSideChange,
            onVerticalDrag = onVerticalDrag,
            modifier = modifier
        )
'''
text = text[:start] + new_collapsed + text[end:]
p.write_text(text, encoding="utf-8")
print("patched DrawingToolbar to pure magnifying lens handle")

# Sanity checks
checks = {
    repo: ["fallbackIndex = if (isXpPen) null", "K$keyIndex ($chord)", "XP-Pen unmapped"],
    vm: ["fallbackIndex = if (isXpPen) null", "XpPenExpressKeyResolver.isCompositePrefix"],
    canvas: ["lensSourceXPx", "lensSourceYPx"],
    toolbar: ["GlassLensHandle(", "lensPanOffset", "lensLiveStroke"],
}
for path, needles in checks.items():
    data = Path(path).read_text(encoding="utf-8")
    for needle in needles:
        if needle not in data:
            raise SystemExit(f"Trial 10 sanity check failed: {needle} missing from {path}")

print("Trial 10 patch complete")

from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Trial 7 patch anchor count for {path}: expected 1, found {count}\nANCHOR:\n{old[:300]}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"patched {path}")


# -----------------------------------------------------------------------------
# IME repository: restore tablet feedback + live config mutation used by the
# clean keyboard's tablet quick controls. The barrel router remains transient.
# -----------------------------------------------------------------------------
repo = "app/src/main/java/com/example/ime/data/ImeDocumentRepository.kt"
replace_once(
    repo,
    """    private val _tabletConfig = MutableStateFlow(com.example.model.TabletConfig.load(context))
    val tabletConfig = _tabletConfig.asStateFlow()
    private val barrelInput = com.example.util.BarrelInputRouter({ _tabletConfig.value }, ::executeTabletAction)
""",
    """    private val _tabletConfig = MutableStateFlow(com.example.model.TabletConfig.load(context))
    val tabletConfig = _tabletConfig.asStateFlow()
    private val _tabletFeedbackMessage = MutableStateFlow<String?>(null)
    val tabletFeedbackMessage = _tabletFeedbackMessage.asStateFlow()
    private val barrelInput = com.example.util.BarrelInputRouter({ _tabletConfig.value }, ::executeTabletAction)
""",
)

replace_once(
    repo,
    """    fun reloadTabletConfig() { _tabletConfig.value = com.example.model.TabletConfig.load(context); barrelInput.tools.refresh() }
    fun clearTransientBarrelTool() = barrelInput.reset()
""",
    """    fun reloadTabletConfig() { _tabletConfig.value = com.example.model.TabletConfig.load(context); barrelInput.tools.refresh() }
    fun updateTabletConfig(config: com.example.model.TabletConfig) {
        _tabletConfig.value = config
        com.example.model.TabletConfig.save(context, config)
        barrelInput.tools.refresh()
    }
    fun clearTabletFeedback() { _tabletFeedbackMessage.value = null }
    fun clearTransientBarrelTool() = barrelInput.reset()
""",
)

replace_once(
    repo,
    """    fun executeTabletAction(action: com.example.model.TabletAction) {
        val tool = com.example.util.BarrelToolController.toolFor(action)
""",
    """    fun executeTabletAction(action: com.example.model.TabletAction) {
        if (action != com.example.model.TabletAction.NONE) {
            _tabletFeedbackMessage.value = action.displayName
        }
        val tool = com.example.util.BarrelToolController.toolFor(action)
""",
)

# -----------------------------------------------------------------------------
# Keyboard UI: restore the clean build's keyboard-local tablet controls and
# consume barrel state that was already routed at the root ComposeView.
# -----------------------------------------------------------------------------
keyboard = "app/src/main/java/com/example/ime/ui/KeyboardCanvasView.kt"
replace_once(
    keyboard,
    """    val canUndo by repository.canUndo.collectAsState()
    val canRedo by repository.canRedo.collectAsState()

    val currentPage = document.pages.getOrElse(currentPageIndex) {
""",
    """    val canUndo by repository.canUndo.collectAsState()
    val canRedo by repository.canRedo.collectAsState()
    val tabletConfig by repository.tabletConfig.collectAsState()
    val tabletFeedback by repository.tabletFeedbackMessage.collectAsState()

    val currentPage = document.pages.getOrElse(currentPageIndex) {
""",
)

replace_once(
    keyboard,
    """    var showEraserSizePicker by remember { mutableStateOf(false) }
    var showSymbolsDrawer by remember { mutableStateOf(false) }
    var selectedSymbolCategory by remember { mutableStateOf(\"Physics\") }
""",
    """    var showEraserSizePicker by remember { mutableStateOf(false) }
    var showSymbolsDrawer by remember { mutableStateOf(false) }
    var showTabletQuickBar by remember { mutableStateOf(false) }
    var selectedSymbolCategory by remember { mutableStateOf(\"Physics\") }
""",
)

replace_once(
    keyboard,
    """                // Direct Settings Button (Starts ImeSettingsActivity cleanly)
                IconButton(
""",
    """                // Tablet quick controls restored from the clean APK behavior.
                IconButton(
                    onClick = { showTabletQuickBar = !showTabletQuickBar },
                    modifier = Modifier.size(28.dp).testTag(\"ime_tablet_quick_toggle\")
                ) {
                    Icon(
                        Icons.Default.Brush,
                        contentDescription = \"Tablet Quick Controls\",
                        tint = if (showTabletQuickBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Direct Settings Button (Starts ImeSettingsActivity cleanly)
                IconButton(
""",
)

quick_bar = r'''            AnimatedVisibility(visible = showTabletQuickBar) {
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

'''
replace_once(
    keyboard,
    """            // =========================================================================
            // ROW 2: CUSTOMIZABLE DRAWING TOOLS ROW
            // =========================================================================
""",
    quick_bar + """            // =========================================================================
            // ROW 2: CUSTOMIZABLE DRAWING TOOLS ROW
            // =========================================================================
""",
)

# Root IME dispatch now owns input routing, so do not feed the same touch packet
# again from the canvas child. Event-key dedupe remains a safety net.
replace_once(
    keyboard,
    """                            repository.handleTouchMotionEvent(motionEvent)
                            val activeTool = repository.transientBarrelTool.value ?: currentTool
""",
    """                            // Routed once at the IME root before Compose dispatch.
                            val activeTool = repository.transientBarrelTool.value ?: currentTool
""",
)

# -----------------------------------------------------------------------------
# InputMethodService: route touch and generic motion at the root view so upper
# pre-hold/hover events are seen even when the pointer is over keyboard chrome,
# not only over the handwriting Canvas.
# -----------------------------------------------------------------------------
service = "app/src/main/java/com/example/ime/InkKeyInputMethodService.kt"
replace_once(
    service,
    """        val themedContext = ContextThemeWrapper(this, R.style.Theme_MyApplication)
        val composeView = ComposeView(themedContext)
""",
    """        val themedContext = ContextThemeWrapper(this, R.style.Theme_MyApplication)
        val inputRepo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
        val composeView = object : ComposeView(themedContext) {
            override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
                com.example.util.UpperTrialTrace.motion(event, \"IME_VIEW_TOUCH\")
                inputRepo.handleTouchMotionEvent(event)
                return super.dispatchTouchEvent(event)
            }

            override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
                com.example.util.UpperTrialTrace.motion(event, \"IME_VIEW_GENERIC\")
                val handled = inputRepo.handleGenericMotionEvent(event)
                return if (handled) true else super.dispatchGenericMotionEvent(event)
            }
        }
""",
)

replace_once(
    service,
    'com.example.util.UpperTrialTrace.motion(event, "IME_GENERIC")',
    'com.example.util.UpperTrialTrace.motion(event, "IME_SERVICE_GENERIC")',
)

print("Trial 7 patch complete: IME root routing + tablet quick controls restored")

from pathlib import Path
import re

root = Path('.')
def replace_once(code, old, new):
    assert old in code, f'Missing patch target: {old[:70]}'
    return code.replace(old, new, 1)

gradle = root / 'app/build.gradle.kts'
code = gradle.read_text()
code = replace_once(code, 'applicationId = "com.aistudio.inkkey.vzqwx"', 'applicationId = "com.aistudio.inkkey.uppertrial5"')
gradle.write_text(re.sub(r'versionName = "[^"]+"', 'versionName = "1.8-upper-trial-5"', code, count=1))
manifest = root / 'app/src/main/AndroidManifest.xml'
code = manifest.read_text().replace('android:label="@string/app_name"', 'android:label="InkKey Upper Trial 5"')
code = re.sub(r'\s*<service\s+android:name="\.ime\.InkKeyInputMethodService"[\s\S]*?</service>', '', code, count=1)
manifest.write_text(code)

path = root / 'app/src/main/java/com/example/viewmodel/MainViewModel.kt'
code = path.read_text()
start = code.index('    private var lastUpperBarrelToggleTime')
end = code.index('fun handleGenericMotionEvent(event: MotionEvent): Boolean {', start)
handler = '''    private val upperRestartDetector = com.example.util.ContactRestartDetector()
    private val upperTrialHeld = com.example.util.HeldBarrelState()
    private var upperTrialLiftJob: Job? = null
    val upperTrialToolOverride: ToolType?
        get() = if (!upperTrialHeld.pressed) null else when (_tabletConfig.value.upperBarrelAction) {
            TabletAction.AREA_ERASER -> ToolType.AREA_ERASER
            TabletAction.STROKE_ERASER -> ToolType.STROKE_ERASER
            TabletAction.HAND -> ToolType.HAND
            TabletAction.PEN -> ToolType.PEN
            TabletAction.LASSO -> ToolType.LASSO
            else -> null
        }

    fun handleUpperTrialHover(event: MotionEvent) {
        val dev = event.device ?: return
        val eligible = dev.vendorId == 0x28BD && dev.productId == 0x0948 &&
            event.pointerCount == 1 && event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS &&
            event.buttonState == 0 && event.actionButton == 0
        if (event.pointerCount == 0) return
        val edge = upperRestartDetector.observeHover(com.example.util.ContactRestartDetector.Sample(
            event.actionMasked, event.eventTime, event.x, event.y, event.pressure, event.deviceId), eligible)
        if (edge != com.example.util.ContactRestartDetector.Edge.NONE) {
            val alreadyHeld = upperTrialHeld.pressed
            upperTrialHeld.update(edge)
            com.example.util.UpperTrialTrace.record("HOVER_EDGE=$edge time=${event.eventTime} selected=${_currentTool.value} override=$upperTrialToolOverride")
            if (edge == com.example.util.ContactRestartDetector.Edge.PRESS_CANDIDATE && !alreadyHeld && upperTrialToolOverride == null && _tabletConfig.value.upperBarrelAction != TabletAction.NONE) executeTabletAction(_tabletConfig.value.upperBarrelAction)
        }
        if (eligible && event.actionMasked == MotionEvent.ACTION_HOVER_EXIT) {
            upperTrialLiftJob?.cancel()
            upperTrialLiftJob = viewModelScope.launch { delay(200L); clearUpperTrial() }
        } else if (eligible && event.actionMasked == MotionEvent.ACTION_HOVER_ENTER) {
            upperTrialLiftJob?.cancel()
        }
    }

    fun resetUpperTrialInput() { upperTrialLiftJob?.cancel(); clearUpperTrial() }

    private fun clearUpperTrial() {
        upperTrialHeld.clear()
        upperRestartDetector.reset()
        com.example.util.UpperTrialTrace.record("TRANSIENT_CLEARED")
    }

    fun handleTouchMotionEvent(event: MotionEvent): Boolean {
        // Hover is processed once, at the Activity, with screen coordinates.
        if (event.actionMasked == MotionEvent.ACTION_HOVER_ENTER || event.actionMasked == MotionEvent.ACTION_HOVER_MOVE || event.actionMasked == MotionEvent.ACTION_HOVER_EXIT) return false
        val dev = event.device
        val eligible = dev?.vendorId == 0x28BD && dev.productId == 0x0948 &&
            event.pointerCount == 1 && event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS &&
            event.buttonState == 0 && event.actionButton == 0
        if (!eligible || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            upperTrialLiftJob?.cancel()
            clearUpperTrial()
            if (event.pointerCount > 0 && (event.buttonState != 0 || event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER)) handleGenericMotionEvent(event)
            return false
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN) upperTrialLiftJob?.cancel()
        val sample = com.example.util.ContactRestartDetector.Sample(
            event.actionMasked, event.eventTime, event.x, event.y, event.pressure, event.deviceId)
        com.example.util.UpperTrialTrace.record("time=${event.eventTime} down=${event.downTime} action=${MotionEvent.actionToString(event.actionMasked)} pressure=${event.pressure} x=${event.x} y=${event.y} tool=${event.getToolType(0)} buttons=${event.buttonState}")
        val edge = upperRestartDetector.observe(sample, true)
        val wasHeld = upperTrialHeld.pressed
        upperTrialHeld.update(edge)
        if (edge != com.example.util.ContactRestartDetector.Edge.NONE) {
            com.example.util.UpperTrialTrace.record("INFERRED_EDGE=$edge time=${event.eventTime} held=${upperTrialHeld.pressed} selected=${_currentTool.value} override=$upperTrialToolOverride")
        }
        if (edge == com.example.util.ContactRestartDetector.Edge.PRESS_CANDIDATE && !wasHeld) {
            if (upperTrialToolOverride == null && _tabletConfig.value.upperBarrelAction != TabletAction.NONE) executeTabletAction(_tabletConfig.value.upperBarrelAction)
        }
        // A tip lift does not imply a button release. Keep the inferred mode while
        // hovering in range; hover/contact release, cancellation or leaving range clears it.
        // Canvas reads the override synchronously for this very event.
        return false
    }

    '''
code = code[:start] + handler + code[end:]
path.write_text(code)

path = root / 'app/src/main/java/com/example/ui/canvas/HandwritingCanvas.kt'
canvas = path.read_text()
canvas = replace_once(canvas, 'else -> currentTool\n                }', 'else -> viewModel.upperTrialToolOverride ?: currentTool\n                }')
path.write_text(canvas)

path = root / 'app/src/main/java/com/example/MainActivity.kt'
activity = path.read_text()
activity = replace_once(activity, 'override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {', 'override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {\n        com.example.util.UpperTrialTrace.motion(event, "GENERIC")\n        mainViewModel?.handleUpperTrialHover(event)')
activity = replace_once(activity, '    override fun onTrimMemory(level: Int) {', '    override fun onPause() {\n        mainViewModel?.resetUpperTrialInput()\n        super.onPause()\n    }\n\n    override fun onTrimMemory(level: Int) {')
path.write_text(activity)

path = root / 'app/src/main/java/com/example/ui/settings/SettingsScreen.kt'
code = path.read_text()
code = replace_once(code, '// Section 1: Canvas & Paper Settings', '''OutlinedButton(onClick = { com.example.util.UpperTrialTrace.share(context) }) {
                Text("Share upper-button trial log")
            }
            // Section 1: Canvas & Paper Settings''')
path.write_text(code)

for name in ['ContactRestartDetector.kt', 'UpperTrialTrace.kt']:
    (root / 'app/src/main/java/com/example/util' / name).write_text((root / '.github/workflows' / name).read_text())
test = root / 'app/src/test/java/com/example/ContactRestartDetectorTest.kt'
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text((root / '.github/workflows/ContactRestartDetectorTest.kt').read_text())
print('Upper Trial prepared: isolated app; guarded contact-restart inference; original zero-pressure guess disabled')

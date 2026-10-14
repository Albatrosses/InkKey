from pathlib import Path
import re

root = Path('.')
def replace_once(code, old, new):
    assert old in code, f'Missing patch target: {old[:70]}'
    return code.replace(old, new, 1)

gradle = root / 'app/build.gradle.kts'
code = gradle.read_text()
code = replace_once(code, 'applicationId = "com.aistudio.inkkey.vzqwx"', 'applicationId = "com.aistudio.inkkey.uppertrial"')
gradle.write_text(re.sub(r'versionName = "[^"]+"', 'versionName = "1.8-upper-trial-1"', code, count=1))
manifest = root / 'app/src/main/AndroidManifest.xml'
code = manifest.read_text().replace('android:label="@string/app_name"', 'android:label="InkKey Upper Trial"')
code = re.sub(r'\s*<service\s+android:name="\.ime\.InkKeyInputMethodService"[\s\S]*?</service>', '', code, count=1)
manifest.write_text(code)

path = root / 'app/src/main/java/com/example/viewmodel/MainViewModel.kt'
code = path.read_text()
start = code.index('    private var lastUpperBarrelToggleTime')
end = code.index('fun handleGenericMotionEvent(event: MotionEvent): Boolean {', start)
handler = '''    private val upperRestartDetector = com.example.util.ContactRestartDetector()

    fun handleTouchMotionEvent(event: MotionEvent): Boolean {
        val dev = event.device
        val eligible = dev?.vendorId == 0x28BD && dev.productId == 0x0948 &&
            event.pointerCount == 1 && event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS &&
            event.buttonState == 0 && event.actionButton == 0
        if (event.pointerCount == 0) { upperRestartDetector.reset(); return false }
        val sample = com.example.util.ContactRestartDetector.Sample(
            event.actionMasked, event.eventTime, event.x, event.y, event.pressure, event.deviceId)
        if (eligible) {
            com.example.util.UpperTrialTrace.record("time=${event.eventTime} down=${event.downTime} action=${MotionEvent.actionToString(event.actionMasked)} pressure=${event.pressure} x=${event.x} y=${event.y} tool=${event.getToolType(0)} buttons=${event.buttonState} history=${(0 until event.historySize).joinToString { event.getHistoricalPressure(0,it).toString() }}")
        }
        val edge = upperRestartDetector.observe(sample, eligible)
        if (edge != com.example.util.ContactRestartDetector.Edge.NONE) {
            com.example.util.UpperTrialTrace.record("INFERRED_EDGE=$edge time=${event.eventTime}")
        }
        if (edge == com.example.util.ContactRestartDetector.Edge.PRESS_CANDIDATE) {
            val next = if (_currentTool.value == ToolType.STROKE_ERASER || _currentTool.value == ToolType.AREA_ERASER) ToolType.PEN else ToolType.STROKE_ERASER
            setTool(next)
            _tabletFeedbackMessage.value = "Upper trial: Switched to ${if(next == ToolType.PEN) "Pen" else "Stroke Eraser"}"
            com.example.util.UpperTrialTrace.record("TOOL=$next")
        }
        if (event.buttonState != 0 || event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) handleGenericMotionEvent(event)
        // Never swallow contact boundaries: the canvas must finish its existing stroke.
        return false
    }

    '''
code = code[:start] + handler + code[end:]
path.write_text(code)

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

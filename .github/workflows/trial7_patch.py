from pathlib import Path

root = Path('.')


def replace_once(path: Path, old: str, new: str) -> None:
    code = path.read_text()
    if old not in code:
        raise RuntimeError(f"Missing Trial 7 patch target in {path}: {old[:80]!r}")
    path.write_text(code.replace(old, new, 1))


# Keep Trial 7 isolated from the user's known-good clean install and Trial 6.
gradle = root / 'app/build.gradle.kts'
replace_once(
    gradle,
    'applicationId = "com.aistudio.inkkey.recoveredtrial6"',
    'applicationId = "com.aistudio.inkkey.recoveredtrial7"',
)
replace_once(
    gradle,
    'versionName = "1.8-recovered-trial-6"',
    'versionName = "1.8-recovered-trial-7"',
)

# Trial 6 showed three press-window hover restarts between 24 and 32 px
# (28.74, 29.37 and 31.52 px), then a clean jump to 34.94 px. Keep the
# pressure-ratio detector unchanged; only widen the hover movement gate.
detector = root / 'app/src/main/java/com/example/util/ContactRestartDetector.kt'
replace_once(
    detector,
    'if (gap in 42L..50L && distance <= 24f) return Edge.PRESS_CANDIDATE',
    'if (gap in 42L..50L && distance <= 32f) return Edge.PRESS_CANDIDATE',
)

# Keep the existing recovery guard test aligned with the intentionally widened
# threshold. The next observed Trial 6 point, 34.94 px, must still be rejected.
recovered_test = root / 'app/src/test/java/com/example/RecoveredInputTest.kt'
replace_once(
    recovered_test,
    'c.observeHover(ContactRestartDetector.Sample(9, 144L, 25f, 0f, 0f, 12), true))',
    'c.observeHover(ContactRestartDetector.Sample(9, 145L, 34.94f, 0f, 0f, 12), true))',
)

# InputMethodService.onGenericMotionEvent was not reached by the XP-Pen hover
# stream in Trial 6 (the shared log had no IME_GENERIC rows). Capture generic
# stylus motion at the actual ComposeView used by the keyboard instead.
service = root / 'app/src/main/java/com/example/ime/InkKeyInputMethodService.kt'
needle = '        val composeView = ComposeView(themedContext)\n'
replacement = '''        val composeView = ComposeView(themedContext)\n        composeView.setOnGenericMotionListener { _, event ->\n            val dev = event.device\n            val isXpPenStylus = dev != null && dev.vendorId == 0x28BD && dev.productId == 0x0948 &&\n                event.pointerCount > 0 && event.getToolType(0) == android.view.MotionEvent.TOOL_TYPE_STYLUS\n            if (isXpPenStylus) {\n                com.example.util.UpperTrialTrace.motion(event, "IME_VIEW_GENERIC")\n                com.example.ime.data.ImeDocumentRepository.getInstance(this@InkKeyInputMethodService)\n                    .handleGenericMotionEvent(event)\n                true\n            } else {\n                false\n            }\n        }\n'''
replace_once(service, needle, replacement)

# Add a focused regression test from the Trial 6 boundary. 31.52 px should be
# accepted; 34.94 px should remain rejected, so this is not an open-ended gate.
test = root / 'app/src/test/java/com/example/Trial7DetectorRegressionTest.kt'
test.write_text('''package com.example\n\nimport com.example.util.ContactRestartDetector\nimport org.junit.Assert.assertEquals\nimport org.junit.Test\n\nclass Trial7DetectorRegressionTest {\n    private fun sample(action: Int, time: Long, x: Float, y: Float) =\n        ContactRestartDetector.Sample(action, time, x, y, 0f, 12)\n\n    @Test\n    fun trial6MovingPressAt31Point52PixelsIsAccepted() {\n        val detector = ContactRestartDetector()\n        assertEquals(ContactRestartDetector.Edge.NONE, detector.observeHover(sample(10, 1_000L, 0f, 0f), true))\n        assertEquals(\n            ContactRestartDetector.Edge.PRESS_CANDIDATE,\n            detector.observeHover(sample(9, 1_047L, 31.52f, 0f), true)\n        )\n    }\n\n    @Test\n    fun movementBeyondTrial7BoundaryIsStillRejected() {\n        val detector = ContactRestartDetector()\n        assertEquals(ContactRestartDetector.Edge.NONE, detector.observeHover(sample(10, 2_000L, 0f, 0f), true))\n        assertEquals(\n            ContactRestartDetector.Edge.NONE,\n            detector.observeHover(sample(9, 2_045L, 34.94f, 0f), true)\n        )\n    }\n}\n''')

print('Trial 7 patch applied: IME view-level hover capture + 32 px bounded hover press gate')

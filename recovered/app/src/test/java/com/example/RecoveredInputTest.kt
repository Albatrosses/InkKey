package com.example

import android.view.MotionEvent
import com.example.util.StylusHardwareDetector
import com.example.util.ContactRestartDetector
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecoveredInputTest {
    @Test fun eraserToolTypeIsLowerBarrel() {
        val event = event(MotionEvent.TOOL_TYPE_ERASER, 0)
        assertEquals(StylusHardwareDetector.BarrelTarget.LOWER_BARREL, StylusHardwareDetector.classifyMotionEvent(event))
        event.recycle()
    }

    @Test fun secondaryIsUpperAndStylusPrimaryIsLower() {
        val upper = event(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.BUTTON_SECONDARY)
        val lower = event(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.BUTTON_STYLUS_PRIMARY)
        assertEquals(StylusHardwareDetector.BarrelTarget.UPPER_BARREL, StylusHardwareDetector.classifyMotionEvent(upper))
        assertEquals(StylusHardwareDetector.BarrelTarget.LOWER_BARREL, StylusHardwareDetector.classifyMotionEvent(lower))
        upper.recycle(); lower.recycle()
    }

    @Test fun recordedNearBoundaryHoverPairIsRecognized() {
        val c = ContactRestartDetector()
        c.observeHover(ContactRestartDetector.Sample(10, 101728156L, 383.18115f, 1210.8398f, 0f, 12), true)
        assertEquals(ContactRestartDetector.Edge.PRESS_CANDIDATE,
            c.observeHover(ContactRestartDetector.Sample(9, 101728200L, 393.92578f, 1195.459f, 0f, 12), true))
    }

    @Test fun movingOutOfTheHoverGuardDoesNotPress() {
        val c = ContactRestartDetector()
        c.observeHover(ContactRestartDetector.Sample(10, 100L, 0f, 0f, 0f, 12), true)
        assertEquals(ContactRestartDetector.Edge.NONE,
            c.observeHover(ContactRestartDetector.Sample(9, 144L, 25f, 0f, 0f, 12), true))
    }

    private fun event(tool: Int, buttons: Int): MotionEvent {
        val p = MotionEvent.PointerProperties().apply { id = 0; toolType = tool }
        val c = MotionEvent.PointerCoords().apply { x = 100f; y = 100f; pressure = 0.3f }
        return MotionEvent.obtain(0L, 1L, MotionEvent.ACTION_MOVE, 1, arrayOf(p), arrayOf(c), 0, buttons, 1f, 1f, 0, 0, 0x6002, 0)
    }
}

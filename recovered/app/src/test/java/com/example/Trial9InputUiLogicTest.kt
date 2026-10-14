package com.example

import android.view.KeyEvent
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.ShapeType
import com.example.util.ShapeRecognizer
import com.example.util.XpPenExpressKeyResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Trial9InputUiLogicTest {
    @Test
    fun recoveredXpPenShortcutsResolveOnlyVerifiedExpressIndices() {
        assertEquals(1, XpPenExpressKeyResolver.resolveKnownShortcut(KeyEvent.KEYCODE_B, false, false, false))
        assertEquals(2, XpPenExpressKeyResolver.resolveKnownShortcut(KeyEvent.KEYCODE_E, false, false, false))
        assertEquals(3, XpPenExpressKeyResolver.resolveKnownShortcut(KeyEvent.KEYCODE_ALT_LEFT, false, true, false))
        assertEquals(4, XpPenExpressKeyResolver.resolveKnownShortcut(KeyEvent.KEYCODE_SPACE, false, false, false))
        assertEquals(5, XpPenExpressKeyResolver.resolveKnownShortcut(KeyEvent.KEYCODE_S, true, false, false))
        assertEquals(6, XpPenExpressKeyResolver.resolveKnownShortcut(KeyEvent.KEYCODE_Z, true, false, false))
        assertNull(XpPenExpressKeyResolver.resolveKnownShortcut(KeyEvent.KEYCODE_NUMPAD_ADD, true, false, false))
        assertNull(XpPenExpressKeyResolver.resolveKnownShortcut(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, true, false, false))
    }

    @Test
    fun imeAutoShapePathCanSnapAFreehandLine() {
        val points = (0..12).map { i -> DrawingPoint(i * 10f, 50f + i * 0.2f) }
        val snapped = ShapeRecognizer.recognizeAndSnap(DrawingStroke(points = points))
        assertTrue(snapped.isShape)
        assertEquals(ShapeType.LINE, snapped.shapeType)
        assertEquals(2, snapped.points.size)
    }
}

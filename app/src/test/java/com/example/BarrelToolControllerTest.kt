package com.example

import com.example.model.TabletAction
import com.example.model.ToolType
import com.example.util.BarrelToolController
import org.junit.Assert.*
import org.junit.Test

class BarrelToolControllerTest {
    @Test fun heldToolIsSynchronousAndReleaseRestoresFallback() {
        val selected = ToolType.PEN
        val c = BarrelToolController({ TabletAction.STROKE_ERASER }, { fail("Held tools must not execute a permanent action") })
        c.setPressed(BarrelToolController.Input.INFERRED_UPPER, true)
        assertEquals(ToolType.STROKE_ERASER, c.tool.value)
        assertEquals(ToolType.PEN, selected)
        c.setPressed(BarrelToolController.Input.INFERRED_UPPER, false)
        assertNull(c.tool.value)
        assertFalse(c.active.value)
    }

    @Test fun repeatedAndDuplicateInputSourcesExecuteInstantActionOnce() {
        val actions = mutableListOf<TabletAction>()
        val c = BarrelToolController({ TabletAction.UNDO }, { actions.add(it) })
        repeat(4) { c.setPressed(BarrelToolController.Input.UPPER, true) }
        c.setPressed(BarrelToolController.Input.KEY_UPPER, true)
        assertEquals(listOf(TabletAction.UNDO), actions)
        c.setPressed(BarrelToolController.Input.UPPER, false)
        c.setPressed(BarrelToolController.Input.KEY_UPPER, false)
        c.setPressed(BarrelToolController.Input.UPPER, true)
        assertEquals(2, actions.size)
    }

    @Test fun lowerHardwareWinsAndItsReleaseReturnsToInferredUpper() {
        val c = BarrelToolController({ if(it) TabletAction.HAND else TabletAction.AREA_ERASER }, {})
        c.setPressed(BarrelToolController.Input.INFERRED_UPPER, true)
        c.setPressed(BarrelToolController.Input.LOWER, true)
        assertEquals(ToolType.AREA_ERASER, c.tool.value)
        c.setPressed(BarrelToolController.Input.LOWER, false)
        assertEquals(ToolType.HAND, c.tool.value)
        c.clear()
        assertNull(c.tool.value)
    }

    @Test fun aHoverMotionWithoutButtonBitsCannotReleaseAKeyHeldTool() {
        val c = BarrelToolController({ TabletAction.HAND }, {})
        c.setPressed(BarrelToolController.Input.KEY_UPPER, true)
        c.setPressed(BarrelToolController.Input.UPPER, false)
        c.setPressed(BarrelToolController.Input.INFERRED_UPPER, false)
        assertEquals(ToolType.HAND, c.tool.value)
        c.setPressed(BarrelToolController.Input.KEY_UPPER, false)
        assertNull(c.tool.value)
    }
}

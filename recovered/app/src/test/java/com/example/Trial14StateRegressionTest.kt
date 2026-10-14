package com.example

import android.view.MotionEvent
import com.example.model.TabletAction
import com.example.model.TabletConfig
import com.example.model.ToolType
import com.example.util.BarrelInputRouter
import com.example.util.BarrelToolController
import com.example.util.ContactRestartDetector
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Trial14StateRegressionTest {
    @Test fun lowerButtonDoesNotDeleteAnAlreadyHeldUpperButton() {
        val router = BarrelInputRouter({ TabletConfig(lowerBarrelAction = TabletAction.AREA_ERASER, upperBarrelAction = TabletAction.HAND) }, {})
        router.tools.setPressed(BarrelToolController.Input.INFERRED_UPPER, true)
        val down = event(MotionEvent.TOOL_TYPE_STYLUS,32)
        router.touch(down)
        assertEquals(ToolType.AREA_ERASER,router.tools.tool.value)
        val release = event(MotionEvent.TOOL_TYPE_STYLUS,0)
        router.touch(release)
        assertEquals(ToolType.HAND,router.tools.tool.value)
        router.reset(); down.recycle(); release.recycle()
    }
    @Test fun palmContactDoesNotReleaseThePhysicalLowerButton() {
        val router = BarrelInputRouter({ TabletConfig(lowerBarrelAction = TabletAction.AREA_ERASER) }, {})
        val down = event(MotionEvent.TOOL_TYPE_STYLUS,32); router.touch(down)
        val palm = event(MotionEvent.TOOL_TYPE_FINGER,0); router.touch(palm)
        assertEquals(ToolType.AREA_ERASER,router.tools.tool.value)
        router.reset(); down.recycle(); palm.recycle()
    }
    @Test fun keyboardHidePreservesTheSymbolBeingEdited() {
        val repo = com.example.ime.data.ImeDocumentRepository.getInstance(org.robolectric.RuntimeEnvironment.getApplication())
        repo.clearCurrentPage(); repo.insertSymbolOnCanvas("ω",100f,200f)
        val id = repo.selectedSymbolId.value!!
        repo.clearTransientState()
        assertEquals(id,repo.selectedSymbolId.value)
        repo.updateSymbolText(id,"π")
        assertEquals("π",repo.document.value.pages[repo.currentPageIndex.value].textAnnotations.first { it.id == id }.text)
    }
    @Test fun anEditorCanSaveByIdentityAfterSelectionChanges() {
        val repo = com.example.ime.data.ImeDocumentRepository.getInstance(org.robolectric.RuntimeEnvironment.getApplication())
        repo.clearCurrentPage(); repo.insertSymbolOnCanvas("x",100f,200f)
        val id = repo.selectedSymbolId.value!!
        repo.selectSymbol(null)
        repo.updateSymbolText(id,"y")
        assertEquals("y",repo.document.value.pages[repo.currentPageIndex.value].textAnnotations.first { it.id == id }.text)
    }
    @Test fun defaultMatcherRestoresTheEarlierContactMovementLimit() {
        val c = ContactRestartDetector()
        c.observe(ContactRestartDetector.Sample(1,100L,0f,0f,.1f,12),true)
        c.observeHover(ContactRestartDetector.Sample(9,100L,0f,0f,0f,12),true)
        c.observeHover(ContactRestartDetector.Sample(10,112L,100f,0f,0f,12),true)
        assertEquals(ContactRestartDetector.Edge.NONE,c.observe(ContactRestartDetector.Sample(0,112L,110f,0f,.3f,12),true))
    }
    private fun event(tool: Int,buttons: Int): MotionEvent {
        val p = MotionEvent.PointerProperties().apply { id=0;toolType=tool }
        val c = MotionEvent.PointerCoords().apply { x=100f;y=100f;pressure=.3f }
        return MotionEvent.obtain(0L,if(tool==MotionEvent.TOOL_TYPE_FINGER)2L else if(buttons==0)3L else 1L,MotionEvent.ACTION_MOVE,1,arrayOf(p),arrayOf(c),0,buttons,1f,1f,0,0,0x6002,0)
    }
}

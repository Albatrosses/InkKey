package com.example

import com.example.model.TextAnnotation
import com.example.util.ContactRestartDetector
import com.example.util.InputOwnership
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Trial13InputSymbolTest {
    @Test fun editingKeyboardSymbolPreservesItsIdentityAndSupportsUndo() {
        val repo = com.example.ime.data.ImeDocumentRepository.getInstance(org.robolectric.RuntimeEnvironment.getApplication())
        repo.clearCurrentPage()
        repo.insertSymbolOnCanvas("ω",100f,200f)
        val id = repo.selectedSymbolId.value!!
        val before = repo.document.value.pages[repo.currentPageIndex.value].textAnnotations.first { it.id == id }
        repo.updateSelectedSymbolText("2πf")
        val after = repo.document.value.pages[repo.currentPageIndex.value].textAnnotations.first { it.id == id }
        assertEquals(before.copy(text = "2πf"), after)
        repo.undo()
        assertEquals(before, repo.document.value.pages[repo.currentPageIndex.value].textAnnotations.first { it.id == id })
    }
    @Test fun hiddenKeyboardDoesNotExecuteKeysBeforeTheyReachMainApp() {
        var imeActions = 0; var mainActions = 0
        val handled = InputOwnership.routeImeHardwareKey(false) { imeActions++; true }
        if (!handled) mainActions++
        assertEquals(0, imeActions)
        assertEquals(1, mainActions)
    }
    @Test fun shownKeyboardExecutesKeyOnceAndDoesNotForwardIt() {
        var imeActions = 0; var mainActions = 0
        val handled = InputOwnership.routeImeHardwareKey(true) { imeActions++; true }
        if (!handled) mainActions++
        assertEquals(1, imeActions)
        assertEquals(0, mainActions)
    }
    @Test fun unhandledKeyboardKeyStillReachesForeground() {
        var forwarded = 0
        if (!InputOwnership.routeImeHardwareKey(true) { false }) forwarded++
        assertEquals(1, forwarded)
    }
    @Test fun smallSymbolBodyDragsInsteadOfResizing() {
        val symbol = TextAnnotation(text = "ω", x = 100f, y = 200f, fontSize = 24f)
        assertTrue(symbol.hitTest(108f,192f,padding = 0f))
        assertFalse(symbol.hitResizeHandle(108f,192f,1f))
        assertFalse(symbol.hitResizeHandle(108f,192f,0.5f))
    }
    @Test fun rotatedResizeHandlesMatchTheirDisplayedCorners() {
        val symbol = TextAnnotation(text = "F", x = 100f, y = 200f, rotation = 90f)
        val corner = symbol.selectionCorners().first()
        assertTrue(symbol.hitResizeHandle(corner.x,corner.y,1f))
        assertFalse(symbol.hitResizeHandle(corner.x + 80f,corner.y,1f))
    }
    @Test fun fastRecordedRestartUsesItsIntermediateHoverTrajectory() {
        val detector = ContactRestartDetector(allowHoverTrajectory = true)
        detector.observe(ContactRestartDetector.Sample(1,118901191L,322.83325f,544.2627f,0.06116096f,12),true)
        detector.observeHover(ContactRestartDetector.Sample(9,118901191L,319.04297f,555.3955f,0f,12),true)
        detector.observeHover(ContactRestartDetector.Sample(10,118901203L,292.54395f,634.79004f,0f,12),true)
        assertEquals(ContactRestartDetector.Edge.PRESS_CANDIDATE,
            detector.observe(ContactRestartDetector.Sample(0,118901203L,290.896f,647.5342f,0.14673747f,12),true))
    }
    @Test fun uncorroboratedLargeContactJumpIsStillRejected() {
        val detector = ContactRestartDetector()
        detector.observe(ContactRestartDetector.Sample(1,100L,0f,0f,0.1f,12),true)
        assertEquals(ContactRestartDetector.Edge.NONE,
            detector.observe(ContactRestartDetector.Sample(0,112L,110f,0f,0.3f,12),true))
    }
    @Test fun hoverDoesNotBypassNormalTipReleaseGuard() {
        val detector = ContactRestartDetector()
        detector.observe(ContactRestartDetector.Sample(1,100L,0f,0f,0.009949338f,12),true)
        detector.observeHover(ContactRestartDetector.Sample(9,100L,0f,0f,0f,12),true)
        detector.observeHover(ContactRestartDetector.Sample(10,112L,100f,0f,0f,12),true)
        assertEquals(ContactRestartDetector.Edge.NONE,
            detector.observe(ContactRestartDetector.Sample(0,112L,110f,0f,0.3f,12),true))
    }
}

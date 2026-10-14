package com.example

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.StrokesSerializer
import com.example.export.ExportManager
import com.example.ime.InkKeyInputMethodService
import com.example.ime.data.ImeDocumentRepository
import com.example.ime.data.KeyboardHeightMode
import com.example.model.AnswerDocument
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.EraserSize
import com.example.model.NeetSubject
import com.example.model.PageData
import com.example.model.PaperStyle
import com.example.model.ShapeType
import com.example.model.TextAnnotation
import com.example.model.ToolType
import com.example.model.ToolbarToolItem
import com.example.util.ShapeRecognizer
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.ScreenState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImeServiceTest {

    // -------------------------------------------------------------
    // Scenario 1: Insert symbol, select it, move it, resize it, rotate it, and delete it
    // -------------------------------------------------------------
    @Test
    fun testScenario1_SymbolObjectLifecycle() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)

        // 1. Insert symbol
        vm.addTextAnnotation("ΔH", 100f, 150f, fontSize = 24f)
        val page = vm.currentDocument.value.pages[vm.currentPageIndex.value]
        assertEquals(1, page.textAnnotations.size)
        val symbol = page.textAnnotations[0]
        assertEquals("ΔH", symbol.text)
        assertEquals(100f, symbol.x)
        assertEquals(150f, symbol.y)
        assertEquals(symbol.id, vm.selectedAnnotationId.value)

        // 2. Move symbol
        vm.moveAnnotation(symbol.id, 200f, 250f)
        val movedSymbol = vm.currentDocument.value.pages[vm.currentPageIndex.value].textAnnotations[0]
        assertEquals(200f, movedSymbol.x)
        assertEquals(250f, movedSymbol.y)

        // 3. Resize symbol (+6pt)
        vm.resizeAnnotation(symbol.id, 6f)
        val resizedSymbol = vm.currentDocument.value.pages[vm.currentPageIndex.value].textAnnotations[0]
        assertEquals(30f, resizedSymbol.fontSize)

        // 4. Rotate symbol (+45 degrees)
        vm.rotateAnnotation(symbol.id, 45f)
        val rotatedSymbol = vm.currentDocument.value.pages[vm.currentPageIndex.value].textAnnotations[0]
        assertEquals(45f, rotatedSymbol.rotation)

        // 5. Delete symbol independently
        vm.deleteSelectedAnnotation()
        val afterDelete = vm.currentDocument.value.pages[vm.currentPageIndex.value].textAnnotations
        assertEquals(0, afterDelete.size)
        assertNull(vm.selectedAnnotationId.value)

        // 6. Undo delete restores the symbol
        assertTrue(vm.canUndo.value)
        vm.undo()
        val restored = vm.currentDocument.value.pages[vm.currentPageIndex.value].textAnnotations
        assertEquals(1, restored.size)
        assertEquals("ΔH", restored[0].text)
    }

    // -------------------------------------------------------------
    // Scenario 2: Open and close Settings repeatedly without crashing
    // -------------------------------------------------------------
    @Test
    fun testScenario2_SettingsNavigationAndStatePreservation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)

        // Start on canvas with drawings
        vm.navigateTo(ScreenState.CANVAS)
        vm.onDrawingPointerDown(DrawingPoint(50f, 50f))
        vm.onDrawingPointerMove(DrawingPoint(100f, 100f))
        vm.onDrawingPointerUp()
        assertEquals(1, vm.currentDocument.value.pages[0].strokes.size)

        // Repeatedly navigate to SETTINGS and back to CANVAS
        for (i in 1..5) {
            vm.navigateTo(ScreenState.SETTINGS)
            assertEquals(ScreenState.SETTINGS, vm.currentScreen.value)

            // Modify a settings property
            vm.setPaperStyle(if (i % 2 == 0) PaperStyle.GRAPH else PaperStyle.RULED)
            vm.togglePalmRejection()

            // Navigate back
            vm.navigateBack()
            assertEquals(ScreenState.CANVAS, vm.currentScreen.value)

            // Verify strokes are NOT lost
            assertEquals(1, vm.currentDocument.value.pages[0].strokes.size)
        }
    }

    // -------------------------------------------------------------
    // Scenario 3: Draw several strokes, erase only one, and undo
    // -------------------------------------------------------------
    @Test
    fun testScenario3_PrecisionEraserAndUndo() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)
        vm.clearCurrentPage()

        // Stroke 1: horizontal line at y = 100
        val stroke1 = DrawingStroke(
            id = "stroke_1",
            points = (0..10).map { i -> DrawingPoint(10f + i * 10f, 100f) },
            width = 4f
        )
        // Stroke 2: horizontal line at y = 300
        val stroke2 = DrawingStroke(
            id = "stroke_2",
            points = (0..10).map { i -> DrawingPoint(10f + i * 10f, 300f) },
            width = 4f
        )
        // Stroke 3: vertical line at x = 500
        val stroke3 = DrawingStroke(
            id = "stroke_3",
            points = (0..10).map { i -> DrawingPoint(500f, 10f + i * 10f) },
            width = 4f
        )

        val doc = vm.currentDocument.value
        val pages = doc.pages.map { if (it.pageIndex == 0) it.copy(strokes = listOf(stroke1, stroke2, stroke3)) else it }
        vm.setCurrentDocument(doc.copy(pages = pages))
        // Set strokes and save snapshot
        vm.saveUndoSnapshot()
        // Erase only stroke 1 near (50, 100)
        val erased = vm.eraseStrokesNear(50f, 100f, radius = 20f)
        assertTrue("Stroke 1 should be erased", erased)

        val remaining = vm.currentDocument.value.pages[0].strokes
        assertEquals(2, remaining.size)
        assertFalse(remaining.any { it.id == "stroke_1" })
        assertTrue(remaining.any { it.id == "stroke_2" })
        assertTrue(remaining.any { it.id == "stroke_3" })

        // Undo the erasure
        assertTrue(vm.canUndo.value)
        vm.undo()
        val afterUndo = vm.currentDocument.value.pages[0].strokes
        assertEquals(3, afterUndo.size)
        assertTrue(afterUndo.any { it.id == "stroke_1" })
    }

    // -------------------------------------------------------------
    // Scenario 4: Clear one page while preserving another page
    // -------------------------------------------------------------
    @Test
    fun testScenario4_ClearOnePagePreservesOtherPage() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)

        // Setup Page 0 with stroke
        vm.goToPage(0)
        vm.onDrawingPointerDown(DrawingPoint(20f, 20f))
        vm.onDrawingPointerMove(DrawingPoint(60f, 60f))
        vm.onDrawingPointerUp()
        assertEquals(1, vm.currentDocument.value.pages[0].strokes.size)

        // Add Page 1 with stroke
        vm.addPage()
        assertEquals(1, vm.currentPageIndex.value)
        vm.onDrawingPointerDown(DrawingPoint(200f, 200f))
        vm.onDrawingPointerMove(DrawingPoint(250f, 250f))
        vm.onDrawingPointerUp()
        assertEquals(1, vm.currentDocument.value.pages[1].strokes.size)

        // Clear Page 1
        vm.clearCurrentPage()
        assertEquals(0, vm.currentDocument.value.pages[1].strokes.size)

        // Page 0 MUST remain preserved!
        assertEquals(1, vm.currentDocument.value.pages[0].strokes.size)

        // Undo on Page 1 restores it
        vm.undo()
        assertEquals(1, vm.currentDocument.value.pages[1].strokes.size)
    }

    // -------------------------------------------------------------
    // Scenario 5: Rearrange Brush and Hand/Pan, restart app, verify arrangement persists
    // -------------------------------------------------------------
    @Test
    fun testScenario5_ToolbarRearrangementPersistence() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        // 1. Arrange Brush and Hand/Pan right next to each other
        val customOrder = listOf(
            ToolbarToolItem.PEN,
            ToolbarToolItem.HAND,
            ToolbarToolItem.STROKE_ERASER,
            ToolbarToolItem.AREA_ERASER,
            ToolbarToolItem.SYMBOLS,
            ToolbarToolItem.SHAPE,
            ToolbarToolItem.UNDO_REDO
        )

        val vm1 = MainViewModel(app)
        vm1.updateToolbarOrder(customOrder)
        assertEquals(customOrder, vm1.toolbarItems.value)

        // 2. Simulate restarting app by creating a new MainViewModel instance
        val vm2 = MainViewModel(app)
        assertEquals(customOrder, vm2.toolbarItems.value)
        assertEquals(ToolbarToolItem.PEN, vm2.toolbarItems.value[0])
        assertEquals(ToolbarToolItem.HAND, vm2.toolbarItems.value[1])
    }

    // -------------------------------------------------------------
    // Scenario 6: Draw circle, triangle, rectangle, arrow, line; verify each is recognized
    // -------------------------------------------------------------
    @Test
    fun testScenario6_ShapeRecognitionAccuracy() {
        // 1. Straight Line
        val linePoints = (0..12).map { i -> DrawingPoint(10f + i * 15f, 50f) }
        val lineStroke = DrawingStroke(points = linePoints)
        val recognizedLine = ShapeRecognizer.recognizeAndSnap(lineStroke)
        assertTrue(recognizedLine.isShape)
        assertEquals(ShapeType.LINE, recognizedLine.shapeType)

        // 2. Circle
        val circlePoints = mutableListOf<DrawingPoint>()
        for (i in 0..30) {
            val a = 2.0 * PI * i / 30.0
            circlePoints.add(DrawingPoint(200f + 60f * cos(a).toFloat(), 200f + 60f * sin(a).toFloat()))
        }
        val circleStroke = DrawingStroke(points = circlePoints)
        val recognizedCircle = ShapeRecognizer.recognizeAndSnap(circleStroke)
        assertTrue(recognizedCircle.isShape)
        assertEquals(ShapeType.CIRCLE, recognizedCircle.shapeType)

        // 3. Triangle (3 distinct corners)
        val trianglePoints = mutableListOf<DrawingPoint>()
        // Side 1: (100, 100) to (200, 100)
        for (i in 0..10) trianglePoints.add(DrawingPoint(100f + i * 10f, 100f))
        // Side 2: (200, 100) to (150, 20f)
        for (i in 0..10) trianglePoints.add(DrawingPoint(200f - i * 5f, 100f - i * 8f))
        // Side 3: (150, 20) to (100, 100)
        for (i in 0..10) trianglePoints.add(DrawingPoint(150f - i * 5f, 20f + i * 8f))

        val triangleStroke = DrawingStroke(points = trianglePoints)
        val recognizedTriangle = ShapeRecognizer.recognizeAndSnap(triangleStroke)
        assertTrue("Triangle must be recognized as TRIANGLE", recognizedTriangle.isShape)
        assertEquals(ShapeType.TRIANGLE, recognizedTriangle.shapeType)

        // 4. Rectangle (4 distinct corners)
        val rectPoints = mutableListOf<DrawingPoint>()
        for (i in 0..10) rectPoints.add(DrawingPoint(50f + i * 10f, 50f))
        for (i in 0..10) rectPoints.add(DrawingPoint(150f, 50f + i * 10f))
        for (i in 0..10) rectPoints.add(DrawingPoint(150f - i * 10f, 150f))
        for (i in 0..10) rectPoints.add(DrawingPoint(50f, 150f - i * 10f))

        val rectStroke = DrawingStroke(points = rectPoints)
        val recognizedRect = ShapeRecognizer.recognizeAndSnap(rectStroke)
        assertTrue("Rectangle must be recognized as RECTANGLE", recognizedRect.isShape)
        assertEquals(ShapeType.RECTANGLE, recognizedRect.shapeType)

        // 5. Arrow (shaft + head)
        val arrowPoints = mutableListOf<DrawingPoint>()
        for (i in 0..15) arrowPoints.add(DrawingPoint(50f + i * 10f, 100f)) // shaft
        arrowPoints.add(DrawingPoint(180f, 85f)) // upper fin
        arrowPoints.add(DrawingPoint(200f, 100f)) // tip
        arrowPoints.add(DrawingPoint(180f, 115f)) // lower fin

        val arrowStroke = DrawingStroke(points = arrowPoints)
        val recognizedArrow = ShapeRecognizer.recognizeAndSnap(arrowStroke)
        assertTrue("Arrow must be recognized as ARROW", recognizedArrow.isShape)
        assertEquals(ShapeType.ARROW, recognizedArrow.shapeType)
    }

    // -------------------------------------------------------------
    // Scenario 7: Export page and verify drawings and symbols are preserved
    // -------------------------------------------------------------
    @Test
    fun testScenario7_ExportIntegrityPreservesDrawingsAndSymbols() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val doc = AnswerDocument(
            title = "NEET Optics & Chemistry",
            subject = NeetSubject.PHYSICS,
            questionNumber = "Q.28",
            paperStyle = PaperStyle.RULED,
            pages = listOf(
                PageData(
                    pageIndex = 0,
                    strokes = listOf(
                        DrawingStroke(
                            points = (0..10).map { i -> DrawingPoint(100f + i * 10f, 100f) },
                            toolType = ToolType.PEN,
                            color = 0xFF1D4ED8L
                        )
                    ),
                    textAnnotations = listOf(
                        TextAnnotation(text = "sin(i) / sin(r) = μ", x = 120f, y = 180f, fontSize = 26f, rotation = 0f),
                        TextAnnotation(text = "H₂SO₄", x = 120f, y = 260f, fontSize = 24f, rotation = 45f)
                    )
                )
            )
        )

        // PNG Export
        val pngs = ExportManager.exportAsPngImages(context, doc, scale = 1.0f)
        assertTrue("PNG files must be generated", pngs.isNotEmpty())
        assertTrue("PNG file exists", pngs[0].exists())
        assertTrue("PNG file has content", pngs[0].length() > 500)

        // PDF Export
        val pdf = ExportManager.exportAsPdf(context, doc)
        assertTrue("PDF file exists", pdf.exists())
        assertTrue("PDF file has content", pdf.length() > 0)
    }

    // -------------------------------------------------------------
    // Service & Provider Checks
    // -------------------------------------------------------------
    @Test
    fun testImeServiceDeclarationAndCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager

        val intent = Intent("android.view.InputMethod")
        intent.setPackage(context.packageName)
        val services = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
        assertTrue(services.isNotEmpty())

        val serviceInfo = services[0].serviceInfo
        assertEquals(InkKeyInputMethodService::class.java.name, serviceInfo.name)
        assertEquals("android.permission.BIND_INPUT_METHOD", serviceInfo.permission)

        val controller = Robolectric.buildService(InkKeyInputMethodService::class.java)
        val service = controller.create().get()
        assertNotNull(service)

        val inputView = service.onCreateInputView()
        assertNotNull(inputView)
    }

    @Test
    fun testKeyboardDeletePageAndClearPage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ImeDocumentRepository.getInstance(context)

        // Setup Page 0 with a stroke
        repo.goToPage(0)
        repo.addStroke(DrawingStroke(id = "p0_stroke", points = listOf(DrawingPoint(10f, 10f), DrawingPoint(20f, 20f))))
        assertEquals(1, repo.document.value.pages[0].strokes.size)

        // Add Page 1 with a stroke
        repo.addPage()
        assertEquals(1, repo.currentPageIndex.value)
        assertEquals(2, repo.document.value.pages.size)
        repo.addStroke(DrawingStroke(id = "p1_stroke", points = listOf(DrawingPoint(30f, 30f), DrawingPoint(40f, 40f))))
        assertEquals(1, repo.document.value.pages[1].strokes.size)

        // Add Page 2 with a stroke
        repo.addPage()
        assertEquals(2, repo.currentPageIndex.value)
        assertEquals(3, repo.document.value.pages.size)
        repo.addStroke(DrawingStroke(id = "p2_stroke", points = listOf(DrawingPoint(50f, 50f), DrawingPoint(60f, 60f))))

        // Clear Page 2
        repo.clearCurrentPage()
        assertEquals(0, repo.document.value.pages[2].strokes.size)
        // Pages 0 and 1 must be preserved!
        assertEquals(1, repo.document.value.pages[0].strokes.size)
        assertEquals(1, repo.document.value.pages[1].strokes.size)

        // Delete Page 2 -> Document now has 2 pages
        repo.deleteCurrentPage()
        assertEquals(2, repo.document.value.pages.size)
        assertEquals(1, repo.currentPageIndex.value) // Active index moved to page 1
        assertEquals("p1_stroke", repo.document.value.pages[1].strokes[0].id)
        assertEquals("p0_stroke", repo.document.value.pages[0].strokes[0].id)
    }

    @Test
    fun testKeyboardSymbolPlacementAndEditing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ImeDocumentRepository.getInstance(context)

        repo.clearCurrentPage()
        repo.insertSymbolOnCanvas("ΔH", 150f, 150f)

        val page = repo.document.value.pages[repo.currentPageIndex.value]
        assertEquals(1, page.textAnnotations.size)
        val sym = page.textAnnotations[0]
        assertEquals("ΔH", sym.text)
        assertEquals(150f, sym.x)
        assertEquals(150f, sym.y)
        assertEquals(repo.selectedSymbolId.value, sym.id)

        // Move symbol
        repo.moveSelectedSymbol(30f, 20f)
        val movedSym = repo.document.value.pages[repo.currentPageIndex.value].textAnnotations[0]
        assertEquals(180f, movedSym.x)
        assertEquals(170f, movedSym.y)

        // Resize symbol
        val initialSize = movedSym.fontSize
        repo.resizeSelectedSymbol(8f)
        val resizedSym = repo.document.value.pages[repo.currentPageIndex.value].textAnnotations[0]
        assertEquals(initialSize + 8f, resizedSym.fontSize)

        // Rotate symbol
        repo.rotateSelectedSymbol(45f)
        val rotatedSym = repo.document.value.pages[repo.currentPageIndex.value].textAnnotations[0]
        assertEquals(45f, rotatedSym.rotation)

        // Delete symbol
        repo.deleteSelectedSymbol()
        val afterDelete = repo.document.value.pages[repo.currentPageIndex.value]
        assertEquals(0, afterDelete.textAnnotations.size)
        assertNull(repo.selectedSymbolId.value)
    }

    @Test
    fun testKeyboardEraserModesAndUndo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ImeDocumentRepository.getInstance(context)

        repo.clearCurrentPage()
        val stroke = DrawingStroke(
            id = "test_stroke",
            points = (0..10).map { i -> DrawingPoint(100f + i * 10f, 100f) },
            width = 4f
        )
        repo.addStroke(stroke)
        assertEquals(1, repo.document.value.pages[repo.currentPageIndex.value].strokes.size)

        // Stroke erase
        val erased = repo.eraseStrokesAt(150f, 100f, 20f)
        assertTrue(erased)
        assertEquals(0, repo.document.value.pages[repo.currentPageIndex.value].strokes.size)

        // Undo
        assertTrue(repo.canUndo.value)
        repo.undo()
        assertEquals(1, repo.document.value.pages[repo.currentPageIndex.value].strokes.size)
    }

    @Test
    fun testMainActivityOpensSettingsWithIntentWithoutCrash() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra("OPEN_SETTINGS", true)
        }
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent)
        val activity = controller.setup().get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
    }
}

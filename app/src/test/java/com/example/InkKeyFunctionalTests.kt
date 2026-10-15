package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.export.ExportManager
import com.example.model.AnswerDocument
import com.example.model.PageData
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.ShapeType
import com.example.model.TextAnnotation
import com.example.model.ToolType
import com.example.util.ShapeRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
class InkKeyFunctionalTests {

    // =========================================================================
    // BUG 1 & 2: TOOL SWITCHING & PRESERVATION
    // =========================================================================
    @Test
    fun testToolSwitchingPreservesProperties() {
        val initialColor = 0xFF2563EB // Blue
        val initialEraserRadius = 24f

        var stroke = DrawingStroke(
            toolType = ToolType.PEN,
            color = initialColor,
            width = 4f
        )
        assertEquals(ToolType.PEN, stroke.toolType)
        assertEquals(initialColor, stroke.color)

        // Switching to eraser preserves color property of pen
        val eraserStroke = stroke.copy(toolType = ToolType.STROKE_ERASER)
        assertEquals(ToolType.STROKE_ERASER, eraserStroke.toolType)
        assertEquals(initialColor, eraserStroke.color)

        // Switching back to pen keeps initial color
        val backToPen = eraserStroke.copy(toolType = ToolType.PEN)
        assertEquals(ToolType.PEN, backToPen.toolType)
        assertEquals(initialColor, backToPen.color)
    }

    // =========================================================================
    // BUG 3: EXPORT DIRECTORY & FILE ACCESSIBILITY
    // =========================================================================
    @Test
    fun testExportDirectoryAndAccessibility() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val exportDir = ExportManager.getExportDirectory(context)
        assertTrue("Export directory must exist", exportDir.exists())
        assertEquals("InkKey_Exports", exportDir.name)

        val displayPath = ExportManager.getExportDirectoryDisplayPath(context)
        assertTrue(displayPath.contains("InkKey_Exports") || displayPath.contains("Documents"))

        // Create a test exported PNG file
        val dummyPng = File(exportDir, "InkKey_test_solution_export.png").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        }
        val dummyPdf = File(exportDir, "InkKey_test_solution_export.pdf").apply {
            writeBytes("test pdf".toByteArray())
        }

        val exportedFiles = ExportManager.getExportedFiles(context)
        assertTrue("Exported files list should contain created files", exportedFiles.any { it.name == dummyPng.name })
        assertTrue("Exported files list should contain PDF", exportedFiles.any { it.name == dummyPdf.name })

        // Test Delete All Exported Files removes them cleanly
        val deletedCount = ExportManager.clearAllExportedFiles(context)
        assertTrue("clearAllExportedFiles should delete at least the 2 test files", deletedCount >= 2)

        val filesAfterDelete = ExportManager.getExportedFiles(context)
        assertFalse("Exported files should not contain dummy PNG after clear", filesAfterDelete.any { it.name == dummyPng.name })
        assertFalse("Exported files should not contain dummy PDF after clear", filesAfterDelete.any { it.name == dummyPdf.name })
    }

    @Test
    fun testAmoledBlackPaperStyle_ExportAndRendering() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val doc = AnswerDocument(
            title = "OLED Physics Problem",
            paperStyle = com.example.model.PaperStyle.AMOLED_BLACK
        )
        assertEquals(com.example.model.PaperStyle.AMOLED_BLACK, doc.paperStyle)

        // Render page to bitmap with AMOLED black style
        val bitmap = ExportManager.renderPageToBitmap(
            page = doc.pages[0],
            document = doc,
            totalPages = 1,
            scale = 1.0f,
            forceWhiteBackground = false,
            minWidth = 400,
            minHeight = 300
        )
        assertNotNull("Bitmap should be rendered", bitmap)
        assertEquals(400, bitmap.width)
        assertEquals(300, bitmap.height)
    }

    // =========================================================================
    // BUG 4: SHAPE RECOGNITION (Straight line, Circle, Ellipse, Triangle, Rectangle, Arrow, and Freehand)
    // =========================================================================
    @Test
    fun testShapeRecognition_StraightLine() {
        // Horizontal straight line from (10, 10) to (150, 10)
        val points = (10..150 step 10).map { x -> DrawingPoint(x.toFloat(), 10f) }
        val stroke = DrawingStroke(points = points)
        val recognized = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Should be recognized as shape", recognized.isShape)
        assertEquals(ShapeType.LINE, recognized.shapeType)
    }

    @Test
    fun testShapeRecognition_Circle() {
        // Circle centered at (100, 100) with radius 40
        val points = mutableListOf<DrawingPoint>()
        val numPoints = 36
        for (i in 0..numPoints) {
            val angle = 2.0 * PI * i / numPoints
            val x = (100 + 40 * cos(angle)).toFloat()
            val y = (100 + 40 * sin(angle)).toFloat()
            points.add(DrawingPoint(x, y))
        }
        val stroke = DrawingStroke(points = points)
        val recognized = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Should be recognized as shape", recognized.isShape)
        assertEquals(ShapeType.CIRCLE, recognized.shapeType)
    }

    @Test
    fun testShapeRecognition_Ellipse() {
        // Ellipse centered at (100, 100) with semi-major a=60, semi-minor b=25 (aspect ratio ~2.4)
        val points = mutableListOf<DrawingPoint>()
        val numPoints = 36
        for (i in 0..numPoints) {
            val angle = 2.0 * PI * i / numPoints
            val x = (100 + 60 * cos(angle)).toFloat()
            val y = (100 + 25 * sin(angle)).toFloat()
            points.add(DrawingPoint(x, y))
        }
        val stroke = DrawingStroke(points = points)
        val recognized = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Should be recognized as shape", recognized.isShape)
        assertEquals(ShapeType.ELLIPSE, recognized.shapeType)
    }

    @Test
    fun testShapeRecognition_Triangle() {
        // Triangle with vertices (50, 20), (90, 90), (10, 90)
        val p1 = DrawingPoint(50f, 20f)
        val p2 = DrawingPoint(90f, 90f)
        val p3 = DrawingPoint(10f, 90f)

        val points = mutableListOf<DrawingPoint>()
        fun interpolate(from: DrawingPoint, to: DrawingPoint, steps: Int = 8) {
            for (s in 0..steps) {
                val t = s.toFloat() / steps
                points.add(DrawingPoint(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t))
            }
        }
        interpolate(p1, p2)
        interpolate(p2, p3)
        interpolate(p3, p1)

        val stroke = DrawingStroke(points = points)
        val recognized = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Should be recognized as shape", recognized.isShape)
        assertEquals(ShapeType.TRIANGLE, recognized.shapeType)
    }

    @Test
    fun testShapeRecognition_Rectangle() {
        // Rectangle from (20, 20) to (120, 80)
        val points = mutableListOf<DrawingPoint>()
        // Top edge: (20, 20) -> (120, 20)
        for (x in 20..120 step 10) points.add(DrawingPoint(x.toFloat(), 20f))
        // Right edge: (120, 20) -> (120, 80)
        for (y in 20..80 step 10) points.add(DrawingPoint(120f, y.toFloat()))
        // Bottom edge: (120, 80) -> (20, 80)
        for (x in 120 downTo 20 step 10) points.add(DrawingPoint(x.toFloat(), 80f))
        // Left edge: (20, 80) -> (20, 20)
        for (y in 80 downTo 20 step 10) points.add(DrawingPoint(20f, y.toFloat()))

        val stroke = DrawingStroke(points = points)
        val recognized = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Should be recognized as shape", recognized.isShape)
        assertEquals(ShapeType.RECTANGLE, recognized.shapeType)
    }

    @Test
    fun testShapeRecognition_Arrow() {
        // Shaft: (10, 50) -> (100, 50), then barb 1 back to (80, 40), then barb 2 to (80, 60)
        val points = mutableListOf<DrawingPoint>()
        for (x in 10..100 step 10) points.add(DrawingPoint(x.toFloat(), 50f))
        // Arrow head barb 1
        points.add(DrawingPoint(90f, 45f))
        points.add(DrawingPoint(80f, 40f))
        // Back to tip and barb 2
        points.add(DrawingPoint(100f, 50f))
        points.add(DrawingPoint(90f, 55f))
        points.add(DrawingPoint(80f, 60f))

        val stroke = DrawingStroke(points = points)
        val recognized = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Should be recognized as shape", recognized.isShape)
        assertEquals(ShapeType.ARROW, recognized.shapeType)
    }

    @Test
    fun testShapeRecognition_PreservesFreehandWhenUncertain() {
        // Random squiggly handwritten wave / word stroke (not a regular geometric shape)
        val points = mutableListOf<DrawingPoint>()
        for (i in 0..30) {
            val x = i * 5f
            val y = 50f + sin(i * 0.8f) * 15f
            points.add(DrawingPoint(x, y))
        }
        val stroke = DrawingStroke(points = points)
        val recognized = ShapeRecognizer.recognizeAndSnap(stroke)

        assertFalse("Irregular handwriting stroke must remain freehand", recognized.isShape)
        assertEquals(ShapeType.NONE, recognized.shapeType)
        assertEquals(points.size, recognized.points.size)
    }

    // =========================================================================
    // BUG 5: INSERTED-SYMBOL SELECTION BOUNDS (Tight hit-testing)
    // =========================================================================
    @Test
    fun testSymbolTightHitTesting_AllowsNearbyWriting() {
        // Formula: ω = 2πf
        // Symbol ω placed at (50, 100) with fontSize 28f
        val omegaAnnotation = TextAnnotation(
            text = "ω",
            x = 50f,
            y = 100f,
            fontSize = 28f
        )

        // 1. Tapping directly on the symbol (x=55, y=95) MUST hit
        val hitsDirectly = omegaAnnotation.hitTest(55f, 95f, padding = 8f)
        assertTrue("Touching directly on symbol should hit", hitsDirectly)

        // 2. Writing nearby: e.g. drawing '=' at x = 95f, y = 100f
        // In the old implementation with 65-90px padding, x=95 would accidentally select 'ω'
        // In the tight bounds implementation, x=95 MUST NOT hit 'ω'
        val hitsNearbyWriting = omegaAnnotation.hitTest(95f, 100f, padding = 8f)
        assertFalse("Writing nearby (= sign) must NOT select the symbol", hitsNearbyWriting)

        // 3. Writing above or below the symbol: e.g. at y = 60f or y = 140f
        val hitsAbove = omegaAnnotation.hitTest(55f, 60f, padding = 8f)
        val hitsBelow = omegaAnnotation.hitTest(55f, 140f, padding = 8f)
        assertFalse("Writing above symbol must not hit", hitsAbove)
        assertFalse("Writing below symbol must not hit", hitsBelow)
    }

    @Test
    fun testSymbolHitTesting_WithRotation() {
        val annotation = TextAnnotation(
            text = "F_net",
            x = 100f,
            y = 100f,
            fontSize = 24f,
            rotation = 45f
        )
        // Hit test directly at origin
        assertTrue(annotation.hitTest(100f, 95f, padding = 10f))
        // Far away point must not hit
        assertFalse(annotation.hitTest(200f, 200f, padding = 8f))
    }

    // =========================================================================
    // AREA ERASER & INFINITE CANVAS TESTS
    // =========================================================================
    @Test
    fun testAreaEraser_SplitsStrokeIntoTwoSegments_DoesNotDeleteEntireStroke() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.ime.data.ImeDocumentRepository.getInstance(context)
        repository.clearCurrentPage()

        // Draw a horizontal line from x=0 to x=200 at y=100
        val points = mutableListOf<DrawingPoint>()
        for (x in 0..200 step 10) {
            points.add(DrawingPoint(x.toFloat(), 100f, 1f))
        }
        val initialStroke = DrawingStroke(
            toolType = ToolType.PEN,
            color = 0xFF000000,
            width = 4f,
            points = points
        )
        repository.addStroke(initialStroke)
        assertEquals(1, repository.document.value.pages[0].strokes.size)

        // Area erase at center (100, 100) with radius 25f
        val modified = repository.eraseAreaStrokes(100f, 100f, radius = 25f)
        assertTrue("Area eraser must report stroke modification", modified)

        val remainingStrokes = repository.document.value.pages[0].strokes
        assertEquals("Area eraser must split stroke into 2 separate segments, NOT delete entire stroke", 2, remainingStrokes.size)

        // Segment 1 must be to the left of the eraser
        val seg1 = remainingStrokes[0]
        assertTrue(seg1.points.all { it.x < 100f })

        // Segment 2 must be to the right of the eraser
        val seg2 = remainingStrokes[1]
        assertTrue(seg2.points.all { it.x > 100f })
    }

    @Test
    fun testStrokeEraser_DeletesEntireStroke() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.ime.data.ImeDocumentRepository.getInstance(context)
        repository.clearCurrentPage()

        val points = listOf(
            DrawingPoint(10f, 10f, 1f),
            DrawingPoint(50f, 50f, 1f),
            DrawingPoint(100f, 100f, 1f)
        )
        val stroke = DrawingStroke(
            toolType = ToolType.PEN,
            color = 0xFF000000,
            width = 4f,
            points = points
        )
        repository.addStroke(stroke)
        assertEquals(1, repository.document.value.pages[0].strokes.size)

        // Stroke eraser touches anywhere near stroke: entire stroke deleted
        val removed = repository.eraseStrokesAt(50f, 50f, radius = 20f)
        assertTrue(removed)
        assertTrue(repository.document.value.pages[0].strokes.isEmpty())
    }

    @Test
    fun testInfiniteCanvasPanAndZoom_StateAndClamping() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = com.example.viewmodel.MainViewModel(context as android.app.Application)

        // Initial values
        assertEquals(1f, viewModel.zoomScale.value)
        assertEquals(0f, viewModel.panOffset.value.x)
        assertEquals(0f, viewModel.panOffset.value.y)

        // Infinite 2D pan: moves in any direction (+X, -X, +Y, -Y)
        viewModel.updatePanOffset(150f, -320f)
        assertEquals(150f, viewModel.panOffset.value.x)
        assertEquals(-320f, viewModel.panOffset.value.y)

        // Restored zoom range: 0.1 to 3.0
        viewModel.setZoomScale(2.5f)
        assertEquals(2.5f, viewModel.zoomScale.value)

        viewModel.setZoomScale(10.0f) // Beyond max
        assertEquals(3f, viewModel.zoomScale.value)

        viewModel.setZoomScale(0.01f) // Below min
        assertEquals(0.1f, viewModel.zoomScale.value)

        // Reset zoom & pan
        viewModel.resetZoom()
        assertEquals(1f, viewModel.zoomScale.value)
        assertEquals(0f, viewModel.panOffset.value.x)
        assertEquals(0f, viewModel.panOffset.value.y)
    }

    // =========================================================================
    // XP-PEN DECO MINI 7 V2: HARDWARE STYLUS BUTTONS & EXPRESS KEYS
    // =========================================================================
    @Test
    fun testTabletConfig_StylusBarrelButtonsCustomizationAndPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = com.example.viewmodel.MainViewModel(context as android.app.Application)

        // Maintained default: lower AREA_ERASER, upper STROKE_ERASER
        assertEquals(com.example.model.TabletAction.AREA_ERASER, viewModel.tabletConfig.value.lowerBarrelAction)
        assertEquals(com.example.model.TabletAction.STROKE_ERASER, viewModel.tabletConfig.value.upperBarrelAction)

        // Customize lower barrel button to UNDO, upper barrel button to AREA_ERASER
        val customConfig = viewModel.tabletConfig.value.copy(
            lowerBarrelAction = com.example.model.TabletAction.UNDO,
            upperBarrelAction = com.example.model.TabletAction.AREA_ERASER
        )
        viewModel.updateTabletConfig(customConfig)

        assertEquals(com.example.model.TabletAction.UNDO, viewModel.tabletConfig.value.lowerBarrelAction)
        assertEquals(com.example.model.TabletAction.AREA_ERASER, viewModel.tabletConfig.value.upperBarrelAction)

        // Test persistence: reloading from SharedPreferences preserves user configuration
        val reloaded = com.example.model.TabletConfig.load(context)
        assertEquals(com.example.model.TabletAction.UNDO, reloaded.lowerBarrelAction)
        assertEquals(com.example.model.TabletAction.AREA_ERASER, reloaded.upperBarrelAction)
    }

    @Test
    fun testTabletConfig_ExpressKeysMappingAndScanCodes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = com.example.model.TabletConfig.load(context)

        // 1. Check matching Linux evdev scan codes 256..263 for K1..K8
        assertTrue("Scan code 256 must match K1", config.matchesExpressKey(1, keyCode = 0, scanCode = 256))
        assertTrue("Scan code 257 must match K2", config.matchesExpressKey(2, keyCode = 0, scanCode = 257))
        assertTrue("Scan code 263 must match K8", config.matchesExpressKey(8, keyCode = 0, scanCode = 263))

        // 2. Check standard Android BUTTON_1..8
        assertTrue("KEYCODE_BUTTON_1 must match K1", config.matchesExpressKey(1, keyCode = android.view.KeyEvent.KEYCODE_BUTTON_1, scanCode = 0))
        assertTrue("KEYCODE_BUTTON_2 must match K2", config.matchesExpressKey(2, keyCode = android.view.KeyEvent.KEYCODE_BUTTON_2, scanCode = 0))

        // 3. Custom key learning: bind custom key code
        val customMap = config.customKeyCodes.toMutableMap()
        customMap[4] = 999 // Custom key code
        val customConfig = config.copy(customKeyCodes = customMap)
        assertTrue("Custom key binding must take precedence", customConfig.matchesExpressKey(4, keyCode = 999, scanCode = 0))
        assertFalse("Old key should not match wrong index", customConfig.matchesExpressKey(5, keyCode = 999, scanCode = 0))
    }

    @Test
    fun testTabletHardwareKeyEvent_TriggersCustomAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = com.example.viewmodel.MainViewModel(context as android.app.Application)

        // Set K1 to NEW_PAGE
        val updatedKeys = viewModel.tabletConfig.value.expressKeyMappings.toMutableMap()
        updatedKeys[1] = com.example.model.TabletAction.NEW_PAGE
        viewModel.updateTabletConfig(viewModel.tabletConfig.value.copy(expressKeyMappings = updatedKeys))

        val initialPages = viewModel.currentDocument.value.pages.size
        val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BUTTON_1)
        val handled = viewModel.handleHardwareKeyEvent(android.view.KeyEvent.KEYCODE_BUTTON_1, scanCode = 0, event = downEvent)
        assertTrue("Hardware key event must be handled", handled)
        assertEquals("New page must be created", initialPages + 1, viewModel.currentDocument.value.pages.size)
    }

    @Test
    fun testProlongedHandwritingAndUndoMemoryStressTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = com.example.viewmodel.MainViewModel(context as android.app.Application)

        // Simulate prolonged writing session: 200 strokes
        for (i in 0 until 200) {
            val pts = listOf(
                DrawingPoint(i.toFloat(), i.toFloat(), 0.5f),
                DrawingPoint(i + 10f, i + 10f, 0.7f),
                DrawingPoint(i + 20f, i + 25f, 0.9f)
            )
            val stroke = DrawingStroke(
                toolType = ToolType.PEN,
                color = 0xFF000000,
                width = 3f,
                points = pts
            )
            viewModel.saveUndoSnapshot()
            // Add stroke
            val doc = viewModel.currentDocument.value
            val pIdx = viewModel.currentPageIndex.value
            val updatedPages = doc.pages.map { p ->
                if (p.pageIndex == pIdx) p.copy(strokes = p.strokes + stroke) else p
            }
            viewModel.setCurrentDocument(doc.copy(pages = updatedPages))
        }

        val page = viewModel.currentDocument.value.pages[0]
        assertEquals(200, page.strokes.size)

        // Undo stack must be capped (max 15 snapshots)
        // Memory trim must keep memory small
        viewModel.trimMemory()

        // Erase area with dense movement: must split cleanly without throwing OutOfMemory
        val erased = viewModel.eraseAreaStrokes(15f, 15f, radius = 25f)
        assertTrue(erased)
    }
}

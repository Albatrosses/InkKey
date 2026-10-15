package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.StrokesSerializer
import com.example.export.ExportManager
import com.example.model.AnswerDocument
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.NeetSubject
import com.example.model.PageData
import com.example.model.PaperStyle
import com.example.model.ShapeType
import com.example.model.ToolType
import com.example.util.ShapeRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("InkKey", appName)
    }

    @Test
    fun `strokes serialization and deserialization roundtrip`() {
        val stroke = DrawingStroke(
            id = "test-stroke-1",
            toolType = ToolType.PEN,
            color = 0xFF1E3A8AL,
            width = 4f,
            alpha = 1f,
            points = listOf(
                DrawingPoint(10f, 20f, 1f),
                DrawingPoint(30f, 40f, 0.8f)
            )
        )
        val pages = listOf(
            PageData(pageIndex = 0, strokes = listOf(stroke), textAnnotations = emptyList())
        )

        val json = StrokesSerializer.serializePages(pages)
        val deserialized = StrokesSerializer.deserializePages(json)

        assertEquals(1, deserialized.size)
        assertEquals(1, deserialized[0].strokes.size)
        assertEquals("test-stroke-1", deserialized[0].strokes[0].id)
        assertEquals(2, deserialized[0].strokes[0].points.size)
    }

    @Test
    fun `shape recognizer detects straight line`() {
        // Points in a straight line from (10, 10) to (100, 10)
        val points = (1..15).map { i ->
            DrawingPoint(10f + i * 6f, 10f)
        }
        val stroke = DrawingStroke(
            points = points,
            toolType = ToolType.PEN
        )

        val snapped = ShapeRecognizer.recognizeAndSnap(stroke)
        assertTrue(snapped.isShape)
        assertEquals(ShapeType.LINE, snapped.shapeType)
    }

    @Test
    fun `export manager bitmap rendering generates valid image`() {
        val doc = AnswerDocument(
            title = "Test Kinematics",
            subject = NeetSubject.PHYSICS,
            questionNumber = "Q.1",
            paperStyle = PaperStyle.RULED
        )
        val stroke = DrawingStroke(
            points = listOf(DrawingPoint(50f, 50f), DrawingPoint(100f, 100f)),
            toolType = ToolType.PEN
        )
        val page = PageData(pageIndex = 0, strokes = listOf(stroke))

        val bitmap = ExportManager.renderPageToBitmap(
            page = page,
            document = doc,
            totalPages = 1,
            scale = 1.0f,
            minWidth = 400,
            minHeight = 600
        )

        assertNotNull(bitmap)
        assertTrue(bitmap.width >= 400)
        assertTrue(bitmap.height >= 600)
    }

    @Test
    fun `launch MainActivity and navigate to SettingsScreen without crash`() {
        val intent = android.content.Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra("OPEN_SETTINGS", true)
        }
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java, intent).setup()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}


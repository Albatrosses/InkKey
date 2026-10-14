package com.example

import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.ShapeType
import com.example.util.ShapeRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ShapeRecognizerTest {

    @Test
    fun testStraightLineRecognition() {
        val points = mutableListOf<DrawingPoint>()
        for (i in 0..20) {
            val t = i / 20f
            // Slightly jittered straight line from (10, 10) to (210, 110)
            val jitter = if (i == 0 || i == 20) 0f else if (i % 2 == 0) 0.5f else -0.5f
            points.add(DrawingPoint(10f + 200f * t, 10f + 100f * t + jitter))
        }

        val stroke = DrawingStroke(points = points)
        val result = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Line should be recognized as shape", result.isShape)
        assertEquals(ShapeType.LINE, result.shapeType)
        assertEquals(2, result.points.size)
        assertEquals(10f, result.points[0].x, 0.1f)
        assertEquals(10f, result.points[0].y, 0.1f)
    }

    @Test
    fun testArrowRecognition() {
        val points = mutableListOf<DrawingPoint>()
        // Shaft from (50, 100) to (250, 100)
        for (i in 0..15) {
            val t = i / 15f
            points.add(DrawingPoint(50f + 200f * t, 100f))
        }
        // Barb 1: back-up
        points.add(DrawingPoint(220f, 80f))
        points.add(DrawingPoint(250f, 100f))
        // Barb 2: back-down
        points.add(DrawingPoint(220f, 120f))

        val stroke = DrawingStroke(points = points)
        val result = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Arrow should be recognized as shape", result.isShape)
        assertEquals(ShapeType.ARROW, result.shapeType)
    }

    @Test
    fun testTriangleRecognition() {
        val points = mutableListOf<DrawingPoint>()
        // Side 1: (100, 200) to (200, 50)
        for (i in 0..10) {
            val t = i / 10f
            points.add(DrawingPoint(100f + 100f * t, 200f - 150f * t))
        }
        // Side 2: (200, 50) to (300, 200)
        for (i in 1..10) {
            val t = i / 10f
            points.add(DrawingPoint(200f + 100f * t, 50f + 150f * t))
        }
        // Side 3: (300, 200) back to (100, 200)
        for (i in 1..10) {
            val t = i / 10f
            points.add(DrawingPoint(300f - 200f * t, 200f))
        }

        val stroke = DrawingStroke(points = points)
        val result = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Triangle should be recognized as shape", result.isShape)
        assertEquals(ShapeType.TRIANGLE, result.shapeType)
    }

    @Test
    fun testRectangleRecognition_SquareDoesNotTurnIntoCircle() {
        val points = mutableListOf<DrawingPoint>()
        // Top edge: (100, 100) to (250, 100)
        for (i in 0..10) {
            val t = i / 10f
            points.add(DrawingPoint(100f + 150f * t, 100f))
        }
        // Right edge: (250, 100) to (250, 250)
        for (i in 1..10) {
            val t = i / 10f
            points.add(DrawingPoint(250f, 100f + 150f * t))
        }
        // Bottom edge: (250, 250) to (100, 250)
        for (i in 1..10) {
            val t = i / 10f
            points.add(DrawingPoint(250f - 150f * t, 250f))
        }
        // Left edge: (100, 250) to (100, 100)
        for (i in 1..10) {
            val t = i / 10f
            points.add(DrawingPoint(100f, 250f - 150f * t))
        }

        val stroke = DrawingStroke(points = points)
        val result = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Square should be recognized as shape", result.isShape)
        assertEquals(ShapeType.RECTANGLE, result.shapeType)
    }

    @Test
    fun testCircleRecognition() {
        val points = mutableListOf<DrawingPoint>()
        val cx = 200f
        val cy = 200f
        val r = 80f
        val numSamples = 36
        for (i in 0..numSamples) {
            val angle = 2f * PI.toFloat() * i / numSamples
            // Slight jitter to simulate real handwriting
            val jitter = if (i % 2 == 0) 1.5f else -1.5f
            points.add(DrawingPoint(cx + (r + jitter) * cos(angle), cy + (r + jitter) * sin(angle)))
        }

        val stroke = DrawingStroke(points = points)
        val result = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Circle should be recognized as shape", result.isShape)
        assertEquals(ShapeType.CIRCLE, result.shapeType)
    }

    @Test
    fun testEllipseRecognition() {
        val points = mutableListOf<DrawingPoint>()
        val cx = 250f
        val cy = 200f
        val rx = 120f
        val ry = 50f // Distinctly elongated
        val numSamples = 36
        for (i in 0..numSamples) {
            val angle = 2f * PI.toFloat() * i / numSamples
            val jitter = if (i % 2 == 0) 1.0f else -1.0f
            points.add(DrawingPoint(cx + (rx + jitter) * cos(angle), cy + (ry + jitter) * sin(angle)))
        }

        val stroke = DrawingStroke(points = points)
        val result = ShapeRecognizer.recognizeAndSnap(stroke)

        assertTrue("Ellipse should be recognized as shape", result.isShape)
        assertEquals(ShapeType.ELLIPSE, result.shapeType)
    }

    @Test
    fun testFreehandPreservationWhenUncertain() {
        // Arbitrary squiggle / signature that is neither line, circle, nor polygon
        val points = mutableListOf<DrawingPoint>()
        points.add(DrawingPoint(10f, 10f))
        points.add(DrawingPoint(50f, 80f))
        points.add(DrawingPoint(30f, 40f))
        points.add(DrawingPoint(90f, 120f))
        points.add(DrawingPoint(40f, 90f))
        points.add(DrawingPoint(120f, 30f))
        points.add(DrawingPoint(70f, 150f))

        val stroke = DrawingStroke(points = points)
        val result = ShapeRecognizer.recognizeAndSnap(stroke)

        assertFalse("Uncertain shape should NOT be snapped to a shape", result.isShape)
        assertEquals(ShapeType.NONE, result.shapeType)
        assertEquals(points.size, result.points.size)
    }
}

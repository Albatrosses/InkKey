package com.example

import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.ShapeType
import com.example.util.ShapeRecognizer
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class ShapeRecognitionRegressionTest {
    private fun snap(points: List<DrawingPoint>) = ShapeRecognizer.recognizeAndSnap(DrawingStroke(points = points))
    private fun rotate(p: DrawingPoint, angle: Float) = DrawingPoint(200f + p.x * cos(angle) - p.y * sin(angle), 200f + p.x * sin(angle) + p.y * cos(angle))
    private fun polygon(vertices: List<DrawingPoint>) = vertices.zipWithNext { a, b ->
        (0 until 16).map { i -> val t = i / 16f; DrawingPoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) }
    }.flatten() + vertices.last()

    @Test fun tiltedRectanglePreservesItsOrientation() {
        val pts = polygon(listOf(DrawingPoint(-90f,-40f), DrawingPoint(90f,-40f), DrawingPoint(90f,40f), DrawingPoint(-90f,40f), DrawingPoint(-90f,-40f))).map { rotate(it, 0.6f) }
        val result = snap(pts)
        assertEquals(ShapeType.RECTANGLE, result.shapeType)
        assertTrue(abs(result.points[1].x - result.points[0].x) > 5f)
        assertTrue(abs(result.points[1].y - result.points[0].y) > 5f)
    }

    @Test fun rectangleStartingMidwayAlongAnEdgeIsRecognized() {
        val pts = polygon(listOf(DrawingPoint(100f,0f), DrawingPoint(200f,0f), DrawingPoint(200f,100f), DrawingPoint(0f,100f), DrawingPoint(0f,0f), DrawingPoint(100f,0f)))
        assertEquals(ShapeType.RECTANGLE, snap(pts).shapeType)
    }

    @Test fun tiltedEllipseIsRecognized() {
        val pts = (0..100).map { i -> val t = 2f * PI.toFloat() * i / 100; rotate(DrawingPoint(110f*cos(t), 42f*sin(t)), 0.7f) }
        assertEquals(ShapeType.ELLIPSE, snap(pts).shapeType)
    }

    @Test fun unevenSamplingAndStationaryPacketsDoNotBiasCircleFit() {
        val pts = (0..90).flatMap { i ->
            val t = 2f * PI.toFloat() * i / 90
            val p = DrawingPoint(100f + 60f*cos(t), 100f + 60f*sin(t))
            List(if (i < 20) 12 else 1) { p }
        }
        assertEquals(ShapeType.CIRCLE, snap(pts).shapeType)
    }

    @Test fun nearlyClosedCircleIsRecognized() {
        val pts = (0..80).map { i -> val t = 345f * PI.toFloat()/180f * i/80; DrawingPoint(100f + 60f*cos(t), 100f + 60f*sin(t)) }
        assertEquals(ShapeType.CIRCLE, snap(pts).shapeType)
    }

    @Test fun anOpenArcIsPreserved() {
        val pts = (0..60).map { i -> val t = 240f*PI.toFloat()/180f*i/60; DrawingPoint(100f + 60f*cos(t),100f + 60f*sin(t)) }
        assertFalse(snap(pts).isShape)
    }

    @Test fun curvedStrokeDoesNotBecomeALine() {
        val pts = (0..30).map { i -> val t = i/30f; DrawingPoint(100f*t, 20f*sin(PI.toFloat()*t)) }
        assertFalse(snap(pts).isShape)
    }

    @Test fun selectedArrowWorksWithTwoPoints() {
        val result = ShapeRecognizer.recognizeAndSnap(DrawingStroke(points = listOf(DrawingPoint(0f,0f),DrawingPoint(100f,60f)),shapeType = ShapeType.ARROW))
        assertEquals(ShapeType.ARROW,result.shapeType)
        assertEquals(5,result.points.size)
    }

    @Test fun selectedLineWorksWithTwoPoints() {
        val result = ShapeRecognizer.recognizeAndSnap(DrawingStroke(points = listOf(DrawingPoint(0f,0f),DrawingPoint(100f,60f)),shapeType = ShapeType.LINE))
        assertTrue(result.isShape)
        assertEquals(2,result.points.size)
    }

    @Test fun invalidCoordinatesArePreservedWithoutCrashing() {
        val pts = listOf(DrawingPoint(0f,0f),DrawingPoint(Float.NaN,10f),DrawingPoint(20f,20f))
        assertFalse(snap(pts).isShape)
    }
}

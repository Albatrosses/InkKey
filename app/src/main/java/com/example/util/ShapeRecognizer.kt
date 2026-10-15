package com.example.util

import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.ShapeType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object ShapeRecognizer {

    fun recognizeAndSnap(stroke: DrawingStroke): DrawingStroke {
        if (stroke.points.any { !it.x.isFinite() || !it.y.isFinite() }) return stroke
        var points: List<DrawingPoint> = stroke.points.fold(mutableListOf<DrawingPoint>()) { kept, point ->
            if (kept.isEmpty() || hypot(point.x - kept.last().x, point.y - kept.last().y) > 0.25f) kept.add(point)
            kept
        }
        if (points.size < 2) return stroke
        if (stroke.shapeType != ShapeType.NONE) {
            val s = points.first()
            val e = points.last()
            val minX = points.minOf { it.x }
            val maxX = points.maxOf { it.x }
            val minY = points.minOf { it.y }
            val maxY = points.maxOf { it.y }
            val cx = (minX + maxX) / 2f
            val cy = (minY + maxY) / 2f
            when (stroke.shapeType) {
                ShapeType.LINE -> return stroke.copy(points = listOf(DrawingPoint(s.x, s.y), DrawingPoint(e.x, e.y)), isShape = true, shapeType = ShapeType.LINE)
                ShapeType.ARROW -> {
                    val length = hypot(e.x - s.x, e.y - s.y)
                    if (length < 2f) return stroke
                    val angle = atan2(e.y - s.y, e.x - s.x)
                    val head = min(32f, length * 0.28f)
                    return stroke.copy(points = listOf(s, e,
                        DrawingPoint(e.x - head * cos(angle - 0.48f), e.y - head * sin(angle - 0.48f)), e,
                        DrawingPoint(e.x - head * cos(angle + 0.48f), e.y - head * sin(angle + 0.48f))), isShape = true, shapeType = ShapeType.ARROW)
                }
                ShapeType.RECTANGLE -> return stroke.copy(points = listOf(DrawingPoint(minX, minY), DrawingPoint(maxX, minY), DrawingPoint(maxX, maxY), DrawingPoint(minX, maxY), DrawingPoint(minX, minY)), isShape = true, shapeType = ShapeType.RECTANGLE)
                ShapeType.TRIANGLE -> return stroke.copy(points = listOf(DrawingPoint(cx, minY), DrawingPoint(maxX, maxY), DrawingPoint(minX, maxY), DrawingPoint(cx, minY)), isShape = true, shapeType = ShapeType.TRIANGLE)
                ShapeType.CIRCLE -> {
                    val r = max(maxX - minX, maxY - minY) / 2f
                    val cPts = (0..48).map { idx ->
                        val ang = 2f * PI.toFloat() * idx / 48
                        DrawingPoint(cx + r * cos(ang), cy + r * sin(ang), 1f)
                    }
                    return stroke.copy(points = cPts, isShape = true, shapeType = ShapeType.CIRCLE)
                }
                ShapeType.ELLIPSE -> {
                    val a = (maxX - minX) / 2f
                    val b = (maxY - minY) / 2f
                    val ePts = (0..48).map { idx ->
                        val ang = 2f * PI.toFloat() * idx / 48
                        DrawingPoint(cx + a * cos(ang), cy + b * sin(ang), 1f)
                    }
                    return stroke.copy(points = ePts, isShape = true, shapeType = ShapeType.ELLIPSE)
                }
                else -> {}
            }
        }
        if (points.size < 3) return stroke
        points = resampleByDistance(points)
        if (points.size < 6) return stroke

        val start = points.first()
        val end = points.last()

        var totalLength = 0f
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (i in points.indices) {
            val p = points[i]
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y

            if (i > 0) {
                totalLength += hypot(p.x - points[i - 1].x, p.y - points[i - 1].y)
            }
        }

        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        if (totalLength < 25f || (boxWidth < 10f && boxHeight < 10f)) return stroke

        val directDist = hypot(end.x - start.x, end.y - start.y)
        val maxBoxDim = max(boxWidth, boxHeight)
        val minBoxDim = min(boxWidth, boxHeight)

        // Determine if stroke forms a closed shape
        val isClosed = directDist <= max(6f, 0.20f * maxBoxDim)

        if (isClosed && points.size >= 10 && minBoxDim >= 14f) {
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f

            // Simplify polygon using Ramer-Douglas-Peucker to find prominent corners
            val epsilon = (maxBoxDim * 0.06f).coerceIn(8f, 25f)
            val simplified = rdpSimplify(points, epsilon)
            val corners = cleanPolygonCorners(simplified, directDistThreshold = epsilon * 0.8f)

            // Calculate distance to each of the 4 bounding box corners
            val dTL = points.minOf { hypot(it.x - minX, it.y - minY) }
            val dTR = points.minOf { hypot(it.x - maxX, it.y - minY) }
            val dBR = points.minOf { hypot(it.x - maxX, it.y - maxY) }
            val dBL = points.minOf { hypot(it.x - minX, it.y - maxY) }
            val maxCornerDist = maxOf(dTL, dTR, dBR, dBL)

            // 1. TRIANGLE CHECK (strictly 3 dominant vertices)
            if (corners.size == 3) {
                val anglesValid = areTriangleAngles(corners)
                val sidesLinear = arePolygonSidesLinear(points, corners)
                if (anglesValid && sidesLinear) {
                    val trianglePoints = listOf(
                        corners[0],
                        corners[1],
                        corners[2],
                        corners[0] // close loop
                    )
                    return stroke.copy(
                        points = trianglePoints,
                        isShape = true,
                        shapeType = ShapeType.TRIANGLE
                    )
                }
            }

            // 2. RECTANGLE CHECK (must be evaluated BEFORE circle to avoid falsely classifying squares as circles!)
            // Method A: 4 clear corner vertices with roughly 90-degree internal angles
            if (corners.size == 4) {
                val anglesValid = areRectangleAngles(corners)
                if (anglesValid && arePolygonSidesLinear(points, corners)) {
                    val snappedRect = orientedRectangle(points, corners)
                    return stroke.copy(
                        points = snappedRect,
                        isShape = true,
                        shapeType = ShapeType.RECTANGLE
                    )
                }
            }

            // Method B: Bounding box perimeter and corner coverage check (handles slightly curved/rounded strokes)
            val perimeterBox = 2f * (boxWidth + boxHeight)
            val perimeterRatio = totalLength / (perimeterBox + 0.001f)
            val avgDistToEdges = points.map { p ->
                minOf(
                    abs(p.x - minX),
                    abs(p.x - maxX),
                    abs(p.y - minY),
                    abs(p.y - maxY)
                )
            }.average().toFloat()

            // A rectangle has points reaching near all 4 corners (unlike a circle where distance is >= 0.414 * R)
            val cornersReached = maxCornerDist <= 0.28f * minBoxDim
            val edgesConform = avgDistToEdges <= 0.14f * minBoxDim
            if (cornersReached && edgesConform && perimeterRatio in 0.80f..1.28f) {
                val snappedRect = listOf(
                    DrawingPoint(minX, minY),
                    DrawingPoint(maxX, minY),
                    DrawingPoint(maxX, maxY),
                    DrawingPoint(minX, maxY),
                    DrawingPoint(minX, minY)
                )
                return stroke.copy(
                    points = snappedRect,
                    isShape = true,
                    shapeType = ShapeType.RECTANGLE
                )
            }

            // 3. CIRCLE CHECK (aspect ratio near 1:1, low radial variance, does NOT reach corners of bounding box)
            val aspectRatio = boxWidth / (boxHeight + 0.001f)
            val radii = points.map { hypot(it.x - centerX, it.y - centerY) }
            val meanRadius = radii.average().toFloat()
            val variance = radii.map { (it - meanRadius) * (it - meanRadius) }.average().toFloat()
            val stdDev = sqrt(variance)
            val cv = stdDev / (meanRadius + 0.001f) // coefficient of variation

            val angularCoverage = calculateAngularCoverage(points, centerX, centerY)

            // Circle requires:
            // - Aspect ratio between 0.80 and 1.25
            // - Low radial CV (< 0.13f)
            // - High angular sweep (> 320 degrees)
            // - Minimum distance from bounding box corners (at least 0.18 * minBoxDim, since circles stay away from corners)
            val minCornerDist = minOf(dTL, dTR, dBR, dBL)
            val avoidsCorners = minCornerDist >= 0.14f * minBoxDim

            if (aspectRatio in 0.80f..1.25f && cv < 0.18f && angularCoverage in 300f..420f && avoidsCorners) {
                val circlePoints = mutableListOf<DrawingPoint>()
                val numSamples = 48
                val radius = (boxWidth + boxHeight) / 4f
                for (s in 0..numSamples) {
                    val angle = 2f * PI.toFloat() * s / numSamples
                    val px = centerX + radius * cos(angle)
                    val py = centerY + radius * sin(angle)
                    circlePoints.add(DrawingPoint(px, py, 1f))
                }
                return stroke.copy(
                    points = circlePoints,
                    isShape = true,
                    shapeType = ShapeType.CIRCLE
                )
            }

            // Fit in the stroke's principal axes so tilted ellipses keep their rotation.
            if (angularCoverage in 300f..420f) {
                val ellipse = fitOrientedEllipse(points)
                if (ellipse != null) return stroke.copy(points = ellipse, isShape = true, shapeType = ShapeType.ELLIPSE)
            }

            // If closed but confidence is insufficient: PRESERVE FREEHAND!
            return stroke
        }

        // ==========================================
        // OPEN STROKE RECOGNITION: Line or Arrow
        // ==========================================
        val straightnessRatio = directDist / (totalLength + 0.001f)

        // 5. ARROW CHECK
        // Check if stroke forms a straight shaft followed by an arrowhead barb
        if (points.size >= 8) {
            // Use the first outward peak near the furthest point. An arrow often
            // revisits its tip while drawing the second barb; that revisit is not the shaft.
            val distances = points.map { hypot(it.x - start.x, it.y - start.y) }
            val maxDistFromStart = distances.maxOrNull() ?: 0f
            var tipIdx = distances.indexOfFirst { it >= maxDistFromStart * 0.98f }.coerceAtLeast(0)
            while (tipIdx < distances.lastIndex && distances[tipIdx + 1] > distances[tipIdx]) tipIdx++

            // Tip should be past the middle of the stroke (shaft first, then arrowhead)
            if (tipIdx >= (points.size * 0.50f).toInt() && tipIdx < points.size - 2 && maxDistFromStart >= 30f) {
                val tipPoint = points[tipIdx]
                val shaftLength = points.subList(0, tipIdx + 1).zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }.sum()
                val shaftDirect = hypot(tipPoint.x - start.x, tipPoint.y - start.y)
                val shaftStraightness = shaftDirect / (shaftLength + 0.001f)

                // Remaining points after tip form the arrowhead
                val remainingPoints = points.subList(tipIdx, points.size)
                val maxBarbDist = remainingPoints.maxOf { hypot(it.x - tipPoint.x, it.y - tipPoint.y) }

                // The barbs must stay near the tip (within 40% of shaft length)
                if (shaftStraightness >= 0.88f && maxBarbDist <= maxDistFromStart * 0.42f && maxBarbDist >= 10f) {
                    val angle = atan2(tipPoint.y - start.y, tipPoint.x - start.x)
                    val headLength = min(40f, max(16f, shaftDirect * 0.28f))

                    val head1X = tipPoint.x - headLength * cos(angle - 0.48f)
                    val head1Y = tipPoint.y - headLength * sin(angle - 0.48f)
                    val head2X = tipPoint.x - headLength * cos(angle + 0.48f)
                    val head2Y = tipPoint.y - headLength * sin(angle + 0.48f)

                    val arrowPoints = listOf(
                        DrawingPoint(start.x, start.y),
                        DrawingPoint(tipPoint.x, tipPoint.y),
                        DrawingPoint(head1X, head1Y),
                        DrawingPoint(tipPoint.x, tipPoint.y),
                        DrawingPoint(head2X, head2Y)
                    )
                    return stroke.copy(
                        points = arrowPoints,
                        isShape = true,
                        shapeType = ShapeType.ARROW
                    )
                }
            }
        }

        // 6. STRAIGHT LINE CHECK
        if (straightnessRatio >= 0.92f && totalLength >= 25f) {
            val maxPerpDev = points.maxOf { p -> perpendicularDistance(p, start, end) }
            if (maxPerpDev <= max(2.5f, 0.06f * directDist)) {
                val snappedLine = listOf(
                    DrawingPoint(start.x, start.y),
                    DrawingPoint(end.x, end.y)
                )
                return stroke.copy(
                    points = snappedLine,
                    isShape = true,
                    shapeType = ShapeType.LINE
                )
            }
        }

        // If confidence is insufficient, preserve original freehand stroke!
        return stroke
    }

    /** Equal arc-length sampling prevents slow/dense parts of a stroke from dominating a fit. */
    private fun resampleByDistance(points: List<DrawingPoint>): List<DrawingPoint> {
        val lengths = points.zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }
        val total = lengths.sum()
        if (total < 1f) return points
        val count = (total / 4f).toInt().coerceIn(24, 160)
        val result = mutableListOf(points.first())
        var segment = 0
        var before = 0f
        for (i in 1 until count) {
            val target = total * i / count
            while (segment < lengths.lastIndex && before + lengths[segment] < target) {
                before += lengths[segment]; segment++
            }
            val t = ((target - before) / lengths[segment].coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
            val a = points[segment]; val b = points[segment + 1]
            result.add(DrawingPoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.pressure + (b.pressure - a.pressure) * t))
        }
        result.add(points.last())
        return result
    }

    private fun orientedRectangle(points: List<DrawingPoint>, corners: List<DrawingPoint>): List<DrawingPoint> {
        val a = corners[0]; val b = corners[1]
        val angle = atan2(b.y - a.y, b.x - a.x)
        val ux = cos(angle); val uy = sin(angle)
        val vx = -uy; val vy = ux
        val us = points.map { it.x * ux + it.y * uy }
        val vs = points.map { it.x * vx + it.y * vy }
        val minU = us.minOrNull()!!; val maxU = us.maxOrNull()!!
        val minV = vs.minOrNull()!!; val maxV = vs.maxOrNull()!!
        fun p(u: Float, v: Float) = DrawingPoint(u * ux + v * vx, u * uy + v * vy)
        return listOf(p(minU, minV), p(maxU, minV), p(maxU, maxV), p(minU, maxV), p(minU, minV))
    }

    private fun fitOrientedEllipse(points: List<DrawingPoint>): List<DrawingPoint>? {
        val mx = points.map { it.x }.average().toFloat()
        val my = points.map { it.y }.average().toFloat()
        val xx = points.sumOf { ((it.x - mx) * (it.x - mx)).toDouble() }
        val yy = points.sumOf { ((it.y - my) * (it.y - my)).toDouble() }
        val xy = points.sumOf { ((it.x - mx) * (it.y - my)).toDouble() }
        val angle = (0.5 * atan2(2.0 * xy, xx - yy)).toFloat()
        val ux = cos(angle); val uy = sin(angle)
        val us = points.map { (it.x - mx) * ux + (it.y - my) * uy }
        val vs = points.map { -(it.x - mx) * uy + (it.y - my) * ux }
        val cu = (us.minOrNull()!! + us.maxOrNull()!!) / 2f
        val cv = (vs.minOrNull()!! + vs.maxOrNull()!!) / 2f
        val a = (us.maxOrNull()!! - us.minOrNull()!!) / 2f
        val b = (vs.maxOrNull()!! - vs.minOrNull()!!) / 2f
        if (a < 6f || b < 6f) return null
        val errors = us.indices.map { abs(hypot((us[it] - cu) / a, (vs[it] - cv) / b) - 1f) }
        if (errors.average() > 0.12 || errors.count { it > 0.25f } > points.size / 10) return null
        return (0..64).map { i ->
            val t = 2f * PI.toFloat() * i / 64
            val u = cu + a * cos(t); val v = cv + b * sin(t)
            DrawingPoint(mx + u * ux - v * uy, my + u * uy + v * ux)
        }
    }

    // Douglas-Peucker line simplification algorithm
    private fun rdpSimplify(points: List<DrawingPoint>, epsilon: Float): List<DrawingPoint> {
        if (points.size < 3) return points

        var maxDist = 0f
        var maxIndex = 0
        val start = points.first()
        val end = points.last()

        for (i in 1 until points.size - 1) {
            val d = perpendicularDistance(points[i], start, end)
            if (d > maxDist) {
                maxDist = d
                maxIndex = i
            }
        }

        return if (maxDist > epsilon) {
            val left = rdpSimplify(points.subList(0, maxIndex + 1), epsilon)
            val right = rdpSimplify(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    private fun perpendicularDistance(p: DrawingPoint, start: DrawingPoint, end: DrawingPoint): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val mag = hypot(dx, dy)
        if (mag == 0f) return hypot(p.x - start.x, p.y - start.y)
        return abs(dy * p.x - dx * p.y + end.x * start.y - end.y * start.x) / mag
    }

    private fun cleanPolygonCorners(points: List<DrawingPoint>, directDistThreshold: Float): List<DrawingPoint> {
        val cleaned = mutableListOf<DrawingPoint>()
        for (pt in points) {
            if (cleaned.isEmpty() || hypot(cleaned.last().x - pt.x, cleaned.last().y - pt.y) > directDistThreshold) {
                cleaned.add(pt)
            }
        }
        // If last point is close to first point, drop the duplicate
        if (cleaned.size > 2 && hypot(cleaned.first().x - cleaned.last().x, cleaned.first().y - cleaned.last().y) <= directDistThreshold) {
            cleaned.removeAt(cleaned.size - 1)
        }
        var changed = true
        while (changed && cleaned.size > 3) {
            changed = false
            for (i in cleaned.indices) {
                val before = cleaned[(i + cleaned.size - 1) % cleaned.size]
                val after = cleaned[(i + 1) % cleaned.size]
                if (angleBetween(before, cleaned[i], after) > 155f) {
                    cleaned.removeAt(i); changed = true; break
                }
            }
        }
        return cleaned
    }

    private fun areTriangleAngles(corners: List<DrawingPoint>): Boolean {
        if (corners.size != 3) return false
        var sum = 0f
        for (i in 0 until 3) {
            val p0 = corners[(i + 2) % 3]
            val p1 = corners[i]
            val p2 = corners[(i + 1) % 3]
            val angle = angleBetween(p0, p1, p2)
            if (angle !in 20f..140f) return false
            sum += angle
        }
        return sum in 140f..225f
    }

    private fun arePolygonSidesLinear(points: List<DrawingPoint>, corners: List<DrawingPoint>): Boolean {
        if (corners.size < 3) return false
        val n = corners.size
        // Find indices of corners in original points list
        val indices = corners.map { c ->
            points.indexOfFirst { hypot(it.x - c.x, it.y - c.y) < 1f }.takeIf { it >= 0 } ?: 0
        }.sorted()

        for (i in 0 until n) {
            val i1 = indices[i]
            val i2 = indices[(i + 1) % n]
            val subPoints = if (i2 > i1) {
                points.subList(i1, i2 + 1)
            } else {
                points.subList(i1, points.size) + points.subList(0, i2 + 1)
            }
            if (subPoints.size > 2) {
                val start = subPoints.first()
                val end = subPoints.last()
                val maxDev = subPoints.maxOf { perpendicularDistance(it, start, end) }
                val sideLen = hypot(end.x - start.x, end.y - start.y)
                if (sideLen > 15f && maxDev > sideLen * 0.25f) {
                    return false
                }
            }
        }
        return true
    }

    private fun areRectangleAngles(corners: List<DrawingPoint>): Boolean {
        if (corners.size != 4) return false
        for (i in 0 until 4) {
            val p0 = corners[(i + 3) % 4]
            val p1 = corners[i]
            val p2 = corners[(i + 1) % 4]
            val angle = angleBetween(p0, p1, p2)
            // Expect angles between 65 and 115 degrees
            if (angle !in 65f..115f) return false
        }
        return true
    }

    private fun angleBetween(p1: DrawingPoint, vertex: DrawingPoint, p2: DrawingPoint): Float {
        val a1 = atan2(p1.y - vertex.y, p1.x - vertex.x)
        val a2 = atan2(p2.y - vertex.y, p2.x - vertex.x)
        var diff = abs(Math.toDegrees((a2 - a1).toDouble()).toFloat())
        if (diff > 180f) diff = 360f - diff
        return diff
    }

    private fun calculateAngularCoverage(points: List<DrawingPoint>, cx: Float, cy: Float): Float {
        if (points.size < 4) return 0f
        var totalAngleChange = 0f
        var prevAngle = atan2(points[0].y - cy, points[0].x - cx)

        for (i in 1 until points.size) {
            val curAngle = atan2(points[i].y - cy, points[i].x - cx)
            var delta = curAngle - prevAngle
            while (delta > PI.toFloat()) delta -= 2f * PI.toFloat()
            while (delta < -PI.toFloat()) delta += 2f * PI.toFloat()
            totalAngleChange += delta
            prevAngle = curAngle
        }
        return abs(Math.toDegrees(totalAngleChange.toDouble()).toFloat())
    }
}

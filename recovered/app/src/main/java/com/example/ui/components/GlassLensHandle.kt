package com.example.ui.components

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.PageData
import com.example.model.PaperStyle
import com.example.model.ToolType
import kotlin.math.abs

/**
 * Theme-independent collapsed edge handle. It redraws the page under the handle
 * at a slight magnification so it behaves visually like a clear water droplet.
 * The optical rim and chevron use exactly the same styling on every paper theme.
 */
@Composable
fun GlassLensHandle(
    pageData: PageData,
    paperStyle: PaperStyle,
    panOffset: DrawingPoint,
    zoomScale: Float,
    liveStroke: DrawingStroke?,
    sourceCenterXPx: Float,
    sourceCenterYPx: Float,
    dockOnRight: Boolean,
    onOpen: () -> Unit,
    onDockSideChange: (Boolean) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    val glassShape = if (dockOnRight) {
        RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 5.dp, bottomEnd = 5.dp)
    } else {
        RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp, topStart = 5.dp, bottomStart = 5.dp)
    }
    val rimColor = Color(0xFF7C8AA5).copy(alpha = 0.52f)
    val causticColor = Color(0xFF94A3B8).copy(alpha = 0.34f)
    val chevronColor = Color(0xFF74839B).copy(alpha = 0.88f)

    Box(
        modifier = modifier
            .width(34.dp)
            .height(112.dp)
            .clip(glassShape)
            .pointerInput(dockOnRight) {
                detectDragGestures(
                    onDragStart = { horizontalDrag = 0f },
                    onDragEnd = {
                        if (abs(horizontalDrag) >= 44f) {
                            onDockSideChange(horizontalDrag > 0f)
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = { horizontalDrag = 0f }
                ) { _, dragAmount ->
                    horizontalDrag += dragAmount.x
                    onVerticalDrag(dragAmount.y)
                }
            }
            .clickable(onClick = onOpen)
            .border(width = 1.dp, color = rimColor, shape = glassShape)
            .testTag("vertical_toolbar_collapsed_handle"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawMagnifiedPageLens(
                pageData = pageData,
                paperStyle = paperStyle,
                panOffset = panOffset,
                zoomScale = zoomScale,
                liveStroke = liveStroke,
                sourceCenterXPx = sourceCenterXPx,
                sourceCenterYPx = sourceCenterYPx,
                magnification = 1.42f
            )

            // One small fixed caustic line only. No dark/light-theme branch and no
            // second outline, so AMOLED gets the same glass treatment as ruled paper.
            drawLine(
                color = causticColor,
                start = Offset(size.width * 0.28f, size.height * 0.10f),
                end = Offset(size.width * 0.28f, size.height * 0.34f),
                strokeWidth = 1.15f,
                cap = StrokeCap.Round
            )
        }

        Icon(
            imageVector = if (dockOnRight) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
            contentDescription = "Open Drawing Toolbar",
            tint = chevronColor,
            modifier = Modifier.testTag("open_vertical_toolbar_button")
        )
    }
}

private fun DrawScope.drawMagnifiedPageLens(
    pageData: PageData,
    paperStyle: PaperStyle,
    panOffset: DrawingPoint,
    zoomScale: Float,
    liveStroke: DrawingStroke?,
    sourceCenterXPx: Float,
    sourceCenterYPx: Float,
    magnification: Float
) {
    val z = zoomScale.coerceAtLeast(0.01f)
    val center = Offset(size.width / 2f, size.height / 2f)

    fun toLens(screenX: Float, screenY: Float): Offset = Offset(
        center.x + (screenX - sourceCenterXPx) * magnification,
        center.y + (screenY - sourceCenterYPx) * magnification
    )

    fun horizontalPattern(spacing: Float, color: Color, width: Float) {
        if (spacing <= 0f) return
        val start = ((panOffset.y % spacing) + spacing) % spacing
        val minY = sourceCenterYPx - size.height / (2f * magnification) - spacing
        val maxY = sourceCenterYPx + size.height / (2f * magnification) + spacing
        var y = start
        while (y < minY) y += spacing
        while (y <= maxY) {
            val ly = toLens(sourceCenterXPx, y).y
            drawLine(color, Offset(0f, ly), Offset(size.width, ly), width * magnification)
            y += spacing
        }
    }

    fun verticalPattern(spacing: Float, color: Color, width: Float) {
        if (spacing <= 0f) return
        val start = ((panOffset.x % spacing) + spacing) % spacing
        val minX = sourceCenterXPx - size.width / (2f * magnification) - spacing
        val maxX = sourceCenterXPx + size.width / (2f * magnification) + spacing
        var x = start
        while (x < minX) x += spacing
        while (x <= maxX) {
            val lx = toLens(x, sourceCenterYPx).x
            drawLine(color, Offset(lx, 0f), Offset(lx, size.height), width * magnification)
            x += spacing
        }
    }

    when (paperStyle) {
        PaperStyle.PLAIN_WHITE -> drawRect(Color.White)
        PaperStyle.RULED -> {
            drawRect(Color(0xFFFEFDF9))
            horizontalPattern(36f * z, Color(0xFFD4E0ED), 1f)
            val marginScreenX = 72f * z + panOffset.x
            val localX = toLens(marginScreenX, sourceCenterYPx).x
            if (localX in -3f..(size.width + 3f)) {
                drawLine(Color(0xFFFCA5A5), Offset(localX, 0f), Offset(localX, size.height), 1.8f * magnification)
            }
        }
        PaperStyle.GRAPH -> {
            drawRect(Color(0xFFF8FAFC))
            val spacing = 28f * z
            horizontalPattern(spacing, Color(0xFFE2E8F0), 0.8f)
            verticalPattern(spacing, Color(0xFFE2E8F0), 0.8f)
        }
        PaperStyle.BLACKBOARD -> drawRect(Color(0xFF142019))
        PaperStyle.DARK_MODE -> {
            drawRect(Color(0xFF0F172A))
            horizontalPattern(36f * z, Color(0xFF1E293B), 0.8f)
        }
        PaperStyle.SEPIA -> {
            drawRect(Color(0xFFFBF5E8))
            horizontalPattern(36f * z, Color(0xFFE8DCC8), 1f)
        }
        PaperStyle.AMOLED_BLACK -> drawRect(Color.Black)
    }

    fun drawLensStroke(stroke: DrawingStroke) {
        val pts = stroke.points
        if (pts.isEmpty()) return
        val mapped = pts.map { p ->
            toLens(p.x * z + panOffset.x, p.y * z + panOffset.y)
        }
        val alpha = if (stroke.toolType == ToolType.HIGHLIGHTER) stroke.alpha else 1f
        val color = Color(stroke.color).copy(alpha = alpha)
        val width = (stroke.width * z * magnification).coerceAtLeast(0.8f)

        if (mapped.size == 1) {
            drawCircle(color, radius = (width / 2f).coerceAtLeast(1.2f), center = mapped[0])
            return
        }

        val path = Path().apply {
            moveTo(mapped[0].x, mapped[0].y)
            if (stroke.isShape) {
                for (i in 1 until mapped.size) lineTo(mapped[i].x, mapped[i].y)
            } else {
                for (i in 1 until mapped.size) {
                    val prev = mapped[i - 1]
                    val cur = mapped[i]
                    quadraticTo(prev.x, prev.y, (prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
                }
                lineTo(mapped.last().x, mapped.last().y)
            }
        }
        drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    pageData.strokes.forEach(::drawLensStroke)
    liveStroke?.let(::drawLensStroke)

    val native = drawContext.canvas.nativeCanvas
    pageData.textAnnotations.forEach { text ->
        val p = toLens(text.x * z + panOffset.x, text.y * z + panOffset.y)
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = text.color.toInt()
            textSize = text.fontSize * z * magnification
            isFakeBoldText = true
        }
        native.save()
        native.translate(p.x, p.y)
        if (text.rotation != 0f) native.rotate(text.rotation)
        native.drawText(text.text, 0f, 0f, paint)
        native.restore()
    }
}

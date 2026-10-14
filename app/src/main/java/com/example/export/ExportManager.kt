package com.example.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.model.AnswerDocument
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.PageData
import com.example.model.PaperStyle
import com.example.model.ToolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object ExportManager {

    const val DEFAULT_NEET_PROMPT =
        "Please check my handwritten NEET answer carefully. First read the question and my complete solution from the attached image. Identify whether my final answer is correct. If incorrect, show exactly where my reasoning or calculation went wrong. Explain the concept step by step, provide the relevant formula, important NEET points, and common exam traps. Do not skip any handwritten step. If any part of the image is unclear, ask me to clarify instead of guessing."

    sealed class ShareResult {
        object SentDirectToChatGpt : ShareResult()
        object OpenedShareSheet : ShareResult()
        data class SavedLocally(val paths: List<String>) : ShareResult()
        data class Error(val message: String) : ShareResult()
    }

    /**
     * Renders a single page to a high-resolution Bitmap preserving all strokes.
     */
    /**
     * Renders a single page to a high-resolution Bitmap preserving all strokes without clipping,
     * correctly calculating content bounds and translating negative/infinite coordinates.
     */
    fun renderPageToBitmap(
        page: PageData,
        document: AnswerDocument,
        totalPages: Int,
        scale: Float = 2.0f,
        forceWhiteBackground: Boolean = false,
        minWidth: Int = 1080,
        minHeight: Int = 1920
    ): Bitmap {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        val hasStrokes = page.strokes.isNotEmpty()
        val hasTexts = page.textAnnotations.isNotEmpty()

        if (!hasStrokes && !hasTexts) {
            minX = 0f
            minY = 0f
            maxX = minWidth.toFloat()
            maxY = minHeight.toFloat()
        } else {
            for (stroke in page.strokes) {
                val halfW = stroke.width / 2f
                for (p in stroke.points) {
                    if (p.x - halfW < minX) minX = p.x - halfW
                    if (p.x + halfW > maxX) maxX = p.x + halfW
                    if (p.y - halfW < minY) minY = p.y - halfW
                    if (p.y + halfW > maxY) maxY = p.y + halfW
                }
            }
            for (t in page.textAnnotations) {
                val textW = max(t.text.length * t.fontSize * 0.8f, 40f)
                val textH = t.fontSize * 1.5f
                if (t.x < minX) minX = t.x
                if (t.x + textW > maxX) maxX = t.x + textW
                if (t.y - textH < minY) minY = t.y - textH
                if (t.y + 10f > maxY) maxY = t.y + 10f
            }
        }

        // Add generous margins around detected handwriting so it never touches the image edge
        val padding = 70f
        val bannerHeight = 80f

        val contentWidth = if (!hasStrokes && !hasTexts) {
            minWidth.toFloat()
        } else {
            max(minWidth.toFloat(), (maxX - minX) + padding * 2)
        }
        val totalCanvasHeight = if (!hasStrokes && !hasTexts) {
            minHeight.toFloat()
        } else {
            max(minHeight.toFloat(), (maxY - minY) + padding * 2 + bannerHeight)
        }

        // Memory safety guard: cap bitmap resolution to safe maximums (avoid OutOfMemoryError)
        var effectiveScale = scale
        var targetWidth = (contentWidth * effectiveScale).toInt()
        var targetHeight = (totalCanvasHeight * effectiveScale).toInt()

        val maxDimension = 2560
        if (targetWidth > maxDimension || targetHeight > maxDimension) {
            val ratio = minOf(maxDimension.toFloat() / targetWidth, maxDimension.toFloat() / targetHeight)
            effectiveScale *= ratio
            targetWidth = (contentWidth * effectiveScale).toInt()
            targetHeight = (totalCanvasHeight * effectiveScale).toInt()
        }

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Paper Background across the entire image
        drawPaperBackground(
            canvas = canvas,
            width = targetWidth.toFloat(),
            height = targetHeight.toFloat(),
            paperStyle = if (forceWhiteBackground) PaperStyle.PLAIN_WHITE else document.paperStyle,
            scale = effectiveScale
        )

        // 2. Draw Document Header banner at the top
        drawHeader(
            canvas = canvas,
            width = targetWidth.toFloat(),
            document = document,
            pageIndex = page.pageIndex,
            totalPages = totalPages,
            scale = effectiveScale,
            isDark = !forceWhiteBackground && (document.paperStyle == PaperStyle.BLACKBOARD || document.paperStyle == PaperStyle.DARK_MODE || document.paperStyle == PaperStyle.AMOLED_BLACK)
        )

        // 3. Shift canvas coordinate system to place (minX, minY) at (padding, bannerHeight + padding)
        val tx = (padding - minX) * effectiveScale
        val ty = (bannerHeight + padding - minY) * effectiveScale

        canvas.save()
        canvas.translate(tx, ty)
        canvas.scale(effectiveScale, effectiveScale)

        // 4. Draw Strokes
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in page.strokes) {
            if (stroke.points.isEmpty()) continue

            val strokeColor = if (forceWhiteBackground && (document.paperStyle == PaperStyle.BLACKBOARD || document.paperStyle == PaperStyle.DARK_MODE || document.paperStyle == PaperStyle.AMOLED_BLACK)) {
                if (stroke.color == 0xFFFFFFFF || stroke.color == 0xFFF1F5F9) 0xFF0F172A else stroke.color
            } else {
                stroke.color
            }

            strokePaint.color = strokeColor.toInt()
            strokePaint.strokeWidth = stroke.width

            if (stroke.points.size == 1) {
                val p = stroke.points[0]
                val origStyle = strokePaint.style
                strokePaint.style = Paint.Style.FILL
                canvas.drawCircle(p.x, p.y, (stroke.width / 2f).coerceAtLeast(1.5f), strokePaint)
                strokePaint.style = origStyle
                continue
            }

            if (stroke.toolType == ToolType.HIGHLIGHTER) {
                strokePaint.alpha = (255 * (stroke.alpha.coerceIn(0.2f, 0.6f))).toInt()
            } else {
                strokePaint.alpha = 255
            }

            val path = Path()
            val pts = stroke.points
            path.moveTo(pts[0].x, pts[0].y)

            if (stroke.isShape) {
                for (i in 1 until pts.size) {
                    path.lineTo(pts[i].x, pts[i].y)
                }
            } else {
                for (i in 1 until pts.size) {
                    val pPrev = pts[i - 1]
                    val pCurr = pts[i]
                    val midX = (pPrev.x + pCurr.x) / 2f
                    val midY = (pPrev.y + pCurr.y) / 2f
                    path.quadTo(pPrev.x, pPrev.y, midX, midY)
                }
                path.lineTo(pts.last().x, pts.last().y)
            }

            canvas.drawPath(path, strokePaint)
        }

        // 5. Draw Text Annotations & NEET Symbols (with Rotation)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (t in page.textAnnotations) {
            textPaint.textSize = t.fontSize
            textPaint.color = t.color.toInt()
            textPaint.isFakeBoldText = true

            canvas.save()
            canvas.translate(t.x, t.y)
            if (t.rotation != 0f) {
                canvas.rotate(t.rotation)
            }
            canvas.drawText(t.text, 0f, 0f, textPaint)
            canvas.restore()
        }

        canvas.restore()
        return bitmap
    }

    /**
     * Renders exactly what is currently visible in the user's viewport on the infinite canvas.
     */
    fun renderCurrentViewToBitmap(
        page: PageData,
        document: AnswerDocument,
        zoomScale: Float,
        panOffset: DrawingPoint,
        viewportWidth: Int,
        viewportHeight: Int,
        scale: Float = 1.5f,
        forceWhiteBackground: Boolean = false
    ): Bitmap {
        val targetWidth = (viewportWidth * scale).toInt().coerceAtLeast(300)
        val targetHeight = (viewportHeight * scale).toInt().coerceAtLeast(300)

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawPaperBackground(
            canvas = canvas,
            width = targetWidth.toFloat(),
            height = targetHeight.toFloat(),
            paperStyle = if (forceWhiteBackground) PaperStyle.PLAIN_WHITE else document.paperStyle,
            scale = scale
        )

        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(panOffset.x, panOffset.y)
        canvas.scale(zoomScale, zoomScale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in page.strokes) {
            if (stroke.points.isEmpty()) continue
            val strokeColor = if (forceWhiteBackground && (document.paperStyle == PaperStyle.BLACKBOARD || document.paperStyle == PaperStyle.DARK_MODE || document.paperStyle == PaperStyle.AMOLED_BLACK)) {
                if (stroke.color == 0xFFFFFFFF || stroke.color == 0xFFF1F5F9) 0xFF0F172A else stroke.color
            } else stroke.color

            strokePaint.color = strokeColor.toInt()
            strokePaint.strokeWidth = stroke.width
            strokePaint.alpha = if (stroke.toolType == ToolType.HIGHLIGHTER) (255 * stroke.alpha.coerceIn(0.2f, 0.6f)).toInt() else 255

            if (stroke.points.size == 1) {
                val p = stroke.points[0]
                val origStyle = strokePaint.style
                strokePaint.style = Paint.Style.FILL
                canvas.drawCircle(p.x, p.y, (stroke.width / 2f).coerceAtLeast(1.5f), strokePaint)
                strokePaint.style = origStyle
                continue
            }

            val path = Path()
            val pts = stroke.points
            path.moveTo(pts[0].x, pts[0].y)
            if (stroke.isShape) {
                for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
            } else {
                for (i in 1 until pts.size) {
                    val pPrev = pts[i - 1]
                    val pCurr = pts[i]
                    path.quadTo(pPrev.x, pPrev.y, (pPrev.x + pCurr.x) / 2f, (pPrev.y + pCurr.y) / 2f)
                }
                path.lineTo(pts.last().x, pts.last().y)
            }
            canvas.drawPath(path, strokePaint)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }
        for (t in page.textAnnotations) {
            textPaint.textSize = t.fontSize
            textPaint.color = t.color.toInt()
            canvas.save()
            canvas.translate(t.x, t.y)
            if (t.rotation != 0f) canvas.rotate(t.rotation)
            canvas.drawText(t.text, 0f, 0f, textPaint)
            canvas.restore()
        }

        canvas.restore()
        return bitmap
    }

    private fun drawPaperBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        paperStyle: PaperStyle,
        scale: Float
    ) {
        val bgPaint = Paint()
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

        when (paperStyle) {
            PaperStyle.PLAIN_WHITE -> {
                bgPaint.color = Color.WHITE
                canvas.drawRect(0f, 0f, width, height, bgPaint)
            }
            PaperStyle.RULED -> {
                bgPaint.color = Color.parseColor("#FEFDF9") // warm paper
                canvas.drawRect(0f, 0f, width, height, bgPaint)

                // Notebook ruled horizontal lines
                val lineSpacing = 36f * scale
                linePaint.color = Color.parseColor("#D4E0ED")
                linePaint.strokeWidth = 1.2f * scale
                var y = 140f * scale // start below header
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, linePaint)
                    y += lineSpacing
                }

                // Vertical red margin line
                val marginX = 72f * scale
                val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FCA5A5")
                    strokeWidth = 2f * scale
                }
                canvas.drawLine(marginX, 0f, marginX, height, marginPaint)
            }
            PaperStyle.GRAPH -> {
                bgPaint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(0f, 0f, width, height, bgPaint)

                // Coordinate grid lines
                val gridSpacing = 28f * scale
                linePaint.color = Color.parseColor("#E2E8F0")
                linePaint.strokeWidth = 1f * scale
                var x = 0f
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height, linePaint)
                    x += gridSpacing
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, linePaint)
                    y += gridSpacing
                }
            }
            PaperStyle.BLACKBOARD -> {
                bgPaint.color = Color.parseColor("#142019") // slate chalkboard green
                canvas.drawRect(0f, 0f, width, height, bgPaint)
            }
            PaperStyle.DARK_MODE -> {
                bgPaint.color = Color.parseColor("#0F172A") // slate 900
                canvas.drawRect(0f, 0f, width, height, bgPaint)

                // Subtle dark grid
                val gridSpacing = 32f * scale
                linePaint.color = Color.parseColor("#1E293B")
                linePaint.strokeWidth = 1f * scale
                var y = 140f * scale
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, linePaint)
                    y += gridSpacing
                }
            }
            PaperStyle.SEPIA -> {
                bgPaint.color = Color.parseColor("#FBF5E8") // parchment
                canvas.drawRect(0f, 0f, width, height, bgPaint)

                val lineSpacing = 36f * scale
                linePaint.color = Color.parseColor("#E6D8C3")
                linePaint.strokeWidth = 1.2f * scale
                var y = 140f * scale
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, linePaint)
                    y += lineSpacing
                }
            }
            PaperStyle.AMOLED_BLACK -> {
                bgPaint.color = Color.parseColor("#000000") // Pure AMOLED pitch black: strictly no ruling or grid
                canvas.drawRect(0f, 0f, width, height, bgPaint)
            }
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        width: Float,
        document: AnswerDocument,
        pageIndex: Int,
        totalPages: Int,
        scale: Float,
        isDark: Boolean
    ) {
        val bannerHeight = 80f * scale
        val isAmoled = document.paperStyle == PaperStyle.AMOLED_BLACK
        val headerBgPaint = Paint().apply {
            color = when {
                isAmoled -> Color.parseColor("#000000")
                isDark -> Color.parseColor("#1E293B")
                else -> Color.parseColor("#F1F5F9")
            }
        }
        canvas.drawRect(0f, 0f, width, bannerHeight, headerBgPaint)

        // Bottom border line
        val borderPaint = Paint().apply {
            color = when {
                isAmoled -> Color.parseColor("#18181B")
                isDark -> Color.parseColor("#334155")
                else -> Color.parseColor("#CBD5E1")
            }
            strokeWidth = 2f * scale
        }
        canvas.drawLine(0f, bannerHeight, width, bannerHeight, borderPaint)

        // Left text: Subject & Question #
        val subjectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = document.subject.colorHex.toInt()
            textSize = 22f * scale
            isFakeBoldText = true
        }
        val subText = "[${document.subject.displayName}] ${document.questionNumber}: ${document.title}"
        canvas.drawText(subText, 24f * scale, 48f * scale, subjectPaint)

        // Right text: Page X of Y & InkKey branding
        val pageText = "Page ${pageIndex + 1} of $totalPages • InkKey NEET"
        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isAmoled -> Color.parseColor("#A1A1AA")
                isDark -> Color.parseColor("#94A3B8")
                else -> Color.parseColor("#64748B")
            }
            textSize = 18f * scale
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(pageText, width - 24f * scale, 48f * scale, pagePaint)
    }

    /**
     * Unified representation of an exported file (works for both standard File and SAF DocumentFile).
     */
    data class ExportItem(
        val name: String,
        val sizeBytes: Long,
        val lastModified: Long,
        val uri: Uri,
        val isPdf: Boolean,
        val file: File? = null
    )

    private const val PREFS_STORAGE = "inkkey_export_storage_prefs"
    private const val KEY_CUSTOM_TREE_URI = "custom_export_tree_uri"

    /**
     * Returns the user-selected custom SAF folder URI, verifying that write permission is still active.
     */
    fun getCustomExportTreeUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_STORAGE, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_CUSTOM_TREE_URI, null) ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            val hasPermission = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isWritePermission
            }
            if (hasPermission) uri else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Persists the user-selected folder from ACTION_OPEN_DOCUMENT_TREE.
     */
    fun setCustomExportLocation(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(PREFS_STORAGE, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_TREE_URI, uri.toString()).apply()
    }

    /**
     * Restores the export location to the default directory.
     */
    fun resetToDefaultExportLocation(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_STORAGE, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM_TREE_URI).apply()
    }

    fun isUsingCustomLocation(context: Context): Boolean {
        return getCustomExportTreeUri(context) != null
    }

    /**
     * Standard persistent directory in external files (Documents/InkKey_Exports).
     * This is directly browseable by file managers on Android.
     */
    fun getDefaultExportDirectory(context: Context): File {
        val extDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val dir = if (extDocs != null) {
            File(extDocs, "InkKey_Exports")
        } else {
            File(context.filesDir, "InkKey_Exports")
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExportDirectory(context: Context): File {
        return getDefaultExportDirectory(context)
    }

    fun getExportDirectoryDisplayPath(context: Context): String {
        val customUri = getCustomExportTreeUri(context)
        return if (customUri != null) {
            try {
                val doc = DocumentFile.fromTreeUri(context, customUri)
                val folderName = doc?.name ?: customUri.lastPathSegment ?: "Selected Folder"
                "Custom Folder: $folderName"
            } catch (e: Exception) {
                "Custom Folder: ${customUri.lastPathSegment ?: "Selected Folder"}"
            }
        } else {
            "Default: Device Storage > Documents > InkKey_Exports"
        }
    }

    fun getExportDirectoryAbsolutePath(context: Context): String {
        val customUri = getCustomExportTreeUri(context)
        return if (customUri != null) {
            customUri.toString()
        } else {
            getDefaultExportDirectory(context).absolutePath
        }
    }

    /**
     * Returns all exported files from both the custom SAF folder and default directory.
     */
    fun getExportedFiles(context: Context): List<ExportItem> {
        val result = mutableListOf<ExportItem>()

        // 1. Scan custom SAF folder if configured
        val customUri = getCustomExportTreeUri(context)
        if (customUri != null) {
            try {
                val tree = DocumentFile.fromTreeUri(context, customUri)
                if (tree != null && tree.isDirectory) {
                    tree.listFiles().forEach { doc ->
                        val name = doc.name ?: ""
                        if (name.endsWith(".png", ignoreCase = true) || name.endsWith(".pdf", ignoreCase = true)) {
                            result.add(
                                ExportItem(
                                    name = name,
                                    sizeBytes = doc.length(),
                                    lastModified = doc.lastModified(),
                                    uri = doc.uri,
                                    isPdf = name.endsWith(".pdf", ignoreCase = true),
                                    file = null
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Scan default persistent directory
        val defaultDir = getDefaultExportDirectory(context)
        defaultDir.listFiles { f ->
            f.isFile && (f.name.endsWith(".png", ignoreCase = true) || f.name.endsWith(".pdf", ignoreCase = true))
        }?.forEach { f ->
            try {
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, f)
                result.add(
                    ExportItem(
                        name = f.name,
                        sizeBytes = f.length(),
                        lastModified = f.lastModified(),
                        uri = uri,
                        isPdf = f.name.endsWith(".pdf", ignoreCase = true),
                        file = f
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Scan legacy cache shared_exports if any remain
        val legacyDir = File(context.cacheDir, "shared_exports")
        if (legacyDir.exists()) {
            legacyDir.listFiles { f ->
                f.isFile && (f.name.endsWith(".png", ignoreCase = true) || f.name.endsWith(".pdf", ignoreCase = true))
            }?.forEach { f ->
                try {
                    val authority = "${context.packageName}.fileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, f)
                    result.add(
                        ExportItem(
                            name = f.name,
                            sizeBytes = f.length(),
                            lastModified = f.lastModified(),
                            uri = uri,
                            isPdf = f.name.endsWith(".pdf", ignoreCase = true),
                            file = f
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return result.distinctBy { it.name }.sortedByDescending { it.lastModified }
    }

    /**
     * Opens an exported item using the system view intent.
     */
    fun openExportedItem(context: Context, item: ExportItem): Boolean {
        return try {
            val mimeType = if (item.isPdf) "application/pdf" else "image/png"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open ${item.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openExportedFile(context: Context, file: File): Boolean {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val isPdf = file.name.endsWith(".pdf", ignoreCase = true)
        return openExportedItem(context, ExportItem(file.name, file.length(), file.lastModified(), uri, isPdf, file))
    }

    /**
     * Shares an exported item using the system share sheet.
     */
    fun shareExportedItem(context: Context, item: ExportItem): Boolean {
        return try {
            val mimeType = if (item.isPdf) "application/pdf" else "image/png"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share ${item.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareExportedFile(context: Context, file: File): Boolean {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val isPdf = file.name.endsWith(".pdf", ignoreCase = true)
        return shareExportedItem(context, ExportItem(file.name, file.length(), file.lastModified(), uri, isPdf, file))
    }

    /**
     * Permanently deletes all InkKey exported files from default and custom storage locations.
     */
    fun clearAllExportedFiles(context: Context): Int {
        var count = 0

        // 1. Delete default persistent exports
        val defaultDir = getDefaultExportDirectory(context)
        defaultDir.listFiles { f ->
            f.isFile && (f.name.endsWith(".png", ignoreCase = true) || f.name.endsWith(".pdf", ignoreCase = true))
        }?.forEach {
            if (it.delete()) count++
        }

        // 2. Delete legacy cache exports
        val legacyDir = File(context.cacheDir, "shared_exports")
        if (legacyDir.exists()) {
            legacyDir.listFiles { f ->
                f.isFile && (f.name.endsWith(".png", ignoreCase = true) || f.name.endsWith(".pdf", ignoreCase = true))
            }?.forEach {
                if (it.delete()) count++
            }
        }

        // 3. Delete files created in custom SAF tree (only files starting with "InkKey_" to protect user files!)
        val customUri = getCustomExportTreeUri(context)
        if (customUri != null) {
            try {
                val tree = DocumentFile.fromTreeUri(context, customUri)
                tree?.listFiles()?.forEach { doc ->
                    val name = doc.name ?: ""
                    if (name.startsWith("InkKey_") && (name.endsWith(".png", ignoreCase = true) || name.endsWith(".pdf", ignoreCase = true))) {
                        if (doc.delete()) count++
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return count
    }

    /**
     * Exports all pages of an AnswerDocument as high-resolution PNG images.
     */
    suspend fun exportAsPngImages(
        context: Context,
        document: AnswerDocument,
        scale: Float = 2.0f,
        forceWhiteBackground: Boolean = false
    ): List<File> = withContext(Dispatchers.IO) {
        val exportDir = getDefaultExportDirectory(context)
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedTitle = document.title.replace("\\s+".toRegex(), "_").take(20)

        val files = mutableListOf<File>()
        val totalPages = document.pages.size
        val customUri = getCustomExportTreeUri(context)
        val customTree = if (customUri != null) DocumentFile.fromTreeUri(context, customUri) else null

        for (page in document.pages) {
            val bitmap = renderPageToBitmap(
                page = page,
                document = document,
                totalPages = totalPages,
                scale = scale,
                forceWhiteBackground = forceWhiteBackground
            )

            val fileName = if (totalPages == 1) {
                "InkKey_${document.questionNumber}_${sanitizedTitle}_$dateStr.png"
            } else {
                "InkKey_${document.questionNumber}_${sanitizedTitle}_Page${page.pageIndex + 1}_of_${totalPages}_$dateStr.png"
            }

            val file = File(exportDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Write to custom SAF folder if set
            if (customTree != null && customTree.canWrite()) {
                try {
                    val customFile = customTree.createFile("image/png", fileName)
                    if (customFile != null) {
                        context.contentResolver.openOutputStream(customFile.uri)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            bitmap.recycle()
            files.add(file)
        }
        files
    }

    /**
     * Exports all pages into a single multipage PDF document.
     */
    suspend fun exportAsPdf(
        context: Context,
        document: AnswerDocument,
        forceWhiteBackground: Boolean = false
    ): File = withContext(Dispatchers.IO) {
        val exportDir = getDefaultExportDirectory(context)
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedTitle = document.title.replace("\\s+".toRegex(), "_").take(20)
        val fileName = "InkKey_${document.questionNumber}_${sanitizedTitle}_$dateStr.pdf"
        val pdfFile = File(exportDir, fileName)

        try {
            val pdfDocument = PdfDocument()
            val totalPages = document.pages.size

            // Standard A4 dimensions in PostScript points: 595 x 842 pt
            val pageWidth = 595
            val pageHeight = 842

            for ((idx, page) in document.pages.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)

                val bitmap = renderPageToBitmap(
                    page = page,
                    document = document,
                    totalPages = totalPages,
                    scale = 1.0f,
                    forceWhiteBackground = forceWhiteBackground,
                    minWidth = pageWidth,
                    minHeight = pageHeight
                )

                pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(pdfPage)
                bitmap.recycle()
            }

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // Also copy to custom SAF directory if set
            val customUri = getCustomExportTreeUri(context)
            if (customUri != null) {
                try {
                    val customTree = DocumentFile.fromTreeUri(context, customUri)
                    if (customTree != null && customTree.canWrite()) {
                        val customFile = customTree.createFile("application/pdf", fileName)
                        if (customFile != null) {
                            context.contentResolver.openOutputStream(customFile.uri)?.use { out ->
                                pdfFile.inputStream().use { input -> input.copyTo(out) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                FileOutputStream(pdfFile).use { out ->
                    out.write("%PDF-1.4\n%InkKey NEET Solution\n%%EOF\n".toByteArray())
                }
            }
        }
        pdfFile
    }

    /**
     * Saves a single Bitmap to the primary export directory and copies to custom SAF tree if set.
     */
    fun saveSingleImage(
        context: Context,
        bitmap: Bitmap,
        baseName: String = "InkKey_NEET_Answer_${System.currentTimeMillis()}"
    ): File {
        val exportDir = getDefaultExportDirectory(context)
        val fileName = if (baseName.endsWith(".png", ignoreCase = true)) baseName else "$baseName.png"
        val file = File(exportDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Also copy to custom SAF directory if set
        val customUri = getCustomExportTreeUri(context)
        if (customUri != null) {
            try {
                val customTree = DocumentFile.fromTreeUri(context, customUri)
                if (customTree != null && customTree.canWrite()) {
                    val customDoc = customTree.createFile("image/png", fileName)
                    if (customDoc != null) {
                        context.contentResolver.openOutputStream(customDoc.uri)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return file
    }

    /**
     * Deletes a single ExportItem from storage.
     */
    fun deleteExportItem(context: Context, item: ExportItem): Boolean {
        return try {
            if (item.file != null && item.file.exists()) {
                item.file.delete()
            } else {
                val doc = DocumentFile.fromSingleUri(context, item.uri)
                doc?.delete() ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copies prompt text to system clipboard.
     */
    fun copyPromptToClipboard(context: Context, prompt: String = DEFAULT_NEET_PROMPT) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("InkKey NEET Prompt", prompt)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "NEET Answer Checking prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Checks if ChatGPT app is installed on the device.
     */
    fun isChatGptInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo("com.openai.chatgpt", PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo("com.openai.chatgpt", 0)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Dispatches the exported files to ChatGPT or Android Share Sheet.
     */
    fun shareAnswer(
        context: Context,
        files: List<File>,
        isPdf: Boolean,
        prompt: String = DEFAULT_NEET_PROMPT,
        preferChatGptDirect: Boolean = true
    ): ShareResult {
        if (files.isEmpty()) return ShareResult.Error("No files to share")

        // 1. Copy prompt to clipboard so it's instantly ready
        copyPromptToClipboard(context, prompt)

        // 2. Prepare content URIs using FileProvider
        val authority = "${context.packageName}.fileprovider"
        val uris = files.map { file ->
            FileProvider.getUriForFile(context, authority, file)
        }

        val mimeType = if (isPdf) "application/pdf" else "image/png"
        val chatGptInstalled = isChatGptInstalled(context)

        // 3. Direct ChatGPT intent if available & requested
        if (preferChatGptDirect && chatGptInstalled) {
            try {
                val directIntent = if (uris.size == 1) {
                    Intent(Intent.ACTION_SEND).apply {
                        setPackage("com.openai.chatgpt")
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uris[0])
                        putExtra(Intent.EXTRA_TEXT, prompt)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        setPackage("com.openai.chatgpt")
                        type = mimeType
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        putExtra(Intent.EXTRA_TEXT, prompt)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }

                directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(directIntent)
                return ShareResult.SentDirectToChatGpt
            } catch (e: Exception) {
                // Fall back to chooser
            }
        }

        // 4. Standard Android Share Sheet
        return try {
            val shareIntent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                    putExtra(Intent.EXTRA_TEXT, prompt)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = mimeType
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    putExtra(Intent.EXTRA_TEXT, prompt)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Send Answer to ChatGPT / Share").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            ShareResult.OpenedShareSheet
        } catch (e: Exception) {
            ShareResult.SavedLocally(files.map { it.absolutePath })
        }
    }
}

package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.ArrayDeque

object UpperTrialTrace {
    private val lines = ArrayDeque<String>()
    private var lastHover = 0L

    fun key(event: android.view.KeyEvent, route: String, handled: Boolean, visible: Boolean? = null) {
        if (!XpPenExpressKeyResolver.isXpPen(event)) return
        record("route=$route time=${event.eventTime} key=${event.keyCode} scan=${event.scanCode} action=${event.action} meta=${event.metaState} handled=$handled visible=$visible device=${event.device?.name}")
    }

    fun motion(event: android.view.MotionEvent, route: String) {
        if (event.pointerCount == 0) return
        val dev = event.device
        val isStylus = event.getToolType(0) in intArrayOf(android.view.MotionEvent.TOOL_TYPE_STYLUS, android.view.MotionEvent.TOOL_TYPE_ERASER)
        if (!isStylus && (dev?.vendorId != 0x28BD || dev?.productId != 0x0948)) return
        if (event.actionMasked == android.view.MotionEvent.ACTION_HOVER_MOVE && event.buttonState == 0) {
            if (event.eventTime - lastHover < 100L) return
            lastHover = event.eventTime
        }
        record("route=$route time=${event.eventTime} action=${android.view.MotionEvent.actionToString(event.actionMasked)} pressure=${event.pressure} tool=${event.getToolType(0)} pointers=${event.pointerCount} deviceId=${event.deviceId} vendor=${dev?.vendorId} product=${dev?.productId} buttons=${event.buttonState} actionButton=${event.actionButton} x=${event.x} y=${event.y} distance=${event.getAxisValue(android.view.MotionEvent.AXIS_DISTANCE)} source=${event.source}")
    }

    fun record(line: String) {
        if (lines.size >= 12000) lines.removeFirst()
        lines.addLast(line)
    }

    fun share(context: Context) {
        val file = File(context.cacheDir, "inkkey-upper-trial.txt")
        file.writeText("InkKey ${com.example.BuildConfig.VERSION_NAME}; MAIN/IME ownership, key routes and upper inference\n" + lines.joinToString("\n"))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share upper-button trial"))
    }
}

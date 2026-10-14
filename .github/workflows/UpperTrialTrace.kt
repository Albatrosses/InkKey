package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.ArrayDeque

object UpperTrialTrace {
    private val lines = ArrayDeque<String>()
    fun record(line: String) {
        if (lines.size >= 6000) lines.removeFirst()
        lines.addLast(line)
    }
    fun share(context: Context) {
        val file = File(context.cacheDir, "inkkey-upper-trial.txt")
        file.writeText("InkKey Upper Trial 1; inferred contact-restart candidates, not hardware button bits\n" + lines.joinToString("\n"))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share upper-button trial"))
    }
}

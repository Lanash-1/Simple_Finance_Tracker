package com.codigitech.ft.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Writes the file under cache/exports and opens the system share sheet through a FileProvider URI. */
class AndroidFileSharer(private val context: Context) : FileSharer {
    override fun shareText(fileName: String, mimeType: String, content: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        // Only the latest export is kept; the folder is cache, so the OS may clear it any time.
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Export transactions")
        val activity = ActivityHolder.current
        if (activity != null) {
            activity.startActivity(chooser)
        } else {
            context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

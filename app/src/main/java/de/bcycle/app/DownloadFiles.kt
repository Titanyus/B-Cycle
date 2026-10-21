package de.bcycle.app

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File

internal object DownloadFiles {
    fun write(context: Context, source: File, name: String) {
        val resolver = context.contentResolver
        val safeName = name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").take(180)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safeName)
            put(MediaStore.Downloads.MIME_TYPE, when(source.extension.lowercase()) { "fit"->"application/vnd.ant.fit";"json"->"application/json";"zip"->"application/zip";else->"application/gpx+xml" })
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/B-Cycle")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Downloads nicht verfügbar")
        try {
            resolver.openOutputStream(uri, "w")?.use { output -> source.inputStream().use { it.copyTo(output) } } ?: error("Download kann nicht geschrieben werden")
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) == 1)
        } catch (error: Exception) { resolver.delete(uri, null, null); throw error }
    }
}

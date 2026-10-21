package de.bcycle.app

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Worker-only snapshot: one complete analysis JSON per saved result, including rejected captures. */
internal object HrvDiagnosticExport {
    fun write(store:HrvStore,target:File):Int {
        val ids=store.ids().sorted()
        require(ids.isNotEmpty()) { "Keine gespeicherten HRV-Messungen" }
        try {
            ZipOutputStream(target.outputStream().buffered()).use { zip ->
                ids.forEach { id ->
                    val bytes=store.analysisJson(id)
                    zip.putNextEntry(ZipEntry("hrv-$id.json"));zip.write(bytes);zip.closeEntry()
                }
            }
        } catch(e:Exception) { target.delete();throw e }
        return ids.size
    }
}

package de.bcycle.app

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Worker-only snapshot: one complete analysis JSON per saved result, including rejected captures. */
internal object HrvDiagnosticExport {
    fun write(store:HrvStore,target:File):Int {
        val ids=store.ids().sorted()
        val dailyIds=store.daily().mapTo(hashSetOf()) { it.id }
        val representedDays=store.list().mapTo(hashSetOf()) { HrvDailySelection.day(it).toString() }
        val orphanDays=store.questionnaireDays().filterNot { it in representedDays }
        require(ids.isNotEmpty() || orphanDays.isNotEmpty()) { "Keine gespeicherten HRV-Messungen" }
        try {
            ZipOutputStream(target.outputStream().buffered()).use { zip ->
                ids.forEach { id ->
                    val bytes=store.analysisJson(id,id in dailyIds)
                    zip.putNextEntry(ZipEntry("hrv-$id.json"));zip.write(bytes);zip.closeEntry()
                }
                orphanDays.forEach { day ->
                    zip.putNextEntry(ZipEntry("day-$day.json"));zip.write(store.backupQuestionnaireDay(day));zip.closeEntry()
                }
            }
        } catch(e:Exception) { target.delete();throw e }
        return ids.size+orphanDays.size
    }
}

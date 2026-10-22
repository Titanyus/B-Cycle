package de.bcycle.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvDailySelectionTest {
    private val utc=ZoneId.of("UTC")
    private fun row(id:String,time:String,valid:Boolean=true,zone:String="UTC") =
        HrvHistorySummary(id,Instant.parse(time).toEpochMilli(),valid,42.0,zoneId=zone)

    @Test fun latestWinsEvenWhenRejectedAndDaysUseCapturedZone() {
        val early=row("early","2025-01-01T10:00:00Z")
        val late=row("late","2025-01-01T11:00:00Z",false)
        val next=row("next","2025-01-01T23:30:00Z",zone="Europe/Berlin")
        assertEquals(listOf("next","late"),HrvDailySelection.latest(listOf(next,late,early),utc).map { it.id })
        assertEquals("2025-01-02",HrvDailySelection.day(next,ZoneId.of("America/New_York")).toString())
        assertFalse(HrvDailySelection.latest(listOf(early,late),utc).single().acceptable)
        val legacy=next.copy(zoneId=null)
        assertEquals("2025-01-01",HrvDailySelection.day(legacy,utc).toString())
        // Both occurrences of the repeated local hour belong to the same captured day.
        val first=row("first","2025-10-26T00:30:00Z",zone="Europe/Berlin")
        val second=row("second","2025-10-26T01:30:00Z",zone="Europe/Berlin")
        assertEquals("second",HrvDailySelection.latest(listOf(first,second),utc).single().id)
    }

    @Test fun importedContextAndUnknownTimesCannotDisplaceKnownMeasurements() {
        val native=row("native","2025-01-01T10:00:00Z")
        val imported=row("imported","2025-01-01T11:00:00Z").copy(importedDay="2025-01-01",importedTime="11:00")
        assertEquals("imported",HrvDailySelection.latest(listOf(native,imported),utc).single().id)
        assertEquals("native",HrvDailySelection.latest(listOf(native,imported.copy(importedTime=null)),utc).single().id)
        assertEquals("native",HrvDailySelection.latest(listOf(native,imported.copy(hasMeasurement=false)),utc).single().id)
        val tied=imported.copy(utcMs=native.utcMs)
        assertEquals("native",HrvDailySelection.latest(listOf(tied,native),utc).single().id)
        assertEquals("native",HrvDailySelection.latest(listOf(native,tied),utc).single().id)
        assertEquals("imported",HrvDailySelection.latest(listOf(imported.copy(importedTime=null)),utc).single().id)
    }

    @Test fun dailyReadsRedirectButArchivesAndExportRetainEveryResultUnchanged() {
        val root=File(RuntimeEnvironment.getApplication().cacheDir,"daily-${UUID.randomUUID()}")
        val zip=File(root,"analysis.zip")
        try {
            val store=HrvStore(root)
            fun save(time:String,phase:String,valid:Boolean):String {
                val id=UUID.randomUUID().toString()
                val dir=File(root,id).apply { mkdirs() }
                File(dir,"result.json").writeText(JSONObject().put("schema",1).put("format","bicycle-hrv")
                    .put("id",id).put("utcMs",Instant.parse(time).toEpochMilli()).put("zone","UTC")
                    .put("phase",phase).put("acceptable",valid).toString())
                File(dir,"capture.jsonl").writeText("")
                return id
            }
            val old=save("2025-01-01T10:00:00Z","COMPLETE",true)
            val original=File(root,"$old/result.json").readBytes()
            val latest=save("2025-01-01T11:00:00Z","ABORTED",false)
            assertEquals(2,store.list().size)
            assertEquals(latest,store.daily().single().id)
            assertEquals(latest,store.readDaily(old).getString("id"))
            assertEquals(old,store.read(old).getString("id"))
            assertArrayEquals(original,File(root,"$old/result.json").readBytes())
            assertEquals(2,HrvDiagnosticExport.write(store,zip))
            ZipFile(zip).use { archive ->
                val entries=archive.entries().asSequence().map { entry ->
                    JSONObject(archive.getInputStream(entry).bufferedReader().use { it.readText() })
                }.toList()
                assertEquals(2,entries.size)
                assertEquals(listOf(latest),entries.filter { it.getBoolean("dailySelected") }.map { it.getJSONObject("result").getString("id") })
                assertEquals(String(original),entries.first { it.getJSONObject("result").getString("id")==old }.getJSONObject("result").toString())
            }
        } finally { root.deleteRecursively() }
    }
}

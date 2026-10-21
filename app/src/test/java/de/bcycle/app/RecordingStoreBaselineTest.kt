package de.bcycle.app

import android.os.Looper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RecordingStoreBaselineTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private val pending get()=File(context.filesDir,"pending-rides").apply { mkdirs() }
    private val start=1791626400000L
    private val messages=mutableListOf<Pair<String,Boolean>>()
    private fun store()=RecordingStore(context) { update -> messages+=RecordingStatusText.text(context,update) to update.busy }
    private fun worker(store:RecordingStore)=RecordingStore::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(store) as ExecutorService
    private fun drain(store:RecordingStore) {
        worker(store).submit {}.get(10,TimeUnit.SECONDS)
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun record(store:RecordingStore) {
        store.begin(start,RecordingConfig(setOf("HEART_RATE")))
        store.append(RideSample(start,0,values=mapOf("HEART_RATE" to 130.0)))
        store.finish();drain(store)
    }
    @Test fun localSuccessClosesAndRemovesJournalAndKeepsExactLegacyMessages() {
        RouteExportSettings(context).write(RouteExportConfig(save=false,webdav=false))
        val store=store()
        try {
            record(store)
            assertEquals(listOf("FIT wird gespeichert …" to true,"Gespeichert: FIT in Streckensammlung" to false),messages)
            assertTrue(pending.listFiles()!!.isEmpty())
            assertEquals(1,File(context.filesDir,"fit-library").listFiles { f -> f.extension=="fit" }!!.size)
        } finally { worker(store).shutdownNow() }
    }
    @Test fun failureAfterLocalRetentionKeepsJournalAndRetryPreservesOriginalBytes() {
        RouteExportSettings(context).write(RouteExportConfig(save=false,webdav=true,server="",user="rider"))
        val store=store()
        try {
            record(store)
            assertEquals("FIT lokal gespeichert · Ordnerexport oder Upload-Vormerkung fehlgeschlagen. Im Menü erneut versuchen." to false,messages.last())
            val original=File(context.filesDir,"fit-library").listFiles { f -> f.extension=="fit" }!!.single()
            val bytes=original.readBytes()
            assertEquals(1,pending.listFiles { f -> f.extension=="ride" }!!.size)
            RouteExportSettings(context).write(RouteExportConfig(save=false,webdav=false))
            store.retryPending();drain(store)
            assertEquals("Gespeichert: FIT in Streckensammlung" to false,messages.last())
            assertArrayEquals(bytes,original.readBytes())
            assertTrue(pending.listFiles()!!.isEmpty())
        } finally { worker(store).shutdownNow() }
    }
    @Test fun mixedRecoveryCountsOnlyCompleteRidesAndLeavesUnreadableJournal() {
        RouteExportSettings(context).write(RouteExportConfig(save=false,webdav=false))
        repeat(2) { index ->
            RideJournal(File(pending,"valid-$index.ride"),start+index*1000,RecordingConfig(setOf("HEART_RATE"))).use {
                it.append(RideSample(start+index*1000,0,values=mapOf("HEART_RATE" to (130.0+index))))
            }
        }
        val bad=File(pending,"broken.ride").apply { writeText("broken") }
        val store=store()
        try {
            store.retryPending();drain(store)
            assertEquals("2 von 3 Fahrten gespeichert · übrige Daten bleiben erhalten" to false,messages.last())
            assertEquals(listOf(bad),pending.listFiles { f -> f.extension=="ride" }!!.toList())
            assertEquals(2,File(context.filesDir,"fit-library").listFiles { f -> f.extension=="fit" }!!.size)
        } finally { worker(store).shutdownNow() }
    }
    @Test fun unreadableJournalIsNotReportedAsLocallySaved() {
        val bad=File(pending,"broken.ride").apply { writeText("broken") }
        val store=store()
        try {
            store.retryPending();drain(store)
            assertEquals("FIT nicht gespeichert · Daten bleiben erhalten. Im Menü erneut speichern." to false,messages.last())
            assertTrue(bad.isFile)
        } finally { worker(store).shutdownNow() }
    }
    @Test fun writeFailureKeepsPreviouslyFlushedSampleAndReportsLegacyError() {
        val store=store()
        try {
            store.begin(start,RecordingConfig(setOf("HEART_RATE")))
            store.append(RideSample(start,0,values=mapOf("HEART_RATE" to 130.0)));drain(store)
            worker(store).submit {
                (RecordingStore::class.java.getDeclaredField("journal").apply { isAccessible=true }.get(store) as RideJournal).close()
            }.get(10,TimeUnit.SECONDS)
            store.append(RideSample(start+1000,1000,values=mapOf("HEART_RATE" to 140.0)));drain(store)
            assertTrue(store.failed)
            assertEquals("Fehler beim Schreiben · Zwischendatei bleibt erhalten" to false,messages.last())
            RideJournal.read(pending.listFiles { f -> f.extension=="ride" }!!.single()) { _,_,_,samples ->
                assertEquals(listOf(130.0),samples.map { it.values["HEART_RATE"] }.toList())
            }
        } finally { worker(store).shutdownNow() }
    }
}

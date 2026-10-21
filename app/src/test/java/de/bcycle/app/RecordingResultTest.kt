package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RecordingResultTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private fun text(update:RecordingUpdate)=RecordingStatusText.text(context,update)
    private fun export(source:File):RideCompletion {
        val store=RecordingStore(context) { _ -> }
        val worker=RecordingStore::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(store) as ExecutorService
        val method=RecordingStore::class.java.getDeclaredMethod("export",File::class.java).apply { isAccessible=true }
        try { return worker.submit<RideCompletion> { method.invoke(store,source) as RideCompletion }.get(10,TimeUnit.SECONDS) }
        finally { worker.shutdownNow() }
    }
    private fun source()=File(context.filesDir,"pending-rides/reference.ride").apply {
        parentFile!!.mkdirs()
        RecordingResultTest::class.java.getResourceAsStream("/recording/legacy-recording.ride")!!.use { input -> outputStream().use { input.copyTo(it) } }
    }
    @Test fun actualExportReturnsCompletedWithStableLocalIdentityAndQueuedDestinationsOnly() {
        RouteExportSettings(context).write(RouteExportConfig(save=false,webdav=true,server="https://cloud.example.org",user="rider"))
        val source=source()
        val result=export(source) as RideCompletion.Completed
        assertTrue(result.rideId.startsWith("recorded-"))
        assertEquals(setOf(RecordingDestination.WEBDAV),result.destinations)
        assertTrue(result.warnings.isEmpty())
        assertTrue(FitLibrary.activeFile(context.filesDir,result.rideId).isFile)
        assertFalse(source.exists())
        assertEquals(UploadState.WAITING,RouteUploads.state(context,result.rideId).state)
        assertEquals("Gespeichert: FIT in Streckensammlung · Nextcloud-Upload vorgemerkt",text(RecordingUpdate.Finished(result)))
    }
    @Test fun actualExportDistinguishesRetainedRideFromFailureBeforeRetention() {
        RouteExportSettings(context).write(RouteExportConfig(save=false,webdav=true,server="",user="rider"))
        val source=source()
        val retained=export(source) as RideCompletion.LocallyRetained
        assertEquals(RecordingFinishStage.QUEUE_UPLOADS,retained.failedAt)
        assertTrue(FitLibrary.activeFile(context.filesDir,retained.rideId).isFile)
        assertTrue(source.isFile)
        val broken=File(source.parentFile,"broken.ride").apply { writeText("broken") }
        val failed=export(broken) as RideCompletion.NotSaved
        assertEquals(RecordingFinishStage.ENCODE_FIT,failed.failedAt)
        assertNull(failed.rideId)
        assertTrue(broken.isFile)
    }
    @Test fun actualLocalExportFailureDoesNotCancelAlreadyQueuedCloudUpload() {
        RouteExportSettings(context).write(RouteExportConfig(save=true,tree="content://missing/tree/root",webdav=true,server="https://cloud.example.org",user="rider"))
        val source=source()
        val result=export(source) as RideCompletion.LocallyRetained
        assertEquals(RecordingFinishStage.EXPORT_LOCAL,result.failedAt)
        assertEquals(UploadState.WAITING,RouteUploads.state(context,result.rideId).state)
        assertTrue(source.isFile)
    }
    @Test fun recoveryCountsOnlyCompletedResultsAndPreservesSingleRideMessages() {
        val full=RideCompletion.Completed("recorded-test")
        val partial=RideCompletion.LocallyRetained("recorded-partial",RecordingFinishStage.EXPORT_LOCAL)
        val failed=RideCompletion.NotSaved(RecordingFinishStage.ENCODE_FIT)
        val batch=RecordingUpdate.Recovered(listOf(full,partial,failed))
        assertEquals(1,batch.completedCount)
        assertFalse(batch.busy)
        assertEquals("1 von 3 Fahrten gespeichert · übrige Daten bleiben erhalten",text(batch))
        assertEquals("FIT lokal gespeichert · Ordnerexport oder Upload-Vormerkung fehlgeschlagen. Im Menü erneut versuchen.",text(RecordingUpdate.Recovered(listOf(partial))))
        assertEquals("FIT nicht gespeichert · Daten bleiben erhalten. Im Menü erneut speichern.",text(RecordingUpdate.Finished(failed)))
        assertEquals("Keine ungespeicherten Fahrten",text(RecordingUpdate.Recovered(emptyList())))
    }
    @Test fun progressAndWarningsKeepBusyFlagsAndLegacyTextOrder() {
        assertTrue(RecordingUpdate.Progress.SAVING_FIT.busy)
        assertTrue(RecordingUpdate.Progress.CORRECTING_ELEVATION.busy)
        val notices=listOf("Speicher überlastet · bitte STOPP drücken","Aufzeichnung konnte nicht angelegt werden · bitte STOPP",
            "Fehler beim Schreiben · Zwischendatei bleibt erhalten","Keine Aufzeichnungsdatei vorhanden","Keine ungespeicherten Fahrten")
        assertEquals(notices,RecordingUpdate.Notice.entries.map { text(it) })
        assertTrue(RecordingUpdate.Notice.entries.none { it.busy })
        val result=RideCompletion.Completed("recorded-test",RecordingDestination.entries.toSet(),RecordingWarning.entries.toSet())
        assertEquals("Gespeichert: FIT in Streckensammlung · auch im Zielordner · Nextcloud-Upload vorgemerkt · enthält Daten bis zum Speicherfehler · "+
            context.getString(R.string.intervals_queued)+" · Dreeve · Strava · "+context.getString(R.string.elevation_auto_failed),text(RecordingUpdate.Finished(result)))
    }
    @Test fun recoveryKeepsLegacyAsciiCountsRegardlessOfDeviceNumberLocale() {
        val config=android.content.res.Configuration(context.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag("ar")) }
        val localized=context.createConfigurationContext(config)
        val batch=RecordingUpdate.Recovered(listOf(RideCompletion.Completed("ride"),RideCompletion.NotSaved(RecordingFinishStage.ENCODE_FIT)))
        assertEquals("1 von 2 Fahrten gespeichert · übrige Daten bleiben erhalten",RecordingStatusText.text(localized,batch))
    }
}

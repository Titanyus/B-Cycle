package de.bcycle.app

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import com.garmin.fit.FitDecoder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration

/** Exercises real runtime/service/journal/export against a native ContentProvider test double. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35, 36])
class RecordingFlowTest {
    private fun provider(): DownloadsProvider {
        val app = RuntimeEnvironment.getApplication()
        return DownloadsProvider(File(app.cacheDir, "test-downloads").apply { mkdirs() }).also {
            it.attachInfo(app, ProviderInfo().apply { authority = MediaStore.AUTHORITY })
            ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, it)
        }
    }
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10)
        }
        assertTrue("Worker did not finish", condition())
    }
    @Test fun recordingContinuesWithoutActivityAndStopPublishesSelectedGpsRouteInDownloads() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
        val provider = provider()
        val runtime = RideRuntime.get(app)
        runtime.saveConfig(RecordingConfig(setOf("GPS_SPEED", "GPS_DISTANCE"), "GPS_SPEED", "GPS_DISTANCE"))
        val manager = app.getSystemService(LocationManager::class.java)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        runtime.setVisible(true)
        val controller = Robolectric.buildService(RecordingService::class.java).create()
        val service = controller.get()
        service.onStartCommand(Intent(app, RecordingService::class.java).setAction(RecordingService.START), 0, 1)
        assertTrue(runtime.recording)
        runtime.setVisible(false) // Equivalent to Activity.onStop / display off.
        assertTrue(runtime.gpsSensor.active)
        // Changing next-ride settings must not alter this ride's frozen field selection.
        runtime.saveConfig(RecordingConfig(setOf("RIDER_POWER")))
        repeat(12) { index ->
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            shadowOf(manager).simulateLocation(Location(LocationManager.GPS_PROVIDER).apply {
                latitude = 50.0 + index * 0.000045; longitude = 8.0; accuracy = 3f
                speed = 5f; speedAccuracyMetersPerSecond = 0.2f
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos(); time = System.currentTimeMillis()
            })
            shadowOf(Looper.getMainLooper()).idle()
        }
        service.onStartCommand(Intent(app, RecordingService::class.java).setAction(RecordingService.STOP), 0, 2)
        assertFalse(runtime.recording)
        assertTrue(runtime.saving)
        assertFalse(shadowOf(service).isForegroundStopped)
        assertFalse(runtime.gpsSensor.active)
        await { !runtime.saving }
        assertTrue(shadowOf(service).isForegroundStopped)
        assertTrue(runtime.recordingStatus.startsWith("Gespeichert:"))
        assertTrue((runtime.recordingUpdate as RecordingUpdate.Finished).result is RideCompletion.Completed)
        assertEquals(1, provider.published.size)
        assertEquals("Download/B-Cycle", provider.lastValues!!.getAsString(MediaStore.Downloads.RELATIVE_PATH))
        assertTrue(provider.lastValues!!.getAsString(MediaStore.Downloads.DISPLAY_NAME).endsWith(".fit"))
        val records = provider.published.single().inputStream().use { FitDecoder().decode(it).recordMesgs }
        assertTrue(records.size >= 6)
        assertTrue(records.any { it.positionLat != null && it.speed != null })
        assertTrue(records.last().distance > 0f)
        assertTrue(records.all { it.power == null })
        assertTrue(runtime.mapTrackEnabled) // Frozen START selection, independent of next-ride preferences.
        assertTrue(runtime.mapState.track.points.size >= 3) // Same samples also populate the map while Activity is absent.
        // STOP is idempotent: it must not publish a duplicate ride.
        service.onStartCommand(Intent(app, RecordingService::class.java).setAction(RecordingService.STOP), 0, 3)
        assertEquals(1, provider.published.size)
        controller.destroy()
    }
    @Test fun recordingClockWorksWithoutGpsFreezesAtStopAndMatchesFitTerminalSample() {
        val app = RuntimeEnvironment.getApplication()
        val downloads = provider(); val runtime = RideRuntime.get(app)
        runtime.saveConfig(RecordingConfig(setOf("GPS_ELAPSED_TIME")))
        runtime.setVisible(true); runtime.beginRecording()
        assertEquals(0L, runtime.gpsValues.elapsedMs)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(12250))
        assertEquals(12250L, runtime.elapsedMs); assertEquals(12250L, runtime.gpsValues.elapsedMs)
        runtime.stopRecording()
        val stopped = runtime.elapsedMs
        val now = SystemClock.elapsedRealtime()
        runtime.ride.gps.accept(GpsFix(now, 50.0, 8.0, 3.0, speedMps = 5.0), now)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        assertEquals(stopped, runtime.elapsedMs); assertEquals(stopped, runtime.gpsValues.elapsedMs)
        await { !runtime.saving }
        val fit = downloads.published.single().inputStream().use { FitDecoder().decode(it) }
        assertEquals(stopped / 1000.0, fit.sessionMesgs.single().totalTimerTime.toDouble(), .001)
        val duration = fit.recordMesgs.last().developerFields.single { it.name == "GPS_ELAPSED_TIME" }
        assertEquals(stopped / 1000.0, (duration.getValue(0) as Number).toDouble(), .001)
        runtime.beginRecording(); assertEquals(0L, runtime.elapsedMs); assertEquals(0L, runtime.gpsValues.elapsedMs)
        runtime.stopRecording(); runtime.setVisible(false); await { !runtime.saving }
    }
    @Test fun previewGpsBeforeFirstStartDoesNotRunRecordingClockOrWriteAnyRide() {
        val app = RuntimeEnvironment.getApplication(); val downloads = provider()
        val runtime = RideRuntime.get(app)
        runtime.saveConfig(RecordingConfig(setOf("GPS_ELAPSED_TIME", "GPS_DISTANCE")))
        runtime.setVisible(true)
        repeat(6) { i ->
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            val now = SystemClock.elapsedRealtime()
            assertTrue(runtime.ride.gps.accept(GpsFix(now, 50.0 + i * .00005, 8.0, 3.0, 5.0, .2), now))
            assertEquals(0L, runtime.gpsValues.elapsedMs)
        }
        assertTrue(runtime.gpsValues.speedKmh!! > 0)
        assertNull(runtime.gpsValues.distanceM); assertNull(runtime.gpsValues.movingMs)
        assertFalse(runtime.recording); assertTrue(downloads.published.isEmpty())
        assertTrue(File(app.filesDir, "pending-rides").listFiles().orEmpty().isEmpty())
        runtime.beginRecording()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(12250))
        assertEquals(12250L, runtime.gpsValues.elapsedMs)
        runtime.stopRecording(); runtime.setVisible(false)
        await { !runtime.saving }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        assertEquals(12250L, runtime.gpsValues.elapsedMs)
    }
    @Test fun slowUiWorkDoesNotAccumulateDriftBetweenRecordingSeconds() {
        val app = RuntimeEnvironment.getApplication(); provider()
        val runtime = RideRuntime.get(app); runtime.saveConfig(RecordingConfig(setOf("RIDER_POWER")))
        runtime.setVisible(true)
        // Finish initial library notifications before measuring just the recording clock.
        await { !runtime.mapState.busy }
        val seconds = mutableListOf<Long>()
        val observer: () -> Unit = {
            // Graph publication can notify between ticks; it is not another recording second.
            if (runtime.recording && runtime.elapsedMs >= 1000 && runtime.elapsedMs % 1000L == 0L) {
                seconds += runtime.elapsedMs
                org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(350))
            }
        }
        runtime.observers.add(observer)
        try {
            runtime.beginRecording(); seconds.clear()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
            assertEquals(listOf(1000L, 2000L, 3000L), seconds)
        } finally {
            runtime.observers.remove(observer); runtime.stopRecording(); runtime.setVisible(false)
            await { !runtime.saving }
        }
    }
    @Test fun gpsSwitchControlsPositionsAndCanEnableThemDuringRecordingWithoutASecondFitSetting() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val downloads=provider()
        val runtime = RideRuntime.get(app)
        runtime.saveConfig(RecordingConfig(setOf("HEART_RATE"))) // Legacy GPS checkbox was off.
        runtime.setGpsEnabled(false)
        assertFalse("GPS_POSITION" in runtime.config.fields)
        runtime.setVisible(true); runtime.beginRecording()
        val now = SystemClock.elapsedRealtime()
        runtime.ride.gps.accept(GpsFix(now, 50.0, 8.0, 3.0, 5.0, .2), now)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertTrue(runtime.mapState.track.points.isEmpty())
        runtime.setGpsEnabled(true)
        assertTrue("GPS_POSITION" in runtime.config.fields)
        repeat(2) { i ->
            val stamp=SystemClock.elapsedRealtime()
            runtime.ride.gps.accept(GpsFix(stamp,50.0001+i*.0001,8.0,3.0,5.0,.2),stamp)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        }
        assertTrue(runtime.mapState.track.points.size>=2)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8))
        runtime.stopRecording(); runtime.setVisible(false)
        await { !runtime.saving }
        val records=downloads.published.single().inputStream().use { FitDecoder().decode(it).recordMesgs }
        assertNull(records.first().positionLat)
        assertTrue(records.any { it.positionLat!=null && it.positionLong!=null })
    }
    @Test fun failedDownloadLeavesPrivateJournalAndRetryPublishesOneValidFit() {
        val app = RuntimeEnvironment.getApplication()
        val provider = provider().apply { failWriting = true }
        var status = ""
        val store = RecordingStore(app) { update -> status = RecordingStatusText.text(app,update) }
        val start = 1_791_626_400_000L
        store.begin(start, RecordingConfig(setOf("HEART_RATE")))
        store.append(RideSample(start, 0, values = mapOf("HEART_RATE" to 130.0)))
        store.append(RideSample(start + 1000, 1000, values = mapOf("HEART_RATE" to 140.0)))
        store.finish()
        await { status.startsWith("FIT lokal gespeichert") }
        val directory = File(app.filesDir, "pending-rides")
        assertEquals(1, directory.listFiles { file -> file.extension == "ride" }!!.size)
        assertTrue(provider.published.isEmpty())
        val privateFits = File(app.filesDir, "fit-library").listFiles { file -> file.extension == "fit" }!!
        assertEquals(1, privateFits.size) // Finalized activity survives even an unavailable Downloads provider.
        assertNotNull(RideProfileColor.color(app,privateFits.single().nameWithoutExtension))
        val preserved = privateFits.single().readBytes()
        assertTrue(provider.files.isEmpty()) // Incomplete public entry was deleted.
        provider.failWriting = false
        store.retryPending()
        await { status.startsWith("Gespeichert:") }
        assertEquals(1, provider.published.size)
        assertTrue(directory.listFiles()!!.isEmpty())
        assertArrayEquals(preserved, File(app.filesDir, "fit-library").listFiles { file -> file.extension == "fit" }!!.single().readBytes())
        val records = provider.published.single().inputStream().use { FitDecoder().decode(it).recordMesgs }
        assertEquals(listOf(130, 140), records.map { it.heartRate.toInt() })
    }

    @Test fun powerEngineAndJournalRunWithoutDashboardAndWithoutPowerExportCheckbox() {
        val app = RuntimeEnvironment.getApplication()
        val provider = provider()
        val runtime = RideRuntime.get(app)
        runtime.saveConfig(RecordingConfig(setOf("MECHANICAL_WORK")))
        runtime.beginRecording(); runtime.setVisible(false)
        repeat(10) {
            runtime.onSamples(listOf(McspSample(LiveMetric.RIDER_POWER.address, 200, true)))
            // Repeated cached packets must not create extra actual observations.
            repeat(10) { runtime.onData(LiveData(riderPowerWatts = 200), true) }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        }
        runtime.stopRecording()
        assertEquals(2.0, runtime.performanceValues.mechanicalWorkKj!!, 1e-9)
        await { !runtime.saving }
        val decoded = provider.published.single().inputStream().use { FitDecoder().decode(it) }
        assertEquals(2000L, decoded.sessionMesgs.single().totalWork)
        assertTrue(decoded.recordMesgs.all { it.power == null })
        assertNull(decoded.sessionMesgs.single().normalizedPower)
    }

    private class DownloadsProvider(private val directory: File) : ContentProvider() {
        val files = linkedMapOf<Uri, File>()
        val published = mutableListOf<File>()
        var failWriting = false
        var lastValues: ContentValues? = null
        private var nextId = 0
        override fun onCreate() = true
        override fun insert(uri: Uri, values: ContentValues?): Uri {
            lastValues = ContentValues(values!!)
            assertEquals(1, values.getAsInteger(MediaStore.Downloads.IS_PENDING).toInt())
            val entry = Uri.withAppendedPath(uri, (++nextId).toString())
            files[entry] = File(directory, "entry-$nextId.fit")
            return entry
        }
        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            if (failWriting) throw FileNotFoundException("Simulated unavailable storage")
            return ParcelFileDescriptor.open(files.getValue(uri), ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY)
        }
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            assertEquals(0, values!!.getAsInteger(MediaStore.Downloads.IS_PENDING).toInt())
            published.add(files.getValue(uri)); return 1
        }
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int { files.remove(uri)?.delete(); return 1 }
        override fun getType(uri: Uri) = "application/vnd.ant.fit"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    }
}

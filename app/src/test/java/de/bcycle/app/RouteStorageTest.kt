package de.bcycle.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteStorageTest {
    private fun await(state: MapState) {
        val deadline = System.nanoTime() + 10_000_000_000L
        do { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(5) } while (state.busy && System.nanoTime() < deadline)
        assertFalse(state.message, state.busy)
    }
    private fun fit(context: Context, start: Long = 1_791_626_400_000L): File {
        val journal = File(context.cacheDir, "recorded.ride")
        RideJournal(journal, start, RecordingConfig(setOf("GPS_POSITION", "BIKE_DISTANCE"))).use {
            for (i in 0..3) it.append(RideSample(start + i * 1000, i * 1000L, 50.0 + i * .0001, 8.0, mapOf("BIKE_DISTANCE" to i * 10.0)))
        }
        return File(context.cacheDir, "recorded.fit").also { FitWriter.encode(journal, it) }
    }
    @Test fun summaryExportWithRpeIsOneDownloadAndRemainsOneRideAfterScanAndRestart() {
        val context=RuntimeEnvironment.getApplication()
        val provider=Downloads(context.cacheDir).apply { attachInfo(context,ProviderInfo().apply { authority="media" }) }
        ShadowContentResolver.registerProviderInternal("media",provider)
        val original=FitLibrary.retainRecording(context.filesDir,fit(context))
        val id=original.nameWithoutExtension;val before=original.readBytes()
        val library=FitLibrary(context.filesDir);library.load()
        RideProfileColor.confirm(context,id,RideProfileColor.snapshot(context))
        RideNames.generated(context,id,"Meine Feierabendrunde")
        RideFinishTransfers.hold(context,id,"B-Cycle-2026-11-08-ride.fit",setOf(RecordingDestination.LOCAL_FOLDER),"")
        RideAnnotations.rpe(context,id,6)
        RideFinishTransfers.release(context,id)
        (RideFinishTransfers::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(null) as java.util.concurrent.ExecutorService).submit {}.get(10,java.util.concurrent.TimeUnit.SECONDS)
        (RideAnnotations::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(null) as java.util.concurrent.ScheduledExecutorService).submit {}.get(10,java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(1,provider.files.size)
        val exported=provider.files.values.single()
        assertFalse(before.contentEquals(exported.readBytes()))
        assertEquals(60,exported.inputStream().use { com.garmin.fit.FitDecoder().decode(it) }.sessionMesgs.first().workoutRpe.toInt())
        assertEquals(0,library.discoverDownloads(context))
        assertEquals(listOf(id),library.entries.map { it.id })
        val restored=FitLibrary(context.filesDir);restored.load();restored.discoverDownloads(context)
        assertEquals(listOf(id),restored.entries.map { it.id })
        assertEquals("Meine Feierabendrunde",RideNames.name(context,id))
        assertNotNull(RideProfileColor.color(context,id));assertEquals(6,RideAnnotations.read(context,id).rpe)
        assertArrayEquals(before,original.readBytes())
        RideAnnotations.forget(context,id)
    }
    @Test fun olderAndUpdatedOwnExportsMatchTheOriginalButDifferentRidesStillImport() {
        val context=RuntimeEnvironment.getApplication()
        val provider=Downloads(context.cacheDir).apply { attachInfo(context,ProviderInfo().apply { authority="media" }) }
        ShadowContentResolver.registerProviderInternal("media",provider)
        val source=fit(context);val original=FitLibrary.retainRecording(context.filesDir,source)
        val id=original.nameWithoutExtension;val bytes=original.readBytes()
        val library=FitLibrary(context.filesDir);library.load()
        RideFinishTransfers.hold(context,id,"unused.fit",emptySet(),"")
        RideAnnotations.rpe(context,id,0)
        LocalRouteExport.write(context,RideAnnotationFiles.current(context,id),"B-Cycle-first.fit","")
        val old=provider.files.values.single().readBytes()
        RideAnnotations.rpe(context,id,9);RideAnnotations.note(context,id,"Nachtrag")
        LocalRouteExport.update(context,id,RideAnnotationFiles.current(context,id))
        val latest=provider.files.values.single().readBytes()
        assertFalse(old.contentEquals(latest));assertEquals(1,provider.files.size)
        assertEquals(0,library.discoverDownloads(context));assertEquals(1,library.entries.size)
        val identities=org.json.JSONObject(File(context.filesDir,"fit-export-identities.json").readText()).getJSONObject("owners")
        assertTrue(identities.length()>=2)
        (FitExportIdentities::class.java.getDeclaredField("cache").apply { isAccessible=true }.get(null) as MutableMap<*,*>).clear()
        val restored=FitLibrary(context.filesDir);restored.load()
        assertEquals(id,restored.add(old.inputStream(),"Umbenannt.fit",true)!!.id)
        assertEquals(id,restored.add(latest.inputStream(),"Noch ein Name.fit",true)!!.id)
        assertEquals(1,restored.entries.size)
        val different=fit(context,1_791_630_000_000L).inputStream().use { restored.add(it,"Andere Fahrt.fit",true) }!!
        assertNotEquals(id,different.id);assertEquals(2,restored.entries.size)
        assertArrayEquals(bytes,original.readBytes());assertEquals(9,RideAnnotations.read(context,id).rpe)
        restored.delete(id)
        assertNull(restored.add(latest.inputStream(),"B-Cycle-deleted.fit",true,automatic=true))
        assertEquals(listOf(different.id),restored.entries.map { it.id })
        RideFinishTransfers.forget(context,id);RideAnnotations.forget(context,id)
    }
    @Test fun oldDownloadsJoinGpxInOneManagerAndRenameExportSelectDeletePreserveOriginalsAcrossRestart() {
        val context = RuntimeEnvironment.getApplication()
        val provider = Downloads(context.cacheDir).apply { attachInfo(context, ProviderInfo().apply { authority = "media" }) }
        ShadowContentResolver.registerProviderInternal("media", provider)
        val original = fit(context); val bytes = original.readBytes()
        DownloadFiles.write(context, original, "B-Cycle-2026-10-11.fit")
        val originalDownload = provider.files.values.single()
        val gpx = GpxLibrary(context.filesDir); gpx.load()
        gpx.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(), "Geplante Runde.gpx")
        val state = MapState(context) {}; await(state)
        assertEquals(2, state.entries.size)
        val ride = state.entries.single { it.recorded }; val planned = state.entries.single { !it.recorded }
        assertEquals("FIT", ride.format); assertEquals("GPX", planned.format)
        assertArrayEquals(bytes, originalDownload.readBytes())
        state.renameRoute(ride.id, "Meine Fahrt"); await(state)
        state.selectRoute(ride.id); await(state)
        assertEquals(ride.id, state.selectedRoute); assertNotNull(state.route)
        assertEquals(4, state.route!!.originalPoints)
        state.exportRoute(ride.id); await(state)
        assertEquals(2, provider.files.size)
        state.recheckTransfer(ride.id,RouteTransferTarget.LOCAL);await(state)
        assertEquals(3,provider.files.size) // An explicit repeat saves another copy and preserves existing downloads.
        provider.files.values.forEach { assertArrayEquals(bytes, it.readBytes()) }
        state.selectRoute(null);await(state) // Existing map slots do not change their order when added again.
        state.selectRoute(planned.id); await(state)
        assertEquals(planned.id, state.selectedRoute)
        val restarted = MapState(context) {}; await(restarted)
        assertEquals(2, restarted.entries.size)
        assertEquals("Meine Fahrt", restarted.entries.single { it.recorded }.name)
        restarted.deleteRoute(ride.id); await(restarted)
        assertEquals(1, restarted.entries.size)
        assertEquals(planned.id, restarted.selectedRoute); assertNotNull(restarted.route)
        assertArrayEquals(bytes, originalDownload.readBytes())
        val afterDeletion = MapState(context) {}; await(afterDeletion)
        assertEquals(1, afterDeletion.entries.size) // Existing exports do not resurrect a deleted entry.
    }
    @Test fun finishingRecordingRegistersItsPrivateFitWithoutReadingDownloads() {
        val context = RuntimeEnvironment.getApplication()
        val original = fit(context)
        val state = MapState(context) {}; await(state)
        FitLibrary.retainRecording(context.filesDir, original)
        state.refreshLibrary(); await(state)
        val entry = state.entries.single()
        assertTrue(entry.recorded); assertEquals("FIT", entry.format); assertTrue(entry.hasRoute)
        state.selectRoute(entry.id); await(state)
        val restarted = MapState(context) {}; await(restarted)
        assertEquals(entry.id, restarted.selectedRoute); assertNotNull(restarted.route)
    }
    @Test fun unselectedMapDefaultsToLatestRecordedRideAndHidingKeepsChoiceAcrossRestart() {
        val context = RuntimeEnvironment.getApplication()
        val fits = FitLibrary(context.filesDir); fits.load()
        fit(context, 1_791_626_400_000L).inputStream().use { fits.add(it, "Alt.fit", true) }
        val latest = fit(context, 1_791_630_000_000L).inputStream().use { fits.add(it, "Neu.fit", true) }!!
        fit(context, 1_791_633_600_000L).inputStream().use { fits.add(it, "Import.fit", false) }
        val state = MapState(context) {}; await(state)
        assertEquals(latest.id, state.selectedRoute); assertNotNull(state.route)
        val original = state.route
        state.setRouteVisible(false)
        assertSame(original, state.route); assertEquals(latest.id, state.selectedRoute)
        val prefs = context.getSharedPreferences("bike", 0)
        assertFalse(prefs.getBoolean("mapGpxVisible", true))
        val restored = MapState(context) {}; await(restored)
        assertEquals(latest.id, restored.selectedRoute); assertNotNull(restored.route)
        assertFalse(prefs.getBoolean("mapGpxVisible", true))
        restored.setRouteVisible(true)
        assertEquals(latest.id, restored.selectedRoute); assertTrue(prefs.getBoolean("mapGpxVisible", false))
    }
    private class Downloads(private val root: File) : ContentProvider() {
        val files = linkedMapOf<Uri, File>()
        private val names = mutableMapOf<Uri, String>()
        private var nextId = 0
        override fun onCreate() = true
        override fun insert(uri: Uri, values: ContentValues?): Uri {
            val entry = Uri.withAppendedPath(uri, (++nextId).toString())
            files[entry] = File(root, "download-$nextId.fit")
            names[entry] = values!!.getAsString(MediaStore.Downloads.DISPLAY_NAME)
            return entry
        }
        override fun openFile(uri: Uri, mode: String) = ParcelFileDescriptor.open(files.getValue(uri),
            if (mode.contains('w')) ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE else ParcelFileDescriptor.MODE_READ_ONLY)
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            val columns = projection ?: arrayOf(MediaStore.Downloads._ID, OpenableColumns.DISPLAY_NAME)
            val cursor = MatrixCursor(columns)
            for ((entry, _) in files) if (uri == MediaStore.Downloads.EXTERNAL_CONTENT_URI || uri == entry) {
                cursor.addRow(columns.map { column -> when (column) {
                    MediaStore.Downloads._ID -> entry.lastPathSegment!!.toLong()
                    OpenableColumns.DISPLAY_NAME -> names[entry]
                    else -> null
                } })
            }
            return cursor
        }
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 1
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int { files.remove(uri)?.delete(); names.remove(uri); return 1 }
        override fun getType(uri: Uri) = "application/vnd.ant.fit"
    }
}

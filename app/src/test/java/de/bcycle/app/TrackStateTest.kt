package de.bcycle.app

import android.os.Looper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TrackStateTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun await(state: MapState) {
        val deadline = System.nanoTime() + 10_000_000_000L
        do { shadowOf(Looper.getMainLooper()).idle(); if (state.busy) Thread.sleep(10) }
        while (state.busy && System.nanoTime() < deadline)
        assertFalse(state.busy)
    }
    @Test fun importedGpxPersistsOriginAndElevationWithoutJoiningSegmentsOrNoise() {
        val text = """<gpx><trk><trkseg><trkpt lat="50" lon="8"><ele>100</ele></trkpt>
            <trkpt lat="50.01" lon="8.01"><ele>101</ele></trkpt><trkpt lat="50.02" lon="8.02"><ele>110</ele></trkpt>
            </trkseg><trkseg><trkpt lat="51" lon="9"><ele>500</ele></trkpt><trkpt lat="51.01" lon="9.01"><ele>510</ele></trkpt></trkseg></trk></gpx>"""
        val library = GpxLibrary(context.filesDir)
        val route = library.add(text.byteInputStream(), "Höhenroute.gpx")
        assertEquals(20.0, route.ascentM!!, 0.01)
        val reloaded = GpxLibrary(context.filesDir); reloaded.load()
        assertEquals(TrackOrigin.IMPORTED, reloaded.entries.single().origin)
        assertEquals(20.0, reloaded.entries.single().details.ascentM!!, 0.01)
        val index = File(context.filesDir, "gpx-library/index.json")
        val json = JSONObject(index.readText()); json.getJSONArray("entries").getJSONObject(0).put("origin", "unrecognized")
        index.writeText(json.toString())
        reloaded.load(); assertEquals(TrackOrigin.UNKNOWN, reloaded.entries.single().origin)
    }
    @Test fun localStateUsesReadableOriginalAndMissingFileNeverReportsSaveSuccess() {
        val library = GpxLibrary(context.filesDir)
        library.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(), "Route")
        val id = library.entries.single().id
        val state = MapState(context) {}; await(state)
        assertTrue(state.entries.single().locallySaved)
        assertNotNull(state.entries.single().fileStamp)
        library.file(id).delete(); state.refreshLibrary(); await(state)
        assertFalse(state.entries.single().locallySaved)
        state.saveRouteLocally(id); await(state)
        assertTrue(state.message.contains("Originaldatei fehlt")); assertFalse(state.entries.single().locallySaved)
        state.uploadRoute(id); await(state)
        assertTrue(state.message.contains("nur für selbst gefahrene"))
        assertEquals(UploadState.NOT_UPLOADED, RouteUploads.state(context, id).state)
    }
    @Test fun oldFitOriginIsKnownOnlyFromExplicitBooleanAndUnknownRemainsUnknown() {
        val directory = File(context.filesDir, "fit-library").apply { mkdirs() }
        val id = "imported-${"a".repeat(64)}"
        val item = JSONObject().put("id", id).put("name", "Legacy").put("hasRoute", false).put("details", RouteDetails().json())
        val index = File(directory, "index.json")
        fun readEntry(): StoredFit { index.writeText(JSONObject().put("entries", org.json.JSONArray().put(item)).toString()); return FitLibrary(context.filesDir).apply { load() }.entries.single() }
        assertEquals(TrackOrigin.UNKNOWN, readEntry().origin)
        item.put("recorded", true); assertEquals(TrackOrigin.RECORDED, readEntry().origin)
        item.put("recorded", false); assertEquals(TrackOrigin.IMPORTED, readEntry().origin)
        item.put("origin", "bad"); assertEquals(TrackOrigin.UNKNOWN, readEntry().origin)
    }
    @Test fun importedFitCannotEnterCloudQueueAndRecordedUploadIsVerifiedAndIndependent() {
        RouteExportSettings(context).write(RouteExportConfig(webdav = true, server = "https://cloud.example.org", user = "rider", wifiOnly = false))
        fun file(prefix: String, letter: String) = File(context.filesDir, "fit-library/$prefix-${letter.repeat(64)}.fit").apply { parentFile!!.mkdirs(); writeText("FIT-$letter") }
        val imported = file("imported", "a")
        assertThrows(IllegalArgumentException::class.java) { RouteUploads.enqueue(context, imported, "imported.fit") }
        assertThrows(IllegalArgumentException::class.java) { RouteUploads.retry(context, imported.nameWithoutExtension) }
        val first = file("recorded", "b"); val second = file("recorded", "c")
        RouteUploads.enqueue(context, first, "first.fit"); RouteUploads.enqueue(context, second, "second.fit")
        var uploads = 0
        RouteUploads.run(context, { false }) { _, source, _, _ ->
            uploads++
            if (source == first) {
                RouteUploads.retry(context, first.nameWithoutExtension) // Does not reset running upload.
                RouteUploads.enqueue(context, first, "first.fit")
            } else throw DavFailure(false, "Zugang prüfen")
        }
        assertEquals(2, uploads)
        fun stamp(file: File) = RouteFileStamp(file.length(), file.lastModified())
        assertEquals(UploadState.UPLOADED, RouteUploads.state(context, first.nameWithoutExtension, stamp(first)).state)
        assertEquals(UploadState.FAILED, RouteUploads.state(context, second.nameWithoutExtension, stamp(second)).state)
        assertFalse(RouteUploads.run(context, { false }) { _, _, _, _ -> fail("No duplicate upload") })
        first.appendText("changed")
        assertEquals(UploadState.NOT_UPLOADED, RouteUploads.state(context, first.nameWithoutExtension, stamp(first)).state)
        RouteUploads.retry(context, first.nameWithoutExtension)
        RouteUploads.run(context, { false }) { _, _, _, _ -> uploads++ }
        assertEquals(3, uploads)
        assertEquals(UploadState.UPLOADED, RouteUploads.state(context, first.nameWithoutExtension, stamp(first)).state)
    }
    @Test fun fileChangingDuringUploadCannotGetSuccessfulState() {
        RouteExportSettings(context).write(RouteExportConfig(webdav = true, server = "https://cloud.example.org", user = "rider", wifiOnly = false))
        val file = File(context.filesDir, "fit-library/recorded-${"d".repeat(64)}.fit").apply { parentFile!!.mkdirs(); writeText("first") }
        RouteUploads.enqueue(context, file, "ride.fit")
        RouteUploads.run(context, { false }) { _, source, _, _ -> source.appendText("new") }
        assertEquals(UploadState.FAILED, RouteUploads.state(context, file.nameWithoutExtension).state)
    }
    @Test fun changedTargetQueuedDuringUploadCannotInheritOldDestinationSuccess() {
        val config = RouteExportConfig(webdav = true, server = "https://cloud.example.org", user = "rider", wifiOnly = false)
        val settings = RouteExportSettings(context); settings.write(config)
        val file = File(context.filesDir, "fit-library/recorded-${"e".repeat(64)}.fit").apply { parentFile!!.mkdirs(); writeText("same") }
        RouteUploads.enqueue(context, file, "ride.fit")
        assertTrue(RouteUploads.run(context, { false }) { _, _, _, _ ->
            settings.write(config.copy(server = "https://other.example.org"))
            RouteUploads.retry(context, file.nameWithoutExtension)
        })
        assertEquals(UploadState.WAITING, RouteUploads.state(context, file.nameWithoutExtension).state)
        assertFalse(RouteUploads.run(context, { false }) { destination, _, _, _ -> assertEquals("https://other.example.org", destination.server) })
        assertEquals(UploadState.UPLOADED, RouteUploads.state(context, file.nameWithoutExtension).state)
    }

}

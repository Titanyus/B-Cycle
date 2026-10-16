package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FitLibraryTest {
    private val start = 1_791_626_400_000L
    private fun fit(root: File, gps: Boolean = true): File {
        val journal = File(root, "sample.ride")
        RideJournal(journal, start, RecordingConfig(if (gps) setOf("GPS_POSITION", "SPEED", "BIKE_DISTANCE", "GPS_ASCENT") else setOf("HEART_RATE"))).use {
            for (i in 0..10) it.append(RideSample(start + i * 1000, i * 1000L,
                if (gps) 50.0 + i * .0001 else null, if (gps) 8.0 else null,
                mapOf("SPEED" to 7.0, "BIKE_DISTANCE" to i * 7.0, "HEART_RATE" to 140.0, "GPS_ASCENT" to i * 3.0)))
        }
        return File(root, "sample.fit").also { FitWriter.encode(journal, it) }
    }
    @Test fun finalizedRecordingsAreRecoveredFromOriginalFilesAndDecodeOfficialUnitsWithoutGpxConversion() {
        val root = Files.createTempDirectory("fit-library").toFile()
        try {
            val original = fit(root); val bytes = original.readBytes()
            FitLibrary.retainRecording(root, original)
            val library = FitLibrary(root); library.load()
            val entry = library.entries.single()
            assertTrue(entry.recorded); assertTrue(entry.hasRoute)
            assertEquals(start, entry.details.dateMs)
            assertEquals(70.0, entry.details.distanceM!!, .001)
            assertEquals(10000L, entry.details.durationMs)
            assertEquals(10000L, entry.details.movingMs)
            assertEquals(25.2, entry.details.movingAverageKmh!!, .001)
            assertEquals(entry.details.movingMs, RouteDetails.from(entry.details.json()).movingMs)
            assertEquals(30.0, entry.details.ascentM!!, .01)
            assertEquals(TrackOrigin.RECORDED, entry.origin)
            val decoded = FitRead.read(library.file(entry.id))
            assertEquals(11, decoded.route!!.originalPoints)
            assertEquals(50.001, decoded.route.points.last().latitude, .000001)
            library.rename(entry.id, "Meine Runde")
            val restarted = FitLibrary(root); restarted.load()
            assertEquals("Meine Runde", restarted.entries.single().name)
            assertEquals(entry.details.movingMs, restarted.entries.single().details.movingMs)
            assertArrayEquals(bytes, restarted.file(entry.id).readBytes())
            assertEquals(0, File(root, "fit-library").listFiles()!!.count { it.extension == "gpx" })
        } finally { root.deleteRecursively() }
    }
    @Test fun importsDeduplicateExactBytesAndDeleteNeverTouchesTheExternalOriginalOrResurrectsIt() {
        val root = Files.createTempDirectory("fit-library").toFile()
        try {
            val original = fit(root); val bytes = original.readBytes()
            val library = FitLibrary(root); library.load()
            val imported = original.inputStream().use { library.add(it, "Geplant.fit", false) }!!
            original.inputStream().use { library.add(it, "Nochmals.fit", false) }
            assertEquals(1, library.entries.size); assertFalse(imported.recorded)
            library.delete(imported.id)
            assertArrayEquals(bytes, original.readBytes()); assertTrue(library.entries.isEmpty())
            val restarted = FitLibrary(root); restarted.load()
            assertNull(original.inputStream().use { restarted.add(it, "Alte Fahrt", true, automatic = true) })
            assertTrue(restarted.entries.isEmpty())
            assertNotNull(original.inputStream().use { restarted.add(it, "Bewusst wieder importiert", false) })
        } finally { root.deleteRecursively() }
    }
    @Test fun correctedDownloadsCopyIsTheSameRideAndNeverCreatesASecondEntry() {
        val root=Files.createTempDirectory("fit-corrected-dedup").toFile()
        try {
            val original=fit(root);val raw=original.readBytes()
            val retained=FitLibrary.retainRecording(root,original)
            val library=FitLibrary(root);library.load()
            val entry=library.entries.single();library.rename(entry.id,"Meine Fahrt")
            val corrected=FitLibrary.correctedFile(root,entry.id).apply { parentFile!!.mkdirs() }
            // A different valid FIT represents the corrected export; matching is exact bytes,
            // never merely date/name/distance, so separate real rides remain separate.
            fit(root,false).copyTo(corrected)
            val imported=corrected.inputStream().use { library.add(it,"B-Cycle-export.fit",true,automatic=true) }!!
            assertEquals(entry.id,imported.id);assertEquals("Meine Fahrt",imported.name)
            assertEquals(1,library.entries.size);assertArrayEquals(raw,retained.readBytes())
            val reopened=FitLibrary(root);reopened.load()
            corrected.inputStream().use { reopened.add(it,"Again.fit",false) }
            assertEquals(1,reopened.entries.size)
            // Re-correction invalidates the cached content identity.
            corrected.writeBytes(raw);library.refreshDetails(entry.id)
            val different=fit(root,false)
            different.inputStream().use { library.add(it,"Other ride.fit",false) }
            assertEquals(2,library.entries.size)
        } finally { root.deleteRecursively() }
    }
    @Test fun invalidFitCannotReplaceExistingEntriesAndSensorOnlyActivitiesHaveNoMapAction() {
        val root = Files.createTempDirectory("fit-library").toFile()
        try {
            val original = fit(root, false)
            val library = FitLibrary(root); library.load()
            original.inputStream().use { library.add(it, "Ohne GPS.fit", true) }
            assertFalse(library.entries.single().hasRoute)
            assertNull(FitRead.read(original).route)
            val before = library.entries
            val damaged = original.readBytes().also { it[it.lastIndex] = (it.last() + 1).toByte() }
            assertThrows(Exception::class.java) { library.add(damaged.inputStream(), "Defekt.fit", false) }
            assertEquals(before, library.entries)
            assertArrayEquals(original.readBytes(), library.file(before.single().id).readBytes())
            assertEquals(0, File(root, "fit-library").listFiles()!!.count { it.extension == "tmp" })
        } finally { root.deleteRecursively() }
    }
}

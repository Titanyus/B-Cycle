package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteArchiveImportTest {
    private val start = 1_791_626_400_000L
    private val xml = "<gpx><trk><trkseg><trkpt lat='50' lon='8'><time>2026-10-11T10:00:00Z</time></trkpt>" +
        "<trkpt lat='50.01' lon='8.01'><time>2026-10-11T10:02:00Z</time></trkpt></trkseg></trk></gpx>"
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip -> entries.forEach { (name, data) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry()
        } }
    }.toByteArray()
    private fun gzip(data: ByteArray) = ByteArrayOutputStream().also { bytes -> GZIPOutputStream(bytes).use { it.write(data) } }.toByteArray()
    private fun fit(root: File): ByteArray {
        val journal = File(root, "sample.ride")
        RideJournal(journal, start, RecordingConfig(setOf("HEART_RATE"))).use {
            for (i in 0..10) it.append(RideSample(start + i * 1000, i * 1000L, null, null, mapOf("HEART_RATE" to 140.0)))
        }
        return File(root, "sample.fit").also { FitWriter.encode(journal, it) }.readBytes()
    }
    private fun withLibrary(action: (File, File, FitLibrary, GpxLibrary) -> Unit) {
        val root = Files.createTempDirectory("archive-import").toFile()
        try {
            val cache = File(root, "cache").apply { mkdirs() }
            val fits = FitLibrary(root).also { it.load() }; val gpx = GpxLibrary(root).also { it.load() }
            action(root, cache, fits, gpx)
            assertTrue("Temporary extracted files cleaned", cache.listFiles()!!.isEmpty())
        } finally { root.deleteRecursively() }
    }
    @Test fun mixedArchivePreservesOriginalsDatesSelectionAndIndependentFailures() = withLibrary { root, cache, fits, gpx ->
        gpx.add(xml.byteInputStream(), "Displayed.gpx")
        val selected = gpx.selected
        val bytes = fit(root)
        val archive = zip("activities/ride.fit.gz" to gzip(bytes), "activities/ride-copy.FIT" to bytes,
            "activities/different.gpx.gz" to gzip(xml.replace("50.01", "50.02").toByteArray()),
            "bad.fit" to byteArrayOf(1, 2, 3), "activities.csv" to "filename,name".toByteArray(),
            "activities/old.gpx" to xml.toByteArray())
        val progress = mutableListOf<Int>()
        val result = RouteArchiveImport(root, cache, fits, gpx).read(archive.inputStream(), progress::add)
        assertEquals(ArchiveImportResult(2, 2, 1, 1), result)
        assertEquals(selected, gpx.selected)
        assertEquals(start, fits.entries.single().details.dateMs)
        assertFalse(fits.entries.single().recorded)
        assertEquals(TrackOrigin.IMPORTED, fits.entries.single().origin)
        assertArrayEquals(bytes, fits.file(fits.entries.single().id).readBytes())
        val archivedGpx = gpx.entries.single { it.name == "different" }
        assertEquals(java.time.Instant.parse("2026-10-11T10:00:00Z").toEpochMilli(), archivedGpx.details.dateMs)
        assertEquals(1, FitLibrary(root).also { it.load() }.entries.size)
        assertEquals(2, GpxLibrary(root).also { it.load() }.entries.size)
        assertEquals(5, progress.last())
        val repeat = RouteArchiveImport(root, cache, fits, gpx).read(archive.inputStream())
        assertEquals(0, repeat.imported); assertEquals(4, repeat.duplicates)
    }
    @Test fun invalidFileDoesNotBlockFollowingRideAndPathsNeverEscapePrivateLibrary() = withLibrary { root, cache, fits, gpx ->
        val result = RouteArchiveImport(root, cache, fits, gpx).read(zip(
            "bad.gpx.gz" to byteArrayOf(1, 2), "../../escape.gpx" to xml.toByteArray(),
            "/absolute/other.gpx" to xml.replace("50.01", "50.02").toByteArray()).inputStream())
        assertEquals(ArchiveImportResult(imported = 2, failed = 1), result)
        assertEquals(setOf("escape", "other"), gpx.entries.map { it.name }.toSet())
        assertFalse(File(root, "escape.gpx").exists()); assertNull(gpx.selected)
    }
    @Test fun oversizedMemberIsSkippedWithoutLosingNextRide() = withLibrary { root, cache, fits, gpx ->
        val result = RouteArchiveImport(root, cache, fits, gpx,
            RouteArchiveImport.Limits(fileBytes = 1024)).read(zip(
            "large.fit" to ByteArray(1025), "next.gpx" to xml.toByteArray()).inputStream())
        assertEquals(ArchiveImportResult(imported = 1, failed = 1), result)
    }
    @Test fun totalExpansionLimitStopsGzipBombAndRetainsCompletedRides() = withLibrary { root, cache, fits, gpx ->
        val result = RouteArchiveImport(root, cache, fits, gpx,
            RouteArchiveImport.Limits(totalBytes = 4096)).read(zip(
            "ok.gpx" to xml.toByteArray(), "large.fit.gz" to gzip(ByteArray(100_000))).inputStream())
        assertEquals(1, result.imported); assertTrue(result.interrupted); assertEquals(1, gpx.entries.size)
    }
    @Test fun ignoredLargeFilesAndEntryCountAlsoHaveLimits() = withLibrary { root, cache, fits, gpx ->
        val large = RouteArchiveImport(root, cache, fits, gpx, RouteArchiveImport.Limits(totalBytes = 100)).read(
            zip("photo.jpg" to ByteArray(101), "next.gpx" to xml.toByteArray()).inputStream())
        assertTrue(large.interrupted); assertEquals(0, large.imported)
        val many = RouteArchiveImport(root, cache, fits, gpx, RouteArchiveImport.Limits(entries = 1)).read(
            zip("ok.gpx" to xml.toByteArray(), "next.gpx" to xml.toByteArray()).inputStream())
        assertTrue(many.interrupted); assertEquals(1, many.imported)
    }
    @Test fun invalidArchiveIsReportedAndEmptyZipHasNoInventedFailures() = withLibrary { root, cache, fits, gpx ->
        val importer = RouteArchiveImport(root, cache, fits, gpx)
        assertTrue(importer.read("not a zip".byteInputStream()).interrupted)
        assertEquals(ArchiveImportResult(), importer.read(zip().inputStream()))
    }
    @Test fun repeatedFitKeepsExistingNameAndRecordedOrigin() = withLibrary { root, cache, fits, gpx ->
        val bytes = fit(root)
        val entry = fits.add(bytes.inputStream(), "My own ride.fit", true)!!
        val result = RouteArchiveImport(root, cache, fits, gpx).read(zip("rename.fit" to bytes).inputStream())
        assertEquals(1, result.duplicates); assertEquals(entry, fits.entries.single())
    }
    @Test fun preparationIsReadOnlyAndOnlySelectedRidesAreCommittedWithCleanupAndDuplicateDetection() = withLibrary { root,cache,fits,gpx ->
        gpx.add(xml.byteInputStream(),"Schon vorhanden")
        val selected=gpx.selected
        val bytes=zip("../../old.gpx" to xml.toByteArray(),"nested/new.gpx.gz" to gzip(xml.replace("50.01","50.02").toByteArray()),
            "ride.fit" to fit(root),"bad.fit" to byteArrayOf(1,2),"notes.txt" to byteArrayOf(1))
        val preview=RouteArchiveImport(root,cache,fits,gpx).prepare(bytes.inputStream())
        try {
            assertEquals(3,preview.entries.size);assertEquals(1,preview.entries.count { it.known })
            assertEquals(1,preview.report.failed);assertEquals(1,preview.report.unsupported)
            assertTrue(fits.entries.isEmpty());assertEquals(1,gpx.entries.size);assertEquals(selected,gpx.selected)
            val chosen=preview.entries.single { it.name=="new" }
            assertTrue(chosen.file.canonicalPath.startsWith(cache.canonicalPath+"/"))
            assertEquals("GPX",chosen.selection().fileFormat)
            val result=preview.importSelection(fits,gpx,setOf(chosen.id),{false},{})
            assertEquals(1,result.count(DreeveImportState.IMPORTED))
            assertEquals(2,gpx.entries.size);assertTrue(fits.entries.isEmpty());assertEquals(selected,gpx.selected)
            assertEquals(1,preview.importSelection(fits,gpx,setOf(chosen.id),{false},{}).count(DreeveImportState.DUPLICATE))
        } finally { preview.close() }
    }
    @Test fun cancelledPreparationNeverImportsAndDiscardRemovesStagedFiles() = withLibrary { root,cache,fits,gpx ->
        val preview=RouteArchiveImport(root,cache,fits,gpx).prepare(zip("ride.gpx" to xml.toByteArray()).inputStream(),{true})
        try {
            assertTrue(preview.report.interrupted);assertTrue(preview.entries.isEmpty())
            assertTrue(fits.entries.isEmpty());assertTrue(gpx.entries.isEmpty())
        } finally { preview.close() }
        val completed=RouteArchiveImport(root,cache,fits,gpx).prepare(zip("ride.gpx" to xml.toByteArray()).inputStream())
        assertEquals(1,completed.entries.size);completed.close()
        assertTrue(fits.entries.isEmpty());assertTrue(gpx.entries.isEmpty())
    }
}

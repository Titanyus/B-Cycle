package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GpxLibraryTest {
    private val xml = "<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>"
    @Test fun metadataAndRenameSurviveRestartWithoutChangingOriginalGpxBytes() {
        val root = Files.createTempDirectory("gpx-metadata").toFile()
        try {
            val text = "<gpx><trk><trkseg><trkpt lat='50' lon='8'><time>2026-10-11T10:00:00Z</time></trkpt>" +
                "<trkpt lat='50.01' lon='8.01'><time>2026-10-11T10:02:00Z</time></trkpt></trkseg>" +
                "<trkseg><trkpt lat='51' lon='9'/><trkpt lat='51.01' lon='9.01'/></trkseg></trk></gpx>"
            val library = GpxLibrary(root); library.load(); library.add(text.byteInputStream(), "Runde.gpx")
            val entry = library.entries.single()
            assertTrue(entry.details.distanceM!! in 2600.0..2700.0) // No invented line across the segment gap.
            assertEquals(120000L, entry.details.durationMs)
            library.rename(entry.id, "Neue Runde")
            val restarted = GpxLibrary(root); restarted.load()
            assertEquals("Neue Runde", restarted.entries.single().name)
            assertEquals(entry.details, restarted.entries.single().details)
            assertEquals(text, restarted.file(entry.id).readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun storesMultipleOriginalsSwitchesVisibilityAndRestoresAfterRestart() {
        val root = Files.createTempDirectory("gpx-test").toFile()
        try {
            val library = GpxLibrary(root); assertNull(library.load())
            library.add(xml.byteInputStream(), "Erste.gpx")
            val first = library.entries.single().id
            library.add(xml.replace("50.01", "51.01").byteInputStream(), "Zweite.gpx")
            val second = library.selected
            assertEquals(listOf("Erste", "Zweite"), library.entries.map { it.name })
            val restarted = GpxLibrary(root)
            assertEquals(51.01, restarted.load()!!.points.last().latitude, .00001)
            assertEquals(second, restarted.selected)
            restarted.select(first)
            assertEquals(50.01, GpxLibrary(root).load()!!.points.last().latitude, .00001)
            restarted.select(null)
            val hidden = GpxLibrary(root); assertNull(hidden.load()); assertEquals(2, hidden.entries.size)
            hidden.select(first); hidden.delete(second!!)
            assertEquals(first, hidden.selected)
            hidden.delete(first)
            assertNull(GpxLibrary(root).load())
            assertTrue(hidden.entries.isEmpty())
            assertEquals(0, File(root, "gpx-library").listFiles()!!.count { it.extension == "gpx" })
        } finally { root.deleteRecursively() }
    }
    @Test fun invalidImportRetainsLibrarySelectionAndOriginalFile() {
        val root = Files.createTempDirectory("gpx-test").toFile()
        try {
            val library = GpxLibrary(root); library.load(); library.add(xml.byteInputStream(), "Erste.gpx")
            val before = library.entries
            assertThrows(Exception::class.java) { library.add("<gpx/>".byteInputStream(), "Defekt.gpx") }
            assertEquals(before, library.entries)
            val restored = GpxLibrary(root); assertNotNull(restored.load()); assertEquals(before, restored.entries)
            assertEquals(before.single().id, restored.selected)
            assertEquals(1, File(root, "gpx-library").listFiles()!!.count { it.extension == "gpx" })
        } finally { root.deleteRecursively() }
    }
    @Test fun migratesExistingSingleRouteOnceWithoutDiscardingIt() {
        val root = Files.createTempDirectory("gpx-test").toFile()
        try {
            File(root, "map-route.gpx").writeText(xml)
            val library = GpxLibrary(root)
            assertEquals(2, library.load()!!.originalPoints)
            assertEquals("Bisherige GPX-Strecke", library.entries.single().name)
            assertFalse(File(root, "map-route.gpx").exists())
            val restored = GpxLibrary(root); assertNotNull(restored.load()); assertEquals(library.entries, restored.entries)
        } finally { root.deleteRecursively() }
    }
}

package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MapDataTest {
    private fun read(xml: String) = xml.byteInputStream().use { GpxImport.read(it) }

    @Test fun gpxKeepsTracksRoutesAndSegmentGapsWithNamespace() {
        val route = read("""<gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1"><trk>
            <trkseg><trkpt lat="50" lon="8"/><trkpt lat="50.01" lon="8.01"/></trkseg>
            <trkseg><trkpt lat="51" lon="9"/><trkpt lat="51.01" lon="9.01"/></trkseg></trk>
            <rte><rtept lat="52" lon="10"/><rtept lat="52.01" lon="10.01"/></rte></gpx>""")
        assertEquals(6, route.originalPoints); assertEquals(3, route.segments)
        assertEquals(listOf(true, false, true, false, true, false), route.points.map { it.segmentStart })
        assertEquals(50.0, route.points.first().latitude, 0.0)
        assertEquals(52.01, route.points.last().latitude, 0.0)
    }

    @Test fun gpxRejectsWrongFormatBrokenCoordinatesAndExternalEntities() {
        val invalid = listOf("<notgpx/>", "<gpx/>", "<gpx><trk><trkseg><trkpt lat='NaN' lon='8'/></trkseg></trk></gpx>",
            "<gpx><rte><rtept lat='100' lon='8'/><rtept lat='50' lon='8'/></rte></gpx>",
            "<gpx><rte><rtept lat='50' lon='8'/>",
            "<!DOCTYPE gpx [<!ENTITY x SYSTEM 'file:///does-not-exist'>]><gpx><name>&x;</name></gpx>")
        invalid.forEach { xml -> assertThrows(Exception::class.java) { read(xml) } }
    }

    @Test fun largeGpxBoundsDisplayedPointsAndRetainsEverySegmentsEndpoints() {
        val xml = buildString {
            append("<gpx><trk><trkseg>")
            repeat(60_000) { append("<trkpt lat='50' lon='${8 + it / 1_000_000.0}'/>") }
            append("</trkseg><trkseg><trkpt lat='51' lon='9'/><trkpt lat='51.1' lon='9.1'/></trkseg></trk></gpx>")
        }
        val route = read(xml)
        assertEquals(60_002, route.originalPoints)
        assertTrue(route.points.size <= 20_004)
        val secondStart = route.points.indexOfFirst { it.latitude == 51.0 }
        assertTrue(route.points[secondStart].segmentStart)
        assertEquals(8.059999, route.points[secondStart - 1].longitude, 1e-9)
        assertEquals(51.1, route.points.last().latitude, 0.0)
    }

    @Test fun trackDoesNotDrawAcrossMissingPositionsAndDoesNotGrowWithoutLimit() {
        val track = MapTrack(32)
        fun sample(i: Int, latitude: Double? = 50 + i * .00005) = RideSample(i * 1000L, i * 1000L, latitude, 8.0, mapOf("GPS_SPEED" to 5.0))
        repeat(20) { track.append(sample(it)) }
        track.append(sample(20, null))
        track.append(sample(21)); assertTrue(track.points.last().segmentStart)
        repeat(500) { track.append(sample(it + 22)) }
        assertTrue(track.points.size <= 32)
        assertTrue(track.points.first().segmentStart)
        assertTrue(track.points.count { it.segmentStart } >= 2)
        val count = track.points.size
        track.append(sample(1)); assertEquals(count, track.points.size)
        track.clear(); assertTrue(track.points.isEmpty())
    }

    @Test fun standingNoiseAddsNoTrackAndLongGapStartsNewSegment() {
        val track = MapTrack()
        track.append(RideSample(0, 0, 50.0, 8.0))
        track.append(RideSample(1000, 1000, 50.00005, 8.0, mapOf("GPS_SPEED" to 0.0)))
        assertEquals(1, track.points.size)
        track.append(RideSample(20000, 20000, 50.001, 8.0, mapOf("GPS_SPEED" to 5.0)))
        assertTrue(track.points.last().segmentStart)
    }

    @Test fun courseSmoothingCrossesNorthWithoutTurningSouthAndIgnoresBadAccuracy() {
        val ride = GpsRide()
        assertTrue(ride.accept(GpsFix(1000, 50.0, 8.0, 3.0, 5.0, .2, bearingDegrees = 359.0, bearingAccuracyDegrees = 3.0), 1000))
        assertTrue(ride.accept(GpsFix(2000, 50.000045, 8.0, 3.0, 5.0, .2, bearingDegrees = 1.0, bearingAccuracyDegrees = 3.0), 2000))
        val course = ride.snapshot(2000).bearingDegrees!!
        assertTrue(course > 350 || course < 10)
        ride.accept(GpsFix(3000, 50.000045, 8.0, 3.0, 0.0, .2, bearingDegrees = 180.0, bearingAccuracyDegrees = 3.0), 3000)
        assertEquals(course, ride.snapshot(3000).bearingDegrees!!, .001)
        assertNull(ride.snapshot(9000).bearingDegrees)
        ride.pause(); assertNull(ride.snapshot(9000).bearingDegrees)
    }
}

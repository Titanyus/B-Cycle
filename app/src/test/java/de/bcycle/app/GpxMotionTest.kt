package de.bcycle.app

import java.time.Instant
import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class GpxMotionTest {
    private val start=Instant.parse("2026-01-01T00:00:00Z")
    private fun gpx(repeated:Boolean)=buildString {
        append("<gpx><trk><trkseg>")
        for(i in 0..120) {
            if(!repeated && i%2!=0) continue
            val distance=(if(repeated) i/2*2 else i)*5.0
            append("<trkpt lat='0' lon='${distance/111194.92664455874}'><time>${start.plusSeconds(i.toLong())}</time></trkpt>")
        }
        append("</trkseg></trk></gpx>")
    }
    @Test fun repeatedPositionsDoNotHalveMovingTimeAndSpeedRemainsNearActualPace() {
        val samples=mutableListOf<GpxMeasurement>()
        val route=GpxImport.samples(gpx(true).byteInputStream()) { samples+=it }
        assertTrue(route.movingMs!! in 115_000..120_000)
        assertTrue(route.distanceM!!*3600/route.movingMs!! in 17.9..19.0)
        assertTrue(samples.drop(12).all { it.speed!! in 4.4..5.6 })
        val original=GpxImport.read(gpx(false).byteInputStream())
        assertEquals(original.distanceM!!,route.distanceM!!,.001)
    }
    @Test fun pausesSegmentsAndMissingOrBackwardTimeResetWindowAndNativeSpeedWins() {
        val motion=GpxMotion()
        fun s(t:Long?,x:Double,native:Double?=null,segment:Boolean=false)=GpxMeasurement(MapPoint(0.0,x/111194.92664455874,segment),t,null,native,null,null,null,null)
        motion.speed(s(0,0.0));assertEquals(5.0,motion.speed(s(1000,5.0))!!,.001)
        assertEquals(2.0,motion.speed(s(2000,30.0,2.0))!!,.001)
        assertEquals(0.0,motion.speed(s(60000,30.0))!!,.001)
        assertEquals(0.0,motion.speed(s(61000,3000.0,segment=true))!!,.001)
        assertNull(motion.speed(s(null,3001.0)))
        assertEquals(0.0,motion.speed(s(50000,3002.0))!!,.001)
    }
    @Test fun explicitGpx10SpeedHasPriorityOverPositionDerivative() {
        val samples=mutableListOf<GpxMeasurement>()
        val xml="<gpx xmlns='http://www.topografix.com/GPX/1/0'><trk><trkseg>"+
            "<trkpt lat='0' lon='0'><time>2026-01-01T00:00:00Z</time><speed>2</speed></trkpt>"+
            "<trkpt lat='0' lon='0.0001'><time>2026-01-01T00:00:01Z</time><speed>2</speed></trkpt>"+
            "</trkseg></trk></gpx>"
        GpxImport.samples(xml.byteInputStream()) { samples+=it }
        assertEquals(listOf(2.0,2.0),samples.map { it.speed })
    }
    @Test fun existingGpxMovingMetadataIsRefreshedWithoutChangingBytesIdentityOrOrigin() {
        val root=Files.createTempDirectory("gpx-motion").toFile()
        try {
            val raw=gpx(true);val library=GpxLibrary(root).apply { load();add(raw.byteInputStream(),"Fahrt",select=false,preserveDate=true,origin=TrackOrigin.RECORDED) }
            val entry=library.entries.single();val index=java.io.File(root,"gpx-library/index.json")
            val json=JSONObject(index.readText());json.remove("motionVersion")
            json.getJSONArray("entries").getJSONObject(0).getJSONObject("details").put("moving",60000)
            index.writeText(json.toString())
            val reopened=GpxLibrary(root).apply { load() };val repaired=reopened.entries.single()
            assertTrue(repaired.details.movingMs!!>115000)
            assertEquals(entry.id,repaired.id);assertEquals(entry.name,repaired.name);assertEquals(entry.origin,repaired.origin)
            assertEquals(entry.details.dateMs,repaired.details.dateMs);assertEquals(raw,reopened.file(entry.id).readText())
        } finally { root.deleteRecursively() }
    }
}

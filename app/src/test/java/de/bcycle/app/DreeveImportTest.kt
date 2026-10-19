package de.bcycle.app

import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DreeveImportTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val target="https://dreeve.example.org/sub/"
    private fun activity(id:String="activity-1",hasGpx:Boolean=true)=DreeveActivity(id,"Morgen / Pendeln","2026-10-19T08:00:00",1000.0,"Ride",hasGpx)
    private fun page(page:Int=1,pages:Int=2)=JSONObject().put("activities",JSONArray().put(JSONObject()
        .put("id","activity-$page").put("name","Runde $page").put("startDateLocal","2026-10-19T08:00:00")
        .put("distanceInMeter",1000).put("sportType","Ride").put("hasGpx",true)))
        .put("pagination",JSONObject().put("page",page).put("size",50).put("total",2).put("totalPages",pages))
    private fun gpx()="""<gpx xmlns="http://www.topografix.com/GPX/1/1" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1"><trk><trkseg>
        <trkpt lat="48" lon="10"><ele>500</ele><time>2026-10-19T06:00:00Z</time><extensions><power>0</power><gpxtpx:TrackPointExtension><gpxtpx:hr>120</gpxtpx:hr><gpxtpx:cad>0</gpxtpx:cad><gpxtpx:atemp>15</gpxtpx:atemp></gpxtpx:TrackPointExtension></extensions></trkpt>
        <trkpt lat="48.00001" lon="10.00001"><ele>501</ele><time>2026-10-19T06:00:01Z</time><extensions><power>200</power><gpxtpx:TrackPointExtension><gpxtpx:hr>130</gpxtpx:hr><gpxtpx:cad>80</gpxtpx:cad><gpxtpx:atemp>16</gpxtpx:atemp></gpxtpx:TrackPointExtension></extensions></trkpt>
        <trkpt lat="48.00002" lon="10.00002"><time>2026-10-19T06:00:02Z</time></trkpt>
        </trkseg></trk></gpx>"""
    @Test fun listAndDownloadUseDocumentedPaginationAuthAndSafeSubpath() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(page().toString()))
            val client=DreeveClient(ActivityServiceConfig(),"private-key",base=server.url("/sub/"))
            val rows=client.activities()
            assertEquals("activity-1",rows.activities.single().id);assertEquals(2,rows.totalPages)
            val list=server.takeRequest();val url=list.requestUrl!!
            assertEquals("Bearer private-key",list.getHeader("Authorization"))
            assertEquals("/sub/api/v1/activities",url.encodedPath)
            assertEquals("1",url.queryParameter("pagination[page]"));assertEquals("50",url.queryParameter("pagination[size]"))
            assertTrue(url.queryParameter("filters[sportType]")!!.contains("VirtualRide"))
            server.enqueue(MockResponse().setBody(page(2).toString()))
            assertEquals(2,client.activities(2).page);assertEquals("2",server.takeRequest().requestUrl!!.queryParameter("pagination[page]"))
            server.enqueue(MockResponse().setBody(gpx()))
            val file=File(c.cacheDir,"download.gpx");client.downloadGpx("activity-a/b",file)
            assertEquals(gpx(),file.readText())
            val request=server.takeRequest()
            assertEquals("/sub/api/v1/activities/activity-a%2Fb/gpx",request.requestUrl!!.encodedPath)
            assertEquals("Bearer private-key",request.getHeader("Authorization"))
            assertEquals("application/gpx+xml",request.getHeader("Accept"))
        }
    }
    @Test fun malformedPagesAndMissingGpsAreErrorsAndDownloadsAreBoundedAndCancellable() {
        assertThrows(ActivityServiceFailure::class.java) { DreeveActivityPage.decode(page(2),1) }
        assertThrows(ActivityServiceFailure::class.java) { DreeveActivityPage.decode(page().put("activities",JSONArray().put(JSONObject())),1) }
        MockWebServer().use { server ->
            val file=File(c.cacheDir,"limited.gpx")
            server.enqueue(MockResponse().setResponseCode(404).setBody("gpx_not_available"))
            val client=DreeveClient(ActivityServiceConfig(),"key",base=server.url("/"))
            assertThrows(ActivityServiceFailure::class.java) { client.downloadGpx("activity-1",file) }
            server.enqueue(MockResponse().setBody("a".repeat(32)))
            assertThrows(IOException::class.java) { ActivityHttp().download(Request.Builder().url(server.url("/file")).build(),file,16) }
            val before=server.requestCount
            assertThrows(IOException::class.java) { DreeveClient(ActivityServiceConfig(),"key",{true},server.url("/")).activities() }
            assertEquals(before,server.requestCount)
        }
    }
    @Test fun importsAvailableStreamsKeepsOriginalAndRemoteIdentityAndIsIdempotent() {
        val root=File(c.cacheDir,"library-${UUID.randomUUID()}").apply { mkdirs() }
        val library=GpxLibrary(root);library.load();var downloads=0
        val runner=DreeveImport(c,library,{ library.entries.map { it.id }.toSet() }) { _,file ->downloads++;file.writeText(gpx()) }
        val result=runner.read(target,listOf(activity()))
        assertEquals(1,result.count(DreeveImportState.IMPORTED));assertEquals(DreeveDataField.entries.toSet(),result.items.single().fields)
        val entry=library.entries.single()
        assertEquals("Morgen / Pendeln",entry.name);assertEquals(TrackOrigin.IMPORTED,entry.origin);assertEquals(BikeType.BIKE,entry.details.bikeType)
        assertNull(library.selected);assertEquals(gpx(),library.file(entry.id).readText())
        val link=RideServiceLinks.links(c,entry.id).single()
        assertEquals("activity-1",link.activityId);assertEquals(target,link.accountId);assertEquals(WebDavClient.hash(library.file(entry.id)),link.fileHash)
        assertFalse(RouteUploads.eligible(c,entry.id))
        val analysis=RouteAnalysisRead.read(library.file(entry.id),ManagedRoute(entry.id,entry.name,false,"GPX",entry.details,true))
        assertTrue(analysis.series.single { it.chart==AnalysisChart.POWER }.points.any { it.value==200.0 })
        assertTrue(analysis.series.single { it.chart==AnalysisChart.HEART }.points.any { it.value==130.0 })
        assertTrue(analysis.series.single { it.chart==AnalysisChart.CADENCE }.points.any { it.value==80.0 })
        assertEquals(1,runner.read(target,listOf(activity())).count(DreeveImportState.DUPLICATE));assertEquals(1,downloads)
        assertNotEquals(entry.id,DreeveImport.localId("https://other.example/","activity-1"))
    }
    @Test fun partialFailureDoesNotBlockOtherImportsAndKnownUploadsAreNotRedownloaded() {
        val root=File(c.cacheDir,"batch-${UUID.randomUUID()}").apply { mkdirs() };val library=GpxLibrary(root);library.load()
        val original="recorded-"+"a".repeat(64)
        RideServiceLinks.confirm(c,RideServiceLink(original,RideService.DREEVE,target,"known","b".repeat(64)))
        val requested=mutableListOf<String>()
        val runner=DreeveImport(c,library,{ library.entries.map { it.id }.toSet()+original }) { id,file ->
            requested.add(id);if(id=="bad") throw IOException();file.writeText(gpx())
        }
        val result=runner.read(target,listOf(activity("known"),activity("bad"),activity("ok"),activity("indoor",false)))
        assertEquals(1,result.count(DreeveImportState.IMPORTED));assertEquals(1,result.count(DreeveImportState.DUPLICATE))
        assertEquals(1,result.count(DreeveImportState.FAILED));assertEquals(1,result.count(DreeveImportState.UNAVAILABLE))
        assertEquals(listOf("bad","ok"),requested);assertEquals(1,library.entries.size)
        assertTrue(root.walkTopDown().none { it.name.endsWith(".tmp") })
        assertTrue(runner.read(target,listOf(activity("new")),{true}).interrupted)
    }
    @Test fun extensionValuesDoNotLeakIntoFollowingPointsOrFromUnknownNamespaces() {
        val samples=mutableListOf<GpxMeasurement>()
        GpxImport.samples(gpx().byteInputStream(),samples::add)
        assertEquals(0.0,samples.first().power);assertEquals(0.0,samples.first().cadence)
        assertNull(samples.last().power);assertNull(samples.last().heart);assertNull(samples.last().cadence)
        val hostile=gpx().replace("http://www.garmin.com/xmlschemas/TrackPointExtension/v1","https://unrelated.example/")
        samples.clear();GpxImport.samples(hostile.byteInputStream(),samples::add)
        assertNull(samples.first().heart);assertNull(samples.first().cadence)
    }
}

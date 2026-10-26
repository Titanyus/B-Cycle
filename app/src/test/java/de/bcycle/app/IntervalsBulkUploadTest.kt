package de.bcycle.app

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
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class IntervalsBulkUploadTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val start=1791626400000L
    @Test fun identityUsesActualSessionStartAndElapsedTimeAndMatchesTimezoneAndDistance() {
        val file=File(c.cacheDir,"session-time.fit")
        fun time(ms:Long)=com.garmin.fit.DateTime(java.util.Date(ms))
        val out=com.garmin.fit.FileEncoder(file,com.garmin.fit.Fit.ProtocolVersion.V2_0)
        try {
            out.write(com.garmin.fit.FileIdMesg().apply { setType(com.garmin.fit.File.ACTIVITY);setTimeCreated(time(start+86400_000)) })
            out.write(com.garmin.fit.RecordMesg().apply { setTimestamp(time(start+2000)) })
            out.write(com.garmin.fit.RecordMesg().apply { setTimestamp(time(start+30_000)) })
            out.write(com.garmin.fit.SessionMesg().apply { setStartTime(time(start));setTotalElapsedTime(60f);setTotalTimerTime(30f);setTotalDistance(12000f) })
        } finally { out.close() }
        val key=IntervalsRideIdentity.read(file) {false}
        assertEquals(Instant.ofEpochMilli(start),key.start);assertEquals(60.0,key.seconds,0.0)
        val local=java.time.LocalDateTime.ofInstant(key.start,java.time.ZoneId.of("Europe/Berlin"))
        val row=remote().apply { remove("start_date");put("start_date_local",local.toString());put("timezone","(GMT+02:00) Europe/Berlin");put("distance",12050) }
        assertTrue(IntervalsActivity.decode(JSONArray().put(row).toString()).single().matches(key))
        row.put("distance",13000)
        val changed=IntervalsActivity.decode(JSONArray().put(row).toString()).single()
        assertTrue(changed.nearStart(key));assertFalse(changed.matches(key))
    }
    private fun configure() { c.getSharedPreferences("intervals-settings",0).edit().putBoolean("enabled",true).putString("athlete","i123")
        .putBoolean("wifi",false).putString("password","test-only-cipher").commit() }
    private fun ride(at:Long=start,dreeve:Boolean=true,recorded:Boolean=false):ManagedRoute {
        val journal=File(c.cacheDir,"${UUID.randomUUID()}.ride")
        RideJournal(journal,at,RecordingConfig(setOf("HEART_RATE"),indoor=true)).use { out ->
            repeat(61) { i -> out.append(RideSample(at+i*1000,i*1000L,values=mapOf("HEART_RATE" to 130.0))) }
        }
        val file=File(c.cacheDir,"${UUID.randomUUID()}.fit");FitWriter.encode(journal,file)
        val library=FitLibrary(c.filesDir).apply { load() }
        val entry=file.inputStream().use { library.add(it,"Fahrt $at",recorded,preserveDate=true) }!!
        if(dreeve) RideServiceLinks.confirm(c,RideServiceLink(entry.id,RideService.DREEVE,"https://dreeve.example/","dreeve-$at",WebDavClient.hash(file)))
        return ManagedRoute(entry.id,entry.name,entry.recorded,"FIT",entry.details,entry.hasRoute,entry.origin,true)
    }
    private fun bulk(server:MockWebServer)=IntervalsBulkUpload(c) { config,stop -> IntervalsClient(config,"test-key",stop,server.url("/")) }
    private fun remote(id:String="remote-1",at:Long=start,seconds:Int=60)=JSONObject().put("id",id).put("type","VirtualRide")
        .put("start_date",Instant.ofEpochMilli(at).toString()).put("start_date_local","2026-10-10T12:00:00").put("elapsed_time",seconds)
    @Test fun dreeveImportsAreUploadedOnceWithStableExternalIdAndCorrectActivityFromWrapper() {
        configure();val ride=ride();val bytes=FitLibrary(c.filesDir).originalFile(ride.id).readBytes()
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("[]"))
            server.enqueue(MockResponse().setResponseCode(201).setBody("{\"id\":\"upload-job\",\"activities\":[{\"id\":\"remote-1\"}]}"))
            val report=bulk(server).run(listOf(ride),{false}) { _,_,_-> }
            assertEquals(1,report.count(IntervalsBulkState.UPLOADED))
            val query=server.takeRequest();assertEquals("/athlete/i123/activities",query.requestUrl!!.encodedPath)
            assertNotNull(query.requestUrl!!.queryParameter("oldest"));assertEquals("1000",query.requestUrl!!.queryParameter("limit"))
            val upload=server.takeRequest();assertEquals("POST",upload.method)
            assertEquals("bcycle:${ride.id}",upload.requestUrl!!.queryParameter("external_id"));assertEquals(ride.name,upload.requestUrl!!.queryParameter("name"))
            assertTrue(RideServiceLinks.links(c,ride.id).any { it.service==RideService.INTERVALS && it.activityId=="remote-1" })
            assertEquals(UploadState.UPLOADED,IntervalsUploads.state(c,ride.id).state)
            assertArrayEquals(bytes,FitLibrary(c.filesDir).originalFile(ride.id).readBytes())
            assertEquals(TrackOrigin.IMPORTED,FitLibrary(c.filesDir).apply { load() }.entries.single().origin)
            server.enqueue(MockResponse().setBody("{\"id\":\"remote-1\"}"))
            assertEquals(1,bulk(server).run(listOf(ride),{false}) { _,_,_-> }.count(IntervalsBulkState.EXISTS))
            assertEquals("GET",server.takeRequest().method);assertEquals(3,server.requestCount)
        }
    }
    @Test fun existingUnlinkedRidesAreMatchedWithoutUploadingAndAmbiguousStartsAreNotGuessed() {
        configure();val first=ride();val second=ride(start+3600_000)
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(JSONArray().put(remote()).put(remote("unclear",start+3600_000,45)).toString()))
            val report=bulk(server).run(listOf(first,second),{false}) { _,_,_-> }
            assertEquals(1,report.count(IntervalsBulkState.EXISTS));assertEquals(1,report.count(IntervalsBulkState.UNCLEAR))
            assertEquals(1,server.requestCount)
            assertTrue(RideServiceLinks.links(c,first.id).any { it.service==RideService.INTERVALS })
            assertEquals(UploadState.NOT_UPLOADED,IntervalsUploads.state(c,first.id).state) // Presence is not an exact-file receipt.
            val row=RouteTransferRows.read(c,first,false).single { it.target==RouteTransferTarget.INTERVALS }
            assertTrue(row.completed);assertFalse(row.canStart)
        }
    }
    @Test fun authenticationAndMalformedOrTruncatedResponsesStopBeforeAnyUpload() {
        configure();val first=ride();val second=ride(start+3600_000)
        for(response in listOf(MockResponse().setResponseCode(401),MockResponse().setBody("{}"),
            MockResponse().setBody(JSONArray(List(1000) { remote("id-$it") }).toString()))) {
            MockWebServer().use { server ->
                server.enqueue(response)
                val result=bulk(server).run(listOf(first,second),{false}) { _,_,_-> }
                assertTrue(result.interrupted);assertNotNull(result.problem);assertEquals(1,result.count(IntervalsBulkState.FAILED));assertEquals(1,server.requestCount)
                assertTrue(RideServiceLinks.links(c,first.id).none { it.service==RideService.INTERVALS })
            }
        }
    }
    @Test fun deletedRemoteIsReplacedOnlyAfter404AndCancellationDoesNotUploadTheNextRide() {
        configure();val first=ride();val second=ride(start+3600_000)
        RideServiceLinks.confirm(c,RideServiceLink(first.id,RideService.INTERVALS,"i123","deleted","a".repeat(64)))
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404));server.enqueue(MockResponse().setBody("[]"))
            server.enqueue(MockResponse().setBody("[{\"id\":\"new\"}]"))
            var stop=false
            val result=bulk(server).run(listOf(first,second),{stop}) { _,_,_->stop=true }
            assertTrue(result.interrupted);assertEquals(1,result.count(IntervalsBulkState.UPLOADED));assertEquals(3,server.requestCount)
            assertTrue(RideServiceLinks.links(c,first.id).none { it.activityId=="deleted" })
            assertTrue(RideServiceLinks.links(c,first.id).any { it.activityId=="new" })
        }
    }
    @Test fun importedDreeveGpxUsesRealSamplesWhilePlannedTracksAndSummariesAreSkipped() {
        configure()
        val library=GpxLibrary(c.filesDir).apply { load() }
        val xml="<gpx><trk><trkseg><trkpt lat='50' lon='8'><time>2026-10-10T10:00:00Z</time></trkpt><trkpt lat='50.001' lon='8'><time>2026-10-10T10:01:00Z</time></trkpt></trkseg></trk></gpx>"
        library.add(xml.byteInputStream(),"Dreeve GPX",preserveDate=true)
        val gpx=library.entries.single()
        RideServiceLinks.confirm(c,RideServiceLink(gpx.id,RideService.DREEVE,"https://dreeve.example/","gpx-source",WebDavClient.hash(library.file(gpx.id))))
        val imported=ManagedRoute(gpx.id,gpx.name,false,"GPX",gpx.details,true,TrackOrigin.IMPORTED,true)
        val planned=ride(start+3600_000,dreeve=false)
        val summary=imported.copy(id=UUID.randomUUID().toString(),format="JSON")
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("[]"));server.enqueue(MockResponse().setBody("{\"id\":\"gpx-upload\"}"))
            val result=bulk(server).run(listOf(imported,planned,summary),{false}) { _,_,_-> }
            assertEquals(1,result.count(IntervalsBulkState.UPLOADED));assertEquals(2,result.count(IntervalsBulkState.SKIPPED))
            assertEquals(xml,library.file(gpx.id).readText());assertEquals(TrackOrigin.IMPORTED,GpxLibrary(c.filesDir).apply { load() }.entries.single().origin)
            assertEquals(2,server.requestCount)
        }
    }
    @Test fun recordedOriginalFitWithDreeveLinkShowsCheckEvenWithoutUploadReceiptAndWhenUploadsAreOff() {
        val ride=ride(recorded=true)
        c.getSharedPreferences("activity-service-dreeve",0).edit().putString("address","https://dreeve.example/").commit()
        val row=RouteTransferRows.read(c,ride,false).single { it.target==RouteTransferTarget.DREEVE }
        assertTrue(row.completed);assertFalse(row.canStart);assertEquals(R.string.dreeve_linked,row.text)
        assertEquals(UploadState.NOT_UPLOADED,ActivityServiceUploads.dreeve.state(c,ride.id,null).state)
        c.getSharedPreferences("activity-service-dreeve",0).edit().putString("address","https://other.example/").commit()
        assertFalse(RouteTransferRows.read(c,ride,false).single { it.target==RouteTransferTarget.DREEVE }.completed)
    }
}

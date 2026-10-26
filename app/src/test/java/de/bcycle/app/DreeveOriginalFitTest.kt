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
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DreeveOriginalFitTest {
    @Test fun optionalFitAvailabilityReadsOnlyMetadataAndSupportsPaginationAndOlderServers() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(page(listOf(row(),row("missing","no-file",false)),pages=2).toString()))
            server.enqueue(MockResponse().setBody(page(listOf(row("second","activity-b"),row("gpx","activity-c").put("source","gpxFile")),2,2).toString()))
            val client=DreeveClient(ActivityServiceConfig(),"test",base=server.url("/"))
            assertEquals(setOf("activity-a","activity-b"),client.originalFitActivities())
            repeat(2) { i ->
                val request=server.takeRequest();assertEquals("/api/v1/file-imports",request.requestUrl!!.encodedPath)
                assertEquals("GET",request.method);assertEquals((i+1).toString(),request.requestUrl!!.queryParameter("pagination[page]"))
                assertEquals("fitFile",request.requestUrl!!.queryParameter("filters[source]"))
            }
            server.enqueue(MockResponse().setResponseCode(404));assertTrue(client.originalFitActivities().isEmpty())
            server.enqueue(MockResponse().setBody(page(listOf(row().apply { remove("hasFile") }),pages=9).toString()))
            assertTrue(client.originalFitActivities().isEmpty());assertEquals(4,server.requestCount)
        }
    }
    private val c get()=RuntimeEnvironment.getApplication()
    private val account="https://dreeve.example/sub/"
    private fun row(id:String="fileImport-a",activity:String="activity-a",hasFile:Boolean=true)=JSONObject()
        .put("id",id).put("activityId",activity).put("hasFile",hasFile).put("source","fitFile").put("status","success").put("filename","ride.fit")
    private fun page(rows:List<JSONObject>,page:Int=1,pages:Int=1)=JSONObject().put("fileImports",JSONArray(rows))
        .put("pagination",JSONObject().put("page",page).put("size",100).put("total",rows.size).put("totalPages",pages))
    private fun activity(id:String="activity-a",gpx:Boolean=true)=DreeveActivity(id,"Original / Runde","2026-10-19T08:00:00",1000.0,"EBikeRide",gpx)
    private fun gpx()="<gpx><trk><trkseg><trkpt lat='48' lon='10'><time>2026-10-19T06:00:00Z</time></trkpt><trkpt lat='48.01' lon='10.01'><time>2026-10-19T06:01:00Z</time></trkpt></trkseg></trk></gpx>"
    private fun fit(gps:Boolean=true):File {
        val dir=File(c.cacheDir,UUID.randomUUID().toString()).apply { mkdirs() }
        val start=1_790_000_000_000L
        val fields=setOf("RIDER_POWER","HEART_RATE","CADENCE","SPEED","BIKE_DISTANCE","BATTERY_TEMPERATURE")+if(gps) setOf("GPS_POSITION","GPS_ALTITUDE") else emptySet()
        val journal=File(dir,"ride.ride")
        RideJournal(journal,start,RecordingConfig(fields,indoor=!gps)).use { out -> repeat(65) { i ->
            out.append(RideSample(start+i*1000,i*1000L,if(gps) 48.0+i*.00001 else null,if(gps) 10.0+i*.00001 else null,
                mapOf("RIDER_POWER" to 250.0,"HEART_RATE" to 140.0,"CADENCE" to 90.0,"SPEED" to 5.0,"BIKE_DISTANCE" to i*5.0,"GPS_ALTITUDE" to 500.0,"GPS_ALTITUDE_MSL" to 1.0,"BATTERY_TEMPERATURE" to 24.0)))
        } }
        return File(dir,"ride.fit").also { FitWriter.encode(journal,it) }
    }
    @Test fun originalDownloadFiltersActivityAndSourcePaginatesSkipsForeignFilesAndEscapesIds() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(page(listOf(row(activity="foreign"),row("missing")),pages=2).toString()))
            server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setBody(page(listOf(row("fileImport-a/b")),2,2).toString()))
            val original=fit();server.enqueue(MockResponse().setBody(okio.Buffer().write(original.readBytes())))
            val destination=File(c.cacheDir,"original.fit")
            assertTrue(DreeveClient(ActivityServiceConfig(),"test-key",base=server.url("/sub/")).downloadOriginalFit("activity-a",destination))
            assertArrayEquals(original.readBytes(),destination.readBytes())
            val list=server.takeRequest();assertEquals("/sub/api/v1/file-imports",list.requestUrl!!.encodedPath)
            assertEquals("activity-a",list.requestUrl!!.queryParameter("filters[activityId]"));assertEquals("fitFile",list.requestUrl!!.queryParameter("filters[source]"))
            assertEquals("Bearer test-key",list.getHeader("Authorization"))
            assertEquals("/sub/api/v1/file-imports/missing/file",server.takeRequest().requestUrl!!.encodedPath)
            assertEquals("2",server.takeRequest().requestUrl!!.queryParameter("pagination[page]"))
            val download=server.takeRequest();assertEquals("/sub/api/v1/file-imports/fileImport-a%2Fb/file",download.requestUrl!!.encodedPath)
            assertEquals("application/octet-stream",download.getHeader("Accept"));assertEquals(4,server.requestCount)
        }
    }
    @Test fun unavailableAndLegacyApisFallbackButAuthenticationAndServerErrorsStayErrors() {
        MockWebServer().use { server ->
            val client=DreeveClient(ActivityServiceConfig(),"test",base=server.url("/"));val file=File(c.cacheDir,"unavailable.fit")
            server.enqueue(MockResponse().setResponseCode(404));assertFalse(client.downloadOriginalFit("activity-a",file))
            server.enqueue(MockResponse().setBody(page(listOf(row().apply { remove("hasFile") }),pages=9).toString()))
            assertFalse(client.downloadOriginalFit("activity-a",file));assertEquals(2,server.requestCount)
            server.enqueue(MockResponse().setBody(page(listOf(row(hasFile=false))).toString()));assertFalse(client.downloadOriginalFit("activity-a",file))
            server.enqueue(MockResponse().setResponseCode(401));assertEquals(R.string.service_auth_failed,assertThrows(ActivityServiceFailure::class.java) { client.downloadOriginalFit("activity-a",file) }.problem)
            server.enqueue(MockResponse().setResponseCode(503));assertEquals(R.string.service_server_failed,assertThrows(ActivityServiceFailure::class.java) { client.downloadOriginalFit("activity-a",file) }.problem)
            server.enqueue(MockResponse().setBody(page(listOf(row())).apply { getJSONObject("pagination").put("page",2) }.toString()))
            assertEquals(R.string.service_response_failed,assertThrows(ActivityServiceFailure::class.java) { client.downloadOriginalFit("activity-a",file) }.problem)
            val count=server.requestCount
            assertThrows(IOException::class.java) { DreeveClient(ActivityServiceConfig(),"test",{true},server.url("/")).downloadOriginalFit("activity-a",file) }
            assertEquals(count,server.requestCount)
        }
    }
    @Test fun fitIsPreferredByteForByteForBothOriginsAndIndoorWithoutGpx() {
        for(origin in listOf(TrackOrigin.IMPORTED,TrackOrigin.RECORDED)) {
            val root=File(c.cacheDir,UUID.randomUUID().toString()).apply { mkdirs() };val gpx=GpxLibrary(root).apply { load() };val fits=FitLibrary(root).apply { load() }
            val original=fit(gps=origin==TrackOrigin.IMPORTED);var probes=0
            val importer=DreeveImport(c,gpx,{gpx.entries.map { it.id }.toSet()+fits.entries.map { it.id }},fits=fits,
                downloadFit={ _,file ->probes++;original.copyTo(file,overwrite=true);true }) { _,_->fail("FIT must win over GPX") }
            val selected=activity("activity-${origin.name}",gpx=false)
            val item=importer.read(account,listOf(selected),origin=origin).items.single()
            assertEquals(DreeveImportState.IMPORTED,item.state);assertEquals("FIT",item.fileFormat)
            val entry=fits.entries.single();assertEquals(entry.id,item.localId);assertEquals(origin,entry.origin)
            assertEquals("Original / Runde",entry.name);assertEquals(1_790_000_000_000L,entry.details.dateMs)
            assertArrayEquals(original.readBytes(),fits.originalFile(entry.id).readBytes())
            assertTrue(item.fields.containsAll(setOf(DreeveDataField.TIME,DreeveDataField.POWER,DreeveDataField.HEART,DreeveDataField.CADENCE)))
            assertFalse(DreeveDataField.TEMPERATURE in item.fields) // Battery temperature is not weather.
            assertTrue(gpx.entries.isEmpty());assertEquals(selected.id,RideServiceLinks.links(c,entry.id).single().activityId)
            assertEquals(DreeveImportState.DUPLICATE,importer.read(account,listOf(selected),origin=origin).items.single().state);assertEquals(1,probes)
        }
    }
    @Test fun sameFitAlreadyLocalKeepsNameOriginAndProfileAndOnlyAddsTheRemoteLink() {
        val root=File(c.cacheDir,UUID.randomUUID().toString()).apply { mkdirs() };val gpx=GpxLibrary(root).apply { load() };val fits=FitLibrary(root).apply { load() }
        val original=fit();val old=original.inputStream().use { fits.add(it,"Lokaler Name",false,preserveDate=true) }!!
        RideProfileColor.assign(c,old.id,BikeProfileStore(c,c.getSharedPreferences("bike",0)).current().slot)
        val metadata=RideProfileColor.slot(c,old.id)
        val importer=DreeveImport(c,gpx,{fits.entries.map { it.id }.toSet()},fits=fits,downloadFit={ _,file ->original.copyTo(file,overwrite=true);true }) { _,_->fail("No GPX") }
        val item=importer.read(account,listOf(activity()),origin=TrackOrigin.RECORDED).items.single()
        assertEquals(DreeveImportState.DUPLICATE,item.state);assertEquals(old.id,item.localId)
        assertEquals(old,fits.entries.single());assertEquals(metadata,RideProfileColor.slot(c,old.id))
        assertArrayEquals(original.readBytes(),fits.originalFile(old.id).readBytes());assertEquals("activity-a",RideServiceLinks.links(c,old.id).single().activityId)
    }
    @Test fun missingOrBrokenFitUsesGpxButRealErrorsNeverMasqueradeAsMissingFiles() {
        for(broken in listOf(false,true)) {
            val root=File(c.cacheDir,UUID.randomUUID().toString()).apply { mkdirs() };val gpx=GpxLibrary(root).apply { load() };val fits=FitLibrary(root).apply { load() }
            val selected=activity("fallback-$broken")
            val importer=DreeveImport(c,gpx,{gpx.entries.map { it.id }.toSet()},fits=fits,downloadFit={ _,file ->if(broken) file.writeText("not a fit");broken }) { _,file ->file.writeText(gpx()) }
            val item=importer.read(account,listOf(selected),origin=TrackOrigin.RECORDED).items.single()
            assertEquals(DreeveImportState.IMPORTED,item.state);assertEquals("GPX",item.fileFormat)
            assertEquals(broken,item.fallbackProblem!=null);assertTrue(fits.entries.isEmpty());assertEquals(TrackOrigin.RECORDED,gpx.entries.single().origin)
        }
        val root=File(c.cacheDir,UUID.randomUUID().toString()).apply { mkdirs() };val gpx=GpxLibrary(root).apply { load() };val fits=FitLibrary(root).apply { load() }
        val importer=DreeveImport(c,gpx,{emptySet()},fits=fits,downloadFit={ _,_->throw ActivityServiceFailure(false,R.string.service_auth_failed) }) { _,_->fail("No GPX after authentication failure") }
        val item=importer.read(account,listOf(activity())).items.single();assertEquals(DreeveImportState.FAILED,item.state);assertEquals(R.string.service_auth_failed,item.problem)
        assertTrue(fits.entries.isEmpty());assertTrue(gpx.entries.isEmpty())
    }
    @Test fun cancellationDuringFitRetrievalLeavesNoOriginalOrLinkOrTemporaryFile() {
        val root=File(c.cacheDir,UUID.randomUUID().toString()).apply { mkdirs() };val gpx=GpxLibrary(root).apply { load() };val fits=FitLibrary(root).apply { load() }
        var cancelled=false;val original=fit()
        val importer=DreeveImport(c,gpx,{emptySet()},fits=fits,downloadFit={ _,file ->original.copyTo(file,overwrite=true);cancelled=true;true }) { _,_->fail("No GPX after cancellation") }
        assertTrue(importer.read(account,listOf(activity()),{cancelled}).interrupted)
        assertTrue(fits.entries.isEmpty());assertTrue(RideServiceLinks.forAccount(c,RideService.DREEVE,account).isEmpty())
        assertTrue(c.cacheDir.listFiles().orEmpty().none { it.name.startsWith("dreeve-original-") || it.name.startsWith("dreeve-import-") })
    }
    @Test fun managerAssignsActualFitIdentityAndDeletingAllowsReimport() {
        fun await(condition:()->Boolean) {
            val end=System.nanoTime()+10_000_000_000L
            while(!condition() && System.nanoTime()<end) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(5)
            }
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(condition())
        }
        val profile=BikeProfileStore(c,c.getSharedPreferences("bike",0)).current()
        val original=fit();val state=MapState(c,{})
        try {
            await { !state.busy }
            MockWebServer().use { server ->
                val api=DreeveClient(ActivityServiceConfig(),"test",base=server.url("/"))
                val selected=activity().copy(summary=DreeveSummary(gearId="gear-1"))
                var result:Result<DreeveImportResult>?=null
                fun import() {
                    server.enqueue(MockResponse().setBody(page(listOf(row())).toString()))
                    server.enqueue(MockResponse().setBody(okio.Buffer().write(original.readBytes())))
                    result=null
                    state.importDreeve(account,listOf(selected),api,{false},{},{result=it},mapOf("gear-1" to profile.slot),TrackOrigin.RECORDED)
                    await { result!=null && !state.busy }
                    assertEquals(DreeveImportState.IMPORTED,result!!.getOrThrow().items.single().state)
                }
                import()
                val entry=state.entries.single()
                assertNotEquals(DreeveImport.localId(account,selected.id),entry.id)
                assertEquals(result!!.getOrThrow().items.single().localId,entry.id)
                assertEquals("FIT",entry.format);assertEquals(profile.type,entry.details.bikeType)
                assertEquals(TrackOrigin.RECORDED,entry.origin);assertEquals(selected.name,entry.name)
                assertEquals(profile.slot,RideProfileColor.slot(c,entry.id))
                assertEquals(setOf(selected.id),state.existingDreeve(account,listOf(selected)))
                assertArrayEquals(original.readBytes(),FitLibrary(c.filesDir).originalFile(entry.id).readBytes())
                state.deleteRoute(entry.id);await { !state.busy }
                assertTrue(state.entries.isEmpty());assertTrue(RideServiceLinks.links(c,entry.id).isEmpty())
                assertTrue(state.existingDreeve(account,listOf(selected)).isEmpty())
                import();assertEquals(entry.id,state.entries.single().id)
                assertEquals(4,server.requestCount)
            }
        } finally {
            (MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(state) as java.util.concurrent.ExecutorService).shutdownNow()
        }
    }
}

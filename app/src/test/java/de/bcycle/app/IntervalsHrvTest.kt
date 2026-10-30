package de.bcycle.app

import android.app.Activity
import android.app.job.JobScheduler
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import okhttp3.Credentials
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class IntervalsHrvTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val day=LocalDate.of(2026,10,29)
    private fun configure() {
        val key=KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        IntervalsSettings(c) { key }.write(IntervalsConfig(true,"i123",false),"test-key")
    }
    private fun record(day:LocalDate=this.day,hrv:Double=42.25)=IntervalsWellness(day,JSONObject().put("hrv",hrv).put("hrvSDNN",50.5).put("restingHR",51).put("sleepQuality",2))
    private fun api(server:MockWebServer,config:IntervalsConfig=IntervalsConfig(true,"i123",false),stop:()->Boolean={false})=
        IntervalsClient(config,"test-key",stop,server.url("/api/v1/"))
    private fun run(server:MockWebServer)=IntervalsHrvUploads.run(c,{false}) { config,stop -> api(server,config,stop) }
    @Test fun mappingUsesMillisecondsAndIndependentRoundedSubjectiveScalesWithoutInventingMissingData() {
        val row=HrvHistorySummary("id",0,true,42.25,importedDay=day.toString(),metrics=mapOf("sdnnMs" to 50.5,"restingHrBpm" to 51.6,"pnn50Percent" to 20.0,"lfMs2" to 100.0))
        val answers=HrvQuestionnaire(day.toString(),ratings=mapOf("sleep" to 10,"motivation" to 0,"soreness" to 5,"fatigue" to 0,"energy" to 6,"condition" to 8,"lifestyle" to 9),
            notes="Private text",extras=mapOf("sleepHours" to "6.2166666667"))
        val result=IntervalsWellness.from(row,answers)!!
        assertEquals(42.25,result.values.getDouble("hrv"),0.0);assertEquals(50.5,result.values.getDouble("hrvSDNN"),0.0)
        assertEquals(52,result.values.getInt("restingHR"));assertEquals(1,result.values.getInt("sleepQuality"))
        assertEquals(4,result.values.getInt("motivation"));assertEquals(2,result.values.getInt("soreness"));assertEquals(0,result.values.getInt("fatigue"))
        assertEquals(22380,result.values.getInt("sleepSecs"));assertFalse(result.values.has("comments"));assertFalse(result.values.has("mood"));assertFalse(result.values.has("stress"))
        assertFalse(result.values.has("lf"));assertFalse(result.values.has("readiness"))
        assertEquals(setOf("hrv","hrvSDNN","restingHR"),IntervalsWellness.from(row,null)!!.values.keys().asSequence().toSet())
        assertNull(IntervalsWellness.from(row.copy(acceptable=false,importedDay=null),null))
        assertNotNull(IntervalsWellness.from(row.copy(acceptable=false),null)) // Imported raw measurements remain eligible.
        assertNull(IntervalsWellness.from(row.copy(rmssdMs=null),null))
        assertEquals(4,IntervalsWellness.from(row,answers.copy(ratings=mapOf("sleep" to 0,"fatigue" to 10)))!!.values.getInt("sleepQuality"))
    }
    @Test fun missingDayReturnedAsJsonNullIsUploadedAndMalformedDayStillFailsClosed() {
        configure()
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("[]"))
            server.enqueue(MockResponse().setBody("null"))
            server.enqueue(MockResponse().setBody(record().json().toString()))
            val report=IntervalsHrvUpload(c) { config,stop -> api(server,config,stop) }.run(listOf(record()),{false}) { _,_,_-> }
            assertEquals(1,report.count(IntervalsBulkState.UPLOADED));assertEquals(0,report.count(IntervalsBulkState.FAILED))
            val query=server.takeRequest();assertEquals("/api/v1/athlete/i123/wellness.json",query.requestUrl!!.encodedPath)
            val fields=query.requestUrl!!.queryParameter("fields")!!.split(',').toSet()
            assertEquals(IntervalsWellness.fields+"id",fields)
            assertEquals("GET",server.takeRequest().method);assertEquals("PUT",server.takeRequest().method)
            server.enqueue(MockResponse().setBody("{}"))
            assertThrows(IntervalsFailure::class.java) { api(server).wellness(day) }
            assertEquals(4,server.requestCount)
        }
    }
    @Test fun historySkipsAnyExistingHrvButFillsDaysWithOnlyOtherWellnessFields() {
        configure();val first=record(day.minusDays(1));val second=record()
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(JSONArray().put(JSONObject().put("id",first.day).put("hrv",0)).put(JSONObject().put("id",day).put("sleepSecs",30000)).toString()))
            server.enqueue(MockResponse().setBody(JSONObject().put("id",day).put("sleepSecs",30000).put("restingHR",48).toString()))
            val expected=second.json().apply { remove("restingHR") }
            server.enqueue(MockResponse().setBody(expected.toString()))
            val report=IntervalsHrvUpload(c) { config,stop -> api(server,config,stop) }.run(listOf(first,second),{false}) { _,_,_-> }
            assertEquals(1,report.count(IntervalsBulkState.EXISTS));assertEquals(1,report.count(IntervalsBulkState.UPLOADED))
            val month=server.takeRequest();assertEquals("/api/v1/athlete/i123/wellness.json",month.requestUrl!!.encodedPath)
            assertEquals("2026-10-01",month.requestUrl!!.queryParameter("oldest"))
            assertEquals("GET",server.takeRequest().method)
            val upload=server.takeRequest();assertEquals("PUT",upload.method);assertEquals("/api/v1/athlete/i123/wellness/2026-10-29",upload.path)
            assertEquals(Credentials.basic("API_KEY","test-key"),upload.getHeader("Authorization"))
            val sent=JSONObject(upload.body.readUtf8());assertFalse(sent.has("restingHR"));assertFalse(sent.has("sleepSecs"));assertEquals(42.25,sent.getDouble("hrv"),0.0)
            assertNotNull(IntervalsHrvUploads.receipt(c,"i123",day.toString()))
            // Explicit history never overwrites even our own acknowledged measurement.
            server.enqueue(MockResponse().setBody(JSONArray().put(second.json()).toString()))
            assertEquals(1,IntervalsHrvUpload(c) { config,stop -> api(server,config,stop) }.run(listOf(record(hrv=60.0)),{false}) { _,_,_-> }.count(IntervalsBulkState.EXISTS))
            assertEquals(4,server.requestCount)
        }
    }
    @Test fun remoteHrvAddedBetweenMonthQueryAndPutIsPreservedIncludingSdnnOnly() {
        configure()
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("[]"))
            server.enqueue(MockResponse().setBody(JSONObject().put("id",day).put("hrvSDNN",40).toString()))
            val report=IntervalsHrvUpload(c) { config,stop -> api(server,config,stop) }.run(listOf(record()),{false}) { _,_,_-> }
            assertEquals(1,report.count(IntervalsBulkState.EXISTS));assertEquals(2,server.requestCount)
        }
    }
    @Test fun automaticUploadUpdatesOnlyUnchangedOwnFieldsAndRetainsForeignWellnessValues() {
        configure();val old=record()
        IntervalsHrvUploads.confirm(c,"i123",day.toString(),old.values)
        val next=record(hrv=60.0).apply { values.put("sleepQuality",1).put("restingHR",54) }
        IntervalsHrvUploads.enqueue(c,next)
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(old.json().put("restingHR",48).toString()))
            server.enqueue(MockResponse().setBody(next.json().put("restingHR",48).toString()))
            assertFalse(run(server));server.takeRequest()
            val sent=JSONObject(server.takeRequest().body.readUtf8())
            assertEquals(60.0,sent.getDouble("hrv"),0.0);assertEquals(1,sent.getInt("sleepQuality"));assertFalse(sent.has("restingHR"))
            assertEquals(60.0,IntervalsHrvUploads.receipt(c,"i123",day.toString())!!.getDouble("hrv"),0.0)
            // Another source changes HRV after our upload: do not overwrite it on a later local edit.
            IntervalsHrvUploads.enqueue(c,record(hrv=70.0))
            server.enqueue(MockResponse().setBody(next.json().put("hrv",35).toString()))
            assertFalse(run(server));assertEquals(3,server.requestCount)
        }
    }
    @Test fun newerLocalEditDuringPutRetainsTheReceiptAndRemainsPending() {
        configure();val first=record();val latest=record(hrv=65.0)
        IntervalsHrvUploads.enqueue(c,first)
        MockWebServer().use { server ->
            var remote:JSONObject?=null
            server.dispatcher=object:Dispatcher() {
                override fun dispatch(request:RecordedRequest):MockResponse {
                    if(request.method=="GET") return MockResponse().setBody((remote ?: JSONObject().put("id",day)).toString())
                    val sent=JSONObject(request.body.readUtf8());remote=sent
                    if(sent.getDouble("hrv")==42.25) IntervalsHrvUploads.enqueue(c,latest)
                    return MockResponse().setBody(sent.toString())
                }
            }
            assertTrue(run(server));assertEquals(42.25,IntervalsHrvUploads.receipt(c,"i123",day.toString())!!.getDouble("hrv"),0.0)
            assertFalse(run(server));assertEquals(65.0,remote!!.getDouble("hrv"),0.0);assertEquals(4,server.requestCount)
        }
    }
    @Test fun malformedResponsesAuthenticationAndCancellationNeverTriggerBlindUploads() {
        configure()
        for(response in listOf(MockResponse().setBody("{}"),MockResponse().setBody("[{\"id\":\"2026-10-29\",\"hrv\":\"bad\"}]"),MockResponse().setResponseCode(401))) {
            MockWebServer().use { server ->
                server.enqueue(response)
                val report=IntervalsHrvUpload(c) { config,stop -> api(server,config,stop) }.run(listOf(record()),{false}) { _,_,_-> }
                assertTrue(report.interrupted);assertEquals(1,report.count(IntervalsBulkState.FAILED));assertEquals(1,server.requestCount)
                assertTrue(report.detail!!.startsWith(day.toString()))
                if(report.problem==R.string.service_auth_failed) assertTrue(report.detail!!.contains("HTTP 401"))
                assertFalse(report.detail!!.contains("test-key"))
            }
        }
        MockWebServer().use { server ->
            var stop=false
            server.dispatcher=object:Dispatcher() { override fun dispatch(request:RecordedRequest):MockResponse { stop=true;return MockResponse().setBody("[]") } }
            val report=IntervalsHrvUpload(c) { config,cancel -> api(server,config,cancel) }.run(listOf(record()),{stop}) { _,_,_-> }
            assertTrue(report.interrupted);assertEquals(1,server.requestCount)
        }
    }
    @Test fun outboxSurvivesNetworkFailureAndIsScopedToAccountAndEnableState() {
        configure();IntervalsHrvUploads.enqueue(c,record())
        assertNotNull(c.getSystemService(JobScheduler::class.java).getPendingJob(8306))
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            assertTrue(run(server));assertNull(IntervalsHrvUploads.receipt(c,"i123",day.toString()))
            IntervalsSettings(c).write(IntervalsConfig(true,"i456",false));IntervalsHrvUploads.settingsChanged(c)
            assertFalse(run(server));assertEquals(1,server.requestCount)
            IntervalsSettings(c).write(IntervalsConfig(false,"i123",false));IntervalsHrvUploads.settingsChanged(c)
            assertNull(c.getSystemService(JobScheduler::class.java).getPendingJob(8306));assertFalse(run(server))
            IntervalsSettings(c).write(IntervalsConfig(true,"i123",false));IntervalsHrvUploads.settingsChanged(c)
            server.enqueue(MockResponse().setBody("{\"id\":\"2026-10-29\"}"));server.enqueue(MockResponse().setBody(record().json().toString()))
            assertFalse(run(server));assertNotNull(IntervalsHrvUploads.receipt(c,"i123",day.toString()))
        }
    }
    @Test fun storeSnapshotUsesLatestDayIncludesImportedAnswersAndNeverResurrectsAnOlderRejectedDay() {
        val root=java.io.File(c.cacheDir,"wellness-store-${java.util.UUID.randomUUID()}")
        try {
            val store=HrvStore(root)
            val csv="date,local,time,HR,rMSSD,SDNN,sleep_quality,trainingMotivation,sleep_time\n"+
                "2026-29-10,2026-29-10,07:00,50,40,45,5,5,6\n"+
                "2026-29-10,2026-29-10,08:00,51,55,60,10,8,7.25\n"+
                "2026-30-10,2026-30-10,,,,,5,5,8\n"
            val parsed=Hrv4TrainingCsv.preview(csv.byteInputStream(),java.time.ZoneId.of("Europe/Berlin"),HrvCsvDateOrder.YEAR_DAY_MONTH)
            parsed.entries.forEach { assertTrue(store.importHrv4Training(it)) }
            val snapshot=store.intervalsWellness().single()
            assertEquals(day,snapshot.day);assertEquals(55.0,snapshot.values.getDouble("hrv"),0.0)
            assertEquals(60.0,snapshot.values.getDouble("hrvSDNN"),0.0);assertEquals(26100,snapshot.values.getInt("sleepSecs"))
            assertEquals(1,snapshot.values.getInt("sleepQuality"))
            val id=java.util.UUID.randomUUID().toString()
            val rejected=JSONObject(parsed.entries[1].result.toString()).put("id",id).put("external",JSONObject.NULL)
                .put("acceptable",false).put("utcMs",java.time.Instant.parse("2026-10-29T09:00:00Z").toEpochMilli())
            val folder=java.io.File(root,id).apply { mkdirs() };java.io.File(folder,"result.json").writeText(rejected.toString())
            assertTrue(store.intervalsWellness(day).isEmpty())
        } finally { root.deleteRecursively() }
    }
    @Test fun sleepClockInputsAndMissingSdnnAreMappedWithoutStaleOwnMeasurementValues() {
        val row=HrvHistorySummary("id",0,true,40.0,importedDay=day.toString())
        val mapped=IntervalsWellness.from(row,HrvQuestionnaire(day.toString(),extras=mapOf("bed" to "23:15","wake" to "06:45")))!!
        assertEquals(27000,mapped.values.getInt("sleepSecs"))
        val old=record().values
        val patch=IntervalsWellness.patch(mapped,old,old)
        assertTrue(patch.has("hrvSDNN"));assertTrue(patch.isNull("hrvSDNN"))
    }
    @Test fun cloudOrderCompactCapabilitiesAndIndependentHrvActionAreVisible() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));val root=DataSettingsView(a,themes) {}
            val dreeve=root.findViewWithTag<View>("data_frame_DREEVE");val intervals=root.findViewWithTag<View>("data_frame_INTERVALS")
            assertTrue(root.indexOfChild(dreeve)<root.indexOfChild(intervals))
            fun text(tag:String)=testDescendants(root.findViewWithTag<View>(tag)).filterIsInstance<TextView>().map { it.text.toString() }
            assertTrue(text("data_transfer_DREEVE").contains("FIT-Streckendaten | Wartungsintervalle"))
            assertTrue(text("data_transfer_INTERVALS").contains("FIT-Streckendaten | HRV-Daten"))
            val view=IntervalsView(a,themes,IntervalsConfig(),false) {}
            var count=0;view.uploadAllHrv={ count++ };view.hrvButton.performClick();assertEquals(1,count)
            view.hrvRunning(true);assertFalse(view.bulkButton.isEnabled);assertTrue(view.hrvButton.isEnabled)
            view.hrvRunning(false);view.bulkRunning(true);assertFalse(view.hrvButton.isEnabled)
        } finally { host.pause().stop().destroy() }
    }
}

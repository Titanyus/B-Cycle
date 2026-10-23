package de.bcycle.app

import android.net.Uri
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
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class ActivityServiceTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val key=KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private fun settings(service:ActivityService)=ActivityServiceSettings(c,service) { key }
    private fun fit():File {
        val journal=File(c.cacheDir,"provider.ride")
        RideJournal(journal,1791626400000L,RecordingConfig(setOf("HEART_RATE"))).use {
            it.append(RideSample(1791626400000L,0,values=mapOf("HEART_RATE" to 120.0)))
            it.append(RideSample(1791626401000L,1000,values=mapOf("HEART_RATE" to 130.0)))
        }
        return File(c.cacheDir,"provider.fit").also { FitWriter.encode(journal,it) }
    }
    private fun tokens(expiry:Long=System.currentTimeMillis()/1000+3600)=JSONObject().put("access_token","access-private")
        .put("refresh_token","refresh-private").put("expires_at",expiry).put("athlete",JSONObject().put("id",1234567890123L))
    private fun configureStrava(expiry:Long=System.currentTimeMillis()/1000+3600)=settings(ActivityService.STRAVA).apply {
        write(ActivityServiceConfig(clientId="123"),"secret-private");tokens("123","secret-private",tokens(expiry),true)
        write(read().copy(enabled=true,wifiOnly=false))
    }
    private fun configureDreeve()=settings(ActivityService.DREEVE).apply {
        write(ActivityServiceConfig(enabled=true,wifiOnly=false,address="https://dreeve.example.org/sub/"),"drv_"+"a".repeat(64))
    }
    @Test fun manualDreeveImportRequiresUsableConfigurationButNotAutomaticUploads() {
        val d=settings(ActivityService.DREEVE)
        assertFalse(d.dreeveImportConfigured())
        d.write(ActivityServiceConfig(address="https://dreeve.example.org/"))
        assertFalse(d.dreeveImportConfigured())
        d.write(d.read(),"drv_"+"a".repeat(64))
        assertTrue(d.dreeveImportConfigured())
        assertFalse(d.read().enabled)
        val prefs=c.getSharedPreferences("activity-service-dreeve",0)
        prefs.edit().putBoolean("needsPassword",true).commit()
        assertFalse(d.dreeveImportConfigured())
        prefs.edit().remove("needsPassword").putString("password","unreadable").commit()
        assertFalse(d.dreeveImportConfigured())
        assertFalse(settings(ActivityService.STRAVA).dreeveImportConfigured())
    }
    @Test fun serviceSettingsAreEncryptedIndependentAndRejectUnsafeAddresses() {
        val d=configureDreeve();val s=configureStrava()
        assertTrue(d.read().enabled);assertTrue(s.connected())
        assertEquals("https://dreeve.example.org/sub/",d.read().identity(ActivityService.DREEVE))
        for(service in ActivityService.entries) {
            val stored=c.getSharedPreferences("activity-service-${service.name.lowercase()}",0).all.toString()
            assertFalse(stored.contains("secret-private"));assertFalse(stored.contains("access-private"));assertFalse(stored.contains("drv_"))
        }
        assertThrows(IllegalArgumentException::class.java) { d.write(d.read().copy(address="http://example.org")) }
        assertThrows(IllegalArgumentException::class.java) { d.write(d.read().copy(address="https://user:password@example.org")) }
        assertThrows(IllegalArgumentException::class.java) { d.write(d.read().copy(address="https://example.org/?token=oops")) }
        assertThrows(IllegalArgumentException::class.java) { d.write(d.read(),"invalid") }
        s.write(s.read().copy(enabled=false,clientId="456"))
        assertFalse(s.connected());assertTrue(d.read().enabled)
        assertThrows(IllegalArgumentException::class.java) { s.write(s.read().copy(enabled=true)) }
    }
    @Test fun oauthCallbackRequiresOneTimeStateScopeAndExactRedirect() {
        val s=settings(ActivityService.STRAVA);s.write(ActivityServiceConfig(clientId="123"),"secret-private")
        val auth=s.authorizationUrl()
        assertEquals("activity:write",auth.getQueryParameter("scope"))
        assertEquals(ActivityServiceSettings.REDIRECT,auth.getQueryParameter("redirect_uri"))
        val callback=Uri.parse(ActivityServiceSettings.REDIRECT).buildUpon().appendQueryParameter("state",auth.getQueryParameter("state"))
            .appendQueryParameter("scope","activity:write").appendQueryParameter("code","once").build()
        assertEquals("once",settings(ActivityService.STRAVA).consumeCallback(callback).code) // Survives process/settings instance recreation.
        assertThrows(IllegalArgumentException::class.java) { s.consumeCallback(callback) }
        val fresh=s.authorizationUrl()
        val denied=callback.buildUpon().clearQuery().appendQueryParameter("state",fresh.getQueryParameter("state"))
            .appendQueryParameter("error","access_denied").build()
        assertThrows(IllegalArgumentException::class.java) { s.consumeCallback(denied) }
        s.authorizationUrl()
        assertThrows(IllegalArgumentException::class.java) { s.consumeCallback(Uri.parse("bcycle://other/strava?state=x")) }
        assertFalse(s.connected())
        val generation=s.authorizationGeneration()
        s.authorizationUrl()
        assertThrows(IllegalStateException::class.java) { s.tokens("123","secret-private",tokens(),true,expectedGeneration=generation) }
    }
    private fun importList(filename:String,status:String,id:String?=null)=JSONObject().put("fileImports",JSONArray().put(JSONObject()
        .put("filename",filename).put("source","fitFile").put("status",status).put("activityId",id ?: JSONObject.NULL)))
    @Test fun dreeve202OnlyQueuesAndConfirmedImportProvidesActivityIdWithoutReupload() {
        val server=MockWebServer();server.start()
        try {
            val key="drv_"+"a".repeat(64)
            val client=DreeveClient(ActivityServiceConfig(),key,base=server.url("/sub/"))
            server.enqueue(MockResponse().setBody("{\"app\":\"dreeve\",\"canUpload\":true}"));client.test()
            assertEquals("/sub/api/v1/status",server.takeRequest().path)
            server.enqueue(MockResponse().setBody("{\"fileImports\":[]}"))
            server.enqueue(MockResponse().setResponseCode(202).setBody("{\"status\":\"queued\"}"))
            val first=client.step(fit(),"unique.fit","");assertNull(first.activityId);assertEquals("accepted",first.jobId)
            val search=server.takeRequest();assertEquals("unique.fit",search.requestUrl!!.queryParameter("filters[filename]"))
            assertEquals("Bearer $key",search.getHeader("Authorization"))
            val upload=server.takeRequest();assertEquals("/sub/api/v1/activity/upload",upload.path);assertEquals("POST",upload.method)
            assertTrue(upload.body.readUtf8().contains("filename=\"unique.fit\""))
            server.enqueue(MockResponse().setBody(importList("unique.fit","queued").toString()))
            assertNull(client.step(fit(),"unique.fit",first.jobId).activityId);server.takeRequest()
            server.enqueue(MockResponse().setBody(importList("unique.fit","success","activity-42").toString()))
            assertEquals("activity-42",client.step(fit(),"unique.fit",first.jobId).activityId)
            assertEquals("GET",server.takeRequest().method)
        } finally { server.shutdown() }
    }
    @Test fun dreeveLostAcceptanceIsRecoveredAndSkippedImportNeverInventsAnId() {
        val server=MockWebServer();server.start()
        try {
            val client=DreeveClient(ActivityServiceConfig(),"key",base=server.url("/"))
            server.enqueue(MockResponse().setBody(importList("unique.fit","success","activity-42").toString()))
            assertEquals("activity-42",client.step(fit(),"unique.fit","").activityId)
            assertEquals("GET",server.takeRequest().method)
            server.enqueue(MockResponse().setBody(importList("unique.fit","skipped").toString()))
            assertEquals(R.string.service_duplicate,assertThrows(ActivityServiceFailure::class.java) { client.step(fit(),"unique.fit","") }.problem)
            server.enqueue(MockResponse().setBody("{\"app\":\"dreeve\",\"canUpload\":false}"))
            assertEquals(R.string.dreeve_import_disabled,assertThrows(ActivityServiceFailure::class.java) { client.test() }.problem)
            server.enqueue(MockResponse().setResponseCode(401).setBody("private server data"))
            val error=assertThrows(ActivityServiceFailure::class.java) { client.test() }
            assertFalse(error.retryable);assertFalse(error.toString().contains("private server data"))
        } finally { server.shutdown() }
    }
    @Test fun stravaPersistsUploadIdPollsItAndKeeps64BitActivityIdsExact() {
        val settings=configureStrava();val server=MockWebServer();server.start()
        try {
            val client=StravaClient(settings,base=server.url("/"))
            server.enqueue(MockResponse().setResponseCode(201).setBody("{\"id_str\":\"9223372036854775806\",\"activity_id\":null,\"error\":null}"))
            val first=client.step(fit(),"ride.fit","stable-identity","")
            assertNull(first.activityId)
            val post=server.takeRequest();assertEquals("/api/v3/uploads",post.path);assertEquals("Bearer access-private",post.getHeader("Authorization"))
            val body=post.body.readUtf8();assertTrue(body.contains("stable-identity"));assertTrue(body.contains("name=\"data_type\""))
            server.enqueue(MockResponse().setBody("{\"id_str\":\"9223372036854775806\",\"activity_id\":9223372036854775805,\"error\":null}"))
            assertEquals("9223372036854775805",client.step(fit(),"ride.fit","stable-identity",first.jobId).activityId)
            val poll=server.takeRequest();assertEquals("GET",poll.method);assertEquals("/api/v3/uploads/9223372036854775806",poll.path)
            server.enqueue(MockResponse().setBody("{\"id\":42,\"error\":\"duplicate with private HTML\"}"))
            val error=assertThrows(ActivityServiceFailure::class.java) { client.step(fit(),"ride.fit","stable-identity","42") }
            assertFalse(error.toString().contains("private HTML"))
        } finally { server.shutdown() }
    }
    @Test fun stravaRefreshRotatesEncryptedTokensBeforeUploadAndRejectsStaleRefreshResponse() {
        val settings=configureStrava(System.currentTimeMillis()/1000+10);val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody(tokens().put("access_token","new-access").put("refresh_token","new-refresh").toString()))
            server.enqueue(MockResponse().setResponseCode(201).setBody("{\"id\":42,\"error\":null}"))
            StravaClient(settings,base=server.url("/")).step(fit(),"ride.fit","stable","")
            val refresh=server.takeRequest();assertEquals("/oauth/token",refresh.path)
            assertTrue(refresh.body.readUtf8().contains("grant_type=refresh_token"))
            assertEquals("Bearer new-access",server.takeRequest().getHeader("Authorization"))
            assertEquals("new-refresh",settings.credentials().getString("refresh"))
            assertThrows(IllegalStateException::class.java) { settings.tokens("123","secret-private",tokens(),false,"refresh-private") }
        } finally { server.shutdown() }
    }
    @Test fun stravaExchangesAuthorizationCodeWithoutPersistingItAndCancellationMakesNoRequest() {
        val settings=settings(ActivityService.STRAVA)
        settings.write(ActivityServiceConfig(clientId="123"),"secret-private")
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody(tokens().toString()))
            StravaClient(settings,base=server.url("/")).authorize(StravaAuthorization("one-time-private-code","123",settings.authorizationGeneration()))
            val request=server.takeRequest();assertEquals("/oauth/token",request.path)
            val form=request.body.readUtf8()
            assertTrue(form.contains("grant_type=authorization_code"));assertTrue(form.contains("code=one-time-private-code"))
            assertTrue(settings.connected())
            assertFalse(settings.credentials().toString().contains("one-time-private-code"))
            assertThrows(java.io.IOException::class.java) {
                StravaClient(settings,{true},server.url("/")).step(fit(),"ride.fit","id","")
            }
            assertEquals(1,server.requestCount)
        } finally { server.shutdown() }
    }
    @Test fun independentJobsRetainProcessingIdsAcrossRunsAndNeverConfirmCancelledAttempts() {
        configureDreeve();configureStrava()
        val original=FitLibrary.retainRecording(c.filesDir,fit());val id=original.nameWithoutExtension
        RouteCloudUploads.enqueue(c,original,"ride.fit")
        assertTrue(ActivityServiceUploads.dreeve.run(c,{false}) { _,_,_,_,job,_->assertEquals("",job);UploadProgress("accepted") })
        assertEquals(UploadState.UPLOADING,ActivityServiceUploads.dreeve.state(c,id,null).state)
        assertTrue(RideServiceLinks.links(c,id).isEmpty())
        assertTrue(ActivityServiceUploads.strava.run(c,{false}) { _,_,_,_,_,_->throw ActivityServiceFailure(true,R.string.service_upload_failed) })
        assertFalse(ActivityServiceUploads.dreeve.run(c,{false}) { _,_,_,_,job,_->assertEquals("accepted",job);UploadProgress(job,"activity-1") })
        assertEquals(UploadState.UPLOADED,ActivityServiceUploads.dreeve.state(c,id,null).state)
        assertEquals(UploadState.WAITING,ActivityServiceUploads.strava.state(c,id,null).state)
        var cancelled=false
        assertTrue(ActivityServiceUploads.strava.run(c,{cancelled}) { _,_,_,_,_,_->cancelled=true;UploadProgress("99","12345") })
        assertEquals(1,RideServiceLinks.links(c,id).size)
        assertFalse(ActivityServiceUploads.strava.run(c,{false}) { _,_,_,_,job,_->assertEquals("99",job);UploadProgress(job,"12345") })
        assertEquals(2,RideServiceLinks.links(c,id).size)
        assertEquals(UploadState.UPLOADED,RouteCloudUploads.state(c,id,null).state)
    }
    @Test fun newVersionAndAccountAreIndependentAndImportFilesCannotBeQueued() {
        val settings=configureDreeve();val original=FitLibrary.retainRecording(c.filesDir,fit());val id=original.nameWithoutExtension
        ActivityServiceUploads.dreeve.enqueue(c,original)
        assertTrue(ActivityServiceUploads.dreeve.run(c,{false}) { _,_,_,_,_,_->UploadProgress("accepted") })
        val corrected=FitLibrary.correctedFile(c.filesDir,id).apply { parentFile!!.mkdirs();writeBytes(original.readBytes()+byteArrayOf(0)) }
        ActivityServiceUploads.dreeve.enqueue(c,corrected)
        val stamp=RouteFileStamp(corrected.length(),corrected.lastModified(),WebDavClient.hash(corrected))
        assertTrue(ActivityServiceUploads.dreeve.run(c,{false}) { _,_,_,_,job,_->if(job.isNotEmpty()) UploadProgress(job,"old-version") else UploadProgress("new-job") })
        assertEquals(UploadState.UPLOADING,ActivityServiceUploads.dreeve.state(c,id,stamp).state)
        assertEquals("old-version",RideServiceLinks.links(c,id).single().activityId)
        settings.write(settings.read().copy(address="https://other.example.org/"))
        assertEquals(UploadState.NOT_UPLOADED,ActivityServiceUploads.dreeve.state(c,id,stamp).state)
        val imported=fit().inputStream().use { FitLibrary(c.filesDir).add(it,"import.fit",false) }!!
        assertThrows(IllegalArgumentException::class.java) { ActivityServiceUploads.dreeve.enqueue(c,FitLibrary.activeFile(c.filesDir,imported.id)) }
    }
    @Test fun dreeveConfirmsPromptlyInSameWorkerWithoutPostingAcceptedFileAgain() {
        configureDreeve()
        val file=FitLibrary.retainRecording(c.filesDir,fit());val id=file.nameWithoutExtension
        ActivityServiceUploads.dreeve.enqueue(c,file)
        val waits=mutableListOf<Long>();val jobs=mutableListOf<String>()
        val pending=ActivityServiceUploads.dreeve.run(c,{false},confirmationWait={ delay,stopped ->
            assertFalse(stopped());assertEquals(UploadState.UPLOADING,ActivityServiceUploads.dreeve.state(c,id,null).state)
            assertTrue(RideServiceLinks.links(c,id).isEmpty());waits.add(delay);true
        }) { _,_,_,_,job,_->
            jobs.add(job)
            if(jobs.size==3) UploadProgress("accepted","activity-confirmed") else UploadProgress("accepted")
        }
        assertFalse(pending);assertEquals(listOf("","accepted","accepted"),jobs)
        assertEquals(listOf(2000L,3000L),waits)
        assertEquals(UploadState.UPLOADED,ActivityServiceUploads.dreeve.state(c,id,null).state)
        assertEquals("activity-confirmed",RideServiceLinks.links(c,id).single().activityId)
    }
    @Test fun dreeveConfirmationIsBoundedAndNetworkFailuresKeepTheirBackoff() {
        configureDreeve()
        val file=FitLibrary.retainRecording(c.filesDir,fit());val id=file.nameWithoutExtension
        ActivityServiceUploads.dreeve.enqueue(c,file)
        var calls=0;val waits=mutableListOf<Long>()
        assertTrue(ActivityServiceUploads.dreeve.run(c,{false},confirmationWait={ delay,_-> waits.add(delay);true }) { _,_,_,_,_,_->
            calls++;UploadProgress("accepted")
        })
        assertEquals(6,calls);assertEquals(30000L,waits.sum());assertTrue(RideServiceLinks.links(c,id).isEmpty())
        calls=0;waits.clear()
        assertTrue(ActivityServiceUploads.dreeve.run(c,{false},confirmationWait={ delay,_-> waits.add(delay);true }) { _,_,_,_,job,_->
            assertEquals("accepted",job);calls++;throw java.io.IOException("offline")
        })
        assertEquals(1,calls);assertTrue(waits.isEmpty())
        assertEquals(UploadState.UPLOADING,ActivityServiceUploads.dreeve.state(c,id,null).state)
    }
    @Test fun cancellationDuringConfirmationRetainsJobAndStopsFurtherChecks() {
        configureDreeve()
        val file=FitLibrary.retainRecording(c.filesDir,fit());val id=file.nameWithoutExtension
        ActivityServiceUploads.dreeve.enqueue(c,file)
        var cancelled=false;var calls=0
        assertTrue(ActivityServiceUploads.dreeve.run(c,{cancelled},confirmationWait={ _,_->cancelled=true;false }) { _,_,_,_,_,_->
            calls++;UploadProgress("accepted")
        })
        assertEquals(1,calls);assertTrue(RideServiceLinks.links(c,id).isEmpty())
        assertFalse(ActivityServiceUploads.dreeve.run(c,{false}) { _,_,_,_,job,_->
            assertEquals("accepted",job);UploadProgress(job,"activity-after-cancel")
        })
        assertEquals(UploadState.UPLOADED,ActivityServiceUploads.dreeve.state(c,id,null).state)
    }

    @Test fun slowDreeveProcessingUsesFixedConfirmationJobsAndEventuallyConfirmsWithoutReupload() {
        configureDreeve()
        val uploads=ActivityServiceUploads.dreeve
        val file=FitLibrary.retainRecording(c.filesDir,fit());val id=file.nameWithoutExtension
        uploads.enqueue(c,file)
        val scheduler=c.getSystemService(android.app.job.JobScheduler::class.java)
        var posts=0;var checks=0;var confirmed=false
        val step={ _:ActivityServiceConfig,_:File,_:String,_:String,job:String,_:()->Boolean ->
            if(job.isEmpty()) posts++ else { assertEquals("accepted",job);checks++ }
            UploadProgress("accepted",if(confirmed) "slow-activity" else null)
        }
        assertTrue(uploads.run(c,{false},confirmationWait={ _,_->true },step=step))
        assertEquals(1,posts);assertEquals(5,checks)
        repeat(3) {
            uploads.finishRun(c,true) { retry ->
                assertFalse("Remote processing must not request Android failure backoff",retry)
                // Keep the old job visible: finish delivery to Android can be asynchronous.
            }
            val job=scheduler.getPendingJob(ActivityService.DREEVE.job)!!
            assertEquals(30_000L,job.minLatencyMillis)
            assertEquals(android.app.job.JobInfo.NETWORK_TYPE_ANY,job.networkType)
            assertTrue(job.isPersisted)
            assertEquals(UploadState.UPLOADING,uploads.state(c,id,null).state)
            assertTrue(RideServiceLinks.links(c,id).isEmpty())
            val before=checks
            assertTrue(uploads.run(c,{false},confirmationWait={ _,_->fail("Existing imports need one check per scheduled run");false },step=step))
            assertEquals(before+1,checks)
        }
        val revision=RouteCloudUploads.revision(c)
        confirmed=true
        assertFalse(uploads.run(c,{false},step=step))
        uploads.finishRun(c,false) { retry -> assertFalse(retry);scheduler.cancel(ActivityService.DREEVE.job) }
        assertNull(scheduler.getPendingJob(ActivityService.DREEVE.job))
        assertEquals(1,posts)
        assertEquals(UploadState.UPLOADED,uploads.state(c,id,null).state)
        assertNotEquals(revision,RouteCloudUploads.revision(c))
        assertEquals("slow-activity",RideServiceLinks.links(c,id).single().activityId)
    }

    @Test fun failedDreeveStatusRequestKeepsFailureBackoffAndDisablingStopsConfirmationJobs() {
        val settings=configureDreeve();val uploads=ActivityServiceUploads.dreeve
        val file=FitLibrary.retainRecording(c.filesDir,fit());uploads.enqueue(c,file)
        assertTrue(uploads.run(c,{false}) { _,_,_,_,_,_->UploadProgress("accepted") })
        assertTrue(uploads.run(c,{false}) { _,_,_,_,_,_->throw java.io.IOException("offline") })
        val scheduler=c.getSystemService(android.app.job.JobScheduler::class.java)
        uploads.finishRun(c,true) { assertTrue("Actual network failures retain Android backoff",it) }
        assertEquals(0L,scheduler.getPendingJob(ActivityService.DREEVE.job)!!.minLatencyMillis)
        // A later successful check clears the network error but still awaits remote processing.
        assertTrue(uploads.run(c,{false}) { _,_,_,_,job,_->UploadProgress(job) })
        uploads.finishRun(c,true) {
            assertFalse(it)
            settings.write(settings.read().copy(enabled=false))
        }
        assertNull(scheduler.getPendingJob(ActivityService.DREEVE.job))
    }

    @Test fun activityExistenceTreatsOnly404AsMissingAndRejectsWrongIdsAndAuthErrors() {
        val server=MockWebServer();server.start()
        try {
            val dreeve=DreeveClient(ActivityServiceConfig(),"key",base=server.url("/"))
            val intervals=IntervalsClient(IntervalsConfig(athlete="i1"),"key",base=server.url("/api/v1/"))
            val checks=listOf<()->Boolean>({dreeve.activityExists("42")},{intervals.activityExists("42")})
            for(check in checks) {
                server.enqueue(MockResponse().setBody("{\"id\":42}"));assertTrue(check())
                val request=server.takeRequest();assertEquals("GET",request.method)
                assertTrue(request.path!!.endsWith("/42"));assertNotNull(request.getHeader("Authorization"))
                server.enqueue(MockResponse().setResponseCode(404).setBody("not found"));assertFalse(check());server.takeRequest()
                server.enqueue(MockResponse().setResponseCode(401).setBody("{}"));assertThrows(java.io.IOException::class.java) { check() };server.takeRequest()
                server.enqueue(MockResponse().setResponseCode(503).setBody("{}"));assertThrows(java.io.IOException::class.java) { check() };server.takeRequest()
                server.enqueue(MockResponse().setBody("{\"id\":99}"));assertThrows(java.io.IOException::class.java) { check() };server.takeRequest()
            }
        } finally { server.shutdown() }
    }

}

package de.bcycle.app

import android.content.Context
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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
class IntervalsTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private fun configure():IntervalsSettings {
        val key=KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        return IntervalsSettings(context) { key }.apply { write(IntervalsConfig(true,"i123",false),"private-api-key") }
    }
    private fun fit(name:String="ride",heart:Double=120.0):File {
        val journal=File(context.cacheDir,"$name.ride")
        RideJournal(journal,1791626400000L,RecordingConfig(setOf("HEART_RATE"))).use {
            it.append(RideSample(1791626400000L,0,values=mapOf("HEART_RATE" to heart)))
            it.append(RideSample(1791626401000L,1000,values=mapOf("HEART_RATE" to heart)))
        }
        return File(context.cacheDir,"$name.fit").also { FitWriter.encode(journal,it) }
    }
    @Test fun apiKeyIsEncryptedAndSettingsAreIndependentOfWebDav() {
        val settings=configure()
        assertEquals("private-api-key",settings.apiKey())
        assertFalse(context.getSharedPreferences("intervals-settings",0).all.toString().contains("private-api-key"))
        assertFalse(RouteExportSettings(context).read().webdav)
        settings.write(settings.read().copy(enabled=false))
        assertEquals("private-api-key",settings.apiKey())
        assertThrows(IllegalArgumentException::class.java) { settings.write(settings.read().copy(athlete="../other")) }
    }
    @Test fun clientUsesBasicAuthMultipartAndRequiresConfirmedActivityId() {
        val server=MockWebServer();server.start()
        try {
            val client=IntervalsClient(IntervalsConfig(true,"i123",false),"secret",base=server.url("/api/v1/"))
            server.enqueue(MockResponse().setBody("{\"id\":\"i123\"}"));client.test()
            val test=server.takeRequest()
            assertEquals("/api/v1/athlete/i123",test.path)
            assertEquals(Credentials.basic("API_KEY","secret"),test.getHeader("Authorization"))
            server.enqueue(MockResponse().setBody("[{\"id\":\"a123\"}]"))
            val original=fit();assertEquals("a123",client.upload(original,"tour.fit"))
            val request=server.takeRequest();assertEquals("POST",request.method)
            assertEquals("/api/v1/athlete/i123/activities",request.path)
            assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
            assertTrue(request.body.readByteArray().toString(Charsets.ISO_8859_1).contains("name=\"file\"; filename=\"tour.fit\""))
            server.enqueue(MockResponse().setBody("{}"))
            assertThrows(IntervalsFailure::class.java) { client.upload(original,"tour.fit") }
            server.enqueue(MockResponse().setResponseCode(401).setBody("secret server details"))
            val auth=assertThrows(IntervalsFailure::class.java) { client.test() }
            assertFalse(auth.retryable);assertFalse(auth.toString().contains("secret server details"))
            server.enqueue(MockResponse().setResponseCode(429))
            assertTrue(assertThrows(IntervalsFailure::class.java) { client.test() }.retryable)
        } finally { server.shutdown() }
    }
    @Test fun independentReceiptsDeduplicateAndCorrectedFileRequiresAnotherUpload() {
        configure()
        val original=FitLibrary.retainRecording(context.filesDir,fit());val id=original.nameWithoutExtension
        RouteCloudUploads.enqueue(context,original,"ride.fit")
        var count=0
        assertFalse(IntervalsUploads.run(context,{false}) { _,file,_,_->assertEquals(original,file);count++;"a1" })
        assertEquals(1,count)
        assertEquals(UploadState.UPLOADED,IntervalsUploads.state(context,id).state)
        assertEquals(listOf("a1"),RideServiceLinks.links(context,id).map { it.activityId })
        assertEquals(UploadState.NOT_UPLOADED,RouteUploads.state(context,id).state)
        assertEquals(UploadState.UPLOADED,RouteCloudUploads.state(context,id,null).state)
        RouteCloudUploads.enqueue(context,original,"ride.fit")
        IntervalsUploads.run(context,{false}) { _,_,_,_->count++;"duplicate" }
        assertEquals(1,count)
        val corrected=FitLibrary.correctedFile(context.filesDir,id).apply { parentFile!!.mkdirs();fit("corrected",140.0).copyTo(this) }
        val stamp=RouteFileStamp(corrected.length(),corrected.lastModified(),WebDavClient.hash(corrected))
        assertEquals(UploadState.NOT_UPLOADED,IntervalsUploads.state(context,id,stamp).state)
        IntervalsUploads.retry(context,id)
        assertFalse(IntervalsUploads.run(context,{false}) { _,file,_,_->assertEquals(corrected,file);"a2" })
        assertEquals(UploadState.UPLOADED,IntervalsUploads.state(context,id,stamp).state)
        assertEquals(listOf("a1","a2"),RideServiceLinks.links(context,id).map { it.activityId })
        RouteExportSettings(context).write(RouteExportConfig(webdav=true,server="https://cloud.example.org",user="rider"))
        assertEquals(UploadState.NOT_UPLOADED,RouteCloudUploads.state(context,id,stamp).state)
    }
    @Test fun invalidWebDavDoesNotPreventIntervalsQueueingAndEveryTargetRunsDespiteFailures() {
        configure()
        RouteExportSettings(context).write(RouteExportConfig(webdav=true,server="",user="rider"))
        val original=FitLibrary.retainRecording(context.filesDir,fit())
        assertThrows(Exception::class.java) { RouteCloudUploads.enqueue(context,original,"ride.fit") }
        assertEquals(UploadState.WAITING,IntervalsUploads.state(context,original.nameWithoutExtension).state)
        val visited=mutableListOf<Int>()
        assertThrows(IllegalStateException::class.java) { RouteCloudUploads.dispatch(listOf(
            { visited+=1;throw IllegalStateException() },
            { visited+=2;throw IllegalArgumentException() },
            { visited+=3 }
        )) }
        assertEquals(listOf(1,2,3),visited)
    }
    @Test fun cancellationAndFailureNeverProduceSuccessAndStateSurvivesReopen() {
        val settings=configure();val file=FitLibrary.retainRecording(context.filesDir,fit());val id=file.nameWithoutExtension
        IntervalsUploads.enqueue(context,file,"ride.fit")
        var stopped=false
        assertTrue(IntervalsUploads.run(context,{stopped}) { _,_,_,_->stopped=true;"a1" })
        assertEquals(UploadState.WAITING,IntervalsUploads.state(context,id).state)
        assertTrue(RideServiceLinks.links(context,id).isEmpty())
        assertFalse(IntervalsUploads.run(context,{false}) { _,_,_,_->throw IntervalsFailure(false,R.string.intervals_auth_failed) })
        assertEquals(UploadState.FAILED,IntervalsUploads.state(context,id).state)
        assertTrue(IntervalsSettings(context).read().enabled)
        settings.write(settings.read().copy(athlete="i456"))
        assertEquals(UploadState.NOT_UPLOADED,IntervalsUploads.state(context,id).state)
    }
    @Test fun completedOldGenerationCannotMarkANewerQueuedFileSuccessful() {
        configure()
        val file=FitLibrary.retainRecording(context.filesDir,fit());val id=file.nameWithoutExtension
        IntervalsUploads.enqueue(context,file,"ride.fit")
        assertTrue(IntervalsUploads.run(context,{false}) { _,_,_,_->
            assertTrue(file.setLastModified(file.lastModified()+2000))
            IntervalsUploads.enqueue(context,file,"ride.fit")
            "old-generation"
        })
        assertEquals(UploadState.WAITING,IntervalsUploads.state(context,id).state)
        // The server confirmed an activity, but that is not a receipt for the newer file generation.
        assertEquals("old-generation",RideServiceLinks.links(context,id).single().activityId)
        assertFalse(IntervalsUploads.run(context,{false}) { _,_,_,_->"new-generation" })
        assertEquals(UploadState.UPLOADED,IntervalsUploads.state(context,id).state)
        assertEquals(2,RideServiceLinks.links(context,id).size)
    }
    @Test fun stoppingRecordingQueuesIntervalsWithoutEnablingWebDavOrLocalExport() {
        configure()
        RouteExportSettings(context).write(RouteExportConfig(save=false,webdav=false))
        var message=""
        val store=RecordingStore(context) { text,_->message=text }
        store.begin(1791626400000L,RecordingConfig(setOf("HEART_RATE")))
        store.append(RideSample(1791626400000L,0,values=mapOf("HEART_RATE" to 120.0)))
        store.finish()
        val deadline=System.nanoTime()+10_000_000_000L
        while(!message.startsWith("Gespeichert:") && System.nanoTime()<deadline) {
            Thread.sleep(10);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
        assertTrue(message,message.startsWith("Gespeichert:"))
        val lib=FitLibrary(context.filesDir).apply { load() };val id=lib.entries.single().id
        assertEquals(UploadState.WAITING,IntervalsUploads.state(context,id).state)
        assertEquals(UploadState.NOT_UPLOADED,RouteUploads.state(context,id).state)
    }
    @Test fun importedFitCannotBeQueuedAndPreCancelledClientMakesNoRequest() {
        configure()
        val library=FitLibrary(context.filesDir);val imported=fit().inputStream().use { library.add(it,"import.fit",false) }!!
        assertThrows(IllegalArgumentException::class.java) { IntervalsUploads.enqueue(context,library.file(imported.id),"import.fit") }
        val server=MockWebServer();server.start()
        try {
            assertThrows(java.io.IOException::class.java) { IntervalsClient(IntervalsConfig(true,"i123"),"key",{true},server.url("/api/v1/")).test() }
            assertEquals(0,server.requestCount)
        } finally { server.shutdown() }
    }
}

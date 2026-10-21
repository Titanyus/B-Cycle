package de.bcycle.app

import com.garmin.fit.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RouteTransferPayloadTest {
    @Test fun recordedGpxBecomesRealActivityWithPowerAndProfileTypeWithoutChangingOriginal() {
        val c=RuntimeEnvironment.getApplication();val library=GpxLibrary(c.filesDir).apply { load() }
        val start=Instant.parse("2026-10-20T10:00:00Z")
        val xml="<gpx><trk><trkseg>"+(0..120).joinToString("") { i ->
            "<trkpt lat='${50+i*.00001}' lon='8'><time>${start.plusSeconds(i.toLong())}</time><extensions><power>200</power></extensions></trkpt>"
        }+"</trkseg></trk></gpx>"
        library.add(xml.byteInputStream(),"Dreeve",preserveDate=true,bikeType=BikeType.BIKE,origin=TrackOrigin.RECORDED)
        val ride=library.entries.single();val original=library.file(ride.id);val before=original.readBytes()
        val profiles=BikeProfileStore(c,c.getSharedPreferences("bike",0))
        RideProfileColor.assign(c,ride.id,profiles.activeSlot)
        assertTrue(RouteUploads.eligible(c,ride.id))
        val fit=RouteTransferPayload.file(c,ride.id)
        assertEquals(ride.id,fit.nameWithoutExtension)
        var type:com.garmin.fit.File?=null;var session:SessionMesg?=null;var records=0
        val messages=MesgBroadcaster().apply {
            addListener(FileIdMesgListener { type=it.type });addListener(SessionMesgListener { session=it })
            addListener(RecordMesgListener { records++;assertEquals(200,it.power) })
        }
        fit.inputStream().use { Decode().read(it,messages) }
        assertEquals(com.garmin.fit.File.ACTIVITY,type);assertEquals(121,records)
        assertEquals(SubSport.E_BIKE_FITNESS,session!!.subSport)
        assertEquals(200,session!!.normalizedPower);assertEquals(120f,session!!.totalTimerTime)
        assertEquals(BikeType.EBIKE,FitRead.read(fit).details.bikeType)
        val stamp=RouteFileStamp(original.length(),original.lastModified(),WebDavClient.hash(original))
        val receipt=RouteTransferPayload.annotate(c,fit,JSONObject().put("hash",WebDavClient.hash(fit)).put("length",fit.length()).put("modified",fit.lastModified()))
        assertTrue(RouteTransferPayload.matches(receipt,stamp));assertTrue(RouteTransferPayload.metadataMatches(c,ride.id,receipt))
        // The production queues accept the derived FIT under the original GPX identity.
        val key=javax.crypto.KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        IntervalsSettings(c) { key }.write(IntervalsConfig(true,"i123",false),"test-key")
        IntervalsUploads.retry(c,ride.id)
        assertFalse(IntervalsUploads.run(c,{false}) { _,file,_,_->
            assertEquals(fit,file);assertEquals(BikeType.EBIKE,FitRead.read(file).details.bikeType);"remote-intervals"
        })
        assertEquals(UploadState.UPLOADED,IntervalsUploads.state(c,ride.id,stamp).state)
        RouteExportSettings(c).write(RouteExportConfig(webdav=true,server="https://example.org/",user="rider",wifiOnly=false))
        RouteUploads.retry(c,ride.id)
        assertFalse(RouteUploads.run(c,{false}) { _,file,_,_->assertEquals(fit,file) })
        assertEquals(UploadState.UPLOADED,RouteUploads.state(c,ride.id,stamp).state)
        c.getSharedPreferences("activity-service-dreeve",0).edit().putBoolean("enabled",true).putBoolean("wifi",false).putString("address","https://dreeve.example/").commit()
        ActivityServiceUploads.dreeve.retry(c,ride.id)
        assertFalse(ActivityServiceUploads.dreeve.run(c,{false},step={ _,file,_,_,_,_->assertEquals(fit,file);UploadProgress("job","remote-dreeve") }))
        assertEquals(UploadState.UPLOADED,ActivityServiceUploads.dreeve.state(c,ride.id,stamp).state)
        profiles.save(profiles.current().copy(type=BikeType.INDOOR))
        assertFalse(RouteTransferPayload.metadataMatches(c,ride.id,receipt))
        val next=RouteTransferPayload.file(c,ride.id)
        assertNotEquals(fit,next);assertEquals(BikeType.INDOOR,FitRead.read(next).details.bikeType)
        assertArrayEquals(before,original.readBytes())
        library.add(xml.byteInputStream(),"Geplant",preserveDate=true,origin=TrackOrigin.IMPORTED)
        val planned=library.entries.last()
        assertFalse(RouteUploads.eligible(c,planned.id))
        assertTrue(runCatching { RouteTransferPayload.file(c,planned.id) }.isFailure)
    }
}

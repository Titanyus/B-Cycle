package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.Switch
import com.garmin.fit.*
import com.garmin.fit.plugins.ActivityFileValidationPlugin
import com.garmin.fit.plugins.ActivityFileValidationResult
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File
import java.io.IOException
import kotlin.math.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class FitElevationCorrectionTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private fun fixture(gps:Boolean=true,teleport:Boolean=false):File {
        val root=java.nio.file.Files.createTempDirectory("height-fit").toFile()
        val journal=File(root,"ride.ride")
        val config=RecordingConfig(setOf("GPS_POSITION","GPS_ALTITUDE","GPS_ASCENT","GPS_DESCENT","GPS_GRADIENT","GPS_SPEED","GPS_DISTANCE","RIDER_POWER","CADENCE","HEART_RATE"),"GPS_SPEED","GPS_DISTANCE")
        RideJournal(journal,1760000000000,config).use { writer ->
            for(i in 0..40) writer.append(RideSample(1760000000000+i*1000L,i*1000L,if(gps) 48.2 else null,if(gps) (if(teleport && i==20) 70.0 else 10.3+i*.0001) else null,
                mapOf("GPS_ALTITUDE" to 100.0+i,"GPS_ALTITUDE_MSL" to 1.0,"GPS_ASCENT" to i.toDouble(),"GPS_DESCENT" to 0.0,"GPS_GRADIENT" to 20.0,
                    "GPS_SPEED" to 5.0,"GPS_DISTANCE" to i*5.0,"RIDER_POWER" to 200.0+i,"CADENCE" to 80.0,"HEART_RATE" to 130.0)))
        }
        return File(root,"ride.fit").also { FitWriter.encode(journal,it) }
    }
    private val terrain=ElevationSource { _,lon -> val i=((lon-10.3)/.0001).roundToInt();500.0+min(i,40-i)*.4 }
    private fun decode(file:File)=file.inputStream().use { FitDecoder().decode(it) }
    @Test fun heightCorrectionPreservesOtherMeasurementsDeveloperFieldsAndGarminCompatibility() {
        val original=fixture();val before=original.readBytes();val old=decode(original)
        val output=File(original.parentFile,"corrected.fit")
        FitElevationCorrection.encode(original,output,terrain)
        assertArrayEquals(before,original.readBytes())
        val validator=ActivityFileValidationPlugin()
        val corrected=output.inputStream().use { FitDecoder().decode(it,validator) }
        assertTrue(validator.results.filter { it.status==ActivityFileValidationResult.Status.FAILED }.toString(),validator.results.none { it.status==ActivityFileValidationResult.Status.FAILED })
        assertEquals(old.recordMesgs.size,corrected.recordMesgs.size)
        assertTrue(corrected.memoGlobMesgs.isNotEmpty())
        assertEquals(old.fieldDescriptionMesgs.map { it.fieldName.toList() },corrected.fieldDescriptionMesgs.map { it.fieldName.toList() })
        old.recordMesgs.zip(corrected.recordMesgs).forEach { (a,b) ->
            assertEquals(a.timestamp.timestamp,b.timestamp.timestamp);assertEquals(a.positionLat,b.positionLat);assertEquals(a.positionLong,b.positionLong)
            assertEquals(a.power,b.power);assertEquals(a.heartRate,b.heartRate);assertEquals(a.cadence,b.cadence);assertEquals(a.distance,b.distance)
            assertEquals(a.developerFields.map { it.name }.filter { it!="GPS_GRADIENT" },b.developerFields.map { it.name }.filter { it!="GPS_GRADIENT" })
            for(name in listOf("RIDER_POWER","CADENCE","HEART_RATE","GPS_SPEED","GPS_DISTANCE")) {
                val av=a.developerFields.singleOrNull { it.name==name }?.getValue(0)
                val bv=b.developerFields.singleOrNull { it.name==name }?.getValue(0)
                assertEquals(name,av,bv)
            }
        }
        assertEquals(500.0,corrected.recordMesgs.first().enhancedAltitude.toDouble(),.001)
        assertEquals(508.0,corrected.recordMesgs[20].enhancedAltitude.toDouble(),.001)
        assertEquals(8,corrected.sessionMesgs.single().totalAscent.toInt());assertEquals(8,corrected.sessionMesgs.single().totalDescent.toInt())
        assertEquals(8,corrected.lapMesgs.single().totalAscent.toInt());assertEquals(8,corrected.lapMesgs.single().totalDescent.toInt())
        assertEquals(old.sessionMesgs.single().totalTimerTime,corrected.sessionMesgs.single().totalTimerTime)
        assertEquals(old.sessionMesgs.single().totalDistance,corrected.sessionMesgs.single().totalDistance)
        assertTrue(corrected.recordMesgs.last().developerFields.single { it.name=="GPS_ASCENT" }.getValue(0).let { (it as Number).toDouble()==8.0 })
        assertEquals(8.0,FitRead.read(output).details.ascentM!!,.001)
    }
    @Test fun gpsTeleportIsNotTurnedIntoAnArtificialDemClimb() {
        val original=fixture(teleport=true);val output=File(original.parentFile,"teleport-corrected.fit")
        FitElevationCorrection.encode(original,output,ElevationSource { lat,lon -> assertTrue(lon<11);terrain.height(lat,lon) })
        val result=decode(output)
        assertNull(result.recordMesgs[20].enhancedAltitude)
        assertEquals(decode(original).recordMesgs[20].positionLong,result.recordMesgs[20].positionLong)
        assertTrue(result.sessionMesgs.single().totalAscent<=8)
    }
    @Test fun failuresAndCancellationLeaveExistingVersionAndRawOriginalUntouched() {
        val original=fixture();val target=File(context.filesDir,"corrected.fit").apply { writeText("existing version") }
        val raw=original.readBytes();val existing=target.readBytes()
        var calls=0
        assertThrows(IOException::class.java) { FitElevationCorrection.correct(context,original,target,source=ElevationSource { _,_->if(++calls>5) throw IOException("offline") else 500.0 }) }
        assertEquals(6,calls)
        assertArrayEquals(existing,target.readBytes());assertArrayEquals(raw,original.readBytes())
        assertThrows(IOException::class.java) { FitElevationCorrection.correct(context,original,target,cancelled={true},source=terrain) }
        assertArrayEquals(existing,target.readBytes())
        assertThrows(IllegalArgumentException::class.java) { FitElevationCorrection.correct(context,fixture(false),target,source=terrain) }
        assertArrayEquals(existing,target.readBytes())
    }
    @Test fun activeLibraryVersionIsCorrectedWhileOriginalIsPreservedAcrossReopenAndDelete() {
        val raw=fixture();val original=FitLibrary.retainRecording(context.filesDir,raw);val id=original.nameWithoutExtension
        val library=FitLibrary(context.filesDir).apply { load() }
        assertEquals(original,library.file(id));val bytes=original.readBytes()
        FitElevationCorrection.correct(context,original,FitLibrary.correctedFile(context.filesDir,id),source=terrain)
        library.refreshDetails(id)
        assertArrayEquals(bytes,original.readBytes())
        assertEquals(8.0,library.entries.single().details.ascentM!!,.001)
        val reopened=FitLibrary(context.filesDir).apply { load() }
        assertEquals(FitLibrary.correctedFile(context.filesDir,id),reopened.file(id))
        assertEquals(8.0,reopened.entries.single().details.ascentM!!,.001)
        reopened.delete(id);assertFalse(original.exists());assertFalse(FitLibrary.correctedFile(context.filesDir,id).exists())
    }
    @Test fun correctedContentInvalidatesSuccessAndCloudRetryUsesTheActiveVersion() {
        val original=FitLibrary.retainRecording(context.filesDir,fixture());val id=original.nameWithoutExtension
        val library=FitLibrary(context.filesDir).apply { load() }
        RouteExportSettings(context).write(RouteExportConfig(webdav=true,server="https://cloud.example.invalid",user="tester",wifiOnly=false))
        RouteUploads.enqueue(context,original,"ride.fit")
        assertFalse(RouteUploads.run(context,{false}) { _,_,_,_-> })
        val before=RouteFileStamp(original.length(),original.lastModified(),WebDavClient.hash(original))
        RouteLocalSaves.perform(context,id,original,"test") { }
        assertEquals(UploadState.UPLOADED,RouteUploads.state(context,id,before).state)
        assertTrue(RouteLocalSaves.completed(context,id,before))
        FitElevationCorrection.correct(context,original,FitLibrary.correctedFile(context.filesDir,id),source=terrain)
        library.refreshDetails(id)
        val active=library.file(id);val after=RouteFileStamp(active.length(),active.lastModified(),WebDavClient.hash(active))
        assertEquals(UploadState.NOT_UPLOADED,RouteUploads.state(context,id,after).state)
        assertFalse(RouteLocalSaves.completed(context,id,after))
        RouteUploads.retry(context,id)
        var uploaded=false
        assertFalse(RouteUploads.run(context,{false}) { _,file,_,_->
            assertEquals(active,file);assertEquals(500.0,decode(file).recordMesgs.first().enhancedAltitude.toDouble(),.001);uploaded=true
        })
        assertTrue(uploaded);assertEquals(UploadState.UPLOADED,RouteUploads.state(context,id,after).state)
    }
    @Test fun manualCorrectionIsOfferedOnlyForOwnFitsWhenPreferenceIsEnabled() {
        val themes=ThemeManager(context.getSharedPreferences("bike",0))
        val entry=ManagedRoute("test","Test",true,"FIT",RouteDetails(),true)
        fun view(e:ManagedRoute)=RouteAnalysisView(context,themes,"test") { }.apply { show(RouteAnalysis(e,mapOf(AnalysisMetric.ASCENT to 4.0),null,true,1.0,emptyList())) }
        assertNull(view(entry).findViewWithTag<View>("analysis_correct_elevation"))
        context.getSharedPreferences("bike",0).edit().putBoolean(FitElevationCorrection.preference,true).commit()
        var calls=0
        val enabled=view(entry).apply { correctElevation={calls++} }
        enabled.findViewWithTag<View>("analysis_correct_elevation").performClick();assertEquals(1,calls)
        enabled.correctionStatus(true);assertFalse(enabled.findViewWithTag<View>("analysis_correct_elevation").isEnabled)
        assertNull(view(entry.copy(recorded=false,origin=TrackOrigin.IMPORTED)).findViewWithTag<View>("analysis_correct_elevation"))
        assertNull(view(entry.copy(format="GPX")).findViewWithTag<View>("analysis_correct_elevation"))
    }
    @Test fun preferenceIsOffByDefaultAndMapSwitchPersistsImmediately() {
        assertFalse(FitElevationCorrection.enabled(context))
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get()
            MainActivity::class.java.getDeclaredMethod("mapSettings").apply { isAccessible=true }.invoke(activity)
            shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            val switch=root.findViewWithTag<Switch>("map_elevation_correction")
            assertFalse(switch.isChecked);switch.performClick()
            assertTrue(FitElevationCorrection.enabled(activity))
            switch.performClick();assertFalse(FitElevationCorrection.enabled(activity))
        } finally { controller.pause().stop().destroy() }
    }
}

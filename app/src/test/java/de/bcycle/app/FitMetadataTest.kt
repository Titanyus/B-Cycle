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
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.json.JSONArray
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class FitMetadataTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val start=Instant.parse("2026-10-23T08:00:00Z").toEpochMilli()
    private fun sample(second:Int)=RideSample(start+second*1000,second*1000L,values=mapOf("SPEED" to 5.0,"BIKE_DISTANCE" to second*5.0,"HEART_RATE" to 130.0,"RIDER_POWER" to 180.0))
    private fun encode(metadata:FitMetadata):Pair<File,FitMessages> {
        val source=File(c.cacheDir,"${UUID.randomUUID()}.ride")
        RideJournal(source,start,RecordingConfig(indoor=metadata.type==BikeType.INDOOR)).use { journal -> (0..40).forEach { journal.append(sample(it)) } }
        val target=File(c.cacheDir,"${UUID.randomUUID()}.fit")
        FitWriter.encode(source,target,metadata=metadata)
        val validator=ActivityFileValidationPlugin()
        val messages=target.inputStream().use { FitDecoder().decode(it,validator) }
        assertTrue(validator.results.filter { it.status==ActivityFileValidationResult.Status.FAILED }.toString(),validator.results.none { it.status==ActivityFileValidationResult.Status.FAILED })
        source.delete();return target to messages
    }
    @Test fun standardBikeProfileAndRecorderDecodeAndValidateForEveryBikeType() {
        for(type in BikeType.entries) {
            val (file,messages)=encode(FitMetadata("Matrix ICR50 · ä 🚲",type,3))
            val bike=messages.bikeProfileMesgs.single()
            assertEquals("Matrix ICR50 · ä 🚲",bike.name);assertEquals(3,bike.id.toInt());assertEquals(3,bike.messageIndex.toInt())
            assertEquals(Sport.CYCLING,bike.sport);assertEquals(FitBikeType.subSport(type),bike.subSport)
            assertEquals(bike.subSport,messages.sessionMesgs.single().subSport)
            assertEquals("B|CYCLE",messages.deviceInfoMesgs.single().productName)
            assertEquals(AppVersion.fitSoftwareVersion,messages.deviceInfoMesgs.single().softwareVersion.toDouble(),.001)
            assertEquals(41,messages.recordMesgs.size)
            file.delete()
        }
        assertNull(FitMetadata("Large",BikeType.EBIKE,255).profileId)
        assertEquals(255,FitMetadata("Large",BikeType.EBIKE,255).profileIndex)
        assertNull(FitMetadata("Large",BikeType.EBIKE,4096).profileIndex)
    }
    @Test fun eachOptionalFieldCanBeOmittedWithoutRemovingActivitySportOrMeasurements() {
        val fieldNumbers=mapOf(FitMetadataField.BIKE_NAME to 0,FitMetadataField.SPORT to 1,FitMetadataField.SUB_SPORT to 2,FitMetadataField.PROFILE_ID to 14,FitMetadataField.PROFILE_INDEX to 254)
        for(excluded in FitMetadataField.entries) {
            val (file,messages)=encode(FitMetadata("Bike",BikeType.EBIKE,2,FitMetadataField.defaults-excluded))
            fieldNumbers.forEach { (field,number) -> assertEquals(field!=excluded,messages.bikeProfileMesgs.single().getField(number)!=null) }
            assertEquals(excluded!=FitMetadataField.RECORDER,messages.deviceInfoMesgs.isNotEmpty())
            assertEquals(Sport.CYCLING,messages.sessionMesgs.single().sport)
            assertEquals(SubSport.E_BIKE_FITNESS,messages.sessionMesgs.single().subSport)
            assertEquals(41,messages.recordMesgs.size);file.delete()
        }
        val (file,messages)=encode(FitMetadata("Private",BikeType.INDOOR,1,emptySet()))
        assertTrue(messages.bikeProfileMesgs.isEmpty());assertTrue(messages.deviceInfoMesgs.isEmpty())
        assertEquals(SubSport.INDOOR_CYCLING,messages.sessionMesgs.single().subSport);file.delete()
    }
    @Test fun typeOverlayUpdatesPresentBikeTypeWithoutReintroducingDisabledFields() {
        val (source,_)=encode(FitMetadata("Bike",BikeType.INDOOR,1,FitMetadataField.defaults-FitMetadataField.SPORT))
        val target=File(c.cacheDir,"overlay.fit");FitBikeType.write(source,target,BikeType.EBIKE)
        val decoded=target.inputStream().use { FitDecoder().decode(it) }
        assertEquals(SubSport.E_BIKE_FITNESS,decoded.bikeProfileMesgs.single().subSport)
        assertNull(decoded.bikeProfileMesgs.single().sport)
        assertEquals("Bike",decoded.bikeProfileMesgs.single().name)
        source.delete();target.delete()
    }
    @Test fun settingsAndProfileBackupsRetainIndependentSelectionsAndOldProfilesDefaultToAll() {
        val prefs=c.getSharedPreferences("bike",0);val bikes=BikeProfileStore(c,prefs)
        bikes.save(bikes.current().copy(name="Matrix ICR50",type=BikeType.INDOOR,recording=RecordingConfig(metadata=emptySet())))
        val second=bikes.add()!!;bikes.select(second.slot)
        bikes.save(bikes.current().copy(recording=RecordingConfig(metadata=setOf(FitMetadataField.BIKE_NAME))))
        val backup=SettingsBackup(c,ThemeManager(prefs))
        val bytes=backup.export(setOf(BackupSection.PROFILE,BackupSection.RECORDING));prefs.edit().clear().commit()
        val doc=backup.decode(bytes);backup.apply(backup.plan(doc,doc.sections.keys))
        val restored=BikeProfileStore(c,prefs)
        assertEquals(setOf(FitMetadataField.BIKE_NAME),restored.current().recording.metadata)
        restored.select(0);assertTrue(restored.current().recording.metadata.isEmpty())
        val view=FitMetadataView(c,ThemeManager(prefs),restored.current(),emptySet()) { selected -> restored.save(restored.current().copy(recording=restored.current().recording.copy(metadata=selected))) }
        assertEquals(View.GONE,view.findViewWithTag<View>("fit_metadata_content").visibility)
        view.findViewWithTag<View>("fit_metadata_toggle").performClick()
        val name=view.findViewWithTag<Switch>("fit_metadata_BIKE_NAME")
        assertEquals("Fahrradname (Matrix ICR50)",name.text.toString())
        assertEquals("Aufzeichnendes Gerät (B|CYCLE)",view.findViewWithTag<Switch>("fit_metadata_RECORDER").text.toString())
        name.performClick();assertEquals(setOf(FitMetadataField.BIKE_NAME),restored.current().recording.metadata)
        val recording=backup.export(setOf(BackupSection.RECORDING));prefs.edit().putStringSet("recordMetadata",emptySet()).commit()
        val recordingDoc=backup.decode(recording);backup.apply(backup.plan(recordingDoc,recordingDoc.sections.keys))
        assertEquals(setOf(FitMetadataField.BIKE_NAME),BikeProfileStore(c,prefs).current().recording.metadata)
        val old=JSONArray(prefs.getString(BikeProfileStore.KEY,"[]"));repeat(old.length()) { old.getJSONObject(it).remove("metadata") }
        prefs.edit().putString(BikeProfileStore.KEY,old.toString()).remove("recordMetadata").commit()
        assertEquals(FitMetadataField.defaults,BikeProfileStore(c,prefs).current().recording.metadata)
        assertEquals(FitMetadataField.defaults,RecordingConfig.decode(null,null,null).metadata)
    }
    @Test fun actualIndoorFitSettingsShowProfileValuesAndPersistSwitchesWithoutChangingMeasurements() {
        val prefs=c.getSharedPreferences("bike",0);val profiles=BikeProfileStore(c,prefs)
        profiles.save(profiles.current().copy(name="Matrix ICR50",type=BikeType.INDOOR))
        val host=org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=host.get();val runtime=RideRuntime.get(activity)
            val form=MainActivity::class.java.getDeclaredMethod("recordingControls").apply { isAccessible=true }.invoke(activity) as android.widget.LinearLayout
            activity.setContentView(form)
            val view=form.findViewWithTag<FitMetadataView>("fit_metadata")
            view.findViewWithTag<View>("fit_metadata_toggle").performClick()
            val before=runtime.config.fields
            val name=view.findViewWithTag<Switch>("fit_metadata_BIKE_NAME")
            assertEquals("Fahrradname (Matrix ICR50)",name.text.toString())
            assertTrue(name.isChecked);name.performClick()
            assertEquals(FitMetadataField.defaults-FitMetadataField.BIKE_NAME,runtime.config.metadata)
            assertEquals(runtime.config.metadata,runtime.bikes.current().recording.metadata)
            assertEquals(before,runtime.config.fields)
            runtime.bikes.save(runtime.bikes.current().copy(name="Umbenannt"));shadowOf(Looper.getMainLooper()).idle()
            assertEquals("Fahrradname (Umbenannt)",name.text.toString())
            runtime.bikes.save(runtime.bikes.current().copy(name="Matrix ICR50"));shadowOf(Looper.getMainLooper()).idle()
            assertEquals("Fahrradname (Matrix ICR50)",name.text.toString())
            for(width in listOf(288,328,380)) {
                view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                view.layout(0,0,width,view.measuredHeight)
                assertTrue(view.findViewWithTag<Switch>("fit_metadata_RECORDER").width<=width)
                if(width==328) {
                    val image=android.graphics.Bitmap.createBitmap(width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
                    image.eraseColor(runtime.themes.settings.active.tokens.secondary);view.draw(android.graphics.Canvas(image))
                    File("build/reports/ui/fit-metadata.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };image.recycle()
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun recordingUsesCapturedProfileAndSelectionAfterSwitchRenameAndDeletion() {
        val prefs=c.getSharedPreferences("bike",0);val bikes=BikeProfileStore(c,prefs)
        bikes.save(bikes.current().copy(name="Matrix ICR50",type=BikeType.INDOOR))
        RouteExportSettings(c).write(RouteExportConfig(save=false))
        var result:RideCompletion?=null
        val store=RecordingStore(c) { update -> if(update is RecordingUpdate.Finished) result=update.result }
        val worker=RecordingStore::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(store) as ExecutorService
        try {
            store.begin(start,RecordingConfig(indoor=true,metadata=FitMetadataField.defaults-FitMetadataField.RECORDER))
            worker.submit {}.get(10,TimeUnit.SECONDS)
            for(second in 0..10) store.append(sample(second))
            worker.submit {}.get(10,TimeUnit.SECONDS)
            val second=bikes.add()!!;bikes.select(second.slot);assertTrue(bikes.remove(0))
            store.finish();worker.submit {}.get(10,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle()
            val completion=result as RideCompletion.Completed
            val messages=FitLibrary.activeFile(c.filesDir,completion.rideId).inputStream().use { FitDecoder().decode(it) }
            assertEquals("Matrix ICR50",messages.bikeProfileMesgs.single().name)
            assertEquals(0,messages.bikeProfileMesgs.single().id.toInt())
            assertEquals(SubSport.INDOOR_CYCLING,messages.bikeProfileMesgs.single().subSport)
            assertTrue(messages.deviceInfoMesgs.isEmpty())
            val analysis=RideAnalysisFiles.file(c.filesDir,completion.rideId).inputStream().use { FitDecoder().decode(it) }
            assertEquals("Matrix ICR50",analysis.bikeProfileMesgs.single().name);assertTrue(analysis.deviceInfoMesgs.isEmpty())
        } finally { worker.shutdownNow() }
    }
}

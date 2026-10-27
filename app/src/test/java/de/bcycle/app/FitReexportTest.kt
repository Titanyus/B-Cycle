package de.bcycle.app

import com.garmin.fit.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class FitReexportTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private fun fixture():Pair<FitLibrary,ManagedRoute> {
        val bikes=BikeProfileStore(c,c.getSharedPreferences("bike",0))
        val journal=File(c.cacheDir,"reexport.ride")
        RideJournal(journal,1791626400000L,RecordingConfig(setOf("SPEED","BIKE_DISTANCE","HEART_RATE"))).use { out ->
            for(i in 0..12) out.append(RideSample(1791626400000+i*1000, i*1000L,50.0+i*.00001,8.0,
                mapOf("RIDER_POWER" to 220.0,"MOTOR_POWER" to 400.0,"HEART_RATE" to 130.0,"SPEED" to 5.0,"BIKE_DISTANCE" to i*5.0,"GPS_ALTITUDE" to 100.0,"GPS_ALTITUDE_MSL" to 1.0)))
        }
        val reduced=File(c.cacheDir,"reduced.fit");FitWriter.encode(journal,reduced)
        val original=FitLibrary.retainRecording(c.filesDir,reduced);val id=original.nameWithoutExtension
        RideAnalysisFiles.retain(c.filesDir,id,journal);RideProfileColor.assign(c,id,bikes.activeSlot)
        val library=FitLibrary(c.filesDir).apply { load() }
        return library to ManagedRoute(id,"Original",true,"FIT",library.entries.single().details,true,TrackOrigin.RECORDED,true)
    }
    private fun messages(file:File)=file.inputStream().use { FitDecoder().decode(it) }
    @Test fun restoresPowerFromFullAnalysisUsingAssignedProfileAndKeepsOriginalsAndIdentity() {
        val (library,entry)=fixture();val prefs=c.getSharedPreferences("bike",0);val bikes=BikeProfileStore(c,prefs)
        val original=library.originalFile(entry.id).readBytes();val full=RideAnalysisFiles.file(c.filesDir,entry.id).readBytes()
        assertTrue(messages(library.file(entry.id)).recordMesgs.all { it.power==null })
        bikes.save(bikes.current().copy(recording=RecordingConfig(setOf("SPEED","BIKE_DISTANCE","RIDER_POWER"))))
        val first=bikes.activeSlot
        val other=bikes.add()!!;bikes.select(other.slot);bikes.save(bikes.current().copy(recording=RecordingConfig(emptySet())))
        assertTrue(FitReexport.rebuild(c,entry)) // Uses assigned bicycle, not the currently selected one.
        assertEquals(first,RideProfileColor.slot(c,entry.id))
        val regenerated=messages(library.file(entry.id))
        assertTrue(regenerated.recordMesgs.all { it.power?.toInt()==220 && it.speed.toDouble()==5.0 && it.heartRate==null })
        assertEquals(220,regenerated.sessionMesgs.single().avgPower.toInt())
        assertTrue(regenerated.recordMesgs.all { it.developerFields.none { field -> field.name=="HEART_RATE" } })
        assertArrayEquals(original,library.originalFile(entry.id).readBytes());assertArrayEquals(full,RideAnalysisFiles.file(c.filesDir,entry.id).readBytes())
        assertFalse(FitReexport.rebuild(c,entry)) // Same bytes: no repeated file revision.
        library.refreshDetails(entry.id)
        assertEquals(entry.id,library.add(library.file(entry.id).inputStream(),"Roundtrip",true)!!.id)
        assertEquals(1,library.entries.size)
        assertTrue(messages(library.analysisFile(entry.id)).recordMesgs.all { it.heartRate?.toInt()==130 })
    }
    @Test fun failedOrUnavailableRebuildDoesNotReplaceTheActiveFileAndImportsRemainUntouched() {
        val (library,entry)=fixture();val before=library.file(entry.id).readBytes()
        assertFalse(FitReexport.rebuild(c,entry.copy(id=entry.id.replace("recorded-","imported-"))))
        RideProfileColor.forget(c,entry.id)
        assertThrows(IllegalStateException::class.java) { FitReexport.rebuild(c,entry) }
        assertArrayEquals(before,library.file(entry.id).readBytes())
    }
    @Test fun correctedHeightsSurviveNewFieldSelectionAndLaterHeightUpdatesKeepTheMask() {
        val (library,entry)=fixture();val bikes=BikeProfileStore(c,c.getSharedPreferences("bike",0))
        bikes.save(bikes.current().copy(recording=RecordingConfig(setOf("RIDER_POWER","GPS_ALTITUDE","SPEED","BIKE_DISTANCE"))))
        val correction=FitLibrary.correctedFile(c.filesDir,entry.id);correction.parentFile!!.mkdirs()
        FitElevationCorrection.encode(RideAnalysisFiles.file(c.filesDir,entry.id),correction,ElevationSource { _,_->321.0 })
        assertTrue(FitReexport.rebuild(c,entry))
        assertTrue(messages(library.file(entry.id)).recordMesgs.all { it.altitude.toDouble()==321.0 && it.power?.toInt()==220 && it.heartRate==null })
        FitElevationCorrection.encode(RideAnalysisFiles.file(c.filesDir,entry.id),correction,ElevationSource { _,_->444.0 })
        FitReexport.refreshElevation(c.filesDir,entry.id)
        assertTrue(messages(library.file(entry.id)).recordMesgs.all { it.altitude.toDouble()==444.0 && it.power?.toInt()==220 && it.heartRate==null })
        library.delete(entry.id)
        assertFalse(FitReexport.file(c.filesDir,entry.id).exists())
    }
}

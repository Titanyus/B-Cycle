package de.bcycle.app

import com.garmin.fit.*
import com.garmin.fit.plugins.ActivityFileValidationPlugin
import com.garmin.fit.plugins.ActivityFileValidationResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

class IndoorFitTest {
    @get:Rule val folder=TemporaryFolder()
    @Test fun ftmsJournalReplaysAsIndoorCyclingWithNativeValuesPerformanceAndNoGps() {
        val start=Instant.parse("2026-10-16T10:00:00Z").toEpochMilli()
        val config=IndoorFields.config(RecordingConfig(setOf("SPEED","BIKE_DISTANCE","RIDER_POWER","CADENCE","HEART_RATE","NORMALIZED_POWER","MECHANICAL_WORK","ESTIMATED_FTP")))
        assertTrue(config.indoor);assertFalse(config.needsGps)
        val journal=folder.newFile("trainer.ride")
        RideJournal(journal,start,config).use { writer ->
            for(second in 0..1200) {
                val time=second*1000L;val power=if(second in 300..359) 0.0 else 200.0
                writer.append(RideSample(start+time,time,values=mapOf("SPEED" to 10.0,"BIKE_DISTANCE" to second*10.0,
                    "RIDER_POWER" to power,"CADENCE" to if(power==0.0) 0.0 else 90.0,"HEART_RATE" to 150.0),
                    performanceEvents=listOf(PerformanceEvent(PerformanceChannel.POWER,time,power,source=PowerSource.FTMS.name),
                        PerformanceEvent(PerformanceChannel.SPEED,time,10.0,source="SPEED"),PerformanceEvent(PerformanceChannel.HEART_RATE,time,150.0,source="HEART_RATE"))))
            }
        }
        RideJournal.read(journal) { _,read,_,records->assertEquals(config,read);assertEquals(1201,records.count()) }
        val fit=folder.newFile("trainer.fit");FitWriter.encode(journal,fit)
        val plugin=ActivityFileValidationPlugin()
        val messages=fit.inputStream().use { FitDecoder().decode(it,plugin) }
        assertTrue(plugin.results.isNotEmpty());assertFalse(plugin.results.any { it.status==ActivityFileValidationResult.Status.FAILED })
        val session=messages.sessionMesgs.single()
        assertEquals(SubSport.INDOOR_CYCLING,session.subSport)
        assertEquals(SubSport.INDOOR_CYCLING,messages.lapMesgs.single().subSport)
        assertEquals(12000.0,session.totalDistance.toDouble(),.01)
        assertEquals(1200.0,session.totalMovingTime.toDouble(),.01)
        assertEquals(228000L,session.totalWork)
        assertNotNull(session.normalizedPower)
        val ftp=session.developerFields.single { it.name=="ESTIMATED_FTP" }.doubleValue
        assertEquals(180.5,ftp,.00001)
        assertTrue(messages.recordMesgs.any { it.power?.toInt()==0 && it.cadence?.toInt()==0 })
        assertTrue(messages.recordMesgs.all { it.positionLat==null && it.positionLong==null && it.motorPower==null })
        val read=FitRead.read(fit);assertNull(read.route);assertEquals(BikeType.INDOOR,read.details.bikeType)
        val details=RouteAnalysisRead.read(fit,ManagedRoute("trainer","Indoor",false,"FIT",read.details,false))
        assertNotNull(details.metrics[AnalysisMetric.NP]);assertEquals(180.5,details.metrics[AnalysisMetric.FTP]!!,.00001)
        assertNull(details.route)
    }
    @Test fun oldVersionFiveJournalRemainsReadableAsOutdoor() {
        val file=folder.newFile("old.ride")
        java.io.DataOutputStream(file.outputStream()).use {
            it.writeInt(0x42435935);it.writeLong(1000);it.writeUTF("SPEED");it.writeUTF("BIKE_DISTANCE");it.writeBoolean(false)
            it.writeInt(0);it.writeInt(3);it.writeInt(0)
            it.writeLong(1000);it.writeLong(0);it.writeDouble(Double.NaN);it.writeDouble(Double.NaN);it.writeInt(0)
        }
        RideJournal.read(file) { _,config,_,records -> assertFalse(config.indoor);assertEquals(1,records.count()) }
    }
    @Test fun detailsDoNotContinueAnIndoorZeroThroughMissingPowerWhenSpeedKeepsArriving() {
        val start=Instant.parse("2026-10-16T11:00:00Z").toEpochMilli()
        val journal=folder.newFile("indoor-gap.ride")
        val config=IndoorFields.config(RecordingConfig(setOf("SPEED","BIKE_DISTANCE","RIDER_POWER")))
        RideJournal(journal,start,config).use { writer -> for(second in 0..120) {
            val values=mutableMapOf("SPEED" to 10.0,"BIKE_DISTANCE" to second*10.0)
            if(second<=40) values["RIDER_POWER"]=if(second==40) 0.0 else 200.0
            writer.append(RideSample(start+second*1000L,second*1000L,values=values))
        } }
        val fit=folder.newFile("indoor-gap.fit");FitWriter.encode(journal,fit)
        val read=FitRead.read(fit)
        val details=RouteAnalysisRead.read(fit,ManagedRoute("gap","Indoor",false,"FIT",read.details,false))
        assertFalse(AnalysisMetric.NP in details.metrics)
        assertFalse(AnalysisMetric.WORK in details.metrics)
    }

}

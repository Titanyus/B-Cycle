package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class TrainingZonesTest {
    @Test fun cogganRangesAreInclusiveUnambiguousAndUsePersonalFtp() {
        assertEquals(listOf(0 to 137,138 to 187,188 to 225,226 to 262,263 to 300,301 to 375,376 to null),
            Coggan.zones(250)!!.map { it.lowerW to it.upperW })
        for (ftp in listOf(1,100,201,250,999,2500)) {
            val zones=Coggan.zones(ftp)!!
            for(w in 0..ftp*2) {
                val matches=zones.filter { w>=it.lowerW && (it.upperW==null || w<=it.upperW) }
                assertEquals(1,matches.size);assertEquals(matches.single().index,Coggan.zone(w.toDouble(),ftp))
            }
            Coggan.boundaries.forEachIndexed { i,p ->
                assertEquals(i+1,Coggan.zone(ftp*p/100.0,ftp))
                assertEquals(i+2,Coggan.zone(ftp*p/100.0+.0001,ftp))
            }
        }
        assertNull(Coggan.zones(null));assertNull(Coggan.zones(0));assertNull(Coggan.zone(Double.NaN,250));assertNull(Coggan.zone(-1.0,250))
    }
    @Test fun fractionalHeartBoundariesUseExactTargetsInsteadOfRoundedPercentDivision() {
        for(rest in 20..140 step 11) for(max in 60..250 step 13) if(max>rest) {
            val profile=RiderProfile(restingHeartRate=rest,maximumHeartRate=max)
            listOf(60,70,80,90).forEachIndexed { i,percent ->
                val boundary=rest+(max-rest)*percent/100.0
                assertEquals(i+2,TrainingSnapshot(profile,heart=boundary).heartZone)
                assertEquals(i+1,TrainingSnapshot(profile,heart=boundary-.000001).heartZone)
            }
            val model=profile.heartZones()!!
            for(bpm in rest..max) assertEquals(model.zoneFor(bpm)?.index,TrainingSnapshot(profile,heart=bpm.toDouble()).heartZone)
        }
    }
    @Test fun currentUsesTimeWeightedThreeSecondsZerosAndClearsOnMissingStaleOrReorderedData() {
        val window=TrainingDisplayWindow()
        assertEquals(100.0,window.update(100.0,0,0)!!,0.0)
        window.update(200.0,1000,1000)
        window.update(0.0,2000,2000)
        assertEquals(100.0,window.update(0.0,2000,3000)!!,.0001)
        assertEquals(200.0/3,window.update(0.0,2000,4000)!!,.0001)
        assertNull(window.update(0.0,2000,7001))
        assertEquals(300.0,window.update(300.0,8000,8000)!!,0.0)
        assertNull(window.update(null,null,8100))
        assertEquals(20.0,window.update(20.0,9000,9000)!!,0.0)
        assertNull(window.update(10.0,8900,9100))
    }
    @Test fun missingProfileAndUnavailableInputsDoNotInventZonesAndTripMeansAreReused() {
        val profile=RiderProfile(restingHeartRate=50,maximumHeartRate=150,ftpW=200)
        val presentation=TrainingPresentation()
        val s=presentation.update(TrainingInputs(180.0,1000,100.0,130.0,1000,120.0),profile,1000)
        assertEquals(3,s.powerZone);assertEquals(4,s.heartZone)
        assertEquals(90.0,s.powerPercent!!,0.0);assertEquals(80.0,s.heartPercent!!,0.0)
        assertEquals(100.0,s.averagePower!!,0.0);assertEquals(120.0,s.averageHeart!!,0.0)
        assertEquals(ZoneGlowColors.power(50.0),s.glow(ZoneGlowSource.POWER_AVERAGE))
        assertNull(s.glow(ZoneGlowSource.OFF))
        val stale=presentation.update(TrainingInputs(180.0,1000,100.0,130.0,1000,120.0),profile,6001)
        assertNull(stale.powerZone);assertNull(stale.heartZone)
        assertNull(stale.glow(ZoneGlowSource.POWER_AVERAGE));assertNull(stale.glow(ZoneGlowSource.HEART_AVERAGE))
        assertNull(s.copy(profile=RiderProfile()).powerZone);assertNull(s.copy(profile=RiderProfile()).heartZone)
        assertEquals(1,s.copy(heart=100.0).heartZone);assertEquals(5,s.copy(heart=150.0).heartZone)
        assertNull(s.copy(heart=99.0).heartZone);assertNull(s.copy(heart=151.0).heartZone)
        assertFalse(TrainingMetric.TRAINING_POWER_ZONE.value(stale).available)
    }
    @Test fun zoneColorsInterpolateAcrossEveryBoundaryWithoutChangingExactZone() {
        Coggan.boundaries.forEachIndexed { i,boundary ->
            assertEquals(ZoneGlowColors.powerColors[i],ZoneGlowColors.power(boundary-3.0))
            assertEquals(ZoneGlowColors.powerColors[i+1],ZoneGlowColors.power(boundary+3.0))
            assertEquals(ZoneGlowColors.mix(ZoneGlowColors.powerColors[i],ZoneGlowColors.powerColors[i+1],.5f),ZoneGlowColors.power(boundary.toDouble()))
            assertEquals(i+1,Coggan.zone(boundary*2.0,200));assertEquals(i+2,Coggan.zone(boundary*2.0+.001,200))
        }
        listOf(60,70,80,90).forEachIndexed { i,b ->
            assertEquals(TrainingZoneColors.values[i],ZoneGlowColors.heart(b-3.0))
            assertEquals(TrainingZoneColors.values[i+1],ZoneGlowColors.heart(b+3.0))
        }
    }
    @Test fun configurationIsIndependentPerTileRobustAndLegacyDefaultsToOff() {
        val config=ZoneGlowConfig().replace(2,ZoneGlowSource.POWER_CURRENT).replace(5,ZoneGlowSource.HEART_AVERAGE)
        assertEquals(config,ZoneGlowConfig.decode(config.encode()))
        assertTrue(ZoneGlowConfig.decode(null).sources.all { it==ZoneGlowSource.OFF })
        assertTrue(ZoneGlowConfig.decode("invalid").sources.all { it==ZoneGlowSource.OFF })
        assertEquals(ZoneGlowSource.OFF,config.sources[0])
        assertTrue(DashboardField.groups(LiveData()).none { it.source=="Leistung" })
        assertEquals(4,DashboardField.groups(LiveData(),includeTraining=true).single { it.source=="Leistung" }.fields.size)
        TrainingMetric.entries.forEach { assertNotNull(DashboardField.fromId(it.name)) }
        assertTrue(RecordField.options.none { it.id.startsWith("TRAINING_") })
    }
}

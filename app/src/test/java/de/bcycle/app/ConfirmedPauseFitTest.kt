package de.bcycle.app

import com.garmin.fit.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ConfirmedPauseFitTest {
    @get:Rule val folder=TemporaryFolder()
    @Test fun journalFitAndAnalysisShareNpRestWithoutStoppingTheWholeRideTimer() {
        val start=1790000000000L
        val journal=folder.newFile("rest.ride")
        val config=RecordingConfig(setOf("RIDER_POWER","NORMALIZED_POWER","GPS_ELAPSED_TIME"))
        val live=PerformanceEngine()
        RideJournal(journal,start,config).use { writer ->
            for(second in 0..720) {
                val rest=second in 60 until 660
                val watts=when { rest || second==720->null;second<60->200.0;else->300.0 }
                val t=second*1000L
                val sample=RideSample(start+t,t,values=buildMap {
                    put(ConfirmedBikePause.FLAG,if(rest) 1.0 else 0.0)
                    put("GPS_ELAPSED_TIME",second.toDouble());watts?.let { put("RIDER_POWER",it) }
                },performanceEvents=listOf(PerformanceEvent(PerformanceChannel.POWER,t,watts,source=PowerSource.BOSCH_RIDER.name)))
                live.acceptSample(sample);writer.append(sample)
            }
        }
        val replay=PerformanceEngine()
        RideJournal.read(journal) { _,_,_,rows -> rows.forEach(replay::acceptSample) }
        assertEquals(live.snapshot(),replay.snapshot())
        val fit=folder.newFile("rest.fit");FitWriter.encode(journal,fit)
        val messages=fit.inputStream().use { FitDecoder().decode(it) }
        assertEquals(720f,messages.sessionMesgs.single().totalElapsedTime,.001f)
        assertEquals(720f,messages.sessionMesgs.single().totalTimerTime,.001f)
        assertEquals(1,messages.sessionMesgs.size)
        assertEquals(live.snapshot().normalizedPowerW!!,messages.sessionMesgs.single().normalizedPower.toDouble(),.50001)
        val marker=messages.recordMesgs[100].developerFields.single { it.name==ConfirmedBikePause.FLAG }
        assertEquals(1.0,(marker.getValue(0) as Number).toDouble(),0.0)
        // Remove only the standard NP summary to prove actual read-side recomputation.
        val withoutSummary=folder.newFile("recompute.fit")
        val encoder=FileEncoder(withoutSummary,Fit.ProtocolVersion.V2_0)
        try { fit.inputStream().use { Decode().read(it,MesgListener { source ->
            val m=Mesg(source)
            if(m.num in setOf(18,19)) m.getField("normalized_power")?.let(m::removeField)
            encoder.write(m)
        }) } } finally { encoder.close() }
        val entry=ManagedRoute("recorded-"+"a".repeat(64),"Test",true,"FIT",RouteDetails(),false)
        val analysis=RouteAnalysisRead.read(withoutSummary,entry)
        assertEquals(live.snapshot().normalizedPowerW!!,analysis.metrics[AnalysisMetric.NP]!!,1e-7)
        assertEquals(600000L,analysis.powerQuality!!.npPausedMs)
        assertEquals(720.0,analysis.metrics[AnalysisMetric.ELAPSED]!!,.001)
        val masked=folder.newFile("masked.fit")
        FitReexport.encode(fit,masked,config)
        val withNp=masked.inputStream().use { FitDecoder().decode(it) }
        assertTrue(withNp.recordMesgs[100].developerFields.any { it.name==ConfirmedBikePause.FLAG })
        val rawOnly=folder.newFile("raw-only.fit")
        FitReexport.encode(fit,rawOnly,config.copy(fields=setOf("RIDER_POWER")))
        val withoutNp=rawOnly.inputStream().use { FitDecoder().decode(it) }
        assertTrue(withoutNp.recordMesgs.all { row -> row.developerFields.none { it.name==ConfirmedBikePause.FLAG } })
        assertNull(withoutNp.sessionMesgs.single().normalizedPower)
    }
}

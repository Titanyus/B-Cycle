package de.bcycle.app

import android.os.SystemClock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class BikeBatteryRecordingTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private fun live(runtime:RideRuntime) {
        val phase=runtime.connection.javaClass.getDeclaredField("phase").apply { isAccessible=true }
        phase.set(runtime.connection,phase.type.enumConstants!!.single { it.toString()=="LIVE" })
    }
    private fun sample(runtime:RideRuntime)=RideRuntime::class.java.getDeclaredMethod("sample").apply { isAccessible=true }.invoke(runtime) as RideSample
    @Test fun unchangedConnectedChargeIsStillRecordedAfterSeveralMinutesButDisconnectedDisplayChargeIsNot() {
        val runtime=RideRuntime.get(context);live(runtime)
        runtime.onSamples(listOf(McspSample(LiveMetric.BATTERY.address,80,true)))
        runtime.onData(LiveData(otherValues=mapOf(LiveMetric.BATTERY to 80L)),true)
        SystemClock.sleep(5*60_000)
        assertEquals(80.0,sample(runtime).values["BATTERY"]!!,0.0)
        runtime.connection.stop()
        assertEquals(80L,runtime.currentBikeBattery)
        assertNull(sample(runtime).values["BATTERY"])
        live(runtime) // A reconnected session still needs its own first reading.
        assertNull(sample(runtime).values["BATTERY"])
        runtime.onData(LiveData(otherValues=mapOf(LiveMetric.BATTERY to 101L)),true)
        assertNull(sample(runtime).values["BATTERY"])
        runtime.onData(LiveData(otherValues=mapOf(LiveMetric.BATTERY to 0L)),true)
        assertEquals(0.0,sample(runtime).values["BATTERY"]!!,0.0)
    }
    @Test fun privateAnalysisKeepsConsumptionWhenBatteryIsDisabledInTheExport() {
        val runtime=RideRuntime.get(context);live(runtime)
        val start=1_790_000_000_000L
        runtime.onData(LiveData(otherValues=mapOf(LiveMetric.BATTERY to 80L)),true)
        val first=sample(runtime).copy(utcMs=start,elapsedMs=0)
        SystemClock.sleep(300_000)
        val middle=sample(runtime).copy(utcMs=start+300_000,elapsedMs=300_000)
        runtime.onData(LiveData(otherValues=mapOf(LiveMetric.BATTERY to 70L)),true)
        SystemClock.sleep(300_000)
        val last=sample(runtime).copy(utcMs=start+600_000,elapsedMs=600_000)
        val journal=File(context.cacheDir,"charge.ride")
        RideJournal(journal,start,RecordingConfig(setOf("RIDER_POWER"))).use { writer -> listOf(first,middle,last).forEach(writer::append) }
        val exported=File(context.cacheDir,"charge-export.fit");FitWriter.encode(journal,exported)
        val id="recorded-"+"a".repeat(64)
        val entry=ManagedRoute(id,"Testfahrt",true,"FIT",RouteDetails(start),false)
        assertNull(RouteAnalysisRead.read(exported,entry).metrics[AnalysisMetric.BATTERY_USAGE])
        RideAnalysisFiles.retain(context.filesDir,id,journal)
        val privateAnalysis=RouteAnalysisRead.read(RideAnalysisFiles.file(context.filesDir,id),entry)
        assertEquals(10.0,privateAnalysis.metrics[AnalysisMetric.BATTERY_USAGE]!!,0.0)
    }
}

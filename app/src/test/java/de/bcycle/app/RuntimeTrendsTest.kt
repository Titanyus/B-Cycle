package de.bcycle.app

import android.os.SystemClock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RuntimeTrendsTest {
    @Test fun fitPreferenceAndFallbackDriveOneCachedSnapshotRegardlessOfDisplayedField() {
        val app=RuntimeEnvironment.getApplication();val r=RideRuntime.get(app)
        RideRuntime::class.java.getDeclaredField("gpsEnabled").apply { isAccessible=true }.setBoolean(r,true)
        val phase=r.connection.javaClass.getDeclaredField("phase").apply { isAccessible=true }
        phase.set(r.connection,phase.type.enumConstants!!.single { it.toString()=="LIVE" })
        val prefs=r.preferences
        fun prefer(source:RecordingSource) { prefs.edit().putString(RecordingSources.KEY,
            RecordingSources(mapOf(RecordingQuantity.SPEED to source)).json()).commit() }
        @Suppress("UNCHECKED_CAST")
        val speeds=RideRuntime::class.java.getDeclaredField("rawSpeeds").apply { isAccessible=true }.get(r) as MutableMap<RecordingSource,PerformanceEvent>
        r.onData(LiveData(speedHundredths=1000,otherValues=mapOf(LiveMetric.AVERAGE_SPEED to 2000L)),false)
        r.ride.gps.setCollecting(true)
        val observer:()->Unit={};r.observers+=observer
        var sample=0
        fun next(gps:Boolean=true) {
            ShadowSystemClock.advanceBy(Duration.ofSeconds(1));val now=SystemClock.elapsedRealtime()
            r.ride.gps.accept(GpsFix(now,50.0+sample++*.00005,8.0,3.0,speedMps=5.0),now)
            speeds[RecordingSource.BIKE]=PerformanceEvent(PerformanceChannel.SPEED,now,10.0/3.6)
            if(gps) speeds[RecordingSource.GPS]=PerformanceEvent(PerformanceChannel.SPEED,now,10.0)
            r.notifyChanged()
        }
        try {
            prefer(RecordingSource.BIKE);repeat(5) { next() }
            assertEquals(-3,r.trendSnapshot.speed)
            val cached=r.trendSnapshot
            repeat(50) { r.notifyChanged();assertSame(cached,r.trendSnapshot) }
            prefer(RecordingSource.GPS);repeat(5) { next() }
            assertEquals(3,r.trendSnapshot.speed)
            listOf("SPEED","GPS_SPEED","AVERAGE_SPEED","GPS_MOVING_AVERAGE","GPS_OVERALL_AVERAGE").forEach {
                assertEquals(3,r.trendSnapshot.indicator(DashboardField(it)))
            }
            repeat(10) { next(gps=false) }
            assertEquals(-3,r.trendSnapshot.speed) // Preferred GPS stale: same fallback as FIT.
            assertTrue(r.changeBikeType(BikeType.INDOOR));assertEquals(RideTrendSnapshot(),r.trendSnapshot)
        } finally { r.observers-=observer }
    }
}

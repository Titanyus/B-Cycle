package de.bcycle.app

import de.bcycle.hrv.Phase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvControllerTest {
    @Test fun delayedWorkerUsesStartRequestTimeAndIgnoresOlderQueuedNotification() {
        var now=0L;val controller=HrvController(RuntimeEnvironment.getApplication()) { now }
        val worker=HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as ThreadPoolExecutor
        fun drain() { worker.submit {}.get(10,TimeUnit.SECONDS) }
        val release=java.util.concurrent.CountDownLatch(1)
        try {
            drain();controller.packet(0,byteArrayOf(0x16,75,0,4));drain()
            worker.submit { release.await(5,TimeUnit.SECONDS) }
            now=1000;assertTrue(controller.start("Gurt"))
            now=1200
            controller.packet(900,byteArrayOf(0x16,75,0,4)) // Before Start, delivered late.
            controller.packet(1100,byteArrayOf(0x16,75,0,4)) // After Start, before worker resumes.
            release.countDown();drain()
            assertTrue(controller.busy);assertEquals(Phase.REST,controller.state.phase)
            controller.disconnected();drain()
            assertEquals(1,controller.state.result!!.getInt("packetCount"))
        } finally { release.countDown();controller.close() }
    }
    @Test fun notificationJitterDoesNotSplitGraphButLongMissingTimeDoes() {
        var now=0L;val controller=HrvController(RuntimeEnvironment.getApplication()) { now }
        val worker=HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as ThreadPoolExecutor
        fun packet(at:Long) {
            now=at;controller.packet(at,byteArrayOf(0x16,75,0x33,0x03)) // 819 ticks, about 800 ms.
            worker.submit {}.get(10,TimeUnit.SECONDS)
        }
        try {
            worker.submit {}.get(10,TimeUnit.SECONDS);packet(0);assertTrue(controller.start("Gurt"))
            worker.submit {}.get(10,TimeUnit.SECONDS)
            for(second in 1..32) packet(second*1000L)
            packet(34000) // 2 s arrival interval: RR plus a short delivery delay.
            assertFalse(controller.state.points.last().gap)
            packet(38000) // 4 s interval: substantial time unaccounted for by the RR.
            assertTrue(controller.state.points.last().gap)
            assertTrue(controller.state.points.all { it.rrMs==819*1000.0/1024 })
            assertEquals(Phase.MEASURE,controller.state.phase)
        } finally { controller.close() }
    }
    @Test fun completeMeasurementUsesPacketTimesStoresResultAndDisconnectAbortsNextSession() {
        var now=0L
        val context=RuntimeEnvironment.getApplication()
        HrvSettings(context).write(HrvMeasurementConfig(vibrateAtEnd=true,soundAtEnd=true))
        val vibrator=context.getSystemService(android.os.VibratorManager::class.java).defaultVibrator
        org.robolectric.Shadows.shadowOf(vibrator).setHasVibrator(true)
        val controller=HrvController(RuntimeEnvironment.getApplication()) { now }
        fun drain() {
            val worker=HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as ThreadPoolExecutor
            worker.submit {}.get(10,TimeUnit.SECONDS)
        }
        fun packet(second:Int) {
            now=second*1000L;val rr=819+(12*sin(second/10.0)).toInt()
            controller.packet(now,byteArrayOf(0x16,75,rr.toByte(),(rr ushr 8).toByte()));drain()
        }
        try {
            drain();packet(0);assertTrue(controller.state.ready)
            assertTrue(controller.start("Gurt"));drain()
            for(second in 1..210) packet(second)
            assertFalse(controller.busy)
            val result=controller.state.result!!
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(1,org.robolectric.shadows.ShadowToneGenerator.getPlayedTones().size)
            assertEquals(250L,org.robolectric.Shadows.shadowOf(vibrator).milliseconds)
            assertEquals(Phase.COMPLETE,controller.state.phase);assertTrue(result.getBoolean("acceptable"))
            assertTrue(result.getDouble("coverage")<.95);assertEquals(179,controller.state.points.size)
            assertTrue(controller.start("Gurt"));drain();controller.disconnected();drain()
            assertFalse(controller.busy);assertEquals(Phase.ABORTED,controller.state.phase)
            assertEquals("DISCONNECTED",controller.state.result!!.getString("problem"))
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(1,org.robolectric.shadows.ShadowToneGenerator.getPlayedTones().size)
        } finally { controller.close() }
    }
}

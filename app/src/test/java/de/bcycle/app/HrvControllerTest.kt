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
    @Test fun completeMeasurementUsesPacketTimesStoresResultAndDisconnectAbortsNextSession() {
        var now=0L
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
            assertEquals(Phase.COMPLETE,controller.state.phase);assertTrue(result.getBoolean("acceptable"))
            assertTrue(result.getDouble("coverage")<.95);assertEquals(179,controller.state.points.size)
            assertTrue(controller.start("Gurt"));drain();controller.disconnected();drain()
            assertFalse(controller.busy);assertEquals(Phase.ABORTED,controller.state.phase)
            assertEquals("DISCONNECTED",controller.state.result!!.getString("problem"))
        } finally { controller.close() }
    }
}

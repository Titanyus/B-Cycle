package de.bcycle.app

import de.bcycle.hrv.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvStoreTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private fun root()=File(c.cacheDir,"hrv-${UUID.randomUUID()}")
    @Test fun lowCoverageResultRawPacketsAndCorrectionsSurviveRestartAndPortableBackup() {
        val source=root();val target=root()
        try {
            val store=HrvStore(source);val id=UUID.randomUUID().toString();val session=MeasurementSession(0)
            store.begin(id,1000,"Testgurt",session)
            for(second in 1..209) {
                val rr=819+(12*sin(second/10.0)).toInt()
                session.accept(second*1000L,byteArrayOf(0x16,75,rr.toByte(),(rr ushr 8).toByte()))
                store.append(id,session.packets.last())
            }
            session.advance(210000)
            val row=store.save(id,session,Evaluate.session(session),210000)
            assertTrue(row.getBoolean("acceptable"));assertEquals("LOW_RR_COVERAGE",row.getJSONArray("warnings").getString(0))
            assertEquals(179,row.getJSONArray("rrTicks").length());assertTrue(row.getJSONArray("nnMs").length()>0)
            assertEquals(209,File(source,"$id/capture.jsonl").readLines().size)
            assertEquals(row.toString(),HrvStore(source).read(id).toString())
            val snapshot=store.backup(id);val restored=HrvStore(target)
            assertTrue(restored.restore(snapshot));assertFalse(restored.restore(snapshot))
            assertArrayEquals(snapshot,restored.backup(id));assertEquals(1,restored.list().size)
        } finally { source.deleteRecursively();target.deleteRecursively() }
    }
    @Test fun interruptedDraftRecoversAsAbortedAndNeverBecomesAValidMeasurement() {
        val root=root()
        try {
            val store=HrvStore(root);val id=UUID.randomUUID().toString();val session=MeasurementSession(0)
            store.begin(id,1000,"Gurt",session)
            session.accept(1000,byteArrayOf(0x16,75,0,4));store.append(id,session.packets.last())
            File(root,"$id/capture.jsonl").appendText("{incomplete")
            val reloaded=HrvStore(root);reloaded.recover()
            val result=reloaded.read(id)
            assertEquals("ABORTED",result.getString("phase"));assertFalse(result.getBoolean("acceptable"))
            assertEquals("PROCESS_INTERRUPTED",result.getString("problem"));assertTrue(result.isNull("corrected"))
        } finally { root.deleteRecursively() }
    }
}

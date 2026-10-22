package de.bcycle.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvHistorySummaryTest {
    @Test fun largeLegacyHistoriesKeepOnlySummariesAndLoadDetailsUnchanged() {
        val root=File(RuntimeEnvironment.getApplication().cacheDir,"history-${UUID.randomUUID()}")
        try {
            val store=HrvStore(root)
            val samples=(0 until 600).joinToString(",") { (800+it%20).toString() }
            val ids=mutableListOf<String>()
            for(count in listOf(100,500,1000)) {
                while(ids.size<count) {
                    val i=ids.size;val id=UUID.randomUUID().toString();ids+=id
                    File(root,id).mkdirs()
                    File(root,"$id/result.json").writeText("""{"format":"bicycle-hrv","schema":1,"id":"$id","utcMs":$i,"acceptable":${i%2==0},"corrected":${if(i%2==0) "{\"rmssdMs\":42.5}" else "null"},"rrTicks":[$samples],"nnMs":[$samples],"correctionPasses":[{"beforePeaks":[$samples]}]}""")
                }
                val started=System.nanoTime();val rows=store.list()
                println("HRV history $count: ${(System.nanoTime()-started)/1_000_000} ms; ${rows.size} scalar summaries")
                val baselineStart=System.nanoTime()
                val full=store.ids().map(store::read).sortedByDescending { it.optLong("utcMs") }
                println("HRV full JSON baseline $count: ${(System.nanoTime()-baselineStart)/1_000_000} ms; ${full.sumOf { it.getJSONArray("rrTicks").length()+it.getJSONArray("nnMs").length() }} retained samples")
                assertEquals(full.map { it.getString("id") },rows.map { it.id })
                assertEquals(count,rows.size)
                assertEquals(ids.reversed(),rows.map { it.id })
                assertFalse(rows.first().acceptable);assertNull(rows.first().rmssdMs)
                assertTrue(rows.last().acceptable);assertEquals(42.5,rows.last().rmssdMs!!,0.0)
            }
            val file=File(root,"${ids.first()}/result.json");val original=file.readBytes()
            val detail=store.read(ids.first())
            assertEquals(600,detail.getJSONArray("rrTicks").length())
            assertEquals(600,detail.getJSONArray("nnMs").length())
            assertEquals(600,detail.getJSONArray("correctionPasses").getJSONObject(0).getJSONArray("beforePeaks").length())
            assertArrayEquals(original,file.readBytes())
            // Invalid/interrupted rows do not hide valid history; no new on-disk index is required.
            val broken=UUID.randomUUID().toString();File(root,broken).mkdirs()
            File(root,"$broken/result.json").writeText("{incomplete")
            val wrong=UUID.randomUUID().toString();File(root,wrong).mkdirs()
            File(root,"$wrong/result.json").writeText(JSONObject(detail.toString()).put("id",ids.first()).toString())
            assertEquals(1000,store.list().size)
            assertFalse(root.walkTopDown().any { it.name=="index.json" })
        } finally { root.deleteRecursively() }
    }
}

package de.bcycle.hrv

import java.io.File
import java.util.Properties
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class ReferenceTest {
    @Test fun pinnedNeurokitGoldenCasesMatchPeaksClassesIterationsAndMetrics() {
        val fixtures = File("validation/fixtures").listFiles { f -> f.extension == "properties" }!!.sortedBy { it.name }
        assertEquals("Expected full reference corpus", 62, fixtures.size)
        val manifest = File("validation/fixtures/manifest.json").readText()
        for (file in fixtures) {
            val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            assertTrue("${file.name}: fixture checksum", manifest.contains("\"${file.name}\": \"$hash\""))
            val p = Properties().apply { file.reader().use(::load) }
            fun longs(key: String) = p.getProperty(key).split(',').filter { it.isNotEmpty() }.map(String::toLong)
            fun classes(prefix: String) = ArtifactCorrection.Classes(
                longs("$prefix.ectopic").map(Long::toInt), longs("$prefix.missed").map(Long::toInt),
                longs("$prefix.extra").map(Long::toInt), longs("$prefix.longshort").map(Long::toInt))
            val raw = longs("rawTicks").map(Long::toInt)
            val actual = ArtifactCorrection.correct(raw)
            assertEquals(file.name, longs("peaks"), actual.peaks)
            assertEquals(file.name, p.getProperty("passCount").toInt(), actual.passes.size)
            actual.passes.forEachIndexed { i, pass ->
                assertEquals("${file.name} pass $i classes", classes("pass.$i"), pass.classes)
                assertEquals(file.name, longs("pass.$i.before"), pass.beforePeaks)
                assertEquals(file.name, longs("pass.$i.after"), pass.afterPeaks)
            }
            assertEquals(file.name, classes("residual"), actual.residual)
            for ((prefix, metrics) in listOf("raw" to Metrics.fromNnMs(raw.map { it * 1000.0 / 1024 }),
                "corrected" to Metrics.fromNnMs(actual.nnMs))) {
                fun check(key: String, value: Double, tolerance: Double = 1e-6) =
                    assertEquals("${file.name} $prefix.$key", p.getProperty("$prefix.$key").toDouble(), value, tolerance)
                check("mean", metrics.meanNnMs); check("rmssd", metrics.rmssdMs)
                check("sdnn", metrics.sdnnMs); check("hr", metrics.restingHrBpm)
                check("pnn50_pairs", metrics.pnn50Percent); check("pnn50_n", metrics.pnn50NeurokitPercent)
                if (p.getProperty("$prefix.lnrmssd") == "null") assertNull(metrics.lnRmssd)
                else check("lnrmssd", metrics.lnRmssd!!, 1e-9)
            }
        }
    }

    @Test fun analyticMetricsAndStrictNn50BoundaryAreIndependentOfReferencePort() {
        val m = Metrics.fromNnMs(listOf(1000.0, 1050.0, 1101.0))
        assertEquals(50.502475186865844, m.rmssdMs, 1e-10)
        assertEquals(1, m.nn50Count)
        assertEquals(2, m.adjacentPairCount)
        assertEquals(50.0, m.pnn50Percent, 0.0)
        assertEquals(100.0 / 3, m.pnn50NeurokitPercent, 1e-10)
        val constant = Metrics.fromNnMs(List(20) { 1000.0 })
        assertNull(constant.lnRmssd); assertEquals(0.0, constant.sdnnMs, 0.0)
        assertEquals(60.0, constant.restingHrBpm, 0.0)
    }

    @Test fun affectedIntervalsReferenceRawIndicesInsteadOfCountingEveryIteration() {
        val ticks = MutableList(180) { i -> 900 + (30 * kotlin.math.sin(i * 0.3)).toInt() }
        ticks[80] += 300; ticks[81] -= 300
        val result = ArtifactCorrection.correct(ticks)
        assertTrue(result.affectedRawIndices.containsAll(listOf(80, 81)))
        assertEquals(result.affectedRawIndices.distinct(), result.affectedRawIndices)
        assertEquals(180, ticks.size)
    }
}

package de.bcycle.hrv

import java.io.File
import java.util.Properties
import org.junit.Assert.*
import org.junit.Test

class CorrectionPolicyTest {
    @Test fun measurementProfileNeverIncreasesDetectedResidualsAcrossReferenceCorpus() {
        for(file in File("validation/fixtures").listFiles { f -> f.extension=="properties" }!!) {
            val fixture=Properties().apply { file.reader().use(::load) }
            val raw=fixture.getProperty("rawTicks").split(',').map(String::toInt)
            val original=buildList { add(0L);raw.forEach { add(last()+it) } }
            val result=ArtifactCorrection.correctMeasurement(raw)
            assertTrue(file.name,result.residual.count<=ArtifactCorrection.classify(original).count)
            assertTrue(file.name,result.residual.count<=ArtifactCorrection.correct(raw).residual.count)
            assertTrue(file.name,result.peaks.zipWithNext().all { (a,b) -> b>a })
            assertEquals(file.name,original.first(),result.peaks.first())
            assertEquals(file.name,original.last(),result.peaks.last())
        }
    }
    @Test fun worseningEdgeCorrectionKeepsOriginalInsteadOfAddingResiduals() {
        val p=Properties().apply { File("validation/fixtures/misaligned_1.properties").reader().use(::load) }
        val raw=p.getProperty("rawTicks").split(',').map(String::toInt)
        val original=buildList { add(0L);raw.forEach { add(last()+it) } }
        val initial=ArtifactCorrection.classify(original)
        val reference=ArtifactCorrection.correct(raw)
        assertTrue(reference.residual.count>initial.count)
        val actual=ArtifactCorrection.correctMeasurement(raw)
        assertEquals(original,actual.peaks)
        assertEquals(initial,actual.residual)
        assertTrue(actual.passes.isEmpty())
    }

    @Test fun correctedSeriesAboveFivePercentDisplaysWithWarningAndOriginalDataIntact() {
        val session=MeasurementSession(0)
        for(second in 1..209) session.accept(second*1000L,byteArrayOf(0x16,60,0,4))
        session.advance(210000)
        session.rrTicks.clear()
        val fixture=Properties().apply { File("validation/fixtures/mixed_seed_16.properties").reader().use(::load) }
        val raw=fixture.getProperty("rawTicks").split(',').map(String::toInt)
        session.rrTicks.addAll(raw)
        val value=Evaluate.session(session)
        assertTrue(value.reasons.toString(),value.acceptable)
        assertTrue(value.affectedFraction>.05)
        assertTrue("HIGH_CORRECTION" in value.warnings)
        assertFalse("HIGH_CORRECTION" in value.reasons)
        assertEquals(0,value.correction!!.residual.count)
        assertNotNull(value.corrected?.lnRmssd)
        assertEquals(raw,session.rrTicks)
        var previous=ArtifactCorrection.classify(buildList { add(0L);raw.forEach { add(last()+it) } }).count
        for(pass in value.correction.passes) {
            val next=ArtifactCorrection.classify(pass.afterPeaks).count
            assertTrue(next<previous);previous=next
        }
    }

    @Test fun unresolvedEdgeArtefactsStillRejectInsteadOfBeingHiddenByTolerance() {
        val p=Properties().apply { File("validation/fixtures/misaligned_1.properties").reader().use(::load) }
        val session=MeasurementSession(0)
        for(second in 1..209) session.accept(second*1000L,byteArrayOf(0x16,60,0,4))
        session.advance(210000)
        session.rrTicks.clear();session.rrTicks.addAll(p.getProperty("rawTicks").split(',').map(String::toInt))
        val value=Evaluate.session(session)
        assertFalse(value.acceptable)
        assertTrue("RESIDUAL_ARTIFACTS" in value.reasons)
        assertTrue(value.correction!!.residual.count>0)
    }
}

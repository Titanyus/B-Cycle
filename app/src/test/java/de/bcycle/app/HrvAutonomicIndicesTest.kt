package de.bcycle.app

import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class HrvAutonomicIndicesTest {
    private val day=LocalDate.of(2026,11,1)
    private fun row(age:Long,log:Double=3.0,hr:Double=55.0,valid:Boolean=true)=HrvHistorySummary(
        "day-$age",day.minusDays(age).atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),valid,exp(log),
        zoneId="UTC",metrics=mapOf("restingHrBpm" to hr),restMs=60000,measureMs=180000)
    private fun reference()=(1L..14L).map { row(it,if(it%2==0L) 3.1 else 2.9,if(it%2==0L) 56.0 else 54.0) }
    @Test fun separateSampleStandardizedMetricsExcludeTodayFutureAndOldAndDoNotClamp() {
        val rows=reference()+row(0,3.4,53.0)+row(-1,5.0,100.0)+row(61,5.0,100.0)
        val indices=HrvAutonomicIndices.from(rows,day)
        assertEquals(.4/(.1*sqrt(14.0/13)),indices.pns!!,1e-10)
        assertEquals(-2/sqrt(14.0/13),indices.activation!!,1e-10)
        assertTrue(indices.pns!!>2)
    }
    @Test fun missingInsufficientConstantAndInvalidDataHaveDistinctReferenceCounts() {
        assertEquals(HrvAutonomicIndices(),HrvAutonomicIndices.from(reference(),day))
        val short=HrvAutonomicIndices.from(reference().drop(1)+row(0),day)
        assertNull(short.pns);assertNull(short.activation);assertEquals(13,short.pnsReferenceDays);assertEquals(13,short.activationReferenceDays)
        val constant=HrvAutonomicIndices.from((0L..14L).map { row(it) },day)
        assertNull(constant.pns);assertNull(constant.activation);assertEquals(14,constant.pnsReferenceDays)
        assertEquals(HrvAutonomicIndices(),HrvAutonomicIndices.from(reference()+row(0,valid=false),day))
        val changedProtocol=HrvAutonomicIndices.from(reference()+row(0).copy(measureMs=60000),day)
        assertNotNull(changedProtocol.pns);assertNotNull(changedProtocol.activation)
        val withoutPulse=HrvAutonomicIndices.from(reference()+row(0).copy(metrics=emptyMap()),day)
        assertNotNull(withoutPulse.pns);assertNull(withoutPulse.activation);assertNull(withoutPulse.activationReferenceDays)
    }
    @Test fun importedAndNativeDaysFormOneReferenceAcrossProtocolChanges() {
        val mixed=reference().mapIndexed { index,r ->
            if(index<8) r.copy(acceptable=false,importedDay=day.minusDays(index+1L).toString(),restMs=null,measureMs=null)
            else r.copy(restMs=45000,measureMs=60000)
        }
        val result=HrvAutonomicIndices.from(mixed+row(0,3.4,53.0),day)
        assertEquals(14,result.pnsReferenceDays);assertEquals(14,result.activationReferenceDays)
        assertEquals(.4/(.1*sqrt(14.0/13)),result.pns!!,1e-10)
        assertEquals(-2/sqrt(14.0/13),result.activation!!,1e-10)
    }
    @Test fun lastFailedMeasurementDoesNotResurrectEarlierSuccessfulValue() {
        val newer=row(0,valid=false).copy(id="failed",utcMs=row(0).utcMs+1000)
        assertEquals(HrvAutonomicIndices(),HrvAutonomicIndices.from(reference()+row(0)+newer,day))
    }
}

package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.ln

class HrvDailyAssessmentTest {
    private val day=LocalDate.of(2026,10,28)
    private fun row(date:LocalDate,value:Double,acceptable:Boolean=true,id:String=date.toString(),hour:Int=7)=HrvHistorySummary(id,date.atTime(hour,0).toInstant(ZoneOffset.UTC).toEpochMilli(),acceptable,value,zoneId="UTC",metrics=mapOf("rmssdMs" to value,"restingHrBpm" to 52.0))
    @Test fun logarithmicValueIsTransparentUnclampedAndRejectsUndefinedInputs() {
        assertEquals(2*ln(40.0),HrvDailyAssessment.score(40.0)!!,1e-12)
        assertEquals(0.0,HrvDailyAssessment.score(1.0)!!,0.0)
        assertTrue(HrvDailyAssessment.score(.5)!!<0);assertTrue(HrvDailyAssessment.score(1000.0)!!>10)
        for(value in listOf(null,0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY)) assertNull(HrvDailyAssessment.score(value))
    }
    @Test fun fourCalendarDaysIncludeTodayAndMissingDaysDoNotBecomeGoodOrNormal() {
        val rows=(1L..20L).map { row(day.minusDays(it),40.0+it%5) }+row(day,25.0)
        val answers=mapOf(day to HrvQuestionnaire(day.toString(),ratings=mapOf("sleep" to 10)),
            day.minusDays(3) to HrvQuestionnaire(day.minusDays(3).toString(),ratings=mapOf("sleep" to 0)),
            day.minusDays(4) to HrvQuestionnaire(day.minusDays(4).toString(),ratings=mapOf("sleep" to 0)),
            day.plusDays(1) to HrvQuestionnaire(day.plusDays(1).toString(),ratings=mapOf("sleep" to 0)))
        val value=HrvDailyAssessment.from(rows,answers,day.toString())
        assertEquals(day,value.day);assertEquals(2,value.subjectiveDays);assertEquals(5.0,value.subjectiveBurden!!,0.0)
        assertEquals(HrvSubjectiveState.MIXED,value.subjectiveState);assertEquals(HrvDayPosition.LOW,value.position)
        assertEquals(HrvDayAdvice.LIMIT,value.advice)
        val reference=HrvTrends.series(rows,HrvTrendMetric.RMSSD).last()
        assertEquals(reference,value.hrv);assertEquals(2*ln(reference.lower!!),value.lower!!,1e-12)
        assertEquals(52.0,value.pulse!!.value!!,0.0)
    }
    @Test fun normalAndHighHaveTheSameAdviceAndLowCombinesWithSubjectiveContext() {
        val base=HrvTrendPoint(row(day,50.0),day,"RMSSD",50.0,null,40.0,60.0)
        fun assess(value:Double,burden:Double?)=HrvDailyAssessment(day,base.copy(value=value),null,burden,if(burden==null) 0 else 4)
        for(value in listOf(40.0,50.0,60.0,80.0)) {
            assertEquals(HrvDayAdvice.PLAN,assess(value,0.0).advice)
            assertEquals(HrvDayAdvice.LIMIT,assess(value,8.0).advice)
        }
        assertEquals(HrvDayPosition.NORMAL,assess(40.0,0.0).position);assertEquals(HrvDayPosition.HIGH,assess(80.0,0.0).position)
        assertEquals(HrvDayAdvice.LIMIT,assess(39.0,0.0).advice);assertEquals(HrvDayAdvice.LIMIT,assess(39.0,null).advice)
        assertEquals(HrvDayAdvice.EASY,assess(39.0,8.0).advice)
        assertEquals(HrvDayAdvice.COLLECTING,HrvDailyAssessment(day,base.copy(lower=null,upper=null),null,8.0,4).advice)
        assertEquals(HrvDayAdvice.COLLECTING,HrvDailyAssessment(day,base.copy(lower=0.0,upper=0.0),null,0.0,4).advice)
    }
    @Test fun lastMeasurementRuleAndUnavailableValuesArePreserved() {
        val rows=(1L..20L).map { row(day.minusDays(it),40.0) }+listOf(row(day,40.0,id="old",hour=7),row(day,30.0,acceptable=false,id="new",hour=8))
        val value=HrvDailyAssessment.from(rows,emptyMap(),"old")
        assertEquals("new",value.hrv.row.id);assertNull(value.score);assertEquals(HrvDayAdvice.UNAVAILABLE,value.advice)
        assertEquals(HrvSubjectiveState.UNKNOWN,value.subjectiveState)
        val fresh=HrvDailyAssessment.from(listOf(row(day,50.0)),emptyMap(),day.toString())
        assertNotNull(fresh.score);assertNull(fresh.lower);assertEquals(HrvDayAdvice.COLLECTING,fresh.advice)
    }
}

package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.IsoFields

class HrvTrendPeriodTest {
    private fun row(day:LocalDate,value:Double,id:String=day.toString(),acceptable:Boolean=true,metrics:Map<String,Double> = mapOf("rmssdMs" to value))=
        HrvHistorySummary(id,day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),acceptable,value,zoneId="UTC",metrics=metrics)
    @Test fun calendarBucketsRespectIsoYearsLeapFebruaryAndEqualMonthWidths() {
        val jan=LocalDate.of(2021,1,1)
        assertEquals(LocalDate.of(2020,12,28),HrvTrendPeriod.WEEK.start(jan))
        assertEquals(53,HrvTrendPeriod.WEEK.start(jan).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        assertEquals(2020,HrvTrendPeriod.WEEK.start(jan).get(IsoFields.WEEK_BASED_YEAR))
        assertEquals(LocalDate.of(2021,1,3),HrvTrendPeriod.WEEK.end(jan))
        assertEquals(LocalDate.of(2024,2,29),HrvTrendPeriod.MONTH.end(LocalDate.of(2024,2,4)))
        for(mode in HrvTrendPeriod.entries) for(day in listOf(jan,LocalDate.of(1969,12,31),LocalDate.of(2024,2,29),LocalDate.of(2026,12,31))) {
            val start=mode.start(day)
            assertEquals(start,mode.date(mode.index(day)))
            assertEquals(1,mode.index(mode.next(start))-mode.index(start))
        }
    }
    @Test fun latestDailySelectionMissingValuesAndInvalidLatestCapturesNeverBiasTheMean() {
        val monday=LocalDate.of(2024,1,1)
        val rows=listOf(row(monday,10.0),row(monday,30.0,"later").copy(utcMs=row(monday,10.0).utcMs+1000),
            row(monday.plusDays(1),50.0),row(monday.plusDays(1),999.0,"rejected",false).copy(utcMs=row(monday.plusDays(1),0.0).utcMs+1000),
            row(monday.plusDays(3),50.0),row(monday.plusDays(4),123.0,metrics=emptyMap()).copy(rmssdMs=null))
        val daily=HrvTrends.series(rows,HrvTrendMetric.RMSSD)
        val weekly=HrvTrendAggregation.series(daily,HrvTrendPeriod.WEEK).single()
        assertEquals(40.0,weekly.value!!,0.0);assertEquals(2,weekly.sampleDays)
        assertEquals(monday,weekly.day);assertEquals(monday.plusDays(6),weekly.periodEnd)
        assertNull(weekly.average);assertNull(weekly.lower)
        assertSame(daily,HrvTrendAggregation.series(daily,HrvTrendPeriod.DAY))
    }
    @Test fun monthlyMeanWeightsDaysNotWeeksAndLeavesMissingMonthsAsGaps() {
        val jan=LocalDate.of(2024,1,1)
        val daily=HrvTrends.series(listOf(row(jan,10.0),row(jan.plusDays(6),10.0),row(jan.plusDays(7),70.0),
            row(LocalDate.of(2024,3,1),20.0)),HrvTrendMetric.RMSSD)
        val months=HrvTrendAggregation.series(daily,HrvTrendPeriod.MONTH)
        assertEquals(listOf(jan,LocalDate.of(2024,3,1)),months.map { it.day })
        assertEquals(30.0,months.first().value!!,0.0);assertEquals(3,months.first().sampleDays)
        assertEquals(31,months.first().periodEnd.dayOfMonth)
        val empty=HrvTrendAggregation.series(HrvTrends.series(listOf(row(jan,20.0,acceptable=false)),HrvTrendMetric.HR),HrvTrendPeriod.MONTH).single()
        assertNull(empty.value);assertEquals(0,empty.sampleDays);assertNull(empty.average);assertNull(empty.lower)
    }
    @Test fun trendAndBandSummarizeDailyReferencesWithoutTurningSevenDaysIntoSevenWeeks() {
        val start=LocalDate.of(2024,1,1)
        val daily=HrvTrends.series((0..100).map { i -> row(start.plusDays(i.toLong()),20.0+i) },HrvTrendMetric.RMSSD)
        for(mode in listOf(HrvTrendPeriod.WEEK,HrvTrendPeriod.MONTH)) {
            val grouped=HrvTrendAggregation.series(daily,mode)
            for(bucket in grouped) {
                val members=daily.filter { it.day in bucket.day..bucket.periodEnd }
                assertEquals(members.mapNotNull { it.value }.average(),bucket.value!!,1e-9)
                val trends=members.mapNotNull { it.average };val lows=members.mapNotNull { it.lower };val highs=members.mapNotNull { it.upper }
                assertEquals(trends.size,bucket.trendDays);assertEquals(lows.size,bucket.bandDays)
                if(trends.isEmpty()) assertNull(bucket.average) else assertEquals(trends.average(),bucket.average!!,1e-9)
                if(lows.isEmpty()) assertNull(bucket.lower) else {
                    assertEquals(lows.average(),bucket.lower!!,1e-9);assertEquals(highs.average(),bucket.upper!!,1e-9)
                }
            }
        }
    }
}

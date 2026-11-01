package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.ln

class HrvScoreTrendTest {
    private val first=LocalDate.of(2026,9,1)
    private fun row(offset:Int,value:Double)=HrvHistorySummary("r$offset",first.plusDays(offset.toLong()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),true,value,zoneId="UTC")
    @Test fun scoreAndCorridorMatchTheDailyOverviewAndAggregateWithoutChangingItsReference() {
        val rows=(0..30).map { row(it,30.0+it%9) }
        val scores=HrvTrends.series(rows,HrvTrendMetric.HRV)
        val overview=HrvDailyAssessment.forDay(rows,emptyMap(),first.plusDays(30))
        assertEquals(overview.score!!,scores.last().value!!,1e-12)
        assertEquals(overview.lower!!,scores.last().lower!!,1e-12)
        assertEquals(overview.upper!!,scores.last().upper!!,1e-12)
        val grouped=HrvTrendAggregation.series(scores,HrvTrendPeriod.MONTH)
        assertEquals(scores.take(30).mapNotNull { it.value }.average(),grouped.first().value!!,1e-12)
    }
    @Test fun rollingMeanAveragesActualScoreValuesAndRequiresFiveValidDays() {
        val values=listOf(1.0,2.0,4.0,8.0,16.0,32.0,64.0,128.0)
        val scores=HrvTrends.series(values.mapIndexed(::row),HrvTrendMetric.HRV)
        assertTrue(scores.take(4).all { it.average==null })
        assertEquals(values.take(5).map { 2*ln(it) }.average(),scores[4].average!!,1e-12)
        assertEquals(values.takeLast(7).map { 2*ln(it) }.average(),scores.last().average!!,1e-12)
        assertNotEquals(2*ln(values.takeLast(7).average()),scores.last().average!!,.01)
        val missing=HrvTrends.series(listOf(row(0,0.0),row(1,Double.NaN)),HrvTrendMetric.HRV)
        assertTrue(missing.all { it.value==null && it.average==null })
    }
    @Test fun dimensionlessScoreAxisSupportsNegativeAndAboveTenValues() {
        val scores=HrvTrends.series(listOf(row(0,.1),row(1,1000000.0)),HrvTrendMetric.HRV)
        val axis=HrvTrendAxis.from(scores,HrvTrendMetric.HRV)
        assertTrue(axis.minimum<scores.first().value!!);assertTrue(axis.maximum>scores.last().value!!)
        assertTrue(axis.minimum<0);assertTrue(axis.maximum>10)
    }
}

package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class RouteChartScaleTest {
    @Test fun speedRoundsUpToFiveAndKeepsTenAlignedInteriorMarks() {
        val scale=RouteChartScale.calculate(AnalysisChart.SPEED,listOf(3.0,38.0))
        assertEquals(0.0,scale.minimum,0.0);assertEquals(40.0,scale.maximum,0.0)
        assertEquals(listOf(10.0,20.0,30.0),scale.interior)
        for(max in 1..150) {
            val s=RouteChartScale.calculate(AnalysisChart.SPEED,listOf(max.toDouble()))
            assertEquals(0.0,s.maximum%5.0,0.0);assertTrue(s.maximum>=max)
            assertTrue(s.interior.size<=3);assertTrue(s.interior.all { it%10.0==0.0 })
        }
    }
    @Test fun cadenceAndPowerUseRoundEvenMarksAndFiveUnitCeilings() {
        for(chart in listOf(AnalysisChart.CADENCE,AnalysisChart.POWER)) for(max in listOf(58.0,97.0,118.0,326.0,487.0,2031.0)) {
            val s=RouteChartScale.calculate(chart,listOf(0.0,max))
            assertEquals(0.0,s.minimum,0.0);assertEquals(kotlin.math.ceil(max/5)*5,s.maximum,0.0)
            assertTrue(s.interior.size in 2..3);assertTrue(s.interior.all { it%2.0==0.0 && it>s.minimum && it<s.maximum })
        }
    }
    @Test fun pulseAndHeightStartAtMeasuredMinimumAndDoNotAddPercentagePadding() {
        val pulse=RouteChartScale.calculate(AnalysisChart.HEART,listOf(98.0,159.0))
        assertEquals(98.0,pulse.minimum,0.0);assertEquals(160.0,pulse.maximum,0.0)
        assertTrue(pulse.interior.size in 2..3)
        val height=RouteChartScale.calculate(AnalysisChart.ELEVATION,listOf(487.3,532.1))
        assertEquals(487.3,height.minimum,0.0);assertEquals(535.0,height.maximum,0.0)
        assertTrue(height.interior.size in 2..3)
        val negative=RouteChartScale.calculate(AnalysisChart.ELEVATION,listOf(-12.0,-3.0))
        assertEquals(-12.0,negative.minimum,0.0);assertEquals(0.0,negative.maximum,0.0)
    }
    @Test fun flatMissingAndNonfiniteDataNeverCauseInvalidOrOvercrowdedAxes() {
        for(chart in AnalysisChart.entries) for(values in listOf(emptyList(),listOf(0.0),listOf(100.0,100.0),listOf(Double.NaN,Double.POSITIVE_INFINITY,14.0))) {
            val s=RouteChartScale.calculate(chart,values)
            assertTrue(s.minimum.isFinite());assertTrue(s.maximum.isFinite());assertTrue(s.maximum>s.minimum)
            assertTrue(s.interior.size<=3);assertEquals(s.marks.sorted().distinct(),s.marks)
        }
    }
}

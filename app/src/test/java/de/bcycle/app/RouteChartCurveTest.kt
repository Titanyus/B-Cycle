package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RouteChartCurveTest {
    @Test fun smoothingIsDisplayOnlyPixelAdaptiveAndPreservesZerosPeaksAndGaps() {
        val points = (0..100).map { i -> AnalysisPoint(i.toDouble(), i.toDouble(),
            when { i == 30 -> null; i in 50..55 -> 0.0; i == 70 -> 400.0; else -> if (i % 2 == 0) 100.0 else 102.0 }) }
        val series = AnalysisSeries(AnalysisChart.POWER, points)
        val original = points.toList()
        val segments = RouteChartCurve.prepare(series, false, 100.0, 30f)
        assertEquals(2, segments.size)
        assertEquals(29.0, segments[0].last().x, 0.0); assertEquals(31.0, segments[1].first().x, 0.0)
        val all = segments.flatten()
        assertTrue(all.filter { it.x in 50.0..55.0 }.all { it.value == 0.0 })
        assertEquals(400.0, all.single { it.x == 70.0 }.value, 0.0)
        assertEquals(100.0, all.single { it.x == 56.0 }.value, 0.0)
        assertTrue(all.any { p -> p.value != points[p.x.toInt()].value })
        assertEquals(original, series.points)
        val detailed = RouteChartCurve.prepare(series, false, 100.0, 1000f).flatten()
        assertEquals(102.0, detailed.single { it.x == 5.0 }.value, 0.0)
    }
    @Test fun monotoneCubicsNeverOvershootOrInventNegativePowerAndHandleDuplicatePositions() {
        val points = listOf(0.0 to 0.0, 1.0 to 100.0, 1.1 to 400.0, 3.0 to 10.0,
            4.0 to 0.0, 5.0 to 0.0, 5.0 to 90.0, 6.0 to 91.0).map { RouteCurvePoint(it.first,it.second) }
        val slopes = RouteChartCurve.tangents(points)
        points.zipWithNext().forEachIndexed { i, (a,b) ->
            val dx = b.x - a.x
            if (dx > 0) for (sample in 0..100) {
                val t = sample / 100.0
                val value = (2*t*t*t-3*t*t+1)*a.value + (t*t*t-2*t*t+t)*dx*slopes[i] +
                    (-2*t*t*t+3*t*t)*b.value + (t*t*t-t*t)*dx*slopes[i+1]
                assertTrue("$i/$t: $value", value >= min(a.value,b.value)-1e-8 && value <= max(a.value,b.value)+1e-8)
            }
        }
        assertTrue(slopes.all { it.isFinite() })
    }
    @Test fun distanceAndTimeProjectionAndInvalidAxisSamplesKeepBreaks() {
        val series = AnalysisSeries(AnalysisChart.CADENCE, listOf(AnalysisPoint(0.0,0.0,90.0),
            AnalysisPoint(1000.0,10.0,91.0),AnalysisPoint(null,20.0,92.0),AnalysisPoint(2000.0,30.0,93.0)))
        val distance = RouteChartCurve.prepare(series,true,2.0,500f)
        assertEquals(2,distance.size);assertEquals(1.0,distance[0].last().x,0.0)
        assertEquals(1,RouteChartCurve.prepare(series,false,30.0,500f).size)
    }
}

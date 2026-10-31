package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class RouteChartXAxisTest {
    @Test fun roundTicksStayAnchoredWhileExactViewportEdgesMove() {
        val full=RouteChartXAxis.calculate(0.0,61.5)
        assertEquals(20.0,full.step,0.0)
        assertEquals(listOf(0.0,20.0,40.0,60.0,80.0),full.marks)
        assertEquals(61.5,full.end,0.0)
        val a=RouteChartXAxis.calculate(10.3,30.3)
        val b=RouteChartXAxis.calculate(10.8,30.8)
        assertEquals(5.0,a.step,0.0);assertEquals(a.marks,b.marks)
        assertEquals(10.3,a.start,0.0);assertEquals(30.8,b.end,0.0)
        // Coordinates change continuously; tick values never follow the viewport fractions.
        assertEquals(.5,(15-a.start)-(15-b.start),.000001)
    }
    @Test fun zoomUsesOneTwoFiveStepsAndKeepsOverscanForClippedLabels() {
        for(span in listOf(.013,.15,.6,1.0,3.0,10.0,61.5,200.0,1234.0)) {
            val axis=RouteChartXAxis.calculate(span*.37,span*1.37)
            val unit=Math.pow(10.0,kotlin.math.floor(kotlin.math.log10(axis.step)))
            assertTrue(listOf(1.0,2.0,5.0,10.0).any { kotlin.math.abs(axis.step/unit-it)<.000001 })
            assertTrue(axis.marks.first()<=axis.start);assertTrue(axis.marks.last()>=axis.end)
            assertTrue(axis.marks.size in 3..9)
            assertTrue(axis.marks.all { kotlin.math.abs(it/axis.step-kotlin.math.round(it/axis.step))<.000001 })
        }
        assertEquals(2.0,RouteChartXAxis.calculate(20.0,30.0).step,0.0)
        assertEquals(.2,RouteChartXAxis.calculate(23.0,24.0).step,.000001)
        assertEquals(3,RouteChartXAxis.calculate(0.0,.02).decimals)
    }
}

package de.bcycle.app

import kotlin.math.ceil
import kotlin.math.floor

/** Fixed five-unit upper limit and at most three round interior marks; display only. */
internal data class RouteChartScale(val minimum: Double, val maximum: Double, val interior: List<Double>) {
    val marks get() = listOf(minimum) + interior + maximum
    companion object {
        fun calculate(chart: AnalysisChart, values: List<Double>): RouteChartScale {
            val valid=values.filter { it.isFinite() && (chart==AnalysisChart.ELEVATION || it>=0) }
            val minimum=if(chart==AnalysisChart.HEART || chart==AnalysisChart.ELEVATION) valid.minOrNull() ?: 0.0 else 0.0
            val peak=valid.maxOrNull() ?: minimum
            var maximum=ceil(peak/5.0)*5.0
            if(maximum<=minimum) maximum=ceil(minimum/5.0)*5.0+5.0
            val base=if(chart==AnalysisChart.SPEED) 10.0 else 2.0
            fun ticks(step: Double): List<Double>? {
                val first=(floor(minimum/step)+1)*step
                val count=(ceil((maximum-first)/step)).toInt().coerceAtLeast(0)
                if(count>5) return null
                return List(count) { first+it*step }.filter { it-minimum>=(maximum-minimum)*.12 && maximum-it>=(maximum-minimum)*.12 }
            }
            var fallback=emptyList<Double>()
            for(exponent in 0..12) for(multiplier in listOf(1.0,2.0,3.0,4.0,5.0,6.0)) {
                val step=base*multiplier*Math.pow(10.0,exponent.toDouble())
                val marks=ticks(step) ?: continue
                if(marks.size in 2..3) return RouteChartScale(minimum,maximum,marks)
                if(marks.size<=3 && marks.size>fallback.size) fallback=marks
            }
            return RouteChartScale(minimum,maximum,fallback)
        }
    }
}

package de.bcycle.app

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Round world-coordinate ticks, including one outside each edge for continuous clipped labels. */
internal data class RouteChartXAxis(val start:Double,val end:Double,val step:Double,val marks:List<Double>) {
    val decimals get()=ceil(-log10(step)).toInt().coerceIn(0,9)
    companion object {
        fun calculate(start:Double,end:Double):RouteChartXAxis {
            if(!start.isFinite() || !end.isFinite() || end<=start) return RouteChartXAxis(0.0,1.0,1.0,listOf(0.0,1.0))
            val desired=(end-start)/4
            val magnitude=10.0.pow(floor(log10(desired)))
            val fraction=desired/magnitude
            val step=magnitude*when { fraction<1.5->1;fraction<3->2;fraction<7->5;else->10 }
            val first=floor(start/step).toLong()
            val last=ceil(end/step).toLong()
            val marks=(first..last).map { it*step }
            return RouteChartXAxis(start,end,step,marks)
        }
    }
}

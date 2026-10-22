package de.bcycle.app

import java.time.LocalDate
import kotlin.math.sqrt

internal enum class HrvTrendMetric(val key:String,val label:Int,val unit:Int) {
    HR("restingHrBpm",R.string.hrvt_hr,R.string.hrvt_bpm),
    SDNN("sdnnMs",R.string.hrvt_sdnn,R.string.hrvt_ms),
    RMSSD("rmssdMs",R.string.hrvt_rmssd,R.string.hrvt_ms),
    PNN50("pnn50Percent",R.string.hrvt_pnn50,R.string.hrvt_percent),
    LF("lfMs2",R.string.hrvt_lf,R.string.hrvt_ms2),
    HF("hfMs2",R.string.hrvt_hf,R.string.hrvt_ms2)
}
internal data class HrvTrendPoint(val row:HrvHistorySummary,val day:LocalDate,val cohort:String,val value:Double?,
    val average:Double?,val lower:Double?,val upper:Double?) {
    val belowNormal get()=value!=null && lower!=null && value<lower
}

/** Fixed for the complete loaded series, including its mean and reference band. */
internal data class HrvTrendAxis(val minimum:Double,val maximum:Double) {
    companion object {
        fun from(points:List<HrvTrendPoint>,metric:HrvTrendMetric):HrvTrendAxis {
            val values=points.flatMap { listOfNotNull(it.value,it.average,it.lower,it.upper) }.filter { it.isFinite() && it>=0 }
            val low=values.minOrNull() ?: return HrvTrendAxis(0.0,1.0)
            val high=values.max()
            val lowerPadding=if(metric==HrvTrendMetric.HR) 5.0 else low*.05
            val upperPadding=if(metric==HrvTrendMetric.HR) 5.0 else (high*.05).coerceAtLeast(.1)
            return HrvTrendAxis((low-lowerPadding).coerceAtLeast(0.0),high+upperPadding)
        }
    }
}

/** Read-only descriptive statistics, never a readiness score or a re-evaluation of a capture. */
internal object HrvTrends {
    const val AVERAGE_DAYS=7L
    const val NORMAL_DAYS=60L
    const val MIN_AVERAGE=5
    const val MIN_NORMAL=14
    fun series(rows:List<HrvHistorySummary>,metric:HrvTrendMetric):List<HrvTrendPoint> {
        val selected=HrvDailySelection.latest(rows).sortedBy { HrvDailySelection.day(it) }
        val points=selected.map { row ->
            val usable=row.acceptable || row.importedDay!=null && row.hasMeasurement
            val value=if(usable) (row.metrics[metric.key] ?: row.rmssdMs.takeIf { metric==HrvTrendMetric.RMSSD })
                ?.takeIf { it.isFinite() && it>=0 } else null
            // One personal history per metric; source/protocol remain descriptive metadata.
            // CSV frequency values without a known unit never enter these metric fields.
            val cohort=metric.name
            HrvTrendPoint(row,HrvDailySelection.day(row),cohort,value,null,null,null)
        }
        return points.mapIndexed { index,point ->
            if(point.value==null) return@mapIndexed point
            val week=ArrayList<Double>();val normal=ArrayList<Double>()
            for(i in index downTo 0) {
                val previous=points[i];val age=point.day.toEpochDay()-previous.day.toEpochDay()
                if(age>NORMAL_DAYS) break
                if(previous.cohort!=point.cohort || previous.value==null) continue
                if(age<AVERAGE_DAYS) week+=previous.value
                if(age in 1..NORMAL_DAYS) normal+=previous.value
            }
            val mean=normal.takeIf { it.size>=MIN_NORMAL }?.average()
            val sd=mean?.let { m -> sqrt(normal.sumOf { (it-m)*(it-m) }/(normal.size-1)) }
            point.copy(average=week.takeIf { it.size>=MIN_AVERAGE }?.average(),
                lower=mean?.let { (it-checkNotNull(sd)).coerceAtLeast(0.0) },upper=mean?.let { it+checkNotNull(sd) })
        }
    }
}

/** Day-based viewport: zoom never changes either statistics window. */
internal class HrvTrendViewport {
    var span=14.0;private set
    var end=0.0;private set
    private var first=0.0;private var last=0.0
    fun reset(days:List<LocalDate>) {
        first=(days.minOrNull()?.toEpochDay() ?: LocalDate.now().toEpochDay()).toDouble()-.5
        last=(days.maxOrNull()?.toEpochDay() ?: LocalDate.now().toEpochDay()).toDouble()+.5
        span=14.0.coerceAtMost(maxSpan());end=last;clamp()
    }
    private fun maxSpan()=(last-first).coerceAtLeast(14.0)
    private fun clamp() { end=end.coerceIn(first.coerceAtMost(last-span)+span,last) }
    fun pan(days:Double) { end+=days;clamp() }
    fun zoom(factor:Double,anchor:Double) {
        if(!factor.isFinite() || factor<=0) return
        val pivot=end-span+span*anchor.coerceIn(0.0,1.0)
        span=(span/factor).coerceIn(7.0,maxSpan())
        end=pivot+span*(1-anchor.coerceIn(0.0,1.0));clamp()
    }
    fun contains(day:LocalDate)=day.toEpochDay().toDouble() in (end-span)..end
}

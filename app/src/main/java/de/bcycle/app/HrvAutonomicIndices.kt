package de.bcycle.app

import java.time.LocalDate
import kotlin.math.ln
import kotlin.math.sqrt

/** Descriptive personal deviations, not a diagnosis or an isolated measurement of SNS activity. */
internal data class HrvAutonomicIndices(val pns:Double?=null,val activation:Double?=null) {
    companion object {
        fun from(rows:List<HrvHistorySummary>,day:LocalDate):HrvAutonomicIndices {
            val daily=HrvDailySelection.latest(rows)
            val today=daily.firstOrNull { HrvDailySelection.day(it)==day } ?: return HrvAutonomicIndices()
            fun usable(row:HrvHistorySummary)=row.acceptable || row.importedDay!=null && row.hasMeasurement
            if(!usable(today)) return HrvAutonomicIndices()
            // Unknown import protocols form their own cohort; do not mix them with timed native captures.
            val reference=daily.filter {
                val age=day.toEpochDay()-HrvDailySelection.day(it).toEpochDay()
                age in 1..HrvTrends.NORMAL_DAYS && usable(it) &&
                    (it.importedDay!=null)==(today.importedDay!=null) && it.restMs==today.restMs && it.measureMs==today.measureMs
            }
            fun hrv(row:HrvHistorySummary)=(row.rmssdMs ?: row.metrics["rmssdMs"])
                ?.takeIf { it.isFinite() && it>0 }?.let(::ln)
            fun pulse(row:HrvHistorySummary)=row.metrics["restingHrBpm"]?.takeIf { it.isFinite() && it>0 }
            fun standardized(value:Double?,values:List<Double>):Double? {
                if(value==null || values.size<HrvTrends.MIN_NORMAL) return null
                val mean=values.average()
                val sd=sqrt(values.sumOf { (it-mean)*(it-mean) }/(values.size-1))
                if(!sd.isFinite() || sd<=1e-9) return null
                return ((value-mean)/sd).takeIf(Double::isFinite)
            }
            return HrvAutonomicIndices(standardized(hrv(today),reference.mapNotNull(::hrv)),
                standardized(pulse(today),reference.mapNotNull(::pulse)))
        }
    }
}

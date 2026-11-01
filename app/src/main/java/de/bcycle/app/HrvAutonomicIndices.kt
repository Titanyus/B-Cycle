package de.bcycle.app

import java.time.LocalDate
import kotlin.math.ln
import kotlin.math.sqrt

/** Descriptive personal deviations, not a diagnosis or an isolated measurement of SNS activity. */
internal data class HrvAutonomicIndices(val pns:Double?=null,val activation:Double?=null,
    val pnsReferenceDays:Int?=null,val activationReferenceDays:Int?=null) {
    companion object {
        fun from(rows:List<HrvHistorySummary>,day:LocalDate):HrvAutonomicIndices {
            val daily=HrvDailySelection.latest(rows)
            val today=daily.firstOrNull { HrvDailySelection.day(it)==day } ?: return HrvAutonomicIndices()
            fun usable(row:HrvHistorySummary)=row.acceptable || row.importedDay!=null && row.hasMeasurement
            if(!usable(today)) return HrvAutonomicIndices()
            // One personal history, as in the existing corridor; importing is an input adapter only.
            val reference=daily.filter {
                val age=day.toEpochDay()-HrvDailySelection.day(it).toEpochDay()
                age in 1..HrvTrends.NORMAL_DAYS && usable(it)
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
            val currentHrv=hrv(today);val currentPulse=pulse(today)
            val hrvReference=reference.mapNotNull(::hrv);val pulseReference=reference.mapNotNull(::pulse)
            return HrvAutonomicIndices(standardized(currentHrv,hrvReference),standardized(currentPulse,pulseReference),
                hrvReference.size.takeIf { currentHrv!=null },pulseReference.size.takeIf { currentPulse!=null })
        }
    }
}

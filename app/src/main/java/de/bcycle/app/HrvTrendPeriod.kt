package de.bcycle.app

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Equal calendar buckets on the X axis. No change to the daily statistical windows. */
internal enum class HrvTrendPeriod(val label:Int) {
    DAY(R.string.hrvt_day),WEEK(R.string.hrvt_week),MONTH(R.string.hrvt_month);
    fun start(day:LocalDate):LocalDate=when(this) {
        DAY->day
        WEEK->day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        MONTH->day.withDayOfMonth(1)
    }
    fun next(day:LocalDate):LocalDate=when(this) { DAY->day.plusDays(1);WEEK->day.plusWeeks(1);MONTH->day.plusMonths(1) }
    fun end(day:LocalDate)=next(start(day)).minusDays(1)
    fun index(day:LocalDate):Long=when(this) {
        DAY->day.toEpochDay()
        WEEK->Math.floorDiv(day.toEpochDay()-4,7)
        MONTH->day.year.toLong()*12+day.monthValue-1
    }
    fun date(index:Long):LocalDate=when(this) {
        DAY->LocalDate.ofEpochDay(index)
        WEEK->LocalDate.ofEpochDay(index*7+4)
        MONTH->LocalDate.of(Math.floorDiv(index,12).toInt(),Math.floorMod(index,12).toInt()+1,1)
    }
}

/** Aggregate valid daily values, not captures. Missing values never become zeros.
 * Trend/band are means of the already computed daily statistics on those same valid days;
 * their 7/60-day windows and minimum sample counts remain unchanged.
 */
internal object HrvTrendAggregation {
    fun series(daily:List<HrvTrendPoint>,period:HrvTrendPeriod):List<HrvTrendPoint> {
        if(period==HrvTrendPeriod.DAY) return daily
        return daily.groupBy { period.start(it.day) }.toSortedMap().map { (start,days) ->
            val valid=days.filter { it.value!=null }
            fun mean(values:List<Double>)=values.takeIf { it.isNotEmpty() }?.average()
            val bounds=valid.filter { it.lower!=null && it.upper!=null }
            val representative=(valid.ifEmpty { days }).maxBy { it.day }
            representative.copy(day=start,value=mean(valid.map { it.value!! }),
                average=mean(valid.mapNotNull { it.average }),lower=mean(bounds.map { it.lower!! }),upper=mean(bounds.map { it.upper!! }),
                periodEnd=period.end(start),sampleDays=valid.size,trendDays=valid.count { it.average!=null },bandDays=bounds.size)
        }
    }
}

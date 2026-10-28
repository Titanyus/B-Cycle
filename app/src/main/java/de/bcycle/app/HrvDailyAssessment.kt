package de.bcycle.app

import java.time.LocalDate
import kotlin.math.ln

internal enum class HrvDayPosition { UNAVAILABLE,COLLECTING,LOW,NORMAL,HIGH }
internal enum class HrvSubjectiveState { UNKNOWN,GOOD,MIXED,POOR }
internal enum class HrvDayAdvice(val label:Int) {
    UNAVAILABLE(R.string.hrva_unavailable),COLLECTING(R.string.hrva_collecting_title),
    PLAN(R.string.hrva_plan),LIMIT(R.string.hrva_limit),EASY(R.string.hrva_easy);
    val color get()=when(this) { PLAN->TrainingZoneColors.green;LIMIT->TrainingZoneColors.yellow;EASY->TrainingZoneColors.red;else->TrainingZoneColors.grey }
}

/** Prototype display and daily guidance; no age correction, persisted score or change to capture acceptance. */
internal data class HrvDailyAssessment(val day:LocalDate,val hrv:HrvTrendPoint,val pulse:HrvTrendPoint?,
    val subjectiveBurden:Double?,val subjectiveDays:Int) {
    val score get()=score(hrv.value)
    val lower get()=score(hrv.lower)
    val upper get()=score(hrv.upper)
    val position get()=when {
        score==null->HrvDayPosition.UNAVAILABLE
        hrv.lower==null || hrv.upper==null || hrv.upper<=0->HrvDayPosition.COLLECTING
        hrv.value!!<hrv.lower->HrvDayPosition.LOW
        hrv.value!!>hrv.upper->HrvDayPosition.HIGH
        else->HrvDayPosition.NORMAL
    }
    val subjectiveState get()=when {
        subjectiveBurden==null->HrvSubjectiveState.UNKNOWN
        subjectiveBurden<=4.0->HrvSubjectiveState.GOOD
        subjectiveBurden>=6.0->HrvSubjectiveState.POOR
        else->HrvSubjectiveState.MIXED
    }
    val advice get()=when(position) {
        HrvDayPosition.UNAVAILABLE->HrvDayAdvice.UNAVAILABLE
        HrvDayPosition.COLLECTING->HrvDayAdvice.COLLECTING
        HrvDayPosition.LOW->if(subjectiveState==HrvSubjectiveState.POOR) HrvDayAdvice.EASY else HrvDayAdvice.LIMIT
        HrvDayPosition.NORMAL,HrvDayPosition.HIGH->if(subjectiveState==HrvSubjectiveState.POOR) HrvDayAdvice.LIMIT else HrvDayAdvice.PLAN
    }
    companion object {
        fun score(rmssd:Double?)=rmssd?.takeIf { it.isFinite() && it>0 }?.let { 2*ln(it) }
        fun from(rows:List<HrvHistorySummary>,answers:Map<LocalDate,HrvQuestionnaire>,id:String):HrvDailyAssessment {
            val day=HrvDailySelection.day(rows.first { it.id==id })
            val hrv=HrvTrends.series(rows,HrvTrendMetric.RMSSD).first { it.day==day }
            val pulse=HrvTrends.series(rows,HrvTrendMetric.HR).firstOrNull { it.day==day }
            val context=HrvContextColors.daily(answers)
            val recent=(0L..3L).mapNotNull { context[day.minusDays(it)]?.get(HrvContextMode.WELLBEING) }
            return HrvDailyAssessment(day,hrv,pulse,recent.takeIf { it.isNotEmpty() }?.average(),recent.size)
        }
    }
}

package de.bcycle.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** CTL/ATL are provider-calculated training load, not subjective wellness fatigue. */
internal data class IntervalsFitnessDay(val day:LocalDate,val fitness:Double?,val fatigue:Double?) {
    init { require(listOfNotNull(fitness,fatigue).all { it.isFinite() && it>=0 }) }
    val form:Double? get()=fitness?.let { ctl -> fatigue?.let { ctl-it } }
    /** Available for a later explicit percentage display; zero CTL has no defined percentage. */
    val formPercent:Double? get()=fitness?.takeIf { it>0 }?.let { ctl -> form?.let { it/ctl*100 } }
    fun json()=JSONObject().put("id",day.toString()).put("ctl",fitness ?: JSONObject.NULL).put("atl",fatigue ?: JSONObject.NULL)
    companion object {
        fun decode(row:JSONObject):IntervalsFitnessDay {
            fun number(key:String):Double? {
                if(!row.has(key) || row.isNull(key)) return null
                require(row.get(key) is Number)
                return row.getDouble(key).also { require(it.isFinite() && it>=0) }
            }
            return IntervalsFitnessDay(LocalDate.parse(row.getString("id")),number("ctl"),number("atl"))
        }
        fun decode(text:String,oldest:LocalDate,newest:LocalDate):List<IntervalsFitnessDay> {
            val rows=JSONArray(text);require(rows.length()<=ChronoUnit.DAYS.between(oldest,newest)+1)
            val result=ArrayList<IntervalsFitnessDay>();val seen=HashSet<LocalDate>()
            for(i in 0 until rows.length()) {
                val row=decode(rows.getJSONObject(i))
                require(row.day in oldest..newest && seen.add(row.day));result.add(row)
            }
            return result.sortedBy { it.day }
        }
    }
}

/** Rolling daily windows, inclusive of the selected end day. */
internal enum class IntervalsFitnessPeriod(val days:Int,val label:Int) {
    WEEK(7,R.string.hrvt_week),MONTH(30,R.string.hrvt_month),THREE_MONTHS(90,R.string.intervals_fitness_three_months);
    fun oldest(newest:LocalDate)=newest.minusDays(days-1L)
}
internal data class IntervalsFitnessHistory(val oldest:LocalDate,val newest:LocalDate,
    val days:List<IntervalsFitnessDay>,val fetchedAtMillis:Long) {
    init {
        require(newest>=oldest && ChronoUnit.DAYS.between(oldest,newest)<366 && fetchedAtMillis>=0)
        require(days.size==ChronoUnit.DAYS.between(oldest,newest).toInt()+1)
        require(days.withIndex().all { (index,row) -> row.day==oldest.plusDays(index.toLong()) })
    }
    val hasValues get()=days.any { it.fitness!=null || it.fatigue!=null }
}

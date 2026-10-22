package de.bcycle.app

import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.UUID
import kotlin.math.roundToInt

internal enum class HrvCsvDateOrder(val pattern:String,val title:Int) {
    YEAR_DAY_MONTH("uuuu-dd-MM",R.string.hrv4_date_ydm),
    YEAR_MONTH_DAY("uuuu-MM-dd",R.string.hrv4_date_ymd)
}
internal class HrvCsvException(val messageId:Int):IllegalArgumentException()
internal data class HrvCsvEntry(val result:JSONObject,val questionnaire:HrvQuestionnaire,val hasMetrics:Boolean)
internal data class HrvCsvPreview(val entries:List<HrvCsvEntry>,val invalidRecords:List<Int>,val dateOrder:HrvCsvDateOrder,
    val dateOrderAssumed:Boolean,val zone:String) {
    val measurements=entries.count { it.hasMetrics }
    val contextDays=entries.size-measurements
    val days=entries.map { it.result.getJSONObject("external").getString("day") }.sorted()
}
internal data class HrvCsvImportReport(val imported:Int,val duplicates:Int,val invalid:Int,val failed:Int)

/** HRV4Training summaries only. Never manufacture RR data, BICYCLE quality or frequency units. */
internal object Hrv4TrainingCsv {
    const val PROVIDER="hrv4training"
    const val MAX_BYTES=4*1024*1024
    private const val MAX_ROWS=10000
    /** These fields have matching question direction; original precision remains in originalColumns. */
    fun ratings(source:Map<String,String>):Map<String,Int> = linkedMapOf(
        "sleep_quality" to "sleep","trainingMotivation" to "motivation","physical_condition" to "condition",
        "mental_energy" to "energy","muscle_soreness" to "soreness","fatigue" to "fatigue",
        "lifestyle" to "lifestyle","trainingRPE" to "rpe"
    ).mapNotNull { (column,key) ->
        source[column]?.trim()?.replace(',','.')?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..10.0 }
            ?.let { key to it.roundToInt() }
    }.toMap()
    fun preview(input:InputStream,zone:ZoneId,order:HrvCsvDateOrder?=null):HrvCsvPreview {
        val bytes=input.readNBytes(MAX_BYTES+1)
        if(bytes.size>MAX_BYTES) throw HrvCsvException(R.string.hrv4_too_large)
        val text=try { Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        } catch(_:Exception) { throw HrvCsvException(R.string.hrv4_bad_csv) }
        val table=csv(text)
        if(table.size<2) throw HrvCsvException(R.string.hrv4_bad_csv)
        val headers=table.first().map(String::trim).dropLastWhile(String::isEmpty)
        if(headers.size !in 2..256 || headers.any(String::isEmpty) || headers.distinct().size!=headers.size ||
            "rMSSD" !in headers || ("date" !in headers && "local" !in headers)) throw HrvCsvException(R.string.hrv4_bad_csv)
        val rows=table.drop(1).map { values ->
            if(values.drop(headers.size).any(String::isNotBlank)) null
            else headers.mapIndexed { index,key->key to values.getOrElse(index) { "" } }.toMap()
        }
        fun day(row:Map<String,String>)=row["local"].orEmpty().trim().ifEmpty { row["date"].orEmpty().trim() }
        val parts=rows.filterNotNull().map { day(it).split('-') }.filter { it.size==3 && it[0].length==4 }
        val ydm=parts.any { (it[1].toIntOrNull() ?: 0) in 13..31 && (it[2].toIntOrNull() ?: 0) in 1..12 }
        val ymd=parts.any { (it[2].toIntOrNull() ?: 0) in 13..31 && (it[1].toIntOrNull() ?: 0) in 1..12 }
        if(order==null && ydm && ymd) throw HrvCsvException(R.string.hrv4_mixed_dates)
        val actual=order ?: if(ymd) HrvCsvDateOrder.YEAR_MONTH_DAY else HrvCsvDateOrder.YEAR_DAY_MONTH
        val formatter=DateTimeFormatter.ofPattern(actual.pattern).withResolverStyle(ResolverStyle.STRICT)
        val entries=mutableListOf<HrvCsvEntry>();val invalid=mutableListOf<Int>()
        rows.forEachIndexed { index,row ->
            val parsed=runCatching {
                require(row!=null)
                val localDay=LocalDate.parse(day(row),formatter)
                require(localDay.year in 1970..2200)
                val time=row["time"].orEmpty().trim().takeIf(String::isNotEmpty)?.let(LocalTime::parse)
                entry(row,localDay,time,zone)
            }
            parsed.onSuccess(entries::add).onFailure { invalid+=index+2 }
        }
        return HrvCsvPreview(entries,invalid,actual,order==null && !ydm && !ymd,zone.id)
    }
    private fun entry(source:Map<String,String>,day:LocalDate,time:LocalTime?,zone:ZoneId):HrvCsvEntry {
        fun number(key:String):Double? {
            val value=source[key].orEmpty().trim()
            if(value.isEmpty() || value.lowercase() in setOf("na","nan","null")) return null
            return value.replace(',','.').toDoubleOrNull()?.also { require(it.isFinite()) } ?: error("number")
        }
        val metrics=JSONObject()
        for((column,key) in listOf("HR" to "restingHrBpm","AVNN" to "meanNnMs","SDNN" to "sdnnMs","rMSSD" to "rmssdMs","pNN50" to "pnn50Percent")) {
            number(column)?.let { value ->
                require(value>=0 && value<=if(column=="HR") 300 else if(column=="pNN50") 100 else 10000)
                if(column=="HR" || column=="AVNN") require(value>0)
                metrics.put(key,value)
            }
        }
        // LF/HF have no declared unit in this export; retain them exclusively as original fields.
        val id=UUID.nameUUIDFromBytes("bicycle:hrv4training:$day:${time ?: "date-only"}".toByteArray(Charsets.UTF_8)).toString()
        val external=JSONObject().put("provider",PROVIDER).put("day",day.toString()).put("localTime",time?.toString() ?: JSONObject.NULL)
            .put("timezoneAssumed",true).put("metrics",metrics).put("originalColumns",JSONObject(source))
        val result=JSONObject().put("format","bicycle-hrv").put("schema",1).put("id",id)
            .put("utcMs",day.atTime(time ?: LocalTime.MIDNIGHT).atZone(zone).toInstant().toEpochMilli()).put("zone",zone.id)
            .put("phase","COMPLETE").put("acceptable",false).put("external",external)
        fun text(key:String)=source[key].orEmpty().trim()
        fun choice(key:String,values:List<String>)=values.indexOf(text(key).lowercase()).takeIf { it>=0 }
        val extras=linkedMapOf<String,String>()
        for((column,key) in listOf("sleep_awake" to "wake","sleep_tobed" to "bed")) {
            runCatching { LocalTime.parse(text(column)) }.getOrNull()?.takeIf { it.second==0 && it.nano==0 }?.let { extras[key]=it.toString() }
        }
        if(text("training_phase") in HrvQuestionnaire.phases) extras["phase"]=text("training_phase")
        if(text("menstrual_cycle") in HrvQuestionnaire.cycles) extras["cycle"]=text("menstrual_cycle")
        for((column,key) in listOf("diet" to "diet","location" to "location","supplements" to "supplements"))
            text(column).takeIf { it.isNotEmpty() && it.length<=500 }?.let { extras[key]=it }
        val names=linkedMapOf<String,String>()
        for(i in 1..3) {
            val name=text("custom_tag_${i}_name");val value=text("custom_tag_${i}_value").toDoubleOrNull()
            if(name.length in 1..40 && name.none(Char::isISOControl) && value!=null && value.isFinite() && value in -1_000_000.0..1_000_000.0) {
                names["custom$i"]=name;extras["custom$i"]=value.toString()
            }
        }
        val km=number("trainingDistance")?.div(1000)?.also { require(it in 0.0..1_000_000.0) }
        val minutes=number("trainingTime")?.div(60)?.also { require(it in 0.0..1_000_000.0) }
        val questionnaire=HrvQuestionnaire(day.toString(),ratings=ratings(source),
            training=choice("training",listOf("rest","easy","average","intense")),trainingKm=km,trainingMinutes=minutes,
            trainingSource=if(km!=null || minutes!=null) "manual" else null,
            traveled=when(text("traveling").lowercase()) { "traveling"->true;"not traveling"->false;else->null },
            sick=when(text("sickness").lowercase()) { "sick"->true;"not sick"->false;else->null },
            alcohol=choice("alcohol",listOf("nothing","a little","too much")),
            notes=source["note"].orEmpty().takeIf { it.length<=1000 }.orEmpty(),extras=extras,customNames=names)
        questionnaire.validate()
        return HrvCsvEntry(result,questionnaire,metrics.length()>0)
    }
    fun import(preview:HrvCsvPreview,store:HrvStore):HrvCsvImportReport {
        var imported=0;var duplicates=0
        preview.entries.forEachIndexed { index,entry ->
            try { if(store.importHrv4Training(entry)) imported++ else duplicates++ }
            catch(_:Exception) { return HrvCsvImportReport(imported,duplicates,preview.invalidRecords.size,preview.entries.size-index) }
        }
        return HrvCsvImportReport(imported,duplicates,preview.invalidRecords.size,0)
    }
    private fun csv(text:String):List<List<String>> {
        val rows=mutableListOf<List<String>>();var row=mutableListOf<String>();val field=StringBuilder()
        var quoted=false;var closed=false;var index=0;var cells=0
        fun invalid():Nothing=throw HrvCsvException(R.string.hrv4_bad_csv)
        fun cell() { if(++cells>250000) invalid();row+=field.toString();field.setLength(0);closed=false;if(row.size>257) invalid() }
        fun record() { cell();if(row.any(String::isNotBlank)) rows+=row;row=mutableListOf();if(rows.size>MAX_ROWS+1) invalid() }
        while(index<text.length) {
            val c=text[index++]
            if(quoted) {
                if(c=='"') { if(index<text.length && text[index]=='"') { field.append('"');index++ } else { quoted=false;closed=true } }
                else field.append(c)
            } else when {
                c==','->cell()
                c=='\n' || c=='\r'->{ if(c=='\r' && index<text.length && text[index]=='\n') index++;record() }
                c=='"' && field.isEmpty() && !closed->quoted=true
                closed && (c==' ' || c=='\t')->Unit
                closed || c=='"'->invalid()
                else->field.append(c)
            }
            if(field.length>16384) invalid()
        }
        if(quoted) invalid()
        if(field.isNotEmpty() || row.isNotEmpty() || closed) record()
        return rows
    }
}

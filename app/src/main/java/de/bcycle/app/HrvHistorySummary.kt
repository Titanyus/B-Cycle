package de.bcycle.app

import android.util.JsonReader
import android.util.JsonToken

/** Small history row; RR/NN arrays and correction logs never reach the list UI. */
internal data class HrvHistorySummary(val id:String,val utcMs:Long,val acceptable:Boolean,val rmssdMs:Double?,
    val importedDay:String?=null,val importedTime:String?=null,val zoneId:String?=null,val hasMeasurement:Boolean=true,
    val metrics:Map<String,Double> = emptyMap(),val restMs:Long?=null,val measureMs:Long?=null) {
    companion object {
        fun read(reader:JsonReader,expectedId:String):HrvHistorySummary {
            var id="";var format="";var schema=0;var utc=0L;var acceptable=false;var rmssd:Double?=null
            var provider="";var day:String?=null;var time:String?=null;var externalRmssd:Double?=null
            var zone:String?=null;var externalHasMeasurement=false
            var rest:Long?=null;var measure:Long?=null
            val values=linkedMapOf<String,Double>();val externalValues=linkedMapOf<String,Double>()
            fun numbers(target:MutableMap<String,Double>,keys:Set<String>) {
                if(reader.peek()==JsonToken.NULL) { reader.nextNull();return }
                reader.beginObject()
                while(reader.hasNext()) {
                    val key=reader.nextName()
                    if(key in keys && reader.peek()==JsonToken.NUMBER)
                        reader.nextDouble().takeIf { it.isFinite() && it>=0 }?.let { target[key]=it }
                    else reader.skipValue()
                }
                reader.endObject()
            }
            val timeKeys=setOf("rmssdMs","sdnnMs","meanNnMs","restingHrBpm","pnn50Percent")
            reader.beginObject()
            while(reader.hasNext()) when(reader.nextName()) {
                "format"->format=reader.nextString()
                "schema"->schema=reader.nextInt()
                "id"->id=reader.nextString()
                "utcMs"->utc=reader.nextLong()
                "zone"->zone=reader.nextString()
                "acceptable"->acceptable=reader.nextBoolean()
                "restMs"->if(reader.peek()==JsonToken.NUMBER) rest=reader.nextLong() else reader.skipValue()
                "measureMs"->if(reader.peek()==JsonToken.NUMBER) measure=reader.nextLong() else reader.skipValue()
                "corrected"->{ numbers(values,timeKeys);rmssd=values["rmssdMs"] }
                "frequency"->numbers(values,setOf("lfMs2","hfMs2"))
                "external"->if(reader.peek()==JsonToken.NULL) reader.nextNull() else {
                    reader.beginObject()
                    while(reader.hasNext()) when(reader.nextName()) {
                        "provider"->provider=reader.nextString()
                        "day"->day=runCatching { java.time.LocalDate.parse(reader.nextString()).toString() }.getOrNull()
                        "localTime"->if(reader.peek()==JsonToken.NULL) reader.nextNull() else time=reader.nextString()
                        "metrics"->{
                            numbers(externalValues,timeKeys)
                            externalHasMeasurement=externalValues.isNotEmpty();externalRmssd=externalValues["rmssdMs"]
                        }
                        else->reader.skipValue()
                    }
                    reader.endObject()
                }
                else->reader.skipValue()
            }
            reader.endObject()
            require(format=="bicycle-hrv" && schema==1 && id==expectedId && reader.peek()==JsonToken.END_DOCUMENT)
            return if(provider==Hrv4TrainingCsv.PROVIDER && day!=null) HrvHistorySummary(id,utc,false,externalRmssd,day,time,zone,externalHasMeasurement,externalValues.toMap())
                else HrvHistorySummary(id,utc,acceptable,rmssd,zoneId=zone,metrics=values.toMap(),restMs=rest,measureMs=measure)
        }
    }
}

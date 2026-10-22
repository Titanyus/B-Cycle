package de.bcycle.app

import android.util.JsonReader
import android.util.JsonToken

/** Small history row; RR/NN arrays and correction logs never reach the list UI. */
internal data class HrvHistorySummary(val id:String,val utcMs:Long,val acceptable:Boolean,val rmssdMs:Double?,
    val importedDay:String?=null,val importedTime:String?=null) {
    companion object {
        fun read(reader:JsonReader,expectedId:String):HrvHistorySummary {
            var id="";var format="";var schema=0;var utc=0L;var acceptable=false;var rmssd:Double?=null
            var provider="";var day:String?=null;var time:String?=null;var externalRmssd:Double?=null
            reader.beginObject()
            while(reader.hasNext()) when(reader.nextName()) {
                "format"->format=reader.nextString()
                "schema"->schema=reader.nextInt()
                "id"->id=reader.nextString()
                "utcMs"->utc=reader.nextLong()
                "acceptable"->acceptable=reader.nextBoolean()
                "corrected"->if(reader.peek()==JsonToken.NULL) reader.nextNull() else {
                    reader.beginObject()
                    while(reader.hasNext()) if(reader.nextName()=="rmssdMs" && reader.peek()!=JsonToken.NULL)
                        rmssd=reader.nextDouble().takeIf { it.isFinite() && it>=0 }
                    else reader.skipValue()
                    reader.endObject()
                }
                "external"->if(reader.peek()==JsonToken.NULL) reader.nextNull() else {
                    reader.beginObject()
                    while(reader.hasNext()) when(reader.nextName()) {
                        "provider"->provider=reader.nextString()
                        "day"->day=runCatching { java.time.LocalDate.parse(reader.nextString()).toString() }.getOrNull()
                        "localTime"->if(reader.peek()==JsonToken.NULL) reader.nextNull() else time=reader.nextString()
                        "metrics"->{
                            reader.beginObject()
                            while(reader.hasNext()) if(reader.nextName()=="rmssdMs" && reader.peek()!=JsonToken.NULL)
                                externalRmssd=reader.nextDouble().takeIf { it.isFinite() && it>=0 }
                            else reader.skipValue()
                            reader.endObject()
                        }
                        else->reader.skipValue()
                    }
                    reader.endObject()
                }
                else->reader.skipValue()
            }
            reader.endObject()
            require(format=="bicycle-hrv" && schema==1 && id==expectedId && reader.peek()==JsonToken.END_DOCUMENT)
            return if(provider==Hrv4TrainingCsv.PROVIDER && day!=null) HrvHistorySummary(id,utc,false,externalRmssd,day,time)
                else HrvHistorySummary(id,utc,acceptable,rmssd)
        }
    }
}

package de.bcycle.app

import android.util.JsonReader
import android.util.JsonToken

/** Small history row; RR/NN arrays and correction logs never reach the list UI. */
internal data class HrvHistorySummary(val id:String,val utcMs:Long,val acceptable:Boolean,val rmssdMs:Double?) {
    companion object {
        fun read(reader:JsonReader,expectedId:String):HrvHistorySummary {
            var id="";var format="";var schema=0;var utc=0L;var acceptable=false;var rmssd:Double?=null
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
                else->reader.skipValue()
            }
            reader.endObject()
            require(format=="bicycle-hrv" && schema==1 && id==expectedId && reader.peek()==JsonToken.END_DOCUMENT)
            return HrvHistorySummary(id,utc,acceptable,rmssd)
        }
    }
}

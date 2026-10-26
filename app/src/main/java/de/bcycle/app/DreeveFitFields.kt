package de.bcycle.app

import java.io.File

/** Inspect on the library worker while the normal FIT reader checks CRC and structural limits. */
internal object DreeveFitFields {
    fun read(file:File,cancelled:()->Boolean={false}):Set<DreeveDataField> {
        val fields=mutableSetOf<DreeveDataField>()
        FitRead.read(file) { record ->
            if(cancelled()) throw java.io.IOException()
            fun native(name:String)=record.getFieldDoubleValue(name)?.takeIf { it.isFinite() }
            fun own(name:String,unit:String)=FitDeveloperValues.number(record,name,unit)
            val latitude=native("position_lat")?.times(180.0/2147483648.0)
            val longitude=native("position_long")?.times(180.0/2147483648.0)
            if(latitude!=null && longitude!=null && latitude in -85.05112878..85.05112878 && longitude in -180.0..180.0) fields+=DreeveDataField.POSITION
            if(native("timestamp")!=null) fields+=DreeveDataField.TIME
            if(native("enhanced_altitude")!=null || native("altitude")!=null ||
                FitDeveloperValues.number(record,"PRIMARY_HEIGHT","m",signed=true)!=null || FitDeveloperValues.number(record,"GPS_ALTITUDE","m",signed=true)!=null) fields+=DreeveDataField.HEIGHT
            if((native("power") ?: own("PRIMARY_POWER","W") ?: own("RIDER_POWER","W"))?.let { it in 0.0..65535.0 }==true) fields+=DreeveDataField.POWER
            if((native("heart_rate") ?: own("PRIMARY_HEART","bpm") ?: own("HEART_RATE","bpm"))?.let { it in 1.0..254.0 }==true) fields+=DreeveDataField.HEART
            if((native("cadence") ?: own("PRIMARY_CADENCE","rpm") ?: own("CADENCE","rpm") ?: own("CADENCE","U/min"))?.let { it in 0.0..254.0 }==true) fields+=DreeveDataField.CADENCE
            if(native("temperature")!=null) fields+=DreeveDataField.TEMPERATURE
        }
        return fields
    }
}

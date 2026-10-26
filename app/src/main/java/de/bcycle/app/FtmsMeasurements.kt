package de.bcycle.app

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal enum class TrainerField { SPEED, CADENCE, POWER, DISTANCE, HEART, ELAPSED }
internal data class TrainerMeasurement(val values:Map<TrainerField,Double?>) {
    companion object {
        /** FTMS 2AD2. Absent and explicitly invalid are intentionally different. */
        fun decode(bytes:ByteArray):TrainerMeasurement? = try {
            val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            fun u8()=b.get().toInt() and 255
            fun u16()=b.short.toInt() and 65535
            val flags=u16()
            if(flags and 0xe000!=0) null else {
                val v=linkedMapOf<TrainerField,Double?>()
                if(flags and 1==0) v[TrainerField.SPEED]=u16().takeIf { it!=65535 }?.div(100.0)
                if(flags and 2!=0) u16()
                if(flags and 4!=0) v[TrainerField.CADENCE]=u16().takeIf { it!=65535 }?.div(2.0)
                if(flags and 8!=0) u16()
                if(flags and 16!=0) v[TrainerField.DISTANCE]=(u8() or (u8() shl 8) or (u8() shl 16)).takeIf { it!=0xffffff }?.toDouble()
                if(flags and 32!=0) b.short // Read-only; never write resistance or a control point.
                if(flags and 64!=0) v[TrainerField.POWER]=b.short.toInt().takeIf { it in 0..32766 }?.toDouble()
                if(flags and 128!=0) b.short
                if(flags and 256!=0) { u16();u16();u8() }
                if(flags and 512!=0) v[TrainerField.HEART]=u8().takeIf { it in 1..254 }?.toDouble()
                if(flags and 1024!=0) u8()
                if(flags and 2048!=0) v[TrainerField.ELAPSED]=u16().takeIf { it!=65535 }?.toDouble()
                if(flags and 4096!=0) u16()
                TrainerMeasurement(v)
            }
        } catch(_:BufferUnderflowException) { null }
    }
}
/** Per-field freshness: alternating notifications retain data but do not revive old fields. */
internal class TrainerReadings {
    private val values=mutableMapOf<TrainerField,Pair<Long,Double>>()
    private val received=mutableSetOf<TrainerField>()
    val count get()=received.size
    internal val receivedTypes get()=received.toSet()
    var lastAt:Long?=null;private set
    fun apply(m:TrainerMeasurement,now:Long) {
        m.values.forEach { (field,value)->
            if(value==null) values.remove(field)
            else { values[field]=now to value;received+=field;lastAt=now }
        }
    }
    fun value(field:TrainerField,now:Long)=values[field]?.takeIf { now-it.first in 0..5000 }?.second
    fun at(field:TrainerField)=values[field]?.first
    fun clear() { values.clear();received.clear();lastAt=null }
}

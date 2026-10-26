package de.bcycle.app

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.BufferUnderflowException

/** Bluetooth Cycling Power Measurement 2A63. Crank ticks are 1/1024 s, both counters wrap. */
internal class CyclingPowerMeasurements {
    private data class Crank(val revolutions:Int,val ticks:Int,val at:Long)
    private var crank:Crank?=null
    private var lastTurnAt:Long?=null
    fun reset() { crank=null;lastTurnAt=null }
    fun decode(bytes:ByteArray,now:Long):TrainerMeasurement? {
        try {
            val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            fun u16()=b.short.toInt() and 65535
            fun skip(n:Int) { repeat(n) { b.get() } }
            val flags=u16()
            if(flags and 0xe000!=0) return null
            val watts=b.short.toInt().takeIf { it in 0..32766 }?.toDouble()
            if(flags and 1!=0) skip(1)
            if(flags and 4!=0) skip(2)
            if(flags and 16!=0) skip(6) // Wheel revolutions are not cadence.
            val next=if(flags and 32!=0) Crank(u16(),u16(),now) else null
            for((flag,length) in listOf(64 to 4,128 to 4,256 to 3,512 to 2,1024 to 2,2048 to 2)) if(flags and flag!=0) skip(length)
            // Only commit cadence state after validating the complete packet.
            val values=linkedMapOf<TrainerField,Double?>(TrainerField.POWER to watts)
            if(next!=null) {
                val previous=crank
                if(previous==null || now-previous.at !in 0..5000) {
                    values[TrainerField.CADENCE]=null;lastTurnAt=now
                } else {
                    val turns=(next.revolutions-previous.revolutions) and 65535
                    val ticks=(next.ticks-previous.ticks) and 65535
                    if(turns>0) {
                        values[TrainerField.CADENCE]=if(ticks>0 && turns<=32) (turns*60.0*1024/ticks).takeIf { it in 0.0..254.0 } else null
                        lastTurnAt=now
                    } else if(ticks!=0) values[TrainerField.CADENCE]=null
                    else if(lastTurnAt?.let { now-it>=3000 }==true) values[TrainerField.CADENCE]=0.0
                }
                crank=next
            }
            return TrainerMeasurement(values)
        } catch(_:BufferUnderflowException) { return null }
    }
}

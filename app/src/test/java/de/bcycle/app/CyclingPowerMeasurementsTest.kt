package de.bcycle.app

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class CyclingPowerMeasurementsTest {
    private fun frame(watts:Int,cranks:Int?=null,ticks:Int=0)=ByteBuffer.allocate(if(cranks==null) 4 else 8).order(ByteOrder.LITTLE_ENDIAN).apply {
        putShort(if(cranks==null) 0 else 32);putShort(watts.toShort())
        if(cranks!=null) { putShort(cranks.toShort());putShort(ticks.toShort()) }
    }.array()
    @Test fun wattsZeroInvalidAndOptionalCadenceStayDistinct() {
        val d=CyclingPowerMeasurements()
        assertEquals(250.0,d.decode(frame(250),0)!!.values[TrainerField.POWER]!!,0.0)
        assertEquals(0.0,d.decode(frame(0),1)!!.values[TrainerField.POWER]!!,0.0)
        assertNull(d.decode(frame(-32768),2)!!.values[TrainerField.POWER])
        assertFalse(TrainerField.CADENCE in d.decode(frame(100),3)!!.values)
        assertNull(d.decode(byteArrayOf(0,0,1),4))
        assertNull(d.decode(byteArrayOf(0,0x20,1,0),5))
    }
    @Test fun crankAndEventTimeWrapAndStoppedCadenceIsObserved() {
        val d=CyclingPowerMeasurements()
        assertNull(d.decode(frame(200,65535,65000),0)!!.values[TrainerField.CADENCE])
        assertEquals(60.0,d.decode(frame(210,0,488),1000)!!.values[TrainerField.CADENCE]!!,.001)
        assertFalse(TrainerField.CADENCE in d.decode(frame(210,0,488),2000)!!.values)
        assertEquals(0.0,d.decode(frame(0,0,488),4000)!!.values[TrainerField.CADENCE]!!,0.0)
        assertNull(d.decode(frame(200,10,10000),11000)!!.values[TrainerField.CADENCE])
        d.reset();assertNull(d.decode(frame(200,11,11024),12000)!!.values[TrainerField.CADENCE])
    }
    @Test fun everyOptionalPayloadIsSkippedAndTruncationDoesNotAdvanceCranks() {
        val d=CyclingPowerMeasurements()
        d.decode(frame(100,5,1000),0)
        val packet=ByteBuffer.allocate(34).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(0x0fff);putShort(220);put(50);putShort(10);putInt(123);putShort(777)
            putShort(6);putShort(2024);putInt(0);putInt(0);put(byteArrayOf(0,0,0));putShort(0);putShort(0);putShort(12)
        }.array()
        assertNull(d.decode(packet.copyOf(packet.size-1),1000))
        assertEquals(60.0,d.decode(packet,1000)!!.values[TrainerField.CADENCE]!!,.001)
    }
}

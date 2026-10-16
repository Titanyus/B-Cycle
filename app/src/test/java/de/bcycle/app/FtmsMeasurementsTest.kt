package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun ftmsFrame(flags:Int,vararg values:Int)=byteArrayOf(flags.toByte(),(flags ushr 8).toByte())+values.map { it.toByte() }.toByteArray()
class FtmsMeasurementsTest {
    @Test fun confirmedMatrixFieldsDecodeWithCorrectScalesAndOptionalFieldsDoNotShiftOffsets() {
        val b=ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN)
        b.putShort(0x1ffe.toShort()) // every known field, including speed (bit 0 is clear)
        b.putShort(1090);b.putShort(1000);b.putShort(80);b.putShort(76)
        b.put(byteArrayOf(120,0,0));b.putShort(17);b.putShort(5);b.putShort(6)
        b.putShort(3);b.putShort(4);b.put(5);b.put(145.toByte());b.put(6);b.putShort(37);b.putShort(42)
        val m=TrainerMeasurement.decode(b.array().copyOf(b.position()))!!
        assertEquals(10.9,m.values[TrainerField.SPEED]!!,0.0)
        assertEquals(40.0,m.values[TrainerField.CADENCE]!!,0.0)
        assertEquals(5.0,m.values[TrainerField.POWER]!!,0.0)
        assertEquals(120.0,m.values[TrainerField.DISTANCE]!!,0.0)
        assertEquals(145.0,m.values[TrainerField.HEART]!!,0.0)
        assertEquals(37.0,m.values[TrainerField.ELAPSED]!!,0.0)
    }
    @Test fun absentInvalidAndRealZeroAreDistinctAndTruncatedFramesAreRejected() {
        val noSpeed=TrainerMeasurement.decode(ftmsFrame(1 or 64,0,0))!!
        assertFalse(TrainerField.SPEED in noSpeed.values);assertEquals(0.0,noSpeed.values[TrainerField.POWER]!!,0.0)
        assertNull(TrainerMeasurement.decode(ftmsFrame(64,1,0,7)))
        assertNull(TrainerMeasurement.decode(ftmsFrame(0x2000)))
        val invalid=TrainerMeasurement.decode(ftmsFrame(4 or 16 or 64 or 512 or 2048,
            255,255,255,255,255,255,255,255,127,255,255,255))!!
        assertEquals(6,invalid.values.size);assertTrue(invalid.values.values.all { it==null })
        assertNull(TrainerMeasurement.decode(ftmsFrame(1 or 64,255,255))!!.values[TrainerField.POWER])
    }
    @Test fun alternatingFramesRetainOnlyFreshFieldsAndExplicitInvalidValuesClearImmediately() {
        val state=TrainerReadings()
        state.apply(TrainerMeasurement.decode(ftmsFrame(4 or 64,232,3,180,0,200,0))!!,0)
        state.apply(TrainerMeasurement.decode(ftmsFrame(1 or 64,0,0))!!,4000)
        assertEquals(10.0,state.value(TrainerField.SPEED,4000)!!,0.0)
        assertEquals(90.0,state.value(TrainerField.CADENCE,4000)!!,0.0)
        assertEquals(0.0,state.value(TrainerField.POWER,6000)!!,0.0)
        assertNull(state.value(TrainerField.SPEED,6000));assertNull(state.value(TrainerField.CADENCE,6000))
        state.apply(TrainerMeasurement.decode(ftmsFrame(1 or 64,255,127))!!,6000)
        assertNull(state.value(TrainerField.POWER,6000));assertEquals(3,state.count)
        state.clear();assertEquals(0,state.count);assertNull(state.lastAt)
    }
    @Test fun indoorUsesExistingMotionAndMeansWithoutCountingMissingTimeAsStopped() {
        val ride=IndoorRide()
        ride.add(0,36.0,100.0,90.0);ride.add(1000,36.0,0.0,0.0)
        ride.add(2000,0.0,0.0,0.0);ride.add(3000,null,null,null)
        ride.add(20000,36.0,200.0,80.0)
        val v=ride.times(20000,20)
        assertEquals(1000L,v.movingMs);assertEquals(1000L,v.stoppedMs)
        assertEquals(20000L,v.elapsedMs);assertNull(v.latitude);assertNull(v.speedKmh)
        assertEquals(100.0/3,ride.averagePower!!,.001)
        ride.reset();assertNull(ride.times(0,null).movingMs);assertNull(ride.averagePower)
    }
    @Test fun ftmsPowerDrivesExistingEngineButMissingFtmsPowerDoesNotInheritBoschZeroContinuation() {
        val engine=PerformanceEngine(primaryPowerSource=PowerSource.FTMS)
        engine.accept(PerformanceEvent(PerformanceChannel.POWER,0,200.0,source=PowerSource.FTMS.name))
        engine.advance(1000);assertEquals(.2,engine.snapshot().knownMechanicalWorkKj,.000001)
        engine.accept(PerformanceEvent(PerformanceChannel.POWER,1000,0.0,source=PowerSource.FTMS.name))
        for(t in 2..20) { engine.accept(PerformanceEvent(PerformanceChannel.SPEED,t*1000L,5.0,source="SPEED")) }
        assertTrue(engine.snapshot().quality.powerCoverage<.5)
        assertEquals(0L,engine.snapshot().quality.continuedZeroMs)
    }
}

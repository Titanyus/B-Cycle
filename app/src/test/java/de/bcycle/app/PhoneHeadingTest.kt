package de.bcycle.app

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.SystemClock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import kotlin.math.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class PhoneHeadingTest {
    @Test fun circularFilterCrossesNorthWithoutSpinningBackAndRejectsStaleOrOutOfOrderValues() {
        val filter=PhoneHeadingFilter()
        assertNull(filter.current(0))
        assertEquals(359.0,filter.add(359.0,0)!!,.001)
        val next=filter.add(1.0,100)!!
        assertTrue(abs(PhoneHeadingFilter.delta(359.0,next))<2)
        assertNull(filter.add(180.0,50))
        assertEquals(next,filter.current(100)!!,.001)
        assertNull(filter.current(2101))
        assertEquals(90.0,filter.add(90.0,10000)!!,.001)
        filter.clear();assertNull(filter.current(10000))
    }
    @Test fun stationaryRotationSensorSuppliesDirectionAndStopsWhenHiddenOrUnreliable() {
        val context=RuntimeEnvironment.getApplication()
        val manager=context.getSystemService(SensorManager::class.java)
        val sensor=ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR)
        shadowOf(manager).addSensor(sensor)
        var changes=0
        val heading=PhoneHeading(context,{0}) { changes++ }
        assertNull(heading.current());assertFalse(shadowOf(manager).hasListener(heading))
        heading.setEnabled(true);heading.setEnabled(true)
        assertTrue(shadowOf(manager).hasListener(heading,sensor))
        fun send(degrees:Double) {
            val event=ShadowSensorManager.createSensorEvent(4,Sensor.TYPE_ROTATION_VECTOR)
            event.sensor=sensor;event.accuracy=SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            event.timestamp=SystemClock.elapsedRealtimeNanos()
            event.values[2]=-sin(Math.toRadians(degrees)/2).toFloat()
            event.values[3]=cos(Math.toRadians(degrees)/2).toFloat()
            shadowOf(manager).sendSensorEventToListeners(event)
        }
        send(0.0);assertEquals(0.0,heading.current()!!,.001)
        repeat(10) { ShadowSystemClock.advanceBy(Duration.ofMillis(100));send(90.0) }
        assertTrue(heading.current()!! in 85.0..90.1);assertTrue(changes>1)
        heading.onAccuracyChanged(sensor,SensorManager.SENSOR_STATUS_UNRELIABLE)
        assertNull(heading.current())
        ShadowSystemClock.advanceBy(Duration.ofMillis(100));send(90.0)
        assertEquals(90.0,heading.current()!!,.001)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(3));assertNull(heading.current())
        heading.setEnabled(false)
        assertFalse(shadowOf(manager).hasListener(heading));assertNull(heading.current())
        send(180.0);assertNull(heading.current())
    }
    @Test fun noAbsoluteRotationSensorLeavesDirectionUnavailableWithoutAnySubscription() {
        val context=RuntimeEnvironment.getApplication()
        val manager=context.getSystemService(SensorManager::class.java)
        val heading=PhoneHeading(context,{0}) {}
        heading.setEnabled(true)
        assertFalse(shadowOf(manager).hasListener(heading));assertNull(heading.current())
    }
}

package de.bcycle.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

/** No assumed sea-level pressure: altitude is unavailable until a real GPS reference exists. */
internal class PhoneBarometer(context:Context) : SensorEventListener {
    private val manager=context.getSystemService(SensorManager::class.java)
    private val sensor=manager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    val available get()=sensor!=null
    private var active=false
    private var pressureHeight:Double?=null
    private var at=0L
    private var offset:Double?=null
    private var seaLevel=false
    fun start() { if(!active && sensor!=null) active=manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL) }
    fun stop() { manager.unregisterListener(this);active=false;pressureHeight=null;offset=null }
    fun resetReference() { offset=null }
    override fun onSensorChanged(event:SensorEvent) {
        val pressure=event.values.firstOrNull()?.toDouble() ?: return
        if(pressure !in 300.0..1100.0) return
        pressureHeight=SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE,pressure.toFloat()).toDouble()
        at=event.timestamp/1_000_000
    }
    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int) {}
    fun reading(reference:SourceReading?,now:Long=SystemClock.elapsedRealtime()):SourceReading? {
        val height=pressureHeight ?: return null
        if(!active || now-at !in 0..5000) return null
        if(offset==null && reference!=null && now-reference.atMs in 0..5000) { offset=reference.value-height;seaLevel=reference.seaLevel }
        return offset?.let { SourceReading(height+it,at,seaLevel) }
    }
}

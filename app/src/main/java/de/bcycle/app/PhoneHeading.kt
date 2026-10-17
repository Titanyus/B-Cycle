package de.bcycle.app

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.Surface
import kotlin.math.*

/** Small circular display filter; no heading is inferred from speed or GPS course. */
internal class PhoneHeadingFilter {
    private var at:Long?=null
    private var heading:Double?=null
    fun clear() { at=null;heading=null }
    fun add(value:Double,time:Long):Double? {
        if(!value.isFinite() || at?.let { time<=it }==true) return current(time)
        val old=heading
        val gain=at?.let { 1-exp(-(time-it)/200.0) } ?: 1.0
        heading=if(old==null) normalize(value) else normalize(old+delta(old,value)*gain)
        at=time;return heading
    }
    fun current(now:Long)=heading.takeIf { at?.let { now-it in 0..2000 }==true }
    companion object {
        fun normalize(value:Double)=((value%360)+360)%360
        fun delta(a:Double,b:Double)=((b-a+540)%360)-180
    }
}

/** Only subscribed while the dashboard map is visible; independent of FIT and navigation course. */
internal class PhoneHeading(context:Context,private val rotation:()->Int,private val changed:()->Unit):SensorEventListener {
    private val manager=context.getSystemService(SensorManager::class.java)
    private val sensor=manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    private val matrix=FloatArray(9)
    private val remapped=FloatArray(9)
    private val angles=FloatArray(3)
    private val filter=PhoneHeadingFilter()
    private var running=false
    private var sampledAt=Long.MIN_VALUE
    private var published:Double?=null
    private var declination=0.0
    private var location:Pair<Double,Double>?=null
    private var locationAt=0L
    fun updateLocation(gps:GpsValues) {
        val lat=gps.latitude ?: return;val lon=gps.longitude ?: return
        val now=System.currentTimeMillis();val old=location
        if(old==null || abs(old.first-lat)>.01 || abs(old.second-lon)>.01 || now-locationAt>86_400_000) {
            declination=GeomagneticField(lat.toFloat(),lon.toFloat(),(gps.altitudeM ?: 0.0).toFloat(),now).declination.toDouble()
            location=lat to lon;locationAt=now
        }
    }
    fun setEnabled(enabled:Boolean) {
        if(enabled && !running && sensor!=null) running=manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_UI)
        else if(!enabled) stop()
    }
    fun stop() {
        if(running) manager.unregisterListener(this)
        running=false;filter.clear();published=null;sampledAt=Long.MIN_VALUE
    }
    fun current(now:Long=SystemClock.elapsedRealtime())=filter.current(now)?.let { PhoneHeadingFilter.normalize(it+declination) }
    override fun onSensorChanged(event:SensorEvent) {
        if(!running) return
        val now=SystemClock.elapsedRealtime();val time=event.timestamp/1_000_000
        if(event.accuracy==SensorManager.SENSOR_STATUS_UNRELIABLE || now-time !in 0..2000) { invalidate();return }
        if(sampledAt!=Long.MIN_VALUE && time-sampledAt<100) return
        if(event.values.size<3 || event.values.any { !it.isFinite() }) { invalidate();return }
        sampledAt=time
        SensorManager.getRotationMatrixFromVector(matrix,event.values)
        val (x,y)=when(rotation()) {
            Surface.ROTATION_90->SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180->SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270->SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else->SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        if(!SensorManager.remapCoordinateSystem(matrix,x,y,remapped)) { invalidate();return }
        // A nearly vertical screen-top direction has no reliable horizontal heading.
        if(hypot(remapped[1].toDouble(),remapped[4].toDouble())<.1) { invalidate();return }
        SensorManager.getOrientation(remapped,angles)
        val value=filter.add(Math.toDegrees(angles[0].toDouble()),time) ?: return
        if(published==null || abs(PhoneHeadingFilter.delta(published!!,value))>=.5) { published=value;changed() }
    }
    override fun onAccuracyChanged(sensor:Sensor,accuracy:Int) { if(running && accuracy==SensorManager.SENSOR_STATUS_UNRELIABLE) invalidate() }
    private fun invalidate() { val had=published!=null;filter.clear();published=null;if(had) changed() }
}

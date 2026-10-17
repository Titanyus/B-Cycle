package de.bcycle.app

import kotlin.math.*

/** Display-only, one-fix-delayed playback; never extrapolates or feeds recording. */
internal class MapMotion {
    companion object { const val frameMs = 33L; const val initialDelayMs = 1100.0 }
    private data class Fix(val at:Long,val value:GpsValues)
    private val fixes=ArrayDeque<Fix>()
    private var target=GpsValues()
    private var cadence=1000.0
    private var delay=initialDelayMs
    private var wantedDelay=initialDelayMs
    private var evaluatedAt:Long?=null
    private var playedAt:Double?=null
    var active=false;private set
    fun reset() { snap(GpsValues());cadence=1000.0;delay=initialDelayMs;wantedDelay=delay }
    fun snap(value:GpsValues):GpsValues {
        fixes.clear();target=value;active=false;evaluatedAt=null;playedAt=null
        return value
    }
    fun move(value:GpsValues,now:Long,animate:Boolean):GpsValues {
        at(now)
        if(!animate || !valid(target) || !valid(value) || distance(target,value)>200.0) {
            snap(value)
            if(valid(value)) fixes.addLast(Fix(now,value))
            return value
        }
        if(value.latitude==target.latitude && value.longitude==target.longitude && value.bearingDegrees==target.bearingDegrees) {
            target=value;return at(now) // Metadata/track updates must not invent a new GPS fix.
        }
        if(fixes.isEmpty()) fixes.addLast(Fix(now-1000,target))
        val interval=now-fixes.last().at
        if(interval<=0) return at(now)
        if(interval>5000) { snap(value);fixes.addLast(Fix(now,value));return value }
        if(interval in 200..2500) {
            cadence=cadence*.8+interval*.2
            wantedDelay=(cadence+100).coerceIn(600.0,2600.0)
        }
        target=value;fixes.addLast(Fix(now,value))
        while(fixes.size>8) fixes.removeFirst()
        active=true
        return at(now)
    }
    fun at(now:Long):GpsValues {
        val dt=evaluatedAt?.let { (now-it).coerceAtLeast(0) } ?: 0
        evaluatedAt=now
        // Adapt the buffer gradually: changing GPS cadence must not jump the displayed position.
        delay+=(wantedDelay-delay).coerceIn(-dt*.2,dt*.2)
        val playback=maxOf(playedAt ?: Double.NEGATIVE_INFINITY,now-delay)
        playedAt=playback
        if(fixes.size<2) { active=false;return target }
        while(fixes.size>2 && fixes.elementAt(1).at<=playback) fixes.removeFirst()
        val a=fixes.first();val b=fixes.elementAt(1)
        if(playback>=fixes.last().at) { active=false;return target }
        active=true
        val fraction=((playback-a.at)/(b.at-a.at)).coerceIn(0.0,1.0)
        fun lerp(x:Double,y:Double)=x+(y-x)*fraction
        fun mercator(lat:Double)=ln(tan(PI/4+Math.toRadians(lat)/2))
        val lat=Math.toDegrees(2*atan(exp(lerp(mercator(a.value.latitude!!),mercator(b.value.latitude!!))))-PI/2)
        val lon=wrap(a.value.longitude!!+delta(a.value.longitude!!,b.value.longitude!!)*fraction)
        val bearing=b.value.bearingDegrees?.let { end -> a.value.bearingDegrees?.let { start -> (start+delta(start,end)*fraction+360)%360 } ?: end }
        return target.copy(latitude=lat,longitude=lon,bearingDegrees=bearing)
    }
    private fun valid(v:GpsValues)=v.latitude?.let { it.isFinite() && abs(it)<=85.05112878 }==true &&
        v.longitude?.let { it.isFinite() && abs(it)<=180 }==true
    private fun delta(a:Double,b:Double)=((b-a+540)%360)-180
    private fun wrap(v:Double)=((v+540)%360)-180
    private fun distance(a:GpsValues,b:GpsValues):Double {
        val lat=Math.toRadians(b.latitude!!-a.latitude!!)
        val lon=Math.toRadians(delta(a.longitude!!,b.longitude!!))
        val h=sin(lat/2).pow(2)+cos(Math.toRadians(a.latitude!!))*cos(Math.toRadians(b.latitude!!))*sin(lon/2).pow(2)
        return 12_742_000*asin(sqrt(h.coerceIn(0.0,1.0)))
    }
}

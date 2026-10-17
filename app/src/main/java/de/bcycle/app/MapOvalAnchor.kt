package de.bcycle.app

import kotlin.math.*

/** Display-only north-up look-ahead. Fractions stay one third away from every map edge. */
internal class MapOvalAnchor {
    companion object { const val preference="mapOvalFollow" }
    data class Anchor(val x:Float,val y:Float)
    private var angle=0.0
    private var target=0.0
    private var previous:Long?=null
    var active=false;private set
    fun reset() { angle=0.0;target=0.0;previous=null;active=false }
    fun hold(now:Long) { target=angle;previous=now;active=false }
    fun at(course:Double?,moving:Boolean,now:Long,width:Int,height:Int):Anchor {
        val dt=previous?.let { (now-it).coerceIn(0,100) } ?: 0
        previous=now
        if(!moving || course==null || !course.isFinite()) { hold(now);return point(angle,width,height) }
        if(course!=null && course.isFinite() && abs(PhoneHeadingFilter.delta(target,course))>=4.0)
            target=PhoneHeadingFilter.normalize(course)
        val difference=PhoneHeadingFilter.delta(angle,target)
        if(abs(difference)<.1) { angle=target;active=false }
        else {
            val step=(difference*(1-exp(-dt/900.0))).coerceIn(-dt*.06,dt*.06)
            angle=PhoneHeadingFilter.normalize(angle+step);active=true
        }
        return point(angle,width,height)
    }
    internal fun point(course:Double,width:Int,height:Int):Anchor {
        if(width<=0 || height<=0) return Anchor(.5f,2f/3f)
        val radians=Math.toRadians(course)
        val dx=sin(radians);val dy=-cos(radians)
        val rx=width/6.0;val ry=height/6.0
        // Intersect the reverse course ray with the ellipse, so the course points exactly at its centre.
        val radius=1/sqrt((dx/rx).pow(2)+(dy/ry).pow(2))
        return Anchor((.5-radius*dx/width).toFloat(),(.5-radius*dy/height).toFloat())
    }
}

package de.bcycle.app

import kotlin.math.*

/** Course filtering is unchanged; zoom gently changes the look-ahead ellipse's radius. */
internal class MapOvalAnchor {
    companion object { const val preference="mapOvalFollow" }
    data class Anchor(val x:Float,val y:Float)
    private var angle=0.0
    private var target=0.0
    private var margin:Double?=null
    private var previous:Long?=null
    var active=false;private set
    fun reset() { angle=0.0;target=0.0;previous=null;active=false }
    fun hold(now:Long) { target=angle;previous=now;active=false }
    fun at(course:Double?,moving:Boolean,now:Long,width:Int,height:Int,zoom:Double=14.0):Anchor {
        val dt=previous?.let { (now-it).coerceIn(0,100) } ?: 0
        previous=now
        val desired=MapZoom.margin(zoom)
        val old=margin ?: desired
        val next=old+(desired-old)*(1-exp(-dt/350.0))
        margin=if(abs(next-desired)<.0001) desired else next
        var turning=false
        if(moving && course!=null && course.isFinite()) {
            if(abs(PhoneHeadingFilter.delta(target,course))>=4.0) target=PhoneHeadingFilter.normalize(course)
            val difference=PhoneHeadingFilter.delta(angle,target)
            if(abs(difference)<.1) angle=target else {
                val step=(difference*(1-exp(-dt/900.0))).coerceIn(-dt*.06,dt*.06)
                angle=PhoneHeadingFilter.normalize(angle+step);turning=true
            }
        } else target=angle
        active=turning || margin!=desired
        return pointAtMargin(angle,width,height,margin!!)
    }
    internal fun point(course:Double,width:Int,height:Int,zoom:Double=14.0)=pointAtMargin(course,width,height,MapZoom.margin(zoom))
    private fun pointAtMargin(course:Double,width:Int,height:Int,margin:Double):Anchor {
        if(width<=0 || height<=0 || margin>=.5) return Anchor(.5f,.5f)
        val radians=Math.toRadians(course);val dx=sin(radians);val dy=-cos(radians)
        val rx=width*(.5-margin);val ry=height*(.5-margin)
        val radius=1/sqrt((dx/rx).pow(2)+(dy/ry).pow(2))
        return Anchor((.5-radius*dx/width).toFloat(),(.5-radius*dy/height).toFloat())
    }
}

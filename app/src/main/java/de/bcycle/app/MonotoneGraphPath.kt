package de.bcycle.app

import android.graphics.Path
import android.graphics.PointF
import kotlin.math.hypot

/** Shape-preserving cubic Hermite interpolation for display only, with no new extrema. */
internal object MonotoneGraphPath {
    fun append(path:Path,points:List<PointF>) {
        if(points.isEmpty()) return
        path.moveTo(points.first().x,points.first().y)
        var start=0
        for(i in 1 until points.size) if(points[i].x<=points[i-1].x) {
            run(path,points,start,i-1)
            // Stationary distance/rounded pixel coordinates retain their actual vertical edge.
            path.lineTo(points[i].x,points[i].y);start=i
        }
        run(path,points,start,points.lastIndex)
    }
    private fun run(path:Path,p:List<PointF>,start:Int,end:Int) {
        val n=end-start+1
        if(n<2) return
        if(n==2) { path.lineTo(p[end].x,p[end].y);return }
        val widths=DoubleArray(n-1) { (p[start+it+1].x-p[start+it].x).toDouble() }
        val slopes=DoubleArray(n-1) { (p[start+it+1].y-p[start+it].y)/widths[it] }
        val tangents=DoubleArray(n)
        tangents[0]=slopes.first();tangents[n-1]=slopes.last()
        for(i in 1 until n-1) {
            val before=slopes[i-1];val after=slopes[i]
            if(before*after>0) {
                val a=2*widths[i]+widths[i-1];val b=widths[i]+2*widths[i-1]
                tangents[i]=(a+b)/(a/before+b/after)
            }
        }
        for(i in slopes.indices) {
            if(slopes[i]==0.0) { tangents[i]=0.0;tangents[i+1]=0.0 }
            else {
                val magnitude=hypot(tangents[i]/slopes[i],tangents[i+1]/slopes[i])
                if(magnitude>3) { val scale=3/magnitude;tangents[i]*=scale;tangents[i+1]*=scale }
            }
        }
        for(i in slopes.indices) {
            val a=p[start+i];val b=p[start+i+1];val third=widths[i]/3
            val low=minOf(a.y,b.y);val high=maxOf(a.y,b.y)
            path.cubicTo((a.x+third).toFloat(),(a.y+tangents[i]*third).toFloat().coerceIn(low,high),
                (b.x-third).toFloat(),(b.y-tangents[i+1]*third).toFloat().coerceIn(low,high),b.x,b.y)
        }
    }
}

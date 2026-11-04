package de.bcycle.app

import kotlin.math.ceil
import kotlin.math.floor

/** Marching squares with shared edge vertices and connected paths; no Android/GL state. */
internal object TerrainContours {
    data class Line(val elevation:Int,val points:FloatArray)
    fun elevation(argb:Int):Float {
        val height=((argb ushr 16 and 255)*256+(argb ushr 8 and 255)+(argb and 255)/256f)-32768f
        return if((argb ushr 24)==0 || height !in -12000f..9000f) Float.NaN else height
    }
    fun interval(zoom:Int)=when { zoom<=10 -> 100;zoom==11 -> 50;zoom==12 -> 20;else -> 10 }
    private class Node(val x:Float,val y:Float) { val adjacent=ArrayList<Int>(2) }
    fun trace(grid:FloatArray,size:Int,interval:Int,cancelled:()->Boolean={ false },emit:(Line)->Unit) {
        require(size>=2 && grid.size==size*size && interval>0)
        var minimum=Float.POSITIVE_INFINITY;var maximum=Float.NEGATIVE_INFINITY
        for(value in grid) if(value.isFinite()) { minimum=minOf(minimum,value);maximum=maxOf(maximum,value) }
        if(!minimum.isFinite() || minimum>=maximum) return
        val lows=FloatArray((size-1)*(size-1));val highs=FloatArray(lows.size)
        for(y in 0 until size-1) for(x in 0 until size-1) {
            val a=grid[y*size+x];val b=grid[y*size+x+1];val c=grid[(y+1)*size+x];val d=grid[(y+1)*size+x+1]
            val i=y*(size-1)+x
            if(!a.isFinite() || !b.isFinite() || !c.isFinite() || !d.isFinite()) { lows[i]=Float.POSITIVE_INFINITY;highs[i]=Float.NEGATIVE_INFINITY }
            else { lows[i]=minOf(a,b,c,d);highs[i]=maxOf(a,b,c,d) }
        }
        // A separate graph per elevation bounds temporary memory even in steep terrain.
        for(height in ceil(minimum/interval).toInt()*interval..floor(maximum/interval).toInt()*interval step interval) {
            if(cancelled()) return
            val nodes=HashMap<Int,Node>()
            for(y in 0 until size-1) {
                if(cancelled()) return
                for(x in 0 until size-1) {
                    val cell=y*(size-1)+x
                    if(height<lows[cell] || height>highs[cell] || lows[cell]==highs[cell]) continue
                    val values=floatArrayOf(grid[y*size+x],grid[y*size+x+1],grid[(y+1)*size+x+1],grid[(y+1)*size+x])
                    val edges=IntArray(4);var count=0
                    for(edge in 0..3) if((values[edge]>=height)!=(values[(edge+1)%4]>=height)) edges[count++]=edge
                    if(count==0) continue
                    fun vertex(edge:Int):Int {
                        val id=when(edge) { 0 -> (y*size+x)*2;1 -> (y*size+x+1)*2+1;2 -> ((y+1)*size+x)*2;else -> (y*size+x)*2+1 }
                        nodes.getOrPut(id) {
                            val a=values[edge];val b=values[(edge+1)%4];val t=(height-a)/(b-a)
                            when(edge) {
                                0 -> Node(x+t,y.toFloat());1 -> Node((x+1).toFloat(),y+t)
                                2 -> Node(x+1-t,(y+1).toFloat());else -> Node(x.toFloat(),y+1-t)
                            }
                        }
                        return id
                    }
                    fun connect(a:Int,b:Int) { val first=vertex(a);val second=vertex(b);nodes.getValue(first).adjacent.add(second);nodes.getValue(second).adjacent.add(first) }
                    if(count==2) connect(edges[0],edges[1])
                    else if((values.average()>=height)==(values[0]>=height)) { connect(0,1);connect(2,3) }
                    else { connect(0,3);connect(1,2) }
                }
            }
            val visited=HashSet<Int>()
            // Start open paths at their endpoint before consuming closed loops.
            val starts=nodes.keys.sortedBy { if(nodes.getValue(it).adjacent.size==1) 0 else 1 }
            for(start in starts) {
                if(start in visited) continue
                val points=ArrayList<Float>();var current=start;var previous=-1
                do {
                    val node=nodes.getValue(current);points.add(node.x);points.add(node.y);visited.add(current)
                    val next=node.adjacent.firstOrNull { it!=previous } ?: break
                    previous=current;current=next
                    if(current==start) { val first=nodes.getValue(start);points.add(first.x);points.add(first.y) }
                } while(current!=start && current !in visited)
                if(points.size>=4) emit(Line(height,points.toFloatArray()))
            }
        }
    }
}

package de.bcycle.app

import android.graphics.Color
import org.oscim.backend.GL
import org.oscim.backend.GLAdapter
import org.oscim.backend.canvas.Paint.Cap
import org.oscim.core.MapPosition
import org.oscim.layers.Layer
import org.oscim.renderer.*
import org.oscim.renderer.bucket.*
import org.oscim.theme.styles.LineStyle
import kotlin.math.*

/** Immutable world coordinates, prepared on the existing geometry worker. */
internal class GlRouteGeometry(val xy:DoubleArray,val starts:BooleanArray)
internal data class GlRouteLine(val geometry:GlRouteGeometry,val style:MapLineStyle,val planned:Boolean)
internal data class GlRouteBatch(val pieces:List<FloatArray>,val endpoints:FloatArray,val style:MapLineStyle,val planned:Boolean)

/** Camera-local clipping only; every later frame moves the cached vertices with VTM's actual GL matrix. */
internal object GlRouteProjection {
    fun wrap(x:Double)=x-floor(x+.5)
    fun batches(lines:List<GlRouteLine>,reference:MapPosition,limit:Double):List<GlRouteBatch> {
        val scale=256*reference.scale
        return lines.filter { it.style.opacity>0 }.map { line ->
            val g=line.geometry;val pieces=mutableListOf<FloatArray>();val pending=ArrayList<Float>()
            fun flush() { if(pending.size>=4) pieces.add(pending.toFloatArray());pending.clear() }
            fun point(index:Int)=doubleArrayOf(wrap(g.xy[index*2]-reference.x)*scale,(g.xy[index*2+1]-reference.y)*scale)
            var previous:DoubleArray?=null
            for(i in g.starts.indices) {
                val p=point(i)
                val old=previous
                if(g.starts[i]) flush()
                if(old!=null && !g.starts[i] && abs(p[0]-old[0])<scale*.5) {
                    val clipped=clip(old[0],old[1],p[0],p[1],limit)
                    if(clipped==null) flush() else {
                        if(pending.isNotEmpty() && (abs(pending[pending.size-2]-clipped[0])>.05 || abs(pending.last()-clipped[1])>.05)) flush()
                        if(pending.isEmpty()) { pending.add(clipped[0].toFloat());pending.add(clipped[1].toFloat()) }
                        if(hypot(clipped[2]-pending[pending.size-2],clipped[3]-pending.last())>.05) { pending.add(clipped[2].toFloat());pending.add(clipped[3].toFloat()) }
                    }
                }
                previous=p
            }
            flush()
            val endpoints=if(line.planned && g.starts.isNotEmpty()) point(0)+point(g.starts.lastIndex) else doubleArrayOf()
            GlRouteBatch(pieces,endpoints.map { it.toFloat() }.toFloatArray(),line.style,line.planned)
        }
    }
    private fun clip(x0:Double,y0:Double,x1:Double,y1:Double,limit:Double):DoubleArray? {
        val dx=x1-x0;val dy=y1-y0;var low=0.0;var high=1.0
        fun edge(p:Double,q:Double):Boolean {
            if(abs(p)<1e-12) return q>=0
            val value=q/p
            if(p<0) low=max(low,value) else high=min(high,value)
            return low<=high
        }
        if(!edge(-dx,x0+limit) || !edge(dx,limit-x0) || !edge(-dy,y0+limit) || !edge(dy,limit-y0)) return null
        return doubleArrayOf(x0+low*dx,y0+low*dy,x0+high*dx,y0+high*dy)
    }
    fun setFrameMatrix(v:GLViewport,reference:MapPosition) {
        val scale=256*v.pos.scale
        v.mvp.setTransScale((wrap(reference.x-v.pos.x)*scale).toFloat(),((reference.y-v.pos.y)*scale).toFloat(),
            (v.pos.scale/reference.scale).toFloat()/MapRenderer.COORD_SCALE)
        v.mvp.multiplyLhs(v.viewproj)
    }
    fun needsRebase(reference:MapPosition,current:MapPosition):Boolean {
        val ratio=current.scale/reference.scale
        return ratio<.75 || ratio>1.5 || abs(wrap(current.x-reference.x))*256*reference.scale>512 || abs(current.y-reference.y)*256*reference.scale>512
    }
}

internal class VtmRouteLayer(private val view:BicycleMapView,private val density:Float):Layer(view.map()) {
    private data class Snapshot(val revision:Long,val lines:List<GlRouteLine>)
    @Volatile private var snapshot=Snapshot(0,emptyList())
    private var detached=false
    private val renderer=RouteRenderer()
    init { mRenderer=renderer }
    fun show(lines:List<GlRouteLine>) {
        if(detached || snapshot.lines==lines) return
        snapshot=Snapshot(snapshot.revision+1,lines.toList());map().updateMap(true)
    }
    override fun onDetach() {
        if(detached) return
        detached=true;snapshot=Snapshot(snapshot.revision+1,emptyList())
        view.queueEvent { renderer.release() }
    }
    private inner class RouteRenderer:BucketRenderer() {
        private var revision=-1L
        private val styles=java.util.IdentityHashMap<RouteLineBucket,GlRouteBatch>()
        private lateinit var shader:RouteShader
        override fun setup():Boolean { shader=RouteShader();revision=-1;mInitialized=false;return true }
        override fun update(v:GLViewport) {
            val next=snapshot
            if(mInitialized && revision==next.revision && !GlRouteProjection.needsRebase(mMapPosition,v.pos)) return
            mInitialized=true;revision=next.revision;mMapPosition.copy(v.pos)
            buckets.clear();styles.clear()
            // Native vertex coordinates use signed shorts. Leave room for the stroke caps.
            val limit=32700.0/MapRenderer.COORD_SCALE-32*density
            var level=0
            var head:RenderBucket?=null;var tail:RenderBucket?=null
            fun append(bucket:RenderBucket) { if(tail==null) head=bucket else tail!!.next=bucket;tail=bucket }
            for(batch in GlRouteProjection.batches(next.lines,mMapPosition,limit)) {
                val line=RouteLineBucket(level++).also(::append)
                line.setExtents(-limit.toInt(),limit.toInt())
                val radius=(batch.style.widthDp+if(batch.planned) 0 else 2)*density/2
                line.line=LineStyle(batch.style.argb,radius,Cap.ROUND)
                batch.pieces.forEach { line.addLine(it,it.size,false) };styles[line]=batch
                if(batch.planned) for(i in 0 until batch.endpoints.size/2) {
                    val x=batch.endpoints[i*2];val y=batch.endpoints[i*2+1]
                    if(abs(x)>limit || abs(y)>limit) continue
                    val color=if(i==0) Color.rgb(0,150,70) else Color.rgb(220,40,30)
                    RoutePointBucket(level++,x,y,color).also(::append)
                }
            }
            buckets.set(head);compile()
        }
        override fun render(v:GLViewport) {
            // Use this frame's GLViewport, never a UI-thread camera sample.
            GLState.test(false,false);GLState.blend(true);GlRouteProjection.setFrameMatrix(v,mMapPosition);buckets.bind()
            var bucket:RenderBucket?=buckets.get()
            while(bucket!=null) {
                if(bucket is RouteLineBucket) {
                    val batch=styles.getValue(bucket)
                    shader.useProgram();GLState.enableVertexArrays(shader.position,GLState.DISABLED)
                    GLAdapter.gl.vertexAttribPointer(shader.position,4,GL.SHORT,false,0,buckets.offset[RenderBucket.LINE.toInt()])
                    v.mvp.setAsUniform(shader.matrix)
                    val outer=(batch.style.widthDp+if(batch.planned) 0 else 2)*density/2
                    val div=(v.pos.scale/mMapPosition.scale).toFloat()
                    GLAdapter.gl.uniform1f(shader.width,(outer+.5f)*MapRenderer.COORD_SCALE/LineBucket.DIR_SCALE/div)
                    GLAdapter.gl.uniform1f(shader.radius,outer+.5f)
                    GLAdapter.gl.uniform1f(shader.edge,outer)
                    GLAdapter.gl.uniform1f(shader.core,batch.style.widthDp*density/2)
                    GLAdapter.gl.uniform1f(shader.border,if(batch.planned) 0f else 1f)
                    GLAdapter.gl.uniform1i(shader.point,0)
                    GLAdapter.gl.uniform1i(shader.round,if(bucket.roundCap) 1 else 0)
                    val color=batch.style.argb
                    GLAdapter.gl.uniform4f(shader.color,Color.red(color)/255f,Color.green(color)/255f,Color.blue(color)/255f,Color.alpha(color)/255f)
                    val transparent=Color.alpha(color)<255
                    if(transparent) {
                        GLAdapter.gl.depthMask(true);GLAdapter.gl.clear(GL.DEPTH_BUFFER_BIT);GLState.test(true,false)
                    }
                    GLAdapter.gl.drawArrays(GL.TRIANGLE_STRIP,bucket.vertexOffset,bucket.vertexCount)
                    if(transparent) { GLAdapter.gl.depthMask(false);GLState.test(false,false) }
                    bucket=bucket.next
                } else if(bucket is RoutePointBucket) {
                    shader.useProgram();GLState.enableVertexArrays(shader.position,GLState.DISABLED)
                    GLAdapter.gl.vertexAttribPointer(shader.position,2,GL.SHORT,false,0,bucket.vertexOffset)
                    v.mvp.setAsUniform(shader.matrix)
                    GLAdapter.gl.uniform1i(shader.point,1)
                    GLAdapter.gl.uniform1f(shader.radius,7*density+.5f);GLAdapter.gl.uniform1f(shader.edge,7*density);GLAdapter.gl.uniform1f(shader.core,5*density)
                    GLAdapter.gl.uniform1f(shader.border,1f)
                    val color=bucket.color
                    GLAdapter.gl.uniform4f(shader.color,Color.red(color)/255f,Color.green(color)/255f,Color.blue(color)/255f,1f)
                    GLAdapter.gl.drawArrays(GL.POINTS,0,bucket.vertexCount)
                    bucket=bucket.next
                } else bucket=bucket.next
            }
        }
        fun release() { buckets.clear();styles.clear();setReady(false) }
    }
    private class RouteLineBucket(level:Int):LineBucket(level) { val vertexCount get()=numVertices }
    private class RoutePointBucket(level:Int,x:Float,y:Float,val color:Int):RenderBucket(CIRCLE,false,false) {
        val vertexCount get()=numVertices
        init {
            setLevel(level);vertexItems.add((x*MapRenderer.COORD_SCALE).toInt().toShort(),(y*MapRenderer.COORD_SCALE).toInt().toShort());numVertices=1
        }
    }
    private class RouteShader:GLShader() {
        init { check(create("bcycle_route")) { "VTM route shader could not be loaded" } }
        val matrix=getUniform("u_mvp");val width=getUniform("u_width");val color=getUniform("u_color")
        val radius=getUniform("u_radius");val edge=getUniform("u_edge");val core=getUniform("u_core");val border=getUniform("u_border")
        val round=getUniform("u_round");val point=getUniform("u_point");val position=getAttrib("a_pos")
    }
}

package de.bcycle.app

import android.content.Context
import android.graphics.*
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.backend.GL
import org.oscim.backend.GLAdapter
import org.oscim.core.MercatorProjection
import org.oscim.core.Point
import org.oscim.layers.Layer
import org.oscim.renderer.*
import org.oscim.renderer.bucket.TextureItem
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

internal data class GlGpsSnapshot(val x:Double,val y:Double,val latitude:Double,val accuracy:Double?,val heading:Double?,val height:String?)
internal data class GlGpsPose(val x:Float,val y:Float,val heading:Float?,val accuracyRadius:Float?)

/** Uses only the frozen GL frame, including its off-center follow anchor and date-line wrap. */
internal object GlGpsProjection {
    fun pose(frame:GLViewport,gps:GlGpsSnapshot):GlGpsPose {
        val p=Point()
        frame.toScreenPoint(frame.pos.x+GlRouteProjection.wrap(gps.x-frame.pos.x),gps.y,true,p)
        val heading=gps.heading?.let {
            val angle=Math.toRadians(it);val step=100/(256*frame.pos.scale)
            val tip=Point()
            frame.toScreenPoint(frame.pos.x+GlRouteProjection.wrap(gps.x-frame.pos.x)+sin(angle)*step,gps.y-cos(angle)*step,true,tip)
            Math.toDegrees(atan2(tip.x-p.x,-(tip.y-p.y))).toFloat()
        }
        val radius=gps.accuracy?.let { (it/(40075016.686*cos(Math.toRadians(gps.latitude))/(256*frame.pos.scale))).toFloat() }
        return GlGpsPose(p.x.toFloat(),p.y.toFloat(),heading,radius)
    }
}

/** Small reusable glyphs; no Android overlay or full-map bitmap upload on camera changes. */
internal class VtmGpsLayer(private val view:BicycleMapView,context:Context):Layer(view.map()) {
    private val density=context.resources.displayMetrics.density
    private val heightPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize=14*density;typeface=BicycleDesign.strongTypeface }
    private val dot=marker(false)
    private val arrow=marker(true)
    @Volatile internal var snapshot:GlGpsSnapshot?=null;private set
    private var accuracyShown=false
    @Volatile private var detached=false
    private val renderer=GpsRenderer()
    init { mRenderer=renderer }
    fun show(gps:GpsValues,heading:Double?,showHeight:Boolean) {
        if(detached) return
        val lat=gps.latitude;val lon=gps.longitude
        val next=if(lat==null || lon==null || !lat.isFinite() || !lon.isFinite() || abs(lat)>85.05112878 || abs(lon)>180) {
            accuracyShown=false;null
        } else {
            val accuracy=gps.accuracyM?.takeIf { it.isFinite() && it>0 }
            accuracyShown=accuracy!=null && if(accuracyShown) accuracy>10 else accuracy>15
            GlGpsSnapshot(MercatorProjection.longitudeToX(lon),MercatorProjection.latitudeToY(lat),lat,accuracy?.takeIf { accuracyShown },
                heading?.takeIf { it.isFinite() },gps.altitudeM?.takeIf { showHeight && it.isFinite() }?.let { "${it.roundToInt()} m" })
        }
        if(next!=snapshot) { snapshot=next;map().updateMap(true) }
    }
    private fun marker(compass:Boolean):Bitmap {
        val side=ceil(48*density).toInt()
        return Bitmap.createBitmap(side,side,Bitmap.Config.ARGB_8888).also {
            val canvas=Canvas(it);canvas.translate(side/2f,side/2f)
            val paint=Paint(Paint.ANTI_ALIAS_FLAG)
            if(compass) {
                val path=Path().apply { moveTo(0f,-21*density);lineTo(5*density,-10*density);lineTo(-5*density,-10*density);close() }
                paint.color=Color.WHITE;paint.style=Paint.Style.STROKE;paint.strokeWidth=3*density;canvas.drawPath(path,paint)
                paint.style=Paint.Style.FILL;paint.color=Color.rgb(0,100,245);canvas.drawPath(path,paint)
            } else {
                paint.color=Color.WHITE;canvas.drawCircle(0f,0f,11*density,paint)
                paint.color=Color.rgb(0,100,245);canvas.drawCircle(0f,0f,8*density,paint)
            }
        }
    }
    internal fun heightGlyph(text:String):Bitmap {
        val metrics=heightPaint.fontMetrics
        val w=ceil(heightPaint.measureText(text)+10*density+2).toInt()
        val h=ceil(metrics.descent-metrics.ascent+6*density+2).toInt()
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also {
            val canvas=Canvas(it)
            heightPaint.color=Color.argb(220,255,255,255);canvas.drawRoundRect(1f,1f,w-1f,h-1f,4*density,4*density,heightPaint)
            heightPaint.color=Color.BLACK;canvas.drawText(text,1+5*density,h/2f-(metrics.ascent+metrics.descent)/2,heightPaint)
        }
    }
    override fun onDetach() {
        if(detached) return
        detached=true;snapshot=null
        view.queueEvent { renderer.release();dot.recycle();arrow.recycle() }
    }
    private inner class GpsRenderer:LayerRenderer() {
        private lateinit var shader:GpsShader
        private val quad=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f));position(0)
        }
        private var dotTexture:TextureItem?=null
        private var arrowTexture:TextureItem?=null
        private var heightTexture:TextureItem?=null
        private var heightBitmap:Bitmap?=null
        private var heightText:String?=null
        private var frameGps:GlGpsSnapshot?=null
        override fun setup():Boolean {
            shader=GpsShader()
            // setup is repeated after context loss: discard old GL names, keep the CPU glyphs.
            dotTexture=null;arrowTexture=null;heightTexture=null
            heightBitmap?.recycle();heightBitmap=null;heightText=null
            return true
        }
        override fun update(v:GLViewport) { frameGps=snapshot;setReady(frameGps!=null && !detached) }
        override fun render(v:GLViewport) {
            val gps=frameGps ?: return
            if(detached) return
            val pose=GlGpsProjection.pose(v,gps)
            v.useScreenCoordinates(true,1f)
            shader.useProgram();GLState.test(false,false);GLState.blend(true)
            GLState.bindVertexBuffer(0);GLState.enableVertexArrays(shader.position,GLState.DISABLED)
            quad.position(0);GLAdapter.gl.vertexAttribPointer(shader.position,2,GL.FLOAT,false,0,quad)
            v.mvp.setAsUniform(shader.matrix)
            GLAdapter.gl.uniform1i(shader.sampler,0)
            fun draw(x:Float,y:Float,halfW:Float,halfH:Float,angle:Float=0f,texture:TextureItem?=null) {
                GLAdapter.gl.uniform2f(shader.center,x,y);GLAdapter.gl.uniform2f(shader.size,halfW,halfH)
                val radians=Math.toRadians(angle.toDouble())
                GLAdapter.gl.uniform2f(shader.rotation,cos(radians).toFloat(),sin(radians).toFloat())
                GLAdapter.gl.uniform1i(shader.mode,if(texture==null) 1 else 0)
                texture?.bind()
                GLAdapter.gl.drawArrays(GL.TRIANGLE_STRIP,0,4)
            }
            val texture=dotTexture ?: TextureItem(AndroidBitmap(dot)).also { dotTexture=it }
            GLAdapter.gl.activeTexture(GL.TEXTURE0);texture.bind()
            pose.accuracyRadius?.takeIf { it.isFinite() && it>0 }?.let { radius ->
                GLAdapter.gl.uniform1f(shader.radius,radius);GLAdapter.gl.uniform1f(shader.stroke,density)
                draw(pose.x,pose.y,radius+density+1,radius+density+1)
            }
            pose.heading?.let { angle ->
                val texture=arrowTexture ?: TextureItem(AndroidBitmap(arrow)).also { arrowTexture=it }
                draw(pose.x,pose.y,arrow.width/2f,arrow.height/2f,angle,texture)
            }
            draw(pose.x,pose.y,dot.width/2f,dot.height/2f,texture=texture)
            if(gps.height!=heightText) {
                heightTexture?.dispose();heightTexture=null;heightBitmap?.recycle();heightBitmap=null
                heightText=gps.height
                gps.height?.let { heightBitmap=heightGlyph(it);heightTexture=TextureItem(AndroidBitmap(heightBitmap!!)) }
            }
            heightTexture?.let { label -> draw(pose.x+18*density-1+label.width/2f,pose.y,label.width/2f,label.height/2f,texture=label) }
        }
        fun release() {
            dotTexture?.dispose();arrowTexture?.dispose();heightTexture?.dispose()
            dotTexture=null;arrowTexture=null;heightTexture=null;heightBitmap?.recycle();heightBitmap=null
            setReady(false)
        }
    }
    private class GpsShader:GLShader() {
        init { check(create("bcycle_gps")) { "VTM GPS shader could not be loaded" } }
        val matrix=getUniform("u_mvp");val center=getUniform("u_center");val size=getUniform("u_size")
        val rotation=getUniform("u_rotation");val mode=getUniform("u_mode");val sampler=getUniform("u_tex")
        val radius=getUniform("u_radius");val stroke=getUniform("u_stroke");val position=getAttrib("a_pos")
    }
}

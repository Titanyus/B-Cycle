package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import kotlin.math.ceil
import kotlin.math.sqrt

/** Cached chrome for drawable-only surfaces (popup windows and native switch thumbs). */
internal class BicycleChromaticSurface(private val context:Context,tokens:ThemeTokens,chromatic:Double,
    private val radiusDp:Int,private val fillColor:Int,private val gradientFill:Boolean=false,private val insetDp:Int=0):GradientDrawable() {
    private val density=context.resources.displayMetrics.density
    private val palette=BicycleAccentGradient.palette(tokens.primary,chromatic)
    private val colors=BicycleInputFrame.colors(tokens.primary,chromatic)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect=RectF()
    private val inside=Path()
    private data class Raster(val source:Bitmap,val near:Bitmap,val outer:Bitmap,val scale:Float,val pad:Float)
    private var raster:Raster?=null
    init { setColor(fillColor);cornerRadius=BicycleDesign.dp(context,radiusDp).toFloat() }
    override fun getConstantState():ConstantState?=null
    private fun clear() { raster?.let { it.source.recycle();it.near.recycle();it.outer.recycle() };raster=null }
    override fun setVisible(visible:Boolean,restart:Boolean):Boolean { if(!visible) clear();return super.setVisible(visible,restart) }
    override fun onBoundsChange(bounds:Rect) {
        super.onBoundsChange(bounds);clear();rect.set(bounds)
        rect.inset(insetDp*density,insetDp*density)
    }
    private fun raster():Raster {
        raster?.let { return it }
        val pad=ceil(BicycleDesign.inputFocusOuterBlur*density+4)
        val scale=minOf(1f,sqrt(128_000f/((rect.width()+pad*2)*(rect.height()+pad*2))))
        val source=Bitmap.createBitmap(ceil((rect.width()+pad*2)*scale).toInt().coerceAtLeast(1),ceil((rect.height()+pad*2)*scale).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        val canvas=Canvas(source);canvas.scale(scale,scale);canvas.translate(pad,pad)
        val stroke=BicycleDesign.inputFocusStroke*density
        paint.style=Paint.Style.STROKE;paint.strokeWidth=stroke;paint.alpha=255;paint.blendMode=null
        paint.shader=BicycleAccentGradient.shader(RectF(0f,0f,rect.width(),rect.height()),BicycleDesign.inputFocusAngle,colors,BicycleInputFrame.positions())
        val radius=BicycleDesign.dp(context,radiusDp).toFloat()
        canvas.drawRoundRect(stroke/2,stroke/2,rect.width()-stroke/2,rect.height()-stroke/2,radius,radius,paint)
        val near=BicycleContourBlur.blur(source,BicycleDesign.inputFocusNearBlur*density*scale,BicycleDesign.inputFocusNearIntensity)
        val outer=BicycleContourBlur.blur(source,BicycleDesign.inputFocusOuterBlur*density*scale,BicycleDesign.inputFocusOuterIntensity)
        return Raster(source,near,outer,scale,pad).also { raster=it }
    }
    override fun draw(canvas:Canvas) {
        if(rect.width()<=0 || rect.height()<=0) return
        val radius=BicycleDesign.dp(context,radiusDp).toFloat()
        paint.style=Paint.Style.FILL;paint.color=fillColor;paint.alpha=alpha;paint.blendMode=null
        paint.shader=if(gradientFill) BicycleAccentGradient.shader(rect,BicycleDesign.accentFillAngle,BicycleInputStyle.resolve(BicycleInputStyle.fill,palette),BicycleInputStyle.fill.map { it.position }.toFloatArray()) else null
        canvas.drawRoundRect(rect,radius,radius,paint)
        val r=raster()
        val bitmapBounds=RectF(rect.left-r.pad,rect.top-r.pad,rect.right+r.pad,rect.bottom+r.pad)
        inside.reset();inside.addRoundRect(rect,radius,radius,Path.Direction.CW)
        paint.style=Paint.Style.FILL;paint.shader=null;paint.blendMode=BlendMode.SCREEN
        val saved=canvas.save();canvas.clipOutPath(inside)
        paint.alpha=(alpha*BicycleDesign.inputFocusOuterAlpha).toInt();canvas.drawBitmap(r.outer,null,bitmapBounds,paint);canvas.restoreToCount(saved)
        paint.alpha=(alpha*BicycleDesign.inputFocusNearAlpha).toInt();canvas.drawBitmap(r.near,null,bitmapBounds,paint)
        paint.alpha=alpha;paint.blendMode=null;canvas.drawBitmap(r.source,null,bitmapBounds,paint)
    }
}

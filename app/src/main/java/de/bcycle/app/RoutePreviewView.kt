package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.View

/** A tiny cached drawing, independent of VTM/GL and of the dashboard map lifecycle. */
internal class RoutePreviewView(context:Context):View(context) {
    var points:List<MapPoint> = emptyList()
        set(value) {
            if(field==value) return
            cancel?.invoke();cancel=null;field=value.toList()
            geometry=RouteThumbnails.outline(field);render()
            if(isAttachedToWindow) request()
        }
    var foreground:Int=Color.WHITE
        set(value) { if(field!=value) { field=value;render() } }
    private var fill=Color.TRANSPARENT
    private var border=Color.TRANSPARENT
    private var geometry=RouteThumbnails.outline(emptyList())
    private var bitmap:Bitmap?=null
    private var cancel:(()->Unit)?=null
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val path=Path()
    fun colors(tokens:ThemeTokens) {
        if(foreground==tokens.foreground && fill==tokens.tertiary && border==BicycleComponents.sectionRuleColor(tokens)) return
        fill=tokens.tertiary;border=BicycleComponents.sectionRuleColor(tokens);foreground=tokens.foreground;render()
    }
    private fun request() {
        cancel?.invoke();val expected=points
        cancel=RouteThumbnails.request(context,expected) { next ->
            if(points==expected && isAttachedToWindow) { geometry=next;render() }
        }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();render();request() }
    override fun onDetachedFromWindow() { cancel?.invoke();cancel=null;bitmap?.recycle();bitmap=null;super.onDetachedFromWindow() }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { render() }
    private fun render() {
        if(width<=0 || height<=0) return
        var image=bitmap
        if(image==null || image.width!=width || image.height!=height) {
            image?.recycle();image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);bitmap=image
        }
        image.eraseColor(Color.TRANSPARENT);val canvas=Canvas(image)
        val density=resources.displayMetrics.density;val inset=BicycleDesign.linePixels(context)/2f
        val radius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat()
        val rect=RectF(inset,inset,width-inset,height-inset)
        paint.style=Paint.Style.FILL;paint.color=fill;canvas.drawRoundRect(rect,radius,radius,paint)
        path.reset();path.addRoundRect(rect,radius,radius,Path.Direction.CW)
        val save=canvas.save();canvas.clipPath(path)
        fun line(points:FloatArray,starts:BooleanArray?=null) {
            path.reset()
            for(i in points.indices step 2) {
                val x=points[i]*width;val y=points[i+1]*height
                if(i==0 || starts?.get(i/2)==true) path.moveTo(x,y) else path.lineTo(x,y)
            }
            canvas.drawPath(path,paint)
        }
        paint.style=Paint.Style.STROKE;paint.strokeCap=Paint.Cap.ROUND;paint.strokeJoin=Paint.Join.ROUND
        paint.color=(foreground and 0x00ffffff) or (51 shl 24);paint.strokeWidth=.5f*density
        geometry.contours.forEach { line(it) }
        paint.color=foreground or Color.BLACK;paint.strokeWidth=2f*density;line(geometry.route,geometry.starts)
        canvas.restoreToCount(save)
        paint.color=border;paint.strokeWidth=BicycleDesign.linePixels(context).toFloat();canvas.drawRoundRect(rect,radius,radius,paint)
        invalidate()
    }
    override fun onDraw(canvas:Canvas) { super.onDraw(canvas);bitmap?.let { canvas.drawBitmap(it,0f,0f,null) } }
}

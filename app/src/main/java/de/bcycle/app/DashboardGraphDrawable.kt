package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.Drawable
import java.util.Locale

/** Cached display geometry below the tile children. No analysis or history reduction in draw(). */
internal class DashboardGraphDrawable(private val density: Float) : Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val plot=RectF()
    private val curve=RectF()
    private var boundaryShader: Shader?=null
    private data class Shape(val line: Path, val fill: Path, val chart: AnalysisChart, val range: GraphRange, val fillShader: Shader)
    private var shapes=emptyList<Shape>()
    private var selected=emptyList<AnalysisChart>()
    private var frame: GraphFrame?=null
    private var padding=intArrayOf(0,0,0,0)
    private var mode=GraphDisplayMode.WITH_DATA
    fun update(next: GraphFrame?, graphs: List<AnalysisChart>, insets: IntArray, display: GraphDisplayMode=GraphDisplayMode.WITH_DATA) {
        if(frame===next && selected==graphs && padding.contentEquals(insets) && mode==display) return
        frame=next; selected=graphs; padding=insets; mode=display; rebuild(); invalidateSelf()
    }
    override fun onBoundsChange(bounds: Rect) { rebuild() }
    internal fun plotBounds() = RectF(plot)
    internal val axisCount get() = shapes.size
    private fun rebuild() {
        val inner=RectF(bounds.left+padding[0].toFloat(),bounds.top+padding[1].toFloat(),bounds.right-padding[2].toFloat(),bounds.bottom-padding[3].toFloat())
        val free=inner.height()*(1-LiveGraphStyle.HEIGHT_FRACTION)/2
        plot.set(inner.left,inner.top+free,inner.right,inner.bottom-free)
        val f=frame
        if(f==null || plot.width()<=0 || plot.height()<=0) { shapes=emptyList(); return }
        val axis=minOf(26*density,plot.width()*.14f)
        val left=plot.left+axis; val right=plot.right-if(selected.size==2) axis else 0f
        curve.set(left,plot.top,right,plot.bottom)
        // Graph boundaries are structural lines, independent of secondary text opacity.
        val boundaryColor=Color.rgb(163,163,163) and 0x00ffffff
        boundaryShader=LinearGradient(0f,plot.top,0f,plot.bottom,
            intArrayOf(boundaryColor,boundaryColor or (140 shl 24),boundaryColor),
            floatArrayOf(0f,.5f,1f),Shader.TileMode.CLAMP)
        shapes=selected.mapNotNull { chart -> f.series[chart]?.let { series ->
            val line=Path(); val fill=Path(); val segment=Path(); var active=false; var startX=0f; var lastX=0f; var fillTop=plot.bottom
            fun finish() { if(active) { segment.lineTo(lastX,plot.bottom); segment.lineTo(startX,plot.bottom); segment.close(); fill.addPath(segment); segment.reset(); active=false } }
            for(point in series.points) {
                if(point.breaks and (1 shl chart.ordinal)!=0) finish()
                val v=point.values[chart.ordinal]
                if(v==null || point.distanceM==null) { finish(); continue }
                val x=if(f.distanceM>0) left+(point.distanceM/f.distanceM).toFloat()*(right-left) else left
                val y=plot.bottom-((v-series.range.low)/(series.range.high-series.range.low)).toFloat()*plot.height()
                fillTop=minOf(fillTop,y)
                if(!active) { line.moveTo(x,y); segment.moveTo(x,y); startX=x; active=true } else { line.lineTo(x,y); segment.lineTo(x,y) }
                lastX=x
            }
            finish()
            val color=chart.color(Color.BLACK) and 0x00ffffff
            val fillAlpha=LiveGraphStyle.fillAlpha(mode)
            val gradient=LinearGradient(0f,fillTop.coerceAtMost(plot.bottom-1f),0f,plot.bottom,
                intArrayOf(color or (fillAlpha shl 24),color or ((fillAlpha/3) shl 24),color),
                floatArrayOf(0f,.55f,1f),Shader.TileMode.CLAMP)
            Shape(line,fill,chart,series.range,gradient)
        } }
    }
    override fun draw(canvas: Canvas) {
        if(shapes.isEmpty()) return
        val save=canvas.save(); canvas.clipRect(plot)
        paint.typeface=Typeface.DEFAULT; paint.textSize=12.96f*density
        paint.style=Paint.Style.STROKE; paint.strokeWidth=density
        paint.color=Color.WHITE; paint.alpha=255; paint.shader=boundaryShader
        canvas.drawLine(curve.left,curve.top,curve.left,curve.bottom,paint)
        val boundaryRight=minOf(curve.right,plot.right-density/2)
        canvas.drawLine(boundaryRight,curve.top,boundaryRight,curve.bottom,paint)
        paint.shader=null
        shapes.forEachIndexed { index,s ->
            val color=s.chart.color(Color.BLACK)
            val graphSave=canvas.save(); canvas.clipRect(curve)
            paint.color=color; paint.alpha=255; paint.style=Paint.Style.FILL; paint.shader=s.fillShader
            canvas.drawPath(s.fill,paint)
            paint.shader=null; paint.style=Paint.Style.STROKE
            paint.strokeCap=Paint.Cap.ROUND; paint.strokeJoin=Paint.Join.ROUND
            // Cached paths and a pair of soft strokes: no blur filter or offscreen bitmap.
            paint.alpha=24; paint.strokeWidth=LiveGraphStyle.OUTER_GLOW_WIDTH_DP*density
            canvas.drawPath(s.line,paint)
            paint.alpha=60; paint.strokeWidth=LiveGraphStyle.INNER_GLOW_WIDTH_DP*density
            canvas.drawPath(s.line,paint)
            paint.alpha=LiveGraphStyle.lineAlpha(mode); paint.strokeWidth=LiveGraphStyle.LINE_WIDTH_DP*density
            paint.strokeCap=Paint.Cap.ROUND; paint.strokeJoin=Paint.Join.ROUND
            canvas.drawPath(s.line,paint)
            canvas.restoreToCount(graphSave)
            paint.style=Paint.Style.FILL; paint.color=BicycleDesign.dashboardUnitColor
            paint.textAlign=if(index==0) Paint.Align.LEFT else Paint.Align.RIGHT
            val x=if(index==0) plot.left+2*density else plot.right-2*density
            val top=plot.top+paint.textSize; val bottom=plot.bottom-2*density
            canvas.drawText(String.format(Locale.getDefault(),"%.0f",s.range.high),x,top,paint)
            canvas.drawText(String.format(Locale.getDefault(),"%.0f",s.range.low),x,bottom,paint)
            paint.color=color; paint.alpha=LiveGraphStyle.LINE_ALPHA
            val markerX=if(index==0) plot.left else plot.right-2*density
            canvas.drawRect(markerX,top+2*density,markerX+2*density,top+8*density,paint)
        }
        canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    @Deprecated("Drawable opacity") override fun getOpacity() = PixelFormat.TRANSLUCENT
}

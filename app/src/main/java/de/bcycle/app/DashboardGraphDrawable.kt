package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.Drawable
import java.util.Locale

/** Cached display geometry below the tile children. No analysis or history reduction in draw(). */
internal class DashboardGraphDrawable(private val density: Float) : Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val plot=RectF()
    private data class Shape(val line: Path, val fill: Path, val chart: AnalysisChart, val range: GraphRange)
    private var shapes=emptyList<Shape>()
    private var selected=emptyList<AnalysisChart>()
    private var frame: GraphFrame?=null
    private var padding=intArrayOf(0,0,0,0)
    fun update(next: GraphFrame?, graphs: List<AnalysisChart>, insets: IntArray) {
        if(frame===next && selected==graphs && padding.contentEquals(insets)) return
        frame=next; selected=graphs; padding=insets; rebuild(); invalidateSelf()
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
        shapes=selected.mapNotNull { chart -> f.series[chart]?.let { series ->
            val line=Path(); val fill=Path(); val segment=Path(); var active=false; var startX=0f; var lastX=0f
            fun finish() { if(active) { segment.lineTo(lastX,plot.bottom); segment.lineTo(startX,plot.bottom); segment.close(); fill.addPath(segment); segment.reset(); active=false } }
            for(point in series.points) {
                if(point.breaks and (1 shl chart.ordinal)!=0) finish()
                val v=point.values[chart.ordinal]
                if(v==null || point.distanceM==null) { finish(); continue }
                val x=if(f.distanceM>0) left+(point.distanceM/f.distanceM).toFloat()*(right-left) else left
                val y=plot.bottom-((v-series.range.low)/(series.range.high-series.range.low)).toFloat()*plot.height()
                if(!active) { line.moveTo(x,y); segment.moveTo(x,y); startX=x; active=true } else { line.lineTo(x,y); segment.lineTo(x,y) }
                lastX=x
            }
            finish(); Shape(line,fill,chart,series.range)
        } }
    }
    override fun draw(canvas: Canvas) {
        if(shapes.isEmpty()) return
        val save=canvas.save(); canvas.clipRect(plot)
        paint.typeface=Typeface.DEFAULT; paint.textSize=9*density
        shapes.forEachIndexed { index,s ->
            val color=s.chart.color(Color.BLACK)
            paint.color=color; paint.alpha=LiveGraphStyle.FILL_ALPHA; paint.style=Paint.Style.FILL
            canvas.drawPath(s.fill,paint)
            paint.alpha=LiveGraphStyle.LINE_ALPHA; paint.style=Paint.Style.STROKE; paint.strokeWidth=1.3f*density
            paint.strokeCap=Paint.Cap.ROUND; paint.strokeJoin=Paint.Join.ROUND
            canvas.drawPath(s.line,paint)
            paint.style=Paint.Style.FILL; paint.color=BicycleDesign.dashboardUnitColor; paint.alpha=125
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

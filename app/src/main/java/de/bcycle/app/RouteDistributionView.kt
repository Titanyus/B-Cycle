package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.View

/** Zone-coloured time histogram. Worker-built bins and size-cached geometry; no draw-time analysis. */
internal class RouteDistributionView(context: Context, private val themes: ThemeManager, val distribution: RouteValueDistribution) : View(context) {
    private val density = resources.displayMetrics.density
    private val scaled = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 1f, resources.displayMetrics)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = BicycleDesign.strongTypeface; textAlign = Paint.Align.CENTER }
    private val plot = RectF()
    private data class Bar(val bounds:RectF)
    private var bars:List<Bar> = emptyList()
    private var barFill:Shader?=null
    private val separators=distribution.bands.flatMap { listOf(it.lower,it.upper) }.distinct().filter { it>distribution.minimum && it<distribution.maximum }
    private val dash=DashPathEffect(floatArrayOf(2*density,3*density),0f)
    private var foreground = 0
    private val title = context.getString(distribution.chart.detailLabel)
    private val unit = context.getString(distribution.chart.unit)
    private val marks = (listOf(distribution.minimum) + (1..3).map {
        kotlin.math.round((distribution.minimum + (distribution.maximum - distribution.minimum) * it / 4) / 5) * 5
    }.filter { it > distribution.minimum && it < distribution.maximum } + distribution.maximum).distinct()
    private val labels = marks.map { context.getString(R.string.analysis_integer_format, it) }
    private val zoneLabels = distribution.bands.map { context.getString(R.string.analysis_distribution_zone_number, it.zone) }
    init {
        tag = "analysis_distribution_${distribution.chart.name}"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.analysis_distribution_description, title,
            context.getString(R.string.analysis_integer_format, distribution.minimum),
            context.getString(R.string.analysis_integer_format, distribution.maximum), unit)
        BicycleComponents.bindTheme(this, themes) { foreground=it.foreground;updateFill();invalidate() }
    }
    internal fun plotBounds() = RectF(plot)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(width, heightMeasureSpec))
    }
    private fun x(value: Double) = plot.left + ((value - distribution.minimum) / (distribution.maximum - distribution.minimum)).toFloat() * plot.width()
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        plot.set((BicycleDesign.analysisSideLabelWidth + BicycleDesign.verticalPadding) * density,
            BicycleDesign.analysisDistributionHeaderHeight*density,w-12*density,h-BicycleDesign.analysisDistributionAxisHeight*density)
        if (plot.width() <= 0 || plot.height() <= 0) { bars=emptyList();return }
        val peak=(distribution.bars.maxOfOrNull { it.milliseconds } ?: 0.0).coerceAtLeast(1.0)
        bars=distribution.bars.filter { it.milliseconds>0 }.map { bar ->
            val left=x(bar.lower);val right=x(bar.upper)
            val inset=(right-left)*BicycleDesign.analysisDistributionBarGap/2
            Bar(RectF(left+inset,plot.bottom-(bar.milliseconds/peak).toFloat()*plot.height()*BicycleDesign.analysisDistributionPeakFraction,
                right-inset,plot.bottom))
        }
        updateFill()
    }
    /** Hard stops at the exact zone limits: a boundary bar retains its width and changes colour
     * precisely at the separator, with no blended or invented zone colour. */
    private fun updateFill() {
        if(plot.width()<=0) return
        val colors=mutableListOf<Int>();val positions=mutableListOf<Float>()
        val neutral=(foreground and 0xffffff) or 0x80000000.toInt()
        var previous=distribution.minimum
        fun range(from:Double,to:Double,color:Int) {
            if(to<=from) return
            colors+=color;positions+=((from-distribution.minimum)/(distribution.maximum-distribution.minimum)).toFloat()
            colors+=color;positions+=((to-distribution.minimum)/(distribution.maximum-distribution.minimum)).toFloat()
        }
        distribution.bands.forEach { band ->
            val from=band.lower.coerceIn(distribution.minimum,distribution.maximum)
            val to=band.upper.coerceIn(distribution.minimum,distribution.maximum)
            range(previous,from,neutral);range(from,to,band.color);previous=maxOf(previous,to)
        }
        range(previous,distribution.maximum,neutral)
        if(colors.size>=2) barFill=LinearGradient(plot.left,0f,plot.right,0f,colors.toIntArray(),positions.toFloatArray(),Shader.TileMode.CLAMP)
    }
    override fun onDraw(canvas: Canvas) {
        if (plot.width() <= 0 || plot.height() <= 0) return
        val saved = canvas.save(); canvas.clipRect(plot)
        paint.shader=null;paint.style=Paint.Style.STROKE;paint.color=foreground
        paint.alpha=(255*BicycleDesign.analysisDistributionRuleAlpha).toInt();paint.strokeWidth=density*.5f;paint.pathEffect=dash
        separators.forEach { canvas.drawLine(x(it),plot.top,x(it),plot.bottom,paint) }
        paint.pathEffect=null;paint.style=Paint.Style.FILL
        paint.color=foreground;paint.alpha=255;paint.shader=barFill
        bars.forEach { bar ->
            val radius=minOf(bar.bounds.width()/2,2*density)
            canvas.drawRoundRect(bar.bounds,radius,radius,paint)
        }
        canvas.restoreToCount(saved)
        paint.shader=null;paint.pathEffect=null;paint.style=Paint.Style.STROKE;paint.color=foreground
        paint.alpha=128;paint.strokeWidth=density*.5f
        canvas.drawLine(plot.left,plot.bottom,plot.right,plot.bottom,paint)
        canvas.drawLine(plot.left,plot.top,plot.left,plot.bottom,paint)
        paint.style=Paint.Style.FILL;paint.alpha=255;paint.textSize=BicycleDesign.analysisLabelSp*scaled;paint.textAlign=Paint.Align.CENTER
        distribution.bands.forEachIndexed { i,band ->
            val left=x(band.lower);val right=x(band.upper)
            paint.color=foreground;paint.textSize=BicycleDesign.analysisLabelSp*scaled
            val available=(right-left)*.8f;val measured=paint.measureText(zoneLabels[i])
            if(measured>available) paint.textSize*=available/measured
            canvas.drawText(zoneLabels[i],(left+right)/2,plot.top-2*BicycleDesign.controlGap*density,paint)
            paint.color=band.color;paint.strokeWidth=BicycleDesign.analysisDistributionMarkerWidth*density;paint.strokeCap=Paint.Cap.ROUND
            val inset=minOf(BicycleDesign.smallGap*density,(right-left)*.15f)
            canvas.drawLine(left+inset,plot.top-BicycleDesign.controlGap*density,right-inset,plot.top-BicycleDesign.controlGap*density,paint)
        }
        paint.strokeCap=Paint.Cap.BUTT;paint.textSize=BicycleDesign.analysisLabelSp*scaled
        paint.color=themes.active.tokens.secondaryTextColor
        marks.forEachIndexed { i,value ->
            paint.textAlign=when(i) { 0->Paint.Align.LEFT;marks.lastIndex->Paint.Align.RIGHT;else->Paint.Align.CENTER }
            canvas.drawText(labels[i],x(value),plot.bottom+16*density,paint)
        }
        paint.textAlign=Paint.Align.RIGHT;canvas.drawText(unit,plot.right,height.toFloat()-density,paint)
        titlePaint.color=foreground;titlePaint.textSize=BicycleDesign.analysisSideLabelSp*scaled
        canvas.save();canvas.translate(BicycleDesign.analysisSideLabelWidth*density/2,height/2f);canvas.rotate(-90f)
        canvas.drawText(title,0f,-(titlePaint.ascent()+titlePaint.descent())/2,titlePaint);canvas.restore()
    }
}

package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.View
import android.widget.*
import java.text.NumberFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

/** Statistics content only; navigation and scrolling use the standard settings host. */
internal class RideStatisticsView(context:Context,private val themes:ThemeManager):LinearLayout(context) {
    private var shown:RideStatistics?=null
    private val selectedYears=linkedSetOf<Int>()
    private val monthly=LinearLayout(context).apply { orientation=VERTICAL }
    private val loading=BicycleFeedback.status(context,themes).apply { text=context.getString(R.string.statistics_loading) }
    private val palette get()=ZoneGlowColors.powerColors
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    init {
        orientation=VERTICAL;tag="statistics_screen"
        val pad=dp(BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        addView(loading)
    }
    fun show(data:RideStatistics) {
        if(shown===data) return
        shown=data;removeAllViews()
        if(data.rideCount==0) { addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_empty)));return }
        addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_scope)))
        heading(R.string.statistics_months)
        val years=(data.months.keys+data.year).sortedDescending()
        selectedYears.retainAll(years.toSet())
        if(selectedYears.isEmpty()) { selectedYears.add(if(data.year in data.months) data.year else data.months.keys.maxOrNull() ?: data.year) }
        val row=LinearLayout(context).apply { tag=BicycleDesign.inlineControlRowTag }
        years.forEach { year ->
            row.addView(BicycleSegmentButton(context,themes,multiple=true,standalone=true).apply {
                tag="statistics_year_$year";text=year.toString();isChecked=year in selectedYears
                setOnCheckedChangeListener { _,on -> if(on) selectedYears.add(year) else selectedYears.remove(year);showMonths(data,years) }
            },LayoutParams(-2,-2).apply { marginEnd=dp(BicycleDesign.controlGap) })
        }
        addView(HorizontalScrollView(context).apply { BicycleComponents.scrollStyle(this);addView(row) },LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap);bottomMargin=dp(BicycleDesign.controlGap) })
        (monthly.parent as? android.view.ViewGroup)?.removeView(monthly)
        addView(monthly);showMonths(data,years)
        divider();heading(R.string.statistics_weeks,data.year)
        addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_weeks_hint)))
        val labels=data.weeks.indices.map { context.getString(R.string.statistics_week,it+1) }
        addView(HorizontalScrollView(context).apply {
            tag="statistics_weeks_scroll";BicycleComponents.scrollStyle(this)
            addView(StatisticsDistanceChart(context,themes,listOf(data.weeks to themes.active.tokens.primary),labels,true),
                LayoutParams(dp(BicycleDesign.statisticsWeekWidth)*labels.size,dp(BicycleDesign.statisticsChartHeight)))
        },LayoutParams(-1,-2))
        divider();heading(R.string.statistics_bikes)
        addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_time_hint)))
        if(data.bikes.isEmpty()) addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_no_time)))
        else {
            addView(StatisticsPie(context,themes,data.bikes.map { bike -> bike.milliseconds to (bike.color ?: themes.active.tokens.secondaryTextColor) }),LayoutParams(-1,dp(BicycleDesign.statisticsPieHeight)))
            val total=data.bikes.sumOf { it.milliseconds }
            data.bikes.forEach { bike ->
                val color=bike.color ?: themes.active.tokens.secondaryTextColor
                val name=bike.name ?: context.getString(R.string.statistics_unassigned)
                val text=context.getString(R.string.statistics_bike_share,name,number(bike.milliseconds/3600000),number(bike.milliseconds*100/total))
                addView(BicycleComponents.text(context,themes,text).apply {
                    setPadding(0,dp(BicycleDesign.smallGap),0,dp(BicycleDesign.smallGap))
                    val icon=dot(color)
                    setCompoundDrawablesWithIntrinsicBounds(icon,null,null,null);compoundDrawablePadding=dp(BicycleDesign.controlGap)
                },LayoutParams(-1,-2))
            }
        }
    }
    private fun showMonths(data:RideStatistics,years:List<Int>) {
        monthly.removeAllViews()
        if(selectedYears.isEmpty()) { monthly.addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_choose_year)));return }
        val series=years.mapIndexedNotNull { i,year -> if(year in selectedYears) (data.months[year] ?: List(12) { 0.0 }) to palette[(i+1)%palette.size] else null }
        selectedYears.sortedDescending().forEach { year ->
            monthly.addView(BicycleComponents.text(context,themes,year.toString()).apply {
                setCompoundDrawablesWithIntrinsicBounds(dot(palette[(years.indexOf(year)+1)%palette.size]),null,null,null)
                compoundDrawablePadding=dp(BicycleDesign.controlGap)
            })
        }
        val labels=(1..12).map { Month.of(it).getDisplayName(TextStyle.SHORT,Locale.GERMANY).removeSuffix(".") }
        monthly.addView(StatisticsDistanceChart(context,themes,series,labels,false),LayoutParams(-1,dp(BicycleDesign.statisticsChartHeight)))
    }
    private fun heading(resource:Int,vararg args:Any) {
        addView(BicycleComponents.contentHeading(context,themes,context.getString(resource,*args)),LayoutParams(-1,-2).apply {
            topMargin=dp(BicycleDesign.sectionContentGap);bottomMargin=dp(BicycleDesign.controlGap)
        })
    }
    private fun divider() { addView(BicycleComponents.divider(context,themes),LayoutParams(-1,BicycleDesign.linePixels(context))) }
    private fun dot(color:Int)=android.graphics.drawable.GradientDrawable().apply { shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(color);setSize(dp(BicycleDesign.analysisLabelSp.toInt()),dp(BicycleDesign.analysisLabelSp.toInt())) }
    private fun number(value:Double)=NumberFormat.getNumberInstance().apply { maximumFractionDigits=1 }.format(value)
}

private class StatisticsDistanceChart(context:Context,private val themes:ThemeManager,private val series:List<Pair<List<Double>,Int>>,private val labels:List<String>,private val columns:Boolean):View(context) {
    private val density=resources.displayMetrics.density
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val paths=mutableListOf<Pair<Path,Int>>()
    private val dots=mutableListOf<Pair<List<PointF>,Int>>()
    private val rect=RectF()
    private val maximum:Double
    private val number=NumberFormat.getNumberInstance().apply { maximumFractionDigits=0 }
    private val scaleLabels:List<String>
    init {
        val peak=series.flatMap { it.first }.maxOrNull() ?: 0.0
        val unit=when { peak>1000->500.0;peak>200->100.0;peak>50->25.0;else->5.0 }
        maximum=(ceil(peak/unit)*unit).coerceAtLeast(unit)
        scaleLabels=(0..4).map { context.getString(R.string.statistics_km,number.format(maximum*it/4)) }
        contentDescription=labels.indices.joinToString("; ") { i -> labels[i]+": "+series.joinToString(" / ") { context.getString(R.string.statistics_km,number.format(it.first[i])) } }
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
        BicycleComponents.bindTheme(this,themes) { invalidate() }
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        val desiredWidth=if(columns) BicycleDesign.dp(context,BicycleDesign.statisticsWeekWidth)*labels.size else MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(resolveSize(desiredWidth,widthMeasureSpec),resolveSize(BicycleDesign.dp(context,BicycleDesign.statisticsChartHeight),heightMeasureSpec))
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        rect.set(if(columns) 8*density else 48*density,12*density,w-8*density,h-28*density)
        paths.clear();dots.clear()
        series.forEach { (values,color) ->
            val points=values.mapIndexed { i,value -> PointF(x(i),rect.bottom-(value/maximum).toFloat()*rect.height()) }
            paths.add(Path().also { MonotoneGraphPath.append(it,points) } to color);dots.add(points to color)
        }
    }
    private fun x(index:Int)=if(columns) rect.left+rect.width()*(index+.5f)/labels.size else rect.left+rect.width()*index/(labels.size-1).coerceAtLeast(1)
    override fun onDraw(canvas:Canvas) {
        paint.typeface=BicycleDesign.bodyTypeface;paint.textSize=BicycleDesign.analysisLabelSp*resources.displayMetrics.scaledDensity
        paint.shader=null;paint.style=Paint.Style.STROKE;paint.strokeWidth=density*.5f;paint.color=themes.active.tokens.foreground;paint.alpha=60
        for(i in 0..4) { val y=rect.bottom-rect.height()*i/4;canvas.drawLine(rect.left,y,rect.right,y,paint) }
        paint.alpha=255
        for((index,pair) in paths.withIndex()) {
            paint.color=pair.second;paint.strokeWidth=2*density
            if(columns) dots[index].first.forEach { point -> paint.style=Paint.Style.FILL;canvas.drawRect(point.x-8*density,point.y,point.x+8*density,rect.bottom,paint) }
            else { paint.style=Paint.Style.STROKE;canvas.drawPath(pair.first,paint);paint.style=Paint.Style.FILL;dots[index].first.forEach { canvas.drawCircle(it.x,it.y,2*density,paint) } }
        }
        paint.style=Paint.Style.FILL;paint.color=themes.active.tokens.secondaryTextColor;paint.textAlign=Paint.Align.CENTER
        labels.forEachIndexed { i,label -> canvas.drawText(label,x(i),height-7*density,paint) }
        if(!columns) { paint.textAlign=Paint.Align.RIGHT;scaleLabels.forEachIndexed { i,label -> canvas.drawText(label,rect.left-4*density,rect.bottom-rect.height()*i/4,paint) } }
        else {
            paint.textSize=BicycleDesign.statisticsWeekValueSp*resources.displayMetrics.scaledDensity
            dots.firstOrNull()?.first?.forEachIndexed { i,p -> if(series.first().first[i]>0) canvas.drawText(number.format(series.first().first[i]),p.x,(p.y-4*density).coerceAtLeast(paint.textSize),paint) }
        }
    }
}

private class StatisticsPie(context:Context,themes:ThemeManager,private val slices:List<Pair<Double,Int>>):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect=RectF()
    private val angles=slices.map { (it.first/slices.sumOf { slice -> slice.first }*360).toFloat() }
    init { tag="statistics_bike_pie";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO;BicycleComponents.bindTheme(this,themes) { invalidate() } }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { val size=minOf(w,h)*.9f;rect.set((w-size)/2,(h-size)/2,(w+size)/2,(h+size)/2) }
    override fun onDraw(canvas:Canvas) {
        var start=-90f
        slices.forEachIndexed { i,slice -> paint.color=slice.second;canvas.drawArc(rect,start,angles[i],true,paint);start+=angles[i] }
    }
}

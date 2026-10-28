package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.NumberFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** One shared year selection for distance and bike-time statistics. */
internal class RideStatisticsView(context:Context,private val themes:ThemeManager):LinearLayout(context) {
    private var shown:RideStatistics?=null
    private val selectedYears=linkedSetOf<Int>()
    private var initialized=false
    private var weekly=false
    private var years:List<Int> = emptyList()
    private val chart=StatisticsDistanceChart(context,themes)
    private val pie=LinearLayout(context).apply { orientation=VERTICAL;tag="statistics_bikes_content" }
    private val caption=BicycleComponents.contentHeading(context,themes,"")
    private val hint=BicycleComponents.description(context,themes,context.getString(R.string.statistics_weeks_hint))
    private val empty=BicycleComponents.description(context,themes,context.getString(R.string.statistics_choose_year)).apply { tag="statistics_choose_year" }
    private val palette get()=ZoneGlowColors.powerColors
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    init {
        orientation=VERTICAL;tag="statistics_screen"
        val pad=dp(BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        addView(BicycleFeedback.status(context,themes).apply { text=context.getString(R.string.statistics_loading) })
    }
    fun show(data:RideStatistics) {
        if(shown===data) return
        shown=data;removeAllViews()
        if(data.rideCount==0) { addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_empty)));return }
        years=(data.months.keys+data.weeks.keys+data.bikes.keys+data.year).sortedDescending()
        val hadSelection=selectedYears.isNotEmpty();selectedYears.retainAll(years.toSet())
        if(!initialized || hadSelection && selectedYears.isEmpty()) selectedYears.add(if(data.year in data.months) data.year else data.months.keys.maxOrNull() ?: data.year)
        initialized=true
        val row=LinearLayout(context).apply { orientation=HORIZONTAL;gravity=Gravity.CENTER_HORIZONTAL;tag=BicycleDesign.inlineControlRowTag;isBaselineAligned=false }
        years.forEach { year ->
            row.addView(BicycleSegmentButton(context,themes,multiple=true,standalone=true).apply {
                tag="statistics_year_$year";text=year.toString();isChecked=year in selectedYears
                setCompoundDrawablesRelative(StatisticsYearDot(color(year)).apply { setBounds(0,0,dp(BicycleDesign.analysisLabelSp.toInt()),dp(BicycleDesign.analysisLabelSp.toInt())) },null,null,null)
                compoundDrawablePadding=dp(BicycleDesign.controlGap)
                setOnCheckedChangeListener { _,on -> if(on) selectedYears.add(year) else selectedYears.remove(year);refresh() }
            },LayoutParams(-2,-2).apply { marginStart=dp(BicycleDesign.controlGap/2);marginEnd=dp(BicycleDesign.controlGap/2) })
        }
        addView(HorizontalScrollView(context).apply {
            tag="statistics_years";BicycleComponents.scrollStyle(this);isFillViewport=true;isHorizontalScrollBarEnabled=false
            addView(row,FrameLayout.LayoutParams(-2,-2))
        },LayoutParams(-1,-2))
        addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_scope)))
        val periods=RadioGroup(context).apply { orientation=HORIZONTAL;tag="statistics_periods" }
        BicycleComponents.segmentedControlRow(periods,themes)
        for(isWeek in listOf(false,true)) periods.addView(BicycleSegmentButton(context,themes).apply {
            id=generateViewId();tag=if(isWeek) "statistics_period_week" else "statistics_period_month"
            text=context.getString(if(isWeek) R.string.hrvt_week else R.string.hrvt_month);isChecked=isWeek==weekly
            setOnCheckedChangeListener { _,checked -> if(checked && weekly!=isWeek) { weekly=isWeek;refresh() } }
        },RadioGroup.LayoutParams(0,-2,1f))
        addView(periods,LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
        attach(caption,LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.sectionContentGap);bottomMargin=dp(BicycleDesign.controlGap) })
        attach(empty,LayoutParams(-1,-2));attach(chart,LayoutParams(-1,dp(BicycleDesign.statisticsChartHeight)))
        attach(hint,LayoutParams(-1,-2))
        addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.statistics_bikes)),LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.sectionContentGap) })
        addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_time_hint)))
        attach(pie,LayoutParams(-1,-2));refresh()
    }
    private fun attach(view:View,params:LayoutParams) { (view.parent as? android.view.ViewGroup)?.removeView(view);addView(view,params) }
    private fun color(year:Int)=palette[(years.indexOf(year)+1)%palette.size]
    private fun refresh() {
        val data=shown ?: return
        caption.text=context.getString(if(weekly) R.string.statistics_weeks else R.string.statistics_months)
        hint.visibility=if(weekly && selectedYears.isNotEmpty()) VISIBLE else GONE
        chart.visibility=if(selectedYears.isEmpty()) GONE else VISIBLE;empty.visibility=if(selectedYears.isEmpty()) VISIBLE else GONE
        if(selectedYears.isEmpty()) chart.pause()
        if(selectedYears.isNotEmpty()) {
            val selected=years.filter { it in selectedYears }
            val labels=if(weekly) (1..selected.maxOf { RideStatisticsCache.weekCount(it) }).map(Int::toString)
                else (1..12).map { Month.of(it).getDisplayName(TextStyle.SHORT,Locale.GERMANY).removeSuffix(".") }
            val series=selected.map { year -> StatisticsYearSeries(year,color(year),if(weekly) data.weeks[year] ?: List(RideStatisticsCache.weekCount(year)) { 0.0 }
                else data.months[year] ?: List(12) { 0.0 }) }
            chart.show(series,labels,weekly)
        }
        pie.removeAllViews()
        if(selectedYears.isEmpty()) return
        val bikes=RideStatisticsCache.selectedBikes(data,selectedYears)
        if(bikes.isEmpty()) { pie.addView(BicycleComponents.description(context,themes,context.getString(R.string.statistics_no_time)));return }
        pie.addView(StatisticsPie(context,themes,bikes.map { it.milliseconds to (it.color ?: themes.active.tokens.secondaryTextColor) }),LayoutParams(-1,dp(BicycleDesign.statisticsPieHeight)))
        val total=bikes.sumOf { it.milliseconds }
        bikes.forEach { bike ->
            val color=bike.color ?: themes.active.tokens.secondaryTextColor
            val text=context.getString(R.string.statistics_bike_share,bike.name ?: context.getString(R.string.statistics_unassigned),number(bike.milliseconds/3600000),number(bike.milliseconds*100/total))
            pie.addView(BicycleComponents.text(context,themes,text).apply {
                setPadding(0,dp(BicycleDesign.smallGap),0,dp(BicycleDesign.smallGap))
                setCompoundDrawablesRelative(StatisticsYearDot(color).apply { setBounds(0,0,dp(BicycleDesign.analysisLabelSp.toInt()),dp(BicycleDesign.analysisLabelSp.toInt())) },null,null,null)
                compoundDrawablePadding=dp(BicycleDesign.controlGap)
            },LayoutParams(-1,-2))
        }
    }
    private fun number(value:Double)=NumberFormat.getNumberInstance().apply { maximumFractionDigits=1 }.format(value)
}

/** Decorative data colour must survive native button tinting in checked and unchecked states. */
internal class StatisticsYearDot(color:Int):Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color=color }
    override fun draw(canvas:Canvas) { canvas.drawCircle(bounds.exactCenterX(),bounds.exactCenterY(),minOf(bounds.width(),bounds.height())/2f,paint) }
    override fun setAlpha(alpha:Int) { paint.alpha=alpha }
    override fun setTint(color:Int)=Unit
    override fun setColorFilter(filter:ColorFilter?)=Unit
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

private class StatisticsPie(context:Context,themes:ThemeManager,private val slices:List<Pair<Double,Int>>):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect=RectF()
    private val angles=slices.map { (it.first/slices.sumOf { slice -> slice.first }*360).toFloat() }
    init { tag="statistics_bike_pie";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO;BicycleComponents.bindTheme(this,themes) { invalidate() } }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { val size=minOf(w,h)*.9f;rect.set((w-size)/2,(h-size)/2,(w+size)/2,(h+size)/2) }
    override fun onDraw(canvas:Canvas) { var start=-90f;slices.forEachIndexed { i,slice -> paint.color=slice.second;canvas.drawArc(rect,start,angles[i],true,paint);start+=angles[i] } }
}

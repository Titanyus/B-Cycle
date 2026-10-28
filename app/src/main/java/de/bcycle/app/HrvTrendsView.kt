package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RadioGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

internal class HrvTrendsView(context:Context,private val themes:ThemeManager,series:Map<HrvTrendMetric,List<HrvTrendPoint>>,
    actionFooter:LinearLayout?=null,delete:(String)->Unit={},result:(String)->Unit={},assess:(String)->Unit={},dayContext:Map<LocalDate,Map<HrvContextMode,Double>> = emptyMap(),open:(String)->Unit):LinearLayout(context) {
    var onDaySelected:(LocalDate)->Unit={}
    var onPeriodSelected:(String)->Unit={}
    private var series=series
    private var actionDay:LocalDate?=null
    private var contextData=dayContext
    private var colorMode=HrvContextMode.STANDARD
    private val contextGroups=mutableMapOf<HrvTrendPeriod,Map<LocalDate,Map<HrvContextMode,Double>>>()
    private val colorCache=mutableMapOf<Pair<HrvTrendPeriod,HrvContextMode>,Map<LocalDate,Int>>()
    private val colorName=BicycleComponents.description(context,themes,context.getString(colorMode.label)).apply { tag="hrv_context_name";gravity=Gravity.CENTER }
    private val footer=actionFooter ?: bicycleFooter(context,themes)
    private val grouped=mutableMapOf<Pair<HrvTrendPeriod,HrvTrendMetric>,List<HrvTrendPoint>>()
    private val axes=mutableMapOf<Pair<HrvTrendPeriod,HrvTrendMetric>,HrvTrendAxis>()
    private val metricButtons=linkedMapOf<HrvTrendMetric,BicycleSegmentButton>()
    private var metric=HrvTrendMetric.RMSSD
    private var resolution=HrvTrendPeriod.DAY
    private var selected:HrvTrendPoint?=null
    private val dateFormat=DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val caption=BicycleComponents.contentHeading(context,themes,"")
    private val range=BicycleComponents.description(context,themes,"").apply { gravity=Gravity.CENTER;tag="hrv_trend_range" }
    private val empty=BicycleComponents.description(context,themes,context.getString(R.string.hrvt_empty)).apply { tag="hrv_trend_empty" }
    private val analysis=BicycleFooterAction(context,themes,BicycleIcon.Distance,context.getString(R.string.hrvt_day_analysis)) {
        selected?.takeIf { resolution==HrvTrendPeriod.DAY && actionDay==it.day }?.let { assess(it.row.id) }
    }.apply {
        tag="hrv_day_analysis";isEnabled=false
    }
    private val measurement=BicycleFooterAction(context,themes,BicycleIcon.Heart,context.getString(R.string.hrv_result)) {
        selected?.takeIf { resolution==HrvTrendPeriod.DAY && actionDay==it.day }?.let { result(it.row.id) }
    }.apply { tag="hrv_day_result";isEnabled=false }
    private val questionnaire=BicycleFooterAction(context,themes,BicycleIcon.RenameTrack,context.getString(R.string.hrvq_title)) {
        selected?.takeIf { resolution==HrvTrendPeriod.DAY && actionDay==it.day }?.let { open(it.row.id) }
    }.apply { tag="hrv_day_questionnaire";isEnabled=false }
    private val remove=BicycleFooterAction(context,themes,BicycleIcon.DeleteTrack,context.getString(R.string.route_action_delete)) {
        selected?.takeIf { resolution==HrvTrendPeriod.DAY && actionDay==it.day }?.let { delete(it.row.id) }
    }.apply { tag="hrv_day_delete";isEnabled=false }
    private val chart=HrvTrendChart(context,themes,::select) { first,last ->
        range.text=if(resolution==HrvTrendPeriod.DAY) context.getString(R.string.hrvt_period,first.format(dateFormat),last.format(dateFormat))
            else context.getString(R.string.hrvt_aggregate_range,title(first),title(last),first.format(dateFormat),last.format(dateFormat))
    }.apply { onDayTapped={ point ->
        this@HrvTrendsView.actionDay=point.day;this.actionDay=point.day;analysis.isEnabled=true;questionnaire.isEnabled=true;measurement.isEnabled=true;remove.isEnabled=true;footer.visibility=VISIBLE
    };onDaySelected={ day ->
        if(resolution==HrvTrendPeriod.DAY) this@HrvTrendsView.onDaySelected(day)
        else this@HrvTrendsView.onPeriodSelected(title(day))
    } }
    init {
        orientation=VERTICAL;tag="hrv_trends";clipToPadding=false
        footer.visibility=GONE
        listOf(analysis,measurement,questionnaire,remove).forEach { footer.addView(it,LayoutParams(0,-2,1f)) }
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        val periods=RadioGroup(context).apply { orientation=HORIZONTAL;tag="hrv_trend_periods" }
        BicycleComponents.segmentedControlRow(periods,themes)
        HrvTrendPeriod.entries.forEach { candidate ->
            periods.addView(BicycleSegmentButton(context,themes).apply {
                id=generateViewId();tag="hrv_period_${candidate.name}";text=context.getString(candidate.label);isChecked=candidate==resolution
                setOnCheckedChangeListener { _,checked -> if(checked && resolution!=candidate) {
                    val anchor=selected?.let { HrvDailySelection.day(it.row) }
                    actionDay=null;chart.actionDay=null;footer.visibility=GONE;analysis.isEnabled=false;questionnaire.isEnabled=false;measurement.isEnabled=false;remove.isEnabled=false
                    resolution=candidate;refresh(true,anchor)
                    selected?.let { if(candidate==HrvTrendPeriod.DAY) onDaySelected(it.day) else onPeriodSelected(title(it.day)) }
                } }
            },RadioGroup.LayoutParams(0,-2,1f))
        }
        addView(periods,LayoutParams(-1,-2))
        val choices=RadioGroup(context).apply { orientation=HORIZONTAL;tag="hrv_trend_metrics" }
        BicycleComponents.segmentedControlRow(choices,themes)
        HrvTrendMetric.entries.forEach { candidate ->
            val choice=BicycleSegmentButton(context,themes).apply {
                id=generateViewId();tag="hrv_trend_${candidate.name}"
                textSize=BicycleDesign.analysisLabelSp;isChecked=candidate==metric
                setOnCheckedChangeListener { _,checked -> if(checked) { metric=candidate;refresh() } }
            }
            metricButtons[candidate]=choice;choices.addView(choice,RadioGroup.LayoutParams(0,-2,1f))
        }
        addView(choices,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(caption,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(empty);addView(chart,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.hrvTrendGraphHeight)).apply { marginStart=-pad })
        addView(range)
        val colors=RadioGroup(context).apply { orientation=HORIZONTAL;tag="hrv_context_choices" }
        BicycleComponents.segmentedControlRow(colors,themes)
        HrvContextMode.entries.forEach { mode ->
            colors.addView(BicycleSegmentButton(context,themes).apply {
                id=generateViewId();tag="hrv_context_${mode.name}";contentDescription=context.getString(mode.label)
                iconOnly(mode.icon);isChecked=mode==colorMode
                setOnCheckedChangeListener { _,checked -> if(checked) { colorMode=mode;colorName.text=context.getString(mode.label);applyColors() } }
            },RadioGroup.LayoutParams(0,-2,1f))
        }
        addView(colors,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(colorName)
        if(actionFooter==null) addView(footer)
        refresh(true)
    }
    fun updateSeries(next:Map<HrvTrendMetric,List<HrvTrendPoint>>,dayContext:Map<LocalDate,Map<HrvContextMode,Double>> = contextData,preserveSelection:Boolean=false) {
        series=next;contextData=dayContext;grouped.clear();axes.clear();contextGroups.clear();colorCache.clear()
        if(!preserveSelection) { actionDay=null;chart.actionDay=null;footer.visibility=GONE;analysis.isEnabled=false;questionnaire.isEnabled=false;measurement.isEnabled=false;remove.isEnabled=false }
        refresh()
    }
    private fun applyColors() {
        val colors=if(colorMode==HrvContextMode.STANDARD) null else colorCache.getOrPut(resolution to colorMode) {
            contextGroups.getOrPut(resolution) { HrvContextColors.aggregate(contextData,resolution) }
                .mapValues { HrvContextColors.color(colorMode,it.value[colorMode]) }
        }
        chart.colorBars(colors)
    }
    private fun points(candidate:HrvTrendMetric)=grouped.getOrPut(resolution to candidate) { HrvTrendAggregation.series(series.getValue(candidate),resolution) }
    private fun number(value:Double)=String.format(java.util.Locale.getDefault(),"%.1f",value)
    private fun title(day:LocalDate)=when(resolution) {
        HrvTrendPeriod.DAY->day.format(dateFormat)
        HrvTrendPeriod.WEEK->context.getString(R.string.hrvt_week_title,day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),day.get(IsoFields.WEEK_BASED_YEAR))
        HrvTrendPeriod.MONTH->day.format(DateTimeFormatter.ofPattern("LLLL yyyy",java.util.Locale.getDefault()))
    }
    private fun select(point:HrvTrendPoint) {
        selected=point
        metricButtons.forEach { (candidate,choice) ->
            val value=points(candidate).firstOrNull { it.day==point.day }?.value
            choice.text=context.getString(candidate.label)+"\n"+(value?.let(::number) ?: context.getString(R.string.hrvt_unavailable))
            choice.contentDescription=context.getString(candidate.label)+" "+(value?.let { number(it)+" "+context.getString(candidate.unit) } ?: context.getString(R.string.hrvt_unavailable))
        }
    }
    private fun refresh(reset:Boolean=false,anchor:LocalDate?=null) {
        caption.text=context.getString(metric.label)+" · "+context.getString(metric.unit)
        val points=points(metric)
        val hasValue=points.any { it.value!=null }
        empty.visibility=if(hasValue) GONE else VISIBLE;chart.visibility=if(hasValue) VISIBLE else GONE
        if(points.isEmpty()) {
            selected=null;actionDay=null;chart.actionDay=null;footer.visibility=GONE
            metricButtons.forEach { (candidate,choice) -> choice.text=context.getString(candidate.label)+"\n"+context.getString(R.string.hrvt_unavailable) }
        }
        val axis=axes.getOrPut(resolution to metric) { HrvTrendAxis.from(points,metric) }
        chart.show(points,reset,axis,resolution,anchor)
        applyColors()
    }
}

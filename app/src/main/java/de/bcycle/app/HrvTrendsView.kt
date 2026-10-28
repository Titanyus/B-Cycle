package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RadioGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

internal class HrvTrendsView(context:Context,private val themes:ThemeManager,series:Map<HrvTrendMetric,List<HrvTrendPoint>>,
    open:(String)->Unit):LinearLayout(context) {
    var onDaySelected:(LocalDate)->Unit={}
    var onPeriodSelected:(String)->Unit={}
    private val series=series
    private val grouped=mutableMapOf<Pair<HrvTrendPeriod,HrvTrendMetric>,List<HrvTrendPoint>>()
    private val axes=HrvTrendMetric.entries.associateWith { HrvTrendAxis.from(series.getValue(it),it) }
    private val metricButtons=linkedMapOf<HrvTrendMetric,BicycleSegmentButton>()
    private var metric=HrvTrendMetric.RMSSD
    private var resolution=HrvTrendPeriod.DAY
    private var selected:HrvTrendPoint?=null
    private val dateFormat=DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val caption=BicycleComponents.contentHeading(context,themes,"")
    private val range=BicycleComponents.description(context,themes,"").apply { gravity=Gravity.CENTER;tag="hrv_trend_range" }
    private val detail=BicycleComponents.description(context,themes,"").apply { tag="hrv_trend_selected";accessibilityLiveRegion=ACCESSIBILITY_LIVE_REGION_POLITE }
    private val statistics=BicycleComponents.description(context,themes,"").apply { tag="hrv_trend_statistics" }
    private val empty=BicycleComponents.description(context,themes,context.getString(R.string.hrvt_empty)).apply { tag="hrv_trend_empty" }
    private val button=BicycleComponents.action(context,themes,context.getString(R.string.hrvt_detail)) { selected?.let { open(it.row.id) } }.apply { tag="hrv_trend_detail" }
    private val chart=HrvTrendChart(context,themes,::select) { first,last ->
        range.text=if(resolution==HrvTrendPeriod.DAY) context.getString(R.string.hrvt_period,first.format(dateFormat),last.format(dateFormat))
            else context.getString(R.string.hrvt_aggregate_range,title(first),title(last),first.format(dateFormat),last.format(dateFormat))
    }.apply { onDaySelected={ day ->
        if(resolution==HrvTrendPeriod.DAY) this@HrvTrendsView.onDaySelected(day)
        else this@HrvTrendsView.onPeriodSelected(title(day))
    } }
    init {
        orientation=VERTICAL;tag="hrv_trends";clipToPadding=false
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        val periods=RadioGroup(context).apply { orientation=HORIZONTAL;tag="hrv_trend_periods" }
        BicycleComponents.segmentedControlRow(periods,themes)
        HrvTrendPeriod.entries.forEach { candidate ->
            periods.addView(BicycleSegmentButton(context,themes).apply {
                id=generateViewId();tag="hrv_period_${candidate.name}";text=context.getString(candidate.label);isChecked=candidate==resolution
                setOnCheckedChangeListener { _,checked -> if(checked && resolution!=candidate) {
                    val anchor=selected?.let { HrvDailySelection.day(it.row) }
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
        addView(range);addView(BicycleComponents.description(context,themes,context.getString(R.string.hrvt_legend)))
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrvt_period_gestures)))
        addView(BicycleComponents.sectionDivider(context,themes))
        addView(detail);addView(statistics);addView(button)
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrvt_collecting)))
        refresh(true)
    }
    private fun points(candidate:HrvTrendMetric)=grouped.getOrPut(resolution to candidate) { HrvTrendAggregation.series(series.getValue(candidate),resolution) }
    private fun number(value:Double)=String.format(java.util.Locale.getDefault(),"%.1f",value)
    private fun title(day:LocalDate)=when(resolution) {
        HrvTrendPeriod.DAY->day.format(dateFormat)
        HrvTrendPeriod.WEEK->context.getString(R.string.hrvt_week_title,day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),day.get(IsoFields.WEEK_BASED_YEAR))
        HrvTrendPeriod.MONTH->day.format(DateTimeFormatter.ofPattern("LLLL yyyy",java.util.Locale.getDefault()))
    }
    private fun select(point:HrvTrendPoint) {
        selected=point;button.isEnabled=true
        metricButtons.forEach { (candidate,choice) ->
            val value=points(candidate).firstOrNull { it.day==point.day }?.value
            choice.text=context.getString(candidate.label)+"\n"+(value?.let(::number) ?: context.getString(R.string.hrvt_unavailable))
            choice.contentDescription=context.getString(candidate.label)+" "+(value?.let { number(it)+" "+context.getString(candidate.unit) } ?: context.getString(R.string.hrvt_unavailable))
        }
        val value=point.value?.let { context.getString(R.string.hrvt_value,context.getString(metric.label),number(it),context.getString(metric.unit)) }
            ?: context.getString(R.string.hrvt_unavailable)
        val lines=mutableListOf<String>()
        if(resolution==HrvTrendPeriod.DAY) {
            val origin=context.getString(when { !point.row.hasMeasurement->R.string.hrvt_context
                point.row.importedDay!=null->R.string.hrvt_imported;!point.row.acceptable->R.string.hrvt_rejected;else->R.string.hrvt_native })
            detail.text=context.getString(R.string.hrvt_selected,point.day.format(dateFormat),value,origin)
        } else {
            val today=LocalDate.now();val running=today in point.day..point.periodEnd
            val through=if(running) today else point.periodEnd
            val expected=(through.toEpochDay()-point.day.toEpochDay()+1).toInt()
            detail.text=context.getString(R.string.hrvt_aggregate_selected,title(point.day)+(if(running) context.getString(R.string.hrvt_so_far) else ""),
                point.day.format(dateFormat),through.format(dateFormat),value,point.sampleDays,expected)
            if(point.trendDays>0 || point.bandDays>0) lines+=context.getString(R.string.hrvt_reference_days,point.trendDays,point.bandDays)
        }
        point.average?.let { lines.add(0,context.getString(if(resolution==HrvTrendPeriod.DAY) R.string.hrvt_average else R.string.hrvt_aggregate_average,number(it))) }
        if(point.lower!=null && point.upper!=null) lines+=context.getString(R.string.hrvt_normal,number(point.lower),number(point.upper))
        statistics.text=lines.joinToString("\n");statistics.visibility=if(lines.isEmpty()) GONE else VISIBLE
    }
    private fun refresh(reset:Boolean=false,anchor:LocalDate?=null) {
        caption.text=context.getString(metric.label)+" · "+context.getString(metric.unit)
        val points=points(metric)
        val hasValue=points.any { it.value!=null }
        empty.visibility=if(hasValue) GONE else VISIBLE;chart.visibility=if(hasValue) VISIBLE else GONE
        button.visibility=if(resolution==HrvTrendPeriod.DAY) VISIBLE else GONE
        button.isEnabled=points.isNotEmpty()
        if(points.isEmpty()) {
            selected=null;detail.text="";statistics.text="";statistics.visibility=GONE
            metricButtons.forEach { (candidate,choice) -> choice.text=context.getString(candidate.label)+"\n"+context.getString(R.string.hrvt_unavailable) }
        }
        chart.show(points,reset,axes.getValue(metric),resolution,anchor)
    }
}

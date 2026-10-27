package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RadioGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal class HrvTrendsView(context:Context,private val themes:ThemeManager,series:Map<HrvTrendMetric,List<HrvTrendPoint>>,
    open:(String)->Unit):LinearLayout(context) {
    var onDaySelected:(LocalDate)->Unit={}
    private val series=series
    private val axes=HrvTrendMetric.entries.associateWith { HrvTrendAxis.from(series.getValue(it),it) }
    private val metricButtons=linkedMapOf<HrvTrendMetric,BicycleSegmentButton>()
    private var metric=HrvTrendMetric.RMSSD
    private var selected:HrvTrendPoint?=null
    private val dateFormat=DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val caption=BicycleComponents.contentHeading(context,themes,"")
    private val range=BicycleComponents.description(context,themes,"").apply { gravity=Gravity.CENTER }
    private val detail=BicycleComponents.description(context,themes,"").apply { tag="hrv_trend_selected";accessibilityLiveRegion=ACCESSIBILITY_LIVE_REGION_POLITE }
    private val statistics=BicycleComponents.description(context,themes,"")
    private val empty=BicycleComponents.description(context,themes,context.getString(R.string.hrvt_empty)).apply { tag="hrv_trend_empty" }
    private val button=BicycleComponents.action(context,themes,context.getString(R.string.hrvt_detail)) { selected?.let { open(it.row.id) } }.apply { tag="hrv_trend_detail" }
    private val chart=HrvTrendChart(context,themes,::select) { first,last -> range.text=context.getString(R.string.hrvt_period,first.format(dateFormat),last.format(dateFormat)) }.apply { onDaySelected={ day -> this@HrvTrendsView.onDaySelected(day) } }
    init {
        orientation=VERTICAL;tag="hrv_trends";clipToPadding=false
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        val choices=RadioGroup(context).apply { orientation=HORIZONTAL;tag="hrv_trend_metrics" }
        BicycleComponents.segmentedControlRow(choices,themes)
        HrvTrendMetric.entries.forEach { candidate ->
            val latest=series.getValue(candidate).lastOrNull()?.value
            val choice=BicycleSegmentButton(context,themes).apply {
                id=generateViewId();tag="hrv_trend_${candidate.name}"
                text=context.getString(candidate.label)+"\n"+(latest?.let(::number) ?: context.getString(R.string.hrvt_unavailable))
                textSize=BicycleDesign.analysisLabelSp;isChecked=candidate==metric
                contentDescription=context.getString(candidate.label)+" "+(latest?.let { number(it)+" "+context.getString(candidate.unit) } ?: context.getString(R.string.hrvt_unavailable))
                setOnCheckedChangeListener { _,checked -> if(checked) { metric=candidate;refresh() } }
            }
            metricButtons[candidate]=choice;choices.addView(choice,RadioGroup.LayoutParams(0,-2,1f))
        }
        addView(choices,LayoutParams(-1,-2))
        addView(caption,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(empty);addView(chart,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.hrvTrendGraphHeight)).apply { marginStart=-pad })
        addView(range);addView(BicycleComponents.description(context,themes,context.getString(R.string.hrvt_legend)))
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrvt_gestures)))
        addView(BicycleComponents.sectionDivider(context,themes))
        addView(detail);addView(statistics);addView(button)
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrvt_collecting)))
        refresh(true)
    }
    private fun number(value:Double)=String.format(java.util.Locale.getDefault(),"%.1f",value)
    private fun select(point:HrvTrendPoint) {
        selected=point;button.isEnabled=true
        metricButtons.forEach { (candidate,choice) ->
            val value=series.getValue(candidate).firstOrNull { it.row.id==point.row.id }?.value
            choice.text=context.getString(candidate.label)+"\n"+(value?.let(::number) ?: context.getString(R.string.hrvt_unavailable))
            choice.contentDescription=context.getString(candidate.label)+" "+(value?.let { number(it)+" "+context.getString(candidate.unit) } ?: context.getString(R.string.hrvt_unavailable))
        }
        val value=point.value?.let { context.getString(R.string.hrvt_value,context.getString(metric.label),number(it),context.getString(metric.unit)) }
            ?: context.getString(R.string.hrvt_unavailable)
        val origin=context.getString(when { !point.row.hasMeasurement->R.string.hrvt_context
            point.row.importedDay!=null->R.string.hrvt_imported;!point.row.acceptable->R.string.hrvt_rejected;else->R.string.hrvt_native })
        detail.text=context.getString(R.string.hrvt_selected,point.day.format(dateFormat),value,origin)
        statistics.text=listOfNotNull(point.average?.let { context.getString(R.string.hrvt_average,number(it)) },
            point.lower?.let { context.getString(R.string.hrvt_normal,number(it),number(checkNotNull(point.upper))) }).joinToString("\n")
        statistics.visibility=if(statistics.text.isEmpty()) GONE else VISIBLE
    }
    private fun refresh(reset:Boolean=false) {
        caption.text=context.getString(metric.label)+" · "+context.getString(metric.unit)
        val points=series.getValue(metric)
        empty.visibility=if(points.none { it.value!=null }) VISIBLE else GONE
        chart.visibility=if(points.none { it.value!=null }) GONE else VISIBLE
        button.isEnabled=points.isNotEmpty()
        chart.show(points,reset,axes.getValue(metric))
    }
}

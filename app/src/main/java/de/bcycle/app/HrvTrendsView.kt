package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RadioGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

internal class HrvTrendsView(context:Context,private val themes:ThemeManager,series:Map<HrvTrendMetric,List<HrvTrendPoint>>,
    actionFooter:LinearLayout?=null,delete:(String)->Unit={},result:(String)->Unit={},dayContext:Map<LocalDate,Map<HrvContextMode,Double>> = emptyMap(),initialDay:LocalDate?=null,
    override val footerVisibility:BicycleFooterVisibility=BicycleFooterVisibility(context),open:(String)->Unit):LinearLayout(context),BicycleFooterPage {
    var onDaySelected:(LocalDate)->Unit={}
    var onPeriodSelected:(String)->Unit={}
    private var series=series
    private var actionDay:LocalDate?=null
    private var activeDay:LocalDate?=initialDay
    private var contextData=dayContext
    private var colorMode=HrvContextMode.STANDARD
    private val contextGroups=mutableMapOf<HrvTrendPeriod,Map<LocalDate,Map<HrvContextMode,Double>>>()
    private val colorCache=mutableMapOf<Pair<HrvTrendPeriod,HrvContextMode>,Map<LocalDate,Int>>()
    private val colorName=BicycleComponents.description(context,themes,"").apply { tag="hrv_context_name";gravity=Gravity.CENTER;showHrvContextLegend(colorMode) }
    private val footer=actionFooter ?: bicycleFooter(context,themes)
    private val grouped=mutableMapOf<Pair<HrvTrendPeriod,HrvTrendMetric>,List<HrvTrendPoint>>()
    private val axes=mutableMapOf<Pair<HrvTrendPeriod,HrvTrendMetric>,HrvTrendAxis>()
    private val metricButtons=linkedMapOf<HrvTrendMetric,BicycleSegmentButton>()
    private val primaryMetrics=listOf(HrvTrendMetric.HRV,HrvTrendMetric.HR,HrvTrendMetric.SDNN,HrvTrendMetric.RMSSD)
    private val choices=RadioGroup(context).apply { orientation=HORIZONTAL;isBaselineAligned=false;tag="hrv_trend_metrics" }
    private val advancedChoices=RadioGroup(context).apply { orientation=HORIZONTAL;isBaselineAligned=false;tag="hrv_trend_advanced";visibility=GONE }
    private val moreArrow=android.graphics.drawable.RotateDrawable().apply { fromDegrees=0f;toDegrees=180f }
    private val more=BicycleSegmentButton(context,themes).apply {
        id=generateViewId();tag="hrv_trend_more";text=context.getString(R.string.hrvt_more)
        textSize=BicycleDesign.analysisLabelSp;toggleOnClick=false
        val inset=BicycleDesign.dp(context,BicycleDesign.choiceTextGap)/2;setPadding(inset,0,inset,0)
        compoundDrawablePadding=BicycleDesign.dp(context,2)
    }
    internal var metricPopup:android.widget.PopupWindow?=null
        private set
    private var metric=HrvTrendMetric.HRV
    private var resolution=HrvTrendPeriod.DAY
    private var selected:HrvTrendPoint?=null
    private val dateFormat=DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val caption=BicycleComponents.contentHeading(context,themes,"")
    private val range=BicycleComponents.description(context,themes,"").apply { gravity=Gravity.CENTER;tag="hrv_trend_range" }
    private val empty=BicycleComponents.description(context,themes,context.getString(R.string.hrvt_empty)).apply { tag="hrv_trend_empty" }
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
        activeDay=point.day;syncActions();footerVisibility.temporary(true)
    };onDaySelected={ day ->
        activeDay=if(resolution==HrvTrendPeriod.DAY) day else selected?.let { HrvDailySelection.day(it.row) } ?: day
        syncActions();this@HrvTrendsView.onDaySelected(checkNotNull(activeDay))
        if(resolution!=HrvTrendPeriod.DAY) this@HrvTrendsView.onPeriodSelected(title(day))
    } }
    init {
        orientation=VERTICAL;tag="hrv_trends";clipToPadding=false
        footerVisibility.bindFooter(footer)
        listOf(measurement,questionnaire,remove).forEach { footer.addView(it,LayoutParams(0,-2,1f)) }
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        val periods=RadioGroup(context).apply { orientation=HORIZONTAL;tag="hrv_trend_periods" }
        BicycleComponents.segmentedControlRow(periods,themes)
        HrvTrendPeriod.entries.forEach { candidate ->
            periods.addView(BicycleSegmentButton(context,themes).apply {
                id=generateViewId();tag="hrv_period_${candidate.name}";text=context.getString(candidate.label);isChecked=candidate==resolution
                setOnCheckedChangeListener { _,checked -> if(checked && resolution!=candidate) {
                    val anchor=activeDay
                    actionDay=null;chart.actionDay=null;footerVisibility.temporary(false);questionnaire.isEnabled=false;measurement.isEnabled=false;remove.isEnabled=false
                    resolution=candidate;refresh(true,anchor)
                    selected?.let { if(candidate==HrvTrendPeriod.DAY) onDaySelected(it.day) else onPeriodSelected(title(it.day)) }
                } }
            },RadioGroup.LayoutParams(0,-2,1f))
        }
        addView(periods,LayoutParams(-1,-2))
        BicycleComponents.segmentedControlRow(choices,themes)
        BicycleComponents.segmentedControlRow(advancedChoices,themes)
        fun addMetric(candidate:HrvTrendMetric,row:RadioGroup) {
            val choice=BicycleSegmentButton(context,themes).apply {
                id=generateViewId();tag="hrv_trend_${candidate.name}"
                textSize=BicycleDesign.analysisLabelSp;isChecked=candidate==metric
                // Let the native RadioGroup finish its checked-state callback before coordinating both rows.
                setOnClickListener { chooseMetric(candidate) }
            }
            metricButtons[candidate]=choice;row.addView(choice,RadioGroup.LayoutParams(0,-1,1f))
        }
        primaryMetrics.forEach { addMetric(it,choices) }
        choices.addView(more,RadioGroup.LayoutParams(0,-1,1f))
        listOf(HrvTrendMetric.PNN50,HrvTrendMetric.LF,HrvTrendMetric.HF).forEach { addMetric(it,advancedChoices) }
        more.setOnClickListener { if(metricPopup!=null) metricPopup?.dismiss() else showMetricPopup() }
        BicycleComponents.bindTheme(more,themes) {
            moreArrow.drawable=BicycleIcons.drawable(context,BicycleIcon.Expand,more.currentTextColor)
            val size=BicycleDesign.dp(context,10);moreArrow.setBounds(0,0,size,size)
            more.setCompoundDrawablesRelative(null,null,moreArrow,null)
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
                setOnCheckedChangeListener { _,checked -> if(checked) { colorMode=mode;colorName.showHrvContextLegend(mode);applyColors() } }
            },RadioGroup.LayoutParams(0,-2,1f))
        }
        addView(colors,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(colorName)
        if(actionFooter==null) addView(footer)
        refresh(true)
    }
    fun updateSeries(next:Map<HrvTrendMetric,List<HrvTrendPoint>>,dayContext:Map<LocalDate,Map<HrvContextMode,Double>> = contextData,preserveSelection:Boolean=false) {
        series=next;contextData=dayContext;grouped.clear();axes.clear();contextGroups.clear();colorCache.clear()
        if(!preserveSelection) { actionDay=null;chart.actionDay=null;footerVisibility.temporary(false);questionnaire.isEnabled=false;measurement.isEnabled=false;remove.isEnabled=false }
        refresh()
    }
    fun focusDay(day:LocalDate) { activeDay=day;refresh();chart.focusDay(day) }
    private fun chooseMetric(candidate:HrvTrendMetric) {
        metric=candidate
        choices.check(if(candidate in primaryMetrics) metricButtons.getValue(candidate).id else more.id)
        if(candidate in primaryMetrics) advancedChoices.clearCheck() else advancedChoices.check(metricButtons.getValue(candidate).id)
        metricPopup?.dismiss()
        refresh()
    }
    private fun showMetricPopup() {
        val width=(2..4).sumOf { choices.getChildAt(it).width }+advancedChoices.paddingLeft+advancedChoices.paddingRight
        if(width<=0) return
        advancedChoices.visibility=VISIBLE
        val popup=android.widget.PopupWindow(advancedChoices,width,android.view.ViewGroup.LayoutParams.WRAP_CONTENT,true).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            isOutsideTouchable=true;inputMethodMode=android.widget.PopupWindow.INPUT_METHOD_NOT_NEEDED
            setOnDismissListener {
                metricPopup=null;moreArrow.level=0
                more.stateDescription=context.getString(R.string.zone_collapsed)
                (advancedChoices.parent as? android.view.ViewGroup)?.removeView(advancedChoices)
                advancedChoices.visibility=GONE
            }
        }
        metricPopup=popup;moreArrow.level=10000;more.stateDescription=context.getString(R.string.zone_expanded)
        popup.showAsDropDown(choices,0,BicycleDesign.dp(context,BicycleDesign.controlGap),Gravity.END)
    }
    override fun onDetachedFromWindow() {
        metricPopup?.dismiss()
        super.onDetachedFromWindow()
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
        if(activeDay==null) activeDay=HrvDailySelection.day(point.row)
        metricButtons.forEach { (candidate,choice) ->
            val value=points(candidate).firstOrNull { it.day==point.day }?.value
            val formatted=value?.let(::number)
            choice.text=context.getString(candidate.label)+"\n"+(formatted ?: context.getString(R.string.hrvt_unavailable))
            choice.contentDescription=context.getString(candidate.label)+" "+(formatted?.let { it+" "+context.getString(candidate.unit) } ?: context.getString(R.string.hrvt_unavailable))
        }
        syncActions()
    }
    private fun syncActions() {
        val current=selected?.takeIf { resolution==HrvTrendPeriod.DAY && it.day==activeDay }
        actionDay=current?.day;chart.actionDay=actionDay
        val enabled=current!=null
        measurement.isEnabled=enabled;questionnaire.isEnabled=enabled;remove.isEnabled=enabled
    }
    private fun clearValues() {
        val missing=context.getString(R.string.hrvt_unavailable)
        metricButtons.forEach { (candidate,choice) ->
            val label=context.getString(candidate.label)
            choice.text="$label\n$missing";choice.contentDescription="$label $missing"
        }
    }
    private fun refresh(reset:Boolean=false,anchor:LocalDate?=null) {
        caption.text=listOf(context.getString(metric.label),context.getString(metric.unit)).filter { it.isNotEmpty() }.joinToString(" · ")
        val points=points(metric)
        val hasValue=points.any { it.value!=null }
        empty.visibility=if(hasValue) GONE else VISIBLE;chart.visibility=if(hasValue) VISIBLE else GONE
        if(points.isEmpty()) {
            selected=null;actionDay=null;chart.actionDay=null;footerVisibility.temporary(false)
            clearValues()
        }
        val axis=axes.getOrPut(resolution to metric) { HrvTrendAxis.from(points,metric) }
        selected=null
        chart.show(points,reset,axis,resolution,anchor,activeDay)
        if(selected==null) {
            clearValues()
            syncActions()
        }
        applyColors()
    }
}

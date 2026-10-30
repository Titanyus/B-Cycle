package de.bcycle.app

import android.content.Context
import android.widget.CheckBox
import android.widget.LinearLayout
import android.view.View

internal class GraphOverlayPicker(context: Context, themes: ThemeManager, initial: List<AnalysisChart>, initialWindow:GraphWindow=GraphWindow.FULL,
    private val available:Set<AnalysisChart> = AnalysisChart.entries.toSet()) : LinearLayout(context) {
    private val content:LinearLayout
    private val choice=initial.distinct().take(2).toMutableList()
    var changed: () -> Unit = {}
    var window=initialWindow;private set
    val selected get()=choice.toList()
    private val boxes=linkedMapOf<AnalysisChart,CheckBox>()
    private var reflecting=false
    private var remembered=choice.toList()
    private val enabled=android.widget.Switch(context)
    init {
        orientation=VERTICAL; tag="graph_overlay_picker"
        enabled.apply {
            tag="graph_overlay_enabled";setText(R.string.zone_graph_toggle);isChecked=choice.isNotEmpty()
            BicycleDialogStyle.control(this,themes)
            setOnCheckedChangeListener { _,on ->
                if(!reflecting) {
                    if(on) choice.addAll(remembered.filter { it in available }.ifEmpty { AnalysisChart.entries.filter { it in available }.take(1) })
                    else choice.clear()
                    refresh();changed()
                }
            }
        }
        val frame=BicycleFrameDropdown(context,themes,context.getString(R.string.zone_graph_toggle),open=false,
            trailing=enabled,leadingIcon=BicycleIcon.Distance)
        frame.expandControl.tag="graph_overlay_toggle"
        content=frame.content.apply { tag="graph_overlay_content" }
        addView(frame,LayoutParams(-1,-2))
        val segments=android.widget.RadioGroup(context).apply { orientation=HORIZONTAL;tag="graph_windows";BicycleComponents.segmentedControlRow(this,themes) }
        val buttons=GraphWindow.entries.map { value -> BicycleSegmentButton(context,themes).apply {
            id=View.generateViewId();tag="graph_window_${value.name}";setText(value.label);isChecked=value==window
            setOnClickListener { window=value;changed() }
        } }
        buttons.forEach { segments.addView(it,android.widget.RadioGroup.LayoutParams(0,-2,1f)) }
        content.addView(segments,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        AnalysisChart.entries.forEach { chart ->
            val label=when(chart) { AnalysisChart.SPEED -> R.string.graph_speed; AnalysisChart.HEART -> R.string.graph_heart; else -> chart.label }
            val box=CheckBox(context).apply { tag="graph_${chart.name}"
                val title=context.getString(label)
                text=android.text.SpannableString("\uFFFC  $title").apply {
                    setSpan(RecordingDotSpan(resources.displayMetrics.density,chart.color(android.graphics.Color.BLACK),BicycleDesign.graphChoiceDotDiameter),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                contentDescription=title; BicycleDialogStyle.control(this,themes)
                minHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
                setOnClickListener { if(chart in choice) choice.remove(chart) else if(choice.size<2) choice.add(chart); refresh(); changed() } }
            boxes[chart]=box; content.addView(box)
        }
        refresh()
    }
    private fun refresh() {
        if(choice.isNotEmpty()) remembered=choice.toList()
        reflecting=true;enabled.isChecked=choice.isNotEmpty();reflecting=false
        enabled.isEnabled=available.isNotEmpty() || choice.isNotEmpty()
        boxes.forEach { (chart,box) -> box.isChecked=chart in choice; box.isEnabled=chart in available && (chart in choice || choice.size<2);box.alpha=if(box.isEnabled) 1f else .4f } }
}

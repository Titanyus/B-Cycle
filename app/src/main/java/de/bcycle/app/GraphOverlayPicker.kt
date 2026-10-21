package de.bcycle.app

import android.content.Context
import android.widget.CheckBox
import android.widget.LinearLayout
import android.view.View

internal class GraphOverlayPicker(context: Context, themes: ThemeManager, initial: List<AnalysisChart>, initialWindow:GraphWindow=GraphWindow.FULL,
    private val available:Set<AnalysisChart> = AnalysisChart.entries.toSet()) : LinearLayout(context) {
    private val content=LinearLayout(context).apply { orientation=VERTICAL; visibility=GONE; tag="graph_overlay_content" }
    private val choice=initial.distinct().take(2).toMutableList()
    var changed: () -> Unit = {}
    var window=initialWindow;private set
    val selected get()=choice.toList()
    private val boxes=linkedMapOf<AnalysisChart,CheckBox>()
    private val none=CheckBox(context)
    init {
        orientation=VERTICAL; tag="graph_overlay_picker"
        lateinit var toggle: android.widget.Button
        toggle=BicycleComponents.action(context,themes,context.getString(R.string.zone_graph_toggle)) {
            content.visibility=if(content.visibility==VISIBLE) GONE else VISIBLE
            toggle.stateDescription=context.getString(if(content.visibility==VISIBLE) R.string.zone_expanded else R.string.zone_collapsed)
        }.apply { tag="graph_overlay_toggle";isAccessibilityHeading=true;stateDescription=context.getString(R.string.zone_collapsed) }
        BicycleDialogStyle.accordion(toggle,themes)
        BicycleDialogStyle.actionIcon(toggle,themes,BicycleIcon.Expand);addView(toggle);addView(content)
        val segments=android.widget.RadioGroup(context).apply { orientation=HORIZONTAL;tag="graph_windows";BicycleComponents.segmentedControlRow(this,themes) }
        val buttons=GraphWindow.entries.map { value -> BicycleSegmentButton(context,themes).apply {
            id=View.generateViewId();tag="graph_window_${value.name}";setText(value.label);isChecked=value==window
            setOnClickListener { window=value;changed() }
        } }
        buttons.forEach { segments.addView(it,android.widget.RadioGroup.LayoutParams(0,-2,1f)) }
        content.addView(segments,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        none.setText(R.string.graph_none); BicycleDialogStyle.control(none,themes)
        none.setOnClickListener { choice.clear(); refresh(); changed() }; content.addView(none)
        BicycleComponents.controlSpacing(none)
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
    private fun refresh() { none.isChecked=choice.isEmpty(); boxes.forEach { (chart,box) -> box.isChecked=chart in choice; box.isEnabled=chart in available && (chart in choice || choice.size<2);box.alpha=if(box.isEnabled) 1f else .4f } }
}

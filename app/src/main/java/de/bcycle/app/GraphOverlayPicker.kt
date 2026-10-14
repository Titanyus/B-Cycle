package de.bcycle.app

import android.content.Context
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.view.View

internal class GraphOverlayPicker(context: Context, themes: ThemeManager, initial: List<AnalysisChart>, initialDisplay: GraphDisplayMode=GraphDisplayMode.WITH_DATA) : LinearLayout(context) {
    private val content=LinearLayout(context).apply { orientation=VERTICAL; visibility=GONE; tag="graph_overlay_content" }
    private val choice=initial.distinct().take(2).toMutableList()
    var changed: () -> Unit = {}
    val selected get()=choice.toList()
    private val boxes=linkedMapOf<AnalysisChart,CheckBox>()
    private val none=CheckBox(context)
    private val displaySelector=Spinner(context)
    val displayMode get()=GraphDisplayMode.entries[displaySelector.selectedItemPosition.coerceAtLeast(0)]
    init {
        orientation=VERTICAL; tag="graph_overlay_picker"
        lateinit var toggle: android.widget.Button
        toggle=BicycleComponents.action(context,themes,context.getString(R.string.zone_graph_toggle)) {
            content.visibility=if(content.visibility==VISIBLE) GONE else VISIBLE
            toggle.stateDescription=context.getString(if(content.visibility==VISIBLE) R.string.zone_expanded else R.string.zone_collapsed)
        }.apply { tag="graph_overlay_toggle";stateDescription=context.getString(R.string.zone_collapsed) }
        BicycleDialogStyle.actionIcon(toggle,themes,BicycleIcon.Expand);addView(toggle);addView(content)
        none.setText(R.string.graph_none); BicycleDialogStyle.control(none,themes)
        none.setOnClickListener { choice.clear(); refresh(); changed() }; content.addView(none)
        AnalysisChart.entries.forEach { chart ->
            val label=when(chart) { AnalysisChart.SPEED -> R.string.graph_speed; AnalysisChart.HEART -> R.string.graph_heart; else -> chart.label }
            val box=CheckBox(context).apply { tag="graph_${chart.name}"
                val title=context.getString(label)
                text=android.text.SpannableString("\uFFFC  $title").apply {
                    setSpan(RecordingDotSpan(resources.displayMetrics.density,chart.color(android.graphics.Color.BLACK),7f),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                contentDescription=title; BicycleDialogStyle.control(this,themes)
                minHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
                setOnClickListener { if(chart in choice) choice.remove(chart) else if(choice.size<2) choice.add(chart); refresh(); changed() } }
            boxes[chart]=box; content.addView(box)
        }
        content.addView(TextView(context).apply { setText(R.string.graph_display_title);isAccessibilityHeading=true;BicycleDialogStyle.control(this,themes) })
        displaySelector.apply {
            tag="graph_display_mode";contentDescription=context.getString(R.string.graph_display_title)
            adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,listOf(context.getString(R.string.graph_display_with_data),context.getString(R.string.graph_display_only)))
            setSelection(initialDisplay.ordinal)
        }
        BicycleDialogStyle.spinner(displaySelector,themes);content.addView(displaySelector)
        var shown=initialDisplay.ordinal
        observeSettings(displaySelector) { if(displaySelector.selectedItemPosition!=shown) { shown=displaySelector.selectedItemPosition;changed() } }
        refresh()
    }
    private fun refresh() { none.isChecked=choice.isEmpty(); boxes.forEach { (chart,box) -> box.isChecked=chart in choice; box.isEnabled=chart in choice || choice.size<2 } }
}

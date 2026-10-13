package de.bcycle.app

import android.content.Context
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView

internal class GraphOverlayPicker(context: Context, themes: ThemeManager, initial: List<AnalysisChart>) : LinearLayout(context) {
    private val choice=initial.distinct().take(2).toMutableList()
    val selected get()=choice.toList()
    private val boxes=linkedMapOf<AnalysisChart,CheckBox>()
    private val none=CheckBox(context)
    init {
        orientation=VERTICAL; tag="graph_overlay_picker"
        addView(TextView(context).apply { setText(R.string.graph_overlay); isAccessibilityHeading=true
            BicycleDialogStyle.control(this,themes); setPadding(0,BicycleDesign.dp(context,BicycleDesign.rowGap),0,0) })
        none.setText(R.string.graph_none); BicycleDialogStyle.control(none,themes)
        none.setOnClickListener { choice.clear(); refresh() }; addView(none)
        AnalysisChart.entries.forEach { chart ->
            val label=when(chart) { AnalysisChart.SPEED -> R.string.graph_speed; AnalysisChart.HEART -> R.string.graph_heart; else -> chart.label }
            val box=CheckBox(context).apply { tag="graph_${chart.name}"; setText(label); BicycleDialogStyle.control(this,themes)
                minHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
                setOnClickListener { if(chart in choice) choice.remove(chart) else if(choice.size<2) choice.add(chart); refresh() } }
            boxes[chart]=box; addView(box)
        }
        refresh()
    }
    private fun refresh() { none.isChecked=choice.isEmpty(); boxes.forEach { (chart,box) -> box.isChecked=chart in choice; box.isEnabled=chart in choice || choice.size<2 } }
}

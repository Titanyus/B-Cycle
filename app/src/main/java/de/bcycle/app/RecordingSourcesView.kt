package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.*

internal class RecordingSourcesView(context:Context,themes:ThemeManager,profile:BikeProfile,barometer:Boolean,
    initial:RecordingSources,private val editable:()->Boolean,changed:(RecordingSources)->Unit):LinearLayout(context) {
    private var selection=initial
    private val choices=mutableListOf<Pair<Spinner,Boolean>>()
    fun refreshLock() { choices.forEach { (choice,selectable) -> choice.isEnabled=editable() && selectable;choice.alpha=if(choice.isEnabled) 1f else .4f } }
    init {
        orientation=VERTICAL;tag="recording_sources"
        RecordingSources.available(profile,barometer).forEach { (quantity,sources) ->
            val row=LinearLayout(context).apply {
                gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=false;tag="record_source_${quantity.name}"
                // The row owns vertical spacing, as with setting sliders. A top-only
                // child margin plus CENTER_VERTICAL pushes the spinner past the row edge.
                setTag(R.id.inline_control_row,true)
                val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
                setPadding(0,gap,0,gap)
            }
            var shown=sources.indexOf(initial.preferred[quantity]).coerceAtLeast(0)
            row.addView(BicycleComponents.text(context,themes,context.getString(quantity.label)),LayoutParams(0,-2,1f).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            run {
                val choice=Spinner(context).apply {
                    tag="record_source_choice_${quantity.name}"
                    adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,sources.map { context.getString(it.label) }.ifEmpty { listOf(context.getString(R.string.source_none)) })
                    setSelection(shown);id=android.view.View.generateViewId();isEnabled=editable() && sources.size>=2
                }
                choices+=choice to (sources.size>=2)
                (row.getChildAt(0) as TextView).labelFor=choice.id
                BicycleDialogStyle.spinner(choice,themes,BicycleDesign.profileDropdownSp)
                row.addView(choice,LayoutParams(0,-2,1f))
                choice.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent:AdapterView<*>?) {}
                    override fun onItemSelected(parent:AdapterView<*>?,view:android.view.View?,position:Int,id:Long) {
                        if(position==shown) return
                        if(!editable() || sources.size<2) { choice.setSelection(shown);return }
                        shown=position
                        if(choice.isAttachedToWindow && sources[position]!=selection.preferred[quantity]) {
                            selection=RecordingSources(selection.preferred+(quantity to sources[position]));changed(selection)
                        }
                    }
                }
            }
            addView(row,LayoutParams(-1,-2))
        }
        refreshLock()
        addView(BicycleComponents.description(context,themes,context.getString(R.string.source_fallback_hint)))
    }
}

package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.*

internal class RecordingSourcesView(context:Context,themes:ThemeManager,profile:BikeProfile,barometer:Boolean,
    initial:RecordingSources,private val editable:()->Boolean,changed:(RecordingSources)->Unit):LinearLayout(context) {
    private var selection=initial
    private val choices=mutableListOf<Spinner>()
    fun refreshLock() { choices.forEach { it.isEnabled=editable();it.alpha=if(it.isEnabled) 1f else .4f } }
    init {
        orientation=VERTICAL;tag="recording_sources"
        RecordingSources.available(profile,barometer).forEach { (quantity,sources) ->
            val row=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL;tag="record_source_${quantity.name}" }
            var shown=sources.indexOf(initial.preferred[quantity]).coerceAtLeast(0)
            row.addView(BicycleComponents.text(context,themes,context.getString(quantity.label)),LayoutParams(0,-2,1f).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            if(sources.size>=2) {
                val choice=Spinner(context).apply {
                    tag="record_source_choice_${quantity.name}"
                    adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,sources.map { context.getString(it.label) })
                    setSelection(shown);id=android.view.View.generateViewId();isEnabled=editable()
                }
                choices+=choice
                (row.getChildAt(0) as TextView).labelFor=choice.id
                BicycleDialogStyle.spinner(choice,themes,BicycleDesign.profileDropdownSp)
                row.addView(choice,LayoutParams(0,-2,1f))
                choice.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent:AdapterView<*>?) {}
                    override fun onItemSelected(parent:AdapterView<*>?,view:android.view.View?,position:Int,id:Long) {
                        if(position==shown) return
                        if(!editable()) { choice.setSelection(shown);return }
                        shown=position
                        if(choice.isAttachedToWindow && sources[position]!=selection.preferred[quantity]) {
                            selection=RecordingSources(selection.preferred+(quantity to sources[position]));changed(selection)
                        }
                    }
                }
            } else row.addView(BicycleComponents.description(context,themes,context.getString(sources.firstOrNull()?.label ?: R.string.source_none)),LayoutParams(0,-2,1f))
            addView(row,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        }
        addView(BicycleComponents.description(context,themes,context.getString(R.string.source_fallback_hint)))
    }
}

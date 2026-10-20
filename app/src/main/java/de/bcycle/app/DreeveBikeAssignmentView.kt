package de.bcycle.app

import android.content.Context
import android.widget.*

/** Explicit confirmation: constructing or dismissing this form changes no saved mapping. */
internal class DreeveBikeAssignmentView(context:Context,themes:ThemeManager,gear:List<String>,names:Map<String,String>,
    private val profiles:List<BikeProfile>,remembered:Map<String,Int>):LinearLayout(context) {
    private val choices=linkedMapOf<String,Spinner>()
    init {
        orientation=VERTICAL
        val padding=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(padding,padding,padding,padding)
        gear.distinct().forEach { id ->
            addView(BicycleComponents.contentHeading(context,themes,names[id] ?: id))
            val picker=Spinner(context).apply {
                tag="dreeve_gear_$id";contentDescription=names[id] ?: id
                adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,listOf(context.getString(R.string.route_filter_unknown_bike))+profiles.map { it.name })
                setSelection(profiles.indexOfFirst { it.slot==remembered[id] }+1)
            }
            BicycleDialogStyle.spinner(picker,themes)
            choices[id]=picker;addView(picker,LayoutParams(-1,-2))
        }
    }
    fun assignments()=choices.mapValues { (_,picker)->profiles.getOrNull(picker.selectedItemPosition-1)?.slot ?: -1 }
}

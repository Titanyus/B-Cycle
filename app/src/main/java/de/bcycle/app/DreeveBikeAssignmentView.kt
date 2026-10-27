package de.bcycle.app

import android.content.Context
import android.widget.*

/** Explicit confirmation: constructing or dismissing this form changes no saved mapping. */
internal class DreeveBikeAssignmentView(context:Context,themes:ThemeManager,gear:List<String>,names:Map<String,String>,
    private val profiles:List<BikeProfile>,remembered:Map<String,Int>):LinearLayout(context) {
    private val choices=linkedMapOf<String,Spinner>()
    private var selectedOrigin=TrackOrigin.IMPORTED
    fun origin()=selectedOrigin
    init {
        orientation=VERTICAL
        val padding=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(padding,padding,padding,padding)
        addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.dreeve_import_origin)))
        addView(RadioGroup(context).apply {
            orientation=HORIZONTAL;tag="dreeve_import_origin"
            BicycleComponents.segmentedControlRow(this,themes)
            listOf(TrackOrigin.IMPORTED to R.string.ui_routes_filter_imported,
                TrackOrigin.RECORDED to R.string.ui_routes_filter_recorded).forEach { (origin,label) ->
                addView(BicycleSegmentButton(context,themes).apply {
                    id=generateViewId();tag="dreeve_origin_${origin.name}";setText(label)
                    isChecked=origin==selectedOrigin
                    setOnCheckedChangeListener { _,checked -> if(checked) selectedOrigin=origin }
                },RadioGroup.LayoutParams(0,-2,1f))
            }
        },LayoutParams(-1,-2))
        addView(BicycleComponents.description(context,themes,context.getString(R.string.dreeve_import_origin_hint)))
        if(gear.isNotEmpty()) addView(BicycleComponents.sectionDivider(context,themes))
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

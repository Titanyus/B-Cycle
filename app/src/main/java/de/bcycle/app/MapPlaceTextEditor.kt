package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.*

/** Compact, initially closed typography controls; all values use the existing profile style delta. */
internal class MapPlaceTextEditor(context:Context,private val prefs:SharedPreferences,themes:ThemeManager,
    private val changed:()->Unit):LinearLayout(context) {
    private val profile=prefs.getInt(BikeProfileStore.ACTIVE,0)
    private var selected=MapPlaceClass.TOWN
    private var updating=false
    private val classes=Spinner(context).apply { tag="map_place_class" }
    private val weight=Spinner(context).apply { tag="map_place_weight" }
    private val size=BicycleSettingSlider(context,themes) { "$it %" }.apply { tag="map_place_size";min=50;max=200 }
    init {
        orientation=VERTICAL
        val frame=BicycleFrameDropdown(context,themes,context.getString(R.string.map_place_typography),open=false,leadingIcon=BicycleIcon.RenameTrack).apply { tag="map_place_typography" }
        classes.adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,MapPlaceClass.entries.map { context.getString(it.title) })
        classes.setSelection(selected.ordinal)
        classes.contentDescription=context.getString(R.string.map_place_class)
        weight.adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,(1..9).map { if(it==9) "900 · Black" else "${it*100}" })
        weight.contentDescription=context.getString(R.string.map_place_weight)
        size.contentDescription=context.getString(R.string.map_place_size)
        frame.content.addView(classes,LayoutParams(-1,-2))
        frame.content.addView(BicycleComponents.fieldLabel(context,themes,context.getString(R.string.map_place_weight)))
        frame.content.addView(weight,LayoutParams(-1,-2))
        frame.content.addView(settingSliderRow(context,themes,context.getString(R.string.map_place_size),size).apply {
            minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize);setPadding(0,0,0,0)
        },LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.touchSize)))
        addView(frame,LayoutParams(-1,-2))
        fun listen(spinner:Spinner,action:(Int)->Unit) {
            spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent:AdapterView<*>?)=Unit
                override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long)=action(position)
            }
        }
        listen(classes) { selected=MapPlaceClass.entries[it];render() }
        listen(weight) { if(!updating) save(current().copy(weight=(it+1)*100)) }
        size.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar:SeekBar)=Unit
            override fun onStopTrackingTouch(bar:SeekBar)=Unit
            override fun onProgressChanged(bar:SeekBar,value:Int,fromUser:Boolean) { if(!updating) save(current().copy(sizePercent=value)) }
        })
        render()
    }
    private fun current()=MapStyleOverrides.read(prefs).placeLabels[selected] ?: selected.defaults
    fun render() {
        updating=true
        val value=current();weight.setSelection(value.weight/100-1);size.progress=value.sizePercent
        updating=false
    }
    private fun save(value:MapPlaceTextStyle) {
        if(prefs.getInt(BikeProfileStore.ACTIVE,0)!=profile || value==current()) return
        val old=MapStyleOverrides.read(prefs)
        old.copy(placeLabels=old.placeLabels.toMutableMap().apply { if(value==selected.defaults) remove(selected) else put(selected,value) }).write(prefs)
        changed()
    }
}

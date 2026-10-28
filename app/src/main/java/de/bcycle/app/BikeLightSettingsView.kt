package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

internal class BikeLightSettingsView(context:Context,themes:ThemeManager,private val prefs:SharedPreferences,
    private val canEdit:()->Boolean={true},private val changed:()->Unit={}):LinearLayout(context) {
    private var displayed=0
    private val choices=object:ArrayAdapter<String>(context,android.R.layout.simple_spinner_dropdown_item,BikeLightPlacement.entries.map { context.getString(it.label) }) {
        override fun areAllItemsEnabled()=DashboardFullscreen.enabled(prefs)
        override fun isEnabled(position:Int)=position!=BikeLightPlacement.TOP.ordinal || DashboardFullscreen.enabled(prefs)
        override fun getDropDownView(position:Int,convertView:View?,parent:ViewGroup):View=super.getDropDownView(position,convertView,parent).apply {
            isEnabled=isEnabled(position);alpha=if(isEnabled) 1f else .5f
        }
    }
    private val placement=Spinner(context).apply { tag="bike_light_placement";adapter=choices;id=generateViewId() }
    private val listener=SharedPreferences.OnSharedPreferenceChangeListener { _,key -> if(key==DashboardFullscreen.key || key==BikeLightSettings.key) refresh() }
    init {
        tag="bike_light_settings";gravity=Gravity.CENTER_VERTICAL
        addView(BicycleComponents.description(context,themes,context.getString(R.string.bike_light_title)).apply {
            labelFor=placement.id
        },LayoutParams(0,-2,1f).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        BicycleDialogStyle.spinner(placement,themes,BicycleDesign.profileDropdownSp)
        addView(placement,LayoutParams(0,-2,1f))
        refresh()
        placement.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) {
                if(position==displayed) return
                if(!isAttachedToWindow || !canEdit() || !choices.isEnabled(position)) { refresh();return }
                displayed=position;prefs.edit().putString(BikeLightSettings.key,BikeLightPlacement.entries[position].name).apply();changed()
            }
        }
    }
    private fun refresh() {
        displayed=if(BikeLightSettings.top(prefs)) BikeLightPlacement.TOP.ordinal else BikeLightPlacement.BOTTOM.ordinal
        placement.setSelection(displayed);choices.notifyDataSetChanged()
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();prefs.registerOnSharedPreferenceChangeListener(listener);refresh() }
    override fun onDetachedFromWindow() { prefs.unregisterOnSharedPreferenceChangeListener(listener);super.onDetachedFromWindow() }
}

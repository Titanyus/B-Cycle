package de.bcycle.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.*

internal class MapSourcePopup(private val anchor:View,runtime:RideRuntime,changed:()->Unit,closed:()->Unit):PopupWindow() {
    init {
        val context=anchor.context;val prefs=runtime.preferences;val themes=runtime.themes;val profile=runtime.bikes.activeSlot
        isFocusable=true;isOutsideTouchable=true;setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val body=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;tag="map_source_popup";val pad=BicycleDesign.dp(context,12);setPadding(pad,pad,pad,pad) }
        val source=MapSourcePicker(context,themes,prefs).apply { select(MapSource.decode(prefs.getString("mapSource",null))) }
        body.addView(source,LinearLayout.LayoutParams(-1,-2));body.addView(BicycleComponents.divider(context,themes))
        val switches=mutableMapOf<String,Switch>();var updating=false
        for((key,title,fallback) in listOf(Triple(VtmBaseMap.cyclewaysKey,R.string.map_cycling_ways,true),Triple(MapVectorLayer.CYCLE_ROUTES.key,R.string.map_cycling_routes,false))) {
            val toggle=Switch(context).apply { text=context.getString(title);isChecked=prefs.getBoolean(key,fallback);tag="map_source_$key" }
            switches[key]=toggle;body.addView(toggle)
            toggle.setOnCheckedChangeListener { _,on -> if(!updating && runtime.bikes.activeSlot==profile) { prefs.edit().putBoolean(key,on).apply();changed() } }
        }
        var position=source.selectedItemPosition
        source.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,next:Int,id:Long) {
                if(position==next) return
                position=next
                if(runtime.bikes.activeSlot!=profile) { dismiss();return }
                if(source.selectedSource==MapSource.OPENCYCLEMAP && MapProviderSettings.read(context).isEmpty()) {
                    source.select(MapSource.decode(prefs.getString("mapSource",null)));position=source.selectedItemPosition
                    BicycleFeedback.show(context,R.string.map_thunderforest_required);return
                }
                if(source.selectedSource==MapSource.VECTOR) MapStyleLibrary.load(prefs,source.selectedStyleId)
                else prefs.edit().putString("mapSource",source.selectedSource.name).apply()
                updating=true;switches.forEach { (key,toggle) -> toggle.isChecked=prefs.getBoolean(key,key==VtmBaseMap.cyclewaysKey) };updating=false
                changed()
            }
        }
        BicycleDialogStyle.content(body,themes)
        BicycleComponents.bindTheme(body,themes) { body.background=BicycleComponents.controlBackground(context,it,it.tertiary) }
        contentView=body;width=minOf(BicycleDesign.dp(context,300),context.resources.displayMetrics.widthPixels-12);height=android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        setOnDismissListener { closed() }
    }
    fun show() { showAsDropDown(anchor) }
}

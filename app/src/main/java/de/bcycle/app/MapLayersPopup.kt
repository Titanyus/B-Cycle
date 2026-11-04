package de.bcycle.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

/** Direct profile switches; keep the dropdown open while the unchanged MapView redraws. */
internal class MapLayersPopup(private val anchor:View,private val runtime:RideRuntime,
    private val changed:()->Unit,closed:()->Unit):PopupWindow() {
    private val profile=runtime.bikes.activeSlot
    init {
        val context=anchor.context;val themes=runtime.themes;val prefs=runtime.preferences
        isFocusable=true;isOutsideTouchable=true;inputMethodMode=INPUT_METHOD_NOT_NEEDED
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val column=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL;tag="map_layers_popup"
            val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        }
        column.addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.map_vector_layers)))
        fun toggle(key:String,title:Int,fallback:Boolean) {
            column.addView(Switch(context).apply {
                tag="map_quick_$key";text=context.getString(title);isChecked=prefs.getBoolean(key,fallback)
                setOnCheckedChangeListener { _,on ->
                    if(runtime.bikes.activeSlot==profile) { prefs.edit().putBoolean(key,on).apply();changed() }
                    else dismiss()
                }
            })
        }
        toggle(VtmBaseMap.cyclewaysKey,R.string.map_vector_cycleways,true)
        MapVectorLayer.entries.forEach { toggle(it.key,it.title,it.defaultVisible) }
        BicycleDialogStyle.content(column,themes)
        BicycleComponents.bindTheme(column,themes) { tokens -> column.background=BicycleComponents.controlBackground(context,tokens,tokens.tertiary) }
        contentView=ScrollView(context).apply { isFillViewport=false;BicycleComponents.scrollStyle(this);addView(column) }
        width=minOf(BicycleDesign.dp(context,280),context.resources.displayMetrics.widthPixels-BicycleDesign.dp(context,MapControlDesign.nightOffset+MapControlDesign.edgeGap))
        height=minOf(BicycleDesign.dp(context,600),(context.resources.displayMetrics.heightPixels*.6).toInt())
        setOnDismissListener { closed() }
    }
    fun show() { showAsDropDown(anchor,0,BicycleDesign.dp(anchor.context,MapControlDesign.buttonGap),Gravity.START) }
}

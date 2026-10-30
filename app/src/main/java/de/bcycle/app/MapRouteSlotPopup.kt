package de.bcycle.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow

/** Same map control chrome, with opaque route colour and the same eye-state glyph as the master. */
internal class MapRouteSlotPopup(private val anchor:View,private val runtime:RideRuntime,private val assignId:String?=null,closed:()->Unit):PopupWindow() {
    private val context=anchor.context
    private val profile=runtime.bikes.activeSlot
    private val buttons=mutableListOf<Button>()
    private var painted=""
    init {
        isFocusable=true;isOutsideTouchable=true;inputMethodMode=INPUT_METHOD_NOT_NEEDED
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        contentView=LinearLayout(context).apply {
            orientation=LinearLayout.HORIZONTAL;isBaselineAligned=false;tag="map_route_slot_controls"
            repeat(MapRouteSlots.COUNT) { slot ->
                val button=Button(context).apply {
                    tag="map_route_slot_$slot";gravity=Gravity.CENTER;isAllCaps=false
                    minWidth=0;minHeight=0;minimumWidth=0;minimumHeight=0;stateListAnimator=null;elevation=0f
                    text=context.getString(R.string.map_route_slot_number,slot+1)
                    typeface=BicycleDesign.bodyTypeface;textSize=MapControlDesign.slotLabelSp
                    setPadding(0,0,0,0);compoundDrawablePadding=BicycleDesign.dp(context,MapControlDesign.buttonGap)
                    setOnClickListener {
                        if(runtime.bikes.activeSlot==profile && !runtime.mapState.busy) {
                            if(assignId==null) { runtime.mapState.setRouteSlotVisible(slot);refresh() }
                            else { runtime.mapState.assignMapRoute(assignId,slot);dismiss() }
                        }
                    }
                }
                buttons+=button
                addView(button,LinearLayout.LayoutParams(BicycleDesign.dp(context,MapControlDesign.dashboardWidth),BicycleDesign.dp(context,MapControlDesign.buttonHeight)).apply {
                    if(slot>0) marginStart=BicycleDesign.dp(context,MapControlDesign.buttonGap)
                })
            }
        }
        width=ViewGroup.LayoutParams.WRAP_CONTENT;height=ViewGroup.LayoutParams.WRAP_CONTENT
        setOnDismissListener { closed() };refresh()
    }
    fun refresh() {
        if(runtime.bikes.activeSlot!=profile || assignId!=null && runtime.mapState.entry(assignId)==null) { dismiss();return }
        val signature="${runtime.mapState.routeRevision}:${runtime.mapState.busy}:${runtime.preferences.getString(MapRouteStyles.key,"")}:${runtime.preferences.getBoolean("mapNight",false)}"
        if(signature==painted) return
        painted=signature
        buttons.forEachIndexed { slot,button ->
            val id=runtime.mapState.mapSlots.ids[slot]
            val on=if(assignId==null) id?.let(runtime.mapState::routeVisible)==true else id==assignId
            val color=MapRouteStyles.read(runtime.preferences,MapRouteStyles.saved(slot)).color or Color.BLACK
            val foreground=if(ThemeValidator.contrast(Color.WHITE,color)>=ThemeValidator.contrast(Color.BLACK,color)) Color.WHITE else Color.BLACK
            button.backgroundTintList=null
            button.background=MapControlDesign.background(context).apply {
                setColor(color)
                if(assignId!=null && on) setStroke(BicycleDesign.dp(context,MapControlDesign.strokeWidth*2),foreground)
            }
            button.setTextColor(foreground);button.isEnabled=(assignId!=null || id!=null) && !runtime.mapState.busy
            button.alpha=1f;button.isSelected=on
            val size=BicycleDesign.dp(context,MapControlDesign.slotIconSize)
            button.setCompoundDrawables(if(assignId==null) BicycleIcons.drawable(context,BicycleIcon.ShowOnMap,foreground,negative=!on).apply { setBounds(0,0,size,size) } else null,null,null,null)
            button.stateDescription=context.getString(if(on) R.string.ui_selected else R.string.ui_not_selected)
            button.contentDescription=if(assignId!=null) context.getString(R.string.map_route_slot_assign,slot+1,runtime.mapState.entry(assignId)?.name.orEmpty()) else if(id==null) context.getString(R.string.map_route_slot_empty,slot+1) else
                context.getString(R.string.map_route_slot_description,slot+1,runtime.mapState.entry(id)?.name.orEmpty(),button.stateDescription)
        }
    }
    fun show() { showAsDropDown(anchor,0,BicycleDesign.dp(context,MapControlDesign.buttonGap),Gravity.START) }
}

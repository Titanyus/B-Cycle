package de.bcycle.app

import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow

/** An anchored dropdown with two equal-height, baseline-independent rows of three choices. */
internal class DashboardStatusPopup(anchor:View,themes:ThemeManager,selected:DashboardStatusItem,
    choose:(DashboardStatusItem)->Unit):PopupWindow() {
    init {
        val context=anchor.context
        val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
        val frame=Rect().also(anchor::getWindowVisibleDisplayFrame)
        val available=frame.width().takeIf { it>0 } ?: anchor.rootView.width.takeIf { it>0 } ?: context.resources.displayMetrics.widthPixels
        width=(available-gap*2).coerceAtLeast(1);height=ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable=true;isOutsideTouchable=true;inputMethodMode=INPUT_METHOD_NOT_NEEDED
        setIsClippedToScreen(true)
        setBackgroundDrawable(BicycleComponents.dropdownBackground(context,themes.active.tokens))
        contentView=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL;tag="dashboard_status_choices";clipToOutline=true
            contentDescription=context.getString(R.string.dashboard_status_choose)
            BicycleComponents.bindTheme(this,themes) { tokens ->
                background=android.graphics.drawable.GradientDrawable().apply { setColor(tokens.tertiary);cornerRadius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat() }
                setPadding(gap,gap,gap,gap)
            }
            DashboardStatusItem.entries.chunked(3).forEachIndexed { index,items ->
                addView(LinearLayout(context).apply {
                    orientation=LinearLayout.HORIZONTAL;isBaselineAligned=false;setTag(R.id.inline_control_row,true)
                    items.forEachIndexed { column,item ->
                        addView(BicycleSegmentButton(context,themes,standalone=true).apply {
                            tag="dashboard_status_choice_${item.name}";showSelectionHighlight=false
                            textSize=BicycleDesign.analysisLabelSp;isChecked=item==selected
                            statusSlotAppearance(this,item,themes)
                            setOnClickListener { choose(item);dismiss() }
                        },LinearLayout.LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.rideNamePartHeight),1f).apply { marginStart=if(column==0) 0 else gap })
                    }
                },LinearLayout.LayoutParams(-1,-2).apply { topMargin=if(index==0) 0 else gap })
            }
        }
    }
    fun show(anchor:View) {
        val frame=Rect().also(anchor::getWindowVisibleDisplayFrame)
        val location=IntArray(2);anchor.getLocationOnScreen(location)
        val gap=BicycleDesign.dp(anchor.context,BicycleDesign.controlGap)
        val left=location[0].coerceIn(frame.left+gap,(frame.right-gap-width).coerceAtLeast(frame.left+gap))
        showAsDropDown(anchor,left-location[0],0,Gravity.LEFT)
    }
}

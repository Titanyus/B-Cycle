package de.bcycle.app

import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow

/** The manager's cards at half size: six fit within the safe visible window width. */
internal class BikeQuickPicker(anchor:View,themes:ThemeManager,profiles:List<BikeProfile>,selected:Int,
    choose:(Int)->Unit):PopupWindow() {
    private val frame=Rect().also(anchor::getWindowVisibleDisplayFrame)
    private val gap=BicycleDesign.dp(anchor.context,BicycleDesign.controlGap)
    init {
        val available=frame.width().takeIf { it>0 } ?: anchor.rootView.width.takeIf { it>0 } ?: anchor.resources.displayMetrics.widthPixels
        width=(available-2*gap).coerceAtLeast(1);height=ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable=true;isOutsideTouchable=true;inputMethodMode=INPUT_METHOD_NOT_NEEDED
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        contentView=LinearLayout(anchor.context).apply {
            orientation=LinearLayout.VERTICAL;tag="bike_quick_picker"
            setPadding(gap,gap,gap,gap)
            BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
            addView(BikeProfileStrip(context,themes,{ slot -> choose(slot);dismiss() },{}, { _,_->false },compact=true).apply {
                show(profiles,selected,false)
            },LinearLayout.LayoutParams(-1,-2))
        }
    }
    fun show(anchor:View) {
        anchor.getWindowVisibleDisplayFrame(frame)
        val screenHeight=frame.height().takeIf { it>0 } ?: anchor.resources.displayMetrics.heightPixels
        contentView.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(screenHeight,View.MeasureSpec.AT_MOST))
        height=contentView.measuredHeight.coerceIn(1,screenHeight)
        val location=IntArray(2);anchor.getLocationOnScreen(location)
        val top=(location[1]-height-gap).coerceIn(frame.top,(frame.bottom-height).coerceAtLeast(frame.top))
        // Explicit screen coordinates avoid the platform's below-anchor placement at the bottom toolbar.
        setIsLaidOutInScreen(true)
        setIsClippedToScreen(true)
        showAtLocation(anchor,Gravity.TOP or Gravity.LEFT,frame.left+gap,top)
    }
}

package de.bcycle.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

/** Dedicated on-map editor popup, independent of the visibility switches. */
internal class MapStyleEditorPopup(private val anchor:View,runtime:RideRuntime,changed:()->Unit,
    export:(String)->Unit,closed:()->Unit):PopupWindow() {
    init {
        val context=anchor.context;val themes=runtime.themes
        isFocusable=true;isOutsideTouchable=true;inputMethodMode=INPUT_METHOD_NOT_NEEDED
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val column=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL;tag="map_style_editor_popup"
            val padding=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(padding,padding,padding,padding)
            addView(MapStyleEditorView(context,runtime.preferences,themes,changed,export,::dismiss),LinearLayout.LayoutParams(-1,-2))
        }
        BicycleComponents.bindTheme(column,themes) { tokens -> column.background=BicycleComponents.controlBackground(context,tokens,tokens.tertiary) }
        contentView=ScrollView(context).apply { BicycleComponents.scrollStyle(this);addView(column) }
        width=minOf(BicycleDesign.dp(context,320),context.resources.displayMetrics.widthPixels-BicycleDesign.dp(context,MapControlDesign.edgeGap*2))
        // Size to the compact editor, cap only on short screens; do not reserve empty map area.
        column.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
        height=minOf(column.measuredHeight,(context.resources.displayMetrics.heightPixels*.65).toInt())
        setOnDismissListener { closed() }
    }
    fun show() { showAsDropDown(anchor,0,BicycleDesign.dp(anchor.context,MapControlDesign.buttonGap),Gravity.START) }
}

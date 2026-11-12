package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout

/** Shared framed list: one surface, standard dropdown gap, centered separators. */
internal class BicycleGroupedBox(context:Context,private val themes:ThemeManager):LinearLayout(context) {
    init {
        orientation=VERTICAL;clipToOutline=true
        val border=BicycleDesign.dp(context,BicycleDesign.controlBorder)
        setPadding(border,border,border,border)
        BicycleComponents.bindTheme(this,themes) { background=BicycleComponents.controlBackground(context,it,it.secondary) }
    }
    fun addEntry(view:View) {
        if(childCount>0) addView(BicycleComponents.divider(context,themes),LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.controlBorder)).apply {
            marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap);marginEnd=marginStart
            val space=BicycleDesign.dp(context,BicycleDesign.controlGap)-height
            topMargin=space/2;bottomMargin=space-topMargin
        })
        addView(view,LayoutParams(-1,-2))
    }
}

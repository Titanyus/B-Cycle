package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout

/** Shared framed list: one surface, standard dropdown gap, centered separators. */
internal class BicycleGroupedBox(context:Context,private val themes:ThemeManager,standalone:Boolean=false):LinearLayout(context) {
    init {
        orientation=VERTICAL;clipToOutline=true
        if(standalone) BicycleComponents.controlSpacing(this)
        val border=BicycleDesign.dp(context,BicycleDesign.controlBorder)
        setPadding(border,border,border,border)
        BicycleComponents.bindTheme(this,themes) { background=BicycleComponents.controlBackground(context,it,it.secondary) }
    }
    private val separators=mutableListOf<Pair<View,View>>()
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        var before=false
        for(i in 0 until childCount) {
            val child=getChildAt(i)
            val entry=separators.firstOrNull { it.first===child }?.second
            if(entry!=null) child.visibility=if(before && entry.visibility!=GONE) VISIBLE else GONE
            else if(child.visibility!=GONE) before=true
        }
        super.onMeasure(widthMeasureSpec,heightMeasureSpec)
    }
    fun addEntry(view:View) {
        if(view is BicycleFrameDropdown) view.grouped=true
        if(childCount>0) addView(BicycleComponents.divider(context,themes).also { separators+=it to view },LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.controlBorder)).apply {
            marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap);marginEnd=marginStart
            val space=BicycleDesign.dp(context,BicycleDesign.controlGap)-height
            topMargin=space/2;bottomMargin=space-topMargin
        })
        addView(view,LayoutParams(-1,-2))
    }
}

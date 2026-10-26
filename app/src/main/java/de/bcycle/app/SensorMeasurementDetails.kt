package de.bcycle.app

import android.content.Context
import android.graphics.drawable.RotateDrawable
import android.widget.LinearLayout
import android.widget.TextView

/** Small disclosure on the existing status text; never advertises unobserved capabilities. */
internal class SensorMeasurementDetails(context:Context,private val themes:ThemeManager,val status:TextView):LinearLayout(context) {
    private val values=BicycleComponents.description(context,themes,"").apply { visibility=GONE }
    private var labels=emptyList<String>()
    private var expanded=false
    init {
        orientation=VERTICAL
        addView(status,LayoutParams(-2,-2))
        addView(values,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.smallGap) })
        status.setOnClickListener { if(labels.isNotEmpty()) { expanded=!expanded;render() } }
        BicycleComponents.bindTheme(this,themes) { render() }
    }
    fun update(names:List<String>?) {
        if(labels==names.orEmpty()) return
        labels=names.orEmpty()
        if(labels.isEmpty()) expanded=false
        values.text=labels.joinToString("\n")
        render()
    }
    private fun render() {
        values.visibility=if(expanded && labels.isNotEmpty()) VISIBLE else GONE
        status.isClickable=labels.isNotEmpty();status.isFocusable=status.isClickable
        status.minimumHeight=if(status.isClickable) BicycleDesign.dp(context,BicycleDesign.touchSize) else 0
        status.gravity=android.view.Gravity.CENTER_VERTICAL
        status.stateDescription=if(labels.isEmpty()) null else context.getString(if(expanded) R.string.zone_expanded else R.string.zone_collapsed)
        val arrow=if(labels.isEmpty()) null else RotateDrawable().apply {
            drawable=BicycleIcons.drawable(context,BicycleIcon.Expand,themes.active.tokens.foreground)
            fromDegrees=0f;toDegrees=180f;level=if(expanded) 10000 else 0
            val size=BicycleDesign.dp(context,16);setBounds(0,0,size,size)
        }
        status.compoundDrawablePadding=BicycleDesign.dp(context,BicycleDesign.smallGap)
        status.setCompoundDrawablesRelative(null,null,arrow,null)
    }
}

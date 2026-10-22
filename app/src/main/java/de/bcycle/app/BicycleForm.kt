package de.bcycle.app

import android.graphics.Color
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Shared settings sections and outlined inputs; persistence remains with each owning screen. */
internal object BicycleForm {
    fun section(parent: LinearLayout, themes: ThemeManager, title: Int, inlineRules:Boolean=false): BicycleSectionHeader {
        val context=parent.context
        parent.clipToPadding=false
        return BicycleSectionHeader(context,themes,context.getString(title),inlineRules).also {
            parent.addView(it,LinearLayout.LayoutParams(-1,-2).apply {
                marginStart=-parent.paddingLeft; marginEnd=-parent.paddingRight
                topMargin=0
                bottomMargin=0
            })
        }
    }
    /** Upgrade unbound sticky sections once; preserve existing accordion state and rename headers. */
    fun accordions(root:android.view.View) {
        if(root !is android.view.ViewGroup) return
        if(root is LinearLayout && root.orientation==LinearLayout.VERTICAL) {
            var i=0
            while(i<root.childCount) {
                val header=root.getChildAt(i) as? BicycleSectionHeader
                if(header!=null && header.expanded==null && header.tag!="bike_name_header") {
                    val body=LinearLayout(root.context).apply { orientation=LinearLayout.VERTICAL;tag="sticky_accordion_content" }
                    while(i+1<root.childCount && root.getChildAt(i+1) !is BicycleSectionHeader) {
                        val child=root.getChildAt(i+1);root.removeViewAt(i+1);body.addView(child,child.layoutParams)
                    }
                    root.addView(body,i+1,LinearLayout.LayoutParams(-1,-2));header.accordion(body)
                }
                i++
            }
        }
        (0 until root.childCount).map(root::getChildAt).forEach(::accordions)
    }
    fun outlinedInput(edit: TextView, themes: ThemeManager, numeric: Boolean=false) {
        BicycleDialogStyle.control(edit,themes)
        if(numeric) edit.apply {
            typeface=BicycleDesign.narrowTypeface(context)
            textSize=BicycleDesign.settingsMetricSp
            gravity=Gravity.END or Gravity.CENTER_VERTICAL
            includeFontPadding=false
            setPadding(BicycleDesign.dp(context,BicycleDesign.inputHorizontalPadding),BicycleDesign.dp(context,BicycleDesign.smallGap),
                BicycleDesign.dp(context,BicycleDesign.inputHorizontalPadding),BicycleDesign.dp(context,BicycleDesign.smallGap))
        }
        BicycleComponents.bindTheme(edit,themes) { tokens ->
            edit.backgroundTintList=null
            edit.background=BicycleComponents.controlBackground(edit.context,tokens,Color.TRANSPARENT)
        }
    }
    fun numberRow(edit: EditText, themes: ThemeManager, title: Int, unit: Int, unitTag: String): LinearLayout {
        val context=edit.context
        if(edit.id==android.view.View.NO_ID) edit.id=android.view.View.generateViewId()
        outlinedInput(edit,themes,numeric=true)
        return LinearLayout(context).apply {
            gravity=Gravity.CENTER_VERTICAL
            setTag(R.id.inline_control_row,true)
            addView(BicycleComponents.text(context,themes,context.getString(title)).apply { labelFor=edit.id },
                LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            addView(edit,LinearLayout.LayoutParams(BicycleDesign.dp(context,BicycleDesign.formNumberWidth),-2))
            addView(TextView(context).apply {
                text=context.getString(unit); tag=unitTag; textSize=BicycleDesign.bodySp
                BicycleComponents.secondaryText(this,themes); gravity=Gravity.CENTER
                importantForAccessibility=TextView.IMPORTANT_FOR_ACCESSIBILITY_NO
            },LinearLayout.LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),-2))
        }
    }
}

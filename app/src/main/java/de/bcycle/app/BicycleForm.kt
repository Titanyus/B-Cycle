package de.bcycle.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
    fun outlinedInput(edit: TextView, themes: ThemeManager, numeric: Boolean=false) {
        BicycleDialogStyle.control(edit,themes)
        if(numeric) edit.apply {
            typeface=resources.getFont(R.font.bebas_neue)
            textSize=BicycleDesign.settingsMetricSp
            gravity=Gravity.END or Gravity.CENTER_VERTICAL
            includeFontPadding=false
            setPadding(BicycleDesign.dp(context,BicycleDesign.inputHorizontalPadding),BicycleDesign.dp(context,BicycleDesign.smallGap),
                BicycleDesign.dp(context,BicycleDesign.inputHorizontalPadding),BicycleDesign.dp(context,BicycleDesign.smallGap))
        }
        BicycleComponents.bindTheme(edit,themes) { tokens ->
            edit.backgroundTintList=null
            edit.background=GradientDrawable().apply {
                setColor(Color.TRANSPARENT); setStroke(BicycleDesign.linePixels(edit.context),tokens.tertiary)
            }
        }
    }
    fun numberRow(edit: EditText, themes: ThemeManager, title: Int, unit: Int, unitTag: String): LinearLayout {
        val context=edit.context
        if(edit.id==android.view.View.NO_ID) edit.id=android.view.View.generateViewId()
        outlinedInput(edit,themes,numeric=true)
        return LinearLayout(context).apply {
            gravity=Gravity.CENTER_VERTICAL
            addView(BicycleComponents.text(context,themes,context.getString(title)).apply { labelFor=edit.id },
                LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            addView(edit,LinearLayout.LayoutParams(BicycleDesign.dp(context,BicycleDesign.formNumberWidth),-2))
            addView(TextView(context).apply {
                text=context.getString(unit); tag=unitTag; textSize=BicycleDesign.bodySp
                setTextColor(BicycleDesign.dashboardUnitColor); gravity=Gravity.CENTER
                importantForAccessibility=TextView.IMPORTANT_FOR_ACCESSIBILITY_NO
            },LinearLayout.LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),-2))
        }
    }
}

package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.widget.Button

/** Explicit opt-in button variants. Same native Button behavior in both design modes. */
internal class BicycleSpecialButton(context:Context,themes:ThemeManager,val filled:Boolean):Button(context) {
    init {
        isAllCaps=false;textSize=BicycleDesign.bodySp;typeface=BicycleDesign.strongTypeface
        minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
        minimumWidth=BicycleDesign.dp(context,BicycleDesign.touchSize)
        setPadding(BicycleDesign.dp(context,BicycleDesign.horizontalPadding),BicycleDesign.dp(context,BicycleDesign.verticalPadding),
            BicycleDesign.dp(context,BicycleDesign.horizontalPadding),BicycleDesign.dp(context,BicycleDesign.verticalPadding))
        BicycleComponents.controlSpacing(this)
        BicycleComponents.bindTheme(this,themes) { tokens ->
            styleSpecialButton(this,tokens,filled)
        }
    }
}

/** Shared decoration also for a control whose running state remains a plain text action. */
internal fun styleSpecialButton(view:android.widget.TextView,tokens:ThemeTokens,filled:Boolean) {
    view.setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),intArrayOf(0x80ffffff.toInt(),Color.WHITE)))
    view.typeface=BicycleDesign.strongTypeface;view.textSize=BicycleDesign.bodySp
    view.backgroundTintList=null
    val fill=if(filled) BicycleAccentGradient.fill(view.context,tokens,foreground=Color.WHITE,chromatic=BicycleInputStyle.strength(view),radiusDp=BicycleDesign.specialButtonRadius) else null
    val padding=intArrayOf(view.paddingLeft,view.paddingTop,view.paddingRight,view.paddingBottom)
    view.background=RippleDrawable(ColorStateList.valueOf(0x26ffffff),BicycleInputFrame.decorate(view,tokens,true,fill,BicycleDesign.specialButtonRadius,((1.0-BicycleDesign.specialButtonHeightScale)/2).toFloat()),null)
    view.setPadding(padding[0],padding[1],padding[2],padding[3])
}

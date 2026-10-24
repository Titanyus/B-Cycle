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
            setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),intArrayOf(0x80ffffff.toInt(),Color.WHITE)))
            backgroundTintList=null
            val fill=if(filled) BicycleAccentGradient.fill(context,tokens,foreground=Color.WHITE,chromatic=BicycleInputStyle.strength(this),radiusDp=BicycleDesign.specialButtonRadius) else null
            background=RippleDrawable(ColorStateList.valueOf(0x26ffffff),BicycleInputFrame.decorate(this,tokens,true,fill,BicycleDesign.specialButtonRadius),null)
        }
    }
}

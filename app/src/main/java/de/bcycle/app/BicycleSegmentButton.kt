package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.RadioButton

/** Native mutually exclusive radio semantics, presented as a segment without a selection circle. */
internal class BicycleSegmentButton(context: Context, private val themes: ThemeManager) : RadioButton(context) {
    init {
        buttonDrawable=null;buttonTintList=null
        gravity=Gravity.CENTER;typeface=Typeface.DEFAULT;textSize=BicycleDesign.choiceSp
        minWidth=0;minimumWidth=0;minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
        setPadding(BicycleDesign.dp(context,BicycleDesign.controlGap),0,BicycleDesign.dp(context,BicycleDesign.controlGap),0)
        maxLines=2
        BicycleComponents.bindTheme(this,themes,::paint)
        setOnCheckedChangeListener { _,_->paint(themes.active.tokens) }
    }
    private fun paint(tokens:ThemeTokens) {
        val fill=if(isChecked) tokens.accent else tokens.secondary
        val foreground=if(!isChecked || ThemeValidator.contrast(tokens.foreground,fill)>=ThemeValidator.minimumTextContrast) tokens.foreground
            else if(ThemeValidator.contrast(Color.BLACK,fill)>=ThemeValidator.contrast(Color.WHITE,fill)) Color.BLACK else Color.WHITE
        setTextColor(foreground)
        background=RippleDrawable(ColorStateList.valueOf((foreground and 0x00ffffff) or 0x26000000),GradientDrawable().apply {
            setColor(fill);cornerRadius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat()
        },null)
    }
}

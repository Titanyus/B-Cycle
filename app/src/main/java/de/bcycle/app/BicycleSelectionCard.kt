package de.bcycle.app

import android.graphics.drawable.GradientDrawable
import android.view.View

/** Shared profile/dashboard card chrome; owners retain selection, focus and reorder behaviour. */
internal object BicycleSelectionCard {
    fun apply(view:View,tokens:ThemeTokens,selected:Boolean,add:Boolean=false,scale:Float=1f) {
        fun dp(value:Int)=(BicycleDesign.dp(view.context,value)*scale).toInt().coerceAtLeast(1)
        val color=BicycleComponents.selectionBorder(tokens,selected)
        view.background=GradientDrawable().apply {
            setColor(if(selected) tokens.tertiary else tokens.secondary)
            cornerRadius=dp(BicycleDesign.inputRadius).toFloat()
            if(add) setStroke(BicycleDesign.linePixels(view.context),color,dp(BicycleDesign.smallGap).toFloat(),dp(BicycleDesign.smallGap).toFloat())
            else setStroke(if(selected) dp(2) else BicycleDesign.linePixels(view.context),color)
        }
        val outline=BicycleInputFrame.decorate(view,tokens,selected)
        view.foreground=if(selected) outline else null
    }
}

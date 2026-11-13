package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.Button

/** One UI appearance: the former Glow geometry. Legacy Flat settings have no effect. */
internal object BicycleAppearance {
    const val flatKey="uiTheme.flatDesign"
    @Suppress("UNUSED_PARAMETER")
    fun flat(context:Context)=false
    fun base(view:View) {
        val flat=flat(view.context)
        val shadow=if(flat) android.graphics.Color.TRANSPARENT else android.graphics.Color.BLACK
        view.outlineAmbientShadowColor=shadow;view.outlineSpotShadowColor=shadow
        if(view is Button) {
            view.stateListAnimator=null
            view.elevation=0f
        }
    }
    fun selected(view:View,selected:Boolean) {
        view.stateListAnimator=null
        view.elevation=0f
    }
}

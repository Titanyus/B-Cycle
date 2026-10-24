package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.Button

/** Optional decoration over the same native controls. Never changes geometry or behavior. */
internal object BicycleAppearance {
    const val flatKey="uiTheme.flatDesign"
    fun flat(context:Context)=context.getSharedPreferences("bike",0).getBoolean(flatKey,false)
    fun base(view:View) {
        val flat=flat(view.context)
        val shadow=if(flat) android.graphics.Color.TRANSPARENT else android.graphics.Color.BLACK
        view.outlineAmbientShadowColor=shadow;view.outlineSpotShadowColor=shadow
        if(view is Button) {
            view.stateListAnimator=null
            view.elevation=if(view is BicycleSegmentButton && view.isChecked && !flat) BicycleDesign.dp(view.context,BicycleDesign.selectedControlShadow).toFloat() else 0f
        }
    }
    fun selected(view:View,selected:Boolean) {
        view.stateListAnimator=null
        view.elevation=if(selected && !flat(view.context)) BicycleDesign.dp(view.context,BicycleDesign.selectedControlShadow).toFloat() else 0f
    }
}

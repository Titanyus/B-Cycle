package de.bcycle.app

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import android.view.Window

/** Only dashboard backdrops/cards; shared ownership handles asynchronous dialog handovers. */
internal object DashboardModal {
    private val owners=java.util.WeakHashMap<View,Int>()
    fun blur(dashboard:View):AutoCloseable {
        val count=owners[dashboard] ?: 0
        if(count==0) {
            val radius=BicycleDesign.dp(dashboard.context,BicycleDesign.dashboardModalBlur).toFloat()
            dashboard.setRenderEffect(RenderEffect.createBlurEffect(radius,radius,Shader.TileMode.CLAMP))
        }
        owners[dashboard]=count+1
        var closed=false
        return AutoCloseable {
            if(!closed) {
                closed=true
                val remaining=(owners[dashboard] ?: 1)-1
                if(remaining<=0) { owners.remove(dashboard);dashboard.setRenderEffect(null) } else owners[dashboard]=remaining
            }
        }
    }
    internal fun blurred(view:View)=view in owners
    fun shadow(view:View) {
        view.outlineAmbientShadowColor=Color.BLACK;view.outlineSpotShadowColor=Color.BLACK
        view.elevation=BicycleDesign.dp(view.context,BicycleDesign.dashboardModalShadow).toFloat()
    }
    fun shadow(window:Window) {
        shadow(window.decorView)
    }
}

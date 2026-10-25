package de.bcycle.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

/** Dashboard alert only; does not change ordinary dialogs, settings pages or HRV entry cards. */
internal object BicycleAlertSurface {
    fun bind(view:View,themes:ThemeManager) {
        view.clipToOutline=true
        view.outlineAmbientShadowColor=Color.BLACK
        view.outlineSpotShadowColor=Color.BLACK
        BicycleComponents.bindTheme(view,themes) { tokens ->
            view.background=GradientDrawable().apply {
                cornerRadius=BicycleDesign.dp(view.context,BicycleDesign.dialogRadius).toFloat()
                setColor(tokens.primary)
                setStroke(BicycleDesign.dp(view.context,BicycleDesign.hrvPromptBorder).coerceAtLeast(1),tokens.primary)
            }
            // Explicit alert exception: diffuse shadow in both Flat and Glow.
            view.elevation=BicycleDesign.dp(view.context,BicycleDesign.alertShadow).toFloat()
        }
    }
}

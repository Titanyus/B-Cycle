package de.bcycle.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

/** Dashboard alert only; does not change ordinary dialogs, settings pages or HRV entry cards. */
internal object BicycleAlertSurface {
    fun bind(view:View,themes:ThemeManager,role:ThemeRole=ThemeRole.PRIMARY) {
        view.clipToOutline=true
        BicycleComponents.bindTheme(view,themes) { tokens ->
            // Restore after the generic Flat base clears decorative shadow colours.
            view.outlineAmbientShadowColor=Color.BLACK
            view.outlineSpotShadowColor=Color.BLACK
            view.background=GradientDrawable().apply {
                cornerRadius=BicycleDesign.dp(view.context,BicycleDesign.dialogRadius).toFloat()
                setColor(tokens.color(role))
                setStroke(BicycleDesign.dp(view.context,BicycleDesign.hrvPromptBorder).coerceAtLeast(1),tokens.color(role))
            }
            // Explicit alert exception: diffuse shadow in both Flat and Glow.
            view.elevation=BicycleDesign.dp(view.context,BicycleDesign.alertShadow).toFloat()
        }
    }
}

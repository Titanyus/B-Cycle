package de.bcycle.app

import android.content.SharedPreferences
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController

/** Only Android's status bar; navigation and cutout safety remain with the existing insets. */
internal object DashboardFullscreen {
    const val key="dashboardFullscreen"
    fun enabled(preferences:SharedPreferences)=preferences.getBoolean(key,true)
    fun apply(window:Window,enabled:Boolean) {
        window.insetsController?.let { controller ->
            controller.systemBarsBehavior=WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if(enabled) controller.hide(WindowInsets.Type.statusBars())
            else controller.show(WindowInsets.Type.statusBars())
        }
    }
}

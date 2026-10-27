package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.widget.LinearLayout

internal enum class RideBackupDestination { LOCAL, WEBDAV }
internal enum class RideBackupAction { EXPORT, IMPORT }

/** Two immediate commands; WebDAV availability follows configuration, not automatic-upload enablement. */
internal class BackupDestinationView(context:Context,themes:ThemeManager,
    private val cloudAvailable:()->Boolean,private val action:(RideBackupDestination)->Unit):LinearLayout(context) {
    private val prefs=context.getSharedPreferences("route-export",0)
    private val cloud=BicycleComponents.action(context,themes,context.getString(R.string.ride_backup_webdav)) { action(RideBackupDestination.WEBDAV) }.apply { tag="backup_target_webdav" }
    private val listener=SharedPreferences.OnSharedPreferenceChangeListener { _,_->refresh() }
    init {
        orientation=VERTICAL
        addView(BicycleComponents.action(context,themes,context.getString(R.string.ride_backup_local)) { action(RideBackupDestination.LOCAL) }.apply {
            tag="backup_target_local";BicycleDialogStyle.actionIcon(this,themes,TransferSymbols.local)
        },LayoutParams(-1,-2))
        BicycleDialogStyle.actionIcon(cloud,themes,TransferSymbols.webdav)
        addView(cloud,LayoutParams(-1,-2));refresh()
    }
    fun refresh() { cloud.isEnabled=cloudAvailable();cloud.alpha=if(cloud.isEnabled) 1f else .5f }
    override fun onAttachedToWindow() { super.onAttachedToWindow();prefs.registerOnSharedPreferenceChangeListener(listener);refresh() }
    override fun onDetachedFromWindow() { prefs.unregisterOnSharedPreferenceChangeListener(listener);super.onDetachedFromWindow() }
}

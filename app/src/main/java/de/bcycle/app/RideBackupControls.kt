package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.*

internal enum class RideBackupDestination { LOCAL, WEBDAV }
internal enum class RideBackupAction { EXPORT, IMPORT, SETTINGS }
internal class RideBackupControls(context:Context,private val themes:ThemeManager,
    private val cloudAvailable:()->Boolean,private val canEdit:()->Boolean,
    private val action:(RideBackupDestination,RideBackupAction)->Unit):LinearLayout(context) {
    private val prefs=context.getSharedPreferences("ride-backup",0)
    private val toggle=Switch(context).apply { tag="ride_backup_enabled";setText(R.string.ride_backup_title);isChecked=prefs.getBoolean("enabled",false) }
    private var destination=runCatching { RideBackupDestination.valueOf(prefs.getString("target","LOCAL")!!) }.getOrDefault(RideBackupDestination.LOCAL)
    private val cloud:RadioButton
    private val local:RadioButton
    private val actions=mutableListOf<View>()
    private val status=BicycleFeedback.status(context,themes).apply { tag="ride_backup_status" }
    private var working=false
    private var available=cloudAvailable()
    private val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,_->refreshConnection() }
    init {
        orientation=VERTICAL;tag="ride_backup_controls";addView(toggle)
        BicycleDialogStyle.control(toggle,themes)
        addView(BicycleComponents.description(context,themes,context.getString(R.string.ride_backup_description)))
        fun option(target:RideBackupDestination,title:Int)=RadioButton(context).apply {
            id=View.generateViewId();tag="ride_backup_"+target.name.lowercase();setText(title)
            BicycleDialogStyle.control(this,themes)
            setOnClickListener { destination=target;prefs.edit().putString("target",target.name).apply();refresh() }
        }
        local=option(RideBackupDestination.LOCAL,R.string.ride_backup_local)
        cloud=option(RideBackupDestination.WEBDAV,R.string.ride_backup_webdav)
        addView(RadioGroup(context).apply { orientation=VERTICAL;addView(local);addView(cloud) },LayoutParams(-1,-2))
        listOf(RideBackupAction.EXPORT to R.string.ride_backup_export,
            RideBackupAction.SETTINGS to R.string.ride_backup_restore_settings,
            RideBackupAction.IMPORT to R.string.ride_backup_import).forEach { (mode,title) ->
            addView(BicycleComponents.action(context,themes,context.getString(title)) {
                if(toggle.isChecked && !working && canEdit()) action(destination,mode)
            }.apply { tag="ride_backup_action_"+mode.name.lowercase();actions+=this },LayoutParams(-1,-2))
        }
        toggle.setOnCheckedChangeListener { _,on -> prefs.edit().putBoolean("enabled",on).apply();refresh() }
        addView(status,LayoutParams(-1,-2));refresh()
    }
    fun refresh() {
        cloud.isEnabled=!working && available
        local.isEnabled=!working
        if(destination==RideBackupDestination.WEBDAV && !available) destination=RideBackupDestination.LOCAL
        local.isChecked=destination==RideBackupDestination.LOCAL;cloud.isChecked=destination==RideBackupDestination.WEBDAV
        actions.forEach { it.isEnabled=toggle.isChecked && !working && canEdit() }
        toggle.isEnabled=!working
    }
    fun working(on:Boolean) { working=on;refresh() }
    fun refreshConnection() { available=cloudAvailable();refresh() }
    fun status(text:String) { BicycleFeedback.update(status,text) }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow();context.getSharedPreferences("route-export",0).registerOnSharedPreferenceChangeListener(listener);refreshConnection()
    }
    override fun onDetachedFromWindow() {
        context.getSharedPreferences("route-export",0).unregisterOnSharedPreferenceChangeListener(listener);super.onDetachedFromWindow()
    }
}

package de.bcycle.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.Gravity
import android.view.View
import android.widget.*

internal class MessageNotificationSettingsView(context: Context, private val themes: ThemeManager): LinearLayout(context) {
    private val settings=MessageNotificationSettings(context)
    private val access=BicycleFeedback.status(context,themes)
    private val apps=LinearLayout(context).apply { orientation=VERTICAL;tag="notification_apps" }
    init {
        orientation=VERTICAL
        val gap=BicycleDesign.dp(context,BicycleDesign.horizontalPadding)
        setPadding(gap,0,gap,gap)
        val enabled=Switch(context).apply {
            tag="notifications_enabled";setText(R.string.notifications_enable);isChecked=settings.enabled
            BicycleDialogStyle.control(this,themes)
            setOnCheckedChangeListener { _,enabled ->
                settings.enabled=enabled
                if(enabled && !settings.granted) requestAccess()
                refreshAccess()
            }
        }
        val details=LinearLayout(context).apply { orientation=VERTICAL;visibility=GONE;tag="notification_access_details" }
        details.addView(access,LayoutParams(-1,-2))
        details.addView(BicycleComponents.action(context,themes,context.getString(R.string.notifications_access)) { requestAccess() },LayoutParams(-1,-2))
        details.addView(BicycleComponents.description(context,themes,context.getString(R.string.notifications_privacy)),LayoutParams(-1,-2))
        addView(LinearLayout(context).apply {
            tag=BicycleDesign.inlineControlRowTag;gravity=Gravity.CENTER_VERTICAL
            enabled.gravity=Gravity.START or Gravity.CENTER_VERTICAL
            val expand=BicycleComponents.icon(context,themes,BicycleIcon.Expand,context.getString(R.string.notifications_enable)) {}.apply {
                tag="notification_access_expand";stateDescription=context.getString(R.string.zone_collapsed)
                setOnClickListener {
                    val open=details.visibility==GONE;details.visibility=if(open) VISIBLE else GONE
                    rotation=if(open) 180f else 0f
                    stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
                }
            }
            BicycleComponents.switchAccordion(this,enabled,expand,themes)
        },LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.sectionHeaderMinHeight)))
        addView(details,LayoutParams(-1,-2))
        addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.notification_position_heading)),LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.sectionContentGap) })
        addView(MessagePositionChoices(context,themes,settings.position,"notification_position") { settings.position=it },LayoutParams(-1,-2))
        val slider=BicycleSettingSlider(context,themes) { context.getString(R.string.notifications_seconds,5+it*5) }.apply {
            tag="notification_duration";max=35;progress=(settings.durationSeconds-5)/5;keyProgressIncrement=1
            contentDescription=context.getString(R.string.notifications_duration)
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,progress:Int,fromUser:Boolean) {
                    bar.stateDescription=valueText;if(fromUser) settings.durationSeconds=5+progress*5
                }
            })
        }
        addView(settingSliderRow(context,themes,context.getString(R.string.notifications_duration),slider).apply {
            tag="notification_duration_row"
            (getChildAt(0) as TextView).tag="notification_duration_label"
        },LayoutParams(-1,-2))
        BicycleForm.section(this,themes,R.string.notifications_apps)
        addView(Switch(context).apply {
            tag="notification_messenger_filter";setText(R.string.notifications_messenger_filter);isChecked=settings.messengerOnly
            BicycleDialogStyle.control(this,themes)
            setOnCheckedChangeListener { _,enabled -> settings.messengerOnly=enabled;refreshApps() }
        },LayoutParams(-1,-2))
        addView(apps,LayoutParams(-1,-2));refreshAccess();refreshApps()
    }
    private fun requestAccess() {
        try { settings.requestAccess() }
        catch(_:android.content.ActivityNotFoundException) { BicycleFeedback.show(context,R.string.notifications_access_missing,long=true) }
    }
    fun refreshAccess() {
        BicycleFeedback.update(access,context.getString(if(settings.granted) R.string.notifications_access_granted else R.string.notifications_access_required))
        if(!settings.granted) MessageNotifications.clear()
    }
    @Suppress("DEPRECATION")
    private fun refreshApps() {
        val pm=context.packageManager
        val installed=pm.getInstalledApplications(0).associateBy { it.packageName }
        val names=(installed.keys+
            MessageNotifications.observedPackages+settings.packages).distinct().filter { it!=context.packageName }
        val candidates=names.mapNotNull { name ->
            val info=installed[name] ?: runCatching { pm.getApplicationInfo(name,0) }.getOrNull() ?: return@mapNotNull null
            val messenger=MessageNotificationSettings.isMessenger(name)
            val system=info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)!=0
            // Preinstalled user-facing apps and messengers stay available; hidden system services do not.
            if(system && !messenger && pm.getLaunchIntentForPackage(name)==null) return@mapNotNull null
            if(settings.messengerOnly && !messenger) return@mapNotNull null
            name to runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(name)
        }
            .sortedBy { it.second.lowercase(java.util.Locale.getDefault()) }
        apps.removeAllViews()
        if(candidates.isEmpty()) apps.addView(BicycleComponents.text(context,themes,context.getString(R.string.notifications_no_apps)),LayoutParams(-1,-2))
        candidates.forEach { (name,label) ->
            apps.addView(LinearLayout(context).apply {
                gravity=Gravity.CENTER_VERTICAL
                addView(ImageView(context).apply {
                    setImageDrawable(runCatching { pm.getApplicationIcon(name) }.getOrNull())
                    importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },LayoutParams(BicycleDesign.dp(context,32),BicycleDesign.dp(context,32)))
                addView(CheckBox(context).apply {
                    tag="notification_app_$name";text=label;isChecked=name in settings.packages
                    BicycleDialogStyle.control(this,themes)
                    setOnCheckedChangeListener { _,checked -> settings.packages=if(checked) settings.packages+name else settings.packages-name }
                },LayoutParams(0,-2,1f).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            },LayoutParams(-1,-2))
        }
    }
}

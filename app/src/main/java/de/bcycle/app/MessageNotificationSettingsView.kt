package de.bcycle.app

import android.content.Context
import android.content.pm.PackageManager
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
        addView(Switch(context).apply {
            tag="notifications_enabled";setText(R.string.notifications_enable);isChecked=settings.enabled
            BicycleDialogStyle.control(this,themes)
            setOnCheckedChangeListener { _,enabled ->
                settings.enabled=enabled
                if(enabled && !settings.granted) requestAccess()
                refreshAccess()
            }
        },LayoutParams(-1,-2))
        addView(access,LayoutParams(-1,-2))
        addView(BicycleComponents.action(context,themes,context.getString(R.string.notifications_access)) { requestAccess() },LayoutParams(-1,-2))
        addView(BicycleComponents.text(context,themes,context.getString(R.string.notifications_privacy)),LayoutParams(-1,-2))
        BicycleForm.section(this,themes,R.string.notifications_position)
        val tiles=mutableMapOf<MessagePosition,View>()
        fun paint(tile:View,tokens:ThemeTokens) {
            tile.background=android.graphics.drawable.GradientDrawable().apply {
                cornerRadius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat()
                setColor(if(tile.isSelected) tokens.tertiary else tokens.secondary)
                setStroke(BicycleDesign.linePixels(context),BicycleComponents.selectionBorder(tokens,tile.isSelected))
            }
        }
        val positions=LinearLayout(context)
        MessagePosition.entries.forEach { position ->
            val tile=LinearLayout(context).apply {
                tag="notification_position_${position.name}";orientation=VERTICAL;gravity=Gravity.CENTER
                isClickable=true;isFocusable=true;contentDescription=context.getString(position.label)
                isSelected=settings.position==position
                stateDescription=context.getString(if(isSelected) R.string.ui_selected else R.string.ui_not_selected)
                addView(ImageView(context).apply {
                    setImageDrawable(MessagePositionPreview(position))
                    importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    BicycleComponents.bindTheme(this,themes) { setColorFilter(it.foreground) }
                },LayoutParams(BicycleDesign.dp(context,BicycleDesign.presetPreviewWidth),BicycleDesign.dp(context,BicycleDesign.presetPreviewHeight)))
                addView(BicycleComponents.text(context,themes,context.getString(position.label)).apply { gravity=Gravity.CENTER },LayoutParams(-1,-2))
                setOnClickListener {
                    settings.position=position
                    tiles.forEach { (p,t) ->
                        t.isSelected=p==position
                        t.stateDescription=context.getString(if(t.isSelected) R.string.ui_selected else R.string.ui_not_selected)
                        paint(t,themes.active.tokens)
                    }
                }
            }
            tiles[position]=tile
            BicycleComponents.bindTheme(tile,themes) { paint(tile,it) }
            positions.addView(tile,LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.presetTileHeight),1f).apply {
                setMargins(BicycleDesign.dp(context,BicycleDesign.presetTileGap),BicycleDesign.dp(context,BicycleDesign.presetTileGap),
                    BicycleDesign.dp(context,BicycleDesign.presetTileGap),BicycleDesign.dp(context,BicycleDesign.presetTileGap))
            })
        }
        addView(positions,LayoutParams(-1,-2))
        BicycleForm.section(this,themes,R.string.notifications_duration)
        val value=BicycleComponents.text(context,themes,"").apply { tag="notification_duration_value" }
        fun label(seconds: Int)=context.getString(R.string.notifications_seconds,seconds)
        value.text=label(settings.durationSeconds);addView(value,LayoutParams(-1,-2))
        addView(SeekBar(context).apply {
            tag="notification_duration";min=5;max=180;progress=settings.durationSeconds;keyProgressIncrement=1
            minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
            contentDescription=context.getString(R.string.notifications_duration);stateDescription=value.text
            BicycleComponents.bindTheme(this,themes) { t ->
                thumbTintList=android.content.res.ColorStateList.valueOf(t.accent)
                progressTintList=android.content.res.ColorStateList.valueOf(t.accent)
            }
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,progress:Int,fromUser:Boolean) {
                    if(fromUser) { settings.durationSeconds=progress;value.text=label(progress);bar.stateDescription=value.text }
                }
            })
        },LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
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
        val names=(pm.getInstalledApplications(0).map { it.packageName }+
            MessageNotifications.observedPackages+settings.packages).distinct().filter { it!=context.packageName }
        val candidates=names.filter { !settings.messengerOnly || MessageNotificationSettings.isMessenger(it) }
            .map { name -> name to runCatching { pm.getApplicationLabel(pm.getApplicationInfo(name,0)).toString() }.getOrDefault(name) }
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

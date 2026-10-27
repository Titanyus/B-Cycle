package de.bcycle.app

import android.content.Context
import android.text.InputType
import android.widget.*

/** Same native form/auto-save conventions as intervals.icu; configuration stays provider-local. */
internal class ActivityServiceView(context:Context,themes:ThemeManager,val service:ActivityService,config:ActivityServiceConfig,stored:Boolean,syncNames:()->Unit={},action:()->Unit):LinearLayout(context) {
    val enabled=Switch(context).apply { setText(R.string.service_enabled);isChecked=config.enabled;tag="service_enabled" }
    val address=EditText(context).apply { hint=context.getString(R.string.dreeve_address);setText(config.address);setSingleLine();tag="service_address";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI }
    val clientId=EditText(context).apply { hint=context.getString(R.string.strava_client_id);setText(config.clientId);setSingleLine();tag="service_client_id";inputType=InputType.TYPE_CLASS_NUMBER }
    val secret=EditText(context).apply {
        hint=context.getString(if(stored) R.string.intervals_key_saved else if(service==ActivityService.DREEVE) R.string.intervals_key else R.string.strava_client_secret)
        inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;setSingleLine();tag="service_secret";isSaveEnabled=false
        importantForAutofill=IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
    val wifi=Switch(context).apply { setText(R.string.route_export_wifi);isChecked=config.wifiOnly;tag="service_wifi" }
    val result=BicycleFeedback.status(context,themes).apply { tag="service_result" }
    val account=BicycleComponents.text(context,themes,"").apply { tag="service_account" }
    val button=BicycleComponents.action(context,themes,context.getString(if(service==ActivityService.DREEVE) R.string.route_export_test else R.string.strava_connect),action).apply { tag="service_connect" }
    val nameSync=BicycleComponents.action(context,themes,context.getString(R.string.dreeve_names_sync),syncNames).apply {
        tag="dreeve_names_sync";isEnabled=stored && config.address.isNotBlank()
    }
    val nameSyncStatus=BicycleFeedback.status(context,themes).apply { tag="dreeve_names_status" }
    init {
        orientation=VERTICAL;val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad/2,pad,pad/2)
        addView(enabled)
        if(service==ActivityService.DREEVE) addView(address) else {
            addView(BicycleComponents.text(context,themes,context.getString(R.string.strava_setup)).apply { android.text.util.Linkify.addLinks(this,android.text.util.Linkify.WEB_URLS);BicycleComponents.bindTheme(this,themes) { setLinkTextColor(it.foreground) } })
            addView(clientId)
        }
        run {
            var visible=false
            val eye=BicycleComponents.icon(context,themes,BicycleIcon.HideOnMap,context.getString(R.string.route_export_password_show)) { }
            eye.setOnClickListener {
                visible=!visible;val cursor=secret.selectionStart.coerceAtLeast(0)
                secret.inputType=InputType.TYPE_CLASS_TEXT or if(visible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD
                secret.setSelection(cursor.coerceAtMost(secret.length()))
                eye.contentDescription=context.getString(if(visible) R.string.route_export_password_hide else R.string.route_export_password_show)
                BicycleComponents.bindTheme(eye,themes) { eye.setImageDrawable(BicycleIcons.drawable(context,if(visible) BicycleIcon.ShowOnMap else BicycleIcon.HideOnMap,it.foreground)) }
            }
            addView(BicycleForm.passwordField(secret,eye,themes),LayoutParams(-1,-2))
        }
        addView(wifi);if(service==ActivityService.STRAVA) addView(account)
        addView(button);addView(result)
        if(service==ActivityService.DREEVE) {
            addView(BicycleComponents.description(context,themes,context.getString(R.string.dreeve_names_hint)))
            addView(nameSync);addView(nameSyncStatus)
            BicycleDialogStyle.actionIcon(nameSync,themes,BicycleIcon.RenameTrack)
        }
        BicycleDialogStyle.content(this,themes)
        listOf(address,clientId,secret).forEach { BicycleForm.outlinedInput(it,themes) }
        BicycleDialogStyle.actionIcon(button,themes,BicycleIcon.Connection)
        showAccount(config.account)
    }
    fun config()=ActivityServiceConfig(enabled.isChecked,wifi.isChecked,address.text.toString().trim(),clientId.text.toString().trim())
    fun newSecret()=secret.text.toString().trim().takeIf { it.isNotEmpty() }
    fun busy(value:Boolean) { button.isEnabled=!value;BicycleFeedback.update(result,if(value) context.getString(R.string.route_export_testing) else "") }
    fun showAccount(id:String) { BicycleFeedback.update(account,if(id.isEmpty()) context.getString(R.string.strava_not_connected) else context.getString(R.string.strava_account,id)) }
}

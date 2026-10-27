package de.bcycle.app

import android.content.Context
import android.text.InputType
import android.widget.*

internal class IntervalsView(context:Context,themes:ThemeManager,config:IntervalsConfig,stored:Boolean,test:()->Unit):LinearLayout(context) {
    val enabled=Switch(context).apply { setText(R.string.intervals_enabled);isChecked=config.enabled;tag="intervals_enabled" }
    val athlete=EditText(context).apply { hint=context.getString(R.string.intervals_athlete);setText(config.athlete);setSingleLine();tag="intervals_athlete";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS }
    val apiKey=EditText(context).apply {
        hint=context.getString(if(stored) R.string.intervals_key_saved else R.string.intervals_key)
        inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        setSingleLine();tag="intervals_key";isSaveEnabled=false;importantForAutofill=IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
    val wifi=Switch(context).apply { setText(R.string.route_export_wifi);isChecked=config.wifiOnly;tag="intervals_wifi" }
    val result=BicycleFeedback.status(context,themes).apply { tag="intervals_result" }
    val testButton=BicycleComponents.action(context,themes,context.getString(R.string.route_export_test),test).apply { tag="intervals_test" }
    var uploadAll:()->Unit={}
    val bulkButton=BicycleComponents.action(context,themes,context.getString(R.string.intervals_bulk_button)) { uploadAll() }.apply { tag="intervals_bulk_upload" }
    val bulkResult=BicycleFeedback.status(context,themes).apply { tag="intervals_bulk_result" }
    init {
        orientation=VERTICAL
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad/2,pad,pad/2)
        addView(enabled);addView(athlete)
        run {
            var visible=false
            val eye=BicycleComponents.icon(context,themes,BicycleIcon.HideOnMap,context.getString(R.string.route_export_password_show)) { }
            eye.tag="intervals_key_visibility"
            eye.setOnClickListener {
                visible=!visible
                val cursor=apiKey.selectionStart.coerceAtLeast(0)
                apiKey.inputType=InputType.TYPE_CLASS_TEXT or if(visible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD
                apiKey.setSelection(cursor.coerceAtMost(apiKey.length()))
                eye.contentDescription=context.getString(if(visible) R.string.route_export_password_hide else R.string.route_export_password_show)
                BicycleComponents.bindTheme(eye,themes) { tokens -> eye.setImageDrawable(BicycleIcons.drawable(context,if(visible) BicycleIcon.ShowOnMap else BicycleIcon.HideOnMap,tokens.foreground)) }
            }
            addView(BicycleForm.passwordField(apiKey,eye,themes),LayoutParams(-1,-2))
        }
        addView(wifi);addView(testButton);addView(result)
        addView(BicycleComponents.sectionDivider(context,themes,BicycleDesign.sectionDividerGap))
        addView(BicycleComponents.description(context,themes,context.getString(R.string.intervals_bulk_hint)))
        addView(bulkButton);addView(bulkResult)
        BicycleDialogStyle.content(this,themes)
        listOf(athlete,apiKey).forEach { BicycleForm.outlinedInput(it,themes) }
        BicycleDialogStyle.actionIcon(testButton,themes,BicycleIcon.Connection)
        BicycleDialogStyle.actionIcon(bulkButton,themes,TransferSymbols.intervals)
    }
    fun config()=IntervalsConfig(enabled.isChecked,athlete.text.toString().trim(),wifi.isChecked)
    fun newKey()=apiKey.text.toString().trim().takeIf { it.isNotEmpty() }
    fun testing(busy:Boolean) { testButton.isEnabled=!busy;BicycleFeedback.update(result,if(busy) context.getString(R.string.route_export_testing) else "") }
    fun bulkRunning(running:Boolean) { bulkButton.setText(if(running) R.string.intervals_bulk_cancel else R.string.intervals_bulk_button) }
}

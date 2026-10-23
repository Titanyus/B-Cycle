package de.bcycle.app

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.LinearLayout

/** Password is never view-state/autofill data; caller owns and clears its copied char array. */
internal class SettingsPasswordView(context:Context,themes:ThemeManager,create:Boolean,
    submit:(CharArray,(Result<Unit>)->Unit)->Unit):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="settings_archive_password"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        addView(BicycleComponents.description(context,themes,context.getString(if(create) R.string.backup_password_hint else R.string.backup_unlock_hint)))
        fun input(tagValue:String,hintId:Int)=BicycleComponents.input(context,themes).apply {
            tag=tagValue;hint=context.getString(hintId);contentDescription=hint;setSingleLine()
            inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSaveEnabled=false;importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            layoutParams=LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) }
        }
        val password=input("backup_password",R.string.backup_password)
        val confirmation=input("backup_password_confirm",R.string.backup_password_confirm)
        addView(password);if(create) addView(confirmation)
        lateinit var button:android.widget.Button
        button=BicycleComponents.action(context,themes,context.getString(if(create) R.string.backup_execute else R.string.backup_unlock)) {
            val value=CharArray(password.length());android.text.TextUtils.getChars(password.text,0,password.length(),value,0)
            if(value.isEmpty() || value.size>1024 || create && value.size<12) { value.fill('\u0000');password.error=context.getString(if(create) R.string.backup_password_short else R.string.backup_password_required) }
            else if(create && password.text.toString()!=confirmation.text.toString()) { value.fill('\u0000');confirmation.error=context.getString(R.string.backup_password_mismatch) }
            else {
                password.error=null;confirmation.error=null;button.isEnabled=false
                submit(value) { result -> post {
                    if(isAttachedToWindow) { button.isEnabled=true;result.fold({ password.text.clear();confirmation.text.clear() },{
                        password.error=context.getString(if(it is SettingsBackupFailure && it.problem==BackupProblem.PASSWORD) R.string.backup_password_failed else R.string.backup_io_error)
                    }) }
                } }
            }
        }.apply { tag="backup_password_submit" }
        addView(button);BicycleDialogStyle.content(this,themes)
    }
    override fun onDetachedFromWindow() {
        listOf("backup_password","backup_password_confirm").forEach { tag -> findViewWithTag<android.widget.EditText>(tag)?.text?.clear() }
        super.onDetachedFromWindow()
    }

}

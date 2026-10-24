package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Shared back affordance; titles are centred in the whole window, independent of the icon. */
internal object BicycleNavigation {
    private fun all(v: View): List<View> = listOf(v) + if(v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    fun header(original: View, screen: AlertDialog, themes: ThemeManager, help:(()->Unit)?=null, role:ThemeRole=ThemeRole.PRIMARY): View {
        val context=original.context
        val title=all(original).filterIsInstance<TextView>().firstOrNull { it.tag=="analysis_title" || it.tag=="bicycle_dialog_title" }
            ?: all(original).filterIsInstance<TextView>().firstOrNull()
        val frame=FrameLayout(context).apply {
            tag="bicycle_navigation_header"
            layoutParams=ViewGroup.LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.headerHeight))
            BicycleComponents.background(this,themes,role)
            BicycleComponents.bindTheme(this,themes) { background=BicycleInputFrame.header(this,it,role) }
        }
        if(title!=null) {
            BicycleComponents.headerTypography(title)
            (title.parent as? ViewGroup)?.removeView(title)
            title.gravity=Gravity.CENTER
            BicycleComponents.bindTheme(title,themes) {
                title.setTextColor(BicycleComponents.textColor(title,themes,it))
            }
            val main=all(screen.window!!.decorView).any { it is SettingsMenuView && it.isMainPage }
            val inset=BicycleDesign.dp(context,BicycleDesign.touchSize*(if(main && help!=null) 2 else 1)+BicycleDesign.smallGap)
            title.setPadding(inset,0,inset,0)
            frame.addView(title,FrameLayout.LayoutParams(-1,-1))
        } else {
            (original.parent as? ViewGroup)?.removeView(original)
            frame.addView(original,FrameLayout.LayoutParams(-1,-1))
        }
        frame.addView(BicycleComponents.icon(context,themes,BicycleIcon.Back,context.getString(R.string.ui_back)) { screen.dismiss() }.apply { tag="dialog_back" },
            FrameLayout.LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize),Gravity.START or Gravity.CENTER_VERTICAL))
        if(themes.isSettingsPalette && all(screen.window!!.decorView).any { it is SettingsMenuView && it.isMainPage }) {
            val toggle=BicycleComponents.icon(context,themes,BicycleIcon.Moon,context.getString(R.string.settings_dark_mode)) {
                themes.setSettingsLight(!themes.settingsLight)
            }.apply { tag="settings_light_toggle" }
            BicycleComponents.bindTheme(toggle,themes) { tokens ->
                val light=themes.settingsLight
                toggle.setImageDrawable(BicycleIcons.drawable(context,if(light) BicycleIcon.Sun else BicycleIcon.Moon,tokens.foreground))
                toggle.contentDescription=context.getString(if(light) R.string.settings_light_mode else R.string.settings_dark_mode)
            }
            frame.addView(toggle,FrameLayout.LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize),Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd=if(help!=null) BicycleDesign.dp(context,BicycleDesign.touchSize) else 0
            })
        }
        help?.let { open ->
            frame.addView(BicycleComponents.icon(context,themes,BicycleIcon.Info,context.getString(R.string.settings_help_open)) { open() }.apply { tag="settings_help" },
                FrameLayout.LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize),Gravity.END or Gravity.CENTER_VERTICAL))
        }
        return frame
    }
    fun hideNavigationFooter(screen: AlertDialog) {
        val labels=listOf(R.string.route_cancel,R.string.route_action_save,R.string.analysis_close,R.string.analysis_map_close,R.string.graph_apply)
            .map(screen.context::getString).toSet()+setOf("Speichern","Abbrechen","Schließen")
        listOf(AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEUTRAL).forEach { which ->
            screen.getButton(which)?.takeIf { it.text.toString() in labels }?.visibility=View.GONE
        }
        val panel=screen.window?.decorView?.findViewById<ViewGroup>(screen.context.resources.getIdentifier("buttonPanel","id","android"))
        if(panel!=null && all(panel).filterIsInstance<Button>().none { it.visibility==View.VISIBLE }) panel.visibility=View.GONE
    }
    fun dateHeader(picker: DatePicker, themes: ThemeManager) {
        for((name,size) in listOf("date_picker_header_year" to 18f,"date_picker_header_date" to 28f)) {
            val id=picker.resources.getIdentifier(name,"id","android")
            picker.findViewById<TextView>(id)?.let { label ->
                label.textSize=size; label.typeface=if(name.endsWith("year")) BicycleDesign.bodyTypeface else BicycleDesign.strongTypeface
                BicycleComponents.bindTheme(label,themes) { label.setTextColor(android.graphics.Color.WHITE) }
                (label.parent as? View)?.let { parent -> BicycleComponents.bindTheme(parent,themes) { parent.setBackgroundColor(it.primary) } }
            }
        }
    }
}

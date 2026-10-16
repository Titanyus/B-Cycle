package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton

/** One display source per tile, constrained by its measurement family. */
internal class ZoneGlowPicker(context: Context, private val themes: ThemeManager, initial: ZoneGlowSource, field:DashboardField?=null) : LinearLayout(context) {
    var selected = initial; private set
    var changed: () -> Unit = {}
    private val content = LinearLayout(context).apply { orientation = VERTICAL; visibility = GONE; tag = "zone_glow_content" }
    init {
        orientation = VERTICAL; tag = "zone_glow_picker"
        lateinit var toggle: android.widget.Button
        toggle = BicycleComponents.action(context, themes, context.getString(R.string.zone_visualization)) {
            content.visibility = if (content.visibility == VISIBLE) GONE else VISIBLE
            toggle.stateDescription=context.getString(if(content.visibility==VISIBLE) R.string.zone_expanded else R.string.zone_collapsed)
        }.apply { tag = "zone_glow_toggle";isAccessibilityHeading=true;stateDescription=context.getString(R.string.zone_collapsed) }
        BicycleDialogStyle.accordion(toggle,themes)
        BicycleDialogStyle.actionIcon(toggle, themes, BicycleIcon.Expand)
        addView(toggle)
        addView(content)
        restrictTo(field)
    }
    fun restrictTo(field:DashboardField?) {
        val allowed=ZoneGlowCompatibility.allowed(field)
        val previous=selected
        if(selected !in allowed) selected=ZoneGlowSource.OFF
        content.removeAllViews()
        val buttons = mutableListOf<RadioButton>()
        allowed.forEach { source ->
            val button = RadioButton(context).apply {
                text = context.getString(source.label); tag = "zone_glow_${source.name}"; isChecked = selected == source
                BicycleDialogStyle.control(this, themes)
                setOnClickListener {
                    selected = source; buttons.forEach { it.isChecked = it.tag == "zone_glow_${source.name}" }; changed()
                }
            }
            buttons += button; content.addView(button)
        }
        if(previous!=selected) changed()
    }
}

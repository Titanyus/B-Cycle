package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton

/** One independent display source per tile. No numerical tuning controls. */
internal class ZoneGlowPicker(context: Context, themes: ThemeManager, initial: ZoneGlowSource) : LinearLayout(context) {
    var selected = initial; private set
    var changed: () -> Unit = {}
    init {
        orientation = VERTICAL; tag = "zone_glow_picker"
        val content = LinearLayout(context).apply { orientation = VERTICAL; visibility = GONE; tag = "zone_glow_content" }
        lateinit var toggle: android.widget.Button
        toggle = BicycleComponents.action(context, themes, context.getString(R.string.zone_visualization)) {
            content.visibility = if (content.visibility == VISIBLE) GONE else VISIBLE
            toggle.stateDescription=context.getString(if(content.visibility==VISIBLE) R.string.zone_expanded else R.string.zone_collapsed)
        }.apply { tag = "zone_glow_toggle";stateDescription=context.getString(R.string.zone_collapsed) }
        BicycleDialogStyle.actionIcon(toggle, themes, BicycleIcon.Expand)
        addView(toggle)
        val buttons = mutableListOf<RadioButton>()
        ZoneGlowSource.entries.forEach { source ->
            val button = RadioButton(context).apply {
                text = context.getString(source.label); tag = "zone_glow_${source.name}"; isChecked = initial == source
                BicycleDialogStyle.control(this, themes)
                setOnClickListener {
                    selected = source; buttons.forEach { it.isChecked = it.tag == "zone_glow_${source.name}" }; changed()
                }
            }
            buttons += button; content.addView(button)
        }
        addView(content)
    }
}

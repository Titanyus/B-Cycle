package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast

/** Shared presentation only: callers retain their existing success/error/loading decisions. */
internal object BicycleFeedback {
    fun show(context: Context, text: CharSequence, long: Boolean = true) {
        Toast.makeText(context, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
    fun show(context: Context, resource: Int, long: Boolean = true) = show(context, context.getString(resource), long)

    fun status(context: Context, themes: ThemeManager, text: String = "") =
        BicycleComponents.text(context, themes, text).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }

    fun update(view: TextView, text: CharSequence?, hideWhenEmpty: Boolean = false) {
        val next = text ?: ""
        if (view.text.toString() != next.toString()) view.text = next
        if (hideWhenEmpty) view.visibility = if (next.isEmpty()) View.GONE else View.VISIBLE
    }
}

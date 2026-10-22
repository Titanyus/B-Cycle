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
            addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view:View) {
                    val row=view.parent as? android.widget.LinearLayout ?: return
                    val index=row.indexOfChild(view)
                    val previous=(index-1 downTo 0).map(row::getChildAt).firstOrNull { it.visibility!=View.GONE }
                    if(row.orientation==android.widget.LinearLayout.VERTICAL && previous is android.widget.EditText) {
                        val margins=view.layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return
                        margins.topMargin=BicycleDesign.dp(context,BicycleDesign.fieldFeedbackGap)
                        view.requestLayout()
                    }
                }
                override fun onViewDetachedFromWindow(view:View)=Unit
            })
        }

    fun update(view: TextView, text: CharSequence?, hideWhenEmpty: Boolean = false) {
        val next = text ?: ""
        if (view.text.toString() != next.toString()) view.text = next
        if (hideWhenEmpty) view.visibility = if (next.isEmpty()) View.GONE else View.VISIBLE
    }
}

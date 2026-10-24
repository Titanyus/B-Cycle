package de.bcycle.app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView

/** Opt-in category pinning, preserving the native ScrollView and its gestures/focus/scroll state.
 * Header positions are cached on layout, never searched during scrolling. No duplicate heading,
 * overlay container, nested scrolling or content reparenting is needed.
 */
internal open class BicycleStickyScrollView(context: Context) : ScrollView(context) {
    private data class Section(val header: BicycleSectionHeader, val top: Int, val end: Int)
    private var sections = emptyList<Section>()
    init { BicycleComponents.scrollStyle(this) }
    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        val saved = canvas.save()
        // ScrollView draws its children in content coordinates. Clip to the actual viewport,
        // including its scroll offset, so raised headings cannot cover the fixed window header.
        canvas.clipRect(scrollX, scrollY, scrollX + width, scrollY + height)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(saved)
    }
    /** View overlays draw after raised headers; field halos must leave these opaque areas untouched. */
    internal fun clipInputGlow(canvas:android.graphics.Canvas) {
        sections.forEach { (header,top,_) ->
            if(header.visibility==VISIBLE) {
                val y=top+header.translationY-scrollY
                canvas.clipOutRect(0f,y,width.toFloat(),y+header.height)
            }
        }
    }
    private fun topInContent(view: View): Int {
        var top = view.top
        var parent = view.parent
        while (parent is View && parent !== this) { top += parent.top; parent = parent.parent }
        return top
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        if(childCount>0) { BicycleForm.accordions(getChildAt(0));BicycleContentSpacing.sections(getChildAt(0)) }
        super.onMeasure(widthMeasureSpec,heightMeasureSpec)
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        fun collect(view: View): List<BicycleSectionHeader> {
            if (view.visibility != VISIBLE) return emptyList()
            if (view is BicycleSectionHeader) return listOf(view)
            return if (view is ViewGroup) (0 until view.childCount).flatMap { collect(view.getChildAt(it)) } else emptyList()
        }
        sections.forEach { it.header.translationY = 0f; it.header.translationZ = 0f }
        val headers = if (childCount > 0) collect(getChildAt(0)).sortedBy(::topInContent) else emptyList()
        sections = headers.mapIndexed { index, header ->
            val end = minOf(headers.getOrNull(index + 1)?.let(::topInContent) ?: (getChildAt(0).bottom),
                header.sectionEnd?.let(::topInContent) ?: Int.MAX_VALUE)
            Section(header, topInContent(header), end)
        }
        pin()
    }
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt); pin()
    }
    private fun pin() {
        sections.forEach { (header, top, end) ->
            val active = scrollY >= top && scrollY < end
            header.translationY = if (active) minOf(scrollY - top, end - top - header.height).coerceAtLeast(0).toFloat() else 0f
            // Native buttons have their own elevation, including a pressed-state animation.
            // Keep the opaque category above them without adding a shadow.
            header.translationZ = if (active) BicycleDesign.dp(context, BicycleDesign.sectionHeaderRaisedZ).toFloat() else 0f
        }
    }
}

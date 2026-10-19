package de.bcycle.app

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.ListView

/** Spacing belongs to content, never to a pinned heading. Reapplying is idempotent. */
internal object BicycleContentSpacing {
    fun page(view:View) {
        if(view is RouteManagerView) { view.applyPageSpacing();return }
        val scrolling=view
        val body=when(scrolling) { is ScrollView->scrolling.getChildAt(0) ?: view;is ListView->scrolling;else->view }
        // Remove outer wrapper insets on the path to the actual scrolling content.
        var parent=body.parent
        while(parent is View && parent!==view.parent) {
            if(parent.paddingTop!=0) parent.setPadding(parent.paddingLeft,0,parent.paddingRight,parent.paddingBottom)
            if(parent===view) break
            parent=parent.parent
        }
        val first=(body as? ViewGroup)?.let { (0 until it.childCount).map(it::getChildAt).firstOrNull { child->child.visibility!=View.GONE } }
        if(first is android.widget.Button || first is android.widget.Spinner) BicycleComponents.controlSpacing(first)
        val margin=first?.layoutParams as? ViewGroup.MarginLayoutParams
        val maximum=BicycleDesign.dp(body.context,BicycleDesign.pageContentGap)
        if(margin!=null && margin.topMargin>maximum) { margin.topMargin=maximum;first.layoutParams=margin }
        val top=if(first is BicycleSectionHeader) 0 else (maximum-(margin?.topMargin ?: 0)).coerceAtLeast(0)
        body.setPadding(body.paddingLeft,top,body.paddingRight,body.paddingBottom)
        sections(body)
    }
    fun sections(view:View) {
        descendants(view).filterIsInstance<LinearLayout>().forEach { parent ->
            val visible=(0 until parent.childCount).map(parent::getChildAt).filter { it.visibility!=View.GONE }
            visible.forEachIndexed { index,child -> if(child is BicycleSectionHeader) {
                val lp=child.layoutParams as? ViewGroup.MarginLayoutParams
                if(lp!=null && (lp.topMargin!=0 || lp.bottomMargin!=0)) { lp.topMargin=0;lp.bottomMargin=0;child.layoutParams=lp }
                fun gap(v:View?,before:Boolean) {
                    val margins=v?.layoutParams as? ViewGroup.MarginLayoutParams ?: return
                    val size=BicycleDesign.dp(v.context,if(before) BicycleDesign.sectionEndGap else BicycleDesign.sectionContentGap)
                    if(before && margins.bottomMargin!=size) { margins.bottomMargin=size;v.layoutParams=margins }
                    if(!before && margins.topMargin!=size) { margins.topMargin=size;v.layoutParams=margins }
                }
                gap(previousContent(child,view)?.takeUnless { it is BicycleSectionHeader },true)
                gap(visible.getOrNull(index+1)?.takeUnless { it is BicycleSectionHeader },false)
            } }
        }
    }
    /** A section can start inside a wrapper, e.g. FIT after the complete sensor list. */
    private fun previousContent(header:View,root:View):View? {
        var current=header
        while(current!==root) {
            val parent=current.parent as? LinearLayout ?: return null
            if(parent.orientation!=LinearLayout.VERTICAL) return null
            val index=parent.indexOfChild(current)
            for(i in index-1 downTo 0) {
                val candidate=parent.getChildAt(i)
                if(candidate.visibility!=View.GONE) return candidate
            }
            current=parent
        }
        return null
    }
    private fun descendants(v:View):Sequence<View> = sequence {
        yield(v);if(v is ViewGroup) for(i in 0 until v.childCount) yieldAll(descendants(v.getChildAt(i)))
    }
}

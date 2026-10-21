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
        // Only the page supplies the leading gap, even behind nested wrappers or
        // when forms are inserted after the dialog was shown.
        val path=mutableListOf<View>(body)
        fun first(view:View)=(view as? ViewGroup)?.let { group ->
            (0 until group.childCount).map(group::getChildAt).firstOrNull { it.visibility!=View.GONE }
        }
        fun wrapper(view:View)=view !is BicycleSectionHeader && view !is ProfileAccordion &&
            (view is ScrollView || view is LinearLayout && view.orientation==LinearLayout.VERTICAL ||
                view.javaClass==android.widget.FrameLayout::class.java && (view as ViewGroup).childCount==1)
        var current=body
        while(wrapper(current)) {
            val next=first(current) ?: break
            path+=next
            if(!wrapper(next)) break
            current=next
        }
        val leading=path.last()
        path.forEach { view ->
            val margin=view.layoutParams as? ViewGroup.MarginLayoutParams
            if(margin!=null && margin.topMargin!=0) { margin.topMargin=0;view.layoutParams=margin }
            if(view!==body && (wrapper(view) || view is ListView) && view.paddingTop!=0)
                view.setPadding(view.paddingLeft,0,view.paddingRight,view.paddingBottom)
        }
        val top=if(leading is BicycleSectionHeader) 0 else BicycleDesign.dp(body.context,BicycleDesign.pageContentGap)
        if(body.paddingTop!=top) body.setPadding(body.paddingLeft,top,body.paddingRight,body.paddingBottom)
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

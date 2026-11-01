package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

/** One native scroll surface: the day stays until the incoming history reaches its lower edge. */
internal class HrvEvaluationScrollView(context:Context,private val day:View,details:View):ScrollView(context) {
    private val gap=BicycleDesign.dp(context,BicycleDesign.sectionCardGap)
    private val spacer=View(context)
    private val content=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL }
    private val details=details
    val showingDetails get()=scrollY>=details.top-BicycleDesign.dp(context,BicycleDesign.horizontalPadding)
    var onSectionChanged:()->Unit={}
    init {
        tag="hrv_evaluation_scroll";BicycleComponents.scrollStyle(this)
        content.addView(day,LinearLayout.LayoutParams(-1,-2))
        content.addView(spacer,LinearLayout.LayoutParams(-1,gap))
        content.addView(details,LinearLayout.LayoutParams(-1,-2))
        addView(content)
    }
    override fun onMeasure(w:Int,h:Int) {
        super.onMeasure(w,h)
        val staging=maxOf(gap,measuredHeight-paddingTop-paddingBottom-day.measuredHeight+gap)
        if(spacer.layoutParams.height!=staging) {
            spacer.layoutParams.height=staging
            super.onMeasure(w,h)
        }
    }
    override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int) {
        super.onLayout(changed,l,t,r,b);pin()
    }
    override fun onScrollChanged(l:Int,t:Int,oldl:Int,oldt:Int) {
        super.onScrollChanged(l,t,oldl,oldt);pin();onSectionChanged()
    }
    private fun pin() { day.translationY=scrollY.coerceIn(0,(spacer.height-gap).coerceAtLeast(0)).toFloat() }
    fun showDetails() {
        // Keep the same inset as a direct history visit; a short detail screen may still align at top.
        val target=(details.top-BicycleDesign.dp(context,BicycleDesign.horizontalPadding)).coerceAtLeast(0)
        val needed=(height-(content.height-target)).coerceAtLeast(0)
        if(content.paddingBottom!=needed) { content.setPadding(0,0,0,needed);content.requestLayout();post { smoothScrollTo(0,target) } }
        else smoothScrollTo(0,target)
    }
}

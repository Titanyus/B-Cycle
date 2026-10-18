package de.bcycle.app

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView

/** Stable-height context actions; a small hysteresis prevents flicker near the switching point. */
internal class ProfileContextFooter(context:Context,themes:ThemeManager,private val profileActions:View):FrameLayout(context) {
    private var dashboardActions:View?=null
    var dashboardMode=false;private set
    private var transitioning=false
    init {
        tag="bicycle_action_footer"
        minimumHeight=BicycleDesign.dp(context,BicycleDesign.footerHeight)
        styleBicycleFooter(this,themes)
        addView(profileActions,LayoutParams(-1,-1))
    }
    fun bindDashboard(actions:View?) {
        if(actions===dashboardActions) return
        finishAnimations()
        dashboardActions?.let(::removeView)
        dashboardActions=actions
        actions?.let { addView(it,LayoutParams(-1,-1)) }
        if(actions==null) dashboardMode=false
        showImmediately()
    }
    fun update(scroll:ScrollView,header:BicycleSectionHeader?) {
        if(scroll.height<=0) return
        var y=header?.top ?: 0
        var parent=header?.parent
        while(parent is View && parent!==scroll) { y+=parent.top;parent=parent.parent }
        val relative=y-scroll.scrollY
        val margin=if(dashboardMode) BicycleDesign.dp(context,BicycleDesign.modeFooterHysteresis) else 0
        val dashboard=header!=null && header.expanded!=false && dashboardActions!=null && scroll.scrollY>0 && relative<=scroll.height/2+margin
        if(dashboard==dashboardMode) return
        finishAnimations()
        val outgoing=if(dashboardMode) dashboardActions else profileActions
        dashboardMode=dashboard
        val incoming=if(dashboard) dashboardActions!! else profileActions
        val offset=BicycleDesign.dp(context,BicycleDesign.controlGap).toFloat()
        transitioning=true
        outgoing?.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        incoming.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        incoming.visibility=VISIBLE;incoming.alpha=0f;incoming.translationY=offset
        outgoing?.animate()?.alpha(0f)?.translationY(-offset)?.setDuration(BicycleDesign.modeFooterTransitionMs)?.withEndAction { outgoing.visibility=GONE }?.start()
        incoming.animate().alpha(1f).translationY(0f).setDuration(BicycleDesign.modeFooterTransitionMs).withEndAction {
            transitioning=false;showImmediately()
        }.start()
    }
    private fun showImmediately() {
        profileActions.visibility=if(dashboardMode) GONE else VISIBLE
        dashboardActions?.visibility=if(dashboardMode) VISIBLE else GONE
        listOfNotNull(profileActions,dashboardActions).forEach {
            it.alpha=1f;it.translationY=0f;it.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_AUTO
        }
    }
    private fun finishAnimations() {
        profileActions.animate().withEndAction(null).cancel()
        dashboardActions?.animate()?.withEndAction(null)?.cancel()
        transitioning=false;showImmediately()
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        super.onMeasure(widthMeasureSpec,MeasureSpec.makeMeasureSpec(BicycleDesign.dp(context,BicycleDesign.footerHeight),MeasureSpec.EXACTLY))
    }
    override fun dispatchTouchEvent(event:MotionEvent):Boolean = if(transitioning) true else super.dispatchTouchEvent(event)
    override fun onDetachedFromWindow() { finishAnimations();super.onDetachedFromWindow() }
}

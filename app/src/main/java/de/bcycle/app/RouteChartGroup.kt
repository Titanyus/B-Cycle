package de.bcycle.app

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.LinearLayout
import kotlin.math.abs

/** One touch target for all available ride plots, including the spaces between them. */
internal class RouteChartGroup(context:Context,private val navigation:RouteChartNavigation):LinearLayout(context) {
    private var gestureChart:RouteChartView?=null
    private var downX=0f;private var downY=0f
    private val slop=ViewConfiguration.get(context).scaledTouchSlop
    init { orientation=VERTICAL;isBaselineAligned=false;tag="analysis_chart_group" }
    override fun onInterceptTouchEvent(event:MotionEvent)=true
    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN) {
            val charts=(0 until childCount).mapNotNull { getChildAt(it) as? RouteChartView }
            gestureChart=charts.firstOrNull { event.y<it.bottom } ?: charts.lastOrNull()
            downX=event.x;downY=event.y
        }
        val chart=gestureChart ?: return false
        // A vertical one-finger drag still belongs to the surrounding page, even when zoomed.
        if(event.actionMasked==MotionEvent.ACTION_MOVE && event.pointerCount==1 &&
            abs(event.y-downY)>slop && abs(event.y-downY)>abs(event.x-downX))
            parent?.requestDisallowInterceptTouchEvent(false)
        val local=MotionEvent.obtain(event)
        val handled=try {
            local.offsetLocation(-chart.left.toFloat(),0f)
            navigation.touch(chart,local)
        } finally { local.recycle() }
        if(event.actionMasked==MotionEvent.ACTION_DOWN && navigation.viewport.zoomed) parent?.requestDisallowInterceptTouchEvent(true)
        if(event.actionMasked==MotionEvent.ACTION_UP || event.actionMasked==MotionEvent.ACTION_CANCEL) gestureChart=null
        return handled
    }
}

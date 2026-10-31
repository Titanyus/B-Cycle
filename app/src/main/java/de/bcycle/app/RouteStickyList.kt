package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.widget.SectionIndexer

/** Draw a cached group header above scrolling rides, below the fast-scroll controls. */
internal class RouteStickyList(context:Context,themes:ThemeManager,
    private val groupAt:(Int)->RouteListGroup?):BicycleFastScrollList(context,themes) {
    private lateinit var pinned:RouteListGroupHeader
    internal var pinnedGroup:RouteListGroup?=null
        private set
    internal var pinnedOffset=0
        private set
    internal val pinnedHeight get()=pinned.height
    private var measuredWidth=-1
    private var overlayGesture=false
    init {
        // The cached drawing view is detached, so refresh it with this attached list's theme.
        BicycleComponents.bindTheme(this,themes) {
            pinned=RouteListGroupHeader(context,themes).apply {
                BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
            }
            measuredWidth=-1;invalidate()
        }
    }
    override fun drawContentOverlay(canvas:Canvas) {
        pinnedGroup=null
        if(childCount==0 || count==0) return
        val indexer=adapter as? SectionIndexer ?: return
        val section=indexer.getSectionForPosition(firstVisiblePosition)
        val start=indexer.getPositionForSection(section)
        val header=getChildAt(start-firstVisiblePosition)
        // At entry, retain the page gap; only pin once the real header reaches the top.
        if(header!=null && header.top>=0) return
        val group=groupAt(start) ?: return
        val availableWidth=width-paddingLeft-paddingRight
        if(availableWidth<=0) return
        if(pinnedGroupValue!=group || measuredWidth!=availableWidth) {
            pinned.bind(group);pinnedGroupValue=group;measuredWidth=availableWidth
            pinned.measure(View.MeasureSpec.makeMeasureSpec(availableWidth,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
            pinned.layout(0,0,availableWidth,pinned.measuredHeight)
        }
        val next=if(section+1<indexer.sections.size) indexer.getPositionForSection(section+1) else -1
        val nextHeader=if(next>=firstVisiblePosition) getChildAt(next-firstVisiblePosition) else null
        pinnedOffset=minOf(0,(nextHeader?.top ?: Int.MAX_VALUE)-pinned.height)
        pinnedGroup=group
        val save=canvas.save()
        canvas.clipRect(paddingLeft,0,width-paddingRight,height)
        canvas.translate(paddingLeft.toFloat(),pinnedOffset.toFloat());pinned.draw(canvas)
        canvas.restoreToCount(save)
    }
    private var pinnedGroupValue:RouteListGroup?=null
    override fun dispatchTouchEvent(event:MotionEvent):Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN) {
            val edge=BicycleDesign.dp(context,BicycleDesign.fastScrollTouchWidth.toInt())
            overlayGesture=pinnedGroup!=null && event.y>=0 && event.y<pinnedOffset+pinnedHeight &&
                event.x>=paddingLeft && event.x<width-maxOf(paddingRight,edge)
        }
        // Let native ListView handle pan/fling, without dispatching taps to a covered ride.
        val handled=if(overlayGesture) onTouchEvent(event) else super.dispatchTouchEvent(event)
        if(event.actionMasked==MotionEvent.ACTION_UP || event.actionMasked==MotionEvent.ACTION_CANCEL) overlayGesture=false
        return handled
    }
}

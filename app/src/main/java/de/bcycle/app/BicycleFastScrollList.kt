package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextUtils
import android.view.MotionEvent
import android.widget.HeaderViewListAdapter
import android.widget.ListView
import android.widget.SectionIndexer

/** Shared fast navigation with public ListView APIs and a compact theme-aware preview.
 * ListView retains normal scrolling, recycling, accessibility and item selection.
 */
internal class BicycleFastScrollList(context:Context,themes:ThemeManager):ListView(context) {
    private var tokens=themes.active.tokens
    private val paint=android.text.TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val thumb=RectF()
    internal val previewBounds=RectF()
    private var dragging=false
    private var dragFraction=0f
    private var dragPosition=0
    init {
        isFastScrollEnabled=false;isVerticalScrollBarEnabled=false
        BicycleComponents.scrollStyle(this)
        BicycleComponents.bindTheme(this,themes) { tokens=it;invalidate() }
    }
    private val rows get()=(adapter?.count ?: 0)-headerViewsCount-footerViewsCount
    private fun dp(value:Float)=value*resources.displayMetrics.density
    private fun available()=rows>childCount && childCount>0 && height>dp(64f)
    private fun fraction():Float {
        if(dragging) return dragFraction
        val visible=(lastVisiblePosition-firstVisiblePosition+1).coerceAtLeast(1)
        val maximum=(rows-visible).coerceAtLeast(1)
        val firstChild=getChildAt(0)
        val partial=if(firstChild!=null && firstChild.height>0) (paddingTop-firstChild.top).toFloat()/firstChild.height else 0f
        return ((firstVisiblePosition-headerViewsCount+partial)/maximum).coerceIn(0f,1f)
    }
    private fun track() {
        val top=paddingTop+dp(4f);val bottom=height-paddingBottom-dp(4f)
        val thumbHeight=dp(BicycleDesign.fastScrollThumbHeight).coerceAtMost(bottom-top)
        val y=top+(bottom-top-thumbHeight)*fraction()
        thumb.set(width-dp(8f),y,width-dp(4f),y+thumbHeight)
    }
    private fun label():String {
        val source=(adapter as? HeaderViewListAdapter)?.wrappedAdapter ?: adapter
        val indexer=source as? SectionIndexer
        val position=if(dragging) dragPosition else (firstVisiblePosition-headerViewsCount).coerceAtLeast(0)
        val sections=indexer?.sections
        if(sections!=null && sections.isNotEmpty()) return sections[indexer.getSectionForPosition(position).coerceIn(0,sections.lastIndex)].toString()
        return "${position+1} / $rows"
    }
    override fun dispatchDraw(canvas:Canvas) {
        super.dispatchDraw(canvas)
        if(!available()) return
        track()
        paint.style=Paint.Style.FILL;paint.color=if(dragging) tokens.primary else tokens.foreground
        canvas.drawRoundRect(thumb,dp(2f),dp(2f),paint)
        if(!dragging) return
        paint.textSize=BicycleDesign.bodySp*resources.displayMetrics.scaledDensity
        paint.typeface=BicycleDesign.bodyTypeface;paint.textAlign=Paint.Align.LEFT
        val maximum=minOf(dp(BicycleDesign.fastScrollPreviewMaxWidth),width-dp(56f)).coerceAtLeast(1f)
        val padding=dp(BicycleDesign.fastScrollPreviewPadding)
        val text=TextUtils.ellipsize(label(),paint,(maximum-padding*2).coerceAtLeast(1f),TextUtils.TruncateAt.END).toString()
        val boxWidth=(paint.measureText(text)+padding*2).coerceIn(minOf(dp(48f),maximum),maximum)
        val boxHeight=dp(BicycleDesign.fastScrollPreviewHeight)
        val x=thumb.left-dp(8f)-boxWidth
        val y=(thumb.centerY()-boxHeight/2).coerceIn(0f,(height-boxHeight).coerceAtLeast(0f))
        previewBounds.set(x,y,x+boxWidth,y+boxHeight)
        paint.color=tokens.foreground;canvas.drawRoundRect(previewBounds,dp(BicycleDesign.inputRadius.toFloat()),dp(BicycleDesign.inputRadius.toFloat()),paint)
        paint.color=tokens.secondary
        canvas.drawText(text,x+padding,y+boxHeight/2-(paint.ascent()+paint.descent())/2,paint)
    }
    private fun hit(event:MotionEvent)=event.x>=width-dp(BicycleDesign.fastScrollTouchWidth) && available()
    override fun onInterceptTouchEvent(event:MotionEvent):Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN && hit(event)) return true
        return super.onInterceptTouchEvent(event)
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN && hit(event)) {
            val cancel=MotionEvent.obtain(event).apply { action=MotionEvent.ACTION_CANCEL }
            super.onTouchEvent(cancel);cancel.recycle()
            dragging=true;parent?.requestDisallowInterceptTouchEvent(true)
        }
        if(!dragging) return super.onTouchEvent(event)
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{
                val top=paddingTop+dp(4f);val height=(height-paddingBottom-dp(4f)-top-dp(BicycleDesign.fastScrollThumbHeight)).coerceAtLeast(1f)
                dragFraction=((event.y-top-dp(BicycleDesign.fastScrollThumbHeight)/2)/height).coerceIn(0f,1f)
                dragPosition=(dragFraction*(rows-1).coerceAtLeast(0)).toInt()
                setSelectionFromTop(dragPosition+headerViewsCount,0);invalidate()
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{
                dragging=false;previewBounds.setEmpty();parent?.requestDisallowInterceptTouchEvent(false);invalidate()
                if(event.actionMasked==MotionEvent.ACTION_UP) performClick()
            }
        }
        return true
    }
    override fun performClick():Boolean { super.performClick();return true }
    override fun onDetachedFromWindow() { dragging=false;previewBounds.setEmpty();super.onDetachedFromWindow() }
}

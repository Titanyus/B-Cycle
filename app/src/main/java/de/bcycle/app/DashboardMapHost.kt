package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView

/** One permanent parent for the GL surface. Page slots supply geometry, never GL ownership. */
internal class DashboardMapHost(context:Context,val map:MapPanel):FrameLayout(context) {
    internal class Slot(context:Context,private val owner:DashboardMapHost):ImageView(context) {
        init { scaleType=ScaleType.FIT_XY;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
        override fun draw(canvas:Canvas) {
            if(!canvas.isHardwareAccelerated && owner.active===this) owner.map.draw(canvas)
            else super.draw(canvas)
        }
    }
    var active:Slot?=null;private set
    init { tag="dashboard_map_host";clipChildren=true;addView(map,LayoutParams(-1,-1)) }
    fun slot()=Slot(context,this)
    fun select(slot:Slot) { active=slot;requestLayout();sync() }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        val slot=active
        super.onMeasure(MeasureSpec.makeMeasureSpec(slot?.measuredWidth ?: 0,MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(slot?.measuredHeight ?: 0,MeasureSpec.EXACTLY))
    }
    /** Called on layout AND every drag/animation frame; hardware translations need no new draw pass. */
    fun sync() {
        val slot=active ?: return
        val boundary=parent as? ViewGroup ?: return
        var node:View=slot;var x=0f;var y=0f
        while(node!==boundary) {
            if(node.visibility!=VISIBLE) { visibility=INVISIBLE;return }
            x+=node.left+node.translationX;y+=node.top+node.translationY
            node=node.parent as? View ?: run { visibility=INVISIBLE;return }
        }
        if(slot.width<=0 || slot.height<=0) { requestLayout();return }
        visibility=VISIBLE
        if(measuredWidth!=slot.width || measuredHeight!=slot.height)
            measure(MeasureSpec.makeMeasureSpec(slot.width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(slot.height,MeasureSpec.EXACTLY))
        val top=y.toInt()
        if(left!=0 || this.top!=top || width!=slot.width || height!=slot.height) layout(0,top,slot.width,top+slot.height)
        translationX=x;translationY=y-top
    }
}

package de.bcycle.app

import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.DragEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

/** Display-only insertion preview; persist once on DROP, never during hover. */
internal class TileReorder(
    private val scroller: HorizontalScrollView, private val row: LinearLayout,
    private val themes: ThemeManager, private val ids: () -> List<Int>,
    private val locked: () -> Boolean = { false }, private val move: (Int, Int) -> Boolean
) {
    internal data class Payload(val owner: TileReorder, val id: Int)
    private var source: Int? = null
    private var target = -1
    private var pointer = 0f
    private var direction = 0
    private val cards get() = (ids().indices).mapNotNull { row.getChildAt(it) }
    private val edge = object : Runnable {
        override fun run() {
            if (source == null || direction == 0 || locked()) return
            val before = scroller.scrollX
            scroller.scrollBy(BicycleDesign.dp(row.context, 4) * direction, 0)
            pointer += scroller.scrollX - before
            preview(nearest(pointer))
            row.postDelayed(this, 16)
        }
    }
    init {
        row.setOnDragListener { _, event ->
            val payload = event.localState as? Payload
            if (payload?.owner !== this) return@setOnDragListener false
            if (event.action == DragEvent.ACTION_DRAG_ENDED) {
                clear();return@setOnDragListener true
            }
            if (locked()) { clear();return@setOnDragListener false }
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    if (ids().size < 2 || payload.id !in ids()) false
                    else { source=payload.id;preview(ids().indexOf(payload.id));true }
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    pointer=event.x;preview(nearest(pointer))
                    val x=event.x-scroller.scrollX
                    val margin=BicycleDesign.dp(row.context,32)
                    val next=when { x<margin -> -1; x>scroller.width-margin -> 1; else -> 0 }
                    if (next!=direction) { direction=next;row.removeCallbacks(edge);if(next!=0) row.post(edge) }
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> { resetPreview();target=-1;stopEdge();true }
                DragEvent.ACTION_DROP -> {
                    val index=nearest(event.x)
                    clear()
                    val changed=index>=0 && move(payload.id,index)
                    if(changed) row.post {
                        val card=row.getChildAt(ids().indexOf(payload.id))
                        card?.apply { scaleX=.94f;scaleY=.94f;animate().scaleX(1f).scaleY(1f)
                            .setInterpolator(android.view.animation.OvershootInterpolator(BicycleDesign.tileDropOvershoot))
                            .setDuration(BicycleDesign.tileDropSettleMs).start() }
                    }
                    changed
                }
                else -> true
            }
        }
        row.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v:View) {}
            override fun onViewDetachedFromWindow(v:View) { clear() }
        })
    }
    fun bind(card:View,id:Int) {
        card.tooltipText=row.context.getString(R.string.tiles_move_hint)
        card.setOnLongClickListener {
            !locked() && ids().size>1 && card.startDragAndDrop(
                ClipData.newPlainText("", ""),View.DragShadowBuilder(card),Payload(this,id),0)
        }
        card.accessibilityDelegate=object:View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host:View,info:AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host,info)
                val index=ids().indexOf(id)
                if(!locked() && index>0) info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.bike_move_left,row.context.getString(R.string.tiles_move_left)))
                if(!locked() && index in 0 until ids().lastIndex) info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.bike_move_right,row.context.getString(R.string.tiles_move_right)))
            }
            override fun performAccessibilityAction(host:View,action:Int,args:android.os.Bundle?):Boolean {
                val offset=when(action) { R.id.bike_move_left -> -1;R.id.bike_move_right -> 1;else -> return super.performAccessibilityAction(host,action,args) }
                return !locked() && move(id,ids().indexOf(id)+offset)
            }
        }
    }
    private fun nearest(x:Float)=cards.indices.minByOrNull { kotlin.math.abs(x-(cards[it].left+cards[it].right)/2f) } ?: -1
    private fun preview(index:Int) {
        if(index==target || index<0) return
        target=index
        val list=cards;val from=ids().indexOf(source)
        if(from !in list.indices || index !in list.indices) return
        list.forEachIndexed { i,card ->
            card.animate().cancel()
            val destination=when {
                i==from -> index
                from<index && i in (from+1)..index -> i-1
                from>index && i in index until from -> i+1
                else -> i
            }
            card.animate().translationX((list[destination].left-card.left).toFloat())
                .alpha(if(i==from) .45f else 1f)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator()).setDuration(180).start()
            card.foreground=if(i==from) GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius=BicycleDesign.dp(row.context,BicycleDesign.inputRadius).toFloat()
                setStroke(BicycleDesign.dp(row.context,2),themes.active.tokens.accent)
            } else null
        }
    }
    private fun resetPreview() { cards.forEach { it.animate().cancel();it.translationX=0f;it.alpha=if(locked()) .4f else 1f;it.foreground=null } }
    private fun stopEdge() { direction=0;row.removeCallbacks(edge) }
    private fun clear() { stopEdge();resetPreview();source=null;target=-1 }
}

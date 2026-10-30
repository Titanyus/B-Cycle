package de.bcycle.app

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.DragEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
class TileReorderTest {
    private class Fixture {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        val c=host.get()
        val order=mutableListOf(0,1,2)
        val row=LinearLayout(c).apply { orientation=LinearLayout.HORIZONTAL }
        val scroll=HorizontalScrollView(c).apply { addView(row,FrameLayout.LayoutParams(-2,120)) }
        var locked=false;var saves=0
        val reorder=TileReorder(scroll,row,ThemeManager(c.getSharedPreferences("bike",0)),{order},{locked}) { id,index ->
            val from=order.indexOf(id)
            if(locked || from<0 || index !in order.indices || from==index) false else {
                order.add(index,order.removeAt(from));saves++;rebuild();true
            }
        }
        private var payload=TileReorder.Payload(reorder,0)
        init {
            rebuild();c.setContentView(scroll);shadowOf(Looper.getMainLooper()).idle();layout()
            ShadowChoreographer.setPaused(true)
            android.animation.ValueAnimator::class.java.getDeclaredMethod("setDurationScale",Float::class.javaPrimitiveType).invoke(null,1f)
        }
        fun rebuild() {
            row.removeAllViews()
            order.forEach { id ->
                val card=View(c).apply { tag=id;foreground=ColorDrawable(0xff112233.toInt()+id) }
                row.addView(card,LinearLayout.LayoutParams(96,120).apply { if(id!=order.first()) marginStart=8 })
                reorder.bind(card,id)
            }
            layout()
        }
        fun layout() {
            scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(120,View.MeasureSpec.EXACTLY))
            scroll.layout(0,0,360,120)
        }
        fun start(id:Int=order.first()) { payload=TileReorder.Payload(reorder,id);assertTrue(send(DragEvent.ACTION_DRAG_STARTED,10f)) }
        fun send(action:Int,x:Float=250f):Boolean {
            val method=DragEvent::class.java.declaredMethods.single { it.name=="obtain" && it.parameterCount==11 }.apply { isAccessible=true }
            val event=method.invoke(null,action,x,20f,0f,0f,payload,null,null,null,null,false) as DragEvent
            return row.dispatchDragEvent(event)
        }
        fun card(id:Int)=row.getChildAt(order.indexOf(id))
        fun normal(view:View) { assertEquals(1f,view.scaleX,.0001f);assertEquals(1f,view.scaleY,.0001f);assertEquals(0f,view.translationX,.0001f) }
        fun close() { host.pause().stop().destroy() }
    }
    @Test fun dragEndAfterQueuedDropWorkCannotLeaveTheNewCardShrunk() {
        val f=Fixture()
        try {
            val source=f.card(0);val original=source.foreground
            f.start(0);f.send(DragEvent.ACTION_DRAG_LOCATION)
            assertTrue(f.send(DragEvent.ACTION_DROP));assertEquals(listOf(1,2,0),f.order);assertEquals(1,f.saves)
            val moved=f.card(0)
            shadowOf(Looper.getMainLooper()).idle() // Reproduces DROP post running before DRAG_ENDED.
            f.send(DragEvent.ACTION_DRAG_ENDED);f.normal(moved)
            assertSame(original,source.foreground)
            moved.animate().setUpdateListener {} // Keep native property timing on the controllable UI clock.
            shadowOf(Looper.getMainLooper()).idle();assertTrue(moved.scaleX<1f)
            ShadowChoreographer.setPaused(false);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            f.normal(moved);assertEquals(1,f.saves)
        } finally { f.close() }
    }
    @Test fun newDragCancelsPendingSettleAndCancellationDoesNotSave() = checkNewDrag(false)
    @Test fun newDragCancelsRunningSettleAndCancellationDoesNotSave() = checkNewDrag(true)
    private fun checkNewDrag(startSettle:Boolean) {
        val f=Fixture()
        try {
            val id=f.order.first();f.start(id);assertTrue(f.send(DragEvent.ACTION_DROP));f.send(DragEvent.ACTION_DRAG_ENDED)
            if(startSettle) {
                f.card(id).animate().setUpdateListener {}
                shadowOf(Looper.getMainLooper()).idle();assertTrue(f.card(id).scaleX<1f)
            }
            val saved=f.order.toList();val count=f.saves
            f.start(f.order.first());f.normal(f.card(id))
            f.send(DragEvent.ACTION_DRAG_EXITED);f.send(DragEvent.ACTION_DRAG_ENDED)
            shadowOf(Looper.getMainLooper()).idle()
            f.order.forEach { f.normal(f.card(it)) };assertEquals(saved,f.order);assertEquals(count,f.saves)
        } finally { f.close() }
    }
    @Test fun detachmentAndLockBeforeSettleRestoreFullSizeAndDiscardQueuedWork() {
        val f=Fixture()
        try {
            f.start(0);assertTrue(f.send(DragEvent.ACTION_DROP));f.send(DragEvent.ACTION_DRAG_ENDED)
            shadowOf(Looper.getMainLooper()).idle();val moved=f.card(0);assertTrue(moved.scaleX<1f)
            f.scroll.removeView(f.row);f.normal(moved)
            f.scroll.addView(f.row,FrameLayout.LayoutParams(-2,120));f.layout();shadowOf(Looper.getMainLooper()).idle();f.normal(moved)
            f.start(1);assertTrue(f.send(DragEvent.ACTION_DROP));f.send(DragEvent.ACTION_DRAG_ENDED)
            f.locked=true;shadowOf(Looper.getMainLooper()).idle();f.order.forEach { f.normal(f.card(it)) }
        } finally { f.close() }
    }
}

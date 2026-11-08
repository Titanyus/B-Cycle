package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RideRpePickerTest {
    @Test fun compactPickerMatchesNoteHeightSwipesBothDirectionsClampsAndPreservesZeroAndUnset() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        val id=java.util.UUID.randomUUID().toString();val c=host.get()
        try {
            RideFinishTransfers.hold(c,id,"unused.fit",emptySet(),"")
            val row=RideSummaryFeedbackRow(c,ThemeManager(c.getSharedPreferences("bike",0)),id);c.setContentView(row)
            row.measure(View.MeasureSpec.makeMeasureSpec(296,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
            row.layout(0,0,296,row.measuredHeight)
            val picker=row.findViewWithTag<RideRpePicker>("ride_rpe_picker")
            assertNull(RideAnnotations.read(c,id).rpe)
            assertEquals(row.note.top,picker.top);assertEquals(row.note.bottom,picker.bottom)
            assertEquals(3,row.note.minLines);assertEquals(3,row.note.maxLines)
            fun drag(from:Float,to:Float) {
                for((index,pair) in listOf(MotionEvent.ACTION_DOWN to from,MotionEvent.ACTION_MOVE to to,MotionEvent.ACTION_UP to to).withIndex()) {
                    val event=MotionEvent.obtain(0,index*20L,pair.first,20f,pair.second,0)
                    try { assertTrue(picker.onTouchEvent(event)) } finally { event.recycle() }
                }
            }
            drag(70f,-50f);assertEquals(5,RideAnnotations.read(c,id).rpe)
            drag(20f,68f);assertEquals(3,RideAnnotations.read(c,id).rpe)
            drag(70f,-1000f);assertEquals(10,RideAnnotations.read(c,id).rpe)
            drag(10f,1000f);assertEquals(0,RideAnnotations.read(c,id).rpe)
            assertTrue(picker.stateDescription?.contains(c.getString(R.string.ride_rpe_none))==true)
            val args=android.os.Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,8f) }
            assertTrue(picker.performAccessibilityAction(android.R.id.accessibilityActionSetProgress,args));assertEquals(8,RideAnnotations.read(c,id).rpe)
            val image=Bitmap.createBitmap(296,row.height,Bitmap.Config.ARGB_8888)
            try { row.draw(Canvas(image)) } finally { image.recycle() }
        } finally { RideAnnotations.forget(c,id);RideFinishTransfers.forget(c,id);host.pause().stop().destroy() }
    }
}

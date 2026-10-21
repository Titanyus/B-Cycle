package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.SectionIndexer
import android.widget.TextView
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
class BicycleFastScrollTest {
    @Test fun dragNavigatesWrappedAdapterAndCompactPreviewUsesSecondaryTextAcrossThemeChanges() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val base=ThemeManager(c.getSharedPreferences("bike",0));val themes=base.settings
            val list=BicycleFastScrollList(c,themes)
            list.adapter=object:BaseAdapter(),SectionIndexer {
                override fun getCount()=400
                override fun getItem(position:Int)=position
                override fun getItemId(position:Int)=position.toLong()
                override fun getView(position:Int,convertView:View?,parent:ViewGroup)=TextView(c).apply { text="Fahrt $position";minimumHeight=48 }
                override fun getSections()=arrayOf("Oktober 2026","September 2026","August 2026","Juli 2026")
                override fun getPositionForSection(sectionIndex:Int)=sectionIndex*100
                override fun getSectionForPosition(position:Int)=position/100
            }
            BicycleDialogStyle.content(list,themes);c.setContentView(list)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            fun layout() { list.forceLayout();list.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));list.layout(0,0,360,600) }
            layout()
            fun touch(action:Int,y:Float) { val event=MotionEvent.obtain(0,0,action,356f,y,0);try { list.onTouchEvent(event) } finally { event.recycle() } }
            touch(MotionEvent.ACTION_DOWN,450f)
            layout()
            assertTrue("first=${list.firstVisiblePosition}, children=${list.childCount}, count=${list.count}, size=${list.width}x${list.height}, selected=${list.selectedItemPosition}",list.firstVisiblePosition>250)
            fun preview() {
                val image=Bitmap.createBitmap(360,600,Bitmap.Config.ARGB_8888)
                try {
                    list.draw(Canvas(image));val rect=list.previewBounds
                    assertTrue(rect.width() in 48f..180f);assertEquals(40f,rect.height(),.01f);assertTrue(rect.left>0)
                    var text=0;var background=0
                    for(y in rect.top.toInt() until rect.bottom.toInt()) for(x in rect.left.toInt() until rect.right.toInt()) {
                        when(image.getPixel(x,y)) { themes.active.tokens.secondary->text++;themes.active.tokens.foreground->background++ }
                    }
                    assertTrue("SECONDARY text pixels=$text",text>10);assertTrue("FOREGROUND background pixels=$background",background>1000)
                } finally { image.recycle() }
            }
            preview();themes.setSettingsLight(true)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();layout();preview()
            touch(MotionEvent.ACTION_UP,450f);assertTrue(list.previewBounds.isEmpty)
        } finally { host.pause().stop().destroy() }
    }
}

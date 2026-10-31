package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.widget.SectionIndexer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteStickyListTest {
    @Test fun independentGroupHeadersPinPushAndNeverSelectTheCoveredRide() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));var opened=0
            val manager=RouteManagerView(c,{},themes,{opened++}).apply { fullscreen=true }
            c.setContentView(manager)
            val entries=(0..2).flatMap { month -> (0..5).map { day ->
                val id="$month-$day"
                ManagedRoute(id,id,false,"FIT",RouteDetails(LocalDate.of(2026,10-month,28-day)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),20000.0,movingMs=3600000),true)
            } }
            manager.submit(entries,null,false);manager.applyPageSpacing()
            val list=manager.findViewWithTag<RouteStickyList>("route_list")
            val image=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888)
            fun draw() {
                manager.forceLayout()
                manager.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                manager.layout(0,0,360,800);manager.draw(Canvas(image))
            }
            try {
                for(flat in listOf(false,true)) {
                    themes.setFlatDesign(flat)
                    list.setSelectionFromTop(0,list.paddingTop);draw()
                    assertNull(list.pinnedGroup)
                    list.setSelectionFromTop(1,-20);draw()
                    assertNotNull(list.pinnedGroup);assertEquals(0,list.pinnedGroup!!.first)
                    assertEquals(0,list.pinnedOffset)
                    val headerHeight=list.pinnedHeight
                    assertTrue(headerHeight>0)
                    for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                        MotionEvent.obtain(0,20,action,100f,20f,0).also { list.dispatchTouchEvent(it);it.recycle() }
                    }
                    assertEquals(0,opened);assertFalse(manager.selecting)
                    val indexer=list.adapter as SectionIndexer
                    val next=indexer.getPositionForSection(1)
                    assertEquals(7,next);assertFalse(list.adapter.isEnabled(next))
                    list.setSelectionFromTop(next,headerHeight/2);draw()
                    assertNotNull(list.pinnedGroup);assertEquals(0,list.pinnedGroup!!.first)
                    assertTrue(list.pinnedOffset<0)
                    val actual=list.getChildAt(next-list.firstVisiblePosition)
                    assertEquals(actual.top,list.pinnedOffset+headerHeight)
                    list.setSelectionFromTop(next+1,-20);draw()
                    assertEquals(6,list.pinnedGroup!!.first);assertEquals(0,list.pinnedOffset)
                    themes.activate(BicycleThemes.presets[if(flat) 0 else 1].id)
                    draw();image.eraseColor(android.graphics.Color.TRANSPARENT);list.draw(Canvas(image))
                    assertEquals(themes.active.tokens.tertiary,image.getPixel(list.paddingLeft+1,2))
                }
            } finally { image.recycle() }
        } finally { host.pause().stop().destroy() }
    }
}

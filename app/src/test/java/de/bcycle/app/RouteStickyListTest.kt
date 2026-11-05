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
    @Test fun pinningKeepsAllFourSummaryValuesAndTheirUnitsVisible() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("sticky-summary",0))
            val manager=RouteManagerView(c,{},themes).apply { fullscreen=true }
            c.setContentView(manager)
            manager.submit((1..20).map { day -> ManagedRoute("r$day","Fahrt $day",false,"FIT",
                RouteDetails(LocalDate.of(2026,10,day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    20000.0,movingMs=3600000),true) },null,false)
            val list=manager.findViewWithTag<RouteStickyList>("route_list")
            val bitmap=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888)
            fun draw() {
                manager.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                manager.layout(0,0,360,800);manager.draw(Canvas(bitmap))
            }
            try {
                draw();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();draw()
                list.setSelectionFromTop(1,-20);manager.forceLayout();draw()
                assertNotNull(list.pinnedGroup)
                val pinned=RouteStickyList::class.java.getDeclaredField("pinned").apply { isAccessible=true }.get(list) as RouteListGroupHeader
                val readings=testDescendants(pinned).filterIsInstance<RouteListReading>()
                assertEquals(4,readings.size)
                val headerImage=Bitmap.createBitmap(pinned.width,pinned.height,Bitmap.Config.ARGB_8888)
                pinned.draw(Canvas(headerImage))
                for(reading in readings) {
                    assertTrue("Summary not measured: ${reading.number.tag}; parent=${reading.parent}; size=${reading.width}x${reading.height}; value=${reading.number.width}x${reading.number.height}",reading.number.width>0 && reading.number.height>0)
                    assertTrue("Empty text layout: ${reading.number.tag}",(reading.number.layout?.getLineWidth(0) ?: 0f)>0f)
                    assertTrue(reading.number.text.isNotEmpty())
                    if(reading.unit.text.isNotEmpty()) assertTrue("Missing unit: ${reading.number.tag}",reading.unit.width>0)
                    val rect=android.graphics.Rect(0,0,reading.number.width,reading.number.height)
                    pinned.offsetDescendantRectToMyCoords(reading.number,rect)
                    var ink=0
                    for(y in rect.top.coerceAtLeast(0) until rect.bottom.coerceAtMost(headerImage.height))
                        for(x in rect.left.coerceAtLeast(0) until rect.right.coerceAtMost(headerImage.width))
                            if(headerImage.getPixel(x,y)==themes.active.tokens.foreground) ink++
                    assertTrue("Value not painted: ${reading.number.tag}; ink=$ink; requested=${pinned.isLayoutRequested}; size=${reading.number.textSize}, baseline=${reading.number.baseline}, height=${reading.number.height}; lineLeft=${reading.number.layout.getLineLeft(0)}, layoutWidth=${reading.number.layout.width}, scroll=${reading.number.scrollX}",ink>0)
                }
                headerImage.recycle()
                java.io.File("build/reports/ui/route-sticky-summary.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            } finally { bitmap.recycle() }
        } finally { host.pause().stop().destroy() }
    }
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
                    val expected=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
                    Canvas(expected).apply { drawColor(themes.active.tokens.tertiary);drawColor(BicycleComponents.sectionRuleColor(themes.active.tokens)) }
                    assertEquals(expected.getPixel(0,0),image.getPixel(list.paddingLeft+1,0));expected.recycle()
                }
            } finally { image.recycle() }
        } finally { host.pause().stop().destroy() }
    }
}

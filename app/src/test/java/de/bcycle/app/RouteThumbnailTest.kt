package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteThumbnailTest {
    private val points=listOf(MapPoint(48.2,10.3,true),MapPoint(48.21,10.31),MapPoint(48.22,10.30),MapPoint(48.205,10.28),MapPoint(48.2,10.3))
    @Test fun representativeContoursAndRouteShareOneProjectionAndKeepGapsAndDateline() {
        val box=RouteThumbnails.bounds(points)
        val result=RouteThumbnails.terrain(points) { x,y -> (400+300*(x-box.left)/box.span+100*(y-box.top)/box.span).toFloat() }
        assertTrue(result.contours.size in 2..24)
        assertTrue(result.route.all { it in .09f.. .91f })
        assertTrue(result.contours.all { line -> line.size<=384 && line.all { it in 0f..1f } })
        assertArrayEquals(RouteThumbnails.outline(points).route,result.route,0f)
        val crossing=listOf(MapPoint(48.0,179.9,true),MapPoint(48.01,-179.9),MapPoint(48.02,-179.8,true))
        assertTrue(RouteThumbnails.bounds(crossing).span<.01)
        assertArrayEquals(booleanArrayOf(true,false,true),RouteThumbnails.outline(crossing).starts)
        assertTrue(RouteThumbnails.terrain(points) { _,_->Float.NaN }.contours.isEmpty())
    }
    @Test fun thumbnailUsesTertiaryOpaqueRouteAndFaintSameColorContours() {
        val c=RuntimeEnvironment.getApplication();val view=RoutePreviewView(c)
        val tokens=ThemeTokens(Color.RED,Color.BLUE,Color.BLACK,Color.WHITE)
        view.colors(tokens);view.layout(0,0,100,100)
        val geometry=RouteThumbnailGeometry(floatArrayOf(.2f,.75f,.8f,.75f),booleanArrayOf(true,false),listOf(floatArrayOf(.5f,.1f,.5f,.9f)))
        RoutePreviewView::class.java.getDeclaredField("geometry").apply { isAccessible=true }.set(view,geometry)
        RoutePreviewView::class.java.getDeclaredMethod("render").apply { isAccessible=true }.invoke(view)
        val bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
        try {
            assertEquals(Color.BLACK,bitmap.getPixel(20,20))
            assertEquals(Color.WHITE,bitmap.getPixel(25,75))
            val faint=maxOf(Color.red(bitmap.getPixel(49,30)),Color.red(bitmap.getPixel(50,30)))
            assertTrue("20% line remains faint after antialiasing: $faint",faint in 10..60)
        } finally { bitmap.recycle() }
    }
    @Test fun fullWidthFlatGroupsAndOverlappingSquareBadgeKeepActionsAndDetailsSeparate() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible();val c=host.get()
        try {
            c.getSharedPreferences("bike",0).edit().putBoolean(BicycleAppearance.flatKey,true).commit()
            val themes=ThemeManager(c.getSharedPreferences("bike",0));var opened=0;var toggled=0
            val manager=RouteManagerView(c,{toggled++},themes,{opened++}).apply { fullscreen=true }
            RideProfileColor.confirm(c,"thumbnail-test",RideProfileColor.snapshot(c))
            val rows=(0..20).map { i -> ManagedRoute(if(i==0) "thumbnail-test" else "thumb$i","Wochenendtour $i",true,"FIT",RouteDetails(1790000000000,61500.0,7500000,points,movingMs=7500000,bikeType=BikeType.EBIKE),true,locallySaved=true) }
            c.setContentView(manager);manager.submit(rows,null,false);manager.applyPageSpacing()
            fun layout() { manager.measure(View.MeasureSpec.makeMeasureSpec(420,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));manager.layout(0,0,420,800) }
            layout();val list=manager.findViewWithTag<RouteStickyList>("route_list")
            assertEquals(0,list.paddingLeft);assertEquals(0,list.paddingRight)
            val header=list.getChildAt(0);assertEquals(0,header.left);assertEquals(list.width,header.right)
            val card=list.getChildAt(1) as ViewGroup
            val id=(list.adapter.getItem(1) as ManagedRoute).id
            val preview=card.findViewWithTag<View>("route_thumbnail_$id")
            val badge=card.findViewWithTag<View>("route_bike_badge_$id")
            val p=Rect();val b=Rect();preview.getDrawingRect(p);card.offsetDescendantRectToMyCoords(preview,p)
            badge.getDrawingRect(b);card.offsetDescendantRectToMyCoords(badge,b)
            assertTrue(b.left<p.left && b.top<p.top);assertTrue(Rect.intersects(p,b));assertEquals(b.width(),b.height())
            assertNull(card.findViewWithTag<View>("route_format_$id"))
            card.performClick();assertEquals(1,opened)
            card.findViewWithTag<View>("route_eye_$id").performClick();assertEquals(1,toggled)
            val bitmap=Bitmap.createBitmap(420,800,Bitmap.Config.ARGB_8888);manager.draw(Canvas(bitmap))
            File("build/reports/ui/route-thumbnails.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            list.setSelectionFromTop(3,-20);layout()
            val image=Bitmap.createBitmap(420,800,Bitmap.Config.ARGB_8888);list.draw(Canvas(image));image.recycle()
            assertNotNull(list.pinnedGroup);assertEquals(0,list.paddingLeft);assertEquals(420,list.width)
        } finally { RideProfileColor.forget(c,"thumbnail-test");host.pause().stop().destroy() }
    }
}

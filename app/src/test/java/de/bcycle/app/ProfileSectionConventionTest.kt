package de.bcycle.app

import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileSectionConventionTest {
    @Test fun commonMapAndDesignControlsHaveRequestedOrderForBothBikeTypesAndDesignModes() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val runtime=RideRuntime.get(a)
            val mapMethod=MainActivity::class.java.getDeclaredMethod("mapControls").apply { isAccessible=true }
            val designMethod=MainActivity::class.java.getDeclaredMethod("dashboardControls",Boolean::class.javaPrimitiveType).apply { isAccessible=true }
            for(type in listOf(BikeType.EBIKE,BikeType.INDOOR)) for(flat in listOf(false,true)) {
                runtime.bikes.save(runtime.bikes.current().copy(type=type));runtime.themes.setFlatDesign(flat)
                val map=mapMethod.invoke(a) as android.view.ViewGroup
                assertFalse(testDescendants(map).filterIsInstance<android.widget.TextView>().any { it.text.toString()==a.getString(R.string.ui_map_legend) })
                val track=map.findViewWithTag<View>("map_line_styles")
                assertEquals("map_line_style_divider",map.getChildAt(map.indexOfChild(track)-1).tag)
                for(embedded in listOf(false,true)) {
                    val page=designMethod.invoke(a,embedded) as android.view.ViewGroup
                    val design=if(embedded) page else page.findViewWithTag<BicycleStickyScrollView>("design_ui_scroll").getChildAt(0) as android.view.ViewGroup
                    assertEquals("dashboard_fullscreen_settings",design.getChildAt(0).tag)
                    assertEquals("design_appearance_divider",design.getChildAt(1).tag)
                    assertEquals("flat_design",design.getChildAt(2).tag)
                    assertEquals("theme_picker",design.getChildAt(3).tag)
                    assertEquals("design_dashboard_divider",design.getChildAt(4).tag)
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun profileDetailsUseNormalCollapsibleSectionAndRenamingKeepsBoxAndState() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val runtime=RideRuntime.get(a)
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            fun layout() {
                shadowOf(Looper.getMainLooper()).idle()
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                root.layout(0,0,360,800)
            }
            val header=root.findViewWithTag<BicycleSectionHeader>("bike_name_header")
            val body=root.findViewWithTag<View>("bike_profile_details")
            assertSame(body,header.cardContent)
            assertTrue(testDescendants(body).any { it.tag=="bike_photo" })
            assertTrue(testDescendants(body).any { it.tag=="bike_category" })
            for(flat in listOf(false,true,false)) {
                runtime.themes.setFlatDesign(flat);layout()
                assertEquals(true,header.expanded)
                if(!flat) assertEquals(16,header.left) else assertEquals(0,header.left)
                val title=header.titleView
                val now=android.os.SystemClock.uptimeMillis()
                for(action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)) {
                    val event=android.view.MotionEvent.obtain(now,now+16,action,title.width-2f,title.height/2f,0)
                    title.dispatchTouchEvent(event);event.recycle()
                }
                layout();assertEquals(false,header.expanded);assertEquals(View.GONE,body.visibility)
                assertNull(root.findViewWithTag<EditText>("bike_profile_name_edit"))
                header.titleView.performClick();layout()
                val edit=root.findViewWithTag<EditText>("bike_profile_name_edit")
                assertSame(header,edit.parent)
                assertEquals(View.VISIBLE,header.visibility)
                assertEquals(false,header.expanded);assertEquals(View.GONE,body.visibility)
                edit.setText(if(flat) "Mein Flat-Rad" else "Mein Glow-Rad")
                edit.onEditorAction(EditorInfo.IME_ACTION_DONE);layout()
                assertSame(header,header.titleView.parent)
                assertEquals(runtime.bikes.current().name,header.titleView.text.toString())
                assertEquals(false,header.expanded)
                header.performClick();layout();assertEquals(true,header.expanded);assertEquals(View.VISIBLE,body.visibility)
                val bitmap=android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888)
                root.draw(android.graphics.Canvas(bitmap))
                java.io.File("build/reports/ui/profile-section-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }
                    .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            }
        } finally { host.pause().stop().destroy() }
    }
}

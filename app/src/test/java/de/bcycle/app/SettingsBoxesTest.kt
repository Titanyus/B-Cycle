package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsBoxesTest {
    private fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    @Test fun bothDesignsKeepThreeTwoColumnBoxesAllActionsAndBrandAtNarrowWidths() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup().visible();val a=host.get()
        try {
            val themes=ThemeManager(a.getSharedPreferences("bike",0));val actions=mutableListOf<SettingsAction>()
            val page=SettingsMenuView(a,themes,false) { actions+=it };a.setContentView(page)
            for(flat in listOf(false,true)) for(width in listOf(288,360,480)) {
                themes.setFlatDesign(flat);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                BicycleContentSpacing.page(page)
                page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,width,page.measuredHeight)
                assertEquals(5,page.childCount)
                assertNull(page.findViewWithTag<View>("hrv_open_measurement"))
                for(title in listOf("Bike","Training","HRV")) {
                    val box=page.findViewWithTag<LinearLayout>("settings_box_$title")
                    assertEquals(BicycleDesign.dp(a,if(flat) BicycleDesign.horizontalPadding else BicycleDesign.controlGap),box.paddingTop)
                    val margins=box.layoutParams as ViewGroup.MarginLayoutParams
                    assertEquals(BicycleDesign.dp(a,if(flat) BicycleDesign.smallGap else BicycleDesign.controlGap),margins.leftMargin)
                    assertEquals(themes.active.tokens.tertiary,(box.background as GradientDrawable).color!!.defaultColor)
                    val left=page.findViewWithTag<LinearLayout>("settings_column_${title}_left")
                    val right=page.findViewWithTag<LinearLayout>("settings_column_${title}_right")
                    assertTrue(left.width>0);assertTrue(kotlin.math.abs(left.width-right.width)<=1)
                    if(title=="HRV") {
                        assertTrue("width=$width left=${left.height} right=${right.height}",right.height<left.height);assertEquals(left.top,right.top)
                    } else assertEquals(left.height,right.height)
                    assertTrue(left.right<=right.left)
                    assertEquals(themes.active.tokens.secondary,(left.background as GradientDrawable).color!!.defaultColor)
                    val heading=nodes(box).filterIsInstance<TextView>().single { it.isAccessibilityHeading }
                    val header=heading.parent as View
                    val columns=left.parent as View
                    assertEquals(columns.top/2f,header.top+heading.top+heading.height/2f,1f)
                    assertEquals(BicycleDesign.narrowTypeface(a),heading.typeface)
                    assertEquals(BicycleDesign.headingSp,heading.textSize/a.resources.displayMetrics.scaledDensity,.01f)
                }
                assertEquals(0,page.findViewWithTag<LinearLayout>("settings_column_Training_right").childCount)
                assertNotNull(page.findViewWithTag<View>("settings_brand_art"));assertNotNull(page.findViewWithTag<View>("settings_build_info"))
                val rows=nodes(page).filter { it.tag?.toString()?.startsWith("settings_")==true && it.isClickable }
                assertEquals(13,rows.size)
                rows.forEach { assertTrue(it.width>0 && it.height>=48) }
                if(width==360) {
                    val bitmap=Bitmap.createBitmap(width,page.height,Bitmap.Config.ARGB_8888);page.draw(Canvas(bitmap))
                    File("build/reports/ui/settings-boxes-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
                }
            }
            val expected=listOf(SettingsAction.ROUTES,SettingsAction.ROUTE_TEMPLATES,SettingsAction.STATISTICS,SettingsAction.MAINTENANCE,SettingsAction.PROFILE,SettingsAction.PROFILES,SettingsAction.NOTIFICATIONS,SettingsAction.DATA,SettingsAction.TRAINING,SettingsAction.HRV_MEASUREMENT,SettingsAction.HRV_ANALYSIS,SettingsAction.HRV,SettingsAction.ADVANCED)
            expected.forEach { page.findViewWithTag<View>("settings_${it.name}").performClick() };assertEquals(expected,actions)
            assertEquals(R.drawable.ic_lucide_biceps_flexed,SettingsAction.TRAINING.icon)
        } finally { host.pause().stop().destroy() }
    }
}

package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContentSpacingTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val themes get()=ThemeManager(c.getSharedPreferences("spacing",0))
    @Test fun spacingIsIdempotentForEverySectionContentTypeAndDoesNotPadNestedListsInsteadOfThePage() {
        val body=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL;setPadding(16,8,16,8) }
        val types=listOf<View>(TextView(c),EditText(c),Spinner(c),Button(c),Switch(c),LinearLayout(c),ListView(c))
        types.forEach { view ->
            BicycleForm.section(body,themes,R.string.bikes_type)
            body.addView(view,LinearLayout.LayoutParams(-1,48))
        }
        val scroll=ScrollView(c).apply { addView(body) }
        repeat(3) { BicycleContentSpacing.page(scroll) }
        assertEquals(0,body.paddingTop) // The first category touches the window header.
        for(i in 0 until body.childCount) {
            val child=body.getChildAt(i);val lp=child.layoutParams as LinearLayout.LayoutParams
            if(child is BicycleSectionHeader) { assertEquals(0,lp.topMargin);assertEquals(0,lp.bottomMargin) }
            else { assertEquals(16,lp.topMargin);if(i<body.childCount-1) assertEquals(16,lp.bottomMargin) }
        }
        val ordinary=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL;setPadding(0,3,0,0);addView(TextView(c));addView(ListView(c)) }
        BicycleContentSpacing.page(ordinary)
        assertEquals(16,ordinary.paddingTop);assertEquals(0,ordinary.getChildAt(1).paddingTop)
    }
    @Test fun centeredButtonsDrawIconsImmediatelyBeforeTheirTextInBothDirections() {
        for(direction in listOf(View.LAYOUT_DIRECTION_LTR,View.LAYOUT_DIRECTION_RTL)) {
            val button=BicycleComponents.action(c,themes,"Verbinden") {}.apply { layoutDirection=direction;gravity=Gravity.CENTER }
            val icon=BicycleTextIcon(button,ColorDrawable(Color.RED)).apply { setBounds(0,0,24,24) }
            button.setCompoundDrawablesRelative(icon,null,null,null);button.compoundDrawablePadding=8
            button.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(64,View.MeasureSpec.EXACTLY));button.layout(0,0,320,64)
            val bitmap=Bitmap.createBitmap(320,64,Bitmap.Config.ARGB_8888);button.draw(Canvas(bitmap))
            val pixels=(0 until 320).filter { x -> (0 until 64).any { y -> bitmap.getPixel(x,y)==Color.RED } }
            assertTrue(pixels.isNotEmpty())
            val textLeft=button.compoundPaddingLeft+button.layout.getLineLeft(0)
            val textRight=button.compoundPaddingLeft+button.layout.getLineRight(0)
            if(direction==View.LAYOUT_DIRECTION_LTR) assertEquals(textLeft-8,pixels.last()+1f,1f)
            else assertEquals(textRight+8,pixels.first().toFloat(),1f)
            assertEquals("Verbinden",button.text.toString())
            bitmap.recycle()
        }
    }
    @Test fun lineWidthsFollowDensityAndSectionDividersHaveSymmetricOuterSpacing() {
        for(dpi in listOf(160,240,320,440,480)) {
            val configuration=android.content.res.Configuration(c.resources.configuration).apply { densityDpi=dpi }
            val context=c.createConfigurationContext(configuration)
            assertEquals(Math.round(dpi/160f*BicycleDesign.dividerDp).coerceAtLeast(1),BicycleDesign.linePixels(context))
            assertEquals(Math.round(dpi/160f*.5f).coerceAtLeast(1),BicycleDesign.linePixels(context,.5f))
            val divider=BicycleComponents.sectionDivider(context,themes)
            val margins=divider.layoutParams as LinearLayout.LayoutParams
            assertEquals(BicycleDesign.dp(context,12),margins.topMargin);assertEquals(margins.topMargin,margins.bottomMargin)
            val parent=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL }
            parent.addView(divider)
            val button=BicycleComponents.action(context,themes,"Action") {}
            parent.addView(button,LinearLayout.LayoutParams(-1,-2));BicycleComponents.controlSpacing(button)
            assertEquals(0,(button.layoutParams as LinearLayout.LayoutParams).topMargin)
        }
    }
    @Test fun sensorIdentityUsesTheSameIconsInEveryContext() {
        assertEquals(SensorIcons.forSensor(BikeSensor.GPS),SensorIcons.forSource("GPS"))
        assertEquals(SensorIcons.forSensor(BikeSensor.EBIKE),SensorIcons.forSource("E-Bike"))
        assertEquals(SensorIcons.forSensor(BikeSensor.HEART),SensorIcons.forSource("Pulsmesser"))
        assertEquals(SensorIcons.forSensor(BikeSensor.POWER),SensorIcons.forSource("Leistung"))
    }
    @Test fun bikesRoutesAndRideDateHaveTheSameGapBelowTheirFullscreenHeader() {
        val controller=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            for(kind in 0..2) {
                var analysis:RouteAnalysisView?=null
                val page:View=when(kind) {
                    0 -> BicycleStickyScrollView(activity).apply {
                        addView(BikesView(activity,runtime,{},{},{},{ View(activity) }))
                    }
                    1 -> RouteManagerView(activity,{},runtime.themes)
                    else -> RouteAnalysisView(activity,runtime.themes,"spacing") {}.also {
                        analysis=it
                        it.show(RouteAnalysis(ManagedRoute("spacing","Test",true,"FIT",RouteDetails(1_791_626_400_000),false),emptyMap(),null,true,1.0,emptyList()))
                    }
                }
                val root=LinearLayout(activity).apply {
                    orientation=LinearLayout.VERTICAL
                    addView(page,LinearLayout.LayoutParams(-1,0,1f))
                    addView(bicycleFooter(activity,runtime.themes),LinearLayout.LayoutParams(-1,64))
                }
                val dialog=android.app.AlertDialog.Builder(activity,R.style.SettingsFullscreenTheme).setTitle("Test").setView(root).show()
                try {
                    BicycleDialogStyle.apply(dialog,runtime.themes);BicycleFullscreen.apply(dialog,runtime.themes)
                    val decor=dialog.window!!.decorView
                    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                    decor.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1600,View.MeasureSpec.EXACTLY))
                    decor.layout(0,0,720,1600)
                    val target=decor.findViewWithTag<View>(when(kind) { 0->"bike_profile_strip";1->"route_filter";else->"analysis_date" })
                    val header=decor.findViewWithTag<View>("bicycle_navigation_header")
                    val a=IntArray(2);val b=IntArray(2);target.getLocationOnScreen(a);header.getLocationOnScreen(b)
                    assertEquals("page $kind header/content gap",BicycleDesign.dp(activity,BicycleDesign.pageContentGap),a[1]-b[1]-header.height)
                } finally { analysis?.dispose();dialog.dismiss() }
            }
        } finally { controller.pause().stop().destroy() }
    }

}

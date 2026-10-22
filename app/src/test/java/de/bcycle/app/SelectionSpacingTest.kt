package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
@Config(sdk=[33],qualifiers="de-rDE-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectionSpacingTest {
    private fun layout(view:View,width:Int=360,height:Int=800) {
        shadowOf(Looper.getMainLooper()).idle()
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY))
        view.layout(0,0,width,height)
    }
    @Test fun namingAndGraphSelectionsShareFramedRowsAndFormGapsWhileMultiSelectionStillWorks() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val naming=RideNamingView(c,themes) { true }
            val graph=GraphOverlayPicker(c,themes,emptyList())
            val training=TrainingHelpView(c,themes,DashboardTrainingHelp()) {}
            val root=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL;addView(naming);addView(graph);addView(training) }
            c.setContentView(root)
            naming.findViewWithTag<View>("ride_naming_toggle").performClick()
            graph.findViewWithTag<View>("graph_overlay_toggle").performClick()
            layout(root)
            val input=naming.findViewWithTag<EditText>("ride_naming_text")
            val parts=naming.findViewWithTag<LinearLayout>("ride_naming_parts")
            val windows=graph.findViewWithTag<RadioGroup>("graph_windows")
            assertEquals(8,(input.layoutParams as ViewGroup.MarginLayoutParams).topMargin)
            assertEquals(8,parts.top-input.bottom)
            assertEquals("Fahrtzeit",naming.findViewWithTag<TextView>("ride_name_MOVING").text.toString())
            for(row in listOf(parts,windows,training.findViewWithTag<RadioGroup>("training_help_modes"))) {
                assertEquals(1,row.paddingTop);assertEquals(1,row.paddingBottom)
                assertEquals(true,row.getTag(R.id.inline_control_row))
                val bitmap=Bitmap.createBitmap(row.width,row.height,Bitmap.Config.ARGB_8888)
                val canvas=Canvas(bitmap);canvas.drawColor(themes.active.tokens.secondary);row.draw(canvas)
                assertNotEquals(themes.active.tokens.secondary,bitmap.getPixel(row.width/2,0))
                for(i in 0 until row.childCount) {
                    val lp=row.getChildAt(i).layoutParams as ViewGroup.MarginLayoutParams
                    assertEquals(0,lp.topMargin);assertEquals(0,lp.bottomMargin)
                }
            }
            val settings=RideNamingSettings(c);val before=settings.read()
            naming.findViewWithTag<View>("ride_name_DISTANCE").performClick()
            assertEquals(before.parts+RideNamePart.DISTANCE,settings.read().parts)
            naming.findViewWithTag<View>("ride_name_DATE").performClick()
            assertEquals((before.parts+RideNamePart.DISTANCE)-RideNamePart.DATE,settings.read().parts)
            input.setText("Runde");assertEquals("Runde",settings.read().text)
            val image=Bitmap.createBitmap(360,naming.height,Bitmap.Config.ARGB_8888)
            naming.draw(Canvas(image).apply { drawColor(themes.active.tokens.secondary) })
            java.io.File("build/reports/ui/ride-naming-spacing.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun stackedFieldLabelsShareFourDpAfterFinalLayoutAndRestylingWhileOtherControlsKeepTheirGaps() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("label-spacing",0))
            val root=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL }
            val fields=listOf(0,8,24).map { initialGap ->
                root.addView(BicycleComponents.fieldLabel(c,themes,"Beschriftung"))
                BicycleComponents.input(c,themes).also { edit ->
                    root.addView(edit,LinearLayout.LayoutParams(-1,-2).apply { topMargin=initialGap })
                }
            }
            val associated=TextView(c).apply { text="Zugeordnet" }
            root.addView(associated)
            val explicit=BicycleComponents.input(c,themes).apply { id=View.generateViewId() }
            associated.labelFor=explicit.id;root.addView(explicit)
            val ordinary=BicycleComponents.input(c,themes);root.addView(ordinary)
            c.setContentView(root);layout(root)
            repeat(2) {
                (fields+explicit).forEach { field ->
                    BicycleDialogStyle.control(field,themes)
                    assertEquals(4,(field.layoutParams as ViewGroup.MarginLayoutParams).topMargin)
                    val parent=field.parent as LinearLayout
                    assertEquals(4,field.top-parent.getChildAt(parent.indexOfChild(field)-1).bottom)
                }
                assertEquals(8,(ordinary.layoutParams as ViewGroup.MarginLayoutParams).topMargin)
                layout(root)
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun stackedIconsStayTwoDpAboveLabelAtDifferentSegmentHeights() {
        val c=org.robolectric.RuntimeEnvironment.getApplication();val themes=ThemeManager(c.getSharedPreferences("spacing",0))
        for(height in listOf(72,120)) {
            val button=BicycleSegmentButton(c,themes,multiple=true).apply { text="Fahrtzeit";stackedIcon(BicycleIcon.Duration) }
            val icon=object:Drawable() {
                override fun draw(canvas:Canvas) { canvas.drawRect(bounds,android.graphics.Paint().apply { color=Color.RED }) }
                override fun setAlpha(alpha:Int)=Unit
                override fun setColorFilter(filter:ColorFilter?)=Unit
                @Deprecated("Android") override fun getOpacity()=PixelFormat.OPAQUE
            }
            val drawable=BicycleTextIcon(button,icon,above=true).apply { setBounds(0,0,24,24) }
            button.setCompoundDrawables(null,drawable,null,null)
            layout(button,80,height)
            val bitmap=Bitmap.createBitmap(80,height,Bitmap.Config.ARGB_8888);button.draw(Canvas(bitmap))
            val rows=(0 until height).filter { y -> (0 until 80).any { x -> bitmap.getPixel(x,y)==Color.RED } }
            assertEquals(24,rows.size)
            assertEquals(2,button.totalPaddingTop-rows.last()-1)
            assertTrue(rows.first()>0)
        }
    }
    @Test fun actualSettingsPagesUseSixteenDpAndLeadingStickyHeadersStayFlush() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=host.get()
            fun openMenu():android.app.AlertDialog {
                MainActivity::class.java.getDeclaredMethod("showMenu",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(activity,false)
                return ShadowAlertDialog.getLatestAlertDialog()
            }
            for(action in listOf<SettingsAction?>(null,SettingsAction.PROFILE,SettingsAction.PROFILES,SettingsAction.HRV,SettingsAction.NOTIFICATIONS,SettingsAction.DATA,SettingsAction.ADVANCED)) {
                val menu=openMenu()
                action?.let { menu.window!!.decorView.findViewWithTag<View>("settings_${it.name}").performClick() }
                val dialog=ShadowAlertDialog.getLatestAlertDialog();val decor=dialog.window!!.decorView
                repeat(2) { layout(decor) }
                val target:View=when(action) {
                    SettingsAction.PROFILE,SettingsAction.DATA -> testDescendants(decor).filterIsInstance<BicycleSectionHeader>().first()
                    SettingsAction.PROFILES -> decor.findViewWithTag("bike_profile_strip")
                    SettingsAction.HRV -> decor.findViewWithTag<LinearLayout>("hrv_settings").getChildAt(0)
                    SettingsAction.NOTIFICATIONS -> decor.findViewWithTag("notifications_enabled")
                    SettingsAction.ADVANCED -> decor.findViewWithTag("settings_DIAGNOSTIC")
                    else -> decor.findViewWithTag("settings_PROFILE")
                }
                val header=decor.findViewWithTag<View>("bicycle_navigation_header")
                val a=IntArray(2);val b=IntArray(2);target.getLocationOnScreen(a);header.getLocationOnScreen(b)
                assertEquals("header gap on $action",if(target is BicycleSectionHeader) 0 else 16,a[1]-b[1]-header.height)
                dialog.dismiss()
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun lateFormsNormalizeNestedInsetsAndKeepStickyHeadersFlushWithoutAccumulatingGaps() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("spacing",0))
            val body=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL;setPadding(16,8,16,8) }
            val dialog=BicycleDialogBuilder(c,themes,DialogNavigation()).setTitle("Test").setView(body).show()
            try {
                val nested=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL;setPadding(0,12,0,0) }
                val field=BicycleComponents.input(c,themes)
                nested.addView(field,LinearLayout.LayoutParams(-1,48).apply { topMargin=20 })
                body.addView(nested,LinearLayout.LayoutParams(-1,-2).apply { topMargin=24 })
                val decor=dialog.window!!.decorView
                fun gap(view:View):Int {
                    val a=IntArray(2);val b=IntArray(2);view.getLocationOnScreen(a)
                    decor.findViewWithTag<View>("bicycle_navigation_header").getLocationOnScreen(b)
                    return a[1]-b[1]-BicycleDesign.headerHeight
                }
                repeat(3) { layout(decor);assertEquals(16,gap(field)) }
                nested.removeAllViews()
                val header=BicycleForm.section(nested,themes,R.string.hrv_title)
                nested.addView(TextView(c).apply { text="Test" })
                repeat(3) { layout(decor);assertEquals(0,gap(header)) }
            } finally { dialog.dismiss() }
        } finally { host.pause().stop().destroy() }
    }
}

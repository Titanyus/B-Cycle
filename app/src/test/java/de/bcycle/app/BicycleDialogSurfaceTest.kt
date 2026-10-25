package de.bcycle.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class BicycleDialogSurfaceTest {
    private fun color(view:View)=when(val background=view.background) {
        is GradientDrawable -> background.color!!.defaultColor
        is ColorDrawable -> background.color
        else -> error("No surface: ${view.tag}")
    }
    @Test fun cardsShareTertiaryChromeAndAlertUsesPrimaryInBothModesWithoutChangingActions() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val root=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL;clipChildren=false }
            var started=false
            val daily=HrvDailyCard(a,themes,{}, {started=true})
            val drink=DrinkReminderPanel(a,themes)
            root.addView(daily,LinearLayout.LayoutParams(-1,0,1f));root.addView(drink)
            a.setContentView(root)
            for(flat in listOf(false,true,false)) {
                themes.setFlatDesign(flat);shadowOf(Looper.getMainLooper()).idle()
                assertEquals(themes.active.tokens.tertiary,color(daily))
                assertEquals(themes.active.tokens.primary,color(drink))
                assertEquals(daily.elevation,drink.elevation,0f);assertTrue(drink.elevation>0f)
                assertEquals((daily.background as GradientDrawable).cornerRadius,(drink.background as GradientDrawable).cornerRadius,0f)
                assertNull(drink.findViewWithTag<View>("bicycle_navigation_header"))
                drink.showReminder(5);assertEquals(View.VISIBLE,drink.visibility)
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                root.layout(0,0,360,800)
                val bitmap=android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888)
                val canvas=android.graphics.Canvas(bitmap);canvas.drawColor(themes.active.tokens.secondary);root.draw(canvas)
                java.io.File("build/reports/ui/dialog-cards-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }
                    .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
                drink.performClick();assertEquals(View.GONE,drink.visibility)
            }
            daily.findViewWithTag<View>("hrv_daily_start").performClick();assertTrue(started)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun compactAndFullscreenUseSamePlainHeaderSurfaceAndKeepNativeActionsAndScrolling() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0)).settings
            for(fullscreen in listOf(false,true)) {
                var action=false
                val body=LinearLayout(a).apply { orientation=LinearLayout.VERTICAL
                    BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
                    repeat(30) { addView(android.widget.TextView(a).apply { text="Eintrag $it";minimumHeight=48 }) }
                }
                val scroll=ScrollView(a).apply { addView(body) }
                val screen=BicycleDialogBuilder(a,themes,DialogNavigation(),fullscreen=fullscreen)
                    .setTitle("Dialog").setView(scroll).setPositiveButton("Exportieren") { _,_->action=true }.show()
                shadowOf(Looper.getMainLooper()).idle()
                val decor=screen.window!!.decorView
                for(light in listOf(false,true,false)) {
                    themes.setSettingsLight(light)
                    for(flat in listOf(false,true)) {
                        themes.setFlatDesign(flat);shadowOf(Looper.getMainLooper()).idle()
                        val header=decor.findViewWithTag<View>("bicycle_navigation_header")
                        assertEquals(themes.active.tokens.tertiary,color(header))
                        assertEquals(themes.active.tokens.tertiary,color(body))
                        assertNotNull(header.findViewWithTag<View>("dialog_back"))
                        assertEquals(themes.active.tokens.foreground,header.findViewWithTag<android.widget.TextView>("bicycle_dialog_title").currentTextColor)
                        assertTrue(screen.window!!.decorView.elevation>0f)
                    }
                }
                screen.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                shadowOf(Looper.getMainLooper()).idle();assertTrue(action)
            }
            for(role in listOf(BicycleDialogSurface.normal,BicycleDialogSurface.alert)) {
                val noHeader=BicycleDialogBuilder(a,themes,DialogNavigation(),fullscreen=false,headerRole=role)
                    .setMessage("Ohne Header").show()
                shadowOf(Looper.getMainLooper()).idle()
                assertNull(noHeader.window!!.decorView.findViewWithTag<View>("bicycle_navigation_header"))
                assertEquals(themes.active.tokens.color(role),color(noHeader.window!!.decorView))
                noHeader.dismiss()
            }
        } finally { host.pause().stop().destroy() }
    }
}

package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualFollowupTest {
    private fun layout(view:View,height:Int=800) {
        view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY))
        view.layout(0,0,360,height)
        view.viewTreeObserver.dispatchOnGlobalLayout()
    }
    @Test fun previewUsesLastRideAndStaysOutsideEditableAndSettings() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val c=host.get()
        val oldLocale=Locale.getDefault();val oldZone=java.util.TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY);java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Berlin"))
            val settings=RideNamingSettings(c);val naming=RideNaming("Feierabendrunde",RideNamePart.entries.toSet());settings.write(naming)
            val details=RouteDetails(Instant.parse("2026-10-21T15:30:00Z").toEpochMilli(),32400.0,4520000,movingMs=4520000)
            val view=RideNamingView(c,ThemeManager(c.getSharedPreferences("bike",0)),{details}) { true }
            c.setContentView(view);view.findViewWithTag<View>("ride_naming_toggle").performClick();layout(view)
            val input=view.findViewWithTag<RideNamingInput>("ride_naming_text")
            assertEquals(" · 21.10.2026 17:30h · 32,4km · 1:15:20h · 25,8km/h",input.previewSuffix)
            assertEquals(naming.text,input.text.toString());assertEquals(naming,settings.read())
            input.setSelection(3);view.refreshLock();assertEquals(3,input.selectionStart)
            val image=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888)
            image.eraseColor(ThemeManager(c.getSharedPreferences("bike",0)).active.tokens.secondary);view.draw(Canvas(image))
            val output=java.io.File("build/reports/ui/ride-naming-preview.png").apply { parentFile?.mkdirs() }
            output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            assertTrue(input.height>48) // The example wraps inside the same field.
            view.findViewWithTag<View>("ride_name_DISTANCE").performClick()
            assertFalse(input.previewSuffix.contains("32,4km"));assertEquals(naming.text,settings.read().text)
            input.setText("");assertNull(input.hint);assertFalse(input.previewSuffix.startsWith(" · "));assertEquals("",settings.read().text)
            input.setText("x".repeat(61));assertEquals(60,input.length());assertEquals("x".repeat(60),settings.read().text)
            assertEquals(" · xx.xx.xxxx xx:xxh · xx,xkm · x:xx:xxh · xx,xkm/h",rideNamingPreview(c,naming,null,ZoneId.of("UTC"),Locale.GERMANY))
            assertEquals("",rideNamingPreview(c,naming.copy(parts=emptySet()),details))
        } finally { Locale.setDefault(oldLocale);java.util.TimeZone.setDefault(oldZone);host.pause().stop().destroy() }
    }
    @Test fun openingTrainingDetailsRevealsItsHeadingWithoutChangingSelection() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val c=host.get()
        try {
            val themes=ThemeManager(c.getSharedPreferences("bike",0));var writes=0
            val body=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL }
            body.addView(View(c),LinearLayout.LayoutParams(-1,500))
            val header=BicycleSectionHeader(c,themes,c.getString(R.string.training_help_title)).apply { revealOnOpen=true }
            val help=TrainingHelpView(c,themes,DashboardTrainingHelp()) { writes++ }.apply { revealAnchor=header }
            body.addView(header);body.addView(help);body.addView(View(c),LinearLayout.LayoutParams(-1,800))
            val scroll=ScrollView(c).apply { addView(body);isSmoothScrollingEnabled=false };c.setContentView(scroll);layout(scroll,600)
            help.findViewWithTag<View>("training_help_expand").performClick();layout(scroll,600)
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(header.top,scroll.scrollY)
            assertEquals(View.VISIBLE,help.findViewWithTag<View>("training_help_details").visibility);assertEquals(0,writes)
            header.accordion(help,false);scroll.scrollTo(0,0);header.performClick();layout(scroll,600)
            assertEquals(header.top,scroll.scrollY);assertEquals(View.VISIBLE,help.visibility);assertEquals(0,writes)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun mapDividerPrecedesTrackToggleAndPreservesPreferences() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val before=prefs.getBoolean("mapTrackVisible",true)
            val root=MainActivity::class.java.getDeclaredMethod("mapControls").apply { isAccessible=true }.invoke(a) as LinearLayout
            val track=root.findViewWithTag<View>("mapTrackVisible")
            assertEquals("map_track_divider",root.getChildAt(root.indexOfChild(track)-1).tag)
            assertEquals(before,prefs.getBoolean("mapTrackVisible",true))
        } finally { host.pause().stop().destroy() }
    }
}

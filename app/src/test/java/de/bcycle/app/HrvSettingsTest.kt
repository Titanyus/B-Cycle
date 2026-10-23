package de.bcycle.app

import android.view.View
import android.widget.SeekBar
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class HrvSettingsTest {
    @Test fun protocolRangesAndPortableSettingsRoundTripWithoutChangingDefaults() {
        val c=RuntimeEnvironment.getApplication();val settings=HrvSettings(c)
        assertEquals(HrvMeasurementConfig(),settings.read())
        for(rest in 15..60 step 15) for(measure in 60..300 step 15) {
            settings.write(HrvMeasurementConfig(rest,measure));assertEquals(measure*1000L,settings.read().protocol.measureMs)
        }
        assertThrows(IllegalArgumentException::class.java) { settings.write(HrvMeasurementConfig(16,60)) }
        val theme=ThemeManager(c.getSharedPreferences("bike",0));val backup=SettingsBackup(c,theme)
        settings.write(settings.read().copy(showOnFirstStart=true,vibrateAtEnd=true,soundAtEnd=true))
        HrvDailyLaunch(c).acknowledge()
        val bytes=backup.export(setOf(BackupSection.PROFILE));assertFalse(bytes.toString(Charsets.UTF_8).contains("handledDay"));settings.write(HrvMeasurementConfig())
        val doc=backup.decode(bytes);backup.apply(backup.plan(doc,doc.sections.keys))
        assertEquals(HrvMeasurementConfig(60,300,true,true,true),settings.read())
        val view=HrvSettingsView(c,theme)
        assertEquals(3,view.findViewWithTag<SeekBar>("hrv_rest_seconds").max)
        assertEquals(16,view.findViewWithTag<SeekBar>("hrv_measure_seconds").max)
        for(tag in listOf("hrv_first_start","hrv_vibrate_end","hrv_sound_end")) assertTrue(view.findViewWithTag<android.widget.Switch>(tag).isChecked)
        assertEquals(View.GONE,view.findViewWithTag<View>("hrv_first_start_details").visibility)
        view.findViewWithTag<View>("hrv_first_start_expand").performClick()
        assertEquals(View.VISIBLE,view.findViewWithTag<View>("hrv_first_start_details").visibility)
        view.findViewWithTag<android.widget.Switch>("hrv_sound_end").performClick()
        assertFalse(settings.read().soundAtEnd);assertTrue(settings.read().vibrateAtEnd)
    }
    @Test fun onlyHrvMeasurementHeaderUsesWindowColorAndActiveScreenHasNoOutsideInfo() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();MainActivity::class.java.getDeclaredMethod("showHrv").apply { isAccessible=true }.invoke(a)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val screen=ShadowAlertDialog.getLatestAlertDialog();val root=screen.window!!.decorView
            val header=root.findViewWithTag<View>("bicycle_navigation_header")
            val image=android.graphics.Bitmap.createBitmap(360,64,android.graphics.Bitmap.Config.ARGB_8888)
            header.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(64,View.MeasureSpec.EXACTLY));header.layout(0,0,360,64)
            header.draw(android.graphics.Canvas(image));val color=image.getPixel(1,1);image.recycle()
            assertEquals(RideRuntime.get(a).themes.active.tokens.secondary,color)
            assertFalse(root.findViewWithTag<BicycleFullscreenHost>("bicycle_fullscreen").showHeaderShadow)
            assertEquals(View.GONE,root.findViewWithTag<View>("hrv_status").visibility)
        } finally { host.pause().stop().destroy() }
    }
}

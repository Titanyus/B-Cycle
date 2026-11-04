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
        for((tag,expected) in listOf("hrv_rest_seconds" to "1:00 min","hrv_measure_seconds" to "5:00 min")) {
            val slider=view.findViewWithTag<BicycleSettingSlider>(tag)
            assertEquals(expected,slider.valueText)
            slider.onKeyDown(android.view.KeyEvent.KEYCODE_DPAD_LEFT,android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_LEFT))
        }
        assertEquals(45,settings.read().restSeconds);assertEquals(285,settings.read().measureSeconds)
        assertNull(view.findViewWithTag<View>("hrv_rest_value"));assertNull(view.findViewWithTag<View>("hrv_measure_value"))
        for(tag in listOf("hrv_first_start","hrv_vibrate_end","hrv_sound_end")) assertTrue(view.findViewWithTag<android.widget.Switch>(tag).isChecked)
        assertNull(view.findViewWithTag<View>("hrv_first_start_details"))
        assertNull(view.findViewWithTag<View>("hrv_first_start_expand"))
        val controls=view.findViewWithTag<android.widget.LinearLayout>("hrv_protocol_settings")
        assertEquals(controls.indexOfChild(view.findViewWithTag<View>("hrv_first_start"))+1,controls.indexOfChild(view.findViewWithTag<View>("hrv_vibrate_end")))
        assertEquals(listOf("HRV","Tagesbefinden"),(0 until view.childCount).map { view.getChildAt(it) }.filterIsInstance<BicycleSectionHeader>().map { it.titleView.text.toString() })
        view.findViewWithTag<android.widget.Switch>("hrv_sound_end").performClick()
        assertFalse(settings.read().soundAtEnd);assertTrue(settings.read().vibrateAtEnd)
        val headers=(0 until view.childCount).map { view.getChildAt(it) }.filterIsInstance<BicycleSectionHeader>()
        headers.last().performClick()
        val scroll=BicycleStickyScrollView(c).apply { addView(view) }
        scroll.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));scroll.layout(0,0,720,900)
        for(header in headers) {
            val sample=android.graphics.Bitmap.createBitmap(1,1,android.graphics.Bitmap.Config.ARGB_8888)
            val body=header.cardContent!!
            view.background.setBounds(0,0,view.width,view.height)
            val canvas=android.graphics.Canvas(sample);canvas.translate(-view.width/2f,-body.top.toFloat());view.background.draw(canvas)
            assertEquals(theme.active.tokens.tertiary,sample.getPixel(0,0));sample.recycle()
        }
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

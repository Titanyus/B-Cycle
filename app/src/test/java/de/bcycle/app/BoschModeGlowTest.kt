package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoschModeGlowTest {
    private fun views(view: View): List<View> = listOf(view) + if (view is ViewGroup)
        (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun layout(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }
    @Test fun verifiedMcspSlotsOffUnknownAndBikeSuppliedNamesDoNotGuessAColor() {
        assertEquals(listOf(0xff80ff28.toInt(), 0xff009dff.toInt(), 0xffff36ba.toInt(), 0xffff3030.toInt()),
            (1L..4L).map { BoschModeColors.color(it) })
        listOf(null, -1L, 0L, 5L, 255L).forEach { assertNull(BoschModeColors.color(it)) }
        assertNull(BoschModeColors.color(2, listOf("OFF", "ECO", "OFF")))
        assertNull(BoschModeColors.color(4, listOf("OFF", "ECO")))
        assertEquals(BoschModeColors.color(3), BoschModeColors.color(3, listOf("OFF", "ECO", "TOUR+", "eMTB", "TURBO")))
    }
    @Test fun cachedGradientIsSymmetricFadesInAllDirectionsAndNeverPaintsOutsideDataBounds() {
        var now = 0L
        val glow = BoschModeGlow(24f) { now }
        glow.setBounds(10, 20, 210, 320)
        glow.update(BoschModeColors.color(1)); now = 1500
        val bitmap = Bitmap.createBitmap(230, 360, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.MAGENTA); glow.draw(Canvas(bitmap))
        assertEquals(Color.MAGENTA, bitmap.getPixel(0, 170))
        assertEquals(Color.MAGENTA, bitmap.getPixel(100, 340))
        assertEquals(Color.BLACK, bitmap.getPixel(100, 170))
        val center = Color.green(bitmap.getPixel(11, 170))
        assertTrue(center > 40)
        assertEquals(center, Color.green(bitmap.getPixel(208, 170)))
        assertTrue(center > Color.green(bitmap.getPixel(11, 25)))
        assertTrue(center > Color.green(bitmap.getPixel(28, 170)))
        val shader = glow.leftShader
        repeat(50) { glow.update(BoschModeColors.color(1)) }
        assertSame(shader, glow.leftShader)
        glow.setBounds(10, 20, 210, 160); assertNotSame(shader, glow.leftShader)
        glow.update(null); now += 1500; assertNull(glow.leftShader)
        bitmap.eraseColor(Color.MAGENTA); glow.draw(Canvas(bitmap)); assertEquals(Color.BLACK, bitmap.getPixel(11, 80))
    }
    @Test fun topPlacementMovesTheLightWithoutResettingModeOrBatteryBreathing() {
        var now=0L
        val glow=BoschModeGlow(24f) { now };glow.setBounds(0,0,200,300)
        glow.update(Color.BLUE);now=1500
        val b=Bitmap.createBitmap(200,300,Bitmap.Config.ARGB_8888)
        glow.place(ModeGlowPlacement.TOP);glow.draw(Canvas(b))
        assertTrue(Color.blue(b.getPixel(100,2))>0)
        assertEquals(Color.BLACK,b.getPixel(2,150))
        assertEquals(Color.BLACK,b.getPixel(100,80))
        assertTrue(Color.blue(b.getPixel(100,2))>Color.blue(b.getPixel(2,2)))
        val shader=glow.leftShader;glow.place(ModeGlowPlacement.TOP);assertSame(shader,glow.leftShader)
        glow.update(null,10);now=3500;glow.draw(Canvas(b))
        assertTrue(Color.red(b.getPixel(100,2))>0);assertTrue(Color.red(b.getPixel(100,2))>Color.blue(b.getPixel(100,2)))
        assertEquals(Color.BLACK,b.getPixel(2,150))
        glow.place(ModeGlowPlacement.SIDES);glow.draw(Canvas(b))
        assertTrue(Color.red(b.getPixel(2,150))>0);assertEquals(Color.BLACK,b.getPixel(100,2))
    }
    @Test fun placementChoiceIsSavedImmediatelyAndRestoredInDesignSettings() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();val prefs=a.getSharedPreferences("bike",0)
            assertEquals(ModeGlowPlacement.SIDES,ModeGlowSettings.placement(prefs))
            fun open():android.app.AlertDialog {
                views(a.window.decorView).single { it.contentDescription==a.getString(R.string.ui_menu_open) }.performClick()
                val menu=ShadowAlertDialog.getLatestAlertDialog()
                views(menu.window!!.decorView).single { it.tag=="settings_PROFILES" }.performClick()
                return ShadowAlertDialog.getLatestAlertDialog().also {
                    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                }
            }
            var screen=open()
            screen.window!!.decorView.findViewWithTag<android.widget.Spinner>("mode_glow_placement").setSelection(ModeGlowPlacement.TOP.ordinal)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(ModeGlowPlacement.TOP,ModeGlowSettings.placement(prefs))
            val dashboard=views(a.window.decorView).filterIsInstance<DashboardView>().single()
            assertEquals(ModeGlowPlacement.TOP,dashboard.modeGlow.placement)
            screen.dismiss();ShadowAlertDialog.getLatestAlertDialog().dismiss()
            screen=open()
            assertEquals(ModeGlowPlacement.TOP.ordinal,screen.window!!.decorView.findViewWithTag<android.widget.Spinner>("mode_glow_placement").selectedItemPosition)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun transitionsFadeFor1500msIncludingOffAndInterruptedChangesWithoutShaderChurn() {
        var now = 0L
        val glow = BoschModeGlow(24f) { now }; glow.setBounds(0, 0, 200, 300)
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        fun pixel(): Int { glow.draw(Canvas(bitmap)); return bitmap.getPixel(18, 150) }
        glow.update(BoschModeColors.color(1)); assertEquals(Color.BLACK, pixel())
        now = 750; val halfway = pixel(); assertTrue(Color.green(halfway) > 0)
        val shader = glow.leftShader
        now = 1500; val bright = pixel(); assertTrue(Color.green(bright) > Color.green(halfway))
        assertSame(shader, glow.leftShader)
        glow.update(BoschModeColors.color(2)); assertEquals(bright, pixel())
        now = 2250; val mixed = pixel(); assertTrue(Color.green(mixed) > 0); assertTrue(Color.blue(mixed) > 0)
        glow.update(null); val interrupted = pixel()
        assertTrue(kotlin.math.abs(Color.green(mixed) - Color.green(interrupted)) <= 3)
        now = 3000; assertTrue(Color.green(pixel()) < Color.green(interrupted))
        now = 3750; assertEquals(Color.BLACK, pixel())
    }
    @Test fun batteryBreathBlendsToWarningWidensAtPeakAndRepeatsWithoutShaderChurn() {
        var now = 0L
        val glow = BoschModeGlow(24f) { now }; glow.setBounds(0, 0, 200, 300)
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        fun pixel(x: Int = 4): Int { glow.draw(Canvas(bitmap)); return bitmap.getPixel(x, 150) }
        glow.update(BoschModeColors.color(2)); now = 1500
        val base = pixel(); assertEquals(Color.BLACK, pixel(36))
        glow.update(BoschModeColors.color(2), 20); val shader = glow.warningLeft
        assertEquals(base, pixel())
        now = 2500; val half = pixel()
        assertTrue(Color.red(half) > 0 && Color.blue(half) > 0)
        repeat(30) { glow.update(BoschModeColors.color(2), 19) }
        assertSame(shader, glow.warningLeft)
        now = 3500; val peak = pixel()
        assertTrue(Color.red(peak) > Color.red(half)); assertEquals(0, Color.blue(peak))
        assertTrue(Color.red(pixel(36)) > 0)
        now = 5500; assertEquals(base, pixel()); assertEquals(Color.BLACK, pixel(36))
        glow.update(BoschModeColors.color(2), 10); now = 7500
        assertTrue(Color.red(pixel()) > Color.green(pixel()))
        glow.update(BoschModeColors.color(2), 21)
        assertNull(glow.warningColor); assertEquals(base, pixel())
    }
    @Test fun lowBatteryWarnsWithModeDisabledAndStopsSchedulingWhenHiddenOrCleared() {
        var now = 0L
        val glow = BoschModeGlow(24f) { now }; glow.setBounds(0, 0, 200, 300)
        var schedules = 0
        glow.callback = object : android.graphics.drawable.Drawable.Callback {
            override fun invalidateDrawable(who: android.graphics.drawable.Drawable) {}
            override fun scheduleDrawable(who: android.graphics.drawable.Drawable, what: Runnable, whenMillis: Long) { schedules++ }
            override fun unscheduleDrawable(who: android.graphics.drawable.Drawable, what: Runnable) {}
        }
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        fun pixel(): Int { glow.draw(Canvas(bitmap)); return bitmap.getPixel(4, 150) }
        glow.update(null, 10); assertNull(glow.modeColor); assertEquals(Color.BLACK, pixel())
        now = 1000; val half = pixel(); assertTrue(Color.red(half) > 0)
        now = 2000; assertTrue(Color.red(pixel()) > Color.red(half))
        now = 4000; assertEquals(Color.BLACK, pixel())
        assertTrue(schedules > 0)
        glow.setVisible(false, false); val before = schedules; pixel(); assertEquals(before, schedules)
        glow.setVisible(true, false); pixel(); assertTrue(schedules > before)
        for (level in listOf(null, -1L, 101L, 21L)) {
            glow.update(null, level); val count = schedules
            assertEquals(Color.BLACK, pixel()); assertNull(glow.warningColor); assertEquals(count, schedules)
        }
        glow.update(null, 20); assertEquals(0xffffb000.toInt(), glow.warningColor)
        glow.update(null, 11); assertEquals(0xffffb000.toInt(), glow.warningColor)
        glow.update(null, 0); assertEquals(0xffff3030.toInt(), glow.warningColor)
    }
    @Test fun batteryRetainsLastKnownChargeAcrossDisconnectAndReconnectUntilFreshValues() {
        val runtime = RideRuntime.get(RuntimeEnvironment.getApplication())
        val phase = runtime.connection.javaClass.getDeclaredField("phase").apply { isAccessible = true }
        val live = requireNotNull(phase.type.enumConstants).single { it.toString() == "LIVE" }
        val stopped = requireNotNull(phase.type.enumConstants).single { it.toString() == "STOPPED" }
        assertNull(runtime.currentBikeBattery)
        phase.set(runtime.connection, live)
        runtime.onSamples(listOf(McspSample(LiveMetric.BATTERY.address, 20, true)))
        runtime.onData(LiveData(otherValues = mapOf(LiveMetric.BATTERY to 20L)), true)
        assertEquals(20L, runtime.currentBikeBattery)
        phase.set(runtime.connection, stopped); assertEquals(20L,runtime.currentBikeBattery)
        phase.set(runtime.connection, live)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(61)); assertEquals(20L,runtime.currentBikeBattery)
        runtime.connection.stop();assertEquals(20L,runtime.currentBikeBattery)
        assertEquals(20L,runtime.dashboardBikeData.value(LiveMetric.BATTERY))
        assertNull(runtime.bikeData.value(LiveMetric.BATTERY)) // Cached display charge never enters raw sensor data.
        phase.set(runtime.connection,live);assertEquals(20L,runtime.currentBikeBattery)
        runtime.onData(LiveData(otherValues=mapOf(LiveMetric.BATTERY to 0L)),true);assertEquals(0L,runtime.currentBikeBattery)
        runtime.onData(LiveData(otherValues=mapOf(LiveMetric.BATTERY to 101L)),true);assertEquals(0L,runtime.currentBikeBattery)
    }
    @Test fun everyDashboardKeepsGeometryTouchAndTextWhileGlowTracksActualSizeAndMode() {
        val app = RuntimeEnvironment.getApplication()
        var selected = -1
        val dashboard = DashboardView(app) { selected = it }.apply { setEditing(true) }
        for (template in DashboardTemplate.entries) {
            dashboard.setTemplate(template)
            for (available in listOf(1680, 2100)) {
                val height = dashboard.heightForViewport(1080, available)
                layout(dashboard, 1080, height)
                val before = views(dashboard).map { listOf(it.left, it.top, it.width, it.height) }
                val clicks = views(dashboard).filter { it.isClickable }
                val data = LiveData(otherValues = mapOf(LiveMetric.ASSIST_MODE to 1L))
                dashboard.showModeGlow(true, 1)
                assertEquals(height, dashboard.modeGlow.bounds.height())
                assertEquals(1080, dashboard.modeGlow.bounds.width())
                val cached = dashboard.modeGlow.leftShader
                repeat(10) { dashboard.show(DashboardLayout.defaults, data); dashboard.showModeGlow(true, 1) }
                assertSame(cached, dashboard.modeGlow.leftShader)
                assertEquals(before, views(dashboard).map { listOf(it.left, it.top, it.width, it.height) })
                assertTrue(clicks.first().performClick()); assertTrue(selected >= 0)
                dashboard.showModeGlow(true, 4); assertEquals(BoschModeColors.color(4), dashboard.modeGlow.modeColor)
                dashboard.showModeGlow(false, 4); assertNull(dashboard.modeGlow.modeColor)
                dashboard.showModeGlow(false, 4, batteryPercent = 20)
                assertNull(dashboard.modeGlow.modeColor)
                assertEquals(0xffffb000.toInt(), dashboard.modeGlow.warningColor)
                assertEquals(before, views(dashboard).map { listOf(it.left, it.top, it.width, it.height) })
            }
        }
    }
    @Test fun liveStatusDropsOnDisconnectStalenessAndRestoresOnlyAfterNewModeSample() {
        val app = RuntimeEnvironment.getApplication(); val runtime = RideRuntime.get(app)
        val phase = runtime.connection.javaClass.getDeclaredField("phase").apply { isAccessible = true }
        val live = requireNotNull(phase.type.enumConstants).single { it.toString() == "LIVE" }
        val stopped = requireNotNull(phase.type.enumConstants).single { it.toString() == "STOPPED" }
        phase.set(runtime.connection, live)
        runtime.onSamples(listOf(McspSample(LiveMetric.ASSIST_MODE.address, 2, true)))
        runtime.onData(LiveData(otherValues = mapOf(LiveMetric.ASSIST_MODE to 2L)), true)
        assertEquals(2L, runtime.currentAssistMode)
        phase.set(runtime.connection, stopped); assertNull(runtime.currentAssistMode)
        runtime.onData(LiveData(), false)
        phase.set(runtime.connection, live); assertNull(runtime.currentAssistMode)
        runtime.onSamples(listOf(McspSample(LiveMetric.ASSIST_MODE.address, 3, true)))
        runtime.onData(LiveData(otherValues = mapOf(LiveMetric.ASSIST_MODE to 3L)), true)
        assertEquals(3L, runtime.currentAssistMode)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(61)); assertNull(runtime.currentAssistMode)
    }
    @Test fun switchDefaultsOffChangesImmediatelyAndRestoresWithThemeIndependentColor() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            var activity = controller.get(); val runtime = RideRuntime.get(activity)
            assertFalse(ModeGlowSettings.enabled(runtime.preferences))
            fun open(): AlertDialog {
                views(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
                val menu = ShadowAlertDialog.getLatestAlertDialog()
                views(menu.window!!.decorView).single { it.tag == "settings_PROFILES" }.performClick()
                return ShadowAlertDialog.getLatestAlertDialog()
            }
            val screen = open()
            val toggle = views(screen.window!!.decorView).single { it.tag == "mode_glow_enabled" } as Switch
            assertFalse(toggle.isChecked); toggle.isChecked = true
            testBack(screen); shadowOf(Looper.getMainLooper()).idle()
            assertTrue(ModeGlowSettings.enabled(activity.getSharedPreferences("bike", 0)))
            ShadowAlertDialog.getLatestAlertDialog().dismiss(); shadowOf(Looper.getMainLooper()).idle()
            controller.recreate(); activity = controller.get()
            val next = open()
            val restored = views(next.window!!.decorView).single { it.tag == "mode_glow_enabled" } as Switch
            assertTrue(restored.isChecked); restored.isChecked = false
            runtime.themes.activate("preset:solar-flare"); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(runtime.themes.active.tokens.foreground, restored.currentTextColor)
            assertEquals(0xff80ff28.toInt(), BoschModeColors.color(1))
            testBack(next); shadowOf(Looper.getMainLooper()).idle()
            assertFalse(ModeGlowSettings.enabled(activity.getSharedPreferences("bike", 0)))
        } finally { controller.pause().stop().destroy() }
    }
}

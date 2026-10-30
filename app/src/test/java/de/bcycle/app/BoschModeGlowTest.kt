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
        val center = Color.green(bitmap.getPixel(110, 21))
        assertTrue(center > 40)
        assertTrue(Color.green(bitmap.getPixel(110,319))<center)
        assertTrue(center > Color.green(bitmap.getPixel(11, 21)))
        assertTrue(center > Color.green(bitmap.getPixel(110, 48)))
        val shader = glow.modeShader
        repeat(50) { glow.update(BoschModeColors.color(1)) }
        assertSame(shader, glow.modeShader)
        glow.setBounds(10, 20, 210, 160); assertNotSame(shader, glow.modeShader)
        glow.update(null); now += 1500; assertNull(glow.modeShader)
        bitmap.eraseColor(Color.MAGENTA); glow.draw(Canvas(bitmap)); assertEquals(Color.BLACK, bitmap.getPixel(11, 80))
    }
    @Test fun topPlacementMovesTheLightWithoutResettingModeOrBatteryBreathing() {
        var now=0L
        val glow=BoschModeGlow(24f) { now };glow.setBounds(0,0,200,300)
        glow.update(Color.BLUE);now=1500
        val b=Bitmap.createBitmap(200,300,Bitmap.Config.ARGB_8888)
        glow.draw(Canvas(b))
        assertTrue(Color.blue(b.getPixel(100,2))>0)
        assertEquals(Color.BLACK,b.getPixel(2,150))
        assertEquals(Color.BLACK,b.getPixel(100,80))
        assertTrue(Color.blue(b.getPixel(100,2))>Color.blue(b.getPixel(2,2)))
        val shader=glow.modeShader;glow.update(Color.BLUE);assertSame(shader,glow.modeShader)
        glow.update(null,10);now=3500;glow.draw(Canvas(b))
        assertTrue(Color.red(b.getPixel(100,2))>0);assertTrue(Color.red(b.getPixel(100,2))>Color.blue(b.getPixel(100,2)))
        assertEquals(Color.BLACK,b.getPixel(2,150))
        assertEquals(Color.BLACK,b.getPixel(2,150))
    }
    @Test fun modeIsAlwaysAtTopAndNoLongerConfigurableInDesignSettings() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();val prefs=a.getSharedPreferences("bike",0)
            prefs.edit().putBoolean("modeGlowEnabled",false).putString("modeGlowPlacement","SIDES").commit()
            MainActivity::class.java.getDeclaredMethod("render").apply { isAccessible=true }.invoke(a)
            val dashboard=views(a.window.decorView).filterIsInstance<DashboardView>().single()
            MainActivity::class.java.getDeclaredMethod("dashboardSettings").apply { isAccessible=true }.invoke(a)
            val screen=ShadowAlertDialog.getLatestAlertDialog()
            assertNull(screen.window!!.decorView.findViewWithTag<View>("mode_glow_placement"))
            assertNull(screen.window!!.decorView.findViewWithTag<View>("mode_glow_enabled"))
            screen.dismiss()
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun transitionsFadeFor1500msIncludingOffAndInterruptedChangesWithoutShaderChurn() {
        var now = 0L
        val glow = BoschModeGlow(24f) { now }; glow.setBounds(0, 0, 200, 300)
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        fun pixel(): Int { glow.draw(Canvas(bitmap)); return bitmap.getPixel(100,18) }
        glow.update(BoschModeColors.color(1)); assertEquals(Color.BLACK, pixel())
        now = 750; val halfway = pixel(); assertTrue(Color.green(halfway) > 0)
        val shader = glow.modeShader
        now = 1500; val bright = pixel(); assertTrue(Color.green(bright) > Color.green(halfway))
        assertSame(shader, glow.modeShader)
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
        fun pixel(depth: Int = 4): Int { glow.draw(Canvas(bitmap)); return bitmap.getPixel(100,depth) }
        glow.update(BoschModeColors.color(2)); now = 1500
        val base = pixel(); assertEquals(Color.BLACK, pixel(36))
        glow.update(BoschModeColors.color(2), 20); val shader = glow.warningShader
        assertEquals(base, pixel())
        now = 2500; val half = pixel()
        assertTrue(Color.red(half) > 0 && Color.blue(half) > 0)
        repeat(30) { glow.update(BoschModeColors.color(2), 19) }
        assertSame(shader, glow.warningShader)
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
        fun pixel(): Int { glow.draw(Canvas(bitmap)); return bitmap.getPixel(100,4) }
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
                val cached = dashboard.modeGlow.modeShader
                repeat(10) { dashboard.show(DashboardLayout.defaults, data); dashboard.showModeGlow(true, 1) }
                assertSame(cached, dashboard.modeGlow.modeShader)
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
    @Test fun legacyOffCannotDisableMandatoryModeAndThemeDoesNotChangeFunctionalColours() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            runtime.preferences.edit().putBoolean("modeGlowEnabled",false).commit()
            val phase=runtime.connection.javaClass.getDeclaredField("phase").apply { isAccessible=true }
            phase.set(runtime.connection,phase.type.enumConstants!!.single { it.toString()=="LIVE" })
            runtime.onSamples(listOf(McspSample(LiveMetric.ASSIST_MODE.address,1,true)))
            runtime.onData(LiveData(otherValues=mapOf(LiveMetric.ASSIST_MODE to 1L)),true)
            for(flat in listOf(true,false)) {
                runtime.themes.setFlatDesign(flat);runtime.themes.activate("preset:solar-flare")
                MainActivity::class.java.getDeclaredMethod("render").apply { isAccessible=true }.invoke(activity)
                val dashboard=views(activity.window.decorView).filterIsInstance<DashboardView>().single()
                    assertEquals(0xff80ff28.toInt(),dashboard.modeGlow.modeColor)
            }
        } finally { controller.pause().stop().destroy() }
    }
}

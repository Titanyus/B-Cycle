package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.view.View
import android.widget.Spinner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BikeLightStatusTest {
    private fun layout(v:View,w:Int=360,h:Int=40) { v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h) }
    @Test fun unchangedLightPersistsDuringConnectionButNotAfterDisconnectOrReconnectWithoutNewState() {
        val runtime=RideRuntime.get(org.robolectric.RuntimeEnvironment.getApplication())
        val phase=runtime.connection.javaClass.getDeclaredField("phase").apply { isAccessible=true }
        fun live() { phase.set(runtime.connection,phase.type.enumConstants!!.single { it.toString()=="LIVE" }) }
        fun light(value:Long) {
            runtime.onSamples(listOf(McspSample(LiveMetric.BIKE_LIGHT.address,value,true)))
            runtime.onData(LiveData.sample(LiveMetric.BIKE_LIGHT,value),true)
        }
        live();light(1)
        android.os.SystemClock.sleep(5*60_000)
        assertEquals(true,runtime.currentBikeLight)
        assertEquals(1L,runtime.dashboardBikeData.value(LiveMetric.BIKE_LIGHT))
        light(0);assertEquals(false,runtime.currentBikeLight)
        light(1);android.os.SystemClock.sleep(61_000);assertEquals(true,runtime.currentBikeLight)
        runtime.connection.stop();assertNull(runtime.currentBikeLight)
        live();assertNull(runtime.currentBikeLight)
        light(0);assertEquals(false,runtime.currentBikeLight)
        runtime.connection.stop()
    }
    @Test fun profileCopyAndBackupDiscardRetiredPlacementAndRejectInvalidLegacyValues() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);prefs.edit().clear().commit()
            val themes=ThemeManager(prefs);val bank=BikeUiConfigurationStore(prefs);bank.ensure(listOf(0,1))
            prefs.edit().putString("bikeLightPlacement","TOP").commit();bank.save(0)
            bank.apply(1);assertFalse(prefs.contains("bikeLightPlacement"))
            bank.copy(0,1);assertTrue(DashboardFullscreen.enabled(prefs))
            val backup=SettingsBackup(a,themes);val saved=backup.export(setOf(BackupSection.DESIGN))
            prefs.edit().remove("bikeLightPlacement").commit()
            backup.apply(backup.plan(backup.decode(saved),setOf(BackupSection.DESIGN)))
            assertTrue(DashboardFullscreen.enabled(prefs))
            val invalid=JSONObject(String(saved));invalid.getJSONObject("sections").getJSONObject(BackupSection.DESIGN.id).put("bikeLightPlacement","SIDE")
            assertThrows(SettingsBackupFailure::class.java) { backup.decode(invalid.toString().toByteArray()) }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun realDesignPageDoesNotOfferLightOrBoschPlacementChoices() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get()
            MainActivity::class.java.getDeclaredMethod("dashboardSettings").apply { isAccessible=true }.invoke(a)
            val dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            assertNull(dialog.window!!.decorView.findViewWithTag<View>("bike_light_settings"))
            assertNull(dialog.window!!.decorView.findViewWithTag<View>("mode_glow_enabled"))
            dialog.dismiss()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun goldLightHasFifteenDpGapToNotchAndDoesNotOverlapTheLeftSlots() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("gold-light",0));val view=DashboardStatusStrip(a,themes)
            a.setContentView(view);layout(view);view.insets(0,0,listOf(Rect(165,0,195,30)),Rect(340,0,360,25))
            assertNull(view.lightBounds());view.bikeLightOn=true
            assertEquals(150f,view.lightBounds()!!.right,0f)
            assertEquals(24f,view.lightBounds()!!.width(),0f);assertTrue(view.regions()[0].second<=view.lightBounds()!!.left-6)
            for(flat in listOf(true,false)) {
                themes.setFlatDesign(flat)
                val image=Bitmap.createBitmap(360,40,Bitmap.Config.ARGB_8888);view.draw(Canvas(image))
                val pixels=IntArray(360*40);image.getPixels(pixels,0,360,0,0,360,40)
                assertTrue(pixels.any { Color.alpha(it)>200 && Color.red(it)>240 && Color.green(it) in 180..220 && Color.blue(it)<120 })
                java.io.File("build/reports/ui/bike-light-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
            assertTrue(view.contentDescription.toString().contains(a.getString(R.string.ui_bike_light_on)))
            view.bikeLightOn=false;assertNull(view.lightBounds());assertEquals("",view.contentDescription)
            view.bikeLightOn=true;view.insets(0,0,emptyList());assertEquals(165f,view.lightBounds()!!.right,0f)
        } finally { host.pause().stop().destroy() }
    }
}

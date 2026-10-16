package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33], qualifiers="de-rDE-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrainingZoneUiTest {
    private val context get()=RuntimeEnvironment.getApplication()
    @Test fun editorPersistsGlowPerTileAndRestoresSelectionOnReopen() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val root=activity.findViewById<View>(android.R.id.content)
            val dashboard=testDescendants(root).filterIsInstance<DashboardView>().single()
            val tile=dashboard.findViewWithTag<View>("dashboard_tile_0")
            tile.performLongClick();tile.performClick();shadowOf(Looper.getMainLooper()).idle()
            val dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            val picker=testDescendants(dialog.window!!.decorView).filterIsInstance<ZoneGlowPicker>().single()
            picker.findViewWithTag<View>("zone_glow_POWER_AVERAGE").performClick()
            val prefs=RideRuntime.get(activity).preferences
            assertEquals(ZoneGlowSource.POWER_AVERAGE,ZoneGlowConfig.read(prefs,0).sources[0])
            assertTrue(ZoneGlowConfig.read(prefs,1).sources.all { it==ZoneGlowSource.OFF })
            testBack(dialog);tile.performClick();shadowOf(Looper.getMainLooper()).idle()
            val reopened=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            val restored=testDescendants(reopened.window!!.decorView).filterIsInstance<ZoneGlowPicker>().single()
            assertEquals(ZoneGlowSource.POWER_AVERAGE,restored.selected)
            assertEquals(DashboardLayout.defaults.encode(),prefs.getString("dashboard",DashboardLayout.defaults.encode()))
            restored.findViewWithTag<View>("zone_glow_OFF").performClick()
            assertEquals(ZoneGlowSource.OFF,ZoneGlowConfig.read(prefs,0).sources[0])
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun sourceOptionsFollowMainMeasurementAndChangingFamilyClearsPersistedMismatch() {
        val app=context;val prefs=app.getSharedPreferences("bike",0)
        prefs.edit().putString("dashboard",DashboardLayout.defaults.replace(0,DashboardField("HEART_RATE")).encode())
            .putString(ZoneGlowConfig.key(0),ZoneGlowConfig().replace(0,ZoneGlowSource.POWER_CURRENT).encode()).commit()
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val root=activity.findViewById<View>(android.R.id.content)
            val tile=root.findViewWithTag<View>("dashboard_tile_0")
            assertEquals(ZoneGlowSource.OFF,ZoneGlowConfig.read(prefs,0).sources[0])
            tile.performLongClick();tile.performClick();shadowOf(Looper.getMainLooper()).idle()
            val screen=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            val glow=testDescendants(screen.window!!.decorView).filterIsInstance<ZoneGlowPicker>().single()
            assertNull(glow.findViewWithTag<View>("zone_glow_POWER_CURRENT"))
            glow.findViewWithTag<View>("zone_glow_HEART_CURRENT").performClick()
            assertEquals(ZoneGlowSource.HEART_CURRENT,ZoneGlowConfig.read(prefs,0).sources[0])
            val source=screen.window!!.decorView.findViewWithTag<Spinner>("field_source")
            source.setSelection((0 until source.count).first { source.getItemAtPosition(it)=="E-Bike" })
            shadowOf(Looper.getMainLooper()).idle()
            screen.window!!.decorView.findViewWithTag<View>("RIDER_POWER").performClick()
            assertEquals(ZoneGlowSource.OFF,ZoneGlowConfig.read(prefs,0).sources[0])
            tile.performClick();shadowOf(Looper.getMainLooper()).idle()
            val reopened=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            val power=testDescendants(reopened.window!!.decorView).filterIsInstance<ZoneGlowPicker>().single()
            assertNull(power.findViewWithTag<View>("zone_glow_HEART_CURRENT"))
            assertNotNull(power.findViewWithTag<View>("zone_glow_POWER_CURRENT"))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun profilePowerZonesUpdateWithFtpAndHideForInvalidInputs() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get();val themes=ThemeManager(activity.getSharedPreferences("zones",0))
            val profile=RiderProfileView(activity,themes,RiderProfile(),DialogNavigation())
            activity.setContentView(profile)
            val rows=profile.findViewWithTag<LinearLayout>("rider_power_zones")
            val ftp=profile.findViewWithTag<EditText>("rider_ftp")
            assertEquals(View.GONE,rows.visibility)
            ftp.setText("250");assertEquals(View.VISIBLE,rows.visibility);assertEquals(7,rows.childCount)
            assertEquals("0–137",profile.findViewWithTag<TextView>("rider_power_zone_value_1").text.toString())
            assertEquals("Ab 376",profile.findViewWithTag<TextView>("rider_power_zone_value_7").text.toString())
            ftp.setText("200");assertEquals("0–110",profile.findViewWithTag<TextView>("rider_power_zone_value_1").text.toString())
            ftp.setText("bad");assertEquals(View.GONE,rows.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun visualizationSitsBelowGraphRetainsOneSourceAndSurvivesCategoryChanges() {
        val themes=ThemeManager(context.getSharedPreferences("picker-zones",0))
        val graph=GraphOverlayPicker(context,themes,listOf(AnalysisChart.SPEED))
        val glow=ZoneGlowPicker(context,themes,ZoneGlowSource.OFF)
        var chosen: DashboardField?=null
        val picker=FieldPickerView(context,LiveData(),GpsValues(),null,PulseValues(),DashboardField("SPEED"),false,themes,graph,glow,
            TrainingSnapshot(RiderProfile(ftpW=200),100.0)) { chosen=it }
        val source=picker.findViewWithTag<Spinner>("field_source")
        fun views()=testDescendants(picker)
        assertEquals(View.GONE,graph.findViewWithTag<View>("graph_overlay_content").visibility)
        assertEquals(View.GONE,glow.findViewWithTag<View>("zone_glow_content").visibility)
        glow.findViewWithTag<View>("zone_glow_toggle").performClick()
        glow.findViewWithTag<View>("zone_glow_POWER_CURRENT").performClick()
        glow.findViewWithTag<View>("zone_glow_HEART_AVERAGE").performClick()
        assertEquals(1,testDescendants(glow).filterIsInstance<RadioButton>().count { it.isChecked })
        assertEquals(ZoneGlowSource.HEART_AVERAGE,glow.selected)
        assertEquals(listOf(AnalysisChart.SPEED),graph.selected);assertNull(chosen)
        for(i in 0 until source.count) {
            source.setSelection(i);shadowOf(Looper.getMainLooper()).idle()
            assertTrue(views().indexOf(graph)<views().indexOf(glow))
            assertEquals(ZoneGlowSource.HEART_AVERAGE,glow.selected)
        }
        assertEquals("Leistung",source.selectedItem)
        val row=picker.findViewWithTag<RadioButton>(TrainingMetric.TRAINING_POWER_ZONE.name)
        assertEquals("Leistungszone",row.text.toString());row.performClick()
        assertEquals(DashboardField(TrainingMetric.TRAINING_POWER_ZONE.name),chosen)
    }
    @Test fun settingsBackupRoundTripsAllPagesPresetsAndRejectsInvalidGlowSources() {
        val prefs=context.getSharedPreferences("backup-zones",0);val route=context.getSharedPreferences("route-zones",0)
        val backup=SettingsBackup(prefs,route,ThemeManager(prefs))
        val configs=(0..3).map { page -> ZoneGlowConfig().replace(page,ZoneGlowSource.entries[page+1]) }
        configs.forEachIndexed { i,c -> prefs.edit().putString(ZoneGlowConfig.key(i),c.encode())
            .putString("dashboardTemplate_$i",DashboardTemplate.entries[i].name).commit() }
        prefs.edit().putString("dashboard",DashboardLayout.defaults.replace(1,DashboardField("TRAINING_POWER_ZONE")).encode()).commit()
        val bytes=backup.export(setOf(BackupSection.DASHBOARD))
        val plan=backup.plan(backup.decode(bytes),setOf(BackupSection.DASHBOARD))
        configs.forEachIndexed { i,c -> assertEquals(c.encode(),plan.bike[ZoneGlowConfig.key(i)]) }
        assertTrue(plan.bike["dashboard"].toString().contains("TRAINING_POWER_ZONE"))
        val json=JSONObject(String(bytes));val pages=json.getJSONObject("sections").getJSONObject("dashboard").getJSONArray("pages")
        repeat(4) { pages.getJSONObject(it).remove("zoneGlowSources") }
        val old=backup.plan(backup.decode(json.toString().toByteArray()),setOf(BackupSection.DASHBOARD))
        assertFalse(old.bike.keys.any { it.startsWith("dashboardZoneGlow") })
        pages.getJSONObject(0).put("zoneGlowSources",JSONArray(List(8) { "INVALID" }))
        try { backup.decode(json.toString().toByteArray());fail("Invalid sources must be rejected") } catch(e:SettingsBackupFailure) { assertEquals(BackupProblem.INVALID,e.problem) }
        configs.forEachIndexed { i,c -> assertEquals(c,ZoneGlowConfig.read(prefs,i)) }
    }
    @Test fun zoneAndModeGlowUseTheSameLightIntensityAndFalloff() {
        var now=0L
        val zone=ZoneAmbientGlow { now };zone.setBounds(0,0,200,100);zone.update(Color.BLUE)
        val mode=BoschModeGlow(40f/1.5f) { now };mode.setBounds(0,0,200,200);mode.update(Color.BLUE)
        now=1500
        val a=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        val b=Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888)
        zone.draw(Canvas(a));mode.draw(Canvas(b))
        for(distance in listOf(4,16,24,32)) assertEquals(Color.blue(b.getPixel(distance,100)).toDouble(),Color.blue(a.getPixel(100,100-distance)).toDouble(),12.0)
    }
    @Test fun ambientGlowFadesWithoutJumpIsTransparentAboveFortyPercentAndAtSides() {
        var now=0L;val glow=ZoneAmbientGlow { now };glow.setBounds(0,0,200,100)
        glow.update(Color.BLUE);assertEquals(Color.TRANSPARENT,glow.currentColor())
        now=1500;assertEquals(Color.BLUE,glow.currentColor())
        fun image(): Bitmap=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888).also { glow.draw(Canvas(it)) }
        val b=image()
        assertEquals(0,Color.alpha(b.getPixel(100,20)));assertEquals(0,Color.alpha(b.getPixel(100,59)))
        assertTrue(Color.alpha(b.getPixel(100,95)) > 100)
        assertEquals(0,Color.alpha(b.getPixel(0,99)));assertEquals(0,Color.alpha(b.getPixel(199,99)))
        glow.update(Color.RED);assertEquals(Color.BLUE,glow.currentColor())
        now=2250;val between=glow.currentColor();assertTrue(Color.red(between)>0);assertTrue(Color.blue(between)>0)
        glow.update(Color.GREEN);assertEquals(between,glow.currentColor())
        now=3750;assertEquals(Color.GREEN,glow.currentColor())
        glow.update(null);now=4500;assertTrue(Color.alpha(glow.currentColor()) in 1..254)
        now=5250;assertEquals(0,Color.alpha(glow.currentColor()))
    }
    @Test fun dashboardZoneValueAndGlowAreIndependentFromMainMetricGraphsAndModeWarning() {
        val view=DashboardView(context) {};val profile=RiderProfile(ftpW=200,restingHeartRate=50,maximumHeartRate=150)
        val snapshot=TrainingSnapshot(profile,180.0,130.0)
        view.showTraining(ZoneGlowConfig().replace(0,ZoneGlowSource.HEART_CURRENT),snapshot)
        val layout=DashboardLayout.defaults.replace(0,DashboardField("TRAINING_POWER_ZONE"))
        view.show(layout,LiveData());val tile=view.findViewWithTag<View>("dashboard_tile_0")
        assertTrue(testDescendants(tile).filterIsInstance<TextView>().any { it.text.toString()=="Z3" })
        view.showGraphs(GraphOverlayConfig(),null)
        view.showModeGlow(false,null,batteryPercent=10)
        assertNotNull(view.modeGlow.warningColor)
        assertTrue(tile.background is android.graphics.drawable.LayerDrawable)
    }
}

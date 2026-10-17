package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfilesTest {
    private val c get()=RuntimeEnvironment.getApplication()
    @After fun settle() {
        val r=RideRuntime.get(c);r.stopRecording();r.setVisible(false)
        val end=System.nanoTime()+10_000_000_000L
        do { Shadows.shadowOf(Looper.getMainLooper()).idle();if(!r.saving && !r.mapState.busy) return;Thread.sleep(10) } while(System.nanoTime()<end)
        fail("Workers did not settle")
    }
    @Test fun migrationSeedsEveryExistingBikeAndSwitchRestoresAllDisplayLayersWithoutTouchingAccounts() {
        val prefs=c.getSharedPreferences("migration",0)
        val old=BikeProfileStore(c,prefs);old.add();old.add()
        prefs.edit().remove(BikeUiConfigurationStore.KEY)
            .putString("mapSource","OPENTOPOMAP").putFloat("mapLatitude",48.25f)
            .putString("map_thunderforest_test_global","secret").putString("oauthSecret","global")
            .putString("dashboard_5","existing-fields").putString("dashboardTemplate_5","HERO_SEVEN")
            .putString(DashboardTrainingHelp.key(5),DashboardTrainingHelp(mode=TrainingHelpMode.ZONE_TWO).encode()).apply()
        val migrated=BikeProfileStore(c,prefs)
        for(slot in listOf(1,2,0)) {
            migrated.select(slot)
            assertEquals("OPENTOPOMAP",prefs.getString("mapSource",null))
            assertEquals(48.25f,prefs.getFloat("mapLatitude",0f),0f)
            assertEquals("existing-fields",prefs.getString("dashboard_5",null))
            assertEquals(TrainingHelpMode.ZONE_TWO,DashboardTrainingHelp.read(prefs,5).mode)
        }
        prefs.edit().putString("mapSource","CYCLOSM").putBoolean(DashboardFullscreen.key,false)
            .putString("dashboard_5","changed").remove(DashboardTrainingHelp.key(5)).apply()
        migrated.select(1)
        assertEquals("OPENTOPOMAP",prefs.getString("mapSource",null))
        assertEquals("existing-fields",prefs.getString("dashboard_5",null))
        migrated.select(0)
        assertEquals("CYCLOSM",prefs.getString("mapSource",null))
        assertFalse(prefs.getBoolean(DashboardFullscreen.key,true))
        assertEquals("changed",prefs.getString("dashboard_5",null))
        assertFalse(prefs.contains(DashboardTrainingHelp.key(5)))
        assertEquals("global",prefs.getString("oauthSecret",null))
        assertFalse(prefs.getString(BikeUiConfigurationStore.KEY,"")!!.contains("secret"))
        val reopened=BikeProfileStore(c,prefs);reopened.select(2);reopened.select(0)
        assertEquals("changed",prefs.getString("dashboard_5",null))
    }
    @Test fun copyingPreservesTargetIdentityPhotoAndSensorAssignmentsButCopiesConfiguration() {
        val r=RideRuntime.get(c);val p=r.preferences
        r.bikes.save(r.bikes.current().copy(name="Source",wheelMm=2300,bikeAddress="00:11:22:33:44:55",heartAddress="00:11:22:33:44:66"))
        r.saveConfig(RecordingConfig(setOf("RIDER_POWER","HEART_RATE")))
        r.themes.activate(r.themes.presets[1].id);val sourceTheme=r.themes.active.id
        p.edit().putString("mapSource","OPENTOPOMAP").putString(DashboardTrainingHelp.key(0),DashboardTrainingHelp(mode=TrainingHelpMode.ZONE_TWO).encode()).apply()
        assertTrue(r.addBike());val target=r.bikes.activeSlot
        r.bikes.save(r.bikes.current().copy(name="Target",wheelMm=2100,bikeAddress="00:11:22:33:44:77",heartAddress="00:11:22:33:44:88"))
        r.themes.activate(BicycleThemes.red.id)
        p.edit().putString("mapSource","CYCLOSM").putString("oauthSecret","untouched").apply()
        val photo=BikePhotos.file(c,target).apply { parentFile!!.mkdirs();writeText("photo-bytes") }
        assertTrue(r.copyProfile(0))
        val copied=r.bikes.current()
        assertEquals(target,copied.slot);assertEquals("Target",copied.name);assertEquals(2300,copied.wheelMm)
        assertEquals("00:11:22:33:44:77",copied.bikeAddress);assertEquals("00:11:22:33:44:88",copied.heartAddress)
        assertTrue("RIDER_POWER" in r.config.fields);assertTrue("HEART_RATE" in r.config.fields)
        assertEquals("photo-bytes",photo.readText());assertEquals(sourceTheme,r.themes.active.id)
        assertEquals("OPENTOPOMAP",p.getString("mapSource",null));assertEquals("untouched",p.getString("oauthSecret",null))
        assertEquals(TrainingHelpMode.ZONE_TWO,DashboardTrainingHelp.read(p,0).mode)
        assertFalse(r.copyProfile(target));assertFalse(r.copyProfile(999))
        assertTrue(r.switchBike(0));assertEquals("Source",r.bikes.current().name)
        assertEquals("00:11:22:33:44:55",r.bikes.current().bikeAddress)
        r.setGpsEnabled(false);r.beginRecording();assertFalse(r.copyProfile(target));assertFalse(r.switchBike(target))
    }
    private fun awaitRoutes(r:RideRuntime) {
        val end=System.nanoTime()+10_000_000_000L
        do { Shadows.shadowOf(Looper.getMainLooper()).idle();if(!r.mapState.busy) return;Thread.sleep(5) } while(System.nanoTime()<end)
        fail("Route worker did not settle")
    }
    @Test fun selectedGpxRouteIsRestoredPerProfileAndMissingRoutesAreNotInvented() {
        val library=GpxLibrary(c.filesDir);library.load()
        library.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(),"Teststrecke")
        val r=RideRuntime.get(c);awaitRoutes(r)
        val id=r.mapState.selectedRoute
        assertNotNull(id);assertNotNull(r.mapState.route)
        assertTrue(r.addBike());awaitRoutes(r);val other=r.bikes.activeSlot
        r.mapState.selectRoute(null);awaitRoutes(r);assertNull(r.mapState.route)
        assertTrue(r.switchBike(0));awaitRoutes(r)
        assertEquals(id,r.mapState.selectedRoute);assertNotNull(r.mapState.route)
        assertTrue(r.switchBike(other));awaitRoutes(r)
        assertNull(r.mapState.selectedRoute);assertNull(r.mapState.route)
        r.mapState.deleteRoute(id!!);awaitRoutes(r)
        assertTrue(r.switchBike(0));awaitRoutes(r)
        assertNull(r.mapState.route)
    }
    @Test fun pendingOldSpinnerCallbacksDoNotOverwriteNewProfiles() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();val r=RideRuntime.get(a);val p=r.preferences
            r.saveConfig(RecordingConfig(setOf("RIDER_POWER")))
            p.edit().putString(ModeGlowSettings.placementKey,ModeGlowPlacement.entries[0].name).apply()
            assertTrue(r.addBike());val second=r.bikes.activeSlot
            r.saveConfig(RecordingConfig(setOf("HEART_RATE")))
            p.edit().putString(ModeGlowSettings.placementKey,ModeGlowPlacement.entries[1].name).apply()
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            root.findViewWithTag<View>("bike_profile_0").performClick()
            root.findViewWithTag<View>("bike_profile_$second").performClick()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertEquals(setOf("HEART_RATE","GPS_POSITION"),r.config.fields)
            assertEquals(ModeGlowPlacement.entries[1],ModeGlowSettings.placement(p))
            root.findViewWithTag<View>("bike_profile_0").performClick()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertEquals(setOf("RIDER_POWER","GPS_POSITION"),r.config.fields)
            assertEquals(ModeGlowPlacement.entries[0],ModeGlowSettings.placement(p))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun copyDropdownOffersOnlyOtherProfilesAndPreservesName() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();val r=RideRuntime.get(a)
            r.bikes.save(r.bikes.current().copy(name="Vorlage",wheelMm=2400))
            assertTrue(r.addBike());val name=r.bikes.current().name
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            root.findViewWithTag<View>("profile_copy").performClick()
            val popup=org.robolectric.shadows.ShadowListPopupWindow.getLatestListPopupWindow()
            assertTrue(popup.isShowing)
            assertEquals(1,popup.listView!!.adapter.count)
            assertEquals("Vorlage",popup.listView!!.adapter.getItem(0))
            val list=popup.listView!!
            list.performItemClick(list.adapter.getView(0,null,list),0,0)
            assertFalse(popup.isShowing)
            assertEquals(name,r.bikes.current().name);assertEquals(2400,r.bikes.current().wheelMm)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun sensorDetailsExpandIndependentlyOfSwitchAndRecordingSourcesShareRows() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val r=RideRuntime.get(a)
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            val toggle=root.findViewWithTag<Switch>("bike_sensor_EBIKE")
            val details=root.findViewWithTag<View>("bike_sensor_details_EBIKE")
            val expand=root.findViewWithTag<View>("bike_sensor_expand_EBIKE")
            val enabled=toggle.isChecked
            assertEquals(View.GONE,details.visibility)
            expand.performClick();assertEquals(View.VISIBLE,details.visibility);assertEquals(enabled,toggle.isChecked)
            assertTrue(testDescendants(details).any { it.tag=="bike_disconnect_EBIKE" })
            expand.performClick();assertEquals(View.GONE,details.visibility);assertEquals(enabled,toggle.isChecked)
            val headings=testDescendants(root).filterIsInstance<BicycleSectionHeader>()
            for(id in listOf(R.string.ui_recording_title,R.string.ui_track_recording,R.string.ui_bosch_mode))
                assertEquals(1,headings.count { it.contentDescription==a.getString(id) })
            assertFalse(headings.any { it.contentDescription==a.getString(R.string.bikes_fit_heading) })
            val fit=root.findViewWithTag<View>("bike_fit_settings")
            val choices=testDescendants(fit).filterIsInstance<Spinner>()
            assertEquals(2,choices.size)
            choices.forEach { spinner ->
                val row=spinner.parent as LinearLayout
                assertEquals(LinearLayout.HORIZONTAL,row.orientation)
                assertEquals(android.graphics.Typeface.DEFAULT,(row.getChildAt(0) as TextView).typeface)
            }
            val placement=root.findViewWithTag<Spinner>("mode_glow_placement")
            assertEquals(a.getString(R.string.ui_glow_visible),((placement.parent as LinearLayout).getChildAt(0) as TextView).text.toString())
        } finally { host.pause().stop().destroy() }
    }
    @Test fun longProfileHasSingleScrollerFixedActionsAndEditableName() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();val r=RideRuntime.get(a)
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            assertEquals(1,testDescendants(root).filterIsInstance<ScrollView>().size)
            assertEquals(0,testDescendants(root).filterIsInstance<ProfileAccordion>().size)
            val fixed=root.findViewWithTag<View>("bicycle_fullscreen_footer")
            assertNotNull(fixed.findViewWithTag<View>("profile_copy"))
            assertNull(fixed.findViewWithTag<View>("dashboard_delete"))
            val inline=root.findViewWithTag<View>("dashboard_inline_actions")
            assertTrue(inline.isShown)
            val name=root.findViewWithTag<TextView>("bike_profile_name")
            assertFalse(name.parent is BicycleSectionHeader)
            root.findViewWithTag<View>("bike_rename").performClick()
            root.findViewWithTag<EditText>("bike_profile_name_edit").setText("Mein Profil")
            assertEquals("Mein Profil",r.bikes.current().name)
            assertEquals(View.GONE,root.findViewWithTag<View>("bike_delete").visibility)
            assertFalse(root.findViewWithTag<View>("profile_copy").isEnabled)
            assertNull(fixed.findViewWithTag<View>("dashboard_editor_open"))
            assertNotNull(inline.findViewWithTag<View>("dashboard_editor_open"))
            root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
            root.layout(0,0,360,800)
            val image=android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(image))
            java.io.File("/tmp/bcycle-profiles-overview.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            image.recycle()
        } finally { controller.pause().stop().destroy() }
    }
}

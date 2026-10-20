package de.bcycle.app

import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapPanelTest {
    private fun descendants(v: View): List<View> = listOf(v) + if (v is ViewGroup)
        (0 until v.childCount).flatMap { descendants(v.getChildAt(it)) } else emptyList()
    private fun measure(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }
    private fun speed(panel: MapPanel, mps: Double) {
        val runtime = RideRuntime.get(panel.context)
        val now = SystemClock.elapsedRealtime()
        runtime.ride.gps.accept(GpsFix(now, 51.0, 10.0, 3.0, speedMps = mps), now)
        panel.refresh()
    }
    private fun hideWhileMoving(panel: MapPanel) {
        speed(panel, 5.0)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
        speed(panel, 5.0)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
    }
    @Test fun stationaryAndUnknownSpeedKeepControlsVisibleAndStoppingRestoresThem() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val toolbar = descendants(root).single { it.tag == "ride_toolbar" }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
            assertEquals(View.VISIBLE, toolbar.visibility)
            hideWhileMoving(panel)
            assertEquals(View.GONE, toolbar.visibility)
            speed(panel, 0.0)
            assertEquals(View.VISIBLE, toolbar.visibility)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
            assertEquals(View.VISIBLE, toolbar.visibility)
            hideWhileMoving(panel)
            assertEquals(View.GONE, toolbar.visibility)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
            panel.refresh()
            assertEquals(View.VISIBLE, toolbar.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun nightButtonSitsBesideTrackPersistsAndHidesTogetherWithoutChangingDashboard() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            var root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val night = descendants(panel).single { it.tag == "map_night" }
            val full = descendants(panel).single { it.contentDescription == "Karte im Vollbild" }
            val row = full.parent as View
            val track=descendants(panel).single { it.tag=="map_route_toggle" }
            assertEquals(track.top,night.top);assertEquals(track.bottom,night.bottom)
            assertEquals(track.right+BicycleDesign.dp(panel.context,2),night.left)
            assertTrue(night.right<=row.left)
            for(height in listOf(180,240,360)) {
                measure(panel,1080,BicycleDesign.dp(panel.context,height))
                assertEquals(track.top,night.top);assertEquals(track.bottom,night.bottom)
                assertEquals(BicycleDesign.dp(panel.context,6),panel.width-row.right)
                assertTrue(night.right<=row.left)
            }
            measure(panel,BicycleDesign.dp(panel.context,280),BicycleDesign.dp(panel.context,140))
            assertTrue(night.right<=row.left)
            assertTrue(row.right<=panel.width)
            measure(root,1080,2100)
            val dashboard = descendants(root).filterIsInstance<DashboardView>().single()
            val geometry = listOf(dashboard.width, dashboard.height, dashboard.top)
            val center = panel.mapView.model.mapViewPosition.center
            val zoom = panel.mapView.model.mapViewPosition.zoomLevel
            val rawLayer = panel.mapView.layerManager.layers[0]
            night.performClick(); measure(root, 1080, 2100)
            assertEquals("Tageskarte einschalten", night.contentDescription)
            assertTrue((rawLayer as MapNightLayer).night)
            assertSame(rawLayer, panel.mapView.layerManager.layers[0])
            assertEquals(center, panel.mapView.model.mapViewPosition.center)
            assertEquals(zoom, panel.mapView.model.mapViewPosition.zoomLevel)
            assertEquals(geometry, listOf(dashboard.width, dashboard.height, dashboard.top))
            hideWhileMoving(panel); assertEquals(View.GONE, night.visibility)
            speed(panel, 0.0); assertEquals(View.VISIBLE, night.visibility)
            controller.recreate(); root = controller.get().findViewById(android.R.id.content)
            measure(root, 1080, 2100)
            val restored = descendants(root).single { it.tag == "map_night" }
            assertEquals("Tageskarte einschalten", restored.contentDescription)
            restored.performClick()
            assertEquals("Nachtkarte einschalten", restored.contentDescription)
            assertFalse(RideRuntime.get(controller.get()).preferences.getBoolean("mapNight", true))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun routeShortcutMatchesSettingsWidthAndThemeAndOpensTheManagerDirectly() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val toolbar = descendants(root).single { it.tag == "ride_toolbar" } as ViewGroup
            assertEquals(4, toolbar.childCount)
            val route = toolbar.getChildAt(2) as android.widget.ImageButton
            val settings = toolbar.getChildAt(3)
            assertEquals("Strecken öffnen", route.contentDescription)
            assertEquals(route.width.toDouble(), settings.width.toDouble(),1.0)
            val runtime = RideRuntime.get(activity); runtime.themes.activate("preset:electric-blue")
            val theme = runtime.themes.active.tokens
            assertEquals(theme.foreground, route.imageTintList!!.defaultColor)
            assertEquals(theme.secondary, (route.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
            assertEquals(theme.secondary, (settings.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
            route.performClick()
            val dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            assertTrue(descendants(dialog.window!!.decorView).any { it is RouteManagerView })
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.performClick()
            dialog.dismiss()
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun controlsHideAfterFiveSecondsAndFullscreenReusesMap() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val full = descendants(panel).single { it.contentDescription == "Karte im Vollbild" }
            val controls = full.parent as View
            val toolbar = descendants(root).single { it.tag == "ride_toolbar" }
            val dashboard = descendants(root).filterIsInstance<DashboardView>().single()
            val dataHeight = dashboard.height
            val mapHeight = panel.height
            hideWhileMoving(panel)
            assertEquals(View.GONE, controls.visibility)
            assertEquals(View.GONE, toolbar.visibility)
            measure(root, 1080, 2100)
            assertEquals(dataHeight, dashboard.height)
            assertTrue(panel.height > mapHeight)
            panel.setFollow(false)
            val now = SystemClock.uptimeMillis()
            MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 100f, 100f, 0).also { panel.mapView.dispatchTouchEvent(it); it.recycle() }
            MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, 100f, 100f, 0).also { panel.mapView.dispatchTouchEvent(it); it.recycle() }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400))
            assertEquals(View.VISIBLE, controls.visibility)
            assertEquals(View.VISIBLE, toolbar.visibility)
            measure(root, 1080, 2100)
            assertEquals(dataHeight, dashboard.height)
            assertEquals(mapHeight, panel.height)
            assertFalse(controller.get().getSharedPreferences("bike", 0).getBoolean("mapFollow", true))
            full.performClick(); measure(root, 1080, 2100)
            assertEquals(View.GONE, descendants(root).filterIsInstance<DashboardView>().single().visibility)
            assertSame(panel, descendants(root).filterIsInstance<MapPanel>().single())
            full.performClick(); measure(root, 1080, 2100)
            assertEquals(View.VISIBLE, descendants(root).filterIsInstance<DashboardView>().single().visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun cameraAndMarkerInterpolateIndependentlyWhilePauseAndStandstillCancelFrames() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content); measure(root,1080,2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val runtime = RideRuntime.get(controller.get())
            fun fix(lon: Double, speed: Double) {
                val now = SystemClock.elapsedRealtime()
                assertTrue(runtime.ride.gps.accept(GpsFix(now,51.0,lon,3.0,speed,.2,bearingDegrees=90.0,bearingAccuracyDegrees=3.0),now))
                panel.refresh()
            }
            fix(10.0,5.0)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
            fix(10.00005,5.0)
            val raw = runtime.gpsValues
            assertEquals(10.0,panel.mapView.model.mapViewPosition.center.longitude,.0000001)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(800))
            val midway = panel.mapView.model.mapViewPosition.center.longitude
            assertTrue(midway > 10.0 && midway < raw.longitude!!)
            assertEquals(raw.longitude,runtime.gpsValues.longitude)
            panel.setFollow(false)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))
            assertEquals(midway,panel.mapView.model.mapViewPosition.center.longitude,.0000001)
            fix(10.0001,5.0)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(800))
            val displayed=MapPanel::class.java.getDeclaredField("displayedGps").apply { isAccessible=true }.get(panel) as GpsValues
            assertTrue(displayed.longitude!!>raw.longitude!! && displayed.longitude!!<runtime.gpsValues.longitude!!)
            assertEquals(midway,panel.mapView.model.mapViewPosition.center.longitude,.0000001)
            panel.setFollow(true); panel.pause(); panel.resume()
            assertEquals(runtime.gpsValues.longitude!!,panel.mapView.model.mapViewPosition.center.longitude,.0000001)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
            fix(10.0001,5.0); panel.pause()
            val paused = panel.mapView.model.mapViewPosition.center
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
            assertEquals(paused,panel.mapView.model.mapViewPosition.center)
            panel.resume()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
            fix(10.00012,5.0); panel.cameraVisible = { false }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            assertEquals(runtime.gpsValues.longitude!!,panel.mapView.model.mapViewPosition.center.longitude,.0000001)
            panel.cameraVisible = { true }
            // The GPS acceleration plausibility filter needs a realistic deceleration interval.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))
            fix(10.00015,0.0)
            assertFalse(runtime.isMoving)
            assertEquals(runtime.gpsValues.longitude!!,panel.mapView.model.mapViewPosition.center.longitude,.0000001)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun ovalButtonKeepsNorthUpMovesGpsAnchorAndManualPanStillPreservesCamera() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root=controller.get().findViewById<ViewGroup>(android.R.id.content);measure(root,1080,2100)
            val panel=descendants(root).filterIsInstance<MapPanel>().single()
            val runtime=RideRuntime.get(controller.get())
            val button=panel.findViewWithTag<View>("map_oval_follow")
            assertFalse(button.isActivated)
            fun fix(index:Int,course:Double) {
                val now=SystemClock.elapsedRealtime()
                assertTrue(runtime.ride.gps.accept(GpsFix(now,51.0-index*.000045,10.0,3.0,5.0,.2,
                    bearingDegrees=course,bearingAccuracyDegrees=3.0),now))
                panel.refresh()
            }
            fix(0,180.0)
            button.performClick()
            assertTrue(button.isActivated);assertTrue(runtime.preferences.getBoolean(MapOvalAnchor.preference,false))
            assertTrue(runtime.preferences.getBoolean("mapHeading",true))
            for(i in 1..8) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));fix(i,180.0) }
            assertEquals(0f,panel.mapView.mapRotation.degrees,.001f)
            assertEquals(.5f,panel.mapView.mapViewCenterX,.01f);assertEquals(1f/3,panel.mapView.mapViewCenterY,.01f)
            val pose=MapPanel::class.java.getDeclaredField("displayedGps").apply { isAccessible=true }.get(panel) as GpsValues
            val pixel=panel.mapView.mapViewProjection.toPixels(org.mapsforge.core.model.LatLong(pose.latitude!!,pose.longitude!!),true)
            assertEquals(panel.width*panel.mapView.mapViewCenterX,pixel.x.toFloat(),1f)
            assertEquals(panel.height*panel.mapView.mapViewCenterY,pixel.y.toFloat(),1f)
            panel.setFollow(false)
            val centre=panel.mapView.model.mapViewPosition.center
            val x=panel.mapView.mapViewCenterX;val y=panel.mapView.mapViewCenterY
            for(i in 9..10) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));fix(i,90.0) }
            assertEquals(centre,panel.mapView.model.mapViewPosition.center)
            assertEquals(x,panel.mapView.mapViewCenterX,0f);assertEquals(y,panel.mapView.mapViewCenterY,0f)
            panel.setFollow(true);button.performClick()
            assertFalse(button.isActivated);assertFalse(runtime.preferences.getBoolean(MapOvalAnchor.preference,true))
            assertEquals(.5f,panel.mapView.mapViewCenterX,0f);assertEquals(.68f,panel.mapView.mapViewCenterY,.001f)
            assertTrue(runtime.preferences.getBoolean("mapHeading",true))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun phoneCompassOnlyRunsForVisibleMapAndDoesNotControlGpsCameraHeading() {
        val app=org.robolectric.RuntimeEnvironment.getApplication()
        val manager=app.getSystemService(android.hardware.SensorManager::class.java)
        val sensor=org.robolectric.shadows.ShadowSensor.newInstance(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        shadowOf(manager).addSensor(sensor)
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root=controller.get().findViewById<ViewGroup>(android.R.id.content);measure(root,1080,2100)
            val panel=descendants(root).filterIsInstance<MapPanel>().single()
            val runtime=RideRuntime.get(controller.get())
            assertTrue(shadowOf(manager).listeners.isEmpty())
            val now=SystemClock.elapsedRealtime()
            runtime.ride.gps.accept(GpsFix(now,51.0,10.0,3.0,5.0,.2,bearingDegrees=90.0,bearingAccuracyDegrees=3.0),now)
            panel.refresh()
            assertEquals(1,shadowOf(manager).listeners.size)
            val before=panel.mapView.mapRotation.degrees
            val event=org.robolectric.shadows.ShadowSensorManager.createSensorEvent(4,android.hardware.Sensor.TYPE_ROTATION_VECTOR)
            event.sensor=sensor;event.accuracy=android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            event.timestamp=SystemClock.elapsedRealtimeNanos();event.values[3]=1f
            shadowOf(manager).sendSensorEventToListeners(event)
            assertEquals(before,panel.mapView.mapRotation.degrees,.001f)
            assertEquals(90.0,runtime.gpsValues.bearingDegrees!!,.001)
            panel.cameraVisible={false};panel.refresh()
            assertTrue(shadowOf(manager).listeners.isEmpty())
            panel.cameraVisible={true};panel.refresh()
            assertEquals(1,shadowOf(manager).listeners.size)
            panel.pause();assertTrue(shadowOf(manager).listeners.isEmpty())
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun headingUpCameraKeepsGpsInLowerThirdAndNorthSettingResetsRotation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val runtime = RideRuntime.get(controller.get())
            val now = SystemClock.elapsedRealtime()
            runtime.ride.gps.accept(GpsFix(now, 51.0, 10.0, 3.0, 5.0, .2, bearingDegrees = 90.0, bearingAccuracyDegrees = 3.0), now)
            panel.setFollow(true); panel.refresh()
            assertEquals(-90f, panel.mapView.mapRotation.degrees, .001f)
            val pixel = panel.mapView.mapViewProjection.toPixels(org.mapsforge.core.model.LatLong(51.0, 10.0), true)
            assertEquals(panel.width / 2.0, pixel.x, 1.0)
            assertEquals(panel.height * .68, pixel.y, 1.0)
            descendants(panel).single { it.contentDescription == "Fahrtrichtung oben · wechseln zu Norden" }.performClick()
            assertEquals(0f, panel.mapView.mapRotation.degrees, .001f)
            assertEquals(2f / 3f, panel.mapView.mapViewCenterY, .001f)
            panel.setFollow(false)
            val manualCenter=panel.mapView.model.mapViewPosition.center
            descendants(panel).single { it.contentDescription==controller.get().getString(R.string.ui_map_north_to_heading) }.performClick()
            assertEquals(-90f,panel.mapView.mapRotation.degrees,.001f)
            assertEquals(manualCenter,panel.mapView.model.mapViewPosition.center)
        } finally { controller.pause().stop().destroy() }
    }
    @Test @Config(qualifiers = "xxhdpi") fun mapControlsUseRequestedCornersToggleRouteAndHideTogether() {
        val app = RuntimeEnvironment.getApplication()
        val library = GpxLibrary(app.filesDir); library.load()
        library.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(), "Test.gpx")
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            assertFalse(descendants(panel).any { it.contentDescription == "Nächstes Dashboard" || it.contentDescription == "Vorheriges Dashboard" })
            val center = descendants(panel).single { it.contentDescription == "GPS-Zentrierung aktivieren" }
            assertEquals(4, (center.parent as ViewGroup).childCount)
            val zoom = descendants(panel).single { it.tag == "map_zoom_controls" } as ViewGroup
            assertEquals(2, zoom.childCount)
            assertTrue(zoom.getChildAt(0).top < zoom.getChildAt(1).top)
            assertEquals("Karte vergrößern", zoom.getChildAt(0).contentDescription)
            assertTrue(zoom.left > panel.width / 2)
            val state = RideRuntime.get(controller.get()).mapState
            val deadline = System.nanoTime() + 10_000_000_000L
            while (state.busy && System.nanoTime() < deadline) { Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle() }
            assertFalse(state.busy); panel.refresh()
            val routes = descendants(panel).single { it.contentDescription == "Strecke ausblenden" }
            assertTrue(routes.left < panel.width / 2)
            routes.performClick(); panel.refresh()
            assertFalse(RideRuntime.get(controller.get()).preferences.getBoolean("mapGpxVisible", true))
            assertEquals(library.selected, state.selectedRoute)
            assertEquals("Strecke einblenden", routes.contentDescription)
            routes.performClick(); panel.refresh()
            assertTrue(RideRuntime.get(controller.get()).preferences.getBoolean("mapGpxVisible", false))
            hideWhileMoving(panel)
            assertEquals(View.GONE, zoom.visibility); assertEquals(View.GONE, routes.visibility)
            panel.revealControls(); measure(root, 1080, 2100)
            assertEquals(View.VISIBLE, zoom.visibility); assertEquals(View.VISIBLE, routes.visibility)
            val topControls = center.parent as View
            assertTrue(topControls.right <= zoom.left || topControls.bottom <= zoom.top)
            assertTrue(panel.mapView.layerManager.layers[1] is RouteTrackLayer)
            val output = java.io.File("build/reports/maps/controls.png").apply { parentFile!!.mkdirs() }
            val image = android.graphics.Bitmap.createBitmap(panel.width, panel.height, android.graphics.Bitmap.Config.ARGB_8888)
            panel.draw(android.graphics.Canvas(image))
            output.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            measure(root, 840, 1680)
            assertTrue(zoom.top >= 0)
            assertTrue(zoom.bottom <= panel.height)
            assertTrue(topControls.right <= zoom.left || topControls.bottom <= zoom.top)
            assertTrue(zoom.getChildAt(0).height > 0 && zoom.getChildAt(1).height > 0)
            val credit = descendants(panel).filterIsInstance<MapCreditView>().single()
            assertEquals(0, credit.left); assertEquals(panel.height, credit.bottom)
            assertTrue(credit.top >= 0)
            assertEquals(-90f, credit.label.rotation, 0f)
            assertEquals(MapSource.CYCLOSM.attribution, credit.text.toString())
            assertTrue((0 until credit.label.layout.lineCount).all { credit.label.layout.getEllipsisCount(it) == 0 })
            val source = RideRuntime.get(controller.get()).preferences
            source.edit().putString("mapSource", MapSource.OSM.name).commit(); panel.refreshSettings()
            assertTrue(panel.mapView.layerManager.layers[1] is RouteTrackLayer)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun fourPagesHaveOneResizableMapAndIndependentPersistedFields() {
        val app = RuntimeEnvironment.getApplication()
        val prefs = app.getSharedPreferences("bike", 0)
        prefs.edit().clear().putString("dashboard_1", DashboardLayout.defaults.replace(0, DashboardField("HEART_RATE")).encode()).commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val smallHeight = panel.height
            val expected = listOf(4, 2, 3, 7)
            expected.forEachIndexed { index, count ->
                swipeTestDashboard(root); measure(root, 1080, 2100)
                val dashboard = descendants(root).filterIsInstance<DashboardView>().single()
                assertEquals(count, descendants(dashboard).count { it.isClickable })
                assertSame(panel, descendants(root).filterIsInstance<MapPanel>().single())
                assertTrue(panel.height > 0)
                if (index == 0) {
                    assertTrue(panel.height > smallHeight)
                    assertTrue(descendants(dashboard).first { it.isClickable }.contentDescription.toString().startsWith("Herzfrequenz:"))
                }
            }
            assertEquals(0, prefs.getInt("dashboardPage", -1))
            assertNull(prefs.getString("dashboard", null))
        } finally { controller.pause().stop().destroy() }
    }
}

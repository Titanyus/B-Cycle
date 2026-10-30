package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.model.Model
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThreeMapTracksTest {
    private val ids=(0..5).map { java.util.UUID.nameUUIDFromBytes("map-$it".toByteArray()).toString() }
    private fun drain(state:MapState) {
        (MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(state) as ExecutorService).submit {}.get(15,TimeUnit.SECONDS)
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }
    private fun seed(context:android.content.Context,selected:Boolean=false) {
        val library=GpxLibrary(context.filesDir).apply { load() }
        for(i in 0..3) library.add(("<gpx><trk><trkseg><trkpt lat='${50+i*.001}' lon='8'/><trkpt lat='${50+i*.001}' lon='8.002'/></trkseg></trk></gpx>").byteInputStream(),"Route $i",select=selected && i==0,stableId=ids[i])
    }
    @Test fun stableNumbersKeepStylesAndFifoOrderAcrossVisibilityChangesAndRoundTrips() {
        var slots=MapRouteSlots().add(ids[0]).add(ids[1]).add(ids[2])
        assertEquals(listOf(ids[0],ids[1],ids[2]),slots.ids)
        slots=slots.show(1,false).add(ids[2]) // Revisiting does not make a route younger.
        slots=slots.add(ids[3]);assertEquals(listOf(ids[3],ids[1],ids[2]),slots.ids)
        assertEquals(listOf(1,2,0),slots.order);assertFalse(slots.visible[1])
        slots=MapRouteSlots.parse(slots.json()).add(ids[4]);assertEquals(listOf(ids[3],ids[4],ids[2]),slots.ids)
        slots=slots.remove(ids[3]).add(ids[5]);assertEquals(ids[5],slots.ids[0]);assertEquals(listOf(2,1,0),slots.order)
        assertEquals(slots,MapRouteSlots.parse(slots.json()))
    }
    @Test fun explicitAssignmentReplacesOnlyTheChosenSlotAndMovingASelectionFreesItsOldSlot() {
        var slots=MapRouteSlots().add(ids[0]).add(ids[1]).add(ids[2])
        slots=slots.assign(ids[3],1)
        assertEquals(listOf(ids[0],ids[3],ids[2]),slots.ids)
        slots=slots.assign(ids[0],2)
        assertEquals(listOf(null,ids[3],ids[0]),slots.ids);assertEquals(listOf(1,2),slots.order)
        assertEquals(slots,slots.assign(ids[0],2))
    }
    @Test fun malformedAndDuplicateSlotsAreRejectedBeforeRestore() {
        val good=MapRouteSlots().add(ids[0]).add(ids[1]).json()
        for(bad in listOf(JSONObject(good.toString()).put("version",1.5),JSONObject(good.toString()).put("order",JSONArray(listOf(0,0))),
            JSONObject(good.toString()).put("ids",JSONArray(listOf(ids[0],ids[0],JSONObject.NULL))),
            JSONObject(good.toString()).put("visible",JSONArray(listOf(true,"false",true))),
            JSONObject(good.toString()).put("ids",JSONArray(listOf("../escape",JSONObject.NULL,JSONObject.NULL)))))
            assertThrows(Exception::class.java) { MapRouteSlots.parse(bad) }
    }
    @Test fun libraryDrawsThreeRoutesReplacesTheOldestAndDeletionFreesOnlyItsSlot() {
        val c=RuntimeEnvironment.getApplication();seed(c)
        val state=MapState(c) {};drain(state)
        for(i in 0..2) { state.selectRoute(ids[i]);drain(state) }
        assertEquals(ids.take(3),state.mapSlots.ids);assertEquals(3,state.mapRoutes.size)
        val geometry=state.mapRoutes.single { it.id==ids[2] }.route
        state.setRouteSlotVisible(1);assertFalse(state.routeVisible(ids[1]));assertEquals(2,state.routeNumber(ids[1]))
        assertEquals(setOf(ids[0],ids[2]),RouteQuery().apply { displayedOnly=true }.apply(state.entries,{null},displayedIds=state.visibleRouteIds()).map { it.id }.toSet())
        state.selectRoute(ids[3]);drain(state)
        assertEquals(listOf(ids[3],ids[1],ids[2]),state.mapSlots.ids);assertEquals(1,state.routeNumber(ids[3]));assertNull(state.routeNumber(ids[0]))
        assertFalse(state.routeVisible(ids[1]));assertTrue(state.routeVisible(ids[3]));assertSame(geometry,state.mapRoutes.single { it.id==ids[2] }.route)
        state.setRouteVisible(false);assertTrue(state.visibleRouteIds().isEmpty());assertEquals(3,state.mapRoutes.size)
        state.setRouteSlotVisible(2);assertEquals(setOf(ids[2]),state.visibleRouteIds())
        state.setRouteVisible(true);assertEquals(setOf(ids[1],ids[2],ids[3]),state.visibleRouteIds())
        state.deleteRoute(ids[1]);drain(state);assertNull(state.mapSlots.ids[1]);assertEquals(2,state.mapRoutes.size)
        assertEquals(1,state.routeNumber(ids[3]));assertEquals(3,state.routeNumber(ids[2]))
        state.selectRoute(ids[0]);drain(state);assertEquals(2,state.routeNumber(ids[0]))
        assertFalse(state.busy)
    }
    @Test fun existingSingleSelectionMigratesAndPerBikeSelectionsAndStylesRestore() {
        val c=RuntimeEnvironment.getApplication();seed(c,true)
        val prefs=c.getSharedPreferences("bike",0)
        val bank=BikeUiConfigurationStore(prefs);bank.ensure(listOf(0,1))
        val state=MapState(c) {};drain(state)
        assertEquals(ids[0],state.mapSlots.ids[0]);assertTrue(prefs.contains(MapRouteSlots.KEY))
        state.selectRoute(ids[1]);drain(state);state.selectRoute(ids[2]);drain(state)
        val original=state.mapSlots
        MapRouteStyles.write(prefs,MapLineKind.SAVED_TWO,MapLineStyle(Color.GREEN,2.5f,15));bank.save(0)
        bank.apply(1);state.restoreProfileRoute();drain(state);assertTrue(state.mapRoutes.isEmpty())
        state.selectRoute(ids[3]);drain(state);bank.save(1)
        bank.apply(0);state.restoreProfileRoute();drain(state)
        assertEquals(original,state.mapSlots);assertEquals(3,state.mapRoutes.size)
        assertEquals(Color.GREEN,MapRouteStyles.read(prefs,MapLineKind.SAVED_TWO).color)
        bank.apply(1);state.restoreProfileRoute();drain(state);assertEquals(ids[3],state.mapSlots.ids[0]);assertEquals(1,state.mapRoutes.size)
    }
    @Test fun rapidProfileChangesWhileTheWorkerIsQueuedDoNotOverwriteEitherProfilesSlots() {
        val c=RuntimeEnvironment.getApplication();seed(c)
        val prefs=c.getSharedPreferences("bike",0);val bank=BikeUiConfigurationStore(prefs);bank.ensure(listOf(0,1))
        val state=MapState(c) {};drain(state)
        for(i in 0..2) { state.selectRoute(ids[i]);drain(state) }
        state.setRouteSlotVisible(1);val expected=state.mapSlots;bank.save(0)
        val worker=MapState::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(state) as ExecutorService
        val entered=java.util.concurrent.CountDownLatch(1);val release=java.util.concurrent.CountDownLatch(1)
        worker.submit { entered.countDown();release.await(10,TimeUnit.SECONDS) };assertTrue(entered.await(10,TimeUnit.SECONDS))
        try {
            bank.apply(1);state.restoreProfileRoute();assertTrue(state.mapRoutes.isEmpty())
            bank.apply(0);state.restoreProfileRoute();assertTrue(state.mapRoutes.isEmpty())
        } finally { release.countDown() }
        drain(state);assertEquals(expected,state.mapSlots);assertEquals(expected,MapRouteSlots.read(prefs))
        bank.apply(1);state.restoreProfileRoute();drain(state);assertTrue(state.mapRoutes.isEmpty());assertTrue(state.mapSlots.ids.all { it==null })
    }
    @Test fun mixedFitAndGpxSelectionUsesTheExistingFullAnalysisAndPreservesOriginals() {
        val c=RuntimeEnvironment.getApplication();seed(c)
        val journal=java.io.File(c.cacheDir,"map-fit.ride");val at=1793268000000L
        RideJournal(journal,at,RecordingConfig(setOf("GPS_POSITION","GPS_DISTANCE"))).use { writer ->
            writer.append(RideSample(at,0,50.0,8.0,values=mapOf("GPS_DISTANCE" to 0.0)))
            writer.append(RideSample(at+5000,5000,50.0001,8.0002,values=mapOf("GPS_DISTANCE" to 20.0)))
        }
        val file=java.io.File(c.cacheDir,"map-fit.fit");FitWriter.encode(journal,file)
        val saved=FitLibrary.retainRecording(c.filesDir,file);val original=saved.readBytes()
        val state=MapState(c) {};drain(state)
        state.selectRoute(ids[0]);drain(state);state.selectRoute(ids[1]);drain(state)
        assertTrue(state.mapRoutes.any { it.id==saved.nameWithoutExtension });assertEquals(3,state.mapRoutes.size)
        assertArrayEquals(original,saved.readBytes());assertNotNull(state.route)
    }
    @Test fun backupRestoresAllThreeSlotsVisibilityAgeAndFourIndependentLineStyles() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("map-backup",0)
        val slots=MapRouteSlots().add(ids[0]).add(ids[1]).add(ids[2]).show(1,false).add(ids[3]);slots.write(prefs)
        MapLineKind.entries.forEachIndexed { i,kind -> MapRouteStyles.write(prefs,kind,MapLineStyle(Color.rgb(20*i,40*i,60*i),i+.5f,20*i)) }
        val backup=SettingsBackup(prefs,c.getSharedPreferences("map-export",0),ThemeManager(prefs));val bytes=backup.export(setOf(BackupSection.MAPS))
        prefs.edit().clear().commit();val document=backup.decode(bytes);backup.apply(backup.plan(document,setOf(BackupSection.MAPS)))
        assertEquals(slots,MapRouteSlots.read(prefs));assertEquals(60,MapRouteStyles.read(prefs,MapLineKind.SAVED_THREE).opacity)
        val corrupt=JSONObject(String(bytes));corrupt.getJSONObject("sections").getJSONObject(BackupSection.MAPS.id).getJSONObject("routeSlots").put("order",JSONArray(listOf(0,0)))
        assertThrows(Exception::class.java) { backup.plan(backup.decode(corrupt.toString().toByteArray()),setOf(BackupSection.MAPS)) }
    }
    @Test fun threeOverlaysUseIndependentColoursAndAlphaWithoutReplacingTheLiveTrack() {
        val c=RuntimeEnvironment.getApplication();AndroidGraphicFactory.createInstance(c)
        val model=Model();model.displayModel.setFixedTileSize(256)
        try {
            val size=MercatorProjection.getMapSize(10,256)
            fun point(x:Double,y:Double)=MapPoint(MercatorProjection.pixelYToLatitude(size/2.0-256+y,size),MercatorProjection.pixelXToLongitude(size/2.0-256+x,size))
            val colours=listOf(Color.RED,Color.GREEN,Color.BLUE)
            val routes=(0..2).map { i -> StyledMapRoute(GpxRoute(listOf(point(80.0,80.0+i*80),point(430.0,80.0+i*80)),2,1),MapLineStyle(colours[i],4f,30+i*30)) }
            val layer=RouteTrackLayer(1f);layer.setDisplayModel(model.displayModel)
            layer.update(null,listOf(point(80.0,350.0),point(430.0,350.0)),GpsValues(),true,true,overlays=routes)
            val image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
            layer.draw(BoundingBox(-1.0,-1.0,1.0,1.0),10,AndroidGraphicFactory.createGraphicContext(android.graphics.Canvas(image)),Point(size/2.0-256,size/2.0-256),Rotation.NULL_ROTATION)
            for(i in 0..2) {
                val pixel=image.getPixel(256,80+i*80);assertEquals(colours[i] and 0xffffff,pixel and 0xffffff)
                assertEquals(((30+i*30)*255/100f).toInt().toDouble(),Color.alpha(pixel).toDouble(),1.0)
            }
            assertTrue(Color.alpha(image.getPixel(256,350))>200);image.recycle()
        } finally { model.mapViewPosition.destroy() }
    }
    @Test fun managerShortAndLongEyePressesOfferExplicitNumberedSlotsAndFreeVisibleSelections() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();seed(a);val runtime=RideRuntime.get(a);val state=runtime.mapState;drain(state)
            state.selectRoute(ids[0]);drain(state)
            val manager=RouteManagerView(a,{ state.removeMapRoute(it.id) },runtime.themes)
            var opens=0;var popup:MapRouteSlotPopup?=null
            manager.chooseMapSlot={ anchor,entry ->
                opens++;popup=MapRouteSlotPopup(anchor,runtime,assignId=entry.id) {};manager.showRouteSlots(popup!!)
            }
            fun submit() { manager.submit(state.entries,state.selectedRoute,state.busy,state.mapSlots.ids.mapIndexedNotNull { index,id -> id?.let { it to index+1 } }.toMap(),state.visibleRouteIds()) }
            submit();a.setContentView(manager)
            manager.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY));manager.layout(0,0,360,640)
            fun eye(id:String):View {
                val list=manager.findViewWithTag<ListView>("route_list")
                val position=(0 until list.adapter.count).single { (list.adapter.getItem(it) as ManagedRoute).id==id }
                val row=list.adapter.getView(position,null,list)
                val icon=testDescendants(row).filterIsInstance<android.widget.ImageButton>().single()
                // Anchor must be attached for a real popup window.
                (row.parent as? android.view.ViewGroup)?.removeView(row)
                (manager.parent as? android.view.ViewGroup)?.let { root -> if(root is android.widget.FrameLayout) root.addView(row) }
                return icon
            }
            eye(ids[0]).performLongClick();assertEquals(1,opens);assertTrue(popup!!.isShowing)
            val selected=popup!!.contentView.findViewWithTag<Button>("map_route_slot_0")
            assertTrue(selected.isSelected);assertTrue(selected.text.isEmpty());assertNotNull(selected.foreground)
            assertEquals(a.getString(R.string.map_route_slot_occupied),selected.stateDescription)
            val free=popup!!.contentView.findViewWithTag<Button>("map_route_slot_1")
            assertTrue(free.text.isEmpty());assertNull(free.foreground)
            assertEquals(a.getString(R.string.map_route_slot_available),free.stateDescription)
            popup!!.dismiss()
            eye(ids[0]).performClick();drain(state);assertNull(state.routeNumber(ids[0]));submit()
            eye(ids[1]).performClick();assertEquals(2,opens);assertNull(state.routeNumber(ids[1]))
            popup!!.contentView.findViewWithTag<Button>("map_route_slot_2").performClick();drain(state)
            assertEquals(3,state.routeNumber(ids[1]));assertTrue(state.routeVisible(ids[1]));submit()
            eye(ids[2]).performLongClick();assertEquals(3,opens)
            popup!!.contentView.findViewWithTag<Button>("map_route_slot_2").performClick();drain(state)
            assertNull(state.routeNumber(ids[1]));assertEquals(3,state.routeNumber(ids[2]));assertEquals(1,state.mapRoutes.size)
            submit();state.setRouteSlotVisible(2);submit();eye(ids[2]).performClick();assertEquals(4,opens)
            assertTrue(popup!!.contentView.findViewWithTag<Button>("map_route_slot_2").isSelected)
            popup!!.dismiss()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun dashboardMasterEyeAndLongPressOpenThreeIndividualColouredEyeButtons() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();seed(a);val runtime=RideRuntime.get(a);val state=runtime.mapState;drain(state)
            for(i in 0..2) { state.selectRoute(ids[i]);drain(state) }
            val panel=MapPanel(a,runtime,{}, {}, {})
            a.setContentView(panel)
            panel.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));panel.layout(0,0,360,600)
            val eye=panel.findViewWithTag<Button>("map_route_toggle")
            eye.performClick();assertTrue(state.visibleRouteIds().isEmpty())
            eye.performClick();assertEquals(3,state.visibleRouteIds().size)
            assertTrue(eye.performLongClick())
            val popup=MapPanel::class.java.getDeclaredField("routePopup").apply { isAccessible=true }.get(panel) as MapRouteSlotPopup
            assertTrue(popup.isShowing)
            assertEquals(3,(popup.contentView as android.view.ViewGroup).childCount)
            popup.contentView.findViewWithTag<Button>("map_route_slot_0").performClick();assertFalse(state.routeVisible(ids[0]))
            assertTrue(state.routeVisible(ids[1]));assertTrue(state.routeVisible(ids[2]))
            panel.setEditing(true);assertFalse(popup.isShowing);panel.pause()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun colouredEyesAndVerticalOpaquePopupUseTheSameIndividualVisibilityState() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();seed(a);val runtime=RideRuntime.get(a);val state=runtime.mapState;drain(state)
            for(i in 0..2) { state.selectRoute(ids[i]);drain(state) }
            MapRouteStyles.write(runtime.preferences,MapLineKind.SAVED,MapLineStyle(Color.RED,3f,0))
            MapRouteStyles.write(runtime.preferences,MapLineKind.SAVED_TWO,MapLineStyle(Color.YELLOW,6f,10))
            val manager=RouteManagerView(a,{state.toggleMapRoute(it.id)},runtime.themes)
            val numbers=state.mapSlots.ids.mapIndexedNotNull { i,id -> id?.let { it to i+1 } }.toMap()
            manager.submit(state.entries,state.selectedRoute,false,numbers,state.visibleRouteIds());a.setContentView(manager)
            manager.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY));manager.layout(0,0,360,640)
            val list=manager.findViewWithTag<ListView>("route_list")
            for(i in 0 until list.adapter.count) {
                val entry=list.adapter.getItem(i) as ManagedRoute;val row=list.adapter.getView(i,null,list)
                assertNull(row.findViewWithTag<TextView>("route_map_number"))
                val eye=testDescendants(row).filterIsInstance<android.widget.ImageButton>().single()
                val tint=eye.colorFilter as android.graphics.PorterDuffColorFilter
                val expected=numbers[entry.id]?.let { MapRouteStyles.read(runtime.preferences,MapRouteStyles.saved(it-1)).color }
                    ?: BicycleIcons.color(runtime.themes.active.tokens,BicycleIcon.ShowOnMap,true)
                assertEquals(android.graphics.PorterDuffColorFilter(expected,android.graphics.PorterDuff.Mode.SRC_ATOP),tint)
            }
            val popup=MapRouteSlotPopup(manager,runtime) {};val root=popup.contentView
            assertEquals(android.widget.LinearLayout.VERTICAL,(root as android.widget.LinearLayout).orientation)
            root.measure(View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(250,View.MeasureSpec.AT_MOST));root.layout(0,0,root.measuredWidth,root.measuredHeight)
            val preview=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888);root.draw(android.graphics.Canvas(preview))
            java.io.File("build/reports/ui/map-three-route-buttons.png").apply { parentFile!!.mkdirs() }.outputStream().use { preview.compress(Bitmap.CompressFormat.PNG,100,it) };preview.recycle()
            for(slot in 0..2) {
                val button=root.findViewWithTag<Button>("map_route_slot_$slot")
                assertTrue(button.text.isEmpty());assertEquals(1f,button.alpha,0f)
                assertNotNull(button.foreground)
                if(slot>0) assertTrue(button.top>root.findViewWithTag<Button>("map_route_slot_${slot-1}").bottom)
                val image=Bitmap.createBitmap(button.width,button.height,Bitmap.Config.ARGB_8888);button.background.setBounds(0,0,button.width,button.height);button.background.draw(android.graphics.Canvas(image))
                assertEquals(MapRouteStyles.read(runtime.preferences,MapRouteStyles.saved(slot)).color,image.getPixel(button.width/2,button.height/2));image.recycle()
            }
            root.findViewWithTag<Button>("map_route_slot_1").performClick();assertFalse(state.routeVisible(ids[1]));assertEquals(2,state.routeNumber(ids[1]))
            assertFalse(root.findViewWithTag<Button>("map_route_slot_1").isSelected)
            state.setRouteVisible(false);root.findViewWithTag<Button>("map_route_slot_2").performClick();assertEquals(setOf(ids[2]),state.visibleRouteIds())
            state.setRouteVisible(true);assertEquals(3,state.visibleRouteIds().size)
        } finally { host.pause().stop().destroy() }
    }
}

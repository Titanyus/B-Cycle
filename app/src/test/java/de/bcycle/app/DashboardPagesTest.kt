package de.bcycle.app

import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DashboardPagesTest {
    private fun allViews(v:View):List<View> = listOf(v)+if(v is android.view.ViewGroup) (0 until v.childCount).flatMap { allViews(v.getChildAt(it)) } else emptyList()
    private val c get()=RuntimeEnvironment.getApplication()
    private val prefs get()=c.getSharedPreferences("bike",0)
    @Test fun existingFourPagesMigrateAndNewPagesAreLimitedToSix() {
        prefs.edit().putString("dashboard_2","existing-fields").putString("dashboardGraphs_2","POWER").apply()
        val pages=DashboardPages(c,prefs)
        assertEquals(listOf(0,1,2,3),pages.all().map { it.id })
        assertTrue(pages.add());assertTrue(pages.add());assertFalse(pages.add())
        assertTrue(pages.rename(5,"Indoor Training"));assertEquals(5,pages.selected())
        assertFalse(pages.rename(5," "))
        assertEquals("Indoor Training",DashboardPages(c,prefs).all().last().name)
        assertEquals("existing-fields",prefs.getString("dashboard_2",null))
        assertEquals("POWER",prefs.getString("dashboardGraphs_2",null))
    }
    @Test fun reorderingKeepsSelectedIdAndPerPageSettingsAndPersistsOrder() {
        val pages=DashboardPages(c,prefs);pages.select(2)
        prefs.edit().putString("dashboard_2","retained-fields").apply()
        assertTrue(pages.move(3,0))
        assertEquals(listOf(3,0,1,2),DashboardPages(c,prefs).all().map { it.id })
        assertEquals(2,pages.selected());assertEquals("retained-fields",prefs.getString("dashboard_2",null))
        assertFalse(pages.move(9,0));assertFalse(pages.move(3,6))
    }
    @Test fun dashboardDragPreviewsInsertionAndCancelDoesNotSave() {
        val host=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val picker=DashboardPresetPicker(host.get(),0);host.get().setContentView(picker)
            picker.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY))
            picker.layout(0,0,900,600)
            shadowOf(Looper.getMainLooper()).idle()
            val row=(picker.findViewWithTag<ViewGroup>("dashboard_page_strip")).getChildAt(0) as ViewGroup
            val reorder=DashboardPresetPicker::class.java.getDeclaredField("reorder").apply { isAccessible=true }.get(picker) as TileReorder
            val payload=TileReorder.Payload(reorder,0)
            fun send(action:Int,x:Float):Boolean {
                val method=android.view.DragEvent::class.java.declaredMethods.single { it.name=="obtain" && it.parameterCount==11 }.apply { isAccessible=true }
                val event=method.invoke(null,action,x,20f,0f,0f,payload,null,null,null,null,false) as android.view.DragEvent
                return row.dispatchDragEvent(event)
            }
            val x=(row.getChildAt(1).left+row.getChildAt(1).right)/2f
            assertTrue(row.getChildAt(0).isLongClickable)
            val selectedFrame=row.getChildAt(0).foreground
            send(android.view.DragEvent.ACTION_DRAG_STARTED,10f)
            send(android.view.DragEvent.ACTION_DRAG_LOCATION,x)
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200))
            assertTrue(row.getChildAt(0).translationX>0)
            assertTrue(row.getChildAt(1).translationX<0)
            assertNotNull(row.getChildAt(0).foreground)
            assertEquals(listOf(0,1,2,3),picker.pages.all().map { it.id })
            send(android.view.DragEvent.ACTION_DRAG_ENDED,x)
            assertEquals(0f,row.getChildAt(0).translationX,0f)
            assertSame(selectedFrame,row.getChildAt(0).foreground)
            assertEquals(listOf(0,1,2,3),picker.pages.all().map { it.id })
            send(android.view.DragEvent.ACTION_DRAG_STARTED,10f)
            send(android.view.DragEvent.ACTION_DROP,x)
            send(android.view.DragEvent.ACTION_DRAG_ENDED,x)
            assertEquals(listOf(1,0,2,3),picker.pages.all().map { it.id })
            assertEquals(0,picker.pages.selected())
            assertFalse(picker.findViewWithTag<View>("dashboard_page_add").isLongClickable)
            assertTrue(picker.findViewWithTag<View>("preset_page_0").performAccessibilityAction(R.id.bike_move_left,null))
            assertEquals(listOf(0,1,2,3),picker.pages.all().map { it.id })
        } finally { host.pause().stop().destroy() }
    }
    @Test fun deletingNeverRenumbersAndReusingSlotStartsFresh() {
        val pages=DashboardPages(c,prefs)
        prefs.edit().putString("dashboard_1","removed").putString("dashboardGraphs_1","old").putString("dashboard_2","retained").apply()
        pages.select(1);assertTrue(pages.delete(1))
        assertEquals(listOf(0,2,3),pages.all().map { it.id });assertEquals(0,pages.selected())
        assertEquals("retained",prefs.getString("dashboard_2",null));assertFalse(prefs.contains("dashboard_1"))
        assertTrue(pages.add());assertEquals(1,pages.selected());assertFalse(prefs.contains("dashboardGraphs_1"))
        while(pages.all().size>1) assertTrue(pages.delete(pages.selected()))
        assertFalse(pages.delete(pages.selected()))
    }
    @Test fun pickerShowsNamedCardsAndTenPreviewLayoutsWithIndependentSelection() {
        val host=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val picker=DashboardPresetPicker(host.get(),0);host.get().setContentView(picker)
            picker.findViewWithTag<View>("dashboard_layout_choice").performClick()
            val grid=picker.findViewWithTag<View>("dashboard_layout_details").findViewWithTag<DashboardLayoutGrid>("dashboard_layout_grid")
            assertEquals(14,allViews(grid).count { it.tag?.toString()?.startsWith("preset_")==true && it.isClickable })
            picker.findViewWithTag<View>("dashboard_layout_choice").performClick()
            picker.findViewWithTag<View>("preset_page_1").performClick()
            selectTestDashboardLayout(picker,DashboardTemplate.HERO_SEVEN)
            assertEquals("HERO_SEVEN",prefs.getString("dashboardTemplate_1",null))
            assertNull(prefs.getString("dashboardTemplate_0",null))
            repeat(2) { picker.findViewWithTag<View>("dashboard_page_add").performClick() }
            assertNull(picker.findViewWithTag<View>("dashboard_page_add"))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun sixNamedDashboardsRoundTripThroughSettingsBackup() {
        val pages=DashboardPages(c,prefs);pages.add();pages.add();pages.rename(5,"Trainer")
        prefs.edit().putString("dashboardTemplate_5","HERO_SEVEN")
            .putString("dashboard_5",DashboardLayout.defaults.replace(2,DashboardField("HEART_RATE")).encode())
            .putString(ZoneGlowConfig.key(5),ZoneGlowConfig().encode()).apply()
        val backup=SettingsBackup(c,ThemeManager(prefs))
        val bytes=backup.export(setOf(BackupSection.DASHBOARD))
        val saved=pages.all();prefs.edit().clear().commit()
        backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.DASHBOARD)))
        assertEquals(saved,pages.all());assertEquals(5,pages.selected())
        assertEquals("HERO_SEVEN",prefs.getString("dashboardTemplate_5",null))
        assertEquals("HEART_RATE",DashboardLayout.decode(prefs.getString("dashboard_5",null)).fields[2].id)
    }
    @Test fun recordLongPressSelectsBikeWithoutStartingRecording() {
        val controller=org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            assertTrue(runtime.addBike());val active=runtime.bikes.activeSlot
            val button=activity.window.decorView.findViewWithTag<android.widget.Button>("record_bike_button")
            assertTrue(button.performLongClick())
            val field=MainActivity::class.java.getDeclaredField("bikePicker").apply { isAccessible=true }
            val popup=field.get(activity) as android.widget.PopupWindow
            val strip=popup.contentView.findViewWithTag<BikeProfileStrip>("bike_quick_strip")
            assertEquals(runtime.bikes.all().size,(strip.getChildAt(0) as ViewGroup).childCount)
            val first=runtime.bikes.all().first()
            popup.contentView.findViewWithTag<View>("bike_profile_${first.slot}").performClick()
            assertFalse(popup.isShowing);assertNull(field.get(activity))
            assertNotEquals(active,runtime.bikes.activeSlot);assertEquals(first.slot,runtime.bikes.activeSlot)
            assertFalse(runtime.recording);assertTrue(button.text.contains(first.name))
            assertTrue(button.text is android.text.Spanned)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun bikeUiScopeIncludesThemeDashboardAndMapSettingsButNoAccountsOrRecording() {
        prefs.edit().putString(ThemeManager.activeKey,"embers").putString("dashboard_5","fields")
            .putString("mapSource","OPENTOPOMAP").putBoolean("mapNight",true).putString("mapDataField_0","HEART_RATE")
            .putString("recordSpeed","SPEED").putString("oauthSecret","secret").apply()
        val snapshot=BikeUiConfigurationScope.capture(prefs)
        assertEquals("embers",snapshot.design[ThemeManager.activeKey]);assertEquals("fields",snapshot.design["dashboard_5"])
        assertEquals("HEART_RATE",snapshot.design[MapFieldSelection.key(0)])
        assertEquals("OPENTOPOMAP",snapshot.maps["mapSource"]);assertEquals(true,snapshot.maps["mapNight"])
        assertFalse(snapshot.design.containsKey("oauthSecret"));assertFalse(snapshot.maps.containsKey("recordSpeed"))
        assertEquals("embers",prefs.getString(ThemeManager.activeKey,null))
    }
}

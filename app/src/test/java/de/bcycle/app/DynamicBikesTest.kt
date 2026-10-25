package de.bcycle.app

import android.os.Looper
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DynamicBikesTest {
    private fun measure(view:View,width:Int=1080,height:Int=2100) {
        shadowOf(Looper.getMainLooper()).idle()
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY))
        view.layout(0,0,width,height)
    }
    @Test fun addLimitThreeWideScrollingDeletionConfirmationAndHiddenLastFooter() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(activity)
            val screen=ShadowAlertDialog.getLatestAlertDialog();val decor=screen.window!!.decorView
            measure(decor)
            assertEquals(1,runtime.bikes.all().size)
            assertEquals(View.VISIBLE,decor.findViewWithTag<View>("bicycle_fullscreen_footer").visibility)
            assertEquals(View.GONE,decor.findViewWithTag<View>("bike_delete").visibility)
            assertNotNull(decor.findViewWithTag<View>("bike_rename"))
            repeat(5) { decor.findViewWithTag<View>("bike_profile_add").performClick();measure(decor) }
            assertEquals(6,runtime.bikes.all().size);assertNull(decor.findViewWithTag<View>("bike_profile_add"))
            assertEquals(View.VISIBLE,decor.findViewWithTag<View>("bicycle_fullscreen_footer").visibility)
            for(width in listOf(720,1080)) {
                measure(decor,width)
                val strip=decor.findViewWithTag<BikeProfileStrip>("bike_profile_strip")
                val row=strip.getChildAt(0) as LinearLayout
                val widths=(0 until row.childCount).map { row.getChildAt(it).width }
                assertEquals(1,widths.distinct().size)
                assertEquals(strip.width.toDouble(),(widths.first()*3+BicycleDesign.dp(activity,BicycleDesign.controlGap)*2).toDouble(),2.0)
                assertTrue(row.width>strip.width)
            }
            val removed=runtime.bikes.activeSlot
            decor.findViewWithTag<View>("bike_delete").performClick()
            val confirm=ShadowAlertDialog.getLatestAlertDialog()
            assertNotSame(screen,confirm);assertEquals(6,runtime.bikes.all().size)
            confirm.dismiss();assertEquals(6,runtime.bikes.all().size)
            decor.findViewWithTag<View>("bike_delete").performClick()
            ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
            measure(decor)
            assertEquals(5,runtime.bikes.all().size);assertTrue(runtime.bikes.all().none { it.slot==removed })
            assertNotNull(decor.findViewWithTag<View>("bike_profile_add"))
            repeat(4) {
                decor.findViewWithTag<View>("bike_delete").performClick()
                ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
                measure(decor)
            }
            assertEquals(1,runtime.bikes.all().size)
            assertEquals(View.VISIBLE,decor.findViewWithTag<View>("bicycle_fullscreen_footer").visibility)
            assertEquals(View.GONE,decor.findViewWithTag<View>("bike_delete").visibility)
            assertNotNull(decor.findViewWithTag<View>("bike_rename"))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun gapsSurviveSwitchingBikeAndNameHeaderAlsoRenamesFromFooter() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            runtime.addBike();runtime.addBike()
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(activity)
            val decor=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            for(slot in runtime.bikes.all().map { it.slot }+runtime.bikes.all().first().slot) {
                decor.findViewWithTag<View>("bike_profile_$slot").performClick();measure(decor)
                val strip=decor.findViewWithTag<BikeProfileStrip>("bike_profile_strip")
                val row=strip.getChildAt(0) as android.view.ViewGroup
                for(index in 1 until row.childCount) assertEquals(BicycleDesign.dp(activity,BicycleDesign.controlGap),row.getChildAt(index).left-row.getChildAt(index-1).right)
                measure(decor)
                for(index in 1 until row.childCount) assertEquals(BicycleDesign.dp(activity,BicycleDesign.controlGap),row.getChildAt(index).left-row.getChildAt(index-1).right)
            }
            val title=decor.findViewWithTag<android.widget.TextView>("bike_profile_name")
            assertTrue(title.parent is BicycleSectionHeader)
            assertEquals(true,(title.parent as BicycleSectionHeader).expanded)
            decor.findViewWithTag<View>("bike_rename").performClick()
            val edit=decor.findViewWithTag<android.widget.EditText>("bike_profile_name_edit")
            assertEquals(View.VISIBLE,edit.visibility);edit.setText("Neuer Name")
            assertEquals("Neuer Name",runtime.bikes.current().name)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun dragDropAndAccessibilityReorderWithoutSelectingAnotherProfile() {
        val c=RuntimeEnvironment.getApplication()
        val store=BikeProfileStore(c,c.getSharedPreferences("strip-drag",0))
        repeat(4) { store.add() }
        val theme=ThemeManager(c.getSharedPreferences("strip-theme",0))
        lateinit var strip:BikeProfileStrip
        strip=BikeProfileStrip(c,theme,{ store.select(it) },{}, { id,index ->
            store.move(id,index).also { strip.show(store.all(),store.activeSlot,false) }
        })
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        host.get().setContentView(strip)
        strip.show(store.all(),store.activeSlot,false);measure(strip,900,170)
        val row=strip.getChildAt(0) as ViewGroup
        val first=row.getChildAt(0)
        assertTrue(first.isLongClickable)
        assertTrue(first.performAccessibilityAction(R.id.bike_move_right,null))
        assertEquals(listOf(1,0,2,3,4),store.all().map { it.slot });assertEquals(0,store.activeSlot)
        val payloadClass=Class.forName("de.bcycle.app.TileReorder\$Payload")
        val payload=payloadClass.declaredConstructors.single().apply { isAccessible=true }.newInstance(BikeProfileStrip::class.java.getDeclaredField("reorder").apply { isAccessible=true }.get(strip),4)
        fun event(action:Int,x:Float):DragEvent {
            val method=DragEvent::class.java.declaredMethods.single { it.name=="obtain" && it.parameterCount==11 }.apply { isAccessible=true }
            return method.invoke(null,action,x,20f,0f,0f,payload,null,null,null,null,false) as DragEvent
        }
        assertTrue(row.dispatchDragEvent(event(DragEvent.ACTION_DRAG_STARTED,10f)))
        assertTrue(row.dispatchDragEvent(event(DragEvent.ACTION_DROP,10f)))
        row.dispatchDragEvent(event(DragEvent.ACTION_DRAG_ENDED,10f))
        assertEquals(listOf(4,1,0,2,3),store.all().map { it.slot });assertEquals(0,store.activeSlot)
        assertEquals(store.all(),BikeProfileStore(c,c.getSharedPreferences("strip-drag",0)).all())
        strip.show(store.all(),store.activeSlot,true)
        assertFalse(row.getChildAt(0).performAccessibilityAction(R.id.bike_move_right,null))
        host.pause().stop().destroy()
    }
    @Test fun activeRideBlocksAllProfileMutationsAndDeletingBikePreservesRideFiles() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            assertTrue(runtime.addBike());val slot=runtime.bikes.activeSlot
            val original=java.io.File(activity.filesDir,"fit-library/retained.fit").apply { parentFile!!.mkdirs();writeText("ride") }
            val photo=BikePhotos.file(activity,slot).apply { parentFile!!.mkdirs();writeText("photo") }
            val field=RideRuntime::class.java.getDeclaredField("recording").apply { isAccessible=true }
            field.setBoolean(runtime,true)
            try {
                assertFalse(runtime.addBike());assertFalse(runtime.deleteCurrentBike(slot))
                assertFalse(runtime.moveBike(slot,0));assertFalse(runtime.switchBike(0))
                assertEquals(2,runtime.bikes.all().size)
            } finally { field.setBoolean(runtime,false) }
            assertFalse(runtime.deleteCurrentBike(0)) // stale confirmation must not delete another bike
            assertTrue(runtime.deleteCurrentBike(slot))
            BikePhotos.worker.submit {}.get()
            assertFalse(photo.exists());assertEquals("ride",original.readText())
            assertFalse(runtime.deleteCurrentBike(runtime.bikes.activeSlot))
            assertTrue(runtime.addBike());assertTrue(runtime.bikes.activeSlot>slot)
        } finally { controller.pause().stop().destroy() }
    }
}

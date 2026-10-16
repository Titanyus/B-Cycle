package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BikesViewTest {
    private fun measure(view: View,width: Int=1080,height: Int=2100) {
        shadowOf(Looper.getMainLooper()).idle()
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height)
    }
    @Test fun pickerStoresPhotoForItsOriginalBikeAndRemovalRestoresTheChooseControl() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val app=controller.get();val runtime=RideRuntime.get(app)
            runtime.bikes.add()
            testDescendants(app.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
            ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.findViewWithTag<View>("settings_SENSORS").performClick()
            val decor=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView;measure(decor)
            decor.findViewWithTag<View>("bike_photo_choose").performClick()
            val request=shadowOf(app).nextStartedActivityForResult
            assertEquals(14,request.requestCode);assertEquals("image/*",request.intent.type)
            val input=File(app.cacheDir,"bike-selected.png")
            Bitmap.createBitmap(40,20,Bitmap.Config.ARGB_8888).apply { input.outputStream().use { compress(Bitmap.CompressFormat.PNG,100,it) };recycle() }
            MainActivity::class.java.getDeclaredMethod("onActivityResult",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,android.content.Intent::class.java)
                .apply { isAccessible=true }.invoke(app,14,android.app.Activity.RESULT_OK,android.content.Intent().setData(android.net.Uri.fromFile(input)))
            fun waitFor(condition:()->Boolean) {
                val end=System.nanoTime()+10_000_000_000L
                while(!condition() && System.nanoTime()<end) { Thread.sleep(10);shadowOf(Looper.getMainLooper()).idle() }
                shadowOf(Looper.getMainLooper()).idle();assertTrue(condition())
            }
            waitFor { BikePhotos.file(app,0).isFile && decor.findViewWithTag<View>("bike_photo_remove").visibility==View.VISIBLE }
            assertFalse(BikePhotos.file(app,1).exists())
            decor.findViewWithTag<View>("bike_photo_remove").performClick()
            waitFor { !BikePhotos.file(app,0).exists() && decor.findViewWithTag<View>("bike_photo_choose").visibility==View.VISIBLE }
            assertEquals(0,runtime.bikes.activeSlot)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun dynamicCardsInlineNameDropdownAndFitAccordionPersistOnTheSameScrollingPage() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val app=controller.get();val runtime=RideRuntime.get(app)
            runtime.bikes.add()
            testDescendants(app.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
            val menu=ShadowAlertDialog.getLatestAlertDialog()
            menu.window!!.decorView.findViewWithTag<View>("settings_SENSORS").performClick()
            val screen=ShadowAlertDialog.getLatestAlertDialog();val decor=screen.window!!.decorView;measure(decor)
            assertEquals("Bikes",decor.findViewWithTag<TextView>("bicycle_dialog_title").text.toString())
            val body=decor.findViewWithTag<View>("bikes_manager")
            val cardLayouts=testDescendants(body).filter { it.tag?.toString()?.matches(Regex("bike_profile_[0-3]"))==true }.map { it as LinearLayout }
            cardLayouts.forEach { card ->
                assertEquals("E-Bike",(card.getChildAt(0) as TextView).text.toString())
                assertEquals(2,(card.getChildAt(2) as TextView).minLines)
            }
            assertEquals(2,testDescendants(body).count { it.tag?.toString()?.matches(Regex("bike_profile_[0-3]"))==true })
            assertFalse(body.findViewWithTag<View>("bike_type_BIKE").isEnabled)
            assertFalse(body.findViewWithTag<View>("bike_type_INDOOR").isEnabled)
            assertTrue(body.findViewWithTag<View>("bike_type_EBIKE").isEnabled)
            assertFalse(body.findViewWithTag<View>("bike_sensor_POWER").isEnabled)
            assertNotNull(body.findViewWithTag<View>("bike_sensor_search"))
            val fitContent = body.findViewWithTag<View>("bike_fit_settings")
            assertNull(body.findViewWithTag<View>("bike_fit_toggle"))
            assertEquals(View.VISIBLE,fitContent.visibility)
            val gpsFields=body.findViewWithTag<View>("fit_source_content_GPS")
            assertEquals(View.GONE,gpsFields.visibility)
            body.findViewWithTag<View>("fit_source_GPS").performClick()
            assertEquals(View.VISIBLE,gpsFields.visibility)
            assertNotNull(body.findViewWithTag<View>("record_GPS_SPEED"))
            assertNull(body.findViewWithTag<View>("sensor_recording_toggle"))
            body.findViewWithTag<View>("bike_profile_name").performClick()
            body.findViewWithTag<EditText>("bike_profile_name_edit").setText("Tourenrad")
            assertEquals("Tourenrad",runtime.bikes.current().name)
            val wheel=body.findViewWithTag<EditText>("bike_wheel_mm")
            assertEquals(decor.findViewWithTag<View>("bike_wheel_label").parent,wheel.parent)
            assertEquals(decor.findViewWithTag<View>("bike_wheel_unit").parent,wheel.parent)
            assertEquals(app.resources.getFont(R.font.bebas_neue),wheel.typeface)
            assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,
                BicycleDesign.analysisMetricSp,app.resources.displayMetrics),wheel.textSize,.01f)
            assertEquals(android.graphics.Color.TRANSPARENT,(wheel.background as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
            wheel.setText("2350");assertEquals(2350,runtime.bikes.current().wheelMm)
            wheel.setText("0");assertNotNull(wheel.error);assertEquals(2350,runtime.bikes.current().wheelMm)
            wheel.setText("2350")
            body.findViewWithTag<Spinner>("bike_category").setSelection(BikeCategory.TREKKING.ordinal)
            shadowOf(Looper.getMainLooper()).idle();assertEquals(BikeCategory.TREKKING,runtime.bikes.current().category)
            val fields=runtime.config.fields
            body.findViewWithTag<CheckBox>("record_GPS_SPEED").performClick()
            assertNotEquals(fields,runtime.config.fields)
            val firstFields=runtime.config.fields
            body.findViewWithTag<View>("bike_profile_1").performClick();measure(decor)
            assertEquals(1,runtime.bikes.activeSlot);assertEquals("Bike 2",decor.findViewWithTag<TextView>("bike_profile_name").text.toString())
            assertEquals(2200,decor.findViewWithTag<EditText>("bike_wheel_mm").text.toString().toInt())
            decor.findViewWithTag<View>("bike_profile_0").performClick();measure(decor)
            assertEquals(firstFields,runtime.config.fields)
            assertEquals("Tourenrad",decor.findViewWithTag<TextView>("bike_profile_name").text.toString())
            val scrolling=testDescendants(decor).filterIsInstance<ScrollView>().single { testDescendants(it).contains(body) }
            assertTrue(scrolling.getChildAt(0).height>scrolling.height)
            assertEquals(View.VISIBLE,decor.findViewWithTag<View>("bicycle_fullscreen_footer").visibility)
            // Capture the real Android view, including fixed header, not an illustrative mockup.
            fun snapshot(name: String) {
                val bitmap=Bitmap.createBitmap(decor.width,decor.height,Bitmap.Config.ARGB_8888);decor.draw(Canvas(bitmap))
                File("build/reports/bikes/$name.png").apply { parentFile?.mkdirs();outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } }
                bitmap.recycle()
            }
            snapshot("bikes-top")
            fun showSectionBefore(tag: String) {
                scrolling.scrollTo(0,0);measure(decor)
                val target=decor.findViewWithTag<View>(tag)
                val parent=target.parent as ViewGroup
                val heading=parent.getChildAt(parent.indexOfChild(target)-1)
                val rect=android.graphics.Rect(0,0,heading.width,heading.height)
                scrolling.offsetDescendantRectToMyCoords(heading,rect)
                scrolling.scrollTo(0,rect.top);measure(decor)
            }
            showSectionBefore("bike_auto_connect");snapshot("bikes-sensors")
            showSectionBefore("bike_fit_settings");snapshot("bikes-fit")
            for(width in listOf(720,1080)) { measure(decor,width,1500);assertEquals(BicycleDesign.dp(app,BicycleDesign.headerHeight),decor.findViewWithTag<View>("bicycle_navigation_header").height) }
        } finally { controller.pause().stop().destroy() }
    }
}

package de.bcycle.app

import android.view.View
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IndoorUiTest {
    @Test fun changingTypesAndEditingIndoorFitPreservesHiddenOutdoorSelectionAndPrimarySources() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            runtime.setGpsEnabled(true)
            val gpsFields=RecordField.options.map { it.id }.filter { it.startsWith("GPS_") && it!="GPS_POSITION" }.toSet()
            runtime.saveConfig(RecordingConfig(gpsFields+setOf("MOTOR_POWER","HEART_RATE"),"GPS_SPEED","GPS_DISTANCE"))
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(activity)
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            assertNull(root.findViewWithTag<View>("record_GPS_POSITION"))
            selectTestBikeType(root,BikeType.INDOOR)
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertFalse(runtime.gpsEnabled);assertFalse("GPS_POSITION" in runtime.config.fields)
            root.findViewWithTag<android.widget.Switch>("record_HEART_RATE").isChecked=false
            shadowOf(android.os.Looper.getMainLooper()).idle()
            selectTestBikeType(root,BikeType.EBIKE)
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertTrue(runtime.gpsEnabled)
            assertEquals("GPS_SPEED",runtime.config.speedSource);assertEquals("GPS_DISTANCE",runtime.config.distanceSource)
            assertEquals(RecordingSource.GPS,runtime.config.sourcePreferences!!.preferred[RecordingQuantity.SPEED])
            assertTrue(runtime.config.fields.containsAll(gpsFields+setOf("GPS_POSITION","MOTOR_POWER")))
            assertFalse("HEART_RATE" in runtime.config.fields) // Deliberate edit of a visible, shared field.
            assertNull(root.findViewWithTag<View>("record_GPS_POSITION"))
            runtime.setGpsEnabled(false);assertFalse("GPS_POSITION" in runtime.config.fields)
            runtime.setGpsEnabled(true);assertTrue("GPS_POSITION" in runtime.config.fields)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun indoorSelectionPersistsAndOnlyOffersTrainerHeartAndRelevantFitAndDashboardFields() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(activity)
            val screen=ShadowAlertDialog.getLatestAlertDialog();val decor=screen.window!!.decorView
            selectTestBikeType(decor,BikeType.INDOOR)
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(BikeType.INDOOR,runtime.bikes.current().type)
            assertFalse(runtime.gpsEnabled);assertFalse(runtime.gpsSensor.active);assertFalse(runtime.config.needsGps)
            assertNotNull(decor.findViewWithTag<View>("bike_sensor_TRAINER"));assertNotNull(decor.findViewWithTag<View>("bike_sensor_HEART"))
            for(sensor in listOf("GPS","EBIKE","POWER")) assertNull(decor.findViewWithTag<View>("bike_sensor_$sensor"))
            assertEquals(View.GONE,decor.findViewWithTag<View>("bike_category").visibility)
            val indoorType=decor.findViewWithTag<Spinner>("bike_indoor_type_choice")
            assertEquals(View.VISIBLE,indoorType.visibility)
            assertEquals("Hometrainer",indoorType.selectedItem.toString())
            val indoorAdapter=indoorType.adapter as ListAdapter
            assertEquals(2,indoorAdapter.count)
            assertTrue(indoorAdapter.isEnabled(0));assertFalse(indoorAdapter.isEnabled(1))
            val disabledRow=indoorType.adapter.getDropDownView(1,null,indoorType)
            assertFalse(disabledRow.isEnabled);assertEquals(.4f,disabledRow.alpha,.01f)
            assertNotNull(decor.findViewWithTag<View>("fit_source_Indoor-Bike"))
            assertNull(decor.findViewWithTag<View>("record_GPS_POSITION"));assertNull(decor.findViewWithTag<View>("record_MOTOR_POWER"))
            assertNotNull(decor.findViewWithTag<View>("record_RIDER_POWER"));assertNotNull(decor.findViewWithTag<View>("record_CADENCE"))
            val powerSlider=decor.findViewWithTag<SeekBar>("indoor_correction_power")
            val speedSlider=decor.findViewWithTag<SeekBar>("indoor_correction_speed")
            assertNotNull(powerSlider);assertNotNull(speedSlider)
            assertEquals(0,powerSlider.min);assertEquals(30,powerSlider.max);assertEquals(10,powerSlider.progress)
            powerSlider.onKeyDown(android.view.KeyEvent.KEYCODE_DPAD_RIGHT,android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
            assertEquals(IndoorCorrection(11,10),runtime.bikes.current().indoorCorrection)
            assertEquals(10,speedSlider.progress)
            assertTrue(decor.findViewWithTag<TextView>("indoor_correction_power_value").text.toString().contains("1"))
            val groups=DashboardField.groups(runtime.dashboardBikeData,includeTraining=true)
            assertEquals(setOf("Indoor-Bike","Pulsmesser","Wetter","Akku","Smartphone","Leistung"),groups.map { it.source }.toSet())
            assertEquals(groups.size,groups.map { it.source to it.title }.distinct().size)
            assertFalse(groups.flatMap { it.fields }.any { it.id=="MOTOR_POWER" || it.id=="GPS_POSITION" || it.id=="GPS_SPEED" })
            val selected=runtime.bikes.current();assertEquals(BikeType.INDOOR,BikeProfileStore(activity,runtime.preferences).current().type)
            runtime.setGpsEnabled(true);assertFalse(runtime.gpsEnabled)
            decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2100,View.MeasureSpec.EXACTLY));decor.layout(0,0,1080,2100)
            val bitmap=android.graphics.Bitmap.createBitmap(1080,2100,android.graphics.Bitmap.Config.ARGB_8888)
            decor.draw(android.graphics.Canvas(bitmap))
            java.io.File("build/reports/bikes/indoor.png").apply { parentFile!!.mkdirs();outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } };bitmap.recycle()
            selectTestBikeType(decor,BikeType.EBIKE)
            assertEquals(BikeType.EBIKE,runtime.bikes.current().type);assertEquals(selected.slot,runtime.bikes.activeSlot)
            assertNotNull(decor.findViewWithTag<View>("bike_sensor_GPS"));assertNull(decor.findViewWithTag<View>("bike_sensor_TRAINER"))
            assertNull(decor.findViewWithTag<View>("indoor_correction_power"))
            assertEquals(View.GONE,decor.findViewWithTag<View>("bike_indoor_type_choice").visibility)
            assertFalse(runtime.setIndoorCorrection(IndoorCorrection(12,12)))
            assertEquals("Indoor-Bike",DashboardField("GPS_MOVING_AVERAGE").captionIcon(LiveData(indoor=true)))
        } finally { controller.pause().stop().destroy() }
    }
}

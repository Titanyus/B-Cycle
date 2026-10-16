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
    @Test fun indoorSelectionPersistsAndOnlyOffersTrainerHeartAndRelevantFitAndDashboardFields() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(activity)
            val screen=ShadowAlertDialog.getLatestAlertDialog();val decor=screen.window!!.decorView
            decor.findViewWithTag<View>("bike_type_INDOOR").performClick()
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(BikeType.INDOOR,runtime.bikes.current().type)
            assertFalse(runtime.gpsEnabled);assertFalse(runtime.gpsSensor.active);assertFalse(runtime.config.needsGps)
            assertNotNull(decor.findViewWithTag<View>("bike_sensor_TRAINER"));assertNotNull(decor.findViewWithTag<View>("bike_sensor_HEART"))
            for(sensor in listOf("GPS","EBIKE","POWER")) assertNull(decor.findViewWithTag<View>("bike_sensor_$sensor"))
            assertEquals(View.GONE,decor.findViewWithTag<View>("bike_category").visibility)
            assertNotNull(decor.findViewWithTag<View>("fit_source_Indoor-Bike"))
            assertNull(decor.findViewWithTag<View>("record_GPS_POSITION"));assertNull(decor.findViewWithTag<View>("record_MOTOR_POWER"))
            assertNotNull(decor.findViewWithTag<View>("record_RIDER_POWER"));assertNotNull(decor.findViewWithTag<View>("record_CADENCE"))
            val groups=DashboardField.groups(runtime.dashboardBikeData,includeTraining=true)
            assertEquals(setOf("Indoor-Bike","Pulsmesser","Leistung"),groups.map { it.source }.toSet())
            assertEquals(groups.size,groups.map { it.source to it.title }.distinct().size)
            assertFalse(groups.flatMap { it.fields }.any { it.id=="MOTOR_POWER" || it.id=="GPS_POSITION" || it.id=="GPS_SPEED" })
            val selected=runtime.bikes.current();assertEquals(BikeType.INDOOR,BikeProfileStore(activity,runtime.preferences).current().type)
            runtime.setGpsEnabled(true);assertFalse(runtime.gpsEnabled)
            decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2100,View.MeasureSpec.EXACTLY));decor.layout(0,0,1080,2100)
            val bitmap=android.graphics.Bitmap.createBitmap(1080,2100,android.graphics.Bitmap.Config.ARGB_8888)
            decor.draw(android.graphics.Canvas(bitmap))
            java.io.File("build/reports/bikes/indoor.png").apply { parentFile!!.mkdirs();outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } };bitmap.recycle()
            decor.findViewWithTag<View>("bike_type_EBIKE").performClick()
            assertEquals(BikeType.EBIKE,runtime.bikes.current().type);assertEquals(selected.slot,runtime.bikes.activeSlot)
            assertNotNull(decor.findViewWithTag<View>("bike_sensor_GPS"));assertNull(decor.findViewWithTag<View>("bike_sensor_TRAINER"))
            assertEquals("Indoor-Bike",DashboardField("GPS_MOVING_AVERAGE").captionIcon(LiveData(indoor=true)))
        } finally { controller.pause().stop().destroy() }
    }
}

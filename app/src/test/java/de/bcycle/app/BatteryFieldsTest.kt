package de.bcycle.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.BatteryManager
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Spinner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BatteryFieldsTest {
    private val app get()=RuntimeEnvironment.getApplication()
    private val sources=listOf(BatterySource.EBIKE,BatterySource.HEART,BatterySource.TRAINER,BatterySource.PHONE)
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    private fun measure(v:View,w:Int,h:Int) { v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h) }
    @Test fun oneToFourSourcesPersistInEverySlotAndInvalidOrDuplicateSelectionsAreRejected() {
        for(count in 1..4) {
            val field=BatteryFields.field(sources.take(count))
            for(slot in 0..7) assertEquals(field,DashboardLayout.decode(DashboardLayout.defaults.replace(slot,field).encode()).fields[slot])
            assertEquals(sources.take(count),DashboardField.fromId(field.id)!!.batterySources)
        }
        for(id in listOf("BATTERIES:","BATTERIES:UNKNOWN","BATTERIES:PHONE,PHONE","BATTERIES:EBIKE,HEART,TRAINER,PHONE,POWER")) assertNull(DashboardField.fromId(id))
        assertEquals(0,BatteryFields.warning(20));assertEquals(1,BatteryFields.warning(19))
        assertEquals(1,BatteryFields.warning(10));assertEquals(2,BatteryFields.warning(9));assertEquals(2,BatteryFields.warning(0))
        assertNull(BatterySnapshot(mapOf(BatterySource.PHONE to 101)).level(BatterySource.PHONE))
        assertEquals(0,BatterySnapshot(mapOf(BatterySource.PHONE to 0)).level(BatterySource.PHONE))
    }
    @Test fun systemBatteryBroadcastUpdatesValidPercentagesAndStopsWithRuntimeSubscription() {
        var changes=0;val battery=PhoneBattery(app) { changes++ };battery.start()
        fun send(level:Int,scale:Int=100) {
            app.sendBroadcast(Intent(Intent.ACTION_BATTERY_CHANGED).putExtra(BatteryManager.EXTRA_LEVEL,level).putExtra(BatteryManager.EXTRA_SCALE,scale))
            shadowOf(Looper.getMainLooper()).idle()
        }
        send(28);assertEquals(28,battery.level)
        val before=changes;send(28);assertEquals(before,changes)
        send(0);assertEquals(0,battery.level)
        send(101);assertNull(battery.level)
        send(90,0);assertNull(battery.level)
        battery.stop();send(90);assertNull(battery.level)
    }
    @Test fun pickerSavesMultipleSourcesWithoutClosingAndEnforcesFourAndAtLeastOne() {
        val initial=listOf(BatterySource.EBIKE,BatterySource.HEART,BatterySource.TRAINER,BatterySource.POWER)
        var chosen=BatteryFields.field(initial)
        var closed=false
        val picker=FieldPickerView(app,LiveData(),GpsValues(),null,PulseValues(),chosen,false,
            batteryChoose={ chosen=it },choose={ closed=true })
        val spinner=picker.findViewWithTag<Spinner>("field_source")
        assertEquals("Akku",spinner.selectedItem)
        val phone=picker.findViewWithTag<CheckBox>("battery_select_PHONE")
        assertFalse(phone.isEnabled)
        picker.findViewWithTag<CheckBox>("battery_select_POWER").performClick()
        assertTrue(phone.isEnabled);phone.performClick()
        assertEquals(4,chosen.batterySources!!.size);assertTrue(BatterySource.PHONE in chosen.batterySources!!)
        assertFalse(closed)
        for(source in listOf(BatterySource.EBIKE,BatterySource.HEART,BatterySource.TRAINER)) picker.findViewWithTag<CheckBox>("battery_select_${source.name}").performClick()
        assertEquals(listOf(BatterySource.PHONE),chosen.batterySources)
        assertFalse(phone.isEnabled)
    }
    @Test fun threeColumnDashboardAndMapRenderFourCompactRowsIncludingMissingValuesAndStatusMarks() {
        val snapshot=BatterySnapshot(mapOf(BatterySource.EBIKE to 72,BatterySource.HEART to 19,BatterySource.PHONE to 9))
        val data=LiveData(batteries=snapshot)
        val field=BatteryFields.field(sources)
        val dashboard=DashboardView(app) {}.apply { setTemplate(DashboardTemplate.EIGHT) }
        dashboard.show(DashboardLayout.defaults.replace(2,field),data)
        measure(dashboard,960,840)
        val grid=dashboard.findViewWithTag<View>("dashboard_batteries_2")
        assertEquals(View.VISIBLE,grid.visibility);assertTrue(grid.width<=320);assertTrue(grid.height>0)
        assertTrue(dashboard.findViewWithTag<View>("dashboard_tile_2").contentDescription.toString().contains("72"))
        val prefs=app.getSharedPreferences("battery-map-test",0)
        prefs.edit().putString(MapFieldSelection.key(1),field.id).commit()
        val map=MapDataFieldsView(app);map.reload(prefs);map.show(data,GpsValues(),null,PulseValues(),recording=true,bikeLight=true)
        measure(map,960,BicycleDesign.dp(app,map.preferredHeightDp))
        val middle=map.findViewWithTag<BatteryMapTextView>("map_value_1")
        assertTrue(middle.contentDescription.toString().contains("72"));assertTrue(middle.contentDescription.toString().contains(app.getString(R.string.ui_recording_running)))
        assertEquals(Color.BLACK,middle.currentTextColor);assertFalse(middle.isClickable)
        for((name,v,color) in listOf(Triple("battery-dashboard",dashboard,Color.BLACK),Triple("battery-map",map,Color.LTGRAY))) {
            val image=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888)
            Canvas(image).apply { drawColor(color);v.draw(this) }
            java.io.File("build/reports/battery/$name.png").apply { parentFile!!.mkdirs();outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) } };image.recycle()
        }
        map.show(LiveData(),GpsValues(),null,PulseValues())
        assertEquals(View.VISIBLE,map.visibility);assertTrue(middle.contentDescription.toString().contains(app.getString(R.string.ui_field_missing_value)))
    }
    @Test fun newBatterySourcesAndPhoneClockDoNotDisappearWhenSensorsAreAbsentAndWeatherRemainsSelectableIndoor() {
        for(indoor in listOf(false,true)) {
            val groups=DashboardField.groups(LiveData(indoor=indoor),includeTraining=true)
            assertTrue(groups.any { it.source=="Akku" });assertTrue(groups.any { it.source=="Smartphone" })
            assertEquals(WeatherMetric.entries.map { it.name }.toSet(),groups.filter { it.source=="Wetter" }.flatMap { it.fields }.map { it.id }.toSet())
        }
        assertTrue(DashboardField("PHONE_TIME").value(LiveData()).text.matches(Regex("\\d{2}:\\d{2}")))
        assertEquals("—",DashboardField("PHONE_BATTERY").value(LiveData()).text)
    }
}

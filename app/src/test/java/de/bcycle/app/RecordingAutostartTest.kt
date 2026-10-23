package de.bcycle.app

import android.app.Activity
import android.content.Intent
import android.Manifest
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecordingAutostartTest {
    private val app get()=RuntimeEnvironment.getApplication()
    private fun runtime():RideRuntime {
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN)
        return RideRuntime.get(app).also { it.setGpsEnabled(false);it.setVisible(true) }
    }
    private fun bike(runtime:RideRuntime,live:Boolean) {
        val phase=runtime.connection.javaClass.getDeclaredField("phase").apply { isAccessible=true }
        phase.set(runtime.connection,phase.type.enumConstants!!.single { it.toString()==if(live) "LIVE" else "STOPPED" })
        runtime.onStatus("test")
    }
    private fun flag(runtime:RideRuntime,name:String,value:Boolean)=RideRuntime::class.java.getDeclaredField(name).apply { isAccessible=true }.setBoolean(runtime,value)
    @Test fun ebikeStartsOncePerConnectionAndEnablingWhileConnectedDoesNotStartImmediately() {
        val r=runtime()
        assertFalse(r.bikes.current().autoStartRecording)
        bike(r,true);assertNull(shadowOf(app).nextStartedService)
        r.bikes.save(r.bikes.current().copy(autoStartRecording=true))
        bike(r,true);assertNull(shadowOf(app).nextStartedService)
        bike(r,false);bike(r,true)
        val intent=shadowOf(app).nextStartedService!!
        assertEquals(RecordingService.START,intent.action);assertEquals(RecordingService::class.java.name,intent.component!!.className)
        repeat(3) { bike(r,true) };assertNull(shadowOf(app).nextStartedService)
        flag(r,"recording",true);bike(r,false);bike(r,true);assertNull(shadowOf(app).nextStartedService)
        flag(r,"recording",false);bike(r,true);assertNull(shadowOf(app).nextStartedService)
    }
    @Test fun savingAndHrvNeverStartLaterWithoutANewConnection() {
        val r=runtime();r.bikes.save(r.bikes.current().copy(autoStartRecording=true))
        flag(r,"saving",true);bike(r,true);assertNull(shadowOf(app).nextStartedService)
        flag(r,"saving",false);bike(r,true);assertNull(shadowOf(app).nextStartedService)
        HrvController::class.java.getDeclaredField("busy").apply { isAccessible=true }.setBoolean(r.hrv,true)
        bike(r,false);bike(r,true);assertNull(shadowOf(app).nextStartedService)
        HrvController::class.java.getDeclaredField("busy").apply { isAccessible=true }.setBoolean(r.hrv,false)
        bike(r,true);assertNull(shadowOf(app).nextStartedService)
        flag(r,"visible",false);bike(r,false);bike(r,true);assertNull(shadowOf(app).nextStartedService)
        flag(r,"visible",true);bike(r,true);assertNull(shadowOf(app).nextStartedService)
    }
    @Test fun openingTheAppWithAnAlreadyReadyBikeStartsButNotDuringHrv() {
        val r=runtime();r.bikes.save(r.bikes.current().copy(autoStartRecording=true))
        flag(r,"visible",false);bike(r,true);assertNull(shadowOf(app).nextStartedService)
        r.setVisible(true);assertEquals(RecordingService.START,shadowOf(app).nextStartedService!!.action)
        r.setVisible(true);assertNull(shadowOf(app).nextStartedService)
        flag(r,"visible",false)
        HrvController::class.java.getDeclaredField("busy").apply { isAccessible=true }.setBoolean(r.hrv,true)
        r.setVisible(true);assertNull(shadowOf(app).nextStartedService)
        HrvController::class.java.getDeclaredField("busy").apply { isAccessible=true }.setBoolean(r.hrv,false)
        bike(r,true);assertNull(shadowOf(app).nextStartedService)
    }
    @Test fun indoorStartsWhenNotificationsAreReadyWithoutWaitingForPedallingOrStartingAgainOnSamples() {
        val r=runtime();assertTrue(r.changeBikeType(BikeType.INDOOR))
        r.bikes.save(r.bikes.current().copy(autoStartRecording=true))
        val changed=RideRuntime::class.java.getDeclaredMethod("trainerChanged").apply { isAccessible=true }
        TrainerConnection::class.java.getDeclaredField("active").apply { isAccessible=true }.setBoolean(r.trainer,true)
        TrainerConnection::class.java.getDeclaredField("subscribed").apply { isAccessible=true }.setBoolean(r.trainer,true)
        assertFalse(r.trainer.live)
        changed.invoke(r);assertEquals(RecordingService.START,shadowOf(app).nextStartedService!!.action)
        repeat(3) { changed.invoke(r) };assertNull(shadowOf(app).nextStartedService)
        flag(r,"visible",false);r.setVisible(true)
        assertEquals(RecordingService.START,shadowOf(app).nextStartedService!!.action)
        r.trainer.stop();changed.invoke(r)
    }
    @Test fun foregroundServiceStopsCleanlyIfHrvBecameActiveBeforeTheStartWasHandled() {
        val r=runtime()
        HrvController::class.java.getDeclaredField("busy").apply { isAccessible=true }.setBoolean(r.hrv,true)
        val host=Robolectric.buildService(RecordingService::class.java).create()
        try {
            val service=host.get()
            service.onStartCommand(Intent(app,RecordingService::class.java).setAction(RecordingService.START),0,1)
            assertFalse(r.recording);assertTrue(r.hrv.busy)
            assertTrue(shadowOf(service).isForegroundStopped)
        } finally { host.destroy();HrvController::class.java.getDeclaredField("busy").apply { isAccessible=true }.setBoolean(r.hrv,false) }
    }
    @Test fun switchIsBelowWheelAndDividerAndBothProfileValuesSurviveBackupAndOldProfilesDefaultOff() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val r=RideRuntime.get(a)
            val view=BikesView(a,r,{},{},{},{LinearLayout(a)})
            a.setContentView(view)
            val toggle=view.findViewWithTag<Switch>("bike_auto_start_recording")
            assertFalse(toggle.isChecked)
            val parent=toggle.parent as ViewGroup
            assertSame(view.findViewWithTag<View>("bike_autostart_divider"),parent.getChildAt(parent.indexOfChild(toggle)-1))
            assertSame(view.findViewWithTag<View>("bike_wheel_row"),parent.getChildAt(parent.indexOfChild(toggle)-2))
            toggle.isChecked=true;assertTrue(r.bikes.current().autoStartRecording)
            val second=r.bikes.add()!!;r.bikes.select(second.slot)
            assertFalse(r.bikes.current().autoStartRecording)
            val backup=SettingsBackup(a,ThemeManager(r.preferences));val bytes=backup.export(setOf(BackupSection.PROFILE))
            r.preferences.edit().clear().commit()
            val doc=backup.decode(bytes);backup.apply(backup.plan(doc,doc.sections.keys))
            val restored=BikeProfileStore(a,r.preferences)
            assertFalse(restored.current().autoStartRecording);restored.select(0);assertTrue(restored.current().autoStartRecording)
            val rows=JSONArray(r.preferences.getString(BikeProfileStore.KEY,"[]"))
            repeat(rows.length()) { rows.getJSONObject(it).remove("autoStartRecording") }
            r.preferences.edit().putString(BikeProfileStore.KEY,rows.toString()).commit()
            assertFalse(BikeProfileStore(a,r.preferences).current().autoStartRecording)
            val invalid=JSONObject(String(bytes));val bank=invalid.getJSONObject("sections").getJSONObject("riderProfile").getJSONObject("additional").getJSONObject("bike")
            val profiles=JSONArray(bank.getString(BikeProfileStore.KEY));profiles.getJSONObject(0).put("autoStartRecording","true");bank.put(BikeProfileStore.KEY,profiles.toString())
            assertThrows(SettingsBackupFailure::class.java) { backup.decode(invalid.toString().toByteArray()) }
        } finally { host.pause().stop().destroy() }
    }
}

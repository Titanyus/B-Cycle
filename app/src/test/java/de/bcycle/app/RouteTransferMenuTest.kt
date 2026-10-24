package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.widget.Button
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RouteTransferMenuTest {
    @Test fun targetsRemainIndependentRespectAccountAndFileStampAndKeepDisabledServicesVisible() {
        val c=RuntimeEnvironment.getApplication()
        val stamp=RouteFileStamp(12,34,"hash")
        val entry=ManagedRoute("recorded-test","Test",true,"FIT",RouteDetails(),false,TrackOrigin.RECORDED,true,stamp)
        fun rows()=RouteTransferRows.read(c,entry,false).associateBy { it.target }
        assertEquals(5,rows().size)
        assertTrue(rows().getValue(RouteTransferTarget.LOCAL).canStart)
        assertTrue(rows().filterKeys { it!=RouteTransferTarget.LOCAL }.values.all { it.text==R.string.route_transfer_off && !it.canStart })
        c.getSharedPreferences("intervals-settings",0).edit().putBoolean("enabled",true).putString("athlete","i1").commit()
        val receipt=JSONObject().put("athlete","i1").put("length",12).put("modified",34).put("hash","hash").put("state","saved")
        val prefs=c.getSharedPreferences("intervals-uploads",0)
        fun save() { prefs.edit().putString("entries",JSONObject().put(entry.id,receipt).toString()).commit() }
        save()
        assertTrue(rows().getValue(RouteTransferTarget.INTERVALS).completed)
        assertFalse(rows().getValue(RouteTransferTarget.WEBDAV).completed)
        receipt.put("modified",35);save()
        assertFalse(rows().getValue(RouteTransferTarget.INTERVALS).completed)
        receipt.put("modified",34).put("state","failed");save()
        assertEquals(R.string.route_transfer_failed,rows().getValue(RouteTransferTarget.INTERVALS).text)
        assertTrue(rows().getValue(RouteTransferTarget.INTERVALS).canStart)
        receipt.put("state","pending").put("error","unreachable");save()
        assertEquals(R.string.service_unreachable,rows().getValue(RouteTransferTarget.INTERVALS).text)
        assertFalse(rows().getValue(RouteTransferTarget.INTERVALS).completed)
        receipt.put("modified",35);save()
        assertEquals(R.string.route_transfer_ready,rows().getValue(RouteTransferTarget.INTERVALS).text)
        val imported=RouteTransferRows.read(c,entry.copy(origin=TrackOrigin.IMPORTED),false)
        assertEquals(R.string.route_transfer_ineligible,imported.single { it.target==RouteTransferTarget.INTERVALS }.text)
        assertTrue(RouteTransferRows.read(c,entry,true).none { it.canStart })
    }
    @Test fun configureIsFixedPromptAndPreferenceChangesUpdateOnlyWhileMenuExists() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val button=Button(a);a.setContentView(button)
            a.window.decorView.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));a.window.decorView.layout(0,0,360,800)
            val entry=ManagedRoute("recorded-test","Test",true,"FIT",RouteDetails(),false,TrackOrigin.RECORDED,true,RouteFileStamp(1,2))
            var configured=0;val chosen=mutableListOf<RouteTransferTarget>()
            val menu=RouteTransferMenu(a,ThemeManager(a.getSharedPreferences("bike",0)).settings,button,{entry},{false},{chosen+=it},{configured++})
            menu.show()
            val list=menu.popup.listView!!
            assertEquals(5,list.adapter.count)
            list.performItemClick(list.adapter.getView(2,null,list),2,2);assertTrue(chosen.isEmpty())
            a.getSharedPreferences("intervals-settings",0).edit().putBoolean("enabled",true).putString("athlete","i1").apply()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue((list.adapter.getItem(2) as RouteTransferRow).canStart)
            list.performItemClick(list.adapter.getView(2,null,list),2,2)
            assertEquals(listOf(RouteTransferTarget.INTERVALS),chosen)
            assertTrue(menu.popup.isShowing)
            menu.configureButton.performClick()
            assertEquals(1,configured);assertFalse(menu.popup.isShowing)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun dreeveBackgroundConfirmationUpdatesTheAlreadyOpenMenuWithoutActivityRestart() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val button=Button(a);a.setContentView(button)
            a.window.decorView.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));a.window.decorView.layout(0,0,360,800)
            a.getSharedPreferences("activity-service-dreeve",0).edit().putBoolean("enabled",true).putString("address","https://dreeve.example/").commit()
            val stamp=RouteFileStamp(12,34,"hash")
            val entry=ManagedRoute("recorded-test","Test",true,"FIT",RouteDetails(),false,TrackOrigin.RECORDED,true,stamp)
            val menu=RouteTransferMenu(a,ThemeManager(a.getSharedPreferences("bike",0)).settings,button,{entry},{false},{},{})
            val receipt=JSONObject().put("ride",entry.id).put("target","https://dreeve.example/").put("sequence",1)
                .put("length",12).put("modified",34).put("hash","hash").put("state","processing")
            val prefs=a.getSharedPreferences("activity-uploads-dreeve",0)
            prefs.edit().putString("entries",JSONObject().put("operation",receipt).toString()).commit()
            menu.show();val list=menu.popup.listView!!
            assertEquals(R.string.dreeve_upload_processing,(list.adapter.getItem(3) as RouteTransferRow).text)
            Thread { prefs.edit().putString("entries",JSONObject().put("operation",receipt.put("state","saved")).toString()).commit() }.apply { start();join() }
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(menu.popup.isShowing)
            assertTrue((list.adapter.getItem(3) as RouteTransferRow).completed)
            val row=list.adapter.getView(3,null,list)
            assertEquals(a.getString(R.string.route_transfer_done),row.findViewWithTag<android.widget.TextView>("route_transfer_status_DREEVE").text)
            menu.dismiss()
        } finally { host.pause().stop().destroy() }
    }

    @Test fun completedRowsOfferRecheckWithoutChangingTheirNormalTapActionAndRespectBusyState() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val anchor=Button(a);a.setContentView(anchor)
            a.window.decorView.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));a.window.decorView.layout(0,0,360,800)
            val file=java.io.File(a.cacheDir,"local.fit").apply { writeText("local") }
            val entry=ManagedRoute("recorded-test","Test",true,"FIT",RouteDetails(),false,TrackOrigin.RECORDED,true,
                RouteFileStamp(file.length(),file.lastModified(),WebDavClient.hash(file)))
            RouteLocalSaves.perform(a,entry.id,file,"Downloads/B-Cycle") {}
            a.getSharedPreferences("activity-service-dreeve",0).edit().putBoolean("enabled",true).putString("address","https://dreeve.example/").commit()
            val receipt=JSONObject().put("ride",entry.id).put("target","https://dreeve.example/").put("sequence",1)
                .put("length",file.length()).put("modified",file.lastModified()).put("hash",WebDavClient.hash(file)).put("state","saved")
            a.getSharedPreferences("activity-uploads-dreeve",0).edit().putString("entries",JSONObject().put("operation",receipt).toString()).commit()
            var busy=false;val taps=mutableListOf<RouteTransferTarget>();val checks=mutableListOf<RouteTransferTarget>()
            val menu=RouteTransferMenu(a,ThemeManager(a.getSharedPreferences("bike",0)).settings,anchor,{entry},{busy},{taps+=it},{},recheck={checks+=it})
            menu.show();val list=menu.popup.listView!!
            assertTrue(list.adapter.isEnabled(3))
            val cloud=list.adapter.getView(3,null,list)
            android.widget.FrameLayout(a).addView(cloud) // A rejected long click delegates to its native parent.
            cloud.performClick();assertTrue(taps.isEmpty())
            assertTrue(cloud.performLongClick());assertEquals(listOf(RouteTransferTarget.DREEVE),checks)
            val local=list.adapter.getView(0,null,list)
            local.performClick();assertEquals(listOf(RouteTransferTarget.LOCAL),taps)
            assertTrue(local.performLongClick());assertEquals(RouteTransferTarget.LOCAL,checks.last())
            busy=true;assertFalse(cloud.performLongClick());assertEquals(2,checks.size)
            menu.dismiss();busy=false;assertFalse(cloud.performLongClick())
        } finally { host.pause().stop().destroy() }
    }
    @Test fun transferHoldWaits600msAndCancelsOnReleaseScrollAndDetach() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val button=Button(a);a.setContentView(button)
            var taps=0;var holds=0
            TransferHold.bind(button,{true},{taps++},{holds++})
            fun event(action:Int,x:Float=0f) {
                val now=android.os.SystemClock.uptimeMillis()
                android.view.MotionEvent.obtain(now,now,action,x,0f,0).let { button.dispatchTouchEvent(it);it.recycle() }
            }
            fun wait(ms:Long)=shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(ms))
            event(android.view.MotionEvent.ACTION_DOWN);wait(599);assertEquals(0,holds)
            wait(1);assertEquals(1,holds);event(android.view.MotionEvent.ACTION_UP);assertEquals(0,taps)
            event(android.view.MotionEvent.ACTION_DOWN);wait(100);event(android.view.MotionEvent.ACTION_UP);wait(600)
            assertEquals(1,taps);assertEquals(1,holds)
            event(android.view.MotionEvent.ACTION_DOWN);event(android.view.MotionEvent.ACTION_MOVE,100f);wait(600)
            event(android.view.MotionEvent.ACTION_UP,100f);assertEquals(1,holds);assertEquals(1,taps)
            event(android.view.MotionEvent.ACTION_DOWN);a.setContentView(View(a));wait(600);assertEquals(1,holds)
        } finally { host.pause().stop().destroy() }
    }

}

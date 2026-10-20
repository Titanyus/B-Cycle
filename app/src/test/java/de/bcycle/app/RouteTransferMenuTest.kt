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

}

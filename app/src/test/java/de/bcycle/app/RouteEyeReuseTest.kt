package de.bcycle.app

import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ListView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RouteEyeReuseTest {
    @Test fun trackColoursSurviveDetachReattachReuseAndThemeChanges() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val prefs=c.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            val colours=listOf(Color.MAGENTA,Color.YELLOW,Color.CYAN)
            colours.forEachIndexed { slot,colour -> MapRouteStyles.write(prefs,MapRouteStyles.saved(slot),MapLineStyle(colour,6f,10)) }
            val manager=RouteManagerView(c,{},themes)
            val entries=(0..3).map { i -> ManagedRoute("r$i","Fahrt$i",true,"FIT",RouteDetails(),true,locallySaved=true) }
            manager.submit(entries,null,false,mapOf("r0" to 1,"r1" to 2,"r2" to 3),setOf("r0","r2"))
            val list=manager.findViewWithTag<ListView>("route_list")
            val holder=FrameLayout(c);c.setContentView(holder)
            var row:View?=null
            fun check(index:Int) {
                row?.let(holder::removeView)
                row=list.adapter.getView(index+1,row,list)
                holder.addView(row)
                val eye=row!!.findViewWithTag<ImageButton>("route_eye_r$index")
                val expected=colours.getOrNull(index) ?: BicycleIcons.color(themes.active.tokens,BicycleIcon.ShowOnMap,true)
                assertEquals("r$index after attach",PorterDuffColorFilter(expected,PorterDuff.Mode.SRC_ATOP),eye.colorFilter)
                holder.removeView(row);holder.addView(row)
                assertEquals("r$index after reattach",PorterDuffColorFilter(expected,PorterDuff.Mode.SRC_ATOP),eye.colorFilter)
            }
            repeat(3) { for(index in listOf(0,3,1,2,0)) check(index) }
            themes.activate(BicycleThemes.red.id)
            check(0);check(3);check(2)
        } finally { host.pause().stop().destroy() }
    }
}

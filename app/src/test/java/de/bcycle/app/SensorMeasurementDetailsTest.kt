package de.bcycle.app

import android.app.Activity
import android.view.View
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class SensorMeasurementDetailsTest {
    @Test fun disclosureUpdatesWithoutChangingOpenStateAndClosesWhenDisconnected() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val status=BicycleComponents.description(c,themes,"Live · 3 Messwerte")
            val view=SensorMeasurementDetails(c,themes,status);c.setContentView(view)
            val values=view.getChildAt(1) as TextView
            view.update(listOf("Tempo","Kadenz","Leistung"))
            assertNotNull(status.compoundDrawablesRelative[2]);assertEquals(View.GONE,values.visibility)
            status.performClick();assertEquals(View.VISIBLE,values.visibility)
            view.update(listOf("Tempo","Kadenz","Leistung","Distanzzähler"))
            assertEquals(View.VISIBLE,values.visibility);assertTrue(values.text.contains("Distanzzähler"))
            themes.setFlatDesign(true);assertEquals(View.VISIBLE,values.visibility)
            view.update(null);assertFalse(status.isClickable);assertEquals(View.GONE,values.visibility)
            assertNull(status.compoundDrawablesRelative[2])
            view.update(listOf("Tempo"));assertEquals(View.GONE,values.visibility)
        } finally { host.pause().stop().destroy() }
    }
}

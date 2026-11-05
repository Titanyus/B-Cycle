package de.bcycle.app

import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class MapPresentationSettingsTest {
    private fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    @Test fun settingsStartWithHeightWindUseProviderSpecificZoomAndCentralizeElevationSources() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get()
            MainActivity::class.java.getDeclaredMethod("mapSettings").apply { isAccessible=true }.invoke(a)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            val heading=root.findViewWithTag<View>("map_options_title");val column=heading.parent as ViewGroup;val index=column.indexOfChild(heading)
            assertEquals("mapHeightVisible",column.getChildAt(index+1).tag);assertEquals("mapWindRoseVisible",column.getChildAt(index+2).tag)
            assertFalse(nodes(root).filterIsInstance<TextView>().any { it.text.toString()==a.getString(R.string.map_terrain_credit) })
            val panel=MainActivity::class.java.getDeclaredField("mapPanel").apply { isAccessible=true }.get(a) as MapPanel
            val source=nodes(root).filterIsInstance<Spinner>().single()
            val adapter=source.adapter as android.widget.ListAdapter
            assertEquals(8,adapter.count)
            for(position in listOf(0,5,6)) assertFalse(adapter.isEnabled(position))
            for(position in listOf(1,2,3,4,7)) assertTrue(adapter.isEnabled(position))
            val popupAdapter=source.adapter
            val rasterHeading=popupAdapter.getDropDownView(0,null,column) as TextView
            val vectorHeading=popupAdapter.getDropDownView(6,rasterHeading,column) as TextView
            assertEquals("Vektorkarten",vectorHeading.text.toString());assertEquals(900,vectorHeading.typeface.weight)
            val recycledNormal=popupAdapter.getDropDownView(7,vectorHeading,column) as TextView
            assertEquals("Bright",recycledNormal.text.toString());assertEquals(400,recycledNormal.typeface.weight)
            source.setSelection(MapSourcePicker.position(MapSource.VECTOR));Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(21,panel.mapView.map().viewport().maxZoomLevel)
            panel.mapView.setZoom(21.0);assertEquals(21.0,panel.mapView.zoom,.001)
            source.setSelection(MapSourcePicker.position(MapSource.OSM));Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(19,panel.mapView.map().viewport().maxZoomLevel);assertTrue(panel.mapView.zoom<=19)
            source.setSelection(MapSourcePicker.position(MapSource.OPENTOPOMAP));Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(17,panel.mapView.map().viewport().maxZoomLevel)
            val prefs=a.getSharedPreferences("bike",0);prefs.edit().putFloat(MapZoom.preference,20.5f).apply()
            assertEquals(20.5,MapZoom.read(prefs,MapSource.VECTOR),.001);assertEquals(19.0,MapZoom.read(prefs,MapSource.OSM),.001)
            MainActivity::class.java.getDeclaredMethod("sourcesSettings").apply { isAccessible=true }.invoke(a)
            val sources=nodes(ShadowAlertDialog.getLatestAlertDialog().window!!.decorView).filterIsInstance<TextView>().joinToString("\n") { it.text.toString() }
            for(name in listOf("Mapterhorn","Mapzen / AWS Terrain Tiles","OpenFreeMap","Waymarked Trails")) assertTrue(name,sources.contains(name))
        } finally { host.pause().stop().destroy() }
    }
}

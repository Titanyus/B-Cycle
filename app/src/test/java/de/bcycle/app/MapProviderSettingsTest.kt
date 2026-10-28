package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mapsforge.core.model.Tile
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class MapProviderSettingsTest {
    private val testKey="0123456789abcdef0123456789abcdef"
    @Test fun providerUrlsZoomAndMissingKeyFallbackAreExplicit() {
        val context=RuntimeEnvironment.getApplication()
        val tile=Tile(1,2,3,256)
        val topo=MapSource.OPENTOPOMAP.tiles()
        val topoUrl=topo.getTileUrl(tile)
        assertEquals("https",topoUrl.protocol);assertEquals("a.tile.opentopomap.org",topoUrl.host)
        assertEquals("/3/1/2.png",topoUrl.path);assertEquals(443,topoUrl.port)
        assertEquals(17,topo.zoomLevelMax.toInt())
        assertEquals(MapSource.CYCLOSM,MapProviderSettings.source(context,"OPENCYCLEMAP"))
        assertThrows(IllegalArgumentException::class.java) { MapSource.OPENCYCLEMAP.tiles().getTileUrl(tile) }
        MapProviderSettings.write(context,testKey)
        assertEquals(testKey,MapProviderSettings.read(context))
        assertEquals(MapSource.OPENCYCLEMAP,MapProviderSettings.source(context,"OPENCYCLEMAP"))
        val url=MapSource.OPENCYCLEMAP.tiles(testKey).getTileUrl(tile)
        assertEquals("tile.thunderforest.com",url.host);assertEquals("/cycle/3/1/2.png",url.path)
        assertEquals("apikey=$testKey",url.query)
        assertThrows(IllegalArgumentException::class.java) { MapProviderSettings.write(context,"incomplete") }
        assertEquals(testKey,MapProviderSettings.read(context))
        MapProviderSettings.write(context,"")
        assertEquals(MapSource.CYCLOSM,MapProviderSettings.source(context,"OPENCYCLEMAP"))
        assertEquals(MapSource.CYCLOSM,MapSource.decode("unknown"))
    }
    @Test fun mapSettingsRetainPreviousSourceUntilAValidKeyIsEntered() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            runtime.preferences.edit().putString("mapSource","OSM").commit()
            MainActivity::class.java.getDeclaredMethod("mapSettings").apply { isAccessible=true }.invoke(activity)
            shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView

            assertNull(root.findViewWithTag<View>("mapHeading"));assertNull(root.findViewWithTag<View>("mapFollow"))
            assertNull(root.findViewWithTag<View>("map_track_divider"));assertNotNull(root.findViewWithTag<View>("map_options_title"))
            listOf("mapTrackVisible","mapGpxVisible","mapHeightVisible","map_elevation_correction").forEach { assertNotNull(root.findViewWithTag<View>(it)) }
            val heading=root.findViewWithTag<android.widget.TextView>("map_options_title")
            assertEquals(BicycleDesign.strongTypeface,heading.typeface)
            assertEquals(BicycleDesign.contentHeadingSp*activity.resources.displayMetrics.scaledDensity,heading.textSize,.01f)
            assertTrue((heading.layoutParams as android.view.ViewGroup.MarginLayoutParams).topMargin>=BicycleDesign.dp(activity,BicycleDesign.sectionContentGap))
            val source=testDescendants(root).filterIsInstance<Spinner>().single()
            source.setSelection(MapSource.OPENCYCLEMAP.ordinal);shadowOf(Looper.getMainLooper()).idle()
            assertEquals("OSM",runtime.preferences.getString("mapSource",null))
            assertEquals(View.VISIBLE,root.findViewWithTag<View>("map_provider_key_row").visibility)
            val key=root.findViewWithTag<EditText>("map_thunderforest_key")
            key.setText("bad");assertNotNull(key.error)
            assertEquals("",MapProviderSettings.read(activity))
            key.setText(testKey);assertNull(key.error)
            assertEquals("OPENCYCLEMAP",runtime.preferences.getString("mapSource",null))
            key.setText("x");assertEquals(testKey,MapProviderSettings.read(activity))
            key.setText("");assertEquals(MapSource.CYCLOSM,MapProviderSettings.source(activity,"OPENCYCLEMAP"))
            source.setSelection(MapSource.OPENTOPOMAP.ordinal);shadowOf(Looper.getMainLooper()).idle()
            assertEquals("OPENTOPOMAP",runtime.preferences.getString("mapSource",null))
            assertEquals(View.GONE,root.findViewWithTag<View>("map_provider_key_row").visibility)
        } finally { controller.pause().stop().destroy() }
    }
}

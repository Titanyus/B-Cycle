package de.bcycle.app

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.layers.tile.MapTile
import org.oscim.theme.styles.TextStyle
import org.oscim.tiling.*
import org.oscim.backend.canvas.Bitmap
import org.robolectric.*
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class MapLibraryTextRevisionTest {
    private val c get()=RuntimeEnvironment.getApplication()
    @Test fun originalBrightIsPersistedAndDeletionDoesNotResurrectIt() {
        val prefs=c.getSharedPreferences("bike",0)
        val first=MapStyleLibrary.read(prefs).single();assertEquals("Bright",first.name)
        MapStyleLibrary.delete(prefs,first.id)
        assertTrue(MapStyleLibrary.read(prefs).isEmpty())
        val picker=MapSourcePicker(c,ThemeManager(prefs),prefs);picker.select(MapSource.VECTOR)
        assertEquals(7,picker.adapter.count)
        assertTrue((0 until picker.adapter.count).none { picker.adapter.getItem(it)=="Bright" })
        MapStyleLibrary.save(prefs,"Own",null);assertEquals(listOf("Own"),MapStyleLibrary.read(prefs).map { it.name })
    }
    @Test fun legacyLibraryGainsOneOrdinaryBrightAndKeepsItsDeletionAfterBackup() {
        val prefs=c.getSharedPreferences("bike",0)
        val own=SavedMapStyle(java.util.UUID.randomUUID().toString(),"Existing",MapStyleOverrides().json().toString())
        prefs.edit().putString(MapStyleLibrary.KEY,MapStyleLibrary.json(listOf(own)).toString()).commit()
        val bright=MapStyleLibrary.read(prefs).first();assertEquals("Bright",bright.name)
        assertEquals(2,MapStyleLibrary.read(prefs).size)
        MapStyleLibrary.delete(prefs,bright.id)
        val backup=SettingsBackup(prefs,c.getSharedPreferences("route-export",0),ThemeManager(prefs))
        val bytes=backup.export(setOf(BackupSection.MAPS));prefs.edit().clear().commit()
        backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.MAPS)))
        assertEquals(listOf(own.id),MapStyleLibrary.read(prefs).map { it.id })
        val picker=MapSourcePicker(c,ThemeManager(prefs),prefs);picker.select(MapSource.VECTOR)
        assertEquals(-1,picker.selectedItemPosition)
    }
    @Test fun typographyEditorIncludesAndPersistsRoadWaterAndHouseChoices() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0)
            val editor=MapStyleEditorView(a,prefs,ThemeManager(prefs),{},{});a.setContentView(editor)
            val types=editor.findViewWithTag<android.widget.Spinner>("map_place_class")
            assertEquals(MapPlaceClass.entries.size,types.count)
            for(type in listOf(MapPlaceClass.ROAD,MapPlaceClass.WATER,MapPlaceClass.HOUSE)) {
                types.setSelection(type.ordinal);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                editor.findViewWithTag<BicycleSettingSlider>("map_place_size").progress=130
                assertEquals(130,MapStyleOverrides.read(prefs).placeLabels.getValue(type).sizePercent)
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun realSourceTileContainsUsableHouseLabelsAndAllTextKindsAcceptTypography() {
        val view=BicycleMapView(c)
        val options=MapVectorLayers(MapVectorLayers().visible+MapVectorLayer.HOUSE_NUMBERS)
        val custom=MapStyleOverrides(placeLabels=mapOf(MapPlaceClass.ROAD to MapPlaceTextStyle(900,150),MapPlaceClass.HOUSE to MapPlaceTextStyle(700,140),MapPlaceClass.WATER to MapPlaceTextStyle(500,120)))
        val theme=VtmBrightTheme.load(false,options,custom)
        val baseline=VtmBrightTheme.load(false,options)
        try {
            var houses=0
            val bytes=javaClass.getResourceAsStream("/maps/openfreemap-14-8663-5675.pbf")!!.use { it.readBytes() }
            val decoder=org.oscim.tiling.source.mvt.TileDecoder("de")
            assertTrue(decoder.decode(Tile(8663,5675,14),object:ITileDataSink {
                override fun process(element:MapElement) {
                    if(element.tags.getValue("layer")!="housenumber") return
                    val styles=theme.matchElement(element.type,element.tags,14).orEmpty().filterIsInstance<TextStyle>()
                    assertTrue(styles.isNotEmpty())
                    for(text in styles) { assertEquals("housenumber",text.textKey);assertFalse(element.tags.getValue(text.textKey).isNullOrBlank()) }
                    houses++
                }
                override fun setTileImage(bitmap:Bitmap) {}
                override fun completed(result:QueryResult) {}
            },bytes.inputStream()))
            assertEquals(32,houses)
            for((layer,kind,factor) in listOf(Triple("transportation_name","primary",1.5f),Triple("housenumber","",1.4f),Triple("water_name","lake",1.2f))) {
                val tags=TagSet().apply { add(Tag("layer",layer));add(Tag("class",kind));add(Tag("name","Test"));add(Tag("housenumber","12")) }
                val type=if(layer=="transportation_name") GeometryBuffer.GeometryType.LINE else GeometryBuffer.GeometryType.POINT
                val before=baseline.matchElement(type,tags,18).orEmpty().filterIsInstance<TextStyle>().first()
                val after=theme.matchElement(type,tags,18).orEmpty().filterIsInstance<TextStyle>().first()
                assertEquals(before.fontSize*factor,after.fontSize,.001f)
            }
            val prefs=c.getSharedPreferences("bike",0);custom.write(prefs);assertEquals(custom,MapStyleOverrides.read(prefs))
        } finally { theme.dispose();baseline.dispose();view.disposeMap() }
    }
}

package de.bcycle.app

import android.graphics.Bitmap
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.layers.tile.MapTile
import org.oscim.theme.styles.TextStyle
import org.oscim.tiling.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class MapVectorLayersTest {
    @Test fun realThemeFiltersFeaturesWithoutChangingOtherStylesAndProfileBackupRoundTrips() {
        val context=RuntimeEnvironment.getApplication();val view=BicycleMapView(context)
        val prefs=context.getSharedPreferences("bike",0)
        val cases=listOf(
            Triple(MapVectorLayer.MAIN_ROADS,"transportation","primary"),
            Triple(MapVectorLayer.MINOR_ROADS,"transportation","minor"),
            Triple(MapVectorLayer.PATHS,"transportation","track"),
            Triple(MapVectorLayer.BUILDINGS,"building",""),
            Triple(MapVectorLayer.NATURE,"landcover","grass"),
            Triple(MapVectorLayer.WATER,"water","lake"),
            Triple(MapVectorLayer.PLACE_NAMES,"place","city"),
            Triple(MapVectorLayer.ROAD_NAMES,"transportation_name","primary"),
            Triple(MapVectorLayer.POIS,"poi","fuel"),
            Triple(MapVectorLayer.LOCAL_BOUNDARIES,"boundary",""),
            Triple(MapVectorLayer.ADMIN_AREAS,"boundary",""))
        try {
            val all=MapVectorLayer.entries.toSet();val enabled=VtmBrightTheme.load(true,MapVectorLayers(all))
            for((category,source,kind) in cases) {
                val tags=TagSet().apply { add(Tag("layer",source));add(Tag("class",kind));add(Tag("admin_level",if(category==MapVectorLayer.ADMIN_AREAS) "7" else "8"));add(Tag("name","Example"));add(Tag("rank","1")) }
                val type=when(source) { "place","poi" -> GeometryBuffer.GeometryType.POINT;"building","landcover","water" -> GeometryBuffer.GeometryType.POLY;else -> GeometryBuffer.GeometryType.LINE }
                assertFalse("enabled $source/$kind",enabled.matchElement(type,tags,19).isNullOrEmpty())
                val disabled=VtmBrightTheme.load(true,MapVectorLayers(all-category))
                try { assertTrue("disabled $source/$kind",disabled.matchElement(type,tags,19).isNullOrEmpty()) }
                finally { disabled.dispose() }
            }
            val noMinor=VtmBrightTheme.load(false,MapVectorLayers(all-MapVectorLayer.MINOR_ROADS))
            try {
                val names=TagSet().apply { add(Tag("layer","transportation_name"));add(Tag("class","minor")) }
                assertTrue("hidden roads cannot leave floating names",noMinor.matchElement(GeometryBuffer.GeometryType.LINE,names,19).isNullOrEmpty())
                names.set(arrayOf(Tag("layer","transportation_name"),Tag("class","primary")))
                assertTrue(noMinor.matchElement(GeometryBuffer.GeometryType.LINE,names,19).orEmpty().any { it is TextStyle })
            } finally { noMinor.dispose() }
            enabled.dispose()
            val original=MapVectorLayers.read(prefs)
            assertFalse(MapVectorLayer.POIS in original);assertFalse(MapVectorLayer.CONTOURS in original)
            val edit=prefs.edit();MapVectorLayer.entries.forEach { edit.putBoolean(it.key,!it.defaultVisible) };edit.commit()
            val selected=MapVectorLayers.read(prefs)
            val store=BikeUiConfigurationStore(prefs);store.ensure(listOf(0,1));store.save(0)
            prefs.edit().putBoolean(MapVectorLayer.POIS.key,false).commit();store.save(1);store.apply(0)
            assertEquals(selected,MapVectorLayers.read(prefs))
            assertTrue(BikeUiConfigurationScope.capture(prefs).maps.containsKey(MapVectorLayer.CONTOURS.key))
            val backup=SettingsBackup(prefs,context.getSharedPreferences("route-export",0),ThemeManager(prefs))
            val bytes=backup.export(setOf(BackupSection.MAPS))
            prefs.edit().clear().commit();backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.MAPS)))
            assertEquals(selected,MapVectorLayers.read(prefs))
            val bank=JSONObject().put("0",JSONObject().put(MapVectorLayer.POIS.key,JSONObject().put("type","string").put("value","true")))
            val malformed=JSONObject().put("bike",JSONObject().put(BikeUiConfigurationStore.KEY,bank.toString()))
            try { SettingsBackupSupplement(context).plan(BackupSection.PROFILE,malformed);fail("Profile bank switch must be boolean") } catch(_:IllegalArgumentException) { }
            val invalid=JSONObject(String(bytes));invalid.getJSONObject("sections").getJSONObject("mapsNavigation").getJSONObject("vectorLayers").put("CONTOURS","true")
            try { backup.decode(invalid.toString().toByteArray());fail("String is not a valid switch") } catch(_:SettingsBackupFailure) { }
        } finally { view.disposeMap() }
    }

    @Test fun terrariumPlaneBecomesContinuousContoursWithMatchingTileEdgesAndCachedDownloads() {
        var requests=0
        val http=OkHttpClient.Builder().addInterceptor { chain ->
            requests++
            val parts=chain.request().url.pathSegments;val x=parts[parts.size-2].toInt();val y=parts.last().removeSuffix(".png").toInt()
            // At x=0 this slopes 0..255 m, at x=1 256..511 m: shared east/west edge is 256 m.
            val bitmap=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
            val pixels=IntArray(256*256) { i -> val h=32768+x*256+y*256+i%256+i/256;0xff000000.toInt() or ((h/256) shl 16) or ((h%256) shl 8) }
            bitmap.setPixels(pixels,0,256,0,0,256,256)
            val bytes=ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }.toByteArray();bitmap.recycle()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(bytes.toResponseBody()).build()
        }.build()
        val source=ContourSource(http);val data=source.dataSource
        fun tile(x:Int):List<MapElement> {
            val result=ArrayList<MapElement>()
            data.query(MapTile(null,x,0,13),object:ITileDataSink {
                override fun process(element:MapElement) { result.add(MapElement(element)) }
                override fun setTileImage(bitmap:org.oscim.backend.canvas.Bitmap) { fail("Contours must remain vector lines") }
                override fun completed(query:QueryResult) { assertEquals(QueryResult.SUCCESS,query) }
            });return result
        }
        try {
            assertEquals(0,requests)
            val west=tile(0);assertEquals(4,requests);val east=tile(1);assertEquals(6,requests)
            assertTrue(west.isNotEmpty());assertTrue(east.isNotEmpty())
            assertTrue("connected line instead of isolated cell segments",west.any { it.pointNextPos>20 })
            fun endpoints(lines:List<MapElement>,edge:Float)=lines.flatMap { line ->
                val count=line.pointNextPos
                listOf(line.points[0] to line.points[1],line.points[count-2] to line.points[count-1])
            }.filter { kotlin.math.abs(it.first-edge)<.001f }.map { it.second }.sorted()
            assertEquals(endpoints(west,Tile.SIZE.toFloat()),endpoints(east,0f))
            tile(0);assertEquals("decoded neighboring DEMs reused",6,requests)
            assertEquals(100,TerrainContours.interval(10));assertEquals(10,TerrainContours.interval(13))
            assertEquals(123.5f,TerrainContours.elevation(0xff807b80.toInt()),0f)
            assertTrue(TerrainContours.elevation(0).isNaN())
            val gaps=FloatArray(25) { (it%5+it/5)*10f };gaps[12]=Float.NaN
            val gapLines=ArrayList<TerrainContours.Line>();TerrainContours.trace(gaps,5,10) { gapLines.add(it) }
            assertTrue(gapLines.isNotEmpty());assertTrue(gapLines.all { line -> line.points.all { it.isFinite() } })
            var emitted=0;TerrainContours.trace(FloatArray(9) { 100f },3,10) { emitted++ };assertEquals(0,emitted)
        } finally { data.dispose();source.close() }
    }
}

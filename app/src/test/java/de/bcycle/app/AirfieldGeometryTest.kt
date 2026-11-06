package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.backend.CanvasAdapter
import org.oscim.backend.canvas.Bitmap
import org.oscim.backend.canvas.Paint.Cap
import org.oscim.core.*
import org.oscim.theme.styles.AreaStyle
import org.oscim.theme.styles.LineStyle
import org.oscim.tiling.*
import org.oscim.tiling.source.mvt.TileDecoder
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class AirfieldGeometryTest {
    private val samples=listOf("gundelfingen-13-4331-2828","munich-13-4364-2835","weissenhorn-13-4326-2837","weissenhorn-14-8653-5675")
    @Test fun realRunwayFootprintsStayAreasAndCentreLinesStayNarrowWithNoRoundedLengthExtension() {
        val map=BicycleMapView(RuntimeEnvironment.getApplication())
        var polygons=0;var lines=0
        try {
            val theme=VtmBrightTheme.load(false)
            try {
                for(sample in samples) {
                    val parts=sample.split('-');val z=parts[1].toInt();val x=parts[2].toInt();val y=parts[3].toInt()
                    var runwaySeen=false
                    val sink=object:ITileDataSink {
                        override fun process(element:MapElement) {
                            val kind=element.tags.getValue("class")
                            if(kind !in setOf("runway","taxiway")) return
                            if(kind=="runway") runwaySeen=true
                            val coordinates=element.points.copyOf(element.pointNextPos)
                            for(zoom in 13..MapZoom.maximum(MapSource.VECTOR)) {
                                val paints=theme.matchElement(element.type,element.tags,zoom).orEmpty()
                                if(element.isPoly) {
                                    val areas=paints.filterIsInstance<AreaStyle>()
                                    assertTrue("$sample polygon must retain actual footprint at zoom $zoom",areas.isNotEmpty())
                                    assertTrue(areas.all { android.graphics.Color.alpha(it.color)==255 })
                                    assertTrue("No broad runway strokes around a polygon",paints.none { it is LineStyle })
                                } else if(element.isLine) {
                                    val strokes=paints.filterIsInstance<LineStyle>()
                                    assertTrue(strokes.isNotEmpty())
                                    for(stroke in strokes) {
                                        assertEquals("$sample/$zoom no pill-shaped runway extension",Cap.BUTT,stroke.cap)
                                        assertTrue("$sample/$zoom oversized width ${stroke.width}",stroke.width<=1.25f*CanvasAdapter.getScale()+.001f)
                                    }
                                }
                                assertArrayEquals("Theme matching never changes airport coordinates",coordinates,element.points.copyOf(element.pointNextPos),0f)
                            }
                            if(element.isPoly) polygons++ else if(element.isLine) lines++
                        }
                        override fun setTileImage(bitmap:Bitmap)=fail("MVT stays vector")
                        override fun completed(result:QueryResult) {}
                    }
                    javaClass.getResourceAsStream("/maps/airfield-$sample.pbf")!!.use { assertTrue(TileDecoder("de").decode(Tile(x,y,z.toByte()),sink,it)) }
                    assertTrue(sample,runwaySeen)
                }
                assertTrue(polygons>=3);assertTrue(lines>=5)
            } finally { theme.dispose() }
        } finally { map.disposeMap() }
    }
}

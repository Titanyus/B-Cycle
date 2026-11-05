package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.backend.canvas.Bitmap
import org.oscim.core.*
import org.oscim.layers.tile.MapTile
import org.oscim.tiling.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class VtmOverzoomGeometryTest {
    private fun polygon(building:Boolean)=MapElement().apply {
        layer=5;tags.add(Tag("layer",if(building) "building" else "landuse"));startPolygon()
        addPoint(0f,0f);addPoint(256f,0f);addPoint(256f,256f);addPoint(0f,256f)
    }
    private fun source(building:Boolean)=object:ITileDataSource {
        override fun query(tile:MapTile,sink:ITileDataSink) { sink.process(polygon(building));sink.completed(QueryResult.SUCCESS) }
        override fun cancel()=Unit
        override fun dispose()=Unit
    }
    private fun read(source:ITileDataSource,zoom:Int):List<MapElement> {
        val result=ArrayList<MapElement>()
        source.query(MapTile(null,0,0,zoom),object:ITileDataSink {
            override fun process(element:MapElement) { result.add(MapElement(element)) }
            override fun setTileImage(bitmap:Bitmap)=fail("Vector source")
            override fun completed(status:QueryResult) { assertEquals(QueryResult.SUCCESS,status) }
        });return result
    }
    @Test fun nativeOverzoomBuildingExceedsSignedVertexRangeButClippedSourcePreservesTheFilledTileAtMaxZoom() {
        val view=BicycleMapView(RuntimeEnvironment.getApplication())
        try {
            val native=read(OverzoomTileDataSource(source(true),14),19).single()
            assertTrue("Reproduce native building overflow before packing",native.points.take(native.pointNextPos).any { it*8>Short.MAX_VALUE })
            for(building in listOf(true,false)) for(zoom in 15..21) {
                val safe=read(VtmClippedVectorDataSource(OverzoomTileDataSource(source(building),14),14),zoom).single()
                assertEquals(if(building) "building" else "landuse",safe.tags.getValue("layer"))
                assertTrue(safe.isPoly);assertTrue(safe.pointNextPos>=8)
                for(value in safe.points.take(safe.pointNextPos)) {
                    assertTrue(value.isFinite());assertTrue(value*8 in Short.MIN_VALUE.toFloat()..Short.MAX_VALUE.toFloat())
                }
                val xs=(0 until safe.pointNextPos step 2).map { safe.points[it] }
                val ys=(1 until safe.pointNextPos step 2).map { safe.points[it] }
                assertTrue("Full visible tile stays covered, not inverted",xs.minOrNull()!!<=0 && xs.maxOrNull()!!>=Tile.SIZE && ys.minOrNull()!!<=0 && ys.maxOrNull()!!>=Tile.SIZE)
            }
            val original=read(source(true),14).single();val untouched=read(VtmClippedVectorDataSource(source(true),14),14).single()
            assertArrayEquals(original.points,untouched.points,0f)
        } finally { view.disposeMap() }
    }
}

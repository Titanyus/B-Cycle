package de.bcycle.app

import android.content.Context
import org.oscim.core.MapPosition
import org.oscim.event.Event
import org.oscim.core.MapElement
import org.oscim.core.Tag
import org.oscim.core.Tile
import org.oscim.layers.tile.MapTile
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.map.Map
import org.oscim.theme.ThemeFile
import org.oscim.theme.ThemeLoader
import org.oscim.theme.internal.VtmThemes
import org.oscim.tiling.ITileDataSink
import org.oscim.tiling.ITileDataSource
import org.oscim.tiling.QueryResult
import org.oscim.tiling.TileSource
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** Optional native vector overlay. DEM decoding/contours only run on its one tile worker. */
internal class VtmContourLayer(context:Context,map:Map,style:MapStyleOverrides=MapStyleOverrides()):VectorTileLayer(map,ContourSource(MapterhornTiles(context))) {
    override fun getNumLoaders()=1
    init { setTheme(VtmLiveTheme(ThemeLoader.load(ContourTheme(style.compiledStyles())) as org.oscim.theme.RenderTheme,style,allowBackground=false));pause() }
    fun applyColors(style:MapStyleOverrides) { (theme as VtmLiveTheme).applyColors(style) }
    override fun onMapEvent(event:Event,position:MapPosition) {
        tileRenderer().setBitmapAlpha(if(position.zoomLevel>=10) 1f else 0f)
        super.onMapEvent(event,position)
    }
    fun resume() { isEnabled=true;resumeLoaders() }
    fun pause() { isEnabled=false;pauseLoaders(false) }
    private class ContourTheme(private val style:MapStyleOverrides):ThemeFile by VtmThemes.OPENMAPTILES {
        override fun getRenderThemeAsStream()=MapStyleXml.apply("""
            <rendertheme xmlns="http://opensciencemap.org/rendertheme" version="1" map-background="#00000000">
              <m e="way" k="contour" v="minor"><line id="contour-minor" stroke="#668b7355" width="0.6" fix="true" /></m>
              <m e="way" k="contour" v="major"><line id="contour-major" stroke="#998b7355" width="1" fix="true" /></m>
            </rendertheme>
        """.trimIndent(),style).byteInputStream()
    }
}

/** Mapterhorn's 512 px DEMs; map detail stops at z14 and shares correction's cache. */
internal class ContourSource(private val tiles:MapterhornTiles):TileSource(10,14) {
    override fun open()=OpenResult.SUCCESS
    override fun close() { tiles.close() }
    override fun getDataSource():ITileDataSource=object:ITileDataSource {
        private val generation=AtomicInteger()
        @Volatile private var disposed=false
        override fun cancel() { generation.incrementAndGet();tiles.cancel() }
        override fun dispose() { disposed=true;cancel() }
        override fun query(tile:MapTile,sink:ITileDataSink) {
            val token=generation.get()
            fun cancelled()=disposed || generation.get()!=token
            try {
                val z=tile.zoomLevel.toInt();val size=257
                require(z in 10..14)
                fun dem(x:Int,y:Int):MapterhornTiles.Dem {
                    tiles.load(z,x,y,::cancelled)?.let { return it }
                    // Global data extends to z12. Detailed regional coverage is not universal.
                    if(z>12) tiles.load(12,Math.floorDiv(x,1 shl (z-12)),Math.floorDiv(y,1 shl (z-12)),::cancelled)?.let { return it }
                    throw IOException("DEM unavailable")
                }
                val center=dem(tile.tileX,tile.tileY)
                val east=dem(tile.tileX+1,tile.tileY)
                val south=dem(tile.tileX,tile.tileY+1)
                val corner=dem(tile.tileX+1,tile.tileY+1)
                // One map-grid sample per display pixel, not four times as many marching cells.
                // The shared east/south sample keeps adjacent tile endpoints identical.
                val grid=FloatArray(size*size) { i -> val x=i%size;val y=i/size
                    val tx=tile.tileX+if(x==256) 1 else 0;val ty=tile.tileY+if(y==256) 1 else 0
                    val data=when { x==256 && y==256->corner;x==256->east;y==256->south;else->center }
                    data.at(z,tx,ty,if(x==256) 0 else x*2,if(y==256) 0 else y*2)
                }
                val element=MapElement()
                TerrainContours.trace(grid,size,TerrainContours.interval(z),::cancelled) { line ->
                    element.clear();element.tags.clear();element.tags.add(Tag("contour",if(line.elevation%100==0) "major" else "minor"))
                    element.layer=5;element.startLine()
                    for(i in line.points.indices step 2) element.addPoint(line.points[i]*Tile.SIZE/256f,line.points[i+1]*Tile.SIZE/256f)
                    sink.process(element)
                }
                sink.completed(if(cancelled()) QueryResult.FAILED else QueryResult.SUCCESS)
            } catch(_:IOException) { sink.completed(QueryResult.FAILED) }
        }
    }
}

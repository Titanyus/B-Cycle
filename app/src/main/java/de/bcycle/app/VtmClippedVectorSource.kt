package de.bcycle.app

import org.oscim.backend.CanvasAdapter
import org.oscim.backend.canvas.Bitmap
import org.oscim.core.MapElement
import org.oscim.core.Tile
import org.oscim.layers.tile.MapTile
import org.oscim.tiling.ITileDataSink
import org.oscim.tiling.ITileDataSource
import org.oscim.tiling.QueryResult
import org.oscim.tiling.source.mvt.OpenMapTilesMvtTileSource
import org.oscim.utils.geom.TileClipper

/** Native overzoom keeps whole buildings. Bound their transformed geometry before short-vertex packing. */
internal class VtmClippedVectorSource(builder:OpenMapTilesMvtTileSource.Builder<*>):OpenMapTilesMvtTileSource(builder) {
    override fun getDataSource():ITileDataSource=VtmClippedVectorDataSource(super.getDataSource(),overZoom)
}
internal class VtmClippedVectorDataSource(private val native:ITileDataSource,private val sourceZoom:Int):ITileDataSource {
    override fun query(tile:MapTile,sink:ITileDataSink) {
        if(tile.zoomLevel<=sourceZoom) { native.query(tile,sink);return }
        val buffer=32*CanvasAdapter.getScale()
        val low=-buffer;val high=Tile.SIZE+buffer
        val clipper=TileClipper(low,low,high,high)
        native.query(tile,object:ITileDataSink {
            override fun process(element:MapElement) {
                if(element.isPoly || element.isLine) {
                    // Already clipped ordinary areas/roads take the unchanged fast path.
                    var outside=false
                    for(i in 0 until element.pointNextPos) if(element.points[i]<low || element.points[i]>high) { outside=true;break }
                    if(outside && !clipper.clip(element)) return
                }
                sink.process(element)
            }
            override fun setTileImage(bitmap:Bitmap) { sink.setTileImage(bitmap) }
            override fun completed(result:QueryResult) { sink.completed(result) }
        })
    }
    override fun cancel()=native.cancel()
    override fun dispose()=native.dispose()
}

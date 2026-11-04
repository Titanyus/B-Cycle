package de.bcycle.app

import android.content.Context
import org.oscim.layers.Layer
import org.oscim.layers.tile.TileLayer
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.layers.tile.vector.labeling.BCycleLabelLayer
import org.oscim.layers.tile.vector.labeling.LabelTileLoaderHook
import org.oscim.map.Map
import org.oscim.theme.ThemeFile
import org.oscim.theme.internal.VtmThemes

/** Owns every layer of one base map; saved/live tracks always remain above them. */
internal class VtmBaseMap(context:Context,private val map:Map,source:MapSource,key:String,cycleways:Boolean,private val glQueue:(Runnable)->Unit,options:MapVectorLayers=MapVectorLayers()) {
    companion object { const val cyclewaysKey="mapVectorCycleways" }
    private var detached=false
    private val tiles:TileLayer
    private val contours:VtmContourLayer?
    private val labels:Layer?
    private val resumeTiles:()->Unit
    private val pauseTiles:()->Unit
    init {
        if(source==MapSource.VECTOR) {
            val vector=object:VectorTileLayer(map,VtmTileHttp.vector(context)) {
                override fun getNumLoaders()=2
                fun resumeTiles() { resumeLoaders() }
                fun pauseTiles() { pauseLoaders(false) }
            }
            vector.setTheme(VtmBrightTheme.load(cycleways,options))
            tiles=vector
            contours=if(MapVectorLayer.CONTOURS in options) VtmContourLayer(context,map) else null
            labels=BCycleLabelLayer(map,vector,LabelTileLoaderHook(),source.maximumZoom)
            resumeTiles=vector::resumeTiles;pauseTiles=vector::pauseTiles
        } else {
            val raster=VtmRasterLayer(map,VtmTileHttp.source(context,source,key))
            tiles=raster
            contours=null
            labels=null
            resumeTiles=raster::resume;pauseTiles=raster::pause
        }
        pause()
        map.layers().add(1,tiles)
        applyBackground()
        contours?.let { map.layers().add(2,it) }
        labels?.let { map.layers().add(if(contours==null) 2 else 3,it) }
        map.clearMap()
    }
    // VectorTileLayer.setTheme updates tile styles, but unlike Map.setTheme it does not
    // set the renderer background. VTM also resets that shared color in every new MapView.
    private fun applyBackground() {
        (tiles as? VectorTileLayer)?.theme?.let { org.oscim.renderer.MapRenderer.setBackgroundColor(it.mapBackground) }
    }
    fun resume() { applyBackground();tiles.isEnabled=true;labels?.isEnabled=true;resumeTiles();contours?.resume();map.updateMap(true) }
    fun pause() { tiles.isEnabled=false;labels?.isEnabled=false;pauseTiles();contours?.pause() }
    fun onDetach() {
        if(detached) return
        detached=true
        pause()
        labels?.let { layer ->
            map.layers().remove(layer);layer.onDetach()
            if(layer is BCycleLabelLayer) glQueue(Runnable { layer.releaseRenderer() })
        }
        contours?.let { map.layers().remove(it);it.onDetach() }
        map.layers().remove(tiles);tiles.onDetach()
    }
}

/** Bright adapted to VTM/OpenMapTiles, strictly flat; cycling remains an optional highlight. */
internal class VtmCycleTheme(private val cycleways:Boolean):ThemeFile by VtmThemes.OPENMAPTILES {
    override fun getRenderThemeAsStream():java.io.InputStream {
        val base=org.oscim.backend.AssetAdapter.readFileAsStream("styles/bright-vtm.xml").use { it.bufferedReader().readText() }
        val highlight="""
            <m e="way" k="layer" v="transportation" zoom-min="12">
                <m k="subclass" v="cycleway">
                    <line stroke="#2478c4" width="2.5" fix="true" cap="round" />
                </m>
                <m k="subclass" v="path|footway|pedestrian|track">
                    <m k="bicycle" v="designated|official">
                        <line stroke="#2478c4" width="2.5" fix="true" cap="round" />
                    </m>
                </m>
            </m>
        """.trimIndent()
        return (if(cycleways) base.replace("</rendertheme>","$highlight\n</rendertheme>") else base).byteInputStream()
    }
}

package de.bcycle.app

import android.content.Context
import org.oscim.layers.Layer
import org.oscim.layers.tile.TileLayer
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.layers.tile.vector.labeling.LabelTileLoaderHook
import org.oscim.map.Map
import org.oscim.theme.ThemeFile
import org.oscim.theme.ThemeLoader
import org.oscim.theme.internal.VtmThemes

/** Owns every layer of one base map; saved/live tracks always remain above them. */
internal class VtmBaseMap(context:Context,private val map:Map,source:MapSource,key:String,cycleways:Boolean) {
    companion object { const val cyclewaysKey="mapVectorCycleways" }
    private val tiles:TileLayer
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
            vector.setTheme(ThemeLoader.load(VtmCycleTheme(cycleways)))
            tiles=vector
            labels=LabelLayer(map,vector,LabelTileLoaderHook(),source.maximumZoom)
            resumeTiles=vector::resumeTiles;pauseTiles=vector::pauseTiles
        } else {
            val raster=VtmRasterLayer(map,VtmTileHttp.source(context,source,key))
            tiles=raster
            labels=null
            resumeTiles=raster::resume;pauseTiles=raster::pause
        }
        pause()
        map.layers().add(1,tiles)
        labels?.let { map.layers().add(2,it) }
        map.clearMap()
    }
    fun resume() { tiles.isEnabled=true;labels?.isEnabled=true;resumeTiles();map.updateMap(true) }
    fun pause() { tiles.isEnabled=false;labels?.isEnabled=false;pauseTiles() }
    fun onDetach() {
        pause()
        labels?.let { map.layers().remove(it);it.onDetach() }
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

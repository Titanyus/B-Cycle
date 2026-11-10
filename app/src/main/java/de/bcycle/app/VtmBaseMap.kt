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
internal class VtmBaseMap(context:Context,private val map:Map,source:MapSource,key:String,cycleways:Boolean,private val glQueue:(Runnable)->Unit,options:MapVectorLayers=MapVectorLayers(),style:MapStyleOverrides=MapStyleOverrides()) {
    companion object { const val cyclewaysKey="mapVectorCycleways" }
    @Volatile private var detached=false
    private val colorLock=Any()
    private var pendingColors:MapStyleOverrides?=null
    private var colorQueued=false
    private val tiles:TileLayer
    private val foreground:VtmStyledVectorLayer?
    private val cyclingRoutes:VtmCyclingRoutesLayer?
    private val contours:VtmContourLayer?
    private val hillshade:VtmHillshadeLayer?
    private val labels:Layer?
    private val resumeTiles:()->Unit
    private val pauseTiles:()->Unit
    init {
        if(source==MapSource.VECTOR) {
            val split=MapVectorLayer.CONTOURS in options || MapVectorLayer.HILLSHADE in options
            val vector=VtmStyledVectorLayer(context,map,cycleways,options,style,if(split) VtmMapPass.GROUND else VtmMapPass.ALL)
            tiles=vector
            foreground=if(split) VtmStyledVectorLayer(context,map,cycleways,options,style,VtmMapPass.FOREGROUND) else null
            cyclingRoutes=if(MapVectorLayer.CYCLE_ROUTES in options) VtmCyclingRoutesLayer(context,map,style) else null
            hillshade=if(MapVectorLayer.HILLSHADE in options) VtmHillshadeLayer(map) else null
            contours=if(MapVectorLayer.CONTOURS in options) VtmContourLayer(context,map,style) else null
            labels=BCycleLabelLayer(map,foreground ?: vector,LabelTileLoaderHook(),source.maximumZoom)
            resumeTiles=vector::resumeTiles;pauseTiles=vector::pauseTiles
        } else {
            val raster=VtmRasterLayer(map,VtmTileHttp.source(context,source,key))
            tiles=raster
            foreground=null
            cyclingRoutes=null
            contours=null
            hillshade=null
            labels=null
            resumeTiles=raster::resume;pauseTiles=raster::pause
        }
        pause()
        map.layers().add(1,tiles)
        applyBackground()
        var index=2
        hillshade?.let { map.layers().add(index++,it) }
        contours?.let { map.layers().add(index++,it) }
        foreground?.let { map.layers().add(index++,it) }
        cyclingRoutes?.let { map.layers().add(index++,it) }
        labels?.let { map.layers().add(index,it) }
        map.clearMap()
    }
    // VectorTileLayer.setTheme updates tile styles, but unlike Map.setTheme it does not
    // set the renderer background. VTM also resets that shared color in every new MapView.
    private fun applyBackground() {
        (tiles as? VectorTileLayer)?.theme?.let { BicycleGlContexts.background(map,it.mapBackground) }
    }
    /** Coalesce RGB edits without touching tile managers, workers, label placement or geometry buffers. */
    fun updateColors(previous:MapStyleOverrides,next:MapStyleOverrides):Boolean {
        if(detached || tiles !is VtmStyledVectorLayer || !next.canUpdateColorsFrom(previous)) return false
        synchronized(colorLock) {
            pendingColors=next
            if(colorQueued) return true
            colorQueued=true
        }
        glQueue(Runnable {
            val style=synchronized(colorLock) { colorQueued=false;pendingColors.also { pendingColors=null } }
            if(!detached && style!=null) {
                (tiles.theme as VtmBrightTheme).applyColors(style)
                foreground?.let { (it.theme as VtmBrightTheme).applyColors(style) }
                contours?.applyColors(style)
                cyclingRoutes?.applyColors(style)
                // Include VTM's incoming-tile fade matte as well as the global clear colour.
                tiles.tileRenderer().setOverdrawColor(tiles.theme.mapBackground)
                foreground?.tileRenderer()?.setOverdrawColor(0)
                applyBackground();map.render()
            }
        })
        return true
    }
    fun resume() { applyBackground();tiles.isEnabled=true;labels?.isEnabled=true;resumeTiles();hillshade?.resume();contours?.resume();cyclingRoutes?.resume();foreground?.resumeTiles();foreground?.isEnabled=true;map.updateMap(true) }
    fun pause() { tiles.isEnabled=false;labels?.isEnabled=false;pauseTiles();hillshade?.pause();contours?.pause();cyclingRoutes?.pause();foreground?.pauseTiles();foreground?.isEnabled=false }
    fun onDetach() {
        if(detached) return
        detached=true
        synchronized(colorLock) { pendingColors=null }
        pause()
        labels?.let { layer ->
            map.layers().remove(layer);layer.onDetach()
            if(layer is BCycleLabelLayer) glQueue(Runnable { layer.releaseRenderer() })
        }
        foreground?.let { map.layers().remove(it);it.onDetach() }
        contours?.let { map.layers().remove(it);it.onDetach() }
        hillshade?.let { map.layers().remove(it);it.onDetach() }
        cyclingRoutes?.let { map.layers().remove(it);it.onDetach() }
        map.layers().remove(tiles);tiles.onDetach()
    }
}

/** Bright adapted to VTM/OpenMapTiles, strictly flat; cycling remains an optional highlight. */
internal class VtmCycleTheme(private val cycleways:Boolean,private val style:MapStyleOverrides=MapStyleOverrides()):ThemeFile by VtmThemes.OPENMAPTILES {
    override fun getRenderThemeAsStream():java.io.InputStream {
        val originalBase=MapStyleXml.trackBase(org.oscim.backend.AssetAdapter.readFileAsStream("styles/bright-vtm.xml").use { it.bufferedReader().readText() })
        val base=originalBase.replace("</rendertheme>","""<m k="layer" v="housenumber" zoom-min="14"><text id="house-number" k="housenumber" size="11" fill="#ff555555" stroke="#ffffffff" stroke-width="1.2" caption="true" /></m></rendertheme>""")
        val highlight=listOf(Triple(6,6,.35f),Triple(7,7,.45f),Triple(8,8,.6f),Triple(9,9,.75f),Triple(10,10,.9f),Triple(11,11,1.1f),Triple(12,12,1.4f),Triple(13,13,1.8f),Triple(14,14,2.2f),Triple(15,24,2.5f)).joinToString("\n") { (minimum,maximum,width) ->
            """
            <m e="way" k="layer" v="transportation" zoom-min="$minimum" zoom-max="$maximum">
                <m k="subclass" v="cycleway">
                    <line id="bcycle-cycleway" stroke="#2478c4" width="$width" fix="true" cap="round" />
                </m>
                <m k="subclass" v="path|footway|pedestrian|track">
                    <m k="bicycle" v="designated|official">
                        <line id="bcycle-cycleway" stroke="#2478c4" width="$width" fix="true" cap="round" />
                    </m>
                </m>
            </m>
            """.trimIndent()
        }
        return MapStyleXml.roadSurfaces(MapStyleXml.apply(if(cycleways) base.replace("</rendertheme>","$highlight\n</rendertheme>") else base,style),style).byteInputStream()
    }
}

/** A second pass exists only while contours or hillshade are enabled; decoded ground is never painted over them. */
internal class VtmStyledVectorLayer(context:Context,map:Map,cycleways:Boolean,options:MapVectorLayers,
    style:MapStyleOverrides,internal val pass:VtmMapPass):VectorTileLayer(map,VtmTileHttp.vector(context,sharePasses=pass!=VtmMapPass.ALL)) {
    override fun getNumLoaders()=1
    init { setTheme(VtmBrightTheme.load(cycleways,options,style,pass)) }
    fun resumeTiles() { resumeLoaders() }
    fun pauseTiles() { pauseLoaders(false) }
}

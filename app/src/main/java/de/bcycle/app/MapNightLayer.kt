package de.bcycle.app

import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.graphics.Matrix
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.map.android.graphics.AndroidBitmap
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.download.tilesource.TileSource
import org.mapsforge.map.model.MapViewPosition

/** Raster-only night palette: roads/labels stay readable, terrain and water retain their distinction. */
internal object MapNightPalette {
    const val background = 0xff171d22.toInt()
    private val table by lazy { IntArray(32 * 32 * 32) { index ->
        shade(((index ushr 10) * 8 + 4).coerceAtMost(255), (((index ushr 5) and 31) * 8 + 4).coerceAtMost(255),
            ((index and 31) * 8 + 4).coerceAtMost(255))
    } }
    fun color(color: Int): Int = (color and -0x1000000) or
        (table[(((color ushr 19) and 31) shl 10) or (((color ushr 11) and 31) shl 5) or ((color ushr 3) and 31)] and 0xffffff)
    private fun shade(r: Int, g: Int, b: Int): Int {
        fun rgb(red: Double, green: Double, blue: Double) = android.graphics.Color.rgb(
            red.toInt().coerceIn(0, 255), green.toInt().coerceIn(0, 255), blue.toInt().coerceIn(0, 255))
        val high = maxOf(r, g, b); val low = minOf(r, g, b)
        val light = .2126 * r + .7152 * g + .0722 * b
        return when {
            high - low < 18 -> {
                val gray = when { light < 110 -> 220 - light * .3; light >= 245 -> 110.0
                    light >= 220 -> 28.0; else -> 34 + (220 - light) * .15 }
                rgb(gray, gray + 2, gray + 5)
            }
            b > r + 18 && b >= g - 5 -> rgb(12 + (255 - r) * .08, 26 + (255 - r) * .16, 44 + (255 - r) * .28)
            g > r + 5 && g > b + 10 -> rgb(17 + light * .025, 27 + light * .055, 22 + light * .02)
            r > b + 35 && g > b + 20 && r >= g - 5 -> rgb(95 + (255 - light) * .2, 82 + (255 - light) * .15, 48.0)
            else -> rgb(25 + r * .12, 25 + g * .12, 28 + b * .12)
        }
    }
}

/** Layer-thread-only derived bitmaps. Raw cache, TTL and download jobs are never modified. */
internal class MapNightTiles(private val capacity: () -> Int) {
    private data class Entry(val generation: Int, val converted: Bitmap)
    private val entries = LinkedHashMap<Bitmap, Entry>(16, .75f, true)
    private var pixels = IntArray(0)
    internal val size get() = entries.size
    fun convert(source: Bitmap): Bitmap {
        val raw = AndroidGraphicFactory.getBitmap(source)
        entries[source]?.let { if (it.generation == raw.generationId) return it.converted }
        entries.remove(source)?.let { source.decrementRefCount(); it.converted.decrementRefCount() }
        val count = raw.width * raw.height
        if (pixels.size < count) pixels = IntArray(count)
        raw.getPixels(pixels, 0, raw.width, 0, 0, raw.width, raw.height)
        for (i in 0 until count) pixels[i] = MapNightPalette.color(pixels[i])
        val night = android.graphics.Bitmap.createBitmap(raw.width, raw.height, android.graphics.Bitmap.Config.ARGB_8888)
        night.setPixels(pixels, 0, raw.width, 0, 0, raw.width, raw.height)
        val converted = AndroidBitmap(night)
        // Hold the source so Mapsforge cannot recycle/reuse a cached tile underneath its derived colors.
        source.incrementRefCount()
        entries[source] = Entry(raw.generationId, converted)
        while (entries.size > capacity().coerceAtLeast(16)) {
            val iterator = entries.entries.iterator(); val old = iterator.next(); iterator.remove()
            old.key.decrementRefCount(); old.value.converted.decrementRefCount()
        }
        return converted
    }
    fun clear() {
        if (entries.isEmpty() && pixels.isEmpty()) return
        entries.forEach { (source, entry) -> source.decrementRefCount(); entry.converted.decrementRefCount() }
        entries.clear(); pixels = IntArray(0)
    }
}

/** A canvas adapter affects only this download layer; other map layers keep their original colors. */
private class NightCanvas(private val target: Canvas, private val tiles: MapNightTiles) : Canvas by target {
    override fun drawBitmap(bitmap: Bitmap, x: Int, y: Int) = target.drawBitmap(tiles.convert(bitmap), x, y)
    override fun drawBitmap(bitmap: Bitmap, x: Int, y: Int, alpha: Float) = target.drawBitmap(tiles.convert(bitmap), x, y, alpha)
    override fun drawBitmap(bitmap: Bitmap, matrix: Matrix) = target.drawBitmap(tiles.convert(bitmap), matrix)
    override fun drawBitmap(bitmap: Bitmap, matrix: Matrix, alpha: Float) = target.drawBitmap(tiles.convert(bitmap), matrix, alpha)
    override fun drawBitmap(bitmap: Bitmap, sl: Int, st: Int, sr: Int, sb: Int, dl: Int, dt: Int, dr: Int, db: Int) =
        target.drawBitmap(tiles.convert(bitmap), sl, st, sr, sb, dl, dt, dr, db)
    override fun drawBitmap(bitmap: Bitmap, sl: Int, st: Int, sr: Int, sb: Int, dl: Int, dt: Int, dr: Int, db: Int, alpha: Float) =
        target.drawBitmap(tiles.convert(bitmap), sl, st, sr, sb, dl, dt, dr, db, alpha)
}

internal class MapNightLayer(cache: TileCache, position: MapViewPosition, source: TileSource) :
    TileDownloadLayer(cache, position, source, AndroidGraphicFactory.INSTANCE) {
    @Volatile var night = false
    @Volatile private var active=false
    override fun onResume() { active=true;super.onResume() }
    override fun onPause() { active=false;super.onPause() }
    private val drawLock = Any()
    private val derived = MapNightTiles { cache.capacityFirstLevel }
    override fun draw(box: BoundingBox, zoom: Byte, canvas: Canvas, topLeft: Point, rotation: Rotation) = synchronized(drawLock) {
        if(!active) return@synchronized
        if (night) super.draw(box, zoom, NightCanvas(canvas, derived), topLeft, rotation)
        else { derived.clear(); super.draw(box, zoom, canvas, topLeft, rotation) }
    }
    override fun onDestroy() { synchronized(drawLock) { derived.clear(); super.onDestroy() } }
}

package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.Tile
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Provider wiring is independent of GL; actual GL tile compositing needs an Android device. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class MapTileRenderingTest {
    @Test fun vtmRetainsRasterProviderPathsHeadersAndSourceLimits() {
        val c=RuntimeEnvironment.getApplication();val tile=Tile(3,4,6)
        val key="0123456789abcdef0123456789abcdef"
        for(provider in MapSource.entries) {
            val source=VtmTileHttp.source(c,provider,key)
            val url=java.net.URL(source.getTileUrl(tile))
            assertEquals(provider.host,url.host)
            assertEquals(provider.path+"6/3/4.png",url.path)
            assertEquals(provider.maximumZoom,source.zoomLevelMax)
            assertTrue(source.requestHeader["User-Agent"]!!.startsWith("B-Cycle/"))
            assertEquals(if(provider==MapSource.OPENCYCLEMAP) "apikey=$key" else null,url.query)
        }
    }
}

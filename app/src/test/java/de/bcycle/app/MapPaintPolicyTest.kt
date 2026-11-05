package de.bcycle.app

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.theme.styles.TextStyle
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class MapPaintPolicyTest {
    @Test fun allMapDashesUseTwelveEightAndPlaceHalosAreDoubledOpaqueIncludingNativePaintsAndExport() {
        val map=BicycleMapView(RuntimeEnvironment.getApplication())
        val style=MapStyleOverrides().update(MapStyleTarget.RIVER,MapObjectStyle(dash=MapDash.DOTTED))
        try {
            val xml=VtmCycleTheme(true,style).renderThemeAsStream.use { it.readBytes() }
            val document=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.inputStream())
            val nodes=document.getElementsByTagName("line");var ordinary=0;var dots=0
            for(i in 0 until nodes.length) {
                val e=nodes.item(i) as org.w3c.dom.Element
                if(!e.hasAttribute("dasharray") || e.getAttribute("id").contains("hatching")) continue
                val p=e.getAttribute("dasharray")
                if(p==MapDash.DOTTED.pattern) dots++ else {
                    p.split(',').map(String::toDouble).forEachIndexed { index,value ->
                        if(index%2==1) assertEquals(8.0,value,.001) else assertTrue(value==12.0 || value in 0.0..1.0)
                    }
                    ordinary++
                }
            }
            assertTrue("Covers source boundaries, waterways and cycle highlights",ordinary>10);assertTrue(dots>0)
            val theme=VtmBrightTheme.load(false)
            try {
                for(kind in listOf("city","town","village","hamlet","country","continent")) {
                    val tags=TagSet().apply { add(Tag("layer","place"));add(Tag("class",kind));add(Tag("rank","1"));if(kind=="country") add(Tag("iso_a2","DE")) }
                    val labels=theme.matchElement(GeometryBuffer.GeometryType.POINT,tags,if(kind=="continent") 1 else 17).orEmpty().filterIsInstance<TextStyle>()
                    assertTrue(kind,labels.isNotEmpty())
                    val expected=(if(kind in listOf("country","continent")) 4f else 2.4f)*org.oscim.backend.CanvasAdapter.getScale()
                    for(text in labels) {
                        assertEquals(Color.WHITE,text.stroke.color);assertEquals(expected,text.stroke.strokeWidth,.001f)
                        assertTrue(org.oscim.renderer.bucket.BCycleCaptionLayout.padding(text)>=text.stroke.strokeWidth/2)
                    }
                }
            } finally { theme.dispose() }
            val json=style.json()
            assertEquals(12,json.getJSONArray("dashPattern").getInt(0));assertEquals(8,json.getJSONArray("dashPattern").getInt(1))
            assertEquals(2,json.getJSONObject("placeTextHalo").getInt("widthScale"));assertEquals(1,json.getJSONObject("placeTextHalo").getInt("opacity"))
            assertEquals(style,MapStyleOverrides.parse(json))
        } finally { map.disposeMap() }
    }
}

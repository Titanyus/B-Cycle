package de.bcycle.app

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.theme.styles.LineStyle
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class RoadSurfaceStyleTest {
    @Test fun explicitUnpavedRoadsAndTheirCyclingHighlightShareDashesWhilePavedAndUnknownStaySolid() {
        val map=BicycleMapView(RuntimeEnvironment.getApplication())
        val edits=MapStyleOverrides().update(MapStyleTarget.PATH,MapObjectStyle(Color.MAGENTA,MapDash.DOTTED,widthPercent=150))
            .update(MapStyleTarget.CYCLE,MapObjectStyle(Color.CYAN))
        val theme=VtmBrightTheme.load(true,style=edits) as VtmBrightTheme
        fun lines(kind:String,surface:String?,brunnel:String?=null,cycle:Boolean=false):List<LineStyle> {
            val tags=TagSet().apply {
                add(Tag("layer","transportation"));add(Tag("class",kind))
                surface?.let { add(Tag("surface",it)) };brunnel?.let { add(Tag("brunnel",it)) }
                if(cycle) add(Tag("subclass","cycleway"))
            }
            return theme.matchElement(GeometryBuffer.GeometryType.LINE,tags,17).orEmpty().filterIsInstance<LineStyle>()
        }
        try {
            for(kind in listOf("motorway","primary","secondary","minor","service","track","path")) {
                for(brunnel in listOf(null,"tunnel","bridge")) {
                    for(surface in listOf(null,"paved","asphalt","unknown","cobblestone")) {
                        val solid=lines(kind,surface,brunnel);assertTrue("Known style $kind/$brunnel",solid.isNotEmpty())
                        assertTrue("$kind/$brunnel/$surface stays solid",solid.all { it.dashArray==null && it.texture==null })
                    }
                    for(surface in listOf("unpaved","gravel","compacted","ground","grass","sand")) {
                        val dashed=lines(kind,surface,brunnel);assertTrue(dashed.isNotEmpty())
                        assertTrue("$kind/$brunnel/$surface dashed",dashed.all { it.dashArray!=null && it.texture!=null && !it.randomOffset })
                    }
                }
            }
            val unpaved=lines("path","unpaved",cycle=true)
            val path=unpaved.single { it.style=="highway-path" };val highlight=unpaved.single { it.style=="bcycle-cycleway" }
            assertEquals(Color.MAGENTA,path.current().color);assertEquals(Color.CYAN,highlight.current().color)
            assertNotNull("The coloured overlay itself has gaps",highlight.texture)
            assertArrayEquals("Matching base/overlay pattern keeps the gaps open",path.dashArray,highlight.dashArray,.001f)
            assertEquals(path.stipple,highlight.stipple)
            assertTrue(lines("path",null,cycle=true).all { it.texture==null })
            assertTrue(lines("path","paved",cycle=true).all { it.texture==null })
            val next=edits.update(MapStyleTarget.CYCLE,MapObjectStyle(Color.RED))
            theme.applyColors(next)
            assertEquals(Color.RED,highlight.current().color);assertEquals(Color.RED,highlight.current().stippleColor)
            assertSame(highlight.texture,highlight.current().texture)
            assertEquals(MapRoadSurface.policy,edits.json().getString("roadSurfaceStyle"))
            assertEquals(edits,MapStyleOverrides.parse(edits.json()))
        } finally { theme.dispose();map.disposeMap() }
    }
}

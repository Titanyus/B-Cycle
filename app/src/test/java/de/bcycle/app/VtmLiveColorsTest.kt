package de.bcycle.app

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.theme.styles.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class VtmLiveColorsTest {
    private fun tags(layer:String,kind:String)=TagSet().apply { add(Tag("layer",layer));add(Tag("class",kind));if(layer=="transportation") add(Tag("surface","unpaved")) }
    @Test fun existingNativeHandlesAndDashTexturesRepaintAndResetToExactPerZoomDefaults() {
        val view=BicycleMapView(RuntimeEnvironment.getApplication())
        val initial=MapStyleOverrides().update(MapStyleTarget.PRIMARY,MapObjectStyle(Color.RED,MapDash.DASHED))
            .update(MapStyleTarget.FOREST,MapObjectStyle(Color.BLUE)).update(MapStyleTarget.BUILDING,MapObjectStyle(Color.GREEN))
        val theme=VtmBrightTheme.load(false,style=initial) as VtmBrightTheme
        try {
            val road=theme.matchElement(GeometryBuffer.GeometryType.LINE,tags("transportation","primary"),16).orEmpty().filterIsInstance<LineStyle>().single { it.style=="highway-primary" }
            val forest=theme.matchElement(GeometryBuffer.GeometryType.POLY,tags("landcover","wood"),16).orEmpty().filterIsInstance<AreaStyle>().first()
            fun building(zoom:Int)=theme.matchElement(GeometryBuffer.GeometryType.POLY,tags("building",""),zoom).orEmpty().filterIsInstance<AreaStyle>().first()
            val low=building(16);val high=building(17);val texture=road.texture
            assertNotNull(texture);assertEquals(Color.RED,road.current().color);assertEquals(Color.RED,road.current().stippleColor)
            assertEquals(Color.argb(26,0,0,255),forest.current().color)
            assertNotEquals("Bright has different original building colours by zoom",low.color,high.color)
            val edited=initial.update(MapStyleTarget.PRIMARY,MapObjectStyle(Color.CYAN,MapDash.DASHED))
                .update(MapStyleTarget.FOREST,MapObjectStyle(Color.RED)).update(MapStyleTarget.BACKGROUND,MapObjectStyle(Color.BLACK))
            assertTrue(edited.canUpdateColorsFrom(initial));theme.applyColors(edited)
            assertSame(road,theme.matchElement(GeometryBuffer.GeometryType.LINE,tags("transportation","primary"),16).orEmpty().filterIsInstance<LineStyle>().single { it.style=="highway-primary" })
            assertSame(texture,road.current().texture);assertEquals(Color.CYAN,road.current().color);assertEquals(Color.CYAN,road.current().stippleColor)
            assertEquals(Color.argb(26,255,0,0),forest.current().color);assertEquals(Color.BLACK,theme.mapBackground)
            val translucent=edited.update(MapStyleTarget.PRIMARY,MapObjectStyle(Color.CYAN,MapDash.DASHED,alpha=128))
                .update(MapStyleTarget.FOREST,MapObjectStyle(Color.RED,alpha=128))
            theme.applyColors(translucent)
            assertEquals(Color.argb(128,0,255,255),road.current().color)
            assertEquals(Color.argb(128,0,255,255),road.current().stippleColor)
            assertEquals(Color.argb(128,255,0,0),forest.current().color)
            assertEquals(translucent,MapStyleOverrides.parse(translucent.json()))
            val xml=MapStyleXml.apply(RuntimeEnvironment.getApplication().assets.open("styles/bright-vtm.xml").bufferedReader().use { it.readText() },
                MapStyleOverrides().update(MapStyleTarget.PLACE_TEXT,MapObjectStyle(Color.rgb(17,92,203),alpha=128)))
            assertTrue(xml.contains("#80115CCB"))
            val track=theme.matchElement(GeometryBuffer.GeometryType.LINE,tags("transportation","track"),16).orEmpty().filterIsInstance<LineStyle>().single { it.style=="bcycle-track" }
            val recolored=translucent.update(MapStyleTarget.TRACK,MapObjectStyle(Color.MAGENTA))
            theme.applyColors(recolored)
            assertEquals(Color.MAGENTA,track.current().color);assertEquals(Color.MAGENTA,track.current().stippleColor)
            assertSame(track.texture,track.current().texture)
            val reset=MapStyleOverrides().update(MapStyleTarget.PRIMARY,MapObjectStyle(dash=MapDash.DASHED))
            theme.applyColors(reset)
            assertEquals(road.color,road.current().color);assertEquals(forest.color,forest.current().color)
            assertEquals(low.color,low.current().color);assertEquals(high.color,high.current().color);assertSame(texture,road.current().texture)
            assertFalse(reset.update(MapStyleTarget.PRIMARY,MapObjectStyle(dash=MapDash.SOLID)).canUpdateColorsFrom(reset))
            assertFalse(reset.update(MapStyleTarget.PLACE_TEXT,MapObjectStyle(Color.RED)).canUpdateColorsFrom(reset))
        } finally { theme.dispose();view.disposeMap() }
    }
    @Test fun bothRenderPassesAndContoursKeepTheirInstancesAndOnlyLatestQueuedColourIsApplied() {
        val c=RuntimeEnvironment.getApplication();val view=BicycleMapView(c);val queued=ArrayList<Runnable>()
        val options=MapVectorLayers(MapVectorLayers().visible+MapVectorLayer.CONTOURS)
        val base=VtmBaseMap(c,view.map(),MapSource.VECTOR,"",true,{ queued.add(it) },options)
        val layers=view.map().layers().toList()
        try {
            val red=MapStyleOverrides().update(MapStyleTarget.CONTOUR,MapObjectStyle(Color.RED))
                .update(MapStyleTarget.PATH,MapObjectStyle(Color.RED)).update(MapStyleTarget.GRASS,MapObjectStyle(Color.RED))
            val blue=red.update(MapStyleTarget.CONTOUR,MapObjectStyle(Color.BLUE)).update(MapStyleTarget.BACKGROUND,MapObjectStyle(Color.DKGRAY))
            assertTrue(base.updateColors(MapStyleOverrides(),red));assertTrue(base.updateColors(red,blue))
            assertEquals("Only one pending GL colour update",1,queued.size);queued.removeAt(0).run()
            assertEquals(layers,view.map().layers().toList())
            val contour=view.map().layers().filterIsInstance<VtmContourLayer>().single()
            val lines=contour.theme.matchElement(GeometryBuffer.GeometryType.LINE,TagSet().apply { add(Tag("contour","minor")) },13).orEmpty().filterIsInstance<LineStyle>()
            assertEquals(Color.argb(102,0,0,255),lines.single().current().color);assertEquals(0,contour.theme.mapBackground)
            val ground=view.map().layers().filterIsInstance<VtmStyledVectorLayer>().single { it.pass==VtmMapPass.GROUND }
            val foreground=view.map().layers().filterIsInstance<VtmStyledVectorLayer>().single { it.pass==VtmMapPass.FOREGROUND }
            assertEquals(Color.DKGRAY,ground.theme.mapBackground);assertEquals(0,foreground.theme.mapBackground)
            assertEquals(Color.RED,ground.theme.matchElement(GeometryBuffer.GeometryType.POLY,tags("landcover","grass"),16).orEmpty().filterIsInstance<AreaStyle>().first().current().color)
            assertEquals(Color.RED,foreground.theme.matchElement(GeometryBuffer.GeometryType.LINE,tags("transportation","path"),16).orEmpty().filterIsInstance<LineStyle>().first().current().color)
            assertFalse(base.updateColors(blue,blue.update(MapStyleTarget.PATH,MapObjectStyle(Color.RED,MapDash.DOTTED))))
            assertTrue(base.updateColors(blue,red));val pending=queued.removeAt(0);base.onDetach();pending.run()
            assertFalse(base.updateColors(red,blue))
        } finally { base.onDetach();view.disposeMap() }
    }
}

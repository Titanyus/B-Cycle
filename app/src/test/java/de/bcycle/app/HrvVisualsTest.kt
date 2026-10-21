package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import de.bcycle.hrv.Phase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvVisualsTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private fun render(view:View,width:Int=320,height:Int=320):Bitmap {
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY))
        view.layout(0,0,width,height)
        return Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    }
    @Test fun restAndMeasurementSwapRingAndEnlargedEndDotColors() {
        val themes=ThemeManager(c.getSharedPreferences("hrv-visual-theme",0));val tokens=themes.active.tokens
        val ring=HrvCircleView(c,themes)
        for((phase,line,dot) in listOf(Triple(Phase.REST,tokens.foreground,tokens.accent),Triple(Phase.MEASURE,tokens.accent,tokens.foreground))) {
            ring.show(HrvState(phase=phase,remainingMs=if(phase==Phase.REST) 15000 else 90000,bpm=73))
            val image=render(ring)
            assertEquals(line,image.getPixel(308,160))
            assertEquals(dot,image.getPixel(160,308))
            // Dot diameter 11.7 px versus 9 px ring width.
            val outer=image.getPixel(160,313)
            for(shift in listOf(0,8,16)) assertTrue(kotlin.math.abs(((dot ushr shift) and 255)-((outer ushr shift) and 255))<=2)
            assertTrue(android.graphics.Color.alpha(outer)>0)
            image.recycle()
        }
        assertEquals(18f,BicycleDesign.hrvTimeSp,0f)
    }
    @Test fun glowPeaksOnBeatFadesForOneAndAHalfSecondsAndResetsAtNextBeat() {
        assertEquals(.3f,HrvPulseAnimation.opacity(1000,1000,2000.0,true),.0001f)
        assertEquals(.2f,HrvPulseAnimation.opacity(1750,1000,2000.0,true),.0001f)
        assertEquals(.1f,HrvPulseAnimation.opacity(2500,1000,2000.0,true),.0001f)
        assertEquals(.3f,HrvPulseAnimation.opacity(3000,1000,2000.0,true),.0001f)
        assertEquals(.3f,HrvPulseAnimation.opacity(2800,2800,2000.0,true),.0001f)
        assertEquals(.1f,HrvPulseAnimation.opacity(9000,1000,2000.0,true),.0001f)
        assertEquals(.1f,HrvPulseAnimation.opacity(1000,1000,2000.0,false),.0001f)
    }
    @Test fun progressMovesBetweenStatusUpdatesAndBpmUpdatesDoNotRestartItsTimeline() {
        val themes=ThemeManager(c.getSharedPreferences("hrv-smooth-theme",0));val ring=HrvCircleView(c,themes)
        val rest=HrvState(phase=Phase.REST,remainingMs=30000,bpm=73)
        ring.show(rest)
        val initial=render(ring)
        assertNotEquals(themes.active.tokens.accent,initial.getPixel(168,12));initial.recycle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(125))
        val between=render(ring)
        assertEquals(themes.active.tokens.accent,between.getPixel(168,12));between.recycle()
        ring.show(rest.copy(bpm=74))
        val updated=render(ring)
        assertEquals(themes.active.tokens.accent,updated.getPixel(168,12));updated.recycle()
    }
    @Test fun graphBeginsAtZeroThenSlidesContinuouslyWithAbsoluteTenSecondTicks() {
        assertEquals(HrvGraphWindow(0,60000),HrvGraphWindow.at(0))
        assertEquals(HrvGraphWindow(0,60000),HrvGraphWindow.at(59999))
        val moving=HrvGraphWindow.at(90005)
        assertEquals(30005,moving.startMs);assertEquals(90005,moving.endMs)
        assertEquals(listOf(40000L,50000L,60000L,70000L,80000L,90000L),moving.ticks)
        assertEquals(HrvGraphWindow(120000,180000),HrvGraphWindow.at(180000))
        val themes=ThemeManager(c.getSharedPreferences("hrv-graph-theme",0));val graph=HrvGraphView(c,themes)
        fun accentCount(image:Bitmap):Int {
            val pixels=IntArray(image.width*image.height);image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
            return pixels.count { it==themes.active.tokens.accent }
        }
        graph.show((1..30).map { HrvPoint(it*1000L,800+it.toDouble()) },120000,false)
        val old=render(graph,height=220);assertEquals(0,accentCount(old));old.recycle()
        graph.show((90..120).map { HrvPoint(it*1000L,800+it.toDouble()) },120000,false)
        val recent=render(graph,height=220);assertTrue(accentCount(recent)>50);recent.recycle()
    }
}

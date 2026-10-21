package de.bcycle.app

import android.graphics.Path
import android.graphics.PointF
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonotoneGraphPathTest {
    @Test fun smoothCurvePassesEveryPointWithoutOvershootingEvenWithUnevenIntervalsAndSharpPeaks() {
        val points=listOf(PointF(0f,0f),PointF(1f,100f),PointF(10f,0f),PointF(11f,1f),PointF(60f,99f),PointF(90f,99f))
        val path=Path();MonotoneGraphPath.append(path,points)
        val samples=path.approximate(.001f).asList().chunked(3)
        points.forEach { p->assertTrue(samples.any { abs(it[1]-p.x)<.001 && abs(it[2]-p.y)<.001 }) }
        for(sample in samples) {
            val x=sample[1];val y=sample[2]
            val i=points.indexOfLast { it.x<=x }.coerceIn(0,points.size-2)
            assertTrue(y>=minOf(points[i].y,points[i+1].y)-.001f)
            assertTrue(y<=maxOf(points[i].y,points[i+1].y)+.001f)
        }
        // Interior is visibly curved rather than the old straight segment from (11,1) to (60,99).
        assertTrue(samples.any { it[1] in 20f..45f && abs(it[2]-(1+(it[1]-11)*2))>1f })
    }
    @Test fun dashboardCurveRendersThroughSamplesInFullAndAllRollingWindows() {
        val values=listOf(20.0,70.0,90.0,15.0,40.0,60.0)
        val points=values.mapIndexed { i,v->GraphPoint(i*24000L,i*24000L,i*200.0,List(5) { v }) }
        val chart=AnalysisChart.POWER;val series=mapOf(chart to GraphSeries(points,GraphRange(0.0,100.0)))
        val frame=GraphFrame(1,1000.0,series,120000,GraphWindow.entries.filter { it!=GraphWindow.FULL }.associateWith { series })
        val drawable=DashboardGraphDrawable(1f);drawable.setBounds(0,0,400,240)
        GraphWindow.entries.forEach { window ->
            drawable.update(frame,listOf(chart),intArrayOf(0,0,0,0),GraphDisplayMode.GRAPH_ONLY,window)
            val image=android.graphics.Bitmap.createBitmap(400,240,android.graphics.Bitmap.Config.ARGB_8888)
            drawable.draw(android.graphics.Canvas(image))
            val point=if(window.milliseconds==30000L) points[4] else points[3]
            val start=if(window==GraphWindow.FULL) 0L else (120000-window.milliseconds).coerceAtLeast(0)
            val duration=if(window==GraphWindow.FULL) 120000L else window.milliseconds
            val x=(26+374*(point.elapsedMs-start).toDouble()/duration).toInt()
            val y=(216-192*point.values[chart.ordinal]!!/100).toInt()
            val color=chart.color(android.graphics.Color.BLACK)
            assertTrue((x-2..x+2).any { px->(y-2..y+2).any { py->
                val pixel=image.getPixel(px,py)
                android.graphics.Color.alpha(pixel)>200 && listOf(0,8,16).all { shift->abs(((pixel ushr shift) and 255)-((color ushr shift) and 255))<=3 }
            } })
            if(window==GraphWindow.FULL) {
                val preview=android.graphics.Bitmap.createBitmap(400,240,android.graphics.Bitmap.Config.ARGB_8888)
                preview.eraseColor(0xff171725.toInt());android.graphics.Canvas(preview).drawBitmap(image,0f,0f,null)
                java.io.File("build/reports/ui/dashboard-smooth-curve.png").apply { parentFile?.mkdirs() }.outputStream().use { preview.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };preview.recycle()
            }
            image.recycle()
        }
    }
    @Test fun stationaryDistanceAndSeparateContoursKeepActualValuesAndGaps() {
        val path=Path()
        MonotoneGraphPath.append(path,listOf(PointF(0f,10f),PointF(10f,20f),PointF(10f,50f),PointF(20f,60f)))
        MonotoneGraphPath.append(path,listOf(PointF(40f,10f),PointF(50f,20f)))
        val samples=path.approximate(.01f).asList().chunked(3)
        assertTrue(samples.flatten().all { it.isFinite() })
        assertTrue(samples.any { it[1]==10f && it[2]==50f })
        assertFalse(samples.any { it[1]>20f && it[1]<40f })
    }
}

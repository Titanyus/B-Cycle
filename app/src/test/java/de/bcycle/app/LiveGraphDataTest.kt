package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class LiveGraphDataTest {
    @get:Rule val folder=TemporaryFolder()
    private fun point(t: Long, distance: Double?=t/100.0, speed: Double?=25.0, cadence: Double?=80.0,
        power: Double?=200.0, heart: Double?=130.0, height: Double?=500.0, breaks: Int=0) =
        GraphPoint(1_790_000_000_000+t,t,distance,listOf(speed,cadence,power,heart,height),breaks)
    @Test fun independentScalesGrowInFixedStepsWithHeadroomAndNeverShrink() {
        val s=GraphScaleManager();AnalysisChart.entries.forEach { s.include(it,if(it==AnalysisChart.ELEVATION) 500.0 else 100.0) }
        assertEquals(GraphRange(475.0,525.0),s.range(AnalysisChart.ELEVATION))
        s.include(AnalysisChart.ELEVATION,540.0);val h=s.range(AnalysisChart.ELEVATION)!!
        assertEquals(475.0,h.low,0.0);assertTrue(h.high>525);assertEquals(0.0,h.high%25,0.0)
        s.include(AnalysisChart.POWER,2000.0);val p=s.range(AnalysisChart.POWER)!!
        assertTrue(p.high-2000 >= (p.high-p.low)*.1);assertEquals(0.0,p.high%30,0.0)
        s.include(AnalysisChart.POWER,0.0);assertEquals(p,s.range(AnalysisChart.POWER))
        s.include(AnalysisChart.ELEVATION,500.0);assertEquals(h,s.range(AnalysisChart.ELEVATION))
        assertNotEquals(p,s.range(AnalysisChart.CADENCE))
    }
    @Test fun compactInitialRangesExpandByTheRequestedStepsWithSafetyMargin() {
        val s=GraphScaleManager()
        val starts=listOf(Triple(AnalysisChart.SPEED,10.0,GraphRange(0.0,20.0)),
            Triple(AnalysisChart.CADENCE,30.0,GraphRange(0.0,60.0)),
            Triple(AnalysisChart.POWER,60.0,GraphRange(0.0,120.0)),
            Triple(AnalysisChart.HEART,75.0,GraphRange(50.0,100.0)),
            Triple(AnalysisChart.ELEVATION,499.0,GraphRange(475.0,525.0)))
        starts.forEach { (chart,value,range) -> s.include(chart,value);assertEquals(chart.name,range,s.range(chart)) }
        val nearLimits=listOf(Triple(AnalysisChart.SPEED,18.0,25.0),Triple(AnalysisChart.CADENCE,54.0,70.0),
            Triple(AnalysisChart.POWER,108.0,150.0),Triple(AnalysisChart.HEART,95.0,120.0),Triple(AnalysisChart.ELEVATION,520.0,550.0))
        nearLimits.forEach { (chart,value,high) ->
            s.include(chart,value);val range=s.range(chart)!!
            assertEquals(chart.name,high,range.high,0.0)
            assertTrue(chart.name,range.high-value >= (range.high-range.low)*.1)
        }
    }
    @Test fun missingAndRegressingDistanceBreakContinuityWithoutInventingProgress() {
        val h=GraphHistoryBuffer();h.append(point(0,0.0));h.append(point(1000,10.0));h.append(point(2000,null))
        h.append(point(3000,5.0));h.append(point(4000,20.0))
        val f=h.frame(1,200);assertEquals(20.0,f.distanceM,0.0)
        f.series.values.forEach { assertEquals(31,it.points.last().breaks) }
        h.append(point(10000,30.0));assertEquals(31,h.frame(2,200).series.getValue(AnalysisChart.SPEED).points.last().breaks)
    }
    @Test fun identicalDistanceKeepsLastActualSampleAndTimestamp() {
        val h=GraphHistoryBuffer();repeat(10) { h.append(point(it*1000L,0.0,power=it.toDouble())) }
        val f=h.frame(1,100);assertEquals(0.0,f.distanceM,0.0)
        val p=f.series.getValue(AnalysisChart.POWER).points.single()
        assertEquals(9000,p.elapsedMs);assertEquals(9.0,p.values[2]!!,0.0)
    }
    @Test fun gentleSmoothingIsConvexPreservesZerosAndDoesNotBridgeGaps() {
        val h=GraphHistoryBuffer();val powers=listOf(100.0,110.0,100.0,0.0,0.0,null,300.0,310.0,300.0)
        powers.forEachIndexed { i,p -> h.append(point(i*1000L,i.toDouble(),power=p)) }
        val f=h.frame(1,64);val shown=f.series.getValue(AnalysisChart.POWER).points
        // Radius is narrow at this zoom: do not smooth across a broad distance interval.
        assertTrue(shown.filter { it.values[2]!=null }.all { it.values[2]!! in 0.0..310.0 })
        assertEquals(2,shown.count { it.values[2]==0.0 });assertTrue(shown.any { it.values[2]==null })
        assertTrue(f.series.getValue(AnalysisChart.POWER).range.high>310)
        assertTrue(f.series.keys.all { f.distanceM==8.0 })
        val smooth=GraphHistoryBuffer();repeat(101) { i -> smooth.append(point(i*1000L,i.toDouble(),power=if(i==50) 210.0 else 200.0)) }
        val peak=smooth.frame(2,64).series.getValue(AnalysisChart.POWER).points.first { it.distanceM==50.0 }.values[2]!!
        assertTrue(peak in 200.0..210.0);assertTrue(peak<210.0);assertTrue(peak>=207.0)
    }
    @Test fun severalHoursStayBoundedRetainExtremaZerosAndMissingSections() {
        val h=GraphHistoryBuffer()
        repeat(18_000) { i -> h.append(point(i*1000L,i*10.0,
            power=when { i in 2000..2100 -> null; i in 4000..4120 -> 0.0; i==1000 -> 999.0; else -> 200.0 },
            heart=if(i in 8000..8100) null else 130.0))
            assertTrue(h.size<=LiveGraphStyle.MAX_POINTS)
        }
        val f=h.frame(1,480);val p=f.series.getValue(AnalysisChart.POWER).points
        assertEquals(179990.0,f.distanceM,0.0);assertTrue(p.any { it.values[2]==0.0 });assertTrue(p.any { it.values[2]==null || it.breaks and 4 !=0 })
        assertTrue(f.series.getValue(AnalysisChart.POWER).range.high>999)
        assertTrue(p.any { it.values[2]!=null && it.values[2]!!>700 })
        assertTrue(p.size<4000)
    }
    @Test fun sampleAdapterUsesFrozenSourcesRealZerosAndCentralContinuationProofOnly() {
        val raw=RideSample(1000,1000,values=mapOf("SPEED" to 10.0,"GPS_SPEED" to 8.0,"BIKE_DISTANCE" to 100.0,"GPS_DISTANCE" to 80.0))
        val before=raw.copy(values=raw.values.toMap())
        val bike=graphPoint(raw,RecordingConfig(),true,true)
        assertEquals(listOf(36.0,0.0,0.0,null,null),bike.values);assertEquals(100.0,bike.distanceM!!,0.0)
        val gps=graphPoint(raw,RecordingConfig(speedSource="GPS_SPEED",distanceSource="GPS_DISTANCE"),false,true)
        assertEquals(28.8,gps.values[0]!!,1e-9);assertNull(gps.values[1]);assertNull(gps.values[2]);assertEquals(80.0,gps.distanceM!!,0.0)
        assertEquals(before,raw)
        val actualZero=raw.copy(values=raw.values+mapOf("RIDER_POWER" to 0.0,"CADENCE" to 0.0))
        assertEquals(0.0,graphPoint(actualZero,RecordingConfig(),false,false).values[2]!!,0.0)
    }
    @Test fun centralBoschCoastingProofExpiresAtDisconnectAndDoesNotChangePerformance() {
        val e=PerformanceEngine();e.accept(PerformanceEvent(PerformanceChannel.POWER,0,0.0,source=PowerSource.BOSCH_RIDER.name))
        repeat(20) { i -> e.accept(PerformanceEvent(PerformanceChannel.BIKE_ACTIVITY,i*1000L,8.0,source="SPEED"));e.advance(i*1000L) }
        val before=e.snapshot();assertTrue(before.quality.continuedZeroMs>0)
        val sample=RideSample(1000,19000,values=mapOf("SPEED" to 8.0,"BIKE_DISTANCE" to 100.0))
        val h=GraphHistoryBuffer();h.append(graphPoint(sample,RecordingConfig(),true,true));h.frame(1,300)
        assertEquals(before,e.snapshot())
        e.accept(PerformanceEvent(PerformanceChannel.BIKE_ACTIVITY,20000,connected=false));e.advance(22000)
        val after=e.snapshot().quality.continuedZeroMs;e.advance(24000)
        assertEquals(after,e.snapshot().quality.continuedZeroMs)
        assertNull(graphPoint(sample.copy(elapsedMs=24000),RecordingConfig(),false,true).values[2])
    }
    @Test fun graphPreparationLeavesJournalAndFitExportByteIdentical() {
        val journal=folder.newFile("source.ride");val first=folder.newFile("a.fit");val second=folder.newFile("b.fit")
        val config=RecordingConfig();val samples=(0..40).map { i -> RideSample(1_790_000_000_000+i*1000L,i*1000L,
            values=mapOf("SPEED" to 8.0,"BIKE_DISTANCE" to i*8.0,"RIDER_POWER" to if(i in 10..20) 0.0 else 180.0,"CADENCE" to 70.0)) }
        RideJournal(journal,samples.first().utcMs,config).use { j -> samples.forEach(j::append) }
        val original=journal.readBytes();FitWriter.encode(journal,first)
        val h=GraphHistoryBuffer();samples.forEach { h.append(graphPoint(it,config,false,false)) };h.frame(1,320)
        FitWriter.encode(journal,second);assertArrayEquals(original,journal.readBytes());assertArrayEquals(first.readBytes(),second.readBytes())
    }
}

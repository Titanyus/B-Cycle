package de.bcycle.app

import kotlin.math.ceil
import kotlin.math.floor

internal data class DistributionBand(val zone: Int, val lower: Double, val upper: Double, val color: Int)
internal data class DistributionBar(val lower:Double,val upper:Double,val milliseconds:Double)
internal data class RouteValueDistribution(val chart: AnalysisChart, val minimum: Double, val maximum: Double,
    val milliseconds: List<Double>, val totalMs: Double, val bands: List<DistributionBand>, val observedMaximum: Double,
    val bars:List<DistributionBar>, val zoneMilliseconds:List<Double>)

/** Integer-unit histogram before chart downsampling. FIT power/BPM are integer fields. */
internal class RouteTimeHistogram(private val maximumValue: Int) {
    private val milliseconds = DoubleArray(maximumValue + 1)
    private var observedMin: Double? = null
    private var observedMax: Double? = null
    fun observe(value: Double?) {
        if (value == null || !value.isFinite() || value !in 0.0..maximumValue.toDouble()) return
        observedMin = minOf(observedMin ?: value, value); observedMax = maxOf(observedMax ?: value, value)
    }
    fun add(value: Double?, durationMs: Long) {
        if (value == null || !value.isFinite() || value !in 0.0..maximumValue.toDouble() || durationMs <= 0) return
        milliseconds[value.toInt()] += durationMs.toDouble()
    }
    fun result(chart: AnalysisChart, profile: RiderProfile): RouteValueDistribution? {
        val total = milliseconds.sum().takeIf { it > 0 } ?: return null
        val observed = observedMax ?: return null
        val low: Double
        val high: Double
        val bands: List<DistributionBand>
        if (chart == AnalysisChart.POWER) {
            val ftp = profile.ftpW?.takeIf { Coggan.zones(it) != null } ?: return null
            low = 0.0
            // Show all seven zones, with usable room for open-ended Z7, and every observed peak.
            high = ceil(maxOf(observed, ftp * 1.75) / 5) * 5
            val edges = listOf(0.0) + Coggan.boundaries.map { ftp * it / 100.0 } + high
            bands = List(7) { i -> DistributionBand(i + 1, edges[i], edges[i + 1], ZoneGlowColors.powerColors[i]) }
        } else {
            require(chart == AnalysisChart.HEART)
            val model = profile.heartZones() ?: return null
            low = floor((if(model.openLastZone) minOf(observedMin!!,model.zones[1].lowerBpm-10.0) else minOf(observedMin!!,model.zones.first().lowerBpm.toDouble()))/5)*5
            high = ceil(maxOf(observed, if(model.openLastZone) model.zones.last().lowerBpm+10.0 else model.maximumHeartRate.toDouble()) / 5) * 5
            bands = model.zones.filterNot { it.isEmpty }.map { zone -> DistributionBand(zone.index, if(model.openLastZone && zone.index==1) low else zone.lowerBpm.toDouble(),
                if (zone.index == 5) { if(model.openLastZone) high else model.maximumHeartRate.toDouble() } else zone.upperExclusiveBpm.toDouble(), TrainingZoneColors.values[zone.index - 1]) }
        }
        // Equal numeric intervals preserve a linear axis and identical bar widths. Very large
        // peaks widen the fixed step by whole base units, keeping worker/view storage bounded.
        val base=if(chart==AnalysisChart.POWER) BicycleDesign.analysisDistributionPowerStep else BicycleDesign.analysisDistributionHeartStep
        var step=ceil((high-low)/(BicycleDesign.analysisDistributionMaxBars*base)).coerceAtLeast(1.0)*base
        var axisLow=floor(low/step)*step;var axisHigh=ceil(high/step)*step
        while(kotlin.math.round((axisHigh-axisLow)/step)>BicycleDesign.analysisDistributionMaxBars) {
            step+=base;axisLow=floor(low/step)*step;axisHigh=ceil(high/step)*step
        }
        val zoneTime=DoubleArray(bands.size)
        val heartModel=if(chart==AnalysisChart.HEART) profile.heartZones() else null
        val bins=DoubleArray(kotlin.math.round((axisHigh-axisLow)/step).toInt().coerceAtLeast(1))
        milliseconds.forEachIndexed { value,ms -> if(ms>0) {
            // Derive shares from integer-unit samples, not colour-split display bins.
            val zone=if(chart==AnalysisChart.POWER) Coggan.zone(value.toDouble(),profile.ftpW) else heartModel?.zoneFor(value)?.index
            val band=bands.indexOfFirst { it.zone==zone }
            if(band>=0) zoneTime[band]+=ms
            // Match the profile's inclusive power upper limits and final HR maximum.
            // Otherwise a sample exactly on a zone edge could be coloured as the next zone.
            val rightClosed=chart==AnalysisChart.POWER || value.toDouble()==bands.last().upper
            val position=(value-axisLow)/step
            val index=(if(rightClosed) ceil(position).toInt()-1 else floor(position).toInt()).coerceIn(bins.indices)
            bins[index]+=ms
        } }
        val openHeart=chart==AnalysisChart.HEART && profile.heartZones()?.openLastZone==true
        val displayBands=bands.map { band -> when {
            chart==AnalysisChart.POWER && band.zone==7 -> band.copy(upper=axisHigh)
            openHeart && band.zone==1 -> band.copy(lower=axisLow)
            openHeart && band.zone==5 -> band.copy(upper=axisHigh)
            else->band
        } }
        val bars=bins.indices.map { index -> DistributionBar(axisLow+step*index,axisLow+step*(index+1),bins[index]) }
        return RouteValueDistribution(chart,axisLow,axisHigh,bins.toList(),total,displayBands,observed,bars,zoneTime.toList())
    }
}

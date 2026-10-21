package de.bcycle.app

import com.garmin.fit.*
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Real timestamped GPX measurements become a FIT activity, with explicit inferred
 * pause events and derived summaries. No synthetic samples or invented timestamps.
 */
internal object GpxActivityFit {
    const val VERSION="gpx-activity-v1-stationary-pauses"
    fun write(source:File,destination:File,entry:StoredGpx,type:BikeType?) {
        val details=entry.details.copy(bikeType=type)
        val analysis=RouteAnalysisRead.read(source,ManagedRoute(entry.id,entry.name,false,"GPX",details,true,entry.origin))
        val start=analysis.entry.details.dateMs
        val elapsed=analysis.metrics[AnalysisMetric.ELAPSED] ?: error("GPX ohne Messzeit")
        require(start>=631065600000L && elapsed>0)
        val end=start+(elapsed*1000).roundToLong()
        val timer=(elapsed-analysis.inferredPauseMs/1000.0).coerceAtLeast(0.0).toFloat()
        fun date(ms:Long)=DateTime(java.util.Date(ms))
        val encoder=FileEncoder(destination,Fit.ProtocolVersion.V2_0)
        fun event(at:Long,kind:EventType) { encoder.write(EventMesg().apply { setTimestamp(date(at));setEvent(Event.TIMER);setEventType(kind) }) }
        try {
            encoder.write(FileIdMesg().apply { setType(com.garmin.fit.File.ACTIVITY);setManufacturer(255);setProduct(0);setTimeCreated(date(start)) })
            encoder.write(SportMesg().apply { setSport(Sport.CYCLING);setSubSport(FitBikeType.subSport(type)) })
            event(start,EventType.START)
            val pauses=GpxPauseInference();var previous=start
            source.inputStream().use { input -> GpxImport.samples(input) { sample ->
                val at=sample.time ?: error("GPX-Punkt ohne Zeitstempel")
                require(at in previous..end);previous=at
                pauses.add(sample)?.let { gap -> event(gap.fromMs,EventType.STOP_ALL);event(gap.toMs,EventType.START) }
                encoder.write(RecordMesg().apply {
                    setTimestamp(date(at))
                    setPositionLat((sample.point.latitude/180*2147483648.0).roundToInt())
                    setPositionLong((sample.point.longitude/180*2147483648.0).roundToInt())
                    sample.height?.let { setEnhancedAltitude(it.toFloat()) };sample.speed?.let { setEnhancedSpeed(it.toFloat()) }
                    sample.power?.let { setPower(it.roundToInt()) };sample.heart?.takeIf { it in 1.0..254.0 }?.let { setHeartRate(it.roundToInt().toShort()) }
                    sample.cadence?.takeIf { it in 0.0..254.0 }?.let { setCadence(it.roundToInt().toShort()) }
                    sample.temperature?.let { setTemperature(it.roundToInt().toByte()) }
                })
            } }
            event(end,EventType.STOP_ALL)
            encoder.write(SessionMesg().apply {
                setTimestamp(date(end));setStartTime(date(start));setSport(Sport.CYCLING);setSubSport(FitBikeType.subSport(type))
                setTotalElapsedTime(elapsed.toFloat());setTotalTimerTime(timer);setFirstLapIndex(0);setNumLaps(0)
                analysis.metrics[AnalysisMetric.DISTANCE]?.let { setTotalDistance((it*1000).toFloat()) }
                analysis.metrics[AnalysisMetric.MOVING]?.let { setTotalMovingTime(it.toFloat()) }
                analysis.metrics[AnalysisMetric.NP]?.let { setNormalizedPower(it.roundToInt()) }
                analysis.metrics[AnalysisMetric.AVG_POWER]?.let { setAvgPower(it.roundToInt()) }
                analysis.metrics[AnalysisMetric.MAX_POWER]?.let { setMaxPower(it.roundToInt()) }
                analysis.metrics[AnalysisMetric.AVG_HEART]?.let { setAvgHeartRate(it.roundToInt().toShort()) }
                analysis.metrics[AnalysisMetric.MAX_HEART]?.let { setMaxHeartRate(it.roundToInt().toShort()) }
                analysis.metrics[AnalysisMetric.AVG_CADENCE]?.let { setAvgCadence(it.roundToInt().toShort()) }
                analysis.metrics[AnalysisMetric.ASCENT]?.let { setTotalAscent(it.roundToInt()) }
                analysis.metrics[AnalysisMetric.DESCENT]?.let { setTotalDescent(it.roundToInt()) }
                analysis.metrics[AnalysisMetric.WORK]?.let { setTotalWork((it*1000).roundToLong()) }
            })
            encoder.write(ActivityMesg().apply { setTimestamp(date(end));setTotalTimerTime(timer);setNumSessions(1);setType(Activity.MANUAL) })
        } finally { encoder.close() }
        FitRead.read(destination)
    }
}

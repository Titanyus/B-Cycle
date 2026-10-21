package de.bcycle.app

/** Reuses existing motion and weighted-mean estimators; updated by the recording tick only. */
internal class IndoorRide {
    private var motion=MovingTime()
    private var means=RideAnalysisMeans()
    private var maximumSpeed:Double?=null
    private var maximumPower:Double?=null
    private var maximumCadence:Double?=null
    val averagePower get()=means.riderMean
    fun reset() { motion=MovingTime();means=RideAnalysisMeans();maximumSpeed=null;maximumPower=null;maximumCadence=null }
    fun add(at:Long,speed:Double?,power:Double?,cadence:Double?) {
        motion.add(at,speed?.div(3.6));means.add(at,power,null,null)
        speed?.let { maximumSpeed=maxOf(maximumSpeed ?: it,it) }
        power?.let { maximumPower=maxOf(maximumPower ?: it,it) }
        cadence?.let { maximumCadence=maxOf(maximumCadence ?: it,it) }
    }
    fun times(elapsed:Long,distance:Long?)=GpsValues(elapsedMs=elapsed,movingMs=motion.value,
        stoppedMs=motion.value?.let { (motion.observedMs-it).coerceAtLeast(0) },
        movingAverageKmh=motion.value?.takeIf { it>0 }?.let { ms-> distance?.times(3600.0)?.div(ms) },
        overallAverageKmh=elapsed.takeIf { it>0 }?.let { distance?.times(3600.0)?.div(it) },maximumKmh=maximumSpeed)
    fun summaries()=buildMap {
        averagePower?.let { put(LiveMetric.AVERAGE_POWER,kotlin.math.round(it).toLong()) }
        maximumPower?.let { put(LiveMetric.MAXIMUM_POWER,it.toLong()) }
        maximumCadence?.let { put(LiveMetric.MAXIMUM_CADENCE,kotlin.math.round(it*2).toLong()) }
    }
}

/** Shared stable field IDs; GPS-prefixed ride-timer IDs are retained for compatibility only. */
internal object IndoorFields {
    val rideIds=setOf("GPS_ELAPSED_TIME","GPS_MOVING_TIME","GPS_STOPPED_TIME","GPS_MOVING_AVERAGE","GPS_OVERALL_AVERAGE","GPS_MAXIMUM_SPEED")
    val trainerIds=setOf("SPEED","RIDER_POWER","CADENCE","BIKE_DISTANCE","AVERAGE_SPEED","AVERAGE_POWER","MAXIMUM_POWER","AVERAGE_CADENCE","MAXIMUM_CADENCE")+rideIds
    val allowed get()=trainerIds+setOf("HEART_RATE","HEART_AVERAGE","HEART_MAXIMUM","HEART_BATTERY")+RecordingFields.summaries.map { it.id }
    fun config(config:RecordingConfig)=config.copy(fields=config.fields.filter { it in allowed }.toSet(),speedSource="SPEED",distanceSource="BIKE_DISTANCE",indoor=true)
}

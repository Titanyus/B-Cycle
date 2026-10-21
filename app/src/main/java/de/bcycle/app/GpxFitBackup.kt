package de.bcycle.app

import com.garmin.fit.*
import java.io.File
import kotlin.math.roundToInt

/** Interoperable FIT copy; the untouched GPX is also retained for a lossless restore. */
internal object GpxFitBackup {
    fun write(source:File,destination:File,entry:StoredGpx) {
        if(entry.origin==TrackOrigin.RECORDED && (entry.details.durationMs ?: 0)>0) {
            try { GpxActivityFit.write(source,destination,entry,entry.details.bikeType);return }
            catch(_:IllegalArgumentException) { destination.delete() }
            catch(_:IllegalStateException) { destination.delete() }
            // Incomplete timing can still be preserved as a course plus original GPX/JSON.
        }
        val encoder=FileEncoder(destination,Fit.ProtocolVersion.V2_0)
        try {
            encoder.write(FileIdMesg().apply {
                setType(com.garmin.fit.File.COURSE);setManufacturer(255);setProduct(0)
                if(entry.details.dateMs>=631065600000L) setTimeCreated(DateTime(java.util.Date(entry.details.dateMs)))
            })
            encoder.write(CourseMesg().apply { setName(entry.name.take(80));setSport(Sport.CYCLING) })
            entry.details.bikeType?.let { type -> encoder.write(SportMesg().apply { setSport(Sport.CYCLING);setSubSport(FitBikeType.subSport(type)) }) }
            source.inputStream().use { input -> GpxImport.samples(input) { s ->
                encoder.write(RecordMesg().apply {
                    setPositionLat((s.point.latitude/180.0*2147483648.0).roundToInt())
                    setPositionLong((s.point.longitude/180.0*2147483648.0).roundToInt())
                    s.time?.takeIf { it>=631065600000L }?.let { setTimestamp(DateTime(java.util.Date(it))) }
                    s.height?.let { setEnhancedAltitude(it.toFloat()) }
                    s.speed?.let { setEnhancedSpeed(it.toFloat()) }
                    s.power?.let { setPower(it.roundToInt()) };s.heart?.let { setHeartRate(it.roundToInt().toShort()) }
                    s.cadence?.let { setCadence(it.roundToInt().toShort()) };s.temperature?.let { setTemperature(it.roundToInt().toByte()) }
                })
            } }
        } finally { encoder.close() }
        FitRead.read(destination)
    }
}

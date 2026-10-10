package de.bcycle.app

import java.io.*

/** Disk-backed, fixed-size samples; memory does not grow with ride duration. Worker thread only. */
class RideJournal(val file: File, val startUtcMs: Long, val config: RecordingConfig) : Closeable {
    private val fields = RecordField.options.filter { it.id in config.fields && it.id != "GPS_POSITION" }.map { it.id } +
        if ("GPS_ALTITUDE" in config.fields) listOf("GPS_ALTITUDE_MSL") else emptyList()
    private val stream = FileOutputStream(file)
    private val output = DataOutputStream(BufferedOutputStream(stream))
    private var count = 0
    init {
        output.writeInt(0x42435934); output.writeLong(startUtcMs)
        output.writeUTF(config.speedSource); output.writeUTF(config.distanceSource)
        output.writeBoolean("GPS_POSITION" in config.fields)
        output.writeInt(fields.size); fields.forEach { output.writeUTF(it) }
        output.flush(); stream.fd.sync()
    }
    fun append(sample: RideSample) {
        output.writeLong(sample.utcMs); output.writeLong(sample.elapsedMs)
        output.writeDouble(if ("GPS_POSITION" in config.fields) sample.latitude ?: Double.NaN else Double.NaN)
        output.writeDouble(if ("GPS_POSITION" in config.fields) sample.longitude ?: Double.NaN else Double.NaN)
        fields.forEach { output.writeDouble(sample.values[it] ?: Double.NaN) }
        output.flush()
        if (++count % 5 == 0) stream.fd.sync()
    }
    override fun close() { output.flush(); stream.fd.sync(); output.close() }

    companion object {
        fun read(file: File, consume: (Long, RecordingConfig, List<String>, Sequence<RideSample>) -> Unit) {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                require(input.readInt() == 0x42435934) { "Unknown ride journal" }
                val start = input.readLong(); val speed = input.readUTF(); val distance = input.readUTF()
                val positions = input.readBoolean(); val size = input.readInt()
                require(size in 0..100)
                val fields = List(size) { input.readUTF() }
                val config = RecordingConfig(fields.toSet() + if (positions) setOf("GPS_POSITION") else emptySet(), speed, distance)
                val records = sequence {
                    while (true) {
                        val record = try {
                            val utc = input.readLong(); val elapsed = input.readLong()
                            val lat = input.readDouble(); val lon = input.readDouble()
                            val values = buildMap { fields.forEach { id -> val v = input.readDouble(); if (v.isFinite()) put(id, v) } }
                            RideSample(utc, elapsed, lat.takeIf { it.isFinite() }, lon.takeIf { it.isFinite() }, values)
                        } catch (_: EOFException) { break } // Recover complete records after interruption.
                        yield(record)
                    }
                }
                consume(start, config, fields, records)
            }
        }
    }
}

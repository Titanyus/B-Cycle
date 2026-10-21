package de.bcycle.app

import java.io.*

/** Disk-backed, bounded sensor-event batches in the existing samples; memory does not grow with ride duration. Worker thread only. */
class RideJournal(val file: File, val startUtcMs: Long, val config: RecordingConfig) : Closeable {
    private val fields = RecordingFields.options.filter { it.id != "GPS_POSITION" && it.scope == FitFieldScope.RECORD }.map { it.id } +
        listOf("GPS_ALTITUDE_MSL")
    private val stream = FileOutputStream(file)
    private val output = DataOutputStream(BufferedOutputStream(stream))
    private var count = 0
    init {
        output.writeInt(0x42435937); output.writeLong(startUtcMs)
        output.writeUTF(config.speedSource); output.writeUTF(config.distanceSource);output.writeBoolean(config.indoor)
        output.writeBoolean(!config.indoor)
        output.writeInt(fields.size); fields.forEach { output.writeUTF(it) }
        output.writeInt(PerformanceEngine.MODEL_VERSION)
        output.writeInt(config.fields.size); config.fields.sorted().forEach { output.writeUTF(it) }
        output.flush(); stream.fd.sync()
    }
    fun append(sample: RideSample) {
        output.writeLong(sample.utcMs); output.writeLong(sample.elapsedMs)
        output.writeDouble(if (!config.indoor) sample.latitude ?: Double.NaN else Double.NaN)
        output.writeDouble(if (!config.indoor) sample.longitude ?: Double.NaN else Double.NaN)
        fields.forEach { output.writeDouble(sample.values[it] ?: Double.NaN) }
        require(sample.performanceEvents.size <= 256)
        output.writeInt(sample.performanceEvents.size)
        sample.performanceEvents.forEach { event ->
            require(event.source.length <= 128)
            output.writeByte(event.channel.ordinal); output.writeLong(event.atMs); output.writeLong(event.receivedAtMs)
            output.writeLong(event.sensorTimeMs ?: Long.MIN_VALUE); output.writeDouble(event.value ?: Double.NaN)
            output.writeBoolean(event.connected); output.writeUTF(event.source)
        }
        output.flush()
        if (++count % 5 == 0) stream.fd.sync()
    }
    override fun close() { output.flush(); stream.fd.sync(); output.close() }

    companion object {
        fun read(file: File, consume: (Long, RecordingConfig, List<String>, Sequence<RideSample>) -> Unit) {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                val version = input.readInt()
                require(version in listOf(0x42435934, 0x42435935, 0x42435936, 0x42435937)) { "Unknown ride journal" }
                val start = input.readLong(); val speed = input.readUTF(); val distance = input.readUTF()
                val indoor=if(version>=0x42435936) input.readBoolean() else false
                val positions = input.readBoolean(); val size = input.readInt()
                require(size in 0..100)
                val fields = List(size) { input.readUTF() }
                val selected = if (version >= 0x42435935) {
                    require(input.readInt() in 1..PerformanceEngine.MODEL_VERSION) { "Unknown performance model" }
                    val selectedCount = input.readInt(); require(selectedCount in 0..100)
                    List(selectedCount) { input.readUTF() }.toSet()
                } else fields.toSet() + if (positions) setOf("GPS_POSITION") else emptySet()
                val config = RecordingConfig(selected, speed, distance,indoor)
                val records = sequence {
                    while (true) {
                        val record = try {
                            val utc = input.readLong(); val elapsed = input.readLong()
                            val lat = input.readDouble(); val lon = input.readDouble()
                            val values = buildMap { fields.forEach { id -> val v = input.readDouble(); if (v.isFinite()) put(id, v) } }
                            val events = if (version >= 0x42435935) {
                                val eventCount = input.readInt(); require(eventCount in 0..256)
                                List(eventCount) {
                                    val kind = input.readUnsignedByte(); require(kind in PerformanceChannel.entries.indices)
                                    val at = input.readLong(); val received = input.readLong(); val sensor = input.readLong()
                                    val value = input.readDouble(); val connected = input.readBoolean(); val source = input.readUTF()
                                    require(source.length <= 128 && at in 0..elapsed)
                                    PerformanceEvent(PerformanceChannel.entries[kind], at, value.takeIf { it.isFinite() }, received,
                                        sensor.takeIf { it != Long.MIN_VALUE }, source, connected)
                                }
                            } else emptyList()
                            RideSample(utc, elapsed, lat.takeIf { it.isFinite() }, lon.takeIf { it.isFinite() }, values, events)
                        } catch (_: EOFException) { break } // Recover complete records after interruption.
                        yield(record)
                    }
                }
                consume(start, config, fields, records)
            }
        }
    }
}

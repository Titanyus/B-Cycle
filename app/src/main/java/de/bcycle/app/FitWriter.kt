package de.bcycle.app

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.TimeZone
import java.util.UUID
import kotlin.math.*

/** Small independent FIT 2.0 encoder. Numbers/scales follow Garmin's official 21.218 profile. */
object FitWriter {
    private const val EPOCH_MS = 631065600000L // 1989-12-31 00:00:00 UTC.
    private fun timestamp(ms: Long) = ((ms - EPOCH_MS) / 1000).also { require(it in 0..0xfffffffeL) }
    private data class Field(val number: Int, val type: Int, val bytes: ByteArray)
    private fun number(id: Int, type: Int, size: Int, value: Long) = Field(id, type, ByteArray(size) { (value ushr (it * 8)).toByte() })
    private fun u8(id: Int, v: Double?, scale: Double = 1.0) = number(id, 2, 1, scaled(v, scale, 0.0, 254, 255))
    private fun en(id: Int, v: Int) = number(id, 0, 1, v.toLong())
    private fun u16(id: Int, v: Double?, scale: Double = 1.0, offset: Double = 0.0) = number(id, 0x84, 2, scaled(v, scale, offset, 65534, 65535))
    private fun u32(id: Int, v: Double?, scale: Double = 1.0, offset: Double = 0.0) = number(id, 0x86, 4, scaled(v, scale, offset, 0xfffffffeL, 0xffffffffL))
    private fun scaled(v: Double?, scale: Double, offset: Double, maximum: Long, invalid: Long): Long {
        if (v == null || !v.isFinite()) return invalid
        val raw = (v + offset) * scale
        return if (raw in 0.0..maximum.toDouble()) raw.roundToLong() else invalid
    }
    private fun coord(id: Int, v: Double?) = number(id, 0x85, 4,
        v?.takeIf { it.isFinite() && it in -180.0..180.0 }?.let { (it / 180 * 2147483648.0).roundToLong() } ?: 0x7fffffff)
    private fun string(id: Int, text: String) = Field(id, 7, text.toByteArray(Charsets.UTF_8) + byteArrayOf(0))

    fun encode(journal: File, destination: File) {
        RandomAccessFile(destination, "rw").use { output ->
            output.setLength(0); output.write(ByteArray(14))
            val definitions = mutableMapOf<Int, String>()
            fun message(local: Int, global: Int, fields: List<Field>, developers: List<Double?> = emptyList(), developerIndices: List<Int> = developers.indices.toList()) {
                val key = "$global:${fields.map { Triple(it.number, it.type, it.bytes.size) }}:${developerIndices}"
                if (definitions[local] != key) {
                    output.write(0x40 or local or if (developers.isNotEmpty()) 0x20 else 0)
                    output.write(0); output.write(0); output.write(global and 255); output.write(global ushr 8)
                    output.write(fields.size)
                    fields.forEach { output.write(it.number); output.write(it.bytes.size); output.write(it.type) }
                    if (developers.isNotEmpty()) {
                        output.write(developers.size)
                        developerIndices.forEach { output.write(it); output.write(8); output.write(0) }
                    }
                    definitions[local] = key
                }
                output.write(local); fields.forEach { output.write(it.bytes) }
                developers.forEach { v ->
                    val bits = v?.takeIf { it.isFinite() }?.let { java.lang.Double.doubleToRawLongBits(it) } ?: -1L
                    output.write(ByteArray(8) { (bits ushr (it * 8)).toByte() })
                }
            }
            RideJournal.read(journal) { start, config, developerIds, records ->
                val startStamp = timestamp(start).toDouble()
                message(0, 0, listOf(en(0, 4), u16(1, 255.0), u16(2, 0.0), number(3, 0x8c, 4, 1), u32(4, startStamp)))
                // Device metadata identifies the generating application, not a fabricated Garmin device.
                message(0, 23, listOf(u32(253, startStamp), u8(0, 0.0), u16(2, 255.0), u16(4, 0.0), u16(5, AppVersion.fitSoftwareVersion, 100.0), string(27, "B-Cycle")))
                if (developerIds.isNotEmpty() || config.fields.any { id -> RecordField.summaries.any { it.id == id } }) {
                    val uuid = UUID.fromString("a4d3c076-05cc-4b59-b1ab-c4963f348248")
                    val appId = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN).putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits).array()
                    message(2, 207, listOf(Field(1, 13, appId), u8(3, 0.0), u32(4, AppVersion.code.toDouble())))
                    developerIds.forEachIndexed { index, id ->
                        val field = RecordField.options.find { it.id == id }
                        message(3, 206, listOf(u8(0, 0.0), u8(1, index.toDouble()), u8(2, 0x89.toDouble()),
                            string(3, id), u8(6, 1.0), number(7, 1, 1, 0), string(8, field?.unit ?: "bool"), u16(14, 20.0)))
                    }
                }
                fun event(stamp: Double, type: Int) = message(1, 21, listOf(u32(253, stamp), en(0, 0), en(1, type), u8(4, 0.0)))
                event(startStamp, 0)
                var last: RideSample? = null
                val sums = mutableMapOf<String, Double>(); val counts = mutableMapOf<String, Int>(); val maxima = mutableMapOf<String, Double>()
                val performance = PerformanceEngine()
                val moving = MovingTime()
                var count = 0
                for (sample in records) {
                    sample.performanceEvents.forEach(performance::accept)
                    performance.advance(sample.elapsedMs)
                    val v = sample.values
                    fun selected(id: String) = v[id].takeIf { id in config.fields }
                    val speed = selected(config.speedSource)
                    moving.add(sample.elapsedMs, speed)
                    val distance = selected(config.distanceSource)
                    val cadence = selected("CADENCE")
                    val height = selected("GPS_ALTITUDE").takeIf { v["GPS_ALTITUDE_MSL"] == 1.0 }
                    val grade = selected("GPS_GRADIENT")?.times(100)?.takeIf { it in -32768.0..32766.0 }?.roundToLong() ?: 32767
                    message(4, 20, listOf(u32(253, timestamp(sample.utcMs).toDouble()), coord(0, sample.latitude), coord(1, sample.longitude),
                        u16(2, height, 5.0, 500.0), u8(3, selected("HEART_RATE")), u8(4, cadence?.let { floor(it) }),
                        u32(5, distance, 100.0), u16(6, speed, 1000.0), u16(7, selected("RIDER_POWER")), number(9, 0x83, 2, grade),
                        u8(53, cadence?.let { it - floor(it) }, 128.0), u32(73, speed, 1000.0), u32(78, height, 5.0, 500.0),
                        u8(81, selected("BATTERY"), 2.0), u16(82, selected("MOTOR_POWER"))), developerIds.map { v[it] })
                    for (id in listOf("RIDER_POWER", "HEART_RATE", "CADENCE", config.speedSource)) selected(id)?.let {
                        if (id == "CADENCE" && it <= 0.0) return@let
                        sums[id] = (sums[id] ?: 0.0) + it; counts[id] = (counts[id] ?: 0) + 1; maxima[id] = max(maxima[id] ?: it, it)
                    }
                    last = sample; count++
                }
                require(count > 0) { "No complete samples in ride journal" }
                val end = last!!; val endStamp = timestamp(end.utcMs).toDouble(); val seconds = end.elapsedMs / 1000.0
                event(endStamp, 4)
                val calculated = performance.snapshot()
                fun average(id: String) = counts[id]?.let { sums[id]!! / it }
                fun summary(session: Boolean): List<Field> {
                    val speedId = if (session) 14 else 13
                    val powerId = if (session) 20 else 19
                    val hrId = if (session) 16 else 15
                    val cadId = if (session) 18 else 17
                    val ascentId = if (session) 22 else 21
                    val distance = end.values[config.distanceSource].takeIf { config.distanceSource in config.fields }
                    val fields = mutableListOf(u16(254, 0.0), u32(253, endStamp), en(0, if (session) 8 else 9), en(1, 1),
                        u32(2, startStamp), u32(7, seconds, 1000.0), u32(8, seconds, 1000.0), u32(9, distance, 100.0),
                        u16(speedId, distance?.takeIf { seconds > 0 }?.div(seconds), 1000.0), u16(speedId + 1, maxima[config.speedSource], 1000.0),
                        u8(hrId, average("HEART_RATE")), u8(hrId + 1, maxima["HEART_RATE"]),
                        u8(cadId, average("CADENCE")), u8(cadId + 1, maxima["CADENCE"]),
                        u16(powerId, average("RIDER_POWER")), u16(powerId + 1, maxima["RIDER_POWER"]),
                        u16(ascentId, end.values["GPS_ASCENT"]), u16(ascentId + 1, end.values["GPS_DESCENT"]))
                    if (session) fields += listOf(u32(com.garmin.fit.SessionMesg.TotalMovingTimeFieldNum, moving.value?.div(1000.0), 1000.0), en(5, 2), en(6, 28), u16(25, 0.0), u16(26, 1.0))
                    else fields += listOf(en(24, 7), en(25, 2), en(39, 28))
                    fun optional(id: String, value: Double?, maximum: Double, field: (Double) -> Field) {
                        if (id in config.fields && value != null && value.isFinite() && value in 0.0..maximum) fields += field(value)
                    }
                    optional("NORMALIZED_POWER", calculated.normalizedPowerW, 65534.0) {
                        u16(if (session) com.garmin.fit.SessionMesg.NormalizedPowerFieldNum else com.garmin.fit.LapMesg.NormalizedPowerFieldNum, it)
                    }
                    optional("MECHANICAL_WORK", calculated.mechanicalWorkKj?.times(1000), 4294967294.0) {
                        u32(if (session) com.garmin.fit.SessionMesg.TotalWorkFieldNum else com.garmin.fit.LapMesg.TotalWorkFieldNum, it)
                    }
                    optional("CALORIES", calculated.caloriesKcal, 65534.0) { u16(11, it) }
                    return fields
                }
                val custom = mutableListOf<Triple<String, String, Double>>()
                fun selectedSummary(id: String, value: Double?, unit: String) {
                    if (id in config.fields && value != null && value.isFinite()) custom += Triple(id, unit, value)
                }
                selectedSummary("SPEED_POWER_RATIO", calculated.speedToPowerRatio, "km/h/W")
                selectedSummary("EFFICIENCY_FACTOR", calculated.efficiencyFactor, "W/bpm")
                val exported = RecordField.summaries.any { field -> field.id in config.fields && when (field.id) {
                    "NORMALIZED_POWER" -> calculated.normalizedPowerW != null
                    "MECHANICAL_WORK" -> calculated.mechanicalWorkKj != null
                    "CALORIES" -> calculated.caloriesKcal != null
                    "SPEED_POWER_RATIO" -> calculated.speedToPowerRatio != null
                    else -> calculated.efficiencyFactor != null
                } }
                if (exported) {
                    custom += Triple("POWER_DATA_COVERAGE", "%", calculated.quality.powerCoverage * 100)
                    custom += Triple("POWER_MISSING_TIME", "s", (calculated.quality.activeMs - calculated.quality.powerMs) / 1000.0)
                    custom += Triple("POWER_MAX_GAP", "s", calculated.quality.longestPowerGapMs / 1000.0)
                    custom += Triple("PERFORMANCE_MODEL_VERSION", "version", PerformanceEngine.MODEL_VERSION.toDouble())
                    if ("CALORIES" in config.fields && calculated.caloriesKcal != null) custom += Triple("ENERGY_GROSS_EFFICIENCY", "ratio", .24)
                }
                // Distinct field indices associate the same semantic metric with Lap and Session correctly.
                fun writeSummary(session: Boolean, local: Int, global: Int) {
                    val indices = custom.indices.map { developerIds.size + (if (session) custom.size else 0) + it }
                    custom.forEachIndexed { i, metric ->
                        message(3, 206, listOf(u8(0, 0.0), u8(1, indices[i].toDouble()), u8(2, 0x89.toDouble()),
                            string(3, metric.first), u8(6, 1.0), number(7, 1, 1, 0), string(8, metric.second), u16(14, global.toDouble())))
                    }
                    message(local, global, summary(session), custom.map { it.third }, indices)
                }
                writeSummary(false, 5, 19); writeSummary(true, 6, 18)
                val localStamp = endStamp + TimeZone.getDefault().getOffset(end.utcMs) / 1000
                message(7, 34, listOf(u32(253, endStamp), u32(0, seconds, 1000.0), u16(1, 1.0), en(2, 0), en(3, 26), en(4, 1), u32(5, localStamp)))
            }
            val size = output.length() - 14
            require(size <= 0xffffffffL)
            val header = byteArrayOf(14, 0x20) + number(0, 0, 2, 21218).bytes + number(0, 0, 4, size).bytes + ".FIT".toByteArray(Charsets.US_ASCII)
            output.seek(0); output.write(header); output.write(number(0, 0, 2, crc(header).toLong()).bytes)
            output.seek(0)
            val buffer = ByteArray(8192); var checksum = 0; var remaining = output.length()
            while (remaining > 0) {
                val n = output.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
                checksum = crc(buffer, n, checksum); remaining -= n
            }
            output.write(number(0, 0, 2, checksum.toLong()).bytes); output.fd.sync()
        }
    }

    internal fun crc(bytes: ByteArray, size: Int = bytes.size, initial: Int = 0): Int {
        var crc = initial
        for (index in 0 until size) {
            crc = crc xor (bytes[index].toInt() and 255)
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0xa001 else crc ushr 1 }
        }
        return crc
    }
}

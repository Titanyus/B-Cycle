package de.bcycle.app

/** Only fields with published, verified MCSP meanings and units are displayed. */
enum class LiveMetric(val address: Int, val title: String, val unit: String, val divisor: Double = 1.0,
                      val group: Int = 0, val maxRaw: Long = 0xffffffffL) {
    SPEED(0x182d, "Geschwindigkeit", "km/h", 100.0, maxRaw = 65535),
    RIDER_POWER(0x185b, "Fahrerleistung", "W", maxRaw = 65535),
    CADENCE(0x185a, "Kadenz", "U/min", 2.0, maxRaw = 65535),
    ASSIST_MODE(0x1809, "Unterstützung", "", maxRaw = 255),
    MOTOR_POWER(0x185d, "Motorleistung", "W", maxRaw = 65535),
    RIDER_TORQUE(0x1814, "Fahrerdrehmoment", "Nm", 20.0, maxRaw = 65535),
    MOTOR_TORQUE(0x1815, "Motordrehmoment", "Nm", 20.0, maxRaw = 65535),
    BIKE_LIGHT(0x181c, "Fahrradlicht", "", group = 1, maxRaw = 1),
    BATTERY(0x0088, "Akku", "%", group = 1, maxRaw = 100),
    ODOMETER(0x1818, "Gesamtkilometer", "km", 1000.0, group = 1),
    BATTERY_TEMPERATURE(0x008b, "Akkutemperatur", "°C", 10.0, group = 1),
    REMAINING_ENERGY(0x0091, "Verbleibende Energie", "Wh", 10.0, group = 1),
    DELIVERED_ENERGY(0x009c, "Akkuenergie gesamt abgegeben", "Wh", group = 1),
    AVERAGE_POWER(0x224a, "Ø Fahrerleistung", "W", group = 2),
    MAXIMUM_POWER(0x224b, "Max. Fahrerleistung", "W", group = 2),
    AVERAGE_CADENCE(0x2248, "Ø Kadenz", "U/min", 2.0, group = 2),
    MAXIMUM_CADENCE(0x2249, "Max. Kadenz", "U/min", 2.0, group = 2),
    AVERAGE_SPEED(0x2246, "Ø Geschwindigkeit", "km/h", 100.0, group = 2),
    RIDER_ENERGY_SHARE(0x2254, "Fahreranteil an Antriebsenergie", "%", group = 2, maxRaw = 100);

    companion object {
        fun forAddress(address: Int) = if (address == 0x1808) SPEED else entries.find { it.address == address }
    }
}

/** Null/absent denotes unavailable or unchanged; an explicit zero is retained. */
data class LiveData(
    val speedHundredths: Int? = null,
    val riderPowerWatts: Int? = null,
    val otherValues: Map<LiveMetric, Long> = emptyMap(),
    val assistNames: List<String>? = null,
    val rangesKm: List<Long>? = null,
    val indoor:Boolean=false,
    val batteries: BatterySnapshot = BatterySnapshot(),
) {
    /** Distinct decoded measurements seen on this connection, not notification count or metadata. */
    val measurementCount get() = LiveMetric.entries.count { value(it) != null } + if (rangesKm?.isNotEmpty() == true) 1 else 0
    val hasValues get() = speedHundredths != null || riderPowerWatts != null || otherValues.isNotEmpty() ||
        assistNames != null || rangesKm != null

    fun merge(update: LiveData) = LiveData(
        update.speedHundredths ?: speedHundredths,
        update.riderPowerWatts ?: riderPowerWatts,
        otherValues + update.otherValues,
        update.assistNames ?: assistNames,
        update.rangesKm ?: rangesKm,
    )

    fun value(metric: LiveMetric): Long? = when (metric) {
        LiveMetric.SPEED -> speedHundredths?.toLong()
        LiveMetric.RIDER_POWER -> riderPowerWatts?.toLong()
        else -> otherValues[metric]
    }

    companion object {
        fun sample(metric: LiveMetric, raw: Long) = when (metric) {
            LiveMetric.SPEED -> LiveData(speedHundredths = raw.toInt())
            LiveMetric.RIDER_POWER -> LiveData(riderPowerWatts = raw.toInt())
            else -> LiveData(otherValues = mapOf(metric to raw))
        }
    }
}

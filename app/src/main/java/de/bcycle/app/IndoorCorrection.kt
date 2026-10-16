package de.bcycle.app

import kotlin.math.round

/** Profile-scoped correction, applied once between raw FTMS readings and all ride consumers. */
internal data class IndoorCorrection(val powerTenths: Int = 10, val speedTenths: Int = 10) {
    init { require(powerTenths in 0..30 && speedTenths in 0..30) }
    fun apply(field: TrainerField, value: Double?): Double? = value?.let {
        when (field) {
            TrainerField.POWER -> round(it * powerTenths / 10.0) // Same whole watts in UI, journal and performance events.
            TrainerField.SPEED -> round(it * speedTenths * 10.0) / 100.0 // Existing hundredths of km/h.
            TrainerField.DISTANCE -> round(it * speedTenths / 10.0) // Existing metre counter; START still anchors it.
            else -> it
        }
    }
    companion object {
        fun storedStep(value: Int) = value.takeIf { it in 0..30 } ?: 10
    }
}

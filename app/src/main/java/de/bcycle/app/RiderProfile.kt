package de.bcycle.app

import android.content.SharedPreferences
import org.json.JSONObject
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.Period
import java.util.Locale

/** Canonical personal inputs. Derived age/zones are never persisted or copied from ride estimates. */
data class RiderProfile(val birthDate: LocalDate? = null, val heightCm: Int? = null, val weightKg: Double? = null,
                        val restingHeartRate: Int? = null, val maximumHeartRate: Int? = null, val ftpW: Int? = null) {
    fun ageYears(today: LocalDate = LocalDate.now()): Int? = birthDate?.takeIf { it <= today }?.let { Period.between(it, today).years }
    fun heartZones(): KarvonenResult? = Karvonen.calculate(restingHeartRate, maximumHeartRate)
    /** Adapter for the existing energy architecture; no new estimator is enabled. */
    fun energyProfile(today: LocalDate = LocalDate.now()) = EnergyProfile(weightKg, ageYears(today), heightCm?.toDouble(),
        restingHeartRate, maximumHeartRate, ftpW?.toDouble())
}

enum class RiderField { BIRTH_DATE, HEIGHT, WEIGHT, RESTING_HEART_RATE, MAXIMUM_HEART_RATE, FTP }
enum class RiderInputError { NUMBER, RANGE, FUTURE_DATE, BIRTH_DATE, HEART_ORDER }
data class RiderValidation(val profile: RiderProfile, val errors: Map<RiderField, RiderInputError>) { val valid get() = errors.isEmpty() }

object RiderProfileValidation {
    val heightRange = 50..260
    val weightRange = 20.0..400.0
    val restingRange = 20..140
    val maximumRange = 60..250
    val ftpRange = 1..2500
    fun minimumBirthDate(today: LocalDate) = today.minusYears(150)
    fun validate(profile: RiderProfile, today: LocalDate = LocalDate.now()): Map<RiderField, RiderInputError> = buildMap {
        profile.birthDate?.let {
            if (it > today) put(RiderField.BIRTH_DATE, RiderInputError.FUTURE_DATE)
            else if (it < minimumBirthDate(today)) put(RiderField.BIRTH_DATE, RiderInputError.BIRTH_DATE)
        }
        if (profile.heightCm != null && profile.heightCm !in heightRange) put(RiderField.HEIGHT, RiderInputError.RANGE)
        if (profile.weightKg != null && (!profile.weightKg.isFinite() || profile.weightKg !in weightRange)) put(RiderField.WEIGHT, RiderInputError.RANGE)
        if (profile.restingHeartRate != null && profile.restingHeartRate !in restingRange) put(RiderField.RESTING_HEART_RATE, RiderInputError.RANGE)
        if (profile.maximumHeartRate != null && profile.maximumHeartRate !in maximumRange) put(RiderField.MAXIMUM_HEART_RATE, RiderInputError.RANGE)
        if (profile.ftpW != null && profile.ftpW !in ftpRange) put(RiderField.FTP, RiderInputError.RANGE)
        if (profile.restingHeartRate != null && profile.maximumHeartRate != null && profile.maximumHeartRate <= profile.restingHeartRate)
            put(RiderField.MAXIMUM_HEART_RATE, RiderInputError.HEART_ORDER)
    }
    /** Entire numeric input must be valid; no grouping, exponent, trailing separator or partial parse. */
    fun parse(birthDate: LocalDate?, fields: Map<RiderField, String>, locale: Locale = Locale.getDefault(), today: LocalDate = LocalDate.now()): RiderValidation {
        val errors = mutableMapOf<RiderField, RiderInputError>()
        fun integer(field: RiderField): Int? {
            val text = fields[field].orEmpty().trim(); if (text.isEmpty()) return null
            return text.takeIf { it.all(Char::isDigit) }?.toIntOrNull().also { if (it == null) errors[field] = RiderInputError.NUMBER }
        }
        val weightText = fields[RiderField.WEIGHT].orEmpty().trim()
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val weight = if (weightText.isEmpty()) null else {
            val parts = weightText.split(separator)
            val syntax = parts.size in 1..2 && parts.all { it.isNotEmpty() && it.all(Char::isDigit) }
            weightText.takeIf { syntax }?.map { if (it == separator) '.' else ('0'.code + Character.digit(it, 10)).toChar() }?.joinToString("")?.toDoubleOrNull().also { if (it == null) errors[RiderField.WEIGHT] = RiderInputError.NUMBER }
        }
        val profile = RiderProfile(birthDate, integer(RiderField.HEIGHT), weight, integer(RiderField.RESTING_HEART_RATE), integer(RiderField.MAXIMUM_HEART_RATE), integer(RiderField.FTP))
        validate(profile, today).forEach { (field, error) -> errors.putIfAbsent(field, error) }
        return RiderValidation(profile, errors)
    }
}

/** One versioned JSON entry in the existing application preferences, not a second preferences store. */
class RiderProfileStore(private val preferences: SharedPreferences, private val today: () -> LocalDate = LocalDate::now) {
    companion object { const val key = "riderProfile.v1" }
    fun read(): RiderProfile = runCatching {
        val json = JSONObject(preferences.getString(key, null) ?: return RiderProfile())
        if (json.optInt("version") != 1) return RiderProfile()
        fun integer(name: String): Int? = (json.opt(name) as? Number)?.let { number ->
            number.toDouble().takeIf { it.isFinite() && it == number.toInt().toDouble() }?.toInt()
        }
        val birth = (json.opt("birthDate") as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val raw = RiderProfile(birth, integer("heightCm"), (json.opt("weightKg") as? Number)?.toDouble(), integer("restingHeartRate"), integer("maximumHeartRate"), integer("ftpW"))
        val errors = RiderProfileValidation.validate(raw, today())
        raw.copy(birthDate = raw.birthDate.takeUnless { RiderField.BIRTH_DATE in errors }, heightCm = raw.heightCm.takeUnless { RiderField.HEIGHT in errors },
            weightKg = raw.weightKg.takeUnless { RiderField.WEIGHT in errors }, restingHeartRate = raw.restingHeartRate.takeUnless { RiderField.RESTING_HEART_RATE in errors },
            maximumHeartRate = raw.maximumHeartRate.takeUnless { RiderField.MAXIMUM_HEART_RATE in errors }, ftpW = raw.ftpW.takeUnless { RiderField.FTP in errors })
    }.getOrDefault(RiderProfile())
    fun write(profile: RiderProfile): Boolean {
        if (RiderProfileValidation.validate(profile, today()).isNotEmpty()) return false
        val json = JSONObject().put("version", 1).put("birthDate", profile.birthDate?.toString())
            .put("heightCm", profile.heightCm).put("weightKg", profile.weightKg).put("restingHeartRate", profile.restingHeartRate)
            .put("maximumHeartRate", profile.maximumHeartRate).put("ftpW", profile.ftpW)
        return preferences.edit().putString(key, json.toString()).commit()
    }
}

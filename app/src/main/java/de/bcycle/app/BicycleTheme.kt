package de.bcycle.app

import kotlin.math.pow

/** Only these five roles may vary with a palette. All colors are opaque ARGB. */
data class ThemeTokens(val primary: Int, val secondary: Int, val tertiary: Int, val foreground: Int,
                       val accent: Int = foreground)

sealed interface BicycleTheme {
    val id: String
    val name: String
    val tokens: ThemeTokens
    val isCustom: Boolean
}

data class PresetTheme(override val id: String, override val name: String,
                       override val tokens: ThemeTokens,
                       val approvedLowContrastSurfaces: Set<ThemeRole> = emptySet()) : BicycleTheme {
    override val isCustom = false
}

data class CustomTheme(override val id: String, override val name: String,
                       override val tokens: ThemeTokens) : BicycleTheme {
    override val isCustom = true
}

object BicycleThemes {
    private fun preset(id: String, name: String, primary: Long, secondary: Long, tertiary: Long,
                       foreground: Long = 0xffffffff, accent: Long = foreground,
                       approvedLowContrastSurfaces: Set<ThemeRole> = emptySet()) = PresetTheme("preset:$id", name,
        ThemeTokens(primary.toInt(), secondary.toInt(), tertiary.toInt(), foreground.toInt(), accent.toInt()), approvedLowContrastSurfaces)
    val red = preset("bicycle-red", "BICYCLE RED", 0xffbf281b, 0xff89190e, 0xff680e07)
    /** Immutable templates; stored custom themes cannot overwrite these stable IDs. */
    val presets: List<PresetTheme> = java.util.Collections.unmodifiableList(listOf(
        red,
        preset("black-ops", "EMBERS", 0xffbf281b, 0xff000000, 0xff292929, accent = 0xffbf281b),
        // Exact palette and white-on-yellow header explicitly accepted by the user.
        preset("solar-flare", "FIRE", 0xffffb400, 0xff000000, 0xff292929, accent = 0xffffb400,
            approvedLowContrastSurfaces = setOf(ThemeRole.PRIMARY)),
        preset("electric-blue", "ELECTRIC BLUE", 0xff1168b8, 0xff10508c, 0xff09294d),
        preset("acid-lime", "ACID LIME", 0xff587812, 0xff39510e, 0xff203007),
        preset("ultraviolet", "ULTRAVIOLET", 0xff8242c3, 0xff57238e, 0xff2d114d),
        preset("petrol-attack", "PETROL ATTACK", 0xff117782, 0xff11525c, 0xff092c33)
    ))
}

data class ThemeValidation(val problems: List<String>) {
    val valid get() = problems.isEmpty()
}

object ThemeValidator {
    const val minimumTextContrast = 4.5
    const val minimumIconContrast = 3.0
    const val minimumSurfaceStep = 0.01
    fun validate(theme: BicycleTheme): ThemeValidation {
        val problems = mutableListOf<String>()
        val prefix = if (theme.isCustom) "custom:" else "preset:"
        if (!theme.id.startsWith(prefix) || theme.id.length <= prefix.length ||
            !theme.id.matches(Regex("[a-z0-9_.:-]{1,80}"))) problems += "Ungültige Theme-ID"
        if (theme.name.isBlank() || theme.name.length > 60 || theme.name != theme.name.trim() ||
            theme.name.any { it.isISOControl() }) problems += "Theme-Name muss 1–60 lesbare Zeichen enthalten"
        val t = theme.tokens
        val surfaces = listOf(t.primary, t.secondary, t.tertiary)
        if ((surfaces + listOf(t.foreground, t.accent)).any { it ushr 24 != 255 }) problems += "Fünf deckende ARGB-Farben erforderlich"
        val light = surfaces.map(::luminance)
        val steps = listOf(light[1] - light[0], light[2] - light[1])
        if (steps.any { kotlin.math.abs(it) < minimumSurfaceStep })
            problems += "Benachbarte Flächen müssen erkennbar heller oder dunkler abgestuft sein"
        val approved = (theme as? PresetTheme)?.approvedLowContrastSurfaces.orEmpty()
        val surfaceRoles = listOf(ThemeRole.PRIMARY, ThemeRole.SECONDARY, ThemeRole.TERTIARY)
        if (surfaces.withIndex().any { (index, color) -> surfaceRoles[index] !in approved && contrast(t.foreground, color) < minimumTextContrast })
            problems += "Text und Icons benötigen mindestens 4,5:1 Kontrast zu jeder Fläche"
        // Semantic highlights currently live on SECONDARY. Other surfaces use
        // the shared per-surface foreground fallback when accent is unreadable.
        if (contrast(t.accent, t.secondary) < minimumIconContrast)
            problems += "Accent-Icons benötigen mindestens 3:1 Kontrast zur Dialogfläche"
        return ThemeValidation(problems.toList())
    }
    fun luminance(color: Int): Double {
        fun linear(shift: Int): Double {
            val value = ((color ushr shift) and 255) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(16) + 0.7152 * linear(8) + 0.0722 * linear(0)
    }
    fun contrast(first: Int, second: Int): Double {
        val a = luminance(first); val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }
}

package de.bcycle.app

import kotlin.math.pow
import kotlin.math.roundToInt

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
                       val approvedLowContrastSurfaces: Set<ThemeRole> = emptySet(),
                       val approvedLowContrastAccent: Boolean = false,
                       val approvedSubtleSurfaces: Boolean = false) : BicycleTheme {
    override val isCustom = false
}

data class CustomTheme(override val id: String, override val name: String,
                       override val tokens: ThemeTokens) : BicycleTheme {
    override val isCustom = true
}

object BicycleThemes {
    private fun color(hex:String)=(0xff000000L or hex.toLong(16)).toInt()
    private fun palette(id:String,name:String,primary:String):PresetTheme {
        val t=ThemeTokens(color(primary),color("161522"),color("282735"),0xffffffff.toInt(),color(primary))
        val roles=listOf(ThemeRole.PRIMARY,ThemeRole.SECONDARY,ThemeRole.TERTIARY)
        val approved=roles.filter { ThemeValidator.contrast(t.foreground,t.color(it))<ThemeValidator.minimumTextContrast }.toSet()
        return PresetTheme("preset:$id-color",name,t,approved,
            ThemeValidator.contrast(t.accent,t.secondary)<ThemeValidator.minimumIconContrast,true)
    }
    // Seven original hues alternate with HSV midpoints (shortest hue arc, mean S/V).
    // All presets share the normal dark surfaces; ACCENT always follows PRIMARY.
    val presets:List<PresetTheme> = java.util.Collections.unmodifiableList(listOf(
        palette("hellfire","HELLFIRE","BF281B"),
        palette("ember-rush","EMBER RUSH","D84413"),
        palette("solar-flare","SOLAR FLARE","F16A09"),
        palette("gold-rush","GOLD RUSH","ED8C04"),
        palette("sunstruck","SUNSTRUCK","E9AF00"),
        palette("citrus-charge","CITRUS CHARGE","C9DB02"),
        palette("acid-pop","ACID POP","7ACD04"),
        palette("emerald-surge","EMERALD SURGE","05C121"),
        palette("deep-dive","DEEP DIVE","06B5A1"),
        palette("tidal-blue","TIDAL BLUE","0C96C6"),
        palette("blue-riot","BLUE RIOT","145CD7"),
        palette("indigo-rush","INDIGO RUSH","311CDC"),
        palette("purple-haze","PURPLE HAZE","9224E0"),
        palette("neon-bloom","NEON BLOOM","D01F95")
    ))
    private val darkReplacements=mapOf(
        "preset:hellfire-dark" to "preset:ember-rush-color",
        "preset:solar-flare-dark" to "preset:gold-rush-color",
        "preset:sunstruck-dark" to "preset:citrus-charge-color",
        "preset:acid-pop-dark" to "preset:emerald-surge-color",
        "preset:deep-dive-dark" to "preset:tidal-blue-color",
        "preset:blue-riot-dark" to "preset:indigo-rush-color",
        "preset:purple-haze-dark" to "preset:neon-bloom-color")
    val red get()=presets.first()
    private val legacyIds=mapOf(
        "bicycle-red" to "hellfire-color", "black-ops" to "hellfire-dark", "embers-inv" to "hellfire-dark",
        "solar-flare" to "sunstruck-dark", "electric-blue" to "blue-riot-dark", "acid-lime" to "acid-pop-dark",
        "ultraviolet" to "purple-haze-color", "petrol-attack" to "deep-dive-color")
    fun findPreset(id:String?):PresetTheme? {
        val mapped=legacyIds[id?.removePrefix("preset:")]?.let { "preset:$it" } ?: id
        return presets.find { it.id==(darkReplacements[mapped] ?: mapped) }
    }
}

/** Small, deterministic RGB tint; display-only, never written into the source palette. */
internal fun ThemeTokens.lightSettings(tertiaryBase:Int=0xffd0d0d0.toInt()):ThemeTokens {
    fun tint(base:Int,amount:Float):Int {
        fun channel(shift:Int)=(((base ushr shift) and 255)*(1f-amount)+((primary ushr shift) and 255)*amount).roundToInt()
        return (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
    return copy(secondary=0xffffffff.toInt(),tertiary=tint(tertiaryBase,.08f),foreground=tint(0xff101010.toInt(),.05f))
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
        if (steps.any { kotlin.math.abs(it) < 0.000001 || ((theme as? PresetTheme)?.approvedSubtleSurfaces != true && kotlin.math.abs(it) < minimumSurfaceStep) })
            problems += "Benachbarte Flächen müssen erkennbar heller oder dunkler abgestuft sein"
        val approved = (theme as? PresetTheme)?.approvedLowContrastSurfaces.orEmpty()
        val surfaceRoles = listOf(ThemeRole.PRIMARY, ThemeRole.SECONDARY, ThemeRole.TERTIARY)
        if (surfaces.withIndex().any { (index, color) -> surfaceRoles[index] !in approved && contrast(t.foreground, color) < minimumTextContrast })
            problems += "Text und Icons benötigen mindestens 4,5:1 Kontrast zu jeder Fläche"
        // Semantic highlights currently live on SECONDARY. Other surfaces use
        // the shared per-surface foreground fallback when accent is unreadable.
        if ((theme as? PresetTheme)?.approvedLowContrastAccent != true && contrast(t.accent, t.secondary) < minimumIconContrast)
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

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
    private fun family(id:String,name:String,primary:String,secondary:String,tertiary:String,darkSecondary:String,darkTertiary:String):List<PresetTheme> {
        fun theme(dark:Boolean):PresetTheme {
            val t=ThemeTokens(color(primary),color(if(dark) darkSecondary else secondary),color(if(dark) darkTertiary else tertiary),0xffffffff.toInt(),color(primary))
            val roles=listOf(ThemeRole.PRIMARY,ThemeRole.SECONDARY,ThemeRole.TERTIARY)
            // These exact fixed palettes were chosen by the user; custom-theme checks remain strict.
            val approved=roles.filter { ThemeValidator.contrast(t.foreground,t.color(it))<ThemeValidator.minimumTextContrast }.toSet()
            return PresetTheme("preset:$id-${if(dark) "dark" else "color"}",name+if(dark) " DARK" else "",t,
                approved,ThemeValidator.contrast(t.accent,t.secondary)<ThemeValidator.minimumIconContrast,true)
        }
        return listOf(theme(false),theme(true))
    }
    val presets:List<PresetTheme> = java.util.Collections.unmodifiableList(listOf(
        family("hellfire","HELLFIRE","BF281B","161522","282735","0F0A09","211918"),
        family("solar-flare","SOLAR FLARE","F16A09","161522","282735","100B08","221A15"),
        family("sunstruck","SUNSTRUCK","E9AF00","161522","282735","100E07","242014"),
        family("acid-pop","ACID POP","7ACD04","161522","282735","0A0F07","192215"),
        family("deep-dive","DEEP DIVE","06B5A1","161522","282735","07100F","142221"),
        family("blue-riot","BLUE RIOT","145CD7","161522","282735","080B10","151B23"),
        family("purple-haze","PURPLE HAZE","9224E0","161522","282735","0B0910","1A1624")
    ).flatten())
    val red get()=presets.first()
    private val legacyIds=mapOf(
        "bicycle-red" to "hellfire-color", "black-ops" to "hellfire-dark", "embers-inv" to "hellfire-dark",
        "solar-flare" to "sunstruck-dark", "electric-blue" to "blue-riot-dark", "acid-lime" to "acid-pop-dark",
        "ultraviolet" to "purple-haze-color", "petrol-attack" to "deep-dive-color")
    fun findPreset(id:String?):PresetTheme? {
        val mapped=legacyIds[id?.removePrefix("preset:")]?.let { "preset:$it" } ?: id
        return presets.find { it.id==mapped }
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

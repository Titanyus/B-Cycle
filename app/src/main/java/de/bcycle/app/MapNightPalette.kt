package de.bcycle.app

internal object MapNightPalette {
    const val background = 0xff171d22.toInt()
    private val table by lazy { IntArray(32 * 32 * 32) { index ->
        shade(((index ushr 10) * 8 + 4).coerceAtMost(255), (((index ushr 5) and 31) * 8 + 4).coerceAtMost(255),
            ((index and 31) * 8 + 4).coerceAtMost(255))
    } }
    fun color(color: Int): Int = (color and -0x1000000) or
        (table[(((color ushr 19) and 31) shl 10) or (((color ushr 11) and 31) shl 5) or ((color ushr 3) and 31)] and 0xffffff)
    private fun shade(r: Int, g: Int, b: Int): Int {
        fun rgb(red: Double, green: Double, blue: Double) = android.graphics.Color.rgb(
            red.toInt().coerceIn(0, 255), green.toInt().coerceIn(0, 255), blue.toInt().coerceIn(0, 255))
        val high = maxOf(r, g, b); val low = minOf(r, g, b)
        val light = .2126 * r + .7152 * g + .0722 * b
        return when {
            high - low < 18 -> {
                val gray = when { light < 110 -> 220 - light * .3; light >= 245 -> 110.0
                    light >= 220 -> 28.0; else -> 34 + (220 - light) * .15 }
                rgb(gray, gray + 2, gray + 5)
            }
            b > r + 18 && b >= g - 5 -> rgb(12 + (255 - r) * .08, 26 + (255 - r) * .16, 44 + (255 - r) * .28)
            g > r + 5 && g > b + 10 -> rgb(17 + light * .025, 27 + light * .055, 22 + light * .02)
            r > b + 35 && g > b + 20 && r >= g - 5 -> rgb(95 + (255 - light) * .2, 82 + (255 - light) * .15, 48.0)
            else -> rgb(25 + r * .12, 25 + g * .12, 28 + b * .12)
        }
    }
}


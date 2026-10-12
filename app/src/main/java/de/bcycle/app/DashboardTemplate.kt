package de.bcycle.app

/** Fixed templates; metric selection remains independent of map geometry. */
enum class DashboardTemplate(val title: String, val slots: List<Int>, val dataFraction: Float, val titleResource: Int = 0) {
    DATA("7 Werte · kleine Karte", listOf(0, 1, 2, 3, 4, 5, 6), .70f),
    BALANCED("4 Werte · mittlere Karte", listOf(0, 1, 3, 4), .28f),
    MAP("2 Werte · große Karte", listOf(0, 1), .14f),
    OVERVIEW("3 Werte · Kartenübersicht", listOf(0, 1, 2), .14f),
    FIVE("5 Werte · 2 oben / 3 unten", listOf(0, 1, 2, 3, 4), .28f),
    HERO("4 Werte · groß oben / 3 unten", listOf(2, 3, 4, 5), .42f),
    HERO_SEVEN("", listOf(2, 0, 1, 3, 4, 5, 6), .56f, R.string.dashboard_preset_seven),
    HERO_FIVE("", listOf(2, 0, 1, 3, 4), .56f, R.string.dashboard_preset_eight);
    fun displayTitle(context: android.content.Context) = if (titleResource != 0) context.getString(titleResource) else title
    val largeTop: Boolean get() = this == HERO || this == HERO_SEVEN || this == HERO_FIVE
    val rows: List<List<Int>> get() = when (this) {
        DATA -> listOf(listOf(0, 1), listOf(2), listOf(3, 4), listOf(5, 6))
        OVERVIEW -> listOf(slots)
        FIVE -> listOf(slots.take(2), slots.drop(2))
        HERO -> listOf(slots.take(1), slots.drop(1))
        HERO_SEVEN -> listOf(slots.take(1)) + slots.drop(1).chunked(3)
        HERO_FIVE -> listOf(slots.take(1)) + slots.drop(1).chunked(2)
        else -> slots.chunked(2)
    }
    fun rowWeight(index: Int, wide: Boolean = false) = when {
        this == DATA && index == 1 -> 2f * LARGE_COMPACT_RATIO
        largeTop && index == 0 -> (if (wide && this == HERO) 3f else 2f) * LARGE_COMPACT_RATIO
        else -> 1f
    }
    /** Calibrate all presets against DATA, rather than stretching fewer rows. */
    fun heightForViewport(available: Int, bottomSpaceSaved: Int, wide: Boolean = false): Int {
        val reference = available * DATA.dataFraction
        val rowCount = if (wide) 3 else 5
        val smallRow = ((reference - if (wide) 2 else 3) / rowCount).toInt().coerceAtLeast(0)
        val compactRow = (smallRow - bottomSpaceSaved).coerceAtLeast(0)
        val height = if (this == DATA && wide) compactRow * 3 + 2 else
            rows.indices.sumOf { (compactRow * rowWeight(it, wide)).toInt() } + rows.size - 1
        return height.coerceIn(0, available.coerceAtLeast(0))
    }
    companion object {
        // Halve the large label area while keeping the original value-row space.
        const val LARGE_COMPACT_RATIO = .75f / .85f
        fun decode(id: String?) = entries.firstOrNull { it.name == id } ?: DATA }
}

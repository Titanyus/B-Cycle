package de.bcycle.app

/** Fixed templates; metric selection remains independent of map geometry. */
enum class DashboardTemplate(val title: String, val slots: List<Int>, val dataFraction: Float) {
    DATA("7 Werte · kleine Karte", listOf(0, 1, 2, 3, 4, 5, 6), .70f),
    BALANCED("4 Werte · mittlere Karte", listOf(0, 1, 3, 4), .45f),
    MAP("2 Werte · große Karte", listOf(0, 1), .24f),
    OVERVIEW("3 Werte · Kartenübersicht", listOf(0, 1, 2), .20f),
    FIVE("5 Werte · 2 oben / 3 unten", listOf(0, 1, 2, 3, 4), .40f),
    HERO("4 Werte · groß oben / 3 unten", listOf(2, 3, 4, 5), .55f);
    val rows: List<List<Int>> get() = when (this) {
        DATA -> listOf(listOf(0, 1), listOf(2), listOf(3, 4), listOf(5, 6))
        OVERVIEW -> listOf(slots)
        FIVE -> listOf(slots.take(2), slots.drop(2))
        HERO -> listOf(slots.take(1), slots.drop(1))
        else -> slots.chunked(2)
    }
    fun rowWeight(index: Int) = if ((this == DATA && index == 1) || (this == HERO && index == 0)) 2f else 1f
    companion object { fun decode(id: String?) = entries.firstOrNull { it.name == id } ?: DATA }
}

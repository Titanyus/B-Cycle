package de.bcycle.app

/** Fixed templates; metric selection remains independent of map geometry. */
enum class DashboardTemplate(val title: String, val slots: List<Int>, val dataFraction: Float) {
    DATA("7 Werte · kleine Karte", listOf(0, 1, 2, 3, 4, 5, 6), .70f),
    BALANCED("4 Werte · mittlere Karte", listOf(0, 1, 3, 4), .45f),
    MAP("2 Werte · große Karte", listOf(0, 1), .24f),
    OVERVIEW("3 Werte · Kartenübersicht", listOf(0, 1, 2), .20f);
    companion object { fun decode(id: String?) = entries.firstOrNull { it.name == id } ?: DATA }
}

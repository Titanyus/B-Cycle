package de.bcycle.app

/** Fixed templates; metric selection remains independent of map geometry. */
enum class DashboardTemplate(val title: String, val slots: List<Int>, val dataFraction: Float, val titleResource: Int = 0) {
    DATA("", listOf(0, 1, 2, 3, 4, 5, 6), .70f, R.string.ui_preset_data),
    BALANCED("", listOf(0, 1, 3, 4), .28f, R.string.ui_preset_balanced),
    MAP("", listOf(0, 1), .14f, R.string.ui_preset_map),
    OVERVIEW("", listOf(0, 1, 2), .14f, R.string.ui_preset_overview),
    FIVE("", listOf(0, 1, 2, 3, 4), .28f, R.string.ui_preset_five),
    HERO("", listOf(2, 3, 4, 5), .42f, R.string.ui_preset_hero),
    HERO_SEVEN("", listOf(2, 0, 1, 3, 4, 5, 6), .56f, R.string.dashboard_preset_seven),
    HERO_FIVE("", listOf(2, 0, 1, 3, 4), .56f, R.string.dashboard_preset_eight),
    EIGHT("", (0 until 8).toList(), .42f, R.string.dashboard_preset_nine),
    FULL_DATA("",listOf(0,1,2,3,4),1f,R.string.dashboard_preset_ten),
    FULL_EIGHT("",(0 until 8).toList(),1f,R.string.dashboard_full_eight),
    FULL_FOUR("",(0 until 4).toList(),1f,R.string.dashboard_full_four),
    FULL_NINE("",(0 until 9).toList(),1f,R.string.dashboard_full_nine);
    val hasMap get()=dataFraction<1f
    /** Display-only fallback: trying Indoor must never erase a saved outdoor layout. */
    fun forBike(type:BikeType)=if(type==BikeType.INDOOR && hasMap) FULL_DATA else this
    fun displayTitle(context: android.content.Context) = if (titleResource != 0) context.getString(titleResource) else title
    val largeTop: Boolean get() = this == HERO || this == HERO_SEVEN || this == HERO_FIVE
    val rows: List<List<Int>> get() = when (this) {
        FULL_EIGHT -> listOf(listOf(0),listOf(1),listOf(2,3),listOf(4,5),listOf(6,7))
        FULL_FOUR -> slots.map { listOf(it) }
        FULL_NINE -> listOf(listOf(0),listOf(1),listOf(2),listOf(3,4,5),listOf(6,7,8))
        FULL_DATA -> listOf(listOf(0),listOf(1),listOf(2),listOf(3,4))
        DATA -> listOf(listOf(0, 1), listOf(2), listOf(3, 4), listOf(5, 6))
        OVERVIEW -> listOf(slots)
        FIVE -> listOf(slots.take(2), slots.drop(2))
        EIGHT -> listOf(slots.take(2)) + slots.drop(2).chunked(3)
        HERO -> listOf(slots.take(1), slots.drop(1))
        HERO_SEVEN -> listOf(slots.take(1)) + slots.drop(1).chunked(3)
        HERO_FIVE -> listOf(slots.take(1)) + slots.drop(1).chunked(2)
        else -> slots.chunked(2)
    }
    fun rowWeight(index: Int, wide: Boolean = false) = when {
        !hasMap && rows[index].size==1 -> DashboardTileStyle.ONE.heightWeight
        this == DATA && index == 1 -> DashboardTileStyle.ONE.heightWeight
        largeTop && index == 0 -> if (wide && this == HERO) 3f * LARGE_COMPACT_RATIO else DashboardTileStyle.ONE.heightWeight
        else -> DashboardTileStyle.TWO.heightWeight
    }
    internal fun tileStyle(slot: Int) = when {
        !hasMap && rows.any { it.size==1 && slot in it } -> DashboardTileStyle.ONE
        rows.any { it.size == 3 && slot in it } -> DashboardTileStyle.THREE
        slot == 2 && (this == DATA || largeTop) -> DashboardTileStyle.ONE
        else -> DashboardTileStyle.TWO
    }
    /** Calibrate all presets against DATA, rather than stretching fewer rows. */
    fun heightForViewport(available: Int, bottomSpaceSaved: Int, wide: Boolean = false): Int {
        if(!hasMap) return available.coerceAtLeast(0)
        val reference = available * DATA.dataFraction
        val rowCount = if (wide) 3 else 5
        val smallRow = ((reference - if (wide) 2 else 3) / rowCount).toInt().coerceAtLeast(0)
        val compactRow = (smallRow - bottomSpaceSaved).coerceAtLeast(0)
        val height = if (this == DATA && wide) compactRow * 3 + 2 else
            rows.indices.sumOf { (compactRow * rowWeight(it, wide)).toInt() } + rows.size - 1
        return height.coerceIn(0, available.coerceAtLeast(0))
    }
    fun supportsGraph(slot: Int) = rows.any { it.size == 1 && it.single() == slot }

    companion object {
        /** Picker-only order. Enum order remains the persisted default-page compatibility boundary. */
        val pickerOrder=listOf(FULL_FOUR,FULL_DATA,FULL_NINE,FULL_EIGHT,
            MAP,OVERVIEW,BALANCED,FIVE,EIGHT,HERO,HERO_FIVE,HERO_SEVEN,DATA)
        // Halve the large label area while keeping the original value-row space.
        const val LARGE_COMPACT_RATIO = BicycleDesign.dashboardLargeCompactRatio
        fun decode(id: String?) = entries.firstOrNull { it.name == id } ?: DATA }
}

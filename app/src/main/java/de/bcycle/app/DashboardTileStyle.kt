package de.bcycle.app

/** Three existing dashboard geometries, independent of metric choice and theme. All sizes in dp/sp. */
internal enum class DashboardTileStyle(
    val horizontalPadding: Int = 10,
    val captionWeight: Float = .20f,
    val valueUnitGap: Int = 6,
    val fixedValueSp: Float? = null,
    val fixedUnitSp: Float? = null,
    val unitWidthDivisor: Int = 4,
    val heightWeight: Float = 1f
) {
    ONE(captionWeight = .10f, heightWeight = 2f * BicycleDesign.dashboardLargeCompactRatio),
    TWO,
    THREE(horizontalPadding = BicycleDesign.tripleHorizontalPadding, valueUnitGap = 4,
        unitWidthDivisor = 2);

    val topPadding = 8
    val bottomPadding = 4
    val captionSp = BicycleDesign.dashboardCaptionSp
    val valueWeight = .65f
    val valueMinSp = 10
    val valueMaxSp = 240
    val unitMinSp = 5
    val unitMaxSp = 16

}

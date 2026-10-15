package de.bcycle.app

/** Build metadata is the single source for diagnostics, requests and FIT provenance. */
internal object AppVersion {
    const val name = BuildConfig.VERSION_NAME
    const val code = BuildConfig.VERSION_CODE
    const val buildTimestamp = BuildConfig.BUILD_TIMESTAMP

    fun buildDate(): String = java.time.format.DateTimeFormatter
        .ofPattern("dd.MM.yyyy · HH:mm:ss XXX", java.util.Locale.getDefault())
        .withZone(java.time.ZoneId.systemDefault())
        .format(java.time.Instant.ofEpochSecond(buildTimestamp))
    // Garmin device_info has a numeric, two-decimal software_version; patch releases
    // remain uniquely identified by developer_data_id.application_version (build code).
    val fitSoftwareVersion = name.substringBeforeLast('.').toDouble()
}

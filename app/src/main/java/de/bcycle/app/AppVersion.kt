package de.bcycle.app

/** Build metadata is the single source for diagnostics, requests and FIT provenance. */
internal object AppVersion {
    const val name = BuildConfig.VERSION_NAME
    const val code = BuildConfig.VERSION_CODE
    // Garmin device_info has a numeric, two-decimal software_version; patch releases
    // remain uniquely identified by developer_data_id.application_version (build code).
    val fitSoftwareVersion = name.substringBeforeLast('.').toDouble()
}

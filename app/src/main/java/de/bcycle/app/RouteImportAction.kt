package de.bcycle.app

/** Single list for the route-manager dropdown and Backup & Restore import entries. */
internal enum class RouteImportAction(val title:Int,val icon:BicycleIcon,val viewTag:String) {
    PLANNED(R.string.ui_import_planned,BicycleIcon.TrackImported,"route_import_planned"),
    RECORDED(R.string.ui_import_recorded,BicycleIcon.TrackRecorded,"route_import_recorded"),
    ARCHIVE(R.string.archive_import_title,BicycleIcon.Import,"route_import_archive"),
    DREEVE(R.string.dreeve_import_title,BicycleIcon.Dreeve,"route_import_dreeve")
}

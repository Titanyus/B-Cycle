package de.bcycle.app

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

internal data class SettingsHelpGroup(val heading:Int,val sections:List<SettingsHelpSection>)
internal data class SettingsHelpSection(val heading:Int,val body:Int)
internal enum class SettingsHelpPage(val title:Int,val sections:List<SettingsHelpSection>) {
    MAIN(R.string.ui_settings,listOf(SettingsHelpSection(R.string.help_navigation_heading,R.string.help_navigation_body),SettingsHelpSection(R.string.help_appearance_heading,R.string.help_appearance_body),SettingsHelpSection(R.string.help_rider_heading,R.string.help_rider_body),SettingsHelpSection(R.string.help_profiles_heading,R.string.help_profiles_body),SettingsHelpSection(R.string.help_routes_heading,R.string.help_routes_body),SettingsHelpSection(R.string.help_notifications_heading,R.string.help_notifications_body),SettingsHelpSection(R.string.help_data_heading,R.string.help_data_body),SettingsHelpSection(R.string.help_advanced_heading,R.string.help_advanced_body))),
    RIDER(R.string.rider_profile,listOf(SettingsHelpSection(R.string.help_personal_heading,R.string.help_personal_body),SettingsHelpSection(R.string.help_heart_method_heading,R.string.help_heart_method_body),SettingsHelpSection(R.string.help_karvonen_heading,R.string.help_karvonen_body),SettingsHelpSection(R.string.help_maximum_heading,R.string.help_maximum_body),SettingsHelpSection(R.string.help_threshold_heading,R.string.help_threshold_body),SettingsHelpSection(R.string.help_zone_model_details_heading,R.string.help_zone_model_details_body),SettingsHelpSection(R.string.help_ftp_heading,R.string.help_ftp_body),SettingsHelpSection(R.string.help_power_zones_heading,R.string.help_power_zones_body),SettingsHelpSection(R.string.help_heart_zones_heading,R.string.help_heart_zones_body))),
    PROFILES(R.string.profiles_title,listOf(SettingsHelpSection(R.string.help_profiles_heading,R.string.help_profiles_body),SettingsHelpSection(R.string.help_bike_tiles_heading,R.string.help_bike_tiles_body),SettingsHelpSection(R.string.help_bike_name_photo_heading,R.string.help_bike_name_photo_body),SettingsHelpSection(R.string.help_bike_type_heading,R.string.help_bike_type_body),SettingsHelpSection(R.string.help_sensors_heading,R.string.help_sensors_body),SettingsHelpSection(R.string.help_gps_heading,R.string.help_gps_body),SettingsHelpSection(R.string.help_indoor_correction_heading,R.string.help_indoor_correction_body),SettingsHelpSection(R.string.help_fit_sources_heading,R.string.help_fit_sources_body),SettingsHelpSection(R.string.help_record_fields_heading,R.string.help_record_fields_body),SettingsHelpSection(R.string.help_map_sources_heading,R.string.help_map_sources_body),SettingsHelpSection(R.string.help_map_controls_heading,R.string.help_map_controls_body),SettingsHelpSection(R.string.help_elevation_heading,R.string.help_elevation_body),SettingsHelpSection(R.string.help_design_heading,R.string.help_design_body),SettingsHelpSection(R.string.help_dashboards_heading,R.string.help_dashboards_body),SettingsHelpSection(R.string.help_editor_heading,R.string.help_editor_body),SettingsHelpSection(R.string.help_bosch_mode_heading,R.string.help_bosch_mode_body),SettingsHelpSection(R.string.help_drink_reminder_heading,R.string.help_drink_reminder_body),SettingsHelpSection(R.string.help_heart_method_heading,R.string.help_heart_method_body),SettingsHelpSection(R.string.help_help_mode_heading,R.string.help_help_mode_body),SettingsHelpSection(R.string.help_help_values_heading,R.string.help_help_values_body),SettingsHelpSection(R.string.help_zone_two_heading,R.string.help_zone_two_body),SettingsHelpSection(R.string.help_profile_actions_heading,R.string.help_profile_actions_body))),
    DESIGN(R.string.ui_design_title,listOf(SettingsHelpSection(R.string.help_design_heading,R.string.help_design_body),SettingsHelpSection(R.string.help_dashboards_heading,R.string.help_dashboards_body),SettingsHelpSection(R.string.help_editor_heading,R.string.help_editor_body),SettingsHelpSection(R.string.help_bosch_mode_heading,R.string.help_bosch_mode_body),SettingsHelpSection(R.string.help_drink_reminder_heading,R.string.help_drink_reminder_body),SettingsHelpSection(R.string.help_heart_method_heading,R.string.help_heart_method_body),SettingsHelpSection(R.string.help_help_mode_heading,R.string.help_help_mode_body),SettingsHelpSection(R.string.help_help_values_heading,R.string.help_help_values_body),SettingsHelpSection(R.string.help_zone_two_heading,R.string.help_zone_two_body))),
    MAPS(R.string.ui_map_manager_title,listOf(SettingsHelpSection(R.string.help_map_lines_heading,R.string.help_map_lines_body),SettingsHelpSection(R.string.help_map_sources_heading,R.string.help_map_sources_body),SettingsHelpSection(R.string.help_map_controls_heading,R.string.help_map_controls_body),SettingsHelpSection(R.string.help_elevation_heading,R.string.help_elevation_body))),
    FIELDS(R.string.help_fields_title,listOf(SettingsHelpSection(R.string.help_drink_reminder_heading,R.string.help_drink_reminder_body),SettingsHelpSection(R.string.help_field_data_heading,R.string.help_field_data_body),SettingsHelpSection(R.string.help_field_graph_heading,R.string.help_field_graph_body),SettingsHelpSection(R.string.help_batteries_heading,R.string.help_batteries_body),SettingsHelpSection(R.string.help_heart_method_heading,R.string.help_heart_method_body),SettingsHelpSection(R.string.help_help_mode_heading,R.string.help_help_mode_body),SettingsHelpSection(R.string.help_help_values_heading,R.string.help_help_values_body),SettingsHelpSection(R.string.help_zone_two_heading,R.string.help_zone_two_body),SettingsHelpSection(R.string.help_editor_heading,R.string.help_editor_body))),
    ROUTES(R.string.ui_route_manager_title,listOf(SettingsHelpSection(R.string.help_routes_heading,R.string.help_routes_body),SettingsHelpSection(R.string.help_route_import_heading,R.string.help_route_import_body),SettingsHelpSection(R.string.help_route_actions_heading,R.string.help_route_actions_body))),
    ROUTE_DETAILS(R.string.help_details_title,listOf(SettingsHelpSection(R.string.help_route_title_heading,R.string.help_route_title_body),SettingsHelpSection(R.string.help_route_weather_heading,R.string.help_route_weather_body),SettingsHelpSection(R.string.help_route_metrics_heading,R.string.help_route_metrics_body),SettingsHelpSection(R.string.help_route_training_heading,R.string.help_route_training_body),SettingsHelpSection(R.string.help_route_charts_heading,R.string.help_route_charts_body),SettingsHelpSection(R.string.help_elevation_heading,R.string.help_elevation_body),SettingsHelpSection(R.string.help_route_actions_heading,R.string.help_route_actions_body))),
    ROUTE_MAP(R.string.help_map_title,listOf(SettingsHelpSection(R.string.help_map_controls_heading,R.string.help_map_controls_body),SettingsHelpSection(R.string.help_route_weather_heading,R.string.help_route_weather_body))),
    DATA(R.string.data_title,listOf(SettingsHelpSection(R.string.help_local_heading,R.string.help_local_body),SettingsHelpSection(R.string.help_webdav_heading,R.string.help_webdav_body),SettingsHelpSection(R.string.help_intervals_heading,R.string.help_intervals_body),SettingsHelpSection(R.string.help_dreeve_heading,R.string.help_dreeve_body),SettingsHelpSection(R.string.help_strava_heading,R.string.help_strava_body),SettingsHelpSection(R.string.help_uploads_heading,R.string.help_uploads_body),SettingsHelpSection(R.string.help_backup_heading,R.string.help_backup_body))),
    BACKUP(R.string.data_backup,listOf(SettingsHelpSection(R.string.help_backup_heading,R.string.help_backup_body))),
    LOCAL(R.string.data_local,listOf(SettingsHelpSection(R.string.help_local_heading,R.string.help_local_body))),
    WEBDAV(R.string.data_cloud,listOf(SettingsHelpSection(R.string.help_webdav_heading,R.string.help_webdav_body),SettingsHelpSection(R.string.help_uploads_heading,R.string.help_uploads_body))),
    INTERVALS(R.string.intervals_title,listOf(SettingsHelpSection(R.string.help_intervals_heading,R.string.help_intervals_body),SettingsHelpSection(R.string.help_uploads_heading,R.string.help_uploads_body))),
    DREEVE(R.string.dreeve_title,listOf(SettingsHelpSection(R.string.help_dreeve_heading,R.string.help_dreeve_body),SettingsHelpSection(R.string.help_uploads_heading,R.string.help_uploads_body))),
    STRAVA(R.string.strava_title,listOf(SettingsHelpSection(R.string.help_strava_heading,R.string.help_strava_body),SettingsHelpSection(R.string.help_uploads_heading,R.string.help_uploads_body))),
    NOTIFICATIONS(R.string.notifications_title,listOf(SettingsHelpSection(R.string.help_notify_access_heading,R.string.help_notify_access_body),SettingsHelpSection(R.string.help_notify_apps_heading,R.string.help_notify_apps_body),SettingsHelpSection(R.string.help_notify_position_heading,R.string.help_notify_position_body))),
    ADVANCED(R.string.ui_advanced,listOf(SettingsHelpSection(R.string.help_diagnostics_heading,R.string.help_diagnostics_body),SettingsHelpSection(R.string.help_reset_heading,R.string.help_reset_body),SettingsHelpSection(R.string.help_sources_heading,R.string.help_sources_body),SettingsHelpSection(R.string.help_licenses_heading,R.string.help_licenses_body))),
    RECOVERY(R.string.ui_recovery_title,listOf(SettingsHelpSection(R.string.help_recovery_heading,R.string.help_recovery_body))),
    DIAGNOSTICS(R.string.ui_diagnostic_title,listOf(SettingsHelpSection(R.string.help_diagnostics_heading,R.string.help_diagnostics_body))),
    RESET(R.string.ui_reset_defaults_title,listOf(SettingsHelpSection(R.string.help_reset_heading,R.string.help_reset_body))),
    SOURCES(R.string.sources_title,listOf(SettingsHelpSection(R.string.help_sources_heading,R.string.help_sources_body))),
    LICENSES(R.string.help_licenses_title,listOf(SettingsHelpSection(R.string.help_licenses_heading,R.string.help_licenses_body))),
    GPS(R.string.ui_gps_help_title,listOf(SettingsHelpSection(R.string.help_gps_help_heading,R.string.help_gps_help_body),SettingsHelpSection(R.string.help_gps_heading,R.string.help_gps_body))),
    GENERAL(R.string.ui_settings,listOf(SettingsHelpSection(R.string.help_navigation_heading,R.string.help_navigation_body)))
}

/** Help is local read-only content. It uses the same navigation stack without rebuilding its parent. */
internal object SettingsHelp {
    private fun views(view:View):Sequence<View> = sequence {
        yield(view)
        if(view is ViewGroup) for(i in 0 until view.childCount) yieldAll(views(view.getChildAt(i)))
    }
    fun resolve(screen:AlertDialog):SettingsHelpPage? {
        val nodes=views(screen.window!!.decorView).toList()
        if(nodes.any { it.tag=="settings_help_page" }) return null
        when {
            nodes.any { it is RouteAnalysisView } -> return SettingsHelpPage.ROUTE_DETAILS
            nodes.any { it is RouteAnalysisMap } -> return SettingsHelpPage.ROUTE_MAP
            nodes.any { it is RouteManagerView } -> return SettingsHelpPage.ROUTES
            nodes.any { it is BikesView } -> return SettingsHelpPage.PROFILES
            nodes.any { it is RiderProfileView } -> return SettingsHelpPage.RIDER
            nodes.any { it is FieldPickerView } -> return SettingsHelpPage.FIELDS
        }
        val title=nodes.filterIsInstance<TextView>().firstOrNull { it.tag=="bicycle_dialog_title" }?.text?.toString()
        if(title==screen.context.getString(R.string.data_restore)) return SettingsHelpPage.BACKUP
        if(title in listOf(R.string.ui_font_license_title,R.string.ui_library_license_title).map(screen.context::getString)) return SettingsHelpPage.LICENSES
        return SettingsHelpPage.entries.firstOrNull { screen.context.getString(it.title)==title } ?: SettingsHelpPage.GENERAL
    }
    fun groups(page:SettingsHelpPage):List<SettingsHelpGroup> {
        val all=SettingsHelpPage.entries.flatMap { it.sections }.associateBy { it.heading }
        fun group(title:Int,vararg sections:Int)=SettingsHelpGroup(title,sections.map(all::getValue))
        return when(page) {
            SettingsHelpPage.RIDER->listOf(
                group(R.string.rider_personal,R.string.help_personal_heading),
                group(R.string.rider_training,R.string.help_heart_method_heading,R.string.help_karvonen_heading,R.string.help_maximum_heading,R.string.help_threshold_heading,R.string.help_ftp_heading),
                group(R.string.rider_zones,R.string.help_heart_zones_heading,R.string.help_zone_model_details_heading),
                group(R.string.training_power_zones,R.string.help_power_zones_heading))
            SettingsHelpPage.PROFILES->listOf(
                group(R.string.profiles_bike,R.string.help_profiles_heading,R.string.help_bike_tiles_heading,R.string.help_bike_name_photo_heading,R.string.help_bike_type_heading,R.string.help_profile_actions_heading),
                group(R.string.bikes_sensors,R.string.help_sensors_heading,R.string.help_gps_heading),
                group(R.string.indoor_correction_title,R.string.help_indoor_correction_heading),
                group(R.string.ui_recording_title,R.string.help_fit_sources_heading),
                group(R.string.ui_track_recording,R.string.help_record_fields_heading),
                group(R.string.ui_settings_map,R.string.help_map_sources_heading,R.string.help_map_controls_heading,R.string.help_map_lines_heading,R.string.help_elevation_heading),
                group(R.string.ui_design_title,R.string.help_design_heading,R.string.help_dashboards_heading,R.string.help_editor_heading,R.string.help_bosch_mode_heading,R.string.help_drink_reminder_heading),
                group(R.string.training_help_title,R.string.help_help_mode_heading,R.string.help_help_values_heading,R.string.help_zone_two_heading,R.string.help_heart_method_heading,R.string.help_karvonen_heading,R.string.help_maximum_heading,R.string.help_threshold_heading))
            SettingsHelpPage.DATA->listOf(
                group(R.string.data_global,R.string.help_backup_heading),
                group(R.string.data_routes,R.string.help_route_import_heading,R.string.help_local_heading,R.string.help_webdav_heading,R.string.help_intervals_heading,R.string.help_dreeve_heading,R.string.help_strava_heading,R.string.help_uploads_heading))
            SettingsHelpPage.DESIGN->listOf(
                group(R.string.ui_design_title,R.string.help_design_heading,R.string.help_dashboards_heading,R.string.help_editor_heading,R.string.help_bosch_mode_heading,R.string.help_drink_reminder_heading),
                group(R.string.training_help_title,R.string.help_help_mode_heading,R.string.help_help_values_heading,R.string.help_zone_two_heading,R.string.help_heart_method_heading))
            SettingsHelpPage.FIELDS->listOf(
                group(R.string.drink_reminder_title,R.string.help_drink_reminder_heading),
                group(R.string.help_field_data_heading,R.string.help_field_data_heading,R.string.help_batteries_heading,R.string.help_editor_heading),
                group(R.string.help_field_graph_heading,R.string.help_field_graph_heading),
                group(R.string.training_help_title,R.string.help_help_mode_heading,R.string.help_help_values_heading,R.string.help_zone_two_heading,R.string.help_heart_method_heading))
            SettingsHelpPage.NOTIFICATIONS->listOf(
                group(R.string.notifications_title,R.string.help_notify_access_heading,R.string.help_notify_position_heading),
                group(R.string.notifications_apps,R.string.help_notify_apps_heading))
            else->listOf(SettingsHelpGroup(page.title,page.sections))
        }
    }
    fun body(context:Context,themes:ThemeManager,page:SettingsHelpPage,source:View?=null):BicycleStickyScrollView {
        val content=BicycleComponents.dialogBody(context,themes).apply { tag="settings_help_page" }
        val sourceHeaders=source?.let { views(it).filterIsInstance<BicycleSectionHeader>().map { header->header.contentDescription.toString() }.toSet() }
        val profileName=source?.findViewWithTag<TextView>("bike_profile_name")?.text
        groups(page).filter { group ->
            page!=SettingsHelpPage.PROFILES || sourceHeaders==null || group.heading==R.string.profiles_bike || context.getString(group.heading) in sourceHeaders
        }.forEach { group ->
            val header=BicycleForm.section(content,themes,group.heading)
            if(page==SettingsHelpPage.PROFILES && group.heading==R.string.profiles_bike && profileName!=null) {
                header.titleView.text=profileName;header.contentDescription=profileName
            }
            val body=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;tag="help_group_${group.heading}" }
            group.sections.forEachIndexed { index,section ->
                body.addView(BicycleComponents.contentHeading(context,themes,context.getString(section.heading)),LinearLayout.LayoutParams(-1,-2).apply { if(index>0) topMargin=BicycleDesign.dp(context,BicycleDesign.sectionContentGap) })
                body.addView(BicycleComponents.description(context,themes,context.getString(section.body)).apply { tag="help_body_${section.body}" },
                    LinearLayout.LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            }
            content.addView(body,LinearLayout.LayoutParams(-1,-2))
            header.accordion(body,open=false)
        }
        return BicycleStickyScrollView(context).apply { addView(content) }
    }
    fun show(context:Context,themes:ThemeManager,navigation:DialogNavigation,page:SettingsHelpPage,source:View?=null) {
        BicycleDialogBuilder(context,themes,navigation,helpEnabled=false)
            .setTitle(context.getString(R.string.settings_help_title,context.getString(page.title)))
            .setView(body(context,themes,page,source)).show()
    }
}

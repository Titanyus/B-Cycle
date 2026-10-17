package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Semantic names keep UI independent of Lucide's asset names. Lucide 0.468.0, local vectors. */
internal enum class BicycleIcon(val resource: Int, val colorRole: ThemeRole = ThemeRole.FOREGROUND) {
    RecordReady(R.drawable.ic_record_ready), StopRecording(R.drawable.ic_record_stop),
    EditDashboard(R.drawable.ic_lucide_pencil),
    GpsSensor(R.drawable.ic_lucide_satellite_dish), PowerMeter(R.drawable.ic_lucide_zap), SunriseSensor(R.drawable.ic_lucide_sunrise), SunsetSensor(R.drawable.ic_lucide_sunset),
    Weather(R.drawable.ic_lucide_cloud_sun), Cloud(R.drawable.ic_lucide_cloud), Rain(R.drawable.ic_lucide_cloud_rain), Humidity(R.drawable.ic_lucide_droplet), Wind(R.drawable.ic_lucide_wind),
    Dreeve(R.drawable.ic_dreeve), Strava(R.drawable.ic_strava), WebDav(R.drawable.ic_lucide_server), Intervals(R.drawable.ic_lucide_activity),
    DataStorage(R.drawable.ic_lucide_database), RestoreSettings(R.drawable.ic_lucide_upload), Forward(R.drawable.ic_lucide_chevron_right),
    Battery(R.drawable.ic_lucide_battery), Phone(R.drawable.ic_lucide_smartphone),
    Notifications(R.drawable.ic_lucide_bell),
    EBike(R.drawable.ic_bike_electric), IndoorBike(R.drawable.ic_bike_indoor),
    Bikes(R.drawable.ic_lucide_bike), Sensors(R.drawable.ic_lucide_radio), MapManager(R.drawable.ic_lucide_map_pinned),
    Dashboard(R.drawable.ic_lucide_layout_dashboard), Route(R.drawable.ic_lucide_route),
    RiderProfile(R.drawable.ic_lucide_user_round), Advanced(R.drawable.ic_lucide_settings), Light(R.drawable.ic_lucide_lightbulb),
    TrackImported(R.drawable.ic_lucide_log_in), TrackRecorded(R.drawable.ic_lucide_bike),
    TrackUnknown(R.drawable.ic_lucide_circle_help), FileGPX(R.drawable.ic_lucide_file), FileFIT(R.drawable.ic_lucide_file),
    MoreOptions(R.drawable.ic_lucide_ellipsis), SettingsOptions(R.drawable.ic_lucide_ellipsis_vertical),
    ShowOnMap(R.drawable.ic_lucide_eye), ActiveOnMap(R.drawable.ic_lucide_eye, ThemeRole.ACCENT), HideOnMap(R.drawable.ic_lucide_eye_closed),
    Locate(R.drawable.ic_lucide_locate_fixed), Compass(R.drawable.ic_lucide_compass), OvalFollow(R.drawable.ic_map_oval_follow), HeadingUp(R.drawable.ic_lucide_arrow_up_from_dot), Maximize(R.drawable.ic_lucide_maximize), Minimize(R.drawable.ic_lucide_minimize), Sun(R.drawable.ic_lucide_sun), Moon(R.drawable.ic_lucide_moon), CloudUploaded(R.drawable.ic_lucide_cloud_upload, ThemeRole.ACCENT),
    CloudConfirmed(R.drawable.ic_lucide_cloud_check), SaveConfirmed(R.drawable.ic_lucide_save_check),
    CloudNotUploaded(R.drawable.ic_lucide_cloud_upload), CloudFailed(R.drawable.ic_lucide_cloud_alert), LocalSaved(R.drawable.ic_lucide_hard_drive_download, ThemeRole.ACCENT),
    LocalNotSaved(R.drawable.ic_lucide_hard_drive_download), DeleteTrack(R.drawable.ic_lucide_trash_2),
    RenameTrack(R.drawable.ic_lucide_pencil), CorrectElevation(R.drawable.ic_lucide_mountain), ExportTrack(R.drawable.ic_lucide_download),
    Cancel(R.drawable.ic_lucide_x), Close(R.drawable.ic_lucide_log_out), Back(R.drawable.ic_lucide_chevron_left), Import(R.drawable.ic_lucide_download),
    Date(R.drawable.ic_lucide_calendar_days), Distance(R.drawable.ic_lucide_chart_no_axes_combined),
    Duration(R.drawable.ic_lucide_clock), Ascent(R.drawable.ic_lucide_mountain),
    Bluetooth(R.drawable.ic_lucide_bluetooth), Disconnect(R.drawable.ic_lucide_unplug),
    Map(R.drawable.ic_lucide_map), Save(R.drawable.ic_lucide_save), Folder(R.drawable.ic_lucide_folder_open),
    Reset(R.drawable.ic_lucide_rotate_ccw), AddSensor(R.drawable.ic_lucide_plus),
    Connection(R.drawable.ic_lucide_link), Info(R.drawable.ic_lucide_info),
    ZoomIn(R.drawable.ic_lucide_plus), ZoomOut(R.drawable.ic_lucide_minus),
    Speed(R.drawable.ic_lucide_gauge), Power(R.drawable.ic_lucide_zap), Heart(R.drawable.ic_lucide_heart),
    Cadence(R.drawable.ic_lucide_gauge), Work(R.drawable.ic_lucide_settings), Calories(R.drawable.ic_lucide_flame), SwitchMetric(R.drawable.ic_lucide_repeat_2),
    Expand(R.drawable.ic_lucide_chevron_down), Loading(R.drawable.ic_lucide_loader_circle)
}

internal object BicycleIcons {
    // Only positive semantic status icons opt into ACCENT. Negative/inactive states remain standard.
    fun color(tokens: ThemeTokens, icon: BicycleIcon, negative: Boolean = false,
              backgroundRole: ThemeRole = ThemeRole.SECONDARY): Int =
        tokens.iconColor(if (negative) ThemeRole.FOREGROUND else icon.colorRole, backgroundRole)
    fun drawable(context: Context, icon: BicycleIcon, tokens: ThemeTokens, negative: Boolean = false): Drawable =
        drawable(context, icon, color(tokens, icon, negative), negative)
    fun drawable(context: Context, icon: BicycleIcon, foreground: Int, negative: Boolean = false): Drawable {
        // Eyes use Lucide's dedicated closed state; other status icons retain the shared slash.
        val closedEye = negative && (icon == BicycleIcon.ShowOnMap || icon == BicycleIcon.ActiveOnMap)
        val vector = requireNotNull(context.getDrawable(if (closedEye) BicycleIcon.HideOnMap.resource else icon.resource)).mutate()
        return (if (negative && !closedEye) SlashedIcon(vector) else vector).apply { setTint(foreground) }
    }
}

/** One shared 24-unit diagonal overlay, same weight/caps as Lucide, tinted by the active palette. */
private class SlashedIcon(private val original: Drawable) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f; strokeCap = Paint.Cap.ROUND }
    override fun draw(canvas: Canvas) {
        original.bounds = bounds; original.draw(canvas)
        canvas.save(); canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        canvas.drawLine(3f, 3f, 21f, 21f, paint); canvas.restore()
    }
    override fun setTint(tintColor: Int) { original.setTint(tintColor); paint.color = tintColor; invalidateSelf() }
    override fun setAlpha(alpha: Int) { original.alpha = alpha; paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { original.colorFilter = colorFilter; paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth() = original.intrinsicWidth
    override fun getIntrinsicHeight() = original.intrinsicHeight
}

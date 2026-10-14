package de.bcycle.app

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/** One form draft, validated by the existing profile model before every persistent edit. */
internal class ComposeProfileDraft(initial:RiderProfile,private val store:RiderProfileStore,private val locale:java.util.Locale) {
    var birth by mutableStateOf(initial.birthDate)
    var fields by mutableStateOf(mapOf(RiderField.HEIGHT to initial.heightCm?.toString().orEmpty(),
        RiderField.WEIGHT to initial.weightKg?.let { DecimalFormat("0.################",DecimalFormatSymbols(locale)).format(it) }.orEmpty(),
        RiderField.RESTING_HEART_RATE to initial.restingHeartRate?.toString().orEmpty(),RiderField.MAXIMUM_HEART_RATE to initial.maximumHeartRate?.toString().orEmpty(),RiderField.FTP to initial.ftpW?.toString().orEmpty()))
    var failed by mutableStateOf(false)
    fun validation()=RiderProfileValidation.parse(birth,fields,locale)
    fun persist():Boolean {
        val result=validation();if(!result.valid) return false
        return runCatching { store.write(result.profile) }.getOrDefault(false).also { failed=!it }
    }
    fun edit(field:RiderField,text:String) { fields=fields+(field to text);if(text.isNotBlank() && !text.endsWith('.') && !text.endsWith(',')) persist() }
}
@Composable internal fun ComposeRiderProfile(store:RiderProfileStore,datePicker:(LocalDate?,(LocalDate)->Unit)->Unit) {
    val context=LocalContext.current;val locale=context.resources.configuration.locales[0]
    val draft=remember(store) { ComposeProfileDraft(store.read(),store,locale) }
    DisposableEffect(draft) { onDispose { draft.persist() } }
    val validation=draft.validation()
    val labels=mapOf(RiderField.HEIGHT to R.string.rider_height,RiderField.WEIGHT to R.string.rider_weight,RiderField.RESTING_HEART_RATE to R.string.rider_resting,RiderField.MAXIMUM_HEART_RATE to R.string.rider_maximum,RiderField.FTP to R.string.rider_ftp)
    @Composable fun input(field:RiderField) {
        BicycleText(stringResource(labels.getValue(field)))
        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                BicycleInput(stringResource(labels.getValue(field)),draft.fields[field].orEmpty(),"rider_${field.name.lowercase()}",
                    KeyboardOptions(keyboardType=if(field==RiderField.WEIGHT) KeyboardType.Decimal else KeyboardType.Number),
                    error=if(field in validation.errors) context.getString(R.string.rider_hint_invalid) else null,onBlur={ draft.persist() }) { draft.edit(field,it) }
            }
            val unit=context.getString(when(field) { RiderField.HEIGHT->R.string.rider_cm;RiderField.WEIGHT->R.string.rider_kg;RiderField.FTP->R.string.analysis_watts;else->R.string.analysis_bpm })
            androidx.compose.foundation.text.BasicText(unit,Modifier.width(48.dp),style=androidx.compose.ui.text.TextStyle(color=androidx.compose.ui.graphics.Color(BicycleDesign.dashboardUnitColor),fontSize=BicycleDesign.formatSp.sp))
        }
    }
    BicycleBody {
        BicycleSection(stringResource(R.string.rider_personal))
        BicycleText(stringResource(R.string.rider_birth))
        BicycleAction(draft.birth?.let { RiderProfileView.formatBirthDate(it,locale) } ?: stringResource(R.string.rider_birth_choose),BicycleIcon.Date,tag="rider_birth") {
            datePicker(draft.birth) { draft.birth=it;draft.persist() }
        }
        if(draft.birth!=null) {
            BicycleAction(stringResource(R.string.rider_birth_clear),BicycleIcon.Cancel,tag="rider_birth_clear") { draft.birth=null;draft.persist() }
            draft.birth?.let { RiderProfile(birthDate=it).ageYears() }?.let { BicycleText(context.resources.getQuantityString(R.plurals.rider_age,it,it)) }
        }
        input(RiderField.HEIGHT);input(RiderField.WEIGHT)
        BicycleDivider();BicycleSection(stringResource(R.string.rider_training))
        input(RiderField.RESTING_HEART_RATE);input(RiderField.MAXIMUM_HEART_RATE);input(RiderField.FTP)
        BicycleDivider();BicycleSection(stringResource(R.string.rider_zones));BicycleText(stringResource(R.string.rider_karvonen))
        val hearts=setOf(RiderField.RESTING_HEART_RATE,RiderField.MAXIMUM_HEART_RATE)
        val zones=if(validation.errors.keys.any { it in hearts }) null else validation.profile.heartZones()
        if(zones==null) BicycleText(context.getString(when {
            validation.errors[RiderField.MAXIMUM_HEART_RATE]==RiderInputError.HEART_ORDER -> R.string.rider_hint_order
            validation.errors.keys.any { it in hearts } -> R.string.rider_hint_invalid
            validation.profile.restingHeartRate==null && validation.profile.maximumHeartRate==null -> R.string.rider_hint_both
            validation.profile.restingHeartRate==null -> R.string.rider_hint_resting
            else -> R.string.rider_hint_maximum
        })) else zones.zones.forEachIndexed { i,zone ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                BicycleText(context.getString(R.string.rider_zone_name,i+1,context.getString(listOf(R.string.rider_z1,R.string.rider_z2,R.string.rider_z3,R.string.rider_z4,R.string.rider_z5)[i])))
                Row(Modifier.width(BicycleDesign.riderZoneRangeWidth.dp).height(BicycleDesign.touchSize.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                    val font=androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.bebas_neue))
                    androidx.compose.foundation.text.BasicText(if(zone.isEmpty) context.getString(R.string.analysis_missing) else context.getString(R.string.rider_zone_range,zone.lowerBpm,zone.upperBpm),style=androidx.compose.ui.text.TextStyle(fontFamily=font,fontSize=BicycleDesign.analysisMetricSp.sp,color=androidx.compose.ui.graphics.Color(LocalBicycleColors.current.foreground)))
                    Spacer(Modifier.width(4.dp))
                    androidx.compose.foundation.text.BasicText(context.getString(R.string.analysis_bpm),style=androidx.compose.ui.text.TextStyle(fontFamily=font,fontSize=BicycleDesign.formatSp.sp,color=androidx.compose.ui.graphics.Color(BicycleDesign.dashboardUnitColor)))
                }
            }
        }
        if(draft.failed) BicycleText(stringResource(R.string.rider_save_failed))
    }
}

package de.bcycle.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Valid edits persist automatically; incomplete input never overwrites the last valid profile. */
internal class RiderProfileView(context: Context, private val themes: ThemeManager, initial: RiderProfile,
    private val navigation: DialogNavigation, private val today: () -> LocalDate = LocalDate::now) : BicycleStickyScrollView(context) {
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
    private val locale get() = resources.configuration.locales[0]
    var persist: ((RiderProfile) -> Boolean)? = null
    private fun persistValid() {
        val value=draft()
        if(value.valid && persist?.invoke(value.profile)==false) saveFailed()
    }
    private var birthDate = initial.birthDate
    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding))
        BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
    }
    private val inputs = linkedMapOf<RiderField, EditText>()
    private val birth = Button(context).apply { tag = "rider_birth"; isAllCaps = false }
    private val clearBirth = BicycleComponents.icon(context, themes, BicycleIcon.Cancel, context.getString(R.string.rider_birth_clear)) {
        birthDate = null; birth.error = null; updateBirth(); persistValid()
    }.apply { tag = "rider_birth_clear" }
    private val age = text("").apply { tag = "rider_age" }
    private val hint = text("").apply { tag = "rider_zone_hint"; accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE }
    private val powerRanges = mutableListOf<TextView>()
    private val powerHint = text("").apply { tag="rider_power_zone_hint" }
    private val powerRows = LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; tag="rider_power_zones" }
    private val ranges = mutableListOf<TextView>()
    private val zoneRows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; tag = "rider_zones" }
    private val saveError = text("").apply { tag = "rider_save_error"; visibility = GONE; accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE }
    private fun text(value: String) = BicycleComponents.text(context, themes, value).apply { typeface = Typeface.DEFAULT }
    private fun section(title: Int) {
        BicycleForm.section(root,themes,title)
    }

    private fun label(title: Int, target: View) {
        root.addView(text(context.getString(title)).apply { labelFor = target.id }, LinearLayout.LayoutParams(-1, -2))
    }
    private fun input(field: RiderField, title: Int, unit: Int, value: String, decimal: Boolean = false) {
        val edit = EditText(context).apply {
            id = generateViewId(); tag = "rider_${field.name.lowercase(Locale.ROOT)}"; contentDescription = context.getString(title)
            inputType = InputType.TYPE_CLASS_NUMBER or if (decimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0
            maxLines = 1; setText(value)
        }
        inputs[field] = edit
        root.addView(BicycleForm.numberRow(edit,themes,title,unit,"rider_unit"),
            LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap); bottomMargin=dp(BicycleDesign.verticalPadding) })
    }

    init {
        tag = "rider_profile"; BicycleComponents.scrollStyle(this)
        BicycleComponents.bindTheme(this, themes) {
            setBackgroundColor(it.secondary); verticalScrollbarThumbDrawable = android.graphics.drawable.ColorDrawable(it.foreground)
        }
        addView(root, LayoutParams(-1, -2))
        section(R.string.rider_personal)
        birth.id = generateViewId(); birth.contentDescription = context.getString(R.string.rider_birth)
        BicycleForm.outlinedInput(birth,themes,numeric=true)
        birth.stateListAnimator=null;birth.elevation=0f
        birth.minWidth=0;birth.minimumWidth=0
        root.addView(LinearLayout(context).apply {
            tag=BicycleDesign.inlineControlRowTag
            gravity = Gravity.CENTER_VERTICAL
            addView(text(context.getString(R.string.rider_birth)).apply { labelFor=birth.id },
                LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(BicycleDesign.controlGap) })
            addView(birth, LinearLayout.LayoutParams(dp(BicycleDesign.formDateWidth), dp(BicycleDesign.touchSize)))
            addView(clearBirth)
        }, LinearLayout.LayoutParams(-1, -2))
        age.gravity=Gravity.END
        age.setPadding(0,0,dp(BicycleDesign.touchSize)+birth.paddingEnd,0)
        root.addView(age, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(BicycleDesign.verticalPadding) })
        birth.setOnClickListener { chooseBirthDate() }; updateBirth()
        input(RiderField.HEIGHT, R.string.rider_height, R.string.rider_cm, initial.heightCm?.toString().orEmpty())
        val decimal = DecimalFormat("0.################", DecimalFormatSymbols.getInstance(locale)).apply { isGroupingUsed = false }
        input(RiderField.WEIGHT, R.string.rider_weight, R.string.rider_kg, initial.weightKg?.let(decimal::format).orEmpty(), true)
        section(R.string.rider_training)
        input(RiderField.RESTING_HEART_RATE, R.string.rider_resting, R.string.analysis_bpm, initial.restingHeartRate?.toString().orEmpty())
        input(RiderField.MAXIMUM_HEART_RATE, R.string.rider_maximum, R.string.analysis_bpm, initial.maximumHeartRate?.toString().orEmpty())
        input(RiderField.FTP, R.string.rider_ftp, R.string.analysis_watts, initial.ftpW?.toString().orEmpty())
        section(R.string.rider_zones)
        root.addView(text(context.getString(R.string.rider_karvonen)), LinearLayout.LayoutParams(-1, -2))
        root.addView(hint, LinearLayout.LayoutParams(-1, -2))
        val names = listOf(R.string.rider_z1, R.string.rider_z2, R.string.rider_z3, R.string.rider_z4, R.string.rider_z5)
        names.forEachIndexed { i, name ->
            zoneRows.addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(BicycleDesign.touchSize)
                addView(View(context).apply { setBackgroundColor(TrainingZoneColors.values[i]); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO },
                    LinearLayout.LayoutParams(dp(BicycleDesign.smallGap), dp(BicycleDesign.iconSize)).apply { marginEnd = dp(BicycleDesign.verticalPadding) })
                addView(text(context.getString(R.string.rider_zone_name, i + 1, context.getString(name))).apply {
                    maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(0, -2, 1f))
                val value = BicycleComponents.text(context, themes, "").apply {
                    tag = "rider_zone_value_${i + 1}"; typeface = resources.getFont(R.font.bebas_neue)
                    textSize = BicycleDesign.settingsMetricSp; gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
                    setAutoSizeTextTypeUniformWithConfiguration(12, BicycleDesign.settingsMetricSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
                }
                val unit = TextView(context).apply {
                    text = context.getString(R.string.analysis_bpm); tag = "rider_unit"; typeface = resources.getFont(R.font.bebas_neue)
                    textSize = BicycleDesign.settingsUnitSp; BicycleComponents.secondaryText(this,themes)
                    gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
                }
                ranges += value
                addView(AnalysisValueRow(context, value, unit), LinearLayout.LayoutParams(dp(BicycleDesign.riderZoneRangeWidth), dp(BicycleDesign.touchSize)))
            }, LinearLayout.LayoutParams(-1, -2))
        }
        root.addView(zoneRows, LinearLayout.LayoutParams(-1, -2))
        section(R.string.training_power_zones)
        root.addView(text(context.getString(R.string.training_coggan)))
        root.addView(powerHint)
        val powerNames=listOf(R.string.training_power_z1,R.string.training_power_z2,R.string.training_power_z3,R.string.training_power_z4,R.string.training_power_z5,R.string.training_power_z6,R.string.training_power_z7)
        powerNames.forEachIndexed { i,name ->
            powerRows.addView(LinearLayout(context).apply {
                gravity=Gravity.CENTER_VERTICAL;minimumHeight=dp(BicycleDesign.touchSize)
                addView(View(context).apply { setBackgroundColor(ZoneGlowColors.powerColors[i]);importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },
                    LinearLayout.LayoutParams(dp(BicycleDesign.smallGap),dp(BicycleDesign.iconSize)).apply { marginEnd=dp(BicycleDesign.verticalPadding) })
                addView(text(context.getString(R.string.rider_zone_name,i+1,context.getString(name))).apply { maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END },LinearLayout.LayoutParams(0,-2,1f))
                val value=BicycleComponents.text(context,themes,"").apply {
                    tag="rider_power_zone_value_${i+1}";typeface=resources.getFont(R.font.bebas_neue);textSize=BicycleDesign.settingsMetricSp
                    gravity=Gravity.CENTER;includeFontPadding=false;maxLines=1
                    setAutoSizeTextTypeUniformWithConfiguration(12,BicycleDesign.settingsMetricSp.toInt(),1,android.util.TypedValue.COMPLEX_UNIT_SP)
                }
                val unit=TextView(context).apply { text=context.getString(R.string.analysis_watts);tag="rider_unit";typeface=resources.getFont(R.font.bebas_neue)
                    textSize=BicycleDesign.settingsUnitSp;BicycleComponents.secondaryText(this,themes);gravity=Gravity.CENTER;includeFontPadding=false;maxLines=1 }
                powerRanges+=value
                addView(AnalysisValueRow(context,value,unit),LinearLayout.LayoutParams(dp(BicycleDesign.riderZoneRangeWidth),dp(BicycleDesign.touchSize)))
            },LinearLayout.LayoutParams(-1,-2))
        }
        root.addView(powerRows);root.addView(saveError)
        inputs.forEach { (field, edit) -> edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                edit.error = null; saveError.visibility = GONE
                if (field == RiderField.RESTING_HEART_RATE || field == RiderField.MAXIMUM_HEART_RATE || field == RiderField.FTP) updateZones()
            }
            override fun afterTextChanged(s: Editable?) {
                val value=s?.toString().orEmpty()
                if(value.isNotBlank() && !value.endsWith(",") && !value.endsWith(".")) persistValid()
            }
        })
            edit.setOnFocusChangeListener { _,focused -> if(!focused) persistValid() }
        }
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) { persistValid() }
        })
        groupSections()
        updateZones()
    }
    private fun groupSections() {
        val nodes=(0 until root.childCount).map(root::getChildAt).filter { it!==saveError }
        root.removeAllViews()
        var body:LinearLayout?=null
        nodes.forEach { node ->
            if(node is BicycleSectionHeader) {
                root.addView(node,node.layoutParams)
                body=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;tag="rider_section_${node.contentDescription}" }
                root.addView(body,LinearLayout.LayoutParams(-1,-2))
                node.accordion(body!!)
            } else body?.addView(node,node.layoutParams)
        }
        root.addView(saveError)
    }
    private fun updateBirth() {
        birth.text = birthDate?.let { formatBirthDate(it, locale) } ?: context.getString(R.string.rider_birth_choose)
        clearBirth.isEnabled = birthDate != null
        val years = birthDate?.let { RiderProfile(birthDate = it).ageYears(today()) }
        age.text = years?.let { resources.getQuantityString(R.plurals.rider_age, it, it) }.orEmpty()
        age.visibility = if (years == null) GONE else VISIBLE
    }
    private fun chooseBirthDate() {
        val initial = (birthDate ?: today()).coerceIn(RiderProfileValidation.minimumBirthDate(today()), today())
        val pickerContext=if(themes.isSettingsPalette && themes.settingsLight)
            android.view.ContextThemeWrapper(context,R.style.SettingsLightDatePickerTheme) else context
        val picker = DatePicker(pickerContext).apply {
            tag = "rider_birth_picker"; maxDate = today().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            minDate = RiderProfileValidation.minimumBirthDate(today()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            updateDate(initial.year, initial.monthValue - 1, initial.dayOfMonth)
        }
        var ready=false
        picker.init(initial.year,initial.monthValue-1,initial.dayOfMonth) { _,year,month,day ->
            if(ready) { birthDate=LocalDate.of(year,month+1,day);birth.error=null;updateBirth();persistValid() }
        }
        ready=true
        BicycleDialogBuilder(context,themes,navigation,fullscreen=false).setTitle(R.string.rider_birth).setView(picker).show()
    }
    internal fun draft() = RiderProfileValidation.parse(birthDate, inputs.mapValues { it.value.text.toString() }, locale, today())
    private fun updateZones() {
        val draft = draft()
        val zones=if(RiderField.FTP in draft.errors) null else draft.profile.powerZones()
        powerHint.visibility=if(zones==null) VISIBLE else GONE;powerRows.visibility=if(zones==null) GONE else VISIBLE
        powerHint.text=context.getString(R.string.training_ftp_required)
        zones?.forEachIndexed { i,zone ->
            powerRanges[i].text=when {
                zone.isEmpty -> context.getString(R.string.analysis_missing)
                zone.upperW==null -> context.getString(R.string.training_power_open,zone.lowerW)
                else -> context.getString(R.string.rider_zone_range,zone.lowerW,zone.upperW)
            }
            (powerRanges[i].parent.parent as View).contentDescription=context.getString(R.string.training_power_description,zone.index,powerRanges[i].text)
        }
        val hearts = setOf(RiderField.RESTING_HEART_RATE, RiderField.MAXIMUM_HEART_RATE)
        val result = if (draft.errors.keys.any { it in hearts }) null else draft.profile.heartZones()
        hint.visibility = if (result == null) VISIBLE else GONE; zoneRows.visibility = if (result == null) GONE else VISIBLE
        if (result != null) inputs.filterKeys { it in hearts }.values.forEach { it.error = null }
        if (result == null) {
            hint.text = context.getString(when {
                draft.errors[RiderField.MAXIMUM_HEART_RATE] == RiderInputError.HEART_ORDER -> R.string.rider_hint_order
                draft.errors.keys.any { it in hearts } -> R.string.rider_hint_invalid
                draft.profile.restingHeartRate == null && draft.profile.maximumHeartRate == null -> R.string.rider_hint_both
                draft.profile.restingHeartRate == null -> R.string.rider_hint_resting
                else -> R.string.rider_hint_maximum
            })
        } else result.zones.forEachIndexed { i, zone ->
            ranges[i].text = if (zone.isEmpty) context.getString(R.string.analysis_missing) else context.getString(R.string.rider_zone_range, zone.lowerBpm, zone.upperBpm)
            (ranges[i].parent.parent as View).contentDescription = context.getString(R.string.rider_zone_description, zone.index,
                zone.fromPercent, zone.toPercent, ranges[i].text, context.getString(R.string.analysis_bpm))
        }
    }
    internal fun validate(): RiderProfile? {
        val draft = draft()
        draft.errors.forEach { (field, error) ->
            val target: TextView = if (field == RiderField.BIRTH_DATE) birth else inputs.getValue(field)
            target.error = context.getString(when (error) {
                RiderInputError.NUMBER -> if (field == RiderField.WEIGHT) R.string.rider_error_decimal else R.string.rider_error_integer
                RiderInputError.FUTURE_DATE -> R.string.rider_error_future
                RiderInputError.BIRTH_DATE -> R.string.rider_error_birth
                RiderInputError.HEART_ORDER -> R.string.rider_hint_order
                RiderInputError.RANGE -> when (field) {
                    RiderField.HEIGHT -> R.string.rider_error_height; RiderField.WEIGHT -> R.string.rider_error_weight
                    RiderField.RESTING_HEART_RATE -> R.string.rider_error_resting; RiderField.MAXIMUM_HEART_RATE -> R.string.rider_error_maximum
                    else -> R.string.rider_error_ftp
                }
            })
        }
        draft.errors.keys.firstOrNull()?.let { (inputs[it] ?: birth).requestFocus() }
        return draft.profile.takeIf { draft.valid }
    }
    internal fun saveFailed() { saveError.text = context.getString(R.string.rider_save_failed); saveError.visibility = VISIBLE }
    companion object {
        fun formatBirthDate(date: LocalDate, locale: Locale): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
}

/** Semantic intensity markers, independent of themes and never applied to text or icons. */
internal object TrainingZoneColors {
    val values = listOf(0xff8696a5.toInt(), 0xff66bb6a.toInt(), 0xffffca28.toInt(), 0xffff8a26.toInt(), 0xffef5350.toInt())
}

internal fun showRiderProfile(context: Context, themes: ThemeManager, navigation: DialogNavigation, store: RiderProfileStore): AlertDialog {
    val body = RiderProfileView(context, themes, store.read(), navigation)
    val screen = BicycleDialogBuilder(context, themes, navigation).setTitle(R.string.rider_profile).setView(body)
        .create()
    body.persist = { profile -> runCatching { store.write(profile) }.getOrDefault(false) }
    screen.show()
    return screen
}

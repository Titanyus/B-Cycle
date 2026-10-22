package de.bcycle.app

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.*
import org.json.JSONObject

/** Read-only preview first; only the explicit import action writes on the HRV worker. */
internal class HrvCsvImportView(context:Context,private val themes:ThemeManager,private val controller:HrvController,
    private val uri:Uri):LinearLayout(context) {
    private var generation=0
    init {
        orientation=VERTICAL;tag="hrv4_import_view"
        val inset=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(inset,0,inset,0)
        load(null)
    }
    private fun message(text:String)=BicycleComponents.description(context,themes,text).also { addView(it) }
    private fun load(order:HrvCsvDateOrder?) {
        val request=++generation;removeAllViews();message(context.getString(R.string.hrv4_loading))
        controller.previewHrv4Training(uri,order) { result ->
            if(request!=generation || !isAttachedToWindow) return@previewHrv4Training
            removeAllViews()
            result.fold({ preview -> show(preview) },{ error ->
                message(context.getString((error as? HrvCsvException)?.messageId ?: R.string.hrv4_failed))
            })
        }
    }
    private fun show(preview:HrvCsvPreview) {
        message(context.getString(R.string.hrv4_preview,preview.measurements,preview.contextDays,preview.invalidRecords.size))
        if(preview.days.isNotEmpty()) {
            val format=java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
            message(context.getString(R.string.hrv4_range,java.time.LocalDate.parse(preview.days.first()).format(format),
                java.time.LocalDate.parse(preview.days.last()).format(format)))
        }
        message(context.getString(R.string.hrv4_zone,preview.zone))
        if(preview.dateOrderAssumed) message(context.getString(R.string.hrv4_ambiguous))
        addView(BicycleComponents.fieldLabel(context,themes,context.getString(R.string.hrv4_date_label)))
        val order=Spinner(context).apply {
            tag="hrv4_date_order";contentDescription=context.getString(R.string.hrv4_date_label)
            adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,HrvCsvDateOrder.entries.map { context.getString(it.title) })
            setSelection(preview.dateOrder.ordinal)
            BicycleDialogStyle.spinner(this,themes)
            onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent:AdapterView<*>)=Unit
                override fun onItemSelected(parent:AdapterView<*>,view:View?,position:Int,id:Long) {
                    val next=HrvCsvDateOrder.entries[position];if(next!=preview.dateOrder) load(next)
                }
            }
        }
        addView(order)
        if(preview.invalidRecords.isNotEmpty()) message(context.getString(R.string.hrv4_invalid_rows,
            preview.invalidRecords.take(8).joinToString(", ")))
        message(context.getString(R.string.hrv4_original_note))
        val status=BicycleFeedback.status(context,themes).apply { tag="hrv4_import_status" }
        lateinit var start:Button
        start=BicycleComponents.action(context,themes,context.getString(R.string.hrv4_start)) {
            start.isEnabled=false;order.isEnabled=false
            BicycleFeedback.update(status,context.getString(R.string.hrv4_importing))
            val request=generation
            controller.importHrv4Training(preview) { result ->
                if(request==generation && isAttachedToWindow) {
                    result.fold({ report -> BicycleFeedback.update(status,context.getString(R.string.hrv4_done,
                        report.imported,report.duplicates,report.invalid,report.failed)) },{
                        BicycleFeedback.update(status,context.getString(R.string.hrv4_failed))
                    })
                }
            }
        }.apply { tag="hrv4_import_start";isEnabled=preview.entries.isNotEmpty() }
        addView(start);addView(status)
    }
    override fun onDetachedFromWindow() { generation++;super.onDetachedFromWindow() }
}

internal fun hrv4TrainingResult(context:Context,themes:ThemeManager,row:JSONObject)=LinearLayout(context).apply {
    orientation=LinearLayout.VERTICAL;tag="hrv4_imported_result"
    fun text(value:String)=addView(BicycleComponents.description(context,themes,value))
    addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.hrv4_result)))
    val external=row.getJSONObject("external");val metrics=external.getJSONObject("metrics")
    text(context.getString(R.string.hrv4_origin_date,java.time.LocalDate.parse(external.getString("day")).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        external.optString("localTime").takeIf { it.isNotEmpty() && it!="null" } ?: context.getString(R.string.hrv4_no_time)))
    text(context.getString(R.string.hrv4_original_note))
    if(metrics.length()==0) text(context.getString(R.string.hrv4_no_measurement))
    for((key,label) in listOf("rmssdMs" to R.string.hrv4_rmssd,"sdnnMs" to R.string.hrv4_sdnn,"meanNnMs" to R.string.hrv4_avnn,
        "pnn50Percent" to R.string.hrv4_pnn50,"restingHrBpm" to R.string.hrv4_hr))
        if(metrics.has(key)) text(context.getString(label,metrics.getDouble(key)))
    addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.hrv4_original_fields)))
    val source=external.getJSONObject("originalColumns")
    val labels=mapOf("LF" to R.string.hrv4_lf,"HF" to R.string.hrv4_hf,"HRV4T_Recovery_Points" to R.string.hrv4_score,
        "signal_quality" to R.string.hrv4_signal_quality,"training" to R.string.hrvq_training_setting,
        "trainingRPE" to R.string.hrvq_rpe,"trainingMotivation" to R.string.hrvq_motivation,"training_phase" to R.string.hrvq_phase,
        "trainingDistance" to R.string.hrv4_training_meters,"trainingTime" to R.string.hrv4_training_seconds,
        "sleep_quality" to R.string.hrvq_sleep,"sleep_time" to R.string.hrv4_sleep_hours,"sleep_awake" to R.string.hrvq_wake,
        "sleep_tobed" to R.string.hrvq_bed,"mental_energy" to R.string.hrvq_energy,"muscle_soreness" to R.string.hrvq_soreness,
        "fatigue" to R.string.hrvq_fatigue,"lifestyle" to R.string.hrvq_lifestyle,"note" to R.string.hrvq_notes,
        "physical_condition" to R.string.hrvq_condition,"traveling" to R.string.hrvq_traveled,"sickness" to R.string.hrvq_sick,
        "alcohol" to R.string.hrvq_alcohol)
    val already=setOf("date","local","time","HR","AVNN","SDNN","rMSSD","pNN50")
    source.keys().asSequence().filter { it !in already }.sorted().forEach { key ->
        source.getString(key).takeIf(String::isNotBlank)?.let { value ->
            text(context.getString(R.string.hrv4_pair,labels[key]?.let(context::getString) ?: key,value))
        }
    }
}

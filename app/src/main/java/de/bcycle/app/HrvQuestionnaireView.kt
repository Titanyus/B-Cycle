package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Flexible person-level form. Untouched centered sliders are missing answers, not neutral answers. */
internal class HrvQuestionnaireView(context:Context,private val themes:ThemeManager,initial:HrvQuestionnaire,
    private val training:HrvTrainingSummary?,private val save:(HrvQuestionnaire,(Boolean)->Unit)->Unit):LinearLayout(context) {
    private val settings=HrvQuestionSettings(context)
    private var value=initial
    private var config=settings.read()
    private var cycleAvailable=settings.cycleAvailable()
    private val riderPrefs=context.getSharedPreferences("bike",0)
    private val riderListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,key-> if(key==RiderProfileStore.key) handler.post {
        val next=settings.cycleAvailable();if(next!=cycleAvailable) { cycleAvailable=next;build() }
    } }
    private val handler=Handler(Looper.getMainLooper())
    private var pending=false
    private var revision=0
    private val status=BicycleFeedback.status(context,themes).apply { tag="hrvq_save_status";visibility=GONE }
    private val retry=BicycleComponents.action(context,themes,context.getString(R.string.hrvq_retry)) { flush() }.apply { visibility=GONE }
    private val persist=Runnable { flush() }
    private val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,_->
        handler.post { val next=settings.read();if(next!=config) { config=next;build() } }
    }
    init { orientation=VERTICAL;tag="hrv_questionnaire";val pad=dp(BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad);build() }
    private fun change(next:HrvQuestionnaire) {
        value=next.copy(updatedAtMs=System.currentTimeMillis());pending=true;revision++
        status.visibility=VISIBLE;BicycleFeedback.update(status,context.getString(R.string.hrvq_saving));retry.visibility=GONE
        handler.removeCallbacks(persist);handler.postDelayed(persist,250)
    }
    fun flush() {
        handler.removeCallbacks(persist)
        if(!pending) return
        pending=false;val savedRevision=revision
        save(value) { ok ->
            if(savedRevision==revision) {
                pending=!ok
                BicycleFeedback.update(status,context.getString(if(ok) R.string.hrvq_saved else R.string.hrvq_save_failed))
                retry.visibility=if(ok) GONE else VISIBLE
            }
        }
    }
    private fun label(parent:LinearLayout,text:String) { parent.addView(BicycleComponents.text(context,themes,text)) }
    private fun label(parent:LinearLayout,id:Int)=label(parent,context.getString(id))
    private fun enabled(question:HrvQuestion)=question in config.enabled && (question!=HrvQuestion.CYCLE || cycleAvailable)
    private fun group(id:Int,questions:List<HrvQuestion>,body:(LinearLayout)->Unit) {
        if(questions.none(::enabled)) return
        BicycleForm.section(this,themes,id)
        addView(LinearLayout(context).apply { orientation=VERTICAL;body(this) },LayoutParams(-1,-2))
    }
    private fun rating(parent:LinearLayout,question:HrvQuestion,key:String,left:Int,middle:Int,right:Int,max:Int=8) {
        if(!enabled(question)) return
        val block=LinearLayout(context).apply { orientation=VERTICAL;tag="hrvq_block_$key" }
        label(block,question.label)
        val state=BicycleComponents.description(context,themes,"")
        val slider=SeekBar(context).apply {
            tag="hrvq_$key";this.max=max;progress=value.ratings[key] ?: max/2
            contentDescription=context.getString(question.label)
            minimumHeight=dp(BicycleDesign.touchSize)
            tickMark=GradientDrawable().apply { shape=GradientDrawable.OVAL;setSize(dp(2),dp(2)) }
            BicycleComponents.bindTheme(this,themes) {
                thumbTintList=ColorStateList.valueOf(it.accent);progressTintList=thumbTintList
                progressBackgroundTintList=ColorStateList.valueOf((it.foreground and 0xffffff) or 0x55000000)
                tickMarkTintList=progressBackgroundTintList
            }
        }
        fun show() {
            state.text=if(key in value.ratings) if(key=="rpe") context.getString(R.string.hrvq_rpe_value,slider.progress) else context.getString(R.string.hrvq_rating_value,slider.progress+1,max+1) else context.getString(R.string.hrvq_unanswered)
            slider.stateDescription=state.text
        }
        show()
        slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar:SeekBar)=Unit
            override fun onStopTrackingTouch(bar:SeekBar) { change(value.copy(ratings=value.ratings+(key to bar.progress)));show() }
            override fun onProgressChanged(bar:SeekBar,progress:Int,fromUser:Boolean) {
                if(fromUser) { change(value.copy(ratings=value.ratings+(key to progress)));show() }
            }
        })
        block.addView(slider,LayoutParams(-1,dp(BicycleDesign.touchSize)))
        block.addView(LinearLayout(context).apply {
            orientation=HORIZONTAL
            listOf(left,middle,right).forEachIndexed { i,id ->
                addView(BicycleComponents.description(context,themes,context.getString(id)).apply { gravity=when(i) { 0->Gravity.START;1->Gravity.CENTER;else->Gravity.END } },LayoutParams(0,-2,1f))
            }
        },LayoutParams(-1,-2))
        block.addView(state)
        block.addView(BicycleComponents.textAction(context,themes,context.getString(R.string.hrvq_remove)) {
            change(value.copy(ratings=value.ratings-key));slider.progress=max/2;show()
        })
        parent.addView(block,LayoutParams(-1,-2).apply { bottomMargin=dp(BicycleDesign.controlGap) })
    }
    private fun choices(parent:LinearLayout,key:String,title:String,labels:List<Int>,selected:Int?,changed:(Int)->Unit) {
        label(parent,title)
        val row=RadioGroup(context).apply { orientation=HORIZONTAL;tag="hrvq_$key" }
        BicycleComponents.segmentedControlRow(row,themes)
        val buttons=labels.mapIndexed { index,id -> BicycleSegmentButton(context,themes).apply {
            this.id=View.generateViewId();setText(id);tag="hrvq_${key}_$index"
        }.also { row.addView(it,RadioGroup.LayoutParams(0,-2,1f)) } }
        selected?.let { buttons.getOrNull(it)?.isChecked=true }
        row.setOnCheckedChangeListener { _,id->buttons.indexOfFirst { it.id==id }.takeIf { it>=0 }?.let(changed) }
        parent.addView(row,LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap);bottomMargin=dp(BicycleDesign.controlGap) })
    }
    private fun boolean(parent:LinearLayout,q:HrvQuestion,key:String,current:Boolean?,changed:(Boolean)->Unit) {
        if(enabled(q)) choices(parent,key,context.getString(q.label),listOf(R.string.hrvq_no,R.string.hrvq_yes),current?.let { if(it) 1 else 0 }) { changed(it==1) }
    }
    private fun input(parent:LinearLayout,key:String,title:String,current:String,numeric:Boolean=false,time:Boolean=false,maxLength:Int=500,changed:(String)->Boolean) {
        label(parent,title)
        val edit=BicycleComponents.input(context,themes).apply {
            tag="hrvq_$key";contentDescription=title
            typeface=android.graphics.Typeface.DEFAULT;textSize=BicycleDesign.bodySp
            inputType=when { numeric->android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                time->android.text.InputType.TYPE_CLASS_DATETIME or android.text.InputType.TYPE_DATETIME_VARIATION_TIME
                else->android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES }
            if(time) hint=context.getString(R.string.hrvq_time_hint)
            filters=arrayOf(android.text.InputFilter.LengthFilter(maxLength));setText(current)
        }
        edit.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
            override fun afterTextChanged(s:Editable?)=Unit
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {
                edit.error=if(changed(s?.toString().orEmpty())) null else context.getString(if(time) R.string.hrvq_invalid_time else R.string.hrvq_invalid_number)
            }
        })
        parent.addView(edit,LayoutParams(-1,-2))
    }
    private fun extra(parent:LinearLayout,key:String,title:String,numeric:Boolean=false,time:Boolean=false) {
        input(parent,key,title,value.extras[key].orEmpty(),numeric,time) { text ->
            val normalized=when {
                text.isBlank()->null
                numeric->text.trim().replace(',','.').toDoubleOrNull()?.takeIf { it.isFinite() && it in -1_000_000.0..1_000_000.0 }?.toString() ?: return@input false
                time->runCatching { java.time.LocalTime.parse(text.trim(),DateTimeFormatter.ofPattern("H:mm")).toString() }.getOrNull() ?: return@input false
                else->text
            }
            val names=if(key.startsWith("custom") && normalized!=null) value.customNames+(key to title) else value.customNames
            change(value.copy(extras=if(normalized==null) value.extras-key else value.extras+(key to normalized),customNames=names));true
        }
    }
    private fun dropdown(parent:LinearLayout,key:String,title:Int,values:List<String>,labels:List<Int>,current:String?,changed:(String?)->Unit) {
        label(parent,title)
        val spinner=Spinner(context).apply {
            tag="hrvq_$key";contentDescription=context.getString(title)
            adapter=ArrayAdapter(context,android.R.layout.simple_spinner_item,listOf(context.getString(R.string.hrvq_choose))+labels.map(context::getString)).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(values.indexOf(current)+1)
        }
        BicycleDialogStyle.spinner(spinner,themes)
        var last=spinner.selectedItemPosition
        spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>)=Unit
            override fun onItemSelected(parent:AdapterView<*>,view:View?,position:Int,id:Long) {
                if(position!=last) { last=position;changed(values.getOrNull(position-1)) }
            }
        }
        parent.addView(spinner,LayoutParams(-1,-2))
    }
    private fun extraDropdown(parent:LinearLayout,q:HrvQuestion,key:String,values:List<String>,labels:List<Int>) {
        if(enabled(q)) dropdown(parent,key,q.label,values,labels,value.extras[key]) { selected ->
            change(value.copy(extras=if(selected==null) value.extras-key else value.extras+(key to selected)))
        }
    }
    private fun trainingFields(parent:LinearLayout) {
        if(enabled(HrvQuestion.SPORT)) dropdown(parent,"sport",R.string.hrvq_sport,HrvQuestionnaire.sports,
            listOf(R.string.hrvq_cycling,R.string.hrvq_running,R.string.hrvq_swimming,R.string.hrvq_strength,R.string.hrvq_other),value.sport) { change(value.copy(sport=it)) }
        rating(parent,HrvQuestion.RPE,"rpe",R.string.hrvq_no_effort,R.string.hrvq_middle,R.string.hrvq_max_effort,10)
        if(enabled(HrvQuestion.DISTANCE) || enabled(HrvQuestion.DURATION)) {
            parent.addView(BicycleComponents.description(context,themes,context.getString(when {
                value.trainingSource=="manual"->R.string.hrvq_manual
                value.trainingSource=="rides"->R.string.hrvq_rides
                training==null->R.string.hrvq_library_loading
                training.rideIds.isEmpty()->R.string.hrvq_no_rides
                else->R.string.hrvq_rides
            },if(value.trainingSource=="rides") value.rideIds.size else training?.rideIds?.size ?: 0)))
            val row=LinearLayout(context).apply { orientation=HORIZONTAL;tag="hrvq_training_amounts" }
            fun amount(distance:Boolean) {
                val column=LinearLayout(context).apply { orientation=VERTICAL }
                val old=if(distance) value.trainingKm else value.trainingMinutes
                val text=old?.let { java.text.DecimalFormat("0.##").format(it) }.orEmpty()
                input(column,if(distance) "distance" else "duration",context.getString(if(distance) R.string.hrvq_distance else R.string.hrvq_duration),text,numeric=true,maxLength=20) { raw ->
                    val number=if(raw.isBlank()) null else raw.trim().replace(',','.').toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..1_000_000.0 } ?: return@input false
                    change(if(distance) value.copy(trainingKm=number,trainingSource="manual") else value.copy(trainingMinutes=number,trainingSource="manual"));true
                }
                row.addView(column,LayoutParams(0,-2,1f).apply { if(row.childCount>0) marginStart=dp(BicycleDesign.controlGap) })
            }
            if(enabled(HrvQuestion.DISTANCE)) amount(true)
            if(enabled(HrvQuestion.DURATION)) amount(false)
            parent.addView(row,LayoutParams(-1,-2))
        }
    }
    private fun prefill() {
        if(training==null || training.rideIds.isEmpty() || value.trainingSource=="manual") return
        if(value.trainingSource=="rides" && value.rideIds!=training.rideIds) return
        if(listOf(HrvQuestion.DISTANCE,HrvQuestion.DURATION,HrvQuestion.SPORT).none(::enabled)) return
        val next=value.copy(trainingKm=if(enabled(HrvQuestion.DISTANCE)) value.trainingKm ?: training.km else value.trainingKm,
            trainingMinutes=if(enabled(HrvQuestion.DURATION)) value.trainingMinutes ?: training.minutes else value.trainingMinutes,
            trainingSource="rides",rideIds=training.rideIds,sport=if(enabled(HrvQuestion.SPORT)) value.sport ?: "cycling" else value.sport)
        if(next!=value) change(next)
    }
    private fun build() {
        removeAllViews()
        val date=LocalDate.parse(value.day)
        label(this,context.getString(R.string.hrvq_intro,date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))))
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrvq_optional)))
        if(config.enabled.none(::enabled) && config.customNames.all(String::isBlank)) label(this,R.string.hrvq_empty)
        group(R.string.hrvq_sleep_group,HrvQuestion.entries.filter { it.group==0 }) { body ->
            rating(body,HrvQuestion.SLEEP,"sleep",R.string.hrvq_bad,R.string.hrvq_normal,R.string.hrvq_good)
            if(enabled(HrvQuestion.WAKE)) extra(body,"wake",context.getString(R.string.hrvq_wake),time=true)
            if(enabled(HrvQuestion.BED)) extra(body,"bed",context.getString(R.string.hrvq_bed),time=true)
        }
        group(R.string.hrvq_training_group,HrvQuestion.entries.filter { it.group==1 }) { body ->
            val details=LinearLayout(context).apply { orientation=VERTICAL;tag="hrvq_training_details" }
            fun showDetails() {
                val show=!enabled(HrvQuestion.TRAINING) || value.training?.let { it>0 }==true
                details.removeAllViews();details.visibility=if(show) VISIBLE else GONE
                if(show) { prefill();trainingFields(details) }
            }
            if(enabled(HrvQuestion.TRAINING)) choices(body,"training",context.getString(R.string.hrvq_training,date.minusDays(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
                listOf(R.string.hrvq_rest,R.string.hrvq_easy,R.string.hrvq_moderate,R.string.hrvq_intense),value.training) { selected ->
                change(value.copy(training=selected));showDetails()
            }
            extraDropdown(body,HrvQuestion.PHASE,"phase",HrvQuestionnaire.phases,listOf(R.string.hrvq_base,R.string.hrvq_build,R.string.hrvq_competition,R.string.hrvq_recovery,R.string.hrvq_other))
            body.addView(details,LayoutParams(-1,-2));showDetails()
            rating(body,HrvQuestion.MOTIVATION,"motivation",R.string.hrvq_not_at_all,R.string.hrvq_middle,R.string.hrvq_very)
        }
        group(R.string.hrvq_overview_group,HrvQuestion.entries.filter { it.group==2 }) { body ->
            rating(body,HrvQuestion.CONDITION,"condition",R.string.hrvq_bad,R.string.hrvq_normal,R.string.hrvq_good)
            rating(body,HrvQuestion.ENERGY,"energy",R.string.hrvq_low,R.string.hrvq_normal,R.string.hrvq_high)
            rating(body,HrvQuestion.SORENESS,"soreness",R.string.hrvq_not_at_all,R.string.hrvq_middle,R.string.hrvq_very)
            rating(body,HrvQuestion.FATIGUE,"fatigue",R.string.hrvq_not_at_all,R.string.hrvq_middle,R.string.hrvq_very)
            val injury=LinearLayout(context).apply { orientation=VERTICAL }
            fun showInjury() { injury.removeAllViews();if(value.injured==true) input(injury,"injury",context.getString(R.string.hrvq_injury),value.injury) { change(value.copy(injury=it));true } }
            boolean(body,HrvQuestion.INJURY,"injured",value.injured) { change(value.copy(injured=it));showInjury() }
            if(enabled(HrvQuestion.INJURY)) { body.addView(injury);showInjury() }
            extraDropdown(body,HrvQuestion.CYCLE,"cycle",HrvQuestionnaire.cycles,listOf(R.string.hrvq_menstruation,R.string.hrvq_follicular,R.string.hrvq_ovulation,R.string.hrvq_luteal,R.string.hrvq_unknown))
        }
        group(R.string.hrvq_lifestyle_group,HrvQuestion.entries.filter { it.group==3 }) { body ->
            rating(body,HrvQuestion.LIFESTYLE,"lifestyle",R.string.hrvq_unstable,R.string.hrvq_normal,R.string.hrvq_routine)
            boolean(body,HrvQuestion.TRAVEL,"traveled",value.traveled) { change(value.copy(traveled=it)) }
            if(enabled(HrvQuestion.ALCOHOL)) choices(body,"alcohol",context.getString(R.string.hrvq_alcohol),listOf(R.string.hrvq_none,R.string.hrvq_little,R.string.hrvq_much),value.alcohol) { change(value.copy(alcohol=it)) }
            boolean(body,HrvQuestion.SICK,"sick",value.sick) { change(value.copy(sick=it)) }
            for((q,key) in listOf(HrvQuestion.DIET to "diet",HrvQuestion.LOCATION to "location",HrvQuestion.SUPPLEMENTS to "supplements")) if(enabled(q)) extra(body,key,context.getString(q.label))
        }
        if(enabled(HrvQuestion.NOTES) || config.customNames.any(String::isNotBlank)) {
            BicycleForm.section(this,themes,R.string.hrvq_custom_group)
            if(enabled(HrvQuestion.NOTES)) input(this,"notes",context.getString(R.string.hrvq_notes),value.notes,maxLength=1000) { change(value.copy(notes=it));true }
            config.customNames.forEachIndexed { i,name -> if(name.isNotBlank()) {
                val key="custom${i+1}";extra(this,key,value.customNames[key] ?: name,numeric=true)
            } }
        }
        addView(status,LayoutParams(-1,-2));addView(retry,LayoutParams(-1,-2))
    }
    private fun dp(size:Int)=BicycleDesign.dp(context,size)
    override fun onAttachedToWindow() { super.onAttachedToWindow();settings.preferences.registerOnSharedPreferenceChangeListener(listener);riderPrefs.registerOnSharedPreferenceChangeListener(riderListener) }
    override fun onDetachedFromWindow() { settings.preferences.unregisterOnSharedPreferenceChangeListener(listener);riderPrefs.unregisterOnSharedPreferenceChangeListener(riderListener);flush();super.onDetachedFromWindow() }
}

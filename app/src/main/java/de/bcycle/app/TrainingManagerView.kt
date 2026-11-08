package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

internal class TrainingManagerView(context:Context,private val themes:ThemeManager,private val profile:()->RiderProfile,
    importClicked:(View)->Unit,private val deleteRequested:(Set<String>)->Unit,private val open:((TrainingWorkout)->Unit)?=null):LinearLayout(context) {
    private data class Group(val title:String,var count:Int=0)
    private var workouts=emptyList<TrainingWorkout>()
    private var sort=TrainingSort.DURATION
    private var descending=false
    private var sortingPopup:TrainingSortPopup?=null
    private val selected=linkedSetOf<String>()
    private var rows=emptyList<Any>()
    private var groups=emptyList<Int>()
    val status=BicycleFeedback.status(context,themes).apply {
        tag="training_status"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,BicycleDesign.dp(context,BicycleDesign.controlGap),pad,BicycleDesign.dp(context,BicycleDesign.controlGap))
    }
    private val list=object:BicycleFastScrollList(context,themes) {
        private var pinned:TrainingGroupHeader?=null
        private var pinnedKind:Group?=null
        private var overlayBottom=0
        private var overlayGesture=false
        private var overlayStartY=0f
        private var overlayMoved=false
        init { BicycleComponents.bindTheme(this,themes) {
            pinnedKind=null;pinned=null
            divider=android.graphics.drawable.ColorDrawable(it.foreground).apply { alpha=(255*BicycleDesign.dividerAlpha).toInt() }
            dividerHeight=BicycleDesign.dp(context,BicycleDesign.controlBorder)
            selector=android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            val inset=if(BicycleAppearance.flat(context)) 0 else BicycleDesign.dp(context,BicycleDesign.sectionCardInset)
            setPadding(inset,0,inset,0);invalidate()
        } }
        override fun drawContentOverlay(canvas:Canvas) {
            overlayBottom=0
            val position=groups.lastOrNull { it<=firstVisiblePosition } ?: return
            val actual=getChildAt(position-firstVisiblePosition)
            if(actual!=null && actual.top>=0) return
            val kind=rows[position] as Group
            if(pinnedKind!=kind) { pinnedKind=kind;pinned=TrainingGroupHeader(context,themes).apply { bind(kind.title,kind.count) } }
            val header=pinned ?: return
            val w=width-paddingLeft-paddingRight
            header.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));header.layout(0,0,w,header.measuredHeight)
            val next=groups.firstOrNull { it>position }?.let { getChildAt(it-firstVisiblePosition) }
            val offset=minOf(0,(next?.top ?: Int.MAX_VALUE)-header.height)
            overlayBottom=offset+header.height
            val saved=canvas.save();canvas.clipRect(paddingLeft,0,width-paddingRight,height)
            canvas.translate(paddingLeft.toFloat(),offset.toFloat());header.draw(canvas);canvas.restoreToCount(saved)
        }
        override fun dispatchTouchEvent(event:android.view.MotionEvent):Boolean {
            if(event.actionMasked==android.view.MotionEvent.ACTION_DOWN) {
                overlayGesture=event.y<overlayBottom && event.x<width-BicycleDesign.dp(context,32)
                overlayStartY=event.y;overlayMoved=false
            }
            if(overlayGesture && kotlin.math.abs(event.y-overlayStartY)>android.view.ViewConfiguration.get(context).scaledTouchSlop) overlayMoved=true
            val result=if(overlayGesture && !overlayMoved && event.actionMasked==android.view.MotionEvent.ACTION_UP) {
                val cancel=android.view.MotionEvent.obtain(event).apply { action=android.view.MotionEvent.ACTION_CANCEL }
                try { onTouchEvent(cancel);true } finally { cancel.recycle() }
            } else if(overlayGesture) onTouchEvent(event) else super.dispatchTouchEvent(event)
            if(event.actionMasked in listOf(android.view.MotionEvent.ACTION_UP,android.view.MotionEvent.ACTION_CANCEL)) overlayGesture=false
            return result
        }

    }.apply { tag="training_list" }
    private val adapter=object:BaseAdapter(),SectionIndexer {
        override fun getCount()=rows.size
        override fun getItem(position:Int)=rows[position]
        override fun getItemId(position:Int)=position.toLong()
        override fun getViewTypeCount()=2
        override fun getItemViewType(position:Int)=if(rows[position] is Group) 0 else 1
        override fun areAllItemsEnabled()=false
        override fun isEnabled(position:Int)=rows[position] is TrainingWorkout
        override fun getSections()=groups.map { (rows[it] as Group).title }.toTypedArray()
        override fun getPositionForSection(sectionIndex:Int)=groups.getOrElse(sectionIndex) { 0 }
        override fun getSectionForPosition(position:Int)=groups.indexOfLast { it<=position }.coerceAtLeast(0)
        override fun getView(position:Int,convert:View?,parent:ViewGroup):View {
            val row=rows[position]
            if(row is Group) return ((convert as? TrainingGroupHeader) ?: TrainingGroupHeader(context,themes)).apply { bind(row.title,row.count) }
            val workout=row as TrainingWorkout
            return ((convert as? TrainingWorkoutView) ?: TrainingWorkoutView(context,themes,onVariant=::cycleVariant)).apply {
                val base=workouts.first { it.id==workout.id }
                bind(workout,profile(),workout.id in selected,TrainingVariants.selected(context,base),TrainingVariants.available(base).size>1)
            }
        }
    }
    private val delete=BicycleFooterAction(context,themes,BicycleIcon.DeleteTrack,context.getString(R.string.route_action_delete)) { if(selected.isNotEmpty()) deleteRequested(selected.toSet()) }.apply { tag="training_delete";isEnabled=false }
    init {
        orientation=VERTICAL;tag="training_manager";BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        addView(status,LayoutParams(-1,-2))
        addView(list,LayoutParams(-1,0,1f));list.adapter=adapter
        fun select(position:Int) { val workout=rows.getOrNull(position) as? TrainingWorkout ?: return
            if(!selected.add(workout.id)) selected.remove(workout.id)
            delete.isEnabled=selected.isNotEmpty();delete.setLabel(context.getString(R.string.route_action_delete)+if(selected.isEmpty()) "" else " (${selected.size})");adapter.notifyDataSetChanged()
        }
        list.setOnItemClickListener { _,_,position,_ ->
            val workout=rows.getOrNull(position) as? TrainingWorkout
            if(selecting || open==null) select(position) else if(workout!=null) open.invoke(workout)
        }
        list.setOnItemLongClickListener { _,_,position,_->select(position);true }
        val footer=bicycleFooter(context,themes)
        lateinit var import:BicycleFooterAction
        import=BicycleFooterAction(context,themes,BicycleIcon.Import,context.getString(R.string.training_footer_import)) { importClicked(import) }.apply { tag="training_import" }
        footer.addView(import,LayoutParams(0,-2,1f))
        footer.addView(BicycleFooterAction(context,themes,BicycleIcon.Filter,context.getString(R.string.route_filter_button)) {}.apply { tag="training_filter";isEnabled=false },LayoutParams(0,-2,1f))
        lateinit var sorting:BicycleFooterAction
        sorting=BicycleFooterAction(context,themes,BicycleIcon.Sort,context.getString(R.string.route_sort_button)) {
            sortingPopup?.dismiss()
            sortingPopup=TrainingSortPopup(context,themes,sort,descending,{ sort=it;render(workouts);list.setSelection(0) },{ descending=it;render(workouts);list.setSelection(0) }).also { it.show(sorting) }
        }.apply { tag="training_sort" }
        footer.addView(sorting,LayoutParams(0,-2,1f))
        footer.addView(delete,LayoutParams(0,-2,1f));addView(footer,LayoutParams(-1,-2))
        render(emptyList())
    }
    internal fun applyPageSpacing() {
        // Group headers meet the main header, just like the route manager.
        setPadding(paddingLeft,0,paddingRight,paddingBottom)
        list.setPadding(list.paddingLeft,0,list.paddingRight,list.paddingBottom)
        list.clipToPadding=false
    }
    val selecting get()=selected.isNotEmpty()
    fun clearSelection() { selected.clear();delete.isEnabled=false;delete.setLabel(context.getString(R.string.route_action_delete));adapter.notifyDataSetChanged() }
    fun message(text:String) {
        BicycleFeedback.update(status,text)
        status.visibility=if(text.isBlank()) GONE else VISIBLE
    }
    override fun onDetachedFromWindow() {
        sortingPopup?.dismiss();sortingPopup=null
        super.onDetachedFromWindow()
    }
    private fun cycleVariant(id:String) {
        val base=workouts.firstOrNull { it.id==id } ?: return
        val old=rows.indexOfFirst { it is TrainingWorkout && it.id==id }
        val top=list.getChildAt(old-list.firstVisiblePosition)?.top ?: 0
        TrainingVariants.cycle(context,base)
        render(workouts)
        val next=rows.indexOfFirst { it is TrainingWorkout && it.id==id }
        if(next>=0) list.setSelectionFromTop(next,top)
    }
    fun render(workouts:List<TrainingWorkout>) {
        selected.retainAll(workouts.map { it.id }.toSet());delete.isEnabled=selected.isNotEmpty()
        delete.setLabel(context.getString(R.string.route_action_delete)+if(selected.isEmpty()) "" else " (${selected.size})")
        this.workouts=workouts.toList()
        val rider=profile()
        rows=buildList {
            var previous:Group?=null
            for(workout in sort.ordered(workouts.map { TrainingVariants.apply(it,TrainingVariants.selected(context,it)) },rider,descending)) {
                val value=sort.value(TrainingMetrics.of(workout,rider))
                val label=if(value==null) context.getString(R.string.training_sort_unknown) else {
                    val step=when(sort) { TrainingSort.DURATION->1800.0;TrainingSort.LOAD->25.0;TrainingSort.INTENSITY->.1;TrainingSort.SECTIONS->5.0 }
                    val upper=(kotlin.math.floor((value+1e-9)/step)+1)*step
                    val amount=when(sort) {
                        TrainingSort.DURATION->"${(upper/60).toInt()} ${context.getString(R.string.analysis_minutes)}"
                        TrainingSort.INTENSITY->String.format(Locale.getDefault(),"%.1f",upper)
                        else->upper.toInt().toString()
                    }
                    context.getString(R.string.training_sort_group,context.getString(sort.title),amount)
                }
                if(previous?.title!=label) { previous=Group(label);add(previous!!) }
                previous!!.count++
                add(workout)
            }
        }
        groups=rows.indices.filter { rows[it] is Group }
        message(if(workouts.isEmpty()) context.getString(R.string.training_empty) else "")
        adapter.notifyDataSetChanged()
    }
}

internal class TrainingWorkoutView(context:Context,private val themes:ThemeManager,private val detailAxes:Boolean=false,private val onVariant:((String)->Unit)?=null):LinearLayout(context) {
    private val name=BicycleComponents.text(context,themes,"").apply { typeface=BicycleDesign.strongTypeface;maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END;tag="training_name" }
    private val description=BicycleComponents.description(context,themes,"").apply { maxLines=3;ellipsize=android.text.TextUtils.TruncateAt.END;tag="training_description" }
    private val intensity=BicycleComponents.description(context,themes,"").apply { typeface=BicycleDesign.strongTypeface;tag="training_intensity" }
    private val variant=BicycleSpecialButton(context,themes,filled=false).apply {
        tag="training_variant";maxLines=1;minWidth=BicycleDesign.dp(context,64);minimumWidth=minWidth
        val pad=BicycleDesign.dp(context,BicycleDesign.controlGap);setPadding(pad,0,pad,0)
    }
    private val hardness=BicycleComponents.text(context,themes,"").apply { tag="training_hardness";gravity=android.view.Gravity.CENTER;maxLines=1;minWidth=BicycleDesign.dp(context,64) }
    private var badgeColor:Int?=null
    private val titleRow=LinearLayout(context).apply {
        orientation=HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;isBaselineAligned=false
        tag="training_title_row";setTag(R.id.inline_control_row,true)
        addView(name,LayoutParams(0,-2,1f).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(variant,LayoutParams(-2,BicycleDesign.dp(context,BicycleDesign.touchSize)))
        addView(hardness,LayoutParams(-2,BicycleDesign.dp(context,BicycleDesign.touchSize)).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap) })
    }
    private val duration=RouteListReading(context,themes,R.string.training_metric_duration,R.string.analysis_minutes)
    private val load=RouteListReading(context,themes,R.string.training_metric_load)
    private val effort=RouteListReading(context,themes,R.string.training_metric_intensity)
    private val sections=RouteListReading(context,themes,R.string.training_metric_sections)
    private val metrics=LinearLayout(context).apply {
        orientation=HORIZONTAL;isBaselineAligned=false;tag="training_metrics"
        listOf(duration,load,effort,sections).forEachIndexed { index,reading ->
            reading.number.setHorizontallyScrolling(false);reading.unit.setHorizontallyScrolling(false)
            if(index>0) addView(BicycleComponents.divider(context,themes).apply { alpha=BicycleDesign.dividerAlpha },LayoutParams(BicycleDesign.linePixels(context),-1))
            addView(reading,LayoutParams(0,-2,1f))
        }
    }
    private val chart=TrainingWorkoutChart(context,themes,detailAxes).apply { tag="training_chart" }
    private val details=BicycleComponents.description(context,themes,"").apply { tag="training_details" }
    private var selected=false
    init {
        orientation=VERTICAL;tag="training_card"
        val pad=if(detailAxes) 0 else BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        if(!detailAxes) addView(titleRow)
        addView(description,LayoutParams(-1,-2).apply { if(!detailAxes) topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        if(detailAxes) addView(intensity,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(metrics,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(chart,LayoutParams(-1,BicycleDesign.dp(context,if(detailAxes) 180 else 82)).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        if(detailAxes) addView(details,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        BicycleComponents.bindTheme(this,themes,::paintState)
        descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS
    }
    private fun paintState(tokens:ThemeTokens) {
        val badgeTokens=tokens.copy(primary=badgeColor ?: tokens.primary)
        styleSpecialButton(variant,badgeTokens,false)
        styleSpecialButton(hardness,badgeTokens,true)
        val pad=BicycleDesign.dp(context,BicycleDesign.controlGap)
        hardness.setPadding(pad,0,pad,0)
        if(detailAxes) { background=null;foreground=null }
        else {
            background=android.graphics.drawable.ColorDrawable(if(selected) tokens.tertiary else tokens.secondary)
            foreground=null
        }
    }
    fun bind(workout:TrainingWorkout,profile:RiderProfile,checked:Boolean,choice:TrainingVariant=TrainingVariant.STANDARD,canVary:Boolean=false) {
        selected=checked;paintState(themes.active.tokens)
        name.text=workout.name;description.text=workout.description;description.visibility=if(workout.description.isBlank()) GONE else VISIBLE
        val automatic=TrainingIntensity.of(workout,profile)
        intensity.text=automatic?.let { context.getString(R.string.training_auto_tag,context.getString(it.title)) }.orEmpty()
        intensity.visibility=if(automatic==null) GONE else VISIBLE
        badgeColor=when(automatic) { TrainingIntensity.HARD->TrainingZoneColors.red;TrainingIntensity.EASY->TrainingZoneColors.green;TrainingIntensity.SWEET_SPOT->TrainingZoneColors.yellow;null->null }
        hardness.text=automatic?.let { context.getString(it.title) }.orEmpty()
        hardness.visibility=if(automatic==null) GONE else VISIBLE
        variant.text=choice.label
        variant.contentDescription=context.getString(R.string.training_variant_description,choice.label)
        variant.stateDescription=if(canVary) context.getString(R.string.training_variant_cycle) else context.getString(R.string.training_variant_unavailable)
        variant.setOnClickListener(if(canVary && onVariant!=null) View.OnClickListener { onVariant.invoke(workout.id) } else null)
        variant.isClickable=canVary && onVariant!=null;variant.isFocusable=variant.isClickable
        paintState(themes.active.tokens)
        val values=TrainingMetrics.of(workout,profile)
        val number=java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits=1 }
        duration.bind(values.seconds?.let { number.format(it/60) },workout.id,"training_duration")
        if(values.seconds==null) duration.number.text="–"
        load.bind(values.load?.let { String.format(Locale.getDefault(),"%.0f",it) } ?: "–",workout.id,"training_load")
        effort.bind(values.intensity?.let { String.format(Locale.getDefault(),"%.2f",it) } ?: "–",workout.id,"training_intensity")
        sections.bind(values.sections.toString(),workout.id,"training_sections")
        metrics.contentDescription=context.getString(R.string.training_metrics_description)
        chart.show(workout,profile)
        val duration=workout.seconds?.let { context.getString(R.string.training_minutes,String.format(Locale.getDefault(),"%.1f",it/60)) } ?: context.getString(R.string.training_open_duration)
        val types=workout.steps.flatMap { it.targets }.map { it.unit }.toSet()
        val target=when {
            types.any { it in setOf(TrainingUnit.FTP,TrainingUnit.WATTS,TrainingUnit.POWER_ZONE) }->context.getString(R.string.training_target_power)
            types.any { it in setOf(TrainingUnit.HR_BPM,TrainingUnit.HR_ZONE,TrainingUnit.HR_MAX_PERCENT,TrainingUnit.HR_THRESHOLD_PERCENT,TrainingUnit.HR_RESERVE_PERCENT) }->context.getString(R.string.training_target_hr)
            else->context.getString(R.string.training_target_free)
        }
        details.text=listOf(duration,context.resources.getQuantityString(R.plurals.training_steps,workout.steps.size,workout.steps.size),target,workout.sport,workout.source,workout.format.uppercase()).filter { it.isNotBlank() }.joinToString(" · ")
        if(workout.steps.any { step -> step.target?.let { it.unit in setOf(TrainingUnit.WATTS,TrainingUnit.HR_BPM,TrainingUnit.HR_MAX_PERCENT,TrainingUnit.HR_THRESHOLD_PERCENT,TrainingUnit.HR_RESERVE_PERCENT) && it.zone(profile)==null }==true })
            details.text=details.text.toString()+"\n"+context.getString(R.string.training_zones_missing)
        if(detailAxes && profile.ftpW==null && TrainingUnit.FTP in types)
            details.text=details.text.toString()+"\n"+context.getString(R.string.training_axis_ftp_missing)
        contentDescription=listOf(workout.name,intensity.text.toString(),details.text.toString()).filter { it.isNotBlank() }.joinToString(". ")
        isActivated=checked
    }
}
internal class TrainingWorkoutChart(context:Context,private val themes:ThemeManager?=null,private val axes:Boolean=false):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val path=Path()
    private val axisPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=BicycleDesign.bodyTypeface }
    private var workout:TrainingWorkout?=null
    private var profile=RiderProfile()
    private data class Block(val weight:Double,val start:Double,val end:Double,val color:Int)
    private data class Prepared(val timed:Boolean,val total:Double,val step:Double?,val peak:Double,val blocks:List<Block>)
    private var prepared:Prepared?=null
    fun show(value:TrainingWorkout,p:RiderProfile) {
        if(workout===value && profile==p) return
        workout=value;profile=p;prepared=null
        contentDescription=context.getString(R.string.training_chart_description);invalidate()
    }
    private fun prepare(training:TrainingWorkout):Prepared {
        val timed=training.seconds!=null
        val total=training.seconds ?: training.steps.size.toDouble()
        val maxima=training.steps.mapNotNull { it.target }.groupBy { it.unit }.mapValues { (_,targets) -> targets.maxOf { maxOf(it.low,it.high) }.coerceAtLeast(1.0) }
        fun level(target:TrainingTarget?,end:Boolean=false):Double {
            if(target==null) return .4
            return target.fraction(profile,end) ?: (if(target.ramp) { if(end) target.high else target.low } else (target.low+target.high)/2)/(maxima[target.unit] ?: 1.0)
        }
        val watts=if(axes) TrainingChartAxes.watts(training,profile) else null
        val yStep=watts?.let { TrainingChartAxes.wattStep(it) }
        val peak=if(watts!=null && yStep!=null) kotlin.math.ceil(watts.coerceAtLeast(yStep)/yStep)*yStep else maxOf(1.5,training.steps.maxOf { maxOf(level(it.target),level(it.target,true)) })
        val blocks=training.steps.map { step ->
            val target=step.target
            fun power(end:Boolean)=target?.let { TrainingChartAxes.targetWatts(it,profile,end) } ?: 0.0
            val from=if(watts!=null) power(false) else level(target)
            val to=if(target?.ramp==true) { if(watts!=null) power(true) else level(target,true) } else from
            Block(if(timed) step.seconds!! else 1.0,from,to,target?.color(profile) ?: TrainingZoneColors.grey)
        }
        return Prepared(timed,total,yStep,peak,blocks)
    }
    override fun onDraw(canvas:Canvas) {
        val training=workout ?: return
        val model=prepared ?: prepare(training).also { prepared=it }
        val timed=model.timed;val total=model.total;val yStep=model.step;val peak=model.peak
        val density=resources.displayMetrics.density
        axisPaint.textSize=BicycleDesign.analysisLabelSp*resources.displayMetrics.scaledDensity
        val foreground=themes?.active?.tokens?.foreground ?: Color.WHITE
        val left=if(yStep!=null) axisPaint.measureText("${peak.toInt()} W")+8*density else 0f
        val right=width.toFloat()
        val plotTop=if(axes) -axisPaint.fontMetrics.ascent+2*density else 0f
        val bottom=if(axes) height-axisPaint.fontSpacing-8*density else height.toFloat()
        if(right<=left || bottom<=plotTop) return
        fun y(value:Double)=bottom-(value/peak).coerceIn(0.0,1.0).toFloat()*(bottom-plotTop)
        var x=left
        for(block in model.blocks) {
            val next=x+(block.weight/total*(right-left)).toFloat()
            val top=if(axes) y(block.start) else height*(1-(block.start/peak).coerceIn(.06,1.0)).toFloat()
            val other=if(axes) y(block.end) else height*(1-(block.end/peak).coerceIn(.06,1.0)).toFloat()
            paint.color=block.color
            path.reset();path.moveTo(x,top);path.lineTo(next,other);path.lineTo(next,bottom);path.lineTo(x,bottom);path.close();canvas.drawPath(path,paint)
            x=next
        }
        if(!axes) return
        axisPaint.strokeWidth=density
        if(yStep!=null) {
            axisPaint.textAlign=Paint.Align.RIGHT
            for(i in 0..kotlin.math.round(peak/yStep).toInt()) {
                val value=i*yStep;val at=y(value)
                axisPaint.color=(foreground and 0x00ffffff) or 0x30000000
                canvas.drawLine(left,at,right,at,axisPaint)
                axisPaint.color=foreground
                canvas.drawText("${value.toInt()} W",left-8*density,at-(axisPaint.ascent()+axisPaint.descent())/2,axisPaint)
            }
        }
        if(timed) {
            val ticks=TrainingChartAxes.timeTicks(total,(right-left)/(72*density))
            axisPaint.textAlign=Paint.Align.CENTER
            var previousEnd=Float.NEGATIVE_INFINITY
            val endLabel=trainingClock(total)
            val endStart=right-axisPaint.measureText(endLabel)
            for(tick in ticks) {
                val at=left+(tick/total*(right-left)).toFloat()
                val label=trainingClock(tick)
                val labelWidth=axisPaint.measureText(label)
                val half=minOf(labelWidth/2,(right-left)/2)
                val textX=at.coerceIn(left+half,right-half)
                val textStart=textX-labelWidth/2
                if(tick!=total && tick!=0.0 && (textStart<previousEnd+8*density || textX+labelWidth/2>endStart-8*density)) continue
                axisPaint.color=(foreground and 0x00ffffff) or 0x30000000
                canvas.drawLine(at,plotTop,at,bottom,axisPaint)
                axisPaint.color=foreground
                canvas.drawText(label,textX,height.toFloat()-axisPaint.descent(),axisPaint)
                previousEnd=textX+labelWidth/2
            }
        }
    }
}

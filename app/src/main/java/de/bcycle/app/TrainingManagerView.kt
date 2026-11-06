package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

internal class TrainingManagerView(context:Context,private val themes:ThemeManager,private val profile:()->RiderProfile,
    importClicked:(View)->Unit,private val deleteRequested:(Set<String>)->Unit,private val open:((TrainingWorkout)->Unit)?=null):LinearLayout(context) {
    private val selected=linkedSetOf<String>()
    private var rows=emptyList<Any>()
    private var groups=emptyList<Int>()
    val status=BicycleFeedback.status(context,themes).apply {
        tag="training_status"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,BicycleDesign.dp(context,BicycleDesign.controlGap),pad,BicycleDesign.dp(context,BicycleDesign.controlGap))
    }
    private val list=object:BicycleFastScrollList(context,themes) {
        private var pinned:BicycleSectionHeader?=null
        private var pinnedKind:TrainingKind?=null
        private var overlayBottom=0
        private var overlayGesture=false
        private var overlayStartY=0f
        private var overlayMoved=false
        init { BicycleComponents.bindTheme(this,themes) {
            pinnedKind=null;pinned=null
            val inset=if(BicycleAppearance.flat(context)) 0 else BicycleDesign.dp(context,BicycleDesign.sectionCardInset)
            setPadding(inset,0,inset,0);invalidate()
        } }
        override fun drawContentOverlay(canvas:Canvas) {
            overlayBottom=0
            val position=groups.lastOrNull { it<=firstVisiblePosition } ?: return
            val actual=getChildAt(position-firstVisiblePosition)
            if(actual!=null && actual.top>=0) return
            val kind=rows[position] as TrainingKind
            if(pinnedKind!=kind) { pinnedKind=kind;pinned=BicycleSectionHeader(context,themes,context.getString(kind.title)) }
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

    }.apply { tag="training_list";divider=null;dividerHeight=0 }
    private val adapter=object:BaseAdapter(),SectionIndexer {
        override fun getCount()=rows.size
        override fun getItem(position:Int)=rows[position]
        override fun getItemId(position:Int)=position.toLong()
        override fun getViewTypeCount()=2
        override fun getItemViewType(position:Int)=if(rows[position] is TrainingKind) 0 else 1
        override fun areAllItemsEnabled()=false
        override fun isEnabled(position:Int)=rows[position] is TrainingWorkout
        override fun getSections()=groups.map { context.getString((rows[it] as TrainingKind).title) }.toTypedArray()
        override fun getPositionForSection(sectionIndex:Int)=groups.getOrElse(sectionIndex) { 0 }
        override fun getSectionForPosition(position:Int)=groups.indexOfLast { it<=position }.coerceAtLeast(0)
        override fun getView(position:Int,convert:View?,parent:ViewGroup):View {
            val row=rows[position]
            if(row is TrainingKind) return (convert as? BicycleSectionHeader)?.takeIf { it.contentDescription==context.getString(row.title) } ?: BicycleSectionHeader(context,themes,context.getString(row.title))
            val workout=row as TrainingWorkout
            return ((convert as? TrainingWorkoutView) ?: TrainingWorkoutView(context,themes)).apply { bind(workout,profile(),workout.id in selected) }
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
        footer.addView(BicycleFooterAction(context,themes,BicycleIcon.Sort,context.getString(R.string.route_sort_button)) {}.apply { tag="training_sort";isEnabled=false },LayoutParams(0,-2,1f))
        footer.addView(delete,LayoutParams(0,-2,1f));addView(footer,LayoutParams(-1,-2))
        render(emptyList())
    }
    val selecting get()=selected.isNotEmpty()
    fun clearSelection() { selected.clear();delete.isEnabled=false;delete.setLabel(context.getString(R.string.route_action_delete));adapter.notifyDataSetChanged() }
    fun message(text:String) { BicycleFeedback.update(status,text) }
    fun render(workouts:List<TrainingWorkout>) {
        selected.retainAll(workouts.map { it.id }.toSet());delete.isEnabled=selected.isNotEmpty()
        delete.setLabel(context.getString(R.string.route_action_delete)+if(selected.isEmpty()) "" else " (${selected.size})")
        rows=TrainingKind.entries.flatMap { kind -> workouts.filter { it.kind==kind }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }).let { if(it.isEmpty()) emptyList() else listOf(kind)+it } }
        groups=rows.indices.filter { rows[it] is TrainingKind }
        message(if(workouts.isEmpty()) context.getString(R.string.training_empty) else context.resources.getQuantityString(R.plurals.training_count,workouts.size,workouts.size))
        adapter.notifyDataSetChanged()
    }
}

internal class TrainingWorkoutView(context:Context,private val themes:ThemeManager):LinearLayout(context) {
    private val name=BicycleComponents.text(context,themes,"").apply { typeface=BicycleDesign.strongTypeface;maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END;tag="training_name" }
    private val description=BicycleComponents.description(context,themes,"").apply { maxLines=3;ellipsize=android.text.TextUtils.TruncateAt.END;tag="training_description" }
    private val chart=TrainingWorkoutChart(context)
    private val details=BicycleComponents.description(context,themes,"").apply { tag="training_details" }
    private var selected=false
    init {
        orientation=VERTICAL;tag="training_card"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        addView(name);addView(description,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(chart,LayoutParams(-1,BicycleDesign.dp(context,96)).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(details,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        BicycleComponents.bindTheme(this,themes) { BicycleSelectionCard.apply(this,it,selected) }
        descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS
    }
    fun bind(workout:TrainingWorkout,profile:RiderProfile,checked:Boolean) {
        selected=checked;BicycleSelectionCard.apply(this,themes.active.tokens,checked)
        name.text=workout.name;description.text=workout.description;description.visibility=if(workout.description.isBlank()) GONE else VISIBLE
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
        contentDescription=workout.name+". "+details.text
        isActivated=checked
    }
}
internal class TrainingWorkoutChart(context:Context):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val path=Path()
    private var workout:TrainingWorkout?=null
    private var profile=RiderProfile()
    fun show(value:TrainingWorkout,p:RiderProfile) { workout=value;profile=p;contentDescription=context.getString(R.string.training_chart_description);invalidate() }
    override fun onDraw(canvas:Canvas) {
        val training=workout ?: return
        val timed=training.seconds!=null
        val total=training.seconds ?: training.steps.size.toDouble()
        val maxima=training.steps.mapNotNull { it.target }.groupBy { it.unit }.mapValues { (_,targets) -> targets.maxOf { maxOf(it.low,it.high) }.coerceAtLeast(1.0) }
        fun level(target:TrainingTarget?,end:Boolean=false):Double {
            if(target==null) return .4
            return target.fraction(profile,end) ?: (if(target.ramp) { if(end) target.high else target.low } else (target.low+target.high)/2)/(maxima[target.unit] ?: 1.0)
        }
        val peak=maxOf(1.5,training.steps.maxOf { maxOf(level(it.target),level(it.target,true)) })
        var x=0f
        for(step in training.steps) {
            val next=x+((if(timed) step.seconds!! else 1.0)/total*width).toFloat()
            val target=step.target
            val start=level(target)
            val end=if(target?.ramp==true) level(target,true) else start
            val top=height*(1-(start/peak).coerceIn(.06,1.0)).toFloat()
            val other=height*(1-(end/peak).coerceIn(.06,1.0)).toFloat()
            val color=target?.color(profile) ?: TrainingZoneColors.grey
            paint.color=color
            path.reset();path.moveTo(x,top);path.lineTo(next,other);path.lineTo(next,height.toFloat());path.lineTo(x,height.toFloat());path.close();canvas.drawPath(path,paint)
            x=next
        }
    }
}

package de.bcycle.app

import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** Compact query menu with native dropdowns, retaining drafts until the menu is dismissed. */
internal class RouteQueryPopup(private val context:Context,private val themes:ThemeManager,
    private val navigation:DialogNavigation,private val query:RouteQuery,private val routes:List<ManagedRoute> = emptyList(),private val profile:(String)->Int?={ RideProfileColor.slot(context,it) },private val changed:()->Unit):PopupWindow() {
    private val content=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL }
    internal var groupPopup:PopupWindow?=null;private set
    private val body=BicycleComponents.dialogBody(context,themes)
    private val availabilityUpdates=mutableListOf<(RouteFilterAvailability)->Unit>()
    private val profileSlots by lazy { routes.associate { it.id to profile(it.id) } }
    private fun refreshAvailability() {
        if(availabilityUpdates.isEmpty()) return
        val available=query.availability(routes,{ profileSlots[it] })
        availabilityUpdates.forEach { it(available) }
    }
    private fun option(view:View,selected:()->Boolean,available:(RouteFilterAvailability)->Boolean) {
        availabilityUpdates.add { values ->
            view.isEnabled=selected() || available(values)
            view.alpha=if(view.isEnabled) 1f else .5f
        }
    }
    private fun dp(v:Int)=BicycleDesign.dp(context,v)
    private fun group(title:Int,summary:()->String,build:(LinearLayout,()->Unit)->Unit) {
        val section=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL
            val gap=dp(BicycleDesign.controlGap)
            setPadding(gap,gap,gap,gap)
            BicycleComponents.bindTheme(this,themes) { setBackgroundColor(it.tertiary) }
        }
        lateinit var header:Button
        header=BicycleComponents.action(context,themes,"") {
            openGroup(header,section)
        }.apply { tag="route_query_group_$title";stateDescription=context.getString(R.string.zone_collapsed) }
        BicycleDialogStyle.accordion(header,themes)
        BicycleDialogStyle.actionIcon(header,themes,BicycleIcon.Expand)
        secondaryControl(header)
        fun label() { header.text=context.getString(title)+" · "+summary() }
        build(section) { label();refreshAvailability();changed() };label()
        fun controls(view:View) {
            if(view is Button || view is EditText) BicycleComponents.controlSpacing(view)
            if(view is EditText || view is Button && view !is CompoundButton) secondaryControl(view)
            if(view is android.view.ViewGroup) for(i in 0 until view.childCount) controls(view.getChildAt(i))
        }
        controls(section)
        body.addView(header,LinearLayout.LayoutParams(-1,-2))
        BicycleComponents.controlSpacing(header)
    }
    private fun secondaryControl(view:View) {
        BicycleComponents.bindTheme(view,themes) {
            view.background=BicycleComponents.controlBackground(context,it,it.secondary)
            view.backgroundTintList=null
        }
    }
    private fun count(size:Int)=if(size==0) context.getString(R.string.route_filter_all) else size.toString()
    fun importStatus(onlyNew:()->Boolean,select:(Boolean)->Unit) {
        group(R.string.dreeve_filter_import_status,{ context.getString(if(onlyNew()) R.string.dreeve_filter_not_imported else R.string.dreeve_filter_all) }) { section,update ->
            section.addView(RadioGroup(context).apply {
                orientation=RadioGroup.HORIZONTAL;BicycleComponents.segmentedControlRow(this,themes)
                listOf(false to R.string.dreeve_filter_all,true to R.string.dreeve_filter_not_imported).forEach { (value,label) ->
                    addView(BicycleSegmentButton(context,themes).apply {
                        id=View.generateViewId();tag=if(value) "dreeve_filter_new" else "dreeve_filter_all"
                        setText(label);isChecked=onlyNew()==value
                        setOnCheckedChangeListener { _,on -> if(on) { select(value);update() } }
                    },RadioGroup.LayoutParams(0,-2,1f))
                }
            },LinearLayout.LayoutParams(-1,-2))
        }
    }
    fun filters(reset:()->Unit) {
        group(R.string.route_filter_kind,{ count(query.kinds.size) }) { section,update ->
            val segments=LinearLayout(context).apply { BicycleComponents.segmentedControlRow(this,themes) }
            RouteKind.entries.forEach { kind ->
                segments.addView(BicycleSegmentButton(context,themes,multiple=true).apply {
                    tag="route_kind_${kind.name}";setText(kind.label);isChecked=kind in query.kinds
                    option(this,{ kind in query.kinds },{ kind in it.kinds })
                    setOnCheckedChangeListener { _,on ->
                        query.kinds=if(on) query.kinds+kind else query.kinds-kind;update()
                    }
                },LinearLayout.LayoutParams(0,-2,1f))
            }
            section.addView(segments,LinearLayout.LayoutParams(-1,-2))
        }
        group(R.string.route_filter_bike_type,{ count(query.bikeTypes.size) }) { section,update ->
            val segments=LinearLayout(context).apply { BicycleComponents.segmentedControlRow(this,themes) }
            BikeType.entries.forEach { type ->
                segments.addView(BicycleSegmentButton(context,themes,multiple=true).apply {
                    tag="route_bike_type_${type.name}"
                    setText(when(type) { BikeType.BIKE->R.string.bikes_type_bike;BikeType.EBIKE->R.string.bikes_type_ebike;BikeType.INDOOR->R.string.bikes_type_indoor })
                    isChecked=type in query.bikeTypes
                    option(this,{ type in query.bikeTypes },{ type in it.bikeTypes })
                    setOnCheckedChangeListener { _,on ->
                        query.bikeTypes=if(on) query.bikeTypes+type else query.bikeTypes-type;update()
                    }
                },LinearLayout.LayoutParams(0,-2,1f))
            }
            section.addView(segments,LinearLayout.LayoutParams(-1,-2))
        }
        group(R.string.route_filter_bike,{ count(query.bikes.size) }) { section,update ->
            val profiles=BikeProfileStore(context,context.getSharedPreferences("bike",0)).all()
            val grid=BikeFilterGrid(context,themes,profiles,query.bikes) { slot,on ->
                query.bikes=if(on) query.bikes+slot else query.bikes-slot;update()
            }
            availabilityUpdates.add { grid.showAvailable(it.bikes) }
            section.addView(grid,LinearLayout.LayoutParams(-1,-2))
            section.addView(CheckBox(context).apply {
                tag="route_bike_unknown";setText(R.string.route_filter_unknown_bike);isChecked=-1 in query.bikes
                BicycleDialogStyle.control(this,themes)
                option(this,{ -1 in query.bikes },{ -1 in it.bikes })
                setOnCheckedChangeListener { _,on ->
                    query.bikes=if(on) query.bikes+(-1) else query.bikes-(-1);update()
                }
            },LinearLayout.LayoutParams(-1,-2))
        }

        group(R.string.route_filter_date,{ listOfNotNull(query.fromDate?.toString(),query.toDate?.toString()).joinToString(" – ").ifEmpty { context.getString(R.string.route_filter_range) } }) { section,update ->
            val dates=LinearLayout(context)
            lateinit var from:Button;lateinit var to:Button
            fun labels() {
                from.text=context.getString(R.string.route_filter_from)+" · "+(query.fromDate?.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT)) ?: "—")
                to.text=context.getString(R.string.route_filter_to)+" · "+(query.toDate?.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT)) ?: "—")
            }
            fun picker(start:Boolean) {
                val initial=(if(start) query.fromDate else query.toDate) ?: LocalDate.now()
                val pickerContext=android.view.ContextThemeWrapper(context,if(themes.settingsLight) android.R.style.Theme_Material_Light else android.R.style.Theme_Material)
                val picker=DatePicker(pickerContext)
                var ready=false
                picker.init(initial.year,initial.monthValue-1,initial.dayOfMonth) { _,y,m,d -> if(ready) {
                    val value=LocalDate.of(y,m+1,d)
                    if(start) { query.fromDate=value;if(query.toDate?.isBefore(value)==true) query.toDate=value }
                    else { query.toDate=value;if(query.fromDate?.isAfter(value)==true) query.fromDate=value }
                    labels();update()
                } };ready=true
                BicycleDialogBuilder(context,themes,navigation,fullscreen=false,helpEnabled=false)
                    .setTitle(if(start) R.string.route_filter_from else R.string.route_filter_to).setView(picker).show()
            }
            from=BicycleComponents.action(context,themes,"") { picker(true) }
            to=BicycleComponents.action(context,themes,"") { picker(false) }
            dates.addView(from,LinearLayout.LayoutParams(0,-2,1f));dates.addView(to,LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=dp(8) })
            section.addView(dates);labels()
            val shortcuts=listOf(R.string.route_week_this,R.string.route_week_last,R.string.route_month_this,R.string.route_month_last)
            shortcuts.chunked(2).forEachIndexed { rowIndex,titles ->
                section.addView(LinearLayout(context).apply { titles.forEachIndexed { i,title ->
                    addView(BicycleComponents.action(context,themes,context.getString(title)) {
                        val today=LocalDate.now();val monday=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));val first=today.withDayOfMonth(1)
                        val range=when(rowIndex*2+i) { 0->monday to monday.plusDays(6);1->monday.minusWeeks(1) to monday.minusDays(1);2->first to first.plusMonths(1).minusDays(1);else->first.minusMonths(1) to first.minusDays(1) }
                        query.fromDate=range.first;query.toDate=range.second;labels();update()
                    },LinearLayout.LayoutParams(0,-2,1f).apply { if(i>0) marginStart=dp(8) })
                } })
            }
        }
        range(R.string.route_filter_distance,{ query.distance },{ query.distance=it })
        range(R.string.route_filter_moving,{ query.moving },{ query.moving=it })
        range(R.string.route_filter_speed,{ query.speed },{ query.speed=it })
        body.addView(BicycleComponents.action(context,themes,context.getString(R.string.route_filter_reset)) { query.clearFilters();changed();dismiss();reset() },LinearLayout.LayoutParams(-1,-2))
        refreshAvailability()
        shell(R.string.route_filter_button)
    }
    private fun range(title:Int,get:()->RouteRange,set:(RouteRange)->Unit) {
        val time=title==R.string.route_filter_moving
        fun format(value:Double?):String=if(value==null) "—" else if(time) {
            val minutes=kotlin.math.round(value*60).toLong()
            String.format(java.util.Locale.getDefault(),"%d:%02d",minutes/60,minutes%60)
        } else java.text.DecimalFormat("0.#").format(value)
        group(title,{ get().let { if(!it.active) context.getString(R.string.route_filter_range) else "${format(it.from)} – ${format(it.to)}" } }) { section,update ->
            val row=LinearLayout(context)
            val inputs=listOf(get().from,get().to).mapIndexed { i,value ->
                BicycleComponents.input(context,themes).apply {
                    tag="route_range_${title}_$i";hint=context.getString(if(i==0) R.string.route_filter_from else R.string.route_filter_to)
                    contentDescription=context.getString(title)+" · "+hint
                    inputType=if(time) InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME
                        else InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    if(time) hint=context.getString(if(i==0) R.string.route_time_from else R.string.route_time_to)
                    BicycleForm.outlinedInput(this,themes,numeric=true)
                    setSingleLine();setText(if(value==null) "" else format(value))
                    row.addView(this,LinearLayout.LayoutParams(0,-2,1f).apply { if(i>0) marginStart=dp(8) })
                }
            }
            var syncing=false
            val step=if(time) 1.0/60 else .1
            val measured=routes.mapNotNull { route -> when(title) {
                R.string.route_filter_distance->route.details.distanceM?.div(1000)
                R.string.route_filter_moving->route.details.movingMs?.div(3600000.0)
                else->route.details.movingAverageKmh
            } }.filter { it.isFinite() && it>=0 }.maxOrNull()
            val fallback=when(title) { R.string.route_filter_distance->100.0;R.string.route_filter_moving->8.0;else->50.0 }
            fun domain():Double {
                val value=maxOf(measured ?: fallback,get().from ?: 0.0,get().to ?: 0.0,step)
                val interval=when(title) { R.string.route_filter_distance->10.0;R.string.route_filter_moving->1.0;else->5.0 }
                return kotlin.math.ceil(value/interval)*interval
            }
            lateinit var slider:BicycleRangeSlider
            fun reflect() { val max=domain();slider.show(max,step,get().from ?: 0.0,get().to ?: max) }
            slider=BicycleRangeSlider(context,themes,context.getString(title),{ format(it) }) { a,b ->
                syncing=true
                inputs[0].setText(format(a));inputs[1].setText(format(b));inputs.forEach { it.error=null }
                syncing=false;set(RouteRange(a,b));update()
            }.apply { tag="route_range_slider_$title" }
            reflect()
            inputs.forEach { input -> input.onTextChanged {
                if(syncing) return@onTextChanged
                fun value(index:Int):Double? {
                    val raw=inputs[index].text.toString().replace(',','.')
                    if(!time || ':' !in raw) return raw.toDoubleOrNull()
                    val parts=raw.split(':');if(parts.size!=2) return null
                    val hours=parts[0].toLongOrNull()?.takeIf { it>=0 } ?: return null
                    val minutes=parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
                    return hours.toDouble()+minutes/60.0
                }
                val a=value(0);val b=value(1)
                val valid=inputs.indices.all { inputs[it].text.isBlank() || (value(it)?.let { n -> n.isFinite() && n>=0 }==true) } && (a==null || b==null || a<=b)
                inputs.forEach { it.error=if(valid) null else context.getString(R.string.route_filter_invalid) }
                if(valid) { set(RouteRange(a,b));reflect();update() }
            } }
            section.addView(row)
            section.addView(slider,LinearLayout.LayoutParams(-1,dp(BicycleDesign.touchSize)))
        }
    }
    fun sorting() {
        group(R.string.route_sort_field,{ context.getString(query.sort.label) }) { section,update ->
            val radios=RadioGroup(context)
            RouteSort.entries.forEach { sort -> radios.addView(RadioButton(context).apply {
                id=View.generateViewId();text=context.getString(sort.label);isChecked=sort==query.sort
                BicycleDialogStyle.control(this,themes)
                setOnCheckedChangeListener { _,on -> if(on) { query.sort=sort;update() } }
            }) };section.addView(radios)
        }
        group(R.string.route_sort_direction,{ context.getString(if(query.descending) R.string.route_sort_down else R.string.route_sort_up) }) { section,update ->
            val radios=RadioGroup(context)
            listOf(false,true).forEach { down -> radios.addView(RadioButton(context).apply {
                id=View.generateViewId();text=context.getString(if(down) R.string.route_sort_down else R.string.route_sort_up);isChecked=down==query.descending
                BicycleDialogStyle.control(this,themes)
                setOnCheckedChangeListener { _,on -> if(on) { query.descending=down;update() } }
            }) };section.addView(radios)
        }
        shell(R.string.route_sort_button)
    }
    private fun shell(title:Int) {
        isFocusable=true;isOutsideTouchable=true;inputMethodMode=INPUT_METHOD_NEEDED
        setBackgroundDrawable(BicycleComponents.controlBackground(context,themes.active.tokens))
        // Resolve spacing before measuring; attachment must not make the menu taller afterwards.
        for(i in 0 until body.childCount) if(body.getChildAt(i) is Button) BicycleComponents.controlSpacing(body.getChildAt(i))
        content.contentDescription=context.getString(title)
        content.addView(ScrollView(context).apply { BicycleComponents.scrollStyle(this);isFillViewport=false;addView(body) },LinearLayout.LayoutParams(-1,-2))
        BicycleComponents.bindTheme(body,themes) { body.setBackgroundColor(it.secondary) }
        contentView=content
        setOnDismissListener { groupPopup?.dismiss() }
        softInputMode=android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    }
    private fun dimensions(anchor:View):Triple<android.graphics.Rect,Int,Int> {
        val frame=android.graphics.Rect().also(anchor::getWindowVisibleDisplayFrame)
        if(frame.isEmpty) frame.set(0,0,context.resources.displayMetrics.widthPixels,context.resources.displayMetrics.heightPixels)
        val w=(frame.width()-2*dp(16)).coerceAtLeast(1)
        val limit=(frame.height()*.75f).toInt().coerceAtLeast(dp(48))
        content.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(limit,View.MeasureSpec.AT_MOST))
        return Triple(frame,w,content.measuredHeight.coerceIn(dp(48),limit))
    }
    private fun openGroup(header:Button,section:LinearLayout) {
        groupPopup?.dismiss()
        refreshAvailability()
        val dropdown=PopupWindow().apply {
            isFocusable=true;isOutsideTouchable=true;inputMethodMode=INPUT_METHOD_NEEDED
            softInputMode=android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            setBackgroundDrawable(BicycleComponents.dropdownBackground(context,themes.active.tokens))
        }
        val scroll=ScrollView(context).apply {
            BicycleComponents.scrollStyle(this);isFillViewport=false
            BicycleComponents.bindTheme(this,themes) {
                background=android.graphics.drawable.GradientDrawable().apply {
                    setColor(it.tertiary);cornerRadius=dp(BicycleDesign.inputRadius).toFloat()
                }
            }
            clipToOutline=true;addView(section)
        }
        dropdown.contentView=scroll
        dropdown.width=header.width.coerceAtLeast(dp(BicycleDesign.touchSize))
        val available=dropdown.getMaxAvailableHeight(header).coerceAtLeast(dp(BicycleDesign.touchSize))
        scroll.measure(View.MeasureSpec.makeMeasureSpec(dropdown.width,View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(available,View.MeasureSpec.AT_MOST))
        dropdown.height=scroll.measuredHeight.coerceIn(dp(BicycleDesign.touchSize),available)
        header.stateDescription=context.getString(R.string.zone_expanded)
        dropdown.setOnDismissListener {
            scroll.removeView(section)
            header.stateDescription=context.getString(R.string.zone_collapsed)
            if(groupPopup===dropdown) groupPopup=null
        }
        groupPopup=dropdown
        // Native positioning keeps the triggering title outside the dropdown, flipping above if needed.
        dropdown.showAsDropDown(header,0,0,Gravity.START)
    }
    fun show(anchor:View) {
        val (frame,w,h)=dimensions(anchor);width=w;height=h
        val position=IntArray(2);anchor.getLocationOnScreen(position)
        val top=(position[1]-height).coerceIn(frame.top,(frame.bottom-height).coerceAtLeast(frame.top))
        setIsClippedToScreen(true);setIsLaidOutInScreen(true)
        showAtLocation(anchor,Gravity.TOP or Gravity.LEFT,frame.left+dp(16),top)
    }
}

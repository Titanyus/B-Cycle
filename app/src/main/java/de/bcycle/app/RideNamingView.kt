package de.bcycle.app

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout

/** Profile-scoped, autosaved name template; native multi-select segments share the zone style. */
internal class RideNamingView(context:Context,themes:ThemeManager,private val latestRide:()->RouteDetails?={null},private val canEdit:()->Boolean):LinearLayout(context) {
    private var refreshPreview:()->Unit={}
    private lateinit var reorder:TileReorder
    fun refreshLock() {
        if(findViewWithTag<View>("ride_naming_content")?.visibility==VISIBLE) refreshPreview()
        val enabled=canEdit()
        findViewWithTag<android.view.View>("ride_naming_text")?.isEnabled=enabled
        RideNamePart.entries.forEach { findViewWithTag<android.view.View>("ride_name_${it.name}")?.isEnabled=enabled }
    }
    init {
        orientation=VERTICAL;tag="ride_naming"
        val settings=RideNamingSettings(context);var current=settings.read()
        val content=LinearLayout(context).apply { orientation=VERTICAL;visibility=GONE;tag="ride_naming_content" }
        lateinit var toggle:android.widget.Button
        toggle=BicycleComponents.action(context,themes,context.getString(R.string.ride_name_title)) {
            content.visibility=if(content.visibility==VISIBLE) GONE else VISIBLE
            if(content.visibility==VISIBLE) refreshPreview()
            toggle.stateDescription=context.getString(if(content.visibility==VISIBLE) R.string.zone_expanded else R.string.zone_collapsed)
        }.apply { tag="ride_naming_toggle";stateDescription=context.getString(R.string.zone_collapsed) }
        BicycleDialogStyle.accordion(toggle,themes);BicycleDialogStyle.actionIcon(toggle,themes,BicycleIcon.Expand)
        addView(toggle);addView(content)
        val input=BicycleComponents.input(RideNamingInput(context),themes).apply {
            BicycleDialogStyle.control(this,themes);
            tag="ride_naming_text";hint=context.getString(R.string.ride_name_text);contentDescription=hint
            setSingleLine(false);setHorizontallyScrolling(false);gravity=android.view.Gravity.TOP or android.view.Gravity.START
            setRawInputType(android.text.InputType.TYPE_CLASS_TEXT);imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            filters=arrayOf(android.text.InputFilter.LengthFilter(60));setText(current.text);isEnabled=canEdit()
        }
        refreshPreview={
            input.previewSuffix=rideNamingPreview(context,current,latestRide())
            input.hint=if(input.previewSuffix.isEmpty()) context.getString(R.string.ride_name_text) else null
            input.stateDescription=context.getString(R.string.ride_preview_accessibility,current.text+input.previewSuffix)
        }
        refreshPreview()
        content.addView(input,LayoutParams(-1,-2))
        input.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {
                val value=s?.toString().orEmpty()
                if(canEdit() && value.none(Char::isISOControl)) { current=current.copy(text=value);settings.write(current);refreshPreview() }
            }
            override fun afterTextChanged(s:Editable?)=Unit
        })
        val choices=LinearLayout(context).apply { orientation=HORIZONTAL;tag="ride_naming_parts" }
        val scroller=object:android.widget.HorizontalScrollView(context) {
            override fun onMeasure(w:Int,h:Int) {
                val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
                val partWidth=((MeasureSpec.getSize(w)-paddingLeft-paddingRight-gap*(RideNamePart.entries.size-1))/RideNamePart.entries.size)
                    .coerceAtLeast((BicycleDesign.dp(context,BicycleDesign.touchSize)*resources.configuration.fontScale.coerceAtLeast(1f)).toInt())
                for(i in 0 until choices.childCount) {
                    val count=current.order[i].parts.size
                    val p=choices.getChildAt(i).layoutParams
                    val width=partWidth*count+gap*(count-1)
                    if(p.width!=width) { p.width=width;choices.getChildAt(i).layoutParams=p }
                }
                super.onMeasure(w,h)
            }
        }.apply {
            tag="ride_naming_scroller";isHorizontalScrollBarEnabled=false
            addView(choices,android.widget.FrameLayout.LayoutParams(-2,-2))
        }
        lateinit var buildChoices:()->Unit
        reorder=TileReorder(scroller,choices,themes,{ current.order.map { it.ordinal } },{ !canEdit() }) { id,index ->
            val group=RideNameGroup.entries.getOrNull(id)
            val from=current.order.indexOf(group)
            if(!canEdit() || from<0 || index !in current.order.indices || from==index) false
            else {
                current=current.copy(order=current.order.toMutableList().apply { add(index,removeAt(from)) })
                settings.write(current);buildChoices();refreshPreview();true
            }
        }
        buildChoices={
            choices.removeAllViews()
            current.order.forEachIndexed { index,group ->
                val tile=LinearLayout(context).apply {
                    orientation=HORIZONTAL;tag="ride_name_group_${group.name}"
                    setTag(R.id.inline_control_row,true)
                }
                reorder.bind(tile,group.ordinal)
                group.parts.forEach { part ->
                    val button=BicycleSegmentButton(context,themes,multiple=true,standalone=true).apply {
                        tag="ride_name_${part.name}";setText(part.label);textSize=BicycleDesign.analysisLabelSp
                        isChecked=part in current.parts;isEnabled=canEdit()
                        contentDescription=context.getString(part.label)
                        stackedIcon(part.icon)
                        maxLines=1
                        setAutoSizeTextTypeUniformWithConfiguration(BicycleDesign.analysisUnitMinSp,BicycleDesign.analysisLabelSp.toInt(),1,android.util.TypedValue.COMPLEX_UNIT_SP)
                        setOnClickListener { if(canEdit()) {
                            current=current.copy(parts=if(isChecked) current.parts+part else current.parts-part)
                            settings.write(current);refreshPreview()
                        } }
                    }
                    tile.addView(button,LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.rideNamePartHeight),1f).apply {
                        marginStart=if(part==group.parts.first()) 0 else BicycleDesign.dp(context,BicycleDesign.controlGap)
                    })
                    reorder.bind(button,group.ordinal,tile)
                }
                val width=BicycleDesign.dp(context,BicycleDesign.rideNamePartWidth)*group.parts.size+
                    BicycleDesign.dp(context,BicycleDesign.controlGap)*(group.parts.size-1)
                choices.addView(tile,LayoutParams(width,-2).apply {
                    marginStart=if(index==0) 0 else BicycleDesign.dp(context,BicycleDesign.controlGap)
                })
            }
        }
        buildChoices()
        content.addView(scroller,LayoutParams(-1,-2))
        BicycleComponents.controlSpacing(scroller)
    }
}

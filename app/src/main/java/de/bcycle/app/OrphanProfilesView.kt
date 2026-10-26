package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.*

/** Shared form for the profile button and manager offer. No persistence until Zuordnen is pressed. */
internal class OrphanProfilesView(context:Context,private val themes:ThemeManager,
    private val assign:(OrphanProfileGroup,String,BikeType)->Unit):LinearLayout(context) {
    private data class Target(val id:String,val name:String,val type:BikeType)
    private val choices=mutableMapOf<String,String>()
    private val initialized=mutableSetOf<String>()
    private val expanded=mutableMapOf<String,Boolean>()
    private var shown:Pair<List<OrphanProfileGroup>,List<Target>>?=null
    private var locked=false
    private val locks=mutableListOf<()->Unit>()
    private val status=BicycleFeedback.status(context,themes).apply { tag="profile_assignment_status" }
    init {
        orientation=VERTICAL;tag="orphan_profiles"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
    }
    fun failed() { BicycleFeedback.update(status,context.getString(R.string.profile_assignment_changed)) }
    fun show(groups:List<OrphanProfileGroup>,profiles:List<BikeProfile>,working:Boolean) {
        locked=working
        val prefs=context.getSharedPreferences("bike",0)
        val targets=profiles.map { Target(BikeIdentities.id(prefs,it.slot),it.name,it.type) }
        val signature=groups to targets
        if(shown==signature) { locks.forEach { it() };return }
        shown=signature;removeAllViews();locks.clear()
        choices.keys.retainAll(groups.map { it.key }.toSet())
        initialized.retainAll(groups.map { it.key }.toSet())
        addView(BicycleComponents.description(context,themes,context.getString(R.string.profile_assignment_intro,groups.sumOf { it.rides.size })))
        addView(status);BicycleFeedback.update(status,"")
        groups.forEach { group ->
            val title=context.getString(R.string.profile_assignment_group,group.name ?: context.getString(R.string.profile_assignment_unknown),group.rides.size)
            val body=LinearLayout(context).apply {
                orientation=VERTICAL;tag="orphan_body_${group.key}"
                visibility=if(expanded.getOrPut(group.key) { groups.size==1 }) VISIBLE else GONE
            }
            val toggle=BicycleComponents.action(context,themes,title) {
                val open=body.visibility!=VISIBLE;body.visibility=if(open) VISIBLE else GONE
                expanded[group.key]=open
            }.apply { tag="orphan_group_${group.key}" }
            BicycleDialogStyle.accordion(toggle,themes)
            BicycleDialogStyle.actionIcon(toggle,themes,BicycleIcon.Expand)
            fun state() { toggle.stateDescription=context.getString(if(body.visibility==VISIBLE) R.string.zone_expanded else R.string.zone_collapsed) }
            toggle.setOnClickListener { body.visibility=if(body.visibility==VISIBLE) GONE else VISIBLE;expanded[group.key]=body.visibility==VISIBLE;state() };state()
            addView(toggle);BicycleComponents.controlSpacing(toggle)
            group.rides.forEach { ride -> body.addView(BicycleComponents.text(context,themes,ride.name).apply { tag="orphan_ride_${ride.id}" }) }
            body.addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.profile_assignment_target)))
            if(initialized.add(group.key)) OrphanRideProfiles.suggestion(group,profiles)?.let { choices[group.key]=BikeIdentities.id(prefs,it.slot) }
            val picker=Spinner(context).apply {
                tag="orphan_target_${group.key}";contentDescription=context.getString(R.string.profile_assignment_target)
                adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,
                    listOf(context.getString(R.string.profile_assignment_choose))+targets.map { it.name+" · "+context.getString(it.type.label) })
                setSelection(targets.indexOfFirst { it.id==choices[group.key] }+1)
            }
            BicycleDialogStyle.spinner(picker,themes);body.addView(picker);BicycleComponents.controlSpacing(picker)
            val warning=BicycleComponents.description(context,themes,"").apply { tag="orphan_type_warning_${group.key}" }
            body.addView(warning)
            val button=BicycleComponents.action(context,themes,context.getString(R.string.profile_assignment_apply)) {
                targets.getOrNull(picker.selectedItemPosition-1)?.takeUnless { locked }?.let { assign(group,it.id,it.type) }
            }.apply { tag="orphan_apply_${group.key}" }
            body.addView(button);BicycleComponents.controlSpacing(button)
            fun update() {
                val target=targets.getOrNull(picker.selectedItemPosition-1)
                picker.isEnabled=!locked;button.isEnabled=!locked && target!=null
                val change=target!=null && group.rides.any { it.details.bikeType!=null && it.details.bikeType!=target.type }
                warning.visibility=if(change) VISIBLE else GONE
                warning.text=if(change) context.getString(R.string.profile_assignment_type,context.getString(target!!.type.label)) else ""
            }
            picker.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) {
                    targets.getOrNull(position-1)?.let { choices[group.key]=it.id } ?: choices.remove(group.key);update()
                }
                override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            }
            locks+=::update;update()
            addView(body,LayoutParams(-1,-2).apply { bottomMargin=BicycleDesign.dp(context,BicycleDesign.sectionCardGap) })
        }
    }
}

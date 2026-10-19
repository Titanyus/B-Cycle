package de.bcycle.app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Deliberately small test screen; loading and library mutation remain outside the view. */
internal class DreeveImportView(context:Context,private val themes:ThemeManager,
    private val loadPage:(Int)->Unit,private val importSelection:(List<DreeveActivity>)->Unit,configure:()->Unit):LinearLayout(context) {
    private val activities=mutableListOf<DreeveActivity>()
    private val selected=mutableSetOf<String>()
    private val existing=mutableSetOf<String>()
    private val results=mutableMapOf<String,DreeveItemResult>()
    private var loading=false;private var importing=false;private var nextPage:Int?=null
    private val status=BicycleFeedback.status(context,themes).apply { tag="dreeve_import_status" }
    private val more=BicycleComponents.action(context,themes,context.getString(R.string.dreeve_import_more)) { nextPage?.let(::requestPage) }
        .apply { tag="dreeve_import_more";visibility=GONE }
    private val reload=BicycleFooterAction(context,themes,BicycleIcon.Reset,context.getString(R.string.dreeve_import_reload)) { refresh() }.apply { tag="dreeve_import_reload" }
    private val importAction=BicycleFooterAction(context,themes,BicycleIcon.Import,"") {
        val items=activities.filter { it.id in selected && it.hasGpx && it.id !in existing }
        if(items.isNotEmpty() && !loading && !importing) { importing=true;controls();adapter.notifyDataSetChanged();importSelection(items) }
    }.apply { tag="dreeve_import_selected" }
    private val adapter=object:BaseAdapter() {
        override fun getCount()=activities.size
        override fun getItem(position:Int)=activities[position]
        override fun getItemId(position:Int)=position.toLong()
        override fun getView(position:Int,recycled:View?,parent:ViewGroup):View {
            val item=activities[position]
            val row=(recycled as? CheckBox) ?: CheckBox(context).apply {
                textSize=BicycleDesign.bodySp;minHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
                val gap=BicycleDesign.dp(context,BicycleDesign.controlGap);setPadding(gap,gap,gap,gap)
                BicycleDialogStyle.control(this,themes)
            }
            row.setOnCheckedChangeListener(null)
            row.tag="dreeve_activity_${item.id}"
            val result=results[item.id]
            val state=when {
                result?.state==DreeveImportState.IMPORTED->context.getString(R.string.dreeve_import_imported)+
                    result.fields.takeIf { it.isNotEmpty() }?.joinToString(", "," · ") { context.getString(it.label) }.orEmpty()
                item.id in existing->context.getString(R.string.dreeve_import_exists)
                !item.hasGpx->context.getString(R.string.dreeve_import_no_gpx)
                result?.state==DreeveImportState.FAILED->context.getString(result.problem ?: R.string.dreeve_import_download_failed)
                else->context.getString(R.string.dreeve_import_gpx)
            }
            val date=runCatching { LocalDateTime.parse(item.startLocal).format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)) }
                .getOrDefault(item.startLocal.ifBlank { "—" })
            val distance=item.distanceM?.let { String.format(Locale.getDefault(),"%.1f km",it/1000) }.orEmpty()
            row.text=listOf(item.name,listOf(date,distance,item.sportType).filter { it.isNotBlank() }.joinToString(" · "),state).joinToString("\n")
            row.isChecked=item.id in selected
            row.isEnabled=item.hasGpx && item.id !in existing && !loading && !importing
            row.alpha=if(row.isEnabled) 1f else .5f
            row.setOnCheckedChangeListener { _,on -> if(on) selected.add(item.id) else selected.remove(item.id);controls() }
            return row
        }
    }
    init {
        orientation=VERTICAL;tag="dreeve_import_screen"
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
        addView(BicycleComponents.description(context,themes,context.getString(R.string.dreeve_import_description)),LayoutParams(-1,-2).apply { setMargins(gap,gap,gap,gap) })
        addView(status,LayoutParams(-1,-2).apply { setMargins(gap,0,gap,gap) })
        addView(ListView(context).apply {
            tag="dreeve_import_list";BicycleComponents.scrollStyle(this)
            addFooterView(more,null,false);adapter=this@DreeveImportView.adapter
        },LayoutParams(-1,0,1f))
        addView(bicycleFooter(context,themes).apply {
            addView(reload,LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.footerHeight),1f))
            addView(importAction,LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.footerHeight),1f))
            addView(BicycleFooterAction(context,themes,BicycleIcon.SettingsOptions,context.getString(R.string.dreeve_import_configure),click=configure).apply { tag="dreeve_import_configure" },LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.footerHeight),1f))
        },LayoutParams(-1,-2))
        controls()
    }
    private fun controls() {
        reload.isEnabled=!loading && !importing
        more.isEnabled=!loading && !importing
        importAction.isEnabled=selected.isNotEmpty() && !loading && !importing
        importAction.setLabel(context.getString(R.string.dreeve_import_selection,selected.size))
    }
    fun refresh() { if(!loading && !importing) requestPage(1) }
    private fun requestPage(page:Int) {
        loading=true;controls();adapter.notifyDataSetChanged()
        BicycleFeedback.update(status,context.getString(R.string.dreeve_import_loading))
        loadPage(page)
    }
    fun loaded(page:DreeveActivityPage,known:Set<String>) {
        if(page.page==1) { activities.clear();selected.clear();existing.clear();results.clear() }
        val ids=activities.map { it.id }.toSet()
        activities.addAll(page.activities.filter { it.id !in ids });existing.addAll(known);selected.removeAll(existing)
        nextPage=if(page.page<page.totalPages) page.page+1 else null
        more.visibility=if(nextPage!=null) VISIBLE else GONE
        loading=false;controls();adapter.notifyDataSetChanged()
        BicycleFeedback.update(status,context.getString(R.string.dreeve_import_loaded,activities.size,page.total))
    }
    fun failed(problem:Int) {
        loading=false;importing=false;controls();adapter.notifyDataSetChanged()
        BicycleFeedback.update(status,context.getString(problem))
    }
    fun progress(item:DreeveItemResult) {
        results[item.id]=item
        if(item.state in setOf(DreeveImportState.IMPORTED,DreeveImportState.DUPLICATE)) { existing.add(item.id);selected.remove(item.id) }
        controls();adapter.notifyDataSetChanged()
    }
    fun complete(result:Result<DreeveImportResult>) {
        result.fold({ summary ->
            summary.items.forEach(::progress);importing=false;controls();adapter.notifyDataSetChanged()
            BicycleFeedback.update(status,context.getString(R.string.dreeve_import_result,summary.count(DreeveImportState.IMPORTED),
                summary.count(DreeveImportState.DUPLICATE),summary.count(DreeveImportState.FAILED)))
        },{ failed(R.string.dreeve_import_download_failed) })
    }
}

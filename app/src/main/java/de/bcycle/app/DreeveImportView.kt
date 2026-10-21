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
    private val loadPage:(Int)->Unit,private val importSelection:(List<DreeveActivity>)->Unit,
    val archiveMode:Boolean=false):LinearLayout(context) {
    var navigation:DialogNavigation?=null
    var profileFor:(DreeveActivity)->Int?={null}
    var gearNames:Map<String,String> = emptyMap()
    private val query=RouteQuery()
    private var onlyNotImported=false
    private var routeSections=RouteListSections(emptyList(),RouteSort.DATE,context.getString(R.string.ui_field_missing_value))
    private var queryPopup:RouteQueryPopup?=null
    private lateinit var filterButton:View
    private lateinit var sortButton:View
    private lateinit var selectAllButton:View
    private var visible=emptyList<DreeveActivity>()
    private val activities=mutableListOf<DreeveActivity>()
    private val selected=mutableSetOf<String>()
    private val existing=mutableSetOf<String>()
    private val results=mutableMapOf<String,DreeveItemResult>()
    private var loading=false;private var importing=false;private var nextPage:Int?=null
    private val status=BicycleFeedback.status(context,themes).apply { tag="dreeve_import_status" }
    private val preparation=BicycleComponents.text(context,themes,context.getString(R.string.archive_preparing)).apply {
        tag="archive_preparing";gravity=android.view.Gravity.CENTER;visibility=GONE
        accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    private lateinit var list:ListView
    private val more=BicycleComponents.action(context,themes,context.getString(R.string.dreeve_import_more)) { nextPage?.let(::requestPage) }
        .apply { tag="dreeve_import_more";visibility=GONE }
    private val reload=BicycleFooterAction(context,themes,BicycleIcon.Reset,context.getString(R.string.dreeve_import_reload)) { refresh() }.apply { tag="dreeve_import_reload" }
    private val importAction=BicycleFooterAction(context,themes,BicycleIcon.Import,"") {
        val items=activities.filter { it.id in selected && it.id !in existing }
        if(items.isNotEmpty() && !loading && !importing) { importing=true;controls();adapter.notifyDataSetChanged();importSelection(items) }
    }.apply { tag="dreeve_import_selected" }
    private val adapter=object:BaseAdapter(),SectionIndexer {
        override fun getSections():Array<String> = routeSections.labels
        override fun getPositionForSection(sectionIndex:Int)=routeSections.position(sectionIndex)
        override fun getSectionForPosition(position:Int)=routeSections.section(position)
        override fun getCount()=visible.size
        override fun getItem(position:Int)=visible[position]
        override fun getItemId(position:Int)=position.toLong()
        override fun getView(position:Int,recycled:View?,parent:ViewGroup):View {
            val item=visible[position]
            val row=(recycled as? DreeveActivityRow) ?: DreeveActivityRow(context,themes)
            val box=row.box
            box.setOnCheckedChangeListener(null)
            row.tag="dreeve_row_${item.id}";box.tag="dreeve_activity_${item.id}"
            val result=results[item.id]
            val state=when {
                result?.state==DreeveImportState.IMPORTED && result.summaryOnly->context.getString(R.string.dreeve_import_summary_imported)
                result?.state==DreeveImportState.IMPORTED->context.getString(R.string.dreeve_import_imported)+
                    result.fields.takeIf { it.isNotEmpty() }?.joinToString(", "," · ") { context.getString(it.label) }.orEmpty()
                item.id in existing->context.getString(R.string.dreeve_import_exists)
                !item.hasGpx->context.getString(R.string.dreeve_import_no_gpx)
                result?.state==DreeveImportState.FAILED->context.getString(result.problem ?: R.string.dreeve_import_download_failed)
                else->context.getString(R.string.dreeve_import_gpx)
            }
            val date=runCatching { LocalDateTime.parse(item.startLocal).let { start ->
                start.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT))+" "+start.format(DateTimeFormatter.ofPattern("HH:mm"))
            } }
                .getOrDefault(item.startLocal.substringBefore('T').ifBlank { "—" })
            val distance=item.distanceM?.let { String.format(Locale.getDefault(),"%.1f km",it/1000) } ?: "—"
            val duration=(item.summary.metrics[AnalysisMetric.ELAPSED] ?: item.summary.metrics[AnalysisMetric.MOVING])?.toLong()?.let {
                context.getString(R.string.dreeve_duration_seconds,it/3600,it/60%60,it%60)
            } ?: "—"
            val profile=profileFor(item)?.let { slot -> BikeProfileStore(context,context.getSharedPreferences("bike",0)).all().find { it.slot==slot } }
            val bike=profile?.name ?: item.summary.gearId?.let { gearNames[it] ?: it } ?: context.getString(R.string.dreeve_bike_unknown)
            val type=when(profile?.type ?: item.bikeType) {
                BikeType.BIKE->context.getString(R.string.bikes_type_bike)
                BikeType.EBIKE->context.getString(R.string.bikes_type_ebike)
                BikeType.INDOOR->context.getString(R.string.bikes_type_indoor)
                null->item.sportType.ifBlank { context.getString(R.string.dreeve_type_unknown) }
            }
            val gpx=if(item.fileFormat!=null) context.getString(R.string.archive_file_available,item.fileFormat)
                else context.getString(if(item.hasGpx) R.string.dreeve_import_gpx else R.string.dreeve_gpx_unavailable)
            val outcome=when {
                item.id in existing->context.getString(R.string.dreeve_import_imported)
                result?.state==DreeveImportState.FAILED->context.getString(R.string.dreeve_row_failed)
                else->null
            }
            val lines=listOf(item.name,"$date · $distance · $duration",context.getString(R.string.dreeve_bike_type,bike,type),
                listOfNotNull(gpx,outcome).joinToString(" · "))
            row.labels.forEachIndexed { i,label -> label.text=lines[i] }
            box.contentDescription=lines.joinToString("; ")+"; "+state
            box.isChecked=item.id in selected || item.id in existing
            box.isEnabled=item.id !in existing && !loading && !importing
            row.isEnabled=box.isEnabled;row.alpha=if(box.isEnabled) 1f else .5f
            box.setOnCheckedChangeListener { _,on -> if(on) selected.add(item.id) else selected.remove(item.id);controls() }
            row.setOnClickListener { if(box.isEnabled) box.performClick() }
            return row
        }
    }
    init {
        orientation=VERTICAL;tag=if(archiveMode) "archive_import_screen" else "dreeve_import_screen"
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        val gap=BicycleDesign.dp(context,BicycleDesign.horizontalPadding)
        addView(status,LayoutParams(-1,-2).apply { setMargins(gap,gap,gap,gap) })
        list=BicycleFastScrollList(context,themes).apply {
            val edge=BicycleDesign.dp(context,BicycleDesign.horizontalPadding)
            setPadding(edge,0,edge,BicycleDesign.dp(context,BicycleDesign.pageContentGap));clipToPadding=false
            tag="dreeve_import_list";BicycleComponents.scrollStyle(this)
            addFooterView(more,null,false);adapter=this@DreeveImportView.adapter
        }
        addView(FrameLayout(context).apply {
            addView(list,FrameLayout.LayoutParams(-1,-1))
            addView(preparation,FrameLayout.LayoutParams(-1,-2,android.view.Gravity.CENTER))
        },LayoutParams(-1,0,1f))
        addView(bicycleFooter(context,themes).apply {
            addView(BicycleFooterAction(context,themes,BicycleIcon.Filter,context.getString(R.string.route_filter_button)) { queryAction(true) }.apply { tag="dreeve_filters";filterButton=this },LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.footerHeight),1f))
            addView(BicycleFooterAction(context,themes,BicycleIcon.Sort,context.getString(R.string.route_sort_button)) { queryAction(false) }.apply { tag="dreeve_sort";sortButton=this },LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.footerHeight),1f))
            addView(BicycleFooterAction(context,themes,BicycleIcon.Check,context.getString(R.string.dreeve_select_filtered)) {
                if(!loading && !importing) {
                    val ids=visible.filter { it.id !in existing }.map { it.id }.toSet()
                    if(selected.containsAll(ids)) selected.removeAll(ids) else selected.addAll(ids)
                    controls();adapter.notifyDataSetChanged()
                }
            }.apply { tag="dreeve_select_filtered";selectAllButton=this },LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.footerHeight),1f))
            addView(reload,LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.footerHeight),1f))
            addView(importAction,LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.footerHeight),1f))
        },LayoutParams(-1,-2))
        controls()
    }
    private fun refreshQuery() {
        val indexed=activities.associateBy { it.id }
        visible=query.apply(activities.filter { !onlyNotImported || it.id !in existing }.map { StoredDreeveSummary(it.id,it).managed() },{ id -> indexed[id]?.let(profileFor) }).mapNotNull { indexed[it.id] }
        routeSections=RouteListSections(visible.map { StoredDreeveSummary(it.id,it).managed() },query.sort,context.getString(R.string.ui_field_missing_value))
        adapter.notifyDataSetChanged()
    }
    private fun queryAction(filters:Boolean) {
        val navigation=navigation ?: return
        val anchor=if(filters) filterButton else sortButton
        val routes=activities.map { StoredDreeveSummary(it.id,it).managed() }
        queryPopup?.dismiss()
        queryPopup=RouteQueryPopup(context,themes,navigation,query,routes,{ id -> activities.find { it.id==id }?.let(profileFor) }) { refreshQuery() }.also {
            if(filters) {
                it.importStatus({onlyNotImported}) { value -> onlyNotImported=value }
                it.filters { onlyNotImported=false;refreshQuery();queryAction(true) }
            } else it.sorting()
            it.show(anchor)
        }
    }
    override fun onDetachedFromWindow() { queryPopup?.dismiss();super.onDetachedFromWindow() }
    private fun controls() {
        if(::filterButton.isInitialized) filterButton.isEnabled=!loading && !importing
        if(::sortButton.isInitialized) sortButton.isEnabled=!loading && !importing
        if(::selectAllButton.isInitialized) selectAllButton.isEnabled=!loading && !importing
        reload.isEnabled=!loading && !importing
        more.isEnabled=!loading && !importing
        importAction.isEnabled=selected.isNotEmpty() && !loading && !importing
        importAction.setLabel(context.getString(R.string.dreeve_import_selection,selected.size))
    }
    fun cancelImport() { importing=false;controls();adapter.notifyDataSetChanged() }
    fun preparingArchive() {
        loading=true;controls();list.visibility=INVISIBLE;preparation.visibility=VISIBLE
        BicycleFeedback.update(status,"")
    }
    fun refresh() { if(!loading && !importing) requestPage(1) }
    private fun requestPage(page:Int) {
        loading=true;controls();adapter.notifyDataSetChanged()
        BicycleFeedback.update(status,context.getString(R.string.dreeve_import_loading))
        loadPage(page)
    }
    fun loaded(page:DreeveActivityPage,known:Set<String>,continuing:Boolean=false) {
        preparation.visibility=GONE;list.visibility=VISIBLE
        if(page.page==1) { activities.clear();selected.clear();existing.clear();results.clear() }
        val ids=activities.map { it.id }.toSet()
        activities.addAll(page.activities.filter { it.id !in ids });existing.addAll(known);selected.removeAll(existing)
        nextPage=if(page.page<page.totalPages) page.page+1 else null
        more.visibility=if(nextPage!=null) VISIBLE else GONE
        loading=continuing;controls();refreshQuery()
        BicycleFeedback.update(status,context.getString(R.string.dreeve_import_loaded,activities.size))
    }
    fun failed(problem:Int) {
        preparation.visibility=GONE;list.visibility=VISIBLE
        loading=false;importing=false;controls();adapter.notifyDataSetChanged()
        BicycleFeedback.update(status,context.getString(problem))
    }
    fun progress(item:DreeveItemResult) {
        results[item.id]=item
        if(item.state in setOf(DreeveImportState.IMPORTED,DreeveImportState.DUPLICATE)) { existing.add(item.id);selected.remove(item.id) }
        controls();refreshQuery()
    }
    fun complete(result:Result<DreeveImportResult>) {
        result.fold({ summary ->
            summary.items.forEach(::progress);importing=false;controls();adapter.notifyDataSetChanged()
            BicycleFeedback.update(status,context.getString(R.string.dreeve_import_result,summary.count(DreeveImportState.IMPORTED),
                summary.count(DreeveImportState.DUPLICATE),summary.count(DreeveImportState.FAILED)))
        },{ failed(R.string.dreeve_import_download_failed) })
    }
}

/** Separate labels make title ellipsizing independent of the remaining three information lines. */
internal class DreeveActivityRow(context:Context,themes:ThemeManager):LinearLayout(context) {
    val box=CheckBox(context).apply { BicycleDialogStyle.control(this,themes) }
    val labels=List(4) { index -> BicycleComponents.description(context,themes,"").apply {
        maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
        tag=if(index==0) "dreeve_row_title" else "dreeve_row_line_$index"
        includeFontPadding=false
        BicycleDialogStyle.control(this,themes)
        if(index==0) typeface=BicycleDesign.strongTypeface
    } }
    init {
        orientation=HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL
        val gap=BicycleDesign.dp(context,BicycleDesign.smallGap);setPadding(0,gap,0,gap)
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(box,LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize)))
        addView(LinearLayout(context).apply {
            orientation=VERTICAL;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            labels.forEach { addView(it,LayoutParams(-1,-2)) }
        },LayoutParams(0,-2,1f))
    }
}

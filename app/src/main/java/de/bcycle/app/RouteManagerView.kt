package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos

/** Fixed header, one native recycled list, no per-row decoding or disk/network I/O. */
internal class RouteManagerView(context: Context,
    private val toggle: (ManagedRoute) -> Unit,
    private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    private val details: (ManagedRoute) -> Unit = {}) : LinearLayout(context) {
    private fun dp(value: Int) = BicycleDesign.dp(context, value)
    internal var fullscreen = false
    private var all: List<ManagedRoute> = emptyList()
    private var filtered: List<ManagedRoute> = emptyList()
    private var selected: String? = null
    private var busy = false
    private var importStatus = ""
    internal val query=RouteQuery()
    var queryChanged:()->Unit={}
    var selectionChanged:()->Unit={}
    private val checked=linkedSetOf<String>()
    val checkedIds get()=checked.toSet()
    val selecting get()=checked.isNotEmpty()
    fun clearSelection() { checked.clear();updateStatus();rows.notifyDataSetChanged();selectionChanged() }
    private fun toggleSelection(id:String) { if(!checked.add(id)) checked.remove(id);updateStatus();rows.notifyDataSetChanged();selectionChanged() }
    fun refreshQuery() { updateRows();queryChanged() }
    private var queryPopup:RouteQueryPopup?=null
    fun showFilters(anchor:View,navigation:DialogNavigation) {
        queryPopup?.dismiss()
        queryPopup=RouteQueryPopup(context,themes,navigation,query,all) { refreshQuery() }.also {
            it.filters { showFilters(anchor,navigation) };it.show(anchor)
        }
    }
    fun showSort(anchor:View,navigation:DialogNavigation) {
        queryPopup?.dismiss()
        queryPopup=RouteQueryPopup(context,themes,navigation,query,all) { refreshQuery() }.also { it.sorting();it.show(anchor) }
    }
    override fun onDetachedFromWindow() { queryPopup?.dismiss();super.onDetachedFromWindow() }
    private val status = BicycleFeedback.status(context, themes).apply {
        tag="route_status"
        setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.horizontalPadding), 0)
    }
    private var routeSections=RouteListSections(emptyList(),RouteSort.DATE,context.getString(R.string.ui_field_missing_value))
    private val rows = object : BaseAdapter(), SectionIndexer {
        override fun getSections():Array<String> = routeSections.labels
        override fun getPositionForSection(sectionIndex:Int)=routeSections.position(sectionIndex)
        override fun getSectionForPosition(position:Int)=routeSections.section(position)
        override fun getCount() = filtered.size
        override fun getItem(position: Int) = filtered[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, recycled: View?, parent: ViewGroup): View =
            ((recycled as? Card) ?: Card()).apply { bind(filtered[position]) }
    }
    init {
        orientation = VERTICAL
        BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
        val header = BicycleComponents.header(context, themes).apply { tag = "route_manager_header" }
        header.setPadding(dp(BicycleDesign.verticalPadding), 0, dp(BicycleDesign.verticalPadding), 0)
        header.addView(FrameLayout(context).apply {
            addView(BicycleComponents.heading(context, themes, context.getString(R.string.ui_route_manager_title)), FrameLayout.LayoutParams(-1, -1))
        }, LayoutParams(-1, -1))
        addView(header, LayoutParams(-1, dp(BicycleDesign.headerHeight)))
        addView(status, LayoutParams(-1, -2))
        addView(BicycleFastScrollList(context,themes).apply {
            tag = "route_list"; adapter = rows; dividerHeight = BicycleDesign.linePixels(context)
            setPadding(dp(BicycleDesign.verticalPadding), 0, dp(BicycleDesign.verticalPadding), 0)
            BicycleComponents.scrollStyle(this)
            BicycleComponents.bindTheme(this, themes) {
                divider = ColorDrawable(it.foreground).apply { alpha=(255*BicycleDesign.dividerAlpha).toInt() }; dividerHeight = BicycleDesign.linePixels(context)
                verticalScrollbarThumbDrawable = ColorDrawable(it.foreground)
                selector = ColorDrawable(it.tertiary)
            }
        }, LayoutParams(-1, 0, 1f))
    }
    internal fun applyPageSpacing() {
        // The initial gap scrolls away with the first row, rather than masking the list below the header.
        setPadding(paddingLeft,0,paddingRight,paddingBottom)
        findViewWithTag<ListView>("route_list").apply {
            setPadding(paddingLeft,dp(BicycleDesign.pageContentGap),paddingRight,paddingBottom)
            clipToPadding=false
        }
    }
    fun submit(entries: List<ManagedRoute>, active: String?, working: Boolean) {
        all = entries; selected = active; busy = working;checked.retainAll(entries.map { it.id }.toSet());updateRows();selectionChanged()
    }
    fun showImportStatus(text: String) {
        if (importStatus == text) return
        importStatus = text
        updateStatus()
    }
    private fun updateStatus() {
        BicycleFeedback.update(status, if(selecting) context.getString(R.string.route_selection_count,checked.size) else importStatus.ifEmpty {
            if (!busy && filtered.isEmpty()) context.getString(R.string.ui_routes_empty) else ""
        }, hideWhenEmpty = true)
    }
    private fun updateRows() {
        filtered = query.apply(all,{ RideProfileColor.slot(context,it) })
        updateStatus()
        routeSections=RouteListSections(filtered,query.sort,context.getString(R.string.ui_field_missing_value))
        rows.notifyDataSetChanged()
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (fullscreen) { super.onMeasure(widthMeasureSpec, heightMeasureSpec); return }
        val desired = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) dp(BicycleDesign.managerDefaultHeight)
            else minOf(MeasureSpec.getSize(heightMeasureSpec), dp(BicycleDesign.managerMaxHeight))
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY))
    }
    private inner class Card : LinearLayout(context) {
        private var entry: ManagedRoute? = null
        private val preview = RoutePreviewView(context)
        private val profileColor=View(context).apply { visibility=GONE;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
        private val origin = ImageView(context)
        private val format = BicycleComponents.text(context, themes, "").apply { textSize = BicycleDesign.formatSp;setSingleLine() }
        private val title = BicycleComponents.text(context, themes, "").apply {
            textSize = BicycleDesign.rowTitleSp; maxLines = 1; ellipsize = TextUtils.TruncateAt.END; gravity = Gravity.CENTER_VERTICAL
        }
        private val eye = BicycleComponents.icon(context, themes, BicycleIcon.ShowOnMap,
            context.getString(R.string.route_list_visibility)) { entry?.takeIf { !busy && it.hasRoute && it.locallySaved }?.let(toggle) }
        private inner class Reading(labelId: Int? = null, unitId: Int? = null) : LinearLayout(context) {
            val number = BicycleComponents.text(context, themes, "").apply {
                typeface = BicycleDesign.narrowTypeface(context); textSize = BicycleDesign.routeMetricSp
                gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
                setAutoSizeTextTypeUniformWithConfiguration(if (labelId == null) 10 else 12, BicycleDesign.routeMetricSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            }
            val unit = TextView(context).apply {
                text = unitId?.let(context::getString).orEmpty(); typeface = BicycleDesign.narrowTypeface(context)
                textSize = BicycleDesign.formatSp; gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
                BicycleComponents.secondaryText(this,themes)
                setAutoSizeTextTypeUniformWithConfiguration(8, BicycleDesign.formatSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            }
            val label = BicycleComponents.text(context, themes, labelId?.let(context::getString).orEmpty()).apply {
                textSize = BicycleDesign.routeMetricLabelSp; gravity = Gravity.CENTER; includeFontPadding = false
                maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            }
            init {
                orientation = VERTICAL
                addView(AnalysisValueRow(context, number, unit), LayoutParams(-1, dp(BicycleDesign.routeMetricHeight)))
                addView(label, LayoutParams(-1, -2))
            }
            fun bind(value: String?, id: String, kind: String) {
                number.text = value.orEmpty(); number.tag = "route_${kind}_$id"
                unit.visibility = if (value == null || unit.text.isEmpty()) GONE else VISIBLE
            }
        }
        private val date = Reading()
        private val distance = Reading(R.string.route_list_distance, R.string.analysis_km)
        private val moving = Reading(R.string.route_list_moving, R.string.analysis_hours)
        private val average = Reading(R.string.route_list_average, R.string.analysis_kmh)
        private val metrics = LinearLayout(context).apply {
            orientation = HORIZONTAL
            listOf(date, distance, moving, average).forEachIndexed { index, reading ->
                if (index > 0) addView(View(context).apply {
                    tag = "route_metric_divider"; alpha = BicycleDesign.dividerAlpha
                    BicycleComponents.bindTheme(this, themes) { setBackgroundColor(it.foreground) }
                }, LayoutParams(BicycleDesign.linePixels(context), -1))
                addView(reading, LayoutParams(0, -2, 1f))
            }
        }
        init {
            orientation = VERTICAL; gravity = Gravity.TOP
            setOnClickListener { entry?.takeIf { !busy }?.let { if(selecting) toggleSelection(it.id) else details(it) } }
            setOnLongClickListener { entry?.takeIf { !busy }?.let { toggleSelection(it.id) };true }
            setPadding(dp(BicycleDesign.verticalPadding), dp(BicycleDesign.rowGap), 0, dp(BicycleDesign.rowGap))
            addView(LinearLayout(context).apply {
                orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
                // The identity prefix owns its natural width, independently of the tiny map preview.
                addView(LinearLayout(context).apply {
                    gravity=Gravity.CENTER_VERTICAL
                    addView(profileColor,LayoutParams(dp(5),dp(24)).apply { marginEnd=dp(BicycleDesign.controlGap) })
                    addView(origin,LayoutParams(dp(BicycleDesign.iconSize),dp(BicycleDesign.iconSize)))
                    addView(format,LayoutParams(-2,-2).apply { marginStart=dp(BicycleDesign.verticalPadding) })
                },LayoutParams(-2,dp(BicycleDesign.touchSize)).apply { marginEnd=dp(BicycleDesign.smallGap) })
                addView(title,LayoutParams(0,dp(BicycleDesign.touchSize),1f))
                addView(eye,LayoutParams(dp(BicycleDesign.touchSize),dp(BicycleDesign.touchSize)))
            },LayoutParams(-1,-2))
            addView(LinearLayout(context).apply {
                orientation=HORIZONTAL
                addView(preview,LayoutParams(dp(BicycleDesign.previewWidth),dp(BicycleDesign.previewHeight)).apply { marginEnd=dp(BicycleDesign.rowGap) })
                addView(metrics,LayoutParams(0,-2,1f).apply { topMargin=-dp(BicycleDesign.metadataSpacingTrim)+dp(BicycleDesign.routeMetricTopOffset) })
            },LayoutParams(-1,-2))
            BicycleComponents.bindTheme(this, themes) { paintState(it) }
        }
        override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int) {
            super.onLayout(changed,l,t,r,b)
            if(layoutDirection!=View.LAYOUT_DIRECTION_LTR || date.number.text.isEmpty()) return
            val text=android.graphics.Rect();offsetDescendantRectToMyCoords(date.number,text)
            val heading=android.graphics.Rect();offsetDescendantRectToMyCoords(title,heading)
            val left=text.left+date.number.compoundPaddingLeft+(date.number.layout?.getLineLeft(0) ?: 0f)
            val inset=(left-heading.left).toInt().coerceIn(0,(title.width-dp(BicycleDesign.iconSize)).coerceAtLeast(0))
            if(title.paddingLeft!=inset) title.setPadding(inset,title.paddingTop,title.paddingRight,title.paddingBottom)
        }
        private fun paintState(tokens: ThemeTokens) {
            preview.foreground = tokens.foreground
            val route = entry ?: return
            background=if(route.id in checked) ColorDrawable(tokens.tertiary) else null
            origin.setImageDrawable(BicycleIcons.drawable(context, if(route.id in checked) BicycleIcon.Check else when (route.origin) {
                TrackOrigin.IMPORTED -> BicycleIcon.TrackImported; TrackOrigin.RECORDED -> BikeTypeIcons.forType(route.details.bikeType); TrackOrigin.UNKNOWN -> BicycleIcon.TrackUnknown
            }, if(route.id in checked) tokens.accent else tokens.foreground))
            val eyeKind = if (selected == route.id) BicycleIcon.ActiveOnMap else BicycleIcon.ShowOnMap
            eye.setImageDrawable(BicycleIcons.drawable(context, eyeKind, tokens, selected != route.id))
            eye.setColorFilter(BicycleIcons.color(tokens, eyeKind, selected != route.id))
        }
        fun bind(route: ManagedRoute) {
            isActivated=route.id in checked
            entry = route; tag = "route_entry_${route.id}"; isEnabled = !busy
            contentDescription = context.getString(R.string.analysis_open, route.name)+(if(route.id in checked) " · "+context.getString(R.string.route_selected) else "")
            title.text = route.name; title.tag = "route_name_${route.id}"; title.contentDescription = route.name
            origin.contentDescription = when (route.origin) { TrackOrigin.IMPORTED -> context.getString(R.string.ui_route_imported); TrackOrigin.RECORDED -> context.getString(R.string.ui_route_recorded); TrackOrigin.UNKNOWN -> context.getString(R.string.ui_route_unknown_origin) }
            profileColor.tag="route_profile_color_${route.id}"
            val color=RideProfileColor.color(context,route.id)
            profileColor.visibility=if(color==null) GONE else VISIBLE
            color?.let(profileColor::setBackgroundColor)
            format.text = route.format;format.tag="route_format_${route.id}"; format.contentDescription = context.getString(R.string.ui_file_format_description, route.format)
            val recordedAt = route.details.dateMs.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
            date.bind(recordedAt?.format(DateTimeFormatter.ofPattern(context.getString(R.string.route_list_date_format))), route.id, "date")
            date.label.text = recordedAt?.format(DateTimeFormatter.ofPattern(context.getString(R.string.route_list_clock_format))).orEmpty()
            date.label.tag = "route_clock_${route.id}"
            distance.bind(route.details.distanceM?.takeIf { it.isFinite() && it >= 0 }?.let {
                String.format(Locale.GERMANY, "%.1f", it / 1000)
            }, route.id, "distance")
            moving.bind(route.details.movingMs?.takeIf { it >= 0 && (route.details.durationMs == null || it <= route.details.durationMs) }?.let {
                val minutes = it / 60000; "%d:%02d".format(Locale.GERMANY, minutes / 60, minutes % 60)
            }, route.id, "moving")
            average.bind(route.details.movingAverageKmh?.let { String.format(Locale.GERMANY, "%.1f", it) }, route.id, "average")
            metrics.tag = "route_metrics_${route.id}"
            preview.points = route.details.preview; preview.contentDescription = if (route.hasRoute) context.getString(R.string.ui_route_preview) else context.getString(R.string.ui_route_no_gps)
            eye.visibility=if(route.hasRoute && !selecting) VISIBLE else GONE
            eye.tag = "route_eye_${route.id}"; eye.isEnabled = !busy && route.hasRoute && route.locallySaved
            eye.contentDescription = context.getString(if (selected == route.id) R.string.route_list_hide_named else R.string.route_list_show_named, route.name)
            paintState(themes.active.tokens)
        }
    }
}

internal class RoutePreviewView(context: Context) : View(context) {
    var points: List<MapPoint> = emptyList(); set(value) { field = value; invalidate() }
    var foreground: Int = 0; set(value) { field = value; paint.color = value; invalidate() }
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = foreground; style = Paint.Style.STROKE
        strokeWidth = 2 * resources.displayMetrics.density; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = 4 * resources.displayMetrics.density
        if (points.size < 2) { canvas.drawRect(padding, height * .25f, width - padding, height * .75f, paint); return }
        val origin = points.first().longitude
        val factor = cos(Math.toRadians(points.first().latitude))
        fun x(p: MapPoint): Double = (((p.longitude - origin + 540) % 360) - 180) * factor
        val minX = points.minOf(::x); val maxX = points.maxOf(::x)
        val minY = points.minOf { it.latitude }; val maxY = points.maxOf { it.latitude }
        val scale = minOf((width - padding * 2) / (maxX - minX).coerceAtLeast(.000001),
            (height - padding * 2) / (maxY - minY).coerceAtLeast(.000001))
        path.reset()
        points.forEachIndexed { i, p ->
            val px = (width / 2.0 + (x(p) - (minX + maxX) / 2) * scale).toFloat()
            val py = (height / 2.0 - (p.latitude - (minY + maxY) / 2) * scale).toFloat()
            if (i == 0 || p.segmentStart) path.moveTo(px, py) else path.lineTo(px, py)
        }
        canvas.drawPath(path, paint)
    }
}

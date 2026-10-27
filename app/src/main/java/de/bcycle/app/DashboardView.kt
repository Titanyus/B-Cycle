package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** Native, accessible metric tiles arranged by the selected template. */
class DashboardView(context: Context, private val selectField: (Int) -> Unit) : LinearLayout(context) {
    internal val modeGlow = BoschModeGlow(BicycleDesign.dp(context, BicycleDesign.modeGlowWidth).toFloat())
    fun showModeGlow(enabled: Boolean, mode: Long?, names: List<String>? = null, batteryPercent: Long? = null) {
        modeGlow.update(if (enabled) BoschModeColors.color(mode, names) else null, batteryPercent)
    }
    internal fun resetTrends() { tiles.forEach { it.resetTrend() } }
    private var trendIndicators=TrendIndicatorConfig()
    internal fun showTrendIndicators(config:TrendIndicatorConfig) { trendIndicators=config }
    private val numbers = BicycleDesign.narrowTypeface(context)
    private val captions = BicycleDesign.compactTypeface
    private val tiles = mutableListOf<Tile>()
    var editing = false; private set
    private var editorColor = BicycleThemes.red.tokens.primary
    var activateEditor: (() -> Unit)? = null
    internal fun bindEditorTheme(themes: ThemeManager) {
        BicycleComponents.bindTheme(this, themes) { tokens ->
            editorColor = tokens.primary
            tiles.forEach { it.refreshAppearance() }
            tiles.forEach { (it.container.foreground as? DashboardEditOutline)?.color = editorColor }
        }
    }
    fun setEditing(value: Boolean) {
        editing = value
        tiles.forEach { it.markEditable() }
    }
    private var zoneTwoHelp=false
    private var training = TrainingSnapshot()
    private var zoneConfig = ZoneGlowConfig()
    internal fun showTraining(config: ZoneGlowConfig, snapshot: TrainingSnapshot,zoneTwo:Boolean=false) {
        zoneTwoHelp=zoneTwo;zoneConfig=config;training=snapshot;tiles.forEach { it.updateGlow() }
    }
    private var dataVisibility = DashboardDataVisibility()
    internal fun showDataVisibility(config: DashboardDataVisibility) {
        if(dataVisibility==config) return
        dataVisibility=config;tiles.forEach { it.updateGraph() }
    }
    private var graphConfig = GraphOverlayConfig()
    private var graphFrame: GraphFrame? = null
    var graphWidthChanged: ((Int) -> Unit)? = null
    internal fun showGraphs(config: GraphOverlayConfig, frame: GraphFrame?) {
        graphConfig=config; graphFrame=frame; tiles.forEach { it.updateGraph() }
    }
    private var layout = DashboardLayout.defaults
    private var data = LiveData()
    private var gps = GpsValues()
    private var bikeDistanceM: Long? = null
    private var pulse = PulseValues()
    private var trendSnapshot=RideTrendSnapshot()
    private var landscape = false
    private var viewportWide: Boolean? = null
    private var viewportWidth = 0
    private var captionGeometry = ""
    private var viewportDataHeight = 0
    var template = DashboardTemplate.DATA
        private set
    var changePage: ((Int) -> Unit)? = null

    /** Use the whole dashboard/map viewport so short presets do not look like landscape. */
    fun heightForViewport(width: Int, available: Int): Int {
        val wide = width > DashboardTemplate.DATA.heightForViewport(available, dp(4))
        viewportWide = wide
        viewportWidth = width
        viewportDataHeight = template.heightForViewport(available, dp(4), wide)
        if (wide != landscape) { landscape = wide; buildTiles(wide) }
        return viewportDataHeight
    }

    fun setTemplate(next: DashboardTemplate) {
        if (next == template) return
        template = next; buildTiles(landscape); requestLayout()
    }

    init {
        orientation = VERTICAL
        background = modeGlow
        buildTiles(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if(background===modeGlow) modeGlow.setBounds(0, 0, w, h)
    }

    private fun buildTiles(wide: Boolean) {
        removeAllViews(); tiles.clear(); captionGeometry = ""
        val indexed = List(DashboardLayout.FIELD_COUNT) { Tile(it) }
        tiles.addAll(indexed)
        if (template != DashboardTemplate.DATA) {
            orientation = VERTICAL
            template.rows.forEachIndexed { index, slots ->
                if (index > 0) addView(line(), LayoutParams(LayoutParams.MATCH_PARENT, BicycleDesign.linePixels(context)))
                addView(pair(slots, indexed), LayoutParams(LayoutParams.MATCH_PARENT, 0, template.rowWeight(index, wide)))
            }
            show(layout, data, gps, bikeDistanceM, pulse,trendSnapshot)
            return
        }
        orientation = if (wide) HORIZONTAL else VERTICAL
        if (wide) {
            addView(indexed[2].container, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(line(), LayoutParams(BicycleDesign.linePixels(context), LayoutParams.MATCH_PARENT))
            val right = LinearLayout(context).apply { orientation = VERTICAL }
            for ((rowIndex, indexes) in listOf(listOf(0, 1), listOf(3, 4), listOf(5, 6)).withIndex()) {
                if (rowIndex > 0) right.addView(line(), LayoutParams(LayoutParams.MATCH_PARENT, BicycleDesign.linePixels(context)))
                right.addView(pair(indexes, indexed), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
            }
            addView(right, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            show(layout, data, gps, bikeDistanceM, pulse,trendSnapshot)
            return
        }
        val groups = template.rows
        for ((rowIndex, indexes) in groups.withIndex()) {
            if (rowIndex > 0) addView(line(), LayoutParams(LayoutParams.MATCH_PARENT, BicycleDesign.linePixels(context)))
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; isBaselineAligned = false }
            for ((column, index) in indexes.withIndex()) {
                if (column > 0) row.addView(line(), LayoutParams(BicycleDesign.linePixels(context), LayoutParams.MATCH_PARENT))
                val tile = indexed[index]
                row.addView(tile.container, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, 0, template.rowWeight(rowIndex)))
        }
        show(layout, data, gps, bikeDistanceM, pulse,trendSnapshot)
    }

    private fun pair(indexes: List<Int>, indexed: List<Tile>) = LinearLayout(context).apply {
        orientation = HORIZONTAL; isBaselineAligned = false
        indexes.forEachIndexed { column, index ->
            if (column > 0) addView(line(), LayoutParams(BicycleDesign.linePixels(context), LayoutParams.MATCH_PARENT))
            addView(indexed[index].container, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        val wide = if (width == viewportWidth && measuredHeight == viewportDataHeight) viewportWide ?: false
            else width > measuredHeight
        if (wide != landscape) { landscape = wide; buildTiles(wide) }
        val available = MeasureSpec.getSize(heightMeasureSpec)
        // Keep the former title/value space; reclaim the unit row and removed header.
        // Short viewports use their full reference area so text remains readable.
        val reference = available - if (available >= dp(450)) dp(88) else 0
        val compact = (reference * .85f + dp(16) * (if (wide) 3 else 4) * .15f).toInt().coerceAtMost(available)
        val height = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST)
            MeasureSpec.makeMeasureSpec(compact, MeasureSpec.EXACTLY) else heightMeasureSpec
        val geometry = "$width:$height:" + template.slots.joinToString("|") { tiles[it].title.text.toString() }
        if (geometry != captionGeometry) {
            captionGeometry = geometry
            for (index in template.slots) {
                tiles[index].title.layoutParams = (tiles[index].title.layoutParams as LayoutParams).apply { this.height = 0; weight = tiles[index].captionWeight }
            }
        }
        super.onMeasure(widthMeasureSpec, height)
        // A fixed caption font must not be shrunk to fit a short row. Reserve its
        // actual text height when needed, leaving the value the remaining space.
        var captionsChanged = false
        for (index in template.slots) {
            val title = tiles[index].title
            if(title.visibility==GONE) continue
            val needed = title.layout?.height ?: continue
            if (needed > title.measuredHeight) {
                title.layoutParams = (title.layoutParams as LayoutParams).apply { this.height = needed; weight = 0f }
                captionsChanged = true
            }
        }
        if (captionsChanged) super.onMeasure(widthMeasureSpec, height)

    }

    fun show(layout: DashboardLayout, data: LiveData, gps: GpsValues = GpsValues(), bikeDistanceM: Long? = null, pulse: PulseValues = PulseValues(),trendSnapshot:RideTrendSnapshot=RideTrendSnapshot()) {
        if(gps.elapsedMs!=null && this.gps.elapsedMs?.let { gps.elapsedMs<it }==true) resetTrends()
        this.layout = layout; this.data = data; this.gps = gps; this.bikeDistanceM = bikeDistanceM
        this.trendSnapshot=trendSnapshot
        this.pulse = pulse
        tiles.forEachIndexed { index, tile -> tile.show(layout.fields[index], data) }
    }

    private fun line() = View(context).apply { setBackgroundColor(Color.rgb(44, 44, 44)); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    private inner class Tile(private val index: Int) {
        fun resetTrend() { row.direction=0;row.chevrons.clear() }
        internal val zoneGlow=ZoneAmbientGlow()
        private var zoneShown=false
        fun updateGlow() {
            if(!zoneShown && zoneConfig.sources[index]!=ZoneGlowSource.OFF) {
                if(plainBackground==null) plainBackground=container.background
                zoneShown=true;updateBackground()
            }
            zoneGlow.update(training.glow(zoneConfig.sources[index],zoneTwoHelp))
        }
        fun refreshAppearance() { if(plainBackground!=null) updateBackground();container.invalidate() }
        private fun updateBackground() {
            val layers=mutableListOf(plainBackground!!)
            if(zoneShown) layers+=zoneGlow
            if(graphShown) layers+=overlay
            container.background=if(layers.size==1) layers.single() else android.graphics.drawable.LayerDrawable(layers.toTypedArray())
        }
        private val overlay=DashboardGraphDrawable(resources.displayMetrics.density)
        private var graphShown=false
        private var plainBackground: android.graphics.drawable.Drawable?=null
        private fun selectedGraphs() = if(template.supportsGraph(index)) graphConfig.slots[index] else emptyList()
        fun updateGraph() {
            val graphDisplay=if(dataVisibility.visible[index]) GraphDisplayMode.WITH_DATA else GraphDisplayMode.GRAPH_ONLY
            val selected=selectedGraphs()
            if(selected.isNotEmpty()!=graphShown) {
                if(plainBackground==null) plainBackground=container.background
                graphShown=selected.isNotEmpty()
                updateBackground()
            }
            if(graphShown) overlay.update(graphFrame,selected,intArrayOf(container.paddingLeft,container.paddingTop,container.paddingRight,container.paddingBottom),graphDisplay,graphConfig.windows[index])
            row.visibility=if(dataVisibility.visible[index]) VISIBLE else INVISIBLE
            title.visibility=row.visibility
            showCaption(layout.fields[index])
        }
        private val style = template.tileStyle(index)
        private val threeColumns = style == DashboardTileStyle.THREE
        private val swipe = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                container.cancelLongPress(); container.isPressed = false; return false
            }
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val first = e1 ?: return false
                val change = changePage ?: return false
                val dx = e2.x - first.x; val dy = e2.y - first.y
                if (kotlin.math.abs(dx) > dp(70) && kotlin.math.abs(dx) > 2 * kotlin.math.abs(dy)) {
                    change(if (dx < 0) 1 else -1); return true
                }
                return false
            }
        })
        val container: LinearLayout = object : LinearLayout(context) {
            override fun onSizeChanged(w: Int,h: Int,oldw: Int,oldh: Int) {
                super.onSizeChanged(w,h,oldw,oldh)
                if(template.supportsGraph(index)) graphWidthChanged?.invoke((w-paddingLeft-paddingRight).coerceAtLeast(64))
            }
        }.apply {
            orientation = VERTICAL
            setPadding(dp(style.horizontalPadding), dp(style.topPadding), dp(style.horizontalPadding), dp(style.bottomPadding))
            isClickable = true; isFocusable = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(Color.rgb(24, 24, 24)))
                addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
            }
            tag = "dashboard_tile_$index"
            setOnClickListener { if (editing) selectField(index) }
            setOnLongClickListener { activateEditor?.invoke();true }
            setOnTouchListener { _, event -> swipe.onTouchEvent(event) }
        }
        fun markEditable() {
            container.foreground = if (editing) DashboardEditOutline(resources.displayMetrics.density, editorColor) else null
        }
        val captionWeight = style.captionWeight
        val title = TextView(context).apply {
            typeface = captions; gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 2
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            textSize = style.captionSp; setTextColor(BicycleDesign.dashboardUnitColor)
        }
        private val value = text(style.valueMinSp, style.valueMaxSp, numbers, 1).apply {
            style.fixedValueSp?.let { fixed ->
                setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE)
                textSize = fixed; ellipsize = android.text.TextUtils.TruncateAt.END
            }
        }
        private val unit = text(style.unitMinSp, style.unitMaxSp, captions, 1).apply {
            style.fixedUnitSp?.let { fixed -> setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE); textSize = fixed }
        }

        private val row = ValueRow(value, unit, style)
        private val batteryGrid=BatteryGridView(context).apply { visibility=GONE;tag="dashboard_batteries_$index" }

        init {
            markEditable()
            container.addView(title, LayoutParams(LayoutParams.MATCH_PARENT, 0,
                captionWeight))
            container.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, 0, style.valueWeight))
            container.addView(batteryGrid,LayoutParams(-1,0,1f))
            updateGraph();updateGlow()
        }

        private fun showCaption(field: DashboardField) {
            val short = field.dashboardTitle(context,data)
            val graphs = selectedGraphs()
            val icon=field.captionIcon(data)
            val captionKey = "${icon ?: ""}:$short:${graphs.joinToString { it.name }}"
            if (title.tag == captionKey) return
            title.tag = captionKey
            if (graphs.isEmpty() && icon == null) { title.text=short; return }
            val prefix=if(icon!=null) "\uFFFC $short" else short
            val left=if(graphs.isNotEmpty()) "\uFFFC  " else ""
            val text=android.text.SpannableString(left+prefix+if(graphs.size>1) "  \uFFFC" else "")
            if(icon!=null) text.setSpan(SourceIconSpan(icon!!,context),left.length,left.length+1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            graphs.forEachIndexed { i,chart ->
                val at=if(i==0) 0 else text.length-1
                text.setSpan(RecordingDotSpan(resources.displayMetrics.density,chart.color(Color.BLACK),LiveGraphStyle.MARKER_DIAMETER_DP),at,at+1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            title.text=text
        }

        fun show(field: DashboardField, data: LiveData) {
            val batteries=field.batterySources
            if(batteries!=null) {
                row.visibility=GONE;title.visibility=GONE
                batteryGrid.visibility=if(dataVisibility.visible[index]) VISIBLE else INVISIBLE
                batteryGrid.show(batteries,data.batteries)
                container.contentDescription=if(dataVisibility.visible[index]) BatteryFields.description(context,batteries,data.batteries) else context.getString(R.string.dashboard_no_data)
                return
            }
            batteryGrid.visibility=GONE
            row.visibility=if(dataVisibility.visible[index]) VISIBLE else INVISIBLE;title.visibility=row.visibility
            val next = field.value(data, gps, bikeDistanceM, pulse, training)
            val caption = if (field.showSource) context.getString(R.string.ui_source_field_description, DashboardTextResources.source(context,field.source(data)), field.title(context,data, gps)) else field.title(context,data, gps)
            showCaption(field)
            val direction = trendSnapshot.indicator(field)
            row.direction = if(trendIndicators.shows(field)) direction else 0
            val compactDistance = if (threeColumns && next.unit == "km") next.text.replace(',', '.').toDoubleOrNull()?.takeIf { kotlin.math.abs(it) >= 100 }
                ?.let { String.format(Locale.GERMANY, "%.1f", it) } else null
            setText(value, compactDistance ?: next.text)
            setText(unit, if (next.unit == "h:mm:ss") context.getString(R.string.analysis_hours) else next.unit)
            unit.visibility = if (next.unit.isEmpty()) GONE else VISIBLE
            val font = if (field.numeric) numbers else captions
            if (value.typeface !== font) value.typeface = font
            val color = if (next.available) Color.WHITE else BicycleDesign.dashboardUnitColor
            if (value.currentTextColor != color) value.setTextColor(color)
            unit.setTextColor(BicycleDesign.dashboardUnitColor)
            container.contentDescription = if(!dataVisibility.visible[index]) context.getString(R.string.dashboard_no_data) else context.getString(R.string.ui_field_description, caption, if (next.available) "${next.text} ${next.unit}" else context.getString(R.string.ui_field_missing_value))
        }
    }

    /** Keep the number centred in its tile, with the unit following its actual text edge. */
    private inner class ValueRow(private val value: TextView, private val unit: TextView, private val style: DashboardTileStyle) : ViewGroup(context) {
        private val threeColumns = style == DashboardTileStyle.THREE
        val chevrons=TrendChevrons(this) { if(threeColumns) requestLayout() }
        var direction = 0
            set(next) { if (field != next) {
                field = next;chevrons.update(next);if (threeColumns) requestLayout(); invalidate()
            } }
        private val chevronSpace get() = kotlin.math.ceil(minOf(dp(14).toFloat(), value.textSize * .28f) * TrendChevrons.SCALE + dp(style.valueUnitGap)).toInt()
        init { addView(value); addView(unit) }
        override fun dispatchDraw(canvas: android.graphics.Canvas) {
            super.dispatchDraw(canvas)
            if (chevrons.visible) {
                val size = minOf(dp(14).toFloat(), value.textSize * .28f,height/2.4f) * TrendChevrons.SCALE
                val textWidth = value.layout?.getLineWidth(0) ?: 0f
                val x = if (threeColumns) value.left - dp(style.valueUnitGap) - size else width / 2f - textWidth / 2f - dp(style.valueUnitGap) - size
                chevrons.draw(canvas,x.coerceAtLeast(0f),height/2f,size,value.currentTextColor)
            }
        }
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            if (threeColumns) {
                if (unit.visibility != GONE) unit.measure(MeasureSpec.makeMeasureSpec(width / style.unitWidthDivisor, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
                val reserve = (if (chevrons.visible) chevronSpace else 0) + if (unit.visibility == GONE) 0 else unit.measuredWidth + dp(style.valueUnitGap)
                value.measure(MeasureSpec.makeMeasureSpec((width - reserve).coerceAtLeast(1), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
                setMeasuredDimension(width, height)
                return
            }
            if (unit.visibility != GONE) unit.measure(
                MeasureSpec.makeMeasureSpec(width / style.unitWidthDivisor, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
            val reserve = if (unit.visibility == GONE) 0 else unit.measuredWidth + dp(style.valueUnitGap)
            value.measure(MeasureSpec.makeMeasureSpec((width - 2 * reserve).coerceAtLeast(1), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
            setMeasuredDimension(width, height)
        }
        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            if (threeColumns) {
                val reserve = if (chevrons.visible) chevronSpace else 0
                val unitWidth = if (unit.visibility == GONE) 0 else unit.measuredWidth + dp(style.valueUnitGap)
                val start = (width - reserve - value.measuredWidth - unitWidth) / 2
                value.layout(start + reserve, 0, start + reserve + value.measuredWidth, value.measuredHeight)
                if (unit.visibility != GONE) {
                    val y = (height - unit.measuredHeight).coerceAtLeast(0) / 2
                    unit.layout(value.right + dp(style.valueUnitGap), y, value.right + dp(style.valueUnitGap) + unit.measuredWidth, y + unit.measuredHeight)
                }
                return
            }
            val start = (width - value.measuredWidth) / 2
            value.layout(start, 0, start + value.measuredWidth, value.measuredHeight)
            if (unit.visibility != GONE) {
                // TextView finalises automatic font sizing during layout, before vertical centring.
                unit.layout(0, 0, unit.measuredWidth, unit.measuredHeight)
                val textWidth = value.layout?.getLineWidth(0) ?: 0f
                val x = (width / 2f + textWidth / 2f + dp(style.valueUnitGap)).toInt()
                val y = (height - unit.measuredHeight).coerceAtLeast(0) / 2
                unit.offsetLeftAndRight(x); unit.offsetTopAndBottom(y)
            }
        }
    }

    private fun text(min: Int, max: Int, font: Typeface, lines: Int) = TextView(context).apply {
        typeface = font; gravity = Gravity.CENTER
        setTextColor(Color.WHITE); includeFontPadding = false
        maxLines = lines
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setAutoSizeTextTypeUniformWithConfiguration(min, max, 1, TypedValue.COMPLEX_UNIT_SP)
    }
    private fun setText(view: TextView, next: String) { if (view.text.toString() != next) view.text = next }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

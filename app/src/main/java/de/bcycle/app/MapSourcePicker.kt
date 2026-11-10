package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** One native spinner with disabled category rows; persisted source IDs remain unchanged. */
internal class MapSourcePicker(context:Context,private val themes:ThemeManager,private val prefs:android.content.SharedPreferences?=null):Spinner(context) {
    companion object {
        fun position(source:MapSource)=if(source==MapSource.VECTOR) 7 else source.ordinal+1
    }
    private val saved=prefs?.let(MapStyleLibrary::read).orEmpty()
    private val styles=saved
    private val choices:List<MapSource?> = listOf(null,MapSource.CYCLOSM,MapSource.OSM,MapSource.OPENTOPOMAP,MapSource.OPENCYCLEMAP,null,null)+List(styles.size) { MapSource.VECTOR }
    private var fallback=MapSource.CYCLOSM
    val selectedSource get()=choices.getOrNull(selectedItemPosition) ?: fallback
    val selectedStyleId get()=styles.getOrNull(selectedItemPosition-7)?.id
    fun select(source:MapSource) {
        fallback=source
        val saved=if(source==MapSource.VECTOR) styles.indexOfFirst { it?.id==prefs?.getString(MapStyleLibrary.ACTIVE,null) } else -1
        val original=styles.indexOfFirst { it.name.equals("Bright",true) }
        setSelection(if(source!=MapSource.VECTOR) position(source) else if(saved>=0) 7+saved else if(original>=0) 7+original else -1)
    }
    init {
        contentDescription=context.getString(R.string.ui_map_source)
        adapter=object:BaseAdapter(),SpinnerAdapter {
            override fun getCount()=choices.size
            override fun getItem(position:Int):String=when(position) {
                0->context.getString(R.string.map_source_raster_heading)
                6->context.getString(R.string.map_source_vector_heading)
                5->""
                else->if(position>=7) styles[position-7].name else choices[position]!!.label(context)
            }
            override fun getItemId(position:Int)=if(position>=8) position.toLong() else choices[position]?.ordinal?.toLong() ?: -(position+1L)
            override fun areAllItemsEnabled()=false
            override fun isEnabled(position:Int)=choices[position]!=null
            private fun row(position:Int,recycled:View?,dropdown:Boolean):View {
                if(dropdown && position==5) return FrameLayout(context).apply {
                    importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
                    setPadding(gap,gap,gap,gap)
                    addView(BicycleComponents.divider(context,themes),FrameLayout.LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.controlBorder)))
                }
                return ((recycled as? TextView) ?: BicycleComponents.text(context,themes,"")).apply {
                    val heading=dropdown && position in listOf(0,6)
                    text=if(dropdown || isEnabled(position)) getItem(position) else fallback.label(context)
                    typeface=if(heading) BicycleDesign.strongTypeface else BicycleDesign.bodyTypeface
                    textSize=if(heading) BicycleDesign.contentHeadingSp else BicycleDesign.profileDropdownSp
                    isAccessibilityHeading=heading;isEnabled=if(dropdown) isEnabled(position) else true
                    alpha=1f;gravity=Gravity.START or Gravity.CENTER_VERTICAL
                    minHeight=BicycleDesign.dp(context,if(heading) 32 else BicycleDesign.touchSize)
                    setPadding(BicycleDesign.dp(context,BicycleDesign.horizontalPadding),BicycleDesign.dp(context,BicycleDesign.verticalPadding),
                        BicycleDesign.dp(context,if(dropdown) BicycleDesign.horizontalPadding else BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.verticalPadding))
                    maxLines=if(dropdown) Int.MAX_VALUE else 1
                    ellipsize=if(dropdown) null else android.text.TextUtils.TruncateAt.END
                    BicycleComponents.bindTheme(this,themes) { setTextColor(it.foreground);setBackgroundColor(android.graphics.Color.TRANSPARENT) }
                }
            }
            override fun getView(position:Int,convertView:View?,parent:ViewGroup)=row(position,convertView,false)
            override fun getDropDownView(position:Int,convertView:View?,parent:ViewGroup)=row(position,convertView,true)
        }
        select(fallback)
    }
}

package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout

internal data class SummaryTransfer(val target:RouteTransferTarget,val label:String,val state:UploadState)

/** Compact centered rows; native children retain accessible target/status descriptions. */
internal class RideSummaryTransfers(context:Context,private val themes:ThemeManager):ViewGroup(context) {
    private var shown=emptyList<SummaryTransfer>()
    private var columns=1
    private var rowHeight=0
    private val gap=BicycleDesign.dp(context,BicycleDesign.rideSummaryStatusGap)
    private val blockWidth=BicycleDesign.dp(context,BicycleDesign.rideSummaryStatusWidth)
    init { tag="ride_summary_transfers" }
    fun show(items:List<SummaryTransfer>) {
        if(items==shown) return
        shown=items;removeAllViews()
        items.forEach { item ->
            val stateText=context.getString(when(item.state) {
                UploadState.UPLOADED->R.string.route_transfer_done
                UploadState.WAITING->R.string.route_transfer_waiting
                UploadState.UPLOADING->R.string.route_transfer_uploading
                else->R.string.route_transfer_failed
            })
            addView(LinearLayout(context).apply {
                orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;tag="ride_summary_transfer_${item.target.name}"
                importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
                contentDescription=context.getString(R.string.ride_summary_target_status,item.label,stateText)
                stateDescription=stateText;tooltipText=contentDescription
                addView(LinearLayout(context).apply {
                    gravity=Gravity.CENTER
                    addView(ImageView(context).apply {
                        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                        BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,item.target.icon,it.foreground)) }
                    },LinearLayout.LayoutParams(BicycleDesign.dp(context,BicycleDesign.iconSize),BicycleDesign.dp(context,BicycleDesign.iconSize)))
                    addView(ImageView(context).apply {
                        tag="ride_summary_state_${item.target.name}";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                        setTag(R.id.summary_transfer_state,item.state)
                        val icon=when(item.state) { UploadState.UPLOADED->BicycleIcon.Check
                            UploadState.WAITING,UploadState.UPLOADING->BicycleIcon.Loading;else->BicycleIcon.Cancel }
                        BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,icon,it.foreground)) }
                    },LinearLayout.LayoutParams(BicycleDesign.dp(context,16),BicycleDesign.dp(context,16)).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap) })
                })
                addView(BicycleComponents.text(context,themes,item.label).apply {
                    gravity=Gravity.CENTER;textSize=BicycleDesign.analysisLabelSp;maxLines=2
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LinearLayout.LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.smallGap) })
            })
        }
        requestLayout()
    }
    override fun onMeasure(w:Int,h:Int) {
        val available=if(MeasureSpec.getMode(w)==MeasureSpec.UNSPECIFIED) childCount*blockWidth+(childCount-1).coerceAtLeast(0)*gap else MeasureSpec.getSize(w)
        columns=((available+gap)/(blockWidth+gap)).coerceAtLeast(1)
        rowHeight=0
        for(i in 0 until childCount) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(minOf(available,blockWidth),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED))
            rowHeight=maxOf(rowHeight,getChildAt(i).measuredHeight)
        }
        val rows=(childCount+columns-1)/columns
        setMeasuredDimension(resolveSize(available,w),resolveSize(rows*rowHeight+(rows-1).coerceAtLeast(0)*gap,h))
    }
    override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int) {
        for(i in 0 until childCount step columns) {
            val count=minOf(columns,childCount-i)
            val width=getChildAt(i).measuredWidth
            val start=(measuredWidth-count*width-(count-1)*gap)/2
            for(j in 0 until count) {
                val child=getChildAt(i+j);val left=start+j*(width+gap);val top=(i/columns)*(rowHeight+gap)
                child.layout(left,top,left+child.measuredWidth,top+child.measuredHeight)
            }
        }
    }
}

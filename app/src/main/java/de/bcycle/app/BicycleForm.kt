package de.bcycle.app

import android.graphics.Color
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Shared settings sections and outlined inputs; persistence remains with each owning screen. */
internal object BicycleForm {
    fun section(parent: LinearLayout, themes: ThemeManager, title: Int, inlineRules:Boolean=false): BicycleSectionHeader {
        val context=parent.context
        parent.clipToPadding=false
        return BicycleSectionHeader(context,themes,context.getString(title),inlineRules).also {
            parent.addView(it,LinearLayout.LayoutParams(-1,-2).apply {
                marginStart=-parent.paddingLeft; marginEnd=-parent.paddingRight
                topMargin=0
                bottomMargin=0
            })
        }
    }
    /** Upgrade unbound sticky sections once; preserve existing accordion state and rename headers. */
    fun accordions(root:android.view.View) {
        if(root !is android.view.ViewGroup) return
        if(root is LinearLayout && root.orientation==LinearLayout.VERTICAL) {
            var i=0
            while(i<root.childCount) {
                val header=root.getChildAt(i) as? BicycleSectionHeader
                if(header!=null && header.expanded==null && header.tag!="bike_name_header") {
                    val body=LinearLayout(root.context).apply { orientation=LinearLayout.VERTICAL;tag="sticky_accordion_content" }
                    while(i+1<root.childCount && root.getChildAt(i+1) !is BicycleSectionHeader) {
                        val child=root.getChildAt(i+1);root.removeViewAt(i+1);body.addView(child,child.layoutParams)
                    }
                    root.addView(body,i+1,LinearLayout.LayoutParams(-1,-2));header.accordion(body)
                }
                i++
            }
        }
        (0 until root.childCount).map(root::getChildAt).forEach(::accordions)
    }
    /** Resolve slider boundaries across optional wrappers, without adding an existing trailing gap twice. */
    fun sliderQuestionSpacing(root:android.view.ViewGroup) {
        fun trailing(view:android.view.View):Int? {
            if(view.visibility==android.view.View.GONE) return null
            val own=(view.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
            if(view is LinearLayout && view.getTag(R.id.inline_control_row)!=true) {
                val last=(view.childCount-1 downTo 0).firstNotNullOfOrNull { trailing(view.getChildAt(it)) } ?: return null
                return own+view.paddingBottom+last
            }
            return own
        }
        fun preceding(view:android.view.View):Int? {
            var child=view
            while(child!==root) {
                val parent=child.parent as? LinearLayout ?: return null
                for(index in parent.indexOfChild(child)-1 downTo 0) {
                    val previous=parent.getChildAt(index)
                    if(previous.visibility==android.view.View.GONE) continue
                    if(previous is BicycleSectionHeader) return null
                    trailing(previous)?.let { return it }
                }
                child=parent
            }
            return null
        }
        fun visit(view:android.view.View) {
            if(view.getTag(R.id.slider_question_group)==true) {
                val margins=view.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                val top=preceding(view)?.let { (BicycleDesign.dp(view.context,BicycleDesign.sliderQuestionGap)-it).coerceAtLeast(0) } ?: 0
                if(margins!=null && margins.topMargin!=top) { margins.topMargin=top;view.requestLayout() }
            }
            if(view is android.view.ViewGroup) (0 until view.childCount).forEach { visit(view.getChildAt(it)) }
        }
        visit(root)
    }
    fun outlinedInput(edit: TextView, themes: ThemeManager, numeric: Boolean=false) {
        BicycleDialogStyle.control(edit,themes)
        if(numeric) edit.apply {
            if(this is EditText) { maxLines=1;setHorizontallyScrolling(false) }
            typeface=BicycleDesign.narrowTypeface(context)
            textSize=BicycleDesign.settingsMetricSp
            gravity=Gravity.END or Gravity.CENTER_VERTICAL
            includeFontPadding=false
            setPadding(BicycleDesign.dp(context,BicycleDesign.inputHorizontalPadding),BicycleDesign.dp(context,BicycleDesign.smallGap),
                BicycleDesign.dp(context,BicycleDesign.inputHorizontalPadding),BicycleDesign.dp(context,BicycleDesign.smallGap))
        }
        BicycleComponents.bindTheme(edit,themes) { tokens ->
            edit.backgroundTintList=null
            edit.background=BicycleInputFrame.background(edit,tokens)
        }
    }
    /** Shared label-left/input-right geometry; units belong inside the input. */
    fun inputRow(edit:EditText,themes:ThemeManager,title:String):LinearLayout {
        val context=edit.context
        if(edit.id==android.view.View.NO_ID) edit.id=android.view.View.generateViewId()
        outlinedInput(edit,themes,numeric=true)
        return LinearLayout(context).apply {
            gravity=Gravity.CENTER_VERTICAL
            setTag(R.id.inline_control_row,true)
            addView(BicycleComponents.text(context,themes,title).apply { labelFor=edit.id },
                LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            addView(edit,LinearLayout.LayoutParams(BicycleDesign.dp(context,BicycleDesign.formNumberWidth),-2))
        }
    }
    fun numberRow(edit:EditText,themes:ThemeManager,title:Int,unit:Int):LinearLayout =
        inputRow(edit,themes,edit.context.getString(title)).also {
            BicycleInputUnit.bind(edit,themes,edit.context.getString(unit))
        }
}

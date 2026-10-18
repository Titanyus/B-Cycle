package de.bcycle.app

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Existing AlertDialog lifecycle, buttons, adapters and scroll containers are retained. */
internal class BicycleDialogBuilder(context: Context, private val themes: ThemeManager,
    private val navigation: DialogNavigation, private val fullscreen: Boolean = true) : AlertDialog.Builder(context,
        if (fullscreen) R.style.SettingsFullscreenTheme else R.style.SettingsDialogTheme) {
    override fun setTitle(title: CharSequence?): AlertDialog.Builder {
        super.setTitle(title)
        val header = BicycleComponents.header(context, themes).apply {
            tag = "bicycle_dialog_header"
            setPadding(BicycleDesign.dp(context, BicycleDesign.headerHorizontalPadding), 0,
                BicycleDesign.dp(context, BicycleDesign.headerHorizontalPadding), 0)
            minimumHeight = BicycleDesign.dp(context, BicycleDesign.headerHeight)
            addView(BicycleComponents.heading(context, themes, title?.toString().orEmpty()).apply {
                tag = "bicycle_dialog_title"
                setAutoSizeTextTypeUniformWithConfiguration(18, BicycleDesign.headingSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            }, LinearLayout.LayoutParams(-1, BicycleDesign.dp(context, BicycleDesign.headerHeight)))
        }
        setCustomTitle(header)
        return this
    }
    override fun setTitle(titleId: Int): AlertDialog.Builder = setTitle(context.getText(titleId))
    override fun create(): AlertDialog = super.create().also { screen ->
        navigation.track(screen, overlay = !fullscreen) {
            screen.window?.let { DashboardFullscreen.apply(it,false) }
            BicycleNavigation.hideNavigationFooter(screen)
            BicycleDialogStyle.apply(screen, themes)
            if (fullscreen) BicycleFullscreen.apply(screen, themes)
            else {
                BicycleDialogStyle.messageSpacing(screen)
                val top=screen.window?.decorView?.findViewById<ViewGroup>(context.resources.getIdentifier("topPanel","id","android"))
                if(top!=null && top.childCount>0) {
                    val original=top.getChildAt(0); top.removeAllViews()
                    top.addView(BicycleNavigation.header(original,screen,themes),ViewGroup.LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.headerHeight)))
                }
            }
        }
    }
}

/** Adapt native controls through the existing theme bindings; never replace their event handlers. */
internal object BicycleDialogStyle {
    private val styled = java.util.WeakHashMap<View, Boolean>()
    private fun dp(view: View, n: Int) = BicycleDesign.dp(view.context, n)
    private fun colors(foreground: Int) = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
        intArrayOf(alpha(foreground, .5f), foreground))
    private fun alpha(color: Int, amount: Float) = (color and 0x00ffffff) or ((Color.alpha(color) * amount).toInt() shl 24)
    private fun checkedColors(active: Int, inactive: Int) = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(alpha(active, .5f), alpha(inactive, .5f), active, inactive))
    private fun selectionCircle(view: CompoundButton, tokens: ThemeTokens, inset: Int) = StateListDrawable().apply {
        fun circle(checked: Boolean, enabled: Boolean): android.graphics.drawable.Drawable {
            val opacity = if (enabled) 1f else .5f
            val ring = view.context.getDrawable(R.drawable.selection_circle_empty)!!.mutate().apply { setTint(alpha(tokens.foreground, opacity)) }
            val drawable = if (!checked) ring else LayerDrawable(arrayOf(ring, GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(alpha(tokens.accent, opacity))
            })).apply {
                setLayerSize(1, dp(view, 10), dp(view, 10)); setLayerGravity(1, Gravity.CENTER)
            }
            val nativeCircle=if(inset>0) android.graphics.drawable.InsetDrawable(drawable,inset) else drawable
            return android.graphics.drawable.InsetDrawable(nativeCircle,0,0,dp(view,BicycleDesign.controlGap),0)
        }
        addState(intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_checked), circle(true, false))
        addState(intArrayOf(-android.R.attr.state_enabled), circle(false, false))
        addState(intArrayOf(android.R.attr.state_checked), circle(true, true))
        addState(intArrayOf(), circle(false, true))
    }
    private fun shape(view: View, color: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(view, BicycleDesign.inputRadius).toFloat()
        stroke?.let { setStroke(BicycleDesign.linePixels(view.context), it) }
    }
    private fun inputBackground(view: View, tokens: ThemeTokens) =
        BicycleComponents.controlBackground(view.context,tokens,tokens.tertiary)
    fun apply(screen: AlertDialog, themes: ThemeManager) {
        val decor = screen.window?.decorView ?: return
        // Phase 2 already styles this dialog, including its native footer; leave it unchanged.
        if (descendants(decor).any { it is RouteManagerView }) return
        if (styled.put(decor, true) != null) return
        BicycleComponents.bindTheme(decor, themes) { tokens ->
            screen.window?.setBackgroundDrawable(GradientDrawable().apply {
                setColor(tokens.secondary); cornerRadius = dp(decor, BicycleDesign.dialogRadius).toFloat()
            })
            decor.clipToOutline = true
        }
        content(decor, themes)
        listOf(AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL, AlertDialog.BUTTON_POSITIVE).forEach { which ->
            screen.getButton(which)?.let { button ->
                footer(button, themes)
                when (button.text.toString()) {
                    "Speichern" -> actionIcon(button, themes, BicycleIcon.Save)
                    "Abbrechen" -> actionIcon(button, themes, BicycleIcon.Cancel)
                    "Schließen" -> actionIcon(button, themes, BicycleIcon.Close)
                    "Löschen" -> actionIcon(button, themes, BicycleIcon.DeleteTrack)
                }
            }
        }
    }
    /** Compact confirmations keep native message layout, with the shared minimum header gap. */
    fun messageSpacing(screen: AlertDialog) {
        screen.findViewById<TextView>(android.R.id.message)?.let { message ->
            message.setPadding(message.paddingLeft, maxOf(message.paddingTop, dp(message, BicycleDesign.controlGap)),
                message.paddingRight, message.paddingBottom)
        }
    }
    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(descendants(view.getChildAt(i)))
    }
    fun actionIcon(view: TextView, themes: ThemeManager, icon: BicycleIcon, endIcon: BicycleIcon? = null) {
        view.compoundDrawablePadding = dp(view, BicycleDesign.verticalPadding)
        BicycleComponents.bindTheme(view, themes) { tokens ->
            view.setCompoundDrawablesRelative(BicycleTextIcon(view,BicycleIcons.drawable(view.context, icon, tokens.foreground)).apply {
                setBounds(0, 0, dp(view, BicycleDesign.iconSize), dp(view, BicycleDesign.iconSize))
            }, null, endIcon?.let { BicycleIcons.drawable(view.context,it,tokens.foreground).apply {
                setBounds(0,0,dp(view,BicycleDesign.iconSize),dp(view,BicycleDesign.iconSize))
            } }, null)
        }
    }
    fun content(view: View, themes: ThemeManager) {
        if (view is DatePicker) { BicycleNavigation.dateHeader(view,themes); return }
        if (view is BicycleFooterAction || view.tag == "bicycle_action_footer") return
        // This reference screen owns every detail, including its compact action geometry.
        if (view is SettingsMenuView || view is BikesView || view is RiderProfileView || view is RouteManagerView || view is ThemePickerView || view is RouteAnalysisView || view is RouteAnalysisMap || view.tag == "route_analysis_header" || view.tag == "bicycle_dialog_header") return
        if (view is Spinner) { spinner(view, themes); return }
        if (view is ListView) {
            BicycleComponents.bindTheme(view, themes) { tokens ->
                view.setBackgroundColor(tokens.secondary)
                view.selector = shape(view, tokens.secondary, tokens.foreground)
                view.divider = ColorDrawable(alpha(tokens.foreground, BicycleDesign.dividerAlpha))
                view.dividerHeight = BicycleDesign.linePixels(view.context)
                view.verticalScrollbarThumbDrawable = ColorDrawable(tokens.foreground)
            }
            BicycleComponents.scrollStyle(view)
            val source = view.adapter
            if (source != null && source !is StyledListAdapter) view.adapter = StyledListAdapter(source, themes)
            return
        }
        if (view is TextView && view.id !in setOf(android.R.id.button1, android.R.id.button2, android.R.id.button3)) control(view, themes)
        if (view is ScrollView) {
            BicycleComponents.scrollStyle(view)
            BicycleComponents.bindTheme(view, themes) {
                view.setBackgroundColor(it.secondary); view.verticalScrollbarThumbDrawable = ColorDrawable(it.foreground)
            }
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) content(view.getChildAt(i), themes)
        if (view is DatePicker) BicycleNavigation.dateHeader(view,themes)
    }
    fun control(view: TextView, themes: ThemeManager) {
        if(view is Button) BicycleComponents.controlSpacing(view)
        if (styled.put(view, true) != null) {
            if(view.isAccessibilityHeading) { view.typeface=BicycleDesign.strongTypeface;view.textSize=BicycleDesign.titleSp }
            return
        }
        view.typeface = if(view.isAccessibilityHeading) BicycleDesign.strongTypeface else Typeface.DEFAULT
        if (view !is EditText) view.textSize = if (view.isAccessibilityHeading) BicycleDesign.titleSp else BicycleDesign.bodySp
        if (view is EditText) {
            view.textSize = BicycleDesign.inputSp; view.minimumHeight = dp(view, BicycleDesign.touchSize)
            view.setPadding(dp(view, BicycleDesign.inputHorizontalPadding), dp(view, BicycleDesign.verticalPadding),
                dp(view, BicycleDesign.inputHorizontalPadding), dp(view, BicycleDesign.verticalPadding))
            BicycleComponents.bindTheme(view, themes) { tokens ->
                view.setTextColor(colors(tokens.foreground)); view.setHintTextColor(alpha(tokens.foreground, .75f))
                view.backgroundTintList = null; view.background = inputBackground(view, tokens)
                view.textCursorDrawable?.mutate()?.setTint(tokens.foreground)
                view.textSelectHandle?.mutate()?.setTint(tokens.foreground)
                view.textSelectHandleLeft?.mutate()?.setTint(tokens.foreground)
                view.textSelectHandleRight?.mutate()?.setTint(tokens.foreground)
            }
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = dp(view, BicycleDesign.controlGap)
        } else if (view is CompoundButton) {
            view.minimumHeight = dp(view, BicycleDesign.touchSize)
            if(view is Switch) {
                view.showText=false;view.splitTrack=false
                view.switchMinWidth=dp(view,BicycleDesign.switchWidth)
                view.switchPadding=dp(view,BicycleDesign.controlGap)
                view.thumbDrawable=LayerDrawable(arrayOf(
                    GradientDrawable().apply { setColor(Color.TRANSPARENT);setSize(dp(view,BicycleDesign.switchHeight),dp(view,BicycleDesign.switchHeight)) },
                    GradientDrawable().apply { shape=GradientDrawable.OVAL;setColor(Color.WHITE) }
                )).apply {
                    setLayerSize(1,dp(view,BicycleDesign.switchThumbDiameter),dp(view,BicycleDesign.switchThumbDiameter))
                    setLayerGravity(1,Gravity.CENTER)
                }
            }
            val circular = view is CheckBox || view is RadioButton
            // Keep the native radio-button allocation, with the shared 24-dp circle centered inside.
            val circleInset = if (view is RadioButton) ((view.buttonDrawable?.intrinsicWidth ?: dp(view, 24)) - dp(view, 24)).coerceAtLeast(0) / 2 else 0
            BicycleComponents.bindTheme(view, themes) {
                view.setTextColor(colors(it.foreground))
                if (circular) {
                    view.buttonTintList = null
                    view.buttonDrawable = selectionCircle(view, it, circleInset)
                } else view.buttonTintList = checkedColors(it.accent, it.foreground)
                if (view is Switch) {
                    view.thumbTintList = checkedColors(it.accent, it.foreground)
                    view.trackTintList = null
                    view.trackDrawable=GradientDrawable().apply {
                        setColor(Color.TRANSPARENT)
                        cornerRadius=dp(view,BicycleDesign.switchHeight)/2f
                        setSize(dp(view,BicycleDesign.switchWidth),dp(view,BicycleDesign.switchHeight))
                        setStroke(BicycleDesign.linePixels(view.context),colors(it.foreground))
                        state=view.drawableState
                    }
                }
                view.background = RippleDrawable(ColorStateList.valueOf(alpha(it.foreground, .15f)), null, ColorDrawable(Color.WHITE))
            }
        } else if (view is Button) {
            view.minimumHeight = dp(view, BicycleDesign.touchSize); view.isAllCaps = false
            view.setPadding(dp(view, BicycleDesign.horizontalPadding), dp(view, BicycleDesign.verticalPadding),
                dp(view, BicycleDesign.horizontalPadding), dp(view, BicycleDesign.verticalPadding))
            BicycleComponents.bindTheme(view, themes) {
                view.setTextColor(colors(it.foreground)); view.backgroundTintList = null
                view.background = RippleDrawable(ColorStateList.valueOf(alpha(it.foreground, .15f)), BicycleComponents.controlBackground(view.context,it), null)
                view.compoundDrawablesRelative.filterNotNull().forEach { drawable -> drawable.setTint(it.foreground) }
            }
        } else BicycleComponents.bindTheme(view, themes) { view.setTextColor(BicycleComponents.textColor(view,themes,it)) }
    }
    private fun footer(button: Button, themes: ThemeManager) {
        button.typeface = Typeface.DEFAULT; button.textSize = BicycleDesign.bodySp
        button.isAllCaps = true
        button.minimumHeight = dp(button, BicycleDesign.touchSize)
        button.setPadding(dp(button, BicycleDesign.horizontalPadding), dp(button, BicycleDesign.verticalPadding),
            dp(button, BicycleDesign.horizontalPadding), dp(button, BicycleDesign.verticalPadding))
        BicycleComponents.bindTheme(button, themes) { tokens ->
            button.setTextColor(colors(tokens.foreground)); button.backgroundTintList = null; button.background = null
            (button.parent as? View)?.setBackgroundColor(tokens.secondary)
            button.compoundDrawablesRelative.filterNotNull().forEach { it.setTint(tokens.foreground) }
        }
    }
    /** Accordion labels match dropdowns and text buttons via choiceSp; text at start. */
    fun accordion(view: Button, themes: ThemeManager) {
        view.isAccessibilityHeading=false
        control(view,themes)
        view.typeface=Typeface.DEFAULT
        view.textSize=BicycleDesign.choiceSp
        view.gravity=Gravity.START or Gravity.CENTER_VERTICAL
    }
    fun spinner(view: Spinner, themes: ThemeManager, textSizeSp:Float=BicycleDesign.choiceSp, iconForLabel: ((String) -> BicycleIcon)? = null, renderLabel: ((String, Int) -> CharSequence)? = null) {
        BicycleComponents.controlSpacing(view)
        if (styled.put(view, true) != null) return
        view.minimumHeight = dp(view, BicycleDesign.touchSize)
        BicycleComponents.bindTheme(view, themes) { tokens ->
            view.backgroundTintList = null
            view.background = LayerDrawable(arrayOf(BicycleComponents.controlBackground(view.context,tokens),
                BicycleIcons.drawable(view.context, BicycleIcon.Expand, tokens.foreground))).apply {
                setLayerSize(1, dp(view, BicycleDesign.iconSize), dp(view, BicycleDesign.iconSize))
                setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
                setLayerInsetEnd(1, dp(view, BicycleDesign.horizontalPadding))
            }
            view.setPopupBackgroundDrawable(BicycleComponents.controlBackground(view.context,tokens))
        }
        val selected = view.selectedItemPosition
        val original = view.adapter ?: return
        view.adapter = object : BaseAdapter(), SpinnerAdapter {
            override fun getCount() = original.count
            override fun getItem(position: Int) = original.getItem(position)
            override fun getItemId(position: Int) = original.getItemId(position)
            override fun areAllItemsEnabled() = (original as? ListAdapter)?.areAllItemsEnabled() ?: true
            override fun isEnabled(position:Int) = (original as? ListAdapter)?.isEnabled(position) ?: true
            private fun row(position: Int, recycled: View?, dropdown: Boolean): View =
                ((recycled as? TextView) ?: BicycleComponents.text(view.context, themes, "").apply {
                    typeface = Typeface.DEFAULT; gravity = Gravity.CENTER_VERTICAL
                    minHeight = dp(view, BicycleDesign.touchSize); textSize = textSizeSp
                    BicycleComponents.bindTheme(this, themes) {
                        setTextColor(it.foreground); setBackgroundColor(if (dropdown) it.secondary else Color.TRANSPARENT)
                        if (renderLabel != null && tag is String) text=renderLabel(tag as String,it.foreground)
                        compoundDrawablesRelative.filterNotNull().forEach { icon -> icon.setTint(it.foreground) }
                    }
                }).apply {
                    isEnabled=isEnabled(position);alpha=if(isEnabled) 1f else .4f
                    val label=getItem(position).toString()
                    if (renderLabel != null) tag=label
                    text = renderLabel?.invoke(label,themes.active.tokens.foreground) ?: label
                    compoundDrawablePadding=dp(view,BicycleDesign.controlGap)
                    val icon=iconForLabel?.invoke(label)?.let { BicycleIcons.drawable(view.context,it,themes.active.tokens.foreground).apply {
                        setBounds(0,0,dp(view,BicycleDesign.iconSize),dp(view,BicycleDesign.iconSize))
                    } }
                    setCompoundDrawablesRelative(icon,null,null,null)
                    if (renderLabel != null) contentDescription=text.toString().removePrefix("\uFFFC ")
                    setPadding(dp(view, BicycleDesign.horizontalPadding), dp(view, BicycleDesign.verticalPadding),
                        dp(view, if (dropdown) BicycleDesign.horizontalPadding else BicycleDesign.touchSize), dp(view, BicycleDesign.verticalPadding))
                }
            override fun getView(position: Int, convertView: View?, parent: ViewGroup) = row(position, convertView, false)
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) = row(position, convertView, true)
        }
        if (selected >= 0) view.setSelection(selected)
    }
    private class StyledListAdapter(private val original: ListAdapter, private val themes: ThemeManager) : BaseAdapter() {
        override fun getCount() = original.count
        override fun getItem(position: Int) = original.getItem(position)
        override fun getItemId(position: Int) = original.getItemId(position)
        override fun areAllItemsEnabled() = original.areAllItemsEnabled()
        override fun isEnabled(position: Int) = original.isEnabled(position)
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            original.getView(position, convertView, parent).also { content(it, themes) }
    }
}

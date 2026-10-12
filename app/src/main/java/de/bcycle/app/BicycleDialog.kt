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
            BicycleDialogStyle.apply(screen, themes)
            if (fullscreen) BicycleFullscreen.apply(screen, themes)
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
    private fun shape(view: View, color: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(view, BicycleDesign.inputRadius).toFloat()
        stroke?.let { setStroke(BicycleDesign.dividerPixels, it) }
    }
    private fun inputBackground(view: View, tokens: ThemeTokens) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), shape(view, tokens.tertiary, tokens.foreground))
        addState(intArrayOf(android.R.attr.state_selected), shape(view, tokens.tertiary, tokens.foreground))
        addState(intArrayOf(), shape(view, tokens.tertiary))
    }
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
    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(descendants(view.getChildAt(i)))
    }
    fun actionIcon(view: TextView, themes: ThemeManager, icon: BicycleIcon) {
        view.compoundDrawablePadding = dp(view, BicycleDesign.verticalPadding)
        BicycleComponents.bindTheme(view, themes) { tokens ->
            view.setCompoundDrawablesRelative(BicycleIcons.drawable(view.context, icon, tokens.foreground).apply {
                setBounds(0, 0, dp(view, BicycleDesign.iconSize), dp(view, BicycleDesign.iconSize))
            }, null, null, null)
        }
    }
    fun content(view: View, themes: ThemeManager) {
        // This reference screen owns every detail, including its compact action geometry.
        if (view is RouteManagerView || view is ThemePickerView || view is RouteAnalysisView || view is RouteAnalysisMap || view.tag == "route_analysis_header" || view.tag == "bicycle_dialog_header") return
        if (view is Spinner) { spinner(view, themes); return }
        if (view is ListView) {
            BicycleComponents.bindTheme(view, themes) { tokens ->
                view.setBackgroundColor(tokens.secondary)
                view.selector = shape(view, tokens.secondary, tokens.foreground)
                view.divider = ColorDrawable(alpha(tokens.foreground, BicycleDesign.dividerAlpha))
                view.dividerHeight = BicycleDesign.dividerPixels
                view.verticalScrollbarThumbDrawable = ColorDrawable(tokens.foreground)
            }
            view.scrollBarSize = dp(view, BicycleDesign.scrollbarSize)
            val source = view.adapter
            if (source != null && source !is StyledListAdapter) view.adapter = StyledListAdapter(source, themes)
            return
        }
        if (view is TextView && view.id !in setOf(android.R.id.button1, android.R.id.button2, android.R.id.button3)) control(view, themes)
        if (view is ScrollView) {
            view.scrollBarSize = dp(view, BicycleDesign.scrollbarSize)
            BicycleComponents.bindTheme(view, themes) {
                view.setBackgroundColor(it.secondary); view.verticalScrollbarThumbDrawable = ColorDrawable(it.foreground)
            }
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) content(view.getChildAt(i), themes)
    }
    fun control(view: TextView, themes: ThemeManager) {
        if (styled.put(view, true) != null) return
        view.typeface = Typeface.DEFAULT
        if (view !is EditText) view.textSize = if (view.isAccessibilityHeading) BicycleDesign.inputSp else BicycleDesign.bodySp
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
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = dp(view, BicycleDesign.verticalPadding)
        } else if (view is CompoundButton) {
            view.minimumHeight = dp(view, BicycleDesign.touchSize)
            BicycleComponents.bindTheme(view, themes) {
                view.setTextColor(colors(it.foreground)); view.buttonTintList = colors(it.foreground)
                if (view is Switch) {
                    view.thumbTintList = colors(it.foreground)
                    view.trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(alpha(it.foreground, .4f), alpha(it.foreground, .15f)))
                }
                view.background = RippleDrawable(ColorStateList.valueOf(alpha(it.foreground, .15f)), null, ColorDrawable(Color.WHITE))
            }
        } else if (view is Button) {
            view.layoutParams = ((view.layoutParams as? ViewGroup.MarginLayoutParams)
                ?: LinearLayout.LayoutParams(-1, -2)).apply {
                topMargin = dp(view, BicycleDesign.verticalPadding)
            }
            view.minimumHeight = dp(view, BicycleDesign.touchSize); view.isAllCaps = false
            view.setPadding(dp(view, BicycleDesign.horizontalPadding), dp(view, BicycleDesign.verticalPadding),
                dp(view, BicycleDesign.horizontalPadding), dp(view, BicycleDesign.verticalPadding))
            BicycleComponents.bindTheme(view, themes) {
                view.setTextColor(colors(it.foreground)); view.backgroundTintList = null
                view.background = RippleDrawable(ColorStateList.valueOf(alpha(it.foreground, .15f)), inputBackground(view, it), null)
                view.compoundDrawablesRelative.filterNotNull().forEach { drawable -> drawable.setTint(it.foreground) }
            }
        } else BicycleComponents.bindTheme(view, themes) { view.setTextColor(it.foreground) }
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
    fun spinner(view: Spinner, themes: ThemeManager) {
        if (styled.put(view, true) != null) return
        view.minimumHeight = dp(view, BicycleDesign.touchSize)
        BicycleComponents.bindTheme(view, themes) { tokens ->
            view.backgroundTintList = null
            view.background = LayerDrawable(arrayOf(inputBackground(view, tokens),
                BicycleIcons.drawable(view.context, BicycleIcon.Expand, tokens.foreground))).apply {
                setLayerSize(1, dp(view, BicycleDesign.iconSize), dp(view, BicycleDesign.iconSize))
                setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
                setLayerInsetEnd(1, dp(view, BicycleDesign.horizontalPadding))
            }
            view.setPopupBackgroundDrawable(shape(view, tokens.tertiary))
        }
        val selected = view.selectedItemPosition
        val original = view.adapter ?: return
        view.adapter = object : BaseAdapter(), SpinnerAdapter {
            override fun getCount() = original.count
            override fun getItem(position: Int) = original.getItem(position)
            override fun getItemId(position: Int) = original.getItemId(position)
            private fun row(position: Int, recycled: View?, dropdown: Boolean): View =
                ((recycled as? TextView) ?: BicycleComponents.text(view.context, themes, "").apply {
                    typeface = Typeface.DEFAULT; gravity = Gravity.CENTER_VERTICAL
                    minHeight = dp(view, BicycleDesign.touchSize); textSize = BicycleDesign.inputSp
                    BicycleComponents.bindTheme(this, themes) { setTextColor(it.foreground); setBackgroundColor(if (dropdown) it.tertiary else Color.TRANSPARENT) }
                }).apply {
                    text = getItem(position).toString()
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

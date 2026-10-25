package de.bcycle.app

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.database.DataSetObserver
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
    private val navigation: DialogNavigation, private val fullscreen: Boolean = true,
    private val helpEnabled:Boolean = true, private val headerRole:ThemeRole = BicycleDialogSurface.normal,
    private val plainHeader:Boolean = true, private val hideStatusBar:Boolean = false) : AlertDialog.Builder(context,
        if (fullscreen) R.style.SettingsFullscreenTheme else R.style.SettingsDialogTheme) {
    override fun setTitle(title: CharSequence?): AlertDialog.Builder {
        super.setTitle(title)
        if(title.isNullOrBlank()) { setCustomTitle(null);return this }
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
            screen.window?.let {
                DashboardFullscreen.apply(it,hideStatusBar)
                // AlertController blocks the IME when no editor exists at show(). Inline service
                // forms are installed afterwards; allow native keyboard focus in every app dialog.
                it.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }
            BicycleNavigation.hideNavigationFooter(screen)
            BicycleDialogStyle.apply(screen, themes)
            BicycleDialogSurface.window(screen,themes,headerRole)
            val help=if(fullscreen && helpEnabled) SettingsHelp.resolve(screen)?.let { page ->
                { SettingsHelp.show(context,themes,navigation,page,screen.window!!.decorView) }
            } else null
            if (fullscreen) BicycleFullscreen.apply(screen, themes,help,headerRole,plainHeader)
            else {
                BicycleDialogStyle.messageSpacing(screen)
                val top=screen.window?.decorView?.findViewById<ViewGroup>(context.resources.getIdentifier("topPanel","id","android"))
                if(top!=null && top.visibility!=View.GONE && top.childCount>0) {
                    val original=top.getChildAt(0); top.removeAllViews()
                    top.addView(BicycleNavigation.header(original,screen,themes,role=headerRole,plain=plainHeader),ViewGroup.LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.headerHeight)))
                }
            }
            BicycleDialogSurface.content(screen.window!!.decorView,themes,headerRole)
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
    /** Switch geometry for all list choices; native single/multiple selection logic stays intact. */
    private fun selectionSwitch(view:CompoundButton,tokens:ThemeTokens)=StateListDrawable().apply {
        fun toggle(checked:Boolean,enabled:Boolean):android.graphics.drawable.Drawable {
            val opacity=if(enabled) 1f else .5f
            val track=GradientDrawable().apply {
                setColor(Color.TRANSPARENT);cornerRadius=dp(view,BicycleDesign.switchHeight)/2f
                setSize(dp(view,BicycleDesign.switchWidth),dp(view,BicycleDesign.switchHeight))
                setStroke(BicycleDesign.linePixels(view.context),alpha(tokens.foreground,opacity))
            }
            val thumb=if(checked && !BicycleAppearance.flat(view.context)) BicycleChromaticSurface(view.context,tokens,BicycleInputStyle.strength(view),BicycleDesign.specialButtonRadius,tokens.primary,true).apply { alpha=(255*opacity).toInt() }
                else GradientDrawable().apply { shape=GradientDrawable.OVAL;setColor(alpha(if(checked) tokens.primary else tokens.foreground,opacity)) }
            val pill=LayerDrawable(arrayOf(track,thumb)).apply {
                setLayerSize(1,dp(view,BicycleDesign.switchThumbDiameter),dp(view,BicycleDesign.switchThumbDiameter))
                setLayerGravity(1,(if(checked) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL)
                val inset=dp(view,(BicycleDesign.switchHeight-BicycleDesign.switchThumbDiameter)/2)
                setLayerInsetStart(1,inset);setLayerInsetEnd(1,inset)
            }
            return android.graphics.drawable.InsetDrawable(pill,dp(view,BicycleDesign.choiceTextGap),0,0,0)
        }
        addState(intArrayOf(-android.R.attr.state_enabled,android.R.attr.state_checked),toggle(true,false))
        addState(intArrayOf(-android.R.attr.state_enabled),toggle(false,false))
        addState(intArrayOf(android.R.attr.state_checked),toggle(true,true))
        addState(intArrayOf(),toggle(false,true))
    }
    private fun shape(view: View, color: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(view, BicycleDesign.inputRadius).toFloat()
        stroke?.let { setStroke(BicycleDesign.linePixels(view.context), it) }
    }
    private fun inputBackground(view: View, tokens: ThemeTokens) =
        BicycleInputFrame.background(view,tokens)
    fun apply(screen: AlertDialog, themes: ThemeManager) {
        val decor = screen.window?.decorView ?: return
        // Phase 2 already styles this dialog, including its native footer; leave it unchanged.
        if (descendants(decor).any { it is RouteManagerView }) return
        if (styled.put(decor, true) != null) return
        content(decor, themes)
        decor.findViewById<View>(decor.resources.getIdentifier("buttonPanel","id","android"))?.let(BicycleFooterShadow::bind)
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
        if(view.tag=="settings_help_page") return // Help owns sticky groups and smaller subheadings.
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
            // ListView owns header/footer wrapping; wrapping that wrapper would add them twice.
            val source = (view.adapter as? HeaderViewListAdapter)?.wrappedAdapter ?: view.adapter
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
        if(view.getTag(R.id.section_title_role)==true) { BicycleComponents.sectionTypography(view);return }
        if(view is BicycleTextAction || view is BicycleSpecialButton) return // Central transparent accent text action owns its styling.
        if(view.tag=="bicycle_content_heading") return // Central content-heading typography owns this role.
        if(view is BicycleSegmentButton) return // Own radio appearance; generic controls add selection circles.
        if(view is Button || view is EditText) BicycleComponents.controlSpacing(view)
        if (styled.put(view, true) != null) {
            if(view is EditText) { BicycleComponents.inputTypography(view);return }
            if(view.isAccessibilityHeading) { view.typeface=BicycleDesign.strongTypeface;view.textSize=BicycleDesign.titleSp }
            return
        }
        view.typeface = if(view.isAccessibilityHeading) BicycleDesign.strongTypeface else BicycleDesign.bodyTypeface
        if (view !is EditText) view.textSize = if (view.isAccessibilityHeading) BicycleDesign.titleSp else BicycleDesign.bodySp
        if (view is EditText) {
            BicycleComponents.inputTypography(view);view.minimumHeight = dp(view, BicycleDesign.touchSize)
            view.setPadding(dp(view, BicycleDesign.inputHorizontalPadding), dp(view, BicycleDesign.verticalPadding),
                dp(view, BicycleDesign.inputHorizontalPadding), dp(view, BicycleDesign.verticalPadding))
            BicycleComponents.bindTheme(view, themes) { tokens ->
                view.setTextColor(colors(tokens.foreground)); view.setHintTextColor(alpha(tokens.foreground,if(view.getTag(R.id.input_placeholder_hint)==true) BicycleDesign.inputHintAlpha else .75f))
                view.backgroundTintList = null; view.background = inputBackground(view, tokens)
                view.textCursorDrawable?.mutate()?.setTint(tokens.foreground)
                view.textSelectHandle?.mutate()?.setTint(tokens.foreground)
                view.textSelectHandleLeft?.mutate()?.setTint(tokens.foreground)
                view.textSelectHandleRight?.mutate()?.setTint(tokens.foreground)
            }
            BicycleComponents.controlSpacing(view)
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
            val listChoice=view is CheckBox || view is RadioButton
            if(listChoice) {
                view.gravity=Gravity.START or Gravity.CENTER_VERTICAL
                view.setPadding(dp(view,BicycleDesign.horizontalPadding),dp(view,BicycleDesign.verticalPadding),
                    dp(view,BicycleDesign.horizontalPadding),dp(view,BicycleDesign.verticalPadding))
            }
            if(listChoice) view.accessibilityDelegate=object:View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host:View,info:android.view.accessibility.AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host,info)
                    info.className=Switch::class.java.name
                }
            }
            BicycleComponents.bindTheme(view, themes) {
                view.setTextColor(colors(it.foreground))
                if (listChoice) {
                    view.buttonTintList = null
                    view.buttonDrawable = null
                    val indicator=selectionSwitch(view,it)
                    view.setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,indicator,null)
                    indicator.state=view.drawableState
                    view.minHeight=dp(view,BicycleDesign.touchSize)
                } else view.buttonTintList = checkedColors(it.primary, it.foreground)
                if (view is Switch) {
                    view.thumbTintList = null
                    fun thumb(on:Boolean,enabled:Boolean):android.graphics.drawable.Drawable {
                        val circle=if(on && !BicycleAppearance.flat(view.context)) BicycleChromaticSurface(view.context,it,BicycleInputStyle.strength(view),BicycleDesign.specialButtonRadius,it.primary,true)
                            else GradientDrawable().apply { shape=GradientDrawable.OVAL;setColor(if(on) it.primary else it.foreground) }
                        circle.alpha=if(enabled) 255 else 127
                        return circle
                    }
                    val states=StateListDrawable().apply {
                        addState(intArrayOf(-android.R.attr.state_enabled,android.R.attr.state_checked),thumb(true,false))
                        addState(intArrayOf(-android.R.attr.state_enabled),thumb(false,false))
                        addState(intArrayOf(android.R.attr.state_checked),thumb(true,true));addState(intArrayOf(),thumb(false,true))
                    }
                    states.state=view.drawableState
                    (view.thumbDrawable as LayerDrawable).setDrawable(1,states)
                    view.thumbDrawable.state=view.drawableState
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
        button.typeface = BicycleDesign.bodyTypeface; button.textSize = BicycleDesign.bodySp
        button.isAllCaps = false
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
        view.typeface=BicycleDesign.bodyTypeface
        view.textSize=BicycleDesign.choiceSp
        view.gravity=Gravity.START or Gravity.CENTER_VERTICAL
    }
    fun spinner(view: Spinner, themes: ThemeManager, textSizeSp:Float=BicycleDesign.choiceSp, iconForLabel: ((String) -> BicycleIcon)? = null, renderLabel: ((String, Int) -> CharSequence)? = null) {
        BicycleComponents.controlSpacing(view)
        if (styled.put(view, true) != null) return
        view.minimumHeight = dp(view, BicycleDesign.touchSize)
        val border=BicycleDesign.linePixels(view.context,BicycleDesign.controlBorder.toFloat())
        view.setPadding(border,border,border,border)
        BicycleComponents.bindTheme(view, themes) { tokens ->
            view.backgroundTintList = null
            view.background = LayerDrawable(arrayOf(BicycleComponents.controlBackground(view.context,tokens),
                BicycleIcons.drawable(view.context, BicycleIcon.Expand, tokens.foreground))).apply {
                setLayerSize(1, dp(view, BicycleDesign.iconSize), dp(view, BicycleDesign.iconSize))
                setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
                setLayerInsetEnd(1, dp(view, BicycleDesign.horizontalPadding))
            }
            view.setPopupBackgroundDrawable(if(BicycleAppearance.flat(view.context)) BicycleComponents.dropdownBackground(view.context,tokens)
                else BicycleChromaticSurface(view.context,tokens,BicycleInputStyle.strength(view),BicycleDesign.inputRadius,tokens.tertiary,insetDp=BicycleDesign.popupGlowInset).apply {
                    val pad=dp(view,BicycleDesign.popupGlowInset)+border;setPadding(pad,pad,pad,pad)
                })
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
                    typeface = BicycleDesign.bodyTypeface; gravity = Gravity.CENTER_VERTICAL
                    minHeight = dp(view, BicycleDesign.touchSize); textSize = textSizeSp
                    BicycleComponents.bindTheme(this, themes) {
                        setTextColor(it.foreground); setBackgroundColor(Color.TRANSPARENT)
                        if (renderLabel != null && tag is String) text=renderLabel(tag as String,it.foreground)
                        compoundDrawablesRelative.filterNotNull().forEach { icon -> icon.setTint(it.foreground) }
                    }
                }).apply {
                    isEnabled=isEnabled(position);alpha=if(isEnabled) 1f else .4f
                    minHeight=if(dropdown) dp(view,BicycleDesign.touchSize) else dp(view,BicycleDesign.touchSize)-2*border
                    maxLines=if(dropdown) Int.MAX_VALUE else 1
                    ellipsize=if(dropdown) null else android.text.TextUtils.TruncateAt.END
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
    private class StyledListAdapter(private val original: ListAdapter, private val themes: ThemeManager) : BaseAdapter(),android.widget.SectionIndexer {
        override fun getSections():Array<out Any> = (original as? android.widget.SectionIndexer)?.sections ?: emptyArray<String>()
        override fun getPositionForSection(sectionIndex:Int)=(original as? android.widget.SectionIndexer)?.getPositionForSection(sectionIndex) ?: 0
        override fun getSectionForPosition(position:Int)=(original as? android.widget.SectionIndexer)?.getSectionForPosition(position) ?: 0
        override fun registerDataSetObserver(observer:DataSetObserver)=original.registerDataSetObserver(observer)
        override fun unregisterDataSetObserver(observer:DataSetObserver)=original.unregisterDataSetObserver(observer)
        override fun getCount() = original.count
        override fun getItem(position: Int) = original.getItem(position)
        override fun getItemId(position: Int) = original.getItemId(position)
        override fun areAllItemsEnabled() = original.areAllItemsEnabled()
        override fun isEnabled(position: Int) = original.isEnabled(position)
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            original.getView(position, convertView, parent).also { content(it, themes) }
    }
}

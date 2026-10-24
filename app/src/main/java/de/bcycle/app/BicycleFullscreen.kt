package de.bcycle.app

import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.ListView

/** Rehouse the existing AlertController views, preserving its buttons, adapters and handlers. */
internal object BicycleFullscreen {
    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(descendants(view.getChildAt(i)))
    }
    @Suppress("DEPRECATION") // Native edge-to-edge/IME compatibility for API 33+; no new dependency.
    fun apply(screen: AlertDialog, themes: ThemeManager, help:(()->Unit)?=null, headerRole:ThemeRole=ThemeRole.PRIMARY) {
        val window = screen.window ?: return
        if (descendants(window.decorView).any { it.tag == "bicycle_fullscreen" }) return
        val context = screen.context
        fun panel(name: String): ViewGroup? = window.decorView.findViewById(
            context.resources.getIdentifier(name, "id", "android"))
        val nativeFooter = panel("buttonPanel")?.takeIf { it.visibility != View.GONE }
        val nativeHeader = panel("topPanel")?.takeIf { it.visibility != View.GONE }
        val custom = panel("custom")?.let { if (it.childCount > 0) it.getChildAt(0) else null }
        val content = custom ?: panel("contentPanel")?.takeIf { it.visibility != View.GONE }
        val embeddedHeader = content?.let { descendants(it).firstOrNull { child ->
            child.tag == "settings_brand" || child.tag == "route_manager_header"
        } }
        val header = embeddedHeader ?: nativeHeader
        val footer = nativeFooter ?: content?.let { descendants(it).firstOrNull { child -> child.tag == "bicycle_action_footer" || child.tag == "route_analysis_footer" } }
        fun detach(view: View) { (view.parent as? ViewGroup)?.removeView(view) }
        val host = BicycleFullscreenHost(context).apply { orientation = LinearLayout.VERTICAL; tag = "bicycle_fullscreen"; showHeaderShadow=headerRole==ThemeRole.PRIMARY }
        BicycleComponents.bindTheme(host,themes) { host.invalidate() }
        BicycleComponents.bindTheme(host, themes) { _ ->
            // Match the existing dashboard's black system-bar/cutout backdrop.
            host.setBackgroundColor(android.graphics.Color.BLACK)
            window.setBackgroundDrawable(GradientDrawable().apply { setColor(android.graphics.Color.BLACK); cornerRadius = 0f })
        }
        header?.let {
            detach(it); it.tag = if (it.tag == null) "bicycle_fullscreen_header" else it.tag
            if (it === nativeHeader) it.setPadding(0, 0, 0, 0)
            host.addView(BicycleNavigation.header(it,screen,themes,help,headerRole), LinearLayout.LayoutParams(-1, BicycleDesign.dp(context,BicycleDesign.headerHeight)))
            val colored = descendants(it).firstOrNull { child -> child.tag == "bicycle_dialog_header" } ?: it
            BicycleComponents.bindTheme(colored, themes) { tokens -> colored.background = GradientDrawable().apply { setColor(if(headerRole==ThemeRole.SECONDARY) tokens.secondary else tokens.primary); cornerRadius = 0f } }
        }
        content?.let { original ->
            detach(original)
            descendants(original).filterIsInstance<RouteManagerView>().forEach { it.fullscreen = true }
            val scrollContainers = descendants(original).filter { it is ScrollView || it is ListView }.toList()
            scrollContainers.forEach { BicycleComponents.scrollStyle(it as ViewGroup) }
            val scrolling = scrollContainers.isNotEmpty()
            val body = if (scrolling || original is RouteAnalysisMap) original else ScrollView(context).apply {
                BicycleComponents.scrollStyle(this)
                addView(original, ViewGroup.LayoutParams(-1, -2))
            }
            // Fixed-footer wrappers are not a second page inset. Normalize the real
            // first page (Bikes, route list or details) instead of padding its wrapper.
            val first=(original as? LinearLayout)?.let { if(it.childCount>0) it.getChildAt(0) else null }
            val spacingTarget=first?.takeIf { it is ScrollView || it is RouteManagerView } ?: body
            BicycleContentSpacing.page(spacingTarget)
            host.contentSpacingTarget=spacingTarget
            body.tag = body.tag ?: "bicycle_fullscreen_content"
            BicycleComponents.bindTheme(body, themes) { body.setBackgroundColor(it.secondary) }
            host.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        if (content == null) host.addView(android.widget.FrameLayout(context).apply {
            tag = "bicycle_fullscreen_content"
            BicycleComponents.bindTheme(this, themes) { setBackgroundColor(it.secondary) }
        },
            LinearLayout.LayoutParams(-1, 0, 1f))
        footer?.let {
            detach(it); it.tag = "bicycle_fullscreen_footer"
            it.minimumHeight = BicycleDesign.dp(context, BicycleDesign.touchSize)
            BicycleComponents.bindTheme(it, themes) { tokens -> it.setBackgroundColor(tokens.secondary) }
            host.addView(it, LinearLayout.LayoutParams(-1, -2))
        }
        window.setDecorFitsSystemWindows(false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.decorView.clipToOutline = false
        host.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            WindowInsets.CONSUMED
        }
        window.setContentView(host)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        host.requestApplyInsets()
    }
}

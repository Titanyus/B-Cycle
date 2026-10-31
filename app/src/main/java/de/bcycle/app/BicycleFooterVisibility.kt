package de.bcycle.app

import android.content.Context
import android.view.View

internal interface BicycleFooterPage {
    val footerVisibility:BicycleFooterVisibility
}

/** Shared page-local footer visibility; long-press selection may temporarily reveal it. */
internal class BicycleFooterVisibility(private val context:Context) {
    private var pinned=false
    private var temporary=false
    private var footer:View?=null
    private var toggle:View?=null
    val bound get()=footer!=null
    val visible get()=pinned || temporary
    fun bindFooter(view:View) { footer=view;update() }
    fun bindToggle(view:View) {
        toggle=view
        view.setOnClickListener { pinned=!visible;temporary=false;update() }
        update()
    }
    fun temporary(show:Boolean) { temporary=show;update() }
    private fun update() {
        footer?.visibility=if(visible) View.VISIBLE else View.GONE
        toggle?.apply {
            isActivated=visible
            contentDescription=context.getString(if(visible) R.string.route_footer_hide else R.string.route_footer_show)
        }
    }
}

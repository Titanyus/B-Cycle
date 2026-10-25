package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout

/** Same native position previews for app notifications and drink reminders. */
internal class MessagePositionChoices(context:Context,themes:ThemeManager,initial:MessagePosition,
    prefix:String,private val canEdit:()->Boolean={true},private val changed:(MessagePosition)->Unit):LinearLayout(context) {
    private val tiles=mutableMapOf<MessagePosition,View>()
    init {
        fun paint(tile:View,tokens:ThemeTokens) {
            BicycleSelectionCard.apply(tile,tokens,tile.isSelected)
        }
        MessagePosition.entries.forEach { position ->
            val tile=LinearLayout(context).apply {
                tag="${prefix}_${position.name}";orientation=VERTICAL;gravity=Gravity.CENTER
                isClickable=true;isFocusable=true;contentDescription=context.getString(position.label)
                isSelected=initial==position
                stateDescription=context.getString(if(isSelected) R.string.ui_selected else R.string.ui_not_selected)
                addView(ImageView(context).apply {
                    setImageDrawable(MessagePositionPreview(position));importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                    BicycleComponents.bindTheme(this,themes) { setColorFilter(it.foreground) }
                },LayoutParams(BicycleDesign.dp(context,BicycleDesign.presetPreviewWidth),BicycleDesign.dp(context,BicycleDesign.presetPreviewHeight)))
                addView(BicycleComponents.text(context,themes,context.getString(position.label)).apply { gravity=Gravity.CENTER },LayoutParams(-1,-2))
                setOnClickListener {
                    if(!canEdit()) return@setOnClickListener
                    tiles.forEach { (p,t) ->
                        t.isSelected=p==position;t.stateDescription=context.getString(if(t.isSelected) R.string.ui_selected else R.string.ui_not_selected)
                        paint(t,themes.active.tokens)
                    }
                    changed(position)
                }
            }
            tiles[position]=tile;BicycleComponents.bindTheme(tile,themes) { paint(tile,it) }
            addView(tile,LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.bikeTileHeight),1f).apply {
                marginStart=if(position==MessagePosition.entries.first()) 0 else BicycleDesign.dp(context,BicycleDesign.controlGap)
            })
        }
    }
}

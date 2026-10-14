package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.widget.FrameLayout
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.*
import androidx.savedstate.*

/** A dialog-owned composition; the existing Activity, navigation and inset host stay intact. */
internal class ComposeDialogContent(context: Context) : FrameLayout(context), SavedStateRegistryOwner {
    private val ownerLifecycle=LifecycleRegistry(this)
    private val saved=SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get()=ownerLifecycle
    override val savedStateRegistry: SavedStateRegistry get()=saved.savedStateRegistry
    private val compose=ComposeView(context)
    private val main=android.os.Handler(android.os.Looper.getMainLooper())
    private val destroy=Runnable { if(!isAttachedToWindow) { compose.disposeComposition();ownerLifecycle.currentState=Lifecycle.State.DESTROYED } }
    init {
        tag="bicycle_compose_content"
        saved.performAttach();saved.performRestore(null)
        ownerLifecycle.currentState=Lifecycle.State.CREATED
        setViewTreeLifecycleOwner(this);setViewTreeSavedStateRegistryOwner(this)
        compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        addView(compose,LayoutParams(-1,-1))
    }
    fun content(themes:ThemeManager,body:@Composable ()->Unit) {
        compose.setContent { BicycleComposeTheme(themes) { Box(Modifier.fillMaxSize().background(Color(LocalBicycleColors.current.secondary))) { body() } } }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();main.removeCallbacks(destroy);ownerLifecycle.currentState=Lifecycle.State.RESUMED }
    override fun onWindowVisibilityChanged(visibility:Int) {
        super.onWindowVisibilityChanged(visibility)
        if(isAttachedToWindow && ownerLifecycle.currentState!=Lifecycle.State.DESTROYED)
            ownerLifecycle.currentState=if(visibility==VISIBLE) Lifecycle.State.RESUMED else Lifecycle.State.CREATED
    }
    override fun onDetachedFromWindow() { ownerLifecycle.currentState=Lifecycle.State.CREATED;main.post(destroy);super.onDetachedFromWindow() }
}
internal val LocalBicycleColors=staticCompositionLocalOf { BicycleThemes.red.tokens }
@Composable internal fun BicycleComposeTheme(themes:ThemeManager,body:@Composable ()->Unit) {
    var tokens by remember(themes) { mutableStateOf(themes.active.tokens) }
    DisposableEffect(themes) { val subscription=themes.observe { tokens=it.tokens };onDispose { subscription.close() } }
    CompositionLocalProvider(LocalBicycleColors provides tokens,content=body)
}
@Composable internal fun preferenceRevision(prefs:SharedPreferences):Int {
    var revision by remember(prefs) { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val listener=SharedPreferences.OnSharedPreferenceChangeListener { _,_->revision++ }
        prefs.registerOnSharedPreferenceChangeListener(listener);onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return revision
}
@Composable internal fun runtimeRevision(runtime:RideRuntime):Int {
    var revision by remember(runtime) { mutableIntStateOf(0) }
    DisposableEffect(runtime) { val listener:()->Unit={ revision++ };runtime.observers.add(listener);onDispose { runtime.observers.remove(listener) } }
    return revision
}
@Composable internal fun BicycleBody(body:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=BicycleDesign.horizontalPadding.dp,vertical=BicycleDesign.verticalPadding.dp),verticalArrangement=Arrangement.spacedBy(BicycleDesign.verticalPadding.dp),content=body)
}
@Composable internal fun BicycleText(value:String,modifier:Modifier=Modifier,size:Float=BicycleDesign.bodySp,strong:Boolean=false) {
    BasicText(value,modifier,style=TextStyle(color=Color(LocalBicycleColors.current.foreground),fontSize=size.sp,
        fontWeight=if(strong) FontWeight.Black else FontWeight.Normal, fontFamily=if(strong) FontFamily(BicycleDesign.strongTypeface) else FontFamily.Default))
}
@Composable internal fun BicycleSection(value:String) {
    BicycleText(value,Modifier.fillMaxWidth().semantics { heading() },BicycleDesign.titleSp,true)
}
@Composable internal fun BicycleDivider() {
    Box(Modifier.fillMaxWidth().height(with(LocalDensity.current) { BicycleDesign.dividerPixels.toDp() }).background(Color(LocalBicycleColors.current.foreground).copy(alpha=BicycleDesign.dividerAlpha)))
}
@Composable internal fun BicycleIconImage(icon:BicycleIcon,description:String?=null,size:Int=BicycleDesign.iconSize) {
    Image(painterResource(icon.resource),description,Modifier.size(size.dp),colorFilter=ColorFilter.tint(Color(if(icon.colorRole==ThemeRole.ACCENT) LocalBicycleColors.current.accent else LocalBicycleColors.current.foreground)))
}
@Composable internal fun BicycleAction(label:String,icon:BicycleIcon?=null,enabled:Boolean=true,tag:String="",action:()->Unit) {
    val color=Color(LocalBicycleColors.current.tertiary)
    Row(Modifier.fillMaxWidth().heightIn(min=BicycleDesign.touchSize.dp).background(color,RoundedCornerShape(BicycleDesign.inputRadius.dp))
        .testTag(tag).clickable(enabled=enabled,role=Role.Button,onClick=action).padding(horizontal=BicycleDesign.horizontalPadding.dp,vertical=BicycleDesign.verticalPadding.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(BicycleDesign.verticalPadding.dp)) {
        if(icon!=null) BicycleIconImage(icon)
        Box(Modifier.weight(1f).then(if(enabled) Modifier else Modifier.semantics { disabled() })) { BicycleText(label) }
    }
}
@Composable internal fun BicycleMenuRow(label:String,icon:Int=0,large:Boolean=false,tag:String,action:()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=(BicycleDesign.touchSize*(if(large) 2 else 1)).dp).testTag(tag)
        .clickable(role=Role.Button,onClick=action).padding(horizontal=BicycleDesign.horizontalPadding.dp,vertical=BicycleDesign.verticalPadding.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(BicycleDesign.verticalPadding.dp)) {
        if(icon!=0) Image(painterResource(icon),null,Modifier.size((BicycleDesign.iconSize*(if(large) 2 else 1)).dp),colorFilter=ColorFilter.tint(Color(LocalBicycleColors.current.foreground)))
        BicycleText(label,Modifier.weight(1f),BicycleDesign.bodySp*(if(large) 2 else 1))
        BicycleIconImage(BicycleIcon.Forward)
    }
}
@Composable internal fun BicycleCheck(label:String,checked:Boolean,tag:String="",enabled:Boolean=true,radio:Boolean=false,action:(Boolean)->Unit) {
    val color=Color(LocalBicycleColors.current.foreground).copy(alpha=if(enabled) 1f else .5f)
    Row(Modifier.fillMaxWidth().heightIn(min=BicycleDesign.touchSize.dp).testTag(tag)
        .semantics(mergeDescendants=true) { role=if(radio) Role.RadioButton else Role.Checkbox;if(radio) selected=checked else toggleableState=if(checked) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off }
        .clickable(enabled=enabled) { action(!checked) },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.size(22.dp)) {
            if(radio) { drawCircle(color,style=Stroke(2.dp.toPx()));if(checked) drawCircle(color,radius=size.minDimension*.25f) }
            else { drawRect(color,style=Stroke(2.dp.toPx()));if(checked) {
                drawLine(color,androidx.compose.ui.geometry.Offset(size.width*.2f,size.height*.5f),androidx.compose.ui.geometry.Offset(size.width*.45f,size.height*.75f),2.dp.toPx())
                drawLine(color,androidx.compose.ui.geometry.Offset(size.width*.45f,size.height*.75f),androidx.compose.ui.geometry.Offset(size.width*.85f,size.height*.25f),2.dp.toPx())
            } }
        }
        BicycleText(label,Modifier.weight(1f))
    }
}
@Composable internal fun BicycleDropdown(label:String,options:List<String>,selected:Int,tag:String,icon:((String)->BicycleIcon?)?=null,action:(Int)->Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var anchorHeight by remember { mutableIntStateOf(0) }
    Box {
        Row(Modifier.onSizeChanged { anchorHeight=it.height }.fillMaxWidth().heightIn(min=BicycleDesign.touchSize.dp).background(Color(LocalBicycleColors.current.tertiary),RoundedCornerShape(BicycleDesign.inputRadius.dp))
            .testTag(tag).semantics { contentDescription=label }.clickable(role=Role.Button) { expanded=!expanded }
            .padding(horizontal=BicycleDesign.horizontalPadding.dp,vertical=BicycleDesign.verticalPadding.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            val text=options.getOrNull(selected).orEmpty();icon?.invoke(text)?.let { BicycleIconImage(it) }
            BicycleText(text,Modifier.weight(1f),BicycleDesign.inputSp);BicycleIconImage(BicycleIcon.Expand)
        }
        if(expanded) Popup(alignment=Alignment.TopStart,offset=IntOffset(0,anchorHeight),onDismissRequest={ expanded=false },properties=PopupProperties(focusable=true)) {
            Column(Modifier.widthIn(min=240.dp,max=360.dp).heightIn(max=320.dp).background(Color(LocalBicycleColors.current.tertiary),RoundedCornerShape(BicycleDesign.inputRadius.dp)).verticalScroll(rememberScrollState())) {
                options.forEachIndexed { index,text ->
                    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("$tag:$index").clickable { action(index);expanded=false }.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        icon?.invoke(text)?.let { BicycleIconImage(it) };BicycleText(text,Modifier.weight(1f),BicycleDesign.inputSp)
                    }
                }
            }
        }
    }
}
@Composable internal fun BicycleInput(label:String,value:String,tag:String,keyboard:KeyboardOptions=KeyboardOptions.Default,
    visual:VisualTransformation=VisualTransformation.None,error:String?=null,onBlur:()->Unit={},changed:(String)->Unit) {
    val tokens=LocalBicycleColors.current
    var focused by remember { mutableStateOf(false) }
    BasicTextField(value,changed,Modifier.fillMaxWidth().heightIn(min=BicycleDesign.touchSize.dp).testTag(tag)
        .semantics { contentDescription=label }.onFocusChanged { focused=it.isFocused }.background(Color(tokens.tertiary),RoundedCornerShape(BicycleDesign.inputRadius.dp))
        .border(with(LocalDensity.current) { BicycleDesign.dividerPixels.toDp() },if(focused) Color(tokens.foreground) else Color.Transparent,RoundedCornerShape(BicycleDesign.inputRadius.dp))
        .padding(horizontal=BicycleDesign.inputHorizontalPadding.dp,vertical=BicycleDesign.verticalPadding.dp)
        .onBicycleBlur { onBlur() },singleLine=true,keyboardOptions=keyboard,visualTransformation=visual,
        textStyle=TextStyle(color=Color(tokens.foreground),fontSize=BicycleDesign.inputSp.sp),cursorBrush=androidx.compose.ui.graphics.SolidColor(Color(tokens.foreground)),
        decorationBox={ inner -> Box(contentAlignment=Alignment.CenterStart) { if(value.isEmpty()) BicycleText(label,size=BicycleDesign.inputSp);inner() } })
    if(error!=null) BicycleText(error,Modifier.semantics { liveRegion=LiveRegionMode.Polite })
}
private fun Modifier.onBicycleBlur(action:()->Unit):Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val latest by rememberUpdatedState(action)
    onFocusChanged { state -> if(focused && !state.isFocused) latest();focused=state.isFocused }
}

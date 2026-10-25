package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.DragEvent
import android.view.View
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RideNamingReorderTest {
    @Test fun pairDragsAsOneBlockAndPreviewCancelAndLockDoNotSave() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0)
            BikeProfileStore(a,prefs)
            val settings=RideNamingSettings(a);settings.write(RideNaming("Tour",RideNamePart.entries.toSet()))
            val themes=ThemeManager(prefs);var editable=true
            val view=RideNamingView(a,themes) { editable };a.setContentView(view)
            view.findViewWithTag<View>("ride_naming_toggle").performClick()
            fun layout() {
                shadowOf(Looper.getMainLooper()).idle()
                view.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY))
                view.layout(0,0,320,500)
            }
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat);layout()
                val row=view.findViewWithTag<LinearLayout>("ride_naming_parts")
                assertEquals(4,row.childCount)
                val stamp=view.findViewWithTag<LinearLayout>("ride_name_group_DATE_TIME")
                assertEquals(2,stamp.childCount)
                assertSame(stamp,view.findViewWithTag<View>("ride_name_DATE").parent)
                assertSame(stamp,view.findViewWithTag<View>("ride_name_TIME").parent)
                val reorder=RideNamingView::class.java.getDeclaredField("reorder").apply { isAccessible=true }.get(view) as TileReorder
                val sourceGroup=settings.read().order.first()
                val payload=TileReorder.Payload(reorder,sourceGroup.ordinal)
                fun send(action:Int,x:Float):Boolean {
                    val method=DragEvent::class.java.declaredMethods.single { it.name=="obtain" && it.parameterCount==11 }.apply { isAccessible=true }
                    val event=method.invoke(null,action,x,20f,0f,0f,payload,null,null,null,null,false) as DragEvent
                    return row.dispatchDragEvent(event)
                }
                val target=(row.getChildAt(3).left+row.getChildAt(3).right)/2f
                val before=settings.read()
                assertTrue(send(DragEvent.ACTION_DRAG_STARTED,10f))
                send(DragEvent.ACTION_DRAG_LOCATION,target)
                shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200))
                assertEquals(before,settings.read())
                val moving=row.getChildAt(0)
                assertEquals(row.width.toFloat(),moving.left+moving.translationX+moving.width,1f)
                send(DragEvent.ACTION_DRAG_ENDED,target)
                assertEquals(before,settings.read())
                send(DragEvent.ACTION_DRAG_STARTED,10f);assertTrue(send(DragEvent.ACTION_DROP,target));send(DragEvent.ACTION_DRAG_ENDED,target)
                layout()
                assertEquals(before.order.drop(1)+sourceGroup,settings.read().order)
                assertEquals(before.parts,settings.read().parts)
                view.findViewWithTag<View>("ride_name_TIME").performClick()
                assertTrue(RideNamePart.DATE in settings.read().parts)
                assertEquals(if(flat) true else false,RideNamePart.TIME in settings.read().parts)
                val saved=settings.read();editable=false;view.refreshLock()
                assertFalse(view.findViewWithTag<View>("ride_name_DATE").isEnabled)
                assertFalse(row.getChildAt(0).performAccessibilityAction(R.id.bike_move_right,null))
                assertEquals(saved,settings.read());editable=true;view.refreshLock()
                val image=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
                android.graphics.Canvas(image).apply { drawColor(themes.active.tokens.secondary);view.draw(this) }
                java.io.File("build/reports/ui/ride-name-order-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }
                    .outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
        } finally { host.pause().stop().destroy() }
    }
}

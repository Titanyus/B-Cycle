package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvHeaderFadeTest {
    @Test fun contentFadesForTwoSecondsWithoutHidingSurfaceOrChangingActions() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val header=FrameLayout(a).apply { setBackgroundColor(android.graphics.Color.RED) }
            var clicks=0
            val icon=View(a).apply { setOnClickListener { clicks++ } };header.addView(icon)
            a.setContentView(header)
            val fade=HrvHeaderFade(header);fade.show(true)
            val field=HrvHeaderFade::class.java.getDeclaredField("animation").apply { isAccessible=true }
            val animation=field.get(fade) as android.animation.ValueAnimator
            assertEquals(2000,animation.duration)
            animation.currentPlayTime=1000
            assertTrue(icon.alpha in .3f.. .7f)
            fade.show(true) // Live packets must not restart the animation.
            assertSame(animation,field.get(fade));animation.end()
            assertEquals(0f,icon.alpha,.001f);assertEquals(1f,header.alpha,0f)
            assertEquals(View.VISIBLE,icon.visibility);icon.performClick();assertEquals(1,clicks)
            fade.show(false);assertEquals(1f,icon.alpha,0f)
            fade.show(true);fade.close()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2200))
            assertEquals(1f,icon.alpha,0f)
        } finally { host.pause().stop().destroy() }
    }
}

package de.bcycle.app

import android.view.View
import android.view.ViewGroup
import android.window.OnBackInvokedCallback
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33,35,36])
class SdkNavigationTest {
    private fun views(v: View): List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    @Test fun nativeBackClosesMapFullscreenAndReleasesCallbackForDefaultActivityBack() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val root=activity.findViewById<View>(android.R.id.content)
            val registered=MainActivity::class.java.getDeclaredField("mapBackRegistered").apply { isAccessible=true }
            assertFalse(registered.getBoolean(activity))
            views(root).single { it.contentDescription=="Karte im Vollbild" }.performClick()
            assertTrue(registered.getBoolean(activity))
            assertEquals(View.GONE,views(root).filterIsInstance<DashboardView>().single().visibility)
            val back=MainActivity::class.java.getDeclaredField("mapBack").apply { isAccessible=true }.get(activity) as OnBackInvokedCallback
            back.onBackInvoked()
            assertEquals(View.VISIBLE,views(root).filterIsInstance<DashboardView>().single().visibility)
            assertFalse(registered.getBoolean(activity));assertFalse(activity.isFinishing)
        } finally { controller.pause().stop().destroy() }
    }
}

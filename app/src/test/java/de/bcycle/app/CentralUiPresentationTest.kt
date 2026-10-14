package de.bcycle.app

import android.text.TextWatcher
import android.text.Editable
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CentralUiPresentationTest {
    @Test fun allExistingFieldLabelsAreResourceBackedAndKeepTheirGermanWordingAndStableIds() {
        val context = RuntimeEnvironment.getApplication()
        val data = LiveData()
        for (field in DashboardField.baseOptions.filter { it.trainingMetric == null }) {
            assertNotNull(field.id, DashboardTextResources.title(context, field.id))
            assertEquals(field.id, field.title(data), field.title(context, data))
            assertEquals(field.id, field.dashboardTitle(data), field.dashboardTitle(context, data))
            assertEquals(field, DashboardField.fromId(field.id))
        }
        for (group in DashboardField.groups(data)) {
            assertEquals(group.source, DashboardTextResources.source(context, group.source))
            assertEquals(group.title, DashboardTextResources.section(context, group.title))
        }
        assertEquals("Reichweite · Stufe 4", DashboardField("RANGE_3").title(context, data))
        assertEquals("Höhe (MSL)", DashboardField("GPS_ALTITUDE").title(context, data, GpsValues(altitudeM = 123.0, seaLevel = true)))
    }

    @Test fun statusUpdatesArePoliteDoNotRepeatIdenticalTextAndPreserveCallerVisibilityPolicy() {
        val context = RuntimeEnvironment.getApplication()
        val themes = ThemeManager(context.getSharedPreferences("feedback", 0))
        val status = BicycleFeedback.status(context, themes)
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, status.accessibilityLiveRegion)
        var changes = 0
        status.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changes++ }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        BicycleFeedback.update(status, context.getString(R.string.analysis_loading))
        BicycleFeedback.update(status, context.getString(R.string.analysis_loading))
        assertEquals(1, changes)
        BicycleFeedback.update(status, null)
        assertEquals(View.VISIBLE, status.visibility)
        BicycleFeedback.update(status, "", hideWhenEmpty = true)
        assertEquals(View.GONE, status.visibility)
        BicycleFeedback.update(status, context.getString(R.string.analysis_failed), hideWhenEmpty = true)
        assertEquals(View.VISIBLE, status.visibility)
        assertEquals(themes.active.tokens.foreground, status.currentTextColor)
        BicycleFeedback.show(context, R.string.analysis_failed, long = false)
        assertEquals(context.getString(R.string.analysis_failed), ShadowToast.getTextOfLatestToast())
    }
}

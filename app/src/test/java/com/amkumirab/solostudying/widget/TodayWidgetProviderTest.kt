package com.amkumirab.solostudying.widget

import android.content.Context
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodayWidgetProviderTest {

    @Test
    fun `remote views render current mission and quick start duration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val snapshot = TodayWidgetSnapshot(
            isReady = true,
            progressPercent = 40,
            progressLabel = "24 / 60 MIN TODAY",
            streakLabel = "5-DAY STREAK",
            actionTitle = "Review lecture notes",
            actionDetail = "30 min · Today's daily quest",
            quickStartMinutes = 45,
        )

        val root = TodayWidgetProvider.createRemoteViews(context, snapshot).apply(
            context,
            FrameLayout(context),
        )

        assertEquals("5-DAY STREAK", root.findViewById<TextView>(R.id.widget_streak).text)
        assertEquals("24 / 60 MIN TODAY", root.findViewById<TextView>(R.id.widget_progress_label).text)
        assertEquals("Review lecture notes", root.findViewById<TextView>(R.id.widget_action_title).text)
        assertEquals("START 45 MIN", root.findViewById<TextView>(R.id.widget_quick_start).text)
        assertEquals(40, root.findViewById<ProgressBar>(R.id.widget_progress).progress)
    }

    @Test
    fun `setup state uses an open app action`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val snapshot = TodayWidgetSnapshot(
            isReady = false,
            progressPercent = 0,
            progressLabel = "Open the app to set up your study plan",
            streakLabel = "READY FOR YOUR FIRST QUEST",
            actionTitle = "Begin your journey",
            actionDetail = "Complete setup to unlock today's mission",
            quickStartMinutes = 25,
        )

        val root = TodayWidgetProvider.createRemoteViews(context, snapshot).apply(
            context,
            FrameLayout(context),
        )

        assertEquals("OPEN APP", root.findViewById<TextView>(R.id.widget_quick_start).text)
    }
}

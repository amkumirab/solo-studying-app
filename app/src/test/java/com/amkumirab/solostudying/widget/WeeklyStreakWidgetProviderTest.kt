package com.amkumirab.solostudying.widget

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.R
import com.amkumirab.solostudying.domain.streak.WeeklyStreakDay
import com.amkumirab.solostudying.domain.streak.WeeklyStreakDayState
import com.amkumirab.solostudying.domain.streak.WeeklyStreakSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WeeklyStreakWidgetProviderTest {

    @Test
    fun `remote views render streak message week states and companion`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val states = listOf(
            WeeklyStreakDayState.Completed,
            WeeklyStreakDayState.Completed,
            WeeklyStreakDayState.Today,
            WeeklyStreakDayState.Upcoming,
            WeeklyStreakDayState.Upcoming,
            WeeklyStreakDayState.Rest,
            WeeklyStreakDayState.Rest,
        )
        val snapshot = WeeklyStreakSnapshot(
            isReady = true,
            streakDays = 12,
            message = "25 min to protect your streak",
            days = states.mapIndexed { index, state ->
                WeeklyStreakDay(
                    date = LocalDate.of(2026, 9, 14).plusDays(index.toLong()),
                    dayLabel = listOf("M", "T", "W", "T", "F", "S", "S")[index],
                    studiedMinutes = if (state == WeeklyStreakDayState.Completed) 45 else 0,
                    targetMinutes = if (state == WeeklyStreakDayState.Rest) 0 else 45,
                    state = state,
                )
            },
        )

        val root = WeeklyStreakWidgetProvider.createRemoteViews(context, snapshot).apply(
            context,
            FrameLayout(context),
        )

        assertEquals("🔥 12 DAYS", root.findViewById<TextView>(R.id.streak_widget_title).text)
        assertEquals(
            "25 min to protect your streak",
            root.findViewById<TextView>(R.id.streak_widget_message).text,
        )
        assertEquals("M", root.findViewById<TextView>(R.id.streak_day_label_1).text)
        assertEquals("✓", root.findViewById<TextView>(R.id.streak_day_status_1).text)
        assertEquals("•", root.findViewById<TextView>(R.id.streak_day_status_3).text)
        assertEquals("–", root.findViewById<TextView>(R.id.streak_day_status_7).text)
        assertNotNull(root.findViewById<ImageView>(R.id.streak_companion).drawable)
    }
}

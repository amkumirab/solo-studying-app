package com.amkumirab.solostudying.planner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudySessionReassignmentCodecTest {
    @Test fun `legacy bytes survive reads and reassigned v9 save is atomic and restart safe`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME)
        try {
            val today = LocalDate.of(2026, 10, 27)
            val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
            val config = PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(6), 5, 0), PlannerCourse(2, 120, today.plusDays(6), 3, 0)), List(7) { 120 }, 30, studyWindows = windows)
            val original = StudyPlan(config, listOf(StudyBlock(1, 1, today, 30, 1020)))
            val goals = listOf(1, 2).map { BossEntity(id = it, name = "Course $it", difficulty = "Hard", requiredMinutes = 120) }
            val raw = encodeStudyPlan(original)
            assertEquals(6, JSONObject(raw).getInt("version"))
            val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            assertTrue(prefs.edit().putString("plan", raw).commit())
            val store = StudyPlannerStore(context)
            val before = store.readData().snapshot
            assertEquals(original, before.plan)
            assertEquals(raw, prefs.getString("plan", null))
            val changed = previewReassignStudySession(original, goals, today, 1, 2).plan!!
            assertEquals(9, JSONObject(encodeStudyPlan(changed)).getInt("version"))
            assertEquals(changed, decodeStudyPlan(encodeStudyPlan(changed)))
            assertEquals(raw, prefs.getString("plan", null))
            assertTrue(store.writeSnapshot(before, before.copy(plan = changed)))
            assertEquals(changed, StudyPlannerStore(context).readData().snapshot.plan)
            assertFalse(store.writeSnapshot(before, before.copy(plan = original)))
            assertEquals(changed, store.read().plan)
        } finally { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    }
}

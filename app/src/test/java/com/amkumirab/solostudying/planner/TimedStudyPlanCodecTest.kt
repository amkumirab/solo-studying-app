package com.amkumirab.solostudying.planner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimedStudyPlanCodecTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val start = LocalDate.of(2026, 10, 19)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1080))) })
    private val config = PlannerConfig(start, listOf(PlannerCourse(1, 90, start.plusDays(13), 3, 120)), List(7) { 60 }, 25,
        weeklyGoalMinutes = 60, studyWindows = windows).withWeeklyScheduling(listOf(PlannerSubject(1, "Physics", preferredWeeklyMinutes = 45)))
    private val prefs get() = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
    @Before fun reset() { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `v6 round trip retains exact times limits order baselines and frozen windows`() {
        val plan = buildStudyPlan(config)
        val raw = encodeStudyPlan(plan)
        assertEquals(6, JSONObject(raw).getInt("version"))
        assertEquals("timed", JSONObject(raw).getString("kind"))
        assertEquals(plan, decodeStudyPlan(raw))
        assertTrue(StudyPlannerStore(context).write(plan))
        assertEquals(plan, StudyPlannerStore(context).read().plan)
        val noGoal = buildStudyPlan(config.copy(weeklyGoalMinutes = null, weeklyScheduling = null))
        assertEquals(noGoal, decodeStudyPlan(encodeStudyPlan(noGoal)))
    }

    @Test fun `malformed timed records cannot overwrite the saved bytes`() {
        val raw = encodeStudyPlan(buildStudyPlan(config))
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.put("kind", "date_only") },
            { it.getJSONObject("plan").remove("studyWindows") },
            { it.getJSONObject("plan").remove("weeklyGoalMinutes") },
            { it.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).remove("startMinute") },
            { it.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("startMinute", "1020") },
            { it.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("startMinute", 1020.5) },
            { it.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("startMinute", 1430) },
            { it.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("future", true) },
        )
        mutations.forEach { change ->
            val text = JSONObject(raw).also(change).toString()
            prefs.edit().putString("plan", text).commit()
            assertNotNull(StudyPlannerStore(context).read().error)
            assertFalse(StudyPlannerStore(context).write(buildStudyPlan(config)))
            assertEquals(text, prefs.getString("plan", null))
        }
    }

    @Test fun `v5 schedules stay date only and unchanged on read even with saved windows`() {
        val legacy = buildStudyPlan(config.copy(studyWindows = null))
        val raw = encodeStudyPlan(legacy)
        prefs.edit().putString("plan", raw).putString("availability", encodeWeeklyAvailability(windows)).commit()
        val loaded = StudyPlannerStore(context).readData()
        assertEquals(legacy, loaded.plan)
        assertTrue(loaded.plan!!.blocks.all { it.startMinute == null })
        assertNull(loaded.plan.config.studyWindows)
        assertEquals(raw, prefs.getString("plan", null))
    }
}

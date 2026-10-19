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
class WeeklyStudyGoalStorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val start = LocalDate.of(2026, 10, 19)
    private val legacy = buildStudyPlan(PlannerConfig(start,
        listOf(PlannerCourse(1, 300, start.plusDays(6), 5, 120)), List(7) { 60 }, 30))
    @Before fun reset() { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `weekly goal survives reopening with blocks and credit baseline intact`() {
        val plan = legacy.copy(config = legacy.config.copy(weeklyGoalMinutes = 240))
        val encoded = encodeStudyPlan(plan)
        assertEquals(4, JSONObject(encoded).getInt("version"))
        assertEquals(plan, decodeStudyPlan(encoded))
        assertTrue(StudyPlannerStore(context).write(plan))
        assertEquals(plan, StudyPlannerStore(context).read().plan)
    }

    @Test fun `unchanged legacy plan keeps v3 and no goal is invented on read`() {
        val raw = encodeStudyPlan(legacy)
        assertEquals(3, JSONObject(raw).getInt("version"))
        val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("plan", raw).commit()
        assertNull(StudyPlannerStore(context).read().plan!!.config.weeklyGoalMinutes)
        assertEquals(raw, prefs.getString("plan", null))
    }

    @Test fun `goal schema rejects coercion missing fields and invalid bounds`() {
        val plan = legacy.copy(config = legacy.config.copy(weeklyGoalMinutes = 240))
        val raw = encodeStudyPlan(plan)
        listOf<Any>("240", -1, 3361, 240.5, JSONObject.NULL).forEach { invalid ->
            val broken = JSONObject(raw).apply { getJSONObject("plan").put("weeklyGoalMinutes", invalid) }.toString()
            assertThrows(Exception::class.java) { decodeStudyPlan(broken) }
        }
        val missing = JSONObject(raw).apply { getJSONObject("plan").remove("weeklyGoalMinutes") }.toString()
        assertThrows(Exception::class.java) { decodeStudyPlan(missing) }
        val disguised = JSONObject(raw).put("version", 3).toString()
        assertThrows(Exception::class.java) { decodeStudyPlan(disguised) }
    }
}

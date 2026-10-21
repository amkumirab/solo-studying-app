package com.amkumirab.solostudying.planner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeightedWeeklyPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val start = LocalDate.of(2026, 10, 19)
    private val config = PlannerConfig(start, listOf(PlannerCourse(1, 90, start.plusDays(20), 5, 120), PlannerCourse(2, 180, start.plusDays(20), 3, 300)), List(7) { 60 }, 30, 120)
        .withWeeklyScheduling(listOf(PlannerSubject(1, "Physics", preferredWeeklyMinutes = 31)))
    private val prefs get() = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
    @Before fun reset() { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `v5 stores the policy and reopens the exact schedule with its baselines`() {
        val plan = buildStudyPlan(config)
        val raw = encodeStudyPlan(plan)
        assertEquals(5, JSONObject(raw).getInt("version"))
        assertEquals(plan, decodeStudyPlan(raw))
        assertTrue(StudyPlannerStore(context).write(plan))
        assertEquals(plan, StudyPlannerStore(context).read().plan)
        assertEquals(mapOf(1 to 31), decodeStudyPlan(raw).config.weeklyScheduling!!.courseMinuteLimits)
    }

    @Test fun `literal v4 advice plan above its goal remains readable and is never rewritten on open`() {
        val raw = """{"version":4,"kind":"date_only","plan":{"start":"2026-10-19","sessionMinutes":60,"capacity":[60,60,60,60,60,60,60],"weeklyGoalMinutes":60,"courses":[{"bossId":1,"remainingMinutes":120,"finishBy":"2026-11-01","priority":5,"baselineSeconds":120}],"blocks":[{"id":19,"bossId":1,"date":"2026-10-19","minutes":60},{"id":4,"bossId":1,"date":"2026-10-20","minutes":60}]}}"""
        prefs.edit().putString("plan", raw).commit()
        val plan = StudyPlannerStore(context).read().plan!!
        assertNull(plan.config.weeklyScheduling)
        assertEquals(120, plan.blocks.sumOf { it.minutes })
        assertEquals(listOf(19, 4), plan.blocks.map { it.id })
        assertEquals(raw, prefs.getString("plan", null))
    }

    @Test fun `v5 rejects missing duplicate unknown and coercible limits while protecting saved bytes`() {
        val raw = encodeStudyPlan(buildStudyPlan(config))
        val invalid = listOf(
            JSONObject(raw).apply { getJSONObject("plan").remove("weeklyCourseLimits") },
            JSONObject(raw).apply { getJSONObject("plan").put("weeklyCourseLimits", JSONArray().put(JSONObject().put("bossId", 1).put("minutes", "31"))) },
            JSONObject(raw).apply { getJSONObject("plan").put("weeklyCourseLimits", JSONArray().put(JSONObject().put("bossId", 99).put("minutes", 31))) },
            JSONObject(raw).apply { val limits = getJSONObject("plan").getJSONArray("weeklyCourseLimits"); limits.put(limits.getJSONObject(0)) },
            JSONObject(raw).apply { getJSONObject("plan").getJSONArray("weeklyCourseLimits").getJSONObject(0).put("future", true) },
            JSONObject(raw).apply { getJSONObject("plan").put("weeklyGoalMinutes", 0) },
        )
        invalid.forEach { json ->
            val text = json.toString()
            prefs.edit().putString("plan", text).commit()
            assertNotNull(StudyPlannerStore(context).read().error)
            assertFalse(StudyPlannerStore(context).write(buildStudyPlan(config)))
            assertEquals(text, prefs.getString("plan", null))
        }
    }

    @Test fun `removing one subject prunes only its policy limit and preserves surviving ids`() {
        val plan = buildStudyPlan(config)
        val snapshot = PlannerSnapshot(plan, listOf(PlannerSubject(1, "Physics"), PlannerSubject(2, "Math")))
        val updated = snapshot.withoutSubject(1).plan!!
        assertEquals(emptyMap<Int, Int>(), updated.config.weeklyScheduling!!.courseMinuteLimits)
        assertEquals(plan.blocks.filter { it.bossId == 2 }, updated.blocks)
        assertEquals(plan.config.courses.last().baselineSeconds, updated.config.courses.single().baselineSeconds)
    }
}

package com.amkumirab.solostudying.planner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomStudySessionCodecTest {
    private fun plan(): StudyPlan {
        val today = LocalDate.of(2026, 10, 26)
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
        return StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(6), 5, 0)), List(7) { 120 }, 30, studyWindows = windows),
            listOf(StudyBlock(1, 1, today, 90, 1020, locked = true, manual = true), StudyBlock(2, 1, today.plusDays(1), 30, 1020)))
    }

    @Test fun `v9 retains manual generated duration and lock markers`() {
        val original = plan()
        val raw = encodeStudyPlan(original)
        assertEquals(9, JSONObject(raw).getInt("version"))
        assertEquals(original, decodeStudyPlan(raw))
        val edited = original.copy(blocks = original.blocks.map { it.copy(durationEdited = true) })
        assertEquals(edited, decodeStudyPlan(encodeStudyPlan(edited)))
    }

    @Test fun `manual marker is required strictly boolean and confined to v9 timed records`() {
        val raw = encodeStudyPlan(plan())
        listOf<Any>(1, "true", JSONObject.NULL).forEach { value ->
            val json = JSONObject(raw)
            json.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("manual", value)
            assertTrue(runCatching { decodeStudyPlan(json.toString()) }.isFailure)
        }
        val missing = JSONObject(raw)
        missing.getJSONObject("plan").getJSONArray("blocks").getJSONObject(1).remove("manual")
        assertTrue(runCatching { decodeStudyPlan(missing.toString()) }.isFailure)
        val unmarked = JSONObject(raw)
        unmarked.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("manual", false)
        assertTrue(runCatching { decodeStudyPlan(unmarked.toString()) }.isFailure)
        assertTrue(runCatching { decodeStudyPlan(JSONObject(raw).put("version", 8).toString()) }.isFailure)
        assertTrue(runCatching { plan().copy(config = plan().config.copy(studyWindows = null)) }.isFailure)
    }

    @Test fun `v8 read bytes stay intact and manual saves retain snapshot conflict protection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME)
        try {
            val legacy = plan().copy(blocks = plan().blocks.map { it.copy(manual = false, durationEdited = true) })
            val raw = encodeStudyPlan(legacy)
            assertEquals(8, JSONObject(raw).getInt("version"))
            val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            assertTrue(prefs.edit().putString("plan", raw).commit())
            val store = StudyPlannerStore(context)
            val before = store.readData().snapshot
            assertEquals(legacy, before.plan)
            assertEquals(raw, prefs.getString("plan", null))
            assertTrue(store.writeSnapshot(before, before.copy(plan = plan())))
            assertEquals(plan(), StudyPlannerStore(context).read().plan)
            assertFalse(store.writeSnapshot(before, before))
            val corrupt = JSONObject(encodeStudyPlan(plan()))
            corrupt.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("manual", "true")
            assertTrue(prefs.edit().putString("plan", corrupt.toString()).commit())
            assertNotNull(store.read().error)
            assertFalse(store.write(legacy))
            assertEquals(corrupt.toString(), prefs.getString("plan", null))
        } finally { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    }
}

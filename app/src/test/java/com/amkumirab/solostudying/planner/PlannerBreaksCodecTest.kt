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
class PlannerBreaksCodecTest {
    private fun plan(): StudyPlan {
        val day = LocalDate.of(2026, 10, 26)
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 660))) })
        return StudyPlan(PlannerConfig(day, listOf(PlannerCourse(1, 150, day, 5, 120)), List(7) { 120 }, 60,
            studyWindows = windows, breakMinutes = 10), listOf(
            StudyBlock(99, 1, day, 30, 580, locked = true),
            StudyBlock(4, 1, day, 30, 540, durationEdited = true, manual = true)))
    }

    @Test fun `v10 round trip preserves gap markers ledger order and baselines`() {
        val source = plan()
        val raw = encodeStudyPlan(source)
        assertEquals(10, JSONObject(raw).getInt("version"))
        assertEquals(source, decodeStudyPlan(raw))
        val legacy = source.copy(config = source.config.copy(breakMinutes = 0))
        assertEquals(9, JSONObject(encodeStudyPlan(legacy)).getInt("version"))
        assertEquals(legacy, decodeStudyPlan(encodeStudyPlan(legacy)))
    }

    @Test fun `strict gap and marker fields reject malformed and future records`() {
        val raw = encodeStudyPlan(plan())
        listOf<Any>("10", 10.5, true, JSONObject.NULL, -1, 31).forEach { invalid ->
            val json = JSONObject(raw)
            json.getJSONObject("plan").put("breakMinutes", invalid)
            assertTrue("$invalid", runCatching { decodeStudyPlan(json.toString()) }.isFailure)
        }
        val missing = JSONObject(raw)
        missing.getJSONObject("plan").remove("breakMinutes")
        assertTrue(runCatching { decodeStudyPlan(missing.toString()) }.isFailure)
        val old = JSONObject(raw).put("version", 9)
        assertTrue(runCatching { decodeStudyPlan(old.toString()) }.isFailure)
        assertTrue(runCatching { decodeStudyPlan(JSONObject(raw).put("version", 11).toString()) }.isFailure)
    }

    @Test fun `legacy read is byte preserving and stale saves cannot erase a gap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME)
        try {
            val source = plan()
            val legacy = source.copy(config = source.config.copy(breakMinutes = 0))
            val raw = encodeStudyPlan(legacy)
            val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            assertTrue(prefs.edit().putString("plan", raw).commit())
            val store = StudyPlannerStore(context)
            val before = store.readData().snapshot
            assertEquals(legacy, before.plan)
            assertEquals(raw, prefs.getString("plan", null))
            assertTrue(store.writeSnapshot(before, before.copy(plan = source)))
            assertFalse(store.writeSnapshot(before, before.copy(plan = legacy)))
            assertEquals(source, StudyPlannerStore(context).readData().plan)
        } finally { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    }

    @Test fun `malformed v10 storage is never silently replaced`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME)
        try {
            val damaged = JSONObject(encodeStudyPlan(plan()))
            damaged.getJSONObject("plan").put("breakMinutes", "10")
            val raw = damaged.toString()
            val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            assertTrue(prefs.edit().putString("plan", raw).commit())
            val store = StudyPlannerStore(context)
            assertNotNull(store.readData().planError)
            assertFalse(store.write(plan()))
            assertEquals(raw, prefs.getString("plan", null))
        } finally { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    }
}

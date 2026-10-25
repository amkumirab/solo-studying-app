package com.amkumirab.solostudying.planner

import com.amkumirab.solostudying.domain.planner.*
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudyDurationCodecTest {
    private fun plan(): StudyPlan {
        val today = LocalDate.of(2026, 10, 19)
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
        return StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 90, today.plusDays(6), 5, 120)), List(7) { 120 }, 25, studyWindows = windows),
            listOf(StudyBlock(99, 1, today, 45, 1020, true, durationEdited = true), StudyBlock(4, 1, today.plusDays(1), 25, 1020)))
    }

    @Test fun `v8 retains edited lengths independent of preferred length and lock flags`() {
        val original = plan()
        val raw = encodeStudyPlan(original)
        assertEquals(8, JSONObject(raw).getInt("version"))
        assertEquals(original, decodeStudyPlan(raw))
        assertEquals(original.copy(blocks = original.blocks.map { it.copy(locked = false) }),
            decodeStudyPlan(encodeStudyPlan(original.copy(blocks = original.blocks.map { it.copy(locked = false) }))))
    }

    @Test fun `v8 strictly rejects missing coerced and out of range edit fields`() {
        val raw = encodeStudyPlan(plan())
        listOf<Any>(1, "true", JSONObject.NULL).forEach { bad ->
            val json = JSONObject(raw)
            json.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("durationEdited", bad)
            assertTrue(runCatching { decodeStudyPlan(json.toString()) }.isFailure)
        }
        val missing = JSONObject(raw)
        missing.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).remove("durationEdited")
        assertTrue(runCatching { decodeStudyPlan(missing.toString()) }.isFailure)
        val unmarked = JSONObject(raw)
        unmarked.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("durationEdited", false)
        assertTrue(runCatching { decodeStudyPlan(unmarked.toString()) }.isFailure)
        val tooLong = JSONObject(raw)
        tooLong.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("minutes", 121)
        assertTrue(runCatching { decodeStudyPlan(tooLong.toString()) }.isFailure)
        assertTrue(runCatching { decodeStudyPlan(JSONObject(raw).put("version", 7).toString()) }.isFailure)
    }

    @Test fun `legacy bytes survive reads and stale or corrupt records cannot overwrite an edit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME)
        try {
            val legacy = plan().copy(blocks = plan().blocks.map { it.copy(minutes = 25, durationEdited = false) })
            val raw = encodeStudyPlan(legacy)
            assertEquals(7, JSONObject(raw).getInt("version"))
            val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            assertTrue(prefs.edit().putString("plan", raw).commit())
            val store = StudyPlannerStore(context)
            val before = store.readData().snapshot
            assertEquals(legacy, before.plan)
            assertEquals(raw, prefs.getString("plan", null))
            val changed = plan()
            assertTrue(store.writeSnapshot(before, before.copy(plan = changed)))
            assertEquals(changed, StudyPlannerStore(context).read().plan)
            assertFalse(store.writeSnapshot(before, before))
            val corrupt = JSONObject(encodeStudyPlan(changed))
            corrupt.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("durationEdited", "true")
            assertTrue(prefs.edit().putString("plan", corrupt.toString()).commit())
            assertNotNull(store.read().error)
            assertFalse(store.write(legacy))
            assertEquals(corrupt.toString(), prefs.getString("plan", null))
        } finally { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    }
}

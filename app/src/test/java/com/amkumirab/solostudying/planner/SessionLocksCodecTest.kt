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
class SessionLocksCodecTest {
    private fun plan(): StudyPlan {
        val day = LocalDate.of(2026, 10, 19)
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1080))) })
        return StudyPlan(PlannerConfig(day, listOf(PlannerCourse(1, 60, day, 5, 120)), List(7) { 60 }, 30, studyWindows = windows),
            listOf(StudyBlock(99, 1, day, 30, 1050, locked = true), StudyBlock(4, 1, day, 30, 1020)))
    }

    @Test fun `v7 strict round trip preserves locks IDs order times and baselines`() {
        val original = plan()
        val raw = encodeStudyPlan(original)
        assertEquals(7, JSONObject(raw).getInt("version"))
        assertEquals(original, decodeStudyPlan(raw))
        val unlocked = original.copy(blocks = original.blocks.map { it.copy(locked = false) })
        assertEquals(6, JSONObject(encodeStudyPlan(unlocked)).getInt("version"))
        assertEquals(unlocked, decodeStudyPlan(encodeStudyPlan(unlocked)))
    }

    @Test fun `missing coerced and unknown lock fields are rejected`() {
        val raw = encodeStudyPlan(plan())
        listOf<Any>("true", 1, JSONObject.NULL).forEach { invalid ->
            val json = JSONObject(raw)
            json.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("locked", invalid)
            assertTrue(runCatching { decodeStudyPlan(json.toString()) }.isFailure)
        }
        val missing = JSONObject(raw)
        missing.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).remove("locked")
        assertTrue(runCatching { decodeStudyPlan(missing.toString()) }.isFailure)
        val legacy = JSONObject(raw).put("version", 6)
        assertTrue(runCatching { decodeStudyPlan(legacy.toString()) }.isFailure)
    }

    @Test fun `legacy reads never rewrite bytes and stale saves cannot remove a stored lock`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME)
        try {
            val original = plan()
            val unlocked = original.copy(blocks = original.blocks.map { it.copy(locked = false) })
            val raw = encodeStudyPlan(unlocked)
            val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            assertTrue(prefs.edit().putString("plan", raw).commit())
            val store = StudyPlannerStore(context)
            val before = store.readData().snapshot
            assertEquals(unlocked, before.plan)
            assertEquals(raw, prefs.getString("plan", null))
            assertTrue(store.writeSnapshot(before, before.copy(plan = original)))
            assertFalse(store.writeSnapshot(before, before.copy(plan = unlocked)))
            assertEquals(original, StudyPlannerStore(context).readData().plan)
            val damaged = JSONObject(encodeStudyPlan(original))
            damaged.getJSONObject("plan").getJSONArray("blocks").getJSONObject(0).put("locked", "false")
            val invalid = damaged.toString()
            assertTrue(prefs.edit().putString("plan", invalid).commit())
            assertNotNull(store.readData().planError)
            assertFalse(store.write(original))
            assertEquals(invalid, prefs.getString("plan", null))
        } finally { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    }
}

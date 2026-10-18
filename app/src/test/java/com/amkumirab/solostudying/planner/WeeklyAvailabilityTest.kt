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
class WeeklyAvailabilityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store get() = StudyPlannerStore(context)
    private val week get() = defaultWeeklyAvailability(listOf(120, 120, 120, 120, 120, 240, 120))
    @Before fun reset() { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `weekly totals are exact and disabled days keep their windows`() {
        assertEquals(960, week.totalMinutes)
        val disabled = week.copy(days = week.days.mapIndexed { index, day -> if (index == 5) day.copy(enabled = false) else day })
        assertEquals(720, disabled.totalMinutes)
        assertEquals(week.days[5].windows, disabled.days[5].windows)
        assertEquals(0, disabled.weekdayMinutes[5])
        assertEquals(0, week.copy(days = week.days.map { it.copy(enabled = false) }).totalMinutes)
    }

    @Test fun `half hour windows allow midnight boundaries and adjacency but reject overlaps`() {
        val day = StudyDayAvailability(true, listOf(StudyTimeWindow(0, 30), StudyTimeWindow(30, 60), StudyTimeWindow(1410, 1440)))
        assertEquals(90, day.capacityMinutes)
        listOf<() -> Any>(
            { StudyTimeWindow(-30, 30) }, { StudyTimeWindow(0, 1441) },
            { StudyTimeWindow(0, 15) }, { StudyTimeWindow(61, 90) },
            { StudyTimeWindow(120, 120) }, { StudyTimeWindow(1410, 30) },
            { StudyDayAvailability(true, emptyList()) },
            { StudyDayAvailability(true, listOf(StudyTimeWindow(60, 180), StudyTimeWindow(120, 240))) },
            { StudyDayAvailability(true, listOf(StudyTimeWindow(60, 90), StudyTimeWindow(0, 30))) },
            { StudyDayAvailability(true, listOf(StudyTimeWindow(0, 510))) },
            { WeeklyAvailability(emptyList()) },
        ).forEach { assertThrows(IllegalArgumentException::class.java) { it() } }
    }

    @Test fun `first draft rounds legacy minutes only in the suggested windows`() {
        val minutes = listOf(165, 1, 480, 0, 60, 240, 90)
        val suggested = defaultWeeklyAvailability(minutes)
        assertEquals(listOf(150, 0, 480, 0, 60, 240, 90), suggested.weekdayMinutes)
        assertEquals(1440, suggested.days[2].windows.single().endMinute)
        assertNull(store.readData().availability)
        assertEquals(165, minutes.first())
    }

    @Test fun `availability roundtrips without losing disabled windows`() {
        val value = week.copy(days = week.days.mapIndexed { index, day -> if (index == 0) day.copy(enabled = false) else day })
        assertEquals(value, decodeWeeklyAvailability(encodeWeeklyAvailability(value)))
    }

    @Test fun `codec rejects unknown schemas numeric coercion oversized input and trailing data`() {
        val encoded = encodeWeeklyAvailability(week)
        listOf(
            "invalid", "x".repeat(16385), encoded + "{}",
            JSONObject(encoded).put("version", 2).toString(),
            JSONObject(encoded).put("extra", true).toString(),
            JSONObject(encoded).apply { getJSONArray("days").getJSONObject(0).put("enabled", "true") }.toString(),
            JSONObject(encoded).apply { getJSONArray("days").getJSONObject(0).getJSONArray("windows").getJSONObject(0).put("start", "1020") }.toString(),
        ).forEach { raw -> assertThrows(Exception::class.java) { decodeWeeklyAvailability(raw) } }
    }

    @Test fun `saving availability preserves accepted plan and subject bytes`() {
        val today = LocalDate.of(2026, 10, 18)
        val plan = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 60, today.plusDays(6), 3, 0)), List(7) { 60 }, 25))
        val before = PlannerSnapshot(plan, listOf(PlannerSubject(1, "Physics")))
        assertTrue(store.writeSnapshot(store.readData().snapshot, before))
        val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val planBytes = prefs.getString("plan", null)
        val subjectsBytes = prefs.getString("subjects", null)
        assertTrue(store.writeSnapshot(before, before.copy(availability = week)))
        assertEquals(planBytes, prefs.getString("plan", null))
        assertEquals(subjectsBytes, prefs.getString("subjects", null))
        assertEquals(week, StudyPlannerStore(context).readData().availability)
        assertEquals(week, store.readData().snapshot.withoutSubject(1).availability)
    }

    @Test fun `stale plan and availability saves cannot overwrite newly saved windows`() {
        val before = store.readData().snapshot
        val accepted = before.copy(availability = week)
        assertTrue(store.writeSnapshot(before, accepted))
        repeat(2) { assertFalse(store.writeSnapshot(before, before)) }
        assertEquals(accepted, store.readData().snapshot)
    }

    @Test fun `availability only saves leave literal catalog formatting untouched`() {
        val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val literal = "  {\"version\":1, \"subjects\":[]}\n"
        prefs.edit().putString("subjects", literal).commit()
        val before = store.readData().snapshot
        assertTrue(store.writeSnapshot(before, before.copy(availability = week)))
        assertEquals(literal, prefs.getString("subjects", null))
    }

    @Test fun `unreadable availability blocks writes and recovery preserves the other records`() {
        val before = PlannerSnapshot(subjects = listOf(PlannerSubject(1, "Physics")))
        assertTrue(store.writeSnapshot(store.readData().snapshot, before))
        val prefs = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("availability", "future-record").commit()
        assertNotNull(store.readData().availabilityError)
        assertFalse(store.writeSnapshot(store.readData().snapshot, before.copy(availability = week)))
        assertEquals("future-record", prefs.getString("availability", null))
        assertTrue(store.clearAvailability())
        assertEquals(before, store.readData().snapshot)
    }
}

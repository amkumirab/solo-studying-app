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
class PlannerSubjectsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val date = LocalDate.of(2026, 10, 17)
    private val subject = PlannerSubject(7, "Operating Systems", 5, SubjectColor.Cyan, 300, date.plusDays(60))
    private val plan = StudyPlan(PlannerConfig(date, listOf(
        PlannerCourse(7, 100, date.plusDays(6), 3, 123),
        PlannerCourse(8, 50, date.plusDays(6), 2, 999),
    ), List(7) { 60 }, 25), listOf(StudyBlock(9, 7, date, 25), StudyBlock(19, 8, date, 25)))

    @Before fun reset() { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `subject fields roundtrip and weekly preference is not total effort`() {
        assertEquals(listOf(subject), decodePlannerSubjects(encodePlannerSubjects(listOf(subject))))
        assertEquals(subject.copy(preferredWeeklyMinutes = null, deadline = null),
            decodePlannerSubjects(encodePlannerSubjects(listOf(subject.copy(preferredWeeklyMinutes = null, deadline = null)))).single())
        assertEquals(300, subject.preferredWeeklyMinutes)
        assertEquals(100, plan.config.courses.first().remainingMinutes)
    }

    @Test fun `subject validation bounds ids names priorities and weekly hours`() {
        listOf<() -> Any>(
            { subject.copy(bossId = 0) }, { subject.copy(name = " ") }, { subject.copy(name = "x".repeat(81)) },
            { subject.copy(name = "OS\nExam") }, { subject.copy(priority = 0) }, { subject.copy(priority = 6) },
            { subject.copy(preferredWeeklyMinutes = 0) }, { subject.copy(preferredWeeklyMinutes = 10081) },
            { validatePlannerSubjects(listOf(subject, subject)) },
            { validatePlannerSubjects(listOf(subject, subject.copy(bossId = 8, name = "operating  systems"))) },
        ).forEach { assertThrows(IllegalArgumentException::class.java) { it() } }
        (1..5).forEach { assertEquals(it, subject.copy(priority = it).priority) }
        assertEquals(10080, subject.copy(preferredWeeklyMinutes = 10080).preferredWeeklyMinutes)
    }

    @Test fun `plan v3 supports five priorities while legacy versions retain their limits`() {
        val high = plan.copy(config = plan.config.copy(courses = plan.config.courses.map { it.copy(priority = 5) }))
        assertEquals(3, JSONObject(encodeStudyPlan(high)).getInt("version"))
        assertEquals(high, decodeStudyPlan(encodeStudyPlan(high)))
        val v2 = JSONObject(encodeStudyPlan(high)).put("version", 2).toString()
        assertThrows(IllegalArgumentException::class.java) { decodeStudyPlan(v2) }
        val v1 = JSONObject(JSONObject(encodeStudyPlan(high)).getJSONObject("plan").toString()).put("version", 1).toString()
        assertThrows(IllegalArgumentException::class.java) { decodeStudyPlan(v1) }
    }

    @Test fun `catalog rejects unknown fields invalid types future versions and trailing text`() {
        val encoded = encodePlannerSubjects(listOf(subject))
        listOf(
            "not-json", "x".repeat(32769), encoded + "{}",
            JSONObject(encoded).put("version", 99).toString(),
            JSONObject(encoded).put("future", true).toString(),
            JSONObject(encoded).apply { getJSONArray("subjects").getJSONObject(0).put("priority", "5") }.toString(),
            JSONObject(encoded).apply { getJSONArray("subjects").getJSONObject(0).put("color", "Unknown") }.toString(),
            JSONObject(encoded).apply { getJSONArray("subjects").getJSONObject(0).put("deadline", "2026-02-30") }.toString(),
        ).forEach { raw -> assertThrows(Exception::class.java) { decodePlannerSubjects(raw) } }
    }

    @Test fun `catalog settings reopen and do not rewrite a saved plan`() {
        val store = StudyPlannerStore(context)
        assertTrue(store.write(plan))
        val preferences = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val original = preferences.getString("plan", null)
        val before = store.readData().snapshot
        assertTrue(store.writeSnapshot(before, before.copy(subjects = listOf(subject))))
        assertEquals(listOf(subject), StudyPlannerStore(context).readData().subjects)
        assertEquals(original, preferences.getString("plan", null))
    }

    @Test fun `subject deletion preserves surviving blocks and baselines and clears the last course`() {
        val before = PlannerSnapshot(plan, listOf(subject))
        val after = before.withoutSubject(7)
        assertTrue(after.subjects.isEmpty())
        assertEquals(listOf(19), after.plan!!.blocks.map { it.id })
        assertEquals(999L, after.plan.config.courses.single().baselineSeconds)
        assertNull(after.withoutSubject(8).plan)
    }

    @Test fun `atomic deletion cannot be undone by a stale editor`() {
        val store = StudyPlannerStore(context)
        val initial = store.readData().snapshot
        val accepted = PlannerSnapshot(plan, listOf(subject))
        assertTrue(store.writeSnapshot(initial, accepted))
        val removed = accepted.withoutSubject(7)
        assertTrue(store.writeSnapshot(accepted, removed))
        assertFalse(store.writeSnapshot(accepted, accepted))
        assertEquals(removed, StudyPlannerStore(context).readData().snapshot)
    }

    @Test fun `corrupt catalog is preserved and recovery does not remove the plan`() {
        val store = StudyPlannerStore(context)
        assertTrue(store.write(plan))
        val preferences = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString("subjects", "{\"version\":99}").commit()
        assertNotNull(store.readData().subjectsError)
        assertFalse(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(null, listOf(subject))))
        assertEquals("{\"version\":99}", preferences.getString("subjects", null))
        assertTrue(store.clearSubjects())
        assertEquals(plan, store.readData().plan)
        assertNull(store.readData().subjectsError)
    }
}

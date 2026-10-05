package com.amkumirab.solostudying.planner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudyPlannerStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val date = LocalDate.of(2026, 10, 5)
    private val plan = buildStudyPlan(PlannerConfig(date,
        listOf(PlannerCourse(1, 300, date.plusDays(6), 2, 120)), List(7) { 90 }, 25))
    @Before fun reset() { context.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `accepted plan survives reopening and can be cleared`() {
        assertNull(StudyPlannerStore(context).read().plan)
        assertTrue(StudyPlannerStore(context).write(plan))
        assertEquals(plan, StudyPlannerStore(context).read().plan)
        assertTrue(StudyPlannerStore(context).clear())
        assertNull(StudyPlannerStore(context).read().plan)
    }
    @Test fun `unsupported corrupt oversized and invalid records report errors`() {
        val preferences = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        listOf("not-json", "{\"version\":9}", "x".repeat(2_000_001),
            encodeStudyPlan(plan).replace("\"minutes\":25", "\"minutes\":-1"),
            encodeStudyPlan(plan).replace("\"bossId\":1", "\"bossId\":\"1\""),
        ).forEach { invalid ->
            preferences.edit().putString("plan", invalid).commit()
            val state = StudyPlannerStore(context).read()
            assertNull(state.plan)
            assertNotNull(state.error)
            assertEquals(invalid, preferences.getString("plan", null))
        }
    }
    @Test fun `moving a saved block keeps progress baseline and capacity`() {
        val block = plan.blocks.first()
        val moved = moveStudyBlock(plan, block.id, date.plusDays(1), date, 0)!!
        assertTrue(StudyPlannerStore(context).write(moved))
        assertEquals(moved, StudyPlannerStore(context).read().plan)
    }
}

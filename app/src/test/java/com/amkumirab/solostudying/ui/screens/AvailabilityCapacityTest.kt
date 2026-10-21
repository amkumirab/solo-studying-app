package com.amkumirab.solostudying.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AvailabilityCapacityTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 18)
    private val goal = BossEntity(id = 1, name = "Physics", difficulty = "Medium", requiredMinutes = 120)
    private val week = defaultWeeklyAvailability(listOf(60, 60, 60, 60, 60, 0, 0))

    @Test fun `wizard uses saved windows instead of legacy budgets and caps adjustments`() {
        var accepted: StudyPlan? = null
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(goal), today, null, List(7) { 480 }, false,
                { accepted = it }, {}, availability = week)
        } }
        rule.onNodeWithText("Physics").performClick()
        repeat(2) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.onNodeWithTag("availability_total").performScrollTo().assertTextEquals("Weekly capacity: 5 h")
        rule.onNodeWithTag("planner_capacity_0").assertDoesNotExist()
        repeat(2) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("budget_increase").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("budget_decrease").performScrollTo().performClick()
        rule.onNodeWithTag("budget_increase").performScrollTo().assertIsEnabled().performClick()
        rule.onNodeWithTag("budget_increase").assertIsNotEnabled()
        rule.runOnIdle { assertNull(accepted) }
        // Budget edits rebuild the preview off the main thread. Wait for its save control,
        // just as on first opening; slower CI runners may still be calculating here.
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(week.weekdayMinutes, accepted!!.config.weekdayMinutes)
            assertEquals(week, accepted!!.config.studyWindows)
            assertTrue(accepted!!.blocks.all { it.startMinute != null })
        }
    }

    @Test fun `all rest availability prevents generation`() {
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(goal), today, null, List(7) { 480 }, false,
                { fail("An all-rest week cannot generate a schedule") }, {},
                availability = week.copy(days = week.days.map { it.copy(enabled = false) }))
        } }
        rule.onNodeWithText("Physics").performClick()
        repeat(2) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.onNodeWithTag("planner_next").performScrollTo().assertIsNotEnabled()
    }

    @Test fun `capacity ceiling is per day not only a weekly total`() {
        val config = PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(28), 3, 0)), week.weekdayMinutes, 25)
        assertTrue(config.fitsAvailability(week))
        assertTrue(config.fitsAvailability(null))
        assertFalse(config.copy(weekdayMinutes = listOf(90, 30, 60, 60, 60, 0, 0)).fitsAvailability(week))
        assertFalse(config.copy(weekdayMinutes = listOf(30, 60, 60, 60, 60, 30, 0)).fitsAvailability(week))
    }

    @Test fun `uniform required budget cannot exceed a shorter saved weekday`() {
        val start = today.plusDays(1)
        val limited = defaultWeeklyAvailability(listOf(30, 120, 0, 0, 0, 0, 0))
        val config = PlannerConfig(start, listOf(PlannerCourse(1, 120, start.plusDays(1), 3, 0)), limited.weekdayMinutes, 25)
        assertEquals(listOf(60, 60, 0, 0, 0, 0, 0), requiredPlannerDailyTime(config)!!.weekdayMinutes)
        rule.setContent { SoloStudyingTheme {
            PlannerTimeBudgetCard(config, config, listOf(goal), 0, false, {},
                { fail("Uniform required time exceeds Monday's ceiling") }, {}, availability = limited)
        } }
        rule.onNodeWithTag("budget_use_required").assertIsNotEnabled()
    }
}

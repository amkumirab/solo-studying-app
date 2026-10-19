package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
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
class WeeklyStudyGoalUiTest {
    @get:Rule val rule = createComposeRule()
    private val monday = LocalDate.of(2026, 10, 19)
    private val boss = BossEntity(id = 1, name = "Physics", difficulty = "Medium", requiredMinutes = 600)
    private val config = PlannerConfig(monday, listOf(PlannerCourse(1, 600, monday.plusDays(13), 3, 0)), List(7) { 60 }, 30)

    @Test fun `weekly goal validates restores and is included in the accepted plan`() {
        var accepted: StudyPlan? = null
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(boss), monday, buildStudyPlan(config), List(7) { 60 }, false, { accepted = it }, {})
        } }
        repeat(2) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.onNodeWithTag("planner_weekly_goal").performScrollTo().performTextReplacement("57")
        rule.onNodeWithTag("planner_next").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("planner_weekly_goal").performScrollTo().performTextReplacement("4")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("planner_weekly_goal").performScrollTo().assertTextContains("4")
        repeat(2) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("weekly_recommended_total").performScrollTo().assertTextEquals("Recommended: 4 h")
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(240, accepted!!.config.weeklyGoalMinutes) }
    }

    @Test fun `recommendation changes with priorities while actual sessions stay distinct`() {
        val courses = listOf(config.courses.single().copy(priority = 1), config.courses.single().copy(bossId = 2, priority = 5))
        var draft by mutableStateOf(buildStudyPlan(config.copy(courses = courses, weeklyGoalMinutes = 240)))
        val goals = listOf(boss, boss.copy(id = 2, name = "Operating Systems"))
        rule.setContent { SoloStudyingTheme { WeeklyAllocationCard(draft, goals, emptyList()) } }
        rule.onNodeWithTag("weekly_share_1").assertTextContains("40 min", substring = true)
        rule.onNodeWithTag("weekly_share_2").assertTextContains("3 h 20 min", substring = true)
        rule.onNodeWithText("Recommendations are separate from the date-only schedule below. Saving keeps the displayed sessions.").assertExists()
        rule.runOnIdle { draft = draft.copy(config = draft.config.copy(courses = courses.map { it.copy(priority = 3) })) }
        rule.onNodeWithTag("weekly_share_1").assertTextContains("2 h", substring = true)
        rule.onNodeWithTag("weekly_share_2").assertTextContains("2 h", substring = true)
    }

    @Test fun `editing other answers preserves an exact saved minute despite rounded hours label`() {
        var accepted: StudyPlan? = null
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(boss), monday, buildStudyPlan(config.copy(weeklyGoalMinutes = 1)), List(7) { 60 }, false, { accepted = it }, {})
        } }
        repeat(4) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, accepted!!.config.weeklyGoalMinutes) }
    }

    @Test fun `zero target reports actual scheduled time without marking it as completed`() {
        val plan = buildStudyPlan(config.copy(weeklyGoalMinutes = 0))
        val scheduled = plan.blocks.filter { it.date <= monday.plusDays(6) }.sumOf { it.minutes }
        rule.setContent { SoloStudyingTheme { WeeklyGoalSummary(plan, monday) } }
        rule.onNodeWithTag("weekly_actual_total").assertTextEquals("Goal: 0 min · Scheduled: ${plannerTimeLabel(scheduled)}")
        rule.onNodeWithText("Scheduled above your goal: ${plannerTimeLabel(scheduled)}").assertExists()
        rule.onNodeWithText("Scheduled time is planned work, not completed study. Edit answers to review your recommended split.").assertExists()
    }
}

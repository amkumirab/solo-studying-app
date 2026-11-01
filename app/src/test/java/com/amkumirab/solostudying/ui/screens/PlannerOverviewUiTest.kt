package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
class PlannerOverviewUiTest {
    @get:Rule val rule = createComposeRule()
    private val monday = LocalDate.of(2026, 11, 2)
    private val plan = StudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 600, monday.plusDays(13), 5, 0)),
        List(7) { 60 }, 60, 120), listOf(StudyBlock(1, 1, monday, 60),
        StudyBlock(2, 1, monday.plusDays(1), 30), StudyBlock(3, 1, monday.plusWeeks(1), 60)))

    @Test fun `overview exposes exact values and scheduling progress rather than completion`() {
        rule.setContent { SoloStudyingTheme { Surface { Column(Modifier.verticalScroll(rememberScrollState())) {
            PlannerWeekOverviewContent(plan, monday)
        } } } }
        rule.onNodeWithTag("overview_goal").assertTextEquals("Weekly goal", "2 h")
        rule.onNodeWithTag("overview_planned").assertTextEquals("Planned focus", "1 h 30 min")
        rule.onNodeWithTag("overview_budget").assertTextEquals("Study budget", "7 h")
        rule.onNodeWithTag("overview_unallocated").assertTextEquals("Unallocated budget", "5 h 30 min")
        rule.onNodeWithTag("overview_feedback").assertTextEquals("30 min still to plan")
        rule.onNodeWithTag("overview_progress").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0.75f, 0f..1f)))
        rule.onNodeWithText("75% of goal planned").assertExists()
        rule.onNodeWithText("Planned time, not completed study.").assertExists()
    }

    @Test fun `no target zero target over-target and outside-plan states stay explicit`() {
        val current = mutableStateOf(plan.copy(config = plan.config.copy(weeklyGoalMinutes = null)))
        val week = mutableStateOf(monday)
        rule.setContent { SoloStudyingTheme { Surface { Column(Modifier.verticalScroll(rememberScrollState())) {
            PlannerWeekOverviewContent(current.value, week.value)
        } } } }
        rule.onNodeWithTag("overview_goal").assertTextEquals("Weekly goal", "Not set")
        rule.onNodeWithTag("overview_progress").assertDoesNotExist()
        rule.runOnIdle { current.value = StudyPlan(plan.config.copy(weeklyGoalMinutes = 0), emptyList()) }
        rule.onNodeWithTag("overview_feedback").assertTextEquals("No focus time requested")
        rule.onNodeWithTag("overview_progress").assertDoesNotExist()
        rule.runOnIdle { current.value = plan.copy(config = plan.config.copy(weeklyGoalMinutes = 0)) }
        rule.onNodeWithTag("overview_feedback").assertTextEquals("Above goal by 1 h 30 min")
        rule.runOnIdle { week.value = monday.minusWeeks(1) }
        rule.onNodeWithTag("overview_feedback").assertTextEquals("No active plan dates this week")
        rule.onNodeWithTag("overview_goal").assertTextEquals("Weekly goal", "Not applicable")
        rule.onNodeWithTag("overview_budget").assertTextEquals("Study budget", "0 min")
    }

    @Test fun `partial-week shortage is exact and does not silently reduce the configured target`() {
        val config = plan.config.copy(start = monday.plusDays(2), weeklyGoalMinutes = 240,
            courses = listOf(PlannerCourse(1, 300, monday.plusDays(4), 5, 0)))
        rule.setContent { SoloStudyingTheme { Surface { Column(Modifier.verticalScroll(rememberScrollState())) {
            PlannerWeekOverviewContent(StudyPlan(config, emptyList()), monday)
        } } } }
        rule.onNodeWithTag("overview_goal").assertTextEquals("Weekly goal", "4 h")
        rule.onNodeWithText("Partial week · full weekly goal").assertExists()
        rule.onNodeWithTag("overview_feedback").assertTextEquals("Goal exceeds study budget by 1 h")
        rule.onNodeWithText("4 h still to plan").assertExists()
    }

    @Test fun `overview reacts to week and accepted-plan changes with exact target semantics`() {
        val current = mutableStateOf(plan)
        val week = mutableStateOf(monday)
        rule.setContent { SoloStudyingTheme { Surface { Column(Modifier.verticalScroll(rememberScrollState())) {
            PlannerWeekOverviewContent(current.value, week.value)
        } } } }
        rule.runOnIdle { week.value = monday.plusWeeks(1) }
        rule.onNodeWithTag("overview_planned").assertTextEquals("Planned focus", "1 h")
        rule.onNodeWithTag("overview_progress").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0.5f, 0f..1f)))
        rule.runOnIdle { current.value = plan.copy(config = plan.config.copy(weeklyGoalMinutes = 60)) }
        rule.onNodeWithTag("overview_feedback").assertTextEquals("Weekly goal fully planned")
        rule.onNodeWithTag("overview_progress").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(1f, 0f..1f)))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun `accounting disclosure restores and can be activated with a keyboard`() {
        val restore = StateRestorationTester(rule)
        restore.setContent { SoloStudyingTheme { Surface { Column(Modifier.verticalScroll(rememberScrollState())) {
            PlannerWeekOverviewContent(plan, monday)
        } } } }
        val details = rule.onNodeWithTag("overview_details")
        details.performScrollTo().assertHeightIsAtLeast(48.dp)
        details.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        details.performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithText("Unallocated budget is not a guaranteed free slot.", substring = true).performScrollTo().assertIsDisplayed()
        restore.emulateSavedInstanceStateRestore()
        details.performScrollTo().assertTextEquals("Hide planning details")
        details.performClick().assertTextEquals("Planning details")
        rule.onAllNodesWithText("Unallocated budget is not a guaranteed free slot.", substring = true).assertCountEquals(0)
    }

    @Test fun `partial calendar day budgets agree with the overview and exclude out-of-plan dates`() {
        val config = plan.config.copy(start = monday.plusDays(2),
            courses = listOf(PlannerCourse(1, 300, monday.plusDays(4), 5, 0)))
        rule.setContent { SoloStudyingTheme { Surface {
            StudyPlannerWeek(StudyPlan(config, emptyList()), emptyList(), monday, false, { _, _ -> }, {})
        } } }
        rule.onNodeWithTag("overview_budget").assertTextEquals("Study budget", "3 h")
        val list = rule.onNode(hasScrollToNodeAction())
        (0L..6L).forEach { offset ->
            val date = monday.plusDays(offset)
            val tag = "planner_day_budget_$date"
            list.performScrollToNode(hasTestTag(tag))
            rule.onNodeWithTag(tag).assertTextEquals("0 / ${if (offset in 2L..4L) 60 else 0} planned focus minutes")
        }
    }
}

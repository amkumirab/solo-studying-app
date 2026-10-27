package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.mutableStateOf
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
class SessionSubjectUiTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 27)
    private val goals = listOf(1, 2).map { BossEntity(id = it, name = "Course $it", difficulty = "Hard", requiredMinutes = 120) }
    private fun plan(): StudyPlan {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
        return StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(6), 5, 0), PlannerCourse(2, 120, today.plusDays(6), 3, 0)), List(7) { 120 }, 30, studyWindows = windows),
            listOf(StudyBlock(7, 1, today, 30, 1020)))
    }
    private fun action() = rule.onNodeWithTag("planner_subject_7")
    private fun scrollAction() { rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_subject_7")) }
    private fun choose() {
        rule.onNodeWithTag("session_subject_picker").performScrollTo().performClick()
        rule.onNodeWithTag("session_subject_option_2").performClick()
    }
    private fun awaitPreview() { rule.waitUntil(10000) { rule.onAllNodesWithTag("subject_destination_unscheduled").fetchSemanticsNodes().isNotEmpty() } }

    @Test fun `restored subject draft cancels without saving and confirmation updates study course`() {
        val restore = StateRestorationTester(rule)
        val state = mutableStateOf(plan())
        var saved = 0
        var started: Pair<Int, Int>? = null
        restore.setContent { SoloStudyingTheme { StudyPlannerWeek(state.value, goals, today, false,
            { boss, minutes -> started = boss.id to minutes }, { saved++; state.value = it }) } }
        scrollAction(); action().performClick()
        rule.onNodeWithTag("confirm_session_subject").assertIsNotEnabled()
        choose(); awaitPreview()
        restore.emulateSavedInstanceStateRestore()
        awaitPreview()
        rule.onNodeWithTag("session_subject_picker").performScrollTo().assertTextContains("Course 2", substring = true)
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals(0, saved); assertEquals(plan(), state.value) }
        scrollAction(); action().performClick(); choose(); awaitPreview()
        rule.onNodeWithTag("confirm_session_subject").performClick()
        rule.runOnIdle { assertEquals(1, saved) }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_start_8"))
        rule.onNodeWithTag("planner_start_8").performClick()
        rule.runOnIdle { assertEquals(2 to 30, started) }
    }

    @Test fun `busy and progress changes invalidate the open preview and block saving`() {
        val busy = mutableStateOf(false)
        val liveGoals = mutableStateOf(goals)
        var saved = false
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(plan(), liveGoals.value, today, busy.value, { _, _ -> }, { saved = true }) } }
        scrollAction(); action().performClick(); choose(); awaitPreview()
        rule.runOnIdle { busy.value = true }
        rule.onNodeWithTag("confirm_session_subject").assertIsNotEnabled()
        rule.runOnIdle { busy.value = false; liveGoals.value = goals.map { if (it.id == 1) it.copy(timeSpentSeconds = 1) else it } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("session_subject_error").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_session_subject").assertIsNotEnabled()
        rule.runOnIdle { assertFalse(saved) }
    }

    @Test fun `weekly cap explains the rejected preview without saving`() {
        val capped = plan().copy(config = plan().config.copy(weeklyGoalMinutes = 120, weeklyScheduling = WeeklySchedulingPolicy(mapOf(2 to 15))))
        var saved = false
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(capped, goals, today, false, { _, _ -> }, { saved = true }) } }
        scrollAction(); action().performClick(); choose()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("session_subject_error").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("session_subject_error").performScrollTo().assertTextContains("weekly limit", substring = true)
        rule.onNodeWithTag("confirm_session_subject").assertIsNotEnabled()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertFalse(saved) }
    }

    @Test fun `locked past and missing sessions cannot open the editor`() {
        val state = mutableStateOf(plan().copy(blocks = plan().blocks.map { it.copy(locked = true) }))
        val day = mutableStateOf(today)
        val liveGoals = mutableStateOf(goals)
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(state.value, liveGoals.value, day.value, false, { _, _ -> }, {}) } }
        scrollAction(); action().assertIsNotEnabled()
        rule.runOnIdle { state.value = plan(); day.value = today.plusDays(1) }
        scrollAction(); action().assertIsNotEnabled()
        rule.runOnIdle { day.value = today; liveGoals.value = goals.filter { it.id == 2 } }
        scrollAction(); action().assertIsNotEnabled()
    }

    @Test fun `date only plans have no subject action`() {
        val dated = StudyPlan(plan().config.copy(studyWindows = null), listOf(StudyBlock(7, 1, today, 30)))
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(dated, goals, today, false, { _, _ -> }, {}) } }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_move_7"))
        action().assertDoesNotExist()
    }
}

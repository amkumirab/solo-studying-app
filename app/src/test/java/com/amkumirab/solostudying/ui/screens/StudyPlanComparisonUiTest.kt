package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class StudyPlanComparisonUiTest {
    @get:Rule val rule = createComposeRule()
    private val monday = LocalDate.of(2026, 11, 2)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 60)
    private val windows = defaultWeeklyAvailability(List(7) { 60 })
    private val old = StudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 60, monday.plusDays(13), 3, 0)),
        List(7) { 60 }, 60, 120, studyWindows = windows), listOf(StudyBlock(1, 1, monday, 60, 1020, manual = true)))

    @Test fun `comparison shows exact selected week totals shortfalls and restorable session details`() {
        val next = old.copy(blocks = listOf(StudyBlock(99, 1, monday.plusWeeks(1), 45, 1020)))
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlanComparisonContent(compareStudyPlans(old, next), listOf(boss), emptyList(), monday)
        } } }
        rule.onNodeWithTag("comparison_accepted_focus").assertTextEquals("Accepted focus: 1 h")
        rule.onNodeWithTag("comparison_proposed_focus").assertTextEquals("Proposed remaining focus: 0 min")
        rule.onNodeWithTag("comparison_shortfall_1").performScrollTo().assertTextContains("15 min", substring = true)
        rule.onNodeWithTag("comparison_manual_warning").performScrollTo().assertTextContains("1 manual", substring = true)
        rule.onNodeWithTag("comparison_details").performScrollTo().performClick()
        rule.onNodeWithTag("comparison_change_0").performScrollTo().assertTextContains("Replanned", substring = true)
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("comparison_details").performScrollTo().assertTextEquals("Hide session changes")
        rule.onNodeWithTag("comparison_next_week").performScrollTo().performClick()
        rule.onNodeWithTag("comparison_proposed_focus").assertTextEquals("Proposed remaining focus: 45 min")
    }

    @Test fun `rebuild is read only until explicit confirmation and busy state prevents applying`() {
        val snapshot = PlannerSnapshot(old, availability = windows)
        var busy by mutableStateOf(false)
        var accepted: StudyPlan? = null
        rule.setContent { SoloStudyingTheme {
            StudyRebuildReview(snapshot, listOf(boss), monday, busy, {}, {}, { plan, expected ->
                assertEquals(snapshot, expected); accepted = plan
            })
        } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("rebuild_comparison_ready").fetchSemanticsNodes().isNotEmpty() }
        assertNull(accepted)
        rule.runOnIdle { busy = true }
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsNotEnabled()
        rule.runOnIdle { busy = false }
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsEnabled().performClick()
        assertEquals(60, accepted!!.blocks.sumOf { it.minutes })
    }

    @Test fun `reactive conflicts and incomplete rebuilds cannot be applied`() {
        var snapshot by mutableStateOf(PlannerSnapshot(old.copy(blocks = old.blocks.map { it.copy(locked = true) }), availability = windows))
        rule.setContent { SoloStudyingTheme {
            StudyRebuildReview(snapshot, listOf(boss), monday, false, {}, {}, { _, _ -> fail("Invalid proposal applied") })
        } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("rebuild_comparison_ready").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { snapshot = snapshot.copy(availability = WeeklyAvailability(List(7) {
            StudyDayAvailability(true, listOf(StudyTimeWindow(540, 600))) })) }
        rule.waitUntil(10000) { rule.onAllNodesWithText("Locked sessions conflict", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsNotEnabled()
        rule.runOnIdle { snapshot = PlannerSnapshot(old.copy(config = old.config.copy(weeklyGoalMinutes = 0), blocks = emptyList()), availability = windows) }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("comparison_shortfall_1").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsNotEnabled()
        rule.onNodeWithTag("confirm_rebuild_comparison").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag("rebuild_edit_answers").performScrollTo().assertIsEnabled()
    }

    @Test fun `session details expose every changed edit flag even when timing is unchanged`() {
        val edited = old.copy(blocks = old.blocks.map { it.copy(durationEdited = true) })
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlanComparisonContent(compareStudyPlans(edited, old), listOf(boss), emptyList(), monday)
        } } }
        rule.onNodeWithTag("comparison_details").performScrollTo().performClick()
        rule.onNodeWithTag("comparison_change_0").performScrollTo().assertTextContains("Edited duration", substring = true)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun `long changes are paged and keyboard disclosure and page selection restore`() {
        val config = old.config.copy(courses = listOf(old.config.courses.single().copy(remainingMinutes = 840)),
            sessionMinutes = 30, weeklyGoalMinutes = null)
        val accepted = buildStudyPlan(config)
        val proposed = buildStudyPlan(config.copy(studyWindows = WeeklyAvailability(List(7) {
            StudyDayAvailability(true, listOf(StudyTimeWindow(540, 600))) })))
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlanComparisonContent(compareStudyPlans(accepted, proposed), listOf(boss), emptyList(), monday)
        } } }
        val disclosure = rule.onNodeWithTag("comparison_details")
        disclosure.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        disclosure.performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("comparison_change_19").performScrollTo().assertExists()
        rule.onNodeWithTag("comparison_change_20").assertDoesNotExist()
        rule.onNodeWithTag("comparison_next_page").performScrollTo().performClick()
        rule.onNodeWithTag("comparison_change_0").assertDoesNotExist()
        rule.onNodeWithTag("comparison_change_27").performScrollTo().assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("comparison_change_20").performScrollTo().assertExists()
        rule.onNodeWithTag("comparison_change_0").assertDoesNotExist()
    }

    @Test fun `active study and unreadable settings block applying without blocking cancellation`() {
        var active by mutableStateOf(true)
        var blocked by mutableStateOf<String?>(null)
        rule.setContent { SoloStudyingTheme {
            StudyRebuildReview(PlannerSnapshot(old, availability = windows), listOf(boss), monday, false, {}, {},
                { _, _ -> fail("Blocked proposal applied") }, blockedMessage = blocked, isSessionActive = active)
        } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("rebuild_comparison_ready").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsNotEnabled().assertTextEquals("Confirm")
        rule.onNodeWithTag("confirm_rebuild_comparison").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag("cancel_rebuild_comparison").assertIsEnabled()
        rule.runOnIdle { active = false }
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsEnabled()
        rule.runOnIdle { blocked = "Unreadable subject settings" }
        rule.waitUntil(10000) { rule.onAllNodesWithText("Unreadable subject settings").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsNotEnabled()
    }
}

package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
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
class StudyDurationUiTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 19)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 90)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
    private fun plan() = StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 90, today.plusDays(6), 5, 0)), List(7) { 120 }, 30, studyWindows = windows),
        listOf(StudyBlock(7, 1, today, 30, 1020), StudyBlock(20, 1, today.plusDays(1), 30, 1020), StudyBlock(99, 1, today.plusDays(2), 30, 1020)))

    @Test fun `weekly duration action previews transfers and saves only after confirmation`() {
        var current by mutableStateOf(plan())
        var saves = 0
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(current, listOf(boss), today, false, { _, _ -> }, { current = it; saves++ }) } }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_duration_7"))
        rule.onNodeWithTag("planner_duration_7").performClick()
        rule.onNodeWithTag("confirm_duration_edit").assertIsNotEnabled()
        rule.onNodeWithTag("duration_minutes").performTextReplacement("45")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("duration_donor_99").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("duration_donor_99").performScrollTo().assertTextContains("30 → 15 min", substring = true)
        assertEquals(0, saves)
        rule.onNodeWithTag("confirm_duration_edit").assertIsEnabled().performClick()
        assertEquals(1, saves)
        assertEquals(45, current.blocks.first().minutes)
        assertEquals(15, current.blocks.last().minutes)
    }

    @Test fun `shortening previews unscheduled work and cancellation after restoration never saves`() {
        val restore = StateRestorationTester(rule)
        var cancelled = 0
        restore.setContent { SoloStudyingTheme { TimedStudyDurationDialog(plan(), listOf(boss), today, 7, false,
            { cancelled++ }, { fail("Cancelled draft saved") }) } }
        rule.onNodeWithTag("duration_minus").performScrollTo().performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("duration_unscheduled").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("duration_unscheduled").performScrollTo().assertTextContains("15 min", substring = true)
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("duration_minutes").performScrollTo().assertTextContains("15")
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(1, cancelled)
    }

    @Test fun `invalid numbers locked and active sessions cannot save`() {
        var current by mutableStateOf(plan())
        var busy by mutableStateOf(false)
        rule.setContent { SoloStudyingTheme { TimedStudyDurationDialog(current, listOf(boss), today, 7, busy, {}, { fail("Invalid draft saved") }) } }
        listOf("", "0", "121", "abc", "1.5").forEach { text ->
            rule.onNodeWithTag("duration_minutes").performTextReplacement(text)
            rule.onNodeWithTag("confirm_duration_edit").assertIsNotEnabled()
        }
        rule.onNodeWithTag("duration_minutes").performTextReplacement("45")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("duration_donor_99").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { busy = true }
        rule.onNodeWithTag("confirm_duration_edit").assertIsNotEnabled()
        rule.runOnIdle { busy = false; current = current.copy(blocks = current.blocks.map { if (it.id == 7) it.copy(locked = true) else it }) }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("duration_error").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_duration_edit").assertIsNotEnabled()
    }

    @Test fun `new recorded progress invalidates a previously valid preview`() {
        var progress by mutableLongStateOf(0L)
        rule.setContent { SoloStudyingTheme { TimedStudyDurationDialog(plan(), listOf(boss.copy(timeSpentSeconds = progress)), today, 7, false,
            {}, { fail("Credited session resized") }) } }
        rule.onNodeWithTag("duration_minutes").performTextReplacement("45")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("duration_donor_99").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_duration_edit").assertIsEnabled()
        rule.runOnIdle { progress = 1 }
        rule.onNodeWithTag("confirm_duration_edit").assertIsNotEnabled()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("duration_error").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun `preview explains fully removed donors and current week totals`() {
        var saved: StudyPlan? = null
        rule.setContent { SoloStudyingTheme { TimedStudyDurationDialog(plan(), listOf(boss), today, 7, false, {}, { saved = it }) } }
        rule.onNodeWithTag("duration_minutes").performTextReplacement("90")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("duration_donor_99").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("duration_donor_99").performScrollTo().assertTextContains("Removed · 30 min transferred", substring = true)
        rule.onNodeWithTag("duration_donor_20").performScrollTo().assertTextContains("Removed", substring = true)
        rule.onNodeWithTag("duration_week_total").performScrollTo().assertTextContains("90 → 90 planned min", substring = true)
        rule.onNodeWithTag("confirm_duration_edit").performClick()
        assertEquals(90, saved!!.blocks.single().minutes)
    }
}

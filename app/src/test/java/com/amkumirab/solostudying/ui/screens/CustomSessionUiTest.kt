package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import android.app.DatePickerDialog
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
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomSessionUiTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 26)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 120)
    private fun plan(): StudyPlan {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
        return StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(6), 5, 0)), List(7) { 120 }, 30, studyWindows = windows),
            listOf(StudyBlock(7, 1, today, 30, 1020)))
    }

    @Test fun `add previews the full slot and unscheduled minutes then saves only on confirmation`() {
        var current by mutableStateOf(plan())
        var saves = 0
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(current, listOf(boss), today, false, { _, _ -> }, { current = it; saves++ }) } }
        rule.onNodeWithTag("planner_add_session").performScrollTo().performClick()
        rule.onNodeWithTag("custom_start_time").performScrollTo().performTextReplacement("17:30")
        rule.onNodeWithTag("custom_minutes").performScrollTo().performTextReplacement("45")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("custom_new_slot").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("custom_new_slot").performScrollTo().assertTextContains("17:30–18:15", substring = true)
        rule.onNodeWithTag("custom_unscheduled").performScrollTo().assertTextContains("90 → 45", substring = true)
        assertEquals(0, saves)
        rule.onNodeWithTag("confirm_custom_session").performClick()
        assertEquals(1, saves)
        assertEquals(45, current.blocks.last().minutes)
        assertTrue(current.blocks.last().manual)
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("manual_session_8"))
        rule.onNodeWithTag("manual_session_8").assertTextEquals("Manual session")
    }

    @Test fun `removal preview can cancel and requires confirmation to return work`() {
        var current by mutableStateOf(plan())
        var saves = 0
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(current, listOf(boss), today, false, { _, _ -> }, { current = it; saves++ }) } }
        fun open() {
            rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_remove_7"))
            rule.onNodeWithTag("planner_remove_7").performClick()
            rule.waitUntil(10000) { rule.onAllNodesWithTag("remove_unscheduled").fetchSemanticsNodes().isNotEmpty() }
        }
        open()
        rule.onNodeWithTag("remove_unscheduled").performScrollTo().assertTextContains("90 → 120", substring = true)
        assertEquals(0, saves)
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(plan(), current)
        open()
        rule.onNodeWithTag("confirm_remove_session").performClick()
        assertEquals(1, saves)
        assertTrue(current.blocks.isEmpty())
        assertEquals(120, current.shortfalls[1])
    }

    @Test fun `legacy date-only plans offer neither add nor individual removal`() {
        val dated = plan().copy(config = plan().config.copy(studyWindows = null), blocks = plan().blocks.map { it.copy(startMinute = null) })
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(dated, listOf(boss), today, false, { _, _ -> }, {}) } }
        rule.onNodeWithTag("planner_add_session").assertDoesNotExist()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_move_7"))
        rule.onNodeWithTag("planner_remove_7").assertDoesNotExist()
    }

    @Test fun `custom draft restores exact inputs and cancellation saves nothing`() {
        val restore = StateRestorationTester(rule)
        var dismissed = 0
        restore.setContent { SoloStudyingTheme { CustomStudySessionDialog(plan(), listOf(boss), today, today, false, { dismissed++ }, { fail("Cancelled draft saved") }) } }
        rule.onNodeWithTag("custom_start_time").performScrollTo().performTextReplacement("17:40")
        rule.onNodeWithTag("custom_minutes").performScrollTo().performTextReplacement("20")
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("custom_start_time").performScrollTo().assertTextContains("17:40")
        rule.onNodeWithTag("custom_minutes").performScrollTo().assertTextContains("20")
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(1, dismissed)
    }

    @Test fun `invalid inputs full workload and busy state cannot save`() {
        var current by mutableStateOf(plan())
        var busy by mutableStateOf(false)
        rule.setContent { SoloStudyingTheme { CustomStudySessionDialog(current, listOf(boss), today, today, busy, {}, { fail("Invalid draft saved") }) } }
        listOf("", "0", "121", "abc", "1.5").forEach { value ->
            rule.onNodeWithTag("custom_minutes").performScrollTo().performTextReplacement(value)
            rule.onNodeWithTag("confirm_custom_session").assertIsNotEnabled()
        }
        rule.onNodeWithTag("custom_minutes").performScrollTo().performTextReplacement("30")
        rule.onNodeWithTag("custom_start_time").performScrollTo().performTextReplacement("24:00")
        rule.onNodeWithTag("confirm_custom_session").assertIsNotEnabled()
        rule.onNodeWithTag("custom_start_time").performScrollTo().performTextReplacement("17:30")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("custom_new_slot").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { busy = true }
        rule.onNodeWithTag("confirm_custom_session").assertIsNotEnabled()
        rule.runOnIdle { busy = false; current = current.copy(config = current.config.copy(courses = listOf(current.config.courses.single().copy(remainingMinutes = 30)))) }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("custom_error").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("custom_error").performScrollTo().assertTextContains("Not enough unscheduled work", substring = true)
        rule.onNodeWithTag("confirm_custom_session").assertIsNotEnabled()
    }

    @Test fun `course selection and date picker create only the requested course slot`() {
        val other = boss.copy(id = 2, name = "Physics")
        val input = plan().copy(config = plan().config.copy(courses = plan().config.courses + PlannerCourse(2, 60, today.plusDays(3), 3, 0)))
        var saved: StudyPlan? = null
        rule.setContent { SoloStudyingTheme { CustomStudySessionDialog(input, listOf(boss, other), today, today, false, {}, { saved = it }) } }
        rule.onNodeWithTag("custom_course").performScrollTo().performClick()
        rule.onNodeWithTag("custom_course_2").performClick()
        rule.onNodeWithTag("custom_date").performScrollTo().performClick()
        rule.runOnIdle {
            val picker = ShadowDialog.getLatestDialog() as DatePickerDialog
            val date = today.plusDays(1)
            picker.datePicker.updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
            picker.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick()
        }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("custom_new_slot").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_custom_session").performClick()
        assertEquals(2, saved!!.blocks.last().bossId)
        assertEquals(today.plusDays(1), saved!!.blocks.last().date)
        assertEquals(input.blocks, saved!!.blocks.dropLast(1))
    }

    @Test fun `new recorded progress invalidates removal preview and add cannot absorb unallocated credit`() {
        var seconds by mutableLongStateOf(0)
        var removing by mutableStateOf(true)
        rule.setContent { SoloStudyingTheme {
            val goals = listOf(boss.copy(timeSpentSeconds = seconds))
            if (removing) RemoveStudySessionDialog(plan(), goals, today, 7, false, {}, { fail("Studied session removed") })
            else CustomStudySessionDialog(plan(), goals, today, today, false, {}, { fail("Recorded credit absorbed") })
        } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("remove_unscheduled").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_remove_session").assertIsEnabled()
        rule.runOnIdle { seconds = 1 }
        rule.onNodeWithTag("confirm_remove_session").assertIsNotEnabled()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("remove_session_error").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { seconds = 1801; removing = false }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("custom_error").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("custom_error").performScrollTo().assertTextContains("recorded study credit", substring = true)
        rule.onNodeWithTag("confirm_custom_session").assertIsNotEnabled()
    }

    @Test fun `locked and active sessions disable removal even with an open preview`() {
        var current by mutableStateOf(plan())
        var busy by mutableStateOf(false)
        rule.setContent { SoloStudyingTheme { RemoveStudySessionDialog(current, listOf(boss), today, 7, busy, {}, { fail("Unavailable removal saved") }) } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("remove_unscheduled").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { busy = true }
        rule.onNodeWithTag("confirm_remove_session").assertIsNotEnabled()
        rule.runOnIdle { busy = false; current = current.copy(blocks = current.blocks.map { it.copy(locked = true) }) }
        rule.onNodeWithTag("confirm_remove_session").assertIsNotEnabled()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("remove_session_error").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("remove_session_error").performScrollTo().assertTextContains("Unlock", substring = true)
    }
}

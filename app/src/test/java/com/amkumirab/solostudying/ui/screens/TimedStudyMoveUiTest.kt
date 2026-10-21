package com.amkumirab.solostudying.ui.screens

import android.app.DatePickerDialog
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
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimedStudyMoveUiTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 19)
    private val bosses = listOf(BossEntity(id = 1, name = "Physics", difficulty = "Hard", requiredMinutes = 90))
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
    private val plan = StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 90, today.plusDays(6), 5, 0)), List(7) { 120 }, 30, studyWindows = windows),
        listOf(StudyBlock(7, 1, today, 30, 1020), StudyBlock(19, 1, today, 30, 1050)))

    @Test fun `move editor rejects collision then saves an exact free time`() {
        var saved: StudyPlan? = null
        rule.setContent { SoloStudyingTheme { TimedStudyMoveDialog(plan, bosses, today, 7, false, {}, { saved = it }) } }
        rule.onNodeWithTag("move_start_time").performTextReplacement("17:45")
        rule.onNodeWithTag("confirm_timed_move").assertIsNotEnabled()
        rule.onNodeWithTag("move_start_time").performTextReplacement("18:00")
        rule.onNodeWithTag("confirm_timed_move").assertIsEnabled().performClick()
        assertEquals(1080, saved!!.blocks.first().startMinute)
        assertEquals(plan.blocks.last(), saved!!.blocks.last())
        assertEquals(listOf(7, 19), saved!!.blocks.map { it.id })
    }

    @Test fun `edited time survives recreation and cancellation never saves`() {
        val restoration = StateRestorationTester(rule)
        var cancelled = 0
        restoration.setContent { SoloStudyingTheme { TimedStudyMoveDialog(plan, bosses, today, 7, false, { cancelled++ }, { fail("Cancelled draft was saved") }) } }
        rule.onNodeWithTag("move_start_time").performTextReplacement("18:15")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("move_start_time").assertTextContains("18:15")
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(1, cancelled)
        assertEquals(1020, plan.blocks.first().startMinute)
    }

    @Test fun `invalid clocks cannot be saved`() {
        rule.setContent { SoloStudyingTheme { TimedStudyMoveDialog(plan, bosses, today, 7, false, {}, { fail("Invalid time was saved") }) } }
        rule.onNodeWithTag("move_start_time").performTextReplacement("24:00")
        rule.onNodeWithTag("confirm_timed_move").assertIsNotEnabled()
    }

    @Test fun `active or partly studied sessions cannot be moved`() {
        var busy by mutableStateOf(true)
        var credit by mutableLongStateOf(0L)
        rule.setContent { SoloStudyingTheme { TimedStudyMoveDialog(plan, listOf(bosses.single().copy(timeSpentSeconds = credit)), today, 7, busy, {}, { fail("Unavailable slot was moved") }) } }
        rule.onNodeWithTag("confirm_timed_move").assertIsNotEnabled()
        rule.runOnIdle { busy = false; credit = 1 }
        rule.onNodeWithTag("confirm_timed_move").assertIsNotEnabled()
        rule.runOnIdle { credit = 0 }
        rule.onNodeWithTag("confirm_timed_move").assertIsEnabled()
    }

    @Test fun `date picker moves the session to another day without changing its duration`() {
        var saved: StudyPlan? = null
        rule.setContent { SoloStudyingTheme { TimedStudyMoveDialog(plan, bosses, today, 7, false, {}, { saved = it }) } }
        rule.onNodeWithTag("move_study_date").performClick()
        rule.runOnIdle {
            val picker = ShadowDialog.getLatestDialog() as DatePickerDialog
            val date = today.plusDays(1)
            picker.datePicker.updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
            picker.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick()
        }
        rule.onNodeWithTag("confirm_timed_move").performClick()
        assertEquals(today.plusDays(1), saved!!.blocks.first().date)
        assertEquals(30, saved!!.blocks.first().minutes)
        assertEquals(7, saved!!.blocks.first().id)
    }

    @Test fun `week move action opens the explicit time editor`() {
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(plan, bosses, today, false, { _, _ -> }, {}) } }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_move_7"))
        rule.onNodeWithTag("planner_move_7").performClick()
        rule.onNodeWithTag("move_start_time").assertIsDisplayed().assertTextContains("17:00")
    }
}

package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.*
import android.content.Context
import android.app.DatePickerDialog
import android.app.Application
import android.content.DialogInterface
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import com.amkumirab.solostudying.planner.StudyPlannerStore
import com.amkumirab.solostudying.ui.viewmodel.StudyPlannerViewModel
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudyPlannerUiTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 5)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 6000, deadlineDate = today.plusDays(6).toString())
    @Before fun resetStore() { ApplicationProvider.getApplicationContext<Context>().deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanupStore() { resetStore() }

    @Test fun `guided questions validate hours retain draft and save a feasible plan`() {
        var accepted: StudyPlan? = null
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            SoloStudyingTheme { StudyPlannerWizard(listOf(boss), today, null, List(7) { 120 }, false, { accepted = it }, {}) }
        }
        rule.onNodeWithTag("planner_next").assertIsNotEnabled()
        rule.onNodeWithText("Operating Systems").performClick()
        rule.onNodeWithTag("planner_next").performClick()
        rule.onNodeWithTag("planner_hours").performTextReplacement("0")
        rule.onNodeWithTag("planner_next").assertIsNotEnabled()
        rule.onNodeWithTag("planner_hours").performTextReplacement("5")
        rule.onNodeWithText("Finish by: ${today.plusDays(6)}").performScrollTo().performClick()
        val openPicker = ShadowDialog.getLatestDialog()
        restoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle { assertFalse(openPicker.isShowing) }
        rule.onNodeWithTag("planner_hours").assertTextContains("5")
        rule.onNodeWithTag("planner_next").performScrollTo().performClick()
        rule.onNodeWithTag("planner_capacity_0").performScrollTo().performTextReplacement("481")
        rule.onNodeWithTag("planner_next").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("planner_capacity_0").performScrollTo().performTextReplacement("120")
        rule.onNodeWithTag("planner_next").performScrollTo().performClick()
        rule.onNodeWithText("45 min").performClick()
        rule.onNodeWithTag("planner_next").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(300, accepted!!.config.courses.single().remainingMinutes)
            assertEquals(45, accepted!!.config.sessionMinutes)
            assertTrue(accepted!!.shortfalls.isEmpty())
        }
    }

    @Test fun `infeasible plan requires explicit acknowledgement`() {
        val plan = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today, 2, 0)), List(7) { 30 }, 25))
        rule.setContent { SoloStudyingTheme { StudyPlannerPreview(plan, listOf(boss), false, {}) } }
        rule.onNodeWithText("Not enough study time").assertExists()
        rule.onNodeWithTag("planner_accept").assertIsNotEnabled()
        rule.onNodeWithTag("planner_acknowledge").performClick()
        rule.onNodeWithTag("planner_accept").assertIsEnabled()
    }

    @Test fun `weekly session starts selected course without marking it done`() {
        val plan = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 25, today, 2, 0)), List(7) { 60 }, 25))
        var started: Pair<Int, Int>? = null
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWeek(plan, listOf(boss), today, false, { goal, minutes -> started = goal.id to minutes }, {})
        } }
        rule.onNodeWithTag("planner_start_1").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1 to 25, started) }
        rule.onNodeWithText("25 min left").assertExists()
    }

    @Test fun `another active timer disables launch`() {
        val plan = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 25, today, 2, 0)), List(7) { 60 }, 25))
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(plan, listOf(boss), today, true, { _, _ -> fail() }, {}) } }
        rule.onNodeWithTag("planner_start_1").performScrollTo().assertIsNotEnabled()
    }

    @Test fun `entry opens a persisted plan with the default lifecycle owner`() {
        val plan = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 25, today, 2, 0)), List(7) { 60 }, 25))
        StudyPlannerStore(ApplicationProvider.getApplicationContext()).write(plan)
        rule.setContent { SoloStudyingTheme { StudyPlannerEntry(listOf(boss), today, List(7) { 60 }, false, { _, _ -> }) } }
        rule.waitUntil(10000) { rule.onAllNodesWithText("Open weekly plan").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Weekly study plan").assertExists()
        rule.onNodeWithText("Close planner").performClick()
        rule.onNodeWithTag("open_study_planner").assertExists()
    }

    @Test fun `unreadable saved plan requires confirmation before clearing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString("plan", "invalid-record").commit()
        // Corruption is detected on a fresh launch, not by an already-loaded screen.
        val coldStart = StudyPlannerViewModel(ApplicationProvider.getApplicationContext<Application>())
        rule.setContent { SoloStudyingTheme { StudyPlannerEntry(listOf(boss), today, List(7) { 60 }, false, { _, _ -> }, plannerViewModel = coldStart) } }
        rule.waitUntil(10000) { rule.onAllNodesWithText("Plan exam preparation").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("Clear saved plan").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Clear saved plan").performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals("invalid-record", preferences.getString("plan", null)) }
        rule.waitUntil(10000) { rule.onAllNodesWithText("Clear saved plan").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Clear saved plan").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("Which courses are you preparing for?").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { assertNull(preferences.getString("plan", null)) }
    }

    @Test fun `empty goals cannot continue and expired deadlines require correction`() {
        var goals by mutableStateOf(emptyList<BossEntity>())
        rule.setContent { SoloStudyingTheme { StudyPlannerWizard(goals, today, null, List(7) { 60 }, false, {}, {}) } }
        rule.onNodeWithTag("planner_next").assertIsNotEnabled()
        rule.onNodeWithText("Create a course goal in Study Hub first, then return here.").assertExists()
        rule.runOnIdle { goals = listOf(boss.copy(deadlineDate = today.minusDays(1).toString())) }
        rule.onNodeWithText(boss.name).performClick()
        rule.onNodeWithTag("planner_next").performClick()
        rule.onNodeWithTag("planner_next").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Choose a finish date between today and one year ahead.").assertExists()
        rule.onNodeWithText("Finish by: ${today.minusDays(1)}").performScrollTo().performClick()
        rule.runOnIdle {
            val picker = ShadowDialog.getLatestDialog() as DatePickerDialog
            picker.datePicker.updateDate(today.year, today.monthValue - 1, today.dayOfMonth)
            picker.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        }
        rule.onNodeWithTag("planner_next").performScrollTo().assertIsEnabled()
    }

    @Test fun `completed and removed goals cannot start planned sessions`() {
        val plan = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 25, today, 2, 0)), List(7) { 60 }, 25))
        var goals by mutableStateOf(listOf(boss.copy(isCompleted = true)))
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(plan, goals, today, false, { _, _ -> fail() }, {}) } }
        rule.onNodeWithText("All planned courses are complete.").assertExists()
        rule.onNodeWithTag("planner_start_1").assertDoesNotExist()
        rule.runOnIdle { goals = emptyList() }
        rule.onNodeWithText("1 courses were removed. Edit answers to update this plan.").assertExists()
        rule.onNodeWithTag("planner_start_1").assertDoesNotExist()
    }

    @Test fun `moving to a rest day reports a warning and preserves the plan`() {
        val plan = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 25, today.plusDays(2), 2, 0)), listOf(60, 0, 60, 0, 0, 0, 0), 25))
        var moved: StudyPlan? = null
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(plan, listOf(boss), today, false, { _, _ -> }, { moved = it }) } }
        rule.onNodeWithTag("planner_move_1").performScrollTo().performClick()
        rule.runOnIdle {
            val date = today.plusDays(1)
            val picker = ShadowDialog.getLatestDialog() as DatePickerDialog
            picker.datePicker.updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
            picker.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        }
        rule.onNodeWithText("That day is unavailable, full, or this block has already been studied.").performScrollTo().assertExists()
        rule.runOnIdle { assertNull(moved) }
    }

    @Test fun `large goals are not silently capped to the planner limit`() {
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(boss.copy(requiredMinutes = 120000)), today, null, List(7) { 60 }, false, {}, {})
        } }
        rule.onNodeWithText(boss.name).performClick()
        rule.onNodeWithTag("planner_next").performClick()
        rule.onNodeWithTag("planner_hours").assertTextContains("2000")
        rule.onNodeWithTag("planner_next").performScrollTo().assertIsNotEnabled()
    }
}

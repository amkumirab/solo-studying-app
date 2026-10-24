package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.planner.StudyPlannerStore
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import com.amkumirab.solostudying.ui.viewmodel.StudyPlannerViewModel
import java.time.LocalDate
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionLocksUiTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val today = LocalDate.of(2026, 10, 19)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 60)
    private fun windows(start: Int) = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(start, start + 60))) })
    private fun plan(locked: Boolean = true) = StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 60, today.plusDays(6), 5, 0)),
        List(7) { 60 }, 30, studyWindows = windows(1020)), listOf(StudyBlock(7, 1, today, 30, 1035, locked)))
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `weekly lock control preserves slot disables move and unlocks without blocking study`() {
        var accepted by mutableStateOf(plan(false))
        var started = 0
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(accepted, listOf(boss), today, false,
            { _, _ -> started++ }, { accepted = it }) } }
        val list = rule.onNode(hasScrollToNodeAction())
        list.performScrollToNode(hasTestTag("planner_lock_7"))
        rule.onNodeWithTag("planner_lock_7").assertTextContains("Lock session").performClick()
        rule.onNodeWithTag("planner_lock_7").assertTextContains("Unlock session")
        list.performScrollToNode(hasTestTag("planner_move_7"))
        rule.onNodeWithTag("planner_move_7").assertIsNotEnabled()
        rule.onNodeWithTag("planner_start_7").performClick()
        assertEquals(1, started)
        list.performScrollToNode(hasTestTag("planner_lock_7"))
        rule.onNodeWithTag("planner_lock_7").performClick()
        assertEquals(plan(false), accepted)
    }

    @Test fun `busy state disables locking and studied locks can still be unlocked`() {
        var busy by mutableStateOf(true)
        var accepted by mutableStateOf(plan())
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(accepted, listOf(boss.copy(timeSpentSeconds = 1800)), today, busy,
            { _, _ -> }, { accepted = it }) } }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_lock_7"))
        rule.onNodeWithTag("planner_lock_7").assertIsNotEnabled()
        rule.runOnIdle { busy = false }
        rule.onNodeWithTag("planner_lock_7").assertIsEnabled().performClick()
        assertFalse(accepted.blocks.single().locked)
    }

    @Test fun `cancelled rebuild keeps exact bytes and confirmed rebuild preserves lock`() {
        val store = StudyPlannerStore(app)
        val original = plan()
        assertTrue(store.write(original))
        val before = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, 0).getString("plan", null)
        val model = StudyPlannerViewModel(app)
        rule.setContent { SoloStudyingTheme { StudyPlannerEntry(listOf(boss), today, List(7) { 60 }, false,
            { _, _ -> }, plannerViewModel = model, showDailySessions = false) } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(before, app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, 0).getString("plan", null))
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.readData().plan!!.blocks.sumOf { it.minutes } == 60 }
        assertEquals(original.blocks.single(), store.readData().plan!!.blocks.single { it.locked })
    }

    @Test fun `incompatible windows reject rebuild and answer preview without dropping locks`() {
        val store = StudyPlannerStore(app)
        val original = plan()
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(original, emptyList(), windows(540))))
        val model = StudyPlannerViewModel(app)
        rule.setContent { SoloStudyingTheme { StudyPlannerEntry(listOf(boss), today, List(7) { 60 }, false,
            { _, _ -> }, plannerViewModel = model, showDailySessions = false) } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("Locked sessions conflict", substring = true).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(original, store.readData().plan)
        rule.onNodeWithText("Edit answers").performClick()
        repeat(4) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_lock_conflict").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("planner_lock_conflict").performScrollTo().assertTextContains("Locked sessions conflict", substring = true)
        rule.onNodeWithTag("planner_accept").assertDoesNotExist()
        assertEquals(original, store.readData().plan)
    }

    @Test fun `compatible answer preview saves the original locked slot`() {
        val original = plan()
        var accepted: StudyPlan? = null
        rule.setContent { SoloStudyingTheme { StudyPlannerWizard(listOf(boss), today, original, List(7) { 60 }, false,
            { accepted = it }, {}, availability = windows(1020)) } }
        repeat(4) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        assertEquals(original.blocks.single(), accepted!!.blocks.single { it.locked })
        assertEquals(60, accepted!!.blocks.sumOf { it.minutes })
    }

    @Test fun `postponement explains locks and the move editor cannot bypass them`() {
        var moving by mutableStateOf(false)
        rule.setContent { SoloStudyingTheme {
            if (moving) TimedStudyMoveDialog(plan(), listOf(boss), today, 7, false, {}, { fail("Locked slot moved") })
            else StudyPostponeDialog(plan(), listOf(boss), today, false, null, {}, { fail("Locked slot postponed") })
        } }
        rule.waitUntil(10000) { rule.onAllNodesWithText("Unlock today's fixed sessions first").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_postpone_today").assertIsNotEnabled()
        rule.runOnIdle { moving = true }
        rule.onNodeWithTag("confirm_timed_move").assertIsNotEnabled()
        rule.onNodeWithText("Unlock this session in the weekly plan before moving it.").assertExists()
    }
}

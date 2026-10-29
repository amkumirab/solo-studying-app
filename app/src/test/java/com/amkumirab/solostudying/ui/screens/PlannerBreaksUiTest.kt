package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
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
class PlannerBreaksUiTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val today = LocalDate.of(2026, 10, 26)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 120)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 660))) })
    private fun plan() = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today, 5, 0)), List(7) { 120 }, 60, studyWindows = windows))
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }
    private fun next() = rule.onNodeWithTag("planner_next").performScrollTo().performClick()
    private fun awaitPreview() = rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }

    @Test fun `restored break draft previews real shortage and saves only after acknowledgement`() {
        val store = StudyPlannerStore(app)
        val original = plan()
        assertTrue(store.write(original))
        val restore = StateRestorationTester(rule)
        val busy = mutableStateOf(false)
        restore.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(boss), today, original, List(7) { 120 }, busy.value,
                { assertTrue(store.write(it)) }, {}, availability = windows)
        } }
        repeat(3) { next() }
        rule.onNodeWithTag("planner_break_minutes").performScrollTo().performTextReplacement("31")
        rule.onNodeWithTag("planner_next").assertIsNotEnabled()
        rule.onNodeWithTag("planner_break_minutes").performTextReplacement("10")
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("planner_break_minutes").performScrollTo().assertTextContains("10")
        rule.runOnIdle { busy.value = true }
        rule.onNodeWithTag("planner_break_minutes").assertIsNotEnabled()
        rule.onNodeWithTag("planner_next").assertIsNotEnabled()
        rule.runOnIdle { busy.value = false }
        next(); awaitPreview()
        rule.onNodeWithTag("preview_break_1").performScrollTo().assertTextContains("10:00–10:10", substring = true)
        rule.onNodeWithTag("planner_accept").performScrollTo().assertIsNotEnabled()
        assertEquals(original, store.readData().plan)
        rule.onNodeWithTag("planner_acknowledge").performScrollTo().performClick()
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        rule.runOnIdle {
            val accepted = StudyPlannerStore(app).readData().plan!!
            assertEquals(10, accepted.config.breakMinutes)
            assertEquals(110, accepted.blocks.sumOf { it.minutes })
            assertEquals(10, accepted.shortfalls[1])
            assertEquals(0L, boss.timeSpentSeconds)
        }
    }

    @Test fun `incompatible locked spacing rejects preview without replacing saved plan`() {
        val original = plan().copy(blocks = plan().blocks.map { it.copy(locked = true) })
        val store = StudyPlannerStore(app)
        assertTrue(store.write(original))
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(boss), today, original, List(7) { 120 }, false, { store.write(it) }, {})
        } }
        repeat(3) { next() }
        rule.onNodeWithTag("planner_break_minutes").performScrollTo().performTextReplacement("10")
        next()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_lock_conflict").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("planner_lock_conflict").performScrollTo().assertTextContains("Locked sessions conflict", substring = true)
        rule.onNodeWithTag("planner_accept").assertDoesNotExist()
        assertEquals(original, store.readData().plan)
    }

    @Test fun `date only wizard keeps breaks off and explains the window requirement`() {
        val original = buildStudyPlan(plan().config.copy(studyWindows = null))
        var accepted: StudyPlan? = null
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(boss), today, original, List(7) { 120 }, false, { accepted = it }, {})
        } }
        repeat(3) { next() }
        rule.onNodeWithTag("planner_break_minutes").assertDoesNotExist()
        rule.onNodeWithTag("planner_break_windows_required").performScrollTo().assertIsDisplayed()
        next(); awaitPreview()
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        assertEquals(0, accepted!!.config.breakMinutes)
        assertNull(accepted!!.config.studyWindows)
    }

    @Test fun `weekly view labels break interval separately without a trailing break`() {
        val accepted = buildStudyPlan(plan().config.copy(breakMinutes = 10))
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(accepted, listOf(boss), today, false, { _, _ -> }, {}) } }
        val list = rule.onNode(hasScrollToNodeAction())
        list.performScrollToNode(hasTestTag("week_break_1"))
        rule.onNodeWithTag("week_break_1").assertTextContains("10:00–10:10", substring = true)
        rule.onNodeWithTag("week_break_2").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("break_day_$today"))
        rule.onNodeWithTag("break_day_$today").assertTextContains("10 min", substring = true)
        assertEquals(110, accepted.blocks.sumOf { it.minutes })
    }

    @Test fun `confirmed remaining rebuild retains breaks and rebases recorded study without loss`() {
        val config = plan().config.copy(courses = listOf(plan().config.courses.single().copy(finishBy = today.plusDays(6))), breakMinutes = 10)
        val original = buildStudyPlan(config)
        val store = StudyPlannerStore(app)
        assertTrue(store.write(original))
        val model = StudyPlannerViewModel(app)
        rule.setContent { SoloStudyingTheme { StudyPlannerEntry(listOf(boss.copy(timeSpentSeconds = 60)), today, List(7) { 120 }, false,
            { _, _ -> }, plannerViewModel = model, showDailySessions = false) } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(original, store.readData().plan)
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.readData().plan?.config?.courses?.single()?.baselineSeconds == 60L }
        val rebuilt = store.readData().plan!!
        assertEquals(10, rebuilt.config.breakMinutes)
        assertEquals(119, rebuilt.blocks.sumOf { it.minutes })
        assertEquals(windows, rebuilt.config.studyWindows)
    }

    @Test fun `custom session suggestion leaves the accepted minimum gap`() {
        val source = StudyPlan(plan().config.copy(breakMinutes = 10), listOf(StudyBlock(1, 1, today, 30, 540)))
        var saved: StudyPlan? = null
        rule.setContent { SoloStudyingTheme {
            CustomStudySessionDialog(source, listOf(boss), today, today, false, {}, { saved = it })
        } }
        rule.onNodeWithTag("custom_start_time").performScrollTo().assertTextContains("09:40")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("custom_new_slot").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_custom_session").performClick()
        assertEquals(580, saved!!.blocks.last().startMinute)
        assertEquals(source.blocks.single(), saved!!.blocks.first())
    }

    @Test fun `cancelling edited answers does not save or move existing sessions`() {
        val original = plan()
        val store = StudyPlannerStore(app)
        assertTrue(store.write(original))
        var cancelled = false
        var accepted = false
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(boss), today, original, List(7) { 120 }, false,
                { accepted = true }, { cancelled = true })
        } }
        repeat(3) { next() }
        rule.onNodeWithTag("planner_break_minutes").performScrollTo().performTextReplacement("10")
        repeat(3) { rule.onNodeWithText("Back").performScrollTo().performClick() }
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        assertTrue(cancelled)
        assertFalse(accepted)
        assertEquals(original, store.readData().plan)
    }
}

package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
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
class StudyPlanComparisonEntryTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val monday = LocalDate.of(2026, 11, 2)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 120)
    private val windows = defaultWeeklyAvailability(List(7) { 120 })
    private val old = StudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 120, monday.plusDays(13), 3, 0)),
        List(7) { 120 }, 60, 120, studyWindows = windows).withWeeklyScheduling(emptyList()),
        listOf(StudyBlock(1, 1, monday, 60, 1020), StudyBlock(2, 1, monday.plusDays(1), 60, 1020, locked = true)))
    private val store get() = StudyPlannerStore(app)
    private val preferences get() = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, 0)
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    private fun open(): StudyPlannerViewModel {
        val model = StudyPlannerViewModel(app)
        rule.setContent { SoloStudyingTheme {
            StudyPlannerEntry(listOf(boss), monday, List(7) { 120 }, false,
                { _, _ -> fail("Comparison must not start study") }, plannerViewModel = model, showDailySessions = false)
        } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        return model
    }
    private fun ready() = rule.waitUntil(10000) {
        rule.onAllNodesWithTag("rebuild_comparison_ready").fetchSemanticsNodes().isNotEmpty()
    }

    @Test fun `real entry cancellation preserves bytes and confirm saves the displayed locked proposal`() {
        val subject = PlannerSubject(1, "Systems revision", 5, preferredWeeklyMinutes = 120, deadline = monday.plusDays(13))
        val expected = PlannerSnapshot(old, listOf(subject), windows)
        assertTrue(store.writeSnapshot(store.readData().snapshot, expected))
        val before = preferences.all.toMap()
        val model = open()
        ready()
        rule.onNodeWithTag("comparison_proposed_focus").assertTextEquals("Proposed remaining focus: 2 h")
        assertEquals(before, preferences.all)
        rule.onNodeWithTag("cancel_rebuild_comparison").performClick()
        assertEquals(before, preferences.all)
        rule.onNodeWithText("Rebuild remaining").performClick()
        ready()
        val displayed = previewStudyRebuild(expected, listOf(boss), monday).plan!!
        rule.onNodeWithTag("confirm_rebuild_comparison").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.plan == displayed && !model.state.value.busy }
        assertEquals(displayed, store.readData().plan)
        assertEquals(old.blocks.single { it.locked }, displayed.blocks.single { it.locked })
        assertEquals(listOf(subject), store.readData().subjects)
        assertEquals(windows, store.readData().availability)
        assertEquals(displayed, StudyPlannerStore(app).readData().plan)
    }

    @Test fun `concurrent saved setting change rejects the displayed proposal without clobbering it`() {
        val expected = PlannerSnapshot(old, availability = windows)
        assertTrue(store.writeSnapshot(store.readData().snapshot, expected))
        val model = open()
        ready()
        val newer = expected.copy(subjects = listOf(PlannerSubject(1, "New subject settings", 5)))
        assertTrue(store.writeSnapshot(expected, newer))
        val before = preferences.all.toMap()
        rule.onNodeWithTag("confirm_rebuild_comparison").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.error != null && !model.state.value.busy }
        assertEquals(before, preferences.all)
        assertEquals(newer, store.readData().snapshot)
        assertEquals(old, model.state.value.plan)
    }

    @Test fun `edited answers show comparison before saving and cancelling keeps accepted bytes`() {
        assertTrue(store.write(old))
        val before = preferences.all.toMap()
        open()
        ready()
        rule.onNodeWithTag("rebuild_edit_answers").performScrollTo().performClick()
        repeat(2) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.onNodeWithTag("planner_weekly_goal").performScrollTo().performTextReplacement("3")
        repeat(2) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("study_plan_comparison").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(before, preferences.all)
        rule.onNodeWithText("Back").performScrollTo().performClick()
        repeat(3) { rule.onNodeWithText("Back").performScrollTo().performClick() }
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        assertEquals(before, preferences.all)
    }

    @Test fun `restored review reloads storage and recomputes changed recorded progress before confirmation`() {
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(old, availability = windows)))
        val before = preferences.all.toMap()
        var goals by mutableStateOf(listOf(boss))
        var model: StudyPlannerViewModel? = null
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme {
            val current = remember { StudyPlannerViewModel(app) }
            model = current
            StudyPlannerEntry(goals, monday, List(7) { 120 }, false, { _, _ -> },
                plannerViewModel = current, showDailySessions = false)
        } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model?.state?.value?.loaded == true }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        ready()
        rule.runOnIdle { goals = listOf(boss.copy(timeSpentSeconds = 60)) }
        rule.waitUntil(10000) { rule.onAllNodesWithText("Proposed remaining focus: 1 h 59 min").fetchSemanticsNodes().isNotEmpty() }
        val previous = model
        restoration.emulateSavedInstanceStateRestore()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model !== previous && model?.state?.value?.loaded == true }
        ready()
        rule.onNodeWithTag("comparison_proposed_focus").assertTextEquals("Proposed remaining focus: 1 h 59 min")
        assertEquals(before, preferences.all)
        rule.onNodeWithTag("confirm_rebuild_comparison").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.readData().plan!!.config.courses.single().baselineSeconds == 60L }
        assertEquals(119, store.readData().plan!!.blocks.sumOf { it.minutes })
        assertEquals(old.blocks.single { it.locked }, store.readData().plan!!.blocks.single { it.locked })
    }

    @Test fun `unreadable saved subjects cannot be used to replace accepted sessions`() {
        assertTrue(store.write(old))
        assertTrue(preferences.edit().putString("subjects", "future-subject-schema").commit())
        val before = preferences.all.toMap()
        open()
        ready()
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsNotEnabled()
        rule.onNodeWithTag("rebuild_comparison_error").assertTextContains("subjects could not be read", substring = true)
        assertEquals(before, preferences.all)
    }

    @Test fun `unreadable availability recovery link leaves review without replacing the plan`() {
        assertTrue(store.write(old))
        assertTrue(preferences.edit().putString("availability", "future-availability-schema").commit())
        val before = preferences.all.toMap()
        open()
        ready()
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsNotEnabled()
        rule.onNodeWithText("Review unreadable availability settings").performClick()
        rule.onNodeWithText("Reset saved availability").assertExists()
        assertEquals(before, preferences.all)
    }
}

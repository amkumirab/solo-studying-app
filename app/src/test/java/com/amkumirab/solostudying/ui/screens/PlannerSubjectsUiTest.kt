package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlannerSubjectsUiTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val today = LocalDate.of(2026, 10, 17)
    private val goals = listOf(
        BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 6000, deadlineDate = today.plusDays(60).toString()),
        BossEntity(id = 2, name = "Physics", difficulty = "Medium", requiredMinutes = 3000),
    )
    private val store get() = StudyPlannerStore(app)
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    private fun open(restoration: StateRestorationTester? = null) {
        val model = StudyPlannerViewModel(app)
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            SoloStudyingTheme { StudyPlannerEntry(goals, today, List(7) { 60 }, false, { _, _ -> }, plannerViewModel = model, showDailySessions = false) }
        }
        if (restoration == null) rule.setContent(content) else restoration.setContent(content)
        rule.waitUntil(10000) { model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("Subjects").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Subjects", useUnmergedTree = true).performClick()
    }

    @Test fun `create a goal linked subject with five priorities color and weekly preference`() {
        open()
        rule.onNodeWithText("Add subject").performClick()
        rule.onNodeWithTag("subject_goal_1").performClick()
        rule.onNodeWithTag("subject_name").performTextReplacement("OS exam")
        rule.onNodeWithTag("subject_weekly_hours").performScrollTo().performTextReplacement("5")
        rule.onNodeWithTag("subject_priority_5").performScrollTo().performClick()
        rule.onNodeWithTag("subject_color_Green").performScrollTo().performClick()
        rule.onNodeWithTag("save_subject").performScrollTo().performClick()
        rule.waitUntil(10000) { store.readData().subjects.isNotEmpty() }
        val saved = store.readData().subjects.single()
        assertEquals(1, saved.bossId)
        assertEquals("OS exam", saved.name)
        assertEquals(5, saved.priority)
        assertEquals(SubjectColor.Green, saved.color)
        assertEquals(300, saved.preferredWeeklyMinutes)
        assertEquals("Operating Systems", goals.first().name)
    }

    @Test fun `edit draft survives restoration and clearing a deadline stays cleared`() {
        val subject = PlannerSubject(1, "OS exam", 4, SubjectColor.Rose, 1, today.plusDays(60))
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(subjects = listOf(subject))))
        val restoration = StateRestorationTester(rule)
        val subjects = mutableStateOf(listOf(subject))
        // Dialog owns an Android saved-state registry outside StateRestorationTester's registry.
        // Host the screen directly to exercise its saveable editor and draft state.
        restoration.setContent {
            SoloStudyingTheme {
                PlannerSubjectsScreen(subjects.value, goals, today, false, null, false,
                    onSave = { updated, saved ->
                        val before = store.readData().snapshot
                        assertTrue(store.writeSnapshot(before, before.copy(subjects = listOf(updated))))
                        subjects.value = listOf(updated)
                        saved()
                    }, onRemove = { _, _ -> fail("Unexpected removal") },
                    onReset = { fail("Unexpected reset") })
            }
        }
        rule.onNodeWithTag("edit_subject_1").performScrollTo().performClick()
        rule.onNodeWithTag("subject_name").performTextReplacement("OS revision")
        rule.onNodeWithText("Clear deadline").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("subject_name").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("subject_name").assertTextContains("OS revision")
        rule.onNodeWithText("No deadline").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("save_subject").performScrollTo().performClick()
        rule.waitUntil(10000) { store.readData().subjects.single().name == "OS revision" }
        assertNull(store.readData().subjects.single().deadline)
        assertEquals(1, store.readData().subjects.single().preferredWeeklyMinutes)
    }

    @Test fun `duplicate names cannot be saved`() {
        assertTrue(store.writeSnapshot(store.readData().snapshot,
            PlannerSnapshot(subjects = listOf(PlannerSubject(1, "OS exam")))))
        open()
        rule.onNodeWithText("Add subject").performClick()
        rule.onNodeWithTag("subject_goal_2").performClick()
        rule.onNodeWithTag("subject_name").performTextReplacement("os EXAM")
        rule.onNodeWithTag("save_subject").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("A subject with this name already exists.").performScrollTo().assertIsDisplayed()
        assertEquals(1, store.readData().subjects.size)
    }

    @Test fun `confirmed removal deletes only related planned blocks and keeps other credit ids`() {
        val plan = StudyPlan(PlannerConfig(today, listOf(
            PlannerCourse(1, 100, today.plusDays(6), 3, 120),
            PlannerCourse(2, 100, today.plusDays(6), 2, 300),
        ), List(7) { 60 }, 25), listOf(StudyBlock(7, 1, today, 25), StudyBlock(19, 2, today, 25)))
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(plan, listOf(PlannerSubject(1, "OS exam")))))
        open()
        rule.onNodeWithTag("remove_subject_1").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(plan, store.readData().plan)
        rule.onNodeWithTag("remove_subject_1").performScrollTo().performClick()
        rule.onNodeWithTag("confirm_remove_subject").performClick()
        rule.waitUntil(10000) { store.readData().subjects.isEmpty() }
        assertEquals(listOf(19), store.readData().plan!!.blocks.map { it.id })
        assertEquals(300L, store.readData().plan!!.config.courses.single().baselineSeconds)
    }

    @Test fun `retrying a stale plan save never restores a removed subject`() {
        val plan = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 60, today.plusDays(6), 3, 0)), List(7) { 60 }, 25))
        val before = PlannerSnapshot(plan, listOf(PlannerSubject(1, "OS")))
        assertTrue(store.writeSnapshot(store.readData().snapshot, before))
        val model = StudyPlannerViewModel(app)
        rule.setContent { }
        rule.waitUntil(10000) { model.state.value.loaded }
        assertTrue(store.writeSnapshot(before, before.withoutSubject(1)))
        var saves = 0
        repeat(2) {
            rule.runOnIdle { model.save(plan, before) { saves++ } }
            rule.waitUntil(10000) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                !model.state.value.busy
            }
        }
        assertEquals(0, saves)
        assertNull(store.readData().plan)
    }

    @Test fun `confirmed rebuild applies catalog settings without silently moving the saved plan`() {
        val plan = buildStudyPlan(PlannerConfig(today,
            listOf(PlannerCourse(1, 60, today.plusDays(6), 2, 0)), List(7) { 60 }, 25))
        val subject = PlannerSubject(1, "OS exam", 5, deadline = today.plusDays(14))
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(plan, listOf(subject))))
        open()
        assertEquals(plan, store.readData().plan)
        rule.onNodeWithText("Schedule", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(plan, store.readData().plan)
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.waitUntil(10000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            store.readData().plan!!.config.courses.single().priority == 5
        }
        assertEquals(subject.deadline, store.readData().plan!!.config.courses.single().finishBy)
        assertEquals(60, store.readData().plan!!.config.courses.single().remainingMinutes)
    }

    @Test fun `unreadable catalog reset is confirmed and preserves the accepted plan`() {
        val plan = buildStudyPlan(PlannerConfig(today,
            listOf(PlannerCourse(1, 60, today.plusDays(6), 2, 0)), List(7) { 60 }, 25))
        assertTrue(store.write(plan))
        val preferences = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString("subjects", "future-record").commit()
        open()
        rule.onNodeWithText("Reset saved subjects").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals("future-record", preferences.getString("subjects", null))
        rule.onNodeWithText("Reset saved subjects").performClick()
        rule.onNodeWithText("Reset subjects").performClick()
        rule.waitUntil(10000) { store.readData().subjectsError == null }
        assertEquals(plan, store.readData().plan)
    }
}

package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.os.Looper
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
class SessionSubjectEntryTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val today = LocalDate.of(2026, 10, 27)
    private val goals = listOf(1, 2).map { BossEntity(id = it, name = "Course $it", difficulty = "Hard", requiredMinutes = 120) }
    private fun plan(): StudyPlan {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
        return StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(6), 5, 0), PlannerCourse(2, 120, today.plusDays(6), 3, 0)), List(7) { 120 }, 30, studyWindows = windows),
            listOf(StudyBlock(7, 1, today, 30, 1020)))
    }
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }
    private fun open(model: StudyPlannerViewModel) {
        rule.setContent { SoloStudyingTheme { StudyPlannerEntry(goals, today, List(7) { 120 }, false,
            { _, _ -> }, plannerViewModel = model, showDailySessions = false) } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_subject_7"))
        rule.onNodeWithTag("planner_subject_7").performClick()
        rule.onNodeWithTag("session_subject_picker").performClick()
        rule.onNodeWithTag("session_subject_option_2").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("subject_destination_unscheduled").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun `subject preview writes nothing until confirmation and new subject survives restart`() {
        val store = StudyPlannerStore(app)
        assertTrue(store.write(plan()))
        val model = StudyPlannerViewModel(app)
        open(model)
        assertEquals(plan(), store.read().plan)
        rule.onNodeWithTag("subject_source_unscheduled").assertTextContains("90 → 120 min", substring = true)
        rule.onNodeWithTag("subject_destination_unscheduled").assertTextContains("120 → 90 min", substring = true)
        rule.onNodeWithTag("confirm_session_subject").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.read().plan!!.blocks.single().bossId == 2 }
        val restored = StudyPlannerStore(app).read().plan!!
        assertEquals(plan().config, restored.config)
        assertEquals(plan().blocks.single().copy(id = 8, bossId = 2, manual = true), restored.blocks.single())
    }

    @Test fun `a concurrently saved lock defeats an outdated subject preview`() {
        val store = StudyPlannerStore(app)
        assertTrue(store.write(plan()))
        val before = store.readData().snapshot
        val model = StudyPlannerViewModel(app)
        open(model)
        val locked = plan().copy(blocks = plan().blocks.map { it.copy(locked = true) })
        assertTrue(store.writeSnapshot(before, before.copy(plan = locked)))
        rule.onNodeWithTag("confirm_session_subject").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.editorGeneration > 0 }
        assertEquals(locked, store.read().plan)
        assertNotNull(model.state.value.error)
    }
}

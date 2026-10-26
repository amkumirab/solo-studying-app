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
class CustomSessionEntryTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val today = LocalDate.of(2026, 10, 26)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 120)
    private fun plan(): StudyPlan {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
        return StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(6), 5, 0)), List(7) { 120 }, 30, studyWindows = windows),
            listOf(StudyBlock(7, 1, today, 30, 1020)))
    }
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }
    private fun open(model: StudyPlannerViewModel) {
        rule.setContent { SoloStudyingTheme { StudyPlannerEntry(listOf(boss), today, List(7) { 120 }, false,
            { _, _ -> }, plannerViewModel = model, showDailySessions = false) } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
    }
    private fun openAdd() {
        rule.onNodeWithTag("planner_add_session").performScrollTo().performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("custom_new_slot").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun openRemove() {
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_remove_7"))
        rule.onNodeWithTag("planner_remove_7").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("remove_unscheduled").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun `confirmed add survives real save and a new store read`() {
        val store = StudyPlannerStore(app)
        assertTrue(store.write(plan()))
        val model = StudyPlannerViewModel(app)
        open(model); openAdd()
        assertEquals(plan(), store.read().plan)
        rule.onNodeWithTag("confirm_custom_session").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.read().plan!!.blocks.size == 2 }
        val restored = StudyPlannerStore(app).read().plan!!
        assertTrue(restored.blocks.last().manual)
        assertEquals(60, restored.shortfalls[1])
        assertEquals(plan().blocks.single(), restored.blocks.first())
    }

    @Test fun `confirmed removal persists an empty plan without deleting the course`() {
        val store = StudyPlannerStore(app)
        assertTrue(store.write(plan()))
        val model = StudyPlannerViewModel(app)
        open(model); openRemove()
        rule.onNodeWithTag("confirm_remove_session").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.read().plan!!.blocks.isEmpty() }
        val restored = StudyPlannerStore(app).read().plan!!
        assertEquals(plan().config, restored.config)
        assertEquals(120, restored.shortfalls[1])
    }

    @Test fun `concurrent lock save rejects an outdated removal preview`() {
        val store = StudyPlannerStore(app)
        assertTrue(store.write(plan()))
        val before = store.readData().snapshot
        val model = StudyPlannerViewModel(app)
        open(model); openRemove()
        val locked = plan().copy(blocks = plan().blocks.map { it.copy(locked = true) })
        assertTrue(store.writeSnapshot(before, before.copy(plan = locked)))
        rule.onNodeWithTag("confirm_remove_session").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.editorGeneration > 0 }
        assertEquals(locked, store.read().plan)
        assertNotNull(model.state.value.error)
    }

    @Test fun `concurrent saved plan defeats an outdated add preview`() {
        val store = StudyPlannerStore(app)
        assertTrue(store.write(plan()))
        val before = store.readData().snapshot
        val model = StudyPlannerViewModel(app)
        open(model); openAdd()
        val locked = plan().copy(blocks = plan().blocks.map { it.copy(locked = true) })
        assertTrue(store.writeSnapshot(before, before.copy(plan = locked)))
        rule.onNodeWithTag("confirm_custom_session").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.editorGeneration > 0 }
        assertEquals(locked, store.read().plan)
        assertNotNull(model.state.value.error)
    }
}

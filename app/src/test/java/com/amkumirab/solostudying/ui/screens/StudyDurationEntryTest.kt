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
class StudyDurationEntryTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val today = LocalDate.of(2026, 10, 19)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 90)
    private fun plan(): StudyPlan {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
        return StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 90, today.plusDays(6), 5, 0)), List(7) { 120 }, 30, studyWindows = windows),
            listOf(StudyBlock(7, 1, today, 30, 1020), StudyBlock(20, 1, today.plusDays(1), 30, 1020), StudyBlock(99, 1, today.plusDays(2), 30, 1020)))
    }
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }
    private fun open(model: StudyPlannerViewModel) {
        rule.setContent { SoloStudyingTheme { StudyPlannerEntry(listOf(boss), today, List(7) { 120 }, false,
            { _, _ -> }, plannerViewModel = model, showDailySessions = false) } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_duration_7"))
        rule.onNodeWithTag("planner_duration_7").performClick()
        rule.onNodeWithTag("duration_minutes").performTextReplacement("45")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("duration_donor_99").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun `confirmed edit persists through the real planner save path and a new store`() {
        val store = StudyPlannerStore(app)
        assertTrue(store.write(plan()))
        val model = StudyPlannerViewModel(app)
        open(model)
        assertEquals(plan(), store.read().plan)
        rule.onNodeWithTag("confirm_duration_edit").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.read().plan!!.blocks.first().minutes == 45 }
        val restored = StudyPlannerStore(app).read().plan!!
        assertEquals(45, restored.blocks.first().minutes)
        assertTrue(restored.blocks.first().durationEdited)
        assertEquals(15, restored.blocks.last().minutes)
        assertEquals(90, restored.blocks.sumOf { it.minutes })
    }

    @Test fun `concurrent lock save defeats an old duration preview without losing the lock`() {
        val store = StudyPlannerStore(app)
        assertTrue(store.write(plan()))
        val before = store.readData().snapshot
        val model = StudyPlannerViewModel(app)
        open(model)
        val locked = plan().copy(blocks = plan().blocks.map { if (it.id == 99) it.copy(locked = true) else it })
        assertTrue(store.writeSnapshot(before, before.copy(plan = locked)))
        rule.onNodeWithTag("confirm_duration_edit").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.editorGeneration > 0 }
        assertEquals(locked, store.read().plan)
        assertNotNull(model.state.value.error)
        assertEquals(30, store.read().plan!!.blocks.first().minutes)
    }
}

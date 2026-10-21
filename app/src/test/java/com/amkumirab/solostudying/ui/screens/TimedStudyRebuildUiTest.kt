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
class TimedStudyRebuildUiTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `only confirmed rebuild adopts new clock windows and recorded progress`() {
        val today = LocalDate.of(2026, 10, 19)
        fun windows(start: Int) = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(start, start + 60))) })
        val oldWindows = windows(1020)
        val newWindows = windows(540)
        val boss = BossEntity(id = 1, name = "Physics", difficulty = "Hard", requiredMinutes = 120, timeSpentSeconds = 60)
        val subject = PlannerSubject(1, "Physics", preferredWeeklyMinutes = 60)
        val old = buildStudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(13), 5, 0)), List(7) { 60 }, 25, 120,
            studyWindows = oldWindows).withWeeklyScheduling(listOf(subject)))
        val store = StudyPlannerStore(app)
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(old, listOf(subject), newWindows)))
        val model = StudyPlannerViewModel(app)
        rule.setContent { SoloStudyingTheme { StudyPlannerEntry(listOf(boss), today, List(7) { 60 }, false, { _, _ -> }, plannerViewModel = model, showDailySessions = false) } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(old, store.readData().plan)
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.readData().plan?.config?.studyWindows == newWindows }
        val rebuilt = store.readData().plan!!
        assertEquals(119, rebuilt.blocks.sumOf { it.minutes })
        assertEquals(60L, rebuilt.config.courses.single().baselineSeconds)
        assertTrue(rebuilt.blocks.all { it.startMinute!! >= 540 && it.startMinute + it.minutes <= 600 })
    }
}

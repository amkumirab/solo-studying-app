package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.content.Context
import android.os.Looper
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
class PlannerReportIntegrationTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val monday = LocalDate.of(2026, 10, 26)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 120)
    private val plan = StudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 120, monday.plusDays(13), 5, 0)),
        List(7) { 120 }, 60), listOf(StudyBlock(1, 1, monday, 60), StudyBlock(2, 1, monday.plusWeeks(1), 60)))

    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `real entry report restores independently and preserves saved records and calendar state`() {
        val store = StudyPlannerStore(app)
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 660))) })
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(plan,
            listOf(PlannerSubject(1, "OS revision", 5, SubjectColor.Green)), windows)))
        val preferences = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val before = preferences.all.toMap()
        val model = StudyPlannerViewModel(app)
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme {
            StudyPlannerEntry(listOf(boss), monday, List(7) { 120 }, false,
                { _, _ -> fail("Reading reports must not start study") }, plannerViewModel = model, showDailySessions = false)
        } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Next week").performScrollTo().performClick()
        rule.onNodeWithTag("open_weekly_report").performScrollTo().performClick()
        rule.onNodeWithText("Edit answers").assertDoesNotExist()
        rule.onNodeWithText("Rebuild remaining").assertDoesNotExist()
        rule.onNodeWithTag("report_previous_week").performScrollTo().performClick()
        rule.onNodeWithTag("report_section_Priority").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("report_previous_week").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("report_section_Priority").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("report_back").performClick()
        rule.onNodeWithText("Next week").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("open_weekly_report").performScrollTo().performClick()
        rule.onNodeWithTag("report_next_week").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Close planner").performClick()
        rule.runOnIdle {
            assertEquals(before, preferences.all)
            assertEquals(plan, store.readData().plan)
            assertEquals(0L, boss.timeSpentSeconds)
        }
    }
}

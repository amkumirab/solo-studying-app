package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class PlannerOverviewEntryTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val monday = LocalDate.of(2026, 11, 2)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 600)
    private val plan = StudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 600, monday.plusDays(13), 5, 0)),
        List(7) { 60 }, 60, 120), listOf(StudyBlock(1, 1, monday, 60),
        StudyBlock(2, 1, monday.plusDays(1), 30), StudyBlock(3, 1, monday.plusWeeks(1), 60)))
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }
    private fun waitLoaded(model: StudyPlannerViewModel) = rule.waitUntil(10000) {
        shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded
    }
    private fun dialogTag(tag: String) = rule.onNode(hasTestTag(tag) and hasAnyAncestor(isDialog()))

    @Test fun `entry and selected calendar week share exact read-only metrics with restored details`() {
        val store = StudyPlannerStore(app)
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 600))) })
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(plan,
            listOf(PlannerSubject(1, "Systems", 1, SubjectColor.Green)), windows)))
        val preferences = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val before = preferences.all.toMap()
        val model = StudyPlannerViewModel(app)
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            StudyPlannerEntry(listOf(boss), monday, List(7) { 480 }, false,
                { _, _ -> fail("Reading overview must not start study") }, plannerViewModel = model, showDailySessions = false)
        } } }
        waitLoaded(model)
        rule.onNodeWithTag("overview_budget").assertTextEquals("Study budget", "7 h")
        rule.onNodeWithTag("overview_planned").assertTextEquals("Planned focus", "1 h 30 min")
        rule.onNodeWithTag("overview_details").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("overview_details").performScrollTo().assertTextEquals("Hide planning details")
        rule.onNodeWithTag("overview_details").performClick()
        rule.onNodeWithTag("open_study_planner").performScrollTo().assertIsDisplayed().performClick()
        rule.onAllNodes(hasTestTag("planner_week_overview") and hasAnyAncestor(isDialog())).assertCountEquals(1)
        rule.onNodeWithText("Next week").performScrollTo().performClick()
        dialogTag("overview_planned").performScrollTo().assertTextEquals("Planned focus", "1 h")
        rule.onNodeWithTag("open_weekly_report").performScrollTo().performClick()
        rule.onNodeWithTag("report_planned_total").performScrollTo().assertTextEquals("Planned focus: 1 h")
        rule.onNodeWithTag("report_back").performClick()
        dialogTag("overview_planned").performScrollTo().assertTextEquals("Planned focus", "1 h")
        rule.onNodeWithText("Close planner").performClick()
        rule.onNodeWithTag("overview_planned").assertTextEquals("Planned focus", "1 h 30 min")
        rule.runOnIdle { assertEquals(before, preferences.all); assertEquals(plan, store.readData().plan) }
    }

    @Test fun `saved plan updates refresh overview while recorded study never changes planned focus`() {
        val store = StudyPlannerStore(app)
        assertTrue(store.write(plan))
        val model = StudyPlannerViewModel(app)
        rule.setContent { SoloStudyingTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            StudyPlannerEntry(listOf(boss.copy(timeSpentSeconds = 3600)), monday, List(7) { 60 }, false,
                { _, _ -> }, plannerViewModel = model, showDailySessions = false)
        } } }
        waitLoaded(model)
        rule.onNodeWithTag("overview_planned").assertTextEquals("Planned focus", "1 h 30 min")
        val updated = plan.copy(blocks = plan.blocks.filterNot { it.id == 2 })
        rule.runOnIdle { model.save(updated, model.state.value.snapshot) }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.plan == updated && !model.state.value.busy }
        rule.onNodeWithTag("overview_planned").assertTextEquals("Planned focus", "1 h")
        rule.onNodeWithTag("overview_unallocated").assertTextEquals("Unallocated budget", "6 h")
        assertEquals(updated, store.readData().plan)
    }
}

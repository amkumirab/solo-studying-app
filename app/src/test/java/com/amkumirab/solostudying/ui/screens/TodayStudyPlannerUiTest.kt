package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.domain.today.TodayPlan
import com.amkumirab.solostudying.planner.StudyPlannerStore
import com.amkumirab.solostudying.ui.viewmodel.StudyPlannerViewModel
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodayStudyPlannerUiTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 8)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 6000)
    @Before fun resetStore() { ApplicationProvider.getApplicationContext<Context>().deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanupStore() { resetStore() }
    private fun plan(deadline: LocalDate = today.plusDays(2)) = StudyPlan(
        PlannerConfig(today, listOf(PlannerCourse(1, 25, deadline, 2, 0)), List(7) { 60 }, 25),
        listOf(StudyBlock(1, 1, today, 25)),
    )

    @Test fun `today launches remaining minutes and updates only from recorded study`() {
        var goals by mutableStateOf(listOf(boss))
        var active by mutableStateOf(false)
        var started: Int? = null
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlannerToday(plan(), goals, today, true, active, null, {}, { _, minutes -> started = minutes }, { _, _ -> fail() })
        } } }
        rule.onNodeWithTag("today_planner_start_1").performClick()
        rule.runOnIdle { assertEquals(25, started) }
        rule.onNodeWithText("0 of 25 planned minutes credited").assertExists()
        rule.runOnIdle { goals = listOf(boss.copy(timeSpentSeconds = 61)); active = true }
        rule.onNodeWithText("1 of 25 planned minutes credited").assertExists()
        rule.onNodeWithTag("today_planner_start_1").assertIsNotEnabled()
        rule.onNodeWithTag("postpone_today").assertIsNotEnabled()
        rule.runOnIdle { active = false; goals = listOf(boss.copy(timeSpentSeconds = 1500)) }
        rule.onNodeWithTag("today_planner_start_1").assertDoesNotExist()
        rule.onNodeWithText("Today's planned sessions are complete.").assertExists()
    }

    @Test fun `postponement preview cancel leaves plan unchanged and confirm saves once`() {
        var saved: StudyPlan? = null
        var saves = 0
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlannerToday(plan(), listOf(boss), today, true, false, null, {}, { _, _ -> }, { updated, done -> saved = updated; saves++; done() })
        } } }
        rule.onNodeWithTag("postpone_today").performScrollTo().performClick()
        waitForPreview()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertNull(saved) }
        rule.onNodeWithTag("postpone_today").performClick()
        waitForPreview()
        rule.onNodeWithTag("confirm_postpone_today").performClick()
        rule.runOnIdle { assertEquals(1, saves); assertTrue(saved!!.blocks.single().date > today) }
    }

    private fun waitForPreview() = rule.waitUntil(10000) {
        rule.onAllNodesWithTag("confirm_postpone_today").fetchSemanticsNodes().any { !it.config.contains(SemanticsProperties.Disabled) }
    }

    @Test fun `deadline shortage disables confirmation and shows unchanged plan message`() {
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlannerToday(plan(today), listOf(boss), today, true, false, null, {}, { _, _ -> }, { _, _ -> fail() })
        } } }
        rule.onNodeWithTag("postpone_today").performScrollTo().performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("Not enough room before the deadlines").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_postpone_today").assertIsNotEnabled()
        rule.onNodeWithText("Nothing will change. Open the weekly plan to adjust dates or available time.").assertExists()
    }

    @Test fun `today expands long lists and flags missing courses`() {
        val config = PlannerConfig(today, listOf(PlannerCourse(1, 100, today.plusDays(2), 2, 0)), List(7) { 120 }, 25)
        val longPlan = StudyPlan(config, (1..4).map { StudyBlock(it, 1, today, 25) })
        var goals by mutableStateOf(listOf(boss))
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlannerToday(longPlan, goals, today, true, false, null, {}, { _, _ -> }, { _, _ -> })
        } } }
        rule.onNodeWithTag("today_planner_start_4").assertDoesNotExist()
        rule.onNodeWithText("Show all 4 sessions").performScrollTo().performClick()
        rule.onNodeWithTag("today_planner_start_4").performScrollTo().assertExists()
        rule.runOnIdle { goals = emptyList() }
        rule.onNodeWithText("A planned course was removed. Open the weekly plan to update it.").assertExists()
        rule.onNodeWithTag("postpone_today").assertDoesNotExist()
    }

    @Test fun `active session during preview prevents saving and write error retains dialog`() {
        var active by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlannerToday(plan(), listOf(boss), today, true, active, error, {}, { _, _ -> }, { _, _ -> error = "Could not save your plan. Please try again." })
        } } }
        rule.onNodeWithTag("postpone_today").performScrollTo().performClick()
        waitForPreview()
        rule.runOnIdle { active = true }
        rule.onNodeWithTag("confirm_postpone_today").assertIsNotEnabled()
        rule.onNodeWithText("Cancel").assertIsEnabled()
        rule.runOnIdle { active = false }
        rule.onNodeWithTag("confirm_postpone_today").performClick()
        rule.onNodeWithTag("confirm_postpone_today").assertExists()
        rule.onAllNodesWithText("Could not save your plan. Please try again.").assertCountEquals(2)
    }

    @Test fun `integrated dashboard saves confirmed move and reopened storage retains it`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val original = plan()
        assertTrue(StudyPlannerStore(context).write(original))
        val viewModel = StudyPlannerViewModel(context)
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlannerEntry(listOf(boss), today, List(7) { 60 }, false, { _, _ -> }, plannerViewModel = viewModel,
                content = { weekly, section -> TodayDashboardCard(
                    TodayPlan(60, 0, 60, 0f, false, emptyList()), false, {}, today,
                    hasWeeklyPlan = weekly != null, plannedSessions = section,
                ) })
        } } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("postpone_today").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithTag("today_dashboard").assertCountEquals(1)
        rule.onNodeWithText("TODAY'S PLAN COMPLETE").assertDoesNotExist()
        rule.onNodeWithTag("postpone_today").performScrollTo().performClick()
        waitForPreview()
        rule.onNodeWithTag("confirm_postpone_today").performClick()
        rule.waitUntil(10000) { StudyPlannerStore(context).read().plan?.blocks?.single()?.date == today.plusDays(1) }
        rule.waitUntil(10000) { rule.onAllNodesWithText("No course sessions scheduled today.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("No course sessions scheduled today.").assertExists()
        rule.onNodeWithTag("open_study_planner").performScrollTo().performClick()
        rule.onNodeWithText("Weekly study plan").assertExists()
        rule.runOnIdle { assertEquals(original.config, StudyPlannerStore(context).read().plan!!.config) }
    }
}

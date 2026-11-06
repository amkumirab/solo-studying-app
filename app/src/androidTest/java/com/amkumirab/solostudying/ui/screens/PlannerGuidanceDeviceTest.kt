package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
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
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class PlannerGuidanceDeviceTest {
    @get:Rule val rule = createComposeRule()
    private val app = PlannerQaApplication(ApplicationProvider.getApplicationContext())
    private val monday = LocalDate.of(2026, 11, 2)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 240)
    private val store get() = StudyPlannerStore(app)
    private var active by mutableStateOf(false)
    private fun plan(goal: Int = 120, days: Long = 7) = StudyPlan(PlannerConfig(monday,
        listOf(PlannerCourse(1, 240, monday.plusDays(days), 5, 0)), List(7) { 120 }, 60, goal),
        listOf(StudyBlock(1, 1, monday, 60)))
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME); active = false }
    @After fun cleanup() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    private fun preferences() = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, Context.MODE_PRIVATE).all.toMap()
    private fun node(tag: String) = rule.onNode(hasTestTag(tag) and hasAnyAncestor(isDialog()))
    private fun open(source: StudyPlan): Map<String, *> {
        assertTrue(store.write(source))
        val model = StudyPlannerViewModel(app)
        val before = preferences()
        rule.setContent { SoloStudyingTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            StudyPlannerEntry(listOf(boss), monday, List(7) { 120 }, active,
                { _, _ -> fail("Guidance must never start study") }, plannerViewModel = model, showDailySessions = false)
        } } }
        rule.waitUntil(10000) { model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performScrollTo().performClick()
        return before
    }

    @Test fun deadlineReviewIsReadOnlyAndActiveStudyBlocksNavigation() {
        val source = plan()
        val before = open(source)
        rule.runOnIdle { active = true }
        node("guidance_accepted_action_0").performScrollTo().assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag("rebuild_comparison_loading").assertDoesNotExist()
        assertEquals(before, preferences())
        rule.runOnIdle { active = false }
        node("guidance_accepted_action_0").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("rebuild_comparison_ready").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(before, preferences())
        rule.onNodeWithTag("cancel_rebuild_comparison").performClick()
        node("guidance_accepted").performScrollTo().assertExists()
        assertEquals(before, preferences())
        assertEquals(source, store.readData().plan)
    }

    @Test fun budgetAdviceOpensAvailabilityWithoutSaving() {
        val before = open(plan(900))
        node("guidance_accepted_details").performScrollTo().performClick()
        node("guidance_accepted_action_1").performScrollTo().assertTextEquals("Review availability").performClick()
        rule.onNodeWithText("Weekly availability").assertExists()
        assertEquals(before, preferences())
    }

    @Test fun priorityAndGoalAdviceNavigateWithoutSaving() {
        val before = open(plan(180, 30))
        node("guidance_accepted_action_0").performScrollTo().assertTextEquals("Review subjects").performClick()
        rule.onNodeWithText("Add subject").assertExists()
        assertEquals(before, preferences())
        rule.onNodeWithText("Schedule", useUnmergedTree = true).performClick()
        node("guidance_accepted_details").performScrollTo().performClick()
        node("guidance_accepted_action_1").performScrollTo().assertTextEquals("Edit plan answers").performClick()
        rule.onNodeWithTag("planner_next").assertExists()
        assertEquals(before, preferences())
    }
}

package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
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
class WeightedWeeklySessionsUiTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val store get() = StudyPlannerStore(app)
    private val monday = LocalDate.of(2026, 10, 19)
    private val boss = BossEntity(id = 1, name = "Physics", difficulty = "Medium", requiredMinutes = 90, timeSpentSeconds = 60)
    private fun config(goal: Int = 120) = PlannerConfig(monday, listOf(PlannerCourse(1, 90, monday.plusDays(20), 3, 0)), List(7) { 60 }, 30, goal)
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    @Test fun `actual split shows deadline reservations rather than an advisory recalculation`() {
        val input = config().copy(courses = listOf(PlannerCourse(1, 90, monday, 1, 0), PlannerCourse(2, 600, monday.plusDays(41), 5, 0)), weekdayMinutes = List(7) { 120 }, weeklyGoalMinutes = 240).withWeeklyScheduling(emptyList())
        val plan = buildStudyPlan(input)
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            WeeklyAllocationCard(plan, listOf(boss, boss.copy(id = 2, name = "Operating Systems")), emptyList())
        } } }
        rule.onNodeWithTag("weekly_share_1").performScrollTo().assertTextEquals("1 h 30 min scheduled · Priority 1")
        rule.onNodeWithTag("weekly_share_2").performScrollTo().assertTextEquals("2 h 30 min scheduled · Priority 5")
        rule.onNodeWithTag("weekly_recommended_total").performScrollTo().assertTextEquals("Scheduled: 4 h")
    }

    @Test fun `zero goal needs explicit incomplete acknowledgement before accepting empty sessions`() {
        val plan = buildStudyPlan(config(0).withWeeklyScheduling(emptyList()))
        var accepted: StudyPlan? = null
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlannerPreview(plan, listOf(boss), false, { accepted = it })
        } } }
        rule.onNodeWithTag("weekly_recommended_total").performScrollTo().assertTextEquals("Scheduled: 0 min")
        rule.onNodeWithTag("planner_accept").performScrollTo().assertIsNotEnabled()
        assertNull(accepted)
        rule.onNodeWithTag("planner_acknowledge").performScrollTo().performClick()
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        assertEquals(plan, accepted)
    }

    @Test fun `legacy goal upgrades only after confirmed rebuild using current caps and recorded baseline`() {
        val old = buildStudyPlan(config())
        val subject = PlannerSubject(1, "Physics exam", priority = 5, preferredWeeklyMinutes = 40)
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(old, listOf(subject))))
        val model = StudyPlannerViewModel(app)
        rule.setContent { SoloStudyingTheme {
            StudyPlannerEntry(listOf(boss), monday, List(7) { 60 }, false, { _, _ -> }, plannerViewModel = model, showDailySessions = false)
        } }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").performClick()
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(old, store.readData().plan)
        assertNull(store.readData().plan!!.config.weeklyScheduling)
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); store.readData().plan?.config?.weeklyScheduling != null }
        val saved = store.readData().plan!!
        assertEquals(mapOf(1 to 40), saved.config.weeklyScheduling!!.courseMinuteLimits)
        assertEquals(60L, saved.config.courses.single().baselineSeconds)
        assertEquals(89, saved.blocks.sumOf { it.minutes })
        assertEquals(5, saved.config.courses.single().priority)
    }
}

package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
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
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
/** Real Android execution of the critical planner workflows, with isolated storage. */
class PlannerWorkflowDeviceTest {
    @get:Rule val rule = createComposeRule()
    // All writes stay in a dedicated QA preferences namespace, including ViewModel writes.
    private val app = PlannerQaApplication(ApplicationProvider.getApplicationContext())
    private val monday = LocalDate.of(2026, 11, 2)
    private val goals = listOf(
        BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 120),
        BossEntity(id = 2, name = "Physics", difficulty = "Medium", requiredMinutes = 120),
    )
    private val store get() = StudyPlannerStore(app)
    private val model = mutableStateOf<StudyPlannerViewModel?>(null)
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    private fun awaitSaved(condition: () -> Boolean) = rule.waitUntil(10000) {
        model.value?.state?.value?.let { it.loaded && !it.busy } == true && condition()
    }
    private fun open(bosses: List<BossEntity> = goals) {
        model.value = StudyPlannerViewModel(app)
        rule.setContent { SoloStudyingTheme {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                StudyPlannerEntry(bosses, monday, List(7) { 60 }, false,
                    { _, _ -> fail("Planning must not start a study timer") },
                    plannerViewModel = model.value!!, showDailySessions = false)
            }
        } }
        awaitSaved { true }
        rule.onNodeWithTag("open_study_planner").performScrollTo().performClick()
    }
    private fun section(name: String) = rule.onNodeWithText(name, useUnmergedTree = true).performClick()
    private fun next() = rule.onNodeWithTag("planner_next").performScrollTo().performClick()
    private fun overviewTag(tag: String) = rule.onNode(hasTestTag(tag) and hasAnyAncestor(isDialog()))

    @Test fun createTimedPlanAndReloadPreservesExactTotals() {
        open()
        section("Subjects")
        goals.forEach { goal ->
            rule.onNodeWithText("Add subject").performScrollTo().performClick()
            rule.onNodeWithTag("subject_goal_${goal.id}").performClick()
            rule.onNodeWithTag("subject_name").performTextReplacement("${goal.name} revision")
            rule.onNodeWithTag("subject_priority_${if (goal.id == 1) 5 else 2}").performScrollTo().performClick()
            rule.onNodeWithTag("subject_color_Green").performScrollTo().performClick()
            rule.onNodeWithTag("save_subject").performScrollTo().performClick()
            awaitSaved { store.readData().subjects.size == goal.id }
        }
        section("Availability")
        rule.onNodeWithTag("edit_availability").performClick()
        rule.onNodeWithTag("edit_availability_day_0").performScrollTo().performClick()
        rule.onNodeWithTag("window_end_0").performTextReplacement("19:00")
        rule.onNodeWithTag("save_availability_day").performScrollTo().performClick()
        rule.onNodeWithTag("availability_enabled_6").performScrollTo().performClick()
        rule.onNodeWithTag("save_availability").performScrollTo().performClick()
        awaitSaved { store.readData().availability != null }
        section("Schedule")
        goals.forEach { rule.onNodeWithText("${it.name} revision").performClick() }
        next()
        repeat(2) { rule.onNodeWithTag("planner_hours").performTextReplacement("2"); next() }
        rule.onNodeWithTag("planner_weekly_goal").performScrollTo().performTextReplacement("2")
        next()
        rule.onNodeWithText("45 min").performScrollTo().performClick()
        rule.onNodeWithTag("planner_break_minutes").performScrollTo().performTextReplacement("10")
        next()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("planner_accept").performScrollTo().assertIsEnabled().performClick()
        awaitSaved { store.readData().plan != null }
        val snapshot = store.readData().snapshot
        val plan = snapshot.plan!!
        assertEquals(240, plan.blocks.sumOf { it.minutes })
        assertTrue(plan.shortfalls.isEmpty())
        assertEquals(listOf(5, 2), plan.config.courses.map { it.priority })
        assertEquals(snapshot.availability, plan.config.studyWindows)
        assertEquals(10, plan.config.breakMinutes)
        overviewTag("overview_planned").assertTextEquals("Planned focus", "2 h")
        overviewTag("overview_goal").assertTextEquals("Weekly goal", "2 h")
        assertReportMatches(plan)
        rule.onNodeWithText("Close planner").performClick()
        rule.runOnIdle { model.value = StudyPlannerViewModel(app) }
        awaitSaved { model.value!!.state.value.snapshot == snapshot }
        rule.onNodeWithTag("overview_planned").assertTextEquals("Planned focus", "2 h")
        assertEquals(snapshot, store.readData().snapshot)
    }

    @Test fun rebuildGuardsConflictsAndPreservesLocksAndCredit() {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 720))) })
        val subjects = goals.map { PlannerSubject(it.id, it.name, 3, deadline = monday.plusDays(6)) }
        val original = StudyPlan(PlannerConfig(monday, listOf(
            PlannerCourse(1, 120, monday.plusDays(6), 3, 600),
            PlannerCourse(2, 60, monday.plusDays(6), 3, 300)), List(7) { 180 }, 60, 180,
            studyWindows = windows).withWeeklyScheduling(subjects), listOf(
            StudyBlock(1, 1, monday, 30, 540),
            StudyBlock(2, 1, monday.plusDays(1), 30, 540, locked = true),
            StudyBlock(3, 1, monday.plusDays(2), 60, 540),
            StudyBlock(4, 2, monday, 60, 600, manual = true)))
        val recorded = listOf(goals[0].copy(timeSpentSeconds = 1500), goals[1].copy(timeSpentSeconds = 300))
        assertTrue(store.writeSnapshot(store.readData().snapshot, PlannerSnapshot(original, subjects, windows)))
        open(recorded)
        assertEquals(900L, weeklyPlannerReport(original, recorded, monday).creditedSeconds)
        assertReportMatches(original, recorded)
        section("Subjects")
        rule.onNode(hasScrollToNodeAction() and hasAnyAncestor(isDialog()))
            .performScrollToNode(hasTestTag("edit_subject_2"))
        rule.onNodeWithTag("edit_subject_2").performScrollTo().performClick()
        rule.onNodeWithTag("subject_priority_5").performScrollTo().performClick()
        rule.onNodeWithTag("save_subject").performScrollTo().performClick()
        awaitSaved { store.readData().subjects.last().priority == 5 }
        section("Availability")
        rule.onNodeWithTag("edit_availability").performClick()
        rule.onNodeWithTag("edit_availability_day_1").performScrollTo().performClick()
        rule.onNodeWithTag("window_start_0").performTextReplacement("17:00")
        rule.onNodeWithTag("window_end_0").performTextReplacement("20:00")
        rule.onNodeWithTag("save_availability_day").performScrollTo().performClick()
        rule.onNodeWithTag("save_availability").performScrollTo().performClick()
        awaitSaved { store.readData().availability!!.days[1].windows.first().startMinute == 1020 }
        assertEquals(original, store.readData().plan)
        section("Schedule")
        overviewTag("overview_planned").assertTextEquals("Planned focus", "3 h")
        val preferences = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, 0)
        val beforeConflict = preferences.all.toMap()
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("rebuild_comparison_ready").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsNotEnabled()
        rule.waitUntil(10000) {
            rule.onAllNodesWithText("Locked sessions conflict", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(beforeConflict, preferences.all)
        assertEquals(original, store.readData().plan)
        rule.onNodeWithTag("cancel_rebuild_comparison").performClick()
        section("Availability")
        rule.onNodeWithTag("edit_availability").performClick()
        rule.onNodeWithTag("edit_availability_day_1").performScrollTo().performClick()
        rule.onNodeWithTag("window_start_0").performTextReplacement("09:00")
        rule.onNodeWithTag("window_end_0").performTextReplacement("12:00")
        rule.onNodeWithTag("save_availability_day").performScrollTo().performClick()
        rule.onNodeWithTag("save_availability").performScrollTo().performClick()
        awaitSaved { store.readData().availability == windows }
        section("Schedule")
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(original, store.readData().plan)
        rule.onNodeWithText("Rebuild remaining").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("rebuild_comparison_ready").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Confirm").performClick()
        awaitSaved { store.readData().plan != original }
        val rebuilt = store.readData().plan!!
        assertTrue(rebuilt.shortfalls.isEmpty())
        assertEquals(original.blocks.single { it.locked }, rebuilt.blocks.single { it.locked })
        assertEquals(165, rebuilt.blocks.sumOf { it.minutes })
        assertEquals(105, rebuilt.config.courses.first { it.bossId == 1 }.remainingMinutes)
        assertEquals(1500L, rebuilt.config.courses.first { it.bossId == 1 }.baselineSeconds)
        assertEquals(5, rebuilt.config.courses.first { it.bossId == 2 }.priority)
        assertEquals(900L, (original.config.courses.sumOf { it.remainingMinutes } -
            rebuilt.config.courses.sumOf { it.remainingMinutes }) * 60L)
        assertEquals(0L, weeklyPlannerReport(rebuilt, recorded, monday).creditedSeconds)
        overviewTag("overview_planned").assertTextEquals("Planned focus", "2 h 45 min")
        assertReportMatches(rebuilt, recorded)
        val snapshot = store.readData().snapshot
        rule.onNodeWithText("Close planner").performClick()
        rule.runOnIdle { model.value = StudyPlannerViewModel(app) }
        awaitSaved { model.value!!.state.value.snapshot == snapshot }
        rule.onNodeWithTag("overview_planned").assertTextEquals("Planned focus", "2 h 45 min")
    }

    @Test fun subjectRemovalPreservesCreditAndRejectsStaleSaves() {
        val windows = defaultWeeklyAvailability(List(7) { 60 })
        val subjects = goals.map { PlannerSubject(it.id, it.name) }
        val original = StudyPlan(PlannerConfig(monday, listOf(
            PlannerCourse(1, 60, monday.plusDays(6), 3, 0),
            PlannerCourse(2, 60, monday.plusDays(6), 3, 300)), List(7) { 60 }, 30, 120,
            studyWindows = windows).withWeeklyScheduling(subjects), listOf(
            StudyBlock(7, 1, monday, 30, 1020, manual = true),
            StudyBlock(8, 1, monday.plusDays(1), 30, 1020, locked = true),
            StudyBlock(19, 2, monday, 30, 1050),
            StudyBlock(20, 2, monday.plusDays(2), 30, 1020)))
        val recorded = listOf(goals[0], goals[1].copy(timeSpentSeconds = 600))
        val stale = PlannerSnapshot(original, subjects, windows)
        assertTrue(store.writeSnapshot(store.readData().snapshot, stale))
        open(recorded)
        val preferences = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, 0)
        val before = preferences.all.toMap()
        section("Subjects")
        rule.onNodeWithTag("remove_subject_1").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(before, preferences.all)
        rule.onNodeWithTag("remove_subject_1").performScrollTo().performClick()
        rule.onNodeWithTag("confirm_remove_subject").performClick()
        awaitSaved { store.readData().subjects.map { it.bossId } == listOf(2) }
        val survivor = store.readData().snapshot
        assertEquals(original.blocks.filter { it.bossId == 2 }, survivor.plan!!.blocks)
        assertEquals(listOf(19, 20), survivor.plan.blocks.map { it.id })
        assertEquals(300L, survivor.plan.config.courses.single().baselineSeconds)
        assertEquals(windows, survivor.availability)
        assertEquals(listOf(subjects[1]), survivor.subjects)
        val previousCredit = blockProgress(original, recorded).filter { it.block.bossId == 2 }
            .associate { it.block.id to it.creditedSeconds }
        assertEquals(mapOf(19 to 300L, 20 to 0L), previousCredit)
        assertEquals(previousCredit, blockProgress(survivor.plan, recorded).associate { it.block.id to it.creditedSeconds })
        val afterRemoval = preferences.all.toMap()
        var saved = false
        rule.runOnIdle { model.value!!.save(original, stale) { saved = true } }
        awaitSaved { model.value!!.state.value.error != null }
        assertFalse(saved)
        assertEquals(afterRemoval, preferences.all)
        assertEquals(survivor, model.value!!.state.value.snapshot)
        section("Schedule")
        overviewTag("overview_planned").assertTextEquals("Planned focus", "1 h")
        assertReportMatches(survivor.plan, recorded)
        rule.onNodeWithText("Close planner").performClick()
        rule.runOnIdle { model.value = StudyPlannerViewModel(app) }
        awaitSaved { model.value!!.state.value.snapshot == survivor }
        rule.onNodeWithTag("overview_planned").assertTextEquals("Planned focus", "1 h")
        assertEquals(600L, recorded[1].timeSpentSeconds)
    }

    private fun assertReportMatches(plan: StudyPlan, bosses: List<BossEntity> = goals) {
        val expected = weeklyPlannerReport(plan, bosses, monday)
        val preferences = app.getSharedPreferences(StudyPlannerStore.PREFERENCES_NAME, 0)
        val before = preferences.all.toMap()
        assertEquals(plannerWeekOverview(plan, monday).plannedMinutes, expected.plannedMinutes)
        assertEquals(expected.plannedMinutes, expected.courses.sumOf { it.plannedMinutes })
        assertEquals(expected.plannedMinutes, expected.priorities.sumOf { it.plannedMinutes })
        rule.onNodeWithTag("open_weekly_report").performScrollTo().performClick()
        rule.onNodeWithTag("report_planned_total").performScrollTo()
            .assertTextEquals("Planned focus: ${plannerTimeLabel(expected.plannedMinutes)}")
        rule.onNodeWithTag("report_credit")
            .assertTextEquals("Credit assigned to these sessions: ${plannerCreditLabel(expected.creditedSeconds)}")
        expected.days.forEachIndexed { index, day ->
            rule.onNodeWithTag("report_day_$index").performScrollTo()
                .assertTextContains("${plannerTimeLabel(day.plannedMinutes)} planned", substring = true)
        }
        rule.onNodeWithTag("report_section_Subjects").performScrollTo().performClick()
        expected.courses.forEach { course ->
            rule.onNodeWithTag("report_course_${course.bossId}").performScrollTo()
                .assertTextContains("${plannerTimeLabel(course.plannedMinutes)} planned", substring = true)
        }
        rule.onNodeWithTag("report_section_Priority").performScrollTo().performClick()
        expected.priorities.forEach { item ->
            rule.onNodeWithTag("report_priority_${item.band.name}").performScrollTo()
                .assertTextContains("${plannerTimeLabel(item.plannedMinutes)} planned", substring = true)
        }
        rule.onNodeWithTag("report_back").performClick()
        assertEquals(before, preferences.all)
    }
}

internal class PlannerQaApplication(base: Context) : Application() {
    init { attachBaseContext(base) }
    override fun getApplicationContext(): Context = this
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        super.getSharedPreferences("device_acceptance_$name", mode)
    override fun deleteSharedPreferences(name: String): Boolean =
        super.deleteSharedPreferences("device_acceptance_$name")
}

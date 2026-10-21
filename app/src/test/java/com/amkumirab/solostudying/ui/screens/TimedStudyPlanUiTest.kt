package com.amkumirab.solostudying.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimedStudyPlanUiTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 19)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 45)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1050), StudyTimeWindow(1140, 1170))) })
    private val config = PlannerConfig(today, listOf(PlannerCourse(1, 45, today, 5, 0)), List(7) { 60 }, 45)

    @Test fun `wizard previews and explicitly accepts times from saved windows`() {
        var accepted: StudyPlan? = null
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(boss), today, buildStudyPlan(config), List(7) { 60 }, false, { accepted = it }, {}, availability = windows)
        } }
        repeat(4) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("preview_clock_1").performScrollTo().assertTextContains("17:00–17:30", substring = true)
        assertNull(accepted)
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(windows, accepted!!.config.studyWindows)
            assertEquals(listOf(1020, 1140), accepted!!.blocks.map { it.startMinute })
        }
    }

    @Test fun `weekly schedule displays chronological times without reordering credit ids`() {
        val plan = StudyPlan(config.copy(studyWindows = windows), listOf(StudyBlock(7, 1, today, 15, 1140), StudyBlock(19, 1, today, 30, 1020)))
        rule.setContent { SoloStudyingTheme { StudyPlannerWeek(plan, listOf(boss), today, false, { _, _ -> }, {}) } }
        val list = rule.onNode(hasScrollToNodeAction())
        list.performScrollToNode(hasTestTag("week_clock_19"))
        rule.onNodeWithTag("week_clock_19").assertIsDisplayed().assertTextContains("17:00–17:30", substring = true)
        val firstScroll = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        val firstTop = rule.onNodeWithTag("week_clock_19").fetchSemanticsNode().positionInRoot.y
        list.performScrollToNode(hasTestTag("week_clock_7"))
        rule.onNodeWithTag("week_clock_7").assertIsDisplayed().assertTextContains("19:00–19:15", substring = true)
        val secondScroll = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue(secondScroll > firstScroll || secondScroll == firstScroll && firstTop < rule.onNodeWithTag("week_clock_7").fetchSemanticsNode().positionInRoot.y)
        assertEquals(listOf(7, 19), plan.blocks.map { it.id })
    }

    @Test fun `today shows planned times without preventing early study`() {
        val plan = buildStudyPlan(config.copy(studyWindows = windows))
        var started = 0
        rule.setContent { SoloStudyingTheme {
            StudyPlannerToday(plan, listOf(boss), today, true, false, null, {}, { _, _ -> started++ }, { _, _ -> })
        } }
        rule.onNodeWithTag("today_clock_1").assertTextContains("17:00–17:30", substring = true)
        rule.onNodeWithTag("today_planner_start_1").performClick()
        assertEquals(1, started)
    }
}

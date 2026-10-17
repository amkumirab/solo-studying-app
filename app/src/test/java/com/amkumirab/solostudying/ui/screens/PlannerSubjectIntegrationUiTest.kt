package com.amkumirab.solostudying.ui.screens

import androidx.compose.ui.test.*
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
class PlannerSubjectIntegrationUiTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 17)
    private val goal = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard",
        requiredMinutes = 6000, deadlineDate = today.plusDays(6).toString())
    private val subject = PlannerSubject(1, "OS revision", 5, SubjectColor.Green, 300)

    @Test fun `subject defaults preserve total effort and a cleared exam deadline`() {
        var accepted: StudyPlan? = null
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWizard(listOf(goal), today, null, List(7) { 480 }, false,
                { accepted = it }, {}, subjects = listOf(subject))
        } }
        rule.onNodeWithText(subject.name).performClick()
        rule.onNodeWithTag("planner_next").performClick()
        rule.onNodeWithTag("planner_hours").assertTextContains("100")
        rule.onNodeWithText("Finish by: ${today.plusDays(28)}").performScrollTo().assertExists()
        rule.onNodeWithText("Priority 5 of 5 · Critical").performScrollTo().assertExists()
        rule.onNodeWithText("Preferred: 5 h per week").performScrollTo().assertExists()
        repeat(3) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("planner_accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Show deadline breakdown").performScrollTo().performClick()
        rule.onNodeWithText("OS revision: 100 h remaining").performScrollTo().assertExists()
        rule.onNodeWithTag("planner_accept").performScrollTo().performClick()
        rule.runOnIdle {
            val course = accepted!!.config.courses.single()
            assertEquals(6000, course.remainingMinutes)
            assertEquals(5, course.priority)
            assertEquals(today.plusDays(28), course.finishBy)
            assertTrue(accepted!!.shortfalls.isEmpty())
        }
    }

    @Test fun `today uses subject name but starts the linked goal`() {
        val plan = buildStudyPlan(PlannerConfig(today,
            listOf(PlannerCourse(1, 25, today, 3, 0)), List(7) { 60 }, 25))
        var started: Pair<Int, Int>? = null
        rule.setContent { SoloStudyingTheme {
            StudyPlannerToday(plan, listOf(goal), today, true, false, null, {},
                { boss, minutes -> started = boss.id to minutes }, { _, _ -> }, subjects = listOf(subject))
        } }
        rule.onNodeWithText(subject.name).assertExists()
        rule.onNodeWithText("Green").assertExists()
        rule.onNodeWithTag("today_planner_start_1").performClick()
        rule.runOnIdle { assertEquals(1 to 25, started) }
    }

    @Test fun `weekly view shows catalog labels without relocating accepted blocks`() {
        val plan = buildStudyPlan(PlannerConfig(today,
            listOf(PlannerCourse(1, 25, today.plusDays(6), 2, 0)), List(7) { 60 }, 25))
        var started: Int? = null
        rule.setContent { SoloStudyingTheme {
            StudyPlannerWeek(plan, listOf(goal), today, false, { boss, _ -> started = boss.id },
                { fail("Display settings must not move blocks") }, subjects = listOf(subject.copy(deadline = today.plusDays(30))))
        } }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(subject.name))
        rule.onNodeWithText(subject.name).assertExists()
        rule.onNodeWithText("Green").performScrollTo().assertExists()
        rule.onNodeWithTag("planner_start_1").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, started) }
        assertEquals(2, plan.config.courses.single().priority)
        assertEquals(today, plan.blocks.single().date)
    }
}

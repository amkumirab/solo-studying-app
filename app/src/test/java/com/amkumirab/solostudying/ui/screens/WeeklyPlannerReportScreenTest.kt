package com.amkumirab.solostudying.ui.screens

import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
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
class WeeklyPlannerReportScreenTest {
    @get:Rule val rule = createComposeRule()
    private val monday = LocalDate.of(2026, 10, 26)
    private val bosses = listOf(
        BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 120),
        BossEntity(id = 2, name = "Physics", difficulty = "Medium", requiredMinutes = 120),
    )
    private val plan = StudyPlan(PlannerConfig(monday, listOf(
        PlannerCourse(1, 120, monday.plusDays(13), 5, 0),
        PlannerCourse(2, 120, monday.plusDays(13), 1, 0),
    ), List(7) { 120 }, 60), listOf(
        StudyBlock(1, 1, monday, 60), StudyBlock(2, 2, monday.plusDays(1), 30),
        StudyBlock(3, 1, monday.plusWeeks(1), 60),
    ))

    @Test fun `chart sections expose exact selections and accepted priorities without writes`() {
        rule.setContent { SoloStudyingTheme { Surface {
            WeeklyPlannerReportScreen(plan, bosses, monday, monday, {}, {}, subjects = listOf(
                PlannerSubject(1, "Systems", 1, SubjectColor.Gold),
            ))
        } } }
        rule.onNodeWithTag("report_planned_total").assertTextEquals("Planned focus: 1 h 30 min")
        rule.onNodeWithTag("report_day_0").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("report_day_1").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithTag("report_detail").performScrollTo().assertTextContains("Physics", substring = true)
        rule.onNodeWithTag("report_section_Subjects").performScrollTo().performClick()
        rule.onNodeWithTag("report_course_1").performScrollTo().performClick()
        rule.onNodeWithTag("report_detail").performScrollTo().assertTextContains("Priority 5 of 5", substring = true)
        rule.onNodeWithTag("report_section_Priority").performScrollTo().performClick()
        rule.onNodeWithTag("report_priority_Low").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithTag("report_detail").performScrollTo().assertTextContains("Physics", substring = true)
        assertEquals(3, plan.blocks.size)
        assertEquals(5, plan.config.courses.first().priority)
    }

    @Test fun `empty weeks and missing goals stay visible and do not invent completed study`() {
        rule.setContent { SoloStudyingTheme { Surface {
            WeeklyPlannerReportScreen(plan, bosses.drop(1), monday, monday.plusWeeks(2), {}, {})
        } } }
        rule.onNodeWithTag("report_planned_total").assertTextEquals("Planned focus: 0 min")
        rule.onNodeWithTag("report_credit").assertTextEquals("Credit assigned to these sessions: 0 min")
        rule.onNodeWithTag("report_detail").performScrollTo().assertTextContains("No sessions planned", substring = true)
        rule.onNodeWithTag("report_section_Subjects").performScrollTo().performClick()
        rule.onNodeWithTag("report_course_1").performScrollTo().assertExists().performClick()
        rule.onNodeWithTag("report_detail").performScrollTo().assertTextContains("Goal removed", substring = true)
        rule.onAllNodesWithText("Studied this week", substring = true).assertCountEquals(0)
    }

    @Test fun `report recomputes exact credit and planned totals when inputs change`() {
        val currentPlan = mutableStateOf(plan)
        val currentBosses = mutableStateOf(bosses)
        rule.setContent { SoloStudyingTheme { Surface {
            WeeklyPlannerReportScreen(currentPlan.value, currentBosses.value, monday, monday, {}, {})
        } } }
        rule.runOnIdle { currentBosses.value = bosses.map { if (it.id == 1) it.copy(timeSpentSeconds = 45) else it } }
        rule.onNodeWithTag("report_credit").assertTextEquals("Credit assigned to these sessions: 45 sec")
        rule.runOnIdle { currentPlan.value = plan.copy(blocks = plan.blocks.filterNot { it.id == 2 }) }
        rule.onNodeWithTag("report_planned_total").assertTextEquals("Planned focus: 1 h")
    }

    @Test fun `section and selection restore without changing the week`() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme { Surface {
            WeeklyPlannerReportScreen(plan, bosses, monday, monday, {}, {})
        } } }
        rule.onNodeWithTag("report_section_Priority").performScrollTo().performClick()
        rule.onNodeWithTag("report_priority_Low").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("report_section_Priority").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("report_priority_Low").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("report_detail").performScrollTo().assertTextContains("Physics", substring = true)
    }

    @Test fun `week controls respect plan bounds and back remains available`() {
        val displayedWeek = mutableStateOf(monday)
        var backs = 0
        rule.setContent { SoloStudyingTheme { Surface {
            WeeklyPlannerReportScreen(plan, bosses, monday, displayedWeek.value, { displayedWeek.value = it }, { backs++ })
        } } }
        rule.onNodeWithTag("report_previous_week").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("report_next_week").performScrollTo().performClick()
        rule.onNodeWithTag("report_next_week").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("report_planned_total").performScrollTo().assertTextEquals("Planned focus: 1 h")
        rule.onNodeWithTag("report_previous_week").performScrollTo().performClick()
        rule.onNodeWithTag("report_back").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(monday, displayedWeek.value); assertEquals(1, backs) }
    }
}

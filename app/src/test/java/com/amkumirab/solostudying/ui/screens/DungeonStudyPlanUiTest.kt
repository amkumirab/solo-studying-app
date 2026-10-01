package com.amkumirab.solostudying.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.amkumirab.solostudying.data.entity.DungeonEntity
import com.amkumirab.solostudying.domain.dungeon.DungeonStudyPlanInput
import com.amkumirab.solostudying.domain.dungeon.calculateDungeonStudyPlan
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DungeonStudyPlanUiTest {
    @get:Rule val composeRule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 1)
    private val dungeon = DungeonEntity(name = "Operating Systems", description = "", targetMinutes = 6_000, planStartDate = today.toString(), planDeadlineDate = "2026-11-30")

    @Test fun `form validates input previews effort and saves the selected calendar deadline`() {
        var saved: DungeonStudyPlanInput? = null
        var dismissed = false
        composeRule.setContent {
            SoloStudyingTheme {
                DungeonStudyPlanDialog(null, emptyList(), emptyList(), today, { dismissed = true }, { saved = it }, {})
            }
        }
        composeRule.onNodeWithTag("dungeon_plan_save").assertIsNotEnabled()
        composeRule.onNodeWithTag("dungeon_plan_name").performTextInput("Operating Systems")
        composeRule.onNodeWithTag("dungeon_plan_hours").performTextInput("100")
        composeRule.onNodeWithText("2 months").performScrollTo().performClick()
        composeRule.onNodeWithTag("dungeon_plan_deadline").assertTextContains("2026-12-01")
        composeRule.onNodeWithTag("dungeon_plan_save").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertEquals(6_000, saved!!.targetMinutes)
            assertEquals(today.plusMonths(2), saved!!.deadline)
            assertTrue(dismissed)
        }
    }

    @Test fun `all deselected weekdays disable saving`() {
        composeRule.setContent {
            SoloStudyingTheme { DungeonStudyPlanDialog(dungeon, listOf(dungeon), emptyList(), today, {}, {}, {}) }
        }
        for (day in 1..7) composeRule.onNodeWithTag("dungeon_plan_day_$day").performScrollTo().performClick()
        composeRule.onNodeWithTag("dungeon_plan_save").assertIsNotEnabled()
    }

    @Test fun `a save error keeps the form open and allows retry`() {
        var attempts = 0
        var dismissed = false
        composeRule.setContent {
            SoloStudyingTheme {
                DungeonStudyPlanDialog(dungeon, listOf(dungeon), emptyList(), today, { dismissed = true }, {
                    attempts++
                    if (attempts == 1) error("Database unavailable")
                }, {})
            }
        }
        composeRule.onNodeWithTag("dungeon_plan_save").performClick()
        composeRule.onNodeWithText("Could not save the plan. Please try again.").performScrollTo().assertIsDisplayed()
        assertFalse(dismissed)
        composeRule.onNodeWithTag("dungeon_plan_save").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertTrue(dismissed); assertEquals(2, attempts) }
    }

    @Test fun `removing a plan requires explicit confirmation`() {
        var removals = 0
        composeRule.setContent {
            SoloStudyingTheme { DungeonStudyPlanDialog(dungeon, listOf(dungeon), emptyList(), today, {}, {}, { removals++ }) }
        }
        composeRule.onNodeWithTag("dungeon_plan_remove").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(0, removals) }
        composeRule.onNodeWithText("Remove plan").performClick()
        composeRule.runOnIdle { assertEquals(1, removals) }
    }

    @Test fun `plan card shows readable remaining effort and accessible progress`() {
        var edits = 0
        val plan = calculateDungeonStudyPlan(dungeon, emptyList(), today)!!
        composeRule.setContent {
            SoloStudyingTheme { DungeonStudyPlanCard(plan, { edits++ }) }
        }
        composeRule.onNodeWithText("1h 39m per study day").assertIsDisplayed()
        composeRule.onNodeWithTag("dungeon_plan_progress").assertContentDescriptionEquals("Operating Systems study progress")
        composeRule.onNodeWithText("Edit plan").performClick()
        composeRule.runOnIdle { assertEquals(1, edits) }
    }
}

package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeeklyStudyGoalLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test fun `actual weekly shares and frozen limit labels wrap at large text sizes`() {
        val start = LocalDate.of(2026, 10, 19)
        val subjects = listOf(PlannerSubject(1, "Operating Systems", preferredWeeklyMinutes = 180))
        val plan = buildStudyPlan(PlannerConfig(start, listOf(PlannerCourse(1, 600, start.plusDays(41), 5, 0)), List(7) { 60 }, 30, 240).withWeeklyScheduling(subjects))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                SoloStudyingTheme {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        WeeklyAllocationCard(plan, emptyList(), subjects)
                    }
                }
            }
        }
        listOf("Scheduled weekly split", "Scheduled: 3 h", "3 h scheduled · Priority 5", "Saved weekly limit: 3 h",
            "Unused goal: 1 h · Free capacity: 4 h").forEach { text ->
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(text, layouts.isNotEmpty())
            layouts.forEach { layout ->
                assertFalse(text, layout.multiParagraph.didExceedMaxLines)
                assertEquals(text.length, layout.getLineEnd(layout.lineCount - 1))
                repeat(layout.lineCount) { line ->
                    assertFalse(text, layout.isLineEllipsized(line))
                    assertTrue(text, layout.getLineLeft(line) >= 0f)
                    assertTrue(text, layout.getLineRight(line) <= layout.size.width)
                    assertTrue(text, layout.getLineBottom(line) <= layout.size.height)
                }
            }
        }
    }

    @Test fun `goal and recommendation labels remain complete on a narrow screen with large text`() {
        val start = LocalDate.of(2026, 10, 19)
        val plan = buildStudyPlan(PlannerConfig(start, listOf(PlannerCourse(1, 600, start.plusDays(13), 5, 0)), List(7) { 60 }, 30, 240))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                SoloStudyingTheme {
                    // Match the wizard's 16 dp content padding: usable width remains 288 dp.
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        WeeklyStudyGoalEditor("4", {}, true)
                        WeeklyAllocationCard(plan, listOf(BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 600)), emptyList())
                        WeeklyGoalSummary(plan, start)
                    }
                }
            }
        }
        rule.onNodeWithTag("planner_weekly_goal").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        listOf("Weekly goal (hours)", "Recommended weekly split", "Recommended: 4 h",
            "4 h recommended · Priority 5", "Weekly study goal").forEach { text ->
            val layouts = mutableListOf<TextLayoutResult>()
            val node = rule.onNodeWithText(text, useUnmergedTree = true)
            // Scroll the outer text field, not its floating label through the field's
            // horizontal scroll semantics. The label is already visible above.
            if (text != "Weekly goal (hours)") node.performScrollTo()
            node.assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(text, layouts.isNotEmpty())
            layouts.forEach { layout ->
                assertFalse(text, layout.multiParagraph.didExceedMaxLines)
                assertEquals(text.length, layout.getLineEnd(layout.lineCount - 1))
                repeat(layout.lineCount) { line ->
                    assertFalse(text, layout.isLineEllipsized(line))
                    assertTrue(text, layout.getLineLeft(line) >= 0f)
                    assertTrue(text, layout.getLineRight(line) <= layout.size.width)
                    assertTrue(text, layout.getLineBottom(line) <= layout.size.height)
                }
            }
        }
    }
}

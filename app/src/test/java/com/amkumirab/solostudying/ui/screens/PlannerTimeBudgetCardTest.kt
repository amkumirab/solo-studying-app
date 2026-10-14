package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
class PlannerTimeBudgetCardTest {
    @get:Rule val rule = createComposeRule()
    private val start = LocalDate.of(2026, 10, 5)

    @Test fun `early shortage remains visible above collapsed details despite overall spare time`() {
        val config = PlannerConfig(start, listOf(PlannerCourse(1, 120, start, 2, 0),
            PlannerCourse(2, 60, start.plusDays(6), 2, 0)), List(7) { 60 }, 25)
        show(config)
        rule.onNodeWithText("4 h spare overall").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Deadline shortage: 1 h by $start. Later spare time cannot cover this deadline.")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("All deadlines fit this budget.").assertDoesNotExist()
        rule.onNodeWithText("Show deadline breakdown").performScrollTo().performClick()
        rule.onNodeWithText("Finish by $start").performScrollTo().assertIsDisplayed()
    }

    @Test fun `no available days before deadline has no actionable recommendation`() {
        val config = PlannerConfig(start, listOf(PlannerCourse(1, 60, start, 2, 0)),
            listOf(0, 60, 0, 0, 0, 0, 0), 25)
        show(config)
        rule.onNodeWithText("A deadline has no available study days. Change your weekdays or finish date.")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("budget_use_required").assertDoesNotExist()
        rule.onNodeWithText("All deadlines fit this budget.").assertDoesNotExist()
    }

    @Test fun `daily boundaries disable exact shifts instead of silently clamping`() {
        val config = PlannerConfig(start, listOf(PlannerCourse(1, 120, start.plusDays(6), 2, 0)),
            listOf(1, 480, 60, 0, 0, 0, 0), 25)
        show(config)
        rule.onNodeWithTag("budget_increase").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("budget_decrease").performScrollTo().assertIsNotEnabled()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `adjustment controls wrap without text overflow on a narrow screen with larger font`() {
        val config = PlannerConfig(start, listOf(PlannerCourse(1, 6000, start.plusDays(55), 2, 0)),
            listOf(150, 150, 150, 150, 150, 0, 0), 25)
        show(config, fontScale = 1.5f)
        rule.onNodeWithTag("planner_time_budget").assertWidthIsEqualTo(288.dp)
        listOf("budget_decrease", "budget_increase", "budget_reset").forEach { tag ->
            rule.onNodeWithTag(tag).performScrollTo().assertHeightIsAtLeast(48.dp)
        }
        listOf("-15 min / day", "+15 min / day", "150 min per study day").forEach { text ->
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(text, useUnmergedTree = true).performScrollTo()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("No text layout for $text", layouts.isNotEmpty())
            // Paragraph allocation may be wider than the text's painted bounds.
            layouts.forEach { layout ->
                assertFalse("Truncated text: $text", layout.multiParagraph.didExceedMaxLines)
                assertEquals("Missing characters: $text", text.length, layout.getLineEnd(layout.lineCount - 1))
                repeat(layout.lineCount) { line ->
                    assertFalse("Ellipsized text: $text", layout.isLineEllipsized(line))
                    assertTrue("Clipped left edge: $text", layout.getLineLeft(line) >= 0f)
                    assertTrue("Clipped right edge: $text", layout.getLineRight(line) <= layout.size.width)
                    assertTrue("Clipped bottom edge: $text", layout.getLineBottom(line) <= layout.size.height)
                }
            }
        }
    }

    private fun show(config: PlannerConfig, fontScale: Float = 1f) {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                SoloStudyingTheme {
                    // The wizard leaves 16 dp on each side of a 320 dp screen.
                    Column(Modifier.width(288.dp).verticalScroll(rememberScrollState())) {
                        PlannerTimeBudgetCard(config, config, emptyList(), 0, false, {}, {}, {})
                    }
                }
            }
        }
    }
}

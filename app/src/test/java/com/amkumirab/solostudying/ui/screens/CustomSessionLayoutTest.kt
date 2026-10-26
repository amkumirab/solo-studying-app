package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
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
class CustomSessionLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 26)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 120)
    private fun plan(): StudyPlan {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
        return StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(6), 5, 0)), List(7) { 120 }, 30, studyWindows = windows),
            listOf(StudyBlock(7, 1, today, 30, 1020)))
    }
    private fun completeText(tag: String, text: String) {
        rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            assertFalse(layout.multiParagraph.didExceedMaxLines)
            assertEquals(text.length, layout.getLineEnd(layout.lineCount - 1))
            repeat(layout.lineCount) { line ->
                assertFalse(layout.isLineEllipsized(line))
                assertTrue(layout.getLineRight(line) <= layout.size.width)
                assertTrue(layout.getLineBottom(line) <= layout.size.height)
            }
        }
    }

    @Test fun `add fields and confirmation remain readable at large font on a narrow screen`() {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
            SoloStudyingTheme { CustomStudySessionDialog(plan(), listOf(boss), today, today, false, {}, {}) }
        } }
        completeText("custom_start_time", "Start time")
        completeText("custom_minutes", "Minutes")
        rule.onNodeWithTag("confirm_custom_session").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test fun `removal confirmation and returned effort remain reachable with large text`() {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
            SoloStudyingTheme { RemoveStudySessionDialog(plan(), listOf(boss), today, 7, false, {}, {}) }
        } }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("remove_unscheduled").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("remove_unscheduled").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("confirm_remove_session").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}

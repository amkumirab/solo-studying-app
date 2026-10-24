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
class SessionLocksLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test fun `unlock text stays complete and reachable at large font scale`() {
        val today = LocalDate.of(2026, 10, 19)
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1080))) })
        val plan = StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 30, today, 5, 0)), List(7) { 60 }, 30, studyWindows = windows),
            listOf(StudyBlock(7, 1, today, 30, 1020, true)))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                SoloStudyingTheme { StudyPlannerWeek(plan,
                    listOf(BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 30)), today, false, { _, _ -> }, {}) }
            }
        }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_lock_7"))
        rule.onNodeWithTag("planner_lock_7").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        val label = "Unlock session"
        rule.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            assertFalse(layout.multiParagraph.didExceedMaxLines)
            assertEquals(label.length, layout.getLineEnd(layout.lineCount - 1))
            repeat(layout.lineCount) { line ->
                assertFalse(layout.isLineEllipsized(line))
                assertTrue(layout.getLineRight(line) <= layout.size.width)
                assertTrue(layout.getLineBottom(line) <= layout.size.height)
            }
        }
    }
}

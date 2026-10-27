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
class SessionSubjectLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val today = LocalDate.of(2026, 10, 27)
    private val goals = listOf(
        BossEntity(id = 1, name = "Operating Systems and Concurrent Programming", difficulty = "Hard", requiredMinutes = 120),
        BossEntity(id = 2, name = "Electromagnetic Waves and Propagation", difficulty = "Hard", requiredMinutes = 120),
    )
    private fun plan(): StudyPlan {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1140))) })
        return StudyPlan(PlannerConfig(today, listOf(PlannerCourse(1, 120, today.plusDays(6), 5, 0), PlannerCourse(2, 120, today.plusDays(6), 3, 0)), List(7) { 120 }, 30, studyWindows = windows),
            listOf(StudyBlock(7, 1, today, 30, 1020)))
    }
    private fun completeText(text: String) {
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

    @Test fun `long course names preview and confirmation fit narrow screens with large text`() {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
            SoloStudyingTheme { SessionSubjectDialog(plan(), goals, today, 7, false, {}, {}) }
        } }
        rule.onNodeWithTag("session_subject_picker").performScrollTo().assertHeightIsAtLeast(48.dp)
        completeText("New course: ${goals[0].name}")
        rule.onNodeWithTag("session_subject_picker").performClick()
        rule.onNodeWithTag("session_subject_option_2").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("subject_destination_unscheduled").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("subject_destination_unscheduled").performScrollTo().assertIsDisplayed()
        completeText("${goals[1].name} unscheduled work: 120 → 90 min")
        rule.onNodeWithTag("confirm_session_subject").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        completeText("Change subject")
        rule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test fun `weekly subject and removal actions wrap without clipping and retain touch targets`() {
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
            SoloStudyingTheme { StudyPlannerWeek(plan(), goals, today, false, { _, _ -> }, {}) }
        } }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("planner_subject_7"))
        rule.onNodeWithTag("planner_subject_7").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        completeText("Change subject")
        rule.onNodeWithTag("planner_remove_7").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        completeText("Remove session")
    }
}

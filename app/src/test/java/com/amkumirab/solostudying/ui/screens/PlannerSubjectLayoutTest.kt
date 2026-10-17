package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.domain.planner.SubjectColor
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlannerSubjectLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `long subject labels wrap and priority choices remain reachable with larger fonts`() {
        val name = "Operating Systems advanced scheduling and memory management examination revision"
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.5f)) {
                SoloStudyingTheme { Column(Modifier.width(288.dp)) {
                    PlannerSubjectTitle(name, SubjectColor.Green)
                    SubjectPrioritySelector(5, {})
                    SubjectColorSelector(SubjectColor.Green, {})
                } }
            }
        }
        (1..5).forEach {
            rule.onNodeWithTag("subject_priority_$it").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
        listOf(name, "Green").forEach { text ->
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onAllNodesWithText(text, useUnmergedTree = true)[0]
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty())
            layouts.forEach { layout ->
                assertFalse(layout.multiParagraph.didExceedMaxLines)
                assertEquals(text.length, layout.getLineEnd(layout.lineCount - 1))
                repeat(layout.lineCount) { line ->
                    assertFalse(layout.isLineEllipsized(line))
                    assertTrue(layout.getLineLeft(line) >= 0f)
                    assertTrue(layout.getLineRight(line) <= layout.size.width)
                    assertTrue(layout.getLineBottom(line) <= layout.size.height)
                }
            }
        }
    }
}

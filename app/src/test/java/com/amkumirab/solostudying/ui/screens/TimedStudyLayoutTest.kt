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
import com.amkumirab.solostudying.domain.planner.StudyBlock
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
class TimedStudyLayoutTest {
    @get:Rule val rule = createComposeRule()
    @Test fun `time duration and priority stay complete with large text on a narrow screen`() {
        val text = "23:30–24:00 · 30 min · Priority 5"
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                SoloStudyingTheme {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        PlannerSessionTime(StudyBlock(1, 1, LocalDate.of(2026, 10, 19), 30, 1410), 5)
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
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

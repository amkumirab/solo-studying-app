package com.amkumirab.solostudying.ui.screens

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
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h800dp")
class AvailabilityLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `day controls remain readable with a large font on narrow screens`() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                SoloStudyingTheme {
                    AvailabilityDayEditor(0, StudyDayAvailability(true, listOf(StudyTimeWindow(1020, 1080))), false,
                        onSave = {}, onCancel = {}, modifier = Modifier.width(288.dp))
                }
            }
        }
        // The explicit 48 dp thumb keeps both visual and touch bounds large enough.
        rule.onNodeWithTag("window_duration_0").performScrollTo().assertTouchHeightIsEqualTo(48.dp)
        listOf("window_duration_0", "remove_availability_window_0", "add_availability_window", "save_availability_day").forEach {
            try { rule.onNodeWithTag(it).performScrollTo().assertHeightIsAtLeast(48.dp) }
            catch (failure: AssertionError) { throw AssertionError("Control $it: ${failure.message}", failure) }
        }
        listOf("Monday windows", "Daily capacity: 1 h", "Add window", "Cancel day changes").forEach { text ->
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(text, useUnmergedTree = true).performScrollTo()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty())
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

    @Test fun `clock parsing and gap suggestions respect half hours and midnight`() {
        listOf("24:00", "24:30", "9:00", "12:15", "-1:00").forEach { assertNull(availabilityClockMinutes(it)) }
        assertEquals(1440, availabilityClockMinutes("24:00", true))
        assertEquals(0, availabilityClockMinutes("00:00"))
        assertNull(nextAvailabilityWindow(listOf(StudyTimeWindow(540, 1020))))
        val existing = listOf(StudyTimeWindow(1380, 1440))
        val next = nextAvailabilityWindow(existing)!!
        assertTrue(next.endMinute <= 1380)
        assertEquals(0, next.startMinute % 30)
    }
}

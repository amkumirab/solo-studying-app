package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
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
import java.io.File
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
class PlannerBreaksLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val day = LocalDate.of(2026, 10, 26)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 120)
    private fun plan(): StudyPlan {
        val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 660))) })
        return buildStudyPlan(PlannerConfig(day, listOf(PlannerCourse(1, 120, day, 5, 0)), List(7) { 120 }, 60,
            studyWindows = windows, breakMinutes = 10))
    }

    @Test fun `break editor and preview action remain reachable at large font scale`() {
        lateinit var view: View
        rule.setContent {
            val localView = LocalView.current
            SideEffect { view = localView.rootView }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                SoloStudyingTheme { Surface { StudyPlannerWizard(listOf(boss), day, plan(), List(7) { 120 }, false, {}, {}) } }
            }
        }
        repeat(3) { rule.onNodeWithTag("planner_next").performScrollTo().performClick() }
        rule.onNodeWithTag("planner_break_minutes").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        assertComplete("Breaks between sessions")
        capture(view, "editor-large-font")
        rule.onNodeWithTag("planner_next").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        assertComplete("Preview plan")
    }

    @Test fun `break clock label wraps completely without horizontal overflow`() {
        lateinit var view: View
        rule.setContent {
            val localView = LocalView.current
            SideEffect { view = localView.rootView }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                SoloStudyingTheme { Surface { StudyPlannerWeek(plan(), listOf(boss), day, false, { _, _ -> }, {}) } }
            }
        }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("week_break_1"))
        rule.onNodeWithTag("week_break_1").assertIsDisplayed()
        assertComplete("Planned break · 10:00–10:10 · 10 min")
        capture(view, "week-large-font")
    }

    private fun capture(view: View, name: String) = rule.runOnIdle {
        assertTrue(view.width > 0 && view.height > 0)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            val output = File("build/reports/planner-breaks/$name.png")
            check(output.parentFile!!.isDirectory || output.parentFile!!.mkdirs())
            output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
    }

    private fun assertComplete(label: String) {
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue(results.isNotEmpty())
        results.forEach { layout ->
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

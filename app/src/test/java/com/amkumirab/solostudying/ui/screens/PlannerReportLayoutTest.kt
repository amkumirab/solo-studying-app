package com.amkumirab.solostudying.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
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
import java.io.File
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
class PlannerReportLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val monday = LocalDate.of(2026, 10, 26)
    private val name = "Operating Systems and Distributed Computing"
    private val boss = BossEntity(id = 1, name = name, difficulty = "Hard", requiredMinutes = 120)
    private val windows = WeeklyAvailability(List(7) { StudyDayAvailability(true, listOf(StudyTimeWindow(540, 660))) })
    private val plan = buildStudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 120, monday, 5, 0)),
        List(7) { 120 }, 60, studyWindows = windows, breakMinutes = 10))
    private lateinit var view: View

    private fun content(fontScale: Float) = rule.setContent {
        val localView = LocalView.current
        SideEffect { view = localView.rootView }
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            SoloStudyingTheme { Surface {
                WeeklyPlannerReportScreen(plan, listOf(boss), monday, monday, {}, {}, subjects = listOf(
                    PlannerSubject(1, name, 5, SubjectColor.Gold),
                ))
            } }
        }
    }

    @Test fun `narrow large font report retains full labels and touch targets`() {
        content(1.5f)
        rule.onNodeWithTag("report_back").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("report_next_week").performScrollTo().assertHeightIsAtLeast(48.dp)
        complete("Next week")
        rule.onNodeWithTag("report_planned_total").performScrollTo()
        complete("Planned focus: 1 h 50 min")
        capture("narrow-summary")
        rule.onNodeWithTag("report_section_Subjects").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        complete("Subjects", singleLine = true)
        rule.onNodeWithTag("report_course_1").performScrollTo().assertHeightIsAtLeast(48.dp)
        complete(name)
        capture("narrow-subject")
        rule.onNodeWithTag("report_detail").performScrollTo().assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w800dp-h1000dp")
    fun `tablet report shows exact break and session clock labels`() {
        content(1f)
        rule.onNodeWithTag("report_day_0").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("report_detail").performScrollTo().assertTextContains("09:00–10:00", substring = true)
            .assertTextContains("10:10–11:00", substring = true)
        complete("Reserved breaks: 10 min · not study time")
        capture("tablet-days")
    }

    private fun complete(label: String, singleLine: Boolean = false) {
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            if (singleLine) assertEquals("A short tab label must not split inside a word", 1, layout.lineCount)
            assertFalse(layout.multiParagraph.didExceedMaxLines)
            assertEquals(label.length, layout.getLineEnd(layout.lineCount - 1))
            repeat(layout.lineCount) { line ->
                assertFalse(layout.isLineEllipsized(line))
                assertTrue(layout.getLineRight(line) <= layout.size.width)
                assertTrue(layout.getLineBottom(line) <= layout.size.height)
            }
        }
    }

    private fun capture(name: String) = rule.runOnIdle {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            val output = File("build/reports/planner-report/$name.png")
            check(output.parentFile!!.isDirectory || output.parentFile!!.mkdirs())
            output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
    }
}

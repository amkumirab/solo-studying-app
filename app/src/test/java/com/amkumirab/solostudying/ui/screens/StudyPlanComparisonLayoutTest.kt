package com.amkumirab.solostudying.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
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
class StudyPlanComparisonLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val monday = LocalDate.of(2026, 11, 2)
    private val name = "Operating Systems and Concurrent Programming revision"
    private val boss = BossEntity(id = 1, name = name, difficulty = "Hard", requiredMinutes = 120)
    private val windows = defaultWeeklyAvailability(List(7) { 60 })
    private val plan = StudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 120, monday.plusDays(13), 3, 0)),
        List(7) { 60 }, 60, 120, studyWindows = windows).withWeeklyScheduling(emptyList()),
        listOf(StudyBlock(1, 1, monday, 60, 1020, manual = true),
            StudyBlock(2, 1, monday.plusDays(1), 60, 1020, locked = true)))
    private lateinit var view: View

    private fun render(fontScale: Float, short: Boolean = false) {
        rule.setContent {
            val localView = LocalView.current
            SideEffect { view = localView.rootView }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                SoloStudyingTheme { Surface {
                    Box(if (short) Modifier.fillMaxWidth().height(360.dp) else Modifier.fillMaxSize()) {
                        StudyRebuildReview(PlannerSnapshot(plan, availability = windows), listOf(boss), monday,
                            false, {}, {}, { _, _ -> })
                    }
                } }
            }
        }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("rebuild_comparison_ready").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun `narrow large-font review exposes complete values and keeps primary actions reachable`() {
        render(1.5f)
        actions()
        complete("Review schedule changes")
        capture("narrow-top")
        listOf("Compare with accepted plan", "Accepted focus: 2 h", "Proposed remaining focus: 2 h", name).forEach(::complete)
        rule.onNodeWithTag("comparison_details").performScrollTo().performClick()
        rule.onNodeWithTag("comparison_change_0").performScrollTo().assertIsDisplayed()
        capture("narrow-details")
        actions()
    }

    @Test @Config(qualifiers = "w800dp-h1000dp")
    fun `tablet comparison keeps clear accepted and proposed labels without truncation`() {
        render(1f)
        listOf("Review schedule changes", "Compare with accepted plan", "Accepted focus: 2 h", "Proposed remaining focus: 2 h", name).forEach(::complete)
        actions()
        capture("tablet")
    }

    @Test fun `short viewport at large font keeps confirmation and cancel outside the scroll area`() {
        render(1.5f, true)
        actions()
        complete("Confirm", scroll = false)
        complete("Cancel", scroll = false)
        capture("short-primary-actions")
    }

    private fun actions() {
        rule.onNodeWithTag("confirm_rebuild_comparison").assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertIsEnabled()
        rule.onNodeWithTag("cancel_rebuild_comparison").assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertIsEnabled()
    }
    private fun complete(text: String, scroll: Boolean = true) {
        val results = mutableListOf<TextLayoutResult>()
        val node = rule.onNodeWithText(text, useUnmergedTree = true)
        if (scroll) node.performScrollTo()
        node.assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue(results.isNotEmpty())
        results.forEach { result ->
            assertFalse(result.multiParagraph.didExceedMaxLines)
            assertEquals(text.length, result.getLineEnd(result.lineCount - 1))
            repeat(result.lineCount) { line ->
                assertFalse(result.isLineEllipsized(line))
                assertTrue(result.getLineRight(line) <= result.size.width)
                assertTrue(result.getLineBottom(line) <= result.size.height)
            }
        }
    }
    private fun capture(name: String) = rule.runOnIdle {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            val output = File("build/reports/planner-comparison/$name.png")
            check(output.parentFile!!.isDirectory || output.parentFile!!.mkdirs())
            output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
    }
}

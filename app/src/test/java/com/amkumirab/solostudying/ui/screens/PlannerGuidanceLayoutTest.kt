package com.amkumirab.solostudying.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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
class PlannerGuidanceLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val monday = LocalDate.of(2026, 11, 2)
    private val name = "Operating Systems: advanced scheduling, memory management and concurrent processes"
    private val boss = BossEntity(id = 1, name = name, difficulty = "Hard", requiredMinutes = 240)
    private val plan = StudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 240, monday.plusDays(7), 5, 0)),
        List(7) { 120 }, 60, 120), listOf(StudyBlock(1, 1, monday, 60)))
    private lateinit var view: View
    private fun render(fontScale: Float, short: Boolean = false) = rule.setContent {
        val localView = LocalView.current
        SideEffect { view = localView.rootView }
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            SoloStudyingTheme { Surface {
                Column((if (short) Modifier.fillMaxWidth().height(360.dp) else Modifier.fillMaxSize())
                    .verticalScroll(rememberScrollState()).padding(16.dp)) {
                    PlannerGuidanceContent(plan, listOf(boss), monday, monday, onAction = {})
                }
            } }
        }
    }

    @Test fun `long course name and guidance wrap completely on a narrow screen with large text`() {
        render(1.5f)
        complete(rule.onNodeWithTag("guidance_accepted_message_0"))
        val action = rule.onNodeWithTag("guidance_accepted_action_0")
        action.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        complete(rule.onNodeWithText("Review remaining plan", useUnmergedTree = true))
        capture("narrow-large-font")
        rule.onNodeWithTag("guidance_accepted_details").performScrollTo().performClick()
        complete(rule.onNodeWithTag("guidance_accepted_message_1"))
        rule.onNodeWithTag("guidance_accepted_action_1").performScrollTo().assertHeightIsAtLeast(48.dp)
        capture("narrow-expanded")
    }

    @Test @Config(qualifiers = "w800dp-h1000dp")
    fun `tablet guidance retains labelled actions and complete expanded messages`() {
        render(1f)
        complete(rule.onNodeWithTag("guidance_accepted_message_0"))
        rule.onNodeWithTag("guidance_accepted_details").performScrollTo().performClick()
        complete(rule.onNodeWithTag("guidance_accepted_message_1"))
        capture("tablet-expanded")
    }

    @Test fun `short large-font viewport can reach both advice actions without truncation`() {
        render(1.5f, short = true)
        rule.onNodeWithTag("guidance_accepted_action_0").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("guidance_accepted_details").performScrollTo().performClick()
        rule.onNodeWithTag("guidance_accepted_action_1").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        complete(rule.onNodeWithText("Edit plan answers", useUnmergedTree = true))
        capture("short-actions")
    }

    private fun complete(node: SemanticsNodeInteraction) {
        val layouts = mutableListOf<TextLayoutResult>()
        node.performScrollTo().assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val text = node.fetchSemanticsNode().config[SemanticsProperties.Text].single().text
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
    private fun capture(name: String) = rule.runOnIdle {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            val target = File("build/reports/planner-guidance/$name.png")
            check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
            target.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
    }
}

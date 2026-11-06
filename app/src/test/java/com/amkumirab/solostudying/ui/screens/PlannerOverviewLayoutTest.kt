package com.amkumirab.solostudying.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.app.Application
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import com.amkumirab.solostudying.planner.StudyPlannerStore
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import com.amkumirab.solostudying.ui.viewmodel.StudyPlannerViewModel
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlannerOverviewLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val monday = LocalDate.of(2026, 11, 2)
    private val plan = StudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 600, monday.plusDays(13), 5, 0)),
        List(7) { 60 }, 60, 120), listOf(StudyBlock(1, 1, monday, 60), StudyBlock(2, 1, monday.plusDays(1), 30)))
    private lateinit var view: View
    private val app = ApplicationProvider.getApplicationContext<Application>()
    @Before fun reset() { app.deleteSharedPreferences(StudyPlannerStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }

    private fun render(fontScale: Float) = rule.setContent {
        val localView = LocalView.current
        SideEffect { view = localView.rootView }
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            SoloStudyingTheme { Surface { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                OutlinedCard(Modifier.fillMaxWidth()) { PlannerWeekOverviewContent(plan, monday, Modifier.padding(12.dp)) }
            } } }
        }
    }

    @Test fun `narrow overview keeps complete metrics and disclosure at large font scale`() {
        render(1.5f)
        listOf("Weekly overview", "Weekly goal", "Planned focus", "Study budget", "Unallocated budget",
            "1 h 30 min", "5 h 30 min", "30 min still to plan", "75% of goal planned").forEach(::complete)
        rule.onNodeWithTag("overview_details").performScrollTo().assertHeightIsAtLeast(48.dp)
        capture("narrow-large-font")
        rule.onNodeWithTag("overview_details").performClick()
        rule.onNodeWithText("Unallocated budget is not a guaranteed free slot.", substring = true).performScrollTo().assertIsDisplayed()
        capture("narrow-details")
    }

    @Test @Config(qualifiers = "w800dp-h1000dp")
    fun `tablet overview has aligned exact metrics and a labelled goal bar`() {
        render(1f)
        listOf("Weekly goal", "Planned focus", "Study budget", "Unallocated budget", "1 h 30 min", "75% of goal planned").forEach(::complete)
        rule.onNodeWithContentDescription("Weekly goal planned").assertIsDisplayed()
        capture("tablet-overview")
    }

    @Test fun `calendar with no target keeps week navigation and overview reachable at large font scale`() {
        rule.setContent {
            val localView = LocalView.current
            SideEffect { view = localView.rootView }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                SoloStudyingTheme { Surface { StudyPlannerWeek(plan.copy(config = plan.config.copy(weeklyGoalMinutes = null)),
                    listOf(BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 600)),
                    monday, false, { _, _ -> }, {}) } }
            }
        }
        rule.onNodeWithText("Next week").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithTag("overview_goal").performScrollTo().assertTextEquals("Weekly goal", "Not set")
        rule.onNodeWithTag("overview_planned").performScrollTo().assertTextEquals("Planned focus", "0 min")
        capture("calendar-no-goal")
    }

    @Test fun `primary calendar action stays above the overview in a short large-font viewport`() {
        assertTrue(StudyPlannerStore(app).write(plan))
        val model = StudyPlannerViewModel(app)
        rule.setContent {
            val localView = LocalView.current
            SideEffect { view = localView.rootView }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                SoloStudyingTheme { Surface { Column(Modifier.fillMaxWidth().height(360.dp).verticalScroll(rememberScrollState())) {
                    StudyPlannerEntry(emptyList(), monday, List(7) { 60 }, false, { _, _ -> },
                        plannerViewModel = model, showDailySessions = false)
                } } }
            }
        }
        rule.waitUntil(10000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.loaded }
        rule.onNodeWithTag("open_study_planner").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        capture("entry-large-font-primary")
    }

    private fun complete(label: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(label, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
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

    private fun capture(name: String) = rule.runOnIdle {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            val output = File("build/reports/planner-overview/$name.png")
            check(output.parentFile!!.isDirectory || output.parentFile!!.mkdirs())
            output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
    }
}

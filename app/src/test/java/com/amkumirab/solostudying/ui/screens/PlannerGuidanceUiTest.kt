package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlannerGuidanceUiTest {
    @get:Rule val rule = createComposeRule()
    private val monday = LocalDate.of(2026, 11, 2)
    private val boss = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard", requiredMinutes = 240)
    private val plan = StudyPlan(PlannerConfig(monday, listOf(PlannerCourse(1, 240, monday.plusDays(7), 5, 0)),
        List(7) { 120 }, 60, 120), listOf(StudyBlock(1, 1, monday, 60)))

    @OptIn(ExperimentalTestApi::class)
    @Test fun `accepted advice shows actual gap and keyboard disclosure survives restoration`() {
        val restoration = StateRestorationTester(rule)
        var action: PlannerAdviceAction? = null
        restoration.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            PlannerGuidanceContent(plan, listOf(boss), monday, monday, onAction = { action = it })
        } } }
        rule.onNodeWithTag("guidance_accepted_message_0").assertTextContains("3 h", substring = true)
        rule.onNodeWithTag("guidance_accepted_action_0").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(PlannerAdviceAction.ReviewPlan, action)
        rule.onNodeWithTag("guidance_accepted_message_1").assertDoesNotExist()
        val more = rule.onNodeWithTag("guidance_accepted_details")
        more.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        more.performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("guidance_accepted_message_1").performScrollTo().assertTextContains("1 h still to plan", substring = true)
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("guidance_accepted_details").performScrollTo().assertTextEquals("Show less guidance")
        rule.onNodeWithTag("guidance_accepted_message_1").performScrollTo().assertExists()
    }

    @Test fun `busy and active navigation rejects direct accessibility callbacks`() {
        var enabled by mutableStateOf(false)
        var actions = 0
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            PlannerGuidanceContent(plan, listOf(boss), monday, monday, actionsEnabled = enabled, onAction = { actions++ })
        } } }
        val action = rule.onNodeWithTag("guidance_accepted_action_0")
        action.assertIsNotEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
        assertEquals(0, actions)
        rule.runOnIdle { enabled = true }
        action.assertIsEnabled().performClick()
        assertEquals(1, actions)
    }

    @Test fun `proposed advice is labelled read only and reacts to proposed allocations`() {
        var proposed by mutableStateOf(plan)
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            PlannerGuidanceContent(proposed, listOf(boss), monday, monday, proposed = true,
                onAction = { fail("Draft advice must not navigate or save") })
        } } }
        rule.onNodeWithText("Proposed plan guidance").assertExists()
        rule.onNodeWithTag("guidance_proposed_message_0").assertTextContains("3 h", substring = true)
        rule.onAllNodes(hasClickAction() and hasTestTag("guidance_proposed_action_0")).assertCountEquals(0)
        rule.runOnIdle { proposed = plan.copy(blocks = (0..3).map { StudyBlock(it + 1, 1, monday.plusDays(it.toLong()), 60) }) }
        rule.onNodeWithTag("guidance_proposed_message_0").assertTextContains("Remaining work has future slots", substring = true)
    }

    @Test fun `comparison carries read-only proposed guidance and current study removes stale deadline warnings`() {
        var goals by mutableStateOf(listOf(boss))
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyPlanComparisonContent(compareStudyPlans(plan, plan), goals, emptyList(), monday)
        } } }
        rule.onNodeWithTag("guidance_proposed").performScrollTo().assertExists()
        rule.onNodeWithTag("guidance_proposed_message_0").performScrollTo().assertTextContains("Operating Systems", substring = true)
        rule.runOnIdle { goals = listOf(boss.copy(isCompleted = true)) }
        rule.onNodeWithTag("guidance_proposed").assertDoesNotExist()
    }
}

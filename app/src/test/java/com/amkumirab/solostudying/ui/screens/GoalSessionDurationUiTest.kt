package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoalSessionDurationUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `presets and custom duration report only valid minutes`() {
        var selected: Int? = 25
        composeRule.setContent {
            var minutes by remember { mutableStateOf<Int?>(25) }
            SoloStudyingTheme {
                GoalSessionDurationPicker(minutes, 120) { minutes = it; selected = it }
            }
        }
        composeRule.onNodeWithText("45 min").performClick()
        composeRule.runOnIdle { assertEquals(45, selected) }
        composeRule.onNodeWithText("Custom").performClick()
        composeRule.onNodeWithTag("goal_session_custom").performTextReplacement("0")
        composeRule.runOnIdle { assertNull(selected) }
        composeRule.onNodeWithTag("goal_session_custom").performTextReplacement("481")
        composeRule.runOnIdle { assertNull(selected) }
        composeRule.onNodeWithTag("goal_session_custom").performTextReplacement("40")
        composeRule.runOnIdle { assertEquals(40, selected) }
        composeRule.onNodeWithText("Suggested: 60 min").performClick()
        composeRule.runOnIdle { assertEquals(60, selected) }
    }

    @Test fun `preparation requires a valid duration and starts the selected block`() {
        var started: Int? = null
        composeRule.setContent {
            SoloStudyingTheme {
                BeforeTheBattleScreen(
                    boss = BossEntity(name = "Operating Systems", difficulty = "Hard", requiredMinutes = 6000),
                    freeStudyMins = null,
                    selectedSkill = null,
                    userProfile = UserProfileEntity(),
                    onBeginBattle = { started = it },
                    onCancel = {},
                )
            }
        }
        composeRule.onNodeWithText("Custom").performScrollTo().performClick()
        composeRule.onNodeWithTag("goal_session_custom").performScrollTo().performTextReplacement("0")
        composeRule.onNodeWithTag("begin_battle_button").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("goal_session_custom").performScrollTo().performTextReplacement("40")
        composeRule.onNodeWithTag("begin_battle_button").performScrollTo().assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(40, started) }
    }
}

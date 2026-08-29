package com.amkumirab.solostudying.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.domain.session.ProgressSummary
import com.amkumirab.solostudying.domain.session.SessionEndState
import com.amkumirab.solostudying.domain.session.SessionSummary
import com.amkumirab.solostudying.notification.ReminderSettings
import com.amkumirab.solostudying.sound.SoundSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccessibilitySemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `bottom navigation exposes ordered labels and active battle state`() {
        composeRule.setContent {
            MaterialTheme {
                RPGBottomBar(
                    currentTab = Tab.Dungeons,
                    onTabSelected = {},
                    isBattleActive = true,
                )
            }
        }

        composeRule.onNodeWithTag("bottom_nav_dungeons")
            .assertContentDescriptionEquals("Dungeons")
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TraversalIndex,
                    0f,
                ),
            )

        composeRule.onNodeWithTag("bottom_nav_battle")
            .assertContentDescriptionEquals("Battle, study session active")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TraversalIndex,
                    1f,
                ),
            )
    }

    @Test
    fun `system controls expose labels states and minimum touch targets`() {
        var breakSuggestionsUpdate: Boolean? = null
        composeRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        SystemControlsCard(
                            soundSettings = SoundSettings(enabled = true, volume = 0.7f),
                            onSoundEnabledChange = {},
                            onSoundVolumeChange = {},
                            onPreviewSound = {},
                            onReplayTutorial = {},
                            breakSuggestionsEnabled = false,
                            onBreakSuggestionsEnabledChange = { breakSuggestionsUpdate = it },
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("⚙️ SYSTEM CONTROLS")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeRule.onNodeWithTag("sound_enabled_switch")
            .assertContentDescriptionEquals("Interface sound")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "On",
                ),
            )
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag("sound_volume_slider")
            .assertContentDescriptionEquals("Master volume")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "70 percent",
                ),
            )
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag("sound_preview_button")
            .assertContentDescriptionEquals("Preview interface sound")
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag("replay_tutorial_button")
            .assertContentDescriptionEquals("Replay onboarding tutorial")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag("break_suggestions_switch")
            .performScrollTo()
            .assertContentDescriptionEquals("Post-session break suggestions")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Off",
                ),
            )
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(true, breakSuggestionsUpdate) }
    }

    @Test
    fun `disabled sound settings announce their state and disable audio actions`() {
        composeRule.setContent {
            MaterialTheme {
                SystemControlsCard(
                    soundSettings = SoundSettings(enabled = false, volume = 0.7f),
                    onSoundEnabledChange = {},
                    onSoundVolumeChange = {},
                    onPreviewSound = {},
                    onReplayTutorial = {},
                )
            }
        }

        composeRule.onNodeWithTag("sound_enabled_switch")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Off",
                ),
            )

        composeRule.onNodeWithTag("sound_volume_slider").assertIsNotEnabled()
        composeRule.onNodeWithTag("sound_preview_button").assertIsNotEnabled()
    }

    @Test
    fun `reminder controls expose time state and independent switches`() {
        val settings = ReminderSettings()
        var updatedSettings: ReminderSettings? = null
        composeRule.setContent {
            MaterialTheme {
                ReminderSettingsCard(
                    settings = settings,
                    onSettingsChange = { updatedSettings = it },
                    onPreview = {},
                )
            }
        }

        composeRule.onNodeWithTag("morning_reminder_switch")
            .assertContentDescriptionEquals("MORNING QUEST reminder")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "On",
                ),
            )
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag("morning_reminder_time_button")
            .assertContentDescriptionEquals("Set MORNING QUEST time. Current time 09:00")

        composeRule.onNodeWithTag("morning_reminder_preview_button")
            .assertContentDescriptionEquals("Preview MORNING QUEST reminder")

        composeRule.onNodeWithTag("morning_reminder_switch").performClick()
        composeRule.runOnIdle {
            assertEquals(false, updatedSettings?.morning?.enabled)
            assertEquals(settings.beforeStudy, updatedSettings?.beforeStudy)
            assertEquals(settings.evening, updatedSettings?.evening)
        }
    }

    @Test
    fun `session summary exposes results and reachable actions`() {
        var doneClicks = 0
        var startClicks = 0
        var breakClicks = 0
        val summary = SessionSummary(
            sessionId = 8L,
            subject = "Physics Revision",
            durationSeconds = 1_500L,
            xpEarned = 80,
            goldEarned = 35,
            endState = SessionEndState.Completed,
            bossProgress = ProgressSummary("Physics Revision", 300L, 1_800L, 3_000L),
            previousLevel = 3,
            currentLevel = 4,
            previousStreak = 4,
            currentStreak = 5,
            skillUnlocked = true,
        )

        composeRule.setContent {
            MaterialTheme {
                SessionSummaryDialog(
                    summary = summary,
                    onDone = { doneClicks++ },
                    onStartAnotherSession = { startClicks++ },
                    onTakeBreak = { breakClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag("session_summary_dialog")
            .assertContentDescriptionEquals("SESSION COMPLETE for Physics Revision")
        composeRule.onNodeWithText("+80").assertIsEnabled()
        composeRule.onNodeWithText("3 → 4").assertIsEnabled()

        composeRule.onNodeWithTag("session_summary_take_break")
            .assertHeightIsAtLeast(48.dp)
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("session_summary_start_another")
            .assertHeightIsAtLeast(48.dp)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("session_summary_done")
            .assertHeightIsAtLeast(48.dp)
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, startClicks)
            assertEquals(1, doneClicks)
            assertEquals(1, breakClicks)
        }
    }

    @Test
    fun `break duration picker exposes presets and cancellation`() {
        var selectedMinutes: Int? = null
        var cancelClicks = 0
        composeRule.setContent {
            MaterialTheme {
                BreakDurationDialog(
                    onDismiss = { cancelClicks++ },
                    onStartBreak = { selectedMinutes = it },
                )
            }
        }

        composeRule.onNodeWithTag("break_duration_dialog")
            .assertContentDescriptionEquals("Choose break duration")
        composeRule.onNodeWithTag("break_10_minutes")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag("break_duration_cancel")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(10, selectedMinutes)
            assertEquals(1, cancelClicks)
        }
    }

    @Test
    fun `active break announces remaining time and exposes skip action`() {
        var skipClicks = 0
        composeRule.setContent {
            MaterialTheme {
                BreakTimerDialog(
                    durationSeconds = 300L,
                    remainingSeconds = 245L,
                    onSkipBreak = { skipClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag("break_timer_dialog")
            .assertContentDescriptionEquals("Break timer, 04:05 remaining")
        composeRule.onNodeWithText("04:05").assertIsEnabled()
        composeRule.onNodeWithTag("skip_break_button")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, skipClicks) }
    }

    @Test
    fun `completed break offers the next session`() {
        var nextClicks = 0
        composeRule.setContent {
            MaterialTheme {
                BreakCompleteDialog(
                    onDone = {},
                    onStartNextSession = { nextClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag("break_complete_dialog")
            .assertContentDescriptionEquals("Break complete")
        composeRule.onNodeWithTag("break_start_next_session")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, nextClicks) }
    }
}

package com.amkumirab.solostudying.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.SkillEntity
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.domain.session.ProgressSummary
import com.amkumirab.solostudying.domain.session.SessionEndState
import com.amkumirab.solostudying.domain.session.SessionSummary
import com.amkumirab.solostudying.domain.quest.QuestPriority
import com.amkumirab.solostudying.notification.ReminderSettings
import com.amkumirab.solostudying.quickstart.QuickStartSelection
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
    fun `battle actions stay readable reachable and independent`() {
        var pauseClicks = 0
        var finishClicks = 0
        var retreatClicks = 0
        composeRule.setContent {
            MaterialTheme {
                BattleActionControls(
                    isPaused = false,
                    isFreeStudy = false,
                    onPause = { pauseClicks++ },
                    onResume = {},
                    onFinish = { finishClicks++ },
                    onRetreat = { retreatClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag("battle_pause_resume_button")
            .assertHeightIsAtLeast(48.dp)
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithText("PAUSE TIMER").assertExists()

        composeRule.onNodeWithTag("battle_finish_button")
            .assertHeightIsAtLeast(48.dp)
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithText("CONQUER").assertExists()

        composeRule.onNodeWithTag("battle_retreat_button")
            .assertHeightIsAtLeast(48.dp)
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithText("RETREAT").assertExists()

        composeRule.runOnIdle {
            assertEquals(1, pauseClicks)
            assertEquals(1, finishClicks)
            assertEquals(1, retreatClicks)
        }
    }

    @Test
    fun `paused battle exposes a clear resume action`() {
        var resumeClicks = 0
        composeRule.setContent {
            MaterialTheme {
                BattleActionControls(
                    isPaused = true,
                    isFreeStudy = true,
                    onPause = {},
                    onResume = { resumeClicks++ },
                    onFinish = {},
                    onRetreat = {},
                )
            }
        }

        composeRule.onNodeWithText("RESUME TIMER").assertExists()
        composeRule.onNodeWithText("FINISH").assertExists()
        composeRule.onNodeWithTag("battle_pause_resume_button").performClick()
        composeRule.runOnIdle { assertEquals(1, resumeClicks) }
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

    @Test
    fun `study insights expose range controls and chart summary`() {
        val session = StudySessionEntity(
            bossId = 1,
            bossName = "Physics",
            durationSeconds = 1_800L,
            xpEarned = 20,
            goldEarned = 10,
            timestamp = System.currentTimeMillis(),
            wasCompleted = true,
        )

        composeRule.setContent {
            MaterialTheme {
                StudyInsightsCard(
                    sessions = listOf(session),
                    profile = UserProfileEntity(),
                )
            }
        }

        composeRule.onNodeWithTag("study_insights_card").assertExists()
        composeRule.onNodeWithTag("insights_range_Last7Days").assertIsSelected()
        composeRule.onNodeWithTag("insights_chart")
            .assertContentDescriptionEquals("7 focus periods. 10 percent of target completed")

        composeRule.onNodeWithTag("insights_range_AllTime").performClick().assertIsSelected()
        composeRule.onNodeWithText("Top subject: Physics · 30m").assertExists()
    }

    @Test
    fun `quick start updates presets and starts with the selected skill`() {
        val selection = mutableStateOf(QuickStartSelection(durationMinutes = 25))
        var startedWith: QuickStartSelection? = null
        var customClicks = 0
        val physics = SkillEntity(
            id = 7,
            name = "Physics",
            targetMinutes = 600,
        )

        composeRule.setContent {
            MaterialTheme {
                QuickStartCard(
                    skills = listOf(physics),
                    selection = selection.value,
                    isSessionActive = false,
                    onSelectionChange = { selection.value = it },
                    onStart = { startedWith = it },
                    onCustomDuration = { customClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag("quick_start_duration_25").assertIsSelected()
        composeRule.onNodeWithTag("quick_start_duration_45").performClick().assertIsSelected()
        composeRule.onNodeWithTag("quick_start_skill_selector").performClick()
        composeRule.onNodeWithTag("quick_start_skill_7").performClick()
        composeRule.onNodeWithTag("quick_start_skill_selector")
            .assertContentDescriptionEquals("Skill focus, Physics")
        composeRule.onNodeWithTag("quick_start_button")
            .assertContentDescriptionEquals("Start 45 minute focus session for Physics")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag("quick_start_custom").performClick()

        composeRule.runOnIdle {
            assertEquals(QuickStartSelection(durationMinutes = 45, skillId = 7), startedWith)
            assertEquals(1, customClicks)
        }
    }

    @Test
    fun `daily quest board exposes progress actions and a validated editor`() {
        val quest = DailyQuestEntity(
            id = 12,
            title = "Solve physics problems",
            durationMinutes = 25,
            scheduledDate = "2026-09-01",
            priority = QuestPriority.High.value,
        )
        var toggled: Pair<Int, Boolean>? = null
        var startedQuestId: Int? = null
        var createdQuest: Pair<String, Int>? = null

        composeRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        DailyQuestBoard(
                            quests = listOf(quest),
                            allQuests = listOf(quest),
                            skills = emptyList(),
                            today = "2026-09-01",
                            activeQuestId = null,
                            isSessionActive = false,
                            onCreateQuest = { title, duration, _, _ ->
                                createdQuest = title to duration
                            },
                            onUpdateQuest = { _, _, _, _, _ -> },
                            onSetCompleted = { selected, completed ->
                                toggled = selected.id to completed
                            },
                            onDeleteQuest = {},
                            onStartQuest = { startedQuestId = it.id },
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("daily_quest_progress")
            .assertContentDescriptionEquals("0 of 1 daily quests completed")
        composeRule.onNodeWithTag("toggle_daily_quest_12").performClick()
        composeRule.onNodeWithTag("start_daily_quest_12").performClick()
        composeRule.runOnIdle {
            assertEquals(12 to true, toggled)
            assertEquals(12, startedQuestId)
        }

        composeRule.onNodeWithTag("daily_quest_history_button").performClick()
        composeRule.onNodeWithTag("daily_quest_history_dialog").assertExists()
        composeRule.onNodeWithText("CLOSE").performClick()

        composeRule.onNodeWithTag("add_daily_quest_button").performClick()
        composeRule.onNodeWithTag("daily_quest_editor").assertExists()
        composeRule.onNodeWithTag("daily_quest_title_input").performTextInput("Read chapter four")
        composeRule.onNodeWithTag("daily_quest_duration_45").performClick()
        composeRule.onNodeWithTag("save_daily_quest_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule.runOnIdle {
            assertEquals("Read chapter four" to 45, createdQuest)
        }
    }
}

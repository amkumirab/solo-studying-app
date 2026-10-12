package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.notification.ReminderSettings
import com.amkumirab.solostudying.sound.SoundSettings
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h800dp")
class AppOrganizationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun `bottom destinations select independently and session return reports paused state`() {
        var selected by mutableStateOf(Tab.Today)
        var paused by mutableStateOf(false)
        var returns = 0
        rule.setContent { SoloStudyingTheme { Column {
            ActiveSessionBar(paused, "Operating Systems", { returns++ })
            RPGBottomBar(selected, { selected = it }, true)
        } } }
        listOf("today", "dungeons", "shop", "stats").forEach { name ->
            rule.onNodeWithTag("bottom_nav_$name").performClick().assertIsSelected().assertHeightIsAtLeast(48.dp)
        }
        rule.onNodeWithTag("bottom_nav_battle").assertDoesNotExist()
        rule.onNodeWithText("Session in progress").assertExists()
        rule.runOnIdle { paused = true }
        rule.onNodeWithText("Session paused").assertExists()
        rule.onNodeWithTag("return_to_session").performClick()
        rule.runOnIdle { assertEquals(1, returns) }
    }

    @Test fun `Study exposes Goals Focus and Plan without duplicating Today`() {
        rule.setContent { SoloStudyingTheme { DungeonSectionTabs(DungeonSection.Goals, {}) } }
        rule.onNodeWithTag("study_hub_goals").assertIsSelected()
        rule.onNodeWithTag("study_hub_focus").assertExists()
        rule.onNodeWithTag("study_hub_plan").assertExists()
        rule.onNodeWithTag("study_hub_today").assertDoesNotExist()
    }

    @Test fun `Profile separates skills history and settings from overview`() {
        rule.setContent { SoloStudyingTheme { ProfileUnderTest() } }
        rule.onNodeWithTag("profile_sections").assertExists()
        rule.onNodeWithTag("profile_overview").assertIsSelected()
        rule.onNodeWithTag("profile_settings").performClick()
        rule.onNodeWithText("Settings").assertExists()
        rule.onNodeWithTag("profile_sections").assertDoesNotExist()
        rule.onNodeWithTag("back_to_profile").performClick()
        rule.onNodeWithTag("profile_skills").performClick()
        rule.onNodeWithText("No skills yet").assertExists()
        rule.onNodeWithTag("profile_history").performClick()
        rule.onNodeWithText("No sessions yet. Complete a study session to see it here.").assertExists()
        rule.onNodeWithTag("sound_enabled_switch").assertDoesNotExist()
    }

    @Test fun `profile section and daily task expansion survive restoration`() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { SoloStudyingTheme { Column {
            ExpandableStudySection("Daily tasks", "Two tasks") { TextForTest() }
            Box(Modifier.weight(1f)) { ProfileUnderTest() }
        } } }
        rule.onNodeWithTag("daily_tasks_toggle").performClick()
        rule.onNodeWithTag("profile_history").performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("profile_history").assertIsSelected()
        rule.onNodeWithText("Task details").assertExists()
        rule.onNodeWithTag("daily_tasks_toggle").performClick()
        rule.onNodeWithText("Task details").assertDoesNotExist()
    }

    @Test fun `settings editing remains reachable with large fonts on narrow screens`() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                SoloStudyingTheme { ProfileUnderTest() }
            }
        }
        rule.onNodeWithTag("profile_settings").assertIsDisplayed().assertWidthIsAtLeast(48.dp).performClick()
        rule.onNodeWithText("Edit schedule").assertIsDisplayed().performClick()
        rule.onNodeWithText("Edit study schedule").assertExists()
    }

    @Composable private fun TextForTest() { androidx.compose.material3.Text("Task details") }

    @Test fun `skill editor preserves entered values in creation callback`() {
        var created: Triple<String, Int, String>? = null
        rule.setContent { SoloStudyingTheme { ProfileUnderTest { name, minutes, description -> created = Triple(name, minutes, description) } } }
        rule.onNodeWithTag("profile_skills").performClick()
        rule.onNodeWithText("Add skill").assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithText("Skill name").performTextInput("Operating Systems")
        rule.onNodeWithText("Target minutes").performTextReplacement("25")
        rule.onNodeWithText("Description").performTextReplacement("Memory management")
        rule.onNode(hasText("Add skill") and hasAnyAncestor(isDialog())).performClick()
        rule.runOnIdle { assertEquals(Triple("Operating Systems", 25, "Memory management"), created) }
        rule.onNodeWithText("Skill name").assertDoesNotExist()
    }

    @Composable private fun ProfileUnderTest(onCreateSkill: (String, Int, String) -> Unit = { _, _, _ -> }) {
        StatsTab(
            profile = UserProfileEntity(), sessions = emptyList(), skills = emptyList(),
            onCreateSkill = onCreateSkill, onDeleteSkill = {},
            onUpdateSchedule = { _, _, _, _ -> }, onTriggerSimulatedNotification = {},
            reminderSettings = ReminderSettings(), onReminderSettingsChange = {},
            soundSettings = SoundSettings(), onSoundEnabledChange = {}, onSoundVolumeChange = {},
            onPreviewSound = {}, onReplayTutorial = {}, breakSuggestionsEnabled = true,
            onBreakSuggestionsEnabledChange = {}, focusShieldEnabled = false, focusShieldHasAccess = false,
            onFocusShieldEnabledChange = {}, onRequestFocusShieldAccess = {}, onUpdateSessionNote = { _, _ -> },
        )
    }
}

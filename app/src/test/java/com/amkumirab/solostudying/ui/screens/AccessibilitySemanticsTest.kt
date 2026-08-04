package com.amkumirab.solostudying.ui.screens

import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.sound.SoundSettings
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
    fun `sound controls expose labels states and minimum touch targets`() {
        composeRule.setContent {
            MaterialTheme {
                SystemControlsCard(
                    soundSettings = SoundSettings(enabled = true, volume = 0.7f),
                    onSoundEnabledChange = {},
                    onSoundVolumeChange = {},
                    onPreviewSound = {},
                    onReplayTutorial = {},
                )
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
            .assertHeightIsAtLeast(48.dp)
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
}

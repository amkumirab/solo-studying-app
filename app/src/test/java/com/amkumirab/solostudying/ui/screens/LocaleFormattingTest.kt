package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.intl.LocaleList
import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en-rUS-w360dp-h800dp")
class LocaleFormattingTest {
    @get:Rule val rule = createComposeRule()
    @Test fun `goal progress follows observable locale changes`() {
        val language = mutableStateOf("de-DE")
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.Locales(LocaleList(language.value))) {
                SoloStudyingTheme {
                    BossCard(BossEntity(id = 1, name = "Physics", difficulty = "Medium",
                        requiredMinutes = 120, timeSpentSeconds = 3600L),
                        activeBoss = null, isBattleActive = false,
                        onFightBoss = {}, onDeleteBoss = {}, onConquerRealBoss = {})
                }
            }
        }
        rule.onNodeWithText("50,0% DMG").assertExists()
        rule.runOnIdle { language.value = "en-US" }
        rule.onNodeWithText("50.0% DMG").assertExists()
    }
}

package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
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
class TimerAlarmScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun `focus completion is readable with large text and dismissal stops the alert`() {
        var dismissed = false
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
            SoloStudyingTheme { TimerAlarmScreen(false, false, { dismissed = true }, {}) }
        } }
        rule.onNodeWithText("Focus complete").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("timer_alarm_dismiss").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(dismissed)
    }

    @Test fun `break preview is clearly marked and open app is a separate explicit action`() {
        var opened = false
        rule.setContent { SoloStudyingTheme { TimerAlarmScreen(true, true, {}, { opened = true }) } }
        rule.onNodeWithText("Test alert").assertIsDisplayed()
        rule.onNodeWithText("Break complete").assertIsDisplayed()
        rule.onNodeWithTag("timer_alarm_open_app").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(opened)
    }
}

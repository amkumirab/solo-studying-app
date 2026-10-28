package com.amkumirab.solostudying.ui.screens

import android.app.Application
import android.app.NotificationManager
import android.media.AudioManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.notification.TimerAlarmAlerts
import com.amkumirab.solostudying.notification.TimerAlarmSettingsStore
import com.amkumirab.solostudying.notification.TimerCompletionFeedback
import com.amkumirab.solostudying.ui.theme.SoloStudyingTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimerAlarmSettingsTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    @Before fun reset() {
        TimerAlarmAlerts.dismissCurrent(app)
        app.deleteSharedPreferences(TimerAlarmSettingsStore.PREFERENCES_NAME)
        app.deleteSharedPreferences(TimerCompletionFeedback.PREFERENCES_NAME)
        val manager = app.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        app.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_SILENT
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }
    @After fun cleanup() { reset() }

    @Test fun `switch persists opt in and preview does not complete a timer`() {
        rule.setContent { SoloStudyingTheme { Column(Modifier.verticalScroll(rememberScrollState())) { TimerAlarmSettingsCard() } } }
        rule.onNodeWithTag("timer_alarm_enabled").assertIsOff().performClick().assertIsOn()
        assertTrue(TimerAlarmSettingsStore(app).isEnabled())
        rule.onNodeWithTag("timer_alarm_test").performScrollTo().performClick()
        assertTrue(TimerAlarmAlerts.events.value!!.isTest)
        assertTrue(app.getSharedPreferences(TimerCompletionFeedback.PREFERENCES_NAME, 0).all.isEmpty())
        rule.onNodeWithTag("timer_alarm_enabled").performScrollTo().performClick()
        assertNull(TimerAlarmAlerts.events.value)
        assertFalse(TimerAlarmSettingsStore(app).isEnabled())
    }
}

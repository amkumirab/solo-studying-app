package com.amkumirab.solostudying.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimerAlarmActivityTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    @Before fun reset() {
        TimerAlarmAlerts.dismissCurrent(app)
        app.deleteSharedPreferences(TimerAlarmSettingsStore.PREFERENCES_NAME)
        app.deleteSharedPreferences(TimerCompletionFeedback.PREFERENCES_NAME)
        shadowOf(app).grantPermissions(Manifest.permission.VIBRATE, Manifest.permission.POST_NOTIFICATIONS)
        val manager = app.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        app.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_SILENT
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }
    @After fun cleanup() { TimerAlarmAlerts.dismissCurrent(app); app.deleteSharedPreferences(TimerAlarmSettingsStore.PREFERENCES_NAME) }

    @Test fun `invalid and stale screen intents finish without creating an alert`() {
        val controller = Robolectric.buildActivity(TimerAlarmActivity::class.java,
            Intent(app, TimerAlarmActivity::class.java).setData(TimerAlarmAlerts.eventUri("stale"))).create()
        assertTrue(controller.get().isFinishing)
        assertNull(TimerAlarmAlerts.events.value)
        controller.destroy()
    }

    @Test fun `restoring an active alert preserves its identity and expiry without claiming a timer`() {
        assertTrue(TimerAlarmAlerts.begin(app, CompletedTimer.BREAK, isTest = true))
        val before = TimerAlarmAlerts.events.value!!
        val controller = Robolectric.buildActivity(TimerAlarmActivity::class.java, TimerAlarmAlerts.screenIntent(app, before)).create()
        assertFalse(controller.get().isFinishing)
        assertEquals(before, TimerAlarmAlerts.events.value)
        val info = app.packageManager.getActivityInfo(ComponentName(app, TimerAlarmActivity::class.java), 0)
        assertFalse(info.exported)
        assertTrue(app.getSharedPreferences(TimerCompletionFeedback.PREFERENCES_NAME, 0).all.isEmpty())
        controller.destroy()
    }

    @Test @Config(sdk = [24, 26]) fun `older Android uses bounded screen on window flags without unlocking the phone`() {
        assertTrue(TimerAlarmAlerts.begin(app, CompletedTimer.FOCUS, isTest = true))
        val controller = Robolectric.buildActivity(TimerAlarmActivity::class.java,
            TimerAlarmAlerts.screenIntent(app, TimerAlarmAlerts.events.value!!)).create()
        @Suppress("DEPRECATION") // Assert only the supported legacy lock-screen path.
        val expected = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        assertEquals(expected, controller.get().window.attributes.flags and expected)
        assertFalse(controller.get().isFinishing)
        controller.destroy()
    }
}

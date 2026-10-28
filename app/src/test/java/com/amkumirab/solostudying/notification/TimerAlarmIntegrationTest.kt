package com.amkumirab.solostudying.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.breaks.BreakSessionSnapshot
import com.amkumirab.solostudying.breaks.BreakSessionStore
import com.amkumirab.solostudying.focus.FocusSessionSnapshot
import com.amkumirab.solostudying.focus.FocusSessionStore
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimerAlarmIntegrationTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val manager = app.getSystemService(NotificationManager::class.java)
    @Before fun reset() {
        TimerAlarmAlerts.dismissCurrent(app)
        listOf(TimerAlarmSettingsStore.PREFERENCES_NAME, TimerCompletionFeedback.PREFERENCES_NAME,
            FocusSessionStore.PREFERENCES_NAME, BreakSessionStore.PREFERENCES_NAME).forEach { app.deleteSharedPreferences(it) }
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.VIBRATE)
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        app.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_SILENT
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        manager.cancelAll()
        TimerAlarmSettingsStore(app).setEnabled(true)
    }
    @After fun cleanup() { TimerAlarmAlerts.dismissCurrent(app); reset(); app.deleteSharedPreferences(TimerAlarmSettingsStore.PREFERENCES_NAME) }

    @Test fun `background focus emits one alarm card rather than a second completion notification`() {
        FocusSessionStore(app).write(FocusSessionSnapshot(true, true, false, 1, 59, 0,
            System.currentTimeMillis() - 2000, null, null))
        val intent = Intent(app, FocusSessionActionReceiver::class.java).setAction(FocusSessionActionReceiver.ACTION_TIMER_ELAPSED)
        FocusSessionActionReceiver().onReceive(app, intent)
        val first = TimerAlarmAlerts.events.value!!
        FocusSessionActionReceiver().onReceive(app, intent)
        assertEquals(first, TimerAlarmAlerts.events.value)
        assertEquals(CompletedTimer.FOCUS, first.timer)
        assertEquals(1, shadowOf(manager).allNotifications.size)
        assertNull(shadowOf(manager).getNotification(FocusSessionNotifier.COMPLETED_NOTIFICATION_ID))
    }

    @Test fun `background break shows only the bounded alarm and ignores premature delivery`() {
        BreakSessionStore(app).saveSession(BreakSessionSnapshot(60, System.currentTimeMillis() + 60000))
        val intent = Intent(app, NotificationReceiver::class.java).setAction(NotificationReceiver.ACTION_BREAK_COMPLETE)
        NotificationReceiver().onReceive(app, intent)
        assertNull(TimerAlarmAlerts.events.value)
        BreakSessionStore(app).saveSession(BreakSessionSnapshot(60, System.currentTimeMillis() - 1))
        NotificationReceiver().onReceive(app, intent)
        assertEquals(CompletedTimer.BREAK, TimerAlarmAlerts.events.value!!.timer)
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }
}

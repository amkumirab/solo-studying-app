package com.amkumirab.solostudying.notification

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimerAlarmSchedulingTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val manager = app.getSystemService(AlarmManager::class.java)
    @Before fun reset() { app.deleteSharedPreferences(TimerAlarmSettingsStore.PREFERENCES_NAME) }
    @After fun cleanup() { reset() }
    private fun operation() = PendingIntent.getBroadcast(app, 991,
        Intent(app, TimerAlarmDismissReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    @Test fun `alarm style requests exact delivery only with user access`() {
        TimerAlarmSettingsStore(app).setEnabled(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduleTimerCompletion(app, System.currentTimeMillis() + 60_000, operation())
        assertEquals(ShadowAlarmManager.WINDOW_EXACT, shadowOf(manager).scheduledAlarms.single().windowLengthMs)
        manager.cancel(operation())
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduleTimerCompletion(app, System.currentTimeMillis() + 60_000, operation())
        assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, shadowOf(manager).scheduledAlarms.single().windowLengthMs)
    }

    @Test fun `disabled alarm style retains inexact scheduling even with access`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduleTimerCompletion(app, System.currentTimeMillis() + 60_000, operation())
        assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, shadowOf(manager).scheduledAlarms.single().windowLengthMs)
    }
}

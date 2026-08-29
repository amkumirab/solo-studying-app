package com.amkumirab.solostudying.breaks

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.ui.viewmodel.BreakViewModel
import com.amkumirab.solostudying.notification.NotificationReceiver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BreakSessionStoreTest {

    private lateinit var context: Context
    private lateinit var store: BreakSessionStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteSharedPreferences(BreakSessionStore.PREFERENCES_NAME)
        ShadowAlarmManager.reset()
        store = BreakSessionStore(context)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(BreakSessionStore.PREFERENCES_NAME)
        ShadowAlarmManager.reset()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    @Test
    fun `break session and suggestion preference survive store recreation`() {
        val session = BreakSessionSnapshot(durationSeconds = 600L, endTimeMillis = 900_000L)

        store.saveSession(session)
        store.setSuggestionsEnabled(false)

        val restoredStore = BreakSessionStore(context)
        assertEquals(session, restoredStore.readSession())
        assertFalse(restoredStore.areSuggestionsEnabled())

        restoredStore.clearSession()
        assertNull(restoredStore.readSession())
        assertFalse(restoredStore.areSuggestionsEnabled())
    }

    @Test
    fun `remaining time rounds partial seconds and never exceeds duration`() {
        val session = BreakSessionSnapshot(durationSeconds = 300L, endTimeMillis = 400_000L)

        assertEquals(300L, session.remainingSeconds(nowMillis = 0L))
        assertEquals(1L, session.remainingSeconds(nowMillis = 399_001L))
        assertEquals(0L, session.remainingSeconds(nowMillis = 400_000L))
    }

    @Test
    fun `break view model schedules restores completes and skips`() {
        var nowMillis = System.currentTimeMillis()
        val firstViewModel = BreakViewModel(context, store) { nowMillis }

        firstViewModel.startBreak(5)

        assertEquals(300L, firstViewModel.breakTimeLeftSeconds)
        assertEquals(300L, store.readSession()?.durationSeconds)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)

        nowMillis += 125_000L
        val restoredViewModel = BreakViewModel(context, BreakSessionStore(context)) { nowMillis }
        assertEquals(175L, restoredViewModel.breakTimeLeftSeconds)
        assertTrue(restoredViewModel.activeBreak != null)

        nowMillis += 175_000L
        restoredViewModel.syncBreakTime()
        assertNull(restoredViewModel.activeBreak)
        assertTrue(restoredViewModel.showBreakComplete)
        assertNull(store.readSession())

        restoredViewModel.dismissBreakComplete()
        restoredViewModel.startBreak(10)
        restoredViewModel.skipBreak()
        assertNull(restoredViewModel.activeBreak)
        assertFalse(restoredViewModel.showBreakComplete)
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `break completion receiver posts a recovery notification`() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationReceiver().onReceive(
            context,
            Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_BREAK_COMPLETE
            },
        )

        val notifications = shadowOf(notificationManager).allNotifications
        assertEquals(1, notifications.size)
        assertEquals(
            "Break complete",
            notifications.single().extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
        )
    }
}

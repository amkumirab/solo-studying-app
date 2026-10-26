package com.amkumirab.solostudying.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.breaks.BreakSessionSnapshot
import com.amkumirab.solostudying.breaks.BreakSessionStore
import com.amkumirab.solostudying.focus.FocusSessionSnapshot
import com.amkumirab.solostudying.focus.FocusSessionStore
import com.amkumirab.solostudying.ui.viewmodel.BreakViewModel
import java.time.Duration
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimerCompletionIntegrationTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val notifications = app.getSystemService(NotificationManager::class.java)
    private val players = mutableListOf<MediaPlayer>()
    @Before fun reset() {
        listOf(FocusSessionStore.PREFERENCES_NAME, BreakSessionStore.PREFERENCES_NAME, TimerCompletionFeedback.PREFERENCES_NAME)
            .forEach { app.deleteSharedPreferences(it) }
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.VIBRATE)
        shadowOf(notifications).setNotificationPolicyAccessGranted(true)
        app.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_NORMAL
        notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        notifications.cancelAll()
        ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(800, 0) }
        ShadowMediaPlayer.setCreateListener { player, _ -> players += player }
    }
    @After fun cleanup() {
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
        FocusSessionNotifier.cancelCompletionAlarm(app)
        NotificationHelper.cancelBreakAlarm(app)
        notifications.cancelAll()
        listOf(FocusSessionStore.PREFERENCES_NAME, BreakSessionStore.PREFERENCES_NAME, TimerCompletionFeedback.PREFERENCES_NAME)
            .forEach { app.deleteSharedPreferences(it) }
    }
    private fun elapsedFocus(): FocusSessionSnapshot = FocusSessionSnapshot(true, true, false, 1, 59, 0,
        System.currentTimeMillis() - 2_000L, null, null)
    private fun focusAlarm() = FocusSessionActionReceiver().onReceive(app,
        Intent(app, FocusSessionActionReceiver::class.java).setAction(FocusSessionActionReceiver.ACTION_TIMER_ELAPSED))
    private fun breakAlarm() = NotificationReceiver().onReceive(app,
        Intent(app, NotificationReceiver::class.java).setAction(NotificationReceiver.ACTION_BREAK_COMPLETE))

    @Suppress("DEPRECATION") // Inspect pre-channel fields to prove the compatibility notification is silent too.
    @Test fun `background focus completion chimes once and its notification cannot make a second sound`() {
        FocusSessionStore(app).write(elapsedFocus())
        focusAlarm(); focusAlarm()
        assertEquals(1, players.size)
        val notification = shadowOf(notifications).getNotification(FocusSessionNotifier.COMPLETED_NOTIFICATION_ID)
        assertNotNull(notification)
        assertNull(notification.sound)
        assertNull(notification.vibrate)
    }

    @Test fun `notification denial does not discard timer feedback`() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        FocusSessionStore(app).write(elapsedFocus())
        focusAlarm()
        assertEquals(1, players.size)
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
    }

    @Test fun `background and foreground break completion share one persisted event`() {
        var now = System.currentTimeMillis()
        val model = BreakViewModel(app, clock = { now })
        model.startBreak(1)
        val session = BreakSessionStore(app).readSession()!!
        BreakSessionStore(app).saveSession(session.copy(endTimeMillis = System.currentTimeMillis() - 1))
        // Use the expired stored session for a restored foreground instance too.
        breakAlarm()
        val restored = BreakViewModel(app)
        assertTrue(restored.showBreakComplete)
        assertEquals(1, players.size)
        model.skipBreak()
    }

    @Test fun `foreground break completion alerts once while skip remains silent`() {
        var now = System.currentTimeMillis()
        val model = BreakViewModel(app, clock = { now })
        model.startBreak(1)
        now += 60_000L
        model.syncBreakTime(); model.syncBreakTime()
        assertTrue(model.showBreakComplete)
        assertEquals(1, players.size)
        model.startBreak(1)
        model.skipBreak()
        breakAlarm()
        assertEquals(1, players.size)
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
    }

    @Test fun `a premature or cancelled break alarm cannot claim completion`() {
        BreakSessionStore(app).saveSession(BreakSessionSnapshot(60, System.currentTimeMillis() + 60_000))
        breakAlarm()
        assertTrue(players.isEmpty())
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
        BreakSessionStore(app).clearSession()
        breakAlarm()
        assertTrue(players.isEmpty())
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
    }
}

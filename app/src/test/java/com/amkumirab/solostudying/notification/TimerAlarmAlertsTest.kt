package com.amkumirab.solostudying.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Looper
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
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
class TimerAlarmAlertsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val manager = app.getSystemService(NotificationManager::class.java)
    private val vibrator get() = app.getSystemService(VibratorManager::class.java).defaultVibrator
    private val players = mutableListOf<MediaPlayer>()
    @Before fun reset() {
        TimerAlarmAlerts.dismissCurrent(app)
        app.deleteSharedPreferences(TimerAlarmSettingsStore.PREFERENCES_NAME)
        app.deleteSharedPreferences(TimerCompletionFeedback.PREFERENCES_NAME)
        shadowOf(app).grantPermissions(Manifest.permission.VIBRATE, Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        app.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_SILENT
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        shadowOf(vibrator).setHasVibrator(true)
        manager.cancelAll()
        ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(800, 0) }
        ShadowMediaPlayer.setCreateListener { player, _ -> players += player }
    }
    @After fun cleanup() {
        TimerAlarmAlerts.dismissCurrent(app)
        app.deleteSharedPreferences(TimerAlarmSettingsStore.PREFERENCES_NAME)
        app.deleteSharedPreferences(TimerCompletionFeedback.PREFERENCES_NAME)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31))
    }
    private fun signal(timer: CompletedTimer = CompletedTimer.FOCUS) = TimerCompletionFeedback.signal(app, timer, System.currentTimeMillis())

    @Test fun `alarm screen is opt in and enabling survives a new settings instance`() {
        assertFalse(TimerAlarmSettingsStore(app).isEnabled())
        signal()
        assertNull(TimerAlarmAlerts.events.value)
        assertTrue(TimerAlarmSettingsStore(app).setEnabled(true))
        assertTrue(TimerAlarmSettingsStore(app).isEnabled())
    }

    @Test fun `silent completion has a bounded longer vibration and a silent high importance card`() {
        TimerAlarmSettingsStore(app).setEnabled(true)
        assertTrue(signal())
        val active = TimerAlarmAlerts.events.value!!
        assertEquals(CompletedTimer.FOCUS, active.timer)
        assertTrue(active.expiresAtMillis - System.currentTimeMillis() in 1..30_000)
        val pattern = shadowOf(vibrator).pattern!!
        assertTrue(pattern.sum() > 15_000)
        assertTrue(pattern.sum() <= 30_000)
        assertEquals(-1, shadowOf(vibrator).repeat)
        assertTrue(players.isEmpty())
        val notification = shadowOf(manager).getNotification(TimerAlarmAlerts.NOTIFICATION_ID)
        assertNotNull(notification)
        assertNotNull(notification.contentIntent)
        assertEquals("Focus complete", notification.extras.getString("android.title"))
        val channel = manager.getNotificationChannel(TimerAlarmAlerts.CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertNull(channel.sound)
        assertFalse(channel.shouldVibrate())
        assertFalse(channel.canBypassDnd())
    }

    @Test fun `dismiss stops vibration and a stale dismiss cannot stop the next alarm`() {
        TimerAlarmSettingsStore(app).setEnabled(true)
        signal()
        val old = TimerAlarmAlerts.events.value!!
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        signal(CompletedTimer.BREAK)
        val next = TimerAlarmAlerts.events.value!!
        assertNotEquals(old.token, next.token)
        assertFalse(TimerAlarmAlerts.dismiss(app, old.token))
        assertEquals(next, TimerAlarmAlerts.events.value)
        TimerAlarmDismissReceiver().onReceive(app, Intent(app, TimerAlarmDismissReceiver::class.java).setData(TimerAlarmAlerts.eventUri(next.token)))
        assertNull(TimerAlarmAlerts.events.value)
        assertFalse(shadowOf(vibrator).isVibrating)
        assertNull(shadowOf(manager).getNotification(TimerAlarmAlerts.NOTIFICATION_ID))
    }

    @Test fun `duplicate completion cannot restart the screen and watchdog clears output`() {
        TimerAlarmSettingsStore(app).setEnabled(true)
        val end = System.currentTimeMillis()
        TimerCompletionFeedback.signal(app, CompletedTimer.BREAK, end)
        val active = TimerAlarmAlerts.events.value
        assertFalse(TimerCompletionFeedback.signal(app, CompletedTimer.BREAK, end))
        assertEquals(active, TimerAlarmAlerts.events.value)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31))
        assertNull(TimerAlarmAlerts.events.value)
        assertFalse(shadowOf(vibrator).isVibrating)
        TimerAlarmAlerts.restore(app)
        assertNull(TimerAlarmAlerts.events.value)
    }

    @Test fun `denied notifications do not remove the foreground screen or vibration`() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        TimerAlarmSettingsStore(app).setEnabled(true)
        signal()
        assertNotNull(TimerAlarmAlerts.events.value)
        assertTrue(shadowOf(vibrator).isVibrating)
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test fun `do not disturb suppresses alarm screen sound and vibration`() {
        TimerAlarmSettingsStore(app).setEnabled(true)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        assertTrue(signal())
        assertNull(TimerAlarmAlerts.events.value)
        assertFalse(shadowOf(vibrator).isVibrating)
        assertTrue(players.isEmpty())
        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE, manager.currentInterruptionFilter)
    }

    @Test fun `late restored completion never opens a new intrusive screen`() {
        TimerAlarmSettingsStore(app).setEnabled(true)
        TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, System.currentTimeMillis() - 600_000)
        assertNull(TimerAlarmAlerts.events.value)
    }
}

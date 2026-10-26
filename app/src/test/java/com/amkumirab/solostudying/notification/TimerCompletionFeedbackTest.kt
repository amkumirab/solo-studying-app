package com.amkumirab.solostudying.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Vibrator
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.sound.SoundSettings
import com.amkumirab.solostudying.sound.SoundSettingsStore
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
class TimerCompletionFeedbackTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val audio = app.getSystemService(AudioManager::class.java)
    private val notifications = app.getSystemService(NotificationManager::class.java)
    private val vibrator get() = if (android.os.Build.VERSION.SDK_INT >= 31)
        app.getSystemService(VibratorManager::class.java).defaultVibrator else app.getSystemService(Vibrator::class.java)
    private val players = mutableListOf<MediaPlayer>()

    @Before fun reset() {
        app.deleteSharedPreferences(TimerCompletionFeedback.PREFERENCES_NAME)
        app.deleteSharedPreferences(SoundSettingsStore.PREFERENCES_NAME)
        shadowOf(app).grantPermissions(Manifest.permission.VIBRATE)
        shadowOf(notifications).setNotificationPolicyAccessGranted(true)
        notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        shadowOf(vibrator).setHasVibrator(true)
        ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(800, 0) }
        ShadowMediaPlayer.setCreateListener { player, _ -> players += player }
    }
    @After fun cleanup() {
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
        app.deleteSharedPreferences(TimerCompletionFeedback.PREFERENCES_NAME)
        app.deleteSharedPreferences(SoundSettingsStore.PREFERENCES_NAME)
        notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    @Test fun `normal mode plays a scaled themed chime and one non repeating pulse pattern`() {
        SoundSettingsStore(app).write(SoundSettings(volume = 0.5f))
        assertTrue(TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, 1000))
        val player = players.single()
        assertTrue(player.isPlaying)
        assertEquals(AudioAttributes.USAGE_NOTIFICATION_EVENT, shadowOf(player).audioAttributes.usage)
        assertEquals(0.39f, shadowOf(player).leftVolume, 0.001f)
        assertArrayEquals(longArrayOf(0, 90, 80, 90, 140, 320), shadowOf(vibrator).pattern)
        assertEquals(-1, shadowOf(vibrator).repeat)
        shadowOf(player).invokeCompletionListener()
        assertEquals(ShadowMediaPlayer.State.END, shadowOf(player).state)
    }

    @Test fun `silent and vibrate modes use only the distinctive vibration`() {
        listOf(AudioManager.RINGER_MODE_SILENT, AudioManager.RINGER_MODE_VIBRATE).forEachIndexed { index, mode ->
            audio.ringerMode = mode
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            assertTrue(TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, index + 1000L))
            assertTrue(shadowOf(vibrator).isVibrating)
        }
        assertTrue(players.isEmpty())
    }

    @Test fun `disabled and zero volume app sound leave vibration available`() {
        listOf(SoundSettings(enabled = false), SoundSettings(volume = 0f)).forEachIndexed { index, settings ->
            SoundSettingsStore(app).write(settings)
            TimerCompletionFeedback.signal(app, CompletedTimer.BREAK, index + 1000L)
            assertTrue(shadowOf(vibrator).isVibrating)
        }
        assertTrue(players.isEmpty())
    }

    @Test fun `do not disturb is never overridden and suppressed events are not replayed later`() {
        listOf(NotificationManager.INTERRUPTION_FILTER_NONE, NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_ALARMS).forEachIndexed { index, filter ->
            notifications.setInterruptionFilter(filter)
            assertTrue(TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, index + 1000L))
            assertFalse(shadowOf(vibrator).isVibrating)
            assertEquals(filter, notifications.currentInterruptionFilter)
        }
        notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        assertFalse(TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, 1002))
        assertTrue(players.isEmpty())
    }

    @Test fun `completion claims survive store recreation and focus and break have separate slots`() {
        audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        assertTrue(TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, 1000))
        assertFalse(TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, 1000))
        assertTrue(TimerCompletionFeedback.signal(app, CompletedTimer.BREAK, 1000))
        assertFalse(TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, 1000))
        assertEquals(2, app.getSharedPreferences(TimerCompletionFeedback.PREFERENCES_NAME, Context.MODE_PRIVATE).all.size)
        assertTrue(TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, 2000))
    }

    @Test fun `no vibrator does not prevent a chime and errors release the player`() {
        shadowOf(vibrator).setHasVibrator(false)
        TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, 1000)
        val player = players.single()
        shadowOf(player).invokeErrorListener(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        assertEquals(ShadowMediaPlayer.State.END, shadowOf(player).state)
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test fun `a stalled player is released by the bounded watchdog`() {
        ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(60_000, 0) }
        TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, 1000)
        val player = players.single()
        assertTrue(player.isPlaying)
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
        assertEquals(ShadowMediaPlayer.State.END, shadowOf(player).state)
    }

    @Test @Config(sdk = [24, 26]) fun `legacy Android retains the same non repeating pattern`() {
        audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        TimerCompletionFeedback.signal(app, CompletedTimer.FOCUS, 1000)
        assertArrayEquals(longArrayOf(0, 90, 80, 90, 140, 320), shadowOf(vibrator).pattern)
        assertEquals(-1, shadowOf(vibrator).repeat)
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            assertEquals(AudioAttributes.USAGE_ALARM, shadowOf(vibrator).audioAttributesFromLastVibration!!.usage)
        }
    }
}

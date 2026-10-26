package com.amkumirab.solostudying.notification

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.amkumirab.solostudying.R
import com.amkumirab.solostudying.sound.SoundSettingsStore

internal enum class CompletedTimer { FOCUS, BREAK }

/** Timer feedback is independent of notification permission and never changes device policy. */
internal object TimerCompletionFeedback {
    const val PREFERENCES_NAME = "solo_studying_timer_feedback"
    private val playing = mutableSetOf<MediaPlayer>()

    @Synchronized
    fun signal(context: Context, timer: CompletedTimer, endTimeMillis: Long): Boolean {
        if (endTimeMillis <= 0L) return false
        // Claim even when DND suppresses feedback, so reopening does not replay an old alert.
        if (!claimCompletion(context, timer, endTimeMillis)) return false
        val notifications = context.getSystemService(NotificationManager::class.java)
        if (notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return true
        vibrate(context)
        val audio = context.getSystemService(AudioManager::class.java)
        val settings = SoundSettingsStore(context).read()
        if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL && settings.enabled && settings.volume > 0f) {
            playChime(context.applicationContext, settings.volume * 0.78f)
        }
        return true
    }

    private fun claimCompletion(context: Context, timer: CompletedTimer, endTimeMillis: Long): Boolean {
        val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (preferences.getLong(timer.name, -1L) == endTimeMillis) return false
        val editor = preferences.edit()
        editor.putLong(timer.name, endTimeMillis)
        return editor.commit()
    }

    @Suppress("DEPRECATION") // Compatibility paths for the supported Android 7–12 devices.
    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
            else context.getSystemService(Vibrator::class.java)
        if (!vibrator.hasVibrator()) return
        val pattern = longArrayOf(0, 90, 80, 90, 140, 320)
        try {
            when {
                Build.VERSION.SDK_INT >= 33 -> vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1),
                    VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_ALARM).build())
                Build.VERSION.SDK_INT >= 26 -> vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                else -> vibrator.vibrate(pattern, -1, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        } catch (exception: SecurityException) {
            Log.w("TimerCompletionFeedback", "Timer vibration is unavailable", exception)
        }
    }

    private fun playChime(context: Context, volume: Float) {
        var player: MediaPlayer? = null
        try {
            val created = MediaPlayer.create(context, R.raw.session_complete, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(), 0) ?: return
            player = created
            playing += created
            val handler = Handler(Looper.getMainLooper())
            val cleanup = Runnable { synchronized(this) { if (playing.remove(created)) created.release() } }
            created.setOnCompletionListener { handler.removeCallbacks(cleanup); cleanup.run() }
            created.setOnErrorListener { _, _, _ -> handler.removeCallbacks(cleanup); cleanup.run(); true }
            created.setVolume(volume, volume)
            // Bound the lifetime even if a device fails to report playback completion.
            handler.postDelayed(cleanup, 5_000L)
            created.start()
        } catch (exception: RuntimeException) {
            player?.let { playing.remove(it); it.release() }
            Log.w("TimerCompletionFeedback", "Timer chime is unavailable", exception)
        }
    }
}

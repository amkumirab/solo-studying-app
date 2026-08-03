package com.amkumirab.solostudying.sound

import android.content.Context
import androidx.core.content.edit

data class SoundSettings(
    val enabled: Boolean = true,
    val volume: Float = 1f,
)

internal class SoundSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun read(): SoundSettings = SoundSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, true),
        volume = normalizeVolume(preferences.getFloat(KEY_VOLUME, 1f)),
    )

    fun write(settings: SoundSettings) {
        preferences.edit {
            putBoolean(KEY_ENABLED, settings.enabled)
            putFloat(KEY_VOLUME, normalizeVolume(settings.volume))
        }
    }

    private fun normalizeVolume(volume: Float): Float =
        if (volume.isFinite()) volume.coerceIn(0f, 1f) else 1f

    internal companion object {
        const val PREFERENCES_NAME = "solo_studying_sound_settings"
        private const val KEY_ENABLED = "sound_enabled"
        private const val KEY_VOLUME = "sound_volume"
    }
}

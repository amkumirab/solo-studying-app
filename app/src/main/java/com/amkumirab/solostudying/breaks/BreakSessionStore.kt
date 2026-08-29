package com.amkumirab.solostudying.breaks

import android.content.Context
import androidx.core.content.edit

data class BreakSessionSnapshot(
    val durationSeconds: Long,
    val endTimeMillis: Long,
)

fun BreakSessionSnapshot.remainingSeconds(nowMillis: Long): Long {
    val remainingMillis = (endTimeMillis - nowMillis).coerceAtLeast(0L)
    return ((remainingMillis + 999L) / 1_000L).coerceAtMost(durationSeconds)
}

class BreakSessionStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun readSession(): BreakSessionSnapshot? {
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return null
        val durationSeconds = preferences.getLong(KEY_DURATION_SECONDS, 0L)
        val endTimeMillis = preferences.getLong(KEY_END_TIME_MILLIS, 0L)
        if (durationSeconds <= 0L || endTimeMillis <= 0L) return null
        return BreakSessionSnapshot(durationSeconds, endTimeMillis)
    }

    fun saveSession(session: BreakSessionSnapshot) {
        preferences.edit {
            putBoolean(KEY_ACTIVE, true)
            putLong(KEY_DURATION_SECONDS, session.durationSeconds)
            putLong(KEY_END_TIME_MILLIS, session.endTimeMillis)
        }
    }

    fun clearSession() {
        preferences.edit {
            remove(KEY_ACTIVE)
            remove(KEY_DURATION_SECONDS)
            remove(KEY_END_TIME_MILLIS)
        }
    }

    fun areSuggestionsEnabled(): Boolean = preferences.getBoolean(KEY_SUGGESTIONS_ENABLED, true)

    fun setSuggestionsEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_SUGGESTIONS_ENABLED, enabled) }
    }

    companion object {
        const val PREFERENCES_NAME = "solo_studying_break_prefs"

        private const val KEY_ACTIVE = "break_active"
        private const val KEY_DURATION_SECONDS = "break_duration_seconds"
        private const val KEY_END_TIME_MILLIS = "break_end_time_millis"
        private const val KEY_SUGGESTIONS_ENABLED = "break_suggestions_enabled"
    }
}

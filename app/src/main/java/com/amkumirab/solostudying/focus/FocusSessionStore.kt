package com.amkumirab.solostudying.focus

import android.content.Context
import androidx.core.content.edit
import kotlin.math.min

data class FocusSessionSnapshot(
    val isActive: Boolean,
    val isFreeStudy: Boolean,
    val isPaused: Boolean,
    val timeLeftSeconds: Long,
    val timeSpentSeconds: Long,
    val initialBossTimeSpentSeconds: Long,
    val lastTickTimeMillis: Long,
    val bossId: Int?,
    val skillId: Int?,
)

class FocusSessionStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): FocusSessionSnapshot? {
        val isActive = preferences.getBoolean(KEY_ACTIVE, false)
        val isFreeStudy = preferences.getBoolean(KEY_FREE_STUDY, false)
        if (!isActive && !isFreeStudy) return null

        return FocusSessionSnapshot(
            isActive = true,
            isFreeStudy = isFreeStudy,
            isPaused = preferences.getBoolean(KEY_PAUSED, false),
            timeLeftSeconds = preferences.getLong(KEY_TIME_LEFT, 0L).coerceAtLeast(0L),
            timeSpentSeconds = preferences.getLong(KEY_TIME_SPENT, 0L).coerceAtLeast(0L),
            initialBossTimeSpentSeconds = preferences
                .getLong(KEY_INITIAL_BOSS_TIME_SPENT, 0L)
                .coerceAtLeast(0L),
            lastTickTimeMillis = preferences.getLong(KEY_LAST_TICK, 0L),
            bossId = preferences.getInt(KEY_BOSS_ID, NO_ID).takeUnless { it == NO_ID },
            skillId = preferences.getInt(KEY_SKILL_ID, NO_ID).takeUnless { it == NO_ID },
        )
    }

    fun write(snapshot: FocusSessionSnapshot) {
        preferences.edit {
            putBoolean(KEY_ACTIVE, snapshot.isActive)
            putBoolean(KEY_FREE_STUDY, snapshot.isFreeStudy)
            putBoolean(KEY_PAUSED, snapshot.isPaused)
            putLong(KEY_TIME_LEFT, snapshot.timeLeftSeconds.coerceAtLeast(0L))
            putLong(KEY_TIME_SPENT, snapshot.timeSpentSeconds.coerceAtLeast(0L))
            putLong(
                KEY_INITIAL_BOSS_TIME_SPENT,
                snapshot.initialBossTimeSpentSeconds.coerceAtLeast(0L),
            )
            putLong(KEY_LAST_TICK, snapshot.lastTickTimeMillis)
            putInt(KEY_BOSS_ID, snapshot.bossId ?: NO_ID)
            putInt(KEY_SKILL_ID, snapshot.skillId ?: NO_ID)
        }
    }

    fun clear() {
        preferences.edit { clear() }
    }

    companion object {
        const val PREFERENCES_NAME = "solo_studying_battle_prefs"

        private const val KEY_ACTIVE = "session_battle_active"
        private const val KEY_FREE_STUDY = "session_free_active"
        private const val KEY_PAUSED = "session_battle_paused"
        private const val KEY_TIME_LEFT = "session_time_left"
        private const val KEY_TIME_SPENT = "session_time_spent"
        private const val KEY_INITIAL_BOSS_TIME_SPENT = "session_boss_spent_initial"
        private const val KEY_LAST_TICK = "session_last_tick"
        private const val KEY_BOSS_ID = "session_boss_id"
        private const val KEY_SKILL_ID = "session_skill_id"
        private const val NO_ID = -1
    }
}

fun reconcileFocusSession(
    snapshot: FocusSessionSnapshot,
    nowMillis: Long,
): FocusSessionSnapshot {
    if (!snapshot.isActive || snapshot.isPaused || snapshot.lastTickTimeMillis <= 0L) {
        return snapshot
    }

    val elapsedMillis = (nowMillis - snapshot.lastTickTimeMillis).coerceAtLeast(0L)
    val elapsedSeconds = elapsedMillis / 1_000L
    if (elapsedSeconds <= 0L) return snapshot

    val appliedSeconds = min(elapsedSeconds, snapshot.timeLeftSeconds)
    return snapshot.copy(
        timeLeftSeconds = snapshot.timeLeftSeconds - appliedSeconds,
        timeSpentSeconds = snapshot.timeSpentSeconds + appliedSeconds,
        lastTickTimeMillis = snapshot.lastTickTimeMillis + appliedSeconds * 1_000L,
    )
}

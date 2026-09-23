package com.amkumirab.solostudying.focuscycle

import android.content.Context
import androidx.core.content.edit

data class FocusCyclePlan(
    val focusMinutes: Int,
    val breakMinutes: Int,
    val totalRounds: Int,
    val skillId: Int? = null,
) {
    init {
        require(focusMinutes in MIN_FOCUS_MINUTES..MAX_FOCUS_MINUTES)
        require(breakMinutes in MIN_BREAK_MINUTES..MAX_BREAK_MINUTES)
        require(totalRounds in MIN_ROUNDS..MAX_ROUNDS)
    }

    companion object {
        const val MIN_FOCUS_MINUTES = 1
        const val MAX_FOCUS_MINUTES = 480
        const val MIN_BREAK_MINUTES = 1
        const val MAX_BREAK_MINUTES = 60
        const val MIN_ROUNDS = 2
        const val MAX_ROUNDS = 12

        val STANDARD = FocusCyclePlan(25, 5, 4)
        val DEEP_WORK = FocusCyclePlan(50, 10, 2)
    }
}

enum class FocusCyclePhase {
    FOCUS,
    READY_FOR_BREAK,
    BREAK,
    READY_FOR_FOCUS,
    COMPLETE,
}

data class FocusCycleState(
    val plan: FocusCyclePlan,
    val currentRound: Int = 1,
    val completedRounds: Int = 0,
    val focusedSeconds: Long = 0L,
    val phase: FocusCyclePhase = FocusCyclePhase.FOCUS,
    val lastSessionId: Long = -1L,
) {
    val nextRound: Int get() = (completedRounds + 1).coerceAtMost(plan.totalRounds)
}

class FocusCycleStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun read(): FocusCycleState? {
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return null
        return runCatching {
            FocusCycleState(
                plan = FocusCyclePlan(
                    focusMinutes = preferences.getInt(KEY_FOCUS_MINUTES, 25),
                    breakMinutes = preferences.getInt(KEY_BREAK_MINUTES, 5),
                    totalRounds = preferences.getInt(KEY_TOTAL_ROUNDS, 4),
                    skillId = preferences.getInt(KEY_SKILL_ID, NO_SKILL).takeUnless { it == NO_SKILL },
                ),
                currentRound = preferences.getInt(KEY_CURRENT_ROUND, 1),
                completedRounds = preferences.getInt(KEY_COMPLETED_ROUNDS, 0),
                focusedSeconds = preferences.getLong(KEY_FOCUSED_SECONDS, 0L),
                phase = FocusCyclePhase.valueOf(
                    preferences.getString(KEY_PHASE, FocusCyclePhase.FOCUS.name)
                        ?: FocusCyclePhase.FOCUS.name,
                ),
                lastSessionId = preferences.getLong(KEY_LAST_SESSION_ID, -1L),
            )
        }.getOrElse {
            clear()
            null
        }
    }

    fun write(state: FocusCycleState) {
        preferences.edit {
            putBoolean(KEY_ACTIVE, true)
            putInt(KEY_FOCUS_MINUTES, state.plan.focusMinutes)
            putInt(KEY_BREAK_MINUTES, state.plan.breakMinutes)
            putInt(KEY_TOTAL_ROUNDS, state.plan.totalRounds)
            putInt(KEY_SKILL_ID, state.plan.skillId ?: NO_SKILL)
            putInt(KEY_CURRENT_ROUND, state.currentRound)
            putInt(KEY_COMPLETED_ROUNDS, state.completedRounds)
            putLong(KEY_FOCUSED_SECONDS, state.focusedSeconds)
            putString(KEY_PHASE, state.phase.name)
            putLong(KEY_LAST_SESSION_ID, state.lastSessionId)
        }
    }

    fun clear() = preferences.edit { clear() }

    companion object {
        const val PREFERENCES_NAME = "solo_studying_focus_cycle"
        private const val NO_SKILL = -1
        private const val KEY_ACTIVE = "active"
        private const val KEY_FOCUS_MINUTES = "focus_minutes"
        private const val KEY_BREAK_MINUTES = "break_minutes"
        private const val KEY_TOTAL_ROUNDS = "total_rounds"
        private const val KEY_SKILL_ID = "skill_id"
        private const val KEY_CURRENT_ROUND = "current_round"
        private const val KEY_COMPLETED_ROUNDS = "completed_rounds"
        private const val KEY_FOCUSED_SECONDS = "focused_seconds"
        private const val KEY_PHASE = "phase"
        private const val KEY_LAST_SESSION_ID = "last_session_id"
    }
}

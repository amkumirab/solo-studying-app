package com.amkumirab.solostudying.quickstart

import android.content.Context
import androidx.core.content.edit

val QUICK_START_PRESET_MINUTES = listOf(15, 25, 45, 60)

data class QuickStartSelection(
    val durationMinutes: Int = DEFAULT_QUICK_START_MINUTES,
    val skillId: Int? = null,
)

class QuickStartPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun read(): QuickStartSelection = normalizeQuickStartSelection(
        durationMinutes = preferences.getInt(KEY_DURATION_MINUTES, DEFAULT_QUICK_START_MINUTES),
        skillId = preferences.getInt(KEY_SKILL_ID, NO_SKILL_ID).takeIf { it > 0 },
    )

    fun save(selection: QuickStartSelection) {
        val normalized = normalizeQuickStartSelection(
            durationMinutes = selection.durationMinutes,
            skillId = selection.skillId,
        )
        preferences.edit {
            putInt(KEY_DURATION_MINUTES, normalized.durationMinutes)
            putInt(KEY_SKILL_ID, normalized.skillId ?: NO_SKILL_ID)
        }
    }

    companion object {
        const val PREFERENCES_NAME = "solo_studying_quick_start"
        const val MIN_DURATION_MINUTES = 1
        const val MAX_DURATION_MINUTES = 480
        private const val KEY_DURATION_MINUTES = "duration_minutes"
        private const val KEY_SKILL_ID = "skill_id"
        private const val NO_SKILL_ID = -1
    }
}

fun normalizeQuickStartSelection(
    durationMinutes: Int,
    skillId: Int?,
): QuickStartSelection = QuickStartSelection(
    durationMinutes = durationMinutes.coerceIn(
        QuickStartPreferences.MIN_DURATION_MINUTES,
        QuickStartPreferences.MAX_DURATION_MINUTES,
    ),
    skillId = skillId?.takeIf { it > 0 },
)

private const val DEFAULT_QUICK_START_MINUTES = 25

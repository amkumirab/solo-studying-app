package com.amkumirab.solostudying.notification

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReminderSchedule(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
) {
    init {
        require(hour in 0..23) { "Reminder hour must be between 0 and 23" }
        require(minute in 0..59) { "Reminder minute must be between 0 and 59" }
    }

    fun formattedTime(): String =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

data class ReminderSettings(
    val morning: ReminderSchedule = ReminderSchedule(enabled = true, hour = 9, minute = 0),
    val beforeStudy: ReminderSchedule = ReminderSchedule(enabled = true, hour = 18, minute = 0),
    val evening: ReminderSchedule = ReminderSchedule(enabled = true, hour = 21, minute = 0),
)

class ReminderSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val mutableSettings = MutableStateFlow(readFromPreferences())

    val settings: StateFlow<ReminderSettings> = mutableSettings.asStateFlow()

    fun save(settings: ReminderSettings) {
        preferences.edit {
            putSchedule(MORNING_PREFIX, settings.morning)
            putSchedule(BEFORE_STUDY_PREFIX, settings.beforeStudy)
            putSchedule(EVENING_PREFIX, settings.evening)
        }
        mutableSettings.value = settings
    }

    fun read(): ReminderSettings = readFromPreferences()

    private fun readFromPreferences(): ReminderSettings {
        val defaults = ReminderSettings()
        return ReminderSettings(
            morning = preferences.readSchedule(MORNING_PREFIX, defaults.morning),
            beforeStudy = preferences.readSchedule(BEFORE_STUDY_PREFIX, defaults.beforeStudy),
            evening = preferences.readSchedule(EVENING_PREFIX, defaults.evening),
        )
    }

    private fun android.content.SharedPreferences.Editor.putSchedule(
        prefix: String,
        schedule: ReminderSchedule,
    ) = putBoolean("${prefix}_enabled", schedule.enabled)
        .putInt("${prefix}_hour", schedule.hour)
        .putInt("${prefix}_minute", schedule.minute)

    private fun android.content.SharedPreferences.readSchedule(
        prefix: String,
        default: ReminderSchedule,
    ): ReminderSchedule {
        val hour = getInt("${prefix}_hour", default.hour).coerceIn(0, 23)
        val minute = getInt("${prefix}_minute", default.minute).coerceIn(0, 59)
        return ReminderSchedule(
            enabled = getBoolean("${prefix}_enabled", default.enabled),
            hour = hour,
            minute = minute,
        )
    }

    companion object {
        const val PREFERENCES_NAME = "solo_studying_reminder_settings"
        private const val MORNING_PREFIX = "morning"
        private const val BEFORE_STUDY_PREFIX = "before_study"
        private const val EVENING_PREFIX = "evening"
    }
}

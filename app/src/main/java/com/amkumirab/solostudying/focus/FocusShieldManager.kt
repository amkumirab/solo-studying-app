package com.amkumirab.solostudying.focus

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.edit

interface InterruptionFilterController {
    fun hasPolicyAccess(): Boolean
    fun currentFilter(): Int
    fun setFilter(filter: Int): Boolean
}

private class AndroidInterruptionFilterController(context: Context) : InterruptionFilterController {
    private val notificationManager = context.applicationContext
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun hasPolicyAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    override fun currentFilter(): Int = notificationManager.currentInterruptionFilter

    override fun setFilter(filter: Int): Boolean = runCatching {
        notificationManager.setInterruptionFilter(filter)
    }.isSuccess
}

class FocusShieldManager(
    context: Context,
    private val controller: InterruptionFilterController = AndroidInterruptionFilterController(context),
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    fun isSessionOverrideEnabled(): Boolean = preferences.getBoolean(KEY_SESSION_OVERRIDE, false)

    fun hasPolicyAccess(): Boolean = controller.hasPolicyAccess()

    fun isActive(): Boolean = shouldShield() && hasPolicyAccess() &&
        controller.currentFilter() != NotificationManager.INTERRUPTION_FILTER_ALL

    fun setSessionOverride(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_SESSION_OVERRIDE, enabled) }
    }

    fun setEnabled(enabled: Boolean, sessionActive: Boolean, sessionPaused: Boolean) {
        preferences.edit { putBoolean(KEY_ENABLED, enabled) }
        if (enabled && sessionActive && !sessionPaused) {
            activate()
        } else if (!enabled && !isSessionOverrideEnabled()) {
            restorePreviousFilter()
        }
    }

    fun reconcile(sessionActive: Boolean, sessionPaused: Boolean) {
        if (shouldShield() && sessionActive && !sessionPaused) {
            activate()
        } else {
            restorePreviousFilter()
        }
    }

    fun activate() {
        if (!shouldShield() || !hasPolicyAccess() || ownsCurrentFilter()) return
        val currentFilter = controller.currentFilter()
        if (currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return

        preferences.edit {
            putInt(KEY_PREVIOUS_FILTER, currentFilter)
        }
        if (controller.setFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)) {
            preferences.edit { putBoolean(KEY_OWNS_FILTER, true) }
        } else {
            preferences.edit { remove(KEY_PREVIOUS_FILTER) }
        }
    }

    fun restorePreviousFilter() {
        if (!ownsCurrentFilter()) return
        if (hasPolicyAccess() &&
            controller.currentFilter() != NotificationManager.INTERRUPTION_FILTER_PRIORITY
        ) {
            clearOwnedState()
            return
        }
        val previousFilter = preferences.getInt(
            KEY_PREVIOUS_FILTER,
            NotificationManager.INTERRUPTION_FILTER_ALL,
        ).takeIf(::isRestorableFilter) ?: NotificationManager.INTERRUPTION_FILTER_ALL

        if (!hasPolicyAccess() || controller.setFilter(previousFilter)) {
            clearOwnedState()
        }
    }

    internal fun ownsCurrentFilter(): Boolean = preferences.getBoolean(KEY_OWNS_FILTER, false)

    private fun clearOwnedState() {
        preferences.edit {
            remove(KEY_OWNS_FILTER)
            remove(KEY_PREVIOUS_FILTER)
        }
    }

    private fun isRestorableFilter(filter: Int): Boolean = filter in setOf(
        NotificationManager.INTERRUPTION_FILTER_ALL,
        NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        NotificationManager.INTERRUPTION_FILTER_NONE,
        NotificationManager.INTERRUPTION_FILTER_ALARMS,
    )

    private fun shouldShield(): Boolean = isEnabled() || isSessionOverrideEnabled()

    companion object {
        const val PREFERENCES_NAME = "solo_studying_focus_shield"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_OWNS_FILTER = "owns_filter"
        private const val KEY_PREVIOUS_FILTER = "previous_filter"
        private const val KEY_SESSION_OVERRIDE = "session_override"
    }
}

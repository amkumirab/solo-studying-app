package com.amkumirab.solostudying.focus

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import androidx.core.content.edit

class StrictFocusStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun isRequested(): Boolean = preferences.getBoolean(KEY_REQUESTED, false)

    fun setRequested(requested: Boolean) {
        preferences.edit { putBoolean(KEY_REQUESTED, requested) }
    }

    fun ownsPinning(): Boolean = preferences.getBoolean(KEY_OWNS_PINNING, false)

    fun setOwnsPinning(ownsPinning: Boolean) {
        preferences.edit { putBoolean(KEY_OWNS_PINNING, ownsPinning) }
    }

    companion object {
        const val PREFERENCES_NAME = "solo_studying_strict_focus"
        private const val KEY_REQUESTED = "requested"
        private const val KEY_OWNS_PINNING = "owns_pinning"
    }
}

object StrictFocusPinning {
    fun isPinned(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    fun start(activity: Activity): Boolean = runCatching {
        activity.startLockTask()
    }.isSuccess

    fun stop(activity: Activity): Boolean {
        if (!isPinned(activity)) return true
        return runCatching { activity.stopLockTask() }.isSuccess
    }
}

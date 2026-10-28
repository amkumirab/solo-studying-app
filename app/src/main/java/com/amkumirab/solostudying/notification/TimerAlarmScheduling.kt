package com.amkumirab.solostudying.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

internal fun hasPreciseTimerAccess(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
    context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

/** Only user-started timer completions opt into precise delivery, never daily reminders. */
internal fun scheduleTimerCompletion(context: Context, endTimeMillis: Long, operation: PendingIntent) {
    val manager = context.getSystemService(AlarmManager::class.java)
    if (TimerAlarmSettingsStore(context).isEnabled() && (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms())) {
        try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, operation)
            return
        } catch (_: SecurityException) {
            // A revoked grant must degrade to the existing inexact timer, not crash.
        }
    }
    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, operation)
}

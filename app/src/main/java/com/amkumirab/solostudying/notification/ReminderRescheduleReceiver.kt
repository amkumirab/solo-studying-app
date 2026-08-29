package com.amkumirab.solostudying.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.amkumirab.solostudying.breaks.BreakSessionStore

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in SUPPORTED_ACTIONS) {
            val applicationContext = context.applicationContext
            NotificationHelper.scheduleDailyAlarms(applicationContext)
            val breakStore = BreakSessionStore(applicationContext)
            val activeBreak = breakStore.readSession()
            if (activeBreak != null) {
                val nowMillis = System.currentTimeMillis()
                val triggerAtMillis = activeBreak.endTimeMillis.takeIf { it > nowMillis }
                    ?: (nowMillis + 1_000L)
                NotificationHelper.scheduleBreakAlarm(applicationContext, triggerAtMillis)
            }
        }
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}

package com.amkumirab.solostudying.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.amkumirab.solostudying.breaks.BreakSessionStore
import com.amkumirab.solostudying.focus.FocusSessionStore
import com.amkumirab.solostudying.focus.FocusShieldManager
import com.amkumirab.solostudying.focus.displayTitle
import com.amkumirab.solostudying.focus.reconcileFocusSession

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

            val focusStore = FocusSessionStore(applicationContext)
            val savedFocus = focusStore.read()
            if (savedFocus == null) {
                FocusShieldManager(applicationContext).restorePreviousFilter()
            } else {
                val saved = savedFocus
                val current = reconcileFocusSession(saved, System.currentTimeMillis())
                focusStore.write(current)
                if (current.timeLeftSeconds <= 0L) {
                    FocusShieldManager(applicationContext).restorePreviousFilter()
                    FocusSessionNotifier.showCompleted(applicationContext, current.displayTitle())
                } else {
                    FocusShieldManager(applicationContext).reconcile(
                        sessionActive = true,
                        sessionPaused = current.isPaused,
                    )
                    FocusSessionNotifier.createChannel(applicationContext)
                    FocusSessionNotifier.show(applicationContext, current)
                    FocusSessionNotifier.scheduleCompletion(applicationContext, current)
                }
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

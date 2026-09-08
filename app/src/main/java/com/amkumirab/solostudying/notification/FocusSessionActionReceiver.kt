package com.amkumirab.solostudying.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.amkumirab.solostudying.focus.FocusSessionControlAction
import com.amkumirab.solostudying.focus.FocusSessionStore
import com.amkumirab.solostudying.focus.applyFocusSessionControl
import com.amkumirab.solostudying.focus.displayTitle
import com.amkumirab.solostudying.focus.reconcileFocusSession
import kotlinx.coroutines.flow.MutableSharedFlow

class FocusSessionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = FocusSessionStore(context)
        val saved = store.read() ?: run {
            FocusSessionNotifier.cancelActive(context)
            FocusSessionNotifier.cancelCompletionAlarm(context)
            return
        }
        val nowMillis = System.currentTimeMillis()

        when (intent.action) {
            ACTION_PAUSE -> {
                val updated = applyFocusSessionControl(
                    snapshot = saved,
                    action = FocusSessionControlAction.Pause,
                    nowMillis = nowMillis,
                )
                store.write(updated)
                FocusSessionNotifier.cancelCompletionAlarm(context)
                if (updated.timeLeftSeconds > 0L) {
                    FocusSessionNotifier.show(context, updated)
                } else {
                    FocusSessionNotifier.showCompleted(context, updated.displayTitle())
                }
                FocusSessionActionEvents.notifyChanged()
            }

            ACTION_RESUME -> {
                val updated = applyFocusSessionControl(
                    snapshot = saved,
                    action = FocusSessionControlAction.Resume,
                    nowMillis = nowMillis,
                )
                store.write(updated)
                FocusSessionNotifier.show(context, updated)
                FocusSessionNotifier.scheduleCompletion(context, updated)
                FocusSessionActionEvents.notifyChanged()
            }

            ACTION_TIMER_ELAPSED -> {
                val updated = reconcileFocusSession(saved, nowMillis)
                store.write(updated)
                if (updated.timeLeftSeconds <= 0L) {
                    FocusSessionNotifier.showCompleted(context, updated.displayTitle())
                } else {
                    FocusSessionNotifier.show(context, updated)
                    FocusSessionNotifier.scheduleCompletion(context, updated)
                }
                FocusSessionActionEvents.notifyChanged()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.amkumirab.solostudying.ACTION_PAUSE_FOCUS"
        const val ACTION_RESUME = "com.amkumirab.solostudying.ACTION_RESUME_FOCUS"
        const val ACTION_TIMER_ELAPSED = "com.amkumirab.solostudying.ACTION_FOCUS_TIMER_ELAPSED"
    }
}

object FocusSessionActionEvents {
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun notifyChanged() {
        changes.tryEmit(Unit)
    }
}

package com.amkumirab.solostudying.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.amkumirab.solostudying.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

internal class TimerAlarmSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    fun isEnabled(): Boolean = preferences.getBoolean("enabled", false)
    fun setEnabled(enabled: Boolean): Boolean = preferences.edit().putBoolean("enabled", enabled).commit()
    companion object { const val PREFERENCES_NAME = "solo_studying_timer_alarm" }
}

internal data class TimerAlarmEvent(val token: String, val timer: CompletedTimer, val expiresAtMillis: Long, val isTest: Boolean)

/** A single bounded visual alert. It never starts/stops study or grants rewards. */
internal object TimerAlarmAlerts {
    const val CHANNEL_ID = "timer_completion_alarms_v1"
    const val NOTIFICATION_ID = 4101
    const val MAX_ALERT_MILLIS = 30_000L
    private val mutableEvents = MutableStateFlow<TimerAlarmEvent?>(null)
    val events = mutableEvents.asStateFlow()
    fun isShowing(timer: CompletedTimer): Boolean = events.value?.let {
        it.timer == timer && !it.isTest && it.expiresAtMillis > System.currentTimeMillis()
    } == true
    private val handler = Handler(Looper.getMainLooper())
    private var timeout: Runnable? = null
    private fun preferences(context: Context) = context.applicationContext.getSharedPreferences(TimerAlarmSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
    fun eventUri(token: String): Uri = "solostudying://timer-alert/$token".toUri()

    @Synchronized fun begin(context: Context, timer: CompletedTimer, isTest: Boolean = false): Boolean {
        val app = context.applicationContext
        val manager = app.getSystemService(NotificationManager::class.java)
        if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return false
        val event = TimerAlarmEvent(UUID.randomUUID().toString(), timer, System.currentTimeMillis() + MAX_ALERT_MILLIS, isTest)
        if (!preferences(app).edit().putString("token", event.token).putString("timer", timer.name)
                .putLong("expires", event.expiresAtMillis).putBoolean("test", isTest).commit()) return false
        TimerCompletionFeedback.stop(app)
        TimerCompletionFeedback.startAlarm(app)
        scheduleTimeout(app, event)
        post(app, manager, event)
        mutableEvents.value = event
        return true
    }

    @Synchronized fun restore(context: Context) {
        val prefs = preferences(context)
        val token = prefs.getString("token", null)
        val event = runCatching {
            require(token != null && UUID.fromString(token).toString() == token)
            val expires = prefs.getLong("expires", 0)
            require(expires - System.currentTimeMillis() in 1..MAX_ALERT_MILLIS)
            TimerAlarmEvent(token, CompletedTimer.valueOf(prefs.getString("timer", "").orEmpty()), expires, prefs.getBoolean("test", false))
        }.getOrNull()
        if (event == null && token != null) dismiss(context, token)
        mutableEvents.value = event
        if (event != null) scheduleTimeout(context.applicationContext, event)
    }

    @Synchronized fun dismiss(context: Context, token: String): Boolean {
        val prefs = preferences(context)
        if (prefs.getString("token", null) != token) return false
        prefs.edit { remove("token"); remove("timer"); remove("expires"); remove("test") }
        timeout?.let(handler::removeCallbacks)
        timeout = null
        TimerCompletionFeedback.stop(context)
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        mutableEvents.value = null
        return true
    }

    fun dismissCurrent(context: Context) {
        preferences(context).getString("token", null)?.let { dismiss(context, it) }
    }

    fun screenIntent(context: Context, event: TimerAlarmEvent) = Intent(context, TimerAlarmActivity::class.java)
        .setData(eventUri(event.token)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private fun scheduleTimeout(context: Context, event: TimerAlarmEvent) {
        timeout?.let(handler::removeCallbacks)
        timeout = Runnable { dismiss(context, event.token) }.also {
            handler.postDelayed(it, (event.expiresAtMillis - System.currentTimeMillis()).coerceIn(0L, MAX_ALERT_MILLIS))
        }
    }

    fun canShowFullScreen(context: Context): Boolean = Build.VERSION.SDK_INT < 34 ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    private fun post(context: Context, manager: NotificationManager, event: TimerAlarmEvent) {
        if (!canPostNotifications(context)) return
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL_ID,
            "Timer completion alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Optional visual timer alarms. Sound and vibration follow the app's timer settings."
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        })
        val screen = PendingIntent.getActivity(context, 0, screenIntent(context, event), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val dismiss = PendingIntent.getBroadcast(context, 0,
            Intent(context, TimerAlarmDismissReceiver::class.java).setData(eventUri(event.token)), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = if (event.timer == CompletedTimer.FOCUS) "Focus complete" else "Break complete"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(title).setContentText(if (event.isTest) "Test alert · tap to preview" else "Timer finished. Tap to open or dismiss this alert.")
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(screen).setDeleteIntent(dismiss)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismiss)
            .setOnlyAlertOnce(true).setTimeoutAfter(MAX_ALERT_MILLIS).setSound(null).setDefaults(0)
        if (canShowFullScreen(context)) builder.setFullScreenIntent(screen, true)
        postNotificationSafely(context, NOTIFICATION_ID, builder.build())
    }
}

class TimerAlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != "solostudying" || uri.host != "timer-alert") return
        uri.lastPathSegment?.let { TimerAlarmAlerts.dismiss(context, it) }
    }
}

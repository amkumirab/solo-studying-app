package com.amkumirab.solostudying.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.amkumirab.solostudying.MainActivity
import com.amkumirab.solostudying.R
import com.amkumirab.solostudying.focus.FocusSessionSnapshot
import com.amkumirab.solostudying.focus.displayTitle
import java.util.Locale

object FocusSessionNotifier {
    const val CHANNEL_ID = "active_focus_sessions"
    const val ACTIVE_NOTIFICATION_ID = 3001
    const val COMPLETED_NOTIFICATION_ID = 3002
    private const val COMPLETION_ALARM_REQUEST_CODE = 3101
    private const val OPEN_REQUEST_CODE = 3102
    private const val FINISH_REQUEST_CODE = 3103
    private const val PAUSE_REQUEST_CODE = 3104
    private const val RESUME_REQUEST_CODE = 3105

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Active focus sessions",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Live timer controls and focus-session completion alerts."
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    fun show(context: Context, snapshot: FocusSessionSnapshot) {
        if (!snapshot.isActive || snapshot.timeLeftSeconds <= 0L) {
            cancelActive(context)
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val title = snapshot.displayTitle()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(notificationIcon(context))
            .setContentTitle(title)
            .setContentText(
                if (snapshot.isPaused) {
                    "Paused · ${formatDuration(snapshot.timeLeftSeconds)} remaining"
                } else {
                    "Focus in progress · ${formatDuration(snapshot.timeLeftSeconds)} remaining"
                },
            )
            .setContentIntent(openSessionIntent(context))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (snapshot.isPaused) {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Resume",
                controlIntent(context, FocusSessionActionReceiver.ACTION_RESUME, RESUME_REQUEST_CODE),
            )
        } else {
            builder
                .setWhen(System.currentTimeMillis() + snapshot.timeLeftSeconds * 1_000L)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    controlIntent(context, FocusSessionActionReceiver.ACTION_PAUSE, PAUSE_REQUEST_CODE),
                )
        }

        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Finish",
            finishSessionIntent(context),
        )

        try {
            NotificationManagerCompat.from(context).notify(ACTIVE_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ may revoke notification permission while a session is active.
        }
    }

    fun showCompleted(context: Context, subject: String) {
        cancelActive(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        NotificationHelper.createNotificationChannel(context)
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(notificationIcon(context))
            .setContentTitle("Focus complete")
            .setContentText("$subject is complete. Open the app to review your progress.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$subject is complete. Open the app to review your progress."),
            )
            .setContentIntent(openSessionIntent(context))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(COMPLETED_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Android 13+ may revoke notification permission while a session is active.
        }
    }

    fun scheduleCompletion(context: Context, snapshot: FocusSessionSnapshot) {
        if (!snapshot.isActive || snapshot.isPaused || snapshot.timeLeftSeconds <= 0L) {
            cancelCompletionAlarm(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + snapshot.timeLeftSeconds * 1_000L,
            completionAlarmIntent(context),
        )
    }

    fun cancelCompletionAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(completionAlarmIntent(context))
    }

    fun cancelActive(context: Context) {
        NotificationManagerCompat.from(context).cancel(ACTIVE_NOTIFICATION_ID)
    }

    fun cancelCompleted(context: Context) {
        NotificationManagerCompat.from(context).cancel(COMPLETED_NOTIFICATION_ID)
    }

    private fun openSessionIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_FOCUS_SESSION
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            OPEN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun finishSessionIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_FINISH_FOCUS_SESSION
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            FINISH_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun controlIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, FocusSessionActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun completionAlarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        COMPLETION_ALARM_REQUEST_CODE,
        Intent(context, FocusSessionActionReceiver::class.java).apply {
            action = FocusSessionActionReceiver.ACTION_TIMER_ELAPSED
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notificationIcon(@Suppress("UNUSED_PARAMETER") context: Context): Int =
        R.drawable.ic_stat_focus
}

internal fun formatDuration(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0L)
    val hours = safeSeconds / 3_600L
    val minutes = (safeSeconds % 3_600L) / 60L
    val seconds = safeSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

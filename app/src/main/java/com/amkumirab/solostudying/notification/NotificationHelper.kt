package com.amkumirab.solostudying.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar
import java.util.TimeZone

object NotificationHelper {
    const val CHANNEL_ID = "solo_studying_rpg_quests"
    private const val TAG = "NotificationHelper"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Epic RPG Study Quests"
            val descriptionText = "Notifies you of morning daily focus quests and evening twilight warnings."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }

    fun scheduleDailyAlarms(
        context: Context,
        settings: ReminderSettings = ReminderSettingsStore(context).read(),
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nowMillis = System.currentTimeMillis()
        scheduleOrCancel(
            context = context,
            alarmManager = alarmManager,
            action = NotificationReceiver.ACTION_MORNING_QUEST,
            requestCode = 1001,
            label = "Morning",
            schedule = settings.morning,
            nowMillis = nowMillis,
        )
        scheduleOrCancel(
            context = context,
            alarmManager = alarmManager,
            action = NotificationReceiver.ACTION_BEFORE_STUDY,
            requestCode = 1003,
            label = "Before-study",
            schedule = settings.beforeStudy,
            nowMillis = nowMillis,
        )
        scheduleOrCancel(
            context = context,
            alarmManager = alarmManager,
            action = NotificationReceiver.ACTION_EVENING_CAMPAIGN,
            requestCode = 1002,
            label = "Evening",
            schedule = settings.evening,
            nowMillis = nowMillis,
        )
    }

    private fun scheduleOrCancel(
        context: Context,
        alarmManager: AlarmManager,
        action: String,
        requestCode: Int,
        label: String,
        schedule: ReminderSchedule,
        nowMillis: Long,
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (!schedule.enabled) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "$label alarm disabled")
            return
        }

        val triggerAtMillis = calculateNextReminderTimeMillis(
            schedule = schedule,
            nowMillis = nowMillis,
        )
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
            Log.d(TAG, "$label alarm scheduled for ${schedule.formattedTime()}")
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to schedule $label alarm", exception)
        }
    }
}

internal fun calculateNextReminderTimeMillis(
    schedule: ReminderSchedule,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): Long {
    val calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, schedule.hour)
        set(Calendar.MINUTE, schedule.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= nowMillis) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    return calendar.timeInMillis
}

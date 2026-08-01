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

    fun scheduleDailyAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Schedule Morning Alarm at 9:00 AM (local time)
        val morningIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_MORNING_QUEST
        }
        val morningPendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val morningCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time is in the past today, add one day so it fires tomorrow morning instead of immediately
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Schedule repeating daily alarm indexer
        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                morningCalendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                morningPendingIntent
            )
            Log.d(TAG, "Morning alarm scheduled for: ${morningCalendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling morning alarm", e)
        }

        // 2. Schedule Evening Alarm at 9:00 PM (local time)
        val eveningIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_EVENING_CAMPAIGN
        }
        val eveningPendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val eveningCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21) // 9:00 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time is in the past today, add one day so it fires tomorrow evening instead of immediately
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                eveningCalendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                eveningPendingIntent
            )
            Log.d(TAG, "Evening alarm scheduled for: ${eveningCalendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling evening alarm", e)
        }

        // 3. Schedule Before Study Reminder Alarm at 6:00 PM (local time)
        val beforeStudyIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_BEFORE_STUDY
        }
        val beforeStudyPendingIntent = PendingIntent.getBroadcast(
            context,
            1003,
            beforeStudyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val beforeStudyCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18) // 6:00 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                beforeStudyCalendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                beforeStudyPendingIntent
            )
            Log.d(TAG, "Before-study alarm scheduled for: ${beforeStudyCalendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling before-study alarm", e)
        }
    }
}

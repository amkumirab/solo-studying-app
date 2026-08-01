package com.amkumirab.solostudying.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amkumirab.solostudying.MainActivity
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MORNING_QUEST = "com.amkumirab.solostudying.ACTION_MORNING_QUEST"
        const val ACTION_BEFORE_STUDY = "com.amkumirab.solostudying.ACTION_BEFORE_STUDY"
        const val ACTION_EVENING_CAMPAIGN = "com.amkumirab.solostudying.ACTION_EVENING_CAMPAIGN"
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Notification Received: action=$action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleNotificationTrigger(context, action)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleNotificationTrigger(context: Context, action: String) {
        val database = SoloStudyingDatabase.getDatabase(context)
        val dao = database.soloStudyingDao()

        // 1. Fetch User Profile
        val profile = dao.getProfileSync() ?: UserProfileEntity()
        val calendar = Calendar.getInstance()
        val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dailyTargetMinutes = profile.getTargetMinutesForCalendarDay(todayDayOfWeek).coerceAtLeast(1)

        // 2. Fetch Sessions to calculate study time today
        val sessions = dao.getAllSessions().first()
        val startOfToday = getStartOfTodayMillis()
        val studiedSecondsToday = sessions
            .filter { it.timestamp >= startOfToday }
            .sumOf { it.durationSeconds }
        val studiedMinutesToday = (studiedSecondsToday / 60).toInt()

        // 3. Calculate Weekly Progress
        val calendarForWeek = Calendar.getInstance()
        calendarForWeek.set(Calendar.DAY_OF_WEEK, calendarForWeek.firstDayOfWeek)
        calendarForWeek.set(Calendar.HOUR_OF_DAY, 0)
        calendarForWeek.set(Calendar.MINUTE, 0)
        calendarForWeek.set(Calendar.SECOND, 0)
        calendarForWeek.set(Calendar.MILLISECOND, 0)
        val startOfWeekMillis = calendarForWeek.timeInMillis

        val studiedSecondsThisWeek = sessions
            .filter { it.timestamp >= startOfWeekMillis }
            .sumOf { it.durationSeconds }
        val studiedMinutesThisWeek = (studiedSecondsThisWeek / 60).toInt()

        val weeklyTargetMinutes = profile.scheduleWeekdayMinutes.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .sum()
            .coerceAtLeast(30)

        // 4. Fetch bosses
        val bosses = dao.getAllBosses().first()

        // 5. Generate Context-Aware RPG Notification Content
        val notificationPair = CompanionNotificationGenerator.generateNotification(
            profile = profile,
            bosses = bosses,
            studiedMinutesToday = studiedMinutesToday,
            studiedMinutesThisWeek = studiedMinutesThisWeek,
            weeklyTargetMinutes = weeklyTargetMinutes,
            triggerAction = action
        )

        val title = notificationPair.first
        val message = notificationPair.second

        Log.d(TAG, "Generated Smart Notification -> Title: $title, Msg: $message")
        showNotification(context, title, message)
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open Main App when clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = context.applicationInfo.icon
        val actualIcon = if (smallIcon != 0) smallIcon else android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(actualIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show the notification matching ID based on title hash for distinct morning/evening notifications
        notificationManager.notify(title.hashCode(), builder.build())
        Log.d(TAG, "Displayed notification with title: $title")
    }

    private fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}

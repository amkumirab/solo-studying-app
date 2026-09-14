package com.amkumirab.solostudying.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.amkumirab.solostudying.MainActivity
import com.amkumirab.solostudying.R
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.domain.streak.WeeklyStreakDay
import com.amkumirab.solostudying.domain.streak.WeeklyStreakDayState
import com.amkumirab.solostudying.domain.streak.WeeklyStreakSnapshot
import com.amkumirab.solostudying.domain.streak.buildWeeklyStreakSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WeeklyStreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = loadSnapshot(context.applicationContext)
                appWidgetIds.forEach { appWidgetId ->
                    appWidgetManager.updateAppWidget(
                        appWidgetId,
                        createRemoteViews(context, snapshot),
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_DATE_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            requestUpdate(context)
        }
    }

    companion object {
        private const val OPEN_STATS_REQUEST_CODE = 4201

        private val dayLabelIds = intArrayOf(
            R.id.streak_day_label_1,
            R.id.streak_day_label_2,
            R.id.streak_day_label_3,
            R.id.streak_day_label_4,
            R.id.streak_day_label_5,
            R.id.streak_day_label_6,
            R.id.streak_day_label_7,
        )
        private val dayStatusIds = intArrayOf(
            R.id.streak_day_status_1,
            R.id.streak_day_status_2,
            R.id.streak_day_status_3,
            R.id.streak_day_status_4,
            R.id.streak_day_status_5,
            R.id.streak_day_status_6,
            R.id.streak_day_status_7,
        )

        fun updateAll(context: Context, snapshot: WeeklyStreakSnapshot) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, WeeklyStreakWidgetProvider::class.java)
            manager.getAppWidgetIds(provider).forEach { appWidgetId ->
                manager.updateAppWidget(appWidgetId, createRemoteViews(context, snapshot))
            }
        }

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, WeeklyStreakWidgetProvider::class.java)
            val appWidgetIds = manager.getAppWidgetIds(provider)
            if (appWidgetIds.isEmpty()) return
            context.sendBroadcast(
                Intent(context, WeeklyStreakWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                },
            )
        }

        internal fun createRemoteViews(
            context: Context,
            snapshot: WeeklyStreakSnapshot,
        ): RemoteViews = RemoteViews(context.packageName, R.layout.weekly_streak_widget).apply {
            setTextViewText(
                R.id.streak_widget_title,
                if (snapshot.isReady) {
                    val unit = if (snapshot.streakDays == 1) "DAY" else "DAYS"
                    "🔥 ${snapshot.streakDays} $unit"
                } else {
                    "WEEKLY STREAK"
                },
            )
            setTextViewText(R.id.streak_widget_message, snapshot.message)

            snapshot.days.take(dayLabelIds.size).forEachIndexed { index, day ->
                setTextViewText(dayLabelIds[index], day.dayLabel)
                val visual = dayVisual(day.state)
                setTextViewText(dayStatusIds[index], visual.symbol)
                setInt(dayStatusIds[index], "setBackgroundResource", visual.background)
                setContentDescription(dayStatusIds[index], dayContentDescription(day))
            }

            setContentDescription(
                R.id.weekly_streak_widget_root,
                if (snapshot.isReady) {
                    "${snapshot.streakDays} day study streak. ${snapshot.message}"
                } else {
                    snapshot.message
                },
            )
            setOnClickPendingIntent(
                R.id.weekly_streak_widget_root,
                PendingIntent.getActivity(
                    context,
                    OPEN_STATS_REQUEST_CODE,
                    Intent(context, MainActivity::class.java).apply {
                        action = MainActivity.ACTION_OPEN_STATS
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }

        private suspend fun loadSnapshot(context: Context): WeeklyStreakSnapshot {
            val dao = SoloStudyingDatabase.getDatabase(context).soloStudyingDao()
            return buildWeeklyStreakSnapshot(
                profile = dao.getProfileSync(),
                sessions = dao.getAllSessions().first(),
            )
        }

        private fun dayVisual(state: WeeklyStreakDayState): DayVisual = when (state) {
            WeeklyStreakDayState.Completed -> DayVisual("✓", R.drawable.streak_day_completed)
            WeeklyStreakDayState.Missed -> DayVisual("×", R.drawable.streak_day_missed)
            WeeklyStreakDayState.Today -> DayVisual("•", R.drawable.streak_day_today)
            WeeklyStreakDayState.Upcoming -> DayVisual("", R.drawable.streak_day_upcoming)
            WeeklyStreakDayState.Rest -> DayVisual("–", R.drawable.streak_day_rest)
        }

        private fun dayContentDescription(day: WeeklyStreakDay): String {
            val state = when (day.state) {
                WeeklyStreakDayState.Completed -> "target completed"
                WeeklyStreakDayState.Missed -> "target missed"
                WeeklyStreakDayState.Today -> "today, target in progress"
                WeeklyStreakDayState.Upcoming -> "upcoming"
                WeeklyStreakDayState.Rest -> "rest day"
            }
            return "${day.date.dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase)}, $state, " +
                "${day.studiedMinutes} of ${day.targetMinutes} minutes"
        }

        private data class DayVisual(val symbol: String, val background: Int)
    }
}

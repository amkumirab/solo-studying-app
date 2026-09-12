package com.amkumirab.solostudying.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.amkumirab.solostudying.MainActivity
import com.amkumirab.solostudying.R
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.domain.quest.buildRecurringQuestOccurrence
import com.amkumirab.solostudying.domain.quest.dailyQuestDateKey
import com.amkumirab.solostudying.domain.quest.selectVisibleDailyQuests
import com.amkumirab.solostudying.quickstart.QuickStartPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodayWidgetProvider : AppWidgetProvider() {

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
        private const val OPEN_APP_REQUEST_CODE = 4101
        private const val QUICK_START_REQUEST_CODE = 4102

        fun updateAll(context: Context, snapshot: TodayWidgetSnapshot) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, TodayWidgetProvider::class.java)
            manager.getAppWidgetIds(provider).forEach { appWidgetId ->
                manager.updateAppWidget(appWidgetId, createRemoteViews(context, snapshot))
            }
        }

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, TodayWidgetProvider::class.java)
            val appWidgetIds = manager.getAppWidgetIds(provider)
            if (appWidgetIds.isEmpty()) return
            context.sendBroadcast(
                Intent(context, TodayWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                },
            )
        }

        internal fun createRemoteViews(
            context: Context,
            snapshot: TodayWidgetSnapshot,
        ): RemoteViews = RemoteViews(context.packageName, R.layout.today_home_widget).apply {
            setTextViewText(R.id.widget_streak, snapshot.streakLabel)
            setTextViewText(R.id.widget_progress_label, snapshot.progressLabel)
            setProgressBar(R.id.widget_progress, 100, snapshot.progressPercent, false)
            setTextViewText(R.id.widget_action_title, snapshot.actionTitle)
            setTextViewText(R.id.widget_action_detail, snapshot.actionDetail)
            setTextViewText(
                R.id.widget_quick_start,
                if (snapshot.isReady) "START ${snapshot.quickStartMinutes} MIN" else "OPEN APP",
            )
            setViewVisibility(R.id.widget_progress, if (snapshot.isReady) View.VISIBLE else View.INVISIBLE)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context,
                    OPEN_APP_REQUEST_CODE,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )

            val quickStartIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_START_QUICK_FOCUS
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            setOnClickPendingIntent(
                R.id.widget_quick_start,
                PendingIntent.getActivity(
                    context,
                    QUICK_START_REQUEST_CODE,
                    quickStartIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }

        private suspend fun loadSnapshot(context: Context): TodayWidgetSnapshot {
            val dao = SoloStudyingDatabase.getDatabase(context).soloStudyingDao()
            val today = LocalDate.now()
            dao.getActiveRecurringQuests().forEach { recurringQuest ->
                buildRecurringQuestOccurrence(
                    quest = recurringQuest,
                    date = today,
                    createdAt = System.currentTimeMillis(),
                )?.let { occurrence ->
                    dao.insertDailyQuestIfAbsent(occurrence)
                }
            }
            val visibleQuests = selectVisibleDailyQuests(
                quests = dao.getAllDailyQuests().first(),
                today = dailyQuestDateKey(),
            )
            return buildTodayWidgetSnapshot(
                profile = dao.getProfileSync(),
                sessions = dao.getAllSessions().first(),
                quests = visibleQuests,
                bosses = dao.getAllBosses().first(),
                steps = dao.getAllBossSteps().first(),
                quickStart = QuickStartPreferences(context).read(),
                today = today,
            )
        }
    }
}

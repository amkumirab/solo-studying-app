package com.amkumirab.solostudying.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.amkumirab.solostudying.domain.streak.buildWeeklyStreakSnapshot
import com.amkumirab.solostudying.quickstart.QuickStartSelection
import com.amkumirab.solostudying.ui.viewmodel.DailyQuestViewModel
import com.amkumirab.solostudying.ui.viewmodel.DungeonViewModel
import com.amkumirab.solostudying.ui.viewmodel.StatusViewModel
import com.amkumirab.solostudying.widget.TodayWidgetProvider
import com.amkumirab.solostudying.widget.WeeklyStreakWidgetProvider
import com.amkumirab.solostudying.widget.buildTodayWidgetSnapshot

@Composable
internal fun HomeWidgetUpdates(
    status: StatusViewModel,
    goals: DungeonViewModel,
    quests: DailyQuestViewModel,
    quickStart: QuickStartSelection,
) {
    val context = LocalContext.current.applicationContext
    // A session may earn credit while the Activity is stopped. Keep the existing
    // widget refresh lifetime, separate from the lifecycle-aware screen collectors.
    val profile by status.userProfile.collectAsState()
    val history by status.weekSessions.collectAsState()
    val todayQuests by quests.todayQuests.collectAsState()
    val bosses by goals.bosses.collectAsState()
    val steps by goals.bossSteps.collectAsState()

    DisposableEffect(context, status, quests) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                status.refreshSessionPeriod()
                quests.refreshToday()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(profile, history, todayQuests, bosses, steps, quickStart) {
        val period = history.period
        TodayWidgetProvider.updateAll(
            context,
            buildTodayWidgetSnapshot(
                profile, history.sessions, todayQuests, bosses, steps, quickStart, period.today, period.zoneId,
            ),
        )
        WeeklyStreakWidgetProvider.updateAll(
            context,
            buildWeeklyStreakSnapshot(profile, history.sessions, period.today, period.zoneId),
        )
    }
}

package com.amkumirab.solostudying.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
    val sessions by status.sessions.collectAsState()
    val todayQuests by quests.todayQuests.collectAsState()
    val bosses by goals.bosses.collectAsState()
    val steps by goals.bossSteps.collectAsState()

    LaunchedEffect(profile, sessions, todayQuests, bosses, steps, quickStart) {
        TodayWidgetProvider.updateAll(
            context,
            buildTodayWidgetSnapshot(profile, sessions, todayQuests, bosses, steps, quickStart),
        )
        WeeklyStreakWidgetProvider.updateAll(context, buildWeeklyStreakSnapshot(profile, sessions))
    }
}

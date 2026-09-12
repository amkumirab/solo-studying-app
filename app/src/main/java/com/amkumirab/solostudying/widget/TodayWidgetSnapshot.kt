package com.amkumirab.solostudying.widget

import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.BossStepEntity
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.domain.today.buildTodayPlan
import com.amkumirab.solostudying.quickstart.QuickStartSelection
import java.time.LocalDate
import java.time.ZoneId

data class TodayWidgetSnapshot(
    val isReady: Boolean,
    val progressPercent: Int,
    val progressLabel: String,
    val streakLabel: String,
    val actionTitle: String,
    val actionDetail: String,
    val quickStartMinutes: Int,
)

fun buildTodayWidgetSnapshot(
    profile: UserProfileEntity?,
    sessions: List<StudySessionEntity>,
    quests: List<DailyQuestEntity>,
    bosses: List<BossEntity>,
    steps: List<BossStepEntity>,
    quickStart: QuickStartSelection,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): TodayWidgetSnapshot {
    if (profile == null || !profile.hasCompletedOnboarding) {
        return TodayWidgetSnapshot(
            isReady = false,
            progressPercent = 0,
            progressLabel = "Open the app to set up your study plan",
            streakLabel = "READY FOR YOUR FIRST QUEST",
            actionTitle = "Begin your journey",
            actionDetail = "Complete setup to unlock today's mission",
            quickStartMinutes = quickStart.durationMinutes,
        )
    }

    val plan = buildTodayPlan(
        profile = profile,
        sessions = sessions,
        quests = quests,
        bosses = bosses,
        steps = steps,
        today = today,
        zoneId = zoneId,
        maxItems = 1,
    )
    val studiedMinutes = plan.studiedSeconds / 60L
    val nextAction = plan.items.firstOrNull()
    val targetComplete = plan.isDailyTargetComplete && nextAction == null

    return TodayWidgetSnapshot(
        isReady = true,
        progressPercent = (plan.progress * 100f).toInt().coerceIn(0, 100),
        progressLabel = when {
            plan.isRestDay -> "$studiedMinutes MIN STUDIED · REST DAY"
            else -> "$studiedMinutes / ${plan.targetMinutes} MIN TODAY"
        },
        streakLabel = if (profile.currentStreak > 0) {
            "${profile.currentStreak}-DAY STREAK"
        } else {
            "START YOUR STREAK"
        },
        actionTitle = when {
            targetComplete -> "Daily target complete"
            nextAction != null -> nextAction.title
            else -> "Choose your next study quest"
        },
        actionDetail = when {
            targetComplete -> "Mission cleared — keep your momentum"
            nextAction != null -> "${nextAction.durationMinutes} min · ${nextAction.context}"
            else -> "Open Solo Studying to plan your session"
        },
        quickStartMinutes = quickStart.durationMinutes,
    )
}

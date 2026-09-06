package com.amkumirab.solostudying.domain.today

import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.BossStepEntity
import com.amkumirab.solostudying.data.entity.DailyQuestEntity
import com.amkumirab.solostudying.data.entity.StudySessionEntity
import com.amkumirab.solostudying.data.entity.UserProfileEntity
import com.amkumirab.solostudying.domain.deadline.DeadlineGoalPlan
import com.amkumirab.solostudying.domain.deadline.DeadlineGoalStatus
import com.amkumirab.solostudying.domain.deadline.calculateDeadlineGoalPlan
import java.time.LocalDate
import java.time.ZoneId

enum class TodayPlanItemType {
    DailyQuest,
    BossStep,
    Boss,
    QuickFocus,
}

enum class TodayPlanUrgency {
    Urgent,
    High,
    Normal,
}

data class TodayPlanItem(
    val type: TodayPlanItemType,
    val sourceId: Int,
    val bossId: Int? = null,
    val title: String,
    val context: String,
    val durationMinutes: Int,
    val urgency: TodayPlanUrgency,
    internal val rank: Int,
)

data class TodayPlan(
    val targetMinutes: Int,
    val studiedSeconds: Long,
    val remainingMinutes: Int,
    val progress: Float,
    val isRestDay: Boolean,
    val items: List<TodayPlanItem>,
) {
    val isDailyTargetComplete: Boolean
        get() = isRestDay || remainingMinutes == 0
}

fun buildTodayPlan(
    profile: UserProfileEntity,
    sessions: List<StudySessionEntity>,
    quests: List<DailyQuestEntity>,
    bosses: List<BossEntity>,
    steps: List<BossStepEntity>,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
    maxItems: Int = 3,
): TodayPlan {
    val targetMinutes = targetMinutesForDay(profile, today)
    val startOfToday = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val startOfTomorrow = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val studiedSeconds = sessions
        .asSequence()
        .filter { it.timestamp in startOfToday until startOfTomorrow }
        .sumOf { it.durationSeconds.coerceAtLeast(0L) }
    val targetSeconds = targetMinutes * 60L
    val remainingSeconds = (targetSeconds - studiedSeconds).coerceAtLeast(0L)
    val remainingMinutes = ((remainingSeconds + 59L) / 60L).toInt()
    val progress = when {
        targetSeconds <= 0L -> 0f
        else -> (studiedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
    }

    val candidates = buildList {
        val todayKey = today.toString()
        quests
            .asSequence()
            .filter { !it.isCompleted && it.scheduledDate <= todayKey }
            .forEach { quest ->
                val carried = quest.scheduledDate < todayKey
                val priorityRank = when {
                    carried -> 10 - quest.priority.coerceIn(0, 2)
                    quest.priority >= 2 -> 20
                    quest.priority == 1 -> 35
                    else -> 45
                }
                add(
                    TodayPlanItem(
                        type = TodayPlanItemType.DailyQuest,
                        sourceId = quest.id,
                        title = quest.title,
                        context = if (carried) "Unfinished from an earlier day" else "Today's daily quest",
                        durationMinutes = quest.durationMinutes.coerceIn(1, 480),
                        urgency = if (carried || quest.priority >= 2) {
                            TodayPlanUrgency.Urgent
                        } else {
                            TodayPlanUrgency.High
                        },
                        rank = priorityRank,
                    ),
                )
            }

        val stepsByBoss = steps
            .asSequence()
            .filterNot { it.isCompleted }
            .groupBy { it.bossId }
        bosses
            .asSequence()
            .filterNot { it.isCompleted }
            .forEach { boss ->
                val deadline = calculateDeadlineGoalPlan(
                    boss = boss,
                    scheduleDays = profile.scheduleDays,
                    today = today,
                    zoneId = zoneId,
                )
                val nextStep = stepsByBoss[boss.id]
                    ?.minWithOrNull(compareBy<BossStepEntity> { it.sortOrder }.thenBy { it.createdAt })

                if (nextStep != null) {
                    add(
                        TodayPlanItem(
                            type = TodayPlanItemType.BossStep,
                            sourceId = nextStep.id,
                            bossId = boss.id,
                            title = nextStep.title,
                            context = stepContext(boss.name, deadline),
                            durationMinutes = nextStep.estimatedMinutes.coerceIn(1, 480),
                            urgency = deadlineUrgency(deadline),
                            rank = deadlineRank(deadline, hasConcreteStep = true),
                        ),
                    )
                } else if (deadline != null) {
                    add(
                        TodayPlanItem(
                            type = TodayPlanItemType.Boss,
                            sourceId = boss.id,
                            bossId = boss.id,
                            title = "Continue ${boss.name}",
                            context = deadlineContext(deadline),
                            durationMinutes = deadline.remainingMinutes.coerceAtLeast(1),
                            urgency = deadlineUrgency(deadline),
                            rank = deadlineRank(deadline, hasConcreteStep = false),
                        ),
                    )
                }
            }

        if (remainingMinutes > 0) {
            val focusMinutes = minOf(remainingMinutes, 25).coerceAtLeast(1)
            add(
                TodayPlanItem(
                    type = TodayPlanItemType.QuickFocus,
                    sourceId = 0,
                    title = "$focusMinutes-minute focus block",
                    context = "$remainingMinutes min left in today's target",
                    durationMinutes = focusMinutes,
                    urgency = TodayPlanUrgency.Normal,
                    rank = 90,
                ),
            )
        }
    }

    return TodayPlan(
        targetMinutes = targetMinutes,
        studiedSeconds = studiedSeconds,
        remainingMinutes = remainingMinutes,
        progress = progress,
        isRestDay = targetMinutes == 0,
        items = candidates
            .sortedWith(compareBy<TodayPlanItem> { it.rank }.thenBy { it.title })
            .take(maxItems.coerceAtLeast(0)),
    )
}

private fun targetMinutesForDay(profile: UserProfileEntity, today: LocalDate): Int {
    val weekdayTargets = profile.scheduleWeekdayMinutes
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
    return if (weekdayTargets.size == 7) {
        weekdayTargets[today.dayOfWeek.value - 1].coerceAtLeast(0)
    } else {
        profile.scheduleMinutesPerDay.coerceAtLeast(0)
    }
}

private fun deadlineRank(deadline: DeadlineGoalPlan?, hasConcreteStep: Boolean): Int = when {
    deadline?.status == DeadlineGoalStatus.Overdue -> if (hasConcreteStep) 0 else 1
    deadline?.status == DeadlineGoalStatus.DueToday -> if (hasConcreteStep) 3 else 4
    deadline != null && deadline.daysRemaining <= 3 -> if (hasConcreteStep) 22 else 23
    deadline?.status == DeadlineGoalStatus.BehindSchedule -> if (hasConcreteStep) 30 else 31
    deadline != null && deadline.daysRemaining <= 7 -> if (hasConcreteStep) 40 else 41
    hasConcreteStep -> 60
    else -> 70
}

private fun deadlineUrgency(deadline: DeadlineGoalPlan?): TodayPlanUrgency = when {
    deadline?.status == DeadlineGoalStatus.Overdue -> TodayPlanUrgency.Urgent
    deadline?.status == DeadlineGoalStatus.DueToday -> TodayPlanUrgency.Urgent
    deadline != null && deadline.daysRemaining <= 3 -> TodayPlanUrgency.Urgent
    deadline?.status == DeadlineGoalStatus.BehindSchedule -> TodayPlanUrgency.High
    deadline != null && deadline.daysRemaining <= 7 -> TodayPlanUrgency.High
    else -> TodayPlanUrgency.Normal
}

private fun stepContext(bossName: String, deadline: DeadlineGoalPlan?): String = when {
    deadline == null -> "Next step for $bossName"
    deadline.status == DeadlineGoalStatus.Overdue -> "$bossName · deadline overdue"
    deadline.status == DeadlineGoalStatus.DueToday -> "$bossName · due today"
    deadline.daysRemaining == 1 -> "$bossName · due tomorrow"
    else -> "$bossName · ${deadline.daysRemaining} days left"
}

private fun deadlineContext(deadline: DeadlineGoalPlan): String = when (deadline.status) {
    DeadlineGoalStatus.Overdue -> "Deadline overdue · ${deadline.remainingMinutes} min remaining"
    DeadlineGoalStatus.DueToday -> "Due today · ${deadline.remainingMinutes} min remaining"
    DeadlineGoalStatus.BehindSchedule ->
        "Behind schedule · ${deadline.daysRemaining} days left"
    DeadlineGoalStatus.OnTrack ->
        "${deadline.daysRemaining} days left · ${deadline.recommendedMinutesPerStudyDay} min suggested today"
}

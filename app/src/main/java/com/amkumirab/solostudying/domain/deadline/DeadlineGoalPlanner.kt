package com.amkumirab.solostudying.domain.deadline

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class DeadlineGoalStatus {
    OnTrack,
    BehindSchedule,
    DueToday,
    Overdue,
}

data class DeadlineGoalPlan(
    val boss: BossEntity,
    val deadline: LocalDate,
    val daysRemaining: Int,
    val studyDaysRemaining: Int,
    val remainingMinutes: Int,
    val recommendedMinutesPerStudyDay: Int,
    val progress: Float,
    val expectedProgress: Float,
    val status: DeadlineGoalStatus,
)

fun calculateDeadlineGoalPlan(
    boss: BossEntity,
    scheduleDays: String,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): DeadlineGoalPlan? {
    val deadline = boss.deadlineDate
        ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
        ?: return null
    val targetSeconds = boss.requiredMinutes.coerceAtLeast(0) * 60L
    val completedSeconds = boss.timeSpentSeconds.coerceIn(0L, targetSeconds)
    val remainingSeconds = (targetSeconds - completedSeconds).coerceAtLeast(0L)
    val remainingMinutes = ((remainingSeconds + 59L) / 60L).toInt()
    val configuredDays = parseScheduleDays(scheduleDays)
    val rawStudyDaysRemaining = if (deadline < today) {
        0
    } else {
        countStudyDays(today, deadline, configuredDays)
    }
    val planningDays = if (deadline < today) 0 else rawStudyDaysRemaining.coerceAtLeast(1)
    val recommendedMinutes = if (remainingMinutes == 0 || planningDays == 0) {
        0
    } else {
        (remainingMinutes + planningDays - 1) / planningDays
    }

    val createdDate = Instant.ofEpochMilli(boss.createdAt)
        .atZone(zoneId)
        .toLocalDate()
        .coerceAtMost(deadline)
    val totalStudyDays = countStudyDays(createdDate, deadline, configuredDays).coerceAtLeast(1)
    val completedStudyDays = when {
        today <= createdDate -> 0
        today > deadline -> totalStudyDays
        else -> countStudyDays(createdDate, today.minusDays(1), configuredDays)
    }
    val progress = if (targetSeconds == 0L) 1f else completedSeconds.toFloat() / targetSeconds
    val expectedProgress = (completedStudyDays.toFloat() / totalStudyDays).coerceIn(0f, 1f)
    val status = when {
        remainingSeconds == 0L -> DeadlineGoalStatus.OnTrack
        deadline < today -> DeadlineGoalStatus.Overdue
        deadline == today -> DeadlineGoalStatus.DueToday
        progress + 0.05f >= expectedProgress -> DeadlineGoalStatus.OnTrack
        else -> DeadlineGoalStatus.BehindSchedule
    }

    return DeadlineGoalPlan(
        boss = boss,
        deadline = deadline,
        daysRemaining = ChronoUnit.DAYS.between(today, deadline).toInt().coerceAtLeast(0),
        studyDaysRemaining = planningDays,
        remainingMinutes = remainingMinutes,
        recommendedMinutesPerStudyDay = recommendedMinutes,
        progress = progress.coerceIn(0f, 1f),
        expectedProgress = expectedProgress,
        status = status,
    )
}

private fun parseScheduleDays(value: String): Set<DayOfWeek> {
    val mapping = mapOf(
        "MON" to DayOfWeek.MONDAY,
        "TUE" to DayOfWeek.TUESDAY,
        "WED" to DayOfWeek.WEDNESDAY,
        "THU" to DayOfWeek.THURSDAY,
        "FRI" to DayOfWeek.FRIDAY,
        "SAT" to DayOfWeek.SATURDAY,
        "SUN" to DayOfWeek.SUNDAY,
    )
    val parsed = value.split(',')
        .mapNotNull { day -> mapping[day.trim().take(3).uppercase()] }
        .toSet()
    return parsed.ifEmpty { DayOfWeek.entries.toSet() }
}

private fun countStudyDays(
    start: LocalDate,
    endInclusive: LocalDate,
    allowedDays: Set<DayOfWeek>,
): Int {
    if (endInclusive < start) return 0
    var date = start
    var count = 0
    while (date <= endInclusive) {
        if (date.dayOfWeek in allowedDays) count++
        date = date.plusDays(1)
    }
    return count
}

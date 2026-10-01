package com.amkumirab.solostudying.domain.dungeon

import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.data.entity.DungeonEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val MAX_DUNGEON_TARGET_MINUTES = 600_000

data class DungeonStudyPlanInput(
    val dungeonName: String,
    val targetMinutes: Int,
    val deadline: LocalDate,
    val weekdaysMask: Int,
)

fun DungeonStudyPlanInput.isValid(today: LocalDate = LocalDate.now()): Boolean =
    dungeonName.isNotBlank() && dungeonName == dungeonName.trim() &&
        dungeonName.length <= 100 && !dungeonName.equals("All", ignoreCase = true) &&
        targetMinutes in 1..MAX_DUNGEON_TARGET_MINUTES && weekdaysMask in 1..127 &&
        !deadline.isBefore(today) && !deadline.isAfter(today.plusYears(10))

fun parseDungeonStudyPlanInput(
    name: String,
    hours: String,
    deadline: String,
    weekdaysMask: Int,
    today: LocalDate = LocalDate.now(),
): DungeonStudyPlanInput? {
    val cleanHours = hours.trim().replace(',', '.')
    if (!cleanHours.matches(Regex("\\d{1,5}(\\.\\d{1,2})?"))) return null
    val amount = cleanHours.toBigDecimalOrNull() ?: return null
    if (amount > BigDecimal(10_000)) return null
    val minutes = amount.multiply(BigDecimal(60)).setScale(0, RoundingMode.HALF_UP).toInt()
    val date = runCatching { LocalDate.parse(deadline.trim()) }.getOrNull() ?: return null
    return DungeonStudyPlanInput(name.trim(), minutes, date, weekdaysMask).takeIf { it.isValid(today) }
}

enum class DungeonPlanStatus { Completed, OnTrack, BehindSchedule, DueToday, Overdue, NoStudyDays }

data class DungeonStudyPlan(
    val dungeonName: String,
    val targetMinutes: Int,
    val completedSeconds: Long,
    val remainingMinutes: Int,
    val deadline: LocalDate,
    val studyDaysRemaining: Int,
    val minutesPerStudyDay: Int?,
    val progress: Float,
    val status: DungeonPlanStatus,
)

fun calculateDungeonStudyPlan(
    dungeon: DungeonEntity,
    bosses: List<BossEntity>,
    today: LocalDate = LocalDate.now(),
): DungeonStudyPlan? {
    val target = dungeon.targetMinutes?.takeIf { it in 1..MAX_DUNGEON_TARGET_MINUTES } ?: return null
    val start = runCatching { LocalDate.parse(dungeon.planStartDate) }.getOrNull() ?: return null
    val deadline = runCatching { LocalDate.parse(dungeon.planDeadlineDate) }.getOrNull() ?: return null
    if (start.isAfter(deadline) || dungeon.studyWeekdaysMask !in 1..127) return null
    val targetSeconds = target * 60L
    val completed = bosses.asSequence().filter { it.dungeonName == dungeon.name }.fold(0L) { total, boss ->
        (total + boss.timeSpentSeconds.coerceIn(0L, targetSeconds)).coerceAtMost(targetSeconds)
    }
    val remainingSeconds = targetSeconds - completed
    val remaining = ((remainingSeconds + 59) / 60).toInt()
    val studyDays = countStudyDays(maxOf(today, start), deadline, dungeon.studyWeekdaysMask)
    val totalDays = countStudyDays(start, deadline, dungeon.studyWeekdaysMask)
    val elapsedDays = countStudyDays(start, minOf(today.minusDays(1), deadline), dungeon.studyWeekdaysMask)
    val progress = completed.toFloat() / targetSeconds
    val expected = if (totalDays == 0) 0f else elapsedDays.toFloat() / totalDays
    val status = when {
        remainingSeconds == 0L -> DungeonPlanStatus.Completed
        today.isAfter(deadline) -> DungeonPlanStatus.Overdue
        today == deadline -> DungeonPlanStatus.DueToday
        studyDays == 0 -> DungeonPlanStatus.NoStudyDays
        progress + 0.01f < expected -> DungeonPlanStatus.BehindSchedule
        else -> DungeonPlanStatus.OnTrack
    }
    val daily = when {
        remainingSeconds == 0L -> 0
        studyDays == 0 -> null
        else -> ((remainingSeconds + studyDays * 60L - 1) / (studyDays * 60L)).toInt()
    }
    return DungeonStudyPlan(dungeon.name, target, completed, remaining, deadline, studyDays, daily, progress, status)
}

private fun countStudyDays(start: LocalDate, end: LocalDate, mask: Int): Int {
    if (end.isBefore(start)) return 0
    val days = ChronoUnit.DAYS.between(start, end) + 1
    var count = days / 7 * Integer.bitCount(mask)
    repeat((days % 7).toInt()) { offset ->
        if (mask and (1 shl (start.plusDays(offset.toLong()).dayOfWeek.value - 1)) != 0) count++
    }
    return count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

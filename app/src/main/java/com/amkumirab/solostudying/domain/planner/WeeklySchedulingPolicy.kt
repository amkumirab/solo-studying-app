package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate

/** Frozen limits of an accepted weekly schedule, independent of later catalog edits. */
data class WeeklySchedulingPolicy(val courseMinuteLimits: Map<Int, Int> = emptyMap()) {
    init {
        require(courseMinuteLimits.size <= 20)
        require(courseMinuteLimits.all { (id, minutes) -> id > 0 && minutes in 1..10080 })
    }
}

fun PlannerConfig.withWeeklyScheduling(subjects: List<PlannerSubject>): PlannerConfig = copy(
    weeklyScheduling = if (weeklyGoalMinutes == null) null else WeeklySchedulingPolicy(
        subjects.filter { subject -> courses.any { it.bossId == subject.bossId } }
            .mapNotNull { subject -> subject.preferredWeeklyMinutes?.let { subject.bossId to it } }.toMap()),
)

internal fun plannerWeekStart(date: LocalDate): LocalDate = date.minusDays(date.dayOfWeek.value - 1L)

/** Capacity between inclusive dates, capped independently in every ISO week. */
internal fun weeklyPlannerCapacity(config: PlannerConfig, from: LocalDate, through: LocalDate, courseLimit: Int = Int.MAX_VALUE): Int {
    var date = maxOf(from, config.start)
    var total = 0
    while (date <= through) {
        val end = minOf(plannerWeekStart(date).plusDays(6), through)
        var available = 0
        while (date <= end) { available += config.capacity(date); date = date.plusDays(1) }
        total += minOf(available, config.weeklyGoalMinutes ?: available, courseLimit)
    }
    return total
}

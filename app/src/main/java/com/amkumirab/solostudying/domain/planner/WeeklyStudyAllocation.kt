package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import kotlin.math.min

data class WeeklyStudyAllocation(
    val availableMinutes: Int,
    val goalMinutes: Int,
    val minutesByCourse: Map<Int, Int>,
) {
    val allocatedMinutes get() = minutesByCourse.values.sum()
    val unallocatedMinutes get() = (goalMinutes - allocatedMinutes).coerceAtLeast(0)
}

/** Recommends an exact weekly split; it does not move accepted date-only blocks. */
fun recommendWeeklyHours(config: PlannerConfig, subjects: List<PlannerSubject>, weekStart: LocalDate): WeeklyStudyAllocation {
    require(weekStart.dayOfWeek.value == 1)
    val available = (0L..6L).sumOf { offset ->
        val day = weekStart.plusDays(offset)
        if (day < config.start) 0 else config.capacity(day)
    }
    val goal = config.weeklyGoalMinutes ?: available
    val deadlineCapacity = List(7) { index -> (0..index).sumOf { offset ->
        val day = weekStart.plusDays(offset.toLong())
        if (day < config.start) 0 else config.capacity(day)
    } }
    data class Candidate(val id: Int, val cap: Int, val weight: Int, val deadlineIndex: Int)
    val candidates = config.courses.sortedBy { it.bossId }.mapNotNull { course ->
        if (course.finishBy < weekStart) return@mapNotNull null
        val subject = subjects.firstOrNull { it.bossId == course.bossId }
        val untilDeadline = (0L..6L).sumOf { offset ->
            val day = weekStart.plusDays(offset)
            if (day >= config.start && day <= course.finishBy) config.capacity(day) else 0
        }
        val cap = min(course.remainingMinutes, min(subject?.preferredWeeklyMinutes ?: Int.MAX_VALUE, untilDeadline))
        if (cap == 0) return@mapNotNull null
        val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(weekStart, course.finishBy).coerceIn(0L, 7L).toInt()
        Candidate(course.bossId, cap, course.priority * (14 - daysLeft), min(daysLeft, 6))
    }
    val allocations = candidates.associate { it.id to 0 }.toMutableMap()
    val usedPrefix = IntArray(7)
    // Highest averages allocation, bounded by at most 3,360 minutes and 20 courses.
    // Integer cross-products make proportional rounding and ID tie-breaks repeatable.
    repeat(min(goal, available)) {
        val selected = candidates.filter { candidate ->
            allocations.getValue(candidate.id) < candidate.cap &&
                (candidate.deadlineIndex..6).all { usedPrefix[it] < deadlineCapacity[it] }
        }.maxWithOrNull { a, b ->
            val comparison = (a.weight.toLong() * (allocations.getValue(b.id) + 1))
                .compareTo(b.weight.toLong() * (allocations.getValue(a.id) + 1))
            if (comparison != 0) comparison else b.id.compareTo(a.id)
        } ?: return WeeklyStudyAllocation(available, goal, allocations)
        allocations[selected.id] = allocations.getValue(selected.id) + 1
        (selected.deadlineIndex..6).forEach { usedPrefix[it]++ }
    }
    return WeeklyStudyAllocation(available, goal, allocations)
}

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
    return allocateWeeklyHours(config, subjects, weekStart, WeeklyAllocationNeeds())
}

internal data class WeeklyAllocationNeeds(
    val perCourse: Map<Int, Int> = emptyMap(), val throughDeadline: Map<LocalDate, Int> = emptyMap(),
)

internal fun allocateWeeklyHours(config: PlannerConfig, subjects: List<PlannerSubject>, weekStart: LocalDate, needs: WeeklyAllocationNeeds): WeeklyStudyAllocation {
    require(weekStart.dayOfWeek.value == 1)
    val available = (0L..6L).sumOf { offset ->
        val day = weekStart.plusDays(offset)
        if (day < config.start) 0 else config.plannableStudyCapacity(day)
    }
    val goal = config.weeklyGoalMinutes ?: available
    val deadlineCapacity = List(7) { index -> (0..index).sumOf { offset ->
        val day = weekStart.plusDays(offset.toLong())
        if (day < config.start) 0 else config.plannableStudyCapacity(day)
    } }
    data class Candidate(val id: Int, val cap: Int, val weight: Int, val deadlineIndex: Int, val finishBy: LocalDate)
    val candidates = config.courses.sortedBy { it.bossId }.mapNotNull { course ->
        if (course.finishBy < weekStart) return@mapNotNull null
        val subject = subjects.firstOrNull { it.bossId == course.bossId }
        val untilDeadline = (0L..6L).sumOf { offset ->
            val day = weekStart.plusDays(offset)
            if (day >= config.start && day <= course.finishBy) config.plannableStudyCapacity(day) else 0
        }
        val limit = if (config.weeklyScheduling != null) config.weeklyScheduling.courseMinuteLimits[course.bossId]
            else subject?.preferredWeeklyMinutes
        val cap = min(course.remainingMinutes, min(limit ?: Int.MAX_VALUE, untilDeadline))
        if (cap == 0) return@mapNotNull null
        val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(weekStart, course.finishBy).coerceIn(0L, 7L).toInt()
        Candidate(course.bossId, cap, course.priority * (14 - daysLeft), min(daysLeft, 6), course.finishBy)
    }
    val allocations = candidates.associate { it.id to 0 }.toMutableMap()
    val usedPrefix = IntArray(7)
    var budget = min(goal, available)
    fun reserve(candidate: Candidate, requested: Int): Int {
        val granted = minOf(requested.coerceAtLeast(0), candidate.cap - allocations.getValue(candidate.id), budget,
            (candidate.deadlineIndex..6).minOf { deadlineCapacity[it] - usedPrefix[it] })
        allocations[candidate.id] = allocations.getValue(candidate.id) + granted
        (candidate.deadlineIndex..6).forEach { usedPrefix[it] += granted }
        budget -= granted
        return granted
    }
    val earliest = candidates.sortedWith(compareBy<Candidate> { it.finishBy }.thenByDescending { it.weight }.thenBy { it.id })
    (earliest.map { it.finishBy } + needs.throughDeadline.keys).distinct().sorted().forEach { deadline ->
        earliest.filter { it.finishBy == deadline }.forEach { reserve(it, needs.perCourse[it.id] ?: 0) }
        val required = needs.throughDeadline[deadline] ?: 0
        val due = earliest.filter { it.finishBy <= deadline }
        var missing = required - due.sumOf { allocations.getValue(it.id) }
        due.forEach { missing -= reserve(it, missing) }
    }
    // Highest averages allocation, bounded by at most 3,360 minutes and 20 courses.
    // Integer cross-products make proportional rounding and ID tie-breaks repeatable.
    repeat(budget) {
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

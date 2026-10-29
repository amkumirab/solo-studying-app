package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Cumulative work and shared capacity from the start date through this deadline. */
data class DeadlineTimeBudget(
    val finishBy: LocalDate,
    val courseIds: List<Int>,
    val studyDays: Int,
    val requiredMinutes: Int,
    val availableMinutes: Int,
) {
    val balanceMinutes: Int get() = availableMinutes - requiredMinutes
}

data class PlannerTimeBudget(val deadlines: List<DeadlineTimeBudget>, val requiredDailyMinutes: Int?) {
    val studyDays: Int get() = deadlines.last().studyDays
    val requiredMinutes: Int get() = deadlines.last().requiredMinutes
    val availableMinutes: Int get() = deadlines.last().availableMinutes
    val balanceMinutes: Int get() = availableMinutes - requiredMinutes
    val fitsAllDeadlines: Boolean get() = deadlines.all { it.balanceMinutes >= 0 }
}

/** Minimum whole-minute uniform budget across study days, respecting every deadline. */
fun analyzePlannerTimeBudget(config: PlannerConfig): PlannerTimeBudget {
    val days = ChronoUnit.DAYS.between(config.start, config.courses.maxOf { it.finishBy }).toInt() + 1
    val capacity = IntArray(days + 1)
    val studyDays = IntArray(days + 1)
    repeat(days) { index ->
        val minutes = config.plannableStudyCapacity(config.start.plusDays(index.toLong()))
        capacity[index + 1] = capacity[index] + minutes
        studyDays[index + 1] = studyDays[index] + if (minutes > 0) 1 else 0
    }
    var dueMinutes = 0
    val deadlines = config.courses.groupBy { it.finishBy }.toSortedMap().map { (date, courses) ->
        dueMinutes += courses.sumOf { it.remainingMinutes }
        val index = ChronoUnit.DAYS.between(config.start, date).toInt() + 1
        DeadlineTimeBudget(date, courses.map { it.bossId }, studyDays[index], dueMinutes, capacity[index])
    }
    val required = if (deadlines.any { it.studyDays == 0 }) null else deadlines.maxOf {
        (it.requiredMinutes + it.studyDays - 1) / it.studyDays
    }
    return PlannerTimeBudget(deadlines, required)
}

/** Exact shared daily shift; null rejects invalid steps or any out-of-range study day. */
fun adjustPlannerDailyTime(config: PlannerConfig, adjustmentMinutes: Int): PlannerConfig? {
    if (adjustmentMinutes % 15 != 0) return null
    val budgets = config.weekdayMinutes.map { minutes ->
        if (minutes == 0) 0 else {
            val adjusted = minutes.toLong() + adjustmentMinutes
            if (adjusted !in 1L..480L) return null
            adjusted.toInt()
        }
    }
    return config.withDailyBudgets(budgets)
}

/** Uses the required uniform daily budget without changing weekdays or course answers. */
fun requiredPlannerDailyTime(config: PlannerConfig): PlannerConfig? {
    val required = analyzePlannerTimeBudget(config).requiredDailyMinutes ?: return null
    if (required !in 1..480) return null
    return config.withDailyBudgets(config.weekdayMinutes.map { if (it == 0) 0 else required })
}

private fun PlannerConfig.withDailyBudgets(minutes: List<Int>): PlannerConfig? {
    if (studyWindows?.weekdayMinutes?.zip(minutes)?.any { (ceiling, budget) -> budget > ceiling } == true) return null
    return copy(weekdayMinutes = minutes)
}

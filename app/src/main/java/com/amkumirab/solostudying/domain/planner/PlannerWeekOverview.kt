package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate

internal enum class WeekPlanningStatus {
    OutsidePlan, NoGoal, ZeroGoal, AboveGoal, BudgetShortfall, UnfilledGoal, FullyPlanned,
}

internal data class PlannerWeekOverview(
    val weekStart: LocalDate, val activeDays: Int, val goalMinutes: Int?,
    val plannedMinutes: Int, val availableBudgetMinutes: Int, val reservedBreakMinutes: Int,
) {
    val isPartialWeek get() = activeDays in 1..6
    val unallocatedBudgetMinutes get() = (availableBudgetMinutes - plannedMinutes).coerceAtLeast(0)
    val remainingGoalMinutes get() = goalMinutes?.let { (it - plannedMinutes).coerceAtLeast(0) }
    val aboveGoalMinutes get() = goalMinutes?.let { (plannedMinutes - it).coerceAtLeast(0) }
    val budgetShortfallMinutes get() = goalMinutes?.let { (it - availableBudgetMinutes).coerceAtLeast(0) }
    val progress get() = goalMinutes?.takeIf { it > 0 }?.let { (plannedMinutes.toFloat() / it).coerceIn(0f, 1f) }
    val percentPlanned get() = goalMinutes?.takeIf { it > 0 }?.let { (plannedMinutes * 100 / it).coerceIn(0, 100) }
    val status get() = when {
        activeDays == 0 -> WeekPlanningStatus.OutsidePlan
        goalMinutes == null -> WeekPlanningStatus.NoGoal
        aboveGoalMinutes!! > 0 -> WeekPlanningStatus.AboveGoal
        goalMinutes == 0 -> WeekPlanningStatus.ZeroGoal
        budgetShortfallMinutes!! > 0 -> WeekPlanningStatus.BudgetShortfall
        remainingGoalMinutes!! > 0 -> WeekPlanningStatus.UnfilledGoal
        else -> WeekPlanningStatus.FullyPlanned
    }
}

/** Focus budgets are not free clock slots; minimum breaks are reported separately. */
internal fun plannerWeekOverview(plan: StudyPlan, weekStart: LocalDate): PlannerWeekOverview {
    require(weekStart.dayOfWeek.value == 1)
    val end = plan.config.courses.maxOf { it.finishBy }
    val dates = (0L..6L).map(weekStart::plusDays).filter { it in plan.config.start..end }
    val planned = plan.blocks.filter { it.date in weekStart..weekStart.plusDays(6) }.sumOf { it.minutes }
    return PlannerWeekOverview(weekStart, dates.size, plan.config.weeklyGoalMinutes.takeIf { dates.isNotEmpty() },
        planned, dates.sumOf { plan.config.capacity(it) }, dates.sumOf { date -> plannedStudyBreaks(plan, date).sumOf { it.minutes } })
}

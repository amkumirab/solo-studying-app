package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate

internal enum class PlannerPriorityBand(val levels: IntRange) {
    Low(1..2), Normal(3..3), High(4..5),
}

internal data class PlannerReportDay(val date: LocalDate, val plannedMinutes: Int, val sessionCount: Int, val breakMinutes: Int)
internal data class PlannerReportCourse(
    val bossId: Int, val priority: Int, val plannedMinutes: Int, val creditedSeconds: Long, val missingCourse: Boolean,
)
internal data class PlannerReportPriority(val band: PlannerPriorityBand, val plannedMinutes: Int)
internal data class WeeklyPlannerReport(
    val weekStart: LocalDate, val days: List<PlannerReportDay>, val courses: List<PlannerReportCourse>,
    val priorities: List<PlannerReportPriority>,
) {
    val plannedMinutes: Int get() = days.sumOf { it.plannedMinutes }
    val creditedSeconds: Long get() = courses.sumOf { it.creditedSeconds }
    val breakMinutes: Int get() = days.sumOf { it.breakMinutes }
    val busiestDays: List<LocalDate> get() = days.maxOf { it.plannedMinutes }.takeIf { it > 0 }?.let { maximum ->
        days.filter { it.plannedMinutes == maximum }.map { it.date }
    }.orEmpty()
}

/** Scheduled dates describe planned work; credit follows the full ID ledger, not actual activity dates. */
internal fun weeklyPlannerReport(plan: StudyPlan, bosses: List<BossEntity>, weekStart: LocalDate): WeeklyPlannerReport {
    require(weekStart.dayOfWeek.value == 1)
    val weekEnd = weekStart.plusDays(6)
    val entries = blockProgress(plan, bosses).filter { it.block.date in weekStart..weekEnd }
    val byDate = entries.groupBy { it.block.date }
    val byCourse = entries.groupBy { it.block.bossId }
    val availableIds = bosses.map { it.id }.toSet()
    val days = (0L..6L).map { offset ->
        val date = weekStart.plusDays(offset)
        val day = byDate[date].orEmpty()
        PlannerReportDay(date, day.sumOf { it.block.minutes }, day.size, plannedStudyBreaks(plan, date).sumOf { it.minutes })
    }
    val courses = plan.config.courses.map { course ->
        val allocated = byCourse[course.bossId].orEmpty()
        PlannerReportCourse(course.bossId, course.priority, allocated.sumOf { it.block.minutes },
            allocated.sumOf { it.creditedSeconds }, course.bossId !in availableIds)
    }.sortedWith(compareByDescending<PlannerReportCourse> { it.plannedMinutes }.thenBy { it.bossId })
    val priorities = PlannerPriorityBand.entries.map { band ->
        PlannerReportPriority(band, courses.filter { it.priority in band.levels }.sumOf { it.plannedMinutes })
    }
    return WeeklyPlannerReport(weekStart, days, courses, priorities)
}

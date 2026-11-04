package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate

internal enum class PlannerAdviceAction { Availability, Subjects, EditAnswers, ReviewPlan }

internal sealed interface PlannerAdvice {
    val action: PlannerAdviceAction?

    data class MissingCourses(val count: Int) : PlannerAdvice {
        override val action = PlannerAdviceAction.EditAnswers
    }
    data class Deadline(val bossId: Int, val finishBy: LocalDate, val uncoveredMinutes: Int,
        val overdue: Boolean) : PlannerAdvice {
        override val action get() = if (overdue) PlannerAdviceAction.EditAnswers else PlannerAdviceAction.ReviewPlan
    }
    data class Priority(val bossId: Int, val priority: Int, val plannedMinutes: Int) : PlannerAdvice {
        override val action get() = if (plannedMinutes == 0) PlannerAdviceAction.ReviewPlan else PlannerAdviceAction.Subjects
    }
    data class Weekly(val status: WeekPlanningStatus, val plannedMinutes: Int, val goalGapMinutes: Int,
        val budgetGapMinutes: Int) : PlannerAdvice {
        override val action get() = when (status) {
            WeekPlanningStatus.BudgetShortfall -> PlannerAdviceAction.Availability
            WeekPlanningStatus.UnfilledGoal, WeekPlanningStatus.AboveGoal -> PlannerAdviceAction.EditAnswers
            else -> null
        }
    }
}

/** Read-only advice. Coverage uses the same ID-ordered study ledger as the calendar. */
internal fun plannerGuidance(plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate,
    weekStart: LocalDate): List<PlannerAdvice> {
    val overview = plannerWeekOverview(plan, weekStart)
    // An expired plan still needs a repair warning when opening the current week.
    if (overview.activeDays == 0 && weekStart != plannerWeekStart(today)) return emptyList()
    val byId = bosses.associateBy { it.id }
    val remaining = remainingPlannerCourses(plan, bosses)
    val progress = blockProgress(plan, bosses).groupBy { it.block.bossId }
    val missingCount = plan.config.courses.count { it.bossId !in byId }
    val deadline = remaining.filter { it.finishBy <= today.plusDays(14) }.map { course ->
        val accepted = plan.config.courses.first { it.bossId == course.bossId }
        val credit = (byId.getValue(course.bossId).timeSpentSeconds.coerceAtLeast(0) - accepted.baselineSeconds)
            .coerceIn(0, accepted.remainingMinutes * 60L)
        val futureCoverage = progress[course.bossId].orEmpty().filter { it.block.date >= today }.sumOf { it.remainingSeconds }
        val deficit = (accepted.remainingMinutes * 60L - credit - futureCoverage).coerceAtLeast(0)
        PlannerAdvice.Deadline(course.bossId, course.finishBy, ((deficit + 59) / 60).toInt(), course.finishBy < today)
    }.sortedWith(compareByDescending<PlannerAdvice.Deadline> { it.overdue }
        .thenByDescending { it.uncoveredMinutes > 0 }.thenBy { it.finishBy }.thenBy { it.bossId }).firstOrNull()
    val priority = remaining.filter { overview.activeDays > 0 && it.finishBy >= maxOf(today, weekStart) && weekStart.plusDays(6) >= today }
        .sortedWith(compareByDescending<PlannerCourse> { it.priority }.thenBy { it.bossId }).firstOrNull()
        ?.takeIf { it.bossId != deadline?.bossId }?.let { course ->
            PlannerAdvice.Priority(course.bossId, course.priority,
                plan.blocks.filter { it.bossId == course.bossId && it.date in weekStart..weekStart.plusDays(6) }.sumOf { it.minutes })
        }
    return buildList {
        if (missingCount > 0) add(PlannerAdvice.MissingCourses(missingCount))
        deadline?.let(::add)
        if (size < 2) priority?.let(::add)
        if (overview.goalMinutes != null && overview.goalMinutes > 0 && remaining.isNotEmpty() && missingCount == 0) {
            add(PlannerAdvice.Weekly(overview.status, overview.plannedMinutes,
                overview.remainingGoalMinutes!!, overview.budgetShortfallMinutes!!))
        }
    }.take(3)
}

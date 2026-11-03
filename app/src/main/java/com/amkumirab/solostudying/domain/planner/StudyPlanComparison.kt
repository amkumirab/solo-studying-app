package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import java.util.ArrayDeque

internal enum class StudyPlanChangeKind { Kept, Replanned, Added, Removed }

internal data class StudyPlanChange(val before: StudyBlock?, val after: StudyBlock?) {
    init { require(before != null || after != null) }
    val kind get() = when {
        before == null -> StudyPlanChangeKind.Added
        after == null -> StudyPlanChangeKind.Removed
        before.slotKey() == after.slotKey() -> StudyPlanChangeKind.Kept
        else -> StudyPlanChangeKind.Replanned
    }
}

internal data class StudyCourseComparison(val bossId: Int, val acceptedMinutes: Int, val proposedMinutes: Int)
internal data class StudyWeekComparison(
    val accepted: PlannerWeekOverview, val proposed: PlannerWeekOverview, val courses: List<StudyCourseComparison>,
)

internal data class StudyPlanComparison(val accepted: StudyPlan, val proposed: StudyPlan, val changes: List<StudyPlanChange>) {
    val manualSlotsReplaced get() = changes.count { it.before?.manual == true && it.kind != StudyPlanChangeKind.Kept }

    fun week(start: LocalDate): StudyWeekComparison {
        val end = start.plusDays(6)
        fun totals(plan: StudyPlan) = plan.blocks.filter { it.date in start..end }
            .groupBy { it.bossId }.mapValues { (_, blocks) -> blocks.sumOf { it.minutes } }
        val before = totals(accepted)
        val after = totals(proposed)
        val ids = (accepted.config.courses + proposed.config.courses).map { it.bossId }.distinct().sorted()
        return StudyWeekComparison(plannerWeekOverview(accepted, start), plannerWeekOverview(proposed, start),
            ids.map { StudyCourseComparison(it, before[it] ?: 0, after[it] ?: 0) })
    }
}

private data class StudySlotKey(val bossId: Int, val date: LocalDate, val start: Int?, val minutes: Int,
    val locked: Boolean, val manual: Boolean, val durationEdited: Boolean)
private fun StudyBlock.slotKey() = StudySlotKey(bossId, date, startMinute, minutes, locked, manual, durationEdited)
private val slotOrder = compareBy<StudyBlock> { it.date }.thenBy { it.startMinute ?: -1 }.thenBy { it.id }

/** Pair unchanged slots first. Replanned pairs are chronological comparisons, never credit identities. */
internal fun compareStudyPlans(accepted: StudyPlan, proposed: StudyPlan): StudyPlanComparison {
    val candidates = proposed.blocks.sortedWith(slotOrder).groupBy { it.slotKey() }
        .mapValues { (_, slots) -> ArrayDeque(slots) }
    val changes = mutableListOf<StudyPlanChange>()
    val unmatched = mutableListOf<StudyBlock>()
    accepted.blocks.sortedWith(slotOrder).forEach { old ->
        val exact = candidates[old.slotKey()]?.pollFirst()
        if (exact != null) changes += StudyPlanChange(old, exact) else unmatched += old
    }
    val replacements = candidates.values.flatMap { it.toList() }.sortedWith(slotOrder).groupBy { it.bossId }
        .mapValues { (_, slots) -> ArrayDeque(slots) }
    unmatched.forEach { old -> changes += StudyPlanChange(old, replacements[old.bossId]?.pollFirst()) }
    replacements.values.flatMap { it.toList() }.forEach { changes += StudyPlanChange(null, it) }
    return StudyPlanComparison(accepted, proposed, changes.sortedWith(compareBy<StudyPlanChange> {
        minOf(it.before?.date ?: LocalDate.MAX, it.after?.date ?: LocalDate.MAX)
    }.thenBy { it.before?.bossId ?: it.after!!.bossId }.thenBy { it.before?.id ?: it.after!!.id }))
}

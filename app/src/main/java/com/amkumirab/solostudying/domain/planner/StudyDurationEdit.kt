package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate

data class StudyDurationEdit(val plan: StudyPlan?, val error: String? = null)

/** An immutable proposal; callers show all affected slots before saving atomically. */
fun previewStudyDuration(plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, blockId: Int, minutes: Int): StudyDurationEdit {
    fun rejected(message: String) = StudyDurationEdit(null, message)
    if (minutes !in 1..120) return rejected("Enter a whole duration from 1 to 120 minutes.")
    if (plan.config.studyWindows == null) return rejected("Save study windows and accept a timed plan before editing durations.")
    val progress = blockProgress(plan, bosses).associateBy { it.block.id }
    val entry = progress[blockId] ?: return rejected("This session is no longer available.")
    val block = entry.block
    if (block.locked) return rejected("Unlock this session before editing its duration.")
    if (block.date < today || entry.missingCourse || entry.creditedSeconds != 0L) {
        return rejected("Choose an unstudied session dated today or later with an available course.")
    }
    if (minutes == block.minutes) return StudyDurationEdit(plan)
    val delta = minutes - block.minutes
    val reductions = mutableMapOf<Int, Int>()
    if (delta > 0) {
        val week = plannerWeekStart(block.date)
        val weekBlocks = plan.blocks.filter { plannerWeekStart(it.date) == week }
        val policy = plan.config.weeklyScheduling
        val weeklyExcess = if (policy == null) 0 else maxOf(0,
            weekBlocks.sumOf { it.minutes } + delta - plan.config.weeklyGoalMinutes!!,
            weekBlocks.filter { it.bossId == block.bossId }.sumOf { it.minutes } + delta -
                (policy.courseMinuteLimits[block.bossId] ?: Int.MAX_VALUE))
        val donors = plan.blocks.filter {
            it.bossId == block.bossId && it.date > block.date && !it.locked && progress.getValue(it.id).creditedSeconds == 0L
        }.sortedWith(compareByDescending<StudyBlock> { it.date }.thenByDescending { it.startMinute }.thenByDescending { it.id })
        fun take(candidates: List<StudyBlock>, requested: Int): Int {
            var left = requested
            for (donor in candidates) {
                val amount = minOf(left, donor.minutes - (reductions[donor.id] ?: 0))
                if (amount > 0) reductions[donor.id] = (reductions[donor.id] ?: 0) + amount
                left -= amount
                if (left == 0) break
            }
            return left
        }
        // A full target week needs same-week transfers, even when another week has spare time.
        if (take(donors.filter { plannerWeekStart(it.date) == week }, weeklyExcess) > 0) {
            return rejected("This week or course weekly limit has no eligible later time to transfer.")
        }
        val needed = maxOf(weeklyExcess, delta - (plan.shortfalls[block.bossId] ?: 0)) - reductions.values.sum()
        if (take(donors, needed) > 0) {
            return rejected("Not enough unscheduled work or later-day, unlocked sessions for this course. Locked and studied time cannot be taken.")
        }
    }
    val blocks = plan.blocks.mapNotNull { current ->
        val reducedBy = reductions[current.id] ?: 0
        when {
            current.id == blockId -> current.copy(minutes = minutes, durationEdited = true)
            reducedBy == 0 -> current
            reducedBy == current.minutes -> null
            else -> current.copy(minutes = current.minutes - reducedBy, durationEdited = true)
        }
    }
    val proposed = runCatching { plan.copy(blocks = blocks) }.getOrNull()
        ?: return rejected("That duration overlaps a session, leaves saved windows or exceeds a daily or weekly limit. No sessions were changed.")
    if (blockProgress(proposed, bosses).any { it.creditedSeconds != progress.getValue(it.block.id).creditedSeconds }) {
        return rejected("This change would move recorded study credit. No sessions were changed.")
    }
    return StudyDurationEdit(proposed)
}

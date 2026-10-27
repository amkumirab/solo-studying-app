package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate

/** Replace only unstudied planned work; recorded credit must stay on surviving IDs. */
fun previewReassignStudySession(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, blockId: Int, targetBossId: Int,
): StudySessionEdit {
    fun rejected(message: String) = StudySessionEdit(null, message)
    if (plan.config.studyWindows == null) return rejected("Accept a timed plan before changing a session's subject.")
    val progress = blockProgress(plan, bosses)
    val entry = progress.firstOrNull { it.block.id == blockId }
        ?: return rejected("This session is no longer available.")
    val block = entry.block
    if (block.locked) return rejected("Unlock this session before changing its subject.")
    if (block.date < today || entry.missingCourse || entry.creditedSeconds != 0L || bosses.none { it.id == block.bossId && !it.isCompleted }) {
        return rejected("Only unstudied sessions dated today or later with an unfinished course can change subject.")
    }
    val target = plan.config.courses.firstOrNull { it.bossId == targetBossId }
    if (target == null || bosses.none { it.id == targetBossId && !it.isCompleted }) return rejected("Choose an unfinished course from this plan.")
    if (targetBossId == block.bossId) return StudySessionEdit(plan)
    if (block.date > target.finishBy) return rejected("This session falls after the selected course's deadline. Choose another course or move the session first.")
    if (block.minutes > (plan.shortfalls[targetBossId] ?: 0)) return rejected("Not enough unscheduled work for the selected course. Shorten or remove one of its unstudied sessions first, or edit answers to change the workload.")
    val lastId = plan.blocks.maxOf { it.id }
    if (lastId == Int.MAX_VALUE) return rejected("This plan has reached its session ID limit. Rebuild the remaining plan before changing subjects.")

    // Reusing the old ID could take recorded credit from a later destination block.
    val replacement = block.copy(id = lastId + 1, bossId = targetBossId, manual = true)
    val proposed = runCatching { plan.copy(blocks = plan.blocks.filterNot { it.id == blockId } + replacement) }.getOrNull()
        ?: return rejected("The selected course's weekly limit does not allow this session. No sessions were changed.")
    val before = progress.associate { it.block.id to it.creditedSeconds }
    if (blockProgress(proposed, bosses).any { it.creditedSeconds != (before[it.block.id] ?: 0L) }) {
        return rejected("Some unscheduled work already has recorded study credit. Rebuild the remaining plan before changing subjects. Your current plan is unchanged.")
    }
    return StudySessionEdit(proposed)
}

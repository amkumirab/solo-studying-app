package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate

data class StudySessionEdit(val plan: StudyPlan?, val error: String? = null)

/** Use unallocated work only; adding never moves or shortens accepted sessions. */
fun previewAddStudySession(
    plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, bossId: Int,
    date: LocalDate, startMinute: Int, minutes: Int,
): StudySessionEdit {
    fun rejected(message: String) = StudySessionEdit(null, message)
    if (plan.config.studyWindows == null) return rejected("Save study windows and accept a timed plan before adding sessions.")
    if (minutes !in 1..120 || startMinute !in 0..1439) return rejected("Use 1 to 120 whole minutes and a start time from 00:00 to 23:59.")
    val course = plan.config.courses.firstOrNull { it.bossId == bossId }
    if (course == null || bosses.none { it.id == bossId && !it.isCompleted }) return rejected("Choose an unfinished course from this plan.")
    if (date < today || date !in plan.config.start..course.finishBy) return rejected("Choose a date from today or the plan start through the course deadline.")
    if (minutes > (plan.shortfalls[bossId] ?: 0)) return rejected("Not enough unscheduled work for this course. Shorten or remove an unstudied session first, or edit answers to change the workload.")
    val lastId = plan.blocks.maxOfOrNull { it.id } ?: 0
    if (lastId == Int.MAX_VALUE || plan.blocks.size >= 18000) return rejected("This plan has reached its session limit. Rebuild the remaining plan before adding more.")
    val block = StudyBlock(lastId + 1, bossId, date, minutes, startMinute, manual = true)
    val proposed = runCatching { plan.copy(blocks = plan.blocks + block) }.getOrNull()
        ?: return rejected("This slot overlaps another session, leaves saved windows or exceeds a daily or weekly limit. No sessions were changed.")
    val before = blockProgress(plan, bosses).associate { it.block.id to it.creditedSeconds }
    if (blockProgress(proposed, bosses).any { it.creditedSeconds != (before[it.block.id] ?: 0L) }) {
        return rejected("Some unscheduled work already has recorded study credit. Rebuild the remaining plan before adding a session. Your current plan is unchanged.")
    }
    return StudySessionEdit(proposed)
}

/** Removing a zero-credit slot returns planned time to the same course's shortfall. */
fun previewRemoveStudySession(plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate, blockId: Int): StudySessionEdit {
    if (plan.config.studyWindows == null) return StudySessionEdit(null, "Accept a timed plan before removing individual sessions.")
    val progress = blockProgress(plan, bosses)
    val entry = progress.firstOrNull { it.block.id == blockId }
        ?: return StudySessionEdit(null, "This session is no longer available.")
    if (entry.block.locked) return StudySessionEdit(null, "Unlock this session before removing it.")
    if (entry.block.date < today || entry.missingCourse || entry.creditedSeconds != 0L) {
        return StudySessionEdit(null, "Only unstudied sessions dated today or later with an available course can be removed.")
    }
    val proposed = plan.copy(blocks = plan.blocks.filterNot { it.id == blockId })
    val before = progress.associate { it.block.id to it.creditedSeconds }
    check(blockProgress(proposed, bosses).all { it.creditedSeconds == before[it.block.id] })
    return StudySessionEdit(proposed)
}

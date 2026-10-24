package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate

fun todayStudyBlocks(plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate): List<StudyBlockProgress> =
    blockProgress(plan, bosses).filter { it.block.date == today }

data class StudyDayPostponement(
    val plan: StudyPlan?,
    val moves: List<StudyBlock>,
    val unmovedMinutes: Map<Int, Int>,
)

/** Preserve slot sizes and credit IDs; apply only when every available block fits. */
fun postponeTodayStudy(plan: StudyPlan, bosses: List<BossEntity>, today: LocalDate): StudyDayPostponement {
    val courses = plan.config.courses.associateBy { it.bossId }
    val remaining = todayStudyBlocks(plan, bosses, today)
        .filter { !it.missingCourse && it.remainingSeconds > 0 }
        .sortedWith(compareBy<StudyBlockProgress> { courses.getValue(it.block.bossId).finishBy }
            .thenByDescending { courses.getValue(it.block.bossId).priority }.thenBy { it.block.id })
    val ledger = PlannerCapacityLedger(plan.config, plan.blocks)
    val moves = mutableListOf<StudyBlock>()
    val unmoved = mutableMapOf<Int, Int>()
    for (entry in remaining) {
        val block = entry.block
        if (block.locked) {
            unmoved[block.bossId] = (unmoved[block.bossId] ?: 0) + entry.remainingMinutes
            continue
        }
        val deadline = courses.getValue(block.bossId).finishBy
        ledger.release(block)
        var date = today.plusDays(1)
        while (date <= deadline && !ledger.canPlace(block, date)) date = date.plusDays(1)
        if (date > deadline) {
            ledger.reserve(block)
            unmoved[block.bossId] = (unmoved[block.bossId] ?: 0) + entry.remainingMinutes
        }
        else {
            val moved = block.copy(date = date, startMinute = if (plan.config.studyWindows == null) null else ledger.earliestStart(block, date))
            moves += moved
            ledger.reserve(moved)
        }
    }
    val movedById = moves.associateBy { it.id }
    val updated = if (unmoved.isEmpty()) plan.copy(blocks = plan.blocks.map { movedById[it.id] ?: it }) else null
    return StudyDayPostponement(updated, moves, unmoved)
}

package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate

/** Mutable reservations for one proposed move operation; never writes storage. */
internal class PlannerCapacityLedger(private val config: PlannerConfig, blocks: List<StudyBlock>) {
    private val courses = config.courses.associateBy { it.bossId }
    private val daily = mutableMapOf<LocalDate, Int>()
    private val weekly = mutableMapOf<LocalDate, Int>()
    private val courseWeekly = mutableMapOf<Pair<Int, LocalDate>, Int>()
    private val slots = mutableMapOf<Int, StudyBlock>()
    init { blocks.forEach { reserve(it) } }

    fun canPlace(block: StudyBlock, date: LocalDate): Boolean {
        val course = courses[block.bossId] ?: return false
        if (date !in config.start..course.finishBy || (daily[date] ?: 0) + block.minutes > config.capacity(date)) return false
        if (config.studyWindows != null && earliestStart(block, date) == null) return false
        val policy = config.weeklyScheduling ?: return true
        val week = plannerWeekStart(date)
        return (weekly[week] ?: 0) + block.minutes <= config.weeklyGoalMinutes!! &&
            (courseWeekly[block.bossId to week] ?: 0) + block.minutes <= (policy.courseMinuteLimits[block.bossId] ?: Int.MAX_VALUE)
    }

    fun earliestStart(block: StudyBlock, date: LocalDate): Int? {
        val day = config.studyWindows?.days?.get(date.dayOfWeek.value - 1) ?: return null
        if (!day.enabled) return null
        return firstStudySlot(day.windows, slots.values.filter { it.date == date }, block.minutes)
    }

    fun reserve(block: StudyBlock) = change(block, block.minutes)
    fun release(block: StudyBlock) = change(block, -block.minutes)
    private fun change(block: StudyBlock, delta: Int) {
        if (delta > 0) slots[block.id] = block else slots.remove(block.id)
        val week = plannerWeekStart(block.date)
        daily[block.date] = (daily[block.date] ?: 0) + delta
        weekly[week] = (weekly[week] ?: 0) + delta
        val key = block.bossId to week
        courseWeekly[key] = (courseWeekly[key] ?: 0) + delta
    }
}

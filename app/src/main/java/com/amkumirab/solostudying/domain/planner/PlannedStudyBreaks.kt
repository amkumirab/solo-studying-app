package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate

internal fun PlannerConfig.plannableStudyCapacity(date: LocalDate): Int =
    if (breakMinutes == 0) capacity(date) else minOf(capacity(date), breakAwareStudyCapacity(this, date))

/** Free clock intervals after reserving minimum spacing on both sides of sessions. */
internal fun studyFreeGaps(windows: List<StudyTimeWindow>, occupied: List<StudyBlock>, breakMinutes: Int): List<Pair<Int, Int>> = buildList {
    val ordered = occupied.sortedBy { it.startMinute }
    windows.forEach { window ->
        var cursor = window.startMinute
        ordered.forEach { block ->
            val start = checkNotNull(block.startMinute) - breakMinutes
            val end = block.startMinute + block.minutes + breakMinutes
            if (end > cursor && start < window.endMinute) {
                if (cursor < start) add(cursor to minOf(start, window.endMinute))
                cursor = maxOf(cursor, end)
            }
        }
        if (cursor < window.endMinute) add(cursor to window.endMinute)
    }
}

/** Capacity for preferred-length sessions; final packing accounts for course boundaries. */
internal fun breakAwareStudyCapacity(config: PlannerConfig, date: LocalDate, occupied: List<StudyBlock> = emptyList()): Int {
    val day = checkNotNull(config.studyWindows).days[date.dayOfWeek.value - 1]
    if (!day.enabled) return 0
    var lastEnd = -config.breakMinutes
    var total = 0
    studyFreeGaps(day.windows, occupied, config.breakMinutes).forEach { (start, end) ->
        var cursor = maxOf(start, lastEnd + config.breakMinutes)
        while (cursor < end) {
            val minutes = minOf(config.sessionMinutes, end - cursor)
            total += minutes
            lastEnd = cursor + minutes
            cursor = lastEnd + config.breakMinutes
        }
    }
    return total
}

/** Carry fragments to later valid days in the same week; never count dropped work as placed. */
internal fun packStudyWeekWithBreaks(config: PlannerConfig, dated: StudyPlan, locks: List<StudyBlock>): List<StudyBlock> {
    val result = mutableListOf<StudyBlock>()
    val left = dated.blocks.associate { it.id to it.minutes }.toMutableMap()
    val deadlines = config.courses.associate { it.bossId to it.finishBy }
    val end = minOf(plannerWeekStart(dated.config.start).plusDays(6), config.courses.maxOf { it.finishBy })
    var date = dated.config.start
    while (date <= end) {
        val day = checkNotNull(config.studyWindows).days[date.dayOfWeek.value - 1]
        val occupied = locks.filter { it.date == date }.toMutableList()
        var budget = config.capacity(date) - occupied.sumOf { it.minutes }
        if (day.enabled && budget > 0) {
            val ready = dated.blocks.filter { it.date <= date && deadlines.getValue(it.bossId) >= date && left.getValue(it.id) > 0 }
                .sortedWith(compareBy<StudyBlock> { deadlines.getValue(it.bossId) }.thenBy { it.id })
            ready.forEach { block ->
                while (left.getValue(block.id) > 0 && budget > 0) {
                    val gap = studyFreeGaps(day.windows, occupied, config.breakMinutes).firstOrNull() ?: break
                    val minutes = minOf(left.getValue(block.id), gap.second - gap.first, budget)
                    val placed = block.copy(id = result.size + 1, date = date, minutes = minutes, startMinute = gap.first)
                    result += placed
                    occupied += placed
                    left[block.id] = left.getValue(block.id) - minutes
                    budget -= minutes
                }
            }
        }
        date = date.plusDays(1)
    }
    return result
}

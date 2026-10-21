package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import java.util.Locale

fun plannerClockLabel(minute: Int): String {
    require(minute in 0..1440)
    return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)
}

fun parsePlannerClock(text: String): Int? {
    if (!Regex("[0-2][0-9]:[0-5][0-9]").matches(text)) return null
    val hour = text.take(2).toInt()
    return if (hour > 23) null else hour * 60 + text.takeLast(2).toInt()
}

fun StudyBlock.clockRange(): String? = startMinute?.let { "${plannerClockLabel(it)}–${plannerClockLabel(it + minutes)}" }

internal fun validateStudyTimes(config: PlannerConfig, blocks: List<StudyBlock>) {
    val availability = config.studyWindows
    if (availability == null) { require(blocks.all { it.startMinute == null }); return }
    require(blocks.all { block ->
        val start = block.startMinute
        val day = availability.days[block.date.dayOfWeek.value - 1]
        start != null && start in 0..1439 && day.enabled &&
            day.windows.any { start >= it.startMinute && start + block.minutes <= it.endMinute }
    })
    require(blocks.groupBy { it.date }.values.all { day ->
        day.sortedBy { it.startMinute }.zipWithNext().all { (a, b) -> a.startMinute!! + a.minutes <= b.startMinute!! }
    })
}

/** Only used for a newly generated draft; never repack an accepted credit ledger. */
internal fun assignStudyTimes(datePlan: StudyPlan, config: PlannerConfig): StudyPlan {
    val availability = checkNotNull(config.studyWindows)
    val result = mutableListOf<StudyBlock>()
    datePlan.blocks.groupBy { it.date }.toSortedMap().forEach { (date, blocks) ->
        val windows = availability.days[date.dayOfWeek.value - 1].windows
        var index = 0
        var cursor = windows.first().startMinute
        blocks.forEach { block ->
            var remaining = block.minutes
            while (remaining > 0) {
                if (cursor == windows[index].endMinute) { index++; cursor = windows[index].startMinute }
                val minutes = minOf(remaining, windows[index].endMinute - cursor)
                result += block.copy(id = result.size + 1, minutes = minutes, startMinute = cursor)
                cursor += minutes
                remaining -= minutes
            }
        }
    }
    return StudyPlan(config, result)
}

/** Earliest complete gap; accepted blocks cannot be split because their IDs carry credit. */
internal fun firstStudySlot(windows: List<StudyTimeWindow>, occupied: List<StudyBlock>, minutes: Int): Int? {
    val ordered = occupied.sortedBy { it.startMinute }
    windows.forEach { window ->
        var cursor = window.startMinute
        ordered.filter { it.startMinute!! < window.endMinute && it.startMinute + it.minutes > window.startMinute }.forEach { block ->
            if (cursor + minutes <= block.startMinute!!) return cursor
            cursor = maxOf(cursor, block.startMinute + block.minutes)
        }
        if (cursor + minutes <= window.endMinute) return cursor
    }
    return null
}

/** Move one unstudied slot, preserving list order, ID, duration and credit baselines. */
fun moveTimedStudyBlock(plan: StudyPlan, blockId: Int, date: LocalDate, startMinute: Int, today: LocalDate, creditedSeconds: Long): StudyPlan? {
    if (plan.config.studyWindows == null || creditedSeconds != 0L || date < today) return null
    val block = plan.blocks.firstOrNull { it.id == blockId } ?: return null
    val ledger = PlannerCapacityLedger(plan.config, plan.blocks)
    ledger.release(block)
    if (!ledger.canPlaceAt(block, date, startMinute)) return null
    return plan.copy(blocks = plan.blocks.map { if (it.id == blockId) it.copy(date = date, startMinute = startMinute) else it })
}

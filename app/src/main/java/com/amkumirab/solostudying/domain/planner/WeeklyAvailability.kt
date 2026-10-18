package com.amkumirab.solostudying.domain.planner

/** Local wall-clock minutes; 1440 means the exclusive end of the same day. */
data class StudyTimeWindow(val startMinute: Int, val endMinute: Int) {
    init {
        require(startMinute in 0..1410 && endMinute in 30..1440 && startMinute < endMinute)
        require(startMinute % 30 == 0 && endMinute % 30 == 0)
    }
    val minutes get() = endMinute - startMinute
}

data class StudyDayAvailability(val enabled: Boolean, val windows: List<StudyTimeWindow>) {
    init {
        require(windows.size <= 16 && (!enabled || windows.isNotEmpty()))
        require(windows.zipWithNext().all { (first, next) -> first.endMinute <= next.startMinute })
        require(windows.sumOf { it.minutes } <= 480)
    }
    val capacityMinutes get() = if (enabled) windows.sumOf { it.minutes } else 0
}

/** ISO weekday order: Monday first, Sunday last. An all-rest week is valid. */
data class WeeklyAvailability(val days: List<StudyDayAvailability>) {
    init { require(days.size == 7) }
    val weekdayMinutes get() = days.map { it.capacityMinutes }
    val totalMinutes get() = weekdayMinutes.sum()
}

/** An unsaved suggestion only; never migrate legacy budgets into clock times on read. */
fun defaultWeeklyAvailability(weekdayMinutes: List<Int>): WeeklyAvailability {
    require(weekdayMinutes.size == 7 && weekdayMinutes.all { it in 0..480 })
    return WeeklyAvailability(weekdayMinutes.mapIndexed { index, legacyMinutes ->
        val minutes = legacyMinutes / 30 * 30
        val start = minOf(if (index == 5) 600 else 1020, 1440 - minutes)
        StudyDayAvailability(minutes > 0, if (minutes == 0) emptyList() else listOf(StudyTimeWindow(start, start + minutes)))
    })
}

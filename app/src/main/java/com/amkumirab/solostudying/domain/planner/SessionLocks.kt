package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate

/** Flag-only mutation: never changes the accepted credit ledger. */
fun setStudyBlockLocked(plan: StudyPlan, id: Int, locked: Boolean, bosses: List<BossEntity>, today: LocalDate): StudyPlan? {
    val entry = blockProgress(plan, bosses).firstOrNull { it.block.id == id } ?: return null
    if (plan.config.studyWindows == null) return null
    if (locked && (entry.block.date < today || entry.missingCourse || entry.creditedSeconds != 0L)) return null
    return plan.copy(blocks = plan.blocks.map { if (it.id == id) it.copy(locked = locked) else it })
}

data class LockedStudyRebuild(val plan: StudyPlan?, val error: String? = null)

/** A rejected proposal never writes storage or changes the source plan. */
fun rebuildWithStudyLocks(config: PlannerConfig, previous: StudyPlan?, bosses: List<BossEntity>, today: LocalDate): LockedStudyRebuild {
    val locks = previous?.blocks.orEmpty().filter { it.locked }
    if (locks.isEmpty()) return LockedStudyRebuild(buildStudyPlan(config))
    val progress = blockProgress(checkNotNull(previous), bosses).associateBy { it.block.id }
    if (locks.any { it.date < today || progress.getValue(it.id).let { entry -> entry.missingCourse || entry.creditedSeconds != 0L } }) {
        return LockedStudyRebuild(null, "Unlock past, studied or unavailable sessions before rebuilding. Your current plan is unchanged.")
    }
    if (runCatching { StudyPlan(config, locks) }.isFailure) {
        return LockedStudyRebuild(null, "Locked sessions conflict with the selected courses, dates, windows, session length or capacity limits. Keep compatible settings or unlock those sessions. Your current plan is unchanged.")
    }
    return runCatching { LockedStudyRebuild(buildAroundStudyLocks(config, locks)) }.getOrElse {
        LockedStudyRebuild(null, "Could not fit sessions around the locked slots. Review your settings or unlock sessions. Your current plan is unchanged.")
    }
}

/** Reuse weighted quotas and the daily builder, with locks removed from every capacity. */
internal fun buildAroundStudyLocks(config: PlannerConfig, locks: List<StudyBlock>): StudyPlan {
    val byDay = locks.groupBy { it.date }
    val byWeek = locks.groupBy { plannerWeekStart(it.date) }
    val freeDays = mutableMapOf<LocalDate, Int>()
    fun freeDay(date: LocalDate): Int = freeDays.getOrPut(date) {
        val budget = config.capacity(date) - byDay[date].orEmpty().sumOf { it.minutes }
        if (config.breakMinutes == 0) budget else minOf(budget, breakAwareStudyCapacity(config, date, byDay[date].orEmpty()))
    }
    fun freeGoal(week: LocalDate): Int? = config.weeklyGoalMinutes?.minus(byWeek[week].orEmpty().sumOf { it.minutes })
    fun freeLimit(id: Int, week: LocalDate): Int = config.weeklyScheduling?.courseMinuteLimits?.get(id)?.let { limit ->
        limit - byWeek[week].orEmpty().filter { it.bossId == id }.sumOf { it.minutes }
    } ?: Int.MAX_VALUE
    fun futureCapacity(from: LocalDate, through: LocalDate, id: Int? = null): Int {
        var date = maxOf(from, config.start)
        var total = 0
        while (date <= through) {
            val week = plannerWeekStart(date)
            val end = minOf(week.plusDays(6), through)
            var capacity = 0
            while (date <= end) { capacity += freeDay(date); date = date.plusDays(1) }
            total += minOf(capacity, freeGoal(week) ?: capacity, id?.let { freeLimit(it, week) } ?: Int.MAX_VALUE)
        }
        return total
    }
    val remaining = config.courses.associate { course ->
        course.bossId to course.remainingMinutes - locks.filter { it.bossId == course.bossId }.sumOf { it.minutes }
    }.toMutableMap()
    val blocks = locks.toMutableList()
    val usedIds = locks.map { it.id }.toMutableSet()
    var nextId = 1
    fun append(block: StudyBlock) {
        while (nextId in usedIds) nextId++
        check(blocks.size < 18000)
        blocks += block.copy(id = nextId)
        usedIds += nextId++
    }
    val finish = config.courses.maxOf { it.finishBy }
    var week = plannerWeekStart(config.start)
    while (week <= finish) {
        val start = maxOf(config.start, week)
        val end = week.plusDays(6)
        val capacity = List(7) { freeDay(week.plusDays(it.toLong())) }
        val active = config.courses.filter { it.finishBy >= start && remaining.getValue(it.bossId) > 0 && freeLimit(it.bossId, week) > 0 }
            .map { it.copy(remainingMinutes = remaining.getValue(it.bossId)) }
        if (active.isNotEmpty() && capacity.any { it > 0 } && freeGoal(week) != 0) {
            val limits = active.mapNotNull { course -> freeLimit(course.bossId, week).takeIf { it != Int.MAX_VALUE }?.let { course.bossId to it } }.toMap()
            val input = config.copy(start = start, courses = active, weekdayMinutes = capacity, studyWindows = null, breakMinutes = 0,
                weeklyGoalMinutes = freeGoal(week), weeklyScheduling = if (config.weeklyScheduling == null) null else WeeklySchedulingPolicy(limits))
            val needs = WeeklyAllocationNeeds(
                active.associate { it.bossId to (it.remainingMinutes - futureCapacity(end.plusDays(1), it.finishBy, it.bossId)).coerceAtLeast(0) },
                active.map { it.finishBy }.distinct().associateWith { deadline ->
                    (active.filter { it.finishBy <= deadline }.sumOf { it.remainingMinutes } - futureCapacity(end.plusDays(1), deadline)).coerceAtLeast(0)
                },
            )
            val quota = allocateWeeklyHours(input, emptyList(), week, needs)
            val courses = active.mapNotNull { course -> quota.minutesByCourse[course.bossId]?.takeIf { it > 0 }?.let {
                course.copy(remainingMinutes = it, finishBy = minOf(course.finishBy, end))
            } }
            if (courses.isNotEmpty()) {
                val dated = buildStudyPlan(input.copy(courses = courses, weeklyGoalMinutes = null, weeklyScheduling = null))
                check(dated.shortfalls.isEmpty())
                if (config.breakMinutes > 0) {
                    packStudyWeekWithBreaks(config, dated, locks).forEach { block ->
                        append(block)
                        remaining[block.bossId] = remaining.getValue(block.bossId) - block.minutes
                    }
                } else {
                    dated.blocks.groupBy { it.date }.forEach { (date, dayBlocks) ->
                        val gaps = studyFreeGaps(checkNotNull(config.studyWindows).days[date.dayOfWeek.value - 1].windows, byDay[date].orEmpty(), 0)
                        var index = 0
                        var cursor = gaps.first().first
                        dayBlocks.forEach { block ->
                            var minutes = block.minutes
                            while (minutes > 0) {
                                if (cursor == gaps[index].second) { index++; cursor = gaps[index].first }
                                val part = minOf(minutes, gaps[index].second - cursor)
                                append(block.copy(minutes = part, startMinute = cursor))
                                cursor += part
                                minutes -= part
                            }
                            remaining[block.bossId] = remaining.getValue(block.bossId) - block.minutes
                        }
                    }
                }
            }
        }
        week = week.plusWeeks(1)
    }
    return StudyPlan(config, blocks)
}

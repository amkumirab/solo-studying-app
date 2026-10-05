package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PlannerCourse(
    val bossId: Int,
    val remainingMinutes: Int,
    val finishBy: LocalDate,
    val priority: Int,
    val baselineSeconds: Long,
) {
    init {
        require(bossId > 0 && remainingMinutes in 1..60000 && priority in 1..3 && baselineSeconds >= 0)
    }
}

data class PlannerConfig(
    val start: LocalDate,
    val courses: List<PlannerCourse>,
    val weekdayMinutes: List<Int>,
    val sessionMinutes: Int,
) {
    init {
        require(courses.size in 1..20 && courses.distinctBy { it.bossId }.size == courses.size)
        require(courses.all { ChronoUnit.DAYS.between(start, it.finishBy) in 0..365 })
        require(weekdayMinutes.size == 7 && weekdayMinutes.all { it in 0..480 } && weekdayMinutes.any { it > 0 })
        require(sessionMinutes in 15..120)
    }
    fun capacity(date: LocalDate) = weekdayMinutes[date.dayOfWeek.value - 1]
}

data class StudyBlock(val id: Int, val bossId: Int, val date: LocalDate, val minutes: Int)

data class StudyPlan(val config: PlannerConfig, val blocks: List<StudyBlock>) {
    init {
        require(blocks.size <= 18000 && blocks.map { it.id }.distinct().size == blocks.size)
        require(blocks.all { block ->
            val course = config.courses.firstOrNull { it.bossId == block.bossId }
            block.id > 0 && block.minutes in 1..config.sessionMinutes && course != null &&
                block.date in config.start..course.finishBy
        })
        require(blocks.groupBy { it.date }.all { (date, values) -> values.sumOf { it.minutes } <= config.capacity(date) })
        require(config.courses.all { course -> blocks.filter { it.bossId == course.bossId }.sumOf { it.minutes } <= course.remainingMinutes })
    }
    val shortfalls: Map<Int, Int>
        get() = config.courses.associate { course ->
            course.bossId to (course.remainingMinutes - blocks.filter { it.bossId == course.bossId }.sumOf { it.minutes })
        }.filterValues { it > 0 }
}

/** Evenly use capacity while reserving enough work today for every deadline prefix. */
fun buildStudyPlan(config: PlannerConfig): StudyPlan {
    val days = ChronoUnit.DAYS.between(config.start, config.courses.maxOf { it.finishBy }).toInt() + 1
    val capacityPrefix = IntArray(days + 1)
    repeat(days) { index -> capacityPrefix[index + 1] = capacityPrefix[index] + config.capacity(config.start.plusDays(index.toLong())) }
    val remaining = config.courses.associate { it.bossId to it.remainingMinutes }.toMutableMap()
    val blocks = mutableListOf<StudyBlock>()
    repeat(days) { index ->
        val date = config.start.plusDays(index.toLong())
        val available = config.capacity(date)
        val active = config.courses.filter { it.finishBy >= date && remaining.getValue(it.bossId) > 0 }
            .sortedWith(compareBy<PlannerCourse> { it.finishBy }.thenByDescending { it.priority }.thenBy { it.bossId })
        if (available == 0 || active.isEmpty()) return@repeat
        val lastIndex = ChronoUnit.DAYS.between(config.start, active.maxOf { it.finishBy }).toInt()
        val futureCapacity = capacityPrefix[lastIndex + 1] - capacityPrefix[index + 1]
        val total = active.sumOf { remaining.getValue(it.bossId) }
        val spread = ((total.toLong() * available + futureCapacity + available - 1) / (futureCapacity + available)).toInt()
        var dueWork = 0
        var mandatory = 0
        active.groupBy { it.finishBy }.forEach { (deadline, courses) ->
            dueWork += courses.sumOf { remaining.getValue(it.bossId) }
            val deadlineIndex = ChronoUnit.DAYS.between(config.start, deadline).toInt()
            mandatory = maxOf(mandatory, dueWork - (capacityPrefix[deadlineIndex + 1] - capacityPrefix[index + 1]))
        }
        val requested = maxOf(spread, mandatory)
        val fullBlockBudget = ((requested + config.sessionMinutes - 1) / config.sessionMinutes) * config.sessionMinutes
        var budget = minOf(available, fullBlockBudget)
        for (course in active) {
            while (budget > 0 && remaining.getValue(course.bossId) > 0) {
                val minutes = minOf(config.sessionMinutes, budget, remaining.getValue(course.bossId))
                blocks += StudyBlock(blocks.size + 1, course.bossId, date, minutes)
                remaining[course.bossId] = remaining.getValue(course.bossId) - minutes
                budget -= minutes
            }
        }
    }
    return StudyPlan(config, blocks)
}

data class StudyBlockProgress(val block: StudyBlock, val creditedSeconds: Long, val missingCourse: Boolean) {
    val remainingSeconds: Long get() = block.minutes * 60L - creditedSeconds
    val remainingMinutes: Int get() = ((remainingSeconds + 59) / 60).toInt()
}

fun blockProgress(plan: StudyPlan, bosses: List<BossEntity>): List<StudyBlockProgress> {
    val byId = bosses.associateBy { it.id }
    val credit = plan.config.courses.associate { course ->
        val boss = byId[course.bossId]
        course.bossId to when {
            boss == null -> 0L
            boss.isCompleted -> course.remainingMinutes * 60L
            else -> (boss.timeSpentSeconds.coerceAtLeast(0) - course.baselineSeconds).coerceIn(0, course.remainingMinutes * 60L)
        }
    }.toMutableMap()
    // IDs preserve credit order when untouched sessions move to another date.
    return plan.blocks.sortedBy { it.id }.map { block ->
        val used = minOf(block.minutes * 60L, credit.getValue(block.bossId))
        credit[block.bossId] = credit.getValue(block.bossId) - used
        StudyBlockProgress(block, used, block.bossId !in byId)
    }
}

fun remainingPlannerCourses(plan: StudyPlan, bosses: List<BossEntity>): List<PlannerCourse> =
    plan.config.courses.mapNotNull { course ->
        val boss = bosses.firstOrNull { it.id == course.bossId } ?: return@mapNotNull null
        if (boss.isCompleted) return@mapNotNull null
        val credit = (boss.timeSpentSeconds.coerceAtLeast(0) - course.baselineSeconds).coerceIn(0, course.remainingMinutes * 60L)
        val remainingMinutes = ((course.remainingMinutes * 60L - credit + 59) / 60).toInt()
        if (remainingMinutes == 0) null else course.copy(remainingMinutes = remainingMinutes, baselineSeconds = boss.timeSpentSeconds.coerceAtLeast(0))
    }

fun moveStudyBlock(plan: StudyPlan, blockId: Int, date: LocalDate, today: LocalDate, creditedSeconds: Long): StudyPlan? {
    val block = plan.blocks.firstOrNull { it.id == blockId } ?: return null
    val course = plan.config.courses.first { it.bossId == block.bossId }
    if (creditedSeconds != 0L || date < today || date < plan.config.start || date > course.finishBy) return null
    val used = plan.blocks.filter { it.date == date && it.id != blockId }.sumOf { it.minutes }
    if (used + block.minutes > plan.config.capacity(date)) return null
    return plan.copy(blocks = plan.blocks.map { if (it.id == blockId) it.copy(date = date) else it })
}

fun plannerHoursToMinutes(text: String): Int? = runCatching {
    require(text.length <= 12 && Regex("[0-9]+([.,][0-9]{1,2})?").matches(text.trim()))
    BigDecimal(text.trim().replace(',', '.')).multiply(BigDecimal(60))
        .setScale(0, RoundingMode.CEILING).intValueExact().takeIf { it in 1..60000 }
}.getOrNull()

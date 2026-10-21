package com.amkumirab.solostudying.domain.planner

/** Apply weekly quotas, then reuse the tested deadline-aware daily block builder. */
internal fun buildWeightedWeeklyPlan(config: PlannerConfig): StudyPlan {
    val policy = checkNotNull(config.weeklyScheduling)
    val remaining = config.courses.associate { it.bossId to it.remainingMinutes }.toMutableMap()
    val blocks = mutableListOf<StudyBlock>()
    val finish = config.courses.maxOf { it.finishBy }
    var week = plannerWeekStart(config.start)
    while (week <= finish) {
        val end = week.plusDays(6)
        val active = config.courses.filter { it.finishBy >= maxOf(week, config.start) && remaining.getValue(it.bossId) > 0 }
            .map { it.copy(remainingMinutes = remaining.getValue(it.bossId)) }
        if (active.isNotEmpty()) {
            val activeIds = active.map { it.bossId }.toSet()
            val input = config.copy(start = maxOf(week, config.start), courses = active,
                weeklyScheduling = policy.copy(courseMinuteLimits = policy.courseMinuteLimits.filterKeys { it in activeIds }))
            val perCourse = active.associate { course ->
                val future = weeklyPlannerCapacity(input, end.plusDays(1), course.finishBy,
                    policy.courseMinuteLimits[course.bossId] ?: Int.MAX_VALUE)
                course.bossId to (course.remainingMinutes - future).coerceAtLeast(0)
            }
            val throughDeadline = active.map { it.finishBy }.distinct().associateWith { deadline ->
                (active.filter { it.finishBy <= deadline }.sumOf { it.remainingMinutes } -
                    weeklyPlannerCapacity(input, end.plusDays(1), deadline)).coerceAtLeast(0)
            }
            val quota = allocateWeeklyHours(input, emptyList(), week, WeeklyAllocationNeeds(perCourse, throughDeadline))
            val courses = active.mapNotNull { course ->
                val minutes = quota.minutesByCourse[course.bossId] ?: 0
                if (minutes == 0) null else course.copy(remainingMinutes = minutes, finishBy = minOf(course.finishBy, end))
            }
            if (courses.isNotEmpty()) {
                val weekPlan = buildStudyPlan(input.copy(courses = courses, weeklyGoalMinutes = null, weeklyScheduling = null))
                check(weekPlan.shortfalls.isEmpty()) { "Weekly allocation could not fit its deadline capacities" }
                weekPlan.blocks.forEach { block ->
                    blocks += block.copy(id = blocks.size + 1)
                    remaining[block.bossId] = remaining.getValue(block.bossId) - block.minutes
                }
            }
        }
        week = week.plusWeeks(1)
    }
    return StudyPlan(config, blocks)
}

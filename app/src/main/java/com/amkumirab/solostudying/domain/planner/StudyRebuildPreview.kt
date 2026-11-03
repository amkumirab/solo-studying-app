package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate

/** Pure proposal using the same remaining-work, capacity and lock rules as confirmed rebuilds. */
internal fun previewStudyRebuild(snapshot: PlannerSnapshot, bosses: List<BossEntity>, today: LocalDate): LockedStudyRebuild {
    val plan = snapshot.plan ?: return LockedStudyRebuild(null, "No accepted plan is available.")
    val remaining = remainingPlannerCourses(plan, bosses, snapshot.subjects)
    return when {
        snapshot.availability?.totalMinutes == 0 -> LockedStudyRebuild(null,
            "Enable at least one study day in Availability before rebuilding. Your current plan is unchanged.")
        remaining.isEmpty() -> LockedStudyRebuild(null, "All available courses are complete. Edit answers to choose new goals.")
        remaining.any { it.finishBy !in today..today.plusDays(365) } -> LockedStudyRebuild(null,
            "A finish date is outside the next year. Edit answers to choose new dates before rebuilding.")
        else -> runCatching {
            rebuildWithStudyLocks(plan.config.copy(start = today, courses = remaining,
                weekdayMinutes = snapshot.availability?.weekdayMinutes ?: plan.config.weekdayMinutes,
                weeklyScheduling = null, studyWindows = snapshot.availability ?: plan.config.studyWindows)
                .withWeeklyScheduling(snapshot.subjects), plan, bosses, today)
        }.getOrElse { LockedStudyRebuild(null, "Could not rebuild this plan. Edit answers to review your course dates and capacity.") }
    }
}

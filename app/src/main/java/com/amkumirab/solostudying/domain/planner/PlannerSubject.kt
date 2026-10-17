package com.amkumirab.solostudying.domain.planner

import java.time.LocalDate
import java.util.Locale

enum class SubjectColor { Cyan, Gold, Rose, Green }

data class PlannerSubject(
    val bossId: Int,
    val name: String,
    val priority: Int = 3,
    val color: SubjectColor = SubjectColor.Cyan,
    val preferredWeeklyMinutes: Int? = null,
    val deadline: LocalDate? = null,
) {
    init {
        require(bossId > 0 && name.isNotBlank() && name.length <= 80 && name == name.trim())
        require(name.none { it.isISOControl() })
        require(priority in 1..5)
        require(preferredWeeklyMinutes == null || preferredWeeklyMinutes in 1..10080)
        require(deadline == null || deadline.year in 1..9999)
    }
}

fun validatePlannerSubjects(subjects: List<PlannerSubject>) {
    require(subjects.size <= 20 && subjects.distinctBy { it.bossId }.size == subjects.size)
    require(subjects.map { it.name.replace(Regex("\\s+"), " ").lowercase(Locale.ROOT) }.distinct().size == subjects.size)
}

data class PlannerSnapshot(val plan: StudyPlan? = null, val subjects: List<PlannerSubject> = emptyList()) {
    init { validatePlannerSubjects(subjects) }

    fun withoutSubject(bossId: Int): PlannerSnapshot {
        val courses = plan?.config?.courses?.filterNot { it.bossId == bossId }.orEmpty()
        val remainingPlan = if (courses.isEmpty()) null else plan?.copy(
            config = plan.config.copy(courses = courses), blocks = plan.blocks.filterNot { it.bossId == bossId },
        )
        return copy(plan = remainingPlan, subjects = subjects.filterNot { it.bossId == bossId })
    }
}

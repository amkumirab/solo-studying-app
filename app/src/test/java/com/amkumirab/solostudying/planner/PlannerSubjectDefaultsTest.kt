package com.amkumirab.solostudying.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import com.amkumirab.solostudying.domain.planner.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PlannerSubjectDefaultsTest {
    private val today = LocalDate.of(2026, 10, 17)
    private val goal = BossEntity(id = 1, name = "Operating Systems", difficulty = "Hard",
        requiredMinutes = 6000, timeSpentSeconds = 1600)
    private val course = PlannerCourse(1, 100, today.plusDays(6), 2, 100)
    private val plan = buildStudyPlan(PlannerConfig(today, listOf(course), List(7) { 60 }, 25))

    @Test fun `explicit rebuild adopts subject settings but retains recorded progress`() {
        val subject = PlannerSubject(1, "OS", 5, preferredWeeklyMinutes = 300, deadline = today.plusDays(30))
        val rebuilt = remainingPlannerCourses(plan, listOf(goal), listOf(subject)).single()
        assertEquals(75, rebuilt.remainingMinutes)
        assertEquals(1600L, rebuilt.baselineSeconds)
        assertEquals(5, rebuilt.priority)
        assertEquals(subject.deadline, rebuilt.finishBy)
        assertEquals(2, plan.config.courses.single().priority)
        assertEquals(course.finishBy, plan.config.courses.single().finishBy)
    }

    @Test fun `unspecified subject deadline keeps accepted finish date and legacy courses stay unchanged`() {
        val remaining = remainingPlannerCourses(plan, listOf(goal)).single()
        assertEquals(remaining, remainingPlannerCourses(plan, listOf(goal), emptyList()).single())
        val subject = PlannerSubject(1, "OS", 4)
        val rebuilt = remainingPlannerCourses(plan, listOf(goal.copy(deadlineDate = today.plusDays(1).toString())), listOf(subject)).single()
        assertEquals(course.finishBy, rebuilt.finishBy)
        assertEquals(4, rebuilt.priority)
    }
}

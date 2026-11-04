package com.amkumirab.solostudying.domain.planner

import com.amkumirab.solostudying.data.entity.BossEntity
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PlannerGuidanceTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private fun boss(id: Int = 1, seconds: Long = 0) = BossEntity(id = id, name = "Course $id",
        difficulty = "Hard", requiredMinutes = 600, timeSpentSeconds = seconds)
    private fun config(goal: Int? = 120) = PlannerConfig(monday, listOf(
        PlannerCourse(1, 240, monday.plusDays(7), 5, 0),
        PlannerCourse(2, 60, monday.plusDays(21), 3, 0)), List(7) { 120 }, 60, goal)
    private fun plan(goal: Int? = 120) = StudyPlan(config(goal), listOf(StudyBlock(1, 1, monday, 60),
        StudyBlock(2, 2, monday.plusDays(1), 60)))

    @Test fun `upcoming deadline reports exact uncovered work without inventing an exam`() {
        val source = plan()
        val advice = plannerGuidance(source, listOf(boss(), boss(2)), monday, monday)
        val deadline = advice.filterIsInstance<PlannerAdvice.Deadline>().single()
        assertEquals(1, deadline.bossId)
        assertEquals(180, deadline.uncoveredMinutes)
        assertEquals(monday.plusDays(7), deadline.finishBy)
        assertFalse(deadline.overdue)
        assertEquals(PlannerAdviceAction.ReviewPlan, deadline.action)
        assertEquals(source, plan())
    }

    @Test fun `missed slots do not cover remaining work and expired dates are overdue`() {
        val source = StudyPlan(config().copy(courses = listOf(config().courses.first().copy(finishBy = monday))),
            listOf(StudyBlock(1, 1, monday, 60)))
        val deadline = plannerGuidance(source, listOf(boss()), monday.plusDays(1), monday)
            .filterIsInstance<PlannerAdvice.Deadline>().single()
        assertTrue(deadline.overdue)
        assertEquals(240, deadline.uncoveredMinutes)
        assertEquals(PlannerAdviceAction.EditAnswers, deadline.action)
    }

    @Test fun `second-level credit and baseline round the deficit once not per slot`() {
        val source = StudyPlan(config().copy(courses = listOf(config().courses.first().copy(baselineSeconds = 100))),
            listOf(StudyBlock(1, 1, monday, 60), StudyBlock(2, 1, monday.plusDays(1), 60)))
        val deadline = plannerGuidance(source, listOf(boss(seconds = 131)), monday.plusDays(1), monday)
            .filterIsInstance<PlannerAdvice.Deadline>().single()
        assertEquals(180, deadline.uncoveredMinutes)
        val studied = plannerGuidance(source, listOf(boss(seconds = 100 + 240 * 60L)), monday, monday)
        assertTrue(studied.none { it is PlannerAdvice.Deadline || it is PlannerAdvice.Priority })
    }

    @Test fun `completed courses get no deadline or priority advice and missing goals require repair`() {
        val completed = plannerGuidance(plan(), listOf(boss().copy(isCompleted = true), boss(2)), monday, monday)
        assertTrue(completed.none { it is PlannerAdvice.Deadline && it.bossId == 1 || it is PlannerAdvice.Priority && it.bossId == 1 })
        val missing = plannerGuidance(plan(), listOf(boss(2)), monday, monday)
        assertTrue(missing.first() is PlannerAdvice.MissingCourses)
        assertEquals(1, (missing.first() as PlannerAdvice.MissingCourses).count)
        assertTrue(missing.none { it is PlannerAdvice.Deadline && it.bossId == 1 })
    }

    @Test fun `priority uses accepted values and selected week totals not the whole plan`() {
        val source = plan().copy(config = config().copy(courses = config().courses.map { it.copy(finishBy = monday.plusDays(30)) }))
        val current = plannerGuidance(source, listOf(boss(), boss(2)), monday, monday).filterIsInstance<PlannerAdvice.Priority>().single()
        assertEquals(1, current.bossId)
        assertEquals(5, current.priority)
        assertEquals(60, current.plannedMinutes)
        val next = plannerGuidance(source, listOf(boss(), boss(2)), monday, monday.plusWeeks(1)).filterIsInstance<PlannerAdvice.Priority>().single()
        assertEquals(0, next.plannedMinutes)
        assertEquals(PlannerAdviceAction.ReviewPlan, next.action)
        assertTrue(plannerGuidance(source, listOf(boss(), boss(2)), monday, monday.minusWeeks(1)).isEmpty())
    }

    @Test fun `weekly statuses distinguish budget shortage from feasible capacity and actual study`() {
        val full = plannerGuidance(plan(), listOf(boss(), boss(2)), monday, monday).filterIsInstance<PlannerAdvice.Weekly>().single()
        assertEquals(WeekPlanningStatus.FullyPlanned, full.status)
        assertEquals(120, full.plannedMinutes)
        assertNull(full.action)
        val short = plannerGuidance(plan(900), listOf(boss(), boss(2)), monday, monday).filterIsInstance<PlannerAdvice.Weekly>().single()
        assertEquals(60, short.budgetGapMinutes)
        assertEquals(PlannerAdviceAction.Availability, short.action)
        val unfilled = plannerGuidance(plan(180), listOf(boss(), boss(2)), monday, monday).filterIsInstance<PlannerAdvice.Weekly>().single()
        assertEquals(WeekPlanningStatus.UnfilledGoal, unfilled.status)
        assertEquals(60, unfilled.goalGapMinutes)
        assertEquals(PlannerAdviceAction.EditAnswers, unfilled.action)
    }

    @Test fun `zero unset outside-plan and completed goals never encourage extra allocation`() {
        listOf(null, 0).forEach { goal ->
            assertTrue(plannerGuidance(plan(goal), listOf(boss(), boss(2)), monday, monday).none { it is PlannerAdvice.Weekly })
        }
        assertTrue(plannerGuidance(plan(), listOf(boss(), boss(2)), monday, monday.plusWeeks(54)).isEmpty())
        assertTrue(plannerGuidance(plan(), listOf(boss().copy(isCompleted = true), boss(2).copy(isCompleted = true)), monday, monday)
            .none { it.action != null })
        assertTrue(runCatching { plannerGuidance(plan(), listOf(boss()), monday, monday.plusDays(1)) }.isFailure)
    }

    @Test fun `urgent advice is bounded deterministic and suppresses same-course priority duplication`() {
        repeat(60) { seed ->
            val courses = (1..20).map { PlannerCourse(it, 60 + seed, monday.plusDays((it % 15).toLong()), it % 5 + 1, 0) }
            val source = StudyPlan(config().copy(courses = courses), emptyList())
            val bosses = courses.map { boss(it.bossId) }
            val advice = plannerGuidance(source, bosses, monday, monday)
            assertTrue(advice.size <= 3)
            assertEquals(advice, plannerGuidance(source.copy(config = source.config.copy(courses = courses.reversed())), bosses.reversed(), monday, monday))
            val deadlineId = advice.filterIsInstance<PlannerAdvice.Deadline>().single().bossId
            assertTrue(advice.none { it is PlannerAdvice.Priority && it.bossId == deadlineId })
        }
    }

    @Test fun `locked future slots cover work and deadline urgency has an explicit fourteen-day boundary`() {
        val windows = defaultWeeklyAvailability(List(7) { 120 })
        val course = config().courses.first().copy(remainingMinutes = 60, finishBy = monday.plusDays(14))
        val source = StudyPlan(config().copy(courses = listOf(course), studyWindows = windows),
            listOf(StudyBlock(1, 1, monday.plusDays(1), 60, 1020, locked = true)))
        val near = plannerGuidance(source, listOf(boss()), monday, monday).filterIsInstance<PlannerAdvice.Deadline>().single()
        assertEquals(0, near.uncoveredMinutes)
        assertEquals(source.blocks, source.copy().blocks)
        val later = source.copy(config = source.config.copy(courses = listOf(course.copy(finishBy = monday.plusDays(15)))))
        assertTrue(plannerGuidance(later, listOf(boss()), monday, monday).none { it is PlannerAdvice.Deadline })
    }
}
